package com.shoonya.echo.core.data.remote

import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.domain.model.SessionEndReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

@Singleton
class SessionManager internal constructor(private val clearTokens: () -> Unit) {
    @Inject
    constructor(tokenStore: SecureTokenStore) : this(tokenStore::clearTokens)

    private val _sessionExpired =
        MutableSharedFlow<SessionEndReason>(replay = 0, extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<SessionEndReason> = _sessionExpired.asSharedFlow()

    /**
     * Clears all stored tokens and emits a session-ended event with [reason]
     * so the UI layer can navigate to Login and say why.
     */
    fun expire(reason: SessionEndReason = SessionEndReason.EXPIRED) {
        clearTokens()
        Timber.tag("SessionManager").w("Session ended (%s) — tokens cleared", reason)
        _sessionExpired.tryEmit(reason)
    }
}
