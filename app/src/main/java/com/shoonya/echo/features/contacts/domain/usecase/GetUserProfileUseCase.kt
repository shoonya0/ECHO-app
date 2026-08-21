package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.core.domain.model.User
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(userId: String): Result<User> =
        contactsRepository.getUserProfile(userId)
}