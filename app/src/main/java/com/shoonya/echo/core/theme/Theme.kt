package com.shoonya.echo.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = EchoPrimaryLight,
    onPrimary = EchoOnPrimaryLight,
    primaryContainer = EchoPrimaryContainerLight,
    onPrimaryContainer = EchoOnPrimaryContainerLight,
    secondary = EchoSecondaryLight,
    onSecondary = EchoOnSecondaryLight,
    secondaryContainer = EchoSecondaryContainerLight,
    onSecondaryContainer = EchoOnSecondaryContainerLight,
    tertiary = EchoTertiaryLight,
    onTertiary = EchoOnTertiaryLight,
    tertiaryContainer = EchoTertiaryContainerLight,
    onTertiaryContainer = EchoOnTertiaryContainerLight,
    background = EchoBackgroundLight,
    onBackground = EchoOnBackgroundLight,
    surface = EchoSurfaceLight,
    onSurface = EchoOnSurfaceLight,
    surfaceVariant = EchoSurfaceVariantLight,
    onSurfaceVariant = EchoOnSurfaceVariantLight,
    error = EchoErrorLight,
    onError = EchoOnErrorLight,
    errorContainer = EchoErrorContainerLight,
    onErrorContainer = EchoOnErrorContainerLight,
    outline = EchoOutlineLight,
    outlineVariant = EchoOutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = EchoPrimaryDark,
    onPrimary = EchoOnPrimaryDark,
    primaryContainer = EchoPrimaryContainerDark,
    onPrimaryContainer = EchoOnPrimaryContainerDark,
    secondary = EchoSecondaryDark,
    onSecondary = EchoOnSecondaryDark,
    secondaryContainer = EchoSecondaryContainerDark,
    onSecondaryContainer = EchoOnSecondaryContainerDark,
    tertiary = EchoTertiaryDark,
    onTertiary = EchoOnTertiaryDark,
    tertiaryContainer = EchoTertiaryContainerDark,
    onTertiaryContainer = EchoOnTertiaryContainerDark,
    background = EchoBackgroundDark,
    onBackground = EchoOnBackgroundDark,
    surface = EchoSurfaceDark,
    onSurface = EchoOnSurfaceDark,
    surfaceVariant = EchoSurfaceVariantDark,
    onSurfaceVariant = EchoOnSurfaceVariantDark,
    error = EchoErrorDark,
    onError = EchoOnErrorDark,
    errorContainer = EchoErrorContainerDark,
    onErrorContainer = EchoOnErrorContainerDark,
    outline = EchoOutlineDark,
    outlineVariant = EchoOutlineVariantDark,
)

@Composable
fun EchoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EchoTypography,
        shapes = EchoShapes,
        content = content,
    )
}