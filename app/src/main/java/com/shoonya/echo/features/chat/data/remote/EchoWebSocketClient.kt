package com.shoonya.echo.features.chat.data.remote

import com.shoonya.echo.BuildConfig
import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.data.remote.WsClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

sealed interface WebSocketEvent {
    data object Connected : WebSocketEvent
    data class Message(val raw: String) : WebSocketEvent
    data class Disconnected(val code: Int, val reason: String) : WebSocketEvent
    data class Error(val message: String) : WebSocketEvent
    data object ReconnectFailed : WebSocketEvent
}

/**
 * Owns the app's WebSocket to the ECHO server.
 *
 * The primary constructor is the test seam: unit tests pass a fake [WebSocket.Factory]
 * (OkHttpClient implements it) and a token lambda, so they need no Context or network.
 */
@Singleton
class EchoWebSocketClient(
    private val webSocketFactory: WebSocket.Factory,
    private val accessToken: () -> String?,
    private val url: String = "${BuildConfig.WS_BASE_URL}/echo/v1/ws/chat"
) {
    @Inject
    constructor(
        @WsClient okHttpClient: OkHttpClient,
        tokenStore: SecureTokenStore
    ) : this(okHttpClient, tokenStore::getAccessToken)

    // Guards webSocket and isOpen. OkHttp calls the listener on its own threads, so the
    // listener takes the lock too; it only ever does non-blocking work while holding it.
    private val lock = Any()

    // The one socket this client owns, from connect() until it closes, fails or is
    // disconnected. Callbacks from any other socket are stale and ignored (#1).
    private var webSocket: WebSocket? = null
    private var isOpen = false

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    /** Opens the socket. A no-op while one is already connecting or open. */
    fun connect() {
        synchronized(lock) {
            if (webSocket != null) {
                Timber.tag(
                    "WS"
                ).d("connect ignored: a socket is already %s", if (isOpen) "open" else "connecting")
                return
            }
            val token = accessToken()
            if (token == null) {
                // Every attempt would fail the same way, so stop instead of reporting an
                // Error, which the repository answers with a reconnect (#26).
                Timber.tag("WS").w("No auth token — cannot connect, not retrying")
                _events.tryEmit(WebSocketEvent.ReconnectFailed)
                return
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()

            // Assigned under the lock, so a callback that races this call waits for it.
            webSocket = webSocketFactory.newWebSocket(request, Listener())
        }
    }

    fun send(text: String): Boolean = synchronized(lock) { webSocket }?.send(text) ?: false

    /**
     * Closes the socket on the app's request. Its later close callback is ignored, so
     * no Disconnected event (and no reconnect) follows.
     */
    fun disconnect() {
        val ws = synchronized(lock) {
            val current = webSocket
            webSocket = null
            isOpen = false
            current
        }
        ws?.close(NORMAL_CLOSURE, "User disconnected")
    }

    fun isConnected(): Boolean = synchronized(lock) { isOpen }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            synchronized(lock) {
                if (ws !== webSocket) return
                isOpen = true
                Timber.tag("WS").i("connected")
                _events.tryEmit(WebSocketEvent.Connected)
            }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            synchronized(lock) {
                if (ws !== webSocket) return
                Timber.tag("WS").d("received: %s", text.take(200))
                _events.tryEmit(WebSocketEvent.Message(text))
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            synchronized(lock) {
                if (!release(ws)) return
                val sessionEndCode = response?.let { HANDSHAKE_SESSION_END_CODES[it.code] }
                if (response != null && sessionEndCode != null) {
                    // The upgrade was refused (401/403): the session is over, exactly as
                    // when the server closes an open socket with 4401/4403 (#26).
                    val reason = authErrorCode(response) ?: "HTTP ${response.code}"
                    Timber.tag("WS").w("handshake rejected: %d %s", response.code, reason)
                    _events.tryEmit(WebSocketEvent.Disconnected(sessionEndCode, reason))
                    return
                }
                Timber.tag("WS").e(t, "connection failed, code=%d", response?.code ?: -1)
                _events.tryEmit(WebSocketEvent.Error(t.message ?: "Connection failed"))
            }
        }

        /**
         * The `code` of the server's token-error body `{"error": "...", "code": "..."}`, such
         * as TOKEN_REVOKED or ACCOUNT_BANNED; null if the body is missing or not that shape.
         */
        private fun authErrorCode(response: Response): String? = runCatching {
            val body = response.peekBody(MAX_ERROR_BODY_BYTES).string()
            Json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        // The server's close frame decides why the socket ended (e.g. 4401, 4403), so report
        // it here. The onClosed/onFailure that follows belongs to a released socket.
        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(NORMAL_CLOSURE, null)
            synchronized(lock) {
                if (!release(ws)) return
                Timber.tag("WS").i("closing: %d %s", code, reason)
                _events.tryEmit(WebSocketEvent.Disconnected(code, reason))
            }
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            synchronized(lock) {
                if (!release(ws)) return
                Timber.tag("WS").i("closed: %d %s", code, reason)
                _events.tryEmit(WebSocketEvent.Disconnected(code, reason))
            }
        }

        /** Drops [ws] if it is the current socket; false if it is stale. Call under the lock. */
        private fun release(ws: WebSocket): Boolean {
            if (ws !== webSocket) return false
            webSocket = null
            isOpen = false
            return true
        }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
        const val MAX_ERROR_BODY_BYTES = 4_096L

        /** HTTP status of a refused upgrade → the close code that means the same (server #76). */
        val HANDSHAKE_SESSION_END_CODES = mapOf(401 to 4401, 403 to 4403)
    }
}
