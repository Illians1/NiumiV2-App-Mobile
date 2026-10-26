package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * Sort de l'écran de réveil pour chacun des neuf états de la machine commune (SPEC_ANDROID §10.4,
 * §15). Aucun état n'est traité par défaut : un état ajouté au moteur casse la compilation ici.
 */
class AlarmUiStateTest {
    private fun forState(state: SessionStateDto) = AlarmUiState.forSession(state, SESSION_ID)

    @Test
    fun theFourScanStatesAreDisplayed() {
        listOf(
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        ).forEach { state ->
            val ui = forState(state)

            assertThat(ui).isInstanceOf(AlarmUiState.Visible::class.java)
            assertThat((ui as AlarmUiState.Visible).screen.sessionState).isEqualTo(state)
        }
    }

    /** Étape 46 : l'écran 11 relit la session terminée par son identifiant. */
    @Test
    fun aCompletedSessionLeavesForTheSuccessScreenOfThatSession() {
        assertThat(forState(SessionStateDto.COMPLETED))
            .isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Success(SESSION_ID)))
    }

    /** Étape 46 : plus d'écran « Session annulée », l'accueil dit le reste. */
    @Test
    fun aCancelledSessionLeavesForHome() {
        assertThat(forState(SessionStateDto.CANCELLED)).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Home))
    }

    /** §18 réserve `FAILED` à une activation jamais aboutie : aucun écran de §15 ne lui répond. */
    @Test
    fun aFailedSessionFallsBackToHome() {
        assertThat(forState(SessionStateDto.FAILED))
            .isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Home))
    }

    /** Contrat 1.5 : une session désarmée avant son coucher n'a aucun écran de fin. */
    @Test
    fun aDisarmedSessionFallsBackToHome() {
        assertThat(forState(SessionStateDto.DISARMED))
            .isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Home))
    }

    /** Avant la sonnerie, l'écran 7 est le seul écran atteignable (§10.4). */
    @Test
    fun theStatesBeforeRingingCloseTheAlarmScreen() {
        assertThat(forState(SessionStateDto.PREPARING)).isEqualTo(AlarmUiState.Close)
        assertThat(forState(SessionStateDto.ARMED)).isEqualTo(AlarmUiState.Close)
    }

    @Test
    fun everyStateOfTheCommonMachineIsClassified() {
        val classified = SessionStateDto.entries.associateWith { forState(it) }

        assertThat(classified).hasSize(SessionStateDto.entries.size)
        assertThat(classified.filterValues { it is AlarmUiState.Visible }).hasSize(4)
        // COMPLETED, CANCELLED, DISARMED (contrat 1.5, vers l'accueil) et FAILED.
        assertThat(classified.filterValues { it is AlarmUiState.Exit }).hasSize(4)
        assertThat(classified.filterValues { it == AlarmUiState.Close }).hasSize(2)
    }

    private companion object {
        const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
    }
}
