package com.shoonya.echo.features.settings.presentation

import com.shoonya.echo.features.settings.domain.model.UserProfile

data class SettingsUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isLoggingOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
)