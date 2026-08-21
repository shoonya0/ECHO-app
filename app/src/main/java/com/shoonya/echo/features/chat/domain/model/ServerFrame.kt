package com.shoonya.echo.features.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServerFrame {
    val type: String
    val timestamp: String?
    val requestId: String?

    @Serializable
    @SerialName("message")
    data class ChatMessage(
        val chatId: String,
        val userId: String,
        val username: String,
        val data: MessageData,
        override val type: String = "message",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("typing")
    data class Typing(
        val chatId: String,
        val userId: String,
        val data: TypingData,
        override val type: String = "typing",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("presence")
    data class Presence(
        val userId: String,
        val data: PresenceData,
        val chatId: String? = null,
        override val type: String = "presence",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("reaction")
    data class Reaction(
        val chatId: String,
        val userId: String,
        val data: ReactionData,
        override val type: String = "reaction",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("delivery")
    data class Delivery(
        val chatId: String,
        val userId: String,
        val data: DeliveryData,
        override val type: String = "delivery",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("join")
    data class Join(
        val chatId: String,
        val userId: String,
        val data: JoinLeaveData,
        override val type: String = "join",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("leave")
    data class Leave(
        val chatId: String,
        val userId: String,
        val data: JoinLeaveData,
        override val type: String = "leave",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("response")
    data class Response(
        val data: ResponseData,
        override val type: String = "response",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame

    @Serializable
    @SerialName("error")
    data class Error(
        val data: ErrorData,
        override val type: String = "error",
        override val timestamp: String? = null,
        override val requestId: String? = null,
    ) : ServerFrame
}

// Sub-models matching the backend protocol from _docs/05_WEBSOCKET_CLIENT.md

@Serializable
data class MessageData(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val messageType: String = "text",
    val attachments: List<AttachmentDto> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class TypingData(
    val userId: String,
    val username: String? = null,
    val isTyping: Boolean,
    val timestamp: String? = null,
)

@Serializable
data class PresenceData(
    val userId: String,
    val status: String,
    val lastSeen: String? = null,
)

@Serializable
data class ReactionData(
    val messageId: String,
    val userId: String,
    val username: String? = null,
    val emoji: String,
    val action: String,
    val timestamp: String? = null,
)

@Serializable
data class DeliveryData(
    val messageIds: List<String>,
    val userId: String,
    val status: String,
)

@Serializable
data class JoinLeaveData(
    val userId: String,
    val username: String? = null,
    val action: String,
    val timestamp: String? = null,
)

@Serializable
data class ResponseData(
    val success: Boolean,
    val messageId: String? = null,
    val timestamp: String? = null,
    val chatId: String? = null,
    val messageType: String? = null,
)

@Serializable
data class ErrorData(
    val code: String,
    val message: String,
    val details: String? = null,
)

// Reuse the existing DTO shape for attachments within ServerFrame.MessageData
// (avoids cross-feature import — this is a WS-level DTO, same shape as data/model/AttachmentDto)

@Serializable
data class AttachmentDto(
    val id: String,
    val originalName: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)