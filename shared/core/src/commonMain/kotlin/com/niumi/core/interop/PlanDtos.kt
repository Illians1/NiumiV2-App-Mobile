package com.niumi.core.interop

import com.niumi.core.schedule.NextOccurrenceStatus

/**
 * DTO du planning récurrent (contrat 1.5, SPEC_CORE_KMP §8.4, §14). [WakePlanDto.enabledIsoDays]
 * porte les jours de réveil en numérotation ISO (1 = lundi … 7 = dimanche). Non sérialisables,
 * comme les DTO de politique de `PolicyDtos.kt` : la façade calcule, la plateforme stocke son
 * planning dans son propre format.
 */
public data class WakePlanDto(
    val enabledIsoDays: Set<Int>,
    val wakeTimeIso: String,
    val bedtimeIso: String,
)

public data class NextOccurrenceInputDto(
    val plan: WakePlanDto,
    val zoneId: String,
    val nowEpochMillis: Long,
)

public typealias NextOccurrenceStatusDto = NextOccurrenceStatus

public data class NextOccurrenceDto(
    val wakeSchedule: WakeScheduleDto,
    val blockingSchedule: BlockingScheduleDto,
    val blockingStartsImmediately: Boolean,
)

public data class NextOccurrenceResultDto(
    val status: NextOccurrenceStatusDto,
    val occurrence: NextOccurrenceDto?,
)
