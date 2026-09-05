package com.niumi.system.audio

/** Démarre et arrête la vibration répétée qui accompagne la sonnerie. */
interface VibrationController {
    fun startRepeating()

    /**
     * Vibration courte d'erreur (SPEC_ANDROID §11.2, tag non associé). Si la vibration
     * d'alarme était active, elle reprend après le pulse d'erreur — voir
     * [VibrationPatternPolicy].
     */
    fun vibrateError()

    fun stop()
}
