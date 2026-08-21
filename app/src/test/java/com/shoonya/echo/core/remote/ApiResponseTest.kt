package com.shoonya.echo.core.remote

import com.shoonya.echo.core.data.remote.ApiException
import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.core.data.remote.toResult
import com.shoonya.echo.core.data.remote.toUnitResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `given valid success envelope, toResult returns data`() {
        val raw = """{"success":true,"message":"OK","data":{"name":"test"}}"""
        val parsed = json.decodeFromString<ApiResponse<TestData>>(raw)

        val result = parsed.toResult()

        assertTrue(result.isSuccess)
        assertEquals("test", result.getOrNull()?.name)
    }

    @Test
    fun `given failure envelope, toResult returns failure`() {
        val raw = """{"success":false,"message":"Something went wrong","data":null}"""
        val parsed = json.decodeFromString<ApiResponse<TestData>>(raw)

        val result = parsed.toResult()

        assertTrue(result.isFailure)
    }

    @Test
    fun `given response with unknown keys, parser ignores them`() {
        val raw = """{"success":true,"data":{"name":"test"},"extra_field":"ignored","nested":{"a":1}}"""
        val parsed = json.decodeFromString<ApiResponse<TestData>>(raw)

        assertTrue(parsed.success)
        assertEquals("test", parsed.data?.name)
    }

    @Test
    fun `given response with null data on success, toResult returns failure`() {
        val raw = """{"success":true,"data":null,"message":"No data"}"""
        val parsed = json.decodeFromString<ApiResponse<TestData>>(raw)

        val result = parsed.toResult()

        assertTrue(result.isFailure)
    }

    @Test
    fun `given response with missing fields, parser coerces defaults`() {
        val raw = """{"success":true}"""
        val parsed = json.decodeFromString<ApiResponse<TestData>>(raw)

        assertTrue(parsed.success)
        assertEquals(null, parsed.message)
        assertEquals(null, parsed.data)
        assertEquals(null, parsed.error)
    }

    @Test
    fun `given message-only success envelope, toUnitResult returns success`() {
        val raw = """{"success":true,"message":"Invite created"}"""
        val parsed = json.decodeFromString<ApiResponse<Unit>>(raw)

        val result = parsed.toUnitResult()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given message-only failure envelope, toUnitResult returns failure`() {
        val raw = """{"success":false,"error":"NOT_FOUND","message":"Invite not found"}"""
        val parsed = json.decodeFromString<ApiResponse<Unit>>(raw)

        val result = parsed.toUnitResult()

        assertTrue(result.isFailure)
        assertEquals("Invite not found", (result.exceptionOrNull() as? ApiException)?.message)
    }

    @kotlinx.serialization.Serializable
    data class TestData(val name: String)
}
