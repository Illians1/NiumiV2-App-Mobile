package com.niumi.feature.session.active

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * Écran de session active (écran 7, SPEC_ANDROID §15), **version minimale de l'étape 14**. Aucune
 * action : ni arrêt, ni modification — le scan du boîtier est le seul chemin de sortie
 * (SPEC_CORE_KMP §2). Le bouton « Modifier ou annuler », la liste des applications bloquées et les
 * incidents arrivent à l'étape 15 dans ce même fichier.
 */
@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
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
            if (!state.hasSession) {
                Text(text = ActiveSessionTexts.NO_SESSION, style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            Text(text = ActiveSessionTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            state.state?.let { sessionState ->
                Text(text = ActiveSessionTexts.stateLabel(sessionState), style = MaterialTheme.typography.titleMedium)
            }
            state.displayAtActivation?.let { display ->
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
            }
            state.displayInCurrentZone?.let { display ->
                Text(text = ActiveSessionTexts.CURRENT_ZONE_TITLE, style = MaterialTheme.typography.titleMedium)
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
            }
            Text(text = ActiveSessionTexts.COMMITMENT_REMINDER, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Point d'entrée réel. La convention 12/24 h vient du réglage système et est relue à chaque
 * `ON_RESUME`, comme sur les écrans 5 et 6 : l'utilisateur peut la changer pendant qu'une session
 * est armée, et trois écrans portant la même heure ne peuvent pas diverger (§15).
 */
@Composable
fun ActiveSessionRoute(viewModel: ActiveSessionViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh(DateFormat.is24HourFormat(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(DateFormat.is24HourFormat(context))
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ActiveSessionScreen(state = viewModel.state)
}

@Preview(showBackground = true)
@Composable
private fun ActiveSessionScreenPreview() {
    NiumiTheme {
        ActiveSessionScreen(state = ActiveSessionUiState(isLoading = false))
    }
}
