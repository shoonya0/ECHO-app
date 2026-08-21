package com.shoonya.echo.features.auth.domain.usecase

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}