package com.niumi.system.readiness

import com.niumi.core.interop.ReadinessSeverityDto

/**
 * Résultat d'un contrôle de disponibilité (SPEC_ANDROID §13). Ne porte aucun texte affiché :
 * `:core:system` reste sans interface, les messages vivent dans `:feature:setup`.
 */
data class ReadinessCheck(
    val id: ReadinessCheckId,
    val severity: ReadinessSeverityDto,
    val outcome: ReadinessOutcome,
    val action: ReadinessAction,
)

/**
 * [NOT_APPLICABLE] distingue « contrôle réussi » de « contrôle sans objet sur cet appareil ou à
 * ce stade du parcours » : autorisation plein écran avant Android 14, activation du NFC sur un
 * appareil sans matériel NFC, et validité de l'instant de réveil tant qu'aucune heure n'est
 * choisie. Un contrôle sans objet ne peut ni bloquer ni rassurer : il est exclu de
 * `ActivationPolicyInputDto`.
 */
enum class ReadinessOutcome {
    PASSED,
    FAILED,
    NOT_APPLICABLE,
}
