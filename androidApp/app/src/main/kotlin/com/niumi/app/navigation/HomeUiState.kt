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
 *
 * [presentationRequired] (étape 35 de la refonte) : la présentation n'a jamais été lue, aucune
 * session n'est en cours et l'état est lisible. L'accueil ouvre alors l'écran 1 de lui-même, à la
 * première ouverture (`SPECIFICATION.md` §4). Faux tant que l'accusé n'est pas lu : une lecture
 * lente ne doit pas faire passer un utilisateur déjà configuré par la présentation.
 */
data class HomeUiState(
    val destination: NiumiRoute = NiumiRoute.Presentation,
    val hasActiveSession: Boolean = false,
    val alarmScreenRequired: Boolean = false,
    val pendingAlarmScreen: Boolean = false,
    val storageUnreadable: Boolean = false,
    val presentationRequired: Boolean = false,
)
