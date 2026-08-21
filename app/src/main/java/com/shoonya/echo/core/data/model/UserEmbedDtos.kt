package com.shoonya.echo.core.data.model

import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileEmbedDto(
    val displayName: String = "",
    val avatar: String = "",
    val statusMessage: String = "",
    val bio: String = "",
)

@Serializable
data class PresenceDto(
    val status: String = "offline",
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val lastActivity: String? = null,
)

@Serializable
data class AccountStatusDto(
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
)

fun PresenceDto.toDomain(): Presence = Presence(
    status = try {
        PresenceStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
        PresenceStatus.OFFLINE
    },
    isOnline = isOnline,
    lastSeen = lastSeen?.let { Instant.parse(it) },
)