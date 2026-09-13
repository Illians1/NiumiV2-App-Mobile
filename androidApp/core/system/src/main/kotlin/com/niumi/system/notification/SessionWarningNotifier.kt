package com.niumi.system.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.readiness.MonitoredReadinessChecks
import com.niumi.system.readiness.ReadinessCheckId

/**
 * Avertissement de dégradation pendant une session armée (SPEC_ANDROID §13.1). [present] et
 * [clear] sont idempotents, comme tous les adaptateurs système (« Interfaces transverses »).
 */
interface SessionWarningNotifier {
    fun present(id: ReadinessCheckId): OperationResult

    fun clear(id: ReadinessCheckId): OperationResult

    /** Retire tous les avertissements : la session n'est plus armée, ils n'ont plus d'objet. */
    fun clearAll(): OperationResult
}

/**
 * Le tap ouvre `MainActivity`, qui redirige vers le diagnostic d'incident — écran 12, livré à
 * l'étape 16 (§13.1). Le `PendingIntentSpec` est construit par
 * [SessionWarningNotificationSpecs.tap], testable en JVM, plutôt qu'ici : `PendingIntent` et
 * `Intent` lèvent `Stub!` hors d'un appareil.
 */
class AndroidSessionWarningNotifier(
    private val context: Context,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
    private val iconResolver: NotificationIconResolver,
) : SessionWarningNotifier {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun present(id: ReadinessCheckId): OperationResult {
        val spec = SessionWarningNotificationSpecs.forCheck(id)
        val notification =
            Notification
                .Builder(context, spec.channelId)
                .setSmallIcon(iconResolver.smallIconResId())
                .setCategory(spec.category)
                .setContentTitle(spec.title)
                .setContentText(spec.text)
                .setStyle(Notification.BigTextStyle().bigText(spec.text))
                .setOngoing(spec.ongoing)
                .setAutoCancel(true)
                .setContentIntent(pendingIntentFactory.create(SessionWarningNotificationSpecs.tap(id)))
                .build()
        notificationManager.notify(SessionWarningNotificationSpecs.notificationId(id), notification)
        return OperationResult.Success
    }

    override fun clear(id: ReadinessCheckId): OperationResult {
        val notificationId = SessionWarningNotificationSpecs.notificationId(id)
        val wasShowing = notificationManager.activeNotifications.any { it.id == notificationId }
        notificationManager.cancel(notificationId)
        return if (wasShowing) OperationResult.Success else OperationResult.AlreadySatisfied
    }

    override fun clearAll(): OperationResult {
        val results = MonitoredReadinessChecks.incidentCodes.keys.map { clear(it) }
        return if (results.any { it is OperationResult.Success }) {
            OperationResult.Success
        } else {
            OperationResult.AlreadySatisfied
        }
    }
}
