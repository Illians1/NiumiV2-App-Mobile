package com.niumi.system.plan

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Planning récurrent de l'utilisateur, tel qu'il est stocké sur Android (SPEC_CORE_KMP §8.4,
 * décision 4 du Lot 12 : « planning calculé en KMP, stocké en natif »). [enabled] est l'interrupteur
 * général (`design-plan/SPECIFICATION.md` §2) ; jours ISO de 1 (lundi) à 7 (dimanche), heures
 * locales `HH:mm`. Valeurs par défaut de §2 et §9 : interrupteur éteint à la fin de la mise en
 * route, lundi à vendredi, réveil 7:00, coucher 22:30.
 *
 * **Homonyme** de `com.niumi.core.schedule.WakePlan`, le planning commun que reçoit
 * `NextOccurrenceCalculator` : celui-ci n'a pas d'interrupteur, qui reste natif.
 */
data class WakePlan(
    val enabled: Boolean,
    val enabledIsoDays: Set<Int>,
    val wakeTimeIso: String,
    val bedtimeIso: String,
) {
    /**
     * Lève `IllegalArgumentException("WAKE_PLAN_INVALID")` pour un jour hors 1 à 7 ou une heure qui
     * n'est pas `HH:mm`.
     */
    fun requireValid() {
        require(enabledIsoDays.all { it in ISO_DAYS } && isIsoTime(wakeTimeIso) && isIsoTime(bedtimeIso)) {
            INVALID_MESSAGE
        }
    }

    companion object {
        val DEFAULT: WakePlan =
            WakePlan(
                enabled = false,
                enabledIsoDays = setOf(1, 2, 3, 4, 5),
                wakeTimeIso = "07:00",
                bedtimeIso = "22:30",
            )

        private const val INVALID_MESSAGE = "WAKE_PLAN_INVALID"
    }
}

private val ISO_DAYS = 1..7

private val ISO_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun isIsoTime(value: String): Boolean =
    try {
        LocalTime.parse(value, ISO_TIME)
        true
    } catch (_: DateTimeParseException) {
        false
    }

/** Jours ISO triés, séparés par des virgules ; l'ensemble vide s'écrit `""` (aucun jour coché, D17). */
internal fun encodeIsoDays(days: Set<Int>): String = days.sorted().joinToString(",")

/**
 * Traduit ce que le `DataStore` contient littéralement en [WakePlan], hors de toute dépendance
 * Android pour rester testable en JVM. Champ par champ : une valeur absente ou illisible retombe sur
 * celle de [WakePlan.DEFAULT], sans emporter le reste du planning. `""` pour les jours se lit
 * « aucun jour coché », jamais comme une absence.
 */
internal fun decodeWakePlan(
    enabled: Boolean?,
    days: String?,
    wakeTime: String?,
    bedtime: String?,
): WakePlan =
    WakePlan(
        enabled = enabled ?: WakePlan.DEFAULT.enabled,
        enabledIsoDays = days?.let(::decodeIsoDays) ?: WakePlan.DEFAULT.enabledIsoDays,
        wakeTimeIso = wakeTime?.takeIf(::isIsoTime) ?: WakePlan.DEFAULT.wakeTimeIso,
        bedtimeIso = bedtime?.takeIf(::isIsoTime) ?: WakePlan.DEFAULT.bedtimeIso,
    )

private fun decodeIsoDays(raw: String): Set<Int>? {
    if (raw.isEmpty()) return emptySet()
    val days = raw.split(',').map { it.toIntOrNull() }
    return if (days.all { it != null && it in ISO_DAYS }) days.filterNotNull().toSet() else null
}
