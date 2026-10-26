package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_ANDROID §10.4 : tant que la session attend un scan, l'écran de réveil passe devant tout autre
 * écran de Niumi : le Reader Mode NFC n'existe que dans `AlarmActivity` (§11.2). `MainActivity`
 * applique [requiresAlarmScreen] quelle que soit la destination du `NavHost`.
 */
class LauncherDestinationTest {
    private val scanStates =
        listOf(
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        )

    @Test
    fun everyScanStateRequiresTheAlarmScreen() {
        scanStates.forEach { state -> assertThat(requiresAlarmScreen(state, releaseTarget = null)).isTrue() }
    }

    /**
     * Étape 43 (contrat 1.5) : un désarmement passe par `RELEASING` vers `DISARMED`, avant le coucher.
     * Rien n'est à scanner, à la différence de `RELEASING` vers `COMPLETED` ou `CANCELLED`.
     */
    @Test
    fun releasingTowardsDisarmedNeverOpensTheAlarmScreen() {
        assertThat(requiresAlarmScreen(SessionStateDto.RELEASING, ReleaseTargetDto.DISARMED)).isFalse()
        assertThat(requiresAlarmScreen(SessionStateDto.RELEASING, ReleaseTargetDto.COMPLETED)).isTrue()
    }

    /**
     * Étape 46, constaté sur appareil le 2026-10-26 : une annulation se fait depuis l'accueil
     * (`SPECIFICATION.md` §11). L'écran de réveil, ouvert pendant sa libération, passait devant
     * l'accueil puis le rouvrait, et la bulle « Réveil annulé… » se perdait.
     */
    @Test
    fun releasingTowardsCancelledNeverOpensTheAlarmScreen() {
        assertThat(requiresAlarmScreen(SessionStateDto.RELEASING, ReleaseTargetDto.CANCELLED)).isFalse()
    }

    /** Un état ajouté à la machine commune doit être classé explicitement. */
    @Test
    fun onlyTheScanStatesRequireTheAlarmScreen() {
        val required =
            (SessionStateDto.entries.map { it as SessionStateDto? } + null).filter {
                requiresAlarmScreen(
                    it,
                    null,
                )
            }

        assertThat(required).containsExactlyElementsIn(scanStates)
    }
}
