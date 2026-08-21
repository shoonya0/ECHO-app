package com.shoonya.echo.features.chat.data.model

import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatRole
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.Participant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ChatDto(
    @SerialName("_id") val id: String,
    val chatType: String = "direct",
    val name: String = "",
    val description: String = "",
    val avatar: String = "",
    val participants: Map<String, ParticipantDto> = emptyMap(),
    val ownerId: String? = null,
    val adminIds: List<String> = emptyList(),
    val lastMessageId: String? = null,
    val stats: ChatStatsDto? = null,
    val settings: ChatSettingsDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ParticipantDto(
    val requestStatus: String = "accepted",
    val onlineStatus: String = "offline",
    val role: String = "member",
    val userInfo: ContactUserInfoDto? = null,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val lastSeen: String? = null,
    val joinedAt: String? = null,
)

@Serializable
data class ContactUserInfoDto(
    val displayName: String = "",
    val username: String = "",
    val userId: String = "",
    val avatar: String = "",
)

@Serializable
data class ChatStatsDto(
    val participantCount: Int = 0,
    val messageCount: Long = 0,
    val unreadCount: Map<String, Int> = emptyMap(),
)

@Serializable
data class ChatSettingsDto(
    val isPrivate: Boolean = false,
    val allowInvites: Boolean = true,
    val allowFileSharing: Boolean = true,
    val messageRetention: Int = 0,
    val maxParticipants: Int = 50,
)

fun ChatDto.toChat(currentUserId: String): Chat {
    val participants = this.participants.entries.map { (userId, dto) ->
        Participant(
            userId = userId,
            username = dto.userInfo?.username ?: "",
            displayName = dto.userInfo?.displayName ?: dto.userInfo?.username ?: "",
            avatar = dto.userInfo?.avatar ?: "",
            role = when (dto.role) {
                "owner" -> ChatRole.OWNER
                "admin" -> ChatRole.ADMIN
                else -> ChatRole.MEMBER
            },
            isOnline = dto.onlineStatus == "online",
            isBlocked = dto.isBlocked,
            lastSeen = dto.lastSeen?.let { Instant.parse(it) },
        )
    }

    val otherParticipant = participants.firstOrNull { it.userId != currentUserId }

    return Chat(
        id = id,
        type = when (chatType.lowercase()) {
            "group" -> ChatType.GROUP
            "channel" -> ChatType.CHANNEL
            else -> ChatType.DIRECT
        },
        name = name.ifBlank {
            otherParticipant?.displayName ?: "Chat"
        },
        description = description,
        avatar = avatar.ifBlank { otherParticipant?.avatar ?: "" },
        participants = participants,
        ownerId = ownerId,
        participantCount = stats?.participantCount ?: participants.size,
        messageCount = stats?.messageCount ?: 0L,
        unreadCount = stats?.unreadCount?.get(currentUserId) ?: 0,
        lastMessage = null,
        settings = ChatSettings(
            isPrivate = settings?.isPrivate ?: false,
            allowInvites = settings?.allowInvites ?: true,
            allowFileSharing = settings?.allowFileSharing ?: true,
        ),
        createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.EPOCH,
        updatedAt = updatedAt?.let { Instant.parse(it) },
    )
}