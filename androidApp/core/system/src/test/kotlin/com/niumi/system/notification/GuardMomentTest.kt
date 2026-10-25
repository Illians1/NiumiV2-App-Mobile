package com.niumi.system.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Jour d'un instant dans la notification de la garde (décision de Mehdi du 2026-10-25 : avec le
 * planning, un réveil s'arme jusqu'à sept jours à l'avance, la notification dit alors le jour) :
 * rien le jour même, sinon le jour de la semaine. Jamais « demain » : posé la veille, il deviendrait
 * faux après minuit, la notification n'étant pas reposée (décision du 2026-10-25).
 */
class GuardMomentTest {
    private fun paris(local: String) =
        LocalDateTime
            .parse(local)
            .atZone(ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    @Test
    fun theSameDayHasNoDayLabel() {
        assertThat(
            relativeDayLabel(paris("2026-10-25T23:30"), "Europe/Paris", now = paris("2026-10-25T13:00")),
        ).isNull()
    }

    @Test
    fun theNextDayIsNamedRatherThanCalledTomorrow() {
        assertThat(
            relativeDayLabel(paris("2026-10-26T00:05"), "Europe/Paris", now = paris("2026-10-25T23:50")),
        ).isEqualTo("lundi")
        assertThat(
            relativeDayLabel(paris("2026-10-26T07:00"), "Europe/Paris", now = paris("2026-10-25T13:00")),
        ).isEqualTo("lundi")
    }

    @Test
    fun fartherDaysAreNamedInFrench() {
        assertThat(
            relativeDayLabel(paris("2026-10-31T11:45"), "Europe/Paris", now = paris("2026-10-25T13:00")),
        ).isEqualTo("samedi")
        assertThat(
            relativeDayLabel(paris("2026-11-01T00:30"), "Europe/Paris", now = paris("2026-10-25T13:00")),
        ).isEqualTo("dimanche")
    }

    @Test
    fun theDayIsReadInTheActivationZone() {
        // 23:30 à Paris le 25 : déjà le 26 à Tokyo ; le fuseau de l'activation fait foi.
        assertThat(
            relativeDayLabel(paris("2026-10-25T23:30"), "Asia/Tokyo", now = paris("2026-10-25T13:00")),
        ).isEqualTo("lundi")
    }

    @Test
    fun anUnreadableZoneGivesNoLabel() {
        assertThat(relativeDayLabel(0L, "Pas/UnFuseau", now = 0L)).isNull()
    }
}
