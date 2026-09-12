package com.niumi.feature.session.active

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.ScanOutcome

/**
 * Écran 9 : scan requis pour modifier ou annuler (SPEC_ANDROID §15). **Aucune action** hors le
 * scan : pas de bouton d'annulation, pas de confirmation, pas de retour qui libérerait quoi que ce
 * soit (§3, §10.2). Le seul bouton système est le retour arrière, qui ramène à l'écran 7 sans rien
 * modifier.
 */
@Composable
fun ScanToModifyScreen(
    state: ScanToModifyUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = ScanToModifyTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = ScanToModifyTexts.INVITATION, style = MaterialTheme.typography.bodyLarge)

            ScanToModifyTexts.availabilityMessage(state.availability)?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.isReleasing) {
                Text(text = ScanToModifyTexts.RELEASING, style = MaterialTheme.typography.titleMedium)
            }

            outcomeMessage(state.lastOutcome)?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * `Accepted` n'a pas de message : il ne prouve rien à lui seul, la libération peut encore échouer
 * (§11.3). `Ignored` n'en a pas non plus — aucun scan n'a été reconnu.
 */
private fun outcomeMessage(outcome: ScanOutcome?): String? =
    when (outcome) {
        ScanOutcome.UnknownBox -> ScanToModifyTexts.UNKNOWN_BOX
        ScanOutcome.Unreadable -> ScanToModifyTexts.UNREADABLE
        ScanOutcome.Accepted, ScanOutcome.Ignored, null -> null
    }

/**
 * Reader Mode câblé sur le lifecycle, même patron que `PairingRoute` : démarré à `ON_RESUME`,
 * arrêté à `ON_PAUSE` et à la disparition de l'écran, l'`Activity` n'étant jamais retenue.
 */
@Composable
fun ScanToModifyRoute(
    onCancelled: () -> Unit,
    viewModel: ScanToModifyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val state = viewModel.state

    LaunchedEffect(state.isCancelled) {
        if (state.isCancelled) onCancelled()
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshAvailability()
                    activity?.let(viewModel::startReaderMode)
                }
                if (event == Lifecycle.Event.ON_PAUSE) {
                    activity?.let(viewModel::stopReaderMode)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.let(viewModel::stopReaderMode)
        }
    }

    ScanToModifyScreen(state = state)
}

/** `LocalContext` est un `ContextThemeWrapper` dans une `ComponentActivity` : déplier jusqu'à elle. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun ScanToModifyScreenPreview() {
    NiumiTheme {
        ScanToModifyScreen(state = ScanToModifyUiState())
    }
}
