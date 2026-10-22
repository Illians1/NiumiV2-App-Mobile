package com.niumi.system.notification

import android.app.NotificationManager
import android.content.Context
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory

/**
 * Publie/retire la notification d'attente de scan (SPEC_ANDROID §10.5). N'utilise que
 * `NotificationManager`, un service système accessible sans Room ni déverrouillage : fonctionne
 * depuis un `DeviceProtectedStorageContext`, y compris avant le premier déverrouillage
 * (coordinateur Direct Boot, §9.3).
 */
class AndroidScanRequestNotifier(
    private val context: Context,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
    iconResolver: NotificationIconResolver,
) : ScanRequestNotifier {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationFactory = ScanRequestNotificationFactory(context, iconResolver)

    override fun present(sessionId: String): OperationResult {
        val tapPendingIntent = pendingIntentFactory.create(ScanRequestPendingIntentSpecs.tap(sessionId))
        notificationManager.notify(NiumiNotificationIds.SCAN_REQUEST, notificationFactory.create(tapPendingIntent))
        return OperationResult.Success
    }

    /**
     * Pendant que la garde tient cette notification au premier plan (§12.5), Android refuse de la
     * retirer à la demande de l'app (`cancelNotificationWithTag`, `FLAG_FOREGROUND_SERVICE`) : elle
     * disparaît avec la garde, à l'état final, moins d'une seconde après le scan.
     */
    override fun clear(sessionId: String): OperationResult {
        val wasShowing = notificationManager.activeNotifications.any { it.id == NiumiNotificationIds.SCAN_REQUEST }
        notificationManager.cancel(NiumiNotificationIds.SCAN_REQUEST)
        return if (wasShowing) OperationResult.Success else OperationResult.AlreadySatisfied
    }
}
