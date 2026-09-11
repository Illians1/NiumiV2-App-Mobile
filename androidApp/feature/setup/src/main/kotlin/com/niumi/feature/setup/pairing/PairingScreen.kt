package com.niumi.feature.setup.pairing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.setup.readiness.settingsIntentFor
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.readiness.ReadinessAction

/**
 * Écran 3 — association du boîtier (SPEC_ANDROID §11.1, §15). Composable pur et sans état, testé
 * en isolation : aucun clic n'y est simulé, seuls les textes et l'état des actions sont vérifiés.
 *
 * Le token n'apparaît nulle part, ni son empreinte : seul un préfixe de `boxId` est affiché (§16).
 */
@Composable
fun PairingScreen(
    state: PairingUiState,
    actions: PairingActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = PairingTexts.TITLE, style = MaterialTheme.typography.headlineSmall)

            when {
                state.isSessionInProgress -> {
                    Text(text = PairingTexts.SESSION_IN_PROGRESS, style = MaterialTheme.typography.bodyLarge)
                }

                state.nfcAvailability == NfcAvailability.ABSENT -> {
                    Text(text = PairingTexts.NFC_ABSENT, style = MaterialTheme.typography.bodyLarge)
                }

                state.nfcAvailability == NfcAvailability.DISABLED -> {
                    Text(text = PairingTexts.NFC_DISABLED, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(
                        onClick = actions.onOpenNfcSettings,
                        modifier =
                            Modifier.semantics {
                                contentDescription = PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL
                            },
                    ) {
                        Text(PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL)
                    }
                }

                state.pendingReplacement != null -> {
                    ReplacementConfirmation(actions.onConfirmReplacement, actions.onCancelReplacement)
                }

                else -> {
                    Text(text = PairingTexts.INSTRUCTION, style = MaterialTheme.typography.bodyLarge)
                }
            }

            state.pairedBoxIdPrefix?.let { prefix ->
                Text(
                    text = PairingTexts.currentBoxLabel(prefix),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.canContinue && state.pendingReplacement == null) {
                Button(
                    onClick = actions.onContinue,
                    modifier = Modifier.semantics { contentDescription = PairingTexts.CONTINUE_BUTTON_LABEL },
                ) {
                    Text(PairingTexts.CONTINUE_BUTTON_LABEL)
                }
            }
        }
    }
}

@Composable
private fun ReplacementConfirmation(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = PairingTexts.REPLACE_QUESTION, style = MaterialTheme.typography.titleMedium)
    Text(text = PairingTexts.REPLACE_EXPLANATION, style = MaterialTheme.typography.bodyMedium)
    Button(
        onClick = onConfirm,
        modifier = Modifier.semantics { contentDescription = PairingTexts.REPLACE_CONFIRM_BUTTON_LABEL },
    ) {
        Text(PairingTexts.REPLACE_CONFIRM_BUTTON_LABEL)
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.semantics { contentDescription = PairingTexts.REPLACE_CANCEL_BUTTON_LABEL },
    ) {
        Text(PairingTexts.REPLACE_CANCEL_BUTTON_LABEL)
    }
}

/**
 * Point d'entrée réel. Le Reader Mode est branché ici et non dans le ViewModel :
 * `NfcAdapter.enableReaderMode()` exige une `Activity` au premier plan (§11.1). Il démarre sur
 * `ON_RESUME` et s'arrête sur `ON_PAUSE`, comme `AlarmActivity` et `PocPairingActivity` — la
 * lecture ne reste jamais active quand l'écran n'est pas devant l'utilisateur.
 *
 * [onSessionInProgress] applique la garde de `SetupGate` : une session en cours interdit la
 * ré-association (SPEC_CORE_KMP §2 point 11).
 */
@Composable
fun PairingRoute(
    onContinue: () -> Unit,
    onSessionInProgress: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val state = viewModel.state

    LaunchedEffect(state.isSessionInProgress) {
        if (state.isSessionInProgress) onSessionInProgress()
    }

    DisposableEffect(lifecycleOwner, activity, state.isReaderModeExpected) {
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

    PairingScreen(
        state = state,
        actions =
            PairingActions(
                onConfirmReplacement = viewModel::confirmReplacement,
                onCancelReplacement = viewModel::cancelReplacement,
                onOpenNfcSettings = {
                    settingsIntentFor(ReadinessAction.OpenNfcSettings, context.packageName)
                        ?.let(context::startActivity)
                },
                onContinue = onContinue,
            ),
    )
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
private fun PairingScreenPreview() {
    NiumiTheme {
        PairingScreen(
            state = PairingUiState(pairedBoxIdPrefix = "550e8400"),
            actions =
                PairingActions(
                    onConfirmReplacement = {},
                    onCancelReplacement = {},
                    onOpenNfcSettings = {},
                    onContinue = {},
                ),
        )
    }
}
