package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.mapping.SessionSnapshotDtoFixtures
import org.junit.Test

/**
 * Aller-retour `(SessionSnapshotDto, AndroidSessionExtras, receipts, effects) ↔
 * DirectBootSnapshot.Active` (SPEC_CORE_KMP §13 : « Android vérifie en plus que son snapshot
 * Direct Boot actif peut être converti en `SessionSnapshot` avant le premier déverrouillage »).
 */
class DirectBootMapperTest {
    private val extras = SessionSnapshotDtoFixtures.extras()
    private val receipts =
        listOf(
            EventReceipt(
                eventId = "22222222-2222-2222-2222-222222222222",
                sessionId = "11111111-1111-1111-1111-111111111111",
                payloadSha256Hex = "b".repeat(64),
                appliedRevision = 3,
                receivedAtEpochMillis = 1_700_000_002_000L,
            ),
        )
    private val effects =
        listOf(
            PendingEffect(
                effectId = "11111111-1111-1111-1111-111111111111:3:SCHEDULE_ALARM:0",
                sessionId = "11111111-1111-1111-1111-111111111111",
                revision = 3,
                kind = SessionEffectKind.SCHEDULE_ALARM,
                ordinal = 0,
                payloadJson = null,
                status = EffectStatus.PENDING,
                lastError = null,
            ),
            PendingEffect(
                effectId = "11111111-1111-1111-1111-111111111111:3:RECORD_INCIDENT:1",
                sessionId = "11111111-1111-1111-1111-111111111111",
                revision = 3,
                kind = SessionEffectKind.RECORD_INCIDENT,
                ordinal = 1,
                payloadJson = """{"type":"incident","incident":{}}""",
                status = EffectStatus.FAILED,
                lastError = "ANDROID_INCIDENT_WRITE_FAILED",
            ),
        )

    @Test
    fun preparingSnapshotRoundTrips() = assertRoundTrips(SessionSnapshotDtoFixtures.preparingSnapshot())

