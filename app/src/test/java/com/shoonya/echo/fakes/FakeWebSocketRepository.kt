package com.shoonya.echo.fakes

import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeWebSocketRepository : WebSocketRepository {

    private val _events = MutableSharedFlow<DomainEvent>(replay = 0, extraBufferCapacity = 64)
    override val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(WsConnectionStatus.CONNECTED)
    override val connectionStatus: SharedFlow<WsConnectionStatus> = _connectionStatus.asStateFlow()

    private var sendResult: Result<ServerFrame> = Result.failure(Exception("Not set"))
    private var connected = false

    // Allows a test to suspend send() until explicitly completed, so tests can
    // deterministically interleave a broadcast with a deferred ack/failure.
    private var gateSend = false
    private val sendGate = CompletableDeferred<Result<ServerFrame>>()

    // Tracks all sent commands for verification
    val sentCommands = mutableListOf<WebSocketCommand>()

    fun setSendResult(result: Result<ServerFrame>) {
        sendResult = result
    }

    fun enableSendGate() {
        gateSend = true
    }

    fun completeSendGate(result: Result<ServerFrame>) {
        sendGate.complete(result)
    }

    fun setConnected(value: Boolean) {
        connected = value
    }

    fun emitDomainEvent(event: DomainEvent) {
        _events.tryEmit(event)
    }

    fun setConnectionStatus(status: WsConnectionStatus) {
        _connectionStatus.value = status
    }

    override suspend fun connect() {
        connected = true
        _connectionStatus.value = WsConnectionStatus.CONNECTED
    }

    override suspend fun disconnect() {
        connected = false
        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
    }

    override fun isConnected(): Boolean = connected

    override suspend fun send(command: WebSocketCommand): Result<ServerFrame> {
        sentCommands.add(command)
        return if (gateSend) sendGate.await() else sendResult
    }

    override fun sendRaw(command: WebSocketCommand) {
        sentCommands.add(command)
    }
}