package com.niumi.core.interop

import com.niumi.core.domain.ActivationRequest
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.core.domain.SessionIncident
import com.niumi.core.domain.WakeSchedule

// Mappers des types-valeurs de SPEC_CORE_KMP §7. Répartis sur plusieurs fichiers pour rester sous
// le seuil detekt `TooManyFunctions` (voir `SessionSnapshotEventMappers.kt`,
// `SessionEffectDecisionMappers.kt`, `PolicyDtoMappers.kt`, `ETAPE-08.md`). Chaque type ne porte
// que les directions réellement empruntées par la façade ou les tests de round-trip.

internal fun WakeScheduleDto.toDomain(): WakeSchedule =
    WakeSchedule(localDateIso, localTimeIso, zoneIdAtActivation, triggerAtEpochMillis)

internal fun WakeSchedule.toDto(): WakeScheduleDto =
    WakeScheduleDto(localDateIso, localTimeIso, zoneIdAtActivation, triggerAtEpochMillis)

internal fun AppSelectionSummaryDto.toDomain(): AppSelectionSummary = AppSelectionSummary(count)

internal fun AppSelectionSummary.toDto(): AppSelectionSummaryDto = AppSelectionSummaryDto(count)

internal fun ActivationRequestDto.toDomain(): ActivationRequest =
    ActivationRequest(wakeSchedule.toDomain(), appSelection.toDomain())

internal fun ActivationRequest.toDto(): ActivationRequestDto =
    ActivationRequestDto(wakeSchedule.toDto(), appSelection.toDto())

internal fun SessionIncidentDto.toDomain(): SessionIncident =
    SessionIncident(code, severity, occurredAtEpochMillis, platform)

internal fun SessionIncident.toDto(): SessionIncidentDto =
    SessionIncidentDto(code, severity, occurredAtEpochMillis, platform)
