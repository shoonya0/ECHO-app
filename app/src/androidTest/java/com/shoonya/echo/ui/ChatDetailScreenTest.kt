package com.shoonya.echo.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailContent
import org.junit.Rule
import org.junit.Test
import java.time.Instant

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
        createdAt = Instant.now().minusSeconds(300),
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
                    onLoadMore = {},
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
                    onLoadMore = {},
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
                    onLoadMore = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Reconnecting…")
            .assertIsDisplayed()
    }
}