package com.shoonya.echo.features.auth.domain.model

/** Why the server refuses to let an account in, whatever the password. */
enum class AccountStatus {
    /** `403 ACCOUNT_BANNED`, or `"account is banned"` from older servers. */
    BANNED,

    /** `403 ACCOUNT_INACTIVE`, or `"account is not active"` from older servers. */
    INACTIVE
}

/** Login was refused because the account is [status]; the Login screen says so. */
class AccountUnavailableException(val status: AccountStatus) :
    Exception("Account unavailable: $status")
