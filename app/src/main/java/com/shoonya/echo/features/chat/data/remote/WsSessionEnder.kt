package com.shoonya.echo.features.chat.data.remote

import com.shoonya.echo.core.domain.model.SessionEndReason

/**
 * Ends the user's session when the server closes the WebSocket with 4401 or 4403, saying
 * why ([reason] comes from the close frame's reason string).
 * Production binds it to `SessionManager.expire()` (WebSocketModule); unit tests pass a lambda.
 */
fun interface WsSessionEnder {
    fun endSession(reason: SessionEndReason)
}
