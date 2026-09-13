package com.niumi.system.notification

/** Ce que la surveillance doit faire de la notification de sonnerie à un instant donné. */
enum class RingingNotificationAction {
    NONE,
    REPUBLISH_WITH_FULL_SCREEN,
    REPUBLISH_SILENTLY,
}

/**
 * Garantit que l'écran de réveil reste atteignable tant que la session sonne (SPEC_ANDROID §10.2).
 * Le Reader Mode NFC ne vit que dans une activité au premier plan (§11.2) : sans écran, plus aucun
 * moyen de terminer la session, et Niumi n'offre aucun bouton d'arrêt (§3).
 *
 * **La décision porte sur l'état de l'appareil, pas sur un rang d'occurrence.** §10.2 impose de
 * republier avec le `fullScreenIntent`, §10.4 interdit de ramener l'écran « de force en boucle » :
 * les deux ne se contredisent que si l'on ignore *qui* a fait disparaître la notification.
 *
 * - **Appareil verrouillé ou écran éteint** : l'utilisateur dort, c'est le scénario même pour
 *   lequel §10.2 existe. On republie **avec** le plein écran, qui est le seul mécanisme autorisé
 *   par Android pour rouvrir une activité depuis l'arrière-plan.
 * - **Appareil déverrouillé et en cours d'usage** : la notification a été écartée délibérément.
 *   On republie **sans** plein écran, avec le seul `contentIntent` : l'accès au scan est préservé,
 *   l'écran n'est pas imposé.
 *
 * Une garde « plein écran une seule fois » avait été retenue d'abord ; elle a été remplacée à
 * l'étape 17 après mesure sur appareil. Elle pouvait être **consommée par une absence transitoire**
 * de `getActiveNotifications()`, si bien que la première disparition réellement subie — celle qui
 * compte, pendant le sommeil — n'obtenait plus que la republication silencieuse. Le critère
 * ci-dessus n'a pas d'état à épuiser.
 *
 * Android n'honore de toute façon un `fullScreenIntent` que si l'appareil est verrouillé ou
 * l'écran éteint ; ailleurs il le dégrade en notification « heads-up ». La règle suit donc le
 * comportement réel du système au lieu de le contrarier.
 */
object RingingNotificationWatch {
    /** §10.2 ne fixe aucune période. Dix secondes : au pire dix secondes sans chemin de retour
     * vers le scan, pour six lectures par minute sur un service qui joue déjà de l'audio. */
    const val CHECK_INTERVAL_MS = 10_000L

    fun decide(
        isPosted: Boolean,
        deviceInUse: Boolean,
    ): RingingNotificationAction =
        when {
            isPosted -> RingingNotificationAction.NONE
            deviceInUse -> RingingNotificationAction.REPUBLISH_SILENTLY
            else -> RingingNotificationAction.REPUBLISH_WITH_FULL_SCREEN
        }
}
