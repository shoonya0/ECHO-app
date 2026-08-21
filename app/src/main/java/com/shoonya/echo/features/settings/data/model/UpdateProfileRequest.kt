package com.shoonya.echo.features.settings.data.model

import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val profile: UserProfileEmbedDto? = null,
)