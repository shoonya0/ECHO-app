package com.shoonya.echo.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListContent
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ChatListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleChat = Chat(
        id = "test_chat_1",
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
            id = "msg_1",
            chatId = "test_chat_1",
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
        ),
        settings = ChatSettings(),
        createdAt = Instant.now().minusSeconds(3600),
        updatedAt = null,
    )

    @Test
    fun givenChats_rendersChatItemName() {
        composeTestRule.setContent {
            EchoTheme {
                ChatListContent(
                    chats = listOf(sampleChat),
                    isLoading = false,
                    error = null,
                    onRefresh = {},
                    onChatClicked = {},
                    onCreateDirectChat = {},
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
    fun givenEmptyState_showsNoChatsText() {
        composeTestRule.setContent {
            EchoTheme {
                ChatListContent(
                    chats = emptyList(),
                    isLoading = false,
                    error = null,
                    onRefresh = {},
                    onChatClicked = {},
                    onCreateDirectChat = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("No chats yet")
            .assertIsDisplayed()
    }

    @Test
    fun givenErrorState_showsErrorMessage() {
        composeTestRule.setContent {
            EchoTheme {
                ChatListContent(
                    chats = emptyList(),
                    isLoading = false,
                    error = "Something went wrong",
                    onRefresh = {},
                    onChatClicked = {},
                    onCreateDirectChat = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Something went wrong")
            .assertIsDisplayed()
    }
}