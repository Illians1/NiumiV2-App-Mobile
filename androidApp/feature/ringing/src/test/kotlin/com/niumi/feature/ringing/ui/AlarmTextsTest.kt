package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.component.NfcScanSheetContent
import com.niumi.system.nfc.ScanFailureReason
import org.junit.Test

/**
 * Textes de l'écran 10 (étape 38), recopiés de `SPECIFICATION.md` §14 et de
 * `TEXTES-DES-MAQUETTES.md` §28 à §33, apostrophe typographique (D13). Le libellé du chargement
 * de fin de scan et l'absence de NFC ne sont pas dessinés : validés par Mehdi le 2026-10-22.
 */
class AlarmTextsTest {
    @Test
    fun theScreenIsQuotedWordForWord() {
        // Décision de Mehdi du 2026-10-22 : la consigne parle du déplacement, pas du scan, qui ne
        // marche qu'après « Scanner le Niumi Spot » (Q4). Remplace « Scanne ton Niumi Spot ».
        assertThat(AlarmTexts.INSTRUCTION).isEqualTo("Rejoins ton Niumi Spot")
        assertThat(AlarmTexts.SCAN_BUTTON).isEqualTo("Scanner le Niumi Spot")
        assertThat(AlarmTexts.DRAWING).isEqualTo("Un téléphone approché d’un Niumi Spot")
    }

    @Test
    fun theFourFailuresAreQuotedWordForWord() {
        assertThat(AlarmTexts.failure(ScanFailureReason.NOT_A_NIUMI_SPOT))
            .isEqualTo(
                NfcScanSheetContent.Failure(
                    title = "Ce n’est pas un Niumi Spot",
                    help = "Scanne un Niumi Spot que tu as enregistré.",
                    actionLabel = "Réessayer",
                ),
            )
        assertThat(AlarmTexts.failure(ScanFailureReason.NOT_REGISTERED))
            .isEqualTo(
                NfcScanSheetContent.Failure(
                    title = "Ce Niumi Spot n’est pas enregistré",
                    help = "Scanne un Niumi Spot que tu as enregistré.",
                    actionLabel = "Réessayer",
                ),
            )
        assertThat(AlarmTexts.failure(ScanFailureReason.UNREADABLE))
            .isEqualTo(
                NfcScanSheetContent.Failure(
                    title = "Ton Niumi Spot n’a pas pu être lu",
                    help = "Rapproche ton téléphone et réessaie.",
                    actionLabel = "Réessayer",
                ),
            )
        assertThat(AlarmTexts.failure(ScanFailureReason.NFC_DISABLED))
            .isEqualTo(
                NfcScanSheetContent.Failure(
                    title = "Le NFC est coupé",
                    help = "Active-le pour scanner ton Niumi Spot.",
                    actionLabel = "Ouvrir les réglages",
                ),
            )
    }

    @Test
    fun theFailureSheetIsAnnouncedWithTheLabelOfTheMockups() {
        assertThat(AlarmTexts.FAILURE_PANE).isEqualTo("Échec du scan")
    }

    @Test
    fun theUndrawnTextsKeepTheWordingValidatedOnTheTwentySecond() {
        assertThat(AlarmTexts.RELEASE_LOADER).isEqualTo("Fin du réveil")
        assertThat(AlarmTexts.NFC_ABSENT).isEqualTo("Cet appareil ne prend pas en charge le NFC.")
    }

    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    /** « Niumi Spot » partout ; Lot 9 : l'écran n'est jamais affiché verrouillé. */
    @Test
    fun noTextSpeaksOfABoxOrAsksToUnlock() {
        assertThat(allTexts.filter { "boîtier" in it.lowercase() || "errouill" in it }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts: List<String> =
            listOf(
                AlarmTexts.INSTRUCTION,
                AlarmTexts.SCAN_BUTTON,
                AlarmTexts.DRAWING,
                AlarmTexts.FAILURE_PANE,
                AlarmTexts.RELEASE_LOADER,
                AlarmTexts.NFC_ABSENT,
            ) +
                ScanFailureReason.entries.flatMap {
                    val failure = AlarmTexts.failure(it)
                    listOf(failure.title, failure.help, failure.actionLabel)
                }
    }
}
