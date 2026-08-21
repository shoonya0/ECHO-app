package com.shoonya.echo.features.contacts.presentation

import com.shoonya.echo.features.contacts.domain.model.UserSuggestion

data class SuggestionsUiState(
    val isLoading: Boolean = false,
    val suggestions: List<UserSuggestion> = emptyList(),
    val error: String? = null,
    val actionInFlightUserId: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)