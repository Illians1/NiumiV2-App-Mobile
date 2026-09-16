package com.niumi.database.mapping

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.isBlockingPending
import org.junit.Test

/**
 * Aller-retour `SessionSnapshotDto → AlarmSessionEntity → SessionSnapshotDto` pour les neuf états
 * (SPEC_CORE_KMP §13 : « les plateformes doivent avoir un test de mapping aller-retour »).
 */
class SessionSnapshotMapperTest {
    private val extras = SessionSnapshotDtoFixtures.extras()

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
    fun allNullableTimestampsRoundTripWithDistinctValues() {
        val snapshot = SessionSnapshotDtoFixtures.fullyPopulatedSnapshot()

        val roundTripped = snapshot.toEntity(extras).toSnapshotDto()

        // Chaque colonne nullable comparée individuellement : une inversion de deux colonnes
        // adjacentes (ex. ringingAt / alarmSoundStoppedAt) ferait échouer une assertion précise
        // même si l'égalité globale de l'objet passait par coïncidence.
        assertThat(roundTripped.armedAtEpochMillis).isEqualTo(snapshot.armedAtEpochMillis)
        assertThat(roundTripped.ringingAtEpochMillis).isEqualTo(snapshot.ringingAtEpochMillis)
        assertThat(roundTripped.alarmSoundStoppedAtEpochMillis).isEqualTo(snapshot.alarmSoundStoppedAtEpochMillis)
        assertThat(roundTripped.triggerElapsedAtEpochMillis).isEqualTo(snapshot.triggerElapsedAtEpochMillis)
        assertThat(roundTripped.nfcVerifiedAtEpochMillis).isEqualTo(snapshot.nfcVerifiedAtEpochMillis)
        assertThat(roundTripped.releasingAtEpochMillis).isEqualTo(snapshot.releasingAtEpochMillis)
        assertThat(roundTripped.completedAtEpochMillis).isEqualTo(snapshot.completedAtEpochMillis)
        assertThat(roundTripped.cancelledAtEpochMillis).isEqualTo(snapshot.cancelledAtEpochMillis)
        assertThat(roundTripped).isEqualTo(snapshot)
    }

    @Test
    fun extrasRoundTripPreservesBlockedPackageOrder() {
        val snapshot = SessionSnapshotDtoFixtures.preparingSnapshot()
        val entity = snapshot.toEntity(extras)

        val roundTrippedExtras = entity.toExtras(extras.blockedPackages)

        assertThat(roundTrippedExtras).isEqualTo(extras)
        assertThat(roundTrippedExtras.blockedPackages.map { it.packageName })
            .containsExactlyElementsIn(extras.blockedPackages.map { it.packageName })
            .inOrder()
    }

    @Test
    fun aPendingDeferredBlockingRoundTripsAndStaysPending() {
        val snapshot = SessionSnapshotDtoFixtures.armedBlockingPendingSnapshot()

        val entity = snapshot.toEntity(extras)

        // Colonne par colonne avant l'égalité globale, même motif que les horodatages : les quatre
        // colonnes sont adjacentes et de deux types seulement, donc interchangeables par erreur.
        assertThat(entity.blockingLocalDate).isEqualTo(snapshot.blockingSchedule.localDateIso)
        assertThat(entity.blockingLocalTime).isEqualTo(snapshot.blockingSchedule.localTimeIso)
        assertThat(entity.blockingStartsAtEpochMillis).isEqualTo(snapshot.blockingSchedule.startsAtEpochMillis)
        assertThat(entity.blockingAppliedAtEpochMillis).isNull()
        assertThat(entity.toSnapshotDto()).isEqualTo(snapshot)
        assertThat(entity.toSnapshotDto().isBlockingPending).isTrue()
    }

    @Test
    fun anAppliedDeferredBlockingRoundTripsAndIsNoLongerPending() {
        val snapshot = SessionSnapshotDtoFixtures.armedBlockingAppliedSnapshot()

        val entity = snapshot.toEntity(extras)

        assertThat(entity.blockingAppliedAtEpochMillis).isEqualTo(snapshot.blockingAppliedAtEpochMillis)
        assertThat(entity.toSnapshotDto()).isEqualTo(snapshot)
        assertThat(entity.toSnapshotDto().isBlockingPending).isFalse()
    }

    /**
     * Les valeurs par défaut des DTO posées à l'étape 22 sont transitoires : le mapper doit
     * renseigner les quatre colonnes **explicitement**. Ce test construit le DTO sans les mentionner
     * — donc en s'appuyant sur ces défauts — et vérifie que l'entité en porte tout de même la trace
     * exacte. Si un jour le mapper oubliait ces champs, il compilerait toujours et ce test tomberait.
     */
    @Test
    fun aSnapshotBuiltWithoutTheBlockingFieldsStillMapsThemExplicitly() {
        val snapshot = SessionSnapshotDtoFixtures.preparingSnapshot()

        val entity = snapshot.toEntity(extras)

        assertThat(entity.blockingLocalDate).isNull()
        assertThat(entity.blockingLocalTime).isNull()
        assertThat(entity.blockingStartsAtEpochMillis).isNull()
        assertThat(entity.blockingAppliedAtEpochMillis).isNull()
        assertThat(entity.toSnapshotDto()).isEqualTo(snapshot)
    }

    private fun assertRoundTrips(snapshot: SessionSnapshotDto) {
        val entity = snapshot.toEntity(extras)

        assertThat(entity.toSnapshotDto()).isEqualTo(snapshot)
    }
}
