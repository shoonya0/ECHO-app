package com.shoonya.echo.core.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class PaginatedData<T>(
    val items: List<T>,
    val pagination: PaginationInfo,
)

@Serializable
data class PaginationInfo(
    val limit: Int,
    val total: Int,
    val total_pages: Int,
)

fun <T> ApiResponse<T>.toResult(): Result<T> {
    return if (success && data != null) {
        Result.success(data)
    } else {
        Result.failure(ApiException(message ?: error ?: "Unknown error"))
    }
}

fun ApiResponse<Unit>.toUnitResult(): Result<Unit> {
    return if (success) {
        Result.success(Unit)
    } else {
        Result.failure(ApiException(message ?: error ?: "Unknown error"))
    }
}
