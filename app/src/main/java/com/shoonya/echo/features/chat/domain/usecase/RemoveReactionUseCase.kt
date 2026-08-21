package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.ReactionMetadata
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class RemoveReactionUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    suspend operator fun invoke(chatId: String, messageId: String, emoji: String): Result<Unit> {
        val command = WebSocketCommand.RemoveReaction(
            chatId = chatId,
            metadata = ReactionMetadata(messageId = messageId, emoji = emoji),
        )
        wsRepository.sendRaw(command)
        return Result.success(Unit)
    }
}