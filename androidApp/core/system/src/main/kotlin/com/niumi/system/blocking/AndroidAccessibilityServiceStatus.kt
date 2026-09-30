package com.niumi.system.blocking

import android.content.ContentResolver
import android.provider.Settings

/**
 * Lit `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (SPEC_ANDROID §13). Un contrôle
 * `Settings.Secure.ACCESSIBILITY_ENABLED` à 0 signifie qu'aucun service d'accessibilité n'est
 * actif sur l'appareil, indépendamment du contenu de la liste — un utilisateur peut avoir
 * coché puis globalement désactivé l'accessibilité.
 */
class AndroidAccessibilityServiceStatus(
    private val contentResolver: ContentResolver,
    private val expectedComponent: String,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = read() == AccessibilityServiceState.ENABLED

    /**
     * Les deux réglages sont lus séparément depuis l'étape 25 : Niumi inscrit mais
     * `accessibility_enabled` à 0 n'est pas « désactivé par l'utilisateur », c'est « pas encore
     * relié » — voir [AccessibilityServiceState.PENDING].
     */
    override fun read(): AccessibilityServiceState {
        val globallyEnabled =
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        val raw = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val listed = EnabledAccessibilityServicesParser.isEnabled(raw, expectedComponent)
        return AccessibilityServiceState.of(globallyEnabled, listed)
    }
}
