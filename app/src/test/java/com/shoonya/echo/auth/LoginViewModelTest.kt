package com.shoonya.echo.auth

import com.shoonya.echo.fakes.FakeAuthRepository
import com.shoonya.echo.features.auth.domain.usecase.LoginUseCase
import com.shoonya.echo.features.auth.domain.usecase.SignupUseCase
import com.shoonya.echo.features.auth.presentation.LoginViewModel
import com.shoonya.echo.fakes.TestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val loginUseCase = LoginUseCase(fakeRepo)
        val signupUseCase = SignupUseCase(fakeRepo)
        viewModel = LoginViewModel(loginUseCase, signupUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given valid credentials, on login, state becomes authenticated`() = runTest {
        fakeRepo.setLoginSuccess(TestData.user, "token")
        viewModel.onEmailChange("test@echo.com")
        viewModel.onPasswordChange("password123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.isAuthenticated)
    }

    @Test
    fun `given invalid credentials, on login, state contains error`() = runTest {
        fakeRepo.setLoginError("Invalid credentials")
        viewModel.onEmailChange("wrong@echo.com")
        viewModel.onPasswordChange("wrongpass")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `given empty email, on login, state contains validation error`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Please enter your email", state.emailError)
    }

    @Test
    fun `given short password, on login, state contains validation error`() = runTest {
        viewModel.onEmailChange("test@echo.com")
        viewModel.onPasswordChange("123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Password must be at least 8 characters", state.passwordError)
    }

    @Test
    fun `given valid signup data, on submit, isSignupSuccess becomes true and mode flips to login`() = runTest {
        fakeRepo.setSignupSuccess()
        viewModel.onToggleMode() // switch to signup mode
        viewModel.onEmailChange("new@echo.com")
        viewModel.onUsernameChange("newuser")
        viewModel.onPasswordChange("password123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isSignupSuccess)
        // After signup, the ViewModel should auto-redirect to login mode
        assertTrue("Expected isLoginMode to be true after signup success", state.isLoginMode)
        // Password should be cleared for security
        assertEquals("", state.password)
        // Username should be cleared (not relevant in login mode)
        assertEquals("", state.username)
    }

    @Test
    fun `given empty username in signup, state contains username error`() = runTest {
        viewModel.onToggleMode() // switch to signup
        viewModel.onEmailChange("new@echo.com")
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("password123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Please enter a username", state.usernameError)
    }
}