package com.niumi.feature.ringing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.NfcAvailability

/**
 * Écran de réveil (SPEC_ANDROID §10.4). Aucun bouton d'arrêt : la seule façon de faire taire
 * l'alarme est le scan du boîtier NFC (étape 4). `currentTimeText` est fourni par l'appelant
 * (`AlarmActivity`), jamais lu directement ici, pour garder ce composable pur et testable en
 * isolation. Le raccourci vers les réglages NFC (rang 2 de `AlarmScreenState`) n'arrête ni ne
 * met en pause la sonnerie : ce n'est pas une action d'arrêt, seulement un accès aux réglages
 * système nécessaires pour scanner.
 */
@Composable
fun AlarmScreen(
    state: AlarmScreenState,
    currentTimeText: String,
    onOpenNfcSettings: () -> Unit = {},
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
            Text(
                text = currentTimeText,
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = state.instructionText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (state.showsNfcSettingsShortcut) {
                OutlinedButton(onClick = onOpenNfcSettings, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Ouvrir les réglages NFC")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmScreenRingingPreview() {
    NiumiTheme {
        AlarmScreen(
            state = AlarmScreenState.from(AlarmRingingPhase.RINGING, deviceLocked = false),
            currentTimeText = "07:00",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmScreenNfcDisabledPreview() {
    NiumiTheme {
        AlarmScreen(
            state =
                AlarmScreenState.from(
                    AlarmRingingPhase.RINGING,
                    deviceLocked = false,
                    nfcAvailability = NfcAvailability.DISABLED,
                ),
            currentTimeText = "07:00",
        )
    }
}
