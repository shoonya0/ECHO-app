package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.MessagePage
import com.shoonya.echo.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(
        chatId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<MessagePage> = chatRepository.getChatMessages(chatId, limit, before)
}
