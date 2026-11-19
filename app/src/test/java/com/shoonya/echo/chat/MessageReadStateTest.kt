package com.shoonya.echo.chat

import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.chat.domain.model.withReadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MessageReadStateTest {

    // Sent by the current user, seq 5, nobody has read it yet.
    private val message = TestData.message2.copy(seq = 5, readByUserIds = emptySet())

    @Test
    fun `members whose read seq reaches the message's seq have read it`() {
        val result = message.withReadState(
            mapOf("reached" to 5L, "past" to 9L, "behind" to 4L)
        )

        assertEquals(setOf("reached", "past"), result.readByUserIds)
    }

    @Test
    fun `the sender is never counted as a reader`() {
        val result = message.withReadState(mapOf(message.senderId to 9L))

        assertEquals(emptySet<String>(), result.readByUserIds)
    }

    @Test
    fun `a message without a seq is left as it is`() {
        val unsaved = message.copy(seq = null)

        assertSame(unsaved, unsaved.withReadState(mapOf("someone" to 99L)))
    }
}
