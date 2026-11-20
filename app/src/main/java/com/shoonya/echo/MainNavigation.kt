package com.shoonya.echo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shoonya.echo.core.data.remote.SessionManager
import com.shoonya.echo.core.domain.model.SessionEndReason
import com.shoonya.echo.features.auth.presentation.LoginScreen
import com.shoonya.echo.features.auth.presentation.LoginViewModel
import com.shoonya.echo.features.auth.presentation.SplashScreen
import com.shoonya.echo.features.chat.presentation.chatdetail.ChatDetailScreen
import com.shoonya.echo.features.contacts.presentation.ContactRequestsScreen
import com.shoonya.echo.features.contacts.presentation.SuggestionsScreen
import com.shoonya.echo.features.contacts.presentation.UserProfileScreen
import com.shoonya.echo.features.home.presentation.HomeScreen
import com.shoonya.echo.features.settings.presentation.EditProfileScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "auth/login"
    private const val LOGIN_REASON = LoginViewModel.ARG_SESSION_END_REASON

    /** The Login destination; `navigate(LOGIN)` matches it, since the reason is optional. */
    const val LOGIN_PATTERN = "$LOGIN?$LOGIN_REASON={$LOGIN_REASON}"
    const val HOME = "home"
    const val CHAT = "chat/{chatId}"
    const val CONTACT_REQUESTS = "contacts/requests"
    const val USER_PROFILE = "user/{userId}"
    const val SUGGESTIONS = "suggestions"
    const val EDIT_PROFILE = "settings/edit-profile"

    /** Login after the server ended the session, telling the screen why. */
    fun login(reason: SessionEndReason): String = "$LOGIN?$LOGIN_REASON=${reason.name}"

    fun chat(chatId: String): String = "chat/$chatId"
    fun userProfile(userId: String): String = "user/$userId"
}

@Composable
fun EchoNavHost(
    navController: NavHostController = rememberNavController(),
    sessionManager: SessionManager? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(sessionManager) {
        sessionManager?.sessionExpired?.collect { reason ->
            navController.navigate(Routes.login(reason)) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.LOGIN_PATTERN,
            arguments = listOf(
                navArgument(LoginViewModel.ARG_SESSION_END_REASON) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN_PATTERN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                rootNavController = navController,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONTACT_REQUESTS) {
            ContactRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onSendMessage = { contactUserId ->
                    navController.navigate(Routes.chat("new:$contactUserId"))
                }
            )
        }

        composable(Routes.SUGGESTIONS) {
            SuggestionsScreen(
                onBack = { navController.popBackStack() },
                onUserClicked = { userId ->
                    navController.navigate(Routes.userProfile(userId))
                }
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
