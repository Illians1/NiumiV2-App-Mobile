package com.niumi.system.readiness

import com.niumi.database.directboot.UnlockState
import com.niumi.system.common.UptimeClock

/**
 * La fenêtre, juste après le déverrouillage, où Android relie encore le service d'accessibilité et
 * démarre le service NFC (SPEC_ANDROID §13.1 ; étape 25). Aucun des deux ne peut y être jugé.
 *
 * **NFC, mesuré le 2026-09-27 à 19:38** sur le même appareil : le service NFC ne démarre qu'**après**
 * le déverrouillage — absent jusqu'à 44,2 s de démarrage, `turningon`, `on`, de nouveau
 * `turningon`, stable à 53,6 s, pour un déverrouillage à 40,4 s. Une garde comptée depuis le
 * démarrage expirait avant le déverrouillage même. Le service d'accessibilité, lui, était relié
 * 3,6 s au plus après le déverrouillage.
 *
 * **Défaut mesuré le 2026-09-27** sur Xiaomi 25080RABDG / Android 16 : 0,3 s après le
 * déverrouillage, Niumi restait inscrit dans `enabled_accessibility_services` mais
 * `accessibility_enabled` valait 0 ; la passe `USER_UNLOCKED` consignait un
 * `BLOCKING_PERMISSION_REVOKED` `CRITICAL` mensonger et laissait la session `DEGRADED` jusqu'à sa
 * fin. L'étape 19 neutralisait le contrôle **avant** le déverrouillage (point de vigilance 11), pas
 * la seconde qui le suit.
 *
 * Le début de la fenêtre est le **premier instant où ce processus a vu l'appareil déverrouillé** —
 * relevé à la première lecture, que le diagnostic fait à chaque évaluation. Un processus vivant
 * depuis le démarrage verrouillé le relève donc à la passe `USER_UNLOCKED`, soit à la seconde du
 * déverrouillage ; un processus démarré plus tard ouvre sa propre fenêtre, ce qui ne retarde que
 * le verdict d'un service **réellement** non relié (mort du processus sur HyperOS, étape 25).
 *
 * [GRACE_MILLIS] : mesures du 27/09 — accessibilité reliée 3,6 s au plus après le déverrouillage,
 * NFC stable 13,2 s après. Une seule mesure, sur un seul appareil : à revoir si une campagne
 * dépasse la fenêtre.
 *
 * La fenêtre se rouvre si l'appareil est vu verrouillé : dans la vie d'un processus, cela
 * n'arrive qu'avant le premier déverrouillage, et c'est la sémantique voulue.
 */
class UnlockSettling(
    private val unlockState: UnlockState,
    private val uptimeClock: UptimeClock,
) {
    @Volatile
    private var firstUnlockedAt: Long? = null

    /** Relève le premier instant déverrouillé, et dit si la fenêtre de liaison court encore. */
    fun isSettling(): Boolean = remainingMillis() > 0

    /** Déverrouillé **et** fenêtre refermée : ce qui ne pouvait pas être jugé peut désormais l'être. */
    fun isSettled(): Boolean = unlockState.isUserUnlocked && remainingMillis() == 0L

    fun remainingMillis(): Long {
        if (!unlockState.isUserUnlocked) {
            firstUnlockedAt = null
            return 0
        }
        val now = uptimeClock.elapsedSinceBootMillis()
        val since = firstUnlockedAt ?: now.also { firstUnlockedAt = it }
        return (since + GRACE_MILLIS - now).coerceAtLeast(0)
    }

    companion object {
        const val GRACE_MILLIS: Long = 30_000L
    }
}
