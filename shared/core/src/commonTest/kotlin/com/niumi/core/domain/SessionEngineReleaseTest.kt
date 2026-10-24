package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseFailedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseSucceededEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.releasingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.releasingTowardDisarmedSnapshot
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

    @Test
    fun releaseSucceededOnForgedReleasingWithoutReleasedByIsRejected() {
        // Contrat 1.5, §4 : `releasedBy` est renseigné avant l'entrée dans `RELEASING`.
        val forged = releasingSnapshot(ReleaseTarget.COMPLETED).copy(releasedBy = null)

        assertForgedReleasingIsRejected(forged)
    }

    @Test
    fun releaseSucceededOnForgedReleasingWithMismatchedMeansIsRejected() {
        // `nfcVerifiedAt` existe si et seulement si `releasedBy == NFC_SCAN` ; `DISARMED` est
        // atteint par un changement de planning et seulement par lui.
        val disarmedWithoutPlanChange = releasingTowardDisarmedSnapshot().copy(releasedBy = ReleaseMeans.NFC_SCAN)
        val disarmedWithScan =
            releasingTowardDisarmedSnapshot().copy(nfcVerifiedAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS)
        val completedByPlanChange =
            releasingSnapshot(ReleaseTarget.COMPLETED)
                .copy(releasedBy = ReleaseMeans.PLAN_CHANGE, nfcVerifiedAtEpochMillis = null)
        val scanWithoutVerification = releasingSnapshot(ReleaseTarget.CANCELLED).copy(nfcVerifiedAtEpochMillis = null)

        listOf(disarmedWithoutPlanChange, disarmedWithScan, completedByPlanChange, scanWithoutVerification)
            .forEach(::assertForgedReleasingIsRejected)
    }

    @Test
    fun releaseSucceededKeepsTheReleaseMeans() {
        val releasing = releasingSnapshot(ReleaseTarget.COMPLETED)

        val decision = engine.reduce(releasing, releaseSucceededEvent(expectedRevision = releasing.revision))

        assertEquals(ReleaseMeans.NFC_SCAN, decision.snapshot?.releasedBy)
    }

    private fun assertForgedReleasingIsRejected(forged: SessionSnapshot) {
        val decision =
            engine.reduce(
                snapshot = forged,
                event = releaseSucceededEvent(expectedRevision = forged.revision),
            )

        assertEquals(forged, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }
}
