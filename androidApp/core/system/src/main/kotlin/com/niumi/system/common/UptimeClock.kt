package com.niumi.system.common

/**
 * Temps écoulé depuis le démarrage de l'appareil, injectable comme [Clock] : aucune logique ne lit
 * `android.os.SystemClock` directement, pour que les tests n'attendent jamais un vrai démarrage.
 *
 * Premier usage (étape 25) : ne pas juger le NFC pendant que sa pile s'initialise au démarrage
 * (`SessionNfcEvaluability`).
 */
interface UptimeClock {
    fun elapsedSinceBootMillis(): Long
}

/**
 * `elapsedRealtime()` et non `uptimeMillis()` : le second s'arrête pendant le sommeil profond, si
 * bien qu'un appareil démarré la veille pourrait sembler démarré depuis quelques minutes.
 */
class AndroidUptimeClock : UptimeClock {
    override fun elapsedSinceBootMillis(): Long = android.os.SystemClock.elapsedRealtime()
}
