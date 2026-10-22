package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/**
 * Traduit [ScanRequestNotificationSpecs.awaitingScan] en `Notification` réelle (SPEC_ANDROID §10.5).
 * Même motif que [RingingNotificationFactory]. Deux appelants postent cette notification sous
 * [NiumiNotificationIds.SCAN_REQUEST] : [AndroidScanRequestNotifier] (effet `PRESENT_SCAN_REQUEST`)
 * et le service de garde, qui la tient au premier plan en attente de scan (§12.5, 2026-10-22).
 * Une seule fabrique garantit qu'ils postent exactement le même contenu : sinon, le dernier qui
 * poste remplace l'autre (mesuré le 2026-10-22 sur Xiaomi 25080RABDG et émulateur Android 16).
 *
 * N'utilise que `NotificationManager` côté appelant : fonctionne depuis un
 * `DeviceProtectedStorageContext`, y compris avant le premier déverrouillage (§9.3).
 */
class ScanRequestNotificationFactory(
    private val context: Context,
    private val iconResolver: NotificationIconResolver,
) {
    fun create(tapPendingIntent: PendingIntent?): Notification {
        val spec = ScanRequestNotificationSpecs.awaitingScan()
        val builder =
            Notification
                .Builder(context, spec.channelId)
                // Obligatoire : sans petite icône, la publication échoue silencieusement sur
                // certaines versions (même raison que `RingingNotificationFactory`).
                .setSmallIcon(iconResolver.smallIconResId())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setOngoing(spec.ongoing)
                // `SessionReconciler` republie cette notification à chaque passe sur une session
                // en attente de scan (§10.5, étape 18), et la garde la ramène si elle est écartée :
                // sans cela, un canal d'importance haute reproduirait une bannière alors que la
                // notification n'a jamais disparu. `notify()` avec le même identifiant remplace en place.
                .setOnlyAlertOnce(true)
        // Tenue au premier plan par la garde : sans cela, Android peut retarder son affichage
        // d'environ dix secondes (mesuré le 2026-10-22 sur la notification de sonnerie).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (tapPendingIntent != null) builder.setContentIntent(tapPendingIntent)
        return builder.build()
    }
}
