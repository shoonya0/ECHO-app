package com.shoonya.echo.features.settings.data.repository

import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.core.data.remote.toUnitResult
import com.shoonya.echo.core.theme.ThemeStore
import com.shoonya.echo.features.settings.data.model.UpdateProfileRequest
import com.shoonya.echo.features.settings.data.model.toDomain
import com.shoonya.echo.features.settings.data.remote.ProfileApiService
import com.shoonya.echo.features.settings.domain.model.UserProfile
import com.shoonya.echo.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApiService,
    private val themeStore: ThemeStore,
) : SettingsRepository {

    override suspend fun getProfile(): Result<UserProfile> {
        return runCatching {
            profileApi.getProfile()
                .toResult()
                .getOrThrow()
                .toDomain()
        }.onSuccess { profile ->
            Timber.tag("SettingsRepo").d("getProfile success: %s", profile.username)
        }.onFailure { err ->
            Timber.tag("SettingsRepo").e(err, "getProfile failed")
        }
    }

    override suspend fun updateProfile(
        displayName: String?,
        avatar: String?,
        statusMessage: String?,
        bio: String?,
    ): Result<UserProfile> {
        return runCatching {
            val profileEmbed = com.shoonya.echo.core.data.model.UserProfileEmbedDto(
                displayName = displayName ?: "",
                avatar = avatar ?: "",
                statusMessage = statusMessage ?: "",
                bio = bio ?: "",
            )
            profileApi.updateProfile(UpdateProfileRequest(profile = profileEmbed))
                .toResult()
                .getOrThrow()
                .toDomain()
        }.onSuccess { profile ->
            Timber.tag("SettingsRepo").d("updateProfile success: %s", profile.username)
        }.onFailure { err ->
            Timber.tag("SettingsRepo").e(err, "updateProfile failed")
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return runCatching {
            profileApi.deleteAccount()
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("SettingsRepo").d("deleteAccount success")
        }.onFailure { err ->
            Timber.tag("SettingsRepo").e(err, "deleteAccount failed")
        }
    }

    override val isDarkMode: Flow<Boolean?>
        get() = themeStore.darkThemePreference

    override val isDarkModeSync: Boolean?
        get() = themeStore.darkThemePreferenceSync

    override suspend fun setDarkMode(enabled: Boolean?) {
        themeStore.setDarkThemePreference(enabled)
    }
}
