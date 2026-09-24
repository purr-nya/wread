package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.WatchTheme

fun watchThemeToColorScheme(watchTheme: WatchTheme) = darkColorScheme(
    primary = watchTheme.accentColor,
    onPrimary = Color.Black,
    primaryContainer = watchTheme.cardBackground,
    onPrimaryContainer = watchTheme.textColor,
    background = watchTheme.backgroundColor,
    onBackground = watchTheme.textColor,
    surface = watchTheme.cardBackground,
    onSurface = watchTheme.textColor,
    surfaceVariant = watchTheme.cardBackground,
    onSurfaceVariant = watchTheme.secondaryTextColor
)

@Composable
fun WristReaderTheme(
    watchTheme: WatchTheme = WatchTheme.OLED_BLACK,
    content: @Composable () -> Unit
) {
    val colorScheme = watchThemeToColorScheme(watchTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
