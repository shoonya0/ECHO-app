package com.shoonya.echo.core.theme

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's theme preference.
 *
 * Values:
 * - null  → follow system dark mode (default)
 * - true  → force dark mode
 * - false → force light mode
 */
interface ThemeStore {
    /** Emits the saved dark theme preference. null = follow system. */
    val darkThemePreference: Flow<Boolean?>

    /** Returns the current preference synchronously. */
    val darkThemePreferenceSync: Boolean?

    /** Saves the dark theme preference. null = follow system. */
    fun setDarkThemePreference(enabled: Boolean?)
}

@Singleton
class ThemeStoreImpl @Inject constructor(
    @ApplicationContext context: Context,
) : ThemeStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val darkThemePreference: Flow<Boolean?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DARK_THEME) {
                trySend(getPreferenceSync())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Emit initial value
        trySend(getPreferenceSync())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val darkThemePreferenceSync: Boolean?
        get() = getPreferenceSync()

    override fun setDarkThemePreference(enabled: Boolean?) {
        if (enabled == null) {
            prefs.edit().remove(KEY_DARK_THEME).apply()
        } else {
            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        }
    }

    private fun getPreferenceSync(): Boolean? {
        return if (prefs.contains(KEY_DARK_THEME)) {
            prefs.getBoolean(KEY_DARK_THEME, false)
        } else {
            null // follow system
        }
    }

    companion object {
        private const val PREFS_NAME = "echo_theme_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}