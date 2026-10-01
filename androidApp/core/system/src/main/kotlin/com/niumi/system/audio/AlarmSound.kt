package com.niumi.system.audio

/**
 * Ce que sonne une session (SPEC_ANDROID §10.2) : décidé par `RingingSoundResolver` depuis la
 * session persistée, joué par [AlarmAudioEngine]. [volumeRamp] nul = volume constant.
 */
data class AlarmSound(
    val ringtoneKey: String,
    val vibrationEnabled: Boolean,
    val volumeRamp: VolumeRamp?,
)
