package com.niumi.core.diagnostics

/**
 * Bornes communes du nombre de Niumi Spots enregistrés (SPEC_CORE_KMP §2 décision 16, §9.2), au
 * même titre que les bornes 1–50 de `AppSelectionSummary` : une liste fermée, jamais réécrite par
 * une plateforme. Contrat 1.4 (2026-10-13) : de un à cinq ; contrat 1.5 (décision du 2026-10-19,
 * étape 42 du plan de refonte) : de un à dix.
 */
public object PairedBoxLimit {
    public const val MIN: Int = 1
    public const val MAX: Int = 10
}
