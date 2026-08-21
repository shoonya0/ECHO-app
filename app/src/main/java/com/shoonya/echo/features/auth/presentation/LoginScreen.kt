package com.shoonya.echo.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoonya.echo.core.theme.EchoTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onNavigateToHome()
                is LoginEvent.NavigateToLogin -> { /* handled internally via mode toggle */ }
                is LoginEvent.ShowSnackbar -> { /* snackbar handled by parent scaffold */ }
            }
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }

    LoginContent(
        email = state.email,
        password = state.password,
        username = state.username,
        isLoginMode = state.isLoginMode,
        isLoading = state.isLoading,
        error = state.error,
        emailError = state.emailError,
        passwordError = state.passwordError,
        usernameError = state.usernameError,
        passwordVisible = passwordVisible,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onUsernameChange = viewModel::onUsernameChange,
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        onSubmit = viewModel::onSubmit,
        onToggleMode = viewModel::onToggleMode,
        onClearError = viewModel::clearError,
    )
}

@Composable
internal fun LoginContent(
    email: String,
    password: String,
    username: String,
    isLoginMode: Boolean,
    isLoading: Boolean,
    error: String?,
    emailError: String?,
    passwordError: String?,
    usernameError: String?,
    passwordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onClearError: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isLoginMode) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    isError = usernameError != null,
                    supportingText = usernameError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Log In" else "Sign Up",
                    )
                }
            }

            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Log In",
                )
            }
        }
    }
}

@Preview(name = "Light - Login", showBackground = true)
@Preview(name = "Dark - Login", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginContentPreview() {
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

@Preview(name = "Light - Signup", showBackground = true)
@Composable
private fun SignupContentPreview() {
    EchoTheme {
        LoginContent(
            email = "test@echo.com",
            password = "password123",
            username = "testuser",
            isLoginMode = false,
            isLoading = false,
            error = null,
            emailError = null,
            passwordError = null,
            usernameError = null,
            passwordVisible = true,
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

@Preview(name = "Light - Error", showBackground = true)
@Composable
private fun LoginErrorPreview() {
    EchoTheme {
        LoginContent(
            email = "bad@email",
            password = "123",
            username = "",
            isLoginMode = true,
            isLoading = false,
            error = "Invalid credentials",
            emailError = null,
            passwordError = "Password must be at least 8 characters",
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