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
    override fun isEnabled(): Boolean {
        val globallyEnabled =
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!globallyEnabled) return false
        val raw = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return EnabledAccessibilityServicesParser.isEnabled(raw, expectedComponent)
    }
}
