package com.shoonya.echo.features.chat.data.repository

import com.shoonya.echo.core.data.remote.SessionManager
import com.shoonya.echo.core.domain.model.SessionEndReason
import com.shoonya.echo.features.chat.data.remote.EchoWebSocketClient
import com.shoonya.echo.features.chat.data.remote.ReconnectManager
import com.shoonya.echo.features.chat.data.remote.WebSocketEvent
import com.shoonya.echo.features.chat.data.remote.WsSessionEnder
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.model.isWelcomeFrame
import com.shoonya.echo.features.chat.domain.model.toDomainEvent
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class WebSocketRepositoryImpl(
    private val wsClient: EchoWebSocketClient,
    private val reconnectManager: ReconnectManager,
    private val json: Json,
    private val sessionEnder: WsSessionEnder,
    // Fires when the session ends anywhere, e.g. a REST 401/403 (#27).
    private val sessionEnded: Flow<SessionEndReason>,
    // Test seam: unit tests pass a TestScope so reconnect delays run on virtual time.
    private val scope: CoroutineScope
) : WebSocketRepository {

    @Inject
    constructor(
        wsClient: EchoWebSocketClient,
        reconnectManager: ReconnectManager,
        json: Json,
        sessionEnder: WsSessionEnder,
        sessionManager: SessionManager
    ) : this(
        wsClient,
        reconnectManager,
        json,
        sessionEnder,
        sessionManager.sessionExpired,
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    // True after disconnect() or a session-ending close, until the next connect(): an
    // event still queued from the old socket must not schedule a reconnect.
    @Volatile
    private var stopped = false

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<ServerFrame>>()

    private val _events = MutableSharedFlow<DomainEvent>(replay = 0, extraBufferCapacity = 64)
    override val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(WsConnectionStatus.DISCONNECTED)
    override val connectionStatus: SharedFlow<WsConnectionStatus> = _connectionStatus.asStateFlow()

    init {
        // The session can end outside the socket (REST 401/403 → SessionManager.expire): a
        // logged-out user must not keep receiving live events, so close without reconnecting.
        scope.launch {
            sessionEnded.collect { reason ->
                Timber.tag("WS").i("session ended (%s): closing the socket", reason)
                disconnect()
            }
        }
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
                        if (event.code in ReconnectManager.SESSION_ENDED_CODES) {
                            endSession(event.code, event.reason)
                        } else {
                            scheduleReconnect()
                        }
                    }

                    is WebSocketEvent.Error -> {
                        Timber.tag("WS").e("WebSocket error: %s", event.message)
                        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
                        scheduleReconnect()
                    }

                    is WebSocketEvent.ReconnectFailed -> {
                        _connectionStatus.value = WsConnectionStatus.FAILED
                    }
                }
            }
        }
        // The reconnect manager reports giving up on its own flow, not the client's (#25).
        scope.launch {
            reconnectManager.events.collect { event ->
                if (event is WebSocketEvent.ReconnectFailed) {
                    _connectionStatus.value = WsConnectionStatus.FAILED
                }
            }
        }
    }

    override suspend fun connect() {
        // Several screens call connect(); the client keeps the one socket (#1), and an
        // open one must not be shown as connecting again.
        if (wsClient.isConnected()) return
        stopped = false
        _connectionStatus.value = WsConnectionStatus.CONNECTING
        wsClient.connect()
    }

    override suspend fun disconnect() {
        stopped = true
        reconnectManager.stop()
        wsClient.disconnect()
        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
        failPendingRequests()
    }

    // The server closed with 4401/4403: reconnecting would only get 401/403 at the
    // upgrade, so log out instead (server #76, auth-and-connection.md).
    private fun endSession(code: Int, reason: String) {
        Timber.tag("WS").w("session ended by server: %d %s", code, reason)
        stopped = true
        reconnectManager.stop()
        failPendingRequests()
        sessionEnder.endSession(sessionEndReason(code, reason))
    }

    // The close reason is the REST code (server #76), so Login can say why, and a deleted
    // account's local data is cleared (#22).
    private fun sessionEndReason(code: Int, reason: String): SessionEndReason = when {
        code == CLOSE_UNAUTHORIZED && reason == "ACCOUNT_NOT_FOUND" ->
            SessionEndReason.ACCOUNT_NOT_FOUND

        code == CLOSE_FORBIDDEN && reason == "ACCOUNT_BANNED" -> SessionEndReason.ACCOUNT_BANNED

        code == CLOSE_FORBIDDEN && reason == "ACCOUNT_INACTIVE" -> SessionEndReason.ACCOUNT_INACTIVE

        else -> SessionEndReason.EXPIRED
    }

    private fun scheduleReconnect() {
        if (stopped) {
            Timber.tag("WS").d("no reconnect: stopped by disconnect or session end")
            return
        }
        reconnectManager.onDisconnected(scope)
    }

    private fun failPendingRequests() {
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

    override fun sendRaw(command: WebSocketCommand): Boolean {
        val text = json.encodeToString(WebSocketCommand.serializer(), command)
        return wsClient.send(text)
    }

    private fun handleRawMessage(raw: String) {
        try {
            val element = json.parseToJsonElement(raw)
            if (element.isWelcomeFrame()) {
                // Nothing reads the welcome's clientId/instanceId yet (#30).
                Timber.tag("WS").d("welcome frame received")
                return
            }
            val frame = json.decodeFromJsonElement(ServerFrame.serializer(), element)
            val requestId = frame.requestId
            if (requestId != null) {
                val deferred = pendingRequests[requestId]
                if (deferred != null &&
                    (frame is ServerFrame.Response || frame is ServerFrame.Error)
                ) {
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

    private companion object {
        const val CLOSE_UNAUTHORIZED = 4401
        const val CLOSE_FORBIDDEN = 4403
    }
}
