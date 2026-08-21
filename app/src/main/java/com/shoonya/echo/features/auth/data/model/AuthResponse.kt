package com.shoonya.echo.features.auth.data.model

import com.shoonya.echo.core.data.model.AccountStatusDto
import com.shoonya.echo.core.data.model.PresenceDto
import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import com.shoonya.echo.core.data.model.toDomain
import com.shoonya.echo.core.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val user: LoginUserDto,
)

@Serializable
data class LoginUserDto(
    @SerialName("_id") val id: String,
    val email: String,
    val username: String,
    val profile: UserProfileEmbedDto? = null,
    val presence: PresenceDto? = null,
    val accountStatus: AccountStatusDto? = null,
)

fun LoginUserDto.toDomain(): User {
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
            ?: com.shoonya.echo.core.domain.model.Presence(
                com.shoonya.echo.core.domain.model.PresenceStatus.OFFLINE,
                false,
                null,
            ),
        isActive = accountStatus?.isActive ?: true,
        isVerified = accountStatus?.isVerified ?: false,
        isBanned = accountStatus?.isBanned ?: false,
    )
}