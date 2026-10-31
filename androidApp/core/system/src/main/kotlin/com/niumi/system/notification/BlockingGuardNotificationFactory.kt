package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/**
 * Traduit une spec de [BlockingGuardNotificationSpecs] en `Notification` réelle (SPEC_ANDROID §12.5).
 * Même motif que [RingingNotificationFactory] : la spec est pure et testée en JVM, cette classe ne
 * fait que la poser sur un `Notification.Builder`. N'ajoute jamais d'action.
 *
 * [openAppPendingIntent] ouvre `MainActivity`, qui mène à l'écran de l'état de la session ; nul
 * quand le service n'a pas encore relu la session.
 */
class BlockingGuardNotificationFactory(
    private val context: Context,
    private val iconResolver: NotificationIconResolver,
) {
    fun create(
        spec: NotificationSpec,
        openAppPendingIntent: PendingIntent?,
    ): Notification {
        val builder =
            Notification
                .Builder(context, spec.channelId)
                // Obligatoire : sans petite icône, `startForeground()` échoue
                // (`CannotPostForegroundServiceNotificationException`).
                .setSmallIcon(iconResolver.smallIconResId())
                .setColor(iconResolver.accentColor())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setOngoing(spec.ongoing)
                .setOnlyAlertOnce(spec.alertOnlyOnce)
        // Android 12+ peut retarder jusqu'à 10 s l'affichage d'une notification de service de
        // premier plan ; elle doit apparaître dès l'entrée en vigueur du blocage. L'appel n'existe
        // qu'à partir de l'API 31 : sans cette garde, le service plantait sur Android 10 et 11
        // (erreur de lint `NewApi`, corrigée le 2026-10-22).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (spec.visibilityPublic) builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        if (openAppPendingIntent != null) builder.setContentIntent(openAppPendingIntent)
        return builder.build()
    }
}
