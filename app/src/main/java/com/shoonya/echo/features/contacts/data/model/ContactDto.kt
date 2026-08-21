package com.shoonya.echo.features.contacts.data.model

import com.shoonya.echo.core.data.model.PresenceDto
import com.shoonya.echo.core.data.model.UserProfileEmbedDto
import com.shoonya.echo.core.data.model.toDomain
import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.RequestDirection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    @SerialName("_id") val id: String,
    @SerialName("username") val username: String = "",
    val contactInfo: ContactInfoDto? = null,
)

@Serializable
data class ContactInfoDto(
    val profile: UserProfileEmbedDto? = null,
    val username: String = "",
    val presence: PresenceDto? = null,
    val isFavorite: Boolean = false,
)

fun ContactDto.toContact(): Contact = Contact(
    id = id,
    username = username.ifBlank { contactInfo?.username ?: "" },
    displayName = contactInfo?.profile?.displayName
        ?: username.ifBlank { contactInfo?.username ?: "" }
        ?: "",
    avatar = contactInfo?.profile?.avatar ?: "",
    statusMessage = contactInfo?.profile?.statusMessage ?: "",
    presence = contactInfo?.presence?.toDomain()
        ?: Presence(PresenceStatus.OFFLINE, false, null),
    isFavorite = contactInfo?.isFavorite ?: false,
)

fun ContactDto.toIncomingRequest(): ContactRequest = ContactRequest(
    id = id,
    username = username.ifBlank { contactInfo?.username ?: "" },
    displayName = contactInfo?.profile?.displayName
        ?: username.ifBlank { contactInfo?.username ?: "" }
        ?: "",
    avatar = contactInfo?.profile?.avatar ?: "",
    direction = RequestDirection.INCOMING,
)

fun ContactDto.toOutgoingRequest(): ContactRequest = ContactRequest(
    id = id,
    username = username.ifBlank { contactInfo?.username ?: "" },
    displayName = contactInfo?.profile?.displayName
        ?: username.ifBlank { contactInfo?.username ?: "" }
        ?: "",
    avatar = contactInfo?.profile?.avatar ?: "",
    direction = RequestDirection.OUTGOING,
)
