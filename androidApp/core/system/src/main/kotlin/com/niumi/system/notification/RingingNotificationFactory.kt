package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context

/**
 * Traduit [RingingNotificationSpecs.ringing] en `Notification` réelle, avec le full-screen
 * intent fourni par l'appelant (SPEC_ANDROID §10.3). N'ajoute jamais d'action : `spec.actions`
 * est vide et rien dans cette classe n'appelle `addAction`. `fullScreenPendingIntent` peut être
 * nul : cas défensif où le service doit publier une notification de premier plan sans encore
 * connaître de session valide (extras absents, processus recréé sans intent).
 */
class RingingNotificationFactory(
    private val context: Context,
    private val iconResolver: NotificationIconResolver,
) {
    fun create(fullScreenPendingIntent: PendingIntent?): Notification {
        val spec = RingingNotificationSpecs.ringing()
        val builder =
            Notification
                .Builder(context, spec.channelId)
                // Obligatoire : sans petite icône, `startForeground()` échoue avec
                // `CannotPostForegroundServiceNotificationException` et l'alarme ne sonne pas.
                .setSmallIcon(iconResolver.smallIconResId())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setOngoing(spec.ongoing)
        if (fullScreenPendingIntent != null) {
            builder.setFullScreenIntent(fullScreenPendingIntent, spec.hasFullScreenIntent)
        }
        return builder.build()
    }
}
