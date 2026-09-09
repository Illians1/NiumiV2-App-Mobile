package com.niumi.database.mapping

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.entity.AlarmSessionEntity

/**
 * Aller-retour `SessionSnapshotDto ↔ AlarmSessionEntity` (SPEC_CORE_KMP §13). Les mappers de
 * `:shared:core/interop` sont `internal` (ETAPE-08.md) : les DTO sont construits ici directement.
 * `WakeScheduleDto` est aplati dans quatre colonnes (`localDate`, `localTime`,
 * `zoneIdAtActivation`, `triggerAtEpochMillis`) ; `localDateIso`/`localTimeIso` du DTO deviennent
 * `localDate`/`localTime` (nom de colonne imposé par SPEC_ANDROID §7.2, différent du DTO).
 */
fun SessionSnapshotDto.toEntity(extras: AndroidSessionExtras): AlarmSessionEntity =
    AlarmSessionEntity(
        id = sessionId,
        schemaVersion = schemaVersion,
        revision = revision,
        localDate = wakeSchedule.localDateIso,
        localTime = wakeSchedule.localTimeIso,
        zoneIdAtActivation = wakeSchedule.zoneIdAtActivation,
        triggerAtEpochMillis = wakeSchedule.triggerAtEpochMillis,
        state = state,
        releaseTarget = releaseTarget,
        health = health,
        boxId = extras.boxId,
        boxTokenSha256Hex = extras.boxTokenSha256Hex,
        ringtoneKey = extras.ringtoneKey,
        vibrationEnabled = extras.vibrationEnabled,
        createdAtEpochMillis = createdAtEpochMillis,
        armedAtEpochMillis = armedAtEpochMillis,
        ringingAtEpochMillis = ringingAtEpochMillis,
        alarmSoundStoppedAtEpochMillis = alarmSoundStoppedAtEpochMillis,
        triggerElapsedAtEpochMillis = triggerElapsedAtEpochMillis,
        nfcVerifiedAtEpochMillis = nfcVerifiedAtEpochMillis,
        releasingAtEpochMillis = releasingAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        cancelledAtEpochMillis = cancelledAtEpochMillis,
        failureCode = failureCode,
    )

fun AlarmSessionEntity.toSnapshotDto(): SessionSnapshotDto =
    SessionSnapshotDto(
        schemaVersion = schemaVersion,
        revision = revision,
        sessionId = id,
        wakeSchedule =
            WakeScheduleDto(
                localDateIso = localDate,
                localTimeIso = localTime,
                zoneIdAtActivation = zoneIdAtActivation,
                triggerAtEpochMillis = triggerAtEpochMillis,
            ),
        state = state,
        releaseTarget = releaseTarget,
        health = health,
        createdAtEpochMillis = createdAtEpochMillis,
        armedAtEpochMillis = armedAtEpochMillis,
        ringingAtEpochMillis = ringingAtEpochMillis,
        alarmSoundStoppedAtEpochMillis = alarmSoundStoppedAtEpochMillis,
        triggerElapsedAtEpochMillis = triggerElapsedAtEpochMillis,
        nfcVerifiedAtEpochMillis = nfcVerifiedAtEpochMillis,
        releasingAtEpochMillis = releasingAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        cancelledAtEpochMillis = cancelledAtEpochMillis,
        failureCode = failureCode,
    )

/** [blockedPackages] vient d'une requête séparée (`BlockedAppDao`) : pas de colonne dédiée. */
fun AlarmSessionEntity.toExtras(blockedPackages: List<BlockedPackage>): AndroidSessionExtras =
    AndroidSessionExtras(
        boxId = boxId,
        boxTokenSha256Hex = boxTokenSha256Hex,
        ringtoneKey = ringtoneKey,
        vibrationEnabled = vibrationEnabled,
        blockedPackages = blockedPackages,
    )
