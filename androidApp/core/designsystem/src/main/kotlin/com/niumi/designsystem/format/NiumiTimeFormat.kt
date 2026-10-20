package com.niumi.designsystem.format

import java.time.LocalTime

/**
 * Écriture des heures et des durées dans l'app (`design-plan/SPECIFICATION.md` §2) : « 22:30 »,
 * « 7:00 » sans zéro devant l'heure, « 9 h 26 », « 56 min », « 8 h 30 de sommeil ». Une durée de
 * moins d'une heure s'écrit en minutes, jamais « 0 h 56 ». Vérifié par `NiumiTimeFormatTest`.
 */
object NiumiTimeFormat {
    private const val MINUTES_PER_HOUR = 60

    /** Heure d'horloge : « 7:00 », « 22:30 », « 0:05 ». Les secondes sont ignorées. */
    fun clock(time: LocalTime): String = "${time.hour}:${twoDigits(time.minute)}"

    /** Durée en minutes : « 56 min » sous une heure, « 9 h 26 » ou « 1 h 00 » au-delà. */
    fun duration(minutes: Int): String {
        require(minutes >= 0) { "Une durée ne peut pas être négative : $minutes min." }
        if (minutes < MINUTES_PER_HOUR) return "$minutes min"
        return "${minutes / MINUTES_PER_HOUR} h ${twoDigits(minutes % MINUTES_PER_HOUR)}"
    }

    /** Durée de sommeil : « 8 h 30 de sommeil ». */
    fun sleep(minutes: Int): String = "${duration(minutes)} de sommeil"

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
}
