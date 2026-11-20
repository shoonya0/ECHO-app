package com.shoonya.echo.features.chat.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.domain.UserIdProvider
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.PresenceStatus
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import com.shoonya.echo.features.chat.domain.usecase.CreateDirectChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getChatListUseCase: GetChatListUseCase,
    private val createDirectChatUseCase: CreateDirectChatUseCase,
    private val wsRepository: WebSocketRepository,
    userIdProvider: UserIdProvider
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatListUiState(currentUserId = userIdProvider.getUserId() ?: "")
    )
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatListEvent>()
    val events: SharedFlow<ChatListEvent> = _events.asSharedFlow()

    // Cross-feature events forwarded up to HomeScreen (e.g., presence)
    private val _crossFeatureEvents = MutableSharedFlow<ChatListCrossFeatureEvent>()
    val crossFeatureEvents: SharedFlow<ChatListCrossFeatureEvent> =
        _crossFeatureEvents.asSharedFlow()

    private var cachedContactIds: List<String> = emptyList()

    init {
        // Connect WebSocket once (shared singleton) and start collecting presence/chat events
        viewModelScope.launch {
            wsRepository.connect()
            collectWsEvents()
        }
    }

    fun loadChats(contactIds: List<String>) {
        cachedContactIds = contactIds
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getChatListUseCase(contactIds)
                .onSuccess { chats ->
                    _state.update { it.copy(isLoading = false, chats = chats) }
                }
                .onFailure { err ->
                    Timber.tag("ChatListVM").e(err, "loadChats failed")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage()
                        )
                    }
                }
        }
    }

    fun createDirectChat(userId: String) {
        viewModelScope.launch {
            createDirectChatUseCase(userId)
                .onSuccess { chat ->
                    _events.emit(ChatListEvent.NavigateToChat(chat.id))
                }
                .onFailure { err ->
                    Timber.tag("ChatListVM").e(err, "createDirectChat failed: %s", userId)
                    _events.emit(ChatListEvent.ShowSnackbar(err.toUserMessage()))
                }
        }
    }

    private suspend fun collectWsEvents() {
        wsRepository.events.collect { event ->
            when (event) {
                is DomainEvent.NewMessage -> onNewMessage(event)

                is DomainEvent.PresenceUpdate -> {
                    _crossFeatureEvents.emit(
                        ChatListCrossFeatureEvent.PresenceUpdate(event.userId, event.status)
                    )
                }

                is DomainEvent.UserJoined,
                is DomainEvent.UserLeft -> {
                    // Refresh chat list so new/removed chats appear
                    if (cachedContactIds.isNotEmpty()) {
                        loadChats(cachedContactIds)
                    }
                }

                else -> { /* no-op for chat list */ }
            }
        }
    }

    /**
     * Shows a live message as its chat's preview and moves the chat up, from the event alone: a
     * reload costs a `POST chats/direct` and a `GET messages` per contact (#28). A chat that
     * isn't listed is left out; the next load (resume, pull to refresh) picks it up.
     */
    private fun onNewMessage(event: DomainEvent.NewMessage) {
        _state.update { state ->
            val chat = state.chats.firstOrNull { it.id == event.chatId } ?: return@update state
            val current = chat.lastMessage
            if (current != null && current.createdAt > event.message.createdAt) return@update state
            val updated = chat.copy(lastMessage = event.message)
            state.copy(
                chats = state.chats
                    .map { if (it.id == updated.id) updated else it }
                    // Same order as ChatRepository.getChatList: newest first.
                    .sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
            )
        }
    }
}

sealed interface ChatListEvent {
    data class NavigateToChat(val chatId: String) : ChatListEvent
    data class ShowSnackbar(val message: String) : ChatListEvent
}

sealed interface ChatListCrossFeatureEvent {
    data class PresenceUpdate(val userId: String, val status: PresenceStatus) :
        ChatListCrossFeatureEvent
}
