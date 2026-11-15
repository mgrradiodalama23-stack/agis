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

private val AtlasDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B7F),
    onPrimary = Color(0xFF4D0011),
    primaryContainer = AtlasCrimsonDark,
    onPrimaryContainer = AtlasRoseTint,
    secondary = AtlasCoral,
    onSecondary = Color(0xFF3A0900),
    background = AtlasSurfaceDark,
    onBackground = AtlasOnSurfaceDark,
    surface = AtlasSurfaceDark,
    onSurface = AtlasOnSurfaceDark,
    surfaceVariant = AtlasSurfaceContainerDark,
    onSurfaceVariant = Color(0xFFCBBDD6),
    outline = AtlasOutlineDark
)

private val AtlasLightColorScheme = lightColorScheme(
    primary = AtlasCrimson,
    onPrimary = Color.White,
    primaryContainer = AtlasRoseTint,
    onPrimaryContainer = AtlasCrimsonDark,
    secondary = AtlasCoral,
    onSecondary = Color.White,
    background = AtlasSurfaceLight,
    onBackground = AtlasInk,
    surface = AtlasSurfaceLight,
    onSurface = AtlasInk,
    surfaceVariant = AtlasSurfaceVariantLight,
    onSurfaceVariant = AtlasMuted,
    outline = AtlasOutlineLight
)

@Composable
fun AtlasArea7Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AtlasDarkColorScheme
        else -> AtlasLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
