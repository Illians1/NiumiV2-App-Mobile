package com.niumi.core.domain

import com.niumi.core.domain.SessionActivationEventFixtures.activationRequestedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.incidentReportedEvent
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
