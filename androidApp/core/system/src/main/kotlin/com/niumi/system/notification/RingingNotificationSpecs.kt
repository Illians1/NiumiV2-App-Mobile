package com.niumi.system.notification

import android.app.Notification

/**
 * Spec de la notification de sonnerie (SPEC_ANDROID §10.3). Textes repris mot pour mot ;
 * `actions` reste vide : aucune action d'arrêt nulle part dans le parcours de sonnerie.
 */
object RingingNotificationSpecs {
    fun ringing(): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.alarmRinging.id,
            category = Notification.CATEGORY_ALARM,
            title = "Alarme Niumi en cours",
            text = "Scanne ton boîtier pour terminer la session.",
            ongoing = true,
            hasFullScreenIntent = true,
            actions = emptyList(),
        )
}
