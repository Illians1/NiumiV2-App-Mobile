package com.niumi.feature.session.diagnostics

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.readiness.ReadinessCheck

/**
 * Écran 12 — diagnostic d'incident (SPEC_ANDROID §15, §17, §18).
 *
 * Écran de **consultation** : il ne porte aucune action sur la session. Le scan du boîtier reste le
 * seul chemin de sortie (§3, §10.2) ; les recours, eux, vivent sur l'écran 7 (§15). La seule action
 * ici est l'export, que §17 exige explicite.
 */
@Composable
fun IncidentDiagnosticScreen(
    state: IncidentDiagnosticUiState,
    onExport: () -> Unit,
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
            Text(text = IncidentDiagnosticTexts.TITLE, style = MaterialTheme.typography.headlineSmall)

            CriticalIncidents(state.criticalIncidents)
            SessionSection(state)
            ChecksSection(state.checks)
            IncidentsSection(state)
            EventsSection(state.events)

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(text = IncidentDiagnosticTexts.EXPORT_BUTTON)
            }
        }
    }
}

/**
 * SPEC_CORE_KMP §7.3 : un `CRITICAL` « doit en plus être présenté explicitement dans un diagnostic
 * visible par l'utilisateur ». C'est cet écran-là ; d'où un bloc en tête, avant tout le reste.
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
            incidents.forEach { incident ->
                Text(
                    text = IncidentDiagnosticTexts.incidentLabel(incident.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun SessionSection(state: IncidentDiagnosticUiState) {
    Text(text = IncidentDiagnosticTexts.SESSION_TITLE, style = MaterialTheme.typography.titleMedium)
    if (!state.hasSession) {
        Text(text = IncidentDiagnosticTexts.NO_SESSION, style = MaterialTheme.typography.bodyLarge)
        return
    }
    state.state?.let {
        Text(text = IncidentDiagnosticTexts.stateLabel(it), style = MaterialTheme.typography.bodyLarge)
    }
    state.health?.let {
        Text(text = IncidentDiagnosticTexts.healthLabel(it), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChecksSection(checks: List<ReadinessCheck>) {
    if (checks.isEmpty()) return
    Text(text = IncidentDiagnosticTexts.CHECKS_TITLE, style = MaterialTheme.typography.titleMedium)
    checks.forEach { check ->
        Text(
            text =
                "${IncidentDiagnosticTexts.checkLabel(check.id)} : " +
                    IncidentDiagnosticTexts.outcomeLabel(check.outcome),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun IncidentsSection(state: IncidentDiagnosticUiState) {
    Text(text = IncidentDiagnosticTexts.INCIDENTS_TITLE, style = MaterialTheme.typography.titleMedium)
    if (state.incidents.isEmpty()) {
        Text(text = IncidentDiagnosticTexts.NO_INCIDENT, style = MaterialTheme.typography.bodyLarge)
        return
    }
    state.incidents.forEach { incident ->
        Text(
            text =
                "${IncidentDiagnosticTexts.severityLabel(incident.severity)} — " +
                    IncidentDiagnosticTexts.incidentLabel(incident.code),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** §17 : au plus 200 événements, bornés par le journal lui-même. */
@Composable
private fun EventsSection(events: List<TechnicalEventEntry>) {
    Text(text = IncidentDiagnosticTexts.EVENTS_TITLE, style = MaterialTheme.typography.titleMedium)
    if (events.isEmpty()) {
        Text(text = IncidentDiagnosticTexts.NO_EVENT, style = MaterialTheme.typography.bodyLarge)
        return
    }
    events.forEach { event ->
        Text(text = event.type.name, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Point d'entrée réel. L'`ACTION_SEND` est construit ici, comme tout `Intent` (§13), et n'est émis
 * qu'au clic — §17 : « après action explicite de l'utilisateur ».
 */
@Composable
fun IncidentDiagnosticRoute(viewModel: IncidentDiagnosticViewModel = hiltViewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    IncidentDiagnosticScreen(
        state = viewModel.state,
        onExport = {
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, viewModel.exportText())
                }
            context.startActivity(Intent.createChooser(send, IncidentDiagnosticTexts.EXPORT_CHOOSER_TITLE))
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun IncidentDiagnosticScreenPreview() {
    NiumiTheme {
        IncidentDiagnosticScreen(state = IncidentDiagnosticUiState(isLoading = false), onExport = {})
    }
}
