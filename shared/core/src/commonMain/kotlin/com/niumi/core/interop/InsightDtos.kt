package com.niumi.core.interop

import com.niumi.core.insight.TimeToGetUpInput
import com.niumi.core.insight.TimeToGetUpResult

/** DTO du temps gagné (contrat 1.5, `SPECIFICATION.md` §15, SPEC_CORE_KMP §14). */
public data class TimeToGetUpInputDto(
    val alarmAtEpochMillis: Long,
    val releasedAtEpochMillis: Long,
    val declaredMinutes: Int,
)

public data class TimeToGetUpResultDto(
    val elapsedMinutes: Int,
    val savedMinutes: Int?,
)

internal fun TimeToGetUpInputDto.toDomain(): TimeToGetUpInput =
    TimeToGetUpInput(alarmAtEpochMillis, releasedAtEpochMillis, declaredMinutes)

internal fun TimeToGetUpResult.toDto(): TimeToGetUpResultDto = TimeToGetUpResultDto(elapsedMinutes, savedMinutes)
