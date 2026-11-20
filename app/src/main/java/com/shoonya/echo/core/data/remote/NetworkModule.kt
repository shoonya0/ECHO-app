package com.shoonya.echo.core.data.remote

import com.shoonya.echo.BuildConfig
import com.shoonya.echo.core.data.local.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @RestClient
    fun provideRestOkHttpClient(
        tokenStore: SecureTokenStore,
        sessionManager: SessionManager
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(tokenStore, sessionManager))
        .apply {
            if (BuildConfig.ENABLE_LOGGING) {
                addInterceptor(createLoggingInterceptor())
            }
        }
        .build()

    @Provides
    @Singleton
    @WsClient
    fun provideWsOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .pingInterval(54, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(@RestClient okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/echo/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

/**
 * Debug HTTP logger. Logs bodies but never the bearer token: the Authorization
 * header is redacted so the JWT doesn't end up in logcat.
 */
internal fun createLoggingInterceptor(
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
): HttpLoggingInterceptor = HttpLoggingInterceptor(logger).apply {
    level = HttpLoggingInterceptor.Level.BODY
    redactHeader("Authorization")
}
