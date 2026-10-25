package com.niumi.feature.session.home

import com.niumi.designsystem.drawing.DialHandle

/** Gestes de l'accueil, regroupés pour rester sous `LongParameterList` de detekt. */
data class HomeActions(
    val onOpenSettings: () -> Unit,
    val onOpenApps: () -> Unit,
    val onOpenRingtone: () -> Unit,
    val onCancelWake: () -> Unit,
    val onSwitchChanged: (Boolean) -> Unit,
    val onDayToggled: (Int) -> Unit,
    val onDayModeSelected: (DayMode) -> Unit,
    val onTimeChanged: (DialHandle, Int) -> Unit,
    val onDialTapped: (DialHandle) -> Unit,
    val onPickerConfirmed: (Int, Int) -> Unit,
    val onPickerDismissed: () -> Unit,
    val onLockedTouch: () -> Unit,
    val onAnyTouch: () -> Unit,
)
