package com.niumi.feature.session.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.WakeScheduleDto
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Mise en forme du début d'un blocage différé (SPEC_ANDROID §15, écrans 5, 6 et 7 du Lot 6 ;
 * SPEC_CORE_KMP §8.3). Le formateur ne porte aucune règle propre : il délègue à
 * [WakeScheduleFormatter], seule mise en forme des instants de ce module — d'où les comparaisons
 * avec un [WakeScheduleDto] équivalent plutôt qu'avec des chaînes recopiées.
 */
class BlockingScheduleFormatterTest {
    private fun paris(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    @Test
    fun anImmediateBlockingHasNothingToDisplay() {
        val display =
            BlockingScheduleFormatter.format(
                schedule = BlockingScheduleDto(),
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = paris(2026, 9, 3, 20, 0),
            )

        assertThat(display).isNull()
    }

    @Test
    fun aDeferredBlockingIsFormattedExactlyLikeAnEquivalentWakeSchedule() {
        val now = paris(2026, 9, 3, 20, 0)
        val startsAt = paris(2026, 9, 3, 22, 30)
        val blocking =
            BlockingScheduleDto(
                localDateIso = "2026-09-03",
                localTimeIso = "22:30",
                startsAtEpochMillis = startsAt,
            )
        val equivalent =
            WakeScheduleDto(
                localDateIso = "2026-09-03",
                localTimeIso = "22:30",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = startsAt,
            )

        val display =
            BlockingScheduleFormatter.format(
                schedule = blocking,
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = now,
            )

        assertThat(display).isEqualTo(WakeScheduleFormatter.format(equivalent, nowEpochMillis = now))
        assertThat(display?.relativeDayLabel).isEqualTo("Aujourd'hui")
        assertThat(display?.timeLabel).isEqualTo("22:30")
    }

    @Test
    fun aDifferentDisplayZoneIsHonouredWithoutMovingTheInstant() {
        val now = paris(2026, 9, 3, 20, 0)
        val blocking =
            BlockingScheduleDto(
                localDateIso = "2026-09-03",
                localTimeIso = "22:30",
                startsAtEpochMillis = paris(2026, 9, 3, 22, 30),
            )

        val display =
            BlockingScheduleFormatter.format(
                schedule = blocking,
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = now,
                displayZoneId = "Pacific/Auckland",
            )

        // Europe/Paris 22:30 == Pacific/Auckland 08:30 le lendemain (UTC+12 en septembre).
        assertThat(display?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(display?.timeLabel).isEqualTo("08:30")
    }

    @Test
    fun twelveHourFormatIsPassedThrough() {
        val blocking =
            BlockingScheduleDto(
                localDateIso = "2026-09-03",
                localTimeIso = "22:30",
                startsAtEpochMillis = paris(2026, 9, 3, 22, 30),
            )

        val display =
            BlockingScheduleFormatter.format(
                schedule = blocking,
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = paris(2026, 9, 3, 20, 0),
                use24Hour = false,
            )

        assertThat(display?.timeLabel).isEqualTo("10:30 PM")
    }

    @Test
    fun aDaylightSavingGapExplainsTheShiftLikeTheWakeSchedule() {
        // Nuit du 29 mars 2026 : 02:30 n'existe pas en Europe/Paris, le moteur commun retient 03:00.
        val blocking =
            BlockingScheduleDto(
                localDateIso = "2026-03-29",
                localTimeIso = "02:30",
                startsAtEpochMillis = paris(2026, 3, 29, 3, 0),
            )

        val display =
            BlockingScheduleFormatter.format(
                schedule = blocking,
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = paris(2026, 3, 28, 20, 0),
            )

        assertThat(display?.timeLabel).isEqualTo("03:00")
        assertThat(display?.shiftedFromLocalTime).isEqualTo("02:30")
    }

    @Test
    fun aPartiallyFilledScheduleHasNothingToDisplay() {
        // Mélange refusé en amont par `INVALID_BLOCKING_SCHEDULE` (SPEC_CORE_KMP §7.5) : l'écran
        // n'a rien à en dire, et surtout rien à inventer.
        val display =
            BlockingScheduleFormatter.format(
                schedule = BlockingScheduleDto(startsAtEpochMillis = paris(2026, 9, 3, 22, 30)),
                zoneIdAtActivation = "Europe/Paris",
                nowEpochMillis = paris(2026, 9, 3, 20, 0),
            )

        assertThat(display).isNull()
    }
}
