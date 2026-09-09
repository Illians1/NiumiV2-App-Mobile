package com.niumi.core.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Horodatages de référence en Europe/Paris (SPEC_CORE_KMP §8.1), tous en millisecondes Unix,
// calculés depuis des instants ISO connus pour ne dépendre d'aucune horloge réelle.
private const val ZONE = "Europe/Paris"

// 2026-09-08T20:00:00+02:00
private const val NOW_EPOCH_MILLIS_SEPT_20H = 1_788_890_400_000L

// 2026-03-29T00:00:00+01:00 (avant le passage à l'heure d'été de 02:00 à 03:00 ce jour-là)
private const val NOW_EPOCH_MILLIS_SPRING_MIDNIGHT = 1_774_738_800_000L

// 2026-10-25T00:00:00+02:00 (avant le passage à l'heure d'hiver de 03:00 à 02:00 ce jour-là)
private const val NOW_EPOCH_MILLIS_AUTUMN_MIDNIGHT = 1_792_879_200_000L

class WakeScheduleCalculatorTest {
    @Test
    fun laterTimeSameDayIsChosenTheSameDay() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(localTimeIso = "22:00", zoneId = ZONE, nowEpochMillis = NOW_EPOCH_MILLIS_SEPT_20H),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals(WakeScheduleStatus.VALID, result.status)
        assertEquals("2026-09-08", schedule.localDateIso)
    }

    @Test
    fun earlierTimeThanNowRollsOverToNextDay() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(localTimeIso = "07:00", zoneId = ZONE, nowEpochMillis = NOW_EPOCH_MILLIS_SEPT_20H),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals(WakeScheduleStatus.VALID, result.status)
        assertEquals("2026-09-09", schedule.localDateIso)
    }

    @Test
    fun timeExactlyEqualToNowRollsOverToNextDay() {
        // 2026-09-08T07:00:00+02:00
        val nowAtSevenAm = 1_788_843_600_000L

        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(localTimeIso = "07:00", zoneId = ZONE, nowEpochMillis = nowAtSevenAm),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals("2026-09-09", schedule.localDateIso)
        assertEquals(nowAtSevenAm + DAY_MILLIS, schedule.triggerAtEpochMillis)
    }

    @Test
    fun nonExistentTimeInSpringGapResolvesToFirstValidInstant() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(
                    localTimeIso = "02:30",
                    zoneId = ZONE,
                    nowEpochMillis = NOW_EPOCH_MILLIS_SPRING_MIDNIGHT,
                ),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals(WakeScheduleStatus.VALID, result.status)
        assertEquals("2026-03-29", schedule.localDateIso)
        assertEquals("02:30", schedule.localTimeIso)
        // Premier instant valide après le saut de 02:00 à 03:00 (SPEC_CORE_KMP §8.1, point 3),
        // soit 2026-03-29T03:00:00+02:00 — jamais 03:30, ce que rendrait un simple
        // `toInstant(zone)` non corrigé. Voir ETAPE-08.md.
        assertEquals(1_774_746_000_000L, schedule.triggerAtEpochMillis)
    }

    @Test
    fun repeatedTimeInAutumnOverlapResolvesToFirstOccurrence() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(
                    localTimeIso = "02:30",
                    zoneId = ZONE,
                    nowEpochMillis = NOW_EPOCH_MILLIS_AUTUMN_MIDNIGHT,
                ),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals(WakeScheduleStatus.VALID, result.status)
        assertEquals("2026-10-25", schedule.localDateIso)
        // Première occurrence, avant le passage à l'heure d'hiver : encore UTC+2
        // (SPEC_CORE_KMP §8.1, point 4). 2026-10-25T02:30:00+02:00.
        assertEquals(1_792_888_200_000L, schedule.triggerAtEpochMillis)
    }

    @Test
    fun unknownZoneIsRejected() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(
                    localTimeIso = "07:00",
                    zoneId = "Not/AZone",
                    nowEpochMillis = NOW_EPOCH_MILLIS_SEPT_20H,
                ),
            )

        assertEquals(WakeScheduleStatus.UNKNOWN_ZONE, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun invalidLocalTimeIsRejected() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(localTimeIso = "25:00", zoneId = ZONE, nowEpochMillis = NOW_EPOCH_MILLIS_SEPT_20H),
            )

        assertEquals(WakeScheduleStatus.INVALID_TIME, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun validResultPreservesLocalIntentAndZone() {
        val result =
            WakeScheduleCalculator.compute(
                WakeScheduleInput(localTimeIso = "22:00", zoneId = ZONE, nowEpochMillis = NOW_EPOCH_MILLIS_SEPT_20H),
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals("2026-09-08", schedule.localDateIso)
        assertEquals("22:00", schedule.localTimeIso)
        assertEquals(ZONE, schedule.zoneIdAtActivation)
    }

    private companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1_000L
    }
}
