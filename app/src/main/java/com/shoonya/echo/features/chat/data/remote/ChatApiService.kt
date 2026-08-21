package com.shoonya.echo.features.chat.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.features.chat.data.model.ChatDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    @POST("chats/direct/{userId}")
    suspend fun createDirectChat(
        @Path("userId") userId: String,
    ): ApiResponse<ChatDto>

    @GET("chats/hub/stats")
    suspend fun getHubStats(): ApiResponse<Unit>
}