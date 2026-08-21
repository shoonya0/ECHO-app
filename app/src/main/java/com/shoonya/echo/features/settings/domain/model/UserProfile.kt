package com.shoonya.echo.features.settings.domain.model

import com.shoonya.echo.core.domain.model.Presence

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val phone: String,
    val displayName: String,
    val avatar: String,
    val statusMessage: String,
    val bio: String,
    val presence: Presence,
    val isActive: Boolean,
)