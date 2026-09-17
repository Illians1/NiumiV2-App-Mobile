package com.niumi.app.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de l'application, typées (`navigation-compose` 2.8+). Centralisées dans `:app`
 * parce que lui seul voit l'ensemble du graphe : les modules `feature` ne peuvent pas dépendre de
 * `:app` (SPEC_ANDROID §6) et exposent donc des lambdas de navigation, jamais des routes.
 *
 * Les quatorze destinations de §15 sont déclarées ici, et toutes sont enregistrées dans
 * [NiumiNavHost] depuis l'étape 21. La règle qui a présidé à leur arrivée reste valable pour
 * toute destination future : ne l'enregistrer qu'avec son écran, puisque naviguer vers une route
 * non enregistrée lève — ce qui est préférable à un écran vide qui laisserait croire à une
 * fonctionnalité livrée.
 */
sealed interface NiumiRoute {
    @Serializable
    data object Home : NiumiRoute

    @Serializable
    data object Onboarding : NiumiRoute

    @Serializable
    data object Readiness : NiumiRoute

    @Serializable
    data object AccessibilityConsent : NiumiRoute

    /** Étape 13. */
    @Serializable
    data object Pairing : NiumiRoute

    /** Étape 13. */
    @Serializable
    data object AppPicker : NiumiRoute

    /** Étape 14. */
    @Serializable
    data object WakeTime : NiumiRoute

    /**
     * Étape 14, étendue par le Lot 6 (étape 24). **Seule destination à arguments du graphe**, et
     * volontairement : elle transporte les *choix* de l'utilisateur (« 07:00 », « 22:30 »), jamais
     * les horaires calculés qui en dérivent. Transmettre un `WakeScheduleDto` rendrait
     * structurellement possible d'armer une session sur un horaire périmé si le fuseau ou la date
     * changent entre les deux écrans ; ne transmettre que les heures locales rend le recalcul avant
     * activation impossible à contourner (SPEC_CORE_KMP §8.1, §8.3, §10).
     *
     * [blockingLocalTimeIso] nul décrit un blocage immédiat, le défaut du produit : l'argument
     * n'apparaît alors pas dans la route.
     */
    @Serializable
    data class Summary(
        val localTimeIso: String,
        val blockingLocalTimeIso: String? = null,
    ) : NiumiRoute

    /** Étape 14 (version minimale), complétée à l'étape 15. */
    @Serializable
    data object ActiveSession : NiumiRoute

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
