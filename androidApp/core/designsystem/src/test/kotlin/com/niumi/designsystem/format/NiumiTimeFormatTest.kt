package com.niumi.designsystem.format

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Écriture des heures et des durées (`design-plan/SPECIFICATION.md` §2) : « 22:30 », « 7:00 »
 * sans zéro devant, « 9 h 26 », « 56 min », « 8 h 30 de sommeil ». Cas limite 4 du plan de
 * refonte : une durée de moins d'une heure s'écrit en minutes, jamais « 0 h 56 ».
 */
class NiumiTimeFormatTest {
    @Test
    fun clockWritesTheHourWithoutALeadingZero() {
        assertThat(NiumiTimeFormat.clock(LocalTime.of(7, 0))).isEqualTo("7:00")
        assertThat(NiumiTimeFormat.clock(LocalTime.of(22, 30))).isEqualTo("22:30")
    }

    @Test
    fun clockKeepsTwoDigitsForTheMinutesAndWritesMidnightAsZero() {
        assertThat(NiumiTimeFormat.clock(LocalTime.of(0, 5))).isEqualTo("0:05")
        assertThat(NiumiTimeFormat.clock(LocalTime.MIDNIGHT)).isEqualTo("0:00")
    }

    @Test
    fun clockIgnoresSeconds() {
        assertThat(NiumiTimeFormat.clock(LocalTime.of(6, 59, 59))).isEqualTo("6:59")
    }

    @Test
    fun durationUnderAnHourIsWrittenInMinutes() {
        assertThat(NiumiTimeFormat.duration(56)).isEqualTo("56 min")
        assertThat(NiumiTimeFormat.duration(1)).isEqualTo("1 min")
        assertThat(NiumiTimeFormat.duration(0)).isEqualTo("0 min")
    }

    @Test
    fun durationOfAnHourOrMoreWritesHoursAndTwoDigitMinutes() {
        assertThat(NiumiTimeFormat.duration(566)).isEqualTo("9 h 26")
        assertThat(NiumiTimeFormat.duration(60)).isEqualTo("1 h 00")
        assertThat(NiumiTimeFormat.duration(65)).isEqualTo("1 h 05")
        assertThat(NiumiTimeFormat.duration(23 * 60 + 59)).isEqualTo("23 h 59")
    }

    @Test
    fun sleepAppendsTheWordsOfTheDesign() {
        assertThat(NiumiTimeFormat.sleep(510)).isEqualTo("8 h 30 de sommeil")
        assertThat(NiumiTimeFormat.sleep(45)).isEqualTo("45 min de sommeil")
    }

    @Test
    fun aNegativeDurationIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { NiumiTimeFormat.duration(-1) }
        assertThrows(IllegalArgumentException::class.java) { NiumiTimeFormat.sleep(-1) }
    }

    /** Date de l’écran de réveil (maquette 28) : jour en capitale, sans année, « 1er » le premier du mois. */
    @Test
    fun dayAndDateWritesTheWeekdayCapitalisedThenTheDayAndMonth() {
        assertThat(NiumiTimeFormat.dayAndDate(LocalDate.of(2026, 10, 2))).isEqualTo("Vendredi 2 octobre")
        assertThat(NiumiTimeFormat.dayAndDate(LocalDate.of(2026, 8, 17))).isEqualTo("Lundi 17 août")
        assertThat(NiumiTimeFormat.dayAndDate(LocalDate.of(2027, 1, 1))).isEqualTo("Vendredi 1er janvier")
    }
}
