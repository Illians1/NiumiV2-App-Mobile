package com.niumi.system.apps

/**
 * Applications proposables au blocage (SPEC_ANDROID §12.1), exclusions déjà appliquées et liste
 * triée. `suspend` parce que la résolution des libellés et des icônes traverse
 * `PackageManager` : jamais sur le thread principal.
 */
interface InstalledAppsSource {
    suspend fun launchableApps(): List<InstalledApp>
}
