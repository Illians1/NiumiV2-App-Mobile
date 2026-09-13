package com.niumi.system.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.LoadResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

/**
 * Reconstruction du service de sonnerie après une mort de processus (SPEC_ANDROID §10.2 :
 * « reconstruire son état depuis le snapshot si le processus est recréé »). Classe pure : le
 * service, lui, n'a plus qu'à exécuter la décision.
 */
class RingingServiceRecoveryTest {
    private fun present(
        state: SessionStateDto,
        revision: Long = 3,
    ) = LoadResult.Present(
        snapshot = SessionDtoFixtures.snapshotInState(state, revision = revision),
        extras = SessionDtoFixtures.extras(),
        pendingEffects = emptyList(),
    )

    @Test
    fun aRingingSessionResumesTheSound() {
        val decision = RingingServiceRecovery.decide(present(SessionStateDto.RINGING, revision = 7))

        assertThat(decision)
            .isEqualTo(RingingRecovery.ResumeRinging(SessionDtoFixtures.SESSION_ID, revision = 7))
    }

    /**
     * `START_STICKY` peut relancer le service **sans** recréer le processus : `PROCESS_START` n'a
     * alors pas lieu, d'où une raison de réconciliation dédiée.
     */
    @Test
    fun anArmedSessionReconcilesThenStops() {
        val decision = RingingServiceRecovery.decide(present(SessionStateDto.ARMED))

        assertThat(decision).isEqualTo(RingingRecovery.ReconcileAndStop)
    }

    /** §18 : pendant `RELEASING`, conserver les effets incomplets sans rien restaurer. */
    @Test
    fun aReleasingSessionStopsWithoutTouchingTheState() {
        val decision = RingingServiceRecovery.decide(present(SessionStateDto.RELEASING))

        assertThat(decision).isEqualTo(RingingRecovery.StopWithoutTouchingState)
    }

    /** §10.4 : ces deux états attendent un scan, sans audio. */
    @Test
    fun theScanAwaitingStatesStopWithoutSound() {
        listOf(SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC).forEach { state ->
            assertThat(RingingServiceRecovery.decide(present(state)))
                .isEqualTo(RingingRecovery.StopWithoutTouchingState)
        }
    }

    @Test
    fun aPreparingSessionStopsWithoutTouchingTheState() {
        val decision = RingingServiceRecovery.decide(present(SessionStateDto.PREPARING))

        assertThat(decision).isEqualTo(RingingRecovery.StopWithoutTouchingState)
    }

    @Test
    fun everyFinalStateStops() {
        listOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)
            .forEach { state ->
                assertThat(RingingServiceRecovery.decide(present(state)))
                    .isEqualTo(RingingRecovery.StopWithoutTouchingState)
            }
    }

    @Test
    fun noSessionRefusesToRing() {
        val decision = RingingServiceRecovery.decide(LoadResult.Absent)

        assertThat(decision).isEqualTo(RingingRecovery.StopWithoutTouchingState)
    }

    /**
     * Un snapshot illisible ne fait ni sonner ni effacer : une alarme sans bouton d'arrêt
     * (SPEC_ANDROID §10.2) sur une session peut-être terminée serait pire que le silence, et §18
     * conserve le blocage. La réconciliation rendra `SnapshotCorrupted` et le prochain
     * `PROCESS_START` ou `USER_UNLOCKED` reprendra.
     */
    @Test
    fun anUnreadableSnapshotNeitherRingsNorClears() {
        val decision = RingingServiceRecovery.decide(LoadResult.Unreadable("SNAPSHOT_CORRUPTED"))

        assertThat(decision).isEqualTo(RingingRecovery.StopOnCorruptedSnapshot)
    }

    /** Un état ajouté à la machine commune doit être classé explicitement, jamais par défaut. */
    @Test
    fun everyStateOfTheCommonMachineIsClassified() {
        val decisions =
            SessionStateDto.entries.associateWith { RingingServiceRecovery.decide(present(it)) }

        assertThat(decisions).hasSize(SessionStateDto.entries.size)
        assertThat(decisions.filterValues { it is RingingRecovery.ResumeRinging }.keys)
            .containsExactly(SessionStateDto.RINGING)
        assertThat(decisions.filterValues { it == RingingRecovery.ReconcileAndStop }.keys)
            .containsExactly(SessionStateDto.ARMED)
    }
}
