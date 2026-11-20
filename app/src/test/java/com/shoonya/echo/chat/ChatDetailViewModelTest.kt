package com.shoonya.echo.chat

import androidx.lifecycle.SavedStateHandle
import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.FakeUserIdProvider
import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.ResponseData
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
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
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailEvent
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailViewModel
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
            userIdProvider = fakeUserIdProvider
        )
    }

    @Test
    fun `on init, loads messages successfully`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1, TestData.message2)
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
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
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
        )

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `connection status follows the socket after the chat is joined`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()
        assertEquals(WsConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)

        fakeWsRepo.setConnectionStatus(WsConnectionStatus.RECONNECTING)
        runCurrent()
        assertEquals(WsConnectionStatus.RECONNECTING, viewModel.state.value.connectionStatus)

        fakeWsRepo.setConnectionStatus(WsConnectionStatus.CONNECTED)
        runCurrent()
        assertEquals(WsConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
    }

    @Test
    fun `sendMessage optimistically appends message and clears input immediately`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(
                    data = ResponseData(
                        success = true,
                        messageId = TestData.MESSAGE_ID_2,
                        chatId = TestData.CHAT_ID_DIRECT,
                        messageType = "text"
                    ),
                    requestId = null
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
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
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
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
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
            createdAt = Instant.now()
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
    fun `sendMessage timeout without broadcast keeps optimistic message and clears input`() =
        runTest {
            fakeChatRepo.setGetChatMessagesSuccess(emptyList())
            fakeChatRepo.setGetChatSuccess(TestData.directChat)
            fakeWsRepo.setSendResult(
                Result.success(
                    ServerFrame.Response(data = ResponseData(success = true), requestId = null)
                )
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
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
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
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
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
    fun `own typing echoed by the server does not show the indicator`() = runTest {
        // #31: the server sends `typing` to the whole chat, the sender too.
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.TypingUpdate(
                TestData.CHAT_ID_DIRECT,
                TestData.CURRENT_USER_ID,
                isTyping = true
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.typingUserIds.isEmpty())
    }

    /** The isTyping value of each set_typing sent so far, in order. */
    private fun sentTyping(): List<Boolean> =
        fakeWsRepo.sentCommands.filterIsInstance<WebSocketCommand.SetTyping>()
            .map { it.metadata.isTyping }

    private fun TestScope.openChatForTyping() {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()
    }

    @Test
    fun `typing sends set_typing true at once and at most once per refresh window`() = runTest {
        // #3 (RATE_LIMITED): one keystroke every 500 ms for 5 s must not send a frame each time.
        openChatForTyping()

        viewModel.onInputChanged("H")
        runCurrent()
        assertEquals(listOf(true), sentTyping())

        var text = "H"
        repeat(10) {
            advanceTimeBy(500)
            text += "i"
            viewModel.onInputChanged(text)
            runCurrent()
        }

        // 5 s of typing: the first keystroke and one refresh after 3 s.
        assertEquals(listOf(true, true), sentTyping())
    }

    @Test
    fun `typing stops with one set_typing false after the user pauses`() = runTest {
        openChatForTyping()

        viewModel.onInputChanged("Hel")
        runCurrent()
        advanceTimeBy(2_000)
        viewModel.onInputChanged("Hello")
        runCurrent()
        advanceTimeBy(2_900)
        assertEquals(listOf(true), sentTyping())

        advanceTimeBy(200)
        assertEquals(listOf(true, false), sentTyping())

        advanceUntilIdle()
        assertEquals(listOf(true, false), sentTyping())
    }

    @Test
    fun `clearing the input sends false only after a true`() = runTest {
        openChatForTyping()

        viewModel.onInputChanged("")
        runCurrent()
        assertEquals(emptyList<Boolean>(), sentTyping())

        viewModel.onInputChanged("a")
        viewModel.onInputChanged("")
        viewModel.onInputChanged("")
        advanceUntilIdle()

        assertEquals(listOf(true, false), sentTyping())
    }

    @Test
    fun `sending a message stops typing once`() = runTest {
        openChatForTyping()
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
        )

        viewModel.onInputChanged("Hi")
        runCurrent()
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(listOf(true, false), sentTyping())
    }

    @Test
    fun `ws ReadReceipt marks every message up to readUpToSeq as read by that member`() = runTest {
        // Own messages 1..3, none read yet (#42: read state comes from seqs, not readBy).
        val mine = (1L..3L).map { seq ->
            TestData.message2.copy(
                id = "msg_$seq",
                seq = seq,
                readByUserIds = emptySet(),
                createdAt = Instant.now().minusSeconds(300 - seq)
            )
        }
        fakeChatRepo.setGetChatMessagesSuccess(mine)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.messages.all { it.readByUserIds.isEmpty() })

        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, TestData.ALICE_ID, readUpToSeq = 2)
        )
        advanceUntilIdle()

        val readBy = viewModel.state.value.messages.associate { it.id to it.readByUserIds }
        assertEquals(setOf(TestData.ALICE_ID), readBy["msg_1"])
        assertEquals(setOf(TestData.ALICE_ID), readBy["msg_2"])
        assertEquals(emptySet<String>(), readBy["msg_3"])
    }

    @Test
    fun `history readSeqs set who has read each message`() = runTest {
        val first = TestData.message2.copy(id = "msg_1", seq = 1, readByUserIds = emptySet())
        val second = TestData.message2.copy(
            id = "msg_2",
            seq = 2,
            readByUserIds = emptySet(),
            createdAt = first.createdAt.plusSeconds(1)
        )
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(first, second),
            readSeqs = mapOf(TestData.ALICE_ID to 1L, TestData.CURRENT_USER_ID to 2L)
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)

        createViewModel()
        advanceUntilIdle()

        val readBy = viewModel.state.value.messages.associate { it.id to it.readByUserIds }
        // Alice read up to 1; the sender's own read seq doesn't count as "read".
        assertEquals(setOf(TestData.ALICE_ID), readBy["msg_1"])
        assertEquals(emptySet<String>(), readBy["msg_2"])
    }

    @Test
    fun `an older read receipt never takes read state back`() = runTest {
        val msg = TestData.message2.copy(id = "msg_5", seq = 5, readByUserIds = emptySet())
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(msg),
            readSeqs = mapOf(TestData.ALICE_ID to 5L)
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, TestData.ALICE_ID, readUpToSeq = 3)
        )
        advanceUntilIdle()

        assertEquals(5L, viewModel.state.value.readSeqs[TestData.ALICE_ID])
        assertEquals(
            setOf(TestData.ALICE_ID),
            viewModel.state.value.messages.single().readByUserIds
        )
    }

    @Test
    fun `a live message already covered by a member's read seq shows as read`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            emptyList(),
            readSeqs = mapOf(TestData.ALICE_ID to 7L)
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        val live = TestData.message2.copy(id = "msg_7", seq = 7, readByUserIds = emptySet())
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, live))
        advanceUntilIdle()

        assertEquals(
            setOf(TestData.ALICE_ID),
            viewModel.state.value.messages.single().readByUserIds
        )
    }

    @Test
    fun `a read receipt during the history load isn't undone by the older page`() = runTest {
        val msg = TestData.message2.copy(id = "msg_12", seq = 12, readByUserIds = emptySet())
        val gate = CompletableDeferred<Unit>()
        fakeChatRepo.getChatMessagesGate = gate
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(msg),
            readSeqs = mapOf(TestData.ALICE_ID to 10L) // snapshot taken before Alice read on
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, TestData.ALICE_ID, readUpToSeq = 12)
        )
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(12L, viewModel.state.value.readSeqs[TestData.ALICE_ID])
        assertEquals(
            setOf(TestData.ALICE_ID),
            viewModel.state.value.messages.single().readByUserIds
        )
    }

    @Test
    fun `messages that arrive during the history load stay when the page lands`() = runTest {
        val gate = CompletableDeferred<Unit>()
        fakeChatRepo.getChatMessagesGate = gate
        fakeChatRepo.setGetChatMessagesSuccess(listOf(TestData.message1))
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.enableSendGate() // keeps my send in flight, so its bubble stays optimistic
        createViewModel()
        advanceUntilIdle()

        val live = TestData.message1.copy(
            id = "msg_live",
            content = "new",
            createdAt = Instant.now()
        )
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, live))
        viewModel.onInputChanged("mine")
        viewModel.sendMessage()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        assertEquals(3, messages.size)
        assertEquals(TestData.MESSAGE_ID_1, messages.first().id) // oldest first
        assertTrue(messages.any { it.id == "msg_live" })
        assertTrue(messages.any { it.content == "mine" })
    }

    @Test
    fun `own message acked before its live event still gets its seq and read tick`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(emptyList())
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        // The ack carries no seq, as if it arrived before the broadcast.
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(
                    data = ResponseData(success = true, messageId = "msg_9"),
                    requestId = null
                )
            )
        )
        createViewModel()
        advanceUntilIdle()
        viewModel.onInputChanged("Hi")
        viewModel.sendMessage()
        advanceUntilIdle()

        val echoed = viewModel.state.value.messages.single { it.id == "msg_9" }.copy(seq = 9)
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, echoed))
        advanceUntilIdle()
        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, TestData.ALICE_ID, readUpToSeq = 9)
        )
        advanceUntilIdle()

        val mine = viewModel.state.value.messages.single { it.id == "msg_9" }
        assertEquals(9L, mine.seq)
        assertEquals(setOf(TestData.ALICE_ID), mine.readByUserIds)
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
            createdAt = Instant.now().minusSeconds(600)
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
            createdAt = Instant.now().minusSeconds(60)
        )

        // Simulate backend returning newest-first
        fakeChatRepo.setGetChatMessagesSuccess(
            messages = listOf(newer, older)
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        fakeWsRepo.setSendResult(
            Result.success(
                ServerFrame.Response(data = ResponseData(success = true), requestId = null)
            )
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

    @Test
    fun `first load asks for the newest page and keeps the server's cursor`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1),
            hasMore = true,
            nextCursor = "cursor-1"
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)

        createViewModel()
        advanceUntilIdle()

        assertEquals(listOf<String?>(null), fakeChatRepo.getChatMessagesCursors)
        assertTrue(viewModel.state.value.hasMore)
        assertEquals("cursor-1", viewModel.state.value.nextCursor)
    }

    @Test
    fun `loadMore sends the cursor as before and merges the older page`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message2),
            hasMore = true,
            nextCursor = TestData.message2.id
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1),
            hasMore = false,
            nextCursor = null
        )
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(
            listOf<String?>(null, TestData.message2.id),
            fakeChatRepo.getChatMessagesCursors
        )
        val state = viewModel.state.value
        assertEquals(
            setOf(TestData.message1.id, TestData.message2.id),
            state.messages.map {
                it.id
            }.toSet()
        )
        assertFalse(state.hasMore)
        assertEquals(null, state.nextCursor)
    }

    @Test
    fun `loadMore on the last page asks for nothing`() = runTest {
        fakeChatRepo.setGetChatMessagesSuccess(
            listOf(TestData.message1),
            hasMore = false,
            nextCursor = null
        )
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf<String?>(null), fakeChatRepo.getChatMessagesCursors)
    }

    // ── mark_read (#10) ──
    // These tests step virtual time by hand: advanceUntilIdle would also run the 10 s reply
    // timeouts and their retries. Throttle 300 ms, retry 2 s, timeout 10 s.

    private fun markReads(): List<WebSocketCommand.MarkRead> =
        fakeWsRepo.sentCommands.filterIsInstance<WebSocketCommand.MarkRead>()

    private fun markReadIds(): List<List<String>> = markReads().map { it.metadata.messageIds }

    private fun fromAlice(id: String, seq: Long) = TestData.message1.copy(
        id = id,
        seq = seq,
        readByUserIds = emptySet(),
        createdAt = Instant.now().minusSeconds(100 - seq)
    )

    /** Loads [history], lets the socket connect, then shows the screen. */
    private fun TestScope.openChat(
        history: List<Message>,
        readSeqs: Map<String, Long> = emptyMap(),
        visible: Boolean = true
    ) {
        fakeChatRepo.setGetChatMessagesSuccess(history, readSeqs = readSeqs)
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        runCurrent()
        if (visible) viewModel.onScreenVisibilityChanged(true)
        runCurrent()
    }

    private fun TestScope.wait(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private fun aliceSends(message: Message) {
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, message))
    }

    private fun myReadConfirmed(upToSeq: Long) {
        fakeWsRepo.emitDomainEvent(
            DomainEvent.ReadReceipt(TestData.CHAT_ID_DIRECT, TestData.CURRENT_USER_ID, upToSeq)
        )
    }

    @Test
    fun `visible chat marks the unread messages from others in one mark_read`() = runTest {
        openChat(
            listOf(
                fromAlice("msg_1", 1),
                fromAlice("msg_2", 2),
                fromAlice("msg_3", 3),
                TestData.message2.copy(id = "msg_4", seq = 4, readByUserIds = emptySet())
            ),
            readSeqs = mapOf(TestData.CURRENT_USER_ID to 1L)
        )
        wait(301)

        // msg_1 is already read (my seq is 1), msg_4 is my own.
        assertEquals(listOf(listOf("msg_2", "msg_3")), markReadIds())
    }

    @Test
    fun `nothing is marked read while the screen isn't visible`() = runTest {
        openChat(listOf(fromAlice("msg_1", 1)), visible = false)
        aliceSends(fromAlice("msg_2", 2))
        wait(301)
        assertEquals(emptyList<List<String>>(), markReadIds())

        viewModel.onScreenVisibilityChanged(true)
        wait(301)
        assertEquals(listOf(listOf("msg_1", "msg_2")), markReadIds())

        myReadConfirmed(2)
        runCurrent()
        viewModel.onScreenVisibilityChanged(false)
        aliceSends(fromAlice("msg_3", 3))
        wait(301)
        assertEquals(1, markReads().size)
    }

    @Test
    fun `a live message from someone else is marked read, an own one isn't`() = runTest {
        openChat(emptyList())

        aliceSends(TestData.message2.copy(id = "msg_mine", seq = 1, readByUserIds = emptySet()))
        wait(301)
        assertEquals(emptyList<List<String>>(), markReadIds())

        aliceSends(fromAlice("msg_2", 2))
        wait(301)
        assertEquals(listOf(listOf("msg_2")), markReadIds())
    }

    @Test
    fun `one mark_read at a time, the next goes out once mine is confirmed`() = runTest {
        openChat(listOf(fromAlice("msg_1", 1)))
        wait(301)

        aliceSends(fromAlice("msg_2", 2))
        wait(301)
        assertEquals(listOf(listOf("msg_1")), markReadIds()) // still waiting for msg_1

        myReadConfirmed(1)
        wait(301)
        assertEquals(listOf(listOf("msg_1"), listOf("msg_2")), markReadIds())
    }

    @Test
    fun `a busy chat still sends a mark_read`() = runTest {
        openChat(emptyList())

        // A message every 200 ms, faster than the 300 ms window, for 2 s.
        repeat(10) { n ->
            aliceSends(fromAlice("msg_$n", n + 1L))
            wait(200)
        }

        assertTrue(markReads().isNotEmpty())
        assertEquals(listOf("msg_0", "msg_1"), markReadIds().first())
    }

    @Test
    fun `a rate-limited mark_read is retried quietly after a backoff`() = runTest {
        val snackbars = mutableListOf<ChatDetailEvent>()
        openChatCollecting(snackbars)
        wait(301)

        fakeWsRepo.emitDomainEvent(
            DomainEvent.BackendError("RATE_LIMITED", "slow down", markReads().single().requestId)
        )
        wait(1_000)
        assertEquals(1, markReads().size) // still backing off

        wait(1_001)
        assertEquals(listOf(listOf("msg_1"), listOf("msg_1")), markReadIds())
        assertEquals(emptyList<ChatDetailEvent>(), snackbars)
    }

    @Test
    fun `a mark_read the server refuses for good isn't sent again`() = runTest {
        val snackbars = mutableListOf<ChatDetailEvent>()
        openChatCollecting(snackbars)
        wait(301)

        fakeWsRepo.emitDomainEvent(
            DomainEvent.BackendError(
                "PERMISSION_DENIED",
                "User is blocked",
                markReads().single().requestId
            )
        )
        wait(301)
        aliceSends(fromAlice("msg_2", 2))
        wait(301)

        assertEquals(listOf(listOf("msg_1"), listOf("msg_2")), markReadIds())
        assertEquals(emptyList<ChatDetailEvent>(), snackbars)
    }

    @Test
    fun `a mark_read lost with the socket is sent again on reconnect`() = runTest {
        openChat(listOf(fromAlice("msg_1", 1)))
        wait(301)
        assertEquals(1, markReads().size) // the socket took it, then died

        fakeWsRepo.setConnectionStatus(WsConnectionStatus.RECONNECTING)
        runCurrent()
        fakeWsRepo.setConnectionStatus(WsConnectionStatus.CONNECTED)
        wait(301)

        assertEquals(listOf(listOf("msg_1"), listOf("msg_1")), markReadIds())
    }

    @Test
    fun `a mark_read without any reply is retried, but not forever`() = runTest {
        openChat(listOf(fromAlice("msg_1", 1)))
        wait(301)

        wait(10_000 + 301)
        assertEquals(2, markReads().size)

        wait(60_000)
        assertEquals(3, markReads().size) // gives up after 3 unanswered tries
    }

    @Test
    fun `a mark_read the socket couldn't send is sent once it reconnects`() = runTest {
        fakeWsRepo.sendRawSucceeds = false
        openChat(listOf(fromAlice("msg_1", 1)))
        wait(301)
        assertEquals(emptyList<List<String>>(), markReadIds())

        fakeWsRepo.sendRawSucceeds = true
        fakeWsRepo.setConnectionStatus(WsConnectionStatus.DISCONNECTED)
        runCurrent()
        fakeWsRepo.setConnectionStatus(WsConnectionStatus.CONNECTED)
        wait(301)

        assertEquals(listOf(listOf("msg_1")), markReadIds())
    }

    /** [openChat] with msg_1 unread, collecting the ViewModel's one-off events into [into]. */
    private fun TestScope.openChatCollecting(into: MutableList<ChatDetailEvent>) {
        fakeChatRepo.setGetChatMessagesSuccess(listOf(fromAlice("msg_1", 1)))
        fakeChatRepo.setGetChatSuccess(TestData.directChat)
        createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(into)
        }
        runCurrent()
        viewModel.onScreenVisibilityChanged(true)
        runCurrent()
    }
}
