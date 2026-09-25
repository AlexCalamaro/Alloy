package com.squidink.alloy.core.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light theme colors
private val LightPrimary = Color(0xFF6200EE)
private val LightPrimaryContainer = Color(0xFFBB86FC)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightSecondary = Color(0xFF03DAC6)
private val LightSecondaryContainer = Color(0xFF03DAC6)
private val LightOnSecondary = Color(0xFF000000)
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF000000)
private val LightError = Color(0xFFB00020)

// Dark theme colors
private val DarkPrimary = Color(0xFFBB86FC)
private val DarkPrimaryContainer = Color(0xFF6200EE)
private val DarkOnPrimary = Color(0xFF000000)
private val DarkSecondary = Color(0xFF03DAC6)
private val DarkSecondaryContainer = Color(0xFF03DAC6)
private val DarkOnSecondary = Color(0xFF000000)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkOnSurface = Color(0xFFFFFFFF)
private val DarkError = Color(0xFFCF6679)

// Status colors
val SuccessColor = Color(0xFF4CAF50)
val WarningColor = Color(0xFFFF9800)
val ErrorColor = Color(0xFFB00020)
val InfoColor = Color(0xFF2196F3)
val DownloadColor = Color(0xFF4CAF50)
val UploadColor = Color(0xFF2196F3)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    error = LightError
)

@Composable
fun AlloyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
