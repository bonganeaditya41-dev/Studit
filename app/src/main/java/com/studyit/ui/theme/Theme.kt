package com.studyit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = StuditBlue,
    onPrimary = SurfaceLight,
    primaryContainer = StuditSky,
    onPrimaryContainer = StuditBlueDeep,
    secondary = StuditBlueDeep,
    onSecondary = SurfaceLight,
    secondaryContainer = StuditSky,
    onSecondaryContainer = StuditBlueDeep,
    tertiary = StuditYellow,
    onTertiary = StuditInk,
    background = BackgroundLight,
    onBackground = StuditInk,
    surface = SurfaceLight,
    onSurface = StuditInk,
    surfaceVariant = StuditSky,
    onSurfaceVariant = StuditMuted,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = androidx.compose.ui.graphics.Color(0xFFB3261E),
    onError = SurfaceLight
)

private val DarkScheme = darkColorScheme(
    primary = StuditBlueLight,
    onPrimary = BackgroundDark,
    primaryContainer = StuditBlueDeep,
    onPrimaryContainer = StuditSky,
    secondary = StuditSky,
    onSecondary = BackgroundDark,
    secondaryContainer = StuditBlueDeep,
    onSecondaryContainer = StuditSky,
    tertiary = StuditYellow,
    onTertiary = StuditInk,
    background = BackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE7ECF7),
    surface = SurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE7ECF7),
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF9AA6C2),
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF370001)
)

@Composable
fun StuditTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = StuditTypography,
        shapes = StuditShapes,
        content = content
    )
}
