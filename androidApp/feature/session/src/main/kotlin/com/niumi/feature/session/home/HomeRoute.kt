package com.niumi.feature.session.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.feature.session.ringtone.RingtoneSheetRoute
import com.niumi.feature.session.ringtone.RingtoneViewModel

/**
 * Point d'entrée réel de l'accueil (étape 45). Relit la sélection et la sonnerie à chaque retour au
 * premier plan (retour de l'écran 4, de la feuille « Sonnerie » ou d'une autre application). La
 * feuille « Sonnerie » s'ouvre par-dessus l'accueil, en session aussi (§10). Son ViewModel est tenu
 * ici, comme celui de la feuille d'annulation : la feuille enregistre en arrière-plan, parfois après
 * sa fermeture, et l'accueil se relit à chaque fin d'enregistrement (constat du 2026-10-26).
 *
 * Le lien « Annuler le réveil » ouvre la feuille d'annulation (§11, étape 46). Un réveil annulé
 * laisse l'accueil, revenu seul en clair « Réveil désactivé », avec la bulle « Réveil annulé… » ;
 * si l'alarme a sonné entre-temps et que le scan a terminé la session, [onSessionCompleted] mène à
 * l'écran 11.
 */
@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onSessionCompleted: (sessionId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    cancelViewModel: CancelSessionViewModel = hiltViewModel(),
) {
    val ringtoneViewModel: RingtoneViewModel = hiltViewModel()
    var showRingtone by rememberSaveable { mutableStateOf(false) }
    var showCancel by rememberSaveable { mutableStateOf(false) }

    RefreshTriggers(viewModel, ringtoneViewModel)

    HomeScreen(
        state = viewModel.state,
        actions =
            HomeActions(
                onOpenSettings = onOpenSettings,
                onOpenApps = onOpenApps,
                onOpenRingtone = { showRingtone = true },
                onCancelWake = {
                    cancelViewModel.open()
                    showCancel = true
                },
                onSwitchChanged = viewModel::onSwitchChanged,
                onDayToggled = viewModel::onDayToggled,
                onDayModeSelected = viewModel::onDayModeSelected,
                onTimeChanged = viewModel::onTimeChanged,
                onDialTapped = viewModel::onDialTapped,
                onPickerConfirmed = viewModel::onPickerConfirmed,
                onPickerDismissed = viewModel::onPickerDismissed,
                onLockedTouch = viewModel::onLockedTouch,
                onAnyTouch = viewModel::dismissMessage,
                onImmediateBlockingConfirmed = viewModel::onImmediateBlockingConfirmed,
                onImmediateBlockingDismissed = viewModel::onImmediateBlockingDismissed,
            ),
    )

    if (showRingtone) {
        RingtoneSheetRoute(
            viewModel = ringtoneViewModel,
            onDismiss = {
                showRingtone = false
                // La feuille ne déclenche pas `ON_RESUME` : la ligne « Sonnerie » est relue ici.
                viewModel.refresh()
            },
        )
    }

    if (showCancel) {
        CancelSessionRoute(
            onDismiss = { showCancel = false },
            onCancelled = {
                showCancel = false
                viewModel.showCancelledMessage()
            },
            onCompleted = { sessionId ->
                showCancel = false
                onSessionCompleted(sessionId)
            },
            viewModel = cancelViewModel,
        )
    }
}

/** Relit l'accueil à chaque `ON_RESUME` et à chaque fin d'enregistrement de la feuille « Sonnerie ». */
@Composable
private fun RefreshTriggers(
    viewModel: HomeViewModel,
    ringtoneViewModel: RingtoneViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(ringtoneViewModel) { ringtoneViewModel.saved.collect { viewModel.refresh() } }
}
