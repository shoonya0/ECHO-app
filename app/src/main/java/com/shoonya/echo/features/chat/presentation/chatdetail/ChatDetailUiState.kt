package com.shoonya.echo.features.chat.presentation.chatdetail

import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.repository.WsConnectionStatus

data class ChatDetailUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val messages: List<Message> = emptyList(),
    val totalCount: Int = 0,
    val chatName: String = "",
    val error: String? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val currentUserId: String = "",
    val typingUserIds: Set<String> = emptySet(),
    val connectionStatus: WsConnectionStatus = WsConnectionStatus.DISCONNECTED,
    val chatType: ChatType? = null,
    val isCreatingDirectChat: Boolean = false,
) {
    val hasMore: Boolean get() = messages.size < totalCount
}