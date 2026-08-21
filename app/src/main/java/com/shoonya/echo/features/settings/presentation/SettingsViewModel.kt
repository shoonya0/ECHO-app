package com.shoonya.echo.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.auth.domain.usecase.LogoutUseCase
import com.shoonya.echo.features.settings.domain.repository.SettingsRepository
import com.shoonya.echo.features.settings.domain.usecase.DeleteAccountUseCase
import com.shoonya.echo.features.settings.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadProfile()
        observeThemePreference()
    }

    private fun observeThemePreference() {
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { preference ->
                // preference: null = follow system, true = dark, false = light
                _state.update { it.copy(isDarkMode = preference == true) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadProfile -> loadProfile()
            is SettingsEvent.EditProfile -> {
                viewModelScope.launch { _events.emit(SettingsEvent.EditProfile) }
            }
            is SettingsEvent.Logout -> logout()
            is SettingsEvent.DeleteAccount -> deleteAccount()
            is SettingsEvent.ToggleDarkMode -> toggleDarkMode()
            is SettingsEvent.ToggleNotifications -> {
                _state.update {
                    it.copy(notificationsEnabled = !it.notificationsEnabled)
                }
            }
            is SettingsEvent.ShowSnackbar -> {
                viewModelScope.launch { _events.emit(event) }
            }
        }
    }

    private fun toggleDarkMode() {
        val currentIsDark = _state.value.isDarkMode
        // null (follow system) maps to isDarkMode=false in UI, so toggling:
        // false/null → true, true → null (follow system)
        viewModelScope.launch {
            if (currentIsDark) {
                settingsRepository.setDarkMode(null) // go back to follow system
            } else {
                settingsRepository.setDarkMode(true) // force dark
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getProfileUseCase()
                .onSuccess { profile ->
                    _state.update {
                        it.copy(isLoading = false, profile = profile)
                    }
                }
                .onFailure { err ->
                    Timber.tag("SettingsVM").e(err, "loadProfile failed")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }
            logoutUseCase()
            _state.update { it.copy(isLoggingOut = false) }
            _events.emit(SettingsEvent.Logout)
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true) }
            deleteAccountUseCase()
                .onSuccess {
                    _state.update { it.copy(isDeletingAccount = false) }
                    _events.emit(SettingsEvent.Logout)
                }
                .onFailure { err ->
                    Timber.tag("SettingsVM").e(err, "deleteAccount failed")
                    _state.update {
                        it.copy(
                            isDeletingAccount = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }
}
