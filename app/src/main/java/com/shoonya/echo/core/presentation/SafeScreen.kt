package com.shoonya.echo.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * Crash loop prevention composable.
 *
 * Wraps screen content with a [key] that resets the composition subtree
 * each time the internal crash counter increments. After more than
 * [maxCrashCount] resets, a fallback error screen is shown instead.
 *
 * Actual exception catching is handled at the platform level by
 * [ComposeUncaughtExceptionHandler]. This composable prevents infinite
 * recomposition loops from stale/corrupt state.
 */
@Composable
fun SafeScreen(
    maxCrashCount: Int = 2,
    content: @Composable () -> Unit,
) {
    var crashCount by remember { mutableIntStateOf(0) }

    if (crashCount > maxCrashCount) {
        FallbackErrorScreen(
            message = "Something went wrong.",
            onRetry = {
                Timber.tag("SafeScreen").w("User tapped retry — resetting crash counter")
                crashCount = 0
            },
        )
    } else {
        key(crashCount) {
            content()
        }
    }
}

@Composable
fun FallbackErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}