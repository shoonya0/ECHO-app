package com.shoonya.echo.chat

import com.shoonya.echo.core.data.remote.NetworkModule
import com.shoonya.echo.fakes.FakeWebSocketFactory
import com.shoonya.echo.features.chat.data.remote.EchoWebSocketClient
import com.shoonya.echo.features.chat.data.remote.ReconnectManager
import com.shoonya.echo.features.chat.data.repository.WebSocketRepositoryImpl
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drives the real client, reconnect manager and repository against a fake socket factory;
 * reconnect delays run on the test scheduler's virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketRepositoryImplTest {

    private val factory = FakeWebSocketFactory()
    private val client = EchoWebSocketClient(factory, { "tok" }, "ws://test/ws")
    private val reconnectManager = ReconnectManager(client)

    private fun TestScope.repository() = WebSocketRepositoryImpl(
        client,
        reconnectManager,
        NetworkModule.provideJson(),
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
}
