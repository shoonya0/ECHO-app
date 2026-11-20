package com.shoonya.echo.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import timber.log.Timber

class SecureTokenStore(private val context: Context) {
    private val prefs: SharedPreferences = openWithRecovery(
        open = ::openEncryptedPrefs,
        reset = { context.deleteSharedPreferences(PREFS_NAME) }
    )

    private fun openEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun clearTokens() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_USER_ID).apply()
    }

    companion object {
        // Also excluded from backup in res/xml/backup_rules.xml and data_extraction_rules.xml.
        private const val PREFS_NAME = "echo_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_USER_ID = "current_user_id"
    }
}

private const val TAG = "SecureTokenStore"

/**
 * Opens the encrypted prefs; if they can't be decrypted, deletes them and opens a fresh, empty
 * store, so the user lands on Login instead of the app crashing at start-up.
 *
 * This happens when the file outlives its Android Keystore key: restored from a backup or a
 * device transfer (the key never leaves the device), or the Keystore entry was lost. The old
 * tokens are unreadable either way. A second failure is thrown: then the Keystore itself is broken.
 */
internal fun <T> openWithRecovery(open: () -> T, reset: () -> Unit): T = try {
    open()
} catch (e: GeneralSecurityException) {
    recover(e, open, reset)
} catch (e: IOException) {
    recover(e, open, reset)
} catch (e: ProviderException) {
    recover(e, open, reset)
}

private fun <T> recover(cause: Exception, open: () -> T, reset: () -> Unit): T {
    // Log only the exception type, never the stored values.
    val type = cause.javaClass.simpleName
    Timber.tag(TAG).w("Secure prefs unreadable (%s); deleting them, user must log in again", type)
    reset()
    return open()
}
