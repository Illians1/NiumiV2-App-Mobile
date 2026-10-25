package com.niumi.app.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.niumi.app.help.HelpScreen
import com.niumi.feature.session.active.CancelledScreen
import com.niumi.feature.session.active.CompletedScreen
import com.niumi.feature.session.active.ScanToModifyRoute
import com.niumi.feature.session.diagnostics.IncidentDiagnosticRoute
import com.niumi.feature.session.home.HomeRoute
import com.niumi.feature.session.transitional.TransitionalSettingsScreen
import com.niumi.feature.setup.apps.AppSelectionRoute
import com.niumi.feature.setup.pairing.PairingRoute
import com.niumi.feature.setup.permissions.PermissionsRoute
import com.niumi.feature.setup.presentation.PresentationRoute

/**
 * Graphe de navigation de l'application. Toutes les destinations sont typées ([NiumiRoute]).
 *
 * [deepLinkDestination] vient du tap d'une notification d'avertissement (§13.1). Elle est empilée
 * **au-dessus** de l'accueil plutôt que substituée à lui comme destination de départ : le bouton
 * Retour doit ramener à l'application, pas la quitter.
 *
 * Étape 45 : l'accueil (écran 6) est la destination de départ dès que la mise en route est finie ;
 * sinon [HomeEntry] la remplace par l'étape de mise en route où en est l'utilisateur.
 */
@Composable
fun NiumiNavHost(deepLinkDestination: NiumiRoute? = null) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkDestination) {
        deepLinkDestination?.let { navController.navigate(it) { launchSingleTop = true } }
    }

    NavHost(navController = navController, startDestination = NiumiRoute.Home) {
        composable<NiumiRoute.Home> { HomeEntry(navController) }
        setupDestinations(navController)
        sessionDestinations(navController)
    }
}

/**
 * Entrée de l'accueil (étape 45) : tant que la mise en route n'est pas finie, l'accueil cède sa place
 * à la présentation ou à l'écran 2 (`popUpTo` inclusif : le retour quitte l'app au lieu de ramener à
 * un accueil qui les rouvrirait). Un stockage illisible mène au diagnostic d'incident (§15).
 */
@Composable
private fun HomeEntry(
    navController: NavHostController,
    gate: HomeGateViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) gate.refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    when (val destination = gate.destination) {
        null -> {}

        NiumiRoute.Home -> {
            HomeRoute(
                onOpenSettings = { navController.navigate(NiumiRoute.TransitionalSettings) },
                onOpenApps = { navController.navigate(NiumiRoute.AppSelection(editing = true)) },
                // Jusqu'à l'étape 46, l'annulation passe par l'ancien écran de scan.
                onCancelWake = { navController.navigate(NiumiRoute.ScanToModify) },
            )
        }

        else -> {
            LaunchedEffect(destination) {
                navController.navigate(destination) { popUpTo(NiumiRoute.Home) { inclusive = true } }
            }
        }
    }
}

/**
 * Mise en route (écran 1 · Présentation, écran 2 · Autorisations, écrans 3 et 4), extraite pour tenir
 * sous `LongMethod` de detekt. « Continuer » de l'écran 4 ouvre l'accueil, « Réveil désactivé », et
 * vide la pile : la mise en route ne se refait pas par le retour.
 */
private fun NavGraphBuilder.setupDestinations(navController: NavHostController) {
    composable<NiumiRoute.Presentation> {
        PresentationRoute(
            onConfigure = {
                navController.navigate(NiumiRoute.Permissions) {
                    popUpTo(NiumiRoute.Presentation) { inclusive = true }
                }
            },
        )
    }
    composable<NiumiRoute.Permissions> {
        val activity = LocalActivity.current
        PermissionsRoute(
            // Premier écran de la pile pendant la mise en route : son retour quitte l'app.
            onBack = { if (!navController.popBackStack()) activity?.finish() },
            onContinue = { navController.navigate(NiumiRoute.Pairing) },
        )
    }
    composable<NiumiRoute.Pairing> {
        // Ouvert aussi depuis les réglages transitoires : « Continuer » y ramène (relecture de
        // l'étape 45) ; pendant la mise en route, il mène à l'écran 4.
        val fromSettings =
            navController.previousBackStackEntry?.destination?.hasRoute<NiumiRoute.TransitionalSettings>() == true
        PairingRoute(
            onContinue = {
                if (fromSettings) navController.popBackStack() else navController.navigate(NiumiRoute.AppSelection())
            },
        )
    }
    composable<NiumiRoute.AppSelection> { entry ->
        val route = entry.toRoute<NiumiRoute.AppSelection>()
        AppSelectionRoute(
            editing = route.editing,
            onContinue = { navController.navigateToHome(clearStack = true) },
            onBack = { navController.popBackStack() },
            onSessionInProgress = { navController.popBackStack() },
        )
    }
}

/**
 * Destinations ouvertes depuis l'accueil : réglages transitoires (étapes 45 à 48), annulation par
 * scan et ses deux issues (jusqu'à l'étape 46), diagnostic d'incident et aide. Le diagnostic est un
 * écran de **consultation** : il n'ajoute aucune action de sortie de session (§3, §10.2).
 */
private fun NavGraphBuilder.sessionDestinations(navController: NavHostController) {
    composable<NiumiRoute.TransitionalSettings> {
        TransitionalSettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenNiumiSpots = { navController.navigate(NiumiRoute.Pairing) },
            onOpenDiagnostic = { navController.navigate(NiumiRoute.IncidentDiagnostic) },
            onOpenHelp = { navController.navigate(NiumiRoute.Help) },
        )
    }
    composable<NiumiRoute.ScanToModify> {
        ScanToModifyRoute(
            onCancelled = { navController.navigateAfterSession(NiumiRoute.Cancelled) },
            onCompleted = { navController.navigateAfterSession(NiumiRoute.Completed) },
        )
    }
    composable<NiumiRoute.Cancelled> { CancelledScreen(onPrepareAgain = { navController.navigateToHome() }) }
    composable<NiumiRoute.Completed> { CompletedScreen(onBackHome = { navController.navigateToHome() }) }
    composable<NiumiRoute.IncidentDiagnostic> { IncidentDiagnosticRoute() }
    composable<NiumiRoute.Help> { HelpScreen() }
}

/**
 * L'écran de scan quitte la pile : la session est terminée, y revenir par Retour afficherait une
 * session finie comme si elle courait encore (§15, « ne jamais afficher un faux état de
 * fiabilité »). L'accueil reste dessous.
 */
private fun NavController.navigateAfterSession(route: NiumiRoute) {
    navigate(route) {
        popUpTo(NiumiRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * Retour à l'accueil. [clearStack] : fin de la mise en route, toute la pile est remplacée ; sinon
 * l'écran courant quitte la pile pour la même raison que [navigateAfterSession].
 */
private fun NavController.navigateToHome(clearStack: Boolean = false) {
    navigate(NiumiRoute.Home) {
        if (clearStack) popUpTo(graph.id) { inclusive = true } else popUpTo(NiumiRoute.Home) { inclusive = true }
        launchSingleTop = true
    }
}
