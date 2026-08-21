package com.shoonya.echo.features.settings.data.model

import com.shoonya.echo.core.data.model.AccountStatusDto
import com.shoonya.echo.core.data.model.PresenceDto
import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("_id") val id: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val presence: PresenceDto? = null,
    val profile: UserProfileEmbedDto? = null,
    val accountStatus: AccountStatusDto? = null,
)