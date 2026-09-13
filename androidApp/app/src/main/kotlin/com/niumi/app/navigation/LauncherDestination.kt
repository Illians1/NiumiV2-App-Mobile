package com.niumi.app.navigation

import com.niumi.core.interop.SessionStateDto

/** Où mène l'ouverture de Niumi depuis le lanceur (SPEC_ANDROID §10.4). */
sealed interface LauncherDestination {
    /** Une destination du `NavHost`, décidée par [homeDestinationFor]. */
    data class InApp(
        val route: NiumiRoute,
    ) : LauncherDestination

    /** L'écran 8, qui vit dans une activité séparée hors du `NavHost`. */
    data object AlarmScreen : LauncherDestination
}

/**
 * §10.4 : « Ouvrir Niumi depuis le lanceur pendant une session active mène toujours à l'écran
 * correspondant à l'état, jamais à l'accueil : c'est la seconde garantie d'accès au scan,
 * indépendante de la notification. »
 *
 * Jusqu'à l'étape 17, tout état non final menait à l'écran 7. C'était un cul-de-sac pendant la
 * sonnerie : le Reader Mode NFC n'existe que dans une activité au premier plan (§11.2), et
 * `AlarmActivity` est la seule à l'activer. Un utilisateur qui ouvrait Niumi après un
 * déverrouillage — le cas mesuré à l'étape 6, où l'activité de réveil est détruite — n'avait donc
 * plus aucun moyen de terminer sa session.
 *
 * [homeDestinationFor] n'est pas réécrite ici, elle est appelée : une seule définition de la règle
 * « session en cours → écran 7 ».
 */
fun launcherDestinationFor(
    state: SessionStateDto?,
    onboardingAcknowledged: Boolean,
): LauncherDestination =
    if (requiresAlarmScreen(state)) {
        LauncherDestination.AlarmScreen
    } else {
        LauncherDestination.InApp(homeDestinationFor(state, onboardingAcknowledged))
    }

/**
 * Les quatre états qui attendent un scan. Tant que l'un d'eux dure, **aucun autre écran de Niumi
 * n'est atteignable** : `MainActivity` ouvre l'écran de réveil à chaque `onResume`, quelle que soit
 * la destination du `NavHost`.
 *
 * La règle a d'abord été posée sur l'accueil seul ; c'était faux, et mesuré sur appareil à
 * l'étape 17 : après l'armement, le `NavHost` est sur `ActiveSession`, donc l'accueil n'est plus
 * composé et sa redirection ne s'exécutait jamais. La règle est globale, sa mise en œuvre doit
 * l'être aussi.
 */
fun requiresAlarmScreen(state: SessionStateDto?): Boolean =
    when (state) {
        SessionStateDto.RINGING,
        SessionStateDto.AWAITING_NFC,
        SessionStateDto.TRIGGERED_AWAITING_NFC,
        SessionStateDto.RELEASING,
        -> true

        // Avant la sonnerie comme après la session, l'écran 7 ou le parcours de préparation.
        SessionStateDto.PREPARING,
        SessionStateDto.ARMED,
        SessionStateDto.COMPLETED,
        SessionStateDto.CANCELLED,
        SessionStateDto.FAILED,
        null,
        -> false
    }
