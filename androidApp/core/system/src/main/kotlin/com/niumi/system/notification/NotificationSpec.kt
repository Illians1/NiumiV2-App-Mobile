package com.niumi.system.notification

/**
 * Description pure d'une notification, sans dépendre de `Notification.Builder`
 * (SPEC_ANDROID §10.3, §10.5). `category` est la constante `Notification.CATEGORY_ALARM`,
 * inlinée à la compilation. `actions` est volontairement vide dans tout le parcours de
 * sonnerie : aucune action d'arrêt nulle part (SPEC_ANDROID §3, §10.2, §10.4).
 */
data class NotificationSpec(
    val channelId: String,
    val category: String,
    val title: String,
    val text: String,
    val ongoing: Boolean,
    val hasFullScreenIntent: Boolean,
    val actions: List<String> = emptyList(),
)
