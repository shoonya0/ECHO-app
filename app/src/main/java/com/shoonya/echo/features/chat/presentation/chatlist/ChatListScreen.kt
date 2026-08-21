package com.shoonya.echo.features.chat.presentation.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shoonya.echo.core.presentation.InitialsAvatar
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.core.util.toRelativeTime
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.contacts.domain.model.Contact
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    contactIds: List<String>,
    favorites: List<Contact> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatClicked: (String) -> Unit,
    onCreateDirectChat: (String) -> Unit = {},
    onNewChat: () -> Unit = {},
    onSuggestionsClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(contactIds) {
        viewModel.loadChats(contactIds)
    }

    // Robust guarantee: re-fetch chat list whenever Chats tab becomes visible,
    // even if contact IDs are unchanged (covers edge cases after contact refresh).
    LifecycleResumeEffect(contactIds) {
        viewModel.loadChats(contactIds)
        onPauseOrDispose { /* no-op */ }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatListEvent.NavigateToChat -> onChatClicked(event.chatId)
                is ChatListEvent.ShowSnackbar -> { /* handled by parent scaffold */ }
            }
        }
    }

    ChatListContent(
        chats = state.chats,
        favorites = favorites,
        currentUserId = state.currentUserId,
        isLoading = state.isLoading,
        error = state.error,
        onRefresh = { viewModel.loadChats(contactIds) },
        onChatClicked = onChatClicked,
        onCreateDirectChat = onCreateDirectChat,
        onNewChat = onNewChat,
        onSuggestionsClicked = onSuggestionsClicked,
        onToggleFavorite = onToggleFavorite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatListContent(
    chats: List<Chat>,
    favorites: List<Contact> = emptyList(),
    currentUserId: String = "",
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onChatClicked: (String) -> Unit,
    onCreateDirectChat: (String) -> Unit,
    onNewChat: () -> Unit = {},
    onSuggestionsClicked: () -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    IconButton(onClick = onSuggestionsClicked) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Suggestions",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSuggestionsClicked) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New chat",
                )
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                error != null && chats.isEmpty() && favorites.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                !isLoading && chats.isEmpty() && favorites.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No chats yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start a chat from the + button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                    ) {
                        items(chats, key = { it.id }) { chat ->
                            ChatListItem(
                                chat = chat,
                                onClick = { onChatClicked(chat.id) },
                            )
                            ChatStarToggle(
                                chat = chat,
                                favorites = favorites,
                                currentUserId = currentUserId,
                                onToggleFavorite = onToggleFavorite,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatStarToggle(
    chat: Chat,
    favorites: List<Contact>,
    currentUserId: String,
    onToggleFavorite: (String) -> Unit,
) {
    // Only show star toggle on direct chats
    if (chat.type != ChatType.DIRECT) return

    // Target the other participant (the peer), not the current user. The backend
    // returns all chat members in map order (lexicographic by user ID for Go maps),
    // so firstOrNull() can be the current user — which breaks the favourite toggle.
    val participant = chat.participants.firstOrNull { it.userId != currentUserId }
        ?: chat.participants.firstOrNull()
        ?: return
    val favoriteContact = favorites.firstOrNull {
        it.id == participant.userId || it.username == participant.username
    }
    val isFavorite = favoriteContact != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(
            onClick = { onToggleFavorite(favoriteContact?.id ?: participant.userId) },
            modifier = Modifier
                .size(32.dp)
                .padding(end = 8.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box {
            InitialsAvatar(
                name = chat.name,
                avatarUrl = chat.avatar,
                size = 48.dp,
            )
            val onlineParticipant = chat.participants.firstOrNull { it.isOnline }
            if (onlineParticipant != null && chat.type == ChatType.DIRECT) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                chat.lastMessage?.createdAt?.let { time ->
                    Text(
                        text = time.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.lastMessage?.content?.take(60) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else "${chat.unreadCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

// Previews
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatListPreview() {
    EchoTheme {
        ChatListContent(
            chats = listOf(
                Chat(
                    id = "1",
                    type = ChatType.DIRECT,
                    name = "Alice",
                    description = "",
                    avatar = "",
                    participants = emptyList(),
                    ownerId = null,
                    participantCount = 2,
                    messageCount = 5,
                    unreadCount = 2,
                    lastMessage = Message(
                        id = "m1",
                        chatId = "1",
                        senderId = "alice",
                        senderName = "alice",
                        senderDisplayName = "Alice",
                        senderAvatar = "",
                        content = "Hey, how are you?",
                        type = MessageType.TEXT,
                        attachments = emptyList(),
                        reactions = emptyMap(),
                        isEdited = false,
                        readByUserIds = emptySet(),
                        createdAt = Instant.now().minusSeconds(300),
                    ),
                    settings = ChatSettings(),
                    createdAt = Instant.now().minusSeconds(3600),
                    updatedAt = null,
                ),
                Chat(
                    id = "2",
                    type = ChatType.DIRECT,
                    name = "Bob",
                    description = "",
                    avatar = "",
                    participants = emptyList(),
                    ownerId = null,
                    participantCount = 2,
                    messageCount = 120,
                    unreadCount = 0,
                    lastMessage = Message(
                        id = "m2",
                        chatId = "2",
                        senderId = "bob",
                        senderName = "bob",
                        senderDisplayName = "Bob",
                        senderAvatar = "",
                        content = "See you tomorrow!",
                        type = MessageType.TEXT,
                        attachments = emptyList(),
                        reactions = emptyMap(),
                        isEdited = false,
                        readByUserIds = emptySet(),
                        createdAt = Instant.now().minusSeconds(1800),
                    ),
                    settings = ChatSettings(),
                    createdAt = Instant.now().minusSeconds(86400),
                    updatedAt = null,
                ),
            ),
            favorites = emptyList(),
            isLoading = false,
            error = null,
            onRefresh = {},
            onChatClicked = {},
            onCreateDirectChat = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Composable
private fun ChatListEmptyPreview() {
    EchoTheme {
        ChatListContent(
            chats = emptyList(),
            favorites = emptyList(),
            isLoading = false,
            error = null,
            onRefresh = {},
            onChatClicked = {},
            onCreateDirectChat = {},
        )
    }
}