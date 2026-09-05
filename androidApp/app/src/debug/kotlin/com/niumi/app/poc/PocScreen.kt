package com.niumi.app.poc

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
fun PocScreen(viewModel: PocViewModel = hiltViewModel()) {
    val state = viewModel.state
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // `PocPairingActivity` est une activité séparée : au retour, cet écran n'est pas recréé,
    // donc le boîtier associé doit être rechargé explicitement au premier plan.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPairedBox()
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
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
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
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, PocPairingActivity::class.java)) },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Associer ce tag")
            }
        }
    }
}
