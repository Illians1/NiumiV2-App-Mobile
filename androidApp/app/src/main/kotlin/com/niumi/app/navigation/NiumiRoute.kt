package com.niumi.app.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de l'application, typées (`navigation-compose` 2.8+). Centralisées dans `:app`
 * parce que lui seul voit l'ensemble du graphe : les modules `feature` ne peuvent pas dépendre de
 * `:app` (SPEC_ANDROID §6) et exposent donc des lambdas de navigation, jamais des routes.
 *
 * Les treize destinations de §15 sont déclarées ici en une fois, mais **seules celles dont
 * l'écran existe sont enregistrées** dans [NiumiNavHost] : naviguer vers une route non
 * enregistrée lève, ce qui est préférable à un écran vide qui laisserait croire à une
 * fonctionnalité livrée. Les autres sont branchées avec leur écran aux étapes 13 à 17.
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
     * Étape 14. **Seule destination à argument du graphe**, et volontairement : elle transporte le
     * *choix* de l'utilisateur (« 07:00 »), jamais l'horaire calculé qui en dérive. Transmettre un
     * `WakeScheduleDto` rendrait structurellement possible d'armer une session sur un horaire
     * périmé si le fuseau ou la date changent entre les deux écrans ; ne transmettre que l'heure
     * locale rend le recalcul avant activation impossible à contourner (SPEC_CORE_KMP §8.1, §10).
     */
    @Serializable
    data class Summary(
        val localTimeIso: String,
    ) : NiumiRoute

    /** Étape 14 (version minimale), complétée à l'étape 15. */
    @Serializable
    data object ActiveSession : NiumiRoute

    /** Étape 15. */
    @Serializable
    data object ScanToModify : NiumiRoute

    /** Étape 15. */
    @Serializable
    data object Completed : NiumiRoute

    /** Étape 15. */
    @Serializable
    data object Cancelled : NiumiRoute

    /** Étape 16. Cible du tap des notifications d'avertissement de §13.1. */
    @Serializable
    data object IncidentDiagnostic : NiumiRoute
}
