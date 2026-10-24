package com.niumi.core.domain

import com.niumi.core.domain.EmergencyExitEventFixtures.emergencyExitConfirmedEvent
import com.niumi.core.domain.EmergencyExitEventFixtures.proofMatching
import com.niumi.core.domain.NfcScanEventFixtures.validNfcScannedEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.alarmFiredEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures.releaseSucceededEvent
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingAppliedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedBlockingPendingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.armedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.awaitingNfcSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.completedSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.ringingSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures.triggeredAwaitingNfcSnapshot
import com.niumi.core.emergency.EmergencyExitProof
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrat 1.5 (SPEC_CORE_KMP §5.1, §5.2, §12) : la sortie de secours prouvée libère une session
 * comme un scan — `CANCELLED` le soir, `COMPLETED` le matin (D2, décision du 2026-10-24) —, avec
 * `releasedBy = EMERGENCY_EXIT` et sans `nfcVerifiedAtEpochMillis`.
 */
class SessionEngineEmergencyExitTest {
    private val engine = SessionEngine()

    @Test
    fun fromArmedWithBlockingAppliedTheSessionIsCancelled() {
        val armed = armedBlockingAppliedSnapshot()
        val occurredAt = TRIGGER_AT_EPOCH_MILLIS - 1_000L
        val event = emergencyExitConfirmedEvent(expectedRevision = armed.revision, occurredAtEpochMillis = occurredAt)

        val decision = engine.reduce(armed, event)

        val snapshot = requireNotNull(decision.snapshot)
        assertTrue(decision.violations.isEmpty())
        assertEquals(SessionState.RELEASING, snapshot.state)
        assertEquals(ReleaseTarget.CANCELLED, snapshot.releaseTarget)
        assertEquals(ReleaseMeans.EMERGENCY_EXIT, snapshot.releasedBy)
        assertNull(snapshot.nfcVerifiedAtEpochMillis)
        assertEquals(occurredAt, snapshot.releasingAtEpochMillis)
        assertEquals(armed.revision + 1, snapshot.revision)
    }

    @Test
    fun fromImmediateArmedTheSessionIsCancelledToo() {
        val armed = armedSnapshot()
        val event =
            emergencyExitConfirmedEvent(
                expectedRevision = armed.revision,
                occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS - 1_000L,
            )

        assertEquals(ReleaseTarget.CANCELLED, engine.reduce(armed, event).snapshot?.releaseTarget)
    }

    @Test
    fun fromEveryMorningStateTheSessionIsCompleted() {
        for (source in listOf(ringingSnapshot(), awaitingNfcSnapshot(), triggeredAwaitingNfcSnapshot())) {
            val event =
                emergencyExitConfirmedEvent(
                    expectedRevision = source.revision,
                    occurredAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
                )

            val snapshot = requireNotNull(engine.reduce(source, event).snapshot) { "${source.state}" }

            assertEquals(SessionState.RELEASING, snapshot.state, "${source.state}")
            assertEquals(ReleaseTarget.COMPLETED, snapshot.releaseTarget, "${source.state}")
            assertEquals(ReleaseMeans.EMERGENCY_EXIT, snapshot.releasedBy, "${source.state}")
            assertNull(snapshot.nfcVerifiedAtEpochMillis, "${source.state}")
        }
    }

    @Test
    fun theEffectsAreThoseOfAValidScan() {
        val cases =
            listOf(
                armedBlockingAppliedSnapshot() to TRIGGER_AT_EPOCH_MILLIS - 1_000L,
                ringingSnapshot() to NFC_VERIFIED_AT_EPOCH_MILLIS,
            )
        for ((source, occurredAt) in cases) {
            val scan = validNfcScannedEvent(expectedRevision = source.revision, occurredAtEpochMillis = occurredAt)
            val exit =
                emergencyExitConfirmedEvent(expectedRevision = source.revision, occurredAtEpochMillis = occurredAt)

            assertEquals(
                engine.reduce(source, scan).effects.map { it.kind },
                engine.reduce(source, exit).effects.map { it.kind },
                "${source.state}",
            )
        }
    }

    @Test
    fun aDeferredSessionCancelsItsBlockingStartAndANonDeferredOneDoesNot() {
        val applied = armedBlockingAppliedSnapshot()
        val ringing = ringingSnapshot()

        val deferred =
            engine.reduce(
                applied,
                emergencyExitConfirmedEvent(applied.revision, TRIGGER_AT_EPOCH_MILLIS - 1_000L),
            )
        val immediate =
            engine.reduce(
                ringing,
                emergencyExitConfirmedEvent(ringing.revision, NFC_VERIFIED_AT_EPOCH_MILLIS),
            )

        assertEquals(
            listOf(
                SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT,
                SessionEffectKind.CANCEL_ALARM,
                SessionEffectKind.CANCEL_BLOCKING_START,
                SessionEffectKind.STOP_RINGING,
                SessionEffectKind.CLEAR_SCAN_REQUEST,
                SessionEffectKind.REMOVE_BLOCKING,
            ),
            deferred.effects.map { it.kind },
        )
        assertTrue(immediate.effects.none { it.kind == SessionEffectKind.CANCEL_BLOCKING_START })
    }

