package com.shoonya.echo.features.chat.domain.usecase

import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.ServerFrame
import com.shoonya.echo.features.chat.domain.model.WebSocketCommand
import com.shoonya.echo.features.chat.domain.repository.WebSocketRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val wsRepository: WebSocketRepository,
) {
    private fun generateRequestId(): String = UUID.randomUUID().toString().take(8)

    suspend operator fun invoke(
        chatId: String,
        content: String,
        senderId: String,
        messageType: String = "text",
        requestId: String = generateRequestId(),
    ): Result<Message> {
        val command = WebSocketCommand.SendMessage(
            chatId = chatId,
            content = content,
            messageType = messageType,
            requestId = requestId,
        )

        return wsRepository.send(command).map { frame ->
            when (frame) {
                is ServerFrame.Response -> Message(
                    id = frame.data.messageId ?: command.requestId,
                    chatId = chatId,
                    senderId = senderId,
                    senderName = "",
                    senderDisplayName = "",
                    senderAvatar = "",
                    content = content,
                    type = parseMessageType(messageType),
                    attachments = emptyList(),
                    reactions = emptyMap(),
                    isEdited = false,
                    readByUserIds = emptySet(),
                    createdAt = Instant.now(),
                )
                else -> throw IllegalStateException("Unexpected response frame: ${frame.type}")
            }
        }
    }

    private fun parseMessageType(type: String): MessageType = when (type.lowercase()) {
        "image" -> MessageType.IMAGE
        "file" -> MessageType.FILE
        "audio" -> MessageType.AUDIO
        "video" -> MessageType.VIDEO
        else -> MessageType.TEXT
    }
}