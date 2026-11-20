package com.shoonya.echo.features.chat.data.remote

/**
 * Ends the user's session when the server closes the WebSocket with 4401 or 4403.
 * Production binds it to `SessionManager.expire()` (WebSocketModule); unit tests pass a lambda.
 */
fun interface WsSessionEnder {
    fun endSession()
}
