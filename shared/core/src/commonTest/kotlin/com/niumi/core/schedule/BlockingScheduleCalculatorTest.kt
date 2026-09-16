package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Horodatages de référence en Europe/Paris (SPEC_CORE_KMP §8.3), repris de
// `WakeScheduleCalculatorTest` : le début du blocage suit exactement les règles du réveil.
private const val ZONE = "Europe/Paris"

// 2026-09-08T20:00:00+02:00
private const val NOW_EPOCH_MILLIS_SEPT_20H = 1_788_890_400_000L

// 2026-09-09T07:00:00+02:00 — réveil obtenu pour « 07:00 » choisi à 20:00.
private const val TRIGGER_NEXT_MORNING = 1_788_930_000_000L

// 2026-03-29T00:00:00+01:00, avant le saut de 02:00 à 03:00.
private const val NOW_EPOCH_MILLIS_SPRING_MIDNIGHT = 1_774_738_800_000L

// 2026-03-29T07:00:00+02:00, après le saut.
private const val TRIGGER_SPRING_MORNING = 1_774_760_400_000L

// 2026-10-25T00:00:00+02:00, avant le retour de 03:00 à 02:00.
private const val NOW_EPOCH_MILLIS_AUTUMN_MIDNIGHT = 1_792_879_200_000L

// 2026-10-25T07:00:00+01:00, après le retour.
private const val TRIGGER_AUTUMN_MORNING = 1_792_908_000_000L

class BlockingScheduleCalculatorTest {
    @Test
    fun nullLocalTimeDescribesAnImmediateBlocking() {
        val result = compute(localTimeIso = null)

        assertEquals(BlockingScheduleStatus.VALID, result.status)
        assertEquals(BlockingSchedule.IMMEDIATE, result.schedule)
    }

    @Test
    fun laterTimeSameDayIsChosenTheSameDay() {
        val result = compute(localTimeIso = "22:30")

        val schedule = requireNotNull(result.schedule)
        assertEquals(BlockingScheduleStatus.VALID, result.status)
        assertEquals("2026-09-08", schedule.localDateIso)
        assertEquals("22:30", schedule.localTimeIso)
        // 2026-09-08T22:30:00+02:00
        assertEquals(1_788_899_400_000L, schedule.startsAtEpochMillis)
    }

    @Test
    fun timeThatRollsOverAfterTheWakeUpIsRejected() {
        // « 08:00 » choisi à 20:00 désigne demain 08:00, après le réveil de demain 07:00.
        val result = compute(localTimeIso = "08:00")

        assertEquals(BlockingScheduleStatus.NOT_BEFORE_TRIGGER, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun eveningTimeThatRollsOverToTheNextDayIsRejected() {
        // « 19:00 » choisi à 20:00 désigne demain 19:00, bien après le réveil.
        val result = compute(localTimeIso = "19:00")

        assertEquals(BlockingScheduleStatus.NOT_BEFORE_TRIGGER, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun instantEqualToTheWakeUpIsRejected() {
        // L'antériorité exigée est stricte (SPEC_CORE_KMP §7.5).
        val result = compute(localTimeIso = "07:00")

        assertEquals(BlockingScheduleStatus.NOT_BEFORE_TRIGGER, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun nonExistentTimeInSpringGapResolvesToFirstValidInstant() {
        val result =
            compute(
                localTimeIso = "02:30",
                nowEpochMillis = NOW_EPOCH_MILLIS_SPRING_MIDNIGHT,
                triggerAtEpochMillis = TRIGGER_SPRING_MORNING,
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals("2026-03-29", schedule.localDateIso)
        assertEquals("02:30", schedule.localTimeIso)
        // Même résultat que `WakeScheduleCalculatorTest` : 2026-03-29T03:00:00+02:00.
        assertEquals(1_774_746_000_000L, schedule.startsAtEpochMillis)
    }

    @Test
    fun repeatedTimeInAutumnOverlapResolvesToFirstOccurrence() {
        val result =
            compute(
                localTimeIso = "02:30",
                nowEpochMillis = NOW_EPOCH_MILLIS_AUTUMN_MIDNIGHT,
                triggerAtEpochMillis = TRIGGER_AUTUMN_MORNING,
            )

        val schedule = requireNotNull(result.schedule)
        assertEquals("2026-10-25", schedule.localDateIso)
        // Première occurrence, encore UTC+2 : 2026-10-25T02:30:00+02:00.
        assertEquals(1_792_888_200_000L, schedule.startsAtEpochMillis)
    }

    @Test
    fun unknownZoneIsRejected() {
        val result = compute(localTimeIso = "22:30", zoneId = "Not/AZone")

        assertEquals(BlockingScheduleStatus.UNKNOWN_ZONE, result.status)
        assertNull(result.schedule)
    }

    @Test
    fun invalidLocalTimeIsRejected() {
        val result = compute(localTimeIso = "25:00")

        assertEquals(BlockingScheduleStatus.INVALID_TIME, result.status)
        assertNull(result.schedule)
    }

    private fun compute(
        localTimeIso: String?,
        zoneId: String = ZONE,
        nowEpochMillis: Long = NOW_EPOCH_MILLIS_SEPT_20H,
        triggerAtEpochMillis: Long = TRIGGER_NEXT_MORNING,
    ) = BlockingScheduleCalculator.compute(
        BlockingScheduleInput(
            localTimeIso = localTimeIso,
            zoneId = zoneId,
            nowEpochMillis = nowEpochMillis,
            triggerAtEpochMillis = triggerAtEpochMillis,
        ),
    )
}
