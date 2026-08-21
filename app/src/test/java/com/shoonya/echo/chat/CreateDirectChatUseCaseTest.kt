package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.usecase.CreateDirectChatUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDirectChatUseCaseTest {
    private val fakeRepo = FakeChatRepository()
    private val useCase = CreateDirectChatUseCase(fakeRepo)

    @Test
    fun `given valid userId, when invoked, then returns chat`() = runTest {
        fakeRepo.setCreateDirectChatSuccess(TestData.directChat)

        val result = useCase(TestData.ALICE_ID)

        assertTrue(result.isSuccess)
        assertEquals(TestData.directChat, result.getOrNull())
    }

    @Test
    fun `given error, when invoked, then returns failure`() = runTest {
        fakeRepo.setCreateDirectChatError("User not found")

        val result = useCase("bad_user_id")

        assertTrue(result.isFailure)
        assertEquals("User not found", result.exceptionOrNull()?.message)
    }
}