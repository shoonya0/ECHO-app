package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.model.ContactRequest
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class GetOutgoingRequestsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(): Result<List<ContactRequest>> = contactsRepository.getOutgoingRequests()
}