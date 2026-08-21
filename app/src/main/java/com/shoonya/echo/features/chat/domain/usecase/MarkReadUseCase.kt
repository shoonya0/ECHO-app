package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.MarkReadMetadata
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class MarkReadUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    suspend operator fun invoke(chatId: String, messageIds: List<String>): Result<Unit> {
        val command = WebSocketCommand.MarkRead(
            chatId = chatId,
            metadata = MarkReadMetadata(messageIds = messageIds),
        )
        wsRepository.sendRaw(command)
        return Result.success(Unit)
    }
}