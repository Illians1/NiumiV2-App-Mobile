package com.niumi.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes

/**
 * Interrupteur général (VALEURS-DE-DESIGN.md §4 : 52 × 32, pastille de 26 ; maquettes 13 et 14),
 * dans une zone de toucher de 44 de haut. Activé : piste ambre, pastille encre ; éteint : piste
 * `border`, pastille `surface`. Le thème sombre n'a pas de maquette : mêmes rôles. Inactif : aucune
 * action de clic, piste `disabledFill`.
 */
@Composable
fun NiumiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val palette = LocalNiumiPalette.current
    val travel = NiumiDimens.switchWidth - NiumiDimens.switchThumb - NiumiDimens.switchThumbInset * 2
    val position by animateFloatAsState(targetValue = if (checked) 1f else 0f, label = "thumb")
    val action =
        if (enabled) {
            Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
        } else {
            Modifier.semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                disabled()
            }
        }
    Box(
        modifier =
            modifier
                .heightIn(min = NiumiDimens.minTouch)
                .then(action)
                .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = NiumiDimens.switchWidth, height = NiumiDimens.switchHeight)
                    .clip(NiumiShapes.pill)
                    .background(
                        when {
                            !enabled -> palette.disabledFill
                            checked -> palette.amber
                            else -> palette.border
                        },
                    ).padding(NiumiDimens.switchThumbInset),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(x = travel * position)
                        .size(NiumiDimens.switchThumb)
                        .clip(NiumiShapes.pill)
                        .background(if (checked && enabled) palette.ink else palette.surface),
            )
        }
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiSwitchLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiCard(filled = true) { NiumiSwitch(checked = true, onCheckedChange = {}) }
        NiumiCard(filled = false) { NiumiSwitch(checked = false, onCheckedChange = {}) }
    }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiSwitchDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiSwitch(checked = true, onCheckedChange = {})
        NiumiSwitch(checked = false, onCheckedChange = {})
    }
}
