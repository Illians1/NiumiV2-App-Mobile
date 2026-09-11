package com.niumi.system.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory

private const val SCAN_REQUEST_NOTIFICATION_ID = 2

/**
 * Publie/retire la notification d'attente de scan (SPEC_ANDROID §10.5). N'utilise que
 * `NotificationManager`, un service système accessible sans Room ni déverrouillage : fonctionne
 * depuis un `DeviceProtectedStorageContext`, y compris avant le premier déverrouillage
 * (coordinateur Direct Boot, §9.3).
 */
class AndroidScanRequestNotifier(
    private val context: Context,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
    private val iconResolver: NotificationIconResolver,
) : ScanRequestNotifier {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun present(sessionId: String): OperationResult {
        val spec = ScanRequestNotificationSpecs.awaitingScan()
        val tapPendingIntent = pendingIntentFactory.create(ScanRequestPendingIntentSpecs.tap(sessionId))
        val notification =
            Notification
                .Builder(context, spec.channelId)
                // Obligatoire : sans petite icône, la publication échoue silencieusement sur
                // certaines versions (même raison que `RingingNotificationFactory`).
                .setSmallIcon(iconResolver.smallIconResId())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setOngoing(spec.ongoing)
                .setContentIntent(tapPendingIntent)
                .build()
        notificationManager.notify(SCAN_REQUEST_NOTIFICATION_ID, notification)
        return OperationResult.Success
    }

    override fun clear(sessionId: String): OperationResult {
        val wasShowing = notificationManager.activeNotifications.any { it.id == SCAN_REQUEST_NOTIFICATION_ID }
        notificationManager.cancel(SCAN_REQUEST_NOTIFICATION_ID)
        return if (wasShowing) OperationResult.Success else OperationResult.AlreadySatisfied
    }
}
