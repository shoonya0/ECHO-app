package com.shoonya.echo.features.auth.presentation

import com.shoonya.echo.core.domain.model.SessionEndReason

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null,
    val isAuthenticated: Boolean = false,
    val isSignupSuccess: Boolean = false,
    /** Why the last session ended, shown until the next submit; null after a normal start. */
    val sessionEndReason: SessionEndReason? = null
)
