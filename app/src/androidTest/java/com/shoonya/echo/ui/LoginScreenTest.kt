package com.shoonya.echo.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    onClearError = {},
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
                    onClearError = {},
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
                    onClearError = {},
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
}