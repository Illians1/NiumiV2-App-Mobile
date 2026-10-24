package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule
import com.niumi.core.domain.WakeSchedule

/** Issue du calcul de la prochaine occurrence (SPEC_CORE_KMP §8.4). */
public enum class NextOccurrenceStatus {
    VALID,
    NO_DAY_ENABLED,
    INVALID_TIME,
    UNKNOWN_ZONE,
    BEDTIME_EQUALS_WAKE,
}

/**
 * Prochaine session à armer : [wakeSchedule] et [blockingSchedule] sont prêts pour un
 * `ActivationRequest`. [blockingStartsImmediately] vaut vrai quand le coucher est déjà atteint : la
 * période est en cours et [blockingSchedule] vaut alors [BlockingSchedule.IMMEDIATE].
 */
public data class NextOccurrence(
    val wakeSchedule: WakeSchedule,
    val blockingSchedule: BlockingSchedule,
    val blockingStartsImmediately: Boolean,
)

/**
 * Résultat typé, jamais une exception (SPEC_CORE_KMP §14) : [occurrence] est non nul seulement si
 * [status] vaut `VALID`. Même convention que [WakeScheduleResult].
 */
public data class NextOccurrenceResult(
    val status: NextOccurrenceStatus,
    val occurrence: NextOccurrence?,
)
