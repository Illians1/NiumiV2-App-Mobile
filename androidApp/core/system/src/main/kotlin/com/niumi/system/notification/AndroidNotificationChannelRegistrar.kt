package com.niumi.system.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Crée les canaux Niumi auprès du système, de façon idempotente (`createNotificationChannel`). */
class AndroidNotificationChannelRegistrar(
    private val context: Context,
) {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun registerAll() {
        NiumiNotificationChannels.all.forEach { spec -> register(spec) }
    }

    private fun register(spec: NotificationChannelSpec) {
        val channel = NotificationChannel(spec.id, spec.id, spec.importance)
        channel.setSound(null, null)
        channel.enableVibration(spec.hasVibration)
        channel.lockscreenVisibility =
            if (spec.visibilityPublic) {
                Notification.VISIBILITY_PUBLIC
            } else {
                Notification.VISIBILITY_PRIVATE
            }
        notificationManager.createNotificationChannel(channel)
    }
}
