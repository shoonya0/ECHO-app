package com.shoonya.echo.features.chat.data.remote

import com.shoonya.echo.BuildConfig
import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.data.remote.WsClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WebSocketEvent {
    data object Connected : WebSocketEvent
    data class Message(val raw: String) : WebSocketEvent
    data class Disconnected(val code: Int, val reason: String) : WebSocketEvent
    data class Error(val message: String) : WebSocketEvent
    data object ReconnectFailed : WebSocketEvent
}

@Singleton
class EchoWebSocketClient @Inject constructor(
    @WsClient private val okHttpClient: OkHttpClient,
    private val tokenStore: SecureTokenStore,
) {
    private var webSocket: WebSocket? = null
    @Volatile
    private var isConnected = false

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    fun connect() {
        val token = tokenStore.getAccessToken()
        if (token == null) {
            Timber.tag("WS").w("No auth token — cannot connect")
            _events.tryEmit(WebSocketEvent.Error("No auth token"))
            return
        }

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}/echo/v1/ws/chat")
            .header("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                Timber.tag("WS").i("connected")
                _events.tryEmit(WebSocketEvent.Connected)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Timber.tag("WS").d("received: %s", text.take(200))
                _events.tryEmit(WebSocketEvent.Message(text))
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Timber.tag("WS").e(t, "connection failed, code=%d", response?.code ?: -1)
                _events.tryEmit(WebSocketEvent.Error(t.message ?: "Connection failed"))
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Timber.tag("WS").i("closing: %d %s", code, reason)
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                Timber.tag("WS").i("closed: %d %s", code, reason)
                _events.tryEmit(WebSocketEvent.Disconnected(code, reason))
            }
        })
    }

    fun send(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    fun disconnect() {
        isConnected = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    fun isConnected(): Boolean = isConnected
}