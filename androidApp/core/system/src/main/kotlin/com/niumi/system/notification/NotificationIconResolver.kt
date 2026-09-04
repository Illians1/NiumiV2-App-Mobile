package com.niumi.system.notification

/**
 * Fournit la petite icône des notifications Niumi. `:core:system` ne peut pas référencer le `R`
 * de `:core:designsystem`, propriétaire de l'asset, sans créer une dépendance que SPEC_ANDROID
 * §6 n'accorde qu'aux modules `feature` et `:app` : la résolution est déléguée à `:app`, comme
 * pour [com.niumi.system.intent.NiumiComponentResolver].
 *
 * L'icône n'est pas décorative : sans `setSmallIcon`, le système rejette la notification de
 * `startForeground()` et le service de sonnerie meurt au démarrage.
 */
fun interface NotificationIconResolver {
    fun smallIconResId(): Int
}
