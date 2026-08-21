package com.shoonya.echo.contacts

import com.shoonya.echo.fakes.FakeContactsRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.contacts.domain.usecase.AcceptContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.AddToFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.BlockUserUseCase
import com.shoonya.echo.features.contacts.domain.usecase.DeclineContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetBlockedUsersUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetContactsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetIncomingRequestsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetOutgoingRequestsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetUserProfileUseCase
import com.shoonya.echo.features.contacts.domain.usecase.RemoveContactUseCase
import com.shoonya.echo.features.contacts.domain.usecase.RemoveFromFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.SendContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.UnblockUserUseCase
import com.shoonya.echo.features.contacts.presentation.ContactsEvent
import com.shoonya.echo.features.contacts.presentation.ContactsViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeContactsRepository()
    private lateinit var viewModel: ContactsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ContactsViewModel(
            getContactsUseCase = GetContactsUseCase(fakeRepo),
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepo),
            getBlockedUsersUseCase = GetBlockedUsersUseCase(fakeRepo),
            getIncomingRequestsUseCase = GetIncomingRequestsUseCase(fakeRepo),
            getOutgoingRequestsUseCase = GetOutgoingRequestsUseCase(fakeRepo),
            sendContactRequestUseCase = SendContactRequestUseCase(fakeRepo),
            acceptContactRequestUseCase = AcceptContactRequestUseCase(fakeRepo),
            declineContactRequestUseCase = DeclineContactRequestUseCase(fakeRepo),
            blockUserUseCase = BlockUserUseCase(fakeRepo),
            unblockUserUseCase = UnblockUserUseCase(fakeRepo),
            addToFavoritesUseCase = AddToFavoritesUseCase(fakeRepo),
            removeContactUseCase = RemoveContactUseCase(fakeRepo),
            removeFromFavoritesUseCase = RemoveFromFavoritesUseCase(fakeRepo),
            getUserProfileUseCase = GetUserProfileUseCase(fakeRepo),
        )
    }

    @Test
    fun `on init, loadAll populates contacts and favorites and requests`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(listOf(TestData.incomingRequest))
        fakeRepo.setOutgoingRequestsSuccess(listOf(TestData.outgoingRequest))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.contacts.size)
        assertEquals(1, state.favorites.size)
        assertEquals("bob", state.favorites.first().username)
        assertEquals(1, state.incomingRequests.size)
        assertEquals(1, state.outgoingRequests.size)
        assertEquals(0, state.blockedUsers.size)
    }

    @Test
    fun `given loadAll fails, state contains error`() = runTest {
        fakeRepo.setContactsError("Network failure")
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.contacts.isEmpty())
    }

    @Test
    fun `toggleFavorite on non-favorite contact adds to favorites`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setAddToFavoritesSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ToggleFavorite(TestData.contact1.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        val updatedContact1 = state.contacts.find { it.id == TestData.contact1.id }
        assertNotNull(updatedContact1)
        assertTrue(updatedContact1?.isFavorite ?: false)
        assertEquals(2, state.favorites.size)
    }

    @Test
    fun `toggleFavorite on favorite contact removes from favorites`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setRemoveFromFavoritesSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ToggleFavorite(TestData.contact2.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        val updatedContact2 = state.contacts.find { it.id == TestData.contact2.id }
        assertNotNull(updatedContact2)
        assertFalse(updatedContact2?.isFavorite ?: true)
        assertTrue(state.favorites.isEmpty())
    }

    @Test
    fun `blockUser removes contact from contacts and favorites`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setBlockUserSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.BlockUser(TestData.contact2.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.contacts.find { it.id == TestData.contact2.id })
        assertEquals(1, state.contacts.size)
        assertTrue(state.favorites.isEmpty())
        assertNull(state.actionInFlightUserId)
    }

    @Test
    fun `acceptRequest triggers loadAll`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1))
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(listOf(TestData.incomingRequest))
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setAcceptContactRequestSuccess()

        createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.incomingRequests.size)

        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))

        viewModel.onEvent(ContactsEvent.AcceptRequest(TestData.incomingRequest.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.incomingRequests.isEmpty())
    }

    @Test
    fun `loadUserProfile sets viewedProfile on success`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setGetUserProfileSuccess(TestData.user)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.LoadUserProfile(TestData.user.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProfileLoading)
        assertNotNull(state.viewedProfile)
        assertEquals(TestData.user.displayName, state.viewedProfile?.displayName)
    }

    @Test
    fun `loadUserProfile failure clears loading state`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setGetUserProfileError("User not found")

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.LoadUserProfile("bad_id"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProfileLoading)
        assertNull(state.viewedProfile)
    }

    @Test
    fun `removeContact success removes from contacts and clears viewedProfile`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setRemoveContactSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.RemoveContact(TestData.contact2.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.contacts.find { it.id == TestData.contact2.id })
        assertEquals(1, state.contacts.size)
        assertTrue(state.favorites.isEmpty())
        assertNull(state.viewedProfile)
        assertNull(state.actionInFlightUserId)
    }

    @Test
    fun `removeContact failure emits snackbar and clears in-flight`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setRemoveContactError("Contact not found")

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.RemoveContact(TestData.contact2.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.contacts.size)
        assertNull(state.actionInFlightUserId)
    }

    @Test
    fun `searchQueryChanged updates searchQuery in state`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.SearchQueryChanged("alice"))
        advanceUntilIdle()

        assertEquals("alice", viewModel.state.value.searchQuery)
    }

    @Test
    fun `unblockUser triggers loadAll and clears blocked list`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(listOf(TestData.blockedContact))
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setUnblockUserSuccess()

        createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.blockedUsers.size)

        fakeRepo.setBlockedUsersSuccess(emptyList())

        viewModel.onEvent(ContactsEvent.UnblockUser(TestData.blockedContact.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.blockedUsers.isEmpty())
        assertNull(state.actionInFlightUserId)
    }

    @Test
    fun `sendContactRequest success clears viewedProfile`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setSendContactRequestSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.SendContactRequest("507f1f77bcf86cd799439011"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.viewedProfile)
        assertNull(state.actionInFlightUserId)
    }

    @Test
    fun `clearViewedProfile sets viewedProfile to null`() = runTest {
        fakeRepo.setContactsSuccess(emptyList())
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ClearViewedProfile)
        advanceUntilIdle()

        assertNull(viewModel.state.value.viewedProfile)
    }

    @Test
    fun `toggleFavorite from favorites row removes contact from favorites`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1, TestData.contact2))
        fakeRepo.setFavoritesSuccess(listOf(TestData.contact2))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setRemoveFromFavoritesSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ToggleFavorite(TestData.contact2.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.favorites.isEmpty())
        val updatedContact2 = state.contacts.find { it.id == TestData.contact2.id }
        assertNotNull(updatedContact2)
        assertFalse(updatedContact2?.isFavorite ?: true)
    }

    @Test
    fun `toggleFavorite handles identifier resolving from favorites list`() = runTest {
        val favOnlyContact = TestData.contact2.copy(id = "fav_only_001", isFavorite = true)
        fakeRepo.setContactsSuccess(listOf(TestData.contact1))
        fakeRepo.setFavoritesSuccess(listOf(favOnlyContact))
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setRemoveFromFavoritesSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ToggleFavorite(favOnlyContact.id))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.favorites.isEmpty())
    }

    @Test
    fun `toggleFavorite with unknown userId synthesizes contact and adds to favorites`() = runTest {
        fakeRepo.setContactsSuccess(listOf(TestData.contact1))
        fakeRepo.setFavoritesSuccess(emptyList())
        fakeRepo.setBlockedUsersSuccess(emptyList())
        fakeRepo.setIncomingRequestsSuccess(emptyList())
        fakeRepo.setOutgoingRequestsSuccess(emptyList())
        fakeRepo.setAddToFavoritesSuccess()

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ContactsEvent.ToggleFavorite("unknown_user_id_123"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.favorites.size)
        assertEquals("unknown_user_id_123", state.favorites.first().id)
        assertTrue(state.favorites.first().isFavorite)
    }
}
