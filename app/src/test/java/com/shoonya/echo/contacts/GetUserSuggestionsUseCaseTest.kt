package com.shoonya.echo.contacts

import com.shoonya.echo.fakes.FakeContactsRepository
import com.shoonya.echo.features.contacts.domain.model.UserSuggestion
import com.shoonya.echo.features.contacts.domain.usecase.GetUserSuggestionsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetUserSuggestionsUseCaseTest {

    private val fakeRepository = FakeContactsRepository()
    private lateinit var useCase: GetUserSuggestionsUseCase

    @Before
    fun setup() {
        useCase = GetUserSuggestionsUseCase(fakeRepository)
    }

    @Test
    fun `invoke returns suggestions on success`() = runTest {
        val suggestions = listOf(
            UserSuggestion("1", "alice", "Alice", "", "Hey", ""),
            UserSuggestion("2", "bob", "Bob", "", "", ""),
        )
        fakeRepository.setGetUserSuggestionsSuccess(suggestions)

        val result = useCase(1, 20)

        assertTrue(result.isSuccess)
        assertEquals(suggestions, result.getOrThrow())
    }

    @Test
    fun `invoke returns empty list when no suggestions`() = runTest {
        fakeRepository.setGetUserSuggestionsSuccess(emptyList())

        val result = useCase(1, 20)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `invoke returns failure on error`() = runTest {
        fakeRepository.setGetUserSuggestionsError("Network error")

        val result = useCase(1, 20)

        assertTrue(result.isFailure)
    }
}