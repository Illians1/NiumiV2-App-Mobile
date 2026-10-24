package com.niumi.feature.session.ringtone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.niumi.designsystem.component.NiumiBottomSheet
import com.niumi.designsystem.component.NiumiBubble
import com.niumi.designsystem.component.NiumiRadioIndicator
import com.niumi.designsystem.component.NiumiRoundIconButton
import com.niumi.designsystem.component.NiumiSegmentedBar
import com.niumi.designsystem.component.NiumiSwitch
import com.niumi.designsystem.component.NiumiSwitchSize
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.feature.session.R
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.Ringtone
import com.niumi.system.audio.VolumeRampDurations

/**
 * Feuille « Sonnerie » (maquettes 18 à 20, SPECIFICATION.md §10, étape 40 révisée le 2026-10-23). Composable pur et
 * sans état, à une exception près : la bulle du volume est un état d'affichage ([bubbleVisible]),
 * qui n'a rien à faire dans le `ViewModel` (même principe que le sélecteur de l'écran 5). Sa
 * version sombre est la même composition sous `NiumiTheme(dark = true)` : [LocalNiumiPalette]
 * inverse déjà `ink`/`onInk`, la bulle y compris (M7, étape 40).
 */
@Composable
fun RingtoneSheet(
    state: RingtoneUiState,
    actions: RingtoneActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NiumiBottomSheet(
        onDismiss = onDismiss,
        title = RingtoneTexts.TITLE,
        closeLabel = RingtoneTexts.CLOSE,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(top = NiumiDimens.ringtoneSoundSectionTop)) {
            SoundSection(state = state, actions = actions)
            // Le volume avant la montée progressive (maquettes revues le 2026-10-24) : la sous-ligne
            // de l'interrupteur renvoie au « volume du téléphone » qu'on vient de lire.
            VolumeRow(
                percent = state.volumePercent,
                modifier = Modifier.padding(top = NiumiDimens.ringtoneVolumeRowTop),
            )
            RampSection(settings = state.settings, actions = actions)
            state.message?.let { message ->
                CaptionText(message, Modifier.padding(top = NiumiDimens.ringtoneMessageTop))
            }
        }
    }
}

@Composable
private fun SoundSection(
    state: RingtoneUiState,
    actions: RingtoneActions,
) {
    SectionLabel(RingtoneTexts.SOUND_SECTION)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .semantics { contentDescription = RingtoneTexts.SOUND_GROUP_DESCRIPTION },
    ) {
        state.ringtones.forEachIndexed { index, ringtone ->
            SoundRow(
                row =
                    SoundRowState(
                        ringtone = ringtone,
                        selected = ringtone.key == state.settings.ringtoneKey,
                        isPreviewing = ringtone.key == state.previewingKey,
                        isLast = index == state.ringtones.lastIndex,
                        previewEnabled = !state.isAlarmVolumeZero,
                    ),
                onSelected = { actions.onRingtoneSelected(ringtone.key) },
                onPreviewToggled = { actions.onPreviewToggled(ringtone.key) },
            )
        }
    }
    // Aucune couleur d'alerte : ce n'est pas un incident (§10, même principe que l'écran 14).
    if (state.isAlarmVolumeZero) CaptionText(RingtoneTexts.ALARM_VOLUME_ZERO)
}

/**
 * Ligne « Sonnerie progressive » (maquettes 18 et 20, révision du 2026-10-23) : libellé et, quand
 * l'interrupteur est allumé, « Monte jusqu’au volume du téléphone en {durée} » dessous,
 * l'interrupteur de ligne à droite ; puis la barre segmentée des durées. Éteint : ni sous-ligne
 * ni barre (D6).
 */
