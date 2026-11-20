package com.shoonya.echo.core.domain.model

/** Why the server ended the session; the Login screen tells the user for the account cases. */
enum class SessionEndReason {
    /** `401`: the token was revoked, expired or not valid. Log in again. */
    EXPIRED,

    /** `403 ACCOUNT_BANNED`: the account was banned. */
    ACCOUNT_BANNED,

    /** `403 ACCOUNT_INACTIVE`: the account was deactivated. */
    ACCOUNT_INACTIVE
}
