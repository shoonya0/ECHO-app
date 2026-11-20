package com.shoonya.echo.features.chat.domain.repository

import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import kotlinx.coroutines.flow.SharedFlow

enum class WsConnectionStatus {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}

interface WebSocketRepository {
    val events: SharedFlow<DomainEvent>
    val connectionStatus: SharedFlow<WsConnectionStatus>

    suspend fun connect()
    suspend fun disconnect()
    fun isConnected(): Boolean

    /**
     * Sends a command and awaits a correlated [ServerFrame.Response] or [ServerFrame.Error].
     * Times out after 10 seconds.
     */
    suspend fun send(command: WebSocketCommand): Result<ServerFrame>

    /**
     * Sends a command without waiting for a response (fire-and-forget).
     * Returns false when the socket isn't open and the command was dropped.
     */
    fun sendRaw(command: WebSocketCommand): Boolean
}
