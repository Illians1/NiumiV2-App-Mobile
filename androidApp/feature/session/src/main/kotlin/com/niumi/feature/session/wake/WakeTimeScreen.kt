package com.niumi.feature.session.wake

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * Choix de l'heure de réveil et du début du blocage (écran 5, SPEC_ANDROID §15). Composable pur et
 * sans état, à une exception près : l'ouverture du sélecteur de début de blocage est un état
 * d'affichage, qui n'a rien à faire dans le `ViewModel`. Le `TimePicker` Material 3 n'expose aucun
 * callback de changement, [onTimeChanged] est donc appelé depuis un `LaunchedEffect` observant
 * `pickerState.hour`/`pickerState.minute` dans la `Route`.
 */
@Composable
fun WakeTimeScreen(
    state: WakeTimeUiState,
    actions: WakeTimeActions,
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
            BlockingSection(
                state = state,
                onModeChanged = actions.onBlockingModeChanged,
                onTimeChanged = actions.onBlockingTimeChanged,
            )
            Button(
                onClick = actions.onContinue,
                enabled = state.canContinue,
                modifier = Modifier.semantics { contentDescription = WakeTimeTexts.CONTINUE_BUTTON_LABEL },
            ) {
                Text(WakeTimeTexts.CONTINUE_BUTTON_LABEL)
            }
        }
    }
}

/**
 * Section « Blocage des applications » (§15, Lot 6). Deux choix exclusifs, sans carte : la charte
 * §16 préfère l'espace à un conteneur supplémentaire, et son rejet des « grandes capsules très
 * arrondies » vaut aussi pour la forme par défaut du `SegmentedButton` — d'où [BLOCKING_ITEM_SHAPE].
 *
 * Choisir « À partir de » ouvre le sélecteur (§15). L'annuler sans heure connue laisse
 * « Maintenant » sélectionné : le `ViewModel` ne bascule qu'à la confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockingSection(
    state: WakeTimeUiState,
    onModeChanged: (Boolean) -> Unit,
    onTimeChanged: (Int, Int) -> Unit,
) {
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    // Le `ColorScheme` Niumi ne définit pas `secondaryContainer`, que le `SegmentedButton` emploie
    // par défaut : sans ces couleurs, le choix sélectionné prendrait la teinte Material de base au
    // lieu de l'Ambre, que la charte §3 réserve à ce qui est « actif, engagé ou en cours » — et
    // « heure sélectionnée » y figure nommément. Texte sombre sur Ambre (§3, contraste).
    val blockingColors =
        SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primary,
            activeContentColor = MaterialTheme.colorScheme.onPrimary,
            activeBorderColor = MaterialTheme.colorScheme.primary,
            inactiveContainerColor = MaterialTheme.colorScheme.background,
            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            inactiveBorderColor = MaterialTheme.colorScheme.outline,
        )

    Text(text = WakeTimeTexts.BLOCKING_SECTION_TITLE, style = MaterialTheme.typography.titleMedium)
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = state.isBlockingImmediate,
            onClick = { onModeChanged(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2, baseShape = BLOCKING_ITEM_SHAPE),
            colors = blockingColors,
            // La coche : « l'information importante ne doit jamais dépendre uniquement de la
            // couleur » (charte §3).
            icon = { SegmentedButtonDefaults.Icon(active = state.isBlockingImmediate) },
            label = { Text(WakeTimeTexts.BLOCKING_NOW_LABEL) },
        )
        SegmentedButton(
            selected = !state.isBlockingImmediate,
            onClick = {
                onModeChanged(false)
                pickerVisible = true
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2, baseShape = BLOCKING_ITEM_SHAPE),
            colors = blockingColors,
            icon = { SegmentedButtonDefaults.Icon(active = !state.isBlockingImmediate) },
            label = { Text(WakeTimeTexts.BLOCKING_AT_LABEL) },
        )
    }
    if (!state.isBlockingImmediate) {
        // L'heure retenue (§15) : la saisie, dans la convention du système, comme le cadran du
        // réveil — c'est un champ de saisie, et le sélecteur se rouvre dessus. L'instant obtenu est
        // énoncé par la phrase de confirmation, juste en dessous.
        TextButton(onClick = { pickerVisible = true }) {
            Text(text = state.blockingTimeLabel.orEmpty())
        }
    }
    state.blockingSentence?.let { sentence ->
        Text(text = sentence, style = MaterialTheme.typography.bodyLarge)
    }
    state.blockingMessage?.let { message ->
        // Aucune couleur d'alerte : ce refus n'est pas un incident (§15 ; charte §5, le Terracotta
        // reste réservé aux erreurs et aux actions destructives).
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }

    if (pickerVisible) {
        BlockingStartDialog(
            initialLocalTimeIso = state.blockingLocalTimeIso ?: DEFAULT_BLOCKING_START_LOCAL_TIME_ISO,
            use24Hour = state.use24Hour,
            onConfirm = { hour, minute ->
                pickerVisible = false
                onTimeChanged(hour, minute)
            },
            onDismiss = { pickerVisible = false },
        )
    }
}

/** Sélecteur d'heure Material 3 dans une boîte de dialogue (§15), même convention que le cadran. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockingStartDialog(
    initialLocalTimeIso: String,
    use24Hour: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = LocalTime.parse(initialLocalTimeIso)
    val pickerState = rememberTimePickerState(initial.hour, initial.minute, use24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WakeTimeTexts.BLOCKING_SECTION_TITLE) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(WakeTimeTexts.BLOCKING_CONFIRM_LABEL)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(WakeTimeTexts.BLOCKING_DISMISS_LABEL) }
        },
    )
}

/**
 * Rayon modéré plutôt que la capsule par défaut du `SegmentedButton` : charte §16, « éviter les
 * interfaces constituées d'une succession de grandes capsules très arrondies ». Arbitrage validé
 * avec l'utilisateur le 2026-09-17.
 */
private val BLOCKING_ITEM_SHAPE = RoundedCornerShape(8.dp)

/**
 * Point d'entrée réel. `rememberTimePickerState` n'est initialisé qu'une fois, sans clé : le
 * sourcer sur `state.localTimeIso` réinitialiserait le cadran à chaque recomposition et
 * empêcherait tout glissement du doigt. `is24Hour` vient du réglage système, relu à chaque
 * `ON_RESUME` au même titre que le diagnostic (§13), pour que le cadran, le sélecteur de début de
 * blocage et les phrases de confirmation emploient toujours la même convention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeTimeRoute(
    onContinue: (WakeTimeChoice) -> Unit,
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
        actions =
            WakeTimeActions(
                onContinue = {
                    viewModel.continueToSummary { localTimeIso, blockingLocalTimeIso ->
                        onContinue(WakeTimeChoice(localTimeIso, blockingLocalTimeIso))
                    }
                },
                onBlockingModeChanged = viewModel::onBlockingModeChanged,
                onBlockingTimeChanged = viewModel::onBlockingTimeChanged,
            ),
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
            actions = WakeTimeActions(onContinue = {}, onBlockingModeChanged = {}, onBlockingTimeChanged = { _, _ -> }),
            pickerContent = {
                val default = LocalTime.parse(DEFAULT_LOCAL_TIME_ISO)
                TimePicker(state = rememberTimePickerState(default.hour, default.minute, is24Hour = true))
            },
        )
    }
}
