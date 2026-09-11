package com.niumi.system.apps

/**
 * Ce que `PackageManager` doit fournir pour bâtir le sélecteur de SPEC_ANDROID §12.1. Même motif
 * que [NdefRecordData][com.niumi.system.nfc.NdefRecordData] au paquet `nfc` : la traduction
 * Android non testable vit dans `PackageManagerPackageQuery`, la logique (dédoublonnage,
 * exclusions, tri) reste pure dans [PackageManagerInstalledAppsSource] et se vérifie en JVM.
 *
 * Aucune méthode ne passe par `RoleManager` : `getRoleHolders()` est `@SystemApi` et exige la
 * permission `MANAGE_ROLE_HOLDERS`, réservée au système ; l'API publique `isRoleHeld()` ne
 * renseigne que sur l'application appelante. Les détenteurs du rôle Home et le composeur sont
 * donc résolus par intents publics (écart documenté à l'étape 13, SPEC_ANDROID §12.1).
 */
interface PackageQuery {
    /** `ACTION_MAIN` + `CATEGORY_LAUNCHER`, une entrée par activité de lancement résolue. */
    fun launcherEntries(): List<InstalledApp>

    /** Tous les lanceurs installés, pas seulement celui par défaut. */
    fun homePackages(): Set<String>

    /** Le package de l'activité Réglages résolue par le système, `null` si aucune. */
    fun settingsPackage(): String?

    /** Composeur par défaut et cible de `ACTION_DIAL` : le parcours d'appel et d'urgence. */
    fun defaultDialerPackages(): Set<String>
}
