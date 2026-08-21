package com.shoonya.echo.features.chat.data.di

import com.shoonya.echo.features.chat.data.repository.WebSocketRepositoryImpl
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import dagger.Binds
import dagger.Module
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