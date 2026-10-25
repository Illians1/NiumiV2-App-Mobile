package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Horodatages de référence en Europe/Paris (SPEC_CORE_KMP §8.4), en millisecondes Unix, calculés
// depuis des instants locaux connus pour ne dépendre d'aucune horloge réelle. Septembre 2026 est
// loin de tout changement d'heure ; le 2026-09-10 est un jeudi.
private const val ZONE = "Europe/Paris"
private val WEEKDAYS = setOf(1, 2, 3, 4, 5)

private const val THU_07_00 = 1_789_016_400_000L
private const val THU_08_00 = 1_789_020_000_000L
private const val THU_20_00 = 1_789_063_200_000L
private const val THU_22_30 = 1_789_072_200_000L
private const val THU_23_00 = 1_789_074_000_000L
private const val FRI_01_00 = 1_789_081_200_000L
private const val FRI_07_00 = 1_789_102_800_000L
private const val FRI_23_00 = 1_789_160_400_000L
private const val SUN_22_30 = 1_789_331_400_000L
private const val MON_07_00 = 1_789_362_000_000L
private const val NEXT_THU_07_00 = 1_789_621_200_000L
private const val HOUR = 3_600_000L
private const val DAY = 86_400_000L

// 2026-03-28 (samedi) 20:00 et 22:30 +01:00 ; le lendemain, l'horloge saute de 02:00 à 03:00.
private const val SPRING_SAT_20_00 = 1_774_724_400_000L
private const val SPRING_SAT_22_30 = 1_774_733_400_000L

// 2026-03-29T03:00:00+02:00, premier instant valide après le saut.
private const val SPRING_SUN_03_00 = 1_774_746_000_000L

// 2026-10-24 (samedi) 20:00 +02:00 ; le lendemain, 02:00–03:00 existe deux fois.
private const val AUTUMN_SAT_20_00 = 1_792_864_800_000L
private const val AUTUMN_SUN_01_00 = 1_792_882_800_000L

// 2026-10-25T02:30:00+02:00, première des deux occurrences.
private const val AUTUMN_SUN_02_30_FIRST = 1_792_888_200_000L

private fun compute(
    nowEpochMillis: Long,
    days: Set<Int> = WEEKDAYS,
    wake: String = "07:00",
    bedtime: String = "22:30",
    zoneId: String = ZONE,
) = NextOccurrenceCalculator.compute(NextOccurrenceInput(WakePlan(days, wake, bedtime), zoneId, nowEpochMillis))

class NextOccurrenceCalculatorTest {
    @Test
    fun thursdayEveningGivesFridayMorningWithBedtimeOnThursday() {
        val result = compute(nowEpochMillis = THU_20_00)

        assertEquals(NextOccurrenceStatus.VALID, result.status)
        val occurrence = requireNotNull(result.occurrence)
        assertEquals("2026-09-11", occurrence.wakeSchedule.localDateIso)
        assertEquals("07:00", occurrence.wakeSchedule.localTimeIso)
        assertEquals(ZONE, occurrence.wakeSchedule.zoneIdAtActivation)
        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-09-10", "22:30", THU_22_30), occurrence.blockingSchedule)
        assertFalse(occurrence.blockingStartsImmediately)
    }

