package com.shoonya.echo.features.contacts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.presentation.InitialsAvatar
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    viewModel: SuggestionsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUserClicked: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SuggestionsEvent.ShowSnackbar -> { /* handled by parent */ }
                else -> {}
            }
        }
    }

    SuggestionsContent(
        suggestions = state.suggestions,
        isLoading = state.isLoading,
        error = state.error,
        actionInFlightUserId = state.actionInFlightUserId,
        isLoadingMore = state.isLoadingMore,
        hasMore = state.hasMore,
        onSendRequest = { viewModel.onEvent(SuggestionsEvent.SendRequest(it)) },
        onLoadMore = { viewModel.onEvent(SuggestionsEvent.LoadMore) },
        onRefresh = { viewModel.onEvent(SuggestionsEvent.Refresh) },
        onBack = onBack,
        onUserClicked = onUserClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionsContent(
    suggestions: List<UserSuggestion>,
    isLoading: Boolean,
    error: String?,
    actionInFlightUserId: String?,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onSendRequest: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onUserClicked: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggestions") },
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
        if (isLoading && suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // Friend Suggestions section
                if (suggestions.isNotEmpty()) {
                    item(key = "suggestions_header") {
                        Text(
                            text = "Friend Suggestions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                        )
                    }

                    items(suggestions, key = { "sug_${it.id}" }) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            isActionInFlight = actionInFlightUserId == suggestion.id,
                            onSendRequest = { onSendRequest(suggestion.id) },
                            onClick = { onUserClicked(suggestion.id) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                        )
                    }

                    // Load more indicator
                    if (hasMore) {
                        item(key = "load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLoadingMore) {
                                        if (!isLoadingMore) onLoadMore()
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = "Load more",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state when nothing at all
                if (suggestions.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No suggestions available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: UserSuggestion,
    isActionInFlight: Boolean,
    onSendRequest: () -> Unit,
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
        // Avatar
        InitialsAvatar(
            name = suggestion.displayName,
            avatarUrl = suggestion.avatar,
            size = 48.dp,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${suggestion.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (suggestion.statusMessage.isNotBlank()) {
                Text(
                    text = suggestion.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Send request button
        if (isActionInFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(
                onClick = onSendRequest,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Send request",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// Previews
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuggestionsPreview() {
    EchoTheme {
        SuggestionsContent(
            suggestions = listOf(
                UserSuggestion("3", "charlie", "Charlie", "", "Hey there!", ""),
                UserSuggestion("4", "diana", "Diana", "", "", ""),
            ),
            isLoading = false,
            error = null,
            actionInFlightUserId = null,
            isLoadingMore = false,
            hasMore = true,
            onSendRequest = {},
            onLoadMore = {},
            onRefresh = {},
            onBack = {},
            onUserClicked = {},
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Composable
private fun SuggestionsEmptyPreview() {
    EchoTheme {
        SuggestionsContent(
            suggestions = emptyList(),
            isLoading = false,
            error = null,
            actionInFlightUserId = null,
            isLoadingMore = false,
            hasMore = false,
            onSendRequest = {},
            onLoadMore = {},
            onRefresh = {},
            onBack = {},
            onUserClicked = {},
        )
    }
}