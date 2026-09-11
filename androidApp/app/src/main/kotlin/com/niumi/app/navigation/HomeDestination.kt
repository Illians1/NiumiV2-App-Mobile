package com.niumi.app.navigation

import com.niumi.core.interop.SessionStateDto

/**
 * États finaux de la machine commune (SPEC_CORE_KMP §5). `FINAL_STATES` existe déjà dans
 * `shared/core/.../domain/ReducerSupport.kt`, mais y est `internal` : l'ensemble est redéclaré
 * ici, et `HomeDestinationTest` prouve sur `SessionStateDto.entries` qu'aucun état n'échappe au
 * classement si la machine commune en gagne un.
 */
private val FINAL_STATES =
    setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)

/**
 * Destination de l'accueil. Une session non finale mène **toujours** à l'écran de session active :
 * SPEC_ANDROID §10.4 en fait la seconde garantie d'accès au scan, indépendante de la notification,
 * parce qu'un déverrouillage détruit `AlarmActivity` et ramène l'utilisateur à l'accueil pendant
 * que l'alarme sonne (mesure de l'étape 6). Ce n'est pas un confort de navigation.
 *
 * Fonction pure : elle prend l'état seul et non le snapshot entier, pour être prouvée sans
 * fixture. `NiumiRoute.ActiveSession` n'est pas encore enregistrée dans le graphe — l'écran 7
 * arrive à l'étape 15, et aucune session ne peut être armée avant l'étape 14.
 */
fun homeDestinationFor(
    state: SessionStateDto?,
    onboardingAcknowledged: Boolean,
): NiumiRoute =
    when {
        state != null && state !in FINAL_STATES -> NiumiRoute.ActiveSession
        onboardingAcknowledged -> NiumiRoute.Readiness
        else -> NiumiRoute.Onboarding
    }
