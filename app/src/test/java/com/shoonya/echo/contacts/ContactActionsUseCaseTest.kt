package com.shoonya.echo.contacts

import com.shoonya.echo.fakes.FakeContactsRepository
import com.shoonya.echo.features.contacts.domain.usecase.AcceptContactRequestUseCase
import com.shoonya.echo.features.contacts.domain.usecase.AddToFavoritesUseCase
import com.shoonya.echo.features.contacts.domain.usecase.BlockUserUseCase
import com.shoonya.echo.features.contacts.domain.usecase.RemoveContactUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactActionsUseCaseTest {
    private val fakeRepo = FakeContactsRepository()
    private val acceptContactRequestUseCase = AcceptContactRequestUseCase(fakeRepo)
    private val blockUserUseCase = BlockUserUseCase(fakeRepo)
    private val addToFavoritesUseCase = AddToFavoritesUseCase(fakeRepo)
    private val removeContactUseCase = RemoveContactUseCase(fakeRepo)

    // AcceptContactRequest
    @Test
    fun `given valid request, when acceptContactRequest, then returns success`() = runTest {
        fakeRepo.setAcceptContactRequestSuccess()

        val result = acceptContactRequestUseCase("req_in_001")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository fails, when acceptContactRequest, then returns failure`() = runTest {
        fakeRepo.setAcceptContactRequestError("Request not found")

        val result = acceptContactRequestUseCase("bad_req")

        assertTrue(result.isFailure)
    }

    // BlockUser
    @Test
    fun `given valid userId, when blockUser, then returns success`() = runTest {
        fakeRepo.setBlockUserSuccess()

        val result = blockUserUseCase("60af1f77bcf86cd799439001")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository fails, when blockUser, then returns failure`() = runTest {
        fakeRepo.setBlockUserError("Cannot block yourself")

        val result = blockUserUseCase("invalid")

        assertTrue(result.isFailure)
    }

    // RemoveContact
    @Test
    fun `given valid contactId, when removeContact, then returns success`() = runTest {
        fakeRepo.setRemoveContactSuccess()

        val result = removeContactUseCase("60af1f77bcf86cd799439001")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository fails, when removeContact, then returns failure`() = runTest {
        fakeRepo.setRemoveContactError("Contact not found")

        val result = removeContactUseCase("invalid")

        assertTrue(result.isFailure)
    }

    // AddToFavorites
    @Test
    fun `given valid userId, when addToFavorites, then returns success`() = runTest {
        fakeRepo.setAddToFavoritesSuccess()

        val result = addToFavoritesUseCase("60af1f77bcf86cd799439001")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository fails, when addToFavorites, then returns failure`() = runTest {
        fakeRepo.setAddToFavoritesError("Contact not found")

        val result = addToFavoritesUseCase("invalid")

        assertTrue(result.isFailure)
    }
}