package com.shoonya.echo.features.chat.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.features.chat.data.model.MessageListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApiService {
    @GET("messages/{chatId}/messages")
    suspend fun getChatMessages(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): ApiResponse<MessageListResponse>
}