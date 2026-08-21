package com.shoonya.echo.features.contacts.domain.model

data class UserSuggestion(
    val id: String,
    val username: String,
    val displayName: String,
    val avatar: String,
    val statusMessage: String,
    val bio: String,
)