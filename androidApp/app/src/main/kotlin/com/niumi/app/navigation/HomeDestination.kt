package com.niumi.app.navigation

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.isSessionInProgress

/**
 * Destination de l'accueil. Une session non finale mène **toujours** à l'écran de session active :
 * SPEC_ANDROID §10.4 en fait la seconde garantie d'accès au scan, indépendante de la notification,
 * parce qu'un déverrouillage détruit `AlarmActivity` et ramène l'utilisateur à l'accueil pendant
 * que l'alarme sonne (mesure de l'étape 6). Ce n'est pas un confort de navigation.
 *
 * Fonction pure : elle prend l'état seul et non le snapshot entier, pour être prouvée sans
 * fixture. `NiumiRoute.ActiveSession` n'est pas encore enregistrée dans le graphe — l'écran 7
 * arrive à l'étape 15, et aucune session ne peut être armée avant l'étape 14.
 *
 * Le classement des états finaux vient de `:core:system` (`SESSION_FINAL_STATES`) depuis
 * l'étape 13 : il était redéclaré ici et l'aurait été une troisième fois par `SetupGate`.
 */
fun homeDestinationFor(
    state: SessionStateDto?,
    onboardingAcknowledged: Boolean,
): NiumiRoute =
    when {
        state.isSessionInProgress() -> NiumiRoute.ActiveSession
        onboardingAcknowledged -> NiumiRoute.Readiness
        else -> NiumiRoute.Onboarding
    }
