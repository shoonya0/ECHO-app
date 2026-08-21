package com.shoonya.echo.features.settings.domain.usecase

import com.shoonya.echo.features.settings.domain.model.UserProfile
import com.shoonya.echo.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): Result<UserProfile> =
        settingsRepository.getProfile()
}