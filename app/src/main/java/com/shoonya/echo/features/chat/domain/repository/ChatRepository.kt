package com.shoonya.echo.features.chat.domain.repository

import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessagePage

interface ChatRepository {
    suspend fun getChatList(contactIds: List<String>): Result<List<Chat>>
    suspend fun getChat(chatId: String): Result<Chat>
    suspend fun createDirectChat(targetUserId: String): Result<Chat>
    suspend fun getChatMessages(
        chatId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<MessagePage>
    suspend fun sendMessage(
        chatId: String,
        content: String,
        messageType: String = "text"
    ): Result<Message>
    suspend fun getLatestMessage(chatId: String): Result<Message?>
}
