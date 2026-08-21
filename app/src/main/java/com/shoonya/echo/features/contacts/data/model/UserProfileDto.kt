package com.shoonya.echo.features.contacts.data.model

import com.shoonya.echo.core.data.model.AccountStatusDto
import com.shoonya.echo.core.data.model.PresenceDto
import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import com.shoonya.echo.core.data.model.toDomain
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.core.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    @SerialName("_id") val id: String,
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val presence: PresenceDto? = null,
    val profile: UserProfileEmbedDto? = null,
    val accountStatus: AccountStatusDto? = null,
)

fun UserProfileDto.toDomain(): User {
    val profile = this.profile
    val presence = this.presence
    val accountStatus = this.accountStatus
    return User(
        id = id,
        email = email,
        username = username,
        displayName = profile?.displayName ?: username,
        avatar = profile?.avatar ?: "",
        statusMessage = profile?.statusMessage ?: "",
        bio = profile?.bio ?: "",
        presence = presence?.toDomain()
            ?: com.shoonya.echo.core.domain.model.Presence(PresenceStatus.OFFLINE, false, null),
        isActive = accountStatus?.isActive ?: true,
        isVerified = accountStatus?.isVerified ?: false,
        isBanned = accountStatus?.isBanned ?: false,
    )
}