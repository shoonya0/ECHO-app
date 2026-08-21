package com.shoonya.echo.settings

import com.shoonya.echo.fakes.FakeSettingsRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.settings.domain.usecase.GetProfileUseCase
import com.shoonya.echo.features.settings.domain.usecase.UpdateProfileUseCase
import com.shoonya.echo.features.settings.presentation.EditProfileEvent
import com.shoonya.echo.features.settings.presentation.EditProfileViewModel
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
class EditProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeSettingsRepository()
    private lateinit var viewModel: EditProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = EditProfileViewModel(
            getProfileUseCase = GetProfileUseCase(fakeRepo),
            updateProfileUseCase = UpdateProfileUseCase(fakeRepo),
        )
    }

    @Test
    fun `on init, loads profile successfully`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Test User", state.displayName)
        assertEquals("Test bio", state.bio)
        assertNull(state.error)
    }

    @Test
    fun `given load failure, state contains error`() = runTest {
        fakeRepo.setProfileResult(Result.failure(Exception("Network error")))
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `onFieldChanged updates corresponding field`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onFieldChanged("displayName", "New Name")
        assertEquals("New Name", viewModel.state.value.displayName)

        viewModel.onFieldChanged("bio", "New bio text")
        assertEquals("New bio text", viewModel.state.value.bio)

        viewModel.onFieldChanged("avatar", "http://example.com/avatar.png")
        assertEquals("http://example.com/avatar.png", viewModel.state.value.avatar)
    }

    @Test
    fun `save success emits Saved event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    @Test
    fun `save failure sets error`() = runTest {
        fakeRepo.setProfileResult(Result.success(TestData.userProfile))
        fakeRepo.setUpdateProfileResult(Result.failure(Exception("Server error")))
        createViewModel()
        advanceUntilIdle()

        // Update a field so save fires
        viewModel.onFieldChanged("displayName", "Changed")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertNotNull(state.error)
    }
}