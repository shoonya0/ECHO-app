package com.shoonya.echo.features.contacts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.core.presentation.InitialsAvatar
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.RequestDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    state: ContactsUiState? = null,
    onEvent: ((ContactsEvent) -> Unit)? = null,
    onContactClicked: (String) -> Unit,
    onRequestsClicked: () -> Unit,
    onSuggestionsClicked: () -> Unit = {},
    onRefresh: () -> Unit = {
        if (onEvent != null) onEvent(ContactsEvent.Refresh)
        else viewModel.onEvent(ContactsEvent.Refresh)
    },
) {
    val viewModelState by viewModel.state.collectAsStateWithLifecycle()
    val effectiveState = state ?: viewModelState

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContactsEvent.ShowSnackbar -> { /* handled by parent scaffold */ }
                else -> {}
            }
        }
    }

    val eventHandler: (ContactsEvent) -> Unit = onEvent ?: viewModel::onEvent

    ContactsListContent(
        contacts = effectiveState.contacts,
        favorites = effectiveState.favorites,
        outgoingRequests = effectiveState.outgoingRequests,
        pendingRequestCount = effectiveState.pendingRequestCount,
        searchQuery = effectiveState.searchQuery,
        isLoading = effectiveState.isLoading,
        error = effectiveState.error,
        actionInFlightUserId = effectiveState.actionInFlightUserId,
        onSearchQueryChanged = { eventHandler(ContactsEvent.SearchQueryChanged(it)) },
        onContactClicked = onContactClicked,
        onRequestsClicked = onRequestsClicked,
        onSuggestionsClicked = onSuggestionsClicked,
        onCancelOutgoingRequest = { eventHandler(ContactsEvent.CancelOutgoingRequest(it)) },
        onToggleFavorite = { eventHandler(ContactsEvent.ToggleFavorite(it)) },
        onRefresh = onRefresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsListContent(
    contacts: List<Contact>,
    favorites: List<Contact> = emptyList(),
    outgoingRequests: List<ContactRequest>,
    pendingRequestCount: Int,
    searchQuery: String,
    isLoading: Boolean,
    error: String?,
    actionInFlightUserId: String?,
    onSearchQueryChanged: (String) -> Unit,
    onContactClicked: (String) -> Unit,
    onRequestsClicked: () -> Unit,
    onSuggestionsClicked: () -> Unit,
    onCancelOutgoingRequest: (String) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onRefresh: () -> Unit,
) {
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    // Build a set of user IDs with outgoing pending requests
    val outgoingRequestIds = remember(outgoingRequests) {
        outgoingRequests.map { it.id }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    IconButton(onClick = onSuggestionsClicked) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Suggestions",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Error banner
                if (error != null && contacts.isEmpty() && favorites.isEmpty()) {
                    item(key = "error") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                // Search bar
                item(key = "search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search contacts...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                // Favorites horizontal row
                if (favorites.isNotEmpty()) {
                    item(key = "favorites_header") {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    item(key = "favorites_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(favorites, key = { "fav_${it.id}" }) { contact ->
                                Column(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable { onContactClicked(contact.id) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box {
                                        InitialsAvatar(
                                            name = contact.displayName,
                                            avatarUrl = contact.avatar,
                                            size = 56.dp,
                                        )
                                        if (contact.presence.isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50), CircleShape),
                                            )
                                        }
                                        // Star unfavorite overlay
                                        IconButton(
                                            onClick = { onToggleFavorite(contact.id) },
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.TopEnd),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = "Remove from favorites",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = contact.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // Pending requests row — always visible, always clickable
                item(key = "requests") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRequestsClicked)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (pendingRequestCount > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$pendingRequestCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pendingRequestCount > 0) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (pendingRequestCount == 1) "1 pending request" else "$pendingRequestCount pending requests",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // All contacts header
                if (contacts.isNotEmpty()) {
                    item(key = "all_contacts_header") {
                        Text(
                            text = "All Contacts",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                }

                // Empty state when no contacts (shown below pending requests row)
                if (contacts.isEmpty() && !isLoading) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No contacts yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                items(filteredContacts, key = { it.id }) { contact ->
                    val isOutgoingPending = contact.id in outgoingRequestIds
                    ContactListItem(
                        contact = contact,
                        isOutgoingPending = isOutgoingPending,
                        isActionInFlight = actionInFlightUserId == contact.id,
                        onCancelOutgoing = { onCancelOutgoingRequest(contact.id) },
                        onToggleFavorite = { onToggleFavorite(contact.id) },
                        onClick = { onContactClicked(contact.id) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    isOutgoingPending: Boolean,
    isActionInFlight: Boolean,
    onCancelOutgoing: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            InitialsAvatar(
                name = contact.displayName,
                avatarUrl = contact.avatar,
                size = 48.dp,
            )
            if (contact.presence.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50), CircleShape),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${contact.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isActionInFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            // Favorite toggle
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (contact.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (contact.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOutgoingPending) {
                // Request sent — show "Request Sent" chip with cancel action
                Button(
                    onClick = onCancelOutgoing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text("Request Sent")
                }
            }
        }
    }
}

// Previews
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactsListPreview() {
    EchoTheme {
        ContactsListContent(
            contacts = listOf(
                Contact("1", "alice", "Alice", "", Presence(PresenceStatus.ONLINE, true, null), statusMessage = "", isFavorite = false),
                Contact("2", "bob", "Bob", "", Presence(PresenceStatus.OFFLINE, false, null), statusMessage = "", isFavorite = false),
            ),
            favorites = listOf(
                Contact("1", "alice", "Alice", "", Presence(PresenceStatus.ONLINE, true, null), statusMessage = "", isFavorite = true),
            ),
            outgoingRequests = listOf(
                ContactRequest("3", "charlie", "Charlie", "", RequestDirection.OUTGOING),
            ),
            pendingRequestCount = 3,
            searchQuery = "",
            isLoading = false,
            error = null,
            actionInFlightUserId = null,
            onSearchQueryChanged = {},
            onContactClicked = {},
            onRequestsClicked = {},
            onSuggestionsClicked = {},
            onCancelOutgoingRequest = {},
            onToggleFavorite = {},
            onRefresh = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Composable
private fun ContactsEmptyPreview() {
    EchoTheme {
        ContactsListContent(
            contacts = emptyList(),
            favorites = emptyList(),
            outgoingRequests = emptyList(),
            pendingRequestCount = 0,
            searchQuery = "",
            isLoading = false,
            error = null,
            actionInFlightUserId = null,
            onSearchQueryChanged = {},
            onContactClicked = {},
            onRequestsClicked = {},
            onSuggestionsClicked = {},
            onCancelOutgoingRequest = {},
            onToggleFavorite = {},
            onRefresh = {},
        )
    }
}