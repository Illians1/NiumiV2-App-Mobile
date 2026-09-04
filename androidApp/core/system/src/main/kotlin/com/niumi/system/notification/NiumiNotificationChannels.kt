package com.niumi.system.notification

import android.app.NotificationManager

/**
 * Les deux canaux de notification du parcours de réveil (SPEC_ANDROID §10.3, §10.5).
 * `sessionAwaitingScan` est créé dès cette étape même si sa notification n'est publiée qu'à
 * partir de l'étape 17 (`PRESENT_SCAN_REQUEST`) : créer un canal n'exige pas que l'événement
 * qui le déclenche existe déjà.
 */
object NiumiNotificationChannels {
    val alarmRinging =
        NotificationChannelSpec(
            id = "niumi_alarm_ringing",
            importance = NotificationManager.IMPORTANCE_HIGH,
            hasSound = false,
            hasVibration = true,
            visibilityPublic = true,
        )

    val sessionAwaitingScan =
        NotificationChannelSpec(
            id = "niumi_session_awaiting_scan",
            importance = NotificationManager.IMPORTANCE_HIGH,
            hasSound = false,
            hasVibration = false,
            visibilityPublic = true,
        )

    val all = listOf(alarmRinging, sessionAwaitingScan)
}
