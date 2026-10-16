package com.niumi.system.audio

/** Poignée sur un lecteur audio en cours, abstraite du framework Android sous-jacent. */
interface AlarmPlayer {
    /** Amplitude du lecteur, de 0 à 1 (montée progressive, SPEC_ANDROID §10.2). */
    fun setVolume(amplitude: Float)

    fun release()

    /**
     * Vrai si la boucle recolle la fin du son au début sans silence (`AudioTrack` statique à points
     * de boucle) ; faux pour `MediaPlayer`, qui arrête puis relance la piste à chaque tour — 70 à
     * 430 ms de blanc mesurés le 2026-10-15 sur « Oiseaux » (SPEC_ANDROID §10.2). Faux par défaut :
     * un lecteur qui ne dit rien ne prétend pas boucler sans blanc.
     */
    val gaplessLoop: Boolean
        get() = false
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
