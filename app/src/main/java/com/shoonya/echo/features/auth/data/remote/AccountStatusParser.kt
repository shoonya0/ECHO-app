package com.shoonya.echo.features.auth.data.remote

import com.shoonya.echo.features.auth.domain.model.AccountStatus
import com.shoonya.echo.features.auth.domain.model.AccountUnavailableException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/**
 * Reads a banned/inactive account out of a `403` reply (#21). Two shapes reach the app:
 * - login: `{ "success": false, "message": "account is banned" | "account is not active" }`,
 *   plus `"code": "ACCOUNT_BANNED" | "ACCOUNT_INACTIVE"` from server #171 on;
 * - authenticated routes: `{ "error": "...", "code": "ACCOUNT_BANNED" | "ACCOUNT_INACTIVE" }`.
 * `code` is read first; the message is only a fallback for older servers.
 */
internal object AccountStatusParser {
    fun parse(httpCode: Int, body: String?): AccountStatus? {
        if (httpCode != 403 || body.isNullOrBlank()) return null
        val json = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
            ?: return null
        val code = (json["code"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        when (code) {
            "ACCOUNT_BANNED" -> return AccountStatus.BANNED
            "ACCOUNT_INACTIVE" -> return AccountStatus.INACTIVE
        }
        val message = (json["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return when (message?.trim()?.lowercase()) {
            "account is banned" -> AccountStatus.BANNED
            "account is not active" -> AccountStatus.INACTIVE
            else -> null
        }
    }
}

/** A login failure as the domain sees it: banned/inactive becomes [AccountUnavailableException]. */
internal fun Throwable.toLoginFailure(): Throwable {
    if (this !is HttpException) return this
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val status = AccountStatusParser.parse(code(), body) ?: return this
    return AccountUnavailableException(status)
}
