package com.shoonya.echo.core.util

import com.shoonya.echo.BuildConfig

object ApiConfig {
    val baseUrl: String get() = BuildConfig.API_BASE_URL
    val wsBaseUrl: String get() = BuildConfig.WS_BASE_URL
    val isLoggingEnabled: Boolean get() = BuildConfig.ENABLE_LOGGING
}