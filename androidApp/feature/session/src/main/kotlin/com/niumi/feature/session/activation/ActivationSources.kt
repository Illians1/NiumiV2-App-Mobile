package com.niumi.feature.session.activation

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.common.TimeZoneProvider

/**
 * Dépôts que `ArmSessionUseCase` lit au moment de l'activation, regroupés en `data class` pour
 * rester sous le seuil `LongParameterList` de detekt (même motif que `ReadinessSources`,
 * `:core:system`) plutôt qu'un artefact de câblage sans valeur de lisibilité propre.
 *
 * [alarmSoundPreferences] (étape 27, Lot 7) : lu au moment d'armer, jamais transporté par la route,
 * comme l'heure de réveil (`SetupPreferences`).
 */
data class ActivationSources(
    val pairedBoxStore: PairedBoxStore,
    val appSelectionStore: AppSelectionStore,
    val timeZoneProvider: TimeZoneProvider,
    val alarmSoundPreferences: AlarmSoundPreferences,
)
