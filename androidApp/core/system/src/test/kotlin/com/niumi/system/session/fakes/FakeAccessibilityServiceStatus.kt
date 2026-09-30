package com.niumi.system.session.fakes

import com.niumi.system.blocking.AccessibilityServiceState
import com.niumi.system.blocking.AccessibilityServiceStatus

/**
 * [state] nul : l'état est dérivé de [enabled], comme avant l'étape 25 — `enabled = false` vaut
 * « retiré de la liste » ([AccessibilityServiceState.DISABLED]). Les tests de la fenêtre de liaison
 * après déverrouillage posent [state] explicitement à [AccessibilityServiceState.PENDING].
 */
class FakeAccessibilityServiceStatus(
    var enabled: Boolean = true,
    var state: AccessibilityServiceState? = null,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = read() == AccessibilityServiceState.ENABLED

    override fun read(): AccessibilityServiceState =
        state ?: if (enabled) AccessibilityServiceState.ENABLED else AccessibilityServiceState.DISABLED
}
