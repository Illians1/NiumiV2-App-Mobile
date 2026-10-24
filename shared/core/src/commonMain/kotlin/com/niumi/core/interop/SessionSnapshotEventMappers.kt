package com.niumi.core.interop

import com.niumi.core.domain.ReleaseMeans
import com.niumi.core.domain.SessionEvent
import com.niumi.core.domain.SessionSnapshot

// Mappers du snapshot et de l'événement (SPEC_CORE_KMP §7.1, §6). Séparés de `DtoMappers.kt` pour
// rester sous le seuil detekt `TooManyFunctions` (voir `ETAPE-08.md`).
//
// Lecture de `releasedBy` (contrat 1.5) : absent, il vaut `NFC_SCAN` si `nfcVerifiedAtEpochMillis`
// est renseigné, sinon reste nul — avant le contrat 1.5, seul un scan libérait une session. La
// déduction ne dépend pas de `schemaVersion` : jusqu'à la migration Room v6 (étape 43 du plan de
// refonte), Android écrit des snapshots de version 3 sans colonne `releasedBy`, et une session
// relue en `RELEASING` serait sinon refusée par `RELEASE_SUCCEEDED`.

internal fun SessionSnapshotDto.toDomain(): SessionSnapshot =
    SessionSnapshot(
        schemaVersion = schemaVersion,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = wakeSchedule.toDomain(),
        blockingSchedule = blockingSchedule.toDomain(),
        state = state,
        releaseTarget = releaseTarget,
        health = health,
        createdAtEpochMillis = createdAtEpochMillis,
        armedAtEpochMillis = armedAtEpochMillis,
        blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
        ringingAtEpochMillis = ringingAtEpochMillis,
        alarmSoundStoppedAtEpochMillis = alarmSoundStoppedAtEpochMillis,
        triggerElapsedAtEpochMillis = triggerElapsedAtEpochMillis,
        nfcVerifiedAtEpochMillis = nfcVerifiedAtEpochMillis,
        releasingAtEpochMillis = releasingAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        cancelledAtEpochMillis = cancelledAtEpochMillis,
        failureCode = failureCode,
        releasedBy = releasedBy ?: if (nfcVerifiedAtEpochMillis != null) ReleaseMeans.NFC_SCAN else null,
        disarmedAtEpochMillis = disarmedAtEpochMillis,
    )

internal fun SessionSnapshot.toDto(): SessionSnapshotDto =
    SessionSnapshotDto(
        schemaVersion = schemaVersion,
        revision = revision,
        sessionId = sessionId,
        wakeSchedule = wakeSchedule.toDto(),
        blockingSchedule = blockingSchedule.toDto(),
        state = state,
        releaseTarget = releaseTarget,
        health = health,
        createdAtEpochMillis = createdAtEpochMillis,
        armedAtEpochMillis = armedAtEpochMillis,
        blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
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
