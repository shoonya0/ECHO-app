package com.shoonya.echo.fakes

import java.io.IOException
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Stands in for OkHttpClient as a [WebSocket.Factory]: records every socket the client opens
 * and lets a test drive the listener callbacks as the server would.
 */
class FakeWebSocketFactory : WebSocket.Factory {
    val sockets = mutableListOf<FakeWebSocket>()

    val last: FakeWebSocket get() = sockets.last()

    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket =
        FakeWebSocket(request, listener).also { sockets += it }
}

class FakeWebSocket(private val request: Request, val listener: WebSocketListener) : WebSocket {
    val sent = mutableListOf<String>()
    var closeCode: Int? = null
        private set
    var closeReason: String? = null
        private set
    var cancelled = false
        private set

    override fun request(): Request = request

    override fun queueSize(): Long = 0

    override fun send(text: String): Boolean {
        if (closeCode != null || cancelled) return false
        sent += text
        return true
    }

    override fun send(bytes: ByteString): Boolean = false

    override fun close(code: Int, reason: String?): Boolean {
        if (closeCode != null) return false
        closeCode = code
        closeReason = reason
        return true
    }

    override fun cancel() {
        cancelled = true
    }

    /** The upgrade succeeded. */
    fun serverOpens() {
        listener.onOpen(
            this,
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(101)
                .message("Switching Protocols")
                .build()
        )
    }

    fun serverSends(text: String) = listener.onMessage(this, text)

    /** The server sends a close frame; OkHttp reports onClosing, then onClosed once acked. */
    fun serverCloses(code: Int, reason: String) {
        listener.onClosing(this, code, reason)
        listener.onClosed(this, code, reason)
    }

    /** The server answers a close the app started (OkHttp then reports the peer's code). */
    fun serverAcksClose() {
        val code = closeCode ?: 1000
        listener.onClosing(this, code, closeReason.orEmpty())
        listener.onClosed(this, code, closeReason.orEmpty())
    }

    fun fails(message: String = "connection reset") =
        listener.onFailure(this, IOException(message), null)
}
