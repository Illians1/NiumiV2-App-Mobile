package com.niumi.system.blocking

import android.content.ContentResolver
import android.provider.Settings

/**
 * Lit `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (SPEC_ANDROID §13) — le choix de
 * l'utilisateur — et croise avec la liaison du service dans ce processus (§13.1,
 * [AccessibilityServiceBinding]).
 *
 * `Settings.Secure.ACCESSIBILITY_ENABLED` n'est plus lu (2026-10-15) : cet interrupteur global vaut
 * 1 dès qu'**un** service d'accessibilité tourne, le nôtre ou celui d'une autre application. Il
 * disait « actif » pendant que Niumi, délié après une mort du processus, ne bloquait plus rien —
 * voir [AccessibilityServiceBinding].
 */
class AndroidAccessibilityServiceStatus(
    private val contentResolver: ContentResolver,
    private val expectedComponent: String,
    private val binding: AccessibilityServiceBinding,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = read() == AccessibilityServiceState.ENABLED

    /**
     * Les deux sources sont lues séparément depuis l'étape 25 : Niumi inscrit mais non relié n'est
     * pas « désactivé par l'utilisateur », c'est « pas encore relié » — ou « plus relié », voir
     * [AccessibilityServiceState.PENDING].
     */
    override fun read(): AccessibilityServiceState {
        val raw = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val listed = EnabledAccessibilityServicesParser.isEnabled(raw, expectedComponent)
        return AccessibilityServiceState.of(listed = listed, bound = binding.isBound)
    }
}
