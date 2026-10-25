package com.niumi.feature.session.activation

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.common.TimeZoneProvider

/**
 * Dépôts que `ArmSessionUseCase` lit pour son aperçu et son diagnostic, regroupés en `data class`
 * pour rester sous le seuil `LongParameterList` de detekt (même motif que `ReadinessSources`,
 * `:core:system`). La sonnerie n'y figure plus depuis l'étape 44 : `ArmOccurrenceUseCase` la lit
 * lui-même au moment d'armer.
 */
data class ActivationSources(
    val pairedBoxStore: PairedBoxStore,
    val appSelectionStore: AppSelectionStore,
    val timeZoneProvider: TimeZoneProvider,
)
