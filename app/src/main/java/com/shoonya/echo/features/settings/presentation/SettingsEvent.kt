package com.shoonya.echo.features.settings.presentation

sealed interface SettingsEvent {
    data object LoadProfile : SettingsEvent
    data object EditProfile : SettingsEvent
    data object Logout : SettingsEvent
    data object DeleteAccount : SettingsEvent
    data object ToggleDarkMode : SettingsEvent
    data object ToggleNotifications : SettingsEvent
    data class ShowSnackbar(val message: String) : SettingsEvent
}