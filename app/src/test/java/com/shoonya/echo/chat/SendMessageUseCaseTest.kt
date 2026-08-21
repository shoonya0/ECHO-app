package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.ResponseData
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    private val fakeWsRepository = FakeWebSocketRepository()
    private val useCase = SendMessageUseCase(fakeWsRepository)

    @Test
    fun `send message success returns message with response messageId`() = runTest {
        val responseFrame = ServerFrame.Response(
            data = ResponseData(
                success = true,
                messageId = "msg_123",
                chatId = "chat_1",
                messageType = "text",
            ),
            requestId = null,
        )
        fakeWsRepository.setSendResult(Result.success(responseFrame))

        val result = useCase("chat_1", "Hello", "user_1")

        assertTrue(result.isSuccess)
        val message = result.getOrThrow()
        assertEquals("msg_123", message.id)
        assertEquals("chat_1", message.chatId)
        assertEquals("user_1", message.senderId)
        assertEquals("Hello", message.content)
        assertEquals(MessageType.TEXT, message.type)
    }

    @Test
    fun `send message failure returns error`() = runTest {
        fakeWsRepository.setSendResult(Result.failure(RuntimeException("Connection lost")))

        val result = useCase("chat_1", "Hello", "user_1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `send message tracks command in repository`() = runTest {
        val responseFrame = ServerFrame.Response(
            data = ResponseData(success = true, messageId = "msg_456"),
            requestId = null,
        )
        fakeWsRepository.setSendResult(Result.success(responseFrame))

        useCase("chat_2", "Hi", "user_2")

        assertEquals(1, fakeWsRepository.sentCommands.size)
        val sentCommand = fakeWsRepository.sentCommands.first()
        assertTrue(sentCommand is com.shoonya.echo.features.chat.domain.model.WebSocketCommand.SendMessage)
        val sendCmd = sentCommand as com.shoonya.echo.features.chat.domain.model.WebSocketCommand.SendMessage
        assertEquals("chat_2", sendCmd.chatId)
        assertEquals("Hi", sendCmd.content)
    }
}