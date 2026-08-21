package com.shoonya.echo.features.settings.data.model

import com.shoonya.echo.core.data.model.toDomain
import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.features.settings.domain.model.UserProfile

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    email = email,
    phone = phone,
    displayName = profile?.displayName ?: username,
    avatar = profile?.avatar ?: "",
    statusMessage = profile?.statusMessage ?: "",
    bio = profile?.bio ?: "",
    presence = presence?.toDomain() ?: Presence(PresenceStatus.OFFLINE, false, null),
    isActive = accountStatus?.isActive ?: true,
)