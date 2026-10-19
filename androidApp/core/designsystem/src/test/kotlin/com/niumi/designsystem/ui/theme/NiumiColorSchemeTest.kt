package com.niumi.designsystem.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Le `ColorScheme` Material 3 n'existe que pour les composants Material encore présents (anciens
 * écrans jusqu'à leur refonte, champ de recherche, feuille modale). Il est entièrement dérivé de la
 * palette Niumi : aucune couleur Material par défaut, et aucune couleur d'alerte (les échecs se
 * disent en texte, plan de refonte, Lot 10, décision 3).
 */
class NiumiColorSchemeTest {
    private val cases = listOf(false to NiumiPalettes.Light, true to NiumiPalettes.Dark)

    @Test
    fun theMainRolesComeFromThePalette() {
        cases.forEach { (dark, palette) ->
            val scheme = palette.toColorScheme(dark)
            val label = if (dark) "sombre" else "clair"
            assertWithMessage("$label primary").that(scheme.primary).isEqualTo(palette.ink)
            assertWithMessage("$label onPrimary").that(scheme.onPrimary).isEqualTo(palette.onInk)
            assertWithMessage("$label background").that(scheme.background).isEqualTo(palette.background)
            assertWithMessage("$label onBackground").that(scheme.onBackground).isEqualTo(palette.ink)
            assertWithMessage("$label surface").that(scheme.surface).isEqualTo(palette.background)
            assertWithMessage("$label onSurface").that(scheme.onSurface).isEqualTo(palette.ink)
            assertWithMessage("$label onSurfaceVariant").that(scheme.onSurfaceVariant).isEqualTo(palette.textSecondary)
            assertWithMessage("$label outline").that(scheme.outline).isEqualTo(palette.border)
            assertWithMessage("$label outlineVariant").that(scheme.outlineVariant).isEqualTo(palette.separator)
            assertWithMessage("$label cartes et feuilles")
                .that(
                    listOf(
                        scheme.surfaceContainerLowest,
                        scheme.surfaceContainerLow,
                        scheme.surfaceContainer,
                        scheme.surfaceContainerHigh,
                        scheme.surfaceContainerHighest,
                    ).distinct(),
                ).containsExactly(palette.surface)
        }
    }

    @Test
    fun thereIsNoAlertColour() {
        cases.forEach { (dark, palette) ->
            val scheme = palette.toColorScheme(dark)
            assertThat(scheme.error).isEqualTo(palette.ink)
            assertThat(scheme.onError).isEqualTo(palette.onInk)
            assertThat(scheme.errorContainer).isEqualTo(palette.tileGrey)
            assertThat(scheme.onErrorContainer).isEqualTo(palette.ink)
        }
    }

    /** L'ambre ne sert d'accent Material nulle part : il reste réservé aux dessins et à l'interrupteur (D14). */
    @Test
    fun amberIsNotAMaterialAccent() {
        cases.forEach { (dark, palette) ->
            val scheme = palette.toColorScheme(dark)
            val accents =
                listOf(
                    scheme.primary,
                    scheme.secondary,
                    scheme.tertiary,
                    scheme.primaryContainer,
                    scheme.secondaryContainer,
                    scheme.tertiaryContainer,
                )
            assertWithMessage(if (dark) "sombre" else "clair").that(accents).doesNotContain(palette.amber)
        }
    }

    /** Aucune teinte d'élévation : une surface élevée garde la couleur de la palette. */
    @Test
    fun elevationAddsNoTint() {
        cases.forEach { (dark, palette) ->
            assertThat(palette.toColorScheme(dark).surfaceTint).isEqualTo(Color.Transparent)
        }
    }
}
