package com.niumi.feature.session.active

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Écran de session active (écran 7, SPEC_ANDROID §15), complet depuis l'étape 15. La seule action
 * est « Modifier ou annuler », qui mène au scan : ni arrêt, ni annulation directe — le scan du
 * boîtier est le seul chemin de sortie (SPEC_CORE_KMP §2, points 3 et 4 ; SPEC_ANDROID §3).
 */
@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
    onModifyOrCancel: () -> Unit,
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

            CriticalIncidents(state.criticalIncidents)
            HealthSection(state)
            BlockedAppsSection(state)
            OtherIncidents(state)

            Text(text = ActiveSessionTexts.COMMITMENT_REMINDER, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onModifyOrCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = ActiveSessionTexts.MODIFY_OR_CANCEL_BUTTON)
            }
        }
    }
}

/**
 * SPEC_CORE_KMP §7.3 : un `CRITICAL` doit être « présenté explicitement », ce qui le distingue
 * d'un `DEGRADED` simplement consigné. D'où un bloc en tête d'écran, avant même la santé.
 */
@Composable
private fun CriticalIncidents(incidents: List<SessionIncidentDto>) {
    if (incidents.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = ActiveSessionTexts.CRITICAL_INCIDENTS_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            incidents.forEach { incident ->
                Text(
                    text = ActiveSessionTexts.incidentLabel(incident.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun HealthSection(state: ActiveSessionUiState) {
    Text(text = ActiveSessionTexts.HEALTH_TITLE, style = MaterialTheme.typography.titleMedium)
    Text(
        text = if (state.isDegraded) ActiveSessionTexts.DEGRADED else ActiveSessionTexts.HEALTHY,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun BlockedAppsSection(state: ActiveSessionUiState) {
    Text(text = ActiveSessionTexts.BLOCKED_APPS_TITLE, style = MaterialTheme.typography.titleMedium)
    if (state.blockedApps.isEmpty()) {
        Text(text = ActiveSessionTexts.NO_BLOCKED_APP, style = MaterialTheme.typography.bodyLarge)
        return
    }
    state.blockedApps.forEach { app ->
        Text(text = app.displayNameSnapshot, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Les `CRITICAL` sont déjà en tête : ce bloc porte le reste, consigné sans alarmer. */
@Composable
private fun OtherIncidents(state: ActiveSessionUiState) {
    val others = state.incidents - state.criticalIncidents.toSet()
    if (others.isEmpty()) return
    Text(text = ActiveSessionTexts.INCIDENTS_TITLE, style = MaterialTheme.typography.titleMedium)
    others.forEach { incident ->
        Text(
            text =
                "${ActiveSessionTexts.severityLabel(incident.severity)} — " +
                    ActiveSessionTexts.incidentLabel(incident.code),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Point d'entrée réel. La convention 12/24 h vient du réglage système et est relue à chaque
 * `ON_RESUME`, comme sur les écrans 5 et 6 : l'utilisateur peut la changer pendant qu'une session
 * est armée, et trois écrans portant la même heure ne peuvent pas diverger (§15). Le même
 * `ON_RESUME` porte le déclencheur « passage au premier plan » de §13.1.
 */
@Composable
fun ActiveSessionRoute(
    onModifyOrCancel: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
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

    ActiveSessionScreen(state = viewModel.state, onModifyOrCancel = onModifyOrCancel)
}

@Preview(showBackground = true)
@Composable
private fun ActiveSessionScreenPreview() {
    NiumiTheme {
        ActiveSessionScreen(state = ActiveSessionUiState(isLoading = false), onModifyOrCancel = {})
    }
}
