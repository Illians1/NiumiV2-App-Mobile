package com.niumi.core.domain

import com.niumi.core.domain.SessionActivationEventFixtures.activationRequest
import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmFiredEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionEngineValidationTest {
    private val engine = SessionEngine()

    @Test
    fun missingExpectedRevisionOutsideActivationIsStale() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision).copy(expectedRevision = null)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.STALE_REVISION), decision.violations.map { it.code })
    }

    @Test
    fun expectedRevisionDifferentFromSnapshotRevisionIsStale() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision + 1)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.STALE_REVISION), decision.violations.map { it.code })
    }

    @Test
    fun foreignSessionIdIsUnknownSession() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(sessionId = OTHER_SESSION_ID, expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.UNKNOWN_SESSION), decision.violations.map { it.code })
    }

    @Test
    fun nonCanonicalIdentifierIsInvalid() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(eventId = "not-a-uuid", expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.INVALID_IDENTIFIER), decision.violations.map { it.code })
    }

    @Test
    fun nonPositiveTimestampIsInvalid() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision, occurredAtEpochMillis = 0L)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.INVALID_TIMESTAMP), decision.violations.map { it.code })
    }

    @Test
    fun activationRequestOutsideActivationRequestedIsUnexpected() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision).copy(activationRequest = activationRequest())

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.UNEXPECTED_EVENT_PAYLOAD), decision.violations.map { it.code })
    }

    @Test
    fun incidentOnAlarmFiredIsUnexpected() {
        val armed = armedSnapshot()
        val incident =
            SessionIncident(
                code = IncidentCodes.TIME_CHANGED,
                severity = IncidentSeverity.WARNING,
                occurredAtEpochMillis = RINGING_AT_EPOCH_MILLIS,
                platform = Platform.ANDROID,
            )
        val event = alarmFiredEvent(expectedRevision = armed.revision).copy(incident = incident)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(listOf(ViolationCode.UNEXPECTED_EVENT_PAYLOAD), decision.violations.map { it.code })
    }

    @Test
    fun rejectedDecisionReturnsUnchangedSnapshotAndNoEffects() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision, occurredAtEpochMillis = 0L)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertTrue(decision.violations.isNotEmpty())
    }
}
