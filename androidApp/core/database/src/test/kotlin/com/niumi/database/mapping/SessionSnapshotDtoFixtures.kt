package com.niumi.database.mapping

import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage

/**
 * Fixtures propres à `:core:database` : les fixtures de `:shared:core` (`SessionFixtures.kt`)
 * sont `internal` à `commonTest` et inaccessibles d'un autre module Gradle. Chaque horodatage
 * nullable a une valeur **distincte** pour attraper l'inversion de deux colonnes sur un mapper à
 * 24 champs (SessionSnapshotMapperTest.allNullableTimestampsRoundTripWithDistinctValues).
 */
internal object SessionSnapshotDtoFixtures {
    private val referenceWakeSchedule =
        WakeScheduleDto(
            localDateIso = "2026-09-08",
            localTimeIso = "07:00",
            zoneIdAtActivation = "Europe/Paris",
            triggerAtEpochMillis = 1_800_000_000_000L,
        )

    internal fun preparingSnapshot(
        sessionId: String = "11111111-1111-1111-1111-111111111111",
        revision: Long = 1,
    ) = SessionSnapshotDto(
        schemaVersion = 1,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = referenceWakeSchedule,
        state = SessionState.PREPARING,
        releaseTarget = null,
        health = SessionHealth.HEALTHY,
        createdAtEpochMillis = 1_700_000_000_000L,
        armedAtEpochMillis = null,
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

    /** Toutes les colonnes nullables renseignées, chacune à une valeur distincte. */
    internal fun fullyPopulatedSnapshot(
        sessionId: String = "33333333-3333-3333-3333-333333333333",
        revision: Long = 7,
        state: SessionState = SessionState.RELEASING,
        releaseTarget: ReleaseTarget? = ReleaseTarget.COMPLETED,
        failureCode: String? = null,
    ) = preparingSnapshot(sessionId, revision).copy(
        state = state,
        releaseTarget = releaseTarget,
        health = SessionHealth.DEGRADED,
        armedAtEpochMillis = 1_700_000_001_000L,
        ringingAtEpochMillis = 1_700_000_002_000L,
        alarmSoundStoppedAtEpochMillis = 1_700_000_003_000L,
        triggerElapsedAtEpochMillis = 1_700_000_004_000L,
        nfcVerifiedAtEpochMillis = 1_700_000_005_000L,
        releasingAtEpochMillis = 1_700_000_006_000L,
        completedAtEpochMillis = 1_700_000_007_000L,
        cancelledAtEpochMillis = 1_700_000_008_000L,
        failureCode = failureCode,
    )

    internal fun failedSnapshot(
        sessionId: String = "44444444-4444-4444-4444-444444444444",
        revision: Long = 2,
        failureCode: String = "ANDROID_ALARM_SCHEDULE_FAILED",
    ) = preparingSnapshot(sessionId, revision).copy(
        state = SessionState.FAILED,
        failureCode = failureCode,
    )

    internal fun extras(
        boxId: String = "550e8400-e29b-41d4-a716-446655440000",
        boxTokenSha256Hex: String = "a".repeat(64),
        ringtoneKey: String = "niumi_default",
        vibrationEnabled: Boolean = true,
        blockedPackages: List<BlockedPackage> =
            listOf(
                BlockedPackage("com.example.first", "Première application"),
                BlockedPackage("com.example.second", "Deuxième application"),
            ),
    ) = AndroidSessionExtras(boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, blockedPackages)
}
