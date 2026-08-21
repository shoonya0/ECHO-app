package com.shoonya.echo.settings

import com.shoonya.echo.fakes.FakeSettingsRepository
import com.shoonya.echo.features.settings.domain.usecase.DeleteAccountUseCase
import com.shoonya.echo.features.settings.domain.usecase.GetProfileUseCase
import com.shoonya.echo.features.settings.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUseCaseTest {
    private val fakeRepo = FakeSettingsRepository()
    private val getProfileUseCase = GetProfileUseCase(fakeRepo)
    private val updateProfileUseCase = UpdateProfileUseCase(fakeRepo)
    private val deleteAccountUseCase = DeleteAccountUseCase(fakeRepo)

    @Test
    fun `given profile exists, when getProfile, then returns user profile`() = runTest {
        val result = getProfileUseCase()
        assertTrue(result.isSuccess)
        assertEquals("testuser", result.getOrNull()?.username)
        assertEquals("Test User", result.getOrNull()?.displayName)
    }

    @Test
    fun `given getProfile error, when getProfile, then returns failure`() = runTest {
        fakeRepo.setProfileResult(Result.failure(Exception("Network error")))
        val result = getProfileUseCase()
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given valid data, when updateProfile, then returns updated profile and tracks call`() = runTest {
        val result = updateProfileUseCase("New Name", "http://avatar", "New status", "New bio")
        assertTrue(result.isSuccess)
        assertEquals("Updated User", result.getOrNull()?.displayName)
        assertEquals(1, fakeRepo.updateProfileCalls.size)
        assertEquals("New Name", fakeRepo.updateProfileCalls.first()["displayName"])
    }

    @Test
    fun `given updateProfile error, when updateProfile, then returns failure`() = runTest {
        fakeRepo.setUpdateProfileResult(Result.failure(Exception("Permission denied")))
        val result = updateProfileUseCase("Name", null, null, null)
        assertTrue(result.isFailure)
        assertEquals("Permission denied", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given valid state, when deleteAccount, then returns success and tracks call`() = runTest {
        val result = deleteAccountUseCase()
        assertTrue(result.isSuccess)
        assertTrue(fakeRepo.deleteAccountCalled)
    }

    @Test
    fun `given deleteAccount error, when deleteAccount, then returns failure`() = runTest {
        fakeRepo.setDeleteAccountResult(Result.failure(Exception("Server error")))
        val result = deleteAccountUseCase()
        assertTrue(result.isFailure)
        assertEquals("Server error", result.exceptionOrNull()?.message)
    }
}