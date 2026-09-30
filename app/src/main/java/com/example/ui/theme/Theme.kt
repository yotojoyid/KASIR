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
    primary = PosDarkPrimary,
    onPrimary = PosDarkOnPrimary,
    primaryContainer = PosDarkPrimaryContainer,
    onPrimaryContainer = PosDarkOnPrimaryContainer,
    secondary = PosDarkSecondary,
    onSecondary = PosDarkOnSecondary,
    secondaryContainer = PosDarkSecondaryContainer,
    onSecondaryContainer = PosDarkOnSecondaryContainer,
    tertiary = PosDarkTertiary,
    onTertiary = PosDarkOnTertiary,
    tertiaryContainer = PosDarkTertiaryContainer,
    onTertiaryContainer = PosDarkOnTertiaryContainer,
    background = PosDarkBackground,
    onBackground = PosDarkOnBackground,
    surface = PosDarkSurface,
    onSurface = PosDarkOnSurface,
    surfaceVariant = PosDarkSurfaceVariant,
    onSurfaceVariant = PosDarkOnSurfaceVariant,
    outline = PosDarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = PosPrimary,
    onPrimary = PosOnPrimary,
    primaryContainer = PosPrimaryContainer,
    onPrimaryContainer = PosOnPrimaryContainer,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    onTertiary = PosOnTertiary,
    tertiaryContainer = PosTertiaryContainer,
    onTertiaryContainer = PosOnTertiaryContainer,
    background = PosBackground,
    onBackground = PosOnBackground,
    surface = PosSurface,
    onSurface = PosOnSurface,
    surfaceVariant = PosSurfaceVariant,
    onSurfaceVariant = PosOnSurfaceVariant,
    outline = PosOutline,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep POS custom branded colors
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
