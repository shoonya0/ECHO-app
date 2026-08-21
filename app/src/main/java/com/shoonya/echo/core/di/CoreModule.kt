package com.shoonya.echo.core.di

import android.content.Context
import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.domain.UserIdProvider
import com.shoonya.echo.core.theme.ThemeStore
import com.shoonya.echo.core.theme.ThemeStoreImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideSecureTokenStore(
        @ApplicationContext context: Context,
    ): SecureTokenStore = SecureTokenStore(context)

    @Provides
    @Singleton
    fun provideUserIdProvider(
        secureTokenStore: SecureTokenStore,
    ): UserIdProvider = object : UserIdProvider {
        override fun getUserId(): String? = secureTokenStore.getUserId()
    }

    @Provides
    @Singleton
    fun provideThemeStore(
        @ApplicationContext context: Context,
    ): ThemeStore = ThemeStoreImpl(context)
}
