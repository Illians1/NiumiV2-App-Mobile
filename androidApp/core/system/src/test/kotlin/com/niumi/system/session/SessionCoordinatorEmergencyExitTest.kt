package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ViolationCode
import com.niumi.core.interop.EmergencyTranscriptionInputDto
import com.niumi.core.interop.ReleaseMeansDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.emergency.EmergencyExitEventFactory
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sortie de secours prouvée par la recopie (SPEC_CORE_KMP §5.1, §12 ; étape 44). Le coordinateur
 * referme la phase comme pour un scan : pendant la sonnerie, la session se termine ; le soir,
 * blocage déjà appliqué, elle est annulée (décision D2 du 2026-10-24).
 */
class SessionCoordinatorEmergencyExitTest {
    private suspend fun armImmediateSession(harness: TestCoordinatorHarness): SessionSnapshotDto {
        val result =
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                SessionDtoFixtures.extras(),
            )
        return (result as DispatchResult.Applied).snapshot!!
    }

    /** La preuve ne sort que de la façade, pour ce contexte précis : jamais un mock. */
    private fun emergencyExit(
        harness: TestCoordinatorHarness,
        snapshot: SessionSnapshotDto,
    ) = EmergencyExitEventFactory(harness.idGenerator, harness.clock).let { factory ->
        val context = factory.verificationContext(snapshot)
        val text = harness.facade.emergencyExitTexts().first()
        val proof =
            requireNotNull(
                harness.facade
                    .verifyEmergencyTranscription(EmergencyTranscriptionInputDto(text.id, text.text), context)
                    .proof,
            )
        factory.emergencyExitConfirmed(context, proof)
    }

    @Test
    fun anEmergencyExitDuringTheAlarmCompletesTheSession() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armImmediateSession(harness)
            val ringing =
                (harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed)) as DispatchResult.Applied)
                    .snapshot!!

            val result = harness.coordinator.dispatch(emergencyExit(harness, ringing))

            val applied = result as DispatchResult.Applied
            assertThat(applied.requiredEffectsSucceeded).isTrue()
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.COMPLETED)
            assertThat(applied.snapshot?.releasedBy).isEqualTo(ReleaseMeansDto.EMERGENCY_EXIT)
            assertThat(applied.snapshot?.nfcVerifiedAtEpochMillis).isNull()
            assertThat(harness.journal.calls).contains("RingingController.stopRinging")
            assertThat(harness.blockingController.effectivePackages()).isEmpty()
            assertThat(harness.gateway.load()).isEqualTo(LoadResult.Absent)
        }

    @Test
    fun anEmergencyExitInTheEveningCancelsTheSession() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armImmediateSession(harness)

            val result = harness.coordinator.dispatch(emergencyExit(harness, armed))

            val applied = result as DispatchResult.Applied
            assertThat(applied.snapshot?.state).isEqualTo(SessionStateDto.CANCELLED)
            assertThat(applied.snapshot?.releasedBy).isEqualTo(ReleaseMeansDto.EMERGENCY_EXIT)
            assertThat(harness.blockingController.effectivePackages()).isEmpty()
        }

    @Test
    fun anAcceptedEmergencyExitIsLogged() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armImmediateSession(harness)

            harness.coordinator.dispatch(emergencyExit(harness, armed))

            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.EMERGENCY_EXIT_CONFIRMED)
        }

    @Test
    fun anEmergencyExitWithoutProofIsRejectedAndNotLogged() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed = armImmediateSession(harness)

            val result = harness.coordinator.dispatch(emergencyExit(harness, armed).copy(emergencyProof = null))

            assertThat((result as DispatchResult.Rejected).violations.map { it.code })
                .containsExactly(ViolationCode.MISSING_EMERGENCY_PROOF)
            assertThat(harness.technicalEventLog.logged).doesNotContain(TechnicalEventType.EMERGENCY_EXIT_CONFIRMED)
        }

    /** Avant le coucher, on désarme : la recopie n'a pas lieu d'être (SPEC_CORE_KMP §4). */
    @Test
    fun aPendingSessionRefusesTheEmergencyExit() =
        runTest {
            val harness = TestCoordinatorHarness()
            val armed =
                (
                    harness.coordinator.dispatch(
                        SessionDtoFixtures.activationRequested(
                            eventId = "00000000-0000-0000-0000-000000000001",
                            blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                        ),
                        SessionDtoFixtures.extras(),
                    ) as DispatchResult.Applied
                ).snapshot!!

            val result = harness.coordinator.dispatch(emergencyExit(harness, armed))

            assertThat((result as DispatchResult.Rejected).violations.map { it.code })
                .containsExactly(ViolationCode.INVALID_STATE_TRANSITION)
        }
}