    @Test
    fun armedSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.preparingSnapshot().copy(
                state = SessionState.ARMED,
                armedAtEpochMillis = 1_700_000_001_000L,
            ),
        )

    @Test
    fun ringingSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(state = SessionState.RINGING, releaseTarget = null),
        )

    @Test
    fun awaitingNfcSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.AWAITING_NFC,
                releaseTarget = null,
            ),
        )

    @Test
    fun triggeredAwaitingNfcSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.TRIGGERED_AWAITING_NFC,
                releaseTarget = null,
            ),
        )

    @Test
    fun releasingTowardCompletedSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.RELEASING,
                releaseTarget = ReleaseTarget.COMPLETED,
            ),
        )

    @Test
    fun releasingTowardCancelledSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.RELEASING,
                releaseTarget = ReleaseTarget.CANCELLED,
            ),
        )

    @Test
    fun completedSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.COMPLETED,
                releaseTarget = ReleaseTarget.COMPLETED,
            ),
        )

    @Test
    fun cancelledSnapshotRoundTrips() =
        assertRoundTrips(
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.CANCELLED,
                releaseTarget = ReleaseTarget.CANCELLED,
            ),
        )

    @Test
    fun failedSnapshotRoundTrips() = assertRoundTrips(SessionSnapshotDtoFixtures.failedSnapshot())

    @Test
    fun receiptsRoundTripWithTheirFingerprintAndAppliedRevision() {
        val active =
            DirectBootMapper.projectionOf(
                SessionSnapshotDtoFixtures.preparingSnapshot(),
                extras,
                receipts,
                effects,
            )

        assertThat(active.toReceipts()).isEqualTo(receipts)
    }

    @Test
    fun pendingEffectsRoundTripWithTheirPayloadAndError() {
        val active =
            DirectBootMapper.projectionOf(
                SessionSnapshotDtoFixtures.preparingSnapshot(),
                extras,
                receipts,
                effects,
            )

        assertThat(active.toPendingEffects()).isEqualTo(effects)
    }

    @Test
    fun blockedPackagesRoundTripWithTheirDisplayNameSnapshot() {
        val active =
            DirectBootMapper.projectionOf(
                SessionSnapshotDtoFixtures.preparingSnapshot(),
                extras,
                receipts,
                effects,
            )

        assertThat(active.toExtras()).isEqualTo(extras)
    }

    @Test
    fun domainSchemaVersionAndRevisionComeFromTheSnapshot() {
        val snapshot = SessionSnapshotDtoFixtures.preparingSnapshot(revision = 5)

        val active = DirectBootMapper.projectionOf(snapshot, extras, receipts, effects)

        assertThat(active.domainSchemaVersion).isEqualTo(snapshot.schemaVersion)
        assertThat(active.domainRevision).isEqualTo(snapshot.revision)
        assertThat(active.projectionSchemaVersion).isEqualTo(DIRECT_BOOT_PROJECTION_SCHEMA_VERSION)
    }

    @Test
    fun aPendingDeferredBlockingIsProjectedAndReadBackUnchanged() {
        val snapshot = SessionSnapshotDtoFixtures.armedBlockingPendingSnapshot()

        val active = DirectBootMapper.projectionOf(snapshot, extras, receipts, effects)

        assertThat(active.blockingLocalDate).isEqualTo(snapshot.blockingSchedule.localDateIso)
        assertThat(active.blockingLocalTime).isEqualTo(snapshot.blockingSchedule.localTimeIso)
        assertThat(active.blockingStartsAtEpochMillis).isEqualTo(snapshot.blockingSchedule.startsAtEpochMillis)
        assertThat(active.blockingAppliedAtEpochMillis).isNull()
        assertThat(active.toSnapshotDto()).isEqualTo(snapshot)
        assertThat(active.toSnapshotDto().isBlockingPending).isTrue()
    }

    @Test
    fun anAppliedDeferredBlockingIsProjectedAndReadBackUnchanged() {
        val snapshot = SessionSnapshotDtoFixtures.armedBlockingAppliedSnapshot()

        val active = DirectBootMapper.projectionOf(snapshot, extras, receipts, effects)

        assertThat(active.toSnapshotDto()).isEqualTo(snapshot)
        assertThat(active.toSnapshotDto().isBlockingPending).isFalse()
    }

    /** Toute écriture se fait en v2, quelle que soit la session projetée (SPEC_ANDROID §7.3). */
    @Test
    fun theProjectionIsAlwaysWrittenAtTheCurrentVersion() {
        val active =
            DirectBootMapper.projectionOf(
                SessionSnapshotDtoFixtures.armedBlockingPendingSnapshot(),
                extras,
                receipts,
                effects,
            )

        assertThat(active.projectionSchemaVersion).isEqualTo(2)
    }

    /**
     * SPEC_ANDROID §7.3 : « les champs absents se lisent comme un blocage immédiat dont
     * `blockingAppliedAtEpochMillis` vaut `createdAtEpochMillis` ». Le fichier v1 est simulé par une
     * projection dont `projectionSchemaVersion` vaut 1 et dont les quatre champs sont nuls — ce que
     * produit exactement la désérialisation d'un JSON écrit avant ce lot.
     */
    @Test
    fun aVersionOneProjectionReadsAsAnImmediateBlockingAppliedAtCreation() {
        val legacy =
            DirectBootMapper
                .projectionOf(SessionSnapshotDtoFixtures.preparingSnapshot(), extras, receipts, effects)
                .copy(projectionSchemaVersion = 1)

        val snapshot = legacy.toSnapshotDto()

        assertThat(snapshot.blockingSchedule).isEqualTo(BlockingScheduleDto())
        assertThat(snapshot.blockingAppliedAtEpochMillis).isEqualTo(legacy.createdAtEpochMillis)
        assertThat(snapshot.isBlockingPending).isFalse()
    }

    /**
     * La distinction se fait sur la version du format, jamais sur la nullité des champs : en v2, une
     * session à blocage immédiat les laisse nuls elle aussi, et les confondre réécrirait
     * `blockingAppliedAtEpochMillis` avec `createdAtEpochMillis` à chaque relecture.
     */
    @Test
    fun aVersionTwoImmediateBlockingKeepsItsOwnAppliedInstant() {
        val applied = 1_700_000_009_000L
        val active =
            DirectBootMapper
                .projectionOf(SessionSnapshotDtoFixtures.preparingSnapshot(), extras, receipts, effects)
                .copy(blockingAppliedAtEpochMillis = applied)

        val snapshot = active.toSnapshotDto()

        assertThat(snapshot.blockingAppliedAtEpochMillis).isEqualTo(applied)
        assertThat(snapshot.blockingAppliedAtEpochMillis).isNotEqualTo(active.createdAtEpochMillis)
    }

    private fun assertRoundTrips(snapshot: SessionSnapshotDto) {
        val active = DirectBootMapper.projectionOf(snapshot, extras, receipts, effects)

        assertThat(active.toSnapshotDto()).isEqualTo(snapshot)
    }
}
