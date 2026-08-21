package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class LeaveChatUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        val command = WebSocketCommand.LeaveChat(chatId = chatId)
        wsRepository.sendRaw(command)
        return Result.success(Unit)
    }
}