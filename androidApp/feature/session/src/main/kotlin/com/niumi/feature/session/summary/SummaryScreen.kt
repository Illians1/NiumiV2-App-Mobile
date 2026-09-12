package com.niumi.feature.session.summary

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Récapitulatif d'engagement (écran 6, SPEC_ANDROID §15 ; SPEC_CORE_KMP §8.1 « afficher la date
 * complète avant confirmation »). Composable pur et sans état.
 *
 * L'explication du changement d'heure n'apparaît que lorsque l'heure saisie n'existait pas :
 * l'écart entre la saisie et l'heure programmée passerait sinon pour un défaut de l'application.
 */
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onActivate: () -> Unit,
    onChangeTime: () -> Unit,
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
            Text(text = SummaryTexts.TITLE, style = MaterialTheme.typography.headlineSmall)

            state.display?.let { display ->
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
                display.shiftedFromLocalTime?.let { requested ->
                    Text(
                        text = SummaryTexts.daylightSavingShift(requested, display.timeLabel),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            TextButton(
                onClick = onChangeTime,
                modifier = Modifier.semantics { contentDescription = SummaryTexts.CHANGE_TIME_LABEL },
            ) {
                Text(SummaryTexts.CHANGE_TIME_LABEL)
            }

            Text(text = SummaryTexts.BLOCKED_APPS_TITLE, style = MaterialTheme.typography.titleMedium)
            state.blockedPackages.forEach { blocked ->
                Text(text = "•  ${blocked.displayNameSnapshot}", style = MaterialTheme.typography.bodyMedium)
            }

            state.truncatedBoxId?.let { boxId ->
                Text(text = SummaryTexts.BOX_TITLE, style = MaterialTheme.typography.titleMedium)
                Text(text = boxId, style = MaterialTheme.typography.bodyMedium)
            }

            Text(text = SummaryTexts.COMMITMENT_REMINDER, style = MaterialTheme.typography.bodyLarge)

            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = onActivate,
                enabled = state.canActivate,
                modifier = Modifier.semantics { contentDescription = SummaryTexts.ACTIVATE_BUTTON_LABEL },
            ) {
                Text(SummaryTexts.ACTIVATE_BUTTON_LABEL)
            }
        }
    }
}

/**
 * Point d'entrée réel. [localTimeIso] vient de la route : l'écran 5 ne transmet que le **choix**
 * de l'utilisateur, jamais un horaire déjà calculé — le recalcul du fuseau avant activation
 * devient ainsi impossible à contourner (SPEC_CORE_KMP §8.1, §10).
 */
@Composable
fun SummaryRoute(
    localTimeIso: String,
    onArmed: () -> Unit,
    onChangeTime: () -> Unit,
    onSessionInProgress: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refresh(localTimeIso, DateFormat.is24HourFormat(context))
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel.armedSnapshot) {
        if (viewModel.armedSnapshot != null) onArmed()
    }

    LaunchedEffect(viewModel.state.isSessionInProgress) {
        // Une session armée ailleurs (réconciliation) pendant que l'écran est ouvert : le
        // récapitulatif n'a plus de sens, l'écran de session active prend la main.
        if (viewModel.state.isSessionInProgress && viewModel.armedSnapshot == null) onSessionInProgress()
    }

    SummaryScreen(
        state = viewModel.state,
        onActivate = { viewModel.activate(localTimeIso) },
        onChangeTime = onChangeTime,
    )
}

@Preview(showBackground = true)
@Composable
private fun SummaryScreenPreview() {
    NiumiTheme {
        SummaryScreen(state = SummaryUiState(isLoading = false), onActivate = {}, onChangeTime = {})
    }
}
