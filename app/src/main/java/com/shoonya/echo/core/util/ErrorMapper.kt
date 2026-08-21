package com.shoonya.echo.core.util

import com.shoonya.echo.core.data.remote.ApiException

object ErrorMapper {
    fun mapBackendError(code: String, defaultMessage: String): String = when (code) {
        "INVALID_REQUEST" -> "Invalid request. Please try again."
        "PERMISSION_DENIED" -> "You don't have permission to do that."
        "INVALID_CHAT_ID" -> "Chat not found."
        "INVALID_MESSAGE_ID" -> "Message not found."
        "INVALID_CONTENT" -> "Message contains invalid content."
        "MESSAGE_FAILED" -> "Failed to send message. Tap to retry."
        "REACTION_FAILED" -> "Failed to add reaction."
        "READ_FAILED" -> "Failed to mark as read."
        "PARSE_ERROR" -> "Server couldn't process the request."
        "RATE_LIMIT_EXCEEDED" -> "Too many requests. Slow down."
        "CHANNEL_FULL" -> "Server is busy. Please wait."
        "TOKEN_VERIFICATION_FAILED" -> "Session expired. Please log in again."
        "USER_NOT_AUTHENTICATED" -> "Please log in to continue."
        "NOT_IMPLEMENTED" -> "This feature is coming soon."
        "UNKNOWN_REQUEST" -> "Unknown request type."
        else -> defaultMessage
    }

    fun Throwable.toUserMessage(): String = when {
        this is java.net.ConnectException -> "No internet connection. Check your network."
        this is java.net.SocketTimeoutException -> "Server is taking too long. Try again."
        this is java.net.UnknownHostException -> "Cannot reach server. Check your connection."
        // ApiException carries the server's parsed error message — check it first
        this is ApiException -> this.message ?: "Something went wrong."
        this is retrofit2.HttpException -> when (code()) {
            // 401 on unauthenticated requests (login/signup) = wrong credentials
            // Authenticated 401s are handled by AuthInterceptor → SessionManager
            401 -> "Invalid email or password. Please try again."
            403 -> "You don't have permission to do that."
            404 -> "The requested resource was not found."
            429 -> "Too many requests. Please try again later."
            500, 502, 503, 504 -> "Server is having issues. Please try again later."
            else -> "Request failed. Please try again."
        }
        else -> this.message ?: "Something went wrong. Please try again."
    }
}