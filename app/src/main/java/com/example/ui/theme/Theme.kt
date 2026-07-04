package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = YTRed,
    onPrimary = TextWhite,
    secondary = TextSilver,
    onSecondary = TextWhite,
    background = AmoledBlack,
    onBackground = TextWhite,
    surface = DarkGrey,
    onSurface = TextWhite,
    surfaceVariant = LightGrey,
    onSurfaceVariant = TextSilver
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    dynamicColor: Boolean = false, // Use our custom brand colors
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
