package com.shoonya.echo.core.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatar: String,
    val statusMessage: String,
    val bio: String,
    val presence: Presence,
    val isActive: Boolean,
    val isVerified: Boolean,
    val isBanned: Boolean,
)

data class Presence(
    val status: PresenceStatus,
    val isOnline: Boolean,
    val lastSeen: Instant?,
)

enum class PresenceStatus {
    ONLINE,
    AWAY,
    DND,
    INVISIBLE,
    OFFLINE,
}