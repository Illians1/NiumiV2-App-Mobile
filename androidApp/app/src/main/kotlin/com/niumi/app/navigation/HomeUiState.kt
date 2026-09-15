package com.niumi.app.navigation

/**
 * État d'affichage de l'accueil. [destination] est calculée par [homeDestinationFor] : l'accueil
 * ne décide pas lui-même où mener, il applique la règle de SPEC_ANDROID §10.4.
 *
 * [alarmScreenRequired] porte la même règle pour l'écran 8, qui n'est pas une destination du
 * `NavHost` mais une activité : tant que la session attend un scan, **aucun autre écran de Niumi
 * ne doit être atteignable**. L'accueil y redirige à chaque passage au premier plan, et son bouton
 * principal y mène aussi. Quitter Niumi reste possible — c'est l'écran de réveil qui renvoie au
 * lanceur, jamais à l'accueil.
 *
 * [pendingAlarmScreen] n'est que la garde d'un seul lancement par passage au premier plan : sans
 * elle, chaque recomposition relancerait l'activité.
 *
 * [storageUnreadable] (étape 20) : Niumi ne peut plus lire son état enregistré. **Défaut mesuré sur
 * appareil le 2026-09-15** — l'accueil affichait alors « Aucune session » alors qu'une session était
 * armée et le blocage en place, exactement le « faux état de fiabilité » que §15 interdit. Le
 * signal ne pouvait pas vivre dans [destination] seul : celle-ci n'est lue qu'au clic du bouton
 * principal, jamais pour naviguer d'elle-même.
 */
data class HomeUiState(
    val destination: NiumiRoute = NiumiRoute.Onboarding,
    val hasActiveSession: Boolean = false,
    val alarmScreenRequired: Boolean = false,
    val pendingAlarmScreen: Boolean = false,
    val storageUnreadable: Boolean = false,
)
