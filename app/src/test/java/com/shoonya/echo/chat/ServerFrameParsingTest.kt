package com.shoonya.echo.chat

import com.shoonya.echo.core.data.remote.NetworkModule
import com.shoonya.echo.fakes.FakeWebSocketRepository
import com.shoonya.echo.features.chat.domain.model.DomainEvent
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.toDomainEvent
import com.shoonya.echo.features.chat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes frames in the shape the server sends them (shoonya0/ECHO `_docs/websocket-events.md`,
 * `broadcastReadReceipts` in `chat.service.go`) with the app's real Json configuration.
 */
class ServerFrameParsingTest {

    private val json = NetworkModule.provideJson()

    private fun decode(raw: String): DomainEvent =
        json.decodeFromString(ServerFrame.serializer(), raw).toDomainEvent()

    @Test
    fun `delivery frame names the reader at the top level only`() {
        val event = decode(
            """
            {"type":"delivery","chatId":"c1","userId":"reader",
             "data":{"messageIds":["m1"],"status":"read",
                     "timestamp":"2026-11-04T12:00:00Z","readUpToSeq":42,"instanceID":"s1"},
             "timestamp":"2026-11-04T12:00:00Z"}
            """.trimIndent()
        )

        assertTrue(event is DomainEvent.ReadReceipt)
        event as DomainEvent.ReadReceipt
        assertEquals("c1", event.chatId)
        assertEquals("reader", event.userId)
        assertEquals(42L, event.readUpToSeq)
    }

    @Test
    fun `message frame carries the message's seq`() {
        val event = decode(
            """
            {"type":"message","chatId":"c1","userId":"u2","username":"bob",
             "data":{"id":"m7","chatId":"c1","senderId":"u2","content":"hi","seq":7,
                     "createdAt":"2026-11-04T12:00:00Z"}}
            """.trimIndent()
        )

        assertTrue(event is DomainEvent.NewMessage)
        assertEquals(7L, (event as DomainEvent.NewMessage).message.seq)
    }

    @Test
    fun `send_message reply gives the sent message its seq`() = runTest {
        // Real shape (websocket.controller.go): the seq sits in data.data, not in data.
        val frame = json.decodeFromString(
            ServerFrame.serializer(),
            """
            {"type":"response","requestId":"r1",
             "data":{"success":true,"messageId":"m8","requestId":"r1",
                     "data":{"messageId":"m8","seq":8,"timestamp":"2026-11-04T12:00:00Z",
                             "chatId":"c1","messageType":"text"}}}
            """.trimIndent()
        )
        val ws = FakeWebSocketRepository().apply { setSendResult(Result.success(frame)) }

        val sent = SendMessageUseCase(ws)("c1", "hi", "me").getOrThrow()

        assertEquals("m8", sent.id)
        assertEquals(8L, sent.seq)
    }

    @Test
    fun `delivery frame without readUpToSeq fails instead of passing as nothing read`() {
        assertThrows(SerializationException::class.java) {
            decode(
                """
                {"type":"delivery","chatId":"c1","userId":"reader",
                 "data":{"messageIds":["m1"],"status":"read"}}
                """.trimIndent()
            )
        }
    }
}
