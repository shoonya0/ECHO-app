package com.shoonya.echo.chat

import com.shoonya.echo.core.data.remote.NetworkModule
import com.shoonya.echo.core.domain.model.SessionEndReason
import com.shoonya.echo.fakes.FakeWebSocketFactory
import com.shoonya.echo.features.chat.data.remote.EchoWebSocketClient
import com.shoonya.echo.features.chat.data.remote.ReconnectManager
import com.shoonya.echo.features.chat.data.repository.WebSocketRepositoryImpl
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

/**
 * Drives the real client, reconnect manager and repository against a fake socket factory;
 * reconnect delays run on the test scheduler's virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketRepositoryImplTest {

    private val factory = FakeWebSocketFactory()
    private val client = EchoWebSocketClient(factory, { "tok" }, "ws://test/ws")
    private val reconnectManager = ReconnectManager(client)
    private val endReasons = mutableListOf<SessionEndReason>()
    private val sessionsEnded get() = endReasons.size
    private val sessionEnded = MutableSharedFlow<SessionEndReason>(extraBufferCapacity = 1)

    private fun TestScope.repository() = WebSocketRepositoryImpl(
        client,
        reconnectManager,
        NetworkModule.provideJson(),
        { endReasons += it },
        sessionEnded,
        backgroundScope
    ).also { runCurrent() } // start collecting client events

    private suspend fun WebSocketRepositoryImpl.status() = connectionStatus.first()

    // Longer than the largest backoff (16 s + 1 s jitter). advanceUntilIdle() would not
    // run the reconnect: it ignores work in backgroundScope.
    private fun TestScope.waitOutReconnectDelay() {
        advanceTimeBy(60_000)
        runCurrent()
    }

    @Test
    fun `connect reports connecting, then connected on open`() = runTest {
        val repo = repository()

        repo.connect()
        assertEquals(WsConnectionStatus.CONNECTING, repo.status())

        factory.last.serverOpens()
        runCurrent()
        assertEquals(WsConnectionStatus.CONNECTED, repo.status())
        assertEquals(true, repo.isConnected())
    }

    @Test
    fun `connect while connected opens no second socket and stays connected`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()
        runCurrent()

        repo.connect()
        runCurrent()

        assertEquals(1, factory.sockets.size)
        assertEquals(WsConnectionStatus.CONNECTED, repo.status())
    }

    @Test
    fun `server frames become domain events`() = runTest {
        val repo = repository()
        val events = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.events.collect { events += it }
        }
        repo.connect()
        factory.last.serverOpens()

        factory.last.serverSends(
            """{"type":"typing","chatId":"c1","userId":"u2","data":{"userId":"u2","isTyping":true}}"""
        )
        runCurrent()

        assertEquals(1, events.size)
    }

    // #30: the server's first frame on every socket; ResponseData can't hold it.
    @Test
    fun `the welcome frame is recognised quietly and later frames still work`() = runTest {
        val errors = mutableListOf<String>()
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= 5) errors += message // 5 = Log.WARN
            }
        }
        Timber.plant(tree)
        try {
            val repo = repository()
            val events = mutableListOf<DomainEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repo.events.collect { events += it }
            }
            repo.connect()
            factory.last.serverOpens()

            factory.last.serverSends(
                """{"type":"response","userId":"u1",
                   "data":{"clientId":"cl1","instanceId":"s1","status":"connected"},
                   "timestamp":"2026-11-20T10:00:00Z"}"""
            )
            factory.last.serverSends(
                """{"type":"typing","chatId":"c1","userId":"u2","data":{"userId":"u2","isTyping":true}}"""
            )
            runCurrent()

            assertEquals(emptyList<String>(), errors)
            assertEquals(1, events.size)
            assertTrue(events.single() is DomainEvent.TypingUpdate)
        } finally {
            Timber.uproot(tree)
        }
    }

    @Test
    fun `an abnormal close reconnects with a new socket`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()

        factory.last.serverCloses(1001, "going away")
        runCurrent()
        assertEquals(WsConnectionStatus.DISCONNECTED, repo.status())
        waitOutReconnectDelay()

        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `a failure reconnects with a new socket`() = runTest {
        val repo = repository()
        repo.connect()

        factory.last.fails()
        waitOutReconnectDelay()

        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `close 1011 (server could not set up) reconnects`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()

        factory.last.serverCloses(1011, "try again")
        waitOutReconnectDelay()

        assertEquals(2, factory.sockets.size)
    }

    // Server #76: 4401 (TOKEN_REVOKED, TOKEN_EXPIRED, ACCOUNT_NOT_FOUND) and
    // 4403 (ACCOUNT_BANNED, ACCOUNT_INACTIVE) mean the session is over.
    @Test
    fun `close 4401 ends the session and does not reconnect`() = runTest {
        assertSessionEndsOn(4401, "TOKEN_REVOKED")
    }

    @Test
    fun `close 4403 ends the session and does not reconnect`() = runTest {
        assertSessionEndsOn(4403, "ACCOUNT_BANNED")
    }

    // Follow-up to #20/#23: the close reason says why, so Login can tell the user.
    @Test
    fun `close 4403 ACCOUNT_BANNED ends the session as banned`() = runTest {
        assertSessionEndsOn(4403, "ACCOUNT_BANNED", SessionEndReason.ACCOUNT_BANNED)
    }

    @Test
    fun `close 4403 ACCOUNT_INACTIVE ends the session as inactive`() = runTest {
        assertSessionEndsOn(4403, "ACCOUNT_INACTIVE", SessionEndReason.ACCOUNT_INACTIVE)
    }

    @Test
    fun `close 4401 TOKEN_EXPIRED ends the session as expired`() = runTest {
        assertSessionEndsOn(4401, "TOKEN_EXPIRED", SessionEndReason.EXPIRED)
    }

    // #22: the account was deleted; SessionManager clears its local data on this reason.
    @Test
    fun `close 4401 ACCOUNT_NOT_FOUND ends the session as account not found`() = runTest {
        assertSessionEndsOn(4401, "ACCOUNT_NOT_FOUND", SessionEndReason.ACCOUNT_NOT_FOUND)
    }

    @Test
    fun `close 4403 with an unknown reason ends the session as expired`() = runTest {
        assertSessionEndsOn(4403, "SOMETHING_NEW", SessionEndReason.EXPIRED)
    }

    private suspend fun TestScope.assertSessionEndsOn(
        code: Int,
        reason: String,
        expected: SessionEndReason? = null
    ) {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()

        factory.last.serverCloses(code, reason)
        runCurrent()
        waitOutReconnectDelay()

        assertEquals(1, factory.sockets.size)
        assertEquals(1, sessionsEnded)
        expected?.let { assertEquals(listOf(it), endReasons) }
        assertEquals(WsConnectionStatus.DISCONNECTED, repo.status())
    }

    // #27: a REST 401/403 ends the session in SessionManager; the open socket must close too.
    @Test
    fun `a session ended elsewhere closes the socket and does not reconnect`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()
        runCurrent()

        sessionEnded.emit(SessionEndReason.ACCOUNT_INACTIVE)
        runCurrent()
        factory.last.serverAcksClose()
        waitOutReconnectDelay()

        assertEquals(1000, factory.last.closeCode)
        assertEquals(1, factory.sockets.size)
        assertEquals(false, repo.isConnected())
        assertEquals(WsConnectionStatus.DISCONNECTED, repo.status())
    }

    @Test
    fun `an abnormal close does not end the session`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()

        factory.last.serverCloses(1001, "going away")
        waitOutReconnectDelay()

        assertEquals(0, sessionsEnded)
    }

    @Test
    fun `disconnect (logout) does not reconnect`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()
        runCurrent()

        repo.disconnect()
        factory.last.serverAcksClose()
        waitOutReconnectDelay()

        assertEquals(1, factory.sockets.size)
        assertEquals(1000, factory.last.closeCode)
        assertEquals(WsConnectionStatus.DISCONNECTED, repo.status())
        assertEquals(0, sessionsEnded)
    }

    @Test
    fun `disconnect cancels a reconnect that is already scheduled`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.fails()
        runCurrent() // reconnect scheduled

        repo.disconnect()
        waitOutReconnectDelay()

        assertEquals(1, factory.sockets.size)
    }

    @Test
    fun `a failure still queued when disconnect runs does not reconnect`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.fails() // Error queued; the collector has not run yet

        repo.disconnect()
        waitOutReconnectDelay()

        assertEquals(1, factory.sockets.size)
    }

    @Test
    fun `connect after a session-ending close reconnects normally again`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()
        factory.last.serverCloses(4401, "TOKEN_EXPIRED")
        runCurrent()

        repo.connect() // logged in again
        factory.last.fails()
        waitOutReconnectDelay()

        assertEquals(3, factory.sockets.size)
    }

    @Test
    fun `connect after disconnect opens a new socket`() = runTest {
        val repo = repository()
        repo.connect()
        factory.last.serverOpens()
        repo.disconnect()

        repo.connect()

        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `reconnect gives up after five attempts`() = runTest {
        val repo = repository()
        repo.connect()

        repeat(10) {
            factory.last.fails()
            waitOutReconnectDelay()
        }

        assertEquals(6, factory.sockets.size) // the first socket + 5 retries
    }

    // #25: the reconnect manager reports giving up on its own flow.
    @Test
    fun `status becomes failed once the reconnect retries run out`() = runTest {
        val repo = repository()
        repo.connect()

        repeat(6) {
            factory.last.fails()
            waitOutReconnectDelay()
        }

        assertEquals(WsConnectionStatus.FAILED, repo.status())
    }

    @Test
    fun `connect after the retries ran out connects again`() = runTest {
        val repo = repository()
        repo.connect()
        repeat(6) {
            factory.last.fails()
            waitOutReconnectDelay()
        }

        repo.connect()
        factory.last.serverOpens()
        runCurrent()

        assertEquals(WsConnectionStatus.CONNECTED, repo.status())
    }
}
