package com.niumi.core.interop

import com.niumi.core.domain.SessionEvent
import com.niumi.core.domain.SessionSnapshot

// Mappers du snapshot et de l'événement (SPEC_CORE_KMP §7.1, §6). Séparés de `DtoMappers.kt` pour
// rester sous le seuil detekt `TooManyFunctions` (voir `ETAPE-08.md`).

internal fun SessionSnapshotDto.toDomain(): SessionSnapshot =
    SessionSnapshot(
        schemaVersion = schemaVersion,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = wakeSchedule.toDomain(),
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

internal fun SessionSnapshot.toDto(): SessionSnapshotDto =
    SessionSnapshotDto(
        schemaVersion = schemaVersion,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = wakeSchedule.toDto(),
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

internal fun SessionEventDto.toDomain(): SessionEvent =
    SessionEvent(
        eventId = eventId,
        sessionId = sessionId,
        kind = kind,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = activationRequest?.toDomain(),
        nfcProof = nfcProof,
        failureCode = failureCode,
        incident = incident?.toDomain(),
    )

internal fun SessionEvent.toDto(): SessionEventDto =
    SessionEventDto(
        eventId = eventId,
        sessionId = sessionId,
        kind = kind,
        occurredAtEpochMillis = occurredAtEpochMillis,
        expectedRevision = expectedRevision,
        activationRequest = activationRequest?.toDto(),
        nfcProof = nfcProof,
        failureCode = failureCode,
        incident = incident?.toDto(),
    )
