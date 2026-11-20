package com.shoonya.echo.features.chat.data.di

import com.shoonya.echo.core.data.remote.SessionManager
import com.shoonya.echo.features.chat.data.remote.WsSessionEnder
import com.shoonya.echo.features.chat.data.repository.WebSocketRepositoryImpl
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketBindsModule {
    @Binds
    @Singleton
    abstract fun bindWebSocketRepository(impl: WebSocketRepositoryImpl): WebSocketRepository
}

@Module
@InstallIn(SingletonComponent::class)
object WebSocketProvidesModule {
    // A 4401/4403 close ends the session the same way an HTTP 401/403 does, with its reason.
    @Provides
    fun provideWsSessionEnder(sessionManager: SessionManager): WsSessionEnder =
        WsSessionEnder(sessionManager::expire)
}
