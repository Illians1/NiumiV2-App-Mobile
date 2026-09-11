package com.niumi.system.apps

/**
 * Taille de la sélection d'applications courante, hors session (SPEC_CORE_KMP §7.4 : 1..50).
 * Le sélecteur et son `AppSelectionStore` arrivent à l'étape 13 ; d'ici là, la seule
 * implémentation liée est [EmptyAppSelectionSource], qui rapporte la vérité du moment — aucune
 * sélection n'existe encore, donc `evaluateActivation` refuse l'activation avec
 * `INVALID_APP_SELECTION`, ce qui est exact et non un raccourci de test.
 */
fun interface AppSelectionSource {
    suspend fun selectedCount(): Int
}

class EmptyAppSelectionSource : AppSelectionSource {
    override suspend fun selectedCount(): Int = 0
}
