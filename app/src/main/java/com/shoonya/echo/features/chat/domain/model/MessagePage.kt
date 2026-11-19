package com.shoonya.echo.features.chat.domain.model

/**
 * One page of a chat's history, oldest first. To load older messages, pass [nextCursor]
 * as `before`; it is null on the last page, where [hasMore] is false.
 */
data class MessagePage(
    val messages: List<Message>,
    val hasMore: Boolean,
    val nextCursor: String?,
    /** Member user ID to the highest `seq` they have read (#42). */
    val readSeqs: Map<String, Long> = emptyMap()
)
