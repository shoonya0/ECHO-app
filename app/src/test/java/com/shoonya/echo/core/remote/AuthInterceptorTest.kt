package com.shoonya.echo.core.remote

import com.shoonya.echo.core.data.remote.AuthInterceptor
import com.shoonya.echo.core.domain.model.SessionEndReason
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Server auth replies (#33) have the shape `{ "error": "...", "code": "..." }`. Only 401
 * and the two 403 account codes end the session; `503 AUTH_UNAVAILABLE` and other 403s don't.
 */
class AuthInterceptorTest {

    private val ended = mutableListOf<SessionEndReason>()

    private fun call(token: String?, status: Int, body: String): Response {
        val interceptor = AuthInterceptor(
            accessToken = { token },
            endSession = { ended += it }
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("test")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return client.newCall(Request.Builder().url("http://localhost/echo/v1/x").build()).execute()
    }

    @Test
    fun `403 ACCOUNT_BANNED with a token ends the session as banned`() {
        call("t", 403, """{"error":"account is banned","code":"ACCOUNT_BANNED"}""").close()
        assertEquals(listOf(SessionEndReason.ACCOUNT_BANNED), ended)
    }

    @Test
    fun `403 ACCOUNT_INACTIVE with a token ends the session as inactive`() {
        call("t", 403, """{"error":"account is not active","code":"ACCOUNT_INACTIVE"}""").close()
        assertEquals(listOf(SessionEndReason.ACCOUNT_INACTIVE), ended)
    }

    @Test
    fun `401 with a token ends the session as expired`() {
        call("t", 401, """{"error":"token revoked","code":"TOKEN_REVOKED"}""").close()
        assertEquals(listOf(SessionEndReason.EXPIRED), ended)
    }

    @Test
    fun `401 ACCOUNT_NOT_FOUND with a token ends the session as account not found`() {
        call("t", 401, """{"error":"account not found","code":"ACCOUNT_NOT_FOUND"}""").close()
        assertEquals(listOf(SessionEndReason.ACCOUNT_NOT_FOUND), ended)
    }

    @Test
    fun `401 with a body that is not JSON ends the session as expired`() {
        call("t", 401, "Unauthorized").close()
        assertEquals(listOf(SessionEndReason.EXPIRED), ended)
    }

    @Test
    fun `the caller can still read a 401 body after the interceptor peeked it`() {
        val body = """{"error":"account not found","code":"ACCOUNT_NOT_FOUND"}"""
        call("t", 401, body).use { response ->
            assertEquals(body, response.body?.string())
        }
    }

    @Test
    fun `503 AUTH_UNAVAILABLE keeps the session`() {
        call("t", 503, """{"error":"auth unavailable","code":"AUTH_UNAVAILABLE"}""").close()
        assertTrue(ended.isEmpty())
    }

    @Test
    fun `other 403 such as PERMISSION_DENIED keeps the session`() {
        call(
            "t",
            403,
            """{"success":false,"error":"not a member","code":"PERMISSION_DENIED"}"""
        ).close()
        assertTrue(ended.isEmpty())
    }

    @Test
    fun `403 with a body that is not JSON keeps the session`() {
        call("t", 403, "Forbidden").close()
        assertTrue(ended.isEmpty())
    }

    @Test
    fun `403 ACCOUNT_BANNED without a token does not end a session`() {
        // Logging in with a banned account: there is no session to end.
        call(null, 403, """{"error":"account is banned","code":"ACCOUNT_BANNED"}""").close()
        assertTrue(ended.isEmpty())
    }

    @Test
    fun `the caller can still read the body after the interceptor peeked it`() {
        val body = """{"error":"account is banned","code":"ACCOUNT_BANNED"}"""
        call("t", 403, body).use { response ->
            assertEquals(body, response.body?.string())
        }
    }

    @Test
    fun `sends the bearer token only when there is one`() {
        call("abc", 200, "{}").use { response ->
            assertEquals("Bearer abc", response.request.header("Authorization"))
        }
        call(null, 200, "{}").use { response ->
            assertNull(response.request.header("Authorization"))
        }
    }
}
