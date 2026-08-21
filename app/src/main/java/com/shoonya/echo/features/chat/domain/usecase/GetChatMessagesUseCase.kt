package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(
        chatId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<Pair<List<Message>, Int>> = chatRepository.getChatMessages(chatId, limit, offset)
}