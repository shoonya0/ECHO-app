package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.usecase.GetChatMessagesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetChatMessagesUseCaseTest {
    private val fakeRepo = FakeChatRepository()
    private val useCase = GetChatMessagesUseCase(fakeRepo)

    @Test
    fun `given messages exist, when invoked, then returns messages with total count`() = runTest {
        fakeRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1, TestData.message2),
            totalCount = 10,
        )

        val result = useCase(TestData.CHAT_ID_DIRECT, limit = 50, offset = 0)

        assertTrue(result.isSuccess)
        val (messages, total) = result.getOrThrow()
        assertEquals(2, messages.size)
        assertEquals(10, total)
    }

    @Test
    fun `given error, when invoked, then returns failure`() = runTest {
        fakeRepo.setGetChatMessagesError("Network error")

        val result = useCase(TestData.CHAT_ID_DIRECT)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}