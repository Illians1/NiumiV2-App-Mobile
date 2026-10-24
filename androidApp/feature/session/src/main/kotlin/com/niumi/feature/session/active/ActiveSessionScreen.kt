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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.niumi.designsystem.effect.WindowFocusRegainedEffect
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.session.ringtone.RingtoneSheetRoute
import com.niumi.feature.session.ui.AlarmSoundRow
import com.niumi.system.nfc.nfcAdapterSettledChanges
import com.niumi.system.readiness.openRecourse

/**
 * Écran de session active (écran 7, SPEC_ANDROID §15), complet depuis l'étape 15. La seule action
 * est « Modifier ou annuler », qui mène au scan : ni arrêt, ni annulation directe — le scan du
 * boîtier est le seul chemin de sortie (SPEC_CORE_KMP §2, points 3 et 4 ; SPEC_ANDROID §3).
 *
 * La ligne « Sonnerie » est la seule exception : elle ouvre la feuille Sonnerie sans passer par le
 * scan, et seulement en `ARMED` — la sonnerie n'est pas un terme de l'engagement (§3, §9).
 */
@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
    actions: ActiveSessionActions,
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
                Text(
                    // L'heure de début n'est passée que tant que le blocage est en attente : c'est
                    // ce qui dédouble le libellé `ARMED` (§15, Lot 6).
                    text =
                        ActiveSessionTexts.stateLabel(
                            state = sessionState,
                            blockingTimeLabel = state.blockingDisplayAtActivation?.timeLabel,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            state.displayAtActivation?.let { display ->
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
            }
            state.displayInCurrentZone?.let { display ->
                Text(text = ActiveSessionTexts.CURRENT_ZONE_TITLE, style = MaterialTheme.typography.titleMedium)
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
            }

            BlockingStartSection(state)
            state.alarmSoundSummary?.let { summary ->
                AlarmSoundRow(
                    title = ActiveSessionTexts.RINGTONE_TITLE,
                    summary = summary,
                    onClick = actions.onOpenRingtone.takeIf { state.canEditAlarmSound },
                )
            }

            CriticalIncidents(state.criticalIncidents, actions.onRemediate)
            HealthSection(state)
            BlockedAppsSection(state)
            OtherIncidents(state, actions.onRemediate)

            Text(text = ActiveSessionTexts.COMMITMENT_REMINDER, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = actions.onModifyOrCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = ActiveSessionTexts.MODIFY_OR_CANCEL_BUTTON)
            }
            // Les boîtiers se modifient en session (SPEC_CORE_KMP §2, décisions 11 et 16) : ajouter,
            // renommer ou retirer un boîtier ne termine pas la session (étape 43).
            TextButton(onClick = actions.onOpenBoxes, modifier = Modifier.fillMaxWidth()) {
                Text(text = ActiveSessionTexts.OPEN_BOXES_BUTTON)
            }
            // Consultation seule (écran 12) : ne termine ni ne modifie la session, au même titre
            // que les recours ci-dessus. Le scan du boîtier reste le seul chemin de sortie (§3).
            TextButton(onClick = actions.onOpenDiagnostic, modifier = Modifier.fillMaxWidth()) {
                Text(text = ActiveSessionTexts.OPEN_DIAGNOSTIC_BUTTON)
            }
        }
    }
}

/**
 * SPEC_CORE_KMP §7.3 : un `CRITICAL` doit être « présenté explicitement », ce qui le distingue
 * d'un `DEGRADED` simplement consigné. D'où un bloc en tête d'écran, avant même la santé.
 */
@Composable
private fun CriticalIncidents(
    incidents: List<IncidentPresentation>,
    onRemediate: (IncidentPresentation) -> Unit,
) {
    if (incidents.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = LocalNiumiPalette.current.tileGrey),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = ActiveSessionTexts.CRITICAL_INCIDENTS_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = LocalNiumiPalette.current.ink,
            )
            incidents.forEach { incident ->
                Text(
                    text = ActiveSessionTexts.incidentLabel(incident.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalNiumiPalette.current.ink,
                )
                RemediationAction(incident, onRemediate)
            }
        }
    }
}

/**
 * Le recours d'un incident (§15). Il rétablit un sous-système et ne touche jamais à la session :
 * le scan du boîtier reste le seul chemin de sortie (§3, §10.2). Niumi ne réactive rien à la place
 * de l'utilisateur (§12.2) — le bouton ouvre le réglage, l'utilisateur décide.
 */
@Composable
private fun RemediationAction(
    incident: IncidentPresentation,
    onRemediate: (IncidentPresentation) -> Unit,
) {
    val label = incident.actionLabel ?: return
    TextButton(onClick = { onRemediate(incident) }) {
        Text(text = label)
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

/**
 * Instant de début du blocage, tant qu'il n'est pas atteint (§15, Lot 6), lu dans le fuseau
 * d'activation et, s'il diffère, dans le fuseau courant — même règle que le réveil (§8). Rien
 * n'apparaît une fois le blocage appliqué, ni pour une session à blocage immédiat. Information
 * secondaire, en texte secondaire.
 */
@Composable
private fun BlockingStartSection(state: ActiveSessionUiState) {
    val display = state.blockingDisplayAtActivation ?: return
    Text(text = ActiveSessionTexts.BLOCKING_START_TITLE, style = MaterialTheme.typography.titleMedium)
    Text(
        text = display.sentence,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalNiumiPalette.current.textSecondary,
    )
    state.blockingDisplayInCurrentZone?.let { inCurrentZone ->
        Text(
            text = "${ActiveSessionTexts.CURRENT_ZONE_TITLE} : ${inCurrentZone.sentence}",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalNiumiPalette.current.textSecondary,
        )
    }
}

@Composable
private fun BlockedAppsSection(state: ActiveSessionUiState) {
    Text(
        text = ActiveSessionTexts.blockedAppsTitle(state.isBlockingPending),
        style = MaterialTheme.typography.titleMedium,
    )
    if (state.blockedApps.isEmpty()) {
        Text(text = ActiveSessionTexts.NO_BLOCKED_APP, style = MaterialTheme.typography.bodyLarge)
        return
    }
    state.blockedApps.forEach { app ->
        Text(text = app.displayNameSnapshot, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Les `CRITICAL` encore à vérifier sont déjà en tête : ce bloc porte le reste, consigné sans
 * alarmer — dont les `CRITICAL` rétablis (§15, étape 25), avec leur gravité et la mention
 * [ActiveSessionTexts.INCIDENT_RESOLVED], atténuée : une information, pas une alerte.
 */
@Composable
private fun OtherIncidents(
    state: ActiveSessionUiState,
    onRemediate: (IncidentPresentation) -> Unit,
) {
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
        if (incident.resolved) {
            Text(
                text = ActiveSessionTexts.INCIDENT_RESOLVED,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalNiumiPalette.current.textSecondary,
            )
        }
        RemediationAction(incident, onRemediate)
    }
}

/**
 * Point d'entrée réel. La convention 12/24 h vient du réglage système et est relue à chaque
 * `ON_RESUME`, comme sur les écrans 5 et 6 : l'utilisateur peut la changer pendant qu'une session
 * est armée, et trois écrans portant la même heure ne peuvent pas diverger (§15). Le même
 * `ON_RESUME` porte le déclencheur « passage au premier plan » de §13.1, et le retour du focus
 * de la fenêtre celui de la fermeture du volet rapide, qui ne met pas l'écran en pause (écart 10
 * de `RELEASE_REPORT.md`).
 */
@Composable
fun ActiveSessionRoute(
    onModifyOrCancel: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenBoxes: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Feuille « Sonnerie » (étape 40) : un état d'affichage, pas une navigation — elle ne
    // déclenche pas `ON_RESUME`, d'où le `refresh()` explicite à sa fermeture.
    var showRingtoneSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh(DateFormat.is24HourFormat(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(DateFormat.is24HourFormat(context))
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    WindowFocusRegainedEffect { viewModel.refresh(DateFormat.is24HourFormat(context)) }
    // Le NFC met plus d'une seconde à s'allumer : au retour du focus il est souvent encore éteint.
    // Seul son état stable fait foi, annoncé par Android (écart 10).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            context.nfcAdapterSettledChanges().collect { viewModel.refresh(DateFormat.is24HourFormat(context)) }
        }
    }

    ActiveSessionScreen(
        state = viewModel.state,
        actions =
            ActiveSessionActions(
                onModifyOrCancel = onModifyOrCancel,
                onOpenDiagnostic = onOpenDiagnostic,
                onOpenBoxes = onOpenBoxes,
                onOpenRingtone = { showRingtoneSheet = true },
                // §13 : les `Intent` de réglages sont construits par l'écran, jamais par le
                // ViewModel ni par `DeviceReadinessChecker`. Un recours sans `Intent` n'a de toute
                // façon pas de bouton (`IncidentPresentation`). `openRecourse` ouvre le même
                // chemin que l'écran 2, repli compris (demande directe d'exemption, 2026-10-21).
                onRemediate = { incident -> incident.action?.let(context::openRecourse) },
            ),
    )

    if (showRingtoneSheet) {
        RingtoneSheetRoute(
            onDismiss = {
                showRingtoneSheet = false
                viewModel.refresh(DateFormat.is24HourFormat(context))
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveSessionScreenPreview() {
    NiumiTheme(dark = true) {
        ActiveSessionScreen(
            state = ActiveSessionUiState(isLoading = false),
            actions = ActiveSessionActions(onModifyOrCancel = {}),
        )
    }
}
