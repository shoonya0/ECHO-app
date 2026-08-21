package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class DeclineContactRequestUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(requestId: String): Result<Unit> =
        contactsRepository.declineContactRequest(requestId)
}