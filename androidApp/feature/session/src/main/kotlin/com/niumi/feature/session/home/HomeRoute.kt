package com.niumi.feature.session.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.feature.session.ringtone.RingtoneSheetRoute

/**
 * Point d'entrée réel de l'accueil (étape 45). Relit la sélection et la sonnerie à chaque retour au
 * premier plan (retour de l'écran 4, de la feuille « Sonnerie » ou d'une autre application). La
 * feuille « Sonnerie » s'ouvre par-dessus l'accueil, en session aussi (§10).
 *
 * [onCancelWake] mène jusqu'à l'étape 46 à l'ancien écran de scan pour modifier ou annuler.
 */
@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onCancelWake: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRingtone by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        state = viewModel.state,
        actions =
            HomeActions(
                onOpenSettings = onOpenSettings,
                onOpenApps = onOpenApps,
                onOpenRingtone = { showRingtone = true },
                onCancelWake = onCancelWake,
                onSwitchChanged = viewModel::onSwitchChanged,
                onDayToggled = viewModel::onDayToggled,
                onDayModeSelected = viewModel::onDayModeSelected,
                onTimeChanged = viewModel::onTimeChanged,
                onDialTapped = viewModel::onDialTapped,
                onPickerConfirmed = viewModel::onPickerConfirmed,
                onPickerDismissed = viewModel::onPickerDismissed,
                onLockedTouch = viewModel::onLockedTouch,
                onAnyTouch = viewModel::dismissMessage,
            ),
    )

    if (showRingtone) {
        RingtoneSheetRoute(
            onDismiss = {
                showRingtone = false
                // La feuille ne déclenche pas `ON_RESUME` : la ligne « Sonnerie » est relue ici.
                viewModel.refresh()
            },
        )
    }
}
