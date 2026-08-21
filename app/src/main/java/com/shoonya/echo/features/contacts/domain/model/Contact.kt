package com.shoonya.echo.features.contacts.domain.model

import com.shoonya.echo.core.domain.model.Presence

data class Contact(
    val id: String,
    val username: String,
    val displayName: String,
    val avatar: String,
    val presence: Presence,
    val statusMessage: String = "",
    val isFavorite: Boolean,
)
