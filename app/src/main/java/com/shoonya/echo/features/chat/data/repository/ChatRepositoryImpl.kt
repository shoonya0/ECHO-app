package com.shoonya.echo.features.chat.data.repository

import com.shoonya.echo.core.data.local.SecureTokenStore
import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.features.chat.data.local.ChatCache
import com.shoonya.echo.features.chat.data.model.toChat
import com.shoonya.echo.features.chat.data.model.toMessage
import com.shoonya.echo.features.chat.data.remote.ChatApiService
import com.shoonya.echo.features.chat.data.remote.MessageApiService
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApiService,
    private val messageApi: MessageApiService,
    private val tokenStore: SecureTokenStore,
    private val chatCache: ChatCache,
) : ChatRepository {

    override suspend fun getChatList(contactIds: List<String>): Result<List<Chat>> {
        val currentUserId = tokenStore.getUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        val chats = mutableListOf<Chat>()
        val seenChatIds = mutableSetOf<String>()

        // Discover direct chats from existing contacts
        for (contactId in contactIds) {
            runCatching {
                val chatDto = chatApi.createDirectChat(contactId).toResult().getOrThrow()
                if (chatDto.id !in seenChatIds) {
                    seenChatIds.add(chatDto.id)
                    chatCache.addChat(chatDto.id)
                    val chat = chatDto.toChat(currentUserId)
                    // Try to fetch the latest message preview
                    val preview = getLatestMessage(chatDto.id).getOrNull()
                    chats.add(chat.copy(lastMessage = preview))
                }
            }.onFailure { err ->
                Timber.tag("ChatRepo").e(err, "discover direct chat failed for contact: %s", contactId)
            }
        }

        // Sort by latest message time descending (put newest chats first)
        return Result.success(
            chats.sortedByDescending { chat ->
                chat.lastMessage?.createdAt ?: chat.createdAt
            }
        )
    }

    override suspend fun createDirectChat(targetUserId: String): Result<Chat> {
        val currentUserId = tokenStore.getUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return runCatching {
            val chatDto = chatApi.createDirectChat(targetUserId).toResult().getOrThrow()
            val chat = chatDto.toChat(currentUserId)
            chatCache.cacheChat(chat)
            chat
        }.onSuccess { chat ->
            Timber.tag("ChatRepo").d("create direct chat success: %s -> %s", currentUserId, chat.id)
        }.onFailure { err ->
            Timber.tag("ChatRepo").e(err, "create direct chat failed for user: %s", targetUserId)
        }
    }

    override suspend fun getChatMessages(
        chatId: String,
        limit: Int,
        offset: Int,
    ): Result<Pair<List<Message>, Int>> {
        val currentUserId = tokenStore.getUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return runCatching {
            val response = messageApi.getChatMessages(chatId, limit, offset).toResult().getOrThrow()
            val messages = response.messages
                .map { it.toMessage(currentUserId) }
                .sortedBy { it.createdAt } // Normalize to oldest-first for WhatsApp-like ordering
            Pair(messages, response.pagination.totalCount)
        }.onSuccess { (messages, total) ->
            Timber.tag("ChatRepo").d("get messages success: %s (offset=%d, count=%d/%d)", chatId, offset, messages.size, total)
        }.onFailure { err ->
            Timber.tag("ChatRepo").e(err, "get messages failed: %s", chatId)
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        messageType: String,
    ): Result<Message> {
        // REST-based send is not yet available — the backend only supports WebSocket message sending.
        // This returns a NOT_IMPLEMENTED error until Phase 5 wires the WebSocket.
        return Result.failure(
            UnsupportedOperationException("Message sending is coming in Phase 5 (WebSocket). For now, use the WebSocket protocol.")
        )
    }

    override suspend fun getChat(chatId: String): Result<Chat> {
        val cached = chatCache.getChat(chatId)
        return if (cached != null) {
            Result.success(cached)
        } else {
            Result.failure(IllegalStateException("Chat not found in cache: $chatId"))
        }
    }

    override suspend fun getLatestMessage(chatId: String): Result<Message?> {
        val currentUserId = tokenStore.getUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        return runCatching {
            val response = messageApi.getChatMessages(chatId, limit = 1, offset = 0)
                .toResult()
                .getOrNull()
            response?.messages?.firstOrNull()?.toMessage(currentUserId)
        }.onFailure { err ->
            Timber.tag("ChatRepo").e(err, "get latest message failed: %s", chatId)
        }
    }
}