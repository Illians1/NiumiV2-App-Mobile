package com.niumi.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
 * écran aux étapes 13 à 17 (voir [NiumiRoute]).
 */
@Composable
fun NiumiNavHost(contributors: Set<@JvmSuppressWildcards NavGraphContributor>) {
    val navController = rememberNavController()
    val entryPoints = contributors.flatMap { it.entryPoints }

    NavHost(navController = navController, startDestination = NiumiRoute.Home) {
        composable<NiumiRoute.Home> {
            HomeRoute(
                entryPoints = entryPoints,
                onNavigate = { route -> navController.navigate(route) },
                onEntryPointClick = { route -> navController.navigate(route) },
            )
        }
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
        contributors.forEach { it.register(this, navController) }
    }
}
