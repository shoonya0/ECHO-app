package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import javax.inject.Inject

class JoinChatUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    suspend operator fun invoke(chatId: String, senderId: String): Result<Unit> {
        val command = WebSocketCommand.JoinChat(chatId = chatId, senderId = senderId)
        // fire-and-forget on join_chat — the server doesn't send individual response frames
        // for join/leave; it broadcasts join events to the room
        wsRepository.sendRaw(command)
        return Result.success(Unit)
    }
}