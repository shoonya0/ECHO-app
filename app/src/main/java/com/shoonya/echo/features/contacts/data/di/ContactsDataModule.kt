package com.shoonya.echo.features.contacts.data.di

import com.shoonya.echo.features.contacts.data.remote.ContactsApiService
import com.shoonya.echo.features.contacts.data.remote.UsersApiService
import com.shoonya.echo.features.contacts.data.repository.ContactsRepositoryImpl
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsBindsModule {
    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ContactsNetworkModule {
    @Provides
    @Singleton
    fun provideContactsApiService(retrofit: Retrofit): ContactsApiService =
        retrofit.create(ContactsApiService::class.java)

    @Provides
    @Singleton
    fun provideUsersApiService(retrofit: Retrofit): UsersApiService =
        retrofit.create(UsersApiService::class.java)
}