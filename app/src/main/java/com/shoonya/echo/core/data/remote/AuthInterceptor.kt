package com.shoonya.echo.core.data.remote

import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.domain.model.SessionEndReason
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Adds the bearer token and ends the session when the server says it is over (#33):
 * `401` (`ACCOUNT_NOT_FOUND` says the account was deleted, #22), or `403` with code
 * `ACCOUNT_BANNED` / `ACCOUNT_INACTIVE`. Anything else,
 * including `503 AUTH_UNAVAILABLE` and other `403`s, keeps the session.
 */
class AuthInterceptor internal constructor(
    private val accessToken: () -> String?,
    private val endSession: (SessionEndReason) -> Unit
) : Interceptor {

    @Inject
    constructor(tokenStore: SecureTokenStore, sessionManager: SessionManager) :
        this(tokenStore::getAccessToken, sessionManager::expire)

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)

        // Only end the session when a token was actually sent (authenticated request).
        // Unauthenticated requests (e.g., login/signup) that return 401 or 403 mean
        // invalid credentials or a blocked account — there is no session to end.
        if (token != null) {
            sessionEndReason(response)?.let { reason ->
                Timber.tag(
                    "AuthInterceptor"
                ).w("%d received with token — session ended (%s)", response.code, reason)
                endSession(reason)
            }
        }

        return response
    }

    private fun sessionEndReason(response: Response): SessionEndReason? = when (response.code) {
        // peekBody leaves the body readable for the caller (Retrofit's error handling).
        401 -> when (errorCode(response.peekBody(MAX_PEEK_BYTES).string())) {
            "ACCOUNT_NOT_FOUND" -> SessionEndReason.ACCOUNT_NOT_FOUND
            else -> SessionEndReason.EXPIRED
        }

        403 -> when (errorCode(response.peekBody(MAX_PEEK_BYTES).string())) {
            "ACCOUNT_BANNED" -> SessionEndReason.ACCOUNT_BANNED
            "ACCOUNT_INACTIVE" -> SessionEndReason.ACCOUNT_INACTIVE
            else -> null
        }

        else -> null
    }

    /** The `code` of a token-error body `{ "error": "...", "code": "..." }`, or null. */
    private fun errorCode(body: String): String? = runCatching {
        Json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.content
    }.getOrNull()

    private companion object {
        const val MAX_PEEK_BYTES = 16L * 1024
    }
}
