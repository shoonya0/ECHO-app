package com.shoonya.echo.fakes

import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.features.settings.domain.model.UserProfile
import com.shoonya.echo.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository : SettingsRepository {
    private var profileResult: Result<UserProfile> = Result.success(
        UserProfile(
            id = "user1",
            username = "testuser",
            email = "test@echo.com",
            phone = "",
            displayName = "Test User",
            avatar = "",
            statusMessage = "Hello",
            bio = "Test bio",
            presence = Presence(PresenceStatus.ONLINE, true, null),
            isActive = true,
        ),
    )
    private var updateProfileResult: Result<UserProfile> = Result.success(
        UserProfile(
            id = "user1",
            username = "testuser",
            email = "test@echo.com",
            phone = "",
            displayName = "Updated User",
            avatar = "",
            statusMessage = "Updated status",
            bio = "Updated bio",
            presence = Presence(PresenceStatus.ONLINE, true, null),
            isActive = true,
        ),
    )
    private var deleteAccountResult: Result<Unit> = Result.success(Unit)

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)

    val updateProfileCalls = mutableListOf<Map<String, String?>>()
    var deleteAccountCalled = false
    var setDarkModeCallValue: Boolean? = null

    fun setProfileResult(result: Result<UserProfile>) {
        profileResult = result
    }

    fun setUpdateProfileResult(result: Result<UserProfile>) {
        updateProfileResult = result
    }

    fun setDeleteAccountResult(result: Result<Unit>) {
        deleteAccountResult = result
    }

    fun setDarkModePreference(enabled: Boolean?) {
        _isDarkMode.value = enabled
    }

    override suspend fun getProfile(): Result<UserProfile> = profileResult

    override suspend fun updateProfile(
        displayName: String?,
        avatar: String?,
        statusMessage: String?,
        bio: String?,
    ): Result<UserProfile> {
        updateProfileCalls.add(
            mapOf(
                "displayName" to displayName,
                "avatar" to avatar,
                "statusMessage" to statusMessage,
                "bio" to bio,
            ),
        )
        return updateProfileResult
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteAccountCalled = true
        return deleteAccountResult
    }

    override val isDarkMode: Flow<Boolean?>
        get() = _isDarkMode.asStateFlow()

    override val isDarkModeSync: Boolean?
        get() = _isDarkMode.value

    override suspend fun setDarkMode(enabled: Boolean?) {
        setDarkModeCallValue = enabled
        _isDarkMode.value = enabled
    }
}
