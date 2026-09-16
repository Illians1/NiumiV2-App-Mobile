package com.niumi.core.domain

import com.niumi.core.domain.SessionLifecycleEventFixtures.blockingStartElapsedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.incident
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingAppliedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingPendingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.cancelledSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.completedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.failedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.preparingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `BLOCKING_START_ELAPSED` (SPEC_CORE_KMP §5.1, §5.2, §6, §7.5, §8.3). */
class SessionEngineBlockingStartTest {
    private val engine = SessionEngine()

    @Test
    fun blockingStartAtTheContractualInstantAppliesTheBlockingWithoutChangingTheState() {
        val snapshot = armedBlockingPendingSnapshot()

        val decision =
            engine.reduce(
                snapshot,
                blockingStartElapsedEvent(occurredAtEpochMillis = BLOCKING_STARTS_AT_EPOCH_MILLIS),
            )

        val newSnapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.ARMED, newSnapshot.state)
        assertEquals(snapshot.revision + 1, newSnapshot.revision)
        assertEquals(BLOCKING_STARTS_AT_EPOCH_MILLIS, newSnapshot.blockingAppliedAtEpochMillis)
        assertEquals(
            listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT, SessionEffectKind.APPLY_BLOCKING),
            decision.effects.map { effect: SessionEffect -> effect.kind },
        )
        assertTrue(decision.violations.isEmpty())
    }

    @Test
    fun blockingStartAfterTheContractualInstantIsAccepted() {
        val occurredAt = BLOCKING_STARTS_AT_EPOCH_MILLIS + 20 * 60 * 1_000L

        val decision =
            engine.reduce(armedBlockingPendingSnapshot(), blockingStartElapsedEvent(occurredAtEpochMillis = occurredAt))

        assertEquals(occurredAt, decision.snapshot?.blockingAppliedAtEpochMillis)
        assertTrue(decision.violations.isEmpty())
    }

    @Test
    fun blockingStartBeforeTheContractualInstantIsRejected() {
        val snapshot = armedBlockingPendingSnapshot()

        val decision =
            engine.reduce(
                snapshot,
                blockingStartElapsedEvent(occurredAtEpochMillis = BLOCKING_STARTS_AT_EPOCH_MILLIS - 1),
            )

        assertEquals(listOf(ViolationCode.BLOCKING_START_NOT_REACHED), decision.violations.map { it.code })
        assertEquals(snapshot, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
    }

    @Test
    fun missedBlockingStartWindowIsRecordedWithoutDegradingHealth() {
        val missed =
            incident(
                code = IncidentCodes.MISSED_BLOCKING_START_WINDOW,
                severity = IncidentSeverity.WARNING,
                occurredAtEpochMillis = BLOCKING_STARTS_AT_EPOCH_MILLIS,
            )

        val decision = engine.reduce(armedBlockingPendingSnapshot(), blockingStartElapsedEvent(incident = missed))

        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.APPLY_BLOCKING,
                SessionEffectKind.RECORD_INCIDENT,
            ),
            decision.effects.map { effect: SessionEffect -> effect.kind },
        )
        assertEquals(IncidentEffectPayload(missed), decision.effects.last().payload)
        assertEquals(SessionHealth.HEALTHY, decision.snapshot?.health)
    }

    @Test
    fun secondBlockingStartOnTheSameSessionIsRejected() {
        val decision =
            engine.reduce(armedBlockingAppliedSnapshot(), blockingStartElapsedEvent(expectedRevision = 3))

        assertEquals(listOf(ViolationCode.BLOCKING_ALREADY_APPLIED), decision.violations.map { it.code })
        assertTrue(decision.effects.isEmpty())
    }

    @Test
    fun blockingStartOnAnImmediateSessionIsRejected() {
        val decision = engine.reduce(armedSnapshot(), blockingStartElapsedEvent())

        assertEquals(listOf(ViolationCode.BLOCKING_ALREADY_APPLIED), decision.violations.map { it.code })
        assertTrue(decision.effects.isEmpty())
    }

    @Test
    fun blockingStartOutsideArmedIsRejected() {
        val sources =
            listOf(
                preparingSnapshot(revision = 1),
                ringingSnapshot(revision = 3),
                completedSnapshot(revision = 6),
                cancelledSnapshot(revision = 4),
                failedSnapshot(revision = 2),
            )

        for (snapshot in sources) {
            val decision = engine.reduce(snapshot, blockingStartElapsedEvent(expectedRevision = snapshot.revision))

            assertEquals(
                listOf(ViolationCode.INVALID_STATE_TRANSITION),
                decision.violations.map { it.code },
                "attendu un refus depuis ${snapshot.state}",
            )
            assertTrue(decision.effects.isEmpty(), "aucun effet attendu depuis ${snapshot.state}")
        }
    }

    @Test
    fun blockingStartWithoutAnySessionIsRejected() {
        val decision = engine.reduce(null, blockingStartElapsedEvent())

        assertTrue(decision.violations.isNotEmpty())
        assertNull(decision.snapshot)
    }

    @Test
    fun staleRevisionIsRejectedBeforeAnyBlockingDecision() {
        val decision = engine.reduce(armedBlockingPendingSnapshot(revision = 4), blockingStartElapsedEvent())

        assertEquals(listOf(ViolationCode.STALE_REVISION), decision.violations.map { it.code })
        assertTrue(decision.effects.isEmpty())
    }
}
