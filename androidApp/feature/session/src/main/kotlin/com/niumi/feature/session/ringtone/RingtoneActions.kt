package com.niumi.feature.session.ringtone

import com.niumi.system.audio.VolumeRampDurations

/**
 * Les quatre événements de la feuille « Sonnerie », regroupés sur le patron de `WakeTimeActions` :
 * la feuille est pure, ces lambdas sont son unique moyen de rendre la main au `ViewModel`.
 */
data class RingtoneActions(
    val onRingtoneSelected: (String) -> Unit,
    val onPreviewToggled: (String) -> Unit,
    /** Interrupteur « Sonnerie progressive » : éteint = volume constant, allumé = la durée par défaut. */
    val onVolumeRampEnabledChanged: (Boolean) -> Unit,
    /** Un segment de la barre : une des [VolumeRampDurations.SECONDS]. */
    val onVolumeRampSelected: (Int) -> Unit,
)