    @Test
    fun withoutProofTheExitIsRefusedWithMissingEmergencyProof() {
        val ringing = ringingSnapshot()
        val event =
            emergencyExitConfirmedEvent(
                expectedRevision = ringing.revision,
                occurredAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
                withoutProof = true,
            )

        val decision = engine.reduce(ringing, event)

        assertEquals(ringing, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.MISSING_EMERGENCY_PROOF), decision.violations.map { it.code })
    }

    @Test
    fun beforeBedtimeTheExitIsRefusedBecauseThePlanDisarmsInstead() {
        val pending = armedBlockingPendingSnapshot()
        val event =
            emergencyExitConfirmedEvent(
                expectedRevision = pending.revision,
                occurredAtEpochMillis = DISARM_REQUESTED_AT_EPOCH_MILLIS,
            )

        val decision = engine.reduce(pending, event)

        assertEquals(pending, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun fromArmedAtOrAfterTheTriggerTheExitIsRefusedLikeAScan() {
        // Le coordinateur doit produire TRIGGER_ELAPSED d'abord : sinon le matin mènerait à CANCELLED.
        val armed = armedBlockingAppliedSnapshot()
        val event =
            emergencyExitConfirmedEvent(
                expectedRevision = armed.revision,
                occurredAtEpochMillis = TRIGGER_AT_EPOCH_MILLIS,
            )

        val decision = engine.reduce(armed, event)

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.TRIGGER_ALREADY_ELAPSED), decision.violations.map { it.code })
    }

    @Test
    fun aProofBoundToAnotherEventIsRefused() {
        val ringing = ringingSnapshot()
        val shape =
            emergencyExitConfirmedEvent(
                expectedRevision = ringing.revision,
                occurredAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
                withoutProof = true,
            )
        val foreignProof =
            EmergencyExitProof(
                textId = EmergencyExitEventFixtures.TEXT_ID,
                sessionId = shape.sessionId,
                eventId = EVENT_ID_1,
                expectedRevision = ringing.revision,
                verifiedAtEpochMillis = NFC_VERIFIED_AT_EPOCH_MILLIS,
            )

        val decision = engine.reduce(ringing, shape.copy(emergencyProof = foreignProof))

        assertEquals(ringing, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.UNEXPECTED_EVENT_PAYLOAD), decision.violations.map { it.code })
    }

    @Test
    fun aProofCarriedByAnotherEventIsAnUnexpectedPayload() {
        val armed = armedSnapshot()
        val alarm = alarmFiredEvent(expectedRevision = armed.revision)

        val decision = engine.reduce(armed, alarm.copy(emergencyProof = proofMatching(alarm)))

        assertEquals(armed, decision.snapshot)
        assertTrue(decision.effects.isEmpty())
        assertEquals(listOf(ViolationCode.UNEXPECTED_EVENT_PAYLOAD), decision.violations.map { it.code })
    }

    @Test
    fun fromAFinalStateTheExitIsRefused() {
        val completed = completedSnapshot()
        val event = emergencyExitConfirmedEvent(completed.revision, RELEASED_AT_EPOCH_MILLIS + 1_000L)

        val decision = engine.reduce(completed, event)

        assertEquals(completed, decision.snapshot)
        assertEquals(listOf(ViolationCode.INVALID_STATE_TRANSITION), decision.violations.map { it.code })
    }

    @Test
    fun releaseSucceededReachesTheFinalStateAndKeepsTheReleaseMeans() {
        val ringing = ringingSnapshot()
        val releasing =
            requireNotNull(
                engine
                    .reduce(
                        ringing,
                        emergencyExitConfirmedEvent(ringing.revision, NFC_VERIFIED_AT_EPOCH_MILLIS),
                    ).snapshot,
            )
        val armed = armedBlockingAppliedSnapshot()
        val cancelling =
            requireNotNull(
                engine
                    .reduce(
                        armed,
                        emergencyExitConfirmedEvent(armed.revision, TRIGGER_AT_EPOCH_MILLIS - 1_000L),
                    ).snapshot,
            )

        val completed = requireNotNull(engine.reduce(releasing, releaseSucceededEvent(releasing.revision)).snapshot)
        val cancelled = requireNotNull(engine.reduce(cancelling, releaseSucceededEvent(cancelling.revision)).snapshot)

        assertEquals(SessionState.COMPLETED, completed.state)
        assertEquals(ReleaseMeans.EMERGENCY_EXIT, completed.releasedBy)
        assertEquals(RELEASED_AT_EPOCH_MILLIS, completed.completedAtEpochMillis)
        assertEquals(SessionState.CANCELLED, cancelled.state)
        assertEquals(ReleaseMeans.EMERGENCY_EXIT, cancelled.releasedBy)
    }
}
