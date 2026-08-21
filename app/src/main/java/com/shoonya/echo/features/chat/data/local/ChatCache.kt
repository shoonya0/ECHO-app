package com.shoonya.echo.features.chat.data.local

import com.shoonya.echo.features.chat.domain.model.Chat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatCache @Inject constructor() {
    private val discoveredChatIds = mutableSetOf<String>()
    private val cachedChats = mutableMapOf<String, Chat>()

    fun addChat(chatId: String) {
        discoveredChatIds.add(chatId)
    }

    fun cacheChat(chat: Chat) {
        discoveredChatIds.add(chat.id)
        cachedChats[chat.id] = chat
    }

    fun getChat(chatId: String): Chat? = cachedChats[chatId]

    fun getDiscoveredChatIds(): Set<String> = discoveredChatIds.toSet()

    fun clear() {
        discoveredChatIds.clear()
        cachedChats.clear()
    }
}
