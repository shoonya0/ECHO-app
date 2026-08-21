package com.shoonya.echo.settings

import com.shoonya.echo.fakes.FakeAuthRepository
import com.shoonya.echo.fakes.FakeSettingsRepository
import com.shoonya.echo.features.auth.domain.usecase.LogoutUseCase
import com.shoonya.echo.features.settings.domain.usecase.DeleteAccountUseCase
import com.shoonya.echo.features.settings.domain.usecase.GetProfileUseCase
import com.shoonya.echo.features.settings.presentation.SettingsEvent
import com.shoonya.echo.features.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeSettingsRepository()
    private val fakeAuthRepo = FakeAuthRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val getProfile = GetProfileUseCase(fakeRepo)
        val deleteAccount = DeleteAccountUseCase(fakeRepo)
        val logout = LogoutUseCase(fakeAuthRepo)
        viewModel = SettingsViewModel(getProfile, deleteAccount, logout, fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given successful load, state contains profile`() = runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("testuser", state.profile?.username)
        assertEquals("Test User", state.profile?.displayName)
        assertNull(state.error)
    }

    @Test
    fun `given load failure, state contains error`() = runTest {
        fakeRepo.setProfileResult(Result.failure(Exception("Network error")))
        val getProfile = GetProfileUseCase(fakeRepo)
        val deleteAccount = DeleteAccountUseCase(fakeRepo)
        val logout = LogoutUseCase(fakeAuthRepo)
        val vm = SettingsViewModel(getProfile, deleteAccount, logout, fakeRepo)
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertNull(state.profile)
    }

    @Test
    fun `given toggle dark mode, state toggles isDarkMode to true and back to follow system`() = runTest {
        advanceUntilIdle()
        // Initial: null = follow system → isDarkMode=false in UI
        assertFalse(viewModel.state.value.isDarkMode)
        // Toggle: null → true (force dark)
        viewModel.onEvent(SettingsEvent.ToggleDarkMode)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isDarkMode)
        assertEquals(true, fakeRepo.setDarkModeCallValue)
        // Toggle: true → null (back to follow system)
        viewModel.onEvent(SettingsEvent.ToggleDarkMode)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isDarkMode)
        assertEquals(null, fakeRepo.setDarkModeCallValue)
    }

    @Test
    fun `given toggle notifications, state toggles notificationsEnabled`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.state.value.notificationsEnabled)
        viewModel.onEvent(SettingsEvent.ToggleNotifications)
        assertFalse(viewModel.state.value.notificationsEnabled)
        viewModel.onEvent(SettingsEvent.ToggleNotifications)
        assertTrue(viewModel.state.value.notificationsEnabled)
    }

    @Test
    fun `given successful delete account, deletes and clears state`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAccount)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isDeletingAccount)
        assertTrue(fakeRepo.deleteAccountCalled)
    }

    @Test
    fun `given logout, calls repository logout and emits logout event`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.Logout)
        advanceUntilIdle()
        assertTrue(fakeAuthRepo.logoutCalled)
        assertFalse(fakeAuthRepo.isLoggedIn())
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun `given delete account failure, state contains error`() = runTest {
        fakeRepo.setDeleteAccountResult(Result.failure(Exception("Server error")))
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAccount)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isDeletingAccount)
        assertNotNull(state.error)
    }
}