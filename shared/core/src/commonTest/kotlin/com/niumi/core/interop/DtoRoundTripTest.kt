package com.niumi.core.interop

import com.niumi.core.domain.NfcScanEventFixtures
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionActivationEventFixtures
import com.niumi.core.domain.SessionEvent
import com.niumi.core.domain.SessionLifecycleEventFixtures
import com.niumi.core.domain.SessionSnapshot
import com.niumi.core.domain.SessionSnapshotFixtures
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Aller-retour `domaine → dto → domaine` pour chacun des neuf états de SPEC_CORE_KMP §5 et pour
 * un événement représentatif de chacun des onze [com.niumi.core.domain.SessionEventKind]
 * (SPEC_CORE_KMP §13 : « les plateformes doivent avoir un test de mapping aller-retour »).
 */
class DtoRoundTripTest {
    @Test
    fun preparingSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.preparingSnapshot())

    @Test
    fun armedSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.armedSnapshot())

    @Test
    fun ringingSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.ringingSnapshot())

    @Test
    fun awaitingNfcSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.awaitingNfcSnapshot())

    @Test
    fun triggeredAwaitingNfcSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.triggeredAwaitingNfcSnapshot())

    @Test
    fun releasingTowardCompletedSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.releasingSnapshot(ReleaseTarget.COMPLETED))

    @Test
    fun releasingTowardCancelledSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.releasingSnapshot(ReleaseTarget.CANCELLED))

    @Test
    fun completedSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.completedSnapshot())

    @Test
    fun cancelledSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.cancelledSnapshot())

    @Test
    fun failedSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.failedSnapshot())

    @Test
    fun activationRequestedEventRoundTrips() =
        assertEventRoundTrips(SessionActivationEventFixtures.activationRequestedEvent())

    @Test
    fun activationSucceededEventRoundTrips() =
        assertEventRoundTrips(SessionActivationEventFixtures.activationSucceededEvent())

    @Test
    fun activationFailedEventRoundTrips() =
        assertEventRoundTrips(SessionActivationEventFixtures.activationFailedEvent())

    @Test
    fun alarmFiredEventRoundTrips() = assertEventRoundTrips(SessionLifecycleEventFixtures.alarmFiredEvent())

    @Test
    fun alarmSoundStoppedEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.alarmSoundStoppedEvent(expectedRevision = 2))

    @Test
    fun triggerElapsedEventRoundTrips() = assertEventRoundTrips(SessionLifecycleEventFixtures.triggerElapsedEvent())

    @Test
    fun triggerElapsedEventWithIncidentRoundTrips() =
        assertEventRoundTrips(
            SessionLifecycleEventFixtures.triggerElapsedEvent(
                incident = SessionLifecycleEventFixtures.incident(),
            ),
        )

    @Test
    fun validNfcScannedEventRoundTripsIncludingProofByReference() {
        val event =
            NfcScanEventFixtures.validNfcScannedEvent(
                expectedRevision = 2,
                occurredAtEpochMillis = 1_800_000_002_000L,
            )

        assertEventRoundTrips(event)
    }

    @Test
    fun invalidNfcScannedEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.invalidNfcScannedEvent(expectedRevision = 2))

    @Test
    fun releaseSucceededEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.releaseSucceededEvent(expectedRevision = 5))

    @Test
    fun releaseFailedEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.releaseFailedEvent(expectedRevision = 5))

    @Test
    fun incidentReportedEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.incidentReportedEvent(expectedRevision = 2))

    @Test
    fun serializedEventWithProofNeverExposesTheProofOrTheBoxId() {
        val event =
            NfcScanEventFixtures.validNfcScannedEvent(
                expectedRevision = 2,
                occurredAtEpochMillis = 1_800_000_002_000L,
            )
        val boxId = requireNotNull(event.nfcProof).boxId

        val json = Json.encodeToString(SessionEventDto.serializer(), event.toDto())

        assertFalse(json.contains("proof", ignoreCase = true))
        assertFalse(json.contains(boxId))
    }

    private fun assertSnapshotRoundTrips(snapshot: SessionSnapshot) {
        assertEquals(snapshot, snapshot.toDto().toDomain())
    }

    private fun assertEventRoundTrips(event: SessionEvent) {
        assertEquals(event, event.toDto().toDomain())
    }
}
