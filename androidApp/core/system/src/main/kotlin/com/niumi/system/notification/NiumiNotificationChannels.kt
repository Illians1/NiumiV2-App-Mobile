package com.niumi.system.notification

import android.app.NotificationManager

/**
 * Les trois canaux de notification de Niumi (SPEC_ANDROID §10.3, §10.5, §13.1).
 * `sessionAwaitingScan` est créé dès l'étape 3 même si sa notification n'est publiée qu'à
 * partir de l'étape 17 (`PRESENT_SCAN_REQUEST`) : créer un canal n'exige pas que l'événement
 * qui le déclenche existe déjà. `sessionWarning` suit la même logique depuis l'étape 12.
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

    /**
     * Avertissement de dégradation pendant une session armée (§13.1) : importance haute pour
     * être vu, mais **sans son ni vibration** — il ne doit jamais être confondu avec le réveil
     * lui-même, qui n'a pas encore sonné.
     */
    val sessionWarning =
        NotificationChannelSpec(
            id = "niumi_session_warning",
            importance = NotificationManager.IMPORTANCE_HIGH,
            hasSound = false,
            hasVibration = false,
            visibilityPublic = true,
        )

    val all = listOf(alarmRinging, sessionAwaitingScan, sessionWarning)
}
