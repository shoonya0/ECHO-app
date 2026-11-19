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
        // The previous page's `pagination.nextCursor`, as is; null for the newest page.
        // Never send `offset`: the server answers 400 to it (issue #7).
        @Query("before") before: String? = null
    ): ApiResponse<MessageListResponse>
}
