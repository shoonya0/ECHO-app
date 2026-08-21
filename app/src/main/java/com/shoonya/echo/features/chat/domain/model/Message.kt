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
    val readByUserIds: Set<String>,
    val createdAt: Instant,
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    AUDIO,
    VIDEO,
}

data class Attachment(
    val id: String,
    val name: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)

data class Reaction(
    val count: Int,
    val userNames: List<String>,
    val hasReacted: Boolean,
)