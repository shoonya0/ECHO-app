package com.shoonya.echo.features.auth.domain.repository

import com.shoonya.echo.core.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signup(email: String, username: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}