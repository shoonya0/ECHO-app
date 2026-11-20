package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeChatRepository
import com.shoonya.echo.fakes.FakeUserIdProvider
import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import com.shoonya.echo.features.chat.domain.usecase.CreateDirectChatUseCase
import com.shoonya.echo.features.chat.domain.usecase.GetChatListUseCase
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListEvent
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListViewModel
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeChatRepo = FakeChatRepository()
    private val fakeWsRepo = FakeWebSocketRepository()
    private val fakeUserIdProvider = FakeUserIdProvider(TestData.CURRENT_USER_ID)
    private lateinit var viewModel: ChatListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ChatListViewModel(
            getChatListUseCase = GetChatListUseCase(fakeChatRepo),
            createDirectChatUseCase = CreateDirectChatUseCase(fakeChatRepo),
            wsRepository = fakeWsRepo,
            userIdProvider = fakeUserIdProvider
        )
    }

    @Test
    fun `given successful load, state contains chats`() = runTest {
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle() // ws connect

        viewModel.loadChats(listOf(TestData.ALICE_ID, TestData.BOB_ID))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.chats.size)
        assertNull(state.error)
    }

    @Test
    fun `state exposes currentUserId from provider`() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertEquals(TestData.CURRENT_USER_ID, viewModel.state.value.currentUserId)
    }

    @Test
    fun `given load failure, state contains error`() = runTest {
        fakeChatRepo.setChatListError("Network error")
        createViewModel()
        advanceUntilIdle()

        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `createDirectChat success emits NavigateToChat`() = runTest {
        fakeChatRepo.setChatListSuccess(emptyList())
        fakeChatRepo.setCreateDirectChatSuccess(TestData.directChat)
        createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<ChatListEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.createDirectChat(TestData.ALICE_ID)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is ChatListEvent.NavigateToChat)
        assertEquals(
            TestData.CHAT_ID_DIRECT,
            (events.first() as ChatListEvent.NavigateToChat).chatId
        )

        job.cancel()
    }

    @Test
    fun `createDirectChat failure emits ShowSnackbar`() = runTest {
        fakeChatRepo.setChatListSuccess(emptyList())
        fakeChatRepo.setCreateDirectChatError("Permission denied")
        createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<ChatListEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.createDirectChat(TestData.ALICE_ID)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is ChatListEvent.ShowSnackbar)

        job.cancel()
    }

    @Test
    fun `ws NewMessage updates the chat preview and order without a reload`() = runTest {
        // #28: a live message used to reload the whole list (POST chats/direct + GET messages
        // per contact). The event carries everything the row shows.
        val bobChat = TestData.directChat.copy(
            id = "bob_chat",
            name = "Bob",
            lastMessage = TestData.message1.copy(
                id = "bob_msg",
                chatId = "bob_chat",
                createdAt = Instant.now().minusSeconds(10)
            )
        )
        fakeChatRepo.setChatListSuccess(listOf(bobChat, TestData.directChat))
        createViewModel()
        advanceUntilIdle()
        viewModel.loadChats(listOf(TestData.ALICE_ID, TestData.BOB_ID))
        advanceUntilIdle()
        assertEquals(1, fakeChatRepo.getChatListCalls)

        val live = TestData.message1.copy(
            id = "live_msg",
            content = "Are you there?",
            createdAt = Instant.now()
        )
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, live))
        advanceUntilIdle()

        assertEquals(1, fakeChatRepo.getChatListCalls)
        val chats = viewModel.state.value.chats
        assertEquals(listOf(TestData.CHAT_ID_DIRECT, "bob_chat"), chats.map { it.id })
        assertEquals(live, chats.first().lastMessage)
    }

    @Test
    fun `ws NewMessage for a chat not in the list makes no REST call`() = runTest {
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.NewMessage("unknown_chat", TestData.message1.copy(chatId = "unknown_chat"))
        )
        advanceUntilIdle()

        assertEquals(1, fakeChatRepo.getChatListCalls)
        assertEquals(listOf(TestData.directChat), viewModel.state.value.chats)
    }

    @Test
    fun `the user's own join or leave does not reload the list`() = runTest {
        // #29: opening a chat joins it, and the server sends that `join` back to this user.
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(
            DomainEvent.UserJoined(TestData.CHAT_ID_DIRECT, TestData.CURRENT_USER_ID)
        )
        fakeWsRepo.emitDomainEvent(
            DomainEvent.UserLeft(TestData.CHAT_ID_DIRECT, TestData.CURRENT_USER_ID)
        )
        advanceUntilIdle()

        assertEquals(1, fakeChatRepo.getChatListCalls)
    }

    @Test
    fun `another member joining still reloads the list`() = runTest {
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        fakeWsRepo.emitDomainEvent(DomainEvent.UserJoined(TestData.CHAT_ID_DIRECT, TestData.BOB_ID))
        advanceUntilIdle()

        assertEquals(2, fakeChatRepo.getChatListCalls)
    }

    @Test
    fun `a second load of the same contacts while one runs makes no new calls`() = runTest {
        // #29: the screen asked for the list twice on first show (LaunchedEffect + resume).
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()

        viewModel.loadChats(listOf(TestData.ALICE_ID))
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()
        assertEquals(1, fakeChatRepo.getChatListCalls)

        // A later refresh (resume, pull to refresh) still loads.
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()
        assertEquals(2, fakeChatRepo.getChatListCalls)
        assertEquals(listOf(TestData.directChat), viewModel.state.value.chats)
    }

    @Test
    fun `an older live message does not replace a newer preview`() = runTest {
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()
        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        val older = TestData.message1.copy(
            id = "old_msg",
            createdAt = TestData.message1.createdAt.minusSeconds(60)
        )
        fakeWsRepo.emitDomainEvent(DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, older))
        advanceUntilIdle()

        assertEquals(TestData.message1, viewModel.state.value.chats.single().lastMessage)
    }
}
