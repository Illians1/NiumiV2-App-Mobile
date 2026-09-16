package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.fakes.FakeClock
import com.niumi.system.session.fakes.SequentialIdGenerator
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

private const val NOW = 1_757_000_000_000L

/**
 * `ALARM_FIRED` (SPEC_CORE_KMP §6) : « Les autres événements doivent laisser ces champs à `null` »
 * — seuls `expectedRevision` et l'horodatage sont portés.
 */
class SessionEventFactoryTriggerTest {
    private val factory = SessionEventFactory(SequentialIdGenerator(), FakeClock(NOW))

    @Test
    fun alarmFiredCarriesTheSnapshotRevisionAndNoPayload() {
        val snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, revision = 4)

        val event = factory.alarmFired(snapshot)

        assertThat(event.kind).isEqualTo(SessionEventKindDto.ALARM_FIRED)
        assertThat(event.sessionId).isEqualTo(snapshot.sessionId)
        assertThat(event.expectedRevision).isEqualTo(4)
        assertThat(event.occurredAtEpochMillis).isEqualTo(NOW)
        assertThat(event.activationRequest).isNull()
        assertThat(event.nfcProof).isNull()
        assertThat(event.failureCode).isNull()
        assertThat(event.incident).isNull()
    }

    /** `incident` est facultatif pour ce kind (SPEC_CORE_KMP §6) : nul quand le début est à l'heure. */
    @Test
    fun blockingStartElapsedWithoutAnIncidentCarriesOnlyTheRevision() {
        val snapshot = SessionDtoFixtures.deferredArmedSnapshot(revision = 4)

        val event = factory.blockingStartElapsed(snapshot, incident = null)

        assertThat(event.kind).isEqualTo(SessionEventKindDto.BLOCKING_START_ELAPSED)
        assertThat(event.sessionId).isEqualTo(snapshot.sessionId)
        assertThat(event.expectedRevision).isEqualTo(4)
        assertThat(event.occurredAtEpochMillis).isEqualTo(NOW)
        assertThat(event.activationRequest).isNull()
        assertThat(event.nfcProof).isNull()
        assertThat(event.failureCode).isNull()
        assertThat(event.incident).isNull()
    }

    /** Au-delà de quinze minutes, `MISSED_BLOCKING_START_WINDOW` y est joint en `WARNING` (§8.3). */
    @Test
    fun blockingStartElapsedCarriesTheMissedWindowIncidentWhenGiven() {
        val snapshot = SessionDtoFixtures.deferredArmedSnapshot(revision = 4)
        val incident =
            factory.buildIncident(IncidentCodes.MISSED_BLOCKING_START_WINDOW, IncidentSeverityDto.WARNING)

        val event = factory.blockingStartElapsed(snapshot, incident)

        assertThat(event.incident?.code).isEqualTo(IncidentCodes.MISSED_BLOCKING_START_WINDOW)
        assertThat(event.incident?.severity).isEqualTo(IncidentSeverityDto.WARNING)
        assertThat(event.incident?.occurredAtEpochMillis).isEqualTo(NOW)
    }
}
