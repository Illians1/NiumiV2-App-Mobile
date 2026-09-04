package com.niumi.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/** Un point d'entrée affiché sur l'accueil, menant vers une route contribuée. */
data class NavEntryPoint(
    val route: String,
    val label: String,
)

/**
 * Point d'extension de la navigation : un module `feature` déclare ses routes sans que `:app`
 * ait besoin de le connaître (SPEC_ANDROID §6 : `:app` porte le manifeste final et
 * l'assemblage, pas la logique des features). En release, aucun contributeur n'est lié : le
 * NavHost ne contient que l'accueil. La route de debug (`PocNavigation`) est le seul
 * contributeur avant que les vrais écrans `feature` n'existent (étapes suivantes).
 */
interface NavGraphContributor {
    val entryPoints: List<NavEntryPoint>

    fun register(
        builder: NavGraphBuilder,
        navController: NavHostController,
    )
}
