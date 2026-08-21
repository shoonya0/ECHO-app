package com.shoonya.echo.fakes

import com.shoonya.echo.core.domain.UserIdProvider

/**
 * Configurable [UserIdProvider] for unit tests.
 *
 * Unlike the real Hilt binding (which reads from EncryptedSharedPreferences),
 * this fake returns whatever ID string you set.
 */
class FakeUserIdProvider(private var userId: String? = null) : UserIdProvider {
    fun setUserId(id: String?) { userId = id }
    override fun getUserId(): String? = userId
}