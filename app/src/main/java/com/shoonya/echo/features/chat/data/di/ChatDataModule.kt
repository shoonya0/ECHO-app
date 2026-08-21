package com.shoonya.echo.features.chat.data.di

import com.shoonya.echo.features.chat.data.remote.ChatApiService
import com.shoonya.echo.features.chat.data.remote.MessageApiService
import com.shoonya.echo.features.chat.data.repository.ChatRepositoryImpl
import com.shoonya.echo.features.chat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatBindsModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ChatNetworkModule {
    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)

    @Provides
    @Singleton
    fun provideMessageApiService(retrofit: Retrofit): MessageApiService =
        retrofit.create(MessageApiService::class.java)
}