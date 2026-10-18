package com.niumi.system.notification

import android.app.NotificationManager
import android.content.Context

/**
 * Sondes de disponibilité des notifications (SPEC_ANDROID §7.1 `SessionRuntimeStatus`, §13.1
 * surveillance pendant une session armée).
 */
interface NotificationAvailability {
    fun areNotificationsEnabled(): Boolean
}

class AndroidNotificationAvailability(
    private val context: Context,
) : NotificationAvailability {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun areNotificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()
}
