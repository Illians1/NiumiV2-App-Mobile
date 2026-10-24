package com.niumi.core.insight

/** Instant de la sonnerie, instant de la libération et durée déclarée à l'écran 3 bis, en minutes. */
public data class TimeToGetUpInput(
    val alarmAtEpochMillis: Long,
    val releasedAtEpochMillis: Long,
    val declaredMinutes: Int,
)

/** [savedMinutes] vaut `declaredMinutes − elapsedMinutes` s'il est strictement positif, sinon `null`. */
public data class TimeToGetUpResult(
    val elapsedMinutes: Int,
    val savedMinutes: Int?,
)

/**
 * Temps gagné de l'écran 11 (`SPECIFICATION.md` §15, contrat 1.5) : la comparaison ne s'affiche que
 * si l'utilisateur s'est levé plus vite que la durée déclarée. Q10 (décision du 2026-10-24) : le
 * temps écoulé est arrondi à la minute inférieure, cohérent avec « Debout à 7:04 » pour un scan à
 * 7:04:59 ; une libération antérieure à la sonnerie compte pour zéro. D3 : « 1 heure et + » est
 * transmis comme 60 par la plateforme.
 */
public object TimeToGetUpCalculator {
    private const val MILLIS_PER_MINUTE = 60_000L

    public fun compute(input: TimeToGetUpInput): TimeToGetUpResult {
        val elapsedMillis = (input.releasedAtEpochMillis - input.alarmAtEpochMillis).coerceAtLeast(0L)
        val elapsedMinutes = (elapsedMillis / MILLIS_PER_MINUTE).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val saved = input.declaredMinutes - elapsedMinutes
        return TimeToGetUpResult(elapsedMinutes, saved.takeIf { it > 0 })
    }
}
