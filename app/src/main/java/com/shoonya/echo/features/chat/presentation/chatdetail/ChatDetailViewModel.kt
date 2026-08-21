package com.shoonya.echo.features.chat.presentation.chatdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.domain.UserIdProvider
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.PresenceStatus
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import com.shoonya.echo.features.chat.domain.usecase.AddReactionUseCase
import com.shoonya.echo.features.chat.domain.usecase.CreateDirectChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatMessagesUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.JoinChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.LeaveChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.MarkReadUseCase
import com.shoonya.echo.features.chat.domain.usecase.RemoveReactionUseCase
import com.shoonya.echo.features.chat.domain.usecase.SendMessageUseCase
import com.shoonya.echo.features.chat.domain.usecase.SetTypingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createDirectChatUseCase: CreateDirectChatUseCase,
    private val getChatUseCase: GetChatUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val joinChatUseCase: JoinChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val setTypingUseCase: SetTypingUseCase,
    private val markReadUseCase: MarkReadUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val wsRepository: WebSocketRepository,
    private val userIdProvider: UserIdProvider,
) : ViewModel() {

    private val rawChatId: String = savedStateHandle.get<String>("chatId") ?: ""
    private var resolvedChatId: String = rawChatId
    private val currentUserId: String = userIdProvider.getUserId() ?: ""

    private val _state = MutableStateFlow(
        ChatDetailUiState(currentUserId = currentUserId)
    )
    val state: StateFlow<ChatDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatDetailEvent>()
    val events: SharedFlow<ChatDetailEvent> = _events.asSharedFlow()

    private var typingJob: Job? = null

    // Tracks in-flight optimistic sends: requestId -> message content.
    // Used to reconcile a server broadcast of our own sent message (which may
    // arrive before or even without an ack) with the local optimistic bubble.
    private val inFlightSends = mutableMapOf<String, String>()

    init {
        if (rawChatId.startsWith("new:")) {
            val targetUserId = rawChatId.removePrefix("new:")
            _state.update { it.copy(isCreatingDirectChat = true, connectionStatus = WsConnectionStatus.CONNECTING) }
            viewModelScope.launch {
                createDirectChatUseCase(targetUserId)
                    .onSuccess { chat ->
                        resolvedChatId = chat.id
                        _state.update { it.copy(isCreatingDirectChat = false, connectionStatus = WsConnectionStatus.CONNECTING) }
                        loadChatMetadata()
                        loadMessages()
                        connectAndJoin()
                    }
                    .onFailure { err ->
                        _state.update {
                            it.copy(
                                isCreatingDirectChat = false,
                                error = err.toUserMessage(),
                                connectionStatus = WsConnectionStatus.DISCONNECTED,
                            )
                        }
                    }
            }
        } else {
            _state.update { it.copy(connectionStatus = WsConnectionStatus.CONNECTING) }
            loadChatMetadata()
            loadMessages()
            viewModelScope.launch {
                connectAndJoin()
            }
        }
    }

    private suspend fun connectAndJoin() {
        wsRepository.connect()
        joinChat()
        collectWsEvents()
        collectConnectionStatus()
    }

    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }

        // Debounced typing indicator: 300ms
        typingJob?.cancel()
        if (text.isNotBlank()) {
            typingJob = viewModelScope.launch {
                delay(300)
                setTypingUseCase(resolvedChatId, isTyping = true)
            }
        } else {
            viewModelScope.launch {
                setTypingUseCase(resolvedChatId, isTyping = false)
            }
        }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        // Stop typing indicator
        viewModelScope.launch {
            setTypingUseCase(resolvedChatId, isTyping = false)
        }

        val requestId = java.util.UUID.randomUUID().toString().take(8)
        val optimisticMessage = Message(
            id = requestId,
            chatId = resolvedChatId,
            senderId = currentUserId,
            senderName = "",
            senderDisplayName = "",
            senderAvatar = "",
            content = text,
            type = com.shoonya.echo.features.chat.domain.model.MessageType.TEXT,
            attachments = emptyList(),
            reactions = emptyMap(),
            isEdited = false,
            readByUserIds = emptySet(),
            createdAt = java.time.Instant.now(),
        )

        inFlightSends[requestId] = text

        // Optimistic update: append message and clear input immediately
        _state.update {
            it.copy(
                inputText = "",
                isSending = true,
                messages = mergeMessages(it.messages, listOf(optimisticMessage)),
            )
        }

        viewModelScope.launch {
            try {
                sendMessageUseCase(resolvedChatId, text, currentUserId, requestId = requestId)
                    .onSuccess { serverMessage ->
                        inFlightSends.remove(requestId)
                        _state.update {
                            it.copy(
                                isSending = false,
                                messages = replaceOptimistic(it.messages, requestId, serverMessage),
                            )
                        }
                    }
                    .onFailure { err ->
                        Timber.tag("ChatDetailVM").e(err, "sendMessage failed: %s", resolvedChatId)
                        // If the server already delivered this message as a broadcast
                        // (reconciled via the WS NewMessage event), don't restore the
                        // input or surface an error — the message is actually on the wire.
                        if (inFlightSends.remove(requestId) == null) {
                            _state.update {
                                it.copy(
                                    isSending = false,
                                    messages = removeOptimistic(it.messages, requestId),
                                )
                            }
                        } else if (err is TimeoutCancellationException) {
                            // Uncertain outcome: the server likely saved the message even
                            // though we never received a correlated ack. Keep the optimistic
                            // bubble and the cleared input so the user's message doesn't
                            // vanish. Re-add the entry so a late broadcast can still swap in
                            // the real server id.
                            inFlightSends[requestId] = text
                            _state.update { it.copy(isSending = false) }
                        } else {
                            _state.update {
                                it.copy(
                                    isSending = false,
                                    inputText = text, // restore input so user can retry
                                    messages = removeOptimistic(it.messages, requestId),
                                )
                            }
                            _events.emit(ChatDetailEvent.ShowSnackbar(err.toUserMessage()))
                        }
                    }
            } catch (e: Exception) {
                Timber.tag("ChatDetailVM").e(e, "sendMessage exception: %s", resolvedChatId)
                if (inFlightSends.remove(requestId) == null) {
                    _state.update {
                        it.copy(
                            isSending = false,
                            messages = removeOptimistic(it.messages, requestId),
                        )
                    }
                } else if (e is TimeoutCancellationException) {
                    inFlightSends[requestId] = text
                    _state.update { it.copy(isSending = false) }
                } else {
                    _state.update {
                        it.copy(
                            isSending = false,
                            inputText = text,
                            messages = removeOptimistic(it.messages, requestId),
                        )
                    }
                    _events.emit(ChatDetailEvent.ShowSnackbar("Failed to send message"))
                }
            }
        }
    }

    fun loadMore() {
        val currentMessages = _state.value.messages
        if (_state.value.isLoadingMore || !_state.value.hasMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            getChatMessagesUseCase(resolvedChatId, offset = currentMessages.size)
                .onSuccess { (newMessages, total) ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            messages = mergeMessages(it.messages, newMessages),
                            totalCount = total,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.tag("ChatDetailVM").e(err, "loadMore failed: %s", resolvedChatId)
                    _state.update { it.copy(isLoadingMore = false) }
                    _events.emit(ChatDetailEvent.ShowSnackbar(err.toUserMessage()))
                }
        }
    }

    fun markMessagesRead(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            markReadUseCase(resolvedChatId, messageIds)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            addReactionUseCase(resolvedChatId, messageId, emoji)
        }
    }

    fun removeReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            removeReactionUseCase(resolvedChatId, messageId, emoji)
        }
    }

    private fun loadChatMetadata() {
        if (resolvedChatId.isBlank()) return
        viewModelScope.launch {
            getChatUseCase(resolvedChatId)
                .onSuccess { chat ->
                    _state.update {
                        it.copy(
                            chatName = chat.name,
                            chatType = chat.type,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.tag("ChatDetailVM").e(err, "loadChatMetadata failed: %s", resolvedChatId)
                }
        }
    }

    private fun loadMessages(offset: Int = 0) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = offset == 0, error = null) }
            getChatMessagesUseCase(resolvedChatId, offset = offset)
                .onSuccess { (messages, total) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            totalCount = total,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.tag("ChatDetailVM").e(err, "loadMessages failed: %s", resolvedChatId)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }

    private suspend fun joinChat() {
        joinChatUseCase(resolvedChatId, currentUserId)
    }

    private suspend fun collectWsEvents() {
        wsRepository.events.collect { event ->
            when (event) {
                is DomainEvent.NewMessage -> {
                    if (event.chatId == resolvedChatId) {
                        val message = event.message
                        // Reconcile our own optimistic bubble when the server broadcasts
                        // back the message we just sent.
                        val matchedRequestId = if (message.senderId == currentUserId) {
                            inFlightSends.entries.firstOrNull { it.value == message.content }?.key
                        } else {
                            null
                        }

                        _state.update { state ->
                            var messages = mergeMessages(state.messages, listOf(message))
                            if (matchedRequestId != null) {
                                messages = messages.filterNot { it.id == matchedRequestId }
                            }
                            state.copy(messages = messages)
                        }

                        if (matchedRequestId != null) {
                            inFlightSends.remove(matchedRequestId)
                        }
                    }
                }
                is DomainEvent.TypingUpdate -> {
                    if (event.chatId == resolvedChatId) {
                        if (event.isTyping) {
                            _state.update { it.copy(typingUserIds = it.typingUserIds + event.userId) }
                        } else {
                            _state.update { it.copy(typingUserIds = it.typingUserIds - event.userId) }
                        }
                    }
                }
                is DomainEvent.PresenceUpdate -> {
                    // Update participant online status in messages list if the sender is in this chat
                    _state.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.senderId == event.userId) {
                                    msg // Future: could set some presence indicator
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
                is DomainEvent.ReactionUpdate -> {
                    if (event.chatId == resolvedChatId) {
                        _state.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == event.messageId) {
                                        val currentReactions = msg.reactions.toMutableMap()
                                        val existing = currentReactions[event.emoji] ?: com.shoonya.echo.features.chat.domain.model.Reaction(
                                            count = 0,
                                            userNames = emptyList(),
                                            hasReacted = false,
                                        )
                                        if (event.action == "add") {
                                            currentReactions[event.emoji] = existing.copy(
                                                count = existing.count + 1,
                                                hasReacted = true,
                                            )
                                        } else {
                                            val newCount = (existing.count - 1).coerceAtLeast(0)
                                            if (newCount == 0) {
                                                currentReactions.remove(event.emoji)
                                            } else {
                                                currentReactions[event.emoji] = existing.copy(
                                                    count = newCount,
                                                    hasReacted = false,
                                                )
                                            }
                                        }
                                        msg.copy(reactions = currentReactions)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
                }
                is DomainEvent.ReadReceipt -> {
                    if (event.chatId == resolvedChatId) {
                        _state.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id in event.messageIds) {
                                        msg.copy(readByUserIds = msg.readByUserIds + event.userId)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
                }
                is DomainEvent.BackendError -> {
                    val message = com.shoonya.echo.core.util.ErrorMapper.mapBackendError(
                        event.code,
                        event.message
                    )
                    _events.emit(ChatDetailEvent.ShowSnackbar(message))
                }
                is DomainEvent.ResponseAck,
                is DomainEvent.UserJoined,
                is DomainEvent.UserLeft -> { /* handled by chat list or no-op in detail */ }
            }
        }
    }

    private suspend fun collectConnectionStatus() {
        wsRepository.connectionStatus.collect { status ->
            _state.update { it.copy(connectionStatus = status) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            leaveChatUseCase(resolvedChatId)
        }
    }

    /** Merges existing and incoming messages, dedupes by id, and sorts oldest-first. */
    private fun mergeMessages(
        existing: List<Message>,
        incoming: List<Message>,
    ): List<Message> = (existing + incoming)
        .distinctBy { it.id }
        .sortedBy { it.createdAt }

    /** Replaces an optimistic placeholder (identified by [optimisticId]) with the server-confirmed [serverMessage]. */
    private fun replaceOptimistic(
        messages: List<Message>,
        optimisticId: String,
        serverMessage: Message,
    ): List<Message> = messages.map { msg ->
        if (msg.id == optimisticId) serverMessage else msg
    }
        .distinctBy { it.id }
        .sortedBy { it.createdAt }

    /** Removes an optimistic placeholder by its [optimisticId] (e.g., on send failure). */
    private fun removeOptimistic(
        messages: List<Message>,
        optimisticId: String,
    ): List<Message> = messages.filter { it.id != optimisticId }
}

sealed interface ChatDetailEvent {
    data class ShowSnackbar(val message: String) : ChatDetailEvent
}