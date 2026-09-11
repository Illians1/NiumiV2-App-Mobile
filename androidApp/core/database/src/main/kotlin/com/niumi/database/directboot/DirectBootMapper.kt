package com.niumi.database.directboot

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect

/**
 * Aller-retour `(SessionSnapshotDto, AndroidSessionExtras, receipts, effects) ↔
 * DirectBootSnapshot.Active` (SPEC_CORE_KMP §13, SPEC_ANDROID §7.3). N'importe rien de
 * `mapping.SessionSnapshotMapper` : les deux mappers projettent la même session vers deux formats
 * physiques distincts (Room, JSON Direct Boot) qui ne doivent pas dériver ensemble — leur
 * cohérence est prouvée par un test croisé (`DirectBootRoomParityTest`), pas par du partage de
 * code. `WakeScheduleDto` est aplati comme dans `SessionSnapshotMapper` (mêmes noms de champs que
 * `AlarmSessionEntity`). Extensions au niveau fichier (pas membres d'un objet, contrairement à
 * `SessionEffectMapper`) : appelables directement (`active.toSnapshotDto()`), même convention que
 * `SessionSnapshotMapper.kt`.
 */
object DirectBootMapper {
    fun projectionOf(
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
        receipts: List<EventReceipt>,
        effects: List<PendingEffect>,
    ): DirectBootSnapshot.Active =
        DirectBootSnapshot.Active(
            domainSchemaVersion = snapshot.schemaVersion,
            domainRevision = snapshot.revision,
            sessionId = snapshot.sessionId,
            localDate = snapshot.wakeSchedule.localDateIso,
            localTime = snapshot.wakeSchedule.localTimeIso,
            zoneIdAtActivation = snapshot.wakeSchedule.zoneIdAtActivation,
            triggerAtEpochMillis = snapshot.wakeSchedule.triggerAtEpochMillis,
            state = snapshot.state,
            releaseTarget = snapshot.releaseTarget,
            health = snapshot.health,
            createdAtEpochMillis = snapshot.createdAtEpochMillis,
            armedAtEpochMillis = snapshot.armedAtEpochMillis,
            ringingAtEpochMillis = snapshot.ringingAtEpochMillis,
            alarmSoundStoppedAtEpochMillis = snapshot.alarmSoundStoppedAtEpochMillis,
            triggerElapsedAtEpochMillis = snapshot.triggerElapsedAtEpochMillis,
            nfcVerifiedAtEpochMillis = snapshot.nfcVerifiedAtEpochMillis,
            releasingAtEpochMillis = snapshot.releasingAtEpochMillis,
            completedAtEpochMillis = snapshot.completedAtEpochMillis,
            cancelledAtEpochMillis = snapshot.cancelledAtEpochMillis,
            failureCode = snapshot.failureCode,
            ringtoneKey = extras.ringtoneKey,
            vibrationEnabled = extras.vibrationEnabled,
            boxId = extras.boxId,
            boxTokenSha256Hex = extras.boxTokenSha256Hex,
            blockedPackages = extras.blockedPackages.map { it.toProjection() },
            eventReceipts = receipts.map { it.toProjection() },
            pendingEffects = effects.map { it.toProjection() },
        )
}

fun DirectBootSnapshot.Active.toSnapshotDto(): SessionSnapshotDto =
    SessionSnapshotDto(
        schemaVersion = domainSchemaVersion,
        revision = domainRevision,
        sessionId = sessionId,
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

fun DirectBootSnapshot.Active.toExtras(): AndroidSessionExtras =
    AndroidSessionExtras(
        boxId = boxId,
        boxTokenSha256Hex = boxTokenSha256Hex,
        ringtoneKey = ringtoneKey,
        vibrationEnabled = vibrationEnabled,
        blockedPackages = blockedPackages.map { it.toDomain() },
    )

fun DirectBootSnapshot.Active.toReceipts(): List<EventReceipt> = eventReceipts.map { it.toDomain() }

fun DirectBootSnapshot.Active.toPendingEffects(): List<PendingEffect> = pendingEffects.map { it.toDomain() }

private fun BlockedPackage.toProjection(): DirectBootBlockedPackage =
    DirectBootBlockedPackage(packageName, displayNameSnapshot)

private fun DirectBootBlockedPackage.toDomain(): BlockedPackage = BlockedPackage(packageName, displayNameSnapshot)

private fun EventReceipt.toProjection(): DirectBootReceipt =
    DirectBootReceipt(eventId, sessionId, payloadSha256Hex, appliedRevision, receivedAtEpochMillis)

private fun DirectBootReceipt.toDomain(): EventReceipt =
    EventReceipt(eventId, sessionId, payloadSha256Hex, appliedRevision, receivedAtEpochMillis)

private fun PendingEffect.toProjection(): DirectBootEffect =
    DirectBootEffect(effectId, sessionId, revision, kind, ordinal, payloadJson, status, lastError)

private fun DirectBootEffect.toDomain(): PendingEffect =
    PendingEffect(effectId, sessionId, revision, kind, ordinal, payloadJson, status, lastError)
