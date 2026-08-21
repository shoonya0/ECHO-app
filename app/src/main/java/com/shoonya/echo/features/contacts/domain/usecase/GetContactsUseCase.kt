package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.model.Contact
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(): Result<List<Contact>> = contactsRepository.getContacts()
}