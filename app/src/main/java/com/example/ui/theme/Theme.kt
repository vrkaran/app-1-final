package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LightGold,
    onPrimary = DeepNavy,
    primaryContainer = PrimaryNavy,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = DeepNavy,
    secondaryContainer = DeepNavy,
    onSecondaryContainer = LightGold,
    background = DeepNavy,
    onBackground = Color.White,
    surface = Color(0xFF131D33),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A2644),
    onSurfaceVariant = Color(0xFFB0BDD8),
    outline = Color(0xFF455A7C)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = PrimaryNavy,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = DeepNavy,
    secondaryContainer = SurfaceContainerLow,
    onSecondaryContainer = PrimaryNavy,
    background = MainBg,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = StatusDanger,
    onError = Color.White
)

@Composable
fun HBXSolarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HBXTypography,
        content = content
    )
}
