package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule

/** Issue du calcul du début du blocage (SPEC_CORE_KMP §8.3). */
public enum class BlockingScheduleStatus {
    VALID,
    INVALID_TIME,
    UNKNOWN_ZONE,
    NOT_BEFORE_TRIGGER,
}

/**
 * Résultat typé, jamais une exception (SPEC_CORE_KMP §14) : [schedule] est non nul seulement si
 * [status] vaut `VALID`. Même convention que [WakeScheduleResult].
 */
public data class BlockingScheduleResult(
    val status: BlockingScheduleStatus,
    val schedule: BlockingSchedule?,
)
