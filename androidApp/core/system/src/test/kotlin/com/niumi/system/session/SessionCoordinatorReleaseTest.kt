package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Libération atomique (SPEC_CORE_KMP §6, §12 ; SPEC_ANDROID §11.3). `STOP_RINGING` requis pour
 * `RELEASE_SUCCEEDED` — décision validée le 2026-09-10, voir [PhaseCompletion].
 */
class SessionCoordinatorReleaseTest {
    private suspend fun armSession(harness: TestCoordinatorHarness): SessionSnapshotDto {
        val result =
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                SessionDtoFixtures.extras(),
            )
        return (result as DispatchResult.Applied).snapshot!!
    }

    private fun validNfcScanned(
        harness: TestCoordinatorHarness,
        armed: SessionSnapshotDto,
    ) = SessionDtoFixtures.validNfcScanned(
        facade = harness.facade,
        eventId = "00000000-0000-0000-0000-000000000002",
        expectedRevision = armed.revision,
        occurredAtEpochMillis = armed.armedAtEpochMillis!! + 1,
    )

    @Test
    fun validScanFromArmedReleasesToCancelledAndClearsTheActiveSession() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)

            val result = harness.coordinator.dispatch(validNfcScanned(harness, armed))

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.CANCELLED)
            assertThat(applied.snapshot?.releaseTarget).isEqualTo(ReleaseTargetDto.CANCELLED)
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isFalse()
            assertThat(harness.blockingController.effectivePackages()).isEmpty()
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    @Test
    fun removeBlockingFailureWithPreconditionStillHoldingKeepsReleasingAndRecordsPartialFailure() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.blockingController.removeResult = OperationResult.Failure("ANDROID_BLOCKING_REMOVE_FAILED")

            val result = harness.coordinator.dispatch(validNfcScanned(harness, armed))

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isFalse()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.RELEASING)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.RELEASE_PARTIAL_FAILURE)
            val removeBlockingEffect =
                harness.gateway
                    .pendingEffects(SessionDtoFixtures.SESSION_ID)
                    .single { it.kind == com.niumi.core.interop.SessionEffectKindDto.REMOVE_BLOCKING }
            assertThat(removeBlockingEffect.status).isEqualTo(EffectStatus.FAILED)
        }

    @Test
    fun stopRingingFailureAloneIsNowRequiredAndFailsTheRelease() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.ringingController.stopResult = OperationResult.Failure("ANDROID_STOP_RINGING_FAILED")

            val result = harness.coordinator.dispatch(validNfcScanned(harness, armed))

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isFalse()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.RELEASING)
        }

    @Test
    fun removeBlockingAlreadySatisfiedWithServiceDisabledCountsAsSuccessAndRecordsAnIncident() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armSession(harness)
            harness.blockingController.serviceEnabled = false
            harness.blockingController.removeResult = OperationResult.AlreadySatisfied

            val result = harness.coordinator.dispatch(validNfcScanned(harness, armed))

            assertThat(result).isInstanceOf(DispatchResult.Applied::class.java)
            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.CANCELLED)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
        }
}
