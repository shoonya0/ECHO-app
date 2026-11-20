package com.shoonya.echo.core.remote

import com.shoonya.echo.core.data.remote.createLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggingInterceptorTest {

    private val token = "header.payload.signature"

    // Answers every call locally, so the test needs no server.
    private val fakeServer = Interceptor { chain ->
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
    }

    private fun logLinesFor(request: Request): List<String> {
        val lines = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor { lines += it })
            .addInterceptor(fakeServer)
            .build()
        client.newCall(request).execute().close()
        return lines
    }

    @Test
    fun `authorization header is redacted from the log`() {
        val lines = logLinesFor(
            Request.Builder()
                .url("http://localhost/echo/v1/me")
                .header("Authorization", "Bearer $token")
                .build()
        )

        assertFalse(lines.any { it.contains(token) })
        assertTrue(lines.any { it.startsWith("Authorization:") && it.contains("██") })
    }

    @Test
    fun `other headers and the body are still logged`() {
        val lines = logLinesFor(
            Request.Builder()
                .url("http://localhost/echo/v1/me")
                .header("X-Trace", "abc")
                .build()
        )

        assertTrue(lines.contains("X-Trace: abc"))
        assertTrue(lines.contains("{}"))
    }
}
