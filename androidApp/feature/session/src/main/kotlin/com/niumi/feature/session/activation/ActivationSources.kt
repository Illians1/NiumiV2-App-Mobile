package com.niumi.feature.session.activation

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.common.TimeZoneProvider

/**
 * Dépôts que `ArmSessionUseCase` lit au moment de l'activation, regroupés en `data class` pour
 * rester sous le seuil `LongParameterList` de detekt (même motif que `ReadinessSources`,
 * `:core:system`) plutôt qu'un artefact de câblage sans valeur de lisibilité propre.
 */
data class ActivationSources(
    val pairedBoxStore: PairedBoxStore,
    val appSelectionStore: AppSelectionStore,
    val timeZoneProvider: TimeZoneProvider,
)
