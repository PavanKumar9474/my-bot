package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CosmicVioletDark,
    secondary = ElectricTealDark,
    tertiary = NeonPinkDark,
    background = MidnightBlack,
    surface = CardSurfaceDark,
    onBackground = OnSurfaceWhite,
    onSurface = OnSurfaceWhite,
    primaryContainer = CosmicViolet.copy(alpha = 0.3f),
    onPrimaryContainer = OnSurfaceWhite,
    secondaryContainer = ElectricTeal.copy(alpha = 0.2f),
    onSecondaryContainer = ElectricTealDark,
    surfaceVariant = CardSurfaceDark.copy(alpha = 0.8f),
    onSurfaceVariant = OnSurfaceGrey
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicViolet,
    secondary = ElectricTeal,
    tertiary = NeonPink,
    background = OnSurfaceWhite,
    surface = OnSurfaceWhite,
    onBackground = MidnightBlack,
    onSurface = MidnightBlack,
    primaryContainer = CosmicViolet.copy(alpha = 0.15f),
    onPrimaryContainer = CosmicViolet,
    secondaryContainer = ElectricTeal.copy(alpha = 0.15f),
    onSecondaryContainer = ElectricTeal,
    surfaceVariant = OnSurfaceWhite.copy(alpha = 0.9f),
    onSurfaceVariant = MidnightBlack.copy(alpha = 0.6f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to force our gorgeous cosmic violet branding colors!
    content: @Composable () -> Unit,
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
