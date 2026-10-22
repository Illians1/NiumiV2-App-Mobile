package com.niumi.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.niumi.app.help.HelpScreen
import com.niumi.feature.session.active.ActiveSessionRoute
import com.niumi.feature.session.active.CancelledScreen
import com.niumi.feature.session.active.CompletedScreen
import com.niumi.feature.session.active.ScanToModifyRoute
import com.niumi.feature.session.diagnostics.IncidentDiagnosticRoute
import com.niumi.feature.session.ringtone.RingtoneRoute
import com.niumi.feature.session.summary.SummaryRoute
import com.niumi.feature.session.wake.WakeTimeChoice
import com.niumi.feature.session.wake.WakeTimeRoute
import com.niumi.feature.setup.apps.AppPickerRoute
import com.niumi.feature.setup.pairing.PairingRoute
import com.niumi.feature.setup.permissions.PermissionsRoute
import com.niumi.feature.setup.presentation.PresentationRoute

/**
 * Graphe de navigation de l'application. Toutes les destinations sont typées ([NiumiRoute]) : le
 * point d'extension en chaînes qui servait la route POC de debug a disparu avec elle à l'étape 21.
 *
 * Toutes les destinations déclarées sont enregistrées (voir [NiumiRoute]).
 *
 * [deepLinkDestination] vient du tap d'une notification d'avertissement (§13.1). Elle est empilée
 * **au-dessus** de l'accueil plutôt que substituée à lui comme destination de départ : le bouton
 * Retour doit ramener à l'application, pas la quitter.
 */
@Composable
fun NiumiNavHost(deepLinkDestination: NiumiRoute? = null) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkDestination) {
        deepLinkDestination?.let { navController.navigate(it) { launchSingleTop = true } }
    }

    NavHost(navController = navController, startDestination = NiumiRoute.Home) {
        composable<NiumiRoute.Home> {
            HomeRoute(
                onNavigate = { route -> navController.navigate(route) },
                onOpenDiagnostic = { navController.navigate(NiumiRoute.IncidentDiagnostic) },
                onOpenHelp = { navController.navigate(NiumiRoute.Help) },
                // Écran 1 à la place de l'accueil : le retour système sur le panneau 1 quitte
                // l'app au lieu de ramener à un accueil qui la rouvrirait.
                onOpenPresentation = {
                    navController.navigate(NiumiRoute.Presentation) {
                        popUpTo(NiumiRoute.Home) { inclusive = true }
                    }
                },
            )
        }
        setupDestinations(navController)
        composable<NiumiRoute.WakeTime> {
            WakeTimeRoute(
                onContinue = { choice ->
                    navController.navigate(NiumiRoute.Summary(choice.localTimeIso, choice.blockingLocalTimeIso))
                },
                onOpenRingtone = { navController.navigate(NiumiRoute.Ringtone) },
            )
        }
        composable<NiumiRoute.Summary> { backStackEntry ->
            val route = backStackEntry.toRoute<NiumiRoute.Summary>()
            SummaryRoute(
                choice = WakeTimeChoice(route.localTimeIso, route.blockingLocalTimeIso),
                onArmed = { navController.navigateToActiveSession() },
                // Revenir à l'écran 5 plutôt qu'en empiler un second.
                onChangeTime = { navController.popBackStack() },
                onSessionInProgress = { navController.navigateToActiveSession() },
            )
        }
        composable<NiumiRoute.Ringtone> { RingtoneRoute() }
        activeSessionDestinations(navController)
    }
}

/**
 * Destinations du parcours de préparation (écran 1 · Présentation, écran 2 · Autorisations, écrans 3
 * et 4), extraites pour tenir sous `LongMethod` de detekt. Le choix de l'heure et le récapitulatif
 * restent dans [NiumiNavHost] : ils portent un argument de route.
 *
 * **Navigation transitoire jusqu'à l'étape 45 de la refonte.** Le diagnostic qui servait de point
 * de passage vers l'association, la sélection et l'heure a laissé place à l'écran 2 (étape 36) : le
 * parcours s'enchaîne désormais Autorisations → Association → Applications → Heure → Récapitulatif.
 */
