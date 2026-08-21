package com.shoonya.echo.features.contacts.presentation

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<Contact> = emptyList(),
    val favorites: List<Contact> = emptyList(),
    val blockedUsers: List<Contact> = emptyList(),
    val incomingRequests: List<ContactRequest> = emptyList(),
    val outgoingRequests: List<ContactRequest> = emptyList(),
    val viewedProfile: User? = null,
    val isProfileLoading: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
    val actionInFlightUserId: String? = null,
) {
    val pendingRequestCount: Int get() = incomingRequests.size + outgoingRequests.size
}