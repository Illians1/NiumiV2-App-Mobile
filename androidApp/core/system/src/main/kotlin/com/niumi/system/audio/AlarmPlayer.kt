package com.niumi.system.audio

/** Poignée sur un lecteur audio en cours, abstraite du framework Android sous-jacent. */
interface AlarmPlayer {
    /** Amplitude du lecteur, de 0 à 1 (montée progressive, SPEC_ANDROID §10.2). */
    fun setVolume(amplitude: Float)

    fun release()
}

/**
 * Fabrique un [AlarmPlayer] configuré et démarré pour la sonnerie `ringtoneKey`. Peut lancer :
 * l'appelant l'attrape (`DefaultAlarmAudioEngine`). La résolution de `ringtoneKey` vers une
 * ressource concrète est déléguée à l'implémentation Android, propriétaire de son `R.raw`.
 */
fun interface AlarmPlayerFactory {
    fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
    ): AlarmPlayer
}
