package com.shoonya.echo.features.chat.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ReconnectManager @Inject constructor(
    private val wsClient: EchoWebSocketClient,
) {
    private var retryCount = 0
    private val maxRetries = 5
    private val baseDelay = 1_000L

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 16)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    fun onDisconnected(scope: CoroutineScope) {
        if (retryCount >= maxRetries) {
            Timber.tag("WS").w("max retries reached (%d), stopping reconnect", maxRetries)
            _events.tryEmit(WebSocketEvent.ReconnectFailed)
            return
        }
        val delayMs = (baseDelay * (1L shl retryCount)) + Random.nextLong(0, 1000)
        retryCount++
        Timber.tag("WS").i("reconnect attempt %d in %dms", retryCount, delayMs)
        scope.launch {
            delay(delayMs)
            wsClient.connect()
        }
    }

    fun reset() {
        retryCount = 0
        Timber.tag("WS").d("reconnect counter reset")
    }
}