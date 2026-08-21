package com.shoonya.echo.fakes

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository

class FakeContactsRepository : ContactsRepository {
    private var contactsResult: Result<List<Contact>> = Result.success(emptyList())
    private var favoritesResult: Result<List<Contact>> = Result.success(emptyList())
    private var blockedUsersResult: Result<List<Contact>> = Result.success(emptyList())
    private var incomingRequestsResult: Result<List<ContactRequest>> = Result.success(emptyList())
    private var outgoingRequestsResult: Result<List<ContactRequest>> = Result.success(emptyList())
    private var sendContactRequestResult: Result<Unit> = Result.success(Unit)
    private var acceptContactRequestResult: Result<Unit> = Result.success(Unit)
    private var declineContactRequestResult: Result<Unit> = Result.success(Unit)
    private var removeContactResult: Result<Unit> = Result.success(Unit)
    private var blockUserResult: Result<Unit> = Result.success(Unit)
    private var unblockUserResult: Result<Unit> = Result.success(Unit)
    private var addToFavoritesResult: Result<Unit> = Result.success(Unit)
    private var removeFromFavoritesResult: Result<Unit> = Result.success(Unit)
    private var getUserSuggestionsResult: Result<List<UserSuggestion>> = Result.success(emptyList())
    private var getUserProfileResult: Result<User> = Result.failure(Exception("Not set"))

    fun setContactsSuccess(contacts: List<Contact>) {
        contactsResult = Result.success(contacts)
    }

    fun setContactsError(message: String) {
        contactsResult = Result.failure(Exception(message))
    }

    fun setFavoritesSuccess(favorites: List<Contact>) {
        favoritesResult = Result.success(favorites)
    }

    fun setFavoritesError(message: String) {
        favoritesResult = Result.failure(Exception(message))
    }

    fun setBlockedUsersSuccess(blocked: List<Contact>) {
        blockedUsersResult = Result.success(blocked)
    }

    fun setBlockedUsersError(message: String) {
        blockedUsersResult = Result.failure(Exception(message))
    }

    fun setIncomingRequestsSuccess(requests: List<ContactRequest>) {
        incomingRequestsResult = Result.success(requests)
    }

    fun setIncomingRequestsError(message: String) {
        incomingRequestsResult = Result.failure(Exception(message))
    }

    fun setOutgoingRequestsSuccess(requests: List<ContactRequest>) {
        outgoingRequestsResult = Result.success(requests)
    }

    fun setOutgoingRequestsError(message: String) {
        outgoingRequestsResult = Result.failure(Exception(message))
    }

    fun setSendContactRequestSuccess() {
        sendContactRequestResult = Result.success(Unit)
    }

    fun setSendContactRequestError(message: String) {
        sendContactRequestResult = Result.failure(Exception(message))
    }

    fun setAcceptContactRequestSuccess() {
        acceptContactRequestResult = Result.success(Unit)
    }

    fun setAcceptContactRequestError(message: String) {
        acceptContactRequestResult = Result.failure(Exception(message))
    }

    fun setDeclineContactRequestSuccess() {
        declineContactRequestResult = Result.success(Unit)
    }

    fun setDeclineContactRequestError(message: String) {
        declineContactRequestResult = Result.failure(Exception(message))
    }

    fun setBlockUserSuccess() {
        blockUserResult = Result.success(Unit)
    }

    fun setBlockUserError(message: String) {
        blockUserResult = Result.failure(Exception(message))
    }

    fun setUnblockUserSuccess() {
        unblockUserResult = Result.success(Unit)
    }

    fun setUnblockUserError(message: String) {
        unblockUserResult = Result.failure(Exception(message))
    }

    fun setAddToFavoritesSuccess() {
        addToFavoritesResult = Result.success(Unit)
    }

    fun setAddToFavoritesError(message: String) {
        addToFavoritesResult = Result.failure(Exception(message))
    }

    fun setRemoveContactSuccess() {
        removeContactResult = Result.success(Unit)
    }

    fun setRemoveContactError(message: String) {
        removeContactResult = Result.failure(Exception(message))
    }

    fun setRemoveFromFavoritesSuccess() {
        removeFromFavoritesResult = Result.success(Unit)
    }

    fun setRemoveFromFavoritesError(message: String) {
        removeFromFavoritesResult = Result.failure(Exception(message))
    }

    fun setGetUserProfileSuccess(user: User) {
        getUserProfileResult = Result.success(user)
    }

    fun setGetUserProfileError(message: String) {
        getUserProfileResult = Result.failure(Exception(message))
    }

    fun setGetUserSuggestionsSuccess(suggestions: List<UserSuggestion>) {
        getUserSuggestionsResult = Result.success(suggestions)
    }

    fun setGetUserSuggestionsError(message: String) {
        getUserSuggestionsResult = Result.failure(Exception(message))
    }

    override suspend fun getContacts(): Result<List<Contact>> = contactsResult

    override suspend fun getFavorites(): Result<List<Contact>> = favoritesResult

    override suspend fun getBlockedUsers(): Result<List<Contact>> = blockedUsersResult

    override suspend fun getIncomingRequests(): Result<List<ContactRequest>> = incomingRequestsResult

    override suspend fun getOutgoingRequests(): Result<List<ContactRequest>> = outgoingRequestsResult

    override suspend fun sendContactRequest(userId: String): Result<Unit> = sendContactRequestResult

    override suspend fun acceptContactRequest(requestId: String): Result<Unit> = acceptContactRequestResult

    override suspend fun declineContactRequest(requestId: String): Result<Unit> = declineContactRequestResult

    override suspend fun removeContact(contactId: String): Result<Unit> = removeContactResult

    override suspend fun blockUser(userId: String): Result<Unit> = blockUserResult

    override suspend fun unblockUser(userId: String): Result<Unit> = unblockUserResult

    override suspend fun addToFavorites(userId: String): Result<Unit> = addToFavoritesResult

    override suspend fun removeFromFavorites(userId: String): Result<Unit> = removeFromFavoritesResult

    override suspend fun getUserProfile(userId: String): Result<User> = getUserProfileResult

    override suspend fun getUserSuggestions(page: Int, limit: Int): Result<List<UserSuggestion>> =
        getUserSuggestionsResult
}
