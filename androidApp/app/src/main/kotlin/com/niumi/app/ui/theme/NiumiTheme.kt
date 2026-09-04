package com.niumi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Palette Niumi. Volontairement sombre par défaut : l'application se manipule surtout la nuit
 * et près du réveil (SPEC_ANDROID §15 : conserver un contraste lisible la nuit).
 * `dynamicColor` est désactivé pour garder une identité visuelle stable indépendante de l'appareil.
 */
private val NiumiDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF8AB4FF),
        onPrimary = Color(0xFF00285C),
        background = Color(0xFF0E0F13),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF0E0F13),
        onSurface = Color(0xFFE3E2E6),
    )

private val NiumiLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF2F5DCB),
        onPrimary = Color(0xFFFFFFFF),
        background = Color(0xFFFBFAFF),
        onBackground = Color(0xFF1A1B20),
        surface = Color(0xFFFBFAFF),
        onSurface = Color(0xFF1A1B20),
    )

@Composable
fun NiumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NiumiDarkColorScheme else NiumiLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
