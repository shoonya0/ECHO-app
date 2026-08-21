package com.shoonya.echo.features.settings.domain.usecase

import com.shoonya.echo.features.settings.domain.model.UserProfile
import com.shoonya.echo.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        displayName: String?,
        avatar: String?,
        statusMessage: String?,
        bio: String?,
    ): Result<UserProfile> =
        settingsRepository.updateProfile(displayName, avatar, statusMessage, bio)
}