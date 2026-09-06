package com.niumi.app.poc

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Écran de pilotage du POC (debug uniquement) : programmer/annuler l'alarme et observer
 * `AlarmScheduler.isScheduled()`. Aucune donnée fictive n'existe dans `main` (CLAUDE.md).
 */
@Composable
fun PocScreen(
    onOpenAccessibilityConsent: () -> Unit = {},
    viewModel: PocViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // `PocPairingActivity` et l'écran de consentement sont ouverts séparément : au retour, cet
    // écran n'est pas recréé, donc le boîtier associé et l'état du service doivent être
    // rechargés explicitement au premier plan (SPEC_ANDROID §13 : recalculer après un retour
    // des réglages).
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshPairedBox()
                    viewModel.refreshAccessibilityStatus()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AlarmSection(
                state = state,
                viewModel = viewModel,
                onOpenPairing = { context.startActivity(Intent(context, PocPairingActivity::class.java)) },
            )
            BlockingSection(
                state = state,
                viewModel = viewModel,
                onOpenAccessibilityConsent = onOpenAccessibilityConsent,
            )
        }
    }
}

@Composable
private fun AlarmSection(
    state: PocUiState,
    viewModel: PocViewModel,
    onOpenPairing: () -> Unit,
) {
    Text("POC alarme (debug)")
    OutlinedTextField(
        value = state.secondsInput,
        onValueChange = viewModel::onSecondsInputChanged,
        label = { Text("Dans N secondes") },
    )
    Row(modifier = Modifier.padding(top = 16.dp)) {
        Button(onClick = viewModel::schedule) { Text("Programmer") }
        Button(onClick = viewModel::cancel, modifier = Modifier.padding(start = 8.dp)) {
            Text("Annuler")
        }
    }
    Text(
        text = if (state.isScheduled) "Alarme programmée" else "Aucune alarme programmée",
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = state.pairedBoxIdPrefix?.let { "Boîtier associé : $it…" } ?: "Aucun boîtier associé",
        modifier = Modifier.padding(top = 16.dp),
    )
    OutlinedButton(onClick = onOpenPairing, modifier = Modifier.padding(top = 8.dp)) {
        Text("Associer ce tag")
    }
}

@Composable
private fun BlockingSection(
    state: PocUiState,
    viewModel: PocViewModel,
    onOpenAccessibilityConsent: () -> Unit,
) {
    OutlinedTextField(
        value = state.blockPackageInput,
        onValueChange = viewModel::onBlockPackageInputChanged,
        label = { Text("Package à bloquer") },
        modifier = Modifier.padding(top = 24.dp),
    )
    Row(modifier = Modifier.padding(top = 16.dp)) {
        Button(onClick = viewModel::block) { Text("Bloquer") }
        Button(onClick = viewModel::unblock, modifier = Modifier.padding(start = 8.dp)) {
            Text("Débloquer")
        }
    }
    Text(
        text = if (state.isBlocked) "Blocage actif" else "Aucun blocage actif",
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text =
            if (state.isAccessibilityServiceEnabled) {
                "Service d'accessibilité actif"
            } else {
                "Service d'accessibilité inactif"
            },
        modifier = Modifier.padding(top = 8.dp),
    )
    OutlinedButton(onClick = onOpenAccessibilityConsent, modifier = Modifier.padding(top = 8.dp)) {
        Text("Consentement accessibilité")
    }
}
