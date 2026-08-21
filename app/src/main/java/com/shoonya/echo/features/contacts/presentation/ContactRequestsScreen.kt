package com.shoonya.echo.features.contacts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.presentation.InitialsAvatar
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.model.RequestDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactRequestsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContactsEvent.ShowSnackbar -> { /* handled by parent */ }
                else -> {}
            }
        }
    }

    ContactRequestsContent(
        incomingRequests = state.incomingRequests,
        outgoingRequests = state.outgoingRequests,
        actionInFlightUserId = state.actionInFlightUserId,
        onAcceptRequest = { viewModel.onEvent(ContactsEvent.AcceptRequest(it)) },
        onDeclineRequest = { viewModel.onEvent(ContactsEvent.DeclineRequest(it)) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactRequestsContent(
    incomingRequests: List<ContactRequest>,
    outgoingRequests: List<ContactRequest>,
    actionInFlightUserId: String?,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text("Incoming (${incomingRequests.size})")
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text("Outgoing (${outgoingRequests.size})")
                    },
                )
            }

            when (selectedTab) {
                0 -> {
                    if (incomingRequests.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No incoming requests",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn {
                            items(incomingRequests, key = { "in_${it.id}" }) { request ->
                                IncomingRequestItem(
                                    request = request,
                                    isActionInFlight = actionInFlightUserId == request.id,
                                    onAccept = { onAcceptRequest(request.id) },
                                    onDecline = { onDeclineRequest(request.id) },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (outgoingRequests.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No outgoing requests",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn {
                            items(outgoingRequests, key = { "out_${it.id}" }) { request ->
                                OutgoingRequestItem(
                                    request = request,
                                    isActionInFlight = actionInFlightUserId == request.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestItem(
    request: ContactRequest,
    isActionInFlight: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(
            name = request.displayName,
            avatarUrl = request.avatar,
            size = 48.dp,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${request.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Wants to be your contact",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isActionInFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.padding(end = 4.dp)) {
                    Text("Decline")
                }
                Button(onClick = onAccept) {
                    Text("Accept")
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestItem(
    request: ContactRequest,
    isActionInFlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(
            name = request.displayName,
            avatarUrl = request.avatar,
            size = 48.dp,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${request.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Request sent — pending",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isActionInFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Button(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text("Pending")
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactRequestsPreview() {
    EchoTheme {
        ContactRequestsContent(
            incomingRequests = listOf(
                ContactRequest("1", "alice", "Alice", "", RequestDirection.INCOMING),
                ContactRequest("2", "bob", "Bob", "", RequestDirection.INCOMING),
            ),
            outgoingRequests = listOf(
                ContactRequest("3", "charlie", "Charlie", "", RequestDirection.OUTGOING),
            ),
            actionInFlightUserId = null,
            onAcceptRequest = {},
            onDeclineRequest = {},
            onBack = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Composable
private fun ContactRequestsEmptyPreview() {
    EchoTheme {
        ContactRequestsContent(
            incomingRequests = emptyList(),
            outgoingRequests = emptyList(),
            actionInFlightUserId = null,
            onAcceptRequest = {},
            onDeclineRequest = {},
            onBack = {},
        )
    }
}