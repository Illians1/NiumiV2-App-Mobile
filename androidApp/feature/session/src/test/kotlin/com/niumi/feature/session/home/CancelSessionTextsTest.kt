package com.niumi.feature.session.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Feuille « Annuler le réveil ? » et message qui suit (`SPECIFICATION.md` §11, D23 ; maquettes 25
 * à 27, dont les sources régénérées le 2026-10-25 disent « session » par régression, E14 ; étape
 * 46), mot pour mot, tutoiement, apostrophe typographique (D13).
 */
class CancelSessionTextsTest {
    @Test
    fun theSheetTextsAreQuotedWordForWord() {
        assertThat(CancelSessionTexts.TITLE).isEqualTo("Annuler le réveil ?")
        assertThat(CancelSessionTexts.BODY)
            .isEqualTo(
                "Scanne ton Niumi Spot pour annuler. Tes applications seront débloquées et ton réveil désactivé.",
            )
        assertThat(CancelSessionTexts.SCAN_BUTTON).isEqualTo("Scanner le Niumi Spot")
    }

    @Test
    fun theScanSheetSaysWhyItScans() {
        assertThat(
            CancelSessionTexts.SCAN_PHRASE,
        ).isEqualTo("Approche ton téléphone du Niumi Spot pour annuler le réveil")
    }

    /** Choix de Mehdi du 2026-10-26 : cercle seul, lu par TalkBack (texte non dessiné). */
    @Test
    fun theReleaseLoaderIsAnnouncedAsTheCancellationOfTheWake() {
        assertThat(CancelSessionTexts.RELEASE_LOADER).isEqualTo("Annulation du réveil")
    }

    @Test
    fun theHomeMessageConfirmsTheCancellation() {
        assertThat(HomeTexts.WAKE_CANCELLED).isEqualTo("Réveil annulé. Tes applications sont débloquées.")
    }

    @Test
    fun noTextSpeaksOfASessionOrSwitchesToTheFormalAddress() {
        val all =
            listOf(
                CancelSessionTexts.TITLE,
                CancelSessionTexts.BODY,
                CancelSessionTexts.SCAN_BUTTON,
                CancelSessionTexts.SCAN_PHRASE,
                CancelSessionTexts.RELEASE_LOADER,
                HomeTexts.WAKE_CANCELLED,
            )
        assertThat(all.filter { "session" in it.lowercase() }).isEmpty()
        assertThat(all.filter { Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""").containsMatchIn(it) }).isEmpty()
        assertThat(all.filter { it.contains('\'') }).isEmpty()
    }
}
