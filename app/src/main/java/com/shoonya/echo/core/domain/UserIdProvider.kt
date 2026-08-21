package com.shoonya.echo.core.domain

/**
 * Provides the current logged-in user's ID.
 *
 * Decouples ViewModels from Android Context-dependent [SecureTokenStore],
 * enabling pure-JVM unit tests with [test/fakes/FakeUserIdProvider].
 */
interface UserIdProvider {
    fun getUserId(): String?
}