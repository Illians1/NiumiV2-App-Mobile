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
import com.niumi.feature.session.active.ActiveSessionRoute
import com.niumi.feature.session.active.CancelledScreen
import com.niumi.feature.session.active.ScanToModifyRoute
import com.niumi.feature.session.diagnostics.IncidentDiagnosticRoute
import com.niumi.feature.session.summary.SummaryRoute
import com.niumi.feature.session.wake.WakeTimeRoute
import com.niumi.feature.setup.accessibility.AccessibilityConsentRoute
import com.niumi.feature.setup.apps.AppPickerRoute
import com.niumi.feature.setup.onboarding.OnboardingRoute
import com.niumi.feature.setup.pairing.PairingRoute
import com.niumi.feature.setup.readiness.ReadinessRoute

/**
 * Graphe de navigation de l'application. Les destinations de production sont typées
 * ([NiumiRoute]) ; les routes en chaînes des [NavGraphContributor] cohabitent dans le même
 * `NavHost` pour le seul point d'entrée du POC de debug, supprimé à l'étape 21.
 *
 * Seules les destinations dont l'écran existe sont enregistrées : les autres arrivent avec leur
 * écran à l'étape 17 (voir [NiumiRoute]).
 *
 * [deepLinkDestination] vient du tap d'une notification d'avertissement (§13.1). Elle est empilée
 * **au-dessus** de l'accueil plutôt que substituée à lui comme destination de départ : le bouton
 * Retour doit ramener à l'application, pas la quitter.
 */
@Composable
fun NiumiNavHost(
    contributors: Set<@JvmSuppressWildcards NavGraphContributor>,
    deepLinkDestination: NiumiRoute? = null,
) {
    val navController = rememberNavController()
    val entryPoints = contributors.flatMap { it.entryPoints }

    LaunchedEffect(deepLinkDestination) {
        deepLinkDestination?.let { navController.navigate(it) { launchSingleTop = true } }
    }

    NavHost(navController = navController, startDestination = NiumiRoute.Home) {
        composable<NiumiRoute.Home> {
            HomeRoute(
                entryPoints = entryPoints,
                onNavigate = { route -> navController.navigate(route) },
                onEntryPointClick = { route -> navController.navigate(route) },
                onOpenDiagnostic = { navController.navigate(NiumiRoute.IncidentDiagnostic) },
            )
        }
        setupDestinations(navController)
        composable<NiumiRoute.WakeTime> {
            WakeTimeRoute(
                onContinue = { localTimeIso -> navController.navigate(NiumiRoute.Summary(localTimeIso)) },
            )
        }
        composable<NiumiRoute.Summary> { backStackEntry ->
            SummaryRoute(
                localTimeIso = backStackEntry.toRoute<NiumiRoute.Summary>().localTimeIso,
                onArmed = { navController.navigateToActiveSession() },
                // Revenir à l'écran 5 plutôt qu'en empiler un second.
                onChangeTime = { navController.popBackStack() },
                onSessionInProgress = { navController.navigateToActiveSession() },
            )
        }
        activeSessionDestinations(navController)
        contributors.forEach { it.register(this, navController) }
    }
}

/**
 * Destinations du parcours de préparation (écrans 2, 3 et 4), extraites pour tenir sous
 * `LongMethod` de detekt quand l'écran 12 s'est ajouté au graphe (étape 16). Le choix de l'heure et
 * le récapitulatif restent dans [NiumiNavHost] : ils portent un argument de route.
 */
private fun NavGraphBuilder.setupDestinations(navController: NavHostController) {
    composable<NiumiRoute.Onboarding> {
        OnboardingRoute(
            onContinue = {
                navController.navigate(NiumiRoute.Readiness) {
                    // L'onboarding n'est présenté qu'une fois : y revenir par le bouton
                    // Retour après l'avoir acquitté n'aurait aucun sens.
                    popUpTo(NiumiRoute.Onboarding) { inclusive = true }
                }
            },
        )
    }
    composable<NiumiRoute.Readiness> {
        ReadinessRoute(
            onStartPairing = { navController.navigate(NiumiRoute.Pairing) },
            onOpenAppPicker = { navController.navigate(NiumiRoute.AppPicker) },
            onChooseWakeTime = { navController.navigate(NiumiRoute.WakeTime) },
        )
    }
    composable<NiumiRoute.AccessibilityConsent> { AccessibilityConsentRoute() }
    composable<NiumiRoute.Pairing> {
        PairingRoute(
            onContinue = { navController.popBackStack() },
            onSessionInProgress = { navController.popBackStack() },
        )
    }
    composable<NiumiRoute.AppPicker> {
        AppPickerRoute(
            onConfirmed = { navController.popBackStack() },
            onSessionInProgress = { navController.popBackStack() },
        )
    }
}

/**
 * Destinations d'une session déjà active (écrans 7, 9 et 11, étape 15) et le diagnostic d'incident
 * (écran 12, étape 16), extraites pour tenir sous `LongMethod` de detekt. `NiumiRoute.Completed`
 * n'y est pas : l'écran 10 arrive à l'étape 17.
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
        )
    }
    composable<NiumiRoute.ScanToModify> {
        ScanToModifyRoute(
            onCancelled = { navController.navigateToCancelled() },
        )
    }
    composable<NiumiRoute.Cancelled> {
        CancelledScreen(
            onPrepareAgain = { navController.navigateToPreparation() },
        )
    }
    composable<NiumiRoute.IncidentDiagnostic> { IncidentDiagnosticRoute() }
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

/**
 * « Préparer un nouveau réveil » (écran 11) rejoint le diagnostic, entrée du parcours de
 * préparation quand l'onboarding est acquitté — ce qu'il est nécessairement, une session ayant
 * déjà été armée (voir [homeDestinationFor]). L'écran 11 quitte la pile pour la même raison
 * qu'il y est entré.
 */
private fun NavController.navigateToPreparation() {
    navigate(NiumiRoute.Readiness) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}
