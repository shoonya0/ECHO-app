package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.ResponseData
import com.shoonya.echo.features.chat.domain.model.TypingMetadata
import com.shoonya.echo.features.chat.domain.model.MarkReadMetadata
import com.shoonya.echo.features.chat.domain.model.ReactionMetadata
import com.shoonya.echo.features.chat.domain.usecase.GetChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.JoinChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.LeaveChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.SetTypingUseCase
import com.shoonya.echo.features.chat.domain.usecase.MarkReadUseCase
import com.shoonya.echo.features.chat.domain.usecase.AddReactionUseCase
import com.shoonya.echo.features.chat.domain.usecase.RemoveReactionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetChatUseCaseTest {
    private val fakeRepo = FakeChatRepository()
    private val useCase = GetChatUseCase(fakeRepo)

    @Test
    fun `given chat exists, when invoked, then returns chat`() = runTest {
        fakeRepo.setGetChatSuccess(TestData.directChat)

        val result = useCase(TestData.CHAT_ID_DIRECT)

        assertTrue(result.isSuccess)
        assertEquals(TestData.directChat, result.getOrNull())
    }

    @Test
    fun `given error, when invoked, then returns failure`() = runTest {
        fakeRepo.setGetChatError("Chat not found")

        val result = useCase("bad_chat_id")

        assertTrue(result.isFailure)
        assertEquals("Chat not found", result.exceptionOrNull()?.message)
    }
}

class JoinChatUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = JoinChatUseCase(fakeWsRepo)

    @Test
    fun `given valid chatId, when invoked, then sends join_chat command`() = runTest {
        fakeWsRepo.setSendResult(Result.success(
            ServerFrame.Response(data = ResponseData(success = true), requestId = null)
        ))

        useCase(TestData.CHAT_ID_DIRECT, TestData.CURRENT_USER_ID)

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.JoinChat)
    }

    @Test
    fun `given send error, when invoked, then logs and does not throw`() = runTest {
        fakeWsRepo.setSendResult(Result.failure(RuntimeException("Connection lost")))

        // JoinChat uses sendRaw which doesn't throw
        useCase(TestData.CHAT_ID_DIRECT, TestData.CURRENT_USER_ID)

        assertEquals(1, fakeWsRepo.sentCommands.size)
    }
}

class LeaveChatUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = LeaveChatUseCase(fakeWsRepo)

    @Test
    fun `given chatId, when invoked, then sends leave_chat command`() = runTest {
        useCase(TestData.CHAT_ID_DIRECT)

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.LeaveChat)
    }
}

class SetTypingUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = SetTypingUseCase(fakeWsRepo)

    @Test
    fun `when invoked, then sends typing command`() = runTest {
        useCase(TestData.CHAT_ID_DIRECT, isTyping = true)

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.SetTyping)
        assertTrue((cmd as com.shoonya.echo.features.chat.domain.model.WebSocketCommand.SetTyping).metadata.isTyping)
    }
}

class MarkReadUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = MarkReadUseCase(fakeWsRepo)

    @Test
    fun `when invoked, then sends mark_read command`() = runTest {
        val messageIds = listOf(TestData.MESSAGE_ID_1)
        useCase(TestData.CHAT_ID_DIRECT, messageIds)

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.MarkRead)
        assertEquals(messageIds, (cmd as com.shoonya.echo.features.chat.domain.model.WebSocketCommand.MarkRead).metadata.messageIds)
    }
}

class AddReactionUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = AddReactionUseCase(fakeWsRepo)

    @Test
    fun `when invoked, then sends add_reaction command`() = runTest {
        useCase(TestData.CHAT_ID_DIRECT, TestData.MESSAGE_ID_1, "👍")

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.AddReaction)
        assertEquals("👍", (cmd as com.shoonya.echo.features.chat.domain.model.WebSocketCommand.AddReaction).metadata.emoji)
    }
}

class RemoveReactionUseCaseTest {
    private val fakeWsRepo = FakeWebSocketRepository()
    private val useCase = RemoveReactionUseCase(fakeWsRepo)

    @Test
    fun `when invoked, then sends remove_reaction command`() = runTest {
        useCase(TestData.CHAT_ID_DIRECT, TestData.MESSAGE_ID_1, "👍")

        assertEquals(1, fakeWsRepo.sentCommands.size)
        val cmd = fakeWsRepo.sentCommands.first()
        assertTrue(cmd is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.RemoveReaction)
        assertEquals("👍", (cmd as com.shoonya.echo.features.chat.domain.model.WebSocketCommand.RemoveReaction).metadata.emoji)
    }
}