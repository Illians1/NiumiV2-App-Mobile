package com.niumi.system.notification

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Un instant de la notification de la garde : son heure (« 7:00 ») et, s'il n'est pas aujourd'hui,
 * son jour (« lundi »).
 */
data class GuardMoment(
    val time: String,
    val day: String?,
)

/**
 * Jour de [epochMillis] vu de [now], dans le fuseau de l'activation (décision de Mehdi du 2026-10-25 :
 * avec le planning, un réveil s'arme jusqu'à sept jours à l'avance, et la notification de la garde
 * dit alors le jour). `null` le jour même ou si le fuseau est illisible, sinon le jour de la
 * semaine en toutes lettres. Jamais « demain » : la notification n'est pas reposée à minuit, et un
 * « demain » posé la veille deviendrait faux ; un jour de la semaine reste vrai.
 */
fun relativeDayLabel(
    epochMillis: Long,
    zoneId: String,
    now: Long,
): String? {
    val zone =
        try {
            ZoneId.of(zoneId)
        } catch (_: DateTimeException) {
            return null
        }
    val day = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return if (ChronoUnit.DAYS.between(today, day) == 0L) null else WEEKDAYS[day.dayOfWeek.value - 1]
}

private val WEEKDAYS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
