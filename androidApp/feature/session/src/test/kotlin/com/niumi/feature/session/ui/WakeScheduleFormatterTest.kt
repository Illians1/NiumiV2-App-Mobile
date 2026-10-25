package com.niumi.feature.session.ui

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** Heure d'un instant pour la notification du service de garde (SPEC_ANDROID §12.5). */
class WakeScheduleFormatterTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun anInstantIsRenderedAsATimeInTheActivationZone() {
        val sevenInParis = ZonedDateTime.of(2026, 10, 20, 7, 0, 0, 0, ZoneId.of("Europe/Paris"))
        val epochMillis = sevenInParis.toInstant().toEpochMilli()

        // « 7:00 », sans zéro devant (`design-plan/SPECIFICATION.md` §2 ; appliqué aux notifications
        // le 2026-10-25).
        assertThat(WakeScheduleFormatter.formatTime(epochMillis, "Europe/Paris", use24Hour = true)).isEqualTo("7:00")
        assertThat(
            WakeScheduleFormatter.formatTime(epochMillis, "Europe/Paris", use24Hour = false),
        ).isEqualTo("7:00 AM")
        assertThat(WakeScheduleFormatter.formatTime(epochMillis, "Europe/London", use24Hour = true)).isEqualTo("6:00")
    }

    @Test
    fun anUnreadableZoneHasNoTime() {
        assertThat(WakeScheduleFormatter.formatTime(0L, "Pas/UnFuseau", use24Hour = true)).isNull()
    }

    @Test
    fun digitsStayLatinRegardlessOfTheJvmDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        val epochMillis = ZonedDateTime.of(2026, 9, 4, 7, 0, 0, 0, ZoneId.of("Europe/Paris")).toInstant().toEpochMilli()

        assertThat(
            WakeScheduleFormatter.formatTime(epochMillis, "Europe/Paris", use24Hour = false),
        ).isEqualTo("7:00 AM")
    }
}
