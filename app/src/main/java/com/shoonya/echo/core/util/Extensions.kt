package com.shoonya.echo.core.util

private val EMAIL_REGEX = Regex(
    "^[a-zA-Z0-9+._%\\-]{1,256}" +
        "@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{1,25})+$",
)

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)

fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else take(maxLength) + "\u2026"
