package com.niumi.designsystem.format

import java.time.LocalDate
import java.time.LocalTime

/**
 * Écriture des heures et des durées dans l'app (`design-plan/SPECIFICATION.md` §2) : « 22:30 »,
 * « 7:00 » sans zéro devant l'heure, « 9 h 26 », « 56 min », « 8 h 30 de déconnexion ». Une durée de
 * moins d'une heure s'écrit en minutes, jamais « 0 h 56 ». Date de l'écran de réveil :
 * « Vendredi 2 octobre » (maquette 28). Vérifié par `NiumiTimeFormatTest`.
 *
 * Les noms de jours et de mois sont écrits ici plutôt que lus dans les données de langue du
 * téléphone : l'app n'existe qu'en français, et ces données varient d'une version d'Android à
 * l'autre (capitale, abréviations).
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

    /** Durée du coucher au réveil : « 8 h 30 de déconnexion » (décision de Mehdi du 2026-10-31). */
    fun sleep(minutes: Int): String = "${duration(minutes)} de déconnexion"

    /** Date sans année : « Vendredi 2 octobre », « Vendredi 1er janvier ». */
    fun dayAndDate(date: LocalDate): String {
        val day = if (date.dayOfMonth == 1) "1er" else date.dayOfMonth.toString()
        return "${WEEKDAYS[date.dayOfWeek.value - 1]} $day ${MONTHS[date.monthValue - 1]}"
    }

    private val WEEKDAYS = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    private val MONTHS =
        listOf(
            "janvier",
            "février",
            "mars",
            "avril",
            "mai",
            "juin",
            "juillet",
            "août",
            "septembre",
            "octobre",
            "novembre",
            "décembre",
        )

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
}
