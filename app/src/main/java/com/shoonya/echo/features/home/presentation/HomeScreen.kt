package com.shoonya.echo.features.home.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoonya.echo.Routes
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListScreen
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListViewModel
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListCrossFeatureEvent
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListEvent
import com.shoonya.echo.features.chat.presentation.chatlist.ChatListCrossFeatureEvent.PresenceUpdate as WsPresenceUpdate
import com.shoonya.echo.features.contacts.presentation.ContactsEvent
import com.shoonya.echo.features.contacts.presentation.ContactsListScreen
import com.shoonya.echo.features.contacts.presentation.ContactsViewModel
import com.shoonya.echo.features.settings.presentation.SettingsScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("home/chats", "Chats", Icons.Default.Chat),
    BottomNavItem("home/contacts", "Contacts", Icons.Default.People),
    BottomNavItem("home/settings", "Settings", Icons.Default.Settings),
)

@Composable
fun HomeScreen(
    rootNavController: NavHostController,
    onLogout: () -> Unit,
) {
    val nestedNavController = rememberNavController()
    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val contactsState by contactsViewModel.state.collectAsStateWithLifecycle()
    val chatListViewModel: ChatListViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh contacts whenever Home resumes (e.g., returning from ContactRequestsScreen,
    // UserProfileScreen, SuggestionsScreen) so that newly accepted friends appear
    // in both the Contacts tab and the Chats tab.
    LifecycleResumeEffect(Unit) {
        contactsViewModel.onEvent(ContactsEvent.Refresh)
        onPauseOrDispose { /* no-op */ }
    }

    // Collect chat navigation events
    androidx.compose.runtime.LaunchedEffect(Unit) {
        chatListViewModel.events.collect { event ->
            when (event) {
                is ChatListEvent.NavigateToChat -> {
                    rootNavController.navigate(Routes.chat(event.chatId))
                }
                is ChatListEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Surface contacts feature messages (e.g. favourite toggle failures)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        contactsViewModel.events.collect { event ->
            when (event) {
                is ContactsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> { /* no-op */ }
            }
        }
    }

    // Forward presence updates from WS (via ChatListViewModel) to ContactsViewModel
    androidx.compose.runtime.LaunchedEffect(Unit) {
        chatListViewModel.crossFeatureEvents.collect { event ->
            when (event) {
                is WsPresenceUpdate -> {
                    contactsViewModel.onPresenceUpdate(
                        userId = event.userId,
                        isOnline = event.status == com.shoonya.echo.features.chat.domain.model.PresenceStatus.ONLINE,
                    )
                }
            }
        }
    }

    // Derived helpers
    val favoriteContacts = contactsState.favorites

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            nestedNavController.navigate(item.route) {
                                popUpTo(nestedNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = nestedNavController,
            startDestination = "home/contacts",
            modifier = Modifier.padding(paddingValues),
        ) {
            composable("home/contacts") {
                ContactsListScreen(
                    state = contactsState,
                    onEvent = contactsViewModel::onEvent,
                    onContactClicked = { userId ->
                        rootNavController.navigate(Routes.userProfile(userId))
                    },
                    onRequestsClicked = {
                        rootNavController.navigate(Routes.CONTACT_REQUESTS)
                    },
                    onSuggestionsClicked = {
                        rootNavController.navigate(Routes.SUGGESTIONS)
                    },
                )
            }

            composable("home/chats") {
                ChatListScreen(
                    contactIds = contactsState.contacts.map { it.id },
                    favorites = favoriteContacts,
                    onToggleFavorite = { contactId ->
                        contactsViewModel.onEvent(ContactsEvent.ToggleFavorite(contactId))
                    },
                    viewModel = chatListViewModel,
                    onChatClicked = { chatId -> handleChatClick(chatId, chatListViewModel, rootNavController) },
                    onSuggestionsClicked = {
                        rootNavController.navigate(Routes.SUGGESTIONS)
                    },
                )
            }

            composable("home/settings") {
                SettingsScreen(
                    onEditProfile = {
                        rootNavController.navigate(Routes.EDIT_PROFILE)
                    },
                    onLogout = onLogout,
                )
            }
        }
    }
}

private fun handleChatClick(
    chatId: String,
    chatListViewModel: ChatListViewModel,
    navController: NavHostController,
) {
    if (chatId.startsWith("new:")) {
        val userId = chatId.removePrefix("new:")
        chatListViewModel.createDirectChat(userId)
    } else {
        navController.navigate(Routes.chat(chatId))
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    EchoTheme {
        HomeScreen(
            rootNavController = rememberNavController(),
            onLogout = {},
        )
    }
}