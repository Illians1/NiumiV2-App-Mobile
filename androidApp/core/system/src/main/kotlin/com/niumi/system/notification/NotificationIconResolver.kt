package com.niumi.system.notification

import android.app.Notification
import androidx.annotation.ColorInt

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

    /**
     * Couleur de la notification : depuis Android 12, celle du rond derrière la petite icône dans
     * le volet ; avant, la teinte de l'icône. [Notification.COLOR_DEFAULT] laisse le système la
     * choisir (tirée du fond d'écran).
     */
    @ColorInt
    fun accentColor(): Int = Notification.COLOR_DEFAULT
}
