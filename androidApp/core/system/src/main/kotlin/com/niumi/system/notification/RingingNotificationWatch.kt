package com.niumi.system.notification

/** Ce que la surveillance doit faire de la notification de sonnerie à un instant donné. */
enum class RingingNotificationAction {
    NONE,
    REPUBLISH,
}

/**
 * Garantit que l'écran de réveil reste atteignable tant que la session sonne (SPEC_ANDROID §10.2).
 * Le Reader Mode NFC ne vit que dans une activité au premier plan (§11.2), et Niumi n'offre aucun
 * bouton d'arrêt (§3) : la notification de sonnerie est le premier des deux chemins vers le scan,
 * l'icône de Niumi le second (§10.4).
 *
 * **Absente, elle revient, quel que soit l'état de l'appareil (Lot 9).** Elle revient avec son seul
 * `contentIntent` : aucun écran n'est imposé. Jusqu'à l'étape 31, un appareil verrouillé obtenait
 * en plus le plein écran ; il est retiré, le scan ne fonctionnant pas sur écran verrouillé (§4.4).
 *
 * La règle n'a aucun état à épuiser : une garde « une seule fois » avait été consommée, à l'étape
 * 17, par une absence transitoire de `getActiveNotifications()`.
 */
object RingingNotificationWatch {
    /** §10.2 : au pire dix secondes sans chemin de retour vers le scan, pour six lectures par
     * minute sur un service qui joue déjà de l'audio. */
    const val CHECK_INTERVAL_MS = 10_000L

    fun decide(isPosted: Boolean): RingingNotificationAction =
        if (isPosted) RingingNotificationAction.NONE else RingingNotificationAction.REPUBLISH
}
