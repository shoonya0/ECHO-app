package com.shoonya.echo.features.chat.domain.model

import java.time.Instant

data class Chat(
    val id: String,
    val type: ChatType,
    val name: String,
    val description: String,
    val avatar: String,
    val participants: List<Participant>,
    val ownerId: String?,
    val participantCount: Int,
    val messageCount: Long,
    val unreadCount: Int,
    val lastMessage: Message?,
    val settings: ChatSettings,
    val createdAt: Instant,
    val updatedAt: Instant?,
)

enum class ChatType {
    DIRECT,
    GROUP,
    CHANNEL,
}

data class Participant(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatar: String,
    val role: ChatRole,
    val isOnline: Boolean,
    val isBlocked: Boolean,
    val lastSeen: Instant?,
)

enum class ChatRole {
    OWNER,
    ADMIN,
    MEMBER,
}

data class ChatSettings(
    val isPrivate: Boolean = false,
    val allowInvites: Boolean = true,
    val allowFileSharing: Boolean = true,
)