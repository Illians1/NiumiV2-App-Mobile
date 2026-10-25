package com.niumi.app.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de l'application, typées (`navigation-compose` 2.8+). Centralisées dans `:app`
 * parce que lui seul voit l'ensemble du graphe : les modules `feature` ne peuvent pas dépendre de
 * `:app` (SPEC_ANDROID §6) et exposent donc des lambdas de navigation, jamais des routes.
 *
 * Toutes les destinations sont enregistrées dans [NiumiNavHost] depuis l'étape 21. L'étape 40 a retiré
 * `Ringtone` (la feuille Sonnerie s'ouvre par-dessus l'écran qui la présente), l'étape 45 `WakeTime`,
 * `Summary` et `ActiveSession` (écrans 5, 6 et 7 de l'ancien parcours, remplacés par l'accueil). La règle qui a
 * présidé à leur arrivée reste valable pour
 * toute destination future : ne l'enregistrer qu'avec son écran, puisque naviguer vers une route
 * non enregistrée lève — ce qui est préférable à un écran vide qui laisserait croire à une
 * fonctionnalité livrée.
 */
sealed interface NiumiRoute {
    @Serializable
    data object Home : NiumiRoute

    /** Écran 1 · Présentation (étape 35 de la refonte), remplace l'onboarding des limites. */
    @Serializable
    data object Presentation : NiumiRoute

    /**
     * Écran 2 · Autorisations (étape 36 de la refonte), remplace le diagnostic à quatorze lignes
     * dans le parcours de mise en route.
     */
    @Serializable
    data object Permissions : NiumiRoute

    /** Étape 13. */
    @Serializable
    data object Pairing : NiumiRoute

    /**
     * Écran 4 · Choix des applications (étape 13, refait à l'étape 37 de la refonte). [editing] : rouvert
     * depuis l'accueil (étape 45, M11), avec une flèche de retour ; « Continuer » y ramène.
     */
    @Serializable
    data class AppSelection(
        val editing: Boolean = false,
    ) : NiumiRoute

    /**
     * Réglages transitoires (étapes 45 à 48, décision de Mehdi du 2026-10-25) : Niumi Spots,
     * diagnostic et aide, en attendant l'écran 12 de l'étape 48.
     */
    @Serializable
    data object TransitionalSettings : NiumiRoute

    /** Étape 15. */
    @Serializable
    data object ScanToModify : NiumiRoute

    /** Écran 10, étape 17. */
    @Serializable
    data object Completed : NiumiRoute

    /** Étape 15. */
    @Serializable
    data object Cancelled : NiumiRoute

    /** Étape 16. Cible du tap des notifications d'avertissement de §13.1. */
    @Serializable
    data object IncidentDiagnostic : NiumiRoute

    /** Écran 13, étape 21. Consultation seule, atteignable depuis l'accueil. */
    @Serializable
    data object Help : NiumiRoute
}
