package com.niumi.system.audio

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
 * Décide du motif de vibration selon l'état en cours (SPEC_ANDROID §10.2, §11.2). Une erreur
 * de scan doit rester audible sans jamais interrompre définitivement la vibration d'alarme :
 * `error(wasRepeating = true)` encode le pulse d'erreur *puis* la reprise du motif d'alarme
 * dans une seule waveform atomique, plutôt que de rappeler `startRepeating()` après coup.
 */
object VibrationPatternPolicy {
    private const val ALARM_PULSE_MS = 500L
    private const val ERROR_PULSE_MS = 200L
    private const val ERROR_PAUSE_MS = 300L
    private const val NO_REPEAT = -1

    /** Vibration répétée qui accompagne la sonnerie (SPEC_ANDROID §10.2). */
    fun repeating(): VibrationPattern =
        VibrationPattern(longArrayOf(0, ALARM_PULSE_MS, ALARM_PULSE_MS), repeatFromIndex = 0)

    /**
     * Vibration courte d'erreur (SPEC_ANDROID §11.2). Si une vibration d'alarme était active
     * ([wasRepeating]), la reprend après le pulse d'erreur plutôt que de la couper.
     */
    fun error(wasRepeating: Boolean): VibrationPattern =
        if (wasRepeating) {
            VibrationPattern(
                longArrayOf(0, ERROR_PULSE_MS, ERROR_PAUSE_MS, ALARM_PULSE_MS, ALARM_PULSE_MS),
                repeatFromIndex = 3,
            )
        } else {
            VibrationPattern(longArrayOf(0, ERROR_PULSE_MS), repeatFromIndex = NO_REPEAT)
        }
}
