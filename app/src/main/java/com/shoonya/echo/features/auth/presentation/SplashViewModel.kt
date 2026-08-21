package com.shoonya.echo.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Checking)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SplashEvent>(replay = 1)
    val events: SharedFlow<SplashEvent> = _events.asSharedFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()
            if (loggedIn) {
                _events.emit(SplashEvent.NavigateToHome)
            } else {
                _events.emit(SplashEvent.NavigateToLogin)
            }
            _state.value = SplashState.Done
        }
    }
}

sealed interface SplashState {
    data object Checking : SplashState
    data object Done : SplashState
}

sealed interface SplashEvent {
    data object NavigateToLogin : SplashEvent
    data object NavigateToHome : SplashEvent
}