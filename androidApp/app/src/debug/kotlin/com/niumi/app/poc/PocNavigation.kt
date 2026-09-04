package com.niumi.app.poc

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.niumi.app.navigation.NavEntryPoint
import com.niumi.app.navigation.NavGraphContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

private const val POC_ROUTE = "poc"

/**
 * Seul contributeur de navigation existant à cette étape (route de pilotage du POC, debug
 * uniquement — CLAUDE.md, SPEC_ANDROID §22 Lot 0). Absent de `release` : ce fichier entier vit
 * dans `src/debug`.
 */
class PocNavGraphContributor
    @Inject
    constructor() : NavGraphContributor {
        override val entryPoints = listOf(NavEntryPoint(route = POC_ROUTE, label = "POC alarme (debug)"))

        override fun register(
            builder: NavGraphBuilder,
            navController: NavHostController,
        ) {
            builder.composable(POC_ROUTE) { PocScreen() }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
interface PocNavigationModule {
    @Binds
    @IntoSet
    fun bindPocNavGraphContributor(impl: PocNavGraphContributor): NavGraphContributor
}
