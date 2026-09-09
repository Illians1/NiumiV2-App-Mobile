package com.niumi.core.domain

/**
 * Résumé de la sélection d'applications (SPEC_CORE_KMP §7.4). Le coeur commun ne stocke aucun
 * identifiant d'application, uniquement leur nombre ; une activation est refusée si [count] est
 * hors de 1..50 (violation `INVALID_APP_SELECTION`).
 */
public data class AppSelectionSummary(
    val count: Int,
) {
    public companion object {
        // Bornes communes de la sélection (SPEC_CORE_KMP §7.4), extraites à l'étape 8 pour être
        // partagées entre SessionEventValidation (réducteur) et ActivationPolicy (diagnostic
        // avant activation) plutôt que dupliquées : voir ETAPE-08.md.
        public const val MIN_COUNT: Int = 1
        public const val MAX_COUNT: Int = 50
    }
}
