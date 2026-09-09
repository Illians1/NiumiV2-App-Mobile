package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.incidentReportedEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.completedSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionEngineIncidentTest {
    private val engine = SessionEngine()

    @Test
    fun warningIncidentLeavesHealthUnchanged() {
        val armed = armedSnapshot()
        val event =
            incidentReportedEvent(
                expectedRevision = armed.revision,
                incident = incident(severity = IncidentSeverity.WARNING),
            )

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(SessionHealth.HEALTHY, requireNotNull(decision.snapshot).health)
    }

    @Test
    fun degradedOrCriticalIncidentDegradesHealth() {
        val armed = armedSnapshot()
        val degradedIncident = incident(severity = IncidentSeverity.DEGRADED)
        val degraded =
            engine.reduce(
                armed,
                incidentReportedEvent(expectedRevision = armed.revision, incident = degradedIncident),
            )
        assertEquals(SessionHealth.DEGRADED, requireNotNull(degraded.snapshot).health)

        val alreadyDegraded = requireNotNull(degraded.snapshot)
        val criticalIncident = incident(severity = IncidentSeverity.CRITICAL)
        val event = incidentReportedEvent(expectedRevision = alreadyDegraded.revision, incident = criticalIncident)
        val critical = engine.reduce(alreadyDegraded, event)
        assertEquals(SessionHealth.DEGRADED, requireNotNull(critical.snapshot).health)
    }

    @Test
    fun warningIncidentAfterDegradedDoesNotReturnToHealthy() {
        val degraded = armedSnapshot(health = SessionHealth.DEGRADED)
        val event =
            incidentReportedEvent(
                expectedRevision = degraded.revision,
                incident = incident(severity = IncidentSeverity.WARNING),
            )

        val decision = engine.reduce(snapshot = degraded, event = event)

        assertEquals(SessionHealth.DEGRADED, requireNotNull(decision.snapshot).health)
    }

    @Test
    fun incidentOnFinalStateIsRejected() {
        val completed = completedSnapshot()
        val event = incidentReportedEvent(expectedRevision = completed.revision)

        val decision = engine.reduce(snapshot = completed, event = event)

        assertEquals(completed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun incidentProducesRecordIncidentThenPublish() {
        val armed = armedSnapshot()
        val event = incidentReportedEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(
            listOf(SessionEffectKind.RECORD_INCIDENT, SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT),
            decision.effects.map { it.kind },
        )
    }
}
