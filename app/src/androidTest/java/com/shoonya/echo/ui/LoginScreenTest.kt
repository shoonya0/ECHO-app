package com.shoonya.echo.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.shoonya.echo.R
import com.shoonya.echo.core.domain.model.SessionEndReason
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.features.auth.presentation.LoginContent
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenEmptyFields_loginButtonIsShown() {
        composeTestRule.setContent {
            EchoTheme {
                LoginContent(
                    email = "",
                    password = "",
                    username = "",
                    isLoginMode = true,
                    isLoading = false,
                    error = null,
                    emailError = null,
                    passwordError = null,
                    usernameError = null,
                    passwordVisible = false,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onUsernameChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Log In")
            .assertIsDisplayed()
    }

    @Test
    fun givenErrorState_errorMessageIsDisplayed() {
        composeTestRule.setContent {
            EchoTheme {
                LoginContent(
                    email = "test@echo.com",
                    password = "password123",
                    username = "",
                    isLoginMode = true,
                    isLoading = false,
                    error = "Invalid credentials",
                    emailError = null,
                    passwordError = null,
                    usernameError = null,
                    passwordVisible = false,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onUsernameChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Invalid credentials")
            .assertIsDisplayed()
    }

    @Test
    fun givenSignupMode_showsCreateAccountAndUsernameField() {
        composeTestRule.setContent {
            EchoTheme {
                LoginContent(
                    email = "new@echo.com",
                    password = "password123",
                    username = "newuser",
                    isLoginMode = false,
                    isLoading = false,
                    error = null,
                    emailError = null,
                    passwordError = null,
                    usernameError = null,
                    passwordVisible = false,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onUsernameChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Create Account")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Sign Up")
            .assertIsDisplayed()
    }

    private fun setLoginContent(reason: SessionEndReason?) {
        composeTestRule.setContent {
            EchoTheme {
                LoginContent(
                    email = "",
                    password = "",
                    username = "",
                    isLoginMode = true,
                    isLoading = false,
                    error = null,
                    emailError = null,
                    passwordError = null,
                    usernameError = null,
                    passwordVisible = false,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onUsernameChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onClearError = {},
                    sessionEndReason = reason
                )
            }
        }
    }

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun givenBannedAccount_bannedMessageIsDisplayed() {
        setLoginContent(SessionEndReason.ACCOUNT_BANNED)

        composeTestRule
            .onNodeWithText(string(R.string.session_end_account_banned))
            .assertIsDisplayed()
    }

    @Test
    fun givenInactiveAccount_inactiveMessageIsDisplayed() {
        setLoginContent(SessionEndReason.ACCOUNT_INACTIVE)

        composeTestRule
            .onNodeWithText(string(R.string.session_end_account_inactive))
            .assertIsDisplayed()
    }

    @Test
    fun givenExpiredSession_noAccountMessageIsDisplayed() {
        setLoginContent(SessionEndReason.EXPIRED)

        composeTestRule
            .onAllNodesWithText(string(R.string.session_end_account_banned))
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText(string(R.string.session_end_account_inactive))
            .assertCountEquals(0)
    }
}
