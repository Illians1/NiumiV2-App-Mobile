package com.niumi.system.notification

/**
 * Description pure d'une notification, sans dépendre de `Notification.Builder`
 * (SPEC_ANDROID §10.3, §10.5). `category` est la constante `Notification.CATEGORY_ALARM`,
 * inlinée à la compilation. `actions` est volontairement vide dans tout le parcours de
 * sonnerie : aucune action d'arrêt nulle part (SPEC_ANDROID §3, §10.2, §10.4).
 *
 * Aucune notification de Niumi ne porte de plein écran depuis le Lot 9 (§4.1) : le champ qui le
 * décrivait est retiré. [visibilityPublic] force la visibilité publique de la
 * notification elle-même, au-delà de celle de son canal : seule la sonnerie l'exige (§10.3).
 * [alertOnlyOnce] : une notification déjà affichée n'est pas re-présentée quand elle est republiée
 * — la sonnerie l'exige, rejouée chaque minute par l'alarme de secours (§10.2, §10.3).
 */
data class NotificationSpec(
    val channelId: String,
    val category: String,
    val title: String,
    val text: String,
    val ongoing: Boolean,
    val visibilityPublic: Boolean = false,
    val alertOnlyOnce: Boolean = false,
    val actions: List<String> = emptyList(),
)
