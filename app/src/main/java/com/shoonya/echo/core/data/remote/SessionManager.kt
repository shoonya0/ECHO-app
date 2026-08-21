package com.shoonya.echo.core.data.remote

import com.shoonya.echo.core.data.local.SecureTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: SecureTokenStore,
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /**
     * Clears all stored tokens and emits a session-expired event
     * so the UI layer can navigate to Login.
     */
    fun expire() {
        tokenStore.clearTokens()
        Timber.tag("SessionManager").w("Session expired — tokens cleared")
        _sessionExpired.tryEmit(Unit)
    }
}