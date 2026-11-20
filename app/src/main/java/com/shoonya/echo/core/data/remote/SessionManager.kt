package com.shoonya.echo.core.data.remote

import com.shoonya.echo.core.data.local.PerAccountCache
import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.domain.model.SessionEndReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

@Singleton
class SessionManager internal constructor(
    private val clearTokens: () -> Unit,
    private val clearAccountData: () -> Unit
) {
    @Inject
    constructor(
        tokenStore: SecureTokenStore,
        caches: Set<@JvmSuppressWildcards PerAccountCache>
    ) : this(tokenStore::clearTokens, { caches.forEach(PerAccountCache::clear) })

    private val _sessionExpired =
        MutableSharedFlow<SessionEndReason>(replay = 0, extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<SessionEndReason> = _sessionExpired.asSharedFlow()

    /**
     * Clears all stored tokens and emits a session-ended event with [reason]
     * so the UI layer can navigate to Login and say why. For a deleted account
     * ([SessionEndReason.ACCOUNT_NOT_FOUND]) it also clears its in-memory data.
     */
    fun expire(reason: SessionEndReason = SessionEndReason.EXPIRED) {
        clearTokens()
        if (reason == SessionEndReason.ACCOUNT_NOT_FOUND) {
            clearAccountData()
            Timber.tag("SessionManager").i("Account not found — local account data cleared")
        }
        Timber.tag("SessionManager").w("Session ended (%s) — tokens cleared", reason)
        _sessionExpired.tryEmit(reason)
    }
}
