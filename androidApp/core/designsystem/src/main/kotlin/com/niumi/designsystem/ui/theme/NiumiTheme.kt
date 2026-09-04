package com.niumi.designsystem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Palette Niumi (`docs/CHARTE_GRAPHIQUE_APP_MOBILE.md`). Volontairement sombre par défaut :
 * l'application se manipule surtout la nuit et près du réveil (SPEC_ANDROID §15 : conserver un
 * contraste lisible la nuit ; charte §1 « Nuit → matin »).
 *
 * L'Ambre est l'unique accent actif des deux thèmes (charte §3 : « Plus l'Ambre est utilisé,
 * moins il signifie quelque chose ») ; son texte est systématiquement sombre (`SurCreme`), le
 * contraste Ambre/clair étant insuffisant pour du texte (§3 « Contraste » : ~1,9:1 sur Crème,
 * contre ~8,9:1 sur Encre pour l'Ambre lui-même).
 *
 * `dynamicColor` est désactivé pour garder une identité visuelle stable indépendante de
 * l'appareil.
 */
private val NiumiDarkColorScheme =
    darkColorScheme(
        primary = NiumiColors.Ambre,
        onPrimary = NiumiColors.SurCreme,
        background = NiumiColors.Encre,
        onBackground = NiumiColors.TextPrincipal,
        surface = NiumiColors.Surface,
        onSurface = NiumiColors.TextPrincipal,
        surfaceVariant = NiumiColors.Surface,
        onSurfaceVariant = NiumiColors.TextAttenue,
        outline = NiumiColors.Filet,
        outlineVariant = NiumiColors.Filet,
        error = NiumiColors.Terracotta,
        onError = NiumiColors.TextPrincipal,
    )

private val NiumiLightColorScheme =
    lightColorScheme(
        primary = NiumiColors.Ambre,
        onPrimary = NiumiColors.SurCreme,
        background = NiumiColors.Creme,
        onBackground = NiumiColors.SurCreme,
        surface = NiumiColors.Creme,
        onSurface = NiumiColors.SurCreme,
        surfaceVariant = NiumiColors.Creme,
        onSurfaceVariant = NiumiColors.SurCremeAttenue,
        outline = NiumiColors.SurCremeAttenue,
        outlineVariant = NiumiColors.SurCremeAttenue,
        error = NiumiColors.Terracotta,
        onError = NiumiColors.TextPrincipal,
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
