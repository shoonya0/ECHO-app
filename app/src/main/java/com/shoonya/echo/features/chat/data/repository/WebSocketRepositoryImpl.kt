package com.shoonya.echo.features.chat.data.repository

import com.shoonya.echo.features.chat.data.remote.EchoWebSocketClient
import com.shoonya.echo.features.chat.data.remote.ReconnectManager
import com.shoonya.echo.features.chat.data.remote.WebSocketEvent
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.model.toDomainEvent
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketRepositoryImpl @Inject constructor(
    private val wsClient: EchoWebSocketClient,
    private val reconnectManager: ReconnectManager,
    private val json: Json,
) : WebSocketRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<ServerFrame>>()

    private val _events = MutableSharedFlow<DomainEvent>(replay = 0, extraBufferCapacity = 64)
    override val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(WsConnectionStatus.DISCONNECTED)
    override val connectionStatus: SharedFlow<WsConnectionStatus> = _connectionStatus.asStateFlow()

    init {
        scope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        _connectionStatus.value = WsConnectionStatus.CONNECTED
                        reconnectManager.reset()
                    }
                    is WebSocketEvent.Message -> handleRawMessage(event.raw)
                    is WebSocketEvent.Disconnected -> {
                        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
                        reconnectManager.onDisconnected(scope)
                    }
                    is WebSocketEvent.Error -> {
                        Timber.tag("WS").e("WebSocket error: %s", event.message)
                        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
                        reconnectManager.onDisconnected(scope)
                    }
                    is WebSocketEvent.ReconnectFailed -> {
                        _connectionStatus.value = WsConnectionStatus.FAILED
                    }
                }
            }
        }
    }

    override suspend fun connect() {
        _connectionStatus.value = WsConnectionStatus.CONNECTING
        wsClient.connect()
    }

    override suspend fun disconnect() {
        wsClient.disconnect()
        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
        // Fail all pending requests
        pendingRequests.values.forEach { deferred ->
            deferred.completeExceptionally(IllegalStateException("WebSocket disconnected"))
        }
        pendingRequests.clear()
    }

    override fun isConnected(): Boolean = wsClient.isConnected()

    override suspend fun send(command: WebSocketCommand): Result<ServerFrame> {
        val deferred = CompletableDeferred<ServerFrame>()
        pendingRequests[command.requestId] = deferred
        val text = json.encodeToString(WebSocketCommand.serializer(), command)
        val sent = wsClient.send(text)
        if (!sent) {
            pendingRequests.remove(command.requestId)
            deferred.completeExceptionally(IllegalStateException("WebSocket not connected"))
        }

        return try {
            val frame = withTimeout(10_000) { deferred.await() }
            when (frame) {
                is ServerFrame.Response -> Result.success(frame)
                is ServerFrame.Error -> {
                    val msg = frame.data.message
                    Timber.tag("WS").e("command error: code=%s msg=%s", frame.data.code, msg)
                    Result.failure(RuntimeException(msg))
                }
                else -> Result.failure(RuntimeException("Unexpected frame type: ${frame.type}"))
            }
        } catch (e: Exception) {
            pendingRequests.remove(command.requestId)
            Result.failure(e)
        }
    }

    override fun sendRaw(command: WebSocketCommand) {
        val text = json.encodeToString(WebSocketCommand.serializer(), command)
        wsClient.send(text)
    }

    private fun handleRawMessage(raw: String) {
        try {
            val frame = json.decodeFromString(ServerFrame.serializer(), raw)
            val requestId = frame.requestId
            if (requestId != null) {
                val deferred = pendingRequests[requestId]
                if (deferred != null && (frame is ServerFrame.Response || frame is ServerFrame.Error)) {
                    // Only ack/error frames resolve a pending request. Broadcast frames
                    // (e.g. `message` when the server echoes our requestId back to the
                    // sender) must still be emitted as domain events so the sent message
                    // actually appears in the UI.
                    pendingRequests.remove(requestId)
                    deferred.complete(frame)
                    return
                }
            }
            // Broadcast or uncorrelated frame — convert and emit as a domain event
            val domainEvent = frame.toDomainEvent()
            _events.tryEmit(domainEvent)
        } catch (e: Exception) {
            Timber.tag("WS").e(e, "failed to parse WebSocket frame: %s", raw.take(200))
        }
    }
}