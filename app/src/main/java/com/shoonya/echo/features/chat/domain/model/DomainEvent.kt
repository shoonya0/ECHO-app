package com.shoonya.echo.features.chat.domain.model

import java.time.Instant

sealed interface DomainEvent {
    data class NewMessage(val chatId: String, val message: Message) : DomainEvent
    data class TypingUpdate(val chatId: String, val userId: String, val isTyping: Boolean) : DomainEvent
    data class PresenceUpdate(val userId: String, val status: PresenceStatus) : DomainEvent
    data class ReactionUpdate(val chatId: String, val messageId: String, val emoji: String, val action: String) : DomainEvent
    data class ReadReceipt(val chatId: String, val messageIds: List<String>, val userId: String) : DomainEvent
    data class UserJoined(val chatId: String, val userId: String) : DomainEvent
    data class UserLeft(val chatId: String, val userId: String) : DomainEvent
    data class ResponseAck(val requestId: String, val messageId: String?) : DomainEvent
    data class BackendError(val code: String, val message: String) : DomainEvent
}

enum class PresenceStatus { ONLINE, OFFLINE, AWAY }

fun MessageData.toMessage(currentUserId: String? = null): Message = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    senderName = "", // MessageData doesn't carry sender metadata; populated from the enclosing ChatMessage frame
    senderDisplayName = "",
    senderAvatar = "",
    content = content,
    type = when (messageType.lowercase()) {
        "image" -> MessageType.IMAGE
        "file" -> MessageType.FILE
        "audio" -> MessageType.AUDIO
        "video" -> MessageType.VIDEO
        else -> MessageType.TEXT
    },
    attachments = attachments.map { att ->
        Attachment(
            id = att.id,
            name = att.originalName,
            url = att.url,
            mimeType = att.mimeType,
            size = att.size,
        )
    },
    reactions = emptyMap(),
    isEdited = false,
    readByUserIds = emptySet(),
    createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.EPOCH,
)

fun ServerFrame.toDomainEvent(): DomainEvent = when (this) {
    is ServerFrame.ChatMessage -> {
        val message = data.toMessage().copy(
            senderName = username,
            senderDisplayName = username,
        )
        DomainEvent.NewMessage(chatId, message)
    }
    is ServerFrame.Typing -> DomainEvent.TypingUpdate(chatId, userId, data.isTyping)
    is ServerFrame.Presence -> DomainEvent.PresenceUpdate(
        userId,
        try { PresenceStatus.valueOf(data.status.uppercase()) } catch (_: Exception) { PresenceStatus.OFFLINE }
    )
    is ServerFrame.Reaction -> DomainEvent.ReactionUpdate(chatId, data.messageId, data.emoji, data.action)
    is ServerFrame.Delivery -> DomainEvent.ReadReceipt(chatId, data.messageIds, data.userId)
    is ServerFrame.Join -> DomainEvent.UserJoined(chatId, userId)
    is ServerFrame.Leave -> DomainEvent.UserLeft(chatId, userId)
    is ServerFrame.Response -> DomainEvent.ResponseAck(requestId ?: "", data.messageId)
    is ServerFrame.Error -> DomainEvent.BackendError(data.code, data.message)
}