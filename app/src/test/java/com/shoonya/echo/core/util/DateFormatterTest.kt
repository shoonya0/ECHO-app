package com.shoonya.echo.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DateFormatterTest {

    @Test
    fun `ISO 8601 string parses to Instant`() {
        val instant = "2026-08-08T16:00:00Z".toInstant()
        assertEquals(Instant.parse("2026-08-08T16:00:00Z"), instant)
    }

    @Test
    fun `just now for same instant`() {
        val now = Instant.parse("2026-08-08T16:00:00Z")
        val result = now.toRelativeTime(now)
        assertEquals("just now", result)
    }

    @Test
    fun `minutes ago for 5 minute old time`() {
        val past = Instant.parse("2026-08-08T15:55:00Z")
        val now = Instant.parse("2026-08-08T16:00:00Z")
        val result = past.toRelativeTime(now)
        assertEquals("5m ago", result)
    }

    @Test
    fun `hours ago for 3 hour old time`() {
        val past = Instant.parse("2026-08-08T13:00:00Z")
        val now = Instant.parse("2026-08-08T16:00:00Z")
        val result = past.toRelativeTime(now)
        assertEquals("3h ago", result)
    }

    @Test
    fun `days ago for 2 day old time`() {
        val past = Instant.parse("2026-08-06T16:00:00Z")
        val now = Instant.parse("2026-08-08T16:00:00Z")
        val result = past.toRelativeTime(now)
        assertEquals("2d ago", result)
    }

    @Test
    fun `date format for old time matches M_d_yyyy pattern`() {
        val past = Instant.parse("2026-01-15T12:00:00Z")
        val now = Instant.parse("2026-08-08T16:00:00Z")
        val result = past.toRelativeTime(now)
        // Should show date format, timezone-safe (noon UTC guarantees same day everywhere)
        assertTrue(result.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")))
    }
}