package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentEffectPayloadDto
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.database.mapping.SessionEffectMapper
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Rejeu d'outbox (SPEC_CORE_KMP §6.1) : seuls les effets `PENDING`/`FAILED` sont rejoués, jamais
 * un effet déjà `SUCCEEDED`, et jamais un effet d'une autre transition. */
class SessionCoordinatorOutboxReplayTest {
    @Test
    fun reconcileReleasingReplaysOnlyThePendingEffectNeverTheAlreadySucceededOne() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = SessionDtoFixtures.releasingSnapshot()
            val cancelAlarm =
                PendingEffect(
                    effectId = "${snapshot.sessionId}:1:CANCEL_ALARM:0",
                    sessionId = snapshot.sessionId,
                    revision = 1,
                    kind = SessionEffectKindDto.CANCEL_ALARM,
                    ordinal = 0,
                    payloadJson = null,
                    status = EffectStatus.SUCCEEDED,
                    lastError = null,
                )
            val removeBlocking =
                PendingEffect(
                    effectId = "${snapshot.sessionId}:1:REMOVE_BLOCKING:1",
                    sessionId = snapshot.sessionId,
                    revision = 1,
                    kind = SessionEffectKindDto.REMOVE_BLOCKING,
                    ordinal = 1,
                    payloadJson = null,
                    status = EffectStatus.PENDING,
                    lastError = null,
                )
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = listOf(cancelAlarm, removeBlocking),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.journal.calls).doesNotContain("AlarmScheduler.cancel")
            assertThat(harness.journal.calls).contains("BlockingController.remove")
        }

    @Test
    fun reconcileReplaysAPendingRecordIncidentEffectWithItsPayload() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = SessionDtoFixtures.releasingSnapshot()
            val incident =
                SessionIncidentDto(
                    code = IncidentCodes.RELEASE_PARTIAL_FAILURE,
                    severity = IncidentSeverityDto.DEGRADED,
                    occurredAtEpochMillis = 1_200L,
                    platform = PlatformDto.ANDROID,
                )
            val recordIncident =
                PendingEffect(
                    effectId = "${snapshot.sessionId}:1:RECORD_INCIDENT:0",
                    sessionId = snapshot.sessionId,
                    revision = 1,
                    kind = SessionEffectKindDto.RECORD_INCIDENT,
                    ordinal = 0,
                    payloadJson = SessionEffectMapper.encodePayload(IncidentEffectPayloadDto(incident)),
                    status = EffectStatus.PENDING,
                    lastError = null,
                )
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = listOf(recordIncident),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.RELEASE_PARTIAL_FAILURE)
        }

    @Test
    fun presentScanRequestEffectReplayedTwiceCallsTheNotifierBothTimesWithTheSameResult() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = SessionDtoFixtures.releasingSnapshot()
            val effect =
                PendingEffect(
                    effectId = "${snapshot.sessionId}:1:PRESENT_SCAN_REQUEST:0",
                    sessionId = snapshot.sessionId,
                    revision = 1,
                    kind = SessionEffectKindDto.PRESENT_SCAN_REQUEST,
                    ordinal = 0,
                    payloadJson = null,
                    status = EffectStatus.PENDING,
                    lastError = null,
                )

            val first = harness.effectDispatcher.execute(listOf(effect), snapshot, SessionDtoFixtures.extras())
            val second = harness.effectDispatcher.execute(listOf(effect), snapshot, SessionDtoFixtures.extras())

            assertThat(harness.scanRequestNotifier.presentCallCount).isEqualTo(2)
            assertThat(
                first.outcomes.outcomes
                    .single()
                    .result,
            ).isEqualTo(OperationResult.Success)
            assertThat(
                second.outcomes.outcomes
                    .single()
                    .result,
            ).isEqualTo(OperationResult.Success)
        }
}
