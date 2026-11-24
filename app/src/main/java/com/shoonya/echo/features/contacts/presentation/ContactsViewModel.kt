package com.shoonya.echo.features.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.core.util.ErrorMapper.toUserMessage
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.usecase.AcceptContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.AddToFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.BlockUserUseCase
import com.shoonya.echo.features.contacts.domain.usecase.DeclineContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetBlockedUsersUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetContactsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetIncomingRequestsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetOutgoingRequestsUseCase
import com.shoonya.echo.features.contacts.domain.usecase.GetUserProfileUseCase
import com.shoonya.echo.features.contacts.domain.usecase.RemoveContactUseCase
import com.shoonya.echo.features.contacts.domain.usecase.RemoveFromFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.SendContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.UnblockUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val getIncomingRequestsUseCase: GetIncomingRequestsUseCase,
    private val getOutgoingRequestsUseCase: GetOutgoingRequestsUseCase,
    private val sendContactRequestUseCase: SendContactRequestUseCase,
    private val acceptContactRequestUseCase: AcceptContactRequestUseCase,
    private val declineContactRequestUseCase: DeclineContactRequestUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val addToFavoritesUseCase: AddToFavoritesUseCase,
    private val removeContactUseCase: RemoveContactUseCase,
    private val removeFromFavoritesUseCase: RemoveFromFavoritesUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ContactsEvent>()
    val events: SharedFlow<ContactsEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null
    private var profileJob: Job? = null
    private var skipNextResume = true

    init {
        loadAll()
    }

    fun onEvent(event: ContactsEvent) {
        when (event) {
            is ContactsEvent.Refresh -> loadAll()
            is ContactsEvent.ScreenResumed -> onScreenResumed()
            is ContactsEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            is ContactsEvent.ToggleFavorite -> toggleFavorite(event.contactId)
            is ContactsEvent.BlockUser -> blockUser(event.userId)
            is ContactsEvent.UnblockUser -> unblockUser(event.userId)
            is ContactsEvent.AcceptRequest -> acceptRequest(event.requestId)
            is ContactsEvent.DeclineRequest -> declineRequest(event.requestId)
            is ContactsEvent.CancelOutgoingRequest -> cancelOutgoingRequest(event.requestId)
            is ContactsEvent.SendContactRequest -> sendContactRequest(event.userId)
            is ContactsEvent.RemoveContact -> removeContact(event.contactId)
            is ContactsEvent.LoadUserProfile -> loadUserProfile(event.userId)
            is ContactsEvent.ClearViewedProfile -> _state.update { it.copy(viewedProfile = null) }
            is ContactsEvent.ShowSnackbar -> { /* emitted by ViewModel internally */ }
        }
    }

    /**
     * Home shares this ViewModel with the profile and requests screens (#44), so their
     * actions already update this state. A resume therefore refreshes only the lists that
     * can change without the app: contacts (a peer accepts, removes or blocks you; presence
     * moves) and the two request lists. Favorites and blocked users are not re-fetched;
     * this ViewModel keeps them from its own actions.
     * The first resume is skipped (init has just loaded them), and a resume while a load
     * is still running joins it instead of starting another (#29).
     */
    private fun onScreenResumed() {
        if (skipNextResume) {
            skipNextResume = false
            return
        }
        if (loadJob?.isActive == true) return
        refreshOnResume()
    }

    /**
     * Background refresh of the lists that can change server-side, without the full-screen
     * loading state: it runs on every resume at Home, where the contacts list is already on
     * screen. Favorites are not re-fetched (#44); the current list still decides the
     * favorite flag, since the contacts endpoint may not populate it.
     */
    private fun refreshOnResume() {
        loadJob = viewModelScope.launch {
            runCatching {
                val rawContacts = getContactsUseCase().getOrThrow()
                val incoming = getIncomingRequestsUseCase().getOrThrow()
                val outgoing = getOutgoingRequestsUseCase().getOrThrow()
                _state.update { state ->
                    val favoriteIds = state.favorites.map { it.id }.toSet()
                    state.copy(
                        contacts = rawContacts.map {
                            if (it.id in favoriteIds) it.copy(isFavorite = true) else it
                        },
                        incomingRequests = incoming,
                        outgoingRequests = outgoing
                    )
                }
            }.onFailure { err ->
                Timber.tag("ContactsVM").e(err, "refreshOnResume failed")
            }
        }
    }

    private fun loadAll() {
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val rawContacts = getContactsUseCase().getOrThrow()
                val favorites = getFavoritesUseCase().getOrThrow()
                val blocked = getBlockedUsersUseCase().getOrThrow()
                val incoming = getIncomingRequestsUseCase().getOrThrow()
                val outgoing = getOutgoingRequestsUseCase().getOrThrow()
                // Merge favorite status into contacts list — the contacts endpoint
                // may not reliably populate isFavorite for each contact, so we
                // derive it from the actual favorites list.
                val favoriteIds = favorites.map { it.id }.toSet()
                val contacts = rawContacts.map {
                    if (it.id in favoriteIds) it.copy(isFavorite = true) else it
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        contacts = contacts,
                        favorites = favorites,
                        blockedUsers = blocked,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing
                    )
                }
            }.onFailure { err ->
                Timber.tag("ContactsVM").e(err, "loadAll failed")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = err.toUserMessage()
                    )
                }
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    private fun toggleFavorite(identifier: String) {
        val currentState = _state.value
        // Derive favorite status from the favorites list (authoritative source),
        // not from contact.isFavorite which may be stale if the contacts endpoint
        // doesn't reliably populate that field.
        val favoriteIds = currentState.favorites.map { it.id }.toSet()
        val contact = currentState.contacts.find { it.id == identifier }
            ?: currentState.favorites.find { it.id == identifier }
            ?: currentState.contacts.find { it.username == identifier }
            ?: currentState.favorites.find { it.username == identifier }
            ?: Contact(
                id = identifier,
                username = identifier,
                displayName = identifier,
                avatar = "",
                presence = Presence(PresenceStatus.OFFLINE, false, null),
                isFavorite = false
            )
        val contactRecordId = contact.id
        val isCurrentlyFavorite = contactRecordId in favoriteIds
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = contactRecordId) }
            val result = if (isCurrentlyFavorite) {
                removeFromFavoritesUseCase(contactRecordId)
            } else {
                addToFavoritesUseCase(contactRecordId)
            }
            result.onSuccess {
                _state.update { state ->
                    val newIsFavorite = !isCurrentlyFavorite
                    state.copy(
                        contacts = state.contacts.map {
                            if (it.id ==
                                contactRecordId
                            ) {
                                it.copy(isFavorite = newIsFavorite)
                            } else {
                                it
                            }
                        },
                        favorites = if (isCurrentlyFavorite) {
                            state.favorites.filter { it.id != contactRecordId }
                        } else {
                            state.favorites + contact.copy(isFavorite = true)
                        }
                    )
                }
            }.onFailure { err ->
                Timber.tag("ContactsVM").e(err, "toggleFavorite failed: %s", identifier)
                _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
            }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun blockUser(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = userId) }
            blockUserUseCase(userId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            contacts = state.contacts.filter { it.id != userId },
                            favorites = state.favorites.filter { it.id != userId }
                        )
                    }
                    _events.emit(ContactsEvent.ShowSnackbar("User blocked"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "blockUser failed: %s", userId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun unblockUser(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = userId) }
            unblockUserUseCase(userId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            blockedUsers = it.blockedUsers.filter { b ->
                                b.id !=
                                    userId
                            }
                        )
                    }
                    loadAll()
                    _events.emit(ContactsEvent.ShowSnackbar("User unblocked"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "unblockUser failed: %s", userId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = requestId) }
            acceptContactRequestUseCase(requestId)
                .onSuccess {
                    loadAll()
                    _events.emit(ContactsEvent.ShowSnackbar("Request accepted"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "acceptRequest failed: %s", requestId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun declineRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = requestId) }
            declineContactRequestUseCase(requestId)
                .onSuccess {
                    loadAll()
                    _events.emit(ContactsEvent.ShowSnackbar("Request declined"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "declineRequest failed: %s", requestId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun cancelOutgoingRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = requestId) }
            declineContactRequestUseCase(requestId)
                .onSuccess {
                    loadAll()
                    _events.emit(ContactsEvent.ShowSnackbar("Request cancelled"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "cancelOutgoingRequest failed: %s", requestId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun sendContactRequest(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = userId) }
            sendContactRequestUseCase(userId)
                .onSuccess {
                    _state.update { it.copy(viewedProfile = null) }
                    _events.emit(ContactsEvent.ShowSnackbar("Contact request sent"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "sendContactRequest failed: %s", userId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun removeContact(contactId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionInFlightUserId = contactId) }
            removeContactUseCase(contactId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            contacts = state.contacts.filter { it.id != contactId },
                            favorites = state.favorites.filter { it.id != contactId },
                            viewedProfile = null
                        )
                    }
                    _events.emit(ContactsEvent.ShowSnackbar("Contact removed"))
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "removeContact failed: %s", contactId)
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
            _state.update { it.copy(actionInFlightUserId = null) }
        }
    }

    private fun loadUserProfile(userId: String) {
        // One profile at a time: this ViewModel is shared with Home (#44), so a late reply
        // for the previous profile must never land on the profile now on screen.
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            // Clear the previous user first, so a failed load cannot leave someone else's
            // profile on screen.
            _state.update { it.copy(isProfileLoading = true, viewedProfile = null) }
            val result = getUserProfileUseCase(userId)
            // Cancelling a coroutine awaiting the repository call makes runCatching return
            // a failure instead of throwing, so check the job before either branch writes.
            if (!isActive) return@launch
            result
                .onSuccess { user ->
                    _state.update { it.copy(isProfileLoading = false, viewedProfile = user) }
                }
                .onFailure { err ->
                    Timber.tag("ContactsVM").e(err, "loadUserProfile failed: %s", userId)
                    _state.update { it.copy(isProfileLoading = false) }
                    _events.emit(ContactsEvent.ShowSnackbar(err.toUserMessage()))
                }
        }
    }

    fun onPresenceUpdate(userId: String, isOnline: Boolean) {
        _state.update { state ->
            state.copy(
                contacts = state.contacts.map { contact ->
                    if (contact.id == userId) {
                        contact.copy(presence = contact.presence.copy(isOnline = isOnline))
                    } else {
                        contact
                    }
                },
                favorites = state.favorites.map { contact ->
                    if (contact.id == userId) {
                        contact.copy(presence = contact.presence.copy(isOnline = isOnline))
                    } else {
                        contact
                    }
                }
            )
        }
    }
}
