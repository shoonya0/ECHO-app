package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.usecase.GetChatListUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GetChatListUseCaseTest {

    @Test
    fun `invoke returns chat list on success`() = runTest {
        val repo = FakeChatRepository()
        val expected = listOf(
            Chat(
                id = "c1",
                type = ChatType.DIRECT,
                name = "Alice",
                description = "",
                avatar = "",
                participants = emptyList(),
                ownerId = null,
                participantCount = 2,
                messageCount = 0,
                unreadCount = 0,
                lastMessage = null,
                settings = ChatSettings(),
                createdAt = Instant.EPOCH,
                updatedAt = null,
            )
        )
        repo.setChatListSuccess(expected)

        val useCase = GetChatListUseCase(repo)
        val result = useCase(listOf("alice"))

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `invoke returns error on failure`() = runTest {
        val repo = FakeChatRepository()
        repo.setChatListError("Network error")

        val useCase = GetChatListUseCase(repo)
        val result = useCase(listOf("alice"))

        assertTrue(result.isFailure)
    }
}