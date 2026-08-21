package com.shoonya.echo.fakes

import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.repository.ChatRepository

class FakeChatRepository : ChatRepository {
    private var chatListResult: Result<List<Chat>> = Result.success(emptyList())
    private var createDirectChatResult: Result<Chat> = Result.failure(Exception("Not set"))
    private var getChatMessagesResult: Result<Pair<List<Message>, Int>> = Result.success(Pair(emptyList(), 0))
    private var sendMessageResult: Result<Message> = Result.failure(UnsupportedOperationException("Not implemented"))
    private var getLatestMessageResult: Result<Message?> = Result.success(null)
    private var getChatResult: Result<Chat> = Result.failure(Exception("Chat not found in cache"))

    fun setChatListSuccess(chats: List<Chat>) {
        chatListResult = Result.success(chats)
    }

    fun setChatListError(message: String) {
        chatListResult = Result.failure(Exception(message))
    }

    fun setCreateDirectChatSuccess(chat: Chat) {
        createDirectChatResult = Result.success(chat)
    }

    fun setCreateDirectChatError(message: String) {
        createDirectChatResult = Result.failure(Exception(message))
    }

    fun setGetChatMessagesSuccess(messages: List<Message>, totalCount: Int) {
        getChatMessagesResult = Result.success(Pair(messages, totalCount))
    }

    fun setGetChatMessagesError(message: String) {
        getChatMessagesResult = Result.failure(Exception(message))
    }

    fun setSendMessageSuccess(message: Message) {
        sendMessageResult = Result.success(message)
    }

    fun setSendMessageError(message: String) {
        sendMessageResult = Result.failure(Exception(message))
    }

    fun setGetLatestMessageSuccess(message: Message?) {
        getLatestMessageResult = Result.success(message)
    }

    fun setGetLatestMessageError(message: String) {
        getLatestMessageResult = Result.failure(Exception(message))
    }

    fun setGetChatSuccess(chat: Chat) {
        getChatResult = Result.success(chat)
    }

    fun setGetChatError(message: String) {
        getChatResult = Result.failure(Exception(message))
    }

    override suspend fun getChatList(contactIds: List<String>): Result<List<Chat>> = chatListResult

    override suspend fun createDirectChat(targetUserId: String): Result<Chat> = createDirectChatResult

    override suspend fun getChatMessages(
        chatId: String,
        limit: Int,
        offset: Int,
    ): Result<Pair<List<Message>, Int>> =
        getChatMessagesResult.map { (messages, total) ->
            Pair(messages.sortedBy { it.createdAt }, total) // Normalize to oldest-first
        }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        messageType: String,
    ): Result<Message> = sendMessageResult

    override suspend fun getChat(chatId: String): Result<Chat> = getChatResult

    override suspend fun getLatestMessage(chatId: String): Result<Message?> = getLatestMessageResult
}