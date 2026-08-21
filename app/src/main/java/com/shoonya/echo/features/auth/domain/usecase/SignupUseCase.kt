package com.shoonya.echo.features.auth.domain.usecase

import com.shoonya.echo.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, username: String, password: String): Result<Unit> {
        return authRepository.signup(email, username, password)
    }
}