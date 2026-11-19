package com.shoonya.echo.chat

import com.shoonya.echo.core.data.remote.NetworkModule
import com.shoonya.echo.features.chat.data.remote.MessageApiService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Pins the message-history request and reply to the server's contract
 * (backend `_docs/rest-api/chats.md#messages`): cursor paging with `before`,
 * no `offset` (the server answers 400 to any `offset`, issue #7).
 */
class MessageApiServiceTest {

    private var lastUrl: HttpUrl? = null
    private var replyBody: String = PAGE_WITH_MORE

    private val api: MessageApiService = run {
        val fakeServer = Interceptor { chain ->
            lastUrl = chain.request().url
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(replyBody.toResponseBody("application/json".toMediaType()))
                .build()
        }
        Retrofit.Builder()
            .baseUrl("http://test.invalid/echo/v1/")
            .client(OkHttpClient.Builder().addInterceptor(fakeServer).build())
            .addConverterFactory(
                NetworkModule.provideJson().asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(MessageApiService::class.java)
    }

    @Test
    fun `first page sends no offset and no cursor`() = runTest {
        api.getChatMessages(CHAT_ID)

        val url = lastUrl!!
        assertEquals("/echo/v1/messages/$CHAT_ID/messages", url.encodedPath)
        assertNull("offset is rejected by the server", url.queryParameter("offset"))
        assertNull(url.queryParameter("before"))
    }

    @Test
    fun `server page with a cursor parses`() = runTest {
        val data = api.getChatMessages(CHAT_ID).data!!

        assertEquals(2, data.messages.size)
        assertTrue(data.pagination.hasMore)
        assertEquals("m1", data.pagination.nextCursor)
    }

    @Test
    fun `last page has no cursor`() = runTest {
        replyBody = LAST_PAGE

        val data = api.getChatMessages(CHAT_ID).data!!

        assertFalse(data.pagination.hasMore)
        assertNull(data.pagination.nextCursor)
    }

    @Test
    fun `reply without hasMore fails instead of passing as the last page`() {
        replyBody = """
            {"success":true,"data":{"chat":"c1","messages":[],
              "pagination":{"limit":50,"count":0,"totalCount":0}}}
        """.trimIndent()

        assertThrows(SerializationException::class.java) {
            runBlocking { api.getChatMessages(CHAT_ID) }
        }
    }

    private companion object {
        const val CHAT_ID = "c1"

        // Shape of the server's reply at shoonya0/ECHO dfd2473 (chat.controller.go).
        val PAGE_WITH_MORE = """
            {"success":true,"message":"ok","data":{
              "chat":"c1",
              "messages":[
                {"_id":"m2","chatId":"c1","senderId":"u2","content":"newer","seq":2,
                 "createdAt":"2026-11-04T12:01:00Z"},
                {"_id":"m1","chatId":"c1","senderId":"u1","content":"older","seq":1,
                 "createdAt":"2026-11-04T12:00:00Z"}
              ],
              "readSeqs":{"u1":2},
              "pagination":{"limit":2,"count":2,"hasMore":true,"nextCursor":"m1"}
            }}
        """.trimIndent()

        val LAST_PAGE = """
            {"success":true,"data":{
              "chat":"c1","messages":[],"readSeqs":{},
              "pagination":{"limit":50,"count":0,"hasMore":false,"nextCursor":null}
            }}
        """.trimIndent()
    }
}
