package com.shoonya.echo.features.auth.data.repository

import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.core.data.remote.toUnitResult
import com.shoonya.echo.features.chat.data.local.ChatCache
import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.auth.data.model.LoginRequest
import com.shoonya.echo.features.auth.data.model.LoginUserDto
import com.shoonya.echo.features.auth.data.model.SignupRequest
import com.shoonya.echo.features.auth.data.model.toDomain
import com.shoonya.echo.features.auth.data.remote.AuthApiService
import com.shoonya.echo.features.auth.domain.repository.AuthRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenStore: SecureTokenStore,
    private val chatCache: ChatCache,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return runCatching {
            api.login(LoginRequest(email, password))
                .toResult()
                .getOrThrow()
        }.mapCatching { response ->
            require(response.token.isNotBlank()) { "Server returned an empty token" }
            tokenStore.saveAccessToken(response.token)
            tokenStore.saveUserId(response.user.id)
            response.user.toDomain().also {
                Timber.tag("AuthRepo").d("login success: %s", it.username)
            }
        }.onFailure { err ->
            Timber.tag("AuthRepo").e(err, "login failed for: %s", email)
        }
    }

    override suspend fun signup(email: String, username: String, password: String): Result<Unit> {
        return runCatching {
            api.signup(SignupRequest(email, username, password))
                .toResult()
                .getOrThrow()
        }.mapCatching {
            Timber.tag("AuthRepo").d("signup success: %s", username)
        }.onFailure { err ->
            Timber.tag("AuthRepo").e(err, "signup failed for: %s", email)
        }
    }

    override suspend fun logout() {
        runCatching {
            api.logout().toUnitResult().getOrThrow()
        }.onFailure { err ->
            Timber.tag("AuthRepo").w(err, "logout API call failed; clearing local state anyway")
        }
        tokenStore.clearTokens()
        chatCache.clear()
        Timber.tag("AuthRepo").d("logout: token and cache cleared")
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenStore.getAccessToken() != null
    }
}