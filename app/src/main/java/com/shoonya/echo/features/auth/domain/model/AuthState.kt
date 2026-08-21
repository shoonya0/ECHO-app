package com.shoonya.echo.features.auth.domain.model

import com.shoonya.echo.core.domain.model.User

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Error(val message: String) : AuthState
    data object NotAuthenticated : AuthState
}