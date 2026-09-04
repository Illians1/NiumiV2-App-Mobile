package com.niumi.system.audio

/** Poignée sur un lecteur audio en cours, abstraite du framework Android sous-jacent. */
interface AlarmPlayer {
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
