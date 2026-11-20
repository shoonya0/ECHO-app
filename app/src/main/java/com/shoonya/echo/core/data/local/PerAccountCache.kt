package com.shoonya.echo.core.data.local

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * In-memory data that belongs to the signed-in account. `SessionManager` clears every bound
 * one when the account no longer exists (`ACCOUNT_NOT_FOUND`, #22), so the next account to log
 * in within the same process doesn't see it. Features bind theirs with `@Binds @IntoSet`.
 */
interface PerAccountCache {
    fun clear()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PerAccountCacheModule {
    // Lets the set be empty when no feature binds a cache.
    @Multibinds
    abstract fun perAccountCaches(): Set<PerAccountCache>
}
