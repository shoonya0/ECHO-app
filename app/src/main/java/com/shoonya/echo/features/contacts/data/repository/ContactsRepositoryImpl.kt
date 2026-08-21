package com.shoonya.echo.features.contacts.data.repository

import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.core.data.remote.toUnitResult
import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.data.model.UserProfileDto
import com.shoonya.echo.features.contacts.data.model.toContact
import com.shoonya.echo.features.contacts.data.model.toDomain
import com.shoonya.echo.features.contacts.data.model.toIncomingRequest
import com.shoonya.echo.features.contacts.data.model.toOutgoingRequest
import com.shoonya.echo.features.contacts.data.model.UserSuggestionDto
import com.shoonya.echo.features.contacts.data.remote.ContactsApiService
import com.shoonya.echo.features.contacts.data.remote.UsersApiService
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val contactsApi: ContactsApiService,
    private val usersApi: UsersApiService,
) : ContactsRepository {

    override suspend fun getContacts(): Result<List<Contact>> {
        return runCatching {
            contactsApi.getContacts()
                .toResult()
                .getOrThrow()
                .items
                .map { it.toContact() }
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getContacts failed")
        }
    }

    override suspend fun getFavorites(): Result<List<Contact>> {
        return runCatching {
            contactsApi.getFavorites()
                .toResult()
                .getOrThrow()
                .items
                .map { it.toContact() }
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getFavorites failed")
        }
    }

    override suspend fun getBlockedUsers(): Result<List<Contact>> {
        return runCatching {
            contactsApi.getBlockedUsers()
                .toResult()
                .getOrThrow()
                .items
                .map { it.toContact() }
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getBlockedUsers failed")
        }
    }

    override suspend fun getIncomingRequests(): Result<List<ContactRequest>> {
        return runCatching {
            contactsApi.getContactRequests()
                .toResult()
                .getOrThrow()
                .items
                .map { it.toIncomingRequest() }
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getIncomingRequests failed")
        }
    }

    override suspend fun getOutgoingRequests(): Result<List<ContactRequest>> {
        return runCatching {
            contactsApi.getSentContactRequests()
                .toResult()
                .getOrThrow()
                .items
                .map { it.toOutgoingRequest() }
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getOutgoingRequests failed")
        }
    }

    override suspend fun sendContactRequest(userId: String): Result<Unit> {
        return runCatching {
            contactsApi.sendContactRequest(userId)
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("sendContactRequest success: %s", userId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "sendContactRequest failed: %s", userId)
        }
    }

    override suspend fun acceptContactRequest(requestId: String): Result<Unit> {
        return runCatching {
            contactsApi.acceptOrDeclineRequest(requestId, "accepted")
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("acceptContactRequest success: %s", requestId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "acceptContactRequest failed: %s", requestId)
        }
    }

    override suspend fun declineContactRequest(requestId: String): Result<Unit> {
        return runCatching {
            contactsApi.acceptOrDeclineRequest(requestId, "declined")
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("declineContactRequest success: %s", requestId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "declineContactRequest failed: %s", requestId)
        }
    }

    override suspend fun removeContact(contactId: String): Result<Unit> {
        return runCatching {
            contactsApi.removeContact(contactId)
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("removeContact success: %s", contactId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "removeContact failed: %s", contactId)
        }
    }

    override suspend fun blockUser(userId: String): Result<Unit> {
        return runCatching {
            contactsApi.blockUnblockUser(userId, "block")
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("blockUser success: %s", userId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "blockUser failed: %s", userId)
        }
    }

    override suspend fun unblockUser(userId: String): Result<Unit> {
        return runCatching {
            contactsApi.blockUnblockUser(userId, "unblock")
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("unblockUser success: %s", userId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "unblockUser failed: %s", userId)
        }
    }

    override suspend fun addToFavorites(userId: String): Result<Unit> {
        return runCatching {
            contactsApi.addToFavorites(userId)
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("addToFavorites success: %s", userId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "addToFavorites failed: %s", userId)
        }
    }

    override suspend fun removeFromFavorites(userId: String): Result<Unit> {
        return runCatching {
            contactsApi.removeFromFavorites(userId)
                .toUnitResult()
                .getOrThrow()
        }.onSuccess {
            Timber.tag("ContactsRepo").d("removeFromFavorites success: %s", userId)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "removeFromFavorites failed: %s", userId)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<User> {
        return runCatching {
            usersApi.getUserProfile(userId)
                .toResult()
                .getOrThrow()
        }.mapCatching { dto ->
            dto.toDomain()
        }.onSuccess { user ->
            Timber.tag("ContactsRepo").d("getUserProfile success: %s", user.username)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getUserProfile failed: %s", userId)
        }
    }

    override suspend fun getUserSuggestions(page: Int, limit: Int): Result<List<UserSuggestion>> {
        return runCatching {
            usersApi.getUserSuggestions(page = page, limit = limit)
                .toResult()
                .getOrThrow()
                .items
                .map { it.toDomain() }
        }.onSuccess { suggestions ->
            Timber.tag("ContactsRepo").d("getUserSuggestions success: %d items", suggestions.size)
        }.onFailure { err ->
            Timber.tag("ContactsRepo").e(err, "getUserSuggestions failed")
        }
    }
}
