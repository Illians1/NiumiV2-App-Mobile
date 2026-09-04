package com.niumi.system.audio

/** Démarre et arrête la vibration répétée qui accompagne la sonnerie. */
interface VibrationController {
    fun startRepeating()

    fun stop()
}
