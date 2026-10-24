package com.niumi.feature.session.ringtone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Point d'entrée réel de la feuille « Sonnerie » (étape 40) : relit le volume et la session à
 * `ON_RESUME`, suit le volume en direct tant qu'elle est visible (révision du 2026-10-24), arrête
 * la pré-écoute à `ON_PAUSE` et à la fermeture (§10).
 */
@Composable
fun RingtoneSheetRoute(
    onDismiss: () -> Unit,
    viewModel: RingtoneViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Récepteur du volume inscrit seulement feuille affichée et application visible.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.observeVolumeChanges() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewModel.refresh()
                    Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    RingtoneSheet(
        state = viewModel.state,
        actions =
            RingtoneActions(
                onRingtoneSelected = viewModel::onRingtoneSelected,
                onPreviewToggled = viewModel::onPreviewToggled,
                onVolumeRampEnabledChanged = viewModel::onVolumeRampEnabledChanged,
                onVolumeRampSelected = viewModel::onVolumeRampSelected,
            ),
        onDismiss = {
            viewModel.onPause()
            onDismiss()
        },
    )
}

@Preview(name = "Clair", widthDp = 390, heightDp = 844)
@Composable
private fun RingtoneSheetLightPreview() {
    NiumiTheme(dark = false) {
        RingtoneSheet(
            state = RingtoneUiState(isLoading = false, volumePercent = 80),
            actions = RingtoneActions({}, {}, {}, {}),
            onDismiss = {},
        )
    }
}

@Preview(name = "Sombre", widthDp = 390, heightDp = 844)
@Composable
private fun RingtoneSheetDarkPreview() {
    NiumiTheme(dark = true) {
        RingtoneSheet(
            state = RingtoneUiState(isLoading = false, volumePercent = 80),
            actions = RingtoneActions({}, {}, {}, {}),
            onDismiss = {},
        )
    }
}
