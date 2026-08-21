package com.shoonya.echo.features.contacts.domain.repository

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion

interface ContactsRepository {
    suspend fun getContacts(): Result<List<Contact>>
    suspend fun getFavorites(): Result<List<Contact>>
    suspend fun getBlockedUsers(): Result<List<Contact>>
    suspend fun getIncomingRequests(): Result<List<ContactRequest>>
    suspend fun getOutgoingRequests(): Result<List<ContactRequest>>
    suspend fun sendContactRequest(userId: String): Result<Unit>
    suspend fun acceptContactRequest(requestId: String): Result<Unit>
    suspend fun declineContactRequest(requestId: String): Result<Unit>
    suspend fun removeContact(contactId: String): Result<Unit>
    suspend fun blockUser(userId: String): Result<Unit>
    suspend fun unblockUser(userId: String): Result<Unit>
    suspend fun addToFavorites(userId: String): Result<Unit>
    suspend fun removeFromFavorites(userId: String): Result<Unit>
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun getUserSuggestions(page: Int, limit: Int): Result<List<UserSuggestion>>
}
