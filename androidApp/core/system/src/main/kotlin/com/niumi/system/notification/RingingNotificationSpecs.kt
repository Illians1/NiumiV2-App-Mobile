package com.niumi.system.notification

import android.app.Notification

/**
 * Spec de la notification de sonnerie (SPEC_ANDROID §10.3). Textes repris mot pour mot ;
 * `actions` reste vide : aucune action d'arrêt nulle part dans le parcours de sonnerie.
 *
 * Depuis le Lot 9, c'est elle qui mène à l'écran de réveil — l'écran n'est plus imposé par-dessus
 * le verrouillage. Le texte dit donc le geste, et la visibilité publique garde son contenu lisible
 * sur l'écran de verrouillage même si l'utilisateur masque le contenu sensible.
 *
 * `alertOnlyOnce` : l'alarme de secours fait republier cette notification toutes les 60 s (§10.2).
 * Mesuré le 2026-10-18 : sans ce drapeau, Android re-présentait chaque republication — bannière et
 * vibration de notification à chaque minute, y compris par-dessus l'écran de réveil. Elle alerte à
 * sa première publication et après un balayage, jamais tant qu'elle est déjà affichée (§10.3).
 */
object RingingNotificationSpecs {
    fun ringing(): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.alarmRinging.id,
            category = Notification.CATEGORY_ALARM,
            title = "Alarme Niumi en cours",
            text = "Touche pour scanner ton boîtier et terminer la session.",
            ongoing = true,
            visibilityPublic = true,
            alertOnlyOnce = true,
            actions = emptyList(),
        )
}
