package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class BlockUserUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(userId: String): Result<Unit> =
        contactsRepository.blockUser(userId)
}