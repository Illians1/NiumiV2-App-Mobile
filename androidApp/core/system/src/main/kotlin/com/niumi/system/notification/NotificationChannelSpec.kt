package com.niumi.system.notification

/**
 * Description pure d'un canal de notification, sans dépendre de `NotificationChannel`
 * (SPEC_ANDROID §10.3, §10.5). `importance` est la constante entière
 * `NotificationManager.IMPORTANCE_HIGH`, inlinée à la compilation.
 */
data class NotificationChannelSpec(
    val id: String,
    val importance: Int,
    val hasSound: Boolean,
    val hasVibration: Boolean,
    val visibilityPublic: Boolean,
)
