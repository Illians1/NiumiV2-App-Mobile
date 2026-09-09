package com.niumi.core.domain

import com.niumi.core.domain.SessionActivationEventFixtures.activationFailedEvent
import com.niumi.core.domain.SessionActivationEventFixtures.activationRequest
import com.niumi.core.domain.SessionActivationEventFixtures.activationRequestedEvent
import com.niumi.core.domain.SessionActivationEventFixtures.activationSucceededEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.preparingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionEngineActivationTest {
    private val engine = SessionEngine()

    @Test
    fun activationRequestedWithoutSnapshotCreatesPreparingAtRevisionOne() {
        val event = activationRequestedEvent()

        val decision = engine.reduce(snapshot = null, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.PREPARING, snapshot.state)
        assertEquals(1, snapshot.revision)
        assertEquals(SessionHealth.HEALTHY, snapshot.health)
        assertEquals(event.sessionId, snapshot.sessionId)
        assertTrue(decision.violations.isEmpty())
        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.SCHEDULE_ALARM,
                SessionEffectKind.APPLY_BLOCKING,
            ),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun activationRequestedWithExistingSnapshotIsRejected() {
        val existing = preparingSnapshot()
        val event = activationRequestedEvent(sessionId = OTHER_SESSION_ID)

        val decision = engine.reduce(snapshot = existing, event = event)

        assertEquals(existing, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun appSelectionOfZeroIsRejected() {
        val event = activationRequestedEvent(activationRequest = activationRequest(count = 0))

        val decision = engine.reduce(snapshot = null, event = event)

        assertNull(decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_APP_SELECTION), decision.violations.map { it.code })
    }

    @Test
    fun appSelectionOfFiftyOneIsRejected() {
        val event = activationRequestedEvent(activationRequest = activationRequest(count = 51))

        val decision = engine.reduce(snapshot = null, event = event)

        assertEquals(listOf(ViolationCode.INVALID_APP_SELECTION), decision.violations.map { it.code })
    }

    @Test
    fun appSelectionOfOneIsAccepted() {
        val event = activationRequestedEvent(activationRequest = activationRequest(count = 1))

        val decision = engine.reduce(snapshot = null, event = event)

        assertTrue(decision.violations.isEmpty())
    }

    @Test
    fun appSelectionOfFiftyIsAccepted() {
        val event = activationRequestedEvent(activationRequest = activationRequest(count = 50))

        val decision = engine.reduce(snapshot = null, event = event)

        assertTrue(decision.violations.isEmpty())
    }

    @Test
    fun activationSucceededFromPreparingArmsTheSession() {
        val preparing = preparingSnapshot()
        val event = activationSucceededEvent(expectedRevision = preparing.revision)

        val decision = engine.reduce(snapshot = preparing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.ARMED, snapshot.state)
        assertEquals(preparing.revision + 1, snapshot.revision)
        assertEquals(event.occurredAtEpochMillis, snapshot.armedAtEpochMillis)
        assertEquals(listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT), decision.effects.map { it.kind })
    }

    @Test
    fun activationFailedFromPreparingFailsWithCode() {
        val preparing = preparingSnapshot()
        val event =
            activationFailedEvent(expectedRevision = preparing.revision, failureCode = "ANDROID_BLOCKING_APPLY_FAILED")

        val decision = engine.reduce(snapshot = preparing, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.FAILED, snapshot.state)
        assertEquals("ANDROID_BLOCKING_APPLY_FAILED", snapshot.failureCode)
        assertEquals(
            listOf(
                SessionEffectKind.CANCEL_ALARM,
                SessionEffectKind.REMOVE_BLOCKING,
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.CLEAR_ACTIVE_SESSION,
            ),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun activationFailedWithoutFailureCodeIsRejected() {
        val preparing = preparingSnapshot()
        val event = activationFailedEvent(expectedRevision = preparing.revision).copy(failureCode = null)

        val decision = engine.reduce(snapshot = preparing, event = event)

        assertEquals(preparing, decision.snapshot)
        assertEquals(listOf(ViolationCode.MISSING_FAILURE_CODE), decision.violations.map { it.code })
    }

    @Test
    fun activationFailedFromArmedIsRejected() {
        val armed = armedSnapshot()
        val event = activationFailedEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }
}
