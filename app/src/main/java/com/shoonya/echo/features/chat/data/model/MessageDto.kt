package com.shoonya.echo.features.chat.data.model

import com.shoonya.echo.features.chat.domain.model.Attachment
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.Reaction
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageListResponse(
    val chat: String,
    val messages: List<MessageDto>,
    /**
     * Member user ID to the highest seq they have read (#42); the app no longer reads each
     * message's readBy. Null only from a server without #42 (it always sends a map).
     */
    val readSeqs: Map<String, Long>? = null,
    val pagination: MessagePagination
)

/**
 * Cursor paging: send [nextCursor] as `before` for the next (older) page while [hasMore].
 * [hasMore] has no default, so a reply without it fails to parse instead of passing as the
 * last page: the contract drift behind #7 would have been silent otherwise.
 */
@Serializable
data class MessagePagination(
    val hasMore: Boolean,
    val limit: Int = 0,
    val count: Int = 0,
    val nextCursor: String? = null
)

@Serializable
data class MessageDto(
    @SerialName("_id") val id: String? = null,
    val chatId: String,
    val senderId: String,
    val sender: MessageSenderDto? = null,
    val content: String,
    val messageType: String = "text",
    val attachments: List<AttachmentDto> = emptyList(),
    val reactions: Map<String, ReactionDto> = emptyMap(),
    val status: MessageStatusDto? = null,
    val seq: Long? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class MessageSenderDto(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatar: String
)

@Serializable
data class AttachmentDto(
    val id: String,
    val originalName: String,
    val url: String,
    val mimeType: String,
    val size: Long
)

@Serializable
data class ReactionDto(
    val count: Int = 0,
    val users: List<String> = emptyList(),
    val details: Map<String, String> = emptyMap()
)

@Serializable
data class MessageStatusDto(
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false
)

fun MessageDto.toMessage(currentUserId: String): Message = Message(
    id = id ?: "",
    chatId = chatId,
    senderId = senderId,
    senderName = sender?.username ?: "",
    senderDisplayName = sender?.displayName ?: sender?.username ?: "",
    senderAvatar = sender?.avatar ?: "",
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
            size = att.size
        )
    },
    reactions = reactions.mapValues { (_, dto) ->
        Reaction(
            count = dto.count,
            userNames = dto.details.values.toList(),
            hasReacted = dto.users.contains(currentUserId)
        )
    },
    isEdited = status?.isEdited ?: false,
    readByUserIds = emptySet(), // set from the page's readSeqs (Message.withReadState)
    createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.EPOCH,
    seq = seq
)
