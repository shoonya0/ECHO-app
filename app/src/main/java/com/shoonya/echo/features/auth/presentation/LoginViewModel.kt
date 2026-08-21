package com.shoonya.echo.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.core.util.isValidEmail
import com.shoonya.echo.features.auth.domain.usecase.LoginUseCase
import com.shoonya.echo.features.auth.domain.usecase.SignupUseCase
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
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username, usernameError = null, error = null) }
    }

    fun onToggleMode() {
        val loginMode = _state.value.isLoginMode
        _state.update {
            it.copy(
                isLoginMode = !loginMode,
                emailError = null,
                passwordError = null,
                usernameError = null,
                error = null,
            )
        }
    }

    fun onSubmit() {
        if (_state.value.isLoginMode) {
            login()
        } else {
            signup()
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun login() {
        val currentState = _state.value
        val email = currentState.email.trim()
        val password = currentState.password

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginUseCase(email, password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                    _events.emit(LoginEvent.NavigateToHome)
                }
                .onFailure { err ->
                    Timber.tag("LoginVM").e(err, "login failed")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun signup() {
        val currentState = _state.value
        val email = currentState.email.trim()
        val username = currentState.username.trim()
        val password = currentState.password

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val usernameError = validateUsername(username)

        if (emailError != null || passwordError != null || usernameError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    usernameError = usernameError,
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signupUseCase(email, username, password)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSignupSuccess = true,
                            // Auto-redirect to login mode after signup
                            isLoginMode = true,
                            password = "",
                            username = "",
                        )
                    }
                    _events.emit(LoginEvent.NavigateToLogin)
                    _events.emit(LoginEvent.ShowSnackbar("Account created! Please log in."))
                }
                .onFailure { err ->
                    Timber.tag("LoginVM").e(err, "signup failed")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Please enter your email"
            !email.isValidEmail() -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Please enter your password"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isBlank() -> "Please enter a username"
            else -> null
        }
    }
}

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data object NavigateToLogin : LoginEvent
    data class ShowSnackbar(val message: String) : LoginEvent
}