package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Registre idempotent (SPEC_CORE_KMP §5.2, §6.1) : le coordinateur déduplique avant d'appeler
 * `reduce()`, jamais le moteur lui-même. Écrit avant `DefaultSessionCoordinator` (TDD, étape 11).
 */
class SessionCoordinatorIdempotenceTest {
    private val harness = TestCoordinatorHarness()
    private val eventId = "00000000-0000-0000-0000-000000000001"

    @Test
    fun sameEventIdAndSamePayloadIsRecognizedAsDuplicateWithoutCallingTheReducer() =
        runTest {
            val event = SessionDtoFixtures.activationRequested(eventId = eventId)
            val first = harness.coordinator.dispatch(event, SessionDtoFixtures.extras())
            val callsAfterFirst = harness.recordingReducer.callCount

            val second = harness.coordinator.dispatch(event, SessionDtoFixtures.extras())

            assertThat(first).isInstanceOf(DispatchResult.Applied::class.java)
            assertThat(second).isInstanceOf(DispatchResult.Duplicate::class.java)
            assertThat(harness.recordingReducer.callCount).isEqualTo(callsAfterFirst)
        }

    @Test
    fun sameEventIdWithADifferentPayloadIsRejectedWithEventIdConflict() =
        runTest {
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = eventId, appCount = 2),
                SessionDtoFixtures.extras(),
            )

            val result =
                harness.coordinator.dispatch(
                    SessionDtoFixtures.activationRequested(eventId = eventId, appCount = 3),
                    SessionDtoFixtures.extras(),
                )

            assertThat(result).isInstanceOf(DispatchResult.Rejected::class.java)
            assertThat((result as DispatchResult.Rejected).violations.single().code).isEqualTo("EVENT_ID_CONFLICT")
        }

    @Test
    fun eventReferencingAnotherSessionThanTheActiveOneIsRejectedAsUnknownSession() =
        runTest {
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = eventId),
                SessionDtoFixtures.extras(),
            )
            val eventForAnotherSession =
                SessionEventDto(
                    eventId = "00000000-0000-0000-0000-000000000002",
                    sessionId = SessionDtoFixtures.OTHER_SESSION_ID,
                    kind = SessionEventKindDto.RELEASE_SUCCEEDED,
                    occurredAtEpochMillis = 2_000L,
                    expectedRevision = 1L,
                    activationRequest = null,
                    failureCode = null,
                    incident = null,
                )

            val result = harness.coordinator.dispatch(eventForAnotherSession)

            assertThat(result).isInstanceOf(DispatchResult.Rejected::class.java)
            assertThat((result as DispatchResult.Rejected).violations.single().code).isEqualTo("UNKNOWN_SESSION")
        }
}
