package com.niumi.system.notification

import android.app.Notification

/**
 * Spec de la notification d'attente de scan (SPEC_ANDROID §10.5). Textes repris mot pour mot ;
 * `actions` vide : cette notification ne doit jamais simuler une alarme active ni porter d'action
 * d'arrêt. Comme toute notification de Niumi depuis le Lot 9, elle n'a pas de plein écran.
 */
object ScanRequestNotificationSpecs {
    fun awaitingScan(): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.sessionAwaitingScan.id,
            category = Notification.CATEGORY_ALARM,
            title = "Ton réveil Niumi est passé",
            text = "Scanne ton Niumi Spot pour retrouver tes applis.",
            ongoing = true,
            actions = emptyList(),
        )
}
