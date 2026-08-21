package com.shoonya.echo.features.contacts.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.core.data.remote.PaginatedData
import com.shoonya.echo.features.contacts.data.model.UserProfileDto
import com.shoonya.echo.features.contacts.data.model.UserSuggestionDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface UsersApiService {
    @GET("users/{id}")
    suspend fun getUserProfile(
        @Path("id") userId: String,
    ): ApiResponse<UserProfileDto>

    @GET("users/suggestions")
    suspend fun getUserSuggestions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<PaginatedData<UserSuggestionDto>>
}
