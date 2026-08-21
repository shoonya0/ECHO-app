package com.shoonya.echo.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Avatar with a colored-initials fallback when [avatarUrl] is blank.
 */
@Composable
fun InitialsAvatar(
    name: String,
    avatarUrl: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    if (avatarUrl.isNotBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "$name avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        // Generate a stable hue from the name so each user gets a consistent color.
        val hue = name.hashCode().toFloat().let { kotlin.math.abs(it) % 360f }
        val initColor = androidx.compose.ui.graphics.Color.hsl(hue, 0.5f, 0.6f)
        val initial = name.firstOrNull()?.uppercase() ?: "?"

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(initColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}