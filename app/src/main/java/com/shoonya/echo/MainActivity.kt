package com.shoonya.echo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.shoonya.echo.core.data.remote.SessionManager
import com.shoonya.echo.core.presentation.SafeScreen
import com.shoonya.echo.core.theme.EchoBackgroundDark
import com.shoonya.echo.core.theme.EchoBackgroundLight
import com.shoonya.echo.core.theme.EchoTheme
import com.shoonya.echo.core.theme.ThemeStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeStore: ThemeStore

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by themeStore.darkThemePreference.collectAsState(initial = null)
            val darkTheme = themePreference ?: isSystemInDarkTheme()

            EchoTheme(darkTheme = darkTheme) {
                // Sync the window background with the actual Compose theme so that
                // the pre-Compose surface (splash, transition gaps) matches the
                // visible color scheme in both light and dark modes.
                LaunchedEffect(darkTheme) {
                    val bgArgb = if (darkTheme) {
                        EchoBackgroundDark.toArgb()
                    } else {
                        EchoBackgroundLight.toArgb()
                    }
                    window.decorView.setBackgroundColor(bgArgb)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeScreen {
                        EchoNavHost(sessionManager = sessionManager)
                    }
                }
            }
        }
    }
}
