package com.shoonya.echo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailContent
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMessage = Message(
        id = "msg_1",
        chatId = "chat_1",
        senderId = "alice",
        senderName = "alice",
        senderDisplayName = "Alice",
        senderAvatar = "",
        content = "Hey! How are you?",
        type = MessageType.TEXT,
        attachments = emptyList(),
        reactions = emptyMap(),
        isEdited = false,
        readByUserIds = emptySet(),
        createdAt = Instant.now().minusSeconds(300)
    )

    @Test
    fun givenMessages_rendersMessageContent() {
        composeTestRule.setContent {
            EchoTheme {
                ChatDetailContent(
                    chatId = "chat_1",
                    chatName = "Alice",
                    messages = listOf(sampleMessage),
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
                    onLoadMore = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Alice")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Hey! How are you?")
            .assertIsDisplayed()
    }

    @Test
    fun givenEmptyState_showsNoMessagesPlaceholder() {
        composeTestRule.setContent {
            EchoTheme {
                ChatDetailContent(
                    chatId = "chat_2",
                    chatName = "New Chat",
                    messages = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = false,
                    error = null,
                    inputText = "",
                    isSending = false,
                    currentUserId = "me",
                    typingUserIds = emptySet(),
                    connectionStatus = WsConnectionStatus.CONNECTED,
                    onBack = {},
                    onInputChanged = {},
                    onSend = {},
                    onLoadMore = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("No messages yet. Say hello!")
            .assertIsDisplayed()
    }

    @Test
    fun givenReconnectingStatus_showsReconnectingBanner() {
        composeTestRule.setContent {
            EchoTheme {
                ChatDetailContent(
                    chatId = "chat_3",
                    chatName = "Alice",
                    messages = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = true,
                    error = null,
                    inputText = "",
                    isSending = false,
                    currentUserId = "me",
                    typingUserIds = emptySet(),
                    connectionStatus = WsConnectionStatus.RECONNECTING,
                    onBack = {},
                    onInputChanged = {},
                    onSend = {},
                    onLoadMore = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Reconnecting…")
            .assertIsDisplayed()
    }

    private fun message(i: Int, createdAt: Instant) = sampleMessage.copy(
        id = "msg_$i",
        content = "message $i",
        createdAt = createdAt
    )

    // Messages 20..59 from today, oldest first; message 59 is the newest.
    private val todayPage = (20..59).map {
        message(it, Instant.now().minusSeconds((60L - it) * 60))
    }

    @Test
    fun scrollingToTop_afterFirstPageLoads_requestsOlderMessages() {
        var messages by mutableStateOf(emptyList<Message>())
        var hasMore by mutableStateOf(false)
        var isLoading by mutableStateOf(true)
        var loadMoreCalls = 0
        composeTestRule.setContent {
            EchoTheme {
                ChatDetailContent(
                    chatId = "chat_1",
                    chatName = "Alice",
                    messages = messages,
                    isLoading = isLoading,
                    isLoadingMore = false,
                    hasMore = hasMore,
                    error = null,
                    inputText = "",
                    isSending = false,
                    currentUserId = "me",
                    typingUserIds = emptySet(),
                    connectionStatus = WsConnectionStatus.CONNECTED,
                    onBack = {},
                    onInputChanged = {},
                    onSend = {},
                    onLoadMore = { loadMoreCalls++ }
                )
            }
        }

        // The first page arrives after the first composition, as from the ViewModel (#7).
        messages = todayPage
        hasMore = true
        isLoading = false
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        assertTrue("onLoadMore was not called", loadMoreCalls > 0)
    }

    @Test
    fun loadingOlderPage_keepsPositionInsteadOfJumpingToNewest() {
        var messages by mutableStateOf(todayPage)
        var isLoadingMore by mutableStateOf(false)
        composeTestRule.setContent {
            EchoTheme {
                ChatDetailContent(
                    chatId = "chat_1",
                    chatName = "Alice",
                    messages = messages,
                    isLoading = false,
                    isLoadingMore = isLoadingMore,
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
                    onLoadMore = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()

        // An older page from yesterday arrives: a second date separator comes before today's.
        val yesterday = Instant.now().minusSeconds(26 * 60 * 60)
        isLoadingMore = true
        composeTestRule.waitForIdle()
        messages = (0..19).map { message(it, yesterday.plusSeconds(it * 60L)) } + todayPage
        isLoadingMore = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("message 20").assertIsDisplayed()
        composeTestRule.onNodeWithText("message 59").assertDoesNotExist()
    }
}
