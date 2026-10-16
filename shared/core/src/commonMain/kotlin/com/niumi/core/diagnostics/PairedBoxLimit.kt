package com.niumi.core.diagnostics

/**
 * Bornes communes du nombre de boîtiers associés (SPEC_CORE_KMP §2 décision 16, §9.2, contrat
 * 1.4), au même titre que les bornes 1–50 de `AppSelectionSummary` : une liste fermée, décidée
 * avec l'utilisateur le 2026-10-13, jamais réécrite par une plateforme.
 */
public object PairedBoxLimit {
    public const val MIN: Int = 1
    public const val MAX: Int = 5
}
