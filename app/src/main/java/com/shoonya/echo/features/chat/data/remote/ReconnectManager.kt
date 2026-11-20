package com.shoonya.echo.features.chat.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ReconnectManager @Inject constructor(private val wsClient: EchoWebSocketClient) {
    private var retryCount = 0
    private val maxRetries = 5
    private val baseDelay = 1_000L
    private var pending: Job? = null

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 16)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    @Synchronized
    fun onDisconnected(scope: CoroutineScope) {
        if (retryCount >= maxRetries) {
            Timber.tag("WS").w("max retries reached (%d), stopping reconnect", maxRetries)
            _events.tryEmit(WebSocketEvent.ReconnectFailed)
            return
        }
        val delayMs = (baseDelay * (1L shl retryCount)) + Random.nextLong(0, 1000)
        retryCount++
        Timber.tag("WS").i("reconnect attempt %d in %dms", retryCount, delayMs)
        pending?.cancel()
        pending = scope.launch {
            delay(delayMs)
            wsClient.connect()
        }
    }

    fun reset() {
        retryCount = 0
        Timber.tag("WS").d("reconnect counter reset")
    }

    /** Drops a scheduled reconnect and resets the backoff: logout or a session-ending close. */
    @Synchronized
    fun stop() {
        pending?.cancel()
        pending = null
        retryCount = 0
        Timber.tag("WS").d("reconnect stopped")
    }

    companion object {
        /**
         * Server close codes that end the session (server #76): 4401 (TOKEN_REVOKED,
         * TOKEN_EXPIRED, ACCOUNT_NOT_FOUND) and 4403 (ACCOUNT_BANNED, ACCOUNT_INACTIVE).
         * Reconnecting would only get a 401 or 403 at the upgrade.
         */
        val SESSION_ENDED_CODES = setOf(4401, 4403)
    }
}
