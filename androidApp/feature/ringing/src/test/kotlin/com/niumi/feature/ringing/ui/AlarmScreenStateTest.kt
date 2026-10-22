package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanSheetState
import org.junit.Test

/**
 * État d'affichage de l'écran 10 (étape 38) : une seule consigne pour les trois états d'attente
 * (décision du 2026-10-22), le chargement seul pendant `RELEASING`, et une lecture NFC demandée
 * seulement feuille « Prêt à scanner » ouverte (Q4).
 */
class AlarmScreenStateTest {
    private fun stateFor(
        sessionState: SessionStateDto = SessionStateDto.RINGING,
        nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
        sheet: ScanSheetState = ScanSheetState.Hidden,
    ) = AlarmScreenState.from(sessionState, nfcAvailability, sheet)

    @Test
    fun theThreeWaitingStatesShowTheSameInstructionAndTheScanButton() {
        WAITING_STATES.forEach { session ->
            val state = stateFor(session)
            assertThat(state.instructionText).isEqualTo(AlarmTexts.INSTRUCTION)
            assertThat(state.showsScanButton).isTrue()
            assertThat(state.showsReleaseLoader).isFalse()
        }
    }

    /** Décision du 2026-10-22 : après le scan, la date, l'heure et le chargement seuls. */
    @Test
    fun releasingShowsOnlyTheLoader() {
        val state = stateFor(SessionStateDto.RELEASING)

        assertThat(state.showsReleaseLoader).isTrue()
        assertThat(state.instructionText).isNull()
        assertThat(state.showsScanButton).isFalse()
    }

    /** Le scan a eu lieu : la feuille se ferme, quoi qu'elle montrait, et plus rien n'écoute. */
    @Test
    fun releasingClosesTheSheet() {
        listOf(ScanSheetState.Ready, ScanSheetState.Failed(ScanFailureReason.UNREADABLE)).forEach { sheet ->
            val state = stateFor(SessionStateDto.RELEASING, sheet = sheet)
            assertThat(state.sheet).isEqualTo(ScanSheetState.Hidden)
            assertThat(state.readerModeRequested).isFalse()
        }
    }

    @Test
    fun missingNfcHardwareReplacesTheInstructionAndHidesTheButton() {
        val state = stateFor(nfcAvailability = NfcAvailability.ABSENT, sheet = ScanSheetState.Ready)

        assertThat(state.instructionText).isEqualTo(AlarmTexts.NFC_ABSENT)
        assertThat(state.showsScanButton).isFalse()
        assertThat(state.sheet).isEqualTo(ScanSheetState.Hidden)
        assertThat(state.readerModeRequested).isFalse()
    }

    /** Q4 : la lecture n'est demandée que feuille « Prêt à scanner » ouverte et NFC allumé. */
    @Test
    fun theReaderIsRequestedOnlyWhileTheReadySheetIsOpen() {
        assertThat(stateFor(sheet = ScanSheetState.Hidden).readerModeRequested).isFalse()
        assertThat(stateFor(sheet = ScanSheetState.Ready).readerModeRequested).isTrue()
        assertThat(stateFor(sheet = ScanSheetState.Failed(ScanFailureReason.NOT_REGISTERED)).readerModeRequested)
            .isFalse()
        assertThat(
            stateFor(nfcAvailability = NfcAvailability.DISABLED, sheet = ScanSheetState.Ready).readerModeRequested,
        ).isFalse()
    }

    /** L'instruction ne bouge pas avec le NFC ni la feuille : les échecs se disent dans la feuille. */
    @Test
    fun theInstructionDoesNotChangeWithTheNfcOrTheSheet() {
        val state =
            stateFor(
                nfcAvailability = NfcAvailability.DISABLED,
                sheet = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED),
            )

        assertThat(state.instructionText).isEqualTo(AlarmTexts.INSTRUCTION)
        assertThat(state.showsScanButton).isTrue()
    }

    private companion object {
        val WAITING_STATES =
            listOf(SessionStateDto.RINGING, SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC)
    }
}