@Composable
private fun RampSection(
    settings: AlarmSoundSettings,
    actions: RingtoneActions,
) {
    val palette = LocalNiumiPalette.current
    val seconds = settings.volumeRampSeconds
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = NiumiDimens.ringtoneRampRowTop)
                .heightIn(min = NiumiDimens.ringtoneRampRowMinHeight)
                .topAndMaybeBottomSeparator(palette.separator, bottom = false),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NiumiDimens.ringtoneRampSubtitleGap)) {
            Text(text = RingtoneTexts.RAMP_SECTION, style = NiumiType.body16, color = palette.ink)
            seconds?.let { CaptionText(RingtoneTexts.rampMention(it)) }
        }
        NiumiSwitch(
            checked = seconds != null,
            onCheckedChange = actions.onVolumeRampEnabledChanged,
            contentDescription = RingtoneTexts.RAMP_SWITCH_DESCRIPTION,
            size = NiumiSwitchSize.Row,
        )
    }
    seconds?.let {
        NiumiSegmentedBar(
            labels = RingtoneTexts.RAMP_LABELS,
            selectedIndex = VolumeRampDurations.SECONDS.indexOf(it),
            onSelect = { index -> actions.onVolumeRampSelected(VolumeRampDurations.SECONDS[index]) },
            contentDescription = RingtoneTexts.RAMP_GROUP_DESCRIPTION,
            modifier = Modifier.padding(top = NiumiDimens.ringtoneBarTop),
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = NiumiType.caption13,
        color = LocalNiumiPalette.current.textSecondary,
        modifier = modifier.padding(bottom = NiumiDimens.ringtoneSectionLabelBottom),
    )
}

@Composable
private fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = NiumiType.caption13, color = LocalNiumiPalette.current.textSecondary, modifier = modifier)
}

/** Une ligne de la section « Son » (maquette 16) : le son, sa sélection, sa pré-écoute, sa place dans la liste. */
private data class SoundRowState(
    val ringtone: Ringtone,
    val selected: Boolean,
    val isPreviewing: Boolean,
    val isLast: Boolean,
    val previewEnabled: Boolean,
)

@Composable
private fun SoundRow(
    row: SoundRowState,
    onSelected: () -> Unit,
    onPreviewToggled: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = NiumiDimens.ringtoneRowHeight)
                .topAndMaybeBottomSeparator(palette.separator, bottom = row.isLast)
                .selectable(selected = row.selected, role = Role.RadioButton, onClick = onSelected),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.rowGap),
    ) {
        NiumiRadioIndicator(selected = row.selected)
        Text(
            text = row.ringtone.label,
            style = if (row.selected) NiumiType.labelStrong16 else NiumiType.body16,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        val previewDescription =
            if (row.isPreviewing) {
                RingtoneTexts.STOP_PREVIEW_DESCRIPTION
            } else {
                RingtoneTexts.previewDescription(row.ringtone.label)
            }
        NiumiRoundIconButton(
            icon = painterResource(if (row.isPreviewing) R.drawable.ic_stop else R.drawable.ic_play),
            contentDescription = previewDescription,
            onClick = onPreviewToggled,
            enabled = row.previewEnabled,
            modifier = Modifier.offset(x = NiumiDimens.ringtonePreviewInset),
        )
    }
}

@Composable
private fun VolumeRow(
    percent: Int?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    var bubbleVisible by rememberSaveable { mutableStateOf(false) }
    NiumiBubble(
        visible = bubbleVisible,
        text = RingtoneTexts.VOLUME_HINT,
        onDismissRequest = { bubbleVisible = false },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = NiumiDimens.ringtoneVolumeRowMinHeight)
                    .topAndMaybeBottomSeparator(palette.separator, bottom = false)
                    .selectable(
                        selected = bubbleVisible,
                        role = Role.Button,
                        onClick = { bubbleVisible = !bubbleVisible },
                    ).semantics(mergeDescendants = true) {
                        contentDescription =
                            percent?.let { RingtoneTexts.volumeDescription(it) } ?: RingtoneTexts.PHONE_VOLUME
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = RingtoneTexts.PHONE_VOLUME, style = NiumiType.body16, color = palette.ink)
            percent?.let { Text(text = "$it %", style = NiumiType.labelStrong16, color = palette.ink) }
        }
    }
}

/** Séparateur d'un pixel au-dessus d'une rangée, et en dessous pour la dernière. */
private fun Modifier.topAndMaybeBottomSeparator(
    color: Color,
    bottom: Boolean,
): Modifier =
    drawBehind {
        val stroke = NiumiDimens.hairline.toPx()
        drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
        if (bottom) {
            val y = size.height - stroke / 2
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
