package com.niumi.system.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Sondes de disponibilité des notifications (SPEC_ANDROID §7.1 `SessionRuntimeStatus`, §13.1
 * surveillance pendant une session armée).
 */
interface NotificationAvailability {
    fun areNotificationsEnabled(): Boolean

    fun canUseFullScreenIntent(): Boolean
}

class AndroidNotificationAvailability(
    private val context: Context,
) : NotificationAvailability {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun areNotificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()

    // `canUseFullScreenIntent()` n'existe qu'à partir d'Android 14 (API 34) ; en dessous, aucune
    // autorisation distincte n'existe, le full-screen intent est toujours permis.
    override fun canUseFullScreenIntent(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
}
