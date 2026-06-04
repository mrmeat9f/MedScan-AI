package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintTeal,
    secondary = LavenderActive,
    tertiary = ColorValid,
    background = Color(0xFF101C1B),
    surface = Color(0xFF182826),
    onBackground = Color(0xFFEFF7F6),
    onSurface = Color(0xFFEFF7F6),
    onPrimary = Color.White,
    surfaceVariant = Color(0xFF1E3230),
    onSurfaceVariant = Color(0xFFB5C9C6),
    outline = Color(0xFF2E4E4A)
)

private val LightColorScheme = lightColorScheme(
    primary = MintTeal,
    secondary = LavenderText,
    primaryContainer = LavenderActive,
    tertiary = ColorValid,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onPrimary = Color.White,
    surfaceVariant = LightMint,
    onSurfaceVariant = Color(0xFF556664),
    outline = Color(0xFFCCD9D7)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Provide visually beautiful mint-teal light palette as default or requested
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
