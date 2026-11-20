package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.MarkReadMetadata
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class MarkReadUseCase @Inject constructor(private val wsRepository: WebSocketRepository) {
    /** Returns the request's id; the server answers only with a `delivery` event or an error. */
    suspend operator fun invoke(chatId: String, messageIds: List<String>): Result<String> {
        val command = WebSocketCommand.MarkRead(
            chatId = chatId,
            metadata = MarkReadMetadata(messageIds = messageIds)
        )
        return if (wsRepository.sendRaw(command)) {
            Result.success(command.requestId)
        } else {
            Result.failure(IllegalStateException("WebSocket not connected"))
        }
    }
}
