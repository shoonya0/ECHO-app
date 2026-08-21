package com.shoonya.echo.features.contacts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.domain.model.Presence
import com.shoonya.echo.core.domain.model.PresenceStatus
import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.core.presentation.InitialsAvatar
import com.shoonya.echo.core.theme.EchoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    viewModel: ContactsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.onEvent(ContactsEvent.LoadUserProfile(userId))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContactsEvent.ShowSnackbar -> { /* handled by parent */ }
                else -> {}
            }
        }
    }

    val profile = state.viewedProfile
    val isContact = profile != null &&
        state.contacts.any { it.id == profile.id }
    val isOutgoingPending = profile != null &&
        state.outgoingRequests.any { it.username == profile.username }
    val isFavorite = profile != null && (
        state.favorites.any { it.id == profile.id } ||
            state.contacts.any { it.id == profile.id && it.isFavorite }
    )

    UserProfileContent(
        profile = profile,
        isLoading = state.isProfileLoading,
        isActionInFlight = state.actionInFlightUserId != null,
        isContact = isContact,
        isOutgoingPending = isOutgoingPending,
        isFavorite = isFavorite,
        onBack = onBack,
        onSendMessage = { profile?.let { onSendMessage(it.id) } },
        onAddContact = { profile?.let { viewModel.onEvent(ContactsEvent.SendContactRequest(it.id)) } },
        onRemoveContact = { profile?.let { viewModel.onEvent(ContactsEvent.RemoveContact(it.id)) } },
        onCancelOutgoingRequest = {
            val outgoingReq = state.outgoingRequests.find { it.username == profile?.username }
            val requestId = outgoingReq?.id ?: profile?.id ?: return@UserProfileContent
            viewModel.onEvent(ContactsEvent.CancelOutgoingRequest(requestId))
        },
        onToggleFavorite = { profile?.let { viewModel.onEvent(ContactsEvent.ToggleFavorite(it.id)) } },
        onBlockUser = { profile?.let { viewModel.onEvent(ContactsEvent.BlockUser(it.id)) } },
        onUnblockUser = { profile?.let { viewModel.onEvent(ContactsEvent.UnblockUser(it.id)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileContent(
    profile: User?,
    isLoading: Boolean,
    isActionInFlight: Boolean,
    isContact: Boolean = false,
    isOutgoingPending: Boolean = false,
    isFavorite: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: () -> Unit,
    onAddContact: () -> Unit,
    onRemoveContact: () -> Unit = {},
    onCancelOutgoingRequest: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Contact") },
            text = {
                Text("Are you sure you want to remove ${profile?.displayName ?: "this contact"} from your contacts?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemoveContact()
                    },
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.displayName ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (profile != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            enabled = !isActionInFlight,
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "User not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Avatar
                    Box {
                        InitialsAvatar(
                            name = profile.displayName,
                            avatarUrl = profile.avatar,
                            size = 96.dp,
                        )
                        if (profile.presence.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(
                                        color = Color(0xFF4CAF50),
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    if (profile.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (profile.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (profile.presence.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (profile.presence.isOnline) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSendMessage,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Send Message")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (profile.isBanned) {
                        OutlinedButton(
                            onClick = onUnblockUser,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionInFlight,
                        ) {
                            Text("Unblock")
                        }
                    } else if (isContact) {
                        // Contact exists — show remove option
                        OutlinedButton(
                            onClick = { showRemoveDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionInFlight,
                        ) {
                            Text(
                                text = "Remove Contact",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBlockUser,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionInFlight,
                        ) {
                            Text(
                                text = "Block",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else if (isOutgoingPending) {
                        // Request already sent — show cancel option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onCancelOutgoingRequest,
                                modifier = Modifier.weight(1f),
                                enabled = !isActionInFlight,
                            ) {
                                Text("Cancel Request")
                            }
                            OutlinedButton(
                                onClick = onBlockUser,
                                modifier = Modifier.weight(1f),
                                enabled = !isActionInFlight,
                            ) {
                                Text(
                                    text = "Block",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onAddContact,
                                modifier = Modifier.weight(1f),
                                enabled = !isActionInFlight,
                            ) {
                                Text("Add Contact")
                            }
                            OutlinedButton(
                                onClick = onBlockUser,
                                modifier = Modifier.weight(1f),
                                enabled = !isActionInFlight,
                            ) {
                                Text(
                                    text = "Block",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UserProfilePreview() {
    EchoTheme {
        UserProfileContent(
            profile = User(
                id = "1",
                email = "alice@echo.com",
                username = "alice",
                displayName = "Alice",
                avatar = "",
                statusMessage = "Hey there!",
                bio = "I'm using ECHO",
                presence = Presence(PresenceStatus.ONLINE, true, null),
                isActive = true,
                isVerified = true,
                isBanned = false,
            ),
            isLoading = false,
            isActionInFlight = false,
            onBack = {},
            onSendMessage = {},
            onAddContact = {},
            onBlockUser = {},
            onUnblockUser = {},
        )
    }
}

@Preview(name = "Is Contact Light", showBackground = true)
@Composable
private fun UserProfileIsContactPreview() {
    EchoTheme {
        UserProfileContent(
            profile = User(
                id = "1",
                email = "alice@echo.com",
                username = "alice",
                displayName = "Alice",
                avatar = "",
                statusMessage = "Hey there!",
                bio = "I'm using ECHO",
                presence = Presence(PresenceStatus.ONLINE, true, null),
                isActive = true,
                isVerified = true,
                isBanned = false,
            ),
            isLoading = false,
            isActionInFlight = false,
            isContact = true,
            onBack = {},
            onSendMessage = {},
            onAddContact = {},
            onRemoveContact = {},
            onBlockUser = {},
            onUnblockUser = {},
        )
    }
}

@Preview(name = "Outgoing Request Light", showBackground = true)
@Composable
private fun UserProfileOutgoingPreview() {
    EchoTheme {
        UserProfileContent(
            profile = User(
                id = "2",
                email = "bob@echo.com",
                username = "bob",
                displayName = "Bob",
                avatar = "",
                statusMessage = "",
                bio = "Developer",
                presence = Presence(PresenceStatus.OFFLINE, false, null),
                isActive = true,
                isVerified = false,
                isBanned = false,
            ),
            isLoading = false,
            isActionInFlight = false,
            isOutgoingPending = true,
            onBack = {},
            onSendMessage = {},
            onAddContact = {},
            onCancelOutgoingRequest = {},
            onBlockUser = {},
            onUnblockUser = {},
        )
    }
}

@Preview(name = "Loading Light", showBackground = true)
@Composable
private fun UserProfileLoadingPreview() {
    EchoTheme {
        UserProfileContent(
            profile = null,
            isLoading = true,
            isActionInFlight = false,
            onBack = {},
            onSendMessage = {},
            onAddContact = {},
            onBlockUser = {},
            onUnblockUser = {},
        )
    }
}