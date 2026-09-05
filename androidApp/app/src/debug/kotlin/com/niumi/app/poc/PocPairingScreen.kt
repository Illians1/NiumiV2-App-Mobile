package com.niumi.app.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Écran d'association du POC (debug uniquement, SPEC_ANDROID §22 Lot 0, §11.1). N'affiche
 * jamais le token ni son hash complet (§16), seulement les 8 premiers caractères du `boxId`.
 */
@Composable
fun PocPairingScreen(
    state: PocPairingUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Approche le boîtier Niumi du téléphone.")
            Text(
                text = state.pairedBoxIdPrefix?.let { "Boîtier associé : $it…" } ?: "Aucun boîtier associé",
                modifier = Modifier.padding(top = 16.dp),
            )
            state.lastResultText?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) { Text("Terminer") }
        }
    }
}
