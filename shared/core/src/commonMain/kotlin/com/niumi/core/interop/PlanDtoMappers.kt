package com.niumi.core.interop

import com.niumi.core.schedule.NextOccurrence
import com.niumi.core.schedule.NextOccurrenceInput
import com.niumi.core.schedule.NextOccurrenceResult
import com.niumi.core.schedule.WakePlan

// Mappers du planning récurrent (contrat 1.5, SPEC_CORE_KMP §8.4). Séparés des autres mappers pour
// rester sous le seuil detekt `TooManyFunctions`, comme `PolicyDtoMappers.kt`.

internal fun WakePlanDto.toDomain(): WakePlan = WakePlan(enabledIsoDays, wakeTimeIso, bedtimeIso)

internal fun NextOccurrenceInputDto.toDomain(): NextOccurrenceInput =
    NextOccurrenceInput(plan.toDomain(), zoneId, nowEpochMillis)

internal fun NextOccurrence.toDto(): NextOccurrenceDto =
    NextOccurrenceDto(wakeSchedule.toDto(), blockingSchedule.toDto(), blockingStartsImmediately)

internal fun NextOccurrenceResult.toDto(): NextOccurrenceResultDto =
    NextOccurrenceResultDto(status, occurrence?.toDto())
