package com.niumi.system.notification

import android.app.NotificationManager
import android.content.Context

/**
 * État d'un canal de notification Niumi (SPEC_ANDROID §13 : contrôle « canal d'alarme actif »).
 * Un canal coupé par l'utilisateur laisse la notification se publier sans erreur mais sans
 * importance : le plein écran ne s'ouvre pas et l'utilisateur n'est pas réveillé.
 */
fun interface NotificationChannelStatus {
    fun isChannelEnabled(channelId: String): Boolean
}

class AndroidNotificationChannelStatus(
    private val context: Context,
) : NotificationChannelStatus {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Un canal absent compte comme inactif : `AndroidNotificationChannelRegistrar.registerAll()`
     * le crée au démarrage du processus, son absence signale donc un état anormal, pas un défaut
     * de réglage.
     */
    override fun isChannelEnabled(channelId: String): Boolean {
        val channel = notificationManager.getNotificationChannel(channelId)
        val groupBlocked =
            channel?.group?.let { notificationManager.getNotificationChannelGroup(it)?.isBlocked } == true
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE && !groupBlocked
    }
}
