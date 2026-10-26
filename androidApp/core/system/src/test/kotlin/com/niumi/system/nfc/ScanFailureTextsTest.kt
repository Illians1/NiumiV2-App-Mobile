package com.niumi.system.nfc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Textes des échecs du scan, partagés par l'écran 10 et la feuille d'annulation (étape 46),
 * recopiés de `design-plan/SPECIFICATION.md` §14, apostrophe typographique (D13).
 */
class ScanFailureTextsTest {
    @Test
    fun eachFailureHasTheWordingOfTheSpecification() {
        assertThat(ScanFailureTexts.of(ScanFailureReason.NOT_A_NIUMI_SPOT))
            .isEqualTo(
                ScanFailureCopy(
                    "Ce n’est pas un Niumi Spot.",
                    "Scanne un Niumi Spot que tu as enregistré.",
                    "Réessayer",
                ),
            )
        assertThat(ScanFailureTexts.of(ScanFailureReason.NOT_REGISTERED))
            .isEqualTo(
                ScanFailureCopy(
                    "Ce Niumi Spot n’est pas enregistré.",
                    "Scanne un Niumi Spot que tu as enregistré.",
                    "Réessayer",
                ),
            )
        assertThat(ScanFailureTexts.of(ScanFailureReason.UNREADABLE))
            .isEqualTo(
                ScanFailureCopy(
                    "Ton Niumi Spot n’a pas pu être lu.",
                    "Rapproche ton téléphone et réessaie.",
                    "Réessayer",
                ),
            )
        assertThat(ScanFailureTexts.of(ScanFailureReason.NFC_DISABLED))
            .isEqualTo(
                ScanFailureCopy("Le NFC est coupé.", "Active-le pour scanner ton Niumi Spot.", "Ouvrir les réglages"),
            )
    }

    @Test
    fun theFailureSheetIsAnnouncedWithTheLabelOfTheMockups() {
        assertThat(ScanFailureTexts.FAILURE_PANE).isEqualTo("Échec du scan")
    }

    @Test
    fun noTextCarriesAStraightApostropheOrTheFormalAddress() {
        val all =
            ScanFailureReason.entries.flatMap {
                val copy = ScanFailureTexts.of(it)
                listOf(copy.title, copy.help, copy.actionLabel)
            } + ScanFailureTexts.FAILURE_PANE
        assertThat(all.filter { it.contains('\'') }).isEmpty()
        assertThat(all.filter { Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""").containsMatchIn(it) }).isEmpty()
    }
}
