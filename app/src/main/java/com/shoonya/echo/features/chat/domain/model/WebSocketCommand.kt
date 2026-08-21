package com.shoonya.echo.features.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

private fun generateRequestId(): String = UUID.randomUUID().toString().take(8)

@Serializable
sealed interface WebSocketCommand {
    val requestId: String

    @Serializable
    @SerialName("send_message")
    data class SendMessage(
        val chatId: String,
        val content: String,
        val messageType: String = "text",
        val attachments: List<AttachmentRef> = emptyList(),
        val mentions: List<String> = emptyList(),
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("join_chat")
    data class JoinChat(
        val chatId: String,
        val senderId: String,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("leave_chat")
    data class LeaveChat(
        val chatId: String,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("set_typing")
    data class SetTyping(
        val chatId: String,
        val metadata: TypingMetadata,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("mark_read")
    data class MarkRead(
        val chatId: String,
        val metadata: MarkReadMetadata,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("add_reaction")
    data class AddReaction(
        val chatId: String,
        val metadata: ReactionMetadata,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand

    @Serializable
    @SerialName("remove_reaction")
    data class RemoveReaction(
        val chatId: String,
        val metadata: ReactionMetadata,
        override val requestId: String = generateRequestId(),
    ) : WebSocketCommand
}

@Serializable
data class TypingMetadata(val isTyping: Boolean)

@Serializable
data class MarkReadMetadata(val messageIds: List<String>)

@Serializable
data class ReactionMetadata(val messageId: String, val emoji: String)

@Serializable
data class AttachmentRef(
    val id: String,
    val name: String,
    val url: String,
    val mimeType: String,
    val size: Long,
)