package com.niumi.system.apps

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Applique les exclusions de SPEC_ANDROID §12.1 à ce que [PackageQuery] rapporte : package Niumi,
 * détenteurs du rôle Home, Réglages, interface système, composeur. « Toute application sans
 * activité de lancement » n'a pas besoin d'être filtrée ici — la requête
 * `ACTION_MAIN` + `CATEGORY_LAUNCHER` ne peut en rapporter aucune.
 *
 * **Limite assumée** : l'application d'urgence n'a aucune API publique permettant de l'identifier
 * (`RoleManager.ROLE_EMERGENCY` n'est lisible que par le système). Elle n'est exclue que dans la
 * mesure où elle coïncide avec le composeur par défaut, ce qui est le cas sur AOSP et sur la
 * plupart des surcouches. Écart consigné à l'étape 13.
 */
class PackageManagerInstalledAppsSource(
    private val packageQuery: PackageQuery,
    private val niumiPackageName: String,
    private val ioDispatcher: CoroutineDispatcher,
) : InstalledAppsSource {
    override suspend fun launchableApps(): List<InstalledApp> =
        withContext(ioDispatcher) {
            val excluded = excludedPackages()
            packageQuery
                .launcherEntries()
                .distinctBy { it.packageName }
                .filterNot { it.packageName in excluded }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }

    private fun excludedPackages(): Set<String> =
        buildSet {
            add(niumiPackageName)
            add(SYSTEM_UI_PACKAGE)
            addAll(packageQuery.homePackages())
            packageQuery.settingsPackage()?.let(::add)
            addAll(packageQuery.defaultDialerPackages())
        }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
