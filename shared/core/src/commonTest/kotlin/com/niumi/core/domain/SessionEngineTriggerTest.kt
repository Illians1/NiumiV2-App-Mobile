package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmFiredEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmSoundStoppedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.triggerElapsedEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.completedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.triggeredAwaitingNfcSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionEngineTriggerTest {
    private val engine = SessionEngine()

    @Test
    fun alarmFiredFromArmedRings() {
        val armed = armedSnapshot()
        val event = alarmFiredEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.RINGING, snapshot.state)
        assertEquals(event.occurredAtEpochMillis, snapshot.ringingAtEpochMillis)
        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.START_RINGING,
            ),
            decision.effects.map {
                it.kind
            },
        )
    }

    @Test
    fun alarmFiredInFinalStateIsRejected() {
        val completed = completedSnapshot()
        val event = alarmFiredEvent(expectedRevision = completed.revision)

        val decision = engine.reduce(snapshot = completed, event = event)

        assertEquals(completed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun triggerElapsedAtTriggerTimeMovesToTriggeredAwaitingNfc() {
        val armed = armedSnapshot()
        val event =
            triggerElapsedEvent(expectedRevision = armed.revision, occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS)

        val decision = engine.reduce(snapshot = armed, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.TRIGGERED_AWAITING_NFC, snapshot.state)
        assertEquals(TRIGGER_AT_EPOCH_MILLIS, snapshot.triggerElapsedAtEpochMillis)
        assertEquals(SessionHealth.HEALTHY, snapshot.health)
        assertEquals(
            listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT, SessionEffectKind.PRESENT_SCAN_REQUEST),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun triggerElapsedAfterTriggerTimeWithIncidentRecordsItAndDegradesHealth() {
        val armed = armedSnapshot()
        val missedIncident = incident(code = IncidentCodes.MISSED_TRIGGER_WINDOW, severity = IncidentSeverity.DEGRADED)
        val event =
            triggerElapsedEvent(
                expectedRevision = armed.revision,
                occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS + 1_000_000L,
                incident = missedIncident,
            )

        val decision = engine.reduce(snapshot = armed, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.TRIGGERED_AWAITING_NFC, snapshot.state)
        assertEquals(SessionHealth.DEGRADED, snapshot.health)
        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.PRESENT_SCAN_REQUEST,
                SessionEffectKind.RECORD_INCIDENT,
            ),
            decision.effects.map { it.kind },
        )
        val recordIncidentEffect = decision.effects.last()
        assertEquals(IncidentEffectPayload(missedIncident), recordIncidentEffect.payload)
    }

    @Test
    fun triggerElapsedBeforeTriggerTimeIsRejected() {
        val armed = armedSnapshot()
        val event =
            triggerElapsedEvent(
                expectedRevision = armed.revision,
                occurredAtEpochMillis =
                    TRIGGER_AT_EPOCH_MILLIS - 1_000L,
            )

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertEquals(listOf(ViolationCode.TRIGGER_NOT_REACHED), decision.violations.map { it.code })
    }

    @Test
    fun alarmSoundStoppedFromArmedOrRingingMovesToAwaitingNfc() {
        val armed = armedSnapshot()
        val fromArmed =
            engine.reduce(
                snapshot = armed,
                event = alarmSoundStoppedEvent(expectedRevision = armed.revision),
            )
        assertEquals(SessionState.AWAITING_NFC, requireNotNull(fromArmed.snapshot).state)

        val ringing = ringingSnapshot()
        val fromRinging =
            engine.reduce(
                snapshot = ringing,
                event = alarmSoundStoppedEvent(expectedRevision = ringing.revision),
            )
        assertEquals(SessionState.AWAITING_NFC, requireNotNull(fromRinging.snapshot).state)
        assertEquals(
            listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT, SessionEffectKind.PRESENT_SCAN_REQUEST),
            fromRinging.effects.map { it.kind },
        )
    }

    @Test
    fun alarmSoundStoppedFromTriggeredAwaitingNfcKeepsStateAndAdvancesRevision() {
        val triggered = triggeredAwaitingNfcSnapshot()
        val event = alarmSoundStoppedEvent(expectedRevision = triggered.revision)

        val decision = engine.reduce(snapshot = triggered, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.TRIGGERED_AWAITING_NFC, snapshot.state)
        assertEquals(triggered.revision + 1, snapshot.revision)
        assertEquals(event.occurredAtEpochMillis, snapshot.alarmSoundStoppedAtEpochMillis)
    }
}
