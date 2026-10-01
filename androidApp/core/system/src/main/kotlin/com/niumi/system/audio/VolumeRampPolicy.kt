package com.niumi.system.audio

import kotlin.math.pow

/**
 * Courbe de la montée progressive (SPEC_ANDROID §10.2) : amplitude `10^(−2·(1−p))`, `p` étant la
 * fraction écoulée bornée à [0, 1]. −40 dB au départ, −20 dB à mi-parcours, 0 dB à la durée. Une
 * rampe linéaire d'amplitude est écartée : l'oreille la perçoit comme un saut suivi d'un plateau.
 */
object VolumeRampPolicy {
    const val TICK_MS = 250L

    /** −40 dB. */
    const val START_AMPLITUDE = 0.01f

    private const val DECADES = 2.0

    fun amplitudeAt(
        elapsedMs: Long,
        durationMs: Long,
    ): Float =
        when {
            durationMs <= 0 || elapsedMs >= durationMs -> 1f
            elapsedMs <= 0 -> START_AMPLITUDE
            else -> 10.0.pow(-DECADES * (1 - elapsedMs.toDouble() / durationMs)).toFloat()
        }

    fun isComplete(
        elapsedMs: Long,
        durationMs: Long,
    ): Boolean = elapsedMs >= durationMs
}
