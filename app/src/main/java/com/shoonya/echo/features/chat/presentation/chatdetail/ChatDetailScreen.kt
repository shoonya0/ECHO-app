package com.shoonya.echo.features.chat.presentation.chatdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.core.util.formatDaySeparator
import com.shoonya.echo.core.util.toRelativeTime
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ── Chat list item sealed type for WhatsApp-style day separators ──

private sealed interface ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem
    data class DateSeparatorItem(val label: String) : ChatListItem
}

/**
 * Builds a flat list of [ChatListItem] from a chronologically sorted (oldest-first) message list,
 * inserting a [DateSeparatorItem] whenever the calendar date changes between consecutive messages.
 */
private fun buildChatListItems(
    messages: List<Message>,
    now: Instant,
): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()

    val items = mutableListOf<ChatListItem>()
    val zone = ZoneId.systemDefault()
    var lastDate: LocalDate? = null

    for (msg in messages) {
        val msgDate = msg.createdAt.atZone(zone).toLocalDate()
        if (msgDate != lastDate) {
            items.add(ChatListItem.DateSeparatorItem(msg.createdAt.formatDaySeparator(now)))
            lastDate = msgDate
        }
        items.add(ChatListItem.MessageItem(msg))
    }
    return items
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatDetailEvent.ShowSnackbar -> { /* handled by parent scaffold */ }
            }
        }
    }

    ChatDetailContent(
        chatId = "",
        chatName = state.chatName,
        messages = state.messages,
        isLoading = state.isLoading,
        isLoadingMore = state.isLoadingMore,
        hasMore = state.hasMore,
        error = state.error,
        inputText = state.inputText,
        isSending = state.isSending,
        currentUserId = state.currentUserId,
        typingUserIds = state.typingUserIds,
        connectionStatus = state.connectionStatus,
        onBack = onBack,
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::sendMessage,
        onLoadMore = viewModel::loadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatDetailContent(
    chatId: String,
    chatName: String,
    messages: List<Message>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    inputText: String,
    isSending: Boolean,
    currentUserId: String,
    typingUserIds: Set<String>,
    connectionStatus: WsConnectionStatus,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val now = remember { Instant.now() }
    val chatItems = remember(messages) { buildChatListItems(messages, now) }

    // Mark messages as read when visible
    LaunchedEffect(messages.size) {
        val unreadByMe = messages
            .filter { it.senderId != currentUserId && currentUserId !in it.readByUserIds }
            .map { it.id }
        // markRead handled via ViewModel
    }

    // Detect scroll to top for pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index == 0 && hasMore && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    // Auto-scroll to bottom on initial load & new messages (WhatsApp-like)
    val lastItemCount = chatItems.size
    LaunchedEffect(lastItemCount) {
        if (chatItems.isNotEmpty()) {
            listState.animateScrollToItem(chatItems.lastIndex)
        }
    }

    // Preserve scroll position during loadMore by anchoring the first visible pre-paginated item
    val savedItemKey = rememberSaveable { mutableStateOf<String?>(null) }
    if (isLoadingMore && savedItemKey.value == null && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
        savedItemKey.value = listState.layoutInfo.visibleItemsInfo.first().key as? String
    }
    LaunchedEffect(isLoadingMore) {
        if (!isLoadingMore && savedItemKey.value != null) {
            val anchorKey = savedItemKey.value!!
            savedItemKey.value = null
            val index = messages.indexOfFirst { it.id == anchorKey }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(chatName.ifBlank { "Chat" }) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
                // Connection status banner
                AnimatedVisibility(
                    visible = connectionStatus in setOf(
                        WsConnectionStatus.RECONNECTING,
                        WsConnectionStatus.FAILED,
                    ),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (connectionStatus == WsConnectionStatus.FAILED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = when (connectionStatus) {
                                WsConnectionStatus.RECONNECTING -> "Reconnecting…"
                                WsConnectionStatus.FAILED -> "Connection lost — check your network"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Typing indicator
                AnimatedVisibility(
                    visible = typingUserIds.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "Typing…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                // Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChanged,
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSend,
                        enabled = inputText.isNotBlank() && !isSending,
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            !isLoading && messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No messages yet. Say hello!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }

                    items(chatItems, key = { item ->
                        when (item) {
                            is ChatListItem.MessageItem -> item.message.id.ifBlank { item.message.createdAt.toString() }
                            is ChatListItem.DateSeparatorItem -> "sep_${item.label}"
                        }
                    }) { item ->
                        when (item) {
                            is ChatListItem.DateSeparatorItem -> DateSeparator(label = item.label)
                            is ChatListItem.MessageItem -> MessageBubble(
                                message = item.message,
                                isMine = item.message.senderId == currentUserId,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Date separator composable ──

@Composable
private fun DateSeparator(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

// ── Message bubble ──

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Row(
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isMine) 12.dp else 4.dp,
                            topEnd = 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = if (isMine) 4.dp else 12.dp,
                        )
                    )
                    .background(
                        if (isMine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )

                    // Reactions row
                    if (message.reactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            message.reactions.forEach { (emoji, reaction) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "$emoji ${reaction.count}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = message.createdAt.toRelativeTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        // Read receipt ticks for own messages
                        if (isMine) {
                            if (message.readByUserIds.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = "Read",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Previews ──

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatDetailPreview() {
    EchoTheme {
        ChatDetailContent(
            chatId = "c1",
            chatName = "Alice",
            messages = listOf(
                Message(
                    id = "1",
                    chatId = "c1",
                    senderId = "alice",
                    senderName = "alice",
                    senderDisplayName = "Alice",
                    senderAvatar = "",
                    content = "Hey! How's it going?",
                    type = MessageType.TEXT,
                    attachments = emptyList(),
                    reactions = emptyMap(),
                    isEdited = false,
                    readByUserIds = emptySet(),
                    createdAt = Instant.now().minusSeconds(1800),
                ),
                Message(
                    id = "2",
                    chatId = "c1",
                    senderId = "me",
                    senderName = "me",
                    senderDisplayName = "Me",
                    senderAvatar = "",
                    content = "I'm doing great! Just finished the project.",
                    type = MessageType.TEXT,
                    attachments = emptyList(),
                    reactions = mutableMapOf("❤️" to com.shoonya.echo.features.chat.domain.model.Reaction(1, listOf("alice"), hasReacted = true)),
                    isEdited = false,
                    readByUserIds = setOf("alice"),
                    createdAt = Instant.now().minusSeconds(1200),
                ),
            ),
            isLoading = false,
            isLoadingMore = false,
            hasMore = true,
            error = null,
            inputText = "",
            isSending = false,
            currentUserId = "me",
            typingUserIds = emptySet(),
            connectionStatus = WsConnectionStatus.CONNECTED,
            onBack = {},
            onInputChanged = {},
            onSend = {},
            onLoadMore = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Composable
private fun ChatDetailEmptyPreview() {
    EchoTheme {
        ChatDetailContent(
            chatId = "c2",
            chatName = "New Chat",
            messages = emptyList(),
            isLoading = false,
            isLoadingMore = false,
            hasMore = false,
            error = null,
            inputText = "Hello!",
            isSending = false,
            currentUserId = "me",
            typingUserIds = emptySet(),
            connectionStatus = WsConnectionStatus.CONNECTED,
            onBack = {},
            onInputChanged = {},
            onSend = {},
            onLoadMore = {},
        )
    }
}

@Preview(name = "Reconnecting", showBackground = true)
@Composable
private fun ChatDetailReconnectingPreview() {
    EchoTheme {
        ChatDetailContent(
            chatId = "c3",
            chatName = "Alice",
            messages = emptyList(),
            isLoading = false,
            isLoadingMore = false,
            hasMore = false,
            error = null,
            inputText = "",
            isSending = false,
            currentUserId = "me",
            typingUserIds = setOf("alice"),
            connectionStatus = WsConnectionStatus.RECONNECTING,
            onBack = {},
            onInputChanged = {},
            onSend = {},
            onLoadMore = {},
        )
    }
}