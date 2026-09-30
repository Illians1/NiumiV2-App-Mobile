package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.readiness.UnlockSettling
import com.niumi.system.readiness.fakes.FakeAlarmVolumeSource
import com.niumi.system.readiness.fakes.FakeNfcReader
import com.niumi.system.readiness.fakes.FakeNotificationAvailability
import com.niumi.system.session.fakes.FakeAccessibilityServiceStatus
import com.niumi.system.session.fakes.FakeAlarmScheduler
import com.niumi.system.session.fakes.FakeUptimeClock
import org.junit.Test

/**
 * [DefaultSessionRuntimeStatusProbe] n'avait aucun test avant l'étape 25 : la réconciliation passait
 * toujours par une doublure. Ce test porte ce qu'elle seule décide — quand le NFC peut être jugé
 * ([SessionNfcEvaluability]) — sur ses vraies sources.
 */
class DefaultSessionRuntimeStatusProbeTest {
    private val nfcReader = FakeNfcReader(availabilityValue = NfcAvailability.DISABLED)
    private val unlockState = FakeUnlockState(isUserUnlocked = false)
    private val uptimeClock = FakeUptimeClock()
    private val probe =
        DefaultSessionRuntimeStatusProbe(
            alarmScheduler = FakeAlarmScheduler(),
            accessibilityServiceStatus = FakeAccessibilityServiceStatus(),
            notificationAvailability = FakeNotificationAvailability(),
            nfcReader = nfcReader,
            alarmVolumeSource = FakeAlarmVolumeSource(),
            nfcEvaluability = SessionNfcEvaluability(UnlockSettling(unlockState, uptimeClock)),
        )

    /** Avant le premier déverrouillage, le NFC n'est jamais jugé, quel que soit le temps écoulé. */
    @Test
    fun theNfcCannotBeJudgedBeforeTheFirstUnlock() {
        unlockState.isUserUnlocked = false

        assertThat(probe.probe(SESSION_ID).nfcEvaluable).isFalse()
    }

    /**
     * Mesuré le 2026-09-27 : le service NFC démarre **après** le déverrouillage. Le temps écoulé
     * depuis le démarrage n'y dit rien — seul compte le temps écoulé depuis le déverrouillage.
     */
    @Test
    fun theNfcCannotBeJudgedJustBeforeTheEndOfTheWindowThatFollowsTheUnlock() {
        uptimeClock.elapsedMillis = 600_000L
        probe.probe(SESSION_ID)
        unlockState.isUserUnlocked = true
        probe.probe(SESSION_ID)
        uptimeClock.elapsedMillis += UnlockSettling.GRACE_MILLIS - 1

        assertThat(probe.probe(SESSION_ID).nfcEvaluable).isFalse()
    }

    @Test
    fun theNfcIsJudgedOnceTheWindowThatFollowsTheUnlockIsOver() {
        unlockState.isUserUnlocked = true
        probe.probe(SESSION_ID)
        uptimeClock.elapsedMillis += UnlockSettling.GRACE_MILLIS

        assertThat(probe.probe(SESSION_ID).nfcEvaluable).isTrue()
    }

    /**
     * « Non jugeable » ne maquille pas l'adaptateur : `nfcReady` dit toujours ce qu'il lit. C'est
     * [RuntimeStatusGaps] qui décide de n'en rien conclure, pas la sonde qui ment.
     */
    @Test
    fun nfcReadyStillReportsTheAdapterWhenTheNfcCannotBeJudged() {
        assertThat(probe.probe(SESSION_ID).nfcReady).isFalse()

        nfcReader.availabilityValue = NfcAvailability.ENABLED
        assertThat(probe.probe(SESSION_ID).nfcReady).isTrue()
    }

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
