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
    fun `given messages exist, when invoked, then returns the page and its cursor`() = runTest {
        fakeRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1, TestData.message2),
            hasMore = true,
            nextCursor = TestData.message1.id
        )

        val result = useCase(TestData.CHAT_ID_DIRECT, limit = 50, before = "cursor")

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(2, page.messages.size)
        assertTrue(page.hasMore)
        assertEquals(TestData.message1.id, page.nextCursor)
        assertEquals(listOf<String?>("cursor"), fakeRepo.getChatMessagesCursors)
    }

    @Test
    fun `given error, when invoked, then returns failure`() = runTest {
        fakeRepo.setGetChatMessagesError("Network error")

        val result = useCase(TestData.CHAT_ID_DIRECT)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
