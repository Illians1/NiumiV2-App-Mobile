package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_ANDROID §7.1, §18 (étape 20). Prouve que [RuntimeStatusGaps] ne recoupe jamais les quatre
 * contrôles déjà tenus par `SessionReadinessMonitor` (§13.1) — c'est ce test qui matérialise la
 * décision de l'étape 20 de ne brancher la sonde que sur son écart réel.
 */
class RuntimeStatusGapsTest {
    private val healthy =
        SessionRuntimeStatus(
            alarmScheduled = true,
            accessibilityReady = true,
            notificationReady = true,
            fullScreenReady = true,
            nfcReady = true,
            audioReady = true,
            nfcEvaluable = true,
        )

    @Test
    fun anArmedSessionWithoutAScheduledAlarmIsAGap() {
        val status = healthy.copy(alarmScheduled = false)

        assertThat(RuntimeStatusGaps.of(status, SessionStateDto.ARMED)).containsExactly(RuntimeGap.ALARM_NOT_SCHEDULED)
    }

    /** L'absence d'alarme est normale hors `ARMED` : la session sonne, attend un scan ou se ferme. */
    @Test
    fun theAlarmGapIsIgnoredOutsideArmed() {
        val status = healthy.copy(alarmScheduled = false)

        listOf(
            SessionStateDto.PREPARING,
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        ).forEach { state ->
            assertThat(RuntimeStatusGaps.of(status, state)).doesNotContain(RuntimeGap.ALARM_NOT_SCHEDULED)
        }
    }

    @Test
    fun nfcDisabledIsAGapInEveryNonFinalStateExceptPreparing() {
        val status = healthy.copy(nfcReady = false)

        listOf(
            SessionStateDto.ARMED,
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        ).forEach { state ->
            assertThat(RuntimeStatusGaps.of(status, state)).contains(RuntimeGap.NFC_DISABLED)
        }
    }

    /**
     * Étape 25 : un NFC qui ne peut pas être jugé — appareil pas encore déverrouillé, ou démarrage
     * trop récent — ne produit aucun écart, même lu « désactivé ». « Non jugeable » n'est pas
     * « prêt » : le champ `nfcReady` garde sa valeur, seule la conclusion est retenue.
     */
    @Test
    fun anNfcThatCannotBeJudgedYetIsNeverAGap() {
        val status = healthy.copy(nfcReady = false, nfcEvaluable = false)

        SessionStateDto.entries.forEach { state ->
            assertThat(RuntimeStatusGaps.of(status, state)).doesNotContain(RuntimeGap.NFC_DISABLED)
        }
    }

    /** Le drapeau ne concerne que le NFC : une alarme disparue reste un écart. */
    @Test
    fun anUnjudgeableNfcDoesNotHideAMissingAlarm() {
        val status = healthy.copy(alarmScheduled = false, nfcReady = false, nfcEvaluable = false)

        assertThat(RuntimeStatusGaps.of(status, SessionStateDto.ARMED)).containsExactly(RuntimeGap.ALARM_NOT_SCHEDULED)
    }

    /** Une activation interrompue n'a jamais promis de scan : pas d'écart NFC en `PREPARING`. */
    @Test
    fun nfcDisabledIsNotAGapDuringPreparing() {
        val status = healthy.copy(nfcReady = false)

        assertThat(RuntimeStatusGaps.of(status, SessionStateDto.PREPARING)).isEmpty()
    }

    @Test
    fun noGapOutlivesASession() {
        val status = SessionRuntimeStatus(false, false, false, false, false, false, nfcEvaluable = true)

        listOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED).forEach { state ->
            assertThat(RuntimeStatusGaps.of(status, state)).isEmpty()
        }
    }

    /**
     * Les quatre contrôles déjà tenus par `SessionReadinessMonitor` — accessibilité,
     * notifications, plein écran, audio — ne produisent **jamais** d'écart ici, quel que soit
     * l'état : un second contrôle sur les mêmes points recréerait les doublons d'incidents
     * corrigés à l'étape 16.
     */
    @Test
    fun theFourChecksAlreadyOwnedByTheReadinessMonitorAreNeverReportedHere() {
        val status =
            SessionRuntimeStatus(
                alarmScheduled = true,
                accessibilityReady = false,
                notificationReady = false,
                fullScreenReady = false,
                nfcReady = true,
                audioReady = false,
                nfcEvaluable = true,
            )

        SessionStateDto.entries.forEach { state ->
            assertThat(RuntimeStatusGaps.of(status, state)).isEmpty()
        }
    }

    @Test
    fun aHealthyRuntimeProducesNoGap() {
        SessionStateDto.entries.forEach { state ->
            assertThat(RuntimeStatusGaps.of(healthy, state)).isEmpty()
        }
    }
}
