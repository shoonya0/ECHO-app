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
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListViewModel
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListEvent
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
import java.time.Instant

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
            userIdProvider = fakeUserIdProvider,
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
        assertEquals(TestData.CHAT_ID_DIRECT, (events.first() as ChatListEvent.NavigateToChat).chatId)

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
    fun `ws NewMessage triggers reload via loadChats`() = runTest {
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat))
        createViewModel()
        advanceUntilIdle()

        viewModel.loadChats(listOf(TestData.ALICE_ID))
        advanceUntilIdle()

        // Change the fake to return updated data for the reload triggered by WS event
        val anotherChat = TestData.directChat.copy(
            id = "another_id",
            name = "Bob",
        )
        fakeChatRepo.setChatListSuccess(listOf(TestData.directChat, anotherChat))

        fakeWsRepo.emitDomainEvent(
            DomainEvent.NewMessage(TestData.CHAT_ID_DIRECT, TestData.message1)
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.chats.size)
    }
}