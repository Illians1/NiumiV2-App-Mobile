package com.niumi.system.audio

/** Démarre et arrête la vibration répétée qui accompagne la sonnerie. */
interface VibrationController {
    /**
     * Vibration d'alarme en boucle. Avec une [ramp], le rythme suit la montée du volume (même
     * départ, même durée) : impulsions espacées, puis de plus en plus rapprochées jusqu'au rythme
     * plein (SPEC_ANDROID §10.2, *Vibration progressive*). `null` : rythme plein dès le départ.
     */
    fun startRepeating(ramp: VolumeRamp?)

    /**
     * Vibration courte d'erreur (SPEC_ANDROID §11.2, tag non associé). Si la vibration
     * d'alarme était active, elle reprend après le pulse d'erreur, au rythme atteint — voir
     * [VibrationPatternPolicy].
     */
    fun vibrateError()

    fun stop()
}
