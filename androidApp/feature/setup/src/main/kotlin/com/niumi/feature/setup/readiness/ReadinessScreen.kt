package com.niumi.feature.setup.readiness

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome

/**
 * Diagnostic avant activation (SPEC_ANDROID §13). Composable pur et sans état.
 *
 * **Une seule action principale à la fois** : seul `state.primary` porte un bouton, les autres
 * contrôles ne sont qu'affichés. Un recours qui n'existe pas encore laisse le bouton inactif
 * plutôt que de promettre un écran absent (§15, « ne jamais afficher un faux état de fiabilité »).
 */
@Composable
fun ReadinessScreen(
    state: ReadinessUiState,
    onPrimaryAction: (ReadinessItem) -> Unit,
    onChooseWakeTime: () -> Unit,
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
            Text(text = ReadinessMessages.TITLE, style = MaterialTheme.typography.headlineSmall)
            if (state.isDeviceReady && !state.isLoading) {
                Text(text = ReadinessMessages.ALL_CLEAR, style = MaterialTheme.typography.bodyLarge)
                // Continuation de parcours, pas une remédiation : `isDeviceReady` et non
                // `isAllowed`, faux tant qu'aucune heure n'est choisie — c'est précisément
                // l'écran vers lequel ce bouton mène (§13, exception documentée).
                Button(
                    onClick = onChooseWakeTime,
                    modifier = Modifier.semantics { contentDescription = ReadinessMessages.CHOOSE_WAKE_TIME_LABEL },
                ) {
                    Text(ReadinessMessages.CHOOSE_WAKE_TIME_LABEL)
                }
            }
            state.primary?.let { primary -> PrimaryAction(primary, onPrimaryAction) }
            state.items
                .filter { it.id != state.primary?.id }
                .forEach { item ->
                    Text(
                        text = "${statusMarker(item)}  ${item.summary}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // Une étape de parcours satisfaite reste modifiable (§11.1, §12.1) : sans ce
                    // recours, les écrans 3 et 4 seraient inatteignables une fois la ligne verte.
                    if (item.isRevisitable) {
                        TextButton(
                            onClick = { onPrimaryAction(item) },
                            modifier = Modifier.semantics { contentDescription = item.actionLabel },
                        ) {
                            Text(item.actionLabel)
                        }
                    }
                }
        }
    }
}

@Composable
private fun PrimaryAction(
    item: ReadinessItem,
    onPrimaryAction: (ReadinessItem) -> Unit,
) {
    Text(text = item.message, style = MaterialTheme.typography.bodyLarge)
    explanationFor(item.action)?.let { explanation ->
        Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
    }
    Button(
        onClick = { onPrimaryAction(item) },
        enabled = item.isActionAvailable,
        modifier = Modifier.semantics { contentDescription = item.actionLabel },
    ) {
        Text(item.actionLabel)
    }
}

private fun statusMarker(item: ReadinessItem): String =
    when {
        item.outcome == ReadinessOutcome.PASSED -> "✓"
        item.severity == ReadinessSeverityDto.WARNING -> "!"
        else -> "✗"
    }

/**
 * Point d'entrée réel. Recalcule le diagnostic à chaque `ON_RESUME` — « recalculer l'état après
 * chaque retour des réglages » (§13) — et n'ouvre jamais un réglage à la place de l'utilisateur.
 */
@Composable
fun ReadinessRoute(
    onStartPairing: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onChooseWakeTime: () -> Unit,
    viewModel: ReadinessViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Le résultat est ignoré : c'est le diagnostic relancé sur ON_RESUME qui fait foi, jamais
    // la valeur renvoyée par la boîte de dialogue.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ReadinessScreen(
        state = viewModel.state,
        onChooseWakeTime = onChooseWakeTime,
        onPrimaryAction = { item ->
            when {
                item.id == ReadinessCheckId.BATTERY_OPTIMIZATION &&
                    item.actionLabel == ReadinessMessages.BATTERY_CONFIRM_LABEL -> {
                    viewModel.confirmBatteryExemption()
                }

                // Recours internes : une destination de l'application, jamais un réglage système
                // (`settingsIntentFor` renvoie `null` pour ces deux actions).
                item.action == ReadinessAction.StartPairing -> {
                    onStartPairing()
                }

                item.action == ReadinessAction.OpenAppPicker -> {
                    onOpenAppPicker()
                }

                // Ne se produit que pendant la surveillance d'une session (§13.1) : avant tout
                // choix d'heure, `FUTURE_TRIGGER` est `NOT_APPLICABLE` et n'est jamais affiché.
                item.action == ReadinessAction.FixTime -> {
                    onChooseWakeTime()
                }

                item.action == ReadinessAction.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(applicationDetails(context.packageName))
                    }
                }

                else -> {
                    settingsIntentFor(item.action, context.packageName)?.let(context::startActivity)
                    if (item.id == ReadinessCheckId.BATTERY_OPTIMIZATION) viewModel.onBatterySettingsOpened()
                }
            }
        },
    )
}

/**
 * Avant Android 13, `POST_NOTIFICATIONS` n'existe pas : seule la désactivation des notifications
 * dans les réglages de l'application peut expliquer l'échec, il n'y a rien à demander.
 */
private fun applicationDetails(packageName: String): Intent =
    Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", packageName, null),
    )

@Preview(showBackground = true)
@Composable
private fun ReadinessScreenPreview() {
    NiumiTheme {
        ReadinessScreen(
            state =
                ReadinessUiState(
                    items =
                        listOf(
                            ReadinessItem(
                                id = ReadinessCheckId.ALARM_VOLUME,
                                message = ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME),
                                label = ReadinessMessages.labelFor(ReadinessCheckId.ALARM_VOLUME),
                                severity = ReadinessSeverityDto.BLOCKING_FOR_ALARM,
                                outcome = ReadinessOutcome.FAILED,
                                action = ReadinessAction.OpenSoundSettings,
                                actionLabel = ReadinessMessages.actionLabelFor(ReadinessCheckId.ALARM_VOLUME),
                                isActionAvailable = true,
                            ),
                        ),
                    isLoading = false,
                ).let { it.copy(primary = it.items.first()) },
            onPrimaryAction = {},
            onChooseWakeTime = {},
        )
    }
}
