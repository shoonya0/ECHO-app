package com.shoonya.echo.contacts

import com.shoonya.echo.core.data.remote.NetworkModule
import com.shoonya.echo.features.contacts.data.remote.ContactsApiService
import com.shoonya.echo.features.contacts.data.remote.UsersApiService
import com.shoonya.echo.features.contacts.data.repository.ContactsRepositoryImpl
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Pins the contact lists to the server's paging contract (server #39, backend
 * `_docs/rest-api/users.md#contacts`): at most `limit` (1 to 100) entries per reply,
 * `page` counted from 1, and `pagination.total_pages` says how many pages there are.
 * The app must ask for every page, or contacts past the first page never show up.
 */
class ContactsRepositoryImplTest {

    private val requests = mutableListOf<HttpUrl>()

    /** Entries the fake server holds per list path, served in pages like the real one. */
    private val lists = mutableMapOf<String, List<String>>()

    private val repository: ContactsRepositoryImpl = run {
        val fakeServer = Interceptor { chain ->
            val url = chain.request().url
            requests += url
            val all = lists[url.encodedPath.removePrefix(BASE_PATH)] ?: emptyList()
            val limit = (url.queryParameter("limit")?.toIntOrNull() ?: 10).coerceIn(1, 100)
            val page = url.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val items = all.drop((page - 1) * limit).take(limit)
            val totalPages = (all.size + limit - 1) / limit
            val body = """
                {"success":true,"message":"ok","data":{
                  "items":[${items.joinToString(",") { """{"_id":"$it","username":"$it"}""" }}],
                  "pagination":{"limit":$limit,"total":${all.size},"total_pages":$totalPages}
                }}
            """.trimIndent()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("http://test.invalid$BASE_PATH")
            .client(OkHttpClient.Builder().addInterceptor(fakeServer).build())
            .addConverterFactory(
                NetworkModule.provideJson().asConverterFactory("application/json".toMediaType())
            )
            .build()
        ContactsRepositoryImpl(
            retrofit.create(ContactsApiService::class.java),
            retrofit.create(UsersApiService::class.java)
        )
    }

    private fun ids(prefix: String, count: Int) = (1..count).map { "$prefix$it" }

    @Test
    fun `contacts beyond the first page are loaded`() = runTest {
        lists["users/contacts/"] = ids("c", 250)

        val contacts = repository.getContacts().getOrThrow()

        assertEquals(ids("c", 250), contacts.map { it.id })
        assertEquals(listOf("1", "2", "3"), requests.map { it.queryParameter("page") })
    }

    @Test
    fun `every list asks for all its pages`() = runTest {
        lists["users/contacts/favorites"] = ids("f", 101)
        lists["users/contacts/blocked"] = ids("b", 101)
        lists["users/contacts/requests"] = ids("r", 101)
        lists["users/contacts/sent-requests"] = ids("s", 101)

        assertEquals(101, repository.getFavorites().getOrThrow().size)
        assertEquals(101, repository.getBlockedUsers().getOrThrow().size)
        assertEquals(101, repository.getIncomingRequests().getOrThrow().size)
        assertEquals(101, repository.getOutgoingRequests().getOrThrow().size)
    }

    @Test
    fun `a list that fits one page takes one request`() = runTest {
        lists["users/contacts/"] = ids("c", 3)

        assertEquals(3, repository.getContacts().getOrThrow().size)
        assertEquals(1, requests.size)
    }

    @Test
    fun `an empty list takes one request`() = runTest {
        assertTrue(repository.getContacts().getOrThrow().isEmpty())
        assertEquals(1, requests.size)
    }

    @Test
    fun `pages use the largest limit the server allows`() = runTest {
        repository.getContacts().getOrThrow()

        assertEquals("100", requests.single().queryParameter("limit"))
    }

    private companion object {
        const val BASE_PATH = "/echo/v1/"
    }
}
