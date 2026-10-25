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
 * session : le texte se passe alors de l'heure plutôt que d'en inventer une. Un réveil armé des jours
 * à l'avance dit son jour (décision de Mehdi du 2026-10-25 : « Réveil dimanche à 0:30 »).
 */
object BlockingGuardNotificationSpecs {
    /** Session armée, blocage différé pas encore appliqué. */
    fun beforeBlocking(
        wake: GuardMoment?,
        blocking: GuardMoment?,
    ): NotificationSpec =
        spec(
            title = wake?.let { "Réveil${it.dayPart} à ${it.time}" } ?: "Réveil programmé",
            text =
                blocking?.let { "Tes applications choisies seront bloquées${it.dayPart} à partir de ${it.time}." }
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
            text = "Tes applications choisies seront bloquées jusqu'au scan de ton Niumi Spot.",
        )

    /** « dimanche » glissé dans la phrase, rien le jour même. */
    private val GuardMoment.dayPart: String get() = day?.let { " $it" }.orEmpty()

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
