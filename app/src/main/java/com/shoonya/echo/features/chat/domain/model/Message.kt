package com.shoonya.echo.features.chat.domain.model

import java.time.Instant

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderDisplayName: String,
    val senderAvatar: String,
    val content: String,
    val type: MessageType,
    val attachments: List<Attachment>,
    val reactions: Map<String, Reaction>,
    val isEdited: Boolean,
    /** Members other than the sender who have read it; derive with [withReadState]. */
    val readByUserIds: Set<String>,
    val createdAt: Instant,
    /** The chat's number for this message (1, 2, 3, ...); null until the server has saved it. */
    val seq: Long? = null
)

/**
 * Sets [Message.readByUserIds] from the chat's read state (#42): [readSeqs] maps a member's user
 * ID to the highest `seq` they have read, so a message is read by everyone whose value reaches
 * its [Message.seq]. The sender doesn't count. A message without a seq is left as it is.
 */
fun Message.withReadState(readSeqs: Map<String, Long>): Message {
    val ownSeq = seq ?: return this
    val readers = readSeqs.filter { (userId, upTo) -> userId != senderId && upTo >= ownSeq }.keys
    return if (readers == readByUserIds) this else copy(readByUserIds = readers)
}

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    AUDIO,
    VIDEO
}

data class Attachment(
    val id: String,
    val name: String,
    val url: String,
    val mimeType: String,
    val size: Long
)

data class Reaction(val count: Int, val userNames: List<String>, val hasReacted: Boolean)
