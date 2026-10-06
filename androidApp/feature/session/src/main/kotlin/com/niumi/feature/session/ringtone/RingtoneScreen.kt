package com.niumi.feature.session.ringtone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.session.R
import com.niumi.feature.session.ui.AlarmSoundTexts
import com.niumi.system.audio.Ringtone
import com.niumi.system.audio.VolumeRampDurations

/**
 * Écran 14 (SPEC_ANDROID §15, Lot 7). Composable pur et sans état. La sonnerie choisie est signalée
 * par l'Ambre **et** le gras (décision utilisateur du 2026-10-01) : la charte §3 interdit qu'une
 * information dépende uniquement de la couleur, et §15 impose « l'entrée choisie en Ambre ». Le
 * rôle `RadioButton` porte la sélection pour TalkBack, au-delà de la couleur et du gras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneScreen(
    state: RingtoneUiState,
    actions: RingtoneActions,
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
            Text(text = RingtoneTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            if (state.isSessionArmed) {
                Text(text = RingtoneTexts.ARMED_BANNER, style = MaterialTheme.typography.bodyMedium)
            }

            Column(modifier = Modifier.selectableGroup()) {
                state.ringtones.forEach { ringtone ->
                    RingtoneRow(ringtone = ringtone, state = state, actions = actions)
                }
            }

            if (state.isAlarmVolumeZero) {
                // Aucune couleur d'alerte : ce n'est pas un incident (§15, charte §5).
                Text(text = RingtoneTexts.ALARM_VOLUME_ZERO, style = MaterialTheme.typography.bodyMedium)
            }

            VolumeSection(state = state, actions = actions)

            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RingtoneRow(
    ringtone: Ringtone,
    state: RingtoneUiState,
    actions: RingtoneActions,
) {
    val selected = ringtone.key == state.settings.ringtoneKey
    val isPreviewing = ringtone.key == state.previewingKey
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { actions.onRingtoneSelected(ringtone.key) },
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ringtone.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        val previewDescription =
            if (isPreviewing) {
                RingtoneTexts.STOP_PREVIEW_DESCRIPTION
            } else {
                RingtoneTexts.previewDescription(
                    ringtone.label,
                )
            }
        IconButton(
            onClick = { actions.onPreviewToggled(ringtone.key) },
            enabled = !state.isAlarmVolumeZero,
            modifier = Modifier.semantics { contentDescription = previewDescription },
        ) {
            Image(
                painter = painterResource(if (isPreviewing) R.drawable.ic_stop else R.drawable.ic_play),
                contentDescription = null,
                colorFilter =
                    ColorFilter.tint(
                        if (state.isAlarmVolumeZero) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
            )
        }
    }
}

/**
 * Section « Volume » (§15) : interrupteur, sous-texte recalculé sur la durée **qu'on obtiendrait**
 * en l'activant (§27, un interrupteur coupé ne doit pas afficher la dernière durée perdue), puis le
 * choix segmenté seulement quand il est actif. Rayon de 8 dp, comme `WakeTimeScreen` (charte §16,
 * décision 1 de `ETAPE-24.md`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSection(
    state: RingtoneUiState,
    actions: RingtoneActions,
) {
    val rampSeconds = state.settings.volumeRampSeconds
    val rampEnabled = rampSeconds != null
    val descriptionSeconds = rampSeconds ?: VolumeRampDurations.DEFAULT_SECONDS

    Text(text = RingtoneTexts.VOLUME_SECTION_TITLE, style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = RingtoneTexts.RAMP_SWITCH_LABEL, modifier = Modifier.weight(1f))
        Switch(checked = rampEnabled, onCheckedChange = actions.onVolumeRampEnabledChanged)
    }
    Text(
        text = RingtoneTexts.rampDescription(descriptionSeconds),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (rampEnabled) {
        val rampColors =
            SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                inactiveContainerColor = MaterialTheme.colorScheme.background,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.outline,
            )
        SingleChoiceSegmentedButtonRow {
            VolumeRampDurations.SECONDS.forEachIndexed { index, seconds ->
                SegmentedButton(
                    selected = seconds == rampSeconds,
                    onClick = { actions.onVolumeRampSecondsChanged(seconds) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = VolumeRampDurations.SECONDS.size,
                            baseShape = RAMP_ITEM_SHAPE,
                        ),
                    colors = rampColors,
                    icon = { SegmentedButtonDefaults.Icon(active = seconds == rampSeconds) },
                    label = { Text(AlarmSoundTexts.durationLabel(seconds)) },
                )
            }
        }
    }
}

private val RAMP_ITEM_SHAPE = RoundedCornerShape(8.dp)

/** Point d'entrée réel : relit le volume et la session à `ON_RESUME`, arrête la pré-écoute à `ON_PAUSE` (§15). */
@Composable
fun RingtoneRoute(viewModel: RingtoneViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewModel.refresh()
                    Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    RingtoneScreen(
        state = viewModel.state,
        actions =
            RingtoneActions(
                onRingtoneSelected = viewModel::onRingtoneSelected,
                onPreviewToggled = viewModel::onPreviewToggled,
                onVolumeRampEnabledChanged = viewModel::onVolumeRampEnabledChanged,
                onVolumeRampSecondsChanged = viewModel::onVolumeRampSecondsChanged,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun RingtoneScreenPreview() {
    NiumiTheme {
        RingtoneScreen(
            state = RingtoneUiState(isLoading = false),
            actions = RingtoneActions({}, {}, {}, {}),
        )
    }
}
