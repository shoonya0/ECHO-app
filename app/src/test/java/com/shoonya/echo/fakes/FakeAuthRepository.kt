package com.shoonya.echo.fakes

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.auth.domain.repository.AuthRepository

class FakeAuthRepository : AuthRepository {
    private var loginResult: Result<User> = Result.failure(Exception("Not set"))
    private var signupResult: Result<Unit> = Result.failure(Exception("Not set"))
    private var token: String? = null
    var logoutCalled: Boolean = false
        private set

    fun setLoginSuccess(user: User, token: String) {
        this.token = token
        loginResult = Result.success(user)
    }

    fun setLoginError(message: String) {
        loginResult = Result.failure(Exception(message))
    }

    fun setSignupSuccess() {
        signupResult = Result.success(Unit)
    }

    fun setSignupError(message: String) {
        signupResult = Result.failure(Exception(message))
    }

    override suspend fun login(email: String, password: String): Result<User> = loginResult

    override suspend fun signup(email: String, username: String, password: String): Result<Unit> = signupResult

    override suspend fun logout() {
        token = null
        logoutCalled = true
    }

    override suspend fun isLoggedIn(): Boolean = token != null
}