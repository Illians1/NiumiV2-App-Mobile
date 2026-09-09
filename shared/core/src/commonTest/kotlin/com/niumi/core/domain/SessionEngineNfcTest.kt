package com.niumi.core.domain

import com.niumi.core.domain.NfcScanEventFixtures.validNfcScannedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.invalidNfcScannedEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.awaitingNfcSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.preparingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.triggeredAwaitingNfcSnapshot
import com.niumi.core.nfc.NfcVerificationProof
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionEngineNfcTest {
    private val engine = SessionEngine()

    @Test
    fun validScanFromArmedBeforeTriggerReleasesToCancelled() {
        val armed = armedSnapshot()
        val occurredAt = TRIGGER_AT_EPOCH_MILLIS - 1_000L
        val event = validNfcScannedEvent(expectedRevision = armed.revision, occurredAtEpochMillis = occurredAt)

        val decision = engine.reduce(snapshot = armed, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(ReleaseTarget.CANCELLED, snapshot.releaseTarget)
        assertEquals(occurredAt, snapshot.nfcVerifiedAtEpochMillis)
        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.CANCEL_ALARM,
                SessionEffectKind.STOP_RINGING,
                SessionEffectKind.CLEAR_SCAN_REQUEST,
                SessionEffectKind.REMOVE_BLOCKING,
            ),
            decision.effects.map { it.kind },
        )
    }

    @Test
    fun validScanFromArmedAtOrAfterTriggerIsRejected() {
        val armed = armedSnapshot()
        val atTrigger =
            validNfcScannedEvent(expectedRevision = armed.revision, occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS)

        val decision = engine.reduce(snapshot = armed, event = atTrigger)

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.TRIGGER_ALREADY_ELAPSED), decision.violations.map { it.code })
    }

    @Test
    fun validScanFromRingingAwaitingNfcOrTriggeredAwaitingNfcReleasesToCompleted() {
        val ringing = ringingSnapshot()
        val fromRinging =
            engine.reduce(
                snapshot = ringing,
                event =
                    validNfcScannedEvent(
                        expectedRevision = ringing.revision,
                        occurredAtEpochMillis = RELEASED_AT_EPOCH_MILLIS,
                    ),
            )
        assertEquals(ReleaseTarget.COMPLETED, requireNotNull(fromRinging.snapshot).releaseTarget)

        val awaitingNfc = awaitingNfcSnapshot()
        val fromAwaitingNfc =
            engine.reduce(
                snapshot = awaitingNfc,
                event =
                    validNfcScannedEvent(
                        expectedRevision = awaitingNfc.revision,
                        occurredAtEpochMillis = RELEASED_AT_EPOCH_MILLIS,
                    ),
            )
        assertEquals(ReleaseTarget.COMPLETED, requireNotNull(fromAwaitingNfc.snapshot).releaseTarget)

        val triggeredAwaitingNfc = triggeredAwaitingNfcSnapshot()
        val fromTriggered =
            engine.reduce(
                snapshot = triggeredAwaitingNfc,
                event =
                    validNfcScannedEvent(
                        expectedRevision = triggeredAwaitingNfc.revision,
                        occurredAtEpochMillis = RELEASED_AT_EPOCH_MILLIS,
                    ),
            )
        assertEquals(ReleaseTarget.COMPLETED, requireNotNull(fromTriggered.snapshot).releaseTarget)
    }

    @Test
    fun validScanWithoutProofIsRejected() {
        val armed = armedSnapshot()
        val event =
            SessionEvent(
                eventId = EVENT_ID_2,
                sessionId = armed.sessionId,
                kind = SessionEventKind.VALID_NFC_SCANNED,
                occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS - 1_000L,
                expectedRevision = armed.revision,
                activationRequest = null,
                nfcProof = null,
                failureCode = null,
                incident = null,
            )

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertEquals(listOf(ViolationCode.MISSING_NFC_PROOF), decision.violations.map { it.code })
    }

    @Test
    fun validScanWithProofFromAnotherSessionIsRejected() {
        val armed = armedSnapshot()
        val occurredAt = TRIGGER_AT_EPOCH_MILLIS - 1_000L
        val foreignProof =
            NfcVerificationProof(
                boxId = BOX_ID,
                sessionId = OTHER_SESSION_ID,
                eventId = EVENT_ID_2,
                expectedRevision = armed.revision,
                verifiedAtEpochMillis = occurredAt,
            )
        val event =
            validNfcScannedEvent(
                expectedRevision = armed.revision,
                occurredAtEpochMillis = occurredAt,
                proofOverride = foreignProof,
            )

        val decision = engine.reduce(snapshot = armed, event = event)

        assertEquals(armed, decision.snapshot)
        assertEquals(listOf(ViolationCode.UNEXPECTED_EVENT_PAYLOAD), decision.violations.map { it.code })
    }

    @Test
    fun invalidNfcScannedLeavesStateUnchangedWithOnlyPublishEffect() {
        val armed = armedSnapshot()
        val event = invalidNfcScannedEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(snapshot = armed, event = event)

        val snapshot = requireNotNull(decision.snapshot)
        assertEquals(armed.state, snapshot.state)
        assertEquals(armed.revision + 1, snapshot.revision)
        assertEquals(listOf(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT), decision.effects.map { it.kind })
        assertTrue(decision.violations.isEmpty())
    }

    @Test
    fun validScanInPreparingIsRejected() {
        val preparing = preparingSnapshot()
        val event =
            validNfcScannedEvent(expectedRevision = preparing.revision, occurredAtEpochMillis = CREATED_AT_EPOCH_MILLIS)

        val decision = engine.reduce(snapshot = preparing, event = event)

        assertEquals(preparing, decision.snapshot)
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun validScanWithoutActiveSessionIsRejected() {
        val event = validNfcScannedEvent(expectedRevision = 1, occurredAtEpochMillis = CREATED_AT_EPOCH_MILLIS)

        val decision = engine.reduce(snapshot = null, event = event)

        assertNull(decision.snapshot)
        assertTrue(decision.violations.isNotEmpty())
    }
}
