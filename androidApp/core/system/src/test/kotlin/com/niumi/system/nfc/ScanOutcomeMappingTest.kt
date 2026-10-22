package com.niumi.system.nfc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Étape 38 : table des échecs de la feuille de scan (`SPECIFICATION.md` §14, SPEC_ANDROID §11.2).
 * Le NFC coupé l'emporte sur tout résultat ; un scan accepté ou ignoré n'est jamais un échec.
 */
class ScanOutcomeMappingTest {
    @Test
    fun eachRefusedScanHasItsReasonWhenNfcIsOn() {
        assertThat(scanFailureOf(ScanOutcome.NotANiumiPoint, NfcAvailability.ENABLED))
            .isEqualTo(ScanFailureReason.NOT_A_NIUMI_POINT)
        assertThat(scanFailureOf(ScanOutcome.UnknownBox, NfcAvailability.ENABLED))
            .isEqualTo(ScanFailureReason.NOT_REGISTERED)
        assertThat(scanFailureOf(ScanOutcome.Unreadable, NfcAvailability.ENABLED))
            .isEqualTo(ScanFailureReason.UNREADABLE)
    }

    @Test
    fun anAcceptedIgnoredOrMissingOutcomeIsNoFailure() {
        listOf(ScanOutcome.Accepted, ScanOutcome.Ignored, null).forEach { outcome ->
            assertThat(scanFailureOf(outcome, NfcAvailability.ENABLED)).isNull()
        }
    }

    @Test
    fun nfcTurnedOffWinsOverEveryOutcome() {
        ALL_OUTCOMES.forEach { outcome ->
            assertThat(scanFailureOf(outcome, NfcAvailability.DISABLED)).isEqualTo(ScanFailureReason.NFC_DISABLED)
        }
    }

    /** Pas de NFC du tout : l'écran le dit à la place de la consigne, aucune feuille d'échec. */
    @Test
    fun noNfcHardwareIsNoSheetFailure() {
        ALL_OUTCOMES.forEach { outcome ->
            assertThat(scanFailureOf(outcome, NfcAvailability.ABSENT)).isNull()
        }
    }

    private companion object {
        val ALL_OUTCOMES: List<ScanOutcome?> =
            listOf(
                ScanOutcome.Accepted,
                ScanOutcome.NotANiumiPoint,
                ScanOutcome.UnknownBox,
                ScanOutcome.Unreadable,
                ScanOutcome.Ignored,
                null,
            )
    }
}
