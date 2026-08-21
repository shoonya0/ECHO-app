package com.shoonya.echo.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `maps all known backend error codes`() {
        val codes = listOf(
            "INVALID_REQUEST" to "Invalid request. Please try again.",
            "PERMISSION_DENIED" to "You don't have permission to do that.",
            "INVALID_CHAT_ID" to "Chat not found.",
            "INVALID_MESSAGE_ID" to "Message not found.",
            "INVALID_CONTENT" to "Message contains invalid content.",
            "MESSAGE_FAILED" to "Failed to send message. Tap to retry.",
            "REACTION_FAILED" to "Failed to add reaction.",
            "READ_FAILED" to "Failed to mark as read.",
            "PARSE_ERROR" to "Server couldn't process the request.",
            "RATE_LIMIT_EXCEEDED" to "Too many requests. Slow down.",
            "CHANNEL_FULL" to "Server is busy. Please wait.",
            "TOKEN_VERIFICATION_FAILED" to "Session expired. Please log in again.",
            "USER_NOT_AUTHENTICATED" to "Please log in to continue.",
            "NOT_IMPLEMENTED" to "This feature is coming soon.",
            "UNKNOWN_REQUEST" to "Unknown request type.",
        )
        for ((code, expected) in codes) {
            assertEquals(expected, ErrorMapper.mapBackendError(code, "fallback"))
        }
    }

    @Test
    fun `unknown code returns default message`() {
        val result = ErrorMapper.mapBackendError("SOME_NEW_CODE", "Custom fallback")
        assertEquals("Custom fallback", result)
    }
}