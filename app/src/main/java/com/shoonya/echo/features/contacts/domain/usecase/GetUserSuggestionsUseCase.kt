package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class GetUserSuggestionsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(page: Int, limit: Int): Result<List<UserSuggestion>> =
        contactsRepository.getUserSuggestions(page, limit)
}