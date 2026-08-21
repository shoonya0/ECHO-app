package com.shoonya.echo.contacts

import com.shoonya.echo.fakes.FakeContactsRepository
import com.shoonya.echo.fakes.TestData
import com.shoonya.echo.features.contacts.domain.usecase.GetContactsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetContactsUseCaseTest {
    private val fakeRepo = FakeContactsRepository()
    private val getContactsUseCase = GetContactsUseCase(fakeRepo)

    @Test
    fun `given contacts exist, when getContacts, then returns contact list`() = runTest {
        val contacts = listOf(TestData.contact1, TestData.contact2)
        fakeRepo.setContactsSuccess(contacts)

        val result = getContactsUseCase()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("alice", result.getOrNull()?.first()?.username)
    }

    @Test
    fun `given repository fails, when getContacts, then returns failure`() = runTest {
        fakeRepo.setContactsError("Network error")

        val result = getContactsUseCase()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}