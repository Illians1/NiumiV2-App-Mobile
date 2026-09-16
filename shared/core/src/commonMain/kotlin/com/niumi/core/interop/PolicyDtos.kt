package com.niumi.core.interop

import com.niumi.core.diagnostics.ReadinessSeverity
import com.niumi.core.schedule.BlockingScheduleStatus
import com.niumi.core.schedule.TriggerDelayOutcome
import com.niumi.core.schedule.WakeScheduleStatus

/**
 * DTO horaires et de politique (SPEC_CORE_KMP §14, §8, §7.4). Séparés des miroirs de §5 à §7 de
 * `SessionDtos.kt` pour garder les deux fichiers lisibles : fichier non prévu par le plan initial,
 * voir `ETAPE-08.md`.
 */
public data class WakeScheduleInputDto(
    val localTimeIso: String,
    val zoneId: String,
    val nowEpochMillis: Long,
)

public typealias WakeScheduleStatusDto = WakeScheduleStatus

public data class WakeScheduleResultDto(
    val status: WakeScheduleStatusDto,
    val schedule: WakeScheduleDto?,
)

/**
 * Entrée de `evaluateTriggerDelay`, sixième méthode de la façade ajoutée à l'étape 8 en extension
 * de SPEC_CORE_KMP §14 (§8.2 : la fenêtre de grâce Android de 15 minutes n'a pas d'équivalent iOS).
 */
public data class TriggerDelayInputDto(
    val triggerAtEpochMillis: Long,
    val nowEpochMillis: Long,
)

public typealias TriggerDelayOutcomeDto = TriggerDelayOutcome

public data class TriggerDelayResultDto(
    val outcome: TriggerDelayOutcomeDto,
)

/**
 * Entrée de `computeBlockingSchedule`, septième méthode de la façade (contrat 1.3, SPEC_CORE_KMP
 * §8.3, §14). [localTimeIso] nul décrit un blocage immédiat ; [zoneId] et [nowEpochMillis] sont
 * ceux du calcul du réveil, dont [triggerAtEpochMillis] est le résultat.
 */
public data class BlockingScheduleInputDto(
    val localTimeIso: String?,
    val zoneId: String,
    val nowEpochMillis: Long,
    val triggerAtEpochMillis: Long,
)

public typealias BlockingScheduleStatusDto = BlockingScheduleStatus

public data class BlockingScheduleResultDto(
    val status: BlockingScheduleStatusDto,
    val schedule: BlockingScheduleDto?,
)

public typealias ReadinessSeverityDto = ReadinessSeverity

public data class ReadinessCheckInputDto(
    val id: String,
    val severity: ReadinessSeverityDto,
    val passed: Boolean,
)

/**
 * [blockingStartsAtEpochMillis] : `null` pour un blocage immédiat (contrat 1.3). La valeur par
 * défaut est transitoire, comme celles de [BlockingScheduleDto] — le site de construction Android
 * la renseignera explicitement à l'étape 23 du plan Android.
 */
public data class ActivationPolicyInputDto(
    val checks: List<ReadinessCheckInputDto>,
    val appSelectionCount: Int,
    val triggerAtEpochMillis: Long,
    val nowEpochMillis: Long,
    val hasPairedBox: Boolean,
    val blockingStartsAtEpochMillis: Long? = null,
)

public data class ActivationReasonDto(
    val code: String,
    val checkId: String?,
)

public data class ActivationPolicyResultDto(
    val allowed: Boolean,
    val blockingReasons: List<ActivationReasonDto>,
    val warnings: List<ActivationReasonDto>,
)
