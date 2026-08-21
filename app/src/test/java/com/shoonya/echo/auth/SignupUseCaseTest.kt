package com.shoonya.echo.auth

import com.shoonya.echo.fakes.FakeAuthRepository
import com.shoonya.echo.features.auth.domain.usecase.SignupUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupUseCaseTest {
    private val fakeRepo = FakeAuthRepository()
    private val signupUseCase = SignupUseCase(fakeRepo)

    @Test
    fun `given valid signup data, when signup, then returns success`() = runTest {
        fakeRepo.setSignupSuccess()

        val result = signupUseCase("new@echo.com", "newuser", "password123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given server error, when signup, then returns failure`() = runTest {
        fakeRepo.setSignupError("Email already taken")

        val result = signupUseCase("taken@echo.com", "takenuser", "password123")

        assertTrue(result.isFailure)
    }
}