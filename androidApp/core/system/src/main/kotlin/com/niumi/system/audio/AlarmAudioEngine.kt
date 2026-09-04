package com.niumi.system.audio

import com.niumi.system.common.OperationResult

/** Contrat du moteur audio d'alarme (« Interfaces transverses » du plan MVP). */
interface AlarmAudioEngine {
    fun start(
        ringtoneKey: String,
        vibrationEnabled: Boolean,
    ): OperationResult

    fun stop(): OperationResult

    val isPlaying: Boolean
}
