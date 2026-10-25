package com.niumi.designsystem.drawing

import kotlin.math.atan2
import kotlin.math.roundToInt

/** Partie pleine et fondu de l'ambre en session, en degrés le long de l'arc. */
data class AmberSpans(
    val solid: Float,
    val fade: Float,
) {
    companion object {
        /**
         * L'ambre de la session découpé le long de l'arc : plein du coucher au point de l'heure
         * actuelle, puis le fondu de [DialGeometry.FADE_DEGREES] à partir du point, raccourci en fin
         * de nuit pour ne jamais dépasser la période (relecture du 2026-10-25 : un dégradé unique sur
         * un tour ne tenait pas au-delà de 338° écoulés).
         */
        fun of(
            elapsedDegrees: Float,
            sweepDegrees: Float,
        ): AmberSpans {
            val solid = elapsedDegrees.coerceIn(0f, sweepDegrees)
            return AmberSpans(solid = solid, fade = minOf(DialGeometry.FADE_DEGREES, sweepDegrees - solid))
        }
    }
}

/** Les deux poignées du cadran de l'accueil. */
enum class DialHandle { BEDTIME, WAKE }

/**
 * Géométrie du cadran de 24 heures de l'accueil (VALEURS-DE-DESIGN.md §5), sans Compose pour se
 * tester en JVM. Les heures sont des minutes depuis minuit (0 à 1439) ; les angles, des degrés
 * comptés depuis le haut dans le sens horaire (une heure vaut 15°).
 *
 * Décisions de Mehdi du 2026-10-25 : une poignée glissée avance de [DRAG_STEP_MINUTES] en
 * [DRAG_STEP_MINUTES] minutes ; la période du coucher au réveil dure au moins une minute ; deux heures
 * identiques forment une période de 24 h. La poignée glissée ne traverse donc jamais l'autre : côté
 * nuit, elle s'arrête au dernier cran avant elle ; côté jour, elle la rejoint (24 h).
 */
object DialGeometry {
    const val MINUTES_PER_DAY = 1440
    const val DRAG_STEP_MINUTES = 5

    /**
     * Longueur du fondu de l'ambre devant le point de l'heure actuelle, en session : 12° (48 min),
     * pente franche dès le point (décision de Mehdi du 2026-10-25 après essai ; 34° en demi-cosinus
     * dans les maquettes, dont le début paraissait loin du point).
     */
    const val FADE_DEGREES = 12f

    private const val DEGREES_PER_TURN = 360f
    private const val HALF_TURN = 180f
    private const val DEGREES_PER_MINUTE = DEGREES_PER_TURN / MINUTES_PER_DAY

    fun degreesOf(minutes: Int): Float = minutes.mod(MINUTES_PER_DAY) * DEGREES_PER_MINUTE

    /** Angle d'un point vu du centre, [dx] vers la droite et [dy] vers le bas (repère de l'écran). */
    fun degreesOfPoint(
        dx: Float,
        dy: Float,
    ): Float {
        val degrees = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        return if (degrees < 0f) degrees + DEGREES_PER_TURN else degrees
    }

    /** Déplacement le plus court entre deux angles, en minutes : passer minuit n'est pas un tour complet. */
    fun signedDeltaMinutes(
        fromDegrees: Float,
        toDegrees: Float,
    ): Float {
        var delta = (toDegrees - fromDegrees) % DEGREES_PER_TURN
        if (delta > HALF_TURN) delta -= DEGREES_PER_TURN
        if (delta < -HALF_TURN) delta += DEGREES_PER_TURN
        return delta / DEGREES_PER_MINUTE
    }

    /** Durée de la période du coucher au réveil ; deux heures identiques valent 24 h. */
    fun periodMinutes(
        bedtime: Int,
        wake: Int,
    ): Int = (wake - bedtime).mod(MINUTES_PER_DAY).let { if (it == 0) MINUTES_PER_DAY else it }

    /** Minutes écoulées depuis le coucher, en session. */
    fun elapsedMinutes(
        bedtime: Int,
        now: Int,
    ): Int = (now - bedtime).mod(MINUTES_PER_DAY)

    /**
     * Heure de la poignée [handle] partie de [start] et déplacée de [deltaMinutes] (cumul non replié,
     * qui peut dépasser un tour), l'autre poignée restant à [other]. Arrondie au cran de 5 minutes,
     * puis bornée pour que la période reste entre une minute et 24 h.
     */
    fun dragged(
        handle: DialHandle,
        start: Int,
        other: Int,
        deltaMinutes: Float,
    ): Int {
        val startPeriod =
            if (handle == DialHandle.WAKE) periodMinutes(other, start) else periodMinutes(start, other)
        val snapped = ((start + deltaMinutes) / DRAG_STEP_MINUTES).roundToInt() * DRAG_STEP_MINUTES
        val movement = snapped - start
        val period = if (handle == DialHandle.WAKE) startPeriod + movement else startPeriod - movement
        val minutes =
            when {
                period > MINUTES_PER_DAY -> other
                period >= 1 -> snapped
                handle == DialHandle.WAKE -> ceilToStep(other + 1)
                else -> floorToStep(other - 1)
            }
        return minutes.mod(MINUTES_PER_DAY)
    }

    /**
     * Opacité de l'ambre à [degreesPastNow] devant le point de l'heure actuelle : pleine avant lui,
     * puis une pente droite jusqu'à zéro sur [FADE_DEGREES].
     */
    fun fadeAlpha(degreesPastNow: Float): Float = (1f - degreesPastNow / FADE_DEGREES).coerceIn(0f, 1f)

    /** L'heure [now] est-elle dans la nuit, du coucher inclus au réveil exclu ? Une période de 24 h l'est toujours. */
    fun isWithinPeriod(
        bedtime: Int,
        wake: Int,
        now: Int,
    ): Boolean = elapsedMinutes(bedtime, now) < periodMinutes(bedtime, wake)

    /**
     * Ce qu'ouvre un toucher relâché : `null` si le doigt a bougé au-delà de [touchSlop] (un glissé ou
     * un défilement, jamais un toucher) ; sinon l'onglet du coucher si la poignée du coucher était
     * sous le doigt, celui du réveil partout ailleurs (décision de Mehdi du 2026-10-25).
     */
    fun tapTarget(
        grabbed: DialHandle?,
        travelled: Float,
        touchSlop: Float,
    ): DialHandle? =
        when {
            travelled > touchSlop -> null
            grabbed == DialHandle.BEDTIME -> DialHandle.BEDTIME
            else -> DialHandle.WAKE
        }

    private fun ceilToStep(minutes: Int): Int =
        Math.floorDiv(minutes + DRAG_STEP_MINUTES - 1, DRAG_STEP_MINUTES) * DRAG_STEP_MINUTES

    private fun floorToStep(minutes: Int): Int = Math.floorDiv(minutes, DRAG_STEP_MINUTES) * DRAG_STEP_MINUTES
}
