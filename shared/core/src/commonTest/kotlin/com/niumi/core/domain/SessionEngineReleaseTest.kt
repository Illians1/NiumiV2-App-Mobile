package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseFailedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseSucceededEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.releasingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionEngineReleaseTest {
    private val engine = SessionEngine()

    @Test
    fun releaseFailedWithIncidentStaysInReleasingAndDegradesHealth() {
        val releasing = releasingSnapshot(ReleaseTarget.COMPLETED)
        val failure = incident(severity = IncidentSeverity.DEGRADED)
        val event = releaseFailedEvent(expectedRevision = releasing.revision, incident = failure)

        val decision = engine.reduce(snapshot = releasing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(SessionHealth.DEGRADED, snapshot.health)
        assertEquals(
            listOf(SessionEffectKind.RECORD_INCIDENT, SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun releaseFailedWithoutIncidentIsRejected() {
        val releasing = releasingSnapshot(ReleaseTarget.COMPLETED)
        val event = releaseFailedEvent(expectedRevision = releasing.revision).copy(incident = null)

        val decision = engine.reduce(snapshot = releasing, event = event)

        assertEquals(releasing, decision.snapshot)
        assertEquals(listOf(ViolationCode.MISSING_INCIDENT), decision.violations.map { it.code })
    }

    @Test
    fun releaseSucceededFromReleasingCompletedReachesFinalState() {
        val releasing = releasingSnapshot(ReleaseTarget.COMPLETED)
        val event = releaseSucceededEvent(expectedRevision = releasing.revision)

        val decision = engine.reduce(snapshot = releasing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.COMPLETED, snapshot.state)
        assertEquals(event.occurredAtEpochMillis, snapshot.completedAtEpochMillis)
        assertEquals(
            listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT, SessionEffectKind.CLEAR_ACTIVE_SESSION),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun releaseSucceededFromReleasingCancelledReachesCancelled() {
        val releasing = releasingSnapshot(ReleaseTarget.CANCELLED)
        val event = releaseSucceededEvent(expectedRevision = releasing.revision)

        val decision = engine.reduce(snapshot = releasing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.CANCELLED, snapshot.state)
        assertEquals(event.occurredAtEpochMillis, snapshot.cancelledAtEpochMillis)
    }

    @Test
    fun releaseSucceededOutsideReleasingIsRejected() {
        val armed = armedSnapshot()
        val event = releaseSucceededEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun releaseSucceededOnForgedReleasingWithoutNfcVerifiedAtIsRejected() {
        val forged = releasingSnapshot(ReleaseTarget.COMPLETED).copy(nfcVerifiedAtEpochMillis = null)
        val event = releaseSucceededEvent(expectedRevision = forged.revision)

        val decision = engine.reduce(snapshot = forged, event = event)

        assertEquals(forged, decision.snapshot)
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }
}