private fun NavGraphBuilder.setupDestinations(navController: NavHostController) {
    composable<NiumiRoute.Presentation> {
        PresentationRoute(
            onConfigure = {
                // La présentation n'est montrée qu'une fois : elle quitte la pile et l'accueil y
                // reprend sa place, sous l'écran 2.
                navController.navigate(NiumiRoute.Home) {
                    popUpTo(NiumiRoute.Presentation) { inclusive = true }
                }
                navController.navigate(NiumiRoute.Permissions)
            },
        )
    }
    composable<NiumiRoute.Permissions> {
        PermissionsRoute(
            onBack = { navController.popBackStack() },
            onContinue = { navController.navigate(NiumiRoute.Pairing) },
        )
    }
    composable<NiumiRoute.Pairing> {
        PairingRoute(
            onContinue = { navController.navigate(NiumiRoute.AppPicker) },
            onSessionInProgress = { navController.popBackStack() },
        )
    }
    composable<NiumiRoute.AppPicker> {
        AppPickerRoute(
            onConfirmed = { navController.navigate(NiumiRoute.WakeTime) },
            onSessionInProgress = { navController.popBackStack() },
        )
    }
}

/**
 * Destinations d'une session déjà active (écrans 7, 9 et 11, étape 15), le diagnostic d'incident
 * (écran 12, étape 16) et la session terminée (écran 10, étape 17), extraites pour tenir sous
 * `LongMethod` de detekt.
 *
 * L'écran 12 est atteignable depuis l'écran 7, depuis l'accueil et depuis le tap d'un
 * avertissement (§13.1). C'est un écran de **consultation** : il n'ajoute aucune action de sortie
 * de session, le scan du boîtier restant le seul chemin (§3, §10.2).
 */
private fun NavGraphBuilder.activeSessionDestinations(navController: NavHostController) {
    composable<NiumiRoute.ActiveSession> {
        ActiveSessionRoute(
            onModifyOrCancel = { navController.navigate(NiumiRoute.ScanToModify) },
            onOpenDiagnostic = { navController.navigate(NiumiRoute.IncidentDiagnostic) },
            onOpenRingtone = { navController.navigate(NiumiRoute.Ringtone) },
        )
    }
    composable<NiumiRoute.ScanToModify> {
        ScanToModifyRoute(
            onCancelled = { navController.navigateToCancelled() },
            onCompleted = { navController.navigateToCompleted() },
        )
    }
    composable<NiumiRoute.Cancelled> {
        CancelledScreen(
            onPrepareAgain = { navController.navigateToPreparation() },
        )
    }
    composable<NiumiRoute.Completed> {
        CompletedScreen(
            onBackHome = { navController.navigateToHome() },
        )
    }
    composable<NiumiRoute.IncidentDiagnostic> { IncidentDiagnosticRoute() }
    composable<NiumiRoute.Help> { HelpScreen() }
}

/**
 * `popUpTo(Home)` non inclusif : le récapitulatif et le choix de l'heure disparaissent de la pile
 * — y revenir par Retour afficherait un bouton « Activer ma session » que
 * `ActivationReducer.onRequested` refuserait systématiquement, soit un faux état au sens §15 —
 * tandis que l'accueil reste dessous. `inclusive = true` viderait la pile et ferait quitter
 * l'application au premier Retour, en perdant la seconde garantie d'accès au scan de §10.4.
 */
private fun NavController.navigateToActiveSession() {
    navigate(NiumiRoute.ActiveSession) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * L'écran de session active et l'écran de scan quittent la pile : la session est terminée, y
 * revenir par Retour afficherait une session annulée comme si elle courait encore (§15, « ne
 * jamais afficher un faux état de fiabilité »). L'accueil reste dessous.
 */
private fun NavController.navigateToCancelled() {
    navigate(NiumiRoute.Cancelled) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}

/** Même sortie que [navigateToCancelled], vers l'écran 10 : la session s'est terminée, pas annulée. */
private fun NavController.navigateToCompleted() {
    navigate(NiumiRoute.Completed) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * « Revenir à l'accueil » (écran 10). L'écran quitte la pile pour la même raison que l'écran 11 :
 * y revenir par Retour présenterait une session terminée comme si elle courait encore (§15).
 */
private fun NavController.navigateToHome() {
    navigate(NiumiRoute.Home) {
        popUpTo(NiumiRoute.Home) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * « Préparer un nouveau réveil » (écran 11) rejoint l'écran 2, entrée du parcours de
 * préparation quand la présentation est acquittée — ce qu'il est nécessairement, une session ayant
 * déjà été armée (voir [homeDestinationFor]). L'écran 11 quitte la pile pour la même raison
 * qu'il y est entré.
 */
private fun NavController.navigateToPreparation() {
    navigate(NiumiRoute.Permissions) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}