    @Test
    fun fridayNightSkipsTheWeekendToMondayWithBedtimeOnSunday() {
        val occurrence = requireNotNull(compute(nowEpochMillis = FRI_23_00).occurrence)

        assertEquals("2026-09-14", occurrence.wakeSchedule.localDateIso)
        assertEquals(MON_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-09-13", "22:30", SUN_22_30), occurrence.blockingSchedule)
        assertFalse(occurrence.blockingStartsImmediately)
    }

    @Test
    fun bedtimeAfterMidnightFallsOnTheWakeDay() {
        // Cas limite 1 du plan : coucher 1:00, réveil 7:00 → le coucher a lieu le jour même du réveil.
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_20_00, bedtime = "01:00").occurrence)

        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-09-11", "01:00", FRI_01_00), occurrence.blockingSchedule)
    }

    @Test
    fun aPeriodAlreadyInProgressBlocksImmediately() {
        // SPECIFICATION.md §2 : à 23:00, la période 22:30–7:00 est en cours, le blocage commence aussitôt.
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_23_00).occurrence)

        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertTrue(occurrence.blockingStartsImmediately)
        assertEquals(BlockingSchedule.IMMEDIATE, occurrence.blockingSchedule)
    }

    @Test
    fun bedtimeExactlyNowBlocksImmediately() {
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_22_30).occurrence)

        assertTrue(occurrence.blockingStartsImmediately)
        assertEquals(BlockingSchedule.IMMEDIATE, occurrence.blockingSchedule)
    }

    @Test
    fun aWakeTimeAlreadyPassedOnACheckedDayMovesToTheNextCheckedDay() {
        // Cas limite 2 du plan : jeudi coché, il est jeudi 8:00, réveil 7:00 → jamais aujourd'hui.
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_08_00).occurrence)

        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
    }

    @Test
    fun aSingleCheckedDayAlreadyPassedMovesToTheSameDayNextWeek() {
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_08_00, days = setOf(4)).occurrence)

        assertEquals("2026-09-17", occurrence.wakeSchedule.localDateIso)
        assertEquals(NEXT_THU_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
    }

    @Test
    fun aWakeTimeExactlyNowIsNotStrictlyFuture() {
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_07_00).occurrence)

        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
    }

    @Test
    fun noCheckedDayIsAOneOffAlarmTodayWhenTheWakeTimeIsStillAhead() {
        // « Ponctuel » (décision de Mehdi du 2026-10-25) : aucun jour coché, le réveil sonne à la
        // prochaine heure de réveil, quel que soit le jour.
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_07_00 - HOUR, days = emptySet()).occurrence)

        assertEquals("2026-09-10", occurrence.wakeSchedule.localDateIso)
        assertEquals(THU_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertTrue(occurrence.blockingStartsImmediately)
    }

    @Test
    fun noCheckedDayIsAOneOffAlarmTomorrowOnceTheWakeTimeHasPassed() {
        val occurrence = requireNotNull(compute(nowEpochMillis = THU_20_00, days = emptySet()).occurrence)

        assertEquals(FRI_07_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-09-10", "22:30", THU_22_30), occurrence.blockingSchedule)
    }

    @Test
    fun aOneOffAlarmIgnoresTheWeekend() {
        val occurrence = requireNotNull(compute(nowEpochMillis = FRI_23_00, days = emptySet()).occurrence)

        assertEquals("2026-09-12", occurrence.wakeSchedule.localDateIso)
        assertEquals(FRI_07_00 + DAY, occurrence.wakeSchedule.triggerAtEpochMillis)
    }

    @Test
    fun daysOutsideTheIsoWeekAreIgnored() {
        // {0, 8} ne désigne aucun jour : c'est un réveil ponctuel.
        val oneOff = requireNotNull(compute(nowEpochMillis = THU_20_00, days = setOf(0, 8)).occurrence)
        assertEquals(FRI_07_00, oneOff.wakeSchedule.triggerAtEpochMillis)

        val occurrence = requireNotNull(compute(nowEpochMillis = FRI_23_00, days = setOf(0, 5, 8)).occurrence)
        assertEquals("2026-09-18", occurrence.wakeSchedule.localDateIso)
    }

    @Test
    fun bedtimeEqualToWakeIsATwentyFourHourPeriodStartingTheDayBefore() {
        // Décision de Mehdi du 2026-10-25 (Q8 révisé) : deux heures identiques bloquent 24 h.
        val occurrence =
            requireNotNull(
                compute(nowEpochMillis = THU_20_00, days = setOf(5), bedtime = "22:30", wake = "22:30").occurrence,
            )

        assertEquals("2026-09-11", occurrence.wakeSchedule.localDateIso)
        assertEquals(THU_22_30 + DAY, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-09-10", "22:30", THU_22_30), occurrence.blockingSchedule)
        assertFalse(occurrence.blockingStartsImmediately)
    }

    @Test
    fun aOneOffTwentyFourHourPeriodIsAlwaysInProgress() {
        val occurrence =
            requireNotNull(
                compute(nowEpochMillis = THU_20_00, days = emptySet(), bedtime = "22:30", wake = "22:30").occurrence,
            )

        assertEquals(THU_22_30, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertTrue(occurrence.blockingStartsImmediately)
        assertEquals(BlockingSchedule.IMMEDIATE, occurrence.blockingSchedule)
    }

    @Test
    fun nonExistentWakeTimeInSpringGapResolvesToFirstValidInstant() {
        val occurrence =
            requireNotNull(compute(nowEpochMillis = SPRING_SAT_20_00, days = setOf(7), wake = "02:30").occurrence)

        assertEquals("2026-03-29", occurrence.wakeSchedule.localDateIso)
        assertEquals("02:30", occurrence.wakeSchedule.localTimeIso)
        assertEquals(SPRING_SUN_03_00, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-03-28", "22:30", SPRING_SAT_22_30), occurrence.blockingSchedule)
    }

    @Test
    fun bedtimeAndWakeMergedBySpringGapAreReportedAsAnEmptyPeriod() {
        // Q8, décision du 2026-10-24 : 2:15 et 2:45 n'existent pas le 2026-03-29, les deux
        // deviennent 3:00 ; un planning vide serait refusé ensuite par INVALID_BLOCKING_SCHEDULE.
        val result = compute(nowEpochMillis = SPRING_SAT_20_00, days = setOf(7), wake = "02:45", bedtime = "02:15")

        assertEquals(NextOccurrenceStatus.EMPTY_PERIOD, result.status)
        assertNull(result.occurrence)
    }

    @Test
    fun repeatedWakeTimeInAutumnOverlapResolvesToFirstOccurrence() {
        val occurrence =
            requireNotNull(
                compute(
                    nowEpochMillis = AUTUMN_SAT_20_00,
                    days = setOf(7),
                    wake = "02:30",
                    bedtime = "01:00",
                ).occurrence,
            )

        assertEquals(AUTUMN_SUN_02_30_FIRST, occurrence.wakeSchedule.triggerAtEpochMillis)
        assertEquals(BlockingSchedule("2026-10-25", "01:00", AUTUMN_SUN_01_00), occurrence.blockingSchedule)
    }

    @Test
    fun invalidTimesAreReported() {
        assertEquals(NextOccurrenceStatus.INVALID_TIME, compute(nowEpochMillis = THU_20_00, wake = "25:00").status)
        assertEquals(NextOccurrenceStatus.INVALID_TIME, compute(nowEpochMillis = THU_20_00, bedtime = "abc").status)
    }

    @Test
    fun unknownZoneIsReported() {
        val result = compute(nowEpochMillis = THU_20_00, zoneId = "Mars/Olympus")

        assertEquals(NextOccurrenceStatus.UNKNOWN_ZONE, result.status)
        assertNull(result.occurrence)
    }
}
