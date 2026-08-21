package com.shoonya.echo.features.settings.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.features.settings.data.model.ProfileDto
import com.shoonya.echo.features.settings.data.model.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApiService {
    @GET("profile/")
    suspend fun getProfile(): ApiResponse<ProfileDto>

    @PUT("profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<ProfileDto>

    @DELETE("profile/delete")
    suspend fun deleteAccount(): ApiResponse<Unit>
}