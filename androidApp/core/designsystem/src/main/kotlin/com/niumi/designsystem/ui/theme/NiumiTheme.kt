package com.niumi.designsystem.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Thème Niumi (`design-plan/VALEURS-DE-DESIGN.md`). [dark] n'a pas de valeur par défaut : il vient
 * de l'état de la session (`isSessionDark()`, `:core:system`) dans `MainActivity`, et vaut
 * toujours `true` dans `AlarmActivity`. Le réglage clair ou sombre du téléphone n'est jamais lu.
 *
 * Fournit [LocalNiumiPalette], que lisent les écrans, et un `MaterialTheme` entièrement dérivé de
 * la palette et de [NiumiType] pour les composants Material encore présents.
 */
@Composable
fun NiumiTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = if (dark) NiumiPalettes.Dark else NiumiPalettes.Light
    CompositionLocalProvider(LocalNiumiPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(dark),
            typography = NiumiType.material(),
            content = content,
        )
    }
}

/**
 * `ColorScheme` Material 3 dérivé de la palette, pour les composants Material résiduels. Les
 * rôles du design sans équivalent Material (piste, tuile grise, teinte ambre…) ne passent que par
 * [LocalNiumiPalette]. Aucune couleur d'alerte : `error` est l'encre, les échecs se disent en texte.
 * L'ambre n'est l'accent d'aucun rôle Material (règle de l'ambre, D14). Aucune teinte d'élévation.
 */
fun NiumiPalette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink,
        onPrimary = onInk,
        primaryContainer = tileGrey,
        onPrimaryContainer = ink,
        inversePrimary = background,
        secondary = ink,
        onSecondary = onInk,
        secondaryContainer = tileGrey,
        onSecondaryContainer = ink,
        tertiary = ink,
        onTertiary = onInk,
        tertiaryContainer = tileGrey,
        onTertiaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = tileGrey,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = ink,
        inverseOnSurface = background,
        error = ink,
        onError = onInk,
        errorContainer = tileGrey,
        onErrorContainer = ink,
        outline = border,
        outlineVariant = separator,
        scrim = Color.Black,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
    )
}
