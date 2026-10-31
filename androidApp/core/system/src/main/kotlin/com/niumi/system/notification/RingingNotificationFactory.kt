package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build

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
 * Jamais de plein écran (Lot 9, §4.1) : l'écran n'est pas imposé par-dessus le
 * verrouillage, il s'ouvre au tap. La visibilité publique de la spec est posée sur la notification
 * elle-même, pour que son contenu reste lisible sur l'écran de verrouillage.
 */
class RingingNotificationFactory(
    private val context: Context,
    private val iconResolver: NotificationIconResolver,
) {
    fun create(alarmScreenPendingIntent: PendingIntent?): Notification {
        val spec = RingingNotificationSpecs.ringing()
        val builder =
            Notification
                .Builder(context, spec.channelId)
                // Obligatoire : sans petite icône, `startForeground()` échoue avec
                // `CannotPostForegroundServiceNotificationException` et l'alarme ne sonne pas.
                .setSmallIcon(iconResolver.smallIconResId())
                .setColor(iconResolver.accentColor())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setOngoing(spec.ongoing)
                .setOnlyAlertOnce(spec.alertOnlyOnce)
        // La garde partage cette notification pendant la sonnerie (§12.5, `NiumiNotificationIds`) et
        // peut la poster la première : sans cela, Android en retardait l'affichage d'environ dix
        // secondes (mesuré le 2026-10-22 sur Xiaomi 25080RABDG / Android 16). Le type
        // `mediaPlayback` du service de sonnerie l'en exemptait seul.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (spec.visibilityPublic) builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        if (alarmScreenPendingIntent != null) builder.setContentIntent(alarmScreenPendingIntent)
        return builder.build()
    }
}
