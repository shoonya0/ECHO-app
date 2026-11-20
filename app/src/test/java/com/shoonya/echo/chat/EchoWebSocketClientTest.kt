package com.shoonya.echo.chat

import com.shoonya.echo.fakes.FakeWebSocketFactory
import com.shoonya.echo.features.chat.data.remote.EchoWebSocketClient
import com.shoonya.echo.features.chat.data.remote.WebSocketEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EchoWebSocketClientTest {

    private val factory = FakeWebSocketFactory()
    private var token: String? = "tok"
    private val client = EchoWebSocketClient(factory, { token }, "ws://test/echo/v1/ws/chat")

    @Test
    fun `connect opens one socket to the chat endpoint with the bearer token`() {
        client.connect()

        assertEquals(1, factory.sockets.size)
        val request = factory.last.request()
        assertEquals("http://test/echo/v1/ws/chat", request.url.toString())
        assertEquals("Bearer tok", request.header("Authorization"))
    }

    // #26: with no token every attempt fails the same way, so report a stop (no retry),
    // not an Error that the repository answers with a reconnect.
    @Test
    fun `connect without a token opens nothing and reports a stop`() = runTest {
        token = null
        val events = collect(this)

        client.connect()

        assertTrue(factory.sockets.isEmpty())
        assertEquals(listOf(WebSocketEvent.ReconnectFailed), events)
    }

    // #26: a rejected upgrade means the same as the server's session-ending close codes.
    @Test
    fun `handshake 401 is reported like close code 4401 with the server's reason`() = runTest {
        val events = collect(this)
        client.connect()

        factory.last.serverRejectsUpgrade(
            401,
            """{"error":"Token has been revoked","code":"TOKEN_REVOKED"}"""
        )

        assertEquals(listOf(WebSocketEvent.Disconnected(4401, "TOKEN_REVOKED")), events)
        assertFalse(client.isConnected())
    }

    @Test
    fun `handshake 403 is reported like close code 4403 with the server's reason`() = runTest {
        val events = collect(this)
        client.connect()

        factory.last.serverRejectsUpgrade(
            403,
            """{"error":"Account banned","code":"ACCOUNT_BANNED"}"""
        )

        assertEquals(listOf(WebSocketEvent.Disconnected(4403, "ACCOUNT_BANNED")), events)
    }

    @Test
    fun `handshake 401 without a readable body still ends the session`() = runTest {
        val events = collect(this)
        client.connect()

        factory.last.serverRejectsUpgrade(401, "not json")

        assertEquals(listOf(WebSocketEvent.Disconnected(4401, "HTTP 401")), events)
    }

    @Test
    fun `other handshake errors stay errors`() = runTest {
        val events = collect(this)
        client.connect()

        // 503 AUTH_UNAVAILABLE and 429 must not log out: they reconnect with backoff.
        factory.last.serverRejectsUpgrade(503, """{"code":"AUTH_UNAVAILABLE"}""")

        assertEquals(1, events.size)
        assertTrue(events.single() is WebSocketEvent.Error)
    }

    @Test
    fun `open, message and server close are reported in order`() = runTest {
        val events = collect(this)
        client.connect()

        factory.last.serverOpens()
        assertTrue(client.isConnected())
        factory.last.serverSends("""{"type":"response"}""")
        factory.last.serverCloses(1001, "going away")

        assertFalse(client.isConnected())
        assertEquals(
            listOf(
                WebSocketEvent.Connected,
                WebSocketEvent.Message("""{"type":"response"}"""),
                WebSocketEvent.Disconnected(1001, "going away")
            ),
            events
        )
    }

    @Test
    fun `send goes to the open socket`() {
        client.connect()
        factory.last.serverOpens()

        assertTrue(client.send("hello"))
        assertEquals(listOf("hello"), factory.last.sent)
    }

    @Test
    fun `send without a socket fails`() {
        assertFalse(client.send("hello"))
    }

    @Test
    fun `disconnect closes the socket normally`() {
        client.connect()
        factory.last.serverOpens()

        client.disconnect()

        assertEquals(1000, factory.last.closeCode)
        assertFalse(client.isConnected())
        assertFalse(client.send("hello"))
    }

    // #1: ChatListViewModel and ChatDetailViewModel both call connect().
    @Test
    fun `connect while connected keeps the one open socket`() {
        client.connect()
        factory.last.serverOpens()

        client.connect()

        assertEquals(1, factory.sockets.size)
        assertEquals(null, factory.last.closeCode)
    }

    @Test
    fun `connect while connecting keeps the one pending socket`() {
        client.connect()

        client.connect()

        assertEquals(1, factory.sockets.size)
    }

    @Test
    fun `connect after the server closed opens a new socket`() {
        client.connect()
        factory.sockets[0].serverOpens()
        factory.sockets[0].serverCloses(1001, "going away")

        client.connect()

        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `connect after a failure opens a new socket`() {
        client.connect()
        factory.sockets[0].fails()

        client.connect()

        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `a replaced socket's late events are ignored`() = runTest {
        client.connect()
        val old = factory.sockets[0]
        old.serverOpens()
        client.disconnect()
        client.connect()
        val events = collect(this)

        old.serverSends("""{"type":"response"}""")
        old.serverAcksClose()

        assertEquals(emptyList<WebSocketEvent>(), events)
        assertFalse(client.isConnected())
        factory.sockets[1].serverOpens()
        assertTrue(client.isConnected())
    }

    private fun collect(scope: kotlinx.coroutines.test.TestScope): List<WebSocketEvent> {
        val events = mutableListOf<WebSocketEvent>()
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
            client.events.collect { events += it }
        }
        return events
    }
}
