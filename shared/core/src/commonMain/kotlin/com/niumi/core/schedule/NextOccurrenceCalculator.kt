package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule
import com.niumi.core.domain.WakeSchedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Une semaine suffit : le jour coché le plus lointain est au plus le même jour la semaine suivante,
// quand l'heure du réveil est déjà passée aujourd'hui.
private const val DAYS_SEARCHED = 7
private val ISO_DAYS = 1..7

/**
 * Calcule la prochaine occurrence d'un planning récurrent (SPEC_CORE_KMP §8.4) : le premier jour
 * coché, à partir d'aujourd'hui, dont l'instant de réveil est strictement futur ; le coucher la
 * veille si son heure est plus tardive que celle du réveil, sinon le jour même (`SPECIFICATION.md`
 * §2). Les heures locales sont résolues par [WakeScheduleCalculator], dont les règles de changement
 * d'heure (§8.1, points 3 et 4) s'appliquent donc au réveil comme au coucher.
 */
public object NextOccurrenceCalculator {
    // Quatre refus (zone, heures, période vide, aucun jour) en clauses de garde, même motif que
    // `WakeScheduleCalculator.compute`.
    @Suppress("ReturnCount")
    public fun compute(input: NextOccurrenceInput): NextOccurrenceResult {
        val plan = input.plan
        val zone = WakeScheduleCalculator.resolveZone(input.zoneId) ?: return refused(NextOccurrenceStatus.UNKNOWN_ZONE)
        val wakeTime = WakeScheduleCalculator.resolveLocalTime(plan.wakeTimeIso)
        val bedtime = WakeScheduleCalculator.resolveLocalTime(plan.bedtimeIso)
        if (wakeTime == null || bedtime == null) return refused(NextOccurrenceStatus.INVALID_TIME)
        if (wakeTime == bedtime) return refused(NextOccurrenceStatus.BEDTIME_EQUALS_WAKE)
        // Une valeur hors de la semaine ISO ne désigne aucun jour : ignorée plutôt que refusée.
        val days = plan.enabledIsoDays.filter { it in ISO_DAYS }.toSet()
        if (days.isEmpty()) return refused(NextOccurrenceStatus.NO_DAY_ENABLED)

        val now = Instant.fromEpochMilliseconds(input.nowEpochMillis)
        val today = now.toLocalDateTime(zone).date
        // Toujours trouvé : chaque jour coché revient entre demain et dans sept jours, à un instant
        // donc strictement futur — `first` ne peut pas lever d'exception (§14).
        val (wakeDate, wakeAt) =
            (0..DAYS_SEARCHED)
                .asSequence()
                .map { today.plus(it, DateTimeUnit.DAY) }
                .filter { it.dayOfWeek.isoDayNumber in days }
                .map { it to WakeScheduleCalculator.resolveInstant(LocalDateTime(it, wakeTime), zone) }
                .first { (_, instant) -> instant > now }

        val bedtimeDate = if (bedtime > wakeTime) wakeDate.minus(1, DateTimeUnit.DAY) else wakeDate
        val bedtimeAt = WakeScheduleCalculator.resolveInstant(LocalDateTime(bedtimeDate, bedtime), zone)
        // Q8, décision du 2026-10-24 : le trou de printemps peut confondre deux heures distinctes.
        if (bedtimeAt >= wakeAt) return refused(NextOccurrenceStatus.BEDTIME_EQUALS_WAKE)

        val startsImmediately = bedtimeAt <= now
        val occurrence =
            NextOccurrence(
                wakeSchedule = wakeSchedule(wakeDate, wakeTime, input.zoneId, wakeAt),
                blockingSchedule =
                    if (startsImmediately) {
                        BlockingSchedule.IMMEDIATE
                    } else {
                        BlockingSchedule(bedtimeDate.toString(), bedtime.toString(), bedtimeAt.toEpochMilliseconds())
                    },
                blockingStartsImmediately = startsImmediately,
            )
        return NextOccurrenceResult(NextOccurrenceStatus.VALID, occurrence)
    }

    private fun wakeSchedule(
        date: LocalDate,
        time: LocalTime,
        zoneId: String,
        instant: Instant,
    ) = WakeSchedule(date.toString(), time.toString(), zoneId, instant.toEpochMilliseconds())

    private fun refused(status: NextOccurrenceStatus) = NextOccurrenceResult(status, null)
}
