package com.niumi.feature.setup.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Écran 1 (`design-plan/SPECIFICATION.md` §4, `TEXTES-DES-MAQUETTES.md` §01–02) : les textes sont
 * recopiés mot pour mot, avec l'apostrophe typographique (D13, tranché le 2026-10-19). Les deux
 * titres s'écrivent sans point final (D12, tranché le 2026-10-20).
 */
class PresentationTextsTest {
    @Test
    fun theFirstPanelIsQuotedWordForWord() {
        assertThat(PresentationTexts.PANEL_1_TITLE).isEqualTo("À l’heure de te coucher, tes distractions sont bloquées")
        assertThat(PresentationTexts.PANEL_1_BODY)
            .isEqualTo("À l’heure que tu choisis, Niumi bloque les applications que tu as désignées.")
        assertThat(PresentationTexts.CONTINUE).isEqualTo("Continuer")
        assertThat(PresentationTexts.PANEL_1_DRAWING).isEqualTo("Un téléphone dont les applications sont verrouillées")
        assertThat(PresentationTexts.PANEL_1_INDICATOR).isEqualTo("Panneau 1 sur 2")
    }

    @Test
    fun theSecondPanelIsQuotedWordForWord() {
        assertThat(PresentationTexts.PANEL_2_TITLE).isEqualTo("Le matin, Niumi t’aide à te lever")
        assertThat(PresentationTexts.PANEL_2_BODY).isEqualTo(
            "Ton alarme sonne jusqu’à ce que tu scannes le Niumi Point situé dans la pièce de ton choix.",
        )
        assertThat(PresentationTexts.CONFIGURE).isEqualTo("Configurer Niumi")
        assertThat(PresentationTexts.BACK).isEqualTo("Retour")
        assertThat(PresentationTexts.PANEL_2_DRAWING).isEqualTo("Un téléphone approché d’un Niumi Point")
        assertThat(PresentationTexts.PANEL_2_INDICATOR).isEqualTo("Panneau 2 sur 2")
    }

    /** D12 : aucun des deux titres ne finit par un point. */
    @Test
    fun neitherTitleEndsWithAFullStop() {
        assertThat(PresentationTexts.PANEL_1_TITLE).doesNotMatch(".*\\.$")
        assertThat(PresentationTexts.PANEL_2_TITLE).doesNotMatch(".*\\.$")
    }

    /** D13 : l'apostrophe droite se glisse par recopie ; elle n'a pas sa place dans l'app. */
    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    /** Tutoiement partout (`SPECIFICATION.md` §2). */
    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts =
            listOf(
                PresentationTexts.PANEL_1_TITLE,
                PresentationTexts.PANEL_1_BODY,
                PresentationTexts.PANEL_2_TITLE,
                PresentationTexts.PANEL_2_BODY,
                PresentationTexts.CONTINUE,
                PresentationTexts.CONFIGURE,
                PresentationTexts.BACK,
                PresentationTexts.PANEL_1_DRAWING,
                PresentationTexts.PANEL_2_DRAWING,
                PresentationTexts.PANEL_1_INDICATOR,
                PresentationTexts.PANEL_2_INDICATOR,
            )
    }
}
