package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.FakeUserIdProvider
import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.ResponseData
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailViewModel
import com.shoonya.echo.features.chat.domain.usecase.AddReactionUseCase
import com.shoonya.echo.features.chat.domain.usecase.CreateDirectChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatMessagesUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.JoinChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.LeaveChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.MarkReadUseCase
import com.shoonya.echo.features.chat.domain.usecase.RemoveReactionUseCase
import com.shoonya.echo.features.chat.domain.usecase.SendMessageUseCase
import com.shoonya.echo.features.chat.domain.usecase.SetTypingUseCase
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeChatRepo = FakeChatRepository()
    private val fakeWsRepo = FakeWebSocketRepository()
    private val fakeUserIdProvider = FakeUserIdProvider(TestData.CURRENT_USER_ID)
    private lateinit var viewModel: ChatDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(chatId: String = TestData.CHAT_ID_DIRECT) {
        val savedStateHandle = SavedStateHandle(mapOf("chatId" to chatId))
        fakeChatRepo.setCreateDirectChatSuccess(TestData.directChat)
        viewModel = ChatDetailViewModel(
            savedStateHandle = savedStateHandle,
            createDirectChatUseCase = CreateDirectChatUseCase(fakeChatRepo),
            getChatUseCase = GetChatUseCase(fakeChatRepo),
            getChatMessagesUseCase = GetChatMessagesUseCase(fakeChatRepo),
            sendMessageUseCase = SendMessageUseCase(fakeWsRepo),
            joinChatUseCase = JoinChatUseCase(fakeWsRepo),
            leaveChatUseCase = LeaveChatUseCase(fakeWsRepo),
            setTypingUseCase = SetTypingUseCase(fakeWsRepo),
            markReadUseCase = MarkReadUseCase(fakeWsRepo),
            addReactionUseCase = AddReactionUseCase(fakeWsRepo),
            removeReactionUseCase = RemoveReactionUseCase(fakeWsRepo),
            wsRepository = fakeWsRepo,
            userIdProvider = fakeUserIdProvider,
        )
    }

    @Test
    fun `on init, loads messages successfully`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1, TestData.message2),
            totalCount = 10,
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals(TestData.CURRENT_USER_ID, state.currentUserId)
    }

    @Test
    fun `given messages load failure, state contains error`() = runTest {
        fakeChatRepo.setGetChatMessagesError("Network error")
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `sendMessage optimistically appends message and clears input immediately`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(
                    data = ResponseData(
                        success = true,
                        messageId = TestData.MESSAGE_ID_2,
                        chatId = TestData.CHAT_ID_DIRECT,
                        messageType = "text",
                    ),
                    requestId = null,
                )
            )
        )

        createViewModel()
        advanceUntilIdle()

        viewModel.onInputChanged("Hello!")
        // input cleared and message appended immediately — before the coroutine processes
        viewModel.sendMessage()

        // Verify optimistic state immediately (before coroutines run)
        val immediateState = viewModel.state.value
        assertEquals("", immediateState.inputText)
        assertTrue(immediateState.isSending)
        assertEquals(1, immediateState.messages.size)
        assertEquals("Hello!", immediateState.messages.first().content)

        // After coroutines complete, optimistic is replaced by server-confirmed message
        advanceUntilIdle()

        val finalState = viewModel.state.value
        assertEquals("", finalState.inputText)
        assertFalse(finalState.isSending)
        assertEquals(1, finalState.messages.size)
        assertEquals(TestData.MESSAGE_ID_2, finalState.messages.first().id)
        assertEquals("Hello!", finalState.messages.first().content)
    }

    @Test
    fun `sendMessage failure restores input and removes optimistic message`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.failure(RuntimeException("Connection lost"))
        )

        createViewModel()
        advanceUntilIdle()

        viewModel.onInputChanged("Hello!")
        viewModel.sendMessage()

        // Optimistically: input cleared, message appended
        val optimisticState = viewModel.state.value
        assertEquals("", optimisticState.inputText)
        assertEquals(1, optimisticState.messages.size)

        advanceUntilIdle()

        // After failure: input restored, message removed, isSending false
        val state = viewModel.state.value
        assertEquals("Hello!", state.inputText)
        assertFalse(state.isSending)
        assertEquals(0, state.messages.size)
    }

    @Test
    fun `sendMessage failure after server broadcast keeps message and clears input`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        // Gate the send so we can interleave the server broadcast deterministically.
        fakeWsRepo.enableSendGate()

        viewModel.onInputChanged("Hello!")
        viewModel.sendMessage()
        runCurrent()

        // Server broadcasts our own message back (real server id) before the ack fails.
        val serverMessage = Message(
            id = TestData.MESSAGE_ID_2,
            chatId = TestData.CHAT_ID_DIRECT,
            senderId = TestData.CURRENT_USER_ID,
            senderName = "testuser",
            senderDisplayName = "Test User",
            senderAvatar = "",
            content = "Hello!",
            type = MessageType.TEXT,
            attachments = emptyList(),
            reactions = emptyMap(),
            isEdited = false,
            readByUserIds = emptySet(),
            createdAt = Instant.now(),
        )
        fakeWsRepo.emitDomainEvent(
            DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, serverMessage)
        )
        runCurrent()

        // The ack now fails (e.g. the requestId correlation timed out).
        fakeWsRepo.completeSendGate(
            Result.failure(RuntimeException("timeout"))
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        // Input must NOT be restored — the message was actually delivered via broadcast.
        assertEquals("", state.inputText)
        assertFalse(state.isSending)
        assertEquals(1, state.messages.size)
        assertEquals(TestData.MESSAGE_ID_2, state.messages.first().id)
        assertEquals("Hello!", state.messages.first().content)
    }

    @Test
    fun `sendMessage timeout without broadcast keeps optimistic message and clears input`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        // Gate so we control when the send resolves.
        fakeWsRepo.enableSendGate()

        viewModel.onInputChanged("Hello!")
        viewModel.sendMessage()
        runCurrent()

        // Ack never correlates — send times out.
        fakeWsRepo.completeSendGate(
            Result.failure(produceTimeoutCancellationException())
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        // Input stays cleared and the optimistic bubble remains visible.
        assertEquals("", state.inputText)
        assertFalse(state.isSending)
        assertEquals(1, state.messages.size)
        assertEquals("Hello!", state.messages.first().content)
    }

    @Test
    fun `ws NewMessage event appends message to state`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, TestData.message1)
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.messages.size)
        assertEquals(TestData.MESSAGE_ID_1, state.messages.first().id)
    }

    @Test
    fun `ws TypingUpdate updates typingUserIds`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList(), 0)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.TypingUpdate(TestData.CHAT_ID_DIRECT, TestData.ALICE_ID, isTyping = true)
        )
        advanceUntilIdle()

        assertTrue(TestData.ALICE_ID in viewModel.state.value.typingUserIds)
    }

    @Test
    fun `ws ReadReceipt updates readByUserIds on messages`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message2), // message2 has readByUserIds empty initially in test
            totalCount = 1,
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, listOf(TestData.MESSAGE_ID_2), TestData.ALICE_ID)
        )
        advanceUntilIdle()

        val updated = viewModel.state.value.messages.find { it.id == TestData.MESSAGE_ID_2 }
        assertNotNull(updated)
        assertTrue(TestData.ALICE_ID in updated!!.readByUserIds)
    }

    private fun produceTimeoutCancellationException(): TimeoutCancellationException = runBlocking {
        try {
            withTimeout(1) { delay(10_000) }
            throw AssertionError("Expected a timeout")
        } catch (e: TimeoutCancellationException) {
            e
        }
    }

    @Test
    fun `given messages loaded newest-first, state messages are sorted oldest-first`() = runTest {
        val older = Message(
            id = "msg_older",
            chatId = TestData.CHAT_ID_DIRECT,
            senderId = TestData.ALICE_ID,
            senderName = "alice",
            senderDisplayName = "Alice",
            senderAvatar = "",
            content = "Hi",
            type = MessageType.TEXT,
            attachments = emptyList(),
            reactions = emptyMap(),
            isEdited = false,
            readByUserIds = emptySet(),
            createdAt = Instant.now().minusSeconds(600),
        )
        val newer = Message(
            id = "msg_newer",
            chatId = TestData.CHAT_ID_DIRECT,
            senderId = TestData.CURRENT_USER_ID,
            senderName = "testuser",
            senderDisplayName = "Test User",
            senderAvatar = "",
            content = "Hey!",
            type = MessageType.TEXT,
            attachments = emptyList(),
            reactions = emptyMap(),
            isEdited = false,
            readByUserIds = emptySet(),
            createdAt = Instant.now().minusSeconds(60),
        )

        // Simulate backend returning newest-first
        fakeChatRepo.setGetChatMessagesSuccess(
            messages = listOf(newer, older),
            totalCount = 2,
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(ServerFrame.Response(data = ResponseData(success = true), requestId = null))
        )

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        // Oldest first
        assertEquals("msg_older", state.messages.first().id)
        assertEquals("msg_newer", state.messages.last().id)
    }
}
