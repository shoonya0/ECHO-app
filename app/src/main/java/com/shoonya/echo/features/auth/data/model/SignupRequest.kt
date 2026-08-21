package com.shoonya.echo.features.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val username: String,
    val password: String,
)