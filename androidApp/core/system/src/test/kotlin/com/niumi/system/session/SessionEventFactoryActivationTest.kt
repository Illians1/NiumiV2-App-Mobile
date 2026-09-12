package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.AppSelectionSummaryDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.system.session.fakes.FakeClock
import com.niumi.system.session.fakes.SequentialIdGenerator
import org.junit.Test

/**
 * `activationRequested` construit le seul événement que `ArmSessionUseCase` (étape 14) dispatche
 * lui-même : le coordinateur enchaîne les suites (`ACTIVATION_SUCCEEDED`/`FAILED`, SPEC_CORE_KMP
 * §10). Contrairement aux autres fabriques de ce fichier, il n'existe aucun snapshot préalable
 * dont dériver `sessionId` ou `expectedRevision`.
 */
class SessionEventFactoryActivationTest {
    private val activationRequest =
        ActivationRequestDto(
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-13",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = 1_000L,
                ),
            appSelection = AppSelectionSummaryDto(count = 1),
        )

    @Test
    fun buildsAnActivationRequestedEventCarryingTheRequestUnchanged() {
        val factory = SessionEventFactory(SequentialIdGenerator(), FakeClock(now = 42L))

        val event = factory.activationRequested(activationRequest)

        assertThat(event.kind).isEqualTo(SessionEventKindDto.ACTIVATION_REQUESTED)
        assertThat(event.activationRequest).isEqualTo(activationRequest)
        assertThat(event.expectedRevision).isNull()
        assertThat(event.failureCode).isNull()
        assertThat(event.incident).isNull()
        assertThat(event.occurredAtEpochMillis).isEqualTo(42L)
    }

    @Test
    fun eventIdAndSessionIdAreDistinctCanonicalUuids() {
        val factory = SessionEventFactory(SequentialIdGenerator(), FakeClock(now = 1L))

        val event = factory.activationRequested(activationRequest)

        val canonicalUuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assertThat(event.eventId).matches(canonicalUuid.pattern)
        assertThat(event.sessionId).matches(canonicalUuid.pattern)
        assertThat(event.eventId).isNotEqualTo(event.sessionId)
    }

    @Test
    fun occurredAtEpochMillisComesFromTheInjectedClockAndIsStrictlyPositive() {
        val clock = FakeClock(now = 7L)
        val factory = SessionEventFactory(SequentialIdGenerator(), clock)

        val event = factory.activationRequested(activationRequest)

        assertThat(event.occurredAtEpochMillis).isEqualTo(7L)
        assertThat(event.occurredAtEpochMillis).isGreaterThan(0L)
    }

    @Test
    fun theProducedEventIsAcceptedByTheRealFacadeAndYieldsAPreparingSnapshot() {
        val factory = SessionEventFactory(SequentialIdGenerator(), FakeClock(now = 100L))
        val event = factory.activationRequested(activationRequest)

        val decision = NiumiCoreFacade().reduce(null, event)

        assertThat(decision.violations).isEmpty()
        assertThat(decision.snapshot).isNotNull()
        assertThat(decision.snapshot!!.state).isEqualTo(SessionStateDto.PREPARING)
    }

    @Test
    fun successiveCallsProduceDistinctSessionIds() {
        val factory = SessionEventFactory(SequentialIdGenerator(), FakeClock(now = 1L))

        val first = factory.activationRequested(activationRequest)
        val second = factory.activationRequested(activationRequest)

        assertThat(first.sessionId).isNotEqualTo(second.sessionId)
    }
}
