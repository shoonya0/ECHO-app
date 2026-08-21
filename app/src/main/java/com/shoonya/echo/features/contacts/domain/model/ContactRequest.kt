package com.shoonya.echo.features.contacts.domain.model

data class ContactRequest(
    val id: String,
    val username: String,
    val displayName: String,
    val avatar: String,
    val direction: RequestDirection,
)

enum class RequestDirection { INCOMING, OUTGOING }