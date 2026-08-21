package com.shoonya.echo.features.auth.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.features.auth.data.model.AuthResponse
import com.shoonya.echo.features.auth.data.model.LoginRequest
import com.shoonya.echo.features.auth.data.model.LoginUserDto
import com.shoonya.echo.features.auth.data.model.SignupRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<LoginUserDto>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
