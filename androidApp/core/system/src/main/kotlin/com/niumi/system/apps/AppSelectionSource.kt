package com.niumi.system.apps

/**
 * Taille de la sélection d'applications courante, hors session (SPEC_CORE_KMP §7.4 : 1..50).
 * Vue étroite dont le diagnostic de §13 a seul besoin : il compte, il ne lit jamais la liste.
 * Implémentée depuis l'étape 13 par [AppSelectionStore], qui la persiste.
 */
fun interface AppSelectionSource {
    suspend fun selectedCount(): Int
}
