package com.niumi.core.interop

import com.niumi.core.domain.EmergencyExitEventFixtures
import com.niumi.core.domain.NfcScanEventFixtures
import com.niumi.core.domain.ReleaseMeans
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Aller-retour `domaine → dto → domaine` pour chacun des dix états de SPEC_CORE_KMP §5 et pour
 * un événement représentatif de chacun des quatorze [com.niumi.core.domain.SessionEventKind]
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
    fun pendingBlockingSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.armedBlockingPendingSnapshot())

    @Test
    fun appliedBlockingSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.armedBlockingAppliedSnapshot())

    @Test
    fun onlyADeferredSnapshotWithoutAppliedInstantIsPending() {
        assertTrue(SessionSnapshotFixtures.armedBlockingPendingSnapshot().toDto().isBlockingPending)
        assertFalse(SessionSnapshotFixtures.armedBlockingAppliedSnapshot().toDto().isBlockingPending)
        assertFalse(SessionSnapshotFixtures.armedSnapshot().toDto().isBlockingPending)
    }

    @Test
    fun aVersionOneSnapshotIsReadAsAnImmediateBlockingAndIsNeverPending() {
        // SPEC_CORE_KMP §7.1, §13 : un snapshot persisté avant le contrat 1.3 ne porte aucun champ
        // de blocage. Sa relecture doit donner un blocage immédiat, jamais une session en attente.
        val versionOneJson =
            """
            {
              "schemaVersion": 1,
              "revision": 2,
              "sessionId": "11111111-1111-1111-1111-111111111111",
              "wakeSchedule": {
                "localDateIso": "2026-09-08",
                "localTimeIso": "07:00",
                "zoneIdAtActivation": "Europe/Paris",
                "triggerAtEpochMillis": 1800000000000
              },
              "state": "ARMED",
              "releaseTarget": null,
              "health": "HEALTHY",
              "createdAtEpochMillis": 1700000000000,
              "armedAtEpochMillis": 1700000001000,
              "ringingAtEpochMillis": null,
              "alarmSoundStoppedAtEpochMillis": null,
              "triggerElapsedAtEpochMillis": null,
              "nfcVerifiedAtEpochMillis": null,
              "releasingAtEpochMillis": null,
              "completedAtEpochMillis": null,
              "cancelledAtEpochMillis": null,
              "failureCode": null
            }
            """.trimIndent()

        val dto = Json.decodeFromString<SessionSnapshotDto>(versionOneJson)

        assertEquals(BlockingScheduleDto(), dto.blockingSchedule)
        assertEquals(null, dto.blockingAppliedAtEpochMillis)
        assertFalse(dto.isBlockingPending)
        assertTrue(dto.toDomain().blockingSchedule.isImmediate)
    }

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
    fun releasingTowardDisarmedSnapshotRoundTrips() =
        assertSnapshotRoundTrips(SessionSnapshotFixtures.releasingTowardDisarmedSnapshot())

    @Test
    fun disarmedSnapshotRoundTrips() = assertSnapshotRoundTrips(SessionSnapshotFixtures.disarmedSnapshot())

    @Test
    fun aVersionThreeDisarmedSnapshotKeepsItsReleaseMeansThroughJson() {
        val disarmed = SessionSnapshotFixtures.disarmedSnapshot().toDto()

        val decoded =
            Json.decodeFromString<SessionSnapshotDto>(
                Json.encodeToString(SessionSnapshotDto.serializer(), disarmed),
            )

        assertEquals(disarmed, decoded)
        assertEquals(ReleaseMeans.PLAN_CHANGE, decoded.releasedBy)
        assertEquals(SessionSnapshotFixtures.disarmedSnapshot(), decoded.toDomain())
    }

    @Test
    fun aVersionTwoSnapshotWithoutReleaseMeansIsReadAsReleasedByScan() {
        // Contrat 1.5, SPEC_CORE_KMP §7.1 : avant 1.5, seul un scan libérait une session.
        val dto = Json.decodeFromString<SessionSnapshotDto>(versionTwoReleasingJson)

        assertNull(dto.releasedBy)
        assertNull(dto.disarmedAtEpochMillis)
        val snapshot = dto.toDomain()
        assertEquals(2, snapshot.schemaVersion)
        assertEquals(ReleaseMeans.NFC_SCAN, snapshot.releasedBy)
    }

    @Test
    fun aSnapshotWithoutScanNorReleaseMeansKeepsNoReleaseMeans() {
        val dto = SessionSnapshotFixtures.armedSnapshot().toDto().copy(schemaVersion = 2)

        assertNull(dto.toDomain().releasedBy)
    }

    @Test
    fun aVersionThreeSnapshotStoredWithoutReleaseMeansIsReadAsReleasedByScan() {
        // Jusqu'à la migration Room v6 (étape 43), Android relit des snapshots v3 sans releasedBy :
        // la déduction ne dépend pas de schemaVersion, sans quoi RELEASE_SUCCEEDED serait refusé.
        val stored = SessionSnapshotFixtures.releasingSnapshot(ReleaseTarget.COMPLETED).toDto().copy(releasedBy = null)

        assertEquals(ReleaseMeans.NFC_SCAN, stored.toDomain().releasedBy)
    }

    @Test
    fun disarmRequestedEventRoundTrips() =
        assertEventRoundTrips(SessionLifecycleEventFixtures.disarmRequestedEvent(expectedRevision = 2))

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
    fun emergencyExitConfirmedEventRoundTripsIncludingProofByReference() {
        val event =
            EmergencyExitEventFixtures.emergencyExitConfirmedEvent(
                expectedRevision = 3,
                occurredAtEpochMillis = 1_800_000_002_000L,
            )

        assertEventRoundTrips(event)
        assertTrue(event.toDto().emergencyProof === event.emergencyProof)
    }

    @Test
    fun serializedEventNeverExposesTheEmergencyProof() {
        val event =
            EmergencyExitEventFixtures.emergencyExitConfirmedEvent(
                expectedRevision = 3,
                occurredAtEpochMillis = 1_800_000_002_000L,
            )

        val json = Json.encodeToString(SessionEventDto.serializer(), event.toDto())

        assertFalse(json.contains("proof", ignoreCase = true))
        assertFalse(json.contains(EmergencyExitEventFixtures.TEXT_ID))
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

    private val versionTwoReleasingJson =
        """
        {
          "schemaVersion": 2,
          "revision": 5,
          "sessionId": "11111111-1111-1111-1111-111111111111",
          "wakeSchedule": {
            "localDateIso": "2026-09-08",
            "localTimeIso": "07:00",
            "zoneIdAtActivation": "Europe/Paris",
            "triggerAtEpochMillis": 1800000000000
          },
          "blockingSchedule": {},
          "state": "RELEASING",
          "releaseTarget": "COMPLETED",
          "health": "HEALTHY",
          "createdAtEpochMillis": 1700000000000,
          "armedAtEpochMillis": 1700000001000,
          "blockingAppliedAtEpochMillis": 1700000000000,
          "ringingAtEpochMillis": 1800000000000,
          "alarmSoundStoppedAtEpochMillis": null,
          "triggerElapsedAtEpochMillis": null,
          "nfcVerifiedAtEpochMillis": 1800000002000,
          "releasingAtEpochMillis": 1800000002000,
          "completedAtEpochMillis": null,
          "cancelledAtEpochMillis": null,
          "failureCode": null
        }
        """.trimIndent()

    private fun assertSnapshotRoundTrips(snapshot: SessionSnapshot) {
        assertEquals(snapshot, snapshot.toDto().toDomain())
    }

    private fun assertEventRoundTrips(event: SessionEvent) {
        assertEquals(event, event.toDto().toDomain())
    }
}
