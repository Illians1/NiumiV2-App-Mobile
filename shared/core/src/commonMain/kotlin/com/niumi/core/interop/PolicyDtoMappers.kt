package com.niumi.core.interop

import com.niumi.core.diagnostics.ActivationPolicyInput
import com.niumi.core.diagnostics.ActivationPolicyResult
import com.niumi.core.diagnostics.ActivationReason
import com.niumi.core.diagnostics.ReadinessCheckInput
import com.niumi.core.schedule.WakeScheduleInput
import com.niumi.core.schedule.WakeScheduleResult

// Mappers de la politique horaire et de la politique d'activation (SPEC_CORE_KMP §8, §7.4).
// Mêmes directions à sens unique : l'entrée circule natif → commun, la sortie commun → natif.

internal fun WakeScheduleInputDto.toDomain(): WakeScheduleInput =
    WakeScheduleInput(localTimeIso, zoneId, nowEpochMillis)

internal fun WakeScheduleResult.toDto(): WakeScheduleResultDto = WakeScheduleResultDto(status, schedule?.toDto())

internal fun ReadinessCheckInputDto.toDomain(): ReadinessCheckInput = ReadinessCheckInput(id, severity, passed)

internal fun ActivationPolicyInputDto.toDomain(): ActivationPolicyInput =
    ActivationPolicyInput(
        checks = checks.map { it.toDomain() },
        appSelectionCount = appSelectionCount,
        triggerAtEpochMillis = triggerAtEpochMillis,
        nowEpochMillis = nowEpochMillis,
        hasPairedBox = hasPairedBox,
    )

internal fun ActivationReason.toDto(): ActivationReasonDto = ActivationReasonDto(code, checkId)

internal fun ActivationPolicyResult.toDto(): ActivationPolicyResultDto =
    ActivationPolicyResultDto(
        allowed = allowed,
        blockingReasons = blockingReasons.map { it.toDto() },
        warnings = warnings.map { it.toDto() },
    )
