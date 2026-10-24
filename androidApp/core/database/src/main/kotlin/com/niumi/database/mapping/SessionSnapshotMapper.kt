package com.niumi.database.mapping

import com.niumi.core.interop.BlockingScheduleDto
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
 *
 * `BlockingScheduleDto` est aplati de la même façon (v3, Lot 6). Les quatre champs sont renseignés
 * **explicitement** dans les deux sens : les valeurs par défaut posées sur les DTO à l'étape 22 sont
 * transitoires, et s'y appuyer ici ferait relire tout blocage différé comme immédiat. Même règle
 * pour `releasedBy` et `disarmedAtEpochMillis` (v6, contrat 1.5) : les oublier ferait relire toute
 * session libérée par la sortie de secours ou désarmée comme libérée sans moyen connu.
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
        blockingLocalDate = blockingSchedule.localDateIso,
        blockingLocalTime = blockingSchedule.localTimeIso,
        blockingStartsAtEpochMillis = blockingSchedule.startsAtEpochMillis,
        blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
        state = state,
        releaseTarget = releaseTarget,
        health = health,
        ringtoneKey = extras.ringtoneKey,
        vibrationEnabled = extras.vibrationEnabled,
        volumeRampSeconds = extras.volumeRampSeconds,
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
        releasedBy = releasedBy,
        disarmedAtEpochMillis = disarmedAtEpochMillis,
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
        blockingSchedule =
            BlockingScheduleDto(
                localDateIso = blockingLocalDate,
                localTimeIso = blockingLocalTime,
                startsAtEpochMillis = blockingStartsAtEpochMillis,
            ),
        blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
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
        releasedBy = releasedBy,
        disarmedAtEpochMillis = disarmedAtEpochMillis,
    )

/** [blockedPackages] vient d'une requête séparée (`BlockedAppDao`) : pas de colonne dédiée sur `alarm_session`. */
fun AlarmSessionEntity.toExtras(blockedPackages: List<BlockedPackage>): AndroidSessionExtras =
    AndroidSessionExtras(
        ringtoneKey = ringtoneKey,
        vibrationEnabled = vibrationEnabled,
        volumeRampSeconds = volumeRampSeconds,
        blockedPackages = blockedPackages,
    )
