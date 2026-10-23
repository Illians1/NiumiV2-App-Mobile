package com.niumi.system.blocking

/**
 * Retrait de l'écran 7 quand une application non bloquée passe au premier plan (SPEC_ANDROID §12.2,
 * décision de Mehdi du 2026-10-22, étape 39 de la refonte) : un appel entrant, ou toute application
 * qui s'ouvre d'elle-même, ne doit pas rester caché sous l'écran de blocage.
 *
 * Gardent l'écran : les lanceurs (le retour à l'accueil produit aussitôt un événement du lanceur,
 * qui effacerait l'écran avant qu'il soit lu, ETAPE-05.md), l'interface système (volet, barre
 * d'état) et Niumi, dont l'écran 7 est lui-même une fenêtre. Une application bloquée ne passe pas
 * par ici : elle est renvoyée à l'accueil (`BlockAction.GoHome`).
 */
object BlockScreenDismissal {
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    fun dismisses(
        foregroundPackage: String,
        selfPackageName: String,
        homePackages: Set<String>,
    ): Boolean =
        foregroundPackage != selfPackageName &&
            foregroundPackage != SYSTEM_UI_PACKAGE &&
            foregroundPackage !in homePackages
}
