package com.shoonya.echo.core.local

import com.shoonya.echo.core.data.local.openWithRecovery
import java.io.IOException
import java.security.KeyStoreException
import java.security.ProviderException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class SecurePrefsRecoveryTest {

    private var opens = 0
    private var resets = 0

    private fun failFirstOpenWith(error: Exception): () -> String = {
        opens++
        if (opens == 1) throw error
        "fresh"
    }

    private val reset: () -> Unit = { resets++ }

    @Test
    fun `opens without reset when the prefs decrypt`() {
        val result = openWithRecovery(open = {
            opens++
            "prefs"
        }, reset = reset)

        assertEquals("prefs", result)
        assertEquals(1, opens)
        assertEquals(0, resets)
    }

    @Test
    fun `bad tag after a restore deletes the prefs and opens fresh ones`() {
        val result = openWithRecovery(failFirstOpenWith(AEADBadTagException()), reset)

        assertEquals("fresh", result)
        assertEquals(2, opens)
        assertEquals(1, resets)
    }

    @Test
    fun `keystore, unreadable keyset and provider errors also recover`() {
        listOf(KeyStoreException(), IOException(), ProviderException()).forEach { error ->
            opens = 0
            resets = 0

            assertEquals("fresh", openWithRecovery(failFirstOpenWith(error), reset))
            assertEquals(1, resets)
        }
    }

    @Test
    fun `a second failure is thrown, not looped`() {
        val second = KeyStoreException("still broken")
        var calls = 0

        val thrown = assertThrows(KeyStoreException::class.java) {
            openWithRecovery(
                open = {
                    calls++
                    throw if (calls == 1) AEADBadTagException() else second
                },
                reset = reset
            )
        }

        assertSame(second, thrown)
        assertEquals(1, resets)
    }

    @Test
    fun `unrelated errors are not swallowed and nothing is deleted`() {
        assertThrows(IllegalStateException::class.java) {
            openWithRecovery(open = { throw IllegalStateException() }, reset = reset)
        }
        assertEquals(0, resets)
    }
}
