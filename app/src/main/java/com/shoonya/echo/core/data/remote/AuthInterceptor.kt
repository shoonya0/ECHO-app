package com.shoonya.echo.core.data.remote

import com.shoonya.echo.core.data.local.SecureTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)

        // Only expire the session when a token was actually sent (authenticated request).
        // Unauthenticated requests (e.g., login/signup) that return 401 simply
        // mean invalid credentials — not an expired session.
        if (response.code == 401 && token != null) {
            Timber.tag("AuthInterceptor").w("401 received with token — session expired")
            sessionManager.expire()
        }

        return response
    }
}
