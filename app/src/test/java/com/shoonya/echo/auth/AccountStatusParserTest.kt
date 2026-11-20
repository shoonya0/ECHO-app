package com.shoonya.echo.auth

import com.shoonya.echo.features.auth.data.remote.AccountStatusParser
import com.shoonya.echo.features.auth.data.remote.toLoginFailure
import com.shoonya.echo.features.auth.domain.model.AccountStatus
import com.shoonya.echo.features.auth.domain.model.AccountUnavailableException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Login of a banned or inactive account answers `403` (#21). Servers before #171 send
 * `{"success":false,"message":"account is banned"}` with no `code`; after #171 the same
 * body also carries `"code":"ACCOUNT_BANNED"`. Authenticated routes send
 * `{"error":"...","code":"..."}`.
 */
class AccountStatusParserTest {

    @Test
    fun `login 403 after server 171, code ACCOUNT_BANNED, is banned`() {
        val body = """{"success":false,"message":"account is banned","code":"ACCOUNT_BANNED"}"""
        assertEquals(AccountStatus.BANNED, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `login 403 after server 171, code ACCOUNT_INACTIVE, is inactive`() {
        val body =
            """{"success":false,"message":"account is not active","code":"ACCOUNT_INACTIVE"}"""
        assertEquals(AccountStatus.INACTIVE, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `login 403 before server 171, message account is banned, is banned`() {
        val body = """{"success":false,"message":"account is banned"}"""
        assertEquals(AccountStatus.BANNED, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `login 403 before server 171, message account is not active, is inactive`() {
        val body = """{"success":false,"message":"account is not active"}"""
        assertEquals(AccountStatus.INACTIVE, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `token-error shape with code ACCOUNT_BANNED is banned`() {
        val body = """{"error":"account banned","code":"ACCOUNT_BANNED"}"""
        assertEquals(AccountStatus.BANNED, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `code wins over message`() {
        val body = """{"success":false,"message":"account is banned","code":"ACCOUNT_INACTIVE"}"""
        assertEquals(AccountStatus.INACTIVE, AccountStatusParser.parse(403, body))
    }

    @Test
    fun `other 403 is not an account status`() {
        val body = """{"success":false,"message":"forbidden","error":"PERMISSION_DENIED"}"""
        assertNull(AccountStatusParser.parse(403, body))
    }

    @Test
    fun `401 with banned code is not an account status`() {
        assertNull(AccountStatusParser.parse(401, """{"code":"ACCOUNT_BANNED"}"""))
    }

    @Test
    fun `empty or malformed 403 body is not an account status`() {
        assertNull(AccountStatusParser.parse(403, null))
        assertNull(AccountStatusParser.parse(403, ""))
        assertNull(AccountStatusParser.parse(403, "<html>Forbidden</html>"))
        assertNull(AccountStatusParser.parse(403, "[1,2]"))
    }

    @Test
    fun `banned 403 HttpException becomes AccountUnavailableException`() {
        val err = httpError(403, """{"success":false,"message":"account is banned"}""")
        val mapped = err.toLoginFailure()
        assertTrue(mapped is AccountUnavailableException)
        assertEquals(AccountStatus.BANNED, (mapped as AccountUnavailableException).status)
    }

    @Test
    fun `other HttpExceptions are kept as they are`() {
        val forbidden = httpError(403, """{"success":false,"message":"forbidden"}""")
        assertSame(forbidden, forbidden.toLoginFailure())
        val unauthorized = httpError(401, """{"success":false,"message":"invalid credentials"}""")
        assertSame(unauthorized, unauthorized.toLoginFailure())
    }

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
