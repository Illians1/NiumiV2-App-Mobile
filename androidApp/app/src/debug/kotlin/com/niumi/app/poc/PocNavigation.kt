package com.niumi.app.poc

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.niumi.app.navigation.NavEntryPoint
import com.niumi.app.navigation.NavGraphContributor
import com.niumi.feature.setup.accessibility.AccessibilityConsentRoute
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

private const val POC_ROUTE = "poc"
private const val ACCESSIBILITY_CONSENT_ROUTE = "poc/accessibility"

/**
 * Seul contributeur de navigation existant à cette étape (route de pilotage du POC, debug
 * uniquement — CLAUDE.md, SPEC_ANDROID §22 Lot 0). Absent de `release` : ce fichier entier vit
 * dans `src/debug`. La route d'accessibilité ouvre l'écran de consentement réel de
 * `:feature:setup` (étape 5) : rien n'y est simulé, contrairement au reste du POC.
 */
class PocNavGraphContributor
    @Inject
    constructor() : NavGraphContributor {
        override val entryPoints = listOf(NavEntryPoint(route = POC_ROUTE, label = "POC alarme (debug)"))

        override fun register(
            builder: NavGraphBuilder,
            navController: NavHostController,
        ) {
            builder.composable(POC_ROUTE) {
                PocScreen(onOpenAccessibilityConsent = { navController.navigate(ACCESSIBILITY_CONSENT_ROUTE) })
            }
            builder.composable(ACCESSIBILITY_CONSENT_ROUTE) { AccessibilityConsentRoute() }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
interface PocNavigationModule {
    @Binds
    @IntoSet
    fun bindPocNavGraphContributor(impl: PocNavGraphContributor): NavGraphContributor
}
