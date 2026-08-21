package com.shoonya.echo.fakes

import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.chat.domain.model.Chat
import com.shoonya.echo.features.chat.domain.model.ChatRole
import com.shoonya.echo.features.chat.domain.model.ChatSettings
import com.shoonya.echo.features.chat.domain.model.ChatType
import com.shoonya.echo.features.chat.domain.model.Message
import com.shoonya.echo.features.chat.domain.model.MessageType
import com.shoonya.echo.features.chat.domain.model.Participant
import com.shoonya.echo.features.chat.domain.model.Reaction
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.RequestDirection
import com.shoonya.echo.features.settings.domain.model.UserProfile
import java.time.Instant

object TestData {
    // ── Shared IDs ──
    const val CURRENT_USER_ID = "507f1f77bcf86cd799439011"
    const val ALICE_ID = "60af1f77bcf86cd799439001"
    const val BOB_ID = "60af1f77bcf86cd799439002"
    const val CHAT_ID_DIRECT = "6a775700f7feecc064dc54f6"
    const val MESSAGE_ID_1 = "msg_600000000000000000000001"
    const val MESSAGE_ID_2 = "msg_600000000000000000000002"

    // ── Core domain ──
    val user = User(
        id = CURRENT_USER_ID,
        email = "test@echo.com",
        username = "testuser",
        displayName = "Test User",
        avatar = "",
        statusMessage = "Hello",
        bio = "Test bio",
        presence = Presence(PresenceStatus.ONLINE, true, null),
        isActive = true,
        isVerified = true,
        isBanned = false,
    )

    // ── Contacts ──
    val contact1 = Contact(
        id = ALICE_ID,
        username = "alice",
        displayName = "Alice",
        avatar = "",
        presence = Presence(PresenceStatus.ONLINE, true, null),
        statusMessage = "status message",
        isFavorite = false,
    )

    val contact2 = Contact(
        id = BOB_ID,
        username = "bob",
        displayName = "Bob",
        avatar = "",
        presence = Presence(PresenceStatus.OFFLINE, false, null),
        statusMessage = "",
        isFavorite = true,
    )

    val blockedContact = Contact(
        id = "60af1f77bcf86cd799439003",
        username = "spammer",
        displayName = "Spam User",
        avatar = "",
        presence = Presence(PresenceStatus.OFFLINE, false, null),
        statusMessage = "",
        isFavorite = false,
    )

    val incomingRequest = ContactRequest(
        id = "req_in_001",
        username = "charlie",
        displayName = "Charlie",
        avatar = "",
        direction = RequestDirection.INCOMING,
    )

    val outgoingRequest = ContactRequest(
        id = "req_out_001",
        username = "dave",
        displayName = "Dave",
        avatar = "",
        direction = RequestDirection.OUTGOING,
    )

    // ── Chat: Participants ──
    val participantMe = Participant(
        userId = CURRENT_USER_ID,
        username = "testuser",
        displayName = "Test User",
        avatar = "",
        role = ChatRole.MEMBER,
        isOnline = true,
        isBlocked = false,
        lastSeen = null,
    )

    val participantAlice = Participant(
        userId = ALICE_ID,
        username = "alice",
        displayName = "Alice",
        avatar = "",
        role = ChatRole.MEMBER,
        isOnline = true,
        isBlocked = false,
        lastSeen = null,
    )

    val participantBob = Participant(
        userId = BOB_ID,
        username = "bob",
        displayName = "Bob",
        avatar = "",
        role = ChatRole.MEMBER,
        isOnline = false,
        isBlocked = false,
        lastSeen = null,
    )

    // ── Chat: Messages ──
    val message1 = Message(
        id = MESSAGE_ID_1,
        chatId = CHAT_ID_DIRECT,
        senderId = ALICE_ID,
        senderName = "alice",
        senderDisplayName = "Alice",
        senderAvatar = "",
        content = "Hello! How are you?",
        type = MessageType.TEXT,
        attachments = emptyList(),
        reactions = emptyMap(),
        isEdited = false,
        readByUserIds = setOf(CURRENT_USER_ID),
        createdAt = Instant.now().minusSeconds(300),
    )

    val message2 = Message(
        id = MESSAGE_ID_2,
        chatId = CHAT_ID_DIRECT,
        senderId = CURRENT_USER_ID,
        senderName = "testuser",
        senderDisplayName = "Test User",
        senderAvatar = "",
        content = "I'm doing great!",
        type = MessageType.TEXT,
        attachments = emptyList(),
        reactions = mapOf("❤️" to Reaction(1, listOf("alice"), hasReacted = true)),
        isEdited = false,
        readByUserIds = setOf(ALICE_ID),
        createdAt = Instant.now().minusSeconds(120),
    )

    // ── Chat: Chats ──
    val directChat = Chat(
        id = CHAT_ID_DIRECT,
        type = ChatType.DIRECT,
        name = "Alice",
        description = "",
        avatar = "",
        participants = listOf(participantMe, participantAlice),
        ownerId = null,
        participantCount = 2,
        messageCount = 10,
        unreadCount = 0,
        lastMessage = message1,
        settings = ChatSettings(),
        createdAt = Instant.now().minusSeconds(86400),
        updatedAt = null,
    )

    // ── Settings ──
    val userProfile = UserProfile(
        id = CURRENT_USER_ID,
        username = "testuser",
        email = "test@echo.com",
        phone = "",
        displayName = "Test User",
        avatar = "",
        statusMessage = "Hello",
        bio = "Test bio",
        presence = Presence(PresenceStatus.ONLINE, true, null),
        isActive = true,
    )
}