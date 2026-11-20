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
        val manager = SessionManager(clearTokens = { cleared++ })
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
}
