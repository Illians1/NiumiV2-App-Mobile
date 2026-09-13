package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context

/**
 * Traduit [RingingNotificationSpecs.ringing] en `Notification` réelle, avec l'intent d'ouverture
 * de l'écran de réveil fourni par l'appelant (SPEC_ANDROID §10.3). N'ajoute jamais d'action :
 * `spec.actions` est vide et rien dans cette classe n'appelle `addAction`. Un `contentIntent`
 * n'est pas une action : il n'apparaît pas comme un bouton et ne termine aucune session.
 *
 * [alarmScreenPendingIntent] peut être nul : cas défensif où le service doit publier une
 * notification de premier plan sans encore connaître de session valide (extras absents, processus
 * recréé sans intent).
 *
 * [fullScreen] à `false` produit la republication silencieuse de [RingingNotificationWatch] : la
 * notification revient avec son `contentIntent` — l'accès au scan est préservé (§11.2) — mais sans
 * rouvrir l'écran de force (§10.4).
 */
class RingingNotificationFactory(
    private val context: Context,
    private val iconResolver: NotificationIconResolver,
) {
    fun create(
        alarmScreenPendingIntent: PendingIntent?,
        fullScreen: Boolean = true,
    ): Notification {
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
        if (alarmScreenPendingIntent != null) {
            if (fullScreen) {
                builder.setFullScreenIntent(alarmScreenPendingIntent, spec.hasFullScreenIntent)
            }
            builder.setContentIntent(alarmScreenPendingIntent)
        }
        return builder.build()
    }
}
