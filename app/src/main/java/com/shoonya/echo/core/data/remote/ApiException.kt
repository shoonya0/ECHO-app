package com.shoonya.echo.core.data.remote

class ApiException(
    message: String,
    val code: Int = 0,
) : Exception(message)