package com.niumi.app.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Écran de pilotage du POC (debug uniquement) : programmer/annuler l'alarme et observer
 * `AlarmScheduler.isScheduled()`. Aucune donnée fictive n'existe dans `main` (CLAUDE.md).
 */
@Composable
fun PocScreen(viewModel: PocViewModel = hiltViewModel()) {
    val state = viewModel.state

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
        }
    }
}
