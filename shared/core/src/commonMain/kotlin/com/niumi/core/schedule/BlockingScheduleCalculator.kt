package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule

/**
 * Calcule l'instant de début d'un blocage différé (SPEC_CORE_KMP §8.3). Les règles de date et
 * d'heure sont exactement celles du réveil — même fuseau, même `nowEpochMillis`, jour civil suivant
 * si l'instant obtenu n'est pas strictement futur, mêmes règles de changement d'heure : le calcul
 * est donc délégué à [WakeScheduleCalculator] plutôt que réécrit. Seule l'antériorité stricte au
 * réveil s'y ajoute.
 */
public object BlockingScheduleCalculator {
    // Trois issues en clauses de garde (blocage immédiat, échec du calcul horaire, antériorité),
    // même motif que `WakeScheduleCalculator.compute` dont il dépend.
    @Suppress("ReturnCount")
    public fun compute(input: BlockingScheduleInput): BlockingScheduleResult {
        val localTimeIso =
            input.localTimeIso
                ?: return BlockingScheduleResult(BlockingScheduleStatus.VALID, BlockingSchedule.IMMEDIATE)

        val wake = WakeScheduleCalculator.compute(WakeScheduleInput(localTimeIso, input.zoneId, input.nowEpochMillis))
        val schedule = wake.schedule ?: return BlockingScheduleResult(statusOf(wake.status), null)

        return if (schedule.triggerAtEpochMillis < input.triggerAtEpochMillis) {
            BlockingScheduleResult(
                BlockingScheduleStatus.VALID,
                BlockingSchedule(schedule.localDateIso, schedule.localTimeIso, schedule.triggerAtEpochMillis),
            )
        } else {
            BlockingScheduleResult(BlockingScheduleStatus.NOT_BEFORE_TRIGGER, null)
        }
    }

    private fun statusOf(status: WakeScheduleStatus): BlockingScheduleStatus =
        when (status) {
            WakeScheduleStatus.INVALID_TIME -> {
                BlockingScheduleStatus.INVALID_TIME
            }

            WakeScheduleStatus.UNKNOWN_ZONE -> {
                BlockingScheduleStatus.UNKNOWN_ZONE
            }

            WakeScheduleStatus.VALID -> {
                error("un résultat VALID porte toujours un schedule : branche filtrée par l'appelant")
            }
        }
}
