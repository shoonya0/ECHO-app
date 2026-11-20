package com.shoonya.echo.features.contacts.data.remote

import com.shoonya.echo.core.data.remote.ApiResponse
import com.shoonya.echo.core.data.remote.PaginatedData
import com.shoonya.echo.features.contacts.data.model.ContactDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The five contact lists come in pages (server #39): `page` counts from 1, `limit` is 1 to
 * 100, and `pagination.total_pages` says how many pages there are. The repository reads
 * every page.
 */
const val CONTACTS_PAGE_SIZE = 100

interface ContactsApiService {
    @GET("users/contacts/")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = CONTACTS_PAGE_SIZE
    ): ApiResponse<PaginatedData<ContactDto>>

    @GET("users/contacts/requests")
    suspend fun getContactRequests(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = CONTACTS_PAGE_SIZE
    ): ApiResponse<PaginatedData<ContactDto>>

    @GET("users/contacts/sent-requests")
    suspend fun getSentContactRequests(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = CONTACTS_PAGE_SIZE
    ): ApiResponse<PaginatedData<ContactDto>>

    @GET("users/contacts/blocked")
    suspend fun getBlockedUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = CONTACTS_PAGE_SIZE
    ): ApiResponse<PaginatedData<ContactDto>>

    @GET("users/contacts/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = CONTACTS_PAGE_SIZE
    ): ApiResponse<PaginatedData<ContactDto>>

    @POST("users/contacts/{userId}")
    suspend fun sendContactRequest(@Path("userId") userId: String): ApiResponse<Unit>

    @PUT("users/contacts/{requestId}")
    suspend fun acceptOrDeclineRequest(
        @Path("requestId") requestId: String,
        @Query("action") action: String
    ): ApiResponse<Unit>

    @DELETE("users/contacts/{contactId}")
    suspend fun removeContact(@Path("contactId") contactId: String): ApiResponse<Unit>

    @POST("users/contacts/blockUnblock/{userId}")
    suspend fun blockUnblockUser(
        @Path("userId") userId: String,
        @Query("action") action: String = "block"
    ): ApiResponse<Unit>

    @POST("users/contacts/favorite/{userId}")
    suspend fun addToFavorites(@Path("userId") userId: String): ApiResponse<Unit>

    @DELETE("users/contacts/favorite/{userId}")
    suspend fun removeFromFavorites(@Path("userId") userId: String): ApiResponse<Unit>
}
