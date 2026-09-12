package com.niumi.feature.session.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.WakeScheduleDto
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Projection d'affichage d'un [WakeScheduleDto] (SPEC_ANDROID §8, §15 : afficher date, heure et
 * fuseau ; SPEC_CORE_KMP §8.1 : afficher la date complète avant confirmation). Le formateur lit
 * exclusivement `triggerAtEpochMillis`, jamais `localDateIso`/`localTimeIso`, pour ne jamais
 * afficher un faux état lors d'un trou d'heure d'été (§15).
 */
class WakeScheduleFormatterTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

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
    fun tomorrowsTriggerIsLabelledDemain() {
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.relativeDayLabel).isEqualTo("Demain")
        assertThat(display.timeLabel).isEqualTo("07:00")
        assertThat(display.zoneLabel).isEqualTo("Europe/Paris")
        assertThat(display.sentence).isEqualTo("Demain, vendredi 4 septembre à 07:00 (Europe/Paris)")
    }

    @Test
    fun sameDayTriggerIsLabelledAujourdhui() {
        val now = paris(2026, 9, 4, 6, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.relativeDayLabel).isEqualTo("Aujourd'hui")
    }

    @Test
    fun aTriggerBeyondTomorrowHasNoRelativeDayLabel() {
        // Une session `ARMED` relue plus tard, plusieurs jours après son activation.
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-10",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 10, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.relativeDayLabel).isNull()
        assertThat(display.fullDateLabel).isEqualTo("jeudi 10 septembre")
    }

    @Test
    fun aDifferentDisplayZoneRendersBothDateAndTimeInThatZone() {
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        // Europe/Paris 07:00 == Pacific/Auckland 17:00 le même jour civil (été austral, UTC+10).
        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now, displayZoneId = "Pacific/Auckland")

        assertThat(display.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(display.timeLabel).isEqualTo("17:00")
        // Un fuseau d'affichage différent n'est pas un trou d'heure d'été : pas d'explication.
        assertThat(display.shiftedFromLocalTime).isNull()
    }

    @Test
    fun twelveHourFormatRendersAmPm() {
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now, use24Hour = false)

        assertThat(display.timeLabel).isEqualTo("7:00 AM")
    }

    @Test
    fun aDaylightSavingGapDisplaysTheRealInstantAndTheShiftedLocalTime() {
        // Nuit du 29 mars 2026 : 02:00 CET saute directement à 03:00 CEST en Europe/Paris.
        // Une saisie de 02:30 n'existe pas ; le moteur commun programme le premier instant valide.
        val now = paris(2026, 3, 28, 20, 0)
        val realTrigger = paris(2026, 3, 29, 3, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-03-29",
                localTimeIso = "02:30",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = realTrigger,
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.timeLabel).isEqualTo("03:00")
        assertThat(display.shiftedFromLocalTime).isEqualTo("02:30")
    }

    @Test
    fun aNormalScheduleHasNoShiftedLocalTime() {
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.shiftedFromLocalTime).isNull()
    }

    @Test
    fun digitsStayLatinRegardlessOfTheJvmDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        val now = paris(2026, 9, 3, 20, 0)
        val schedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            )

        val display = WakeScheduleFormatter.format(schedule, nowEpochMillis = now)

        assertThat(display.timeLabel).isEqualTo("07:00")
        assertThat(display.timeLabel).matches("[0-9:]+")
    }
}
