package com.niumi.feature.session.ringtone

/**
 * Les quatre événements de l'écran 14, regroupés sur le patron de `WakeTimeActions` : l'écran est
 * pur, ces lambdas sont son unique moyen de rendre la main au `ViewModel`.
 */
data class RingtoneActions(
    val onRingtoneSelected: (String) -> Unit,
    val onPreviewToggled: (String) -> Unit,
    val onVolumeRampEnabledChanged: (Boolean) -> Unit,
    val onVolumeRampSecondsChanged: (Int) -> Unit,
)
