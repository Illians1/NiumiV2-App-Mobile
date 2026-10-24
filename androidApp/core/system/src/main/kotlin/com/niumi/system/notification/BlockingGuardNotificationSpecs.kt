package com.niumi.system.notification

import android.app.Notification

/**
 * Notification du service de garde de session (SPEC_ANDROID §12.5 ; décisions de Mehdi du
 * 2026-10-19 : notification pendant toute la session ; du 2026-10-22 : elle parle du réveil,
 * avec un texte avant le blocage et un pendant). De l'alarme au scan, la garde n'a plus de texte
 * propre : elle reprend la notification de l'alarme (`NiumiNotificationIds`).
 *
 * Une notification d'état, pas une alarme : `CATEGORY_STATUS`, canal d'importance basse, aucune
 * action. `actions` reste vide comme partout dans Niumi : rien n'arrête une session hors du scan.
 * La visibilité publique la garde lisible sur l'écran de verrouillage ; elle ne dit rien que
 * l'utilisateur n'ait choisi lui-même.
 *
 * Les heures sont nulles quand le service, relancé par le système, n'a pas encore relu la
 * session : le texte se passe alors de l'heure plutôt que d'en inventer une.
 */
object BlockingGuardNotificationSpecs {
    /** Session armée, blocage différé pas encore appliqué. */
    fun beforeBlocking(
        wakeTimeLabel: String?,
        blockingTimeLabel: String?,
    ): NotificationSpec =
        spec(
            title = wakeTimeLabel?.let { "Réveil programmé à $it" } ?: "Réveil programmé",
            text =
                blockingTimeLabel?.let { "Tes applications choisies seront bloquées à partir de $it." }
                    ?: "Tes applications choisies seront bloquées au coucher.",
        )

    /** Blocage en vigueur, du coucher jusqu'à l'alarme. */
    fun blocking(wakeTimeLabel: String?): NotificationSpec =
        spec(
            title = wakeTimeLabel?.let { "Réveil prévu à $it" } ?: "Réveil prévu",
            text = "Tes applications choisies restent bloquées jusqu'au scan de ton Niumi Spot.",
        )

    /**
     * Service relancé par le système sans commande, le temps de relire la session (moins d'une
     * seconde) : la phase est inconnue, le texte ne dit que ce qui est vrai dans les deux.
     */
    fun unknownPhase(): NotificationSpec =
        spec(
            title = "Réveil programmé",
            text = "Ta session Niumi est en cours.",
        )

    private fun spec(
        title: String,
        text: String,
    ): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.blockingGuard.id,
            category = Notification.CATEGORY_STATUS,
            title = title,
            text = text,
            ongoing = true,
            visibilityPublic = true,
            alertOnlyOnce = true,
            actions = emptyList(),
        )
}
