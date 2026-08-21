package com.shoonya.echo.features.contacts.presentation

sealed interface SuggestionsEvent {
    data object Load : SuggestionsEvent
    data object LoadMore : SuggestionsEvent
    data class SendRequest(val userId: String) : SuggestionsEvent
    data object Refresh : SuggestionsEvent
    data class ShowSnackbar(val message: String) : SuggestionsEvent
}