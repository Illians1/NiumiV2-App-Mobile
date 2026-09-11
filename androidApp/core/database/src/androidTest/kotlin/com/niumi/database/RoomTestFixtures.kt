package com.niumi.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.directboot.UnlockState

/** Base Room en mémoire, une par test (patron `:core:system` : `AndroidJUnit4`, sans Hilt). */
internal fun newInMemoryDatabase(): NiumiDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            NiumiDatabase::class.java,
        ).build()

/** Déverrouillé en permanence : ces tests portent sur Room, pas sur la garde `ROOM_BEFORE_UNLOCK`. */
internal object AlwaysUnlockedState : UnlockState {
    override val isUserUnlocked: Boolean = true
}

internal object RoomTestFixtures {
    private val referenceWakeSchedule =
        WakeScheduleDto(
            localDateIso = "2026-09-08",
            localTimeIso = "07:00",
            zoneIdAtActivation = "Europe/Paris",
            triggerAtEpochMillis = 1_800_000_000_000L,
        )

    internal fun preparingSnapshot(
        sessionId: String,
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

    internal fun extras(
        boxId: String = "550e8400-e29b-41d4-a716-446655440000",
        boxTokenSha256Hex: String = "a".repeat(64),
        ringtoneKey: String = "niumi_default",
        vibrationEnabled: Boolean = true,
        blockedPackages: List<BlockedPackage> =
            listOf(BlockedPackage("com.example.first", "Première application")),
    ) = AndroidSessionExtras(boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, blockedPackages)

    internal fun receipt(
        eventId: String,
        sessionId: String,
        appliedRevision: Long = 1,
    ) = EventReceipt(
        eventId = eventId,
        sessionId = sessionId,
        payloadSha256Hex = "b".repeat(64),
        appliedRevision = appliedRevision,
        receivedAtEpochMillis = 1_700_000_000_500L,
    )

    internal fun effect(
        sessionId: String,
        revision: Long,
        ordinal: Int,
        kind: com.niumi.core.interop.SessionEffectKindDto = com.niumi.core.interop.SessionEffectKindDto.SCHEDULE_ALARM,
    ) = PendingEffect(
        effectId = "$sessionId:$revision:$kind:$ordinal",
        sessionId = sessionId,
        revision = revision,
        kind = kind,
        ordinal = ordinal,
        payloadJson = null,
        status = EffectStatus.PENDING,
        lastError = null,
    )
}
