package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = ElectricIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = VibrantPink,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceSubtle,
    outline = DarkBorder,
    error = CoralError
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricIndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = ElectricIndigoDark,
    secondary = NeonCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = VibrantPink,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceSubtle,
    outline = LightBorder,
    error = CoralError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent audio-themed styling
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
        typography = Typography,
        content = content
    )
}

