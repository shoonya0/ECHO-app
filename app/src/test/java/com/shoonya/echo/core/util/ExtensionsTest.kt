package com.shoonya.echo.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `valid email returns true`() {
        assertTrue("user@example.com".isValidEmail())
        assertTrue("a.b@c.co".isValidEmail())
        assertTrue("user+tag@domain.org".isValidEmail())
    }

    @Test
    fun `invalid email returns false`() {
        assertFalse("notanemail".isValidEmail())
        assertFalse("@missinglocal.com".isValidEmail())
        assertFalse("missingdomain@".isValidEmail())
        assertFalse("".isValidEmail())
        assertFalse("spaces in@email.com".isValidEmail())
    }

    @Test
    fun `truncate short string returns original`() {
        assertEquals("hello", "hello".truncate(10))
    }

    @Test
    fun `truncate long string appends ellipsis`() {
        assertEquals("abc\u2026", "abcdefgh".truncate(3))
    }

    @Test
    fun `truncate exact length returns original`() {
        assertEquals("exact", "exact".truncate(5))
    }
}