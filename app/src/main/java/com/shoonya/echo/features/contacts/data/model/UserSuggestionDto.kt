package com.shoonya.echo.features.contacts.data.model

import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSuggestionDto(
    @SerialName("_id") val id: String,
    val username: String,
    val profile: UserProfileEmbedDto? = null,
)

fun UserSuggestionDto.toDomain(): UserSuggestion = UserSuggestion(
    id = id,
    username = username,
    displayName = profile?.displayName ?: username,
    avatar = profile?.avatar ?: "",
    statusMessage = profile?.statusMessage ?: "",
    bio = profile?.bio ?: "",
)