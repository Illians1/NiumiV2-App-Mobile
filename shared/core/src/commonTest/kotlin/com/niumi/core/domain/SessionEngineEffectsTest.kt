package com.niumi.core.domain

import com.niumi.core.domain.SessionActivationEventFixtures.activationRequestedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.blockingStartElapsedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.incidentReportedEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingPendingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionEngineEffectsTest {
    private val engine = SessionEngine()

    @Test
    fun effectIdFollowsSessionRevisionKindOrdinalFormat() {
        val event = activationRequestedEvent()

        val decision = engine.reduce(snapshot = null, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        decision.effects.forEachIndexed { ordinal, effect ->
            assertEquals("${snapshot.sessionId}:${snapshot.revision}:${effect.kind}:$ordinal", effect.effectId)
        }
    }

    @Test
    fun identicalCallsProduceIdenticalEffectIds() {
        val event = activationRequestedEvent()

        val first = engine.reduce(snapshot = null, event = event)
        val second = engine.reduce(snapshot = null, event = event)

        assertEquals(first.effects.map { it.effectId }, second.effects.map { it.effectId })
    }

    @Test
    fun blockingStartEffectIdsFollowTheirOrdinalInTheDecision() {
        val pending = armedBlockingPendingSnapshot()
        val event =
            blockingStartElapsedEvent(
                expectedRevision = pending.revision,
                incident =
                    incident(
                        code = IncidentCodes.MISSED_BLOCKING_START_WINDOW,
                        severity = IncidentSeverity.WARNING,
                    ),
            )

        val decision = engine.reduce(snapshot = pending, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(
            listOf(
                "${snapshot.sessionId}:${snapshot.revision}:PUBLISH_PLATFORM_SNAPSHOT:0",
                "${snapshot.sessionId}:${snapshot.revision}:APPLY_BLOCKING:1",
                "${snapshot.sessionId}:${snapshot.revision}:RECORD_INCIDENT:2",
            ),
            decision.effects.map { it.effectId },
        )
    }

    @Test
    fun deferredActivationEffectIdsFollowTheirOrdinalInTheDecision() {
        val event =
            SessionActivationEventFixtures.activationRequestedEvent(
                activationRequest =
                    SessionActivationEventFixtures.activationRequest(
                        blockingSchedule = referenceBlockingSchedule,
                    ),
            )

        val decision = engine.reduce(snapshot = null, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(
            "${snapshot.sessionId}:${snapshot.revision}:SCHEDULE_BLOCKING_START:2",
            decision.effects[2].effectId,
        )
    }

    @Test
    fun onlyRecordIncidentCarriesAPayload() {
        val armed = armedSnapshot()
        val event = incidentReportedEvent(expectedRevision = armed.revision, incident = incident())

        val decision = engine.reduce(snapshot = armed, event = event)

        val recordIncidentEffects = decision.effects.filter { it.kind == SessionEffectKind.RECORD_INCIDENT }
        assertTrue(recordIncidentEffects.isNotEmpty())
        recordIncidentEffects.forEach { assertTrue(it.payload is IncidentEffectPayload) }

        decision.effects.filter { it.kind != SessionEffectKind.RECORD_INCIDENT }.forEach { assertNull(it.payload) }
    }
}
