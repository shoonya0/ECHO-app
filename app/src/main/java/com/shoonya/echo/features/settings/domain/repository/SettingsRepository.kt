package com.shoonya.echo.features.settings.domain.repository

import com.shoonya.echo.features.settings.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(
        displayName: String?,
        avatar: String?,
        statusMessage: String?,
        bio: String?,
    ): Result<UserProfile>
    suspend fun deleteAccount(): Result<Unit>

    /** Emits the saved dark mode preference. null = follow system. */
    val isDarkMode: Flow<Boolean?>
    /** Returns the current dark mode preference synchronously. */
    val isDarkModeSync: Boolean?
    /** Persists the dark mode preference. null = follow system. */
    suspend fun setDarkMode(enabled: Boolean?)
}
