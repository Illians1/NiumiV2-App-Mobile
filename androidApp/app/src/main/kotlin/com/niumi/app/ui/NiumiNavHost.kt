package com.niumi.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.niumi.app.navigation.NavGraphContributor

private const val HOME_ROUTE = "home"

/** Assemble l'accueil et les routes contribuées par les modules `feature` (SPEC_ANDROID §6). */
@Composable
fun NiumiNavHost(contributors: Set<@JvmSuppressWildcards NavGraphContributor>) {
    val navController = rememberNavController()
    val entryPoints = contributors.flatMap { it.entryPoints }

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                entryPoints = entryPoints,
                onEntryPointClick = { route -> navController.navigate(route) },
            )
        }
        contributors.forEach { it.register(this, navController) }
    }
}
