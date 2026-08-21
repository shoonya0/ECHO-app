package com.shoonya.echo.features.contacts.domain.usecase

import com.shoonya.echo.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class AddToFavoritesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(userId: String): Result<Unit> =
        contactsRepository.addToFavorites(userId)
}