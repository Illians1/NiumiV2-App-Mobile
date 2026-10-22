package com.niumi.system.blocking.guard

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.LoadResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.isBlockingInForce
import com.niumi.system.session.isSessionInProgress
import org.junit.Test

/**
 * SPEC_ANDROID §12.5 : le service de garde tourne exactement pendant une session en cours, de
 * l'armement à l'état final, avec la phase qui choisit le texte de sa notification, et rien n'est
 * décidé avant la première lecture de la session dans le processus.
 */
class BlockingGuardPolicyTest {
    @Test
    fun nothingIsDecidedBeforeTheFirstRead() {
        assertThat(BlockingGuardPolicy.decide(null)).isEqualTo(BlockingGuardDecision.Unknown)
    }

    @Test
    fun theGuardIsKeptExactlyWhileTheSessionIsInProgress() {
        allSnapshots().forEach { snapshot ->
            val kept = BlockingGuardPolicy.decide(snapshot) is BlockingGuardDecision.Keep
            assertWithMessage("${snapshot.state} appliqué=${snapshot.blockingAppliedAtEpochMillis}")
                .that(kept)
                .isEqualTo(snapshot.state.isSessionInProgress())
        }
    }

    /**
     * La notification suit la session (décisions du 2026-10-22) : « Réveil programmé » avant le
     * blocage, « Réveil prévu » pendant, puis celle de l'alarme, partagée, jusqu'au scan.
     */
    @Test
    fun thePhaseFollowsTheBlockingThenTheAlarm() {
        allSnapshots().forEach { snapshot ->
            val decision = BlockingGuardPolicy.decide(snapshot) as? BlockingGuardDecision.Keep ?: return@forEach
            val expected =
                when {
                    !snapshot.isBlockingInForce() -> BlockingGuardPhase.BEFORE_BLOCKING
                    snapshot.state == SessionStateDto.ARMED -> BlockingGuardPhase.BLOCKING
                    snapshot.state == SessionStateDto.RINGING -> BlockingGuardPhase.RINGING
                    snapshot.state == SessionStateDto.RELEASING -> BlockingGuardPhase.RELEASING
                    else -> BlockingGuardPhase.AWAITING_SCAN
                }
            assertWithMessage(snapshot.state.name).that(decision.command.phase).isEqualTo(expected)
        }
    }

    @Test
    fun fromTheAlarmToTheScanThePhaseNamesWhatTheAlarmShows() {
        mapOf(
            SessionStateDto.RINGING to BlockingGuardPhase.RINGING,
            SessionStateDto.AWAITING_NFC to BlockingGuardPhase.AWAITING_SCAN,
            SessionStateDto.TRIGGERED_AWAITING_NFC to BlockingGuardPhase.AWAITING_SCAN,
            SessionStateDto.RELEASING to BlockingGuardPhase.RELEASING,
        ).forEach { (state, expected) ->
            val snapshot = SessionDtoFixtures.snapshotInState(state)
            val decision = BlockingGuardPolicy.decide(snapshot) as BlockingGuardDecision.Keep

            assertWithMessage(state.name).that(decision.command.phase).isEqualTo(expected)
        }
    }

    @Test
    fun anArmedSessionWaitingForBedtimeKeepsTheGuardBeforeBlocking() {
        val pending = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null)

        val decision = BlockingGuardPolicy.decide(pending) as BlockingGuardDecision.Keep

        assertThat(decision.command)
            .isEqualTo(
                BlockingGuardCommand(
                    sessionId = pending.sessionId,
                    phase = BlockingGuardPhase.BEFORE_BLOCKING,
                    wakeAtEpochMillis = pending.wakeSchedule.triggerAtEpochMillis,
                    blockingStartsAtEpochMillis = pending.blockingSchedule.startsAtEpochMillis,
                    zoneId = pending.wakeSchedule.zoneIdAtActivation,
                ),
            )
        assertThat(decision.command.blockingStartsAtEpochMillis).isNotNull()
    }

    @Test
    fun onceTheBlockingIsAppliedTheCommandSaysSo() {
        val applied = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L)

        val decision = BlockingGuardPolicy.decide(applied) as BlockingGuardDecision.Keep

        assertThat(decision.command.phase).isEqualTo(BlockingGuardPhase.BLOCKING)
    }

    /** Relancé par le système après une session terminée : rien à garder, le service s'arrête. */
    @Test
    fun afterARestartWithoutSessionTheGuardStops() {
        assertThat(BlockingGuardPolicy.afterRestart(LoadResult.Absent)).isEqualTo(BlockingGuardDecision.Release)
    }

    @Test
    fun afterARestartDuringTheSessionTheGuardIsKept() {
        val ringing = SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING)

        val decision = BlockingGuardPolicy.afterRestart(present(ringing))

        assertThat(decision).isInstanceOf(BlockingGuardDecision.Keep::class.java)
        assertThat((decision as BlockingGuardDecision.Keep).command.phase).isEqualTo(BlockingGuardPhase.RINGING)
    }

    @Test
    fun afterARestartOnAFinishedSessionTheGuardStops() {
        val completed = SessionDtoFixtures.snapshotInState(SessionStateDto.COMPLETED)

        assertThat(BlockingGuardPolicy.afterRestart(present(completed))).isEqualTo(BlockingGuardDecision.Release)
    }

    /** §18 : un snapshot illisible conserve le blocage, la protection reste avec lui. */
    @Test
    fun anUnreadableSnapshotLeavesTheGuardAsItIs() {
        assertThat(BlockingGuardPolicy.afterRestart(LoadResult.Unreadable("corrompu")))
            .isEqualTo(BlockingGuardDecision.Unknown)
    }

    @Test
    fun aCommandMissingAMandatoryExtraIsNotRebuilt() {
        assertThat(BlockingGuardCommand.fromExtras(null, "BLOCKING", 1L, null, "Europe/Paris")).isNull()
        assertThat(BlockingGuardCommand.fromExtras("s", "AUTRE", 1L, null, "Europe/Paris")).isNull()
        assertThat(BlockingGuardCommand.fromExtras("s", "BLOCKING", null, null, "Europe/Paris")).isNull()
        assertThat(BlockingGuardCommand.fromExtras("s", "BLOCKING", 1L, null, " ")).isNull()
    }

    /** L'heure du blocage est facultative : un blocage immédiat n'en a pas. */
    @Test
    fun aCommandIsRebuiltWithOrWithoutABlockingTime() {
        assertThat(BlockingGuardCommand.fromExtras("s", "BEFORE_BLOCKING", 1L, 2L, "Europe/Paris"))
            .isEqualTo(BlockingGuardCommand("s", BlockingGuardPhase.BEFORE_BLOCKING, 1L, 2L, "Europe/Paris"))
        assertThat(BlockingGuardCommand.fromExtras("s", "BLOCKING", 1L, null, "Europe/Paris"))
            .isEqualTo(BlockingGuardCommand("s", BlockingGuardPhase.BLOCKING, 1L, null, "Europe/Paris"))
        BlockingGuardPhase.entries.forEach { phase ->
            assertThat(BlockingGuardCommand.fromExtras("s", phase.name, 1L, null, "Europe/Paris"))
                .isEqualTo(BlockingGuardCommand("s", phase, 1L, null, "Europe/Paris"))
        }
    }

    private fun allSnapshots(): List<SessionSnapshotDto> =
        SessionStateDto.entries.map { SessionDtoFixtures.snapshotInState(it) } +
            SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null) +
            SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L)

    private fun present(snapshot: SessionSnapshotDto): LoadResult =
        LoadResult.Present(snapshot = snapshot, extras = SessionDtoFixtures.extras(), pendingEffects = emptyList())
}
