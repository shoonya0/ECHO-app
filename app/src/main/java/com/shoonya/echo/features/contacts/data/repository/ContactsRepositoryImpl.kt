package com.shoonya.echo.features.contacts.data.repository

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.core.data.remote.PaginatedData
import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.core.data.remote.toUnitResult
import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.data.model.ContactDto
import com.shoonya.echo.features.contacts.data.model.UserProfileDto
import com.shoonya.echo.features.contacts.data.model.UserSuggestionDto
import com.shoonya.echo.features.contacts.data.model.toContact
import com.shoonya.echo.features.contacts.data.model.toDomain
import com.shoonya.echo.features.contacts.data.model.toIncomingRequest
import com.shoonya.echo.features.contacts.data.model.toOutgoingRequest
import com.shoonya.echo.features.contacts.data.remote.ContactsApiService
import com.shoonya.echo.features.contacts.data.remote.UsersApiService
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val contactsApi: ContactsApiService,
    private val usersApi: UsersApiService
) : ContactsRepository {

    /**
     * Reads every page of a contact list (server #39 pages them), so users with more
     * entries than one page see all of them. Most lists fit one page, so one request.
     * Stops at `total_pages`, at an empty page, or at [MAX_PAGES] as a guard against a
     * reply that never ends. Entries that moved between pages while reading are kept once.
     */
    private suspend fun fetchAllPages(
        fetchPage: suspend (page: Int) -> ApiResponse<PaginatedData<ContactDto>>
    ): List<ContactDto> {
        val items = mutableListOf<ContactDto>()
        var page = 1
        while (true) {
            val data = fetchPage(page).toResult().getOrThrow()
            items += data.items
            val lastPage = minOf(data.pagination.total_pages, MAX_PAGES)
            if (data.items.isEmpty() || page >= lastPage) break
            page++
        }
        return items.distinctBy { it.id }
    }

    override suspend fun getContacts(): Result<List<Contact>> = runCatching {
        fetchAllPages { page -> contactsApi.getContacts(page = page) }
            .map { it.toContact() }
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "getContacts failed")
    }

    override suspend fun getFavorites(): Result<List<Contact>> = runCatching {
        fetchAllPages { page -> contactsApi.getFavorites(page = page) }
            .map { it.toContact() }
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "getFavorites failed")
    }

    override suspend fun getBlockedUsers(): Result<List<Contact>> = runCatching {
        fetchAllPages { page -> contactsApi.getBlockedUsers(page = page) }
            .map { it.toContact() }
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "getBlockedUsers failed")
    }

    override suspend fun getIncomingRequests(): Result<List<ContactRequest>> = runCatching {
        fetchAllPages { page -> contactsApi.getContactRequests(page = page) }
            .map { it.toIncomingRequest() }
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "getIncomingRequests failed")
    }

    override suspend fun getOutgoingRequests(): Result<List<ContactRequest>> = runCatching {
        fetchAllPages { page -> contactsApi.getSentContactRequests(page = page) }
            .map { it.toOutgoingRequest() }
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "getOutgoingRequests failed")
    }

    override suspend fun sendContactRequest(userId: String): Result<Unit> = runCatching {
        contactsApi.sendContactRequest(userId)
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("sendContactRequest success: %s", userId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "sendContactRequest failed: %s", userId)
    }

    override suspend fun acceptContactRequest(requestId: String): Result<Unit> = runCatching {
        contactsApi.acceptOrDeclineRequest(requestId, "accepted")
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("acceptContactRequest success: %s", requestId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "acceptContactRequest failed: %s", requestId)
    }

    override suspend fun declineContactRequest(requestId: String): Result<Unit> = runCatching {
        contactsApi.acceptOrDeclineRequest(requestId, "declined")
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("declineContactRequest success: %s", requestId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "declineContactRequest failed: %s", requestId)
    }

    override suspend fun removeContact(contactId: String): Result<Unit> = runCatching {
        contactsApi.removeContact(contactId)
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("removeContact success: %s", contactId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "removeContact failed: %s", contactId)
    }

    override suspend fun blockUser(userId: String): Result<Unit> = runCatching {
        contactsApi.blockUnblockUser(userId, "block")
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("blockUser success: %s", userId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "blockUser failed: %s", userId)
    }

    override suspend fun unblockUser(userId: String): Result<Unit> = runCatching {
        contactsApi.blockUnblockUser(userId, "unblock")
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("unblockUser success: %s", userId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "unblockUser failed: %s", userId)
    }

    override suspend fun addToFavorites(userId: String): Result<Unit> = runCatching {
        contactsApi.addToFavorites(userId)
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("addToFavorites success: %s", userId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "addToFavorites failed: %s", userId)
    }

    override suspend fun removeFromFavorites(userId: String): Result<Unit> = runCatching {
        contactsApi.removeFromFavorites(userId)
            .toUnitResult()
            .getOrThrow()
    }.onSuccess {
        Timber.tag("ContactsRepo").d("removeFromFavorites success: %s", userId)
    }.onFailure { err ->
        Timber.tag("ContactsRepo").e(err, "removeFromFavorites failed: %s", userId)
    }

    override suspend fun getUserProfile(userId: String): Result<User> = runCatching {
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

    override suspend fun getUserSuggestions(page: Int, limit: Int): Result<List<UserSuggestion>> =
        runCatching {
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

    private companion object {
        /** 100 pages of 100: far above any real contact list. */
        const val MAX_PAGES = 100
    }
}
