package com.shoonya.echo.features.chat.presentation.chatlist

import com.shoonya.echo.features.chat.domain.model.Chat

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val error: String? = null,
    val currentUserId: String = "",
)