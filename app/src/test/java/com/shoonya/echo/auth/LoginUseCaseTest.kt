package com.shoonya.echo.auth

import com.shoonya.echo.fakes.FakeAuthRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {
    private val fakeRepo = FakeAuthRepository()
    private val loginUseCase = LoginUseCase(fakeRepo)

    @Test
    fun `given valid credentials, when login, then returns user`() = runTest {
        fakeRepo.setLoginSuccess(TestData.user, "fake-jwt-token")

        val result = loginUseCase("test@echo.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals("Test User", result.getOrNull()?.displayName)
    }

    @Test
    fun `given invalid credentials, when login, then returns failure`() = runTest {
        fakeRepo.setLoginError("Invalid credentials")

        val result = loginUseCase("wrong@echo.com", "wrongpass")

        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
    }
}