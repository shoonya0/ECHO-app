package com.shoonya.echo.features.contacts.presentation

sealed interface ContactsEvent {
    data object Refresh : ContactsEvent
    data class SearchQueryChanged(val query: String) : ContactsEvent
    data class ToggleFavorite(val contactId: String) : ContactsEvent
    data class BlockUser(val userId: String) : ContactsEvent
    data class UnblockUser(val userId: String) : ContactsEvent
    data class AcceptRequest(val requestId: String) : ContactsEvent
    data class DeclineRequest(val requestId: String) : ContactsEvent
    data class CancelOutgoingRequest(val requestId: String) : ContactsEvent
    data class SendContactRequest(val userId: String) : ContactsEvent
    data class RemoveContact(val contactId: String) : ContactsEvent
    data class LoadUserProfile(val userId: String) : ContactsEvent
    data object ClearViewedProfile : ContactsEvent
    data class ShowSnackbar(val message: String) : ContactsEvent
}