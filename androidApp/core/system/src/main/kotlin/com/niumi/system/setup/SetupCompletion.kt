package com.niumi.system.setup

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionSource

/**
 * La mise en route est-elle finie (étape 45 ; `design-plan/SPECIFICATION.md` §3) ? Oui quand
 * « Continuer » de l'écran 4 l'a enregistré ([SetupPreferences.isSetupCompleted]). Une installation
 * antérieure à l'étape 45 n'a pas cette clé : elle est considérée comme configurée dès qu'elle a un
 * Niumi Spot et une application choisie (décision de Mehdi du 2026-10-25). Lectures neutres avant
 * déverrouillage, comme les dépôts lus.
 */
class SetupCompletion(
    private val preferences: SetupPreferences,
    private val pairedBoxStore: PairedBoxStore,
    private val appSelection: AppSelectionSource,
) {
    suspend fun isCompleted(): Boolean =
        preferences.isSetupCompleted() ||
            (pairedBoxStore.all().isNotEmpty() && appSelection.selectedCount() > 0)
}
