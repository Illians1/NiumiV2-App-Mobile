package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.disarmRequestedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseFailedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseSucceededEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingAppliedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingPendingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.disarmedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.preparingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.releasingTowardDisarmedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Désarmement avant le coucher (contrat 1.5, SPEC_CORE_KMP §5.1, §5.2, §12) : une session `ARMED`
 * dont le blocage est encore en attente se libère sans scan vers `DISARMED`, en passant par
 * `RELEASING` (décision commune 8).
 */
class SessionEngineDisarmTest {
    private val engine = SessionEngine()

    @Test
    fun aPendingSessionBeforeBedtimeReleasesTowardDisarmed() {
        val pending = armedBlockingPendingSnapshot()
        val event = disarmRequestedEvent(expectedRevision = pending.revision)

        val decision = engine.reduce(snapshot = pending, event = event)

        assertTrue(decision.violations.isEmpty())
        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(ReleaseTarget.DISARMED, snapshot.releaseTarget)
        assertEquals(ReleaseMeans.PLAN_CHANGE, snapshot.releasedBy)
        assertEquals(event.occurredAtEpochMillis, snapshot.releasingAtEpochMillis)
        assertEquals(pending.revision + 1, snapshot.revision)
        assertNull(snapshot.nfcVerifiedAtEpochMillis)
        assertNull(snapshot.blockingAppliedAtEpochMillis)
        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.CANCEL_ALARM,
                SessionEffectKind.CANCEL_BLOCKING_START,
            ),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun anAppliedDeferredBlockingIsRejected() {
        val applied = armedBlockingAppliedSnapshot()

        assertRejected(
            applied,
            disarmRequestedEvent(expectedRevision = applied.revision),
            ViolationCode.BLOCKING_ALREADY_APPLIED,
        )
    }

    @Test
    fun anImmediateBlockingIsRejected() {
        val armed = armedSnapshot()

        assertRejected(
            armed,
            disarmRequestedEvent(expectedRevision = armed.revision),
            ViolationCode.BLOCKING_ALREADY_APPLIED,
        )
    }

    @Test
    fun aRequestAtOrAfterBedtimeIsRejectedEvenIfTheBlockingStartWasNotProcessed() {
        // Décision du 2026-10-24 : du coucher au scan, tout est verrouillé (SPECIFICATION.md §2),
        // même si l'alarme de début n'a pas encore produit BLOCKING_START_ELAPSED.
        val pending = armedBlockingPendingSnapshot()

        for (occurredAt in listOf(
            BLOCKING_STARTS_AT_EPOCH_MILLIS,
            BLOCKING_STARTS_AT_EPOCH_MILLIS + 1_000L,
            TRIGGER_AT_EPOCH_MILLIS,
            TRIGGER_AT_EPOCH_MILLIS + 1_000L,
        )) {
            assertRejected(
                pending,
                disarmRequestedEvent(expectedRevision = pending.revision, occurredAtEpochMillis = occurredAt),
                ViolationCode.BLOCKING_START_ALREADY_ELAPSED,
            )
        }
    }

    @Test
    fun aRequestOneMillisecondBeforeBedtimeIsAccepted() {
        val pending = armedBlockingPendingSnapshot()
        val event =
            disarmRequestedEvent(
                expectedRevision = pending.revision,
                occurredAtEpochMillis = BLOCKING_STARTS_AT_EPOCH_MILLIS - 1L,
            )

        val decision = engine.reduce(snapshot = pending, event = event)

        assertEquals(SessionState.RELEASING, decision.snapshot?.state)
    }

    @Test
    fun aRequestOutsideArmedIsRejected() {
        val ringing = ringingSnapshot()
        assertRejected(
            ringing,
            disarmRequestedEvent(expectedRevision = ringing.revision),
            ViolationCode.INVALID_STATE_TRANSITION,
        )

        val preparing = preparingSnapshot()
        assertRejected(
            preparing,
            disarmRequestedEvent(expectedRevision = preparing.revision),
            ViolationCode.INVALID_STATE_TRANSITION,
        )

        val disarmed = disarmedSnapshot()
        assertRejected(
            disarmed,
            disarmRequestedEvent(expectedRevision = disarmed.revision),
            ViolationCode.INVALID_STATE_TRANSITION,
        )
    }

    @Test
    fun aRequestWithoutSessionIsRejected() {
        val decision = engine.reduce(snapshot = null, event = disarmRequestedEvent(expectedRevision = 1))

        assertNull(decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertTrue(decision.violations.isNotEmpty())
    }

    @Test
    fun aRequestCarryingAPayloadIsRejected() {
        val pending = armedBlockingPendingSnapshot()
        val event = disarmRequestedEvent(expectedRevision = pending.revision).copy(incident = incident())

        assertRejected(pending, event, ViolationCode.UNEXPECTED_EVENT_PAYLOAD)
    }

    @Test
    fun releaseSucceededReachesDisarmed() {
        val releasing = releasingTowardDisarmedSnapshot()
        val event =
            releaseSucceededEvent(
                expectedRevision = releasing.revision,
                occurredAtEpochMillis = DISARMED_AT_EPOCH_MILLIS,
            )

        val decision = engine.reduce(snapshot = releasing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.DISARMED, snapshot.state)
        assertEquals(DISARMED_AT_EPOCH_MILLIS, snapshot.disarmedAtEpochMillis)
        assertEquals(ReleaseMeans.PLAN_CHANGE, snapshot.releasedBy)
        assertNull(snapshot.completedAtEpochMillis)
        assertNull(snapshot.cancelledAtEpochMillis)
        assertEquals(
            listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT, SessionEffectKind.CLEAR_ACTIVE_SESSION),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun releaseFailedKeepsReleasingTowardDisarmed() {
        val releasing = releasingTowardDisarmedSnapshot()
        val event = releaseFailedEvent(expectedRevision = releasing.revision)

        val snapshot = requireNotNull(engine.reduce(snapshot = releasing, event = event).snapshot)

        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(ReleaseTarget.DISARMED, snapshot.releaseTarget)
    }

    private fun assertRejected(
        snapshot: SessionSnapshot,
        event: SessionEvent,
        code: String,
    ) {
        val decision = engine.reduce(snapshot = snapshot, event = event)

        assertEquals(snapshot, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(code), decision.violations.map { it.code })
    }
}
