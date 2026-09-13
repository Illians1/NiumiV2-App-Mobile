package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
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
}
