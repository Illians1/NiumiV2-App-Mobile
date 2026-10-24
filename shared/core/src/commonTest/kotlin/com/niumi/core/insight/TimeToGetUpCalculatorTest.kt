package com.niumi.core.insight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `SPECIFICATION.md` §15 (écran 11) : temps mis à se lever depuis la sonnerie, et minutes gagnées
 * sur la durée déclarée à l'écran 3 bis, seulement si l'utilisateur s'est levé plus vite. Q10
 * (décision du 2026-10-24) : minute inférieure ; D3 : « 1 heure et + » compte pour 60.
 */
class TimeToGetUpCalculatorTest {
    private val alarmAt = 1_800_000_000_000L // 7:00:00 dans l'exemple de la spec
    private val minute = 60_000L

    private fun compute(
        releasedAfterMillis: Long,
        declaredMinutes: Int,
    ) = TimeToGetUpCalculator.compute(
        TimeToGetUpInput(
            alarmAtEpochMillis = alarmAt,
            releasedAtEpochMillis = alarmAt + releasedAfterMillis,
            declaredMinutes = declaredMinutes,
        ),
    )

    @Test
    fun theSpecificationExampleSavesElevenMinutes() {
        // Sonnerie à 7:00, scan à 7:04, 15 minutes déclarées.
        assertEquals(TimeToGetUpResult(elapsedMinutes = 4, savedMinutes = 11), compute(4 * minute, 15))
    }

    @Test
    fun aSlowerWakeSavesNothing() {
        assertEquals(TimeToGetUpResult(elapsedMinutes = 4, savedMinutes = null), compute(4 * minute, 3))
    }

    @Test
    fun anEqualDurationSavesNothing() {
        assertNull(compute(15 * minute, 15).savedMinutes)
    }

    @Test
    fun elapsedTimeIsRoundedDownToTheMinute() {
        // 7:04:59 reste « Debout à 7:04. 4 minutes après la sonnerie. »
        assertEquals(TimeToGetUpResult(elapsedMinutes = 4, savedMinutes = 11), compute(5 * minute - 1, 15))
    }

    @Test
    fun underAMinuteCountsAsZero() {
        assertEquals(TimeToGetUpResult(elapsedMinutes = 0, savedMinutes = 15), compute(59_000L, 15))
    }

    @Test
    fun aReleaseBeforeTheAlarmCountsAsZero() {
        assertEquals(0, compute(-minute, 15).elapsedMinutes)
    }

    @Test
    fun oneHourAndMoreCountsAsSixtyMinutes() {
        assertEquals(TimeToGetUpResult(elapsedMinutes = 12, savedMinutes = 48), compute(12 * minute, 60))
    }
}
