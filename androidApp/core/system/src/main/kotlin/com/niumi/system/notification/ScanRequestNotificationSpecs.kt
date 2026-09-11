package com.niumi.system.notification

import android.app.Notification

/**
 * Spec de la notification d'attente de scan (SPEC_ANDROID §10.5). Textes repris mot pour mot ;
 * `hasFullScreenIntent = false` et `actions` vide : cette notification ne doit jamais rallumer
 * l'écran ni simuler une alarme active, ni porter d'action d'arrêt.
 */
object ScanRequestNotificationSpecs {
    fun awaitingScan(): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.sessionAwaitingScan.id,
            category = Notification.CATEGORY_ALARM,
            title = "Ton réveil Niumi est passé",
            text = "Scanne ton boîtier pour débloquer tes applications.",
            ongoing = true,
            hasFullScreenIntent = false,
            actions = emptyList(),
        )
}
