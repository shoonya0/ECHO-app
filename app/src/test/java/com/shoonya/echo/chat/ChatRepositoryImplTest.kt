package com.shoonya.echo.chat

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.fakes.FakeUserIdProvider
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.data.local.ChatCache
import com.shoonya.echo.features.chat.data.model.ChatDto
import com.shoonya.echo.features.chat.data.model.ContactUserInfoDto
import com.shoonya.echo.features.chat.data.model.MessageListResponse
import com.shoonya.echo.features.chat.data.model.MessagePagination
import com.shoonya.echo.features.chat.data.model.ParticipantDto
import com.shoonya.echo.features.chat.data.remote.ChatApiService
import com.shoonya.echo.features.chat.data.remote.MessageApiService
import com.shoonya.echo.features.chat.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Data-layer tests for [ChatRepositoryImpl] with fake API services (no network, no Android).
 */
class ChatRepositoryImplTest {

    private val chatApi = FakeChatApiService()
    private val cache = ChatCache()
    private val repo = ChatRepositoryImpl(
        chatApi = chatApi,
        messageApi = EmptyMessageApiService(),
        userIdProvider = FakeUserIdProvider(TestData.CURRENT_USER_ID),
        chatCache = cache
    )

    // Issue #2: a chat opened from the chat list had no header name, because getChatList
    // cached only the chat id and getChat(id) then failed with "not found in cache".
    @Test
    fun `getChat returns a chat discovered by getChatList with the contact's name`() = runTest {
        chatApi.chats[TestData.ALICE_ID] = directChatDto(
            TestData.CHAT_ID_DIRECT,
            TestData.ALICE_ID,
            "Alice"
        )

        repo.getChatList(listOf(TestData.ALICE_ID)).getOrThrow()
        val result = repo.getChat(TestData.CHAT_ID_DIRECT)

        assertTrue("getChat failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("Alice", result.getOrThrow().name)
        assertEquals(TestData.CHAT_ID_DIRECT, result.getOrThrow().id)
    }

    @Test
    fun `getChat fails for a chat that was never discovered`() = runTest {
        val result = repo.getChat("unknown")

        assertTrue(result.isFailure)
    }

    private fun directChatDto(chatId: String, otherId: String, otherName: String) = ChatDto(
        id = chatId,
        participants = mapOf(
            TestData.CURRENT_USER_ID to ParticipantDto(
                userInfo = ContactUserInfoDto(
                    displayName = "Test User",
                    username = "testuser",
                    userId = TestData.CURRENT_USER_ID
                )
            ),
            otherId to ParticipantDto(
                userInfo = ContactUserInfoDto(
                    displayName = otherName,
                    username = otherName.lowercase(),
                    userId = otherId
                )
            )
        )
    )

    private class FakeChatApiService : ChatApiService {
        val chats = mutableMapOf<String, ChatDto>()

        override suspend fun createDirectChat(userId: String): ApiResponse<ChatDto> {
            val chat = chats[userId]
            return if (chat != null) {
                ApiResponse(success = true, data = chat)
            } else {
                ApiResponse(success = false, error = "no chat for $userId")
            }
        }

        override suspend fun getHubStats(): ApiResponse<Unit> = ApiResponse(success = true)
    }

    private class EmptyMessageApiService : MessageApiService {
        override suspend fun getChatMessages(
            chatId: String,
            limit: Int,
            before: String?
        ): ApiResponse<MessageListResponse> = ApiResponse(
            success = true,
            data = MessageListResponse(
                chat = chatId,
                messages = emptyList(),
                pagination = MessagePagination(hasMore = false)
            )
        )
    }
}
