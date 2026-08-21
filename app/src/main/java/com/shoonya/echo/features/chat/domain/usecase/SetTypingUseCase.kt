package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.TypingMetadata
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class SetTypingUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    suspend operator fun invoke(chatId: String, isTyping: Boolean): Result<Unit> {
        val command = WebSocketCommand.SetTyping(
            chatId = chatId,
            metadata = TypingMetadata(isTyping = isTyping),
        )
        wsRepository.sendRaw(command)
        return Result.success(Unit)
    }
}