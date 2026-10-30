package com.niumi.system.audio

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Motif de vibration pur, traduit par `AndroidVibrationController` en
 * `VibrationEffect.createWaveform(timings, repeatFromIndex)` — non testable en JVM
 * (`VibrationEffect` est un stub Android). [timings] alterne repos/vibration à partir de
 * l'indice 0 ; [repeatFromIndex] reprend la boucle à cet indice, ou `-1` pour ne pas boucler.
 */
data class VibrationPattern(
    val timings: LongArray,
    val repeatFromIndex: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VibrationPattern) return false
        return timings.contentEquals(other.timings) && repeatFromIndex == other.repeatFromIndex
    }

    override fun hashCode(): Int = 31 * timings.contentHashCode() + repeatFromIndex
}

/**
 * Décide du motif de vibration selon l'état en cours (SPEC_ANDROID §10.2, §11.2).
 *
 * **Rythme plein (révisé le 2026-10-30).** Impulsion de 800 ms toutes les 3 s : 500/500 était jugé
 * trop fréquent, donc trop violent, sans montée comme en fin de montée.
 *
 * **Vibration progressive (décision du 2026-10-26, départ espacé le 2026-10-30).** Avec une montée
 * du volume, l'impulsion garde 800 ms et la pause se resserre de 15 500 ms (une impulsion toutes
 * les 16 s environ) à 2 200 ms sur la même durée, en décroissance log-linéaire comme le volume en
 * dB. Tout le motif tient dans une seule waveform qui finit sur la boucle du rythme plein :
 * `vibrate()` remplaçant l'effet en cours, aucun rappel au fil du temps ne pourrait garantir la
 * continuité.
 * [repeatFromIndex] désigne toujours l'impulsion de cette boucle.
 *
 * Une erreur de scan doit rester sensible sans jamais interrompre définitivement la vibration
 * d'alarme : [error] encode le pulse d'erreur *puis* la reprise du motif en cours dans une seule
 * waveform atomique, plutôt que de rappeler `startRepeating()` après coup.
 */
object VibrationPatternPolicy {
    private const val ALARM_PULSE_MS = 800L
    private const val START_PAUSE_MS = 15_500L
    private const val END_PAUSE_MS = 2_200L
    private const val ERROR_PULSE_MS = 200L
    private const val ERROR_PAUSE_MS = 300L
    private const val NO_REPEAT = -1

    /** Rythme plein, qui accompagne la sonnerie sans montée (SPEC_ANDROID §10.2). */
    fun repeating(): VibrationPattern =
        VibrationPattern(longArrayOf(0, ALARM_PULSE_MS, END_PAUSE_MS), repeatFromIndex = 1)

    /**
     * Rythme qui suit la montée du volume, à partir de [elapsedMs] écoulées sur [durationMs] :
     * impulsions de plus en plus rapprochées jusqu'à la durée, puis rythme plein en boucle.
     */
    fun repeating(
        elapsedMs: Long,
        durationMs: Long,
    ): VibrationPattern {
        if (durationMs <= 0 || elapsedMs >= durationMs) return repeating()
        val timings = mutableListOf(0L)
        var t = elapsedMs.coerceAtLeast(0)
        while (t < durationMs) {
            val pause = pauseAt(t, durationMs)
            timings += ALARM_PULSE_MS
            timings += pause
            t += ALARM_PULSE_MS + pause
        }
        val repeatFromIndex = timings.size
        timings += ALARM_PULSE_MS
        timings += END_PAUSE_MS
        return VibrationPattern(timings.toLongArray(), repeatFromIndex)
    }

    /** Pause après une impulsion émise à [elapsedMs] : `15500 × (2200/15500)^p`, `p` borné à [0, 1]. */
    fun pauseAt(
        elapsedMs: Long,
        durationMs: Long,
    ): Long {
        if (durationMs <= 0) return END_PAUSE_MS
        val progress = (elapsedMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
        val ratio = END_PAUSE_MS.toDouble() / START_PAUSE_MS
        return (START_PAUSE_MS * ratio.pow(progress)).roundToLong()
    }

    /**
     * Vibration courte d'erreur (SPEC_ANDROID §11.2). Si une vibration d'alarme était active, la
     * reprend après le pulse d'erreur ([resume], le motif au temps écoulé courant) plutôt que de
     * la couper.
     */
    fun error(resume: VibrationPattern?): VibrationPattern =
        if (resume == null) {
            VibrationPattern(longArrayOf(0, ERROR_PULSE_MS), repeatFromIndex = NO_REPEAT)
        } else {
            VibrationPattern(
                longArrayOf(0, ERROR_PULSE_MS, ERROR_PAUSE_MS) + resume.timings.copyOfRange(1, resume.timings.size),
                repeatFromIndex = resume.repeatFromIndex + 2,
            )
        }
}
