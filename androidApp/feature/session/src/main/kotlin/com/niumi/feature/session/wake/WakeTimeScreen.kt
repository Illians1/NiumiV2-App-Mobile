package com.niumi.feature.session.wake

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import java.time.LocalTime

/**
 * Choix de l'heure de réveil (écran 5, SPEC_ANDROID §15). Composable pur et sans état : le
 * `TimePicker` Material 3 n'expose aucun callback de changement, [onTimeChanged] est donc appelé
 * depuis un `LaunchedEffect` observant `pickerState.hour`/`pickerState.minute` dans la `Route`.
 */
@Composable
fun WakeTimeScreen(
    state: WakeTimeUiState,
    onContinue: () -> Unit,
    pickerContent: @Composable () -> Unit,
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
            Text(text = WakeTimeTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            pickerContent()
            state.display?.let { display ->
                Text(text = display.sentence, style = MaterialTheme.typography.bodyLarge)
            }
            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.semantics { contentDescription = WakeTimeTexts.CONTINUE_BUTTON_LABEL },
            ) {
                Text(WakeTimeTexts.CONTINUE_BUTTON_LABEL)
            }
        }
    }
}

/**
 * Point d'entrée réel. `rememberTimePickerState` n'est initialisé qu'une fois, sans clé : le
 * sourcer sur `state.localTimeIso` réinitialiserait le cadran à chaque recomposition et
 * empêcherait tout glissement du doigt. `is24Hour` vient du réglage système, relu à chaque
 * `ON_RESUME` au même titre que le diagnostic (§13), pour que le cadran et la phrase de
 * confirmation emploient toujours la même convention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeTimeRoute(
    onContinue: (String) -> Unit,
    viewModel: WakeTimeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val initial = LocalTime.parse(viewModel.state.localTimeIso)
    val is24Hour = DateFormat.is24HourFormat(context)
    val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour)

    LaunchedEffect(Unit) {
        viewModel.refresh(is24Hour)
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(DateFormat.is24HourFormat(context))
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pickerState.hour, pickerState.minute) {
        viewModel.onTimeChanged(pickerState.hour, pickerState.minute)
    }

    WakeTimeScreen(
        state = viewModel.state,
        onContinue = { viewModel.continueToSummary(onContinue) },
        pickerContent = { TimePicker(state = pickerState) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun WakeTimeScreenPreview() {
    NiumiTheme {
        WakeTimeScreen(
            state = WakeTimeUiState(),
            onContinue = {},
            pickerContent = {
                val default = LocalTime.parse(DEFAULT_LOCAL_TIME_ISO)
                TimePicker(state = rememberTimePickerState(default.hour, default.minute, is24Hour = true))
            },
        )
    }
}
