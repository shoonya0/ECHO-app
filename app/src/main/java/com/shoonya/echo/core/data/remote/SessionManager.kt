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
     * Clears all stored tokens and the account's in-memory data, then emits a
     * session-ended event with [reason] so the UI layer can navigate to Login and
     * say why. Every reason clears the data: whichever account logs in next in this
     * process must not see the previous account's chats (#45).
     */
    fun expire(reason: SessionEndReason = SessionEndReason.EXPIRED) {
        clearTokens()
        clearAccountData()
        Timber.tag(
            "SessionManager"
        ).i("Session ended (%s) — tokens and account data cleared", reason)
        _sessionExpired.tryEmit(reason)
    }
}
