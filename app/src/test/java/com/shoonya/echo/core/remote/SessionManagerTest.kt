package com.shoonya.echo.core.remote

import com.shoonya.echo.core.data.remote.SessionManager
import com.shoonya.echo.core.domain.model.SessionEndReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @Test
    fun `expire clears tokens and emits the reason, EXPIRED by default`() = runTest {
        var cleared = 0
        val manager = SessionManager(clearTokens = { cleared++ }, clearAccountData = {})
        val reasons = mutableListOf<SessionEndReason>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.sessionExpired.toList(reasons)
        }

        manager.expire()
        manager.expire(SessionEndReason.ACCOUNT_BANNED)

        assertEquals(2, cleared)
        assertEquals(listOf(SessionEndReason.EXPIRED, SessionEndReason.ACCOUNT_BANNED), reasons)
        job.cancel()
    }

    // #22: a deleted account's data must not survive for the next login in this process.
    @Test
    fun `ACCOUNT_NOT_FOUND clears tokens and the account's local data`() {
        var tokensCleared = 0
        var dataCleared = 0
        val manager = SessionManager(
            clearTokens = { tokensCleared++ },
            clearAccountData = { dataCleared++ }
        )

        manager.expire(SessionEndReason.ACCOUNT_NOT_FOUND)

        assertEquals(1, tokensCleared)
        assertEquals(1, dataCleared)
    }

    // #45: the next account to log in within this process must not see the previous
    // account's data, whatever ended the session.
    @Test
    fun `every session end clears the account's local data`() {
        var dataCleared = 0
        val manager = SessionManager(clearTokens = {}, clearAccountData = { dataCleared++ })

        manager.expire(SessionEndReason.EXPIRED)
        manager.expire(SessionEndReason.ACCOUNT_BANNED)
        manager.expire(SessionEndReason.ACCOUNT_INACTIVE)
        manager.expire(SessionEndReason.ACCOUNT_NOT_FOUND)

        assertEquals(4, dataCleared)
    }
}
