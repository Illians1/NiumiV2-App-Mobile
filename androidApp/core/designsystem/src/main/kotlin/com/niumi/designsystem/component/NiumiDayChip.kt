package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalette
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Pastille de jour de l'accueil (VALEURS-DE-DESIGN.md §4 : cercle de 44 ; SPECIFICATION §9) : une
 * case à cocher nommée d'après le jour entier ([accessibilityLabel], « Lundi »), qui affiche
 * l'initiale ([label], « L »). Couleurs relevées dans les maquettes 13 à 15 :
 *
 * - [NiumiFieldState.Normal] : cochée, fond encre et lettre sur encre 16/500 ; sinon bord
 *   `border` et lettre `textSecondary` 16/400 ;
 * - [NiumiFieldState.Greyed] : cochée, fond `disabled` ; sinon bord `disabledBorder` et lettre
 *   `border`. Toujours modifiable ;
 * - [NiumiFieldState.Locked] : même aspect que grisé, annoncée « Lundi, Verrouillé », cochée ou
 *   non. [onToggle] n'est jamais appelé ; [onLockedClick], s'il est fourni, reçoit le toucher (message de l'étape 45).
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiDayChip(
    label: String,
    accessibilityLabel: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    state: NiumiFieldState = NiumiFieldState.Normal,
    onLockedClick: (() -> Unit)? = null,
) {
    val palette = LocalNiumiPalette.current
    val colors = dayChipColors(palette, checked, state)
    val action =
        when {
            state != NiumiFieldState.Locked -> {
                Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            }

            onLockedClick != null -> {
                Modifier.clickable(role = Role.Checkbox, onClick = onLockedClick)
            }

            else -> {
                Modifier.semantics { disabled() }
            }
        }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(NiumiDimens.dayChip)
                .clip(NiumiShapes.pill)
                .background(colors.fill)
                .border(NiumiDimens.hairline, colors.outline, NiumiShapes.pill)
                .semantics(mergeDescendants = true) {
                    // « Verrouillé » s'ajoute au nom : un `stateDescription` remplacerait « coché »
                    // dans l'annonce, et TalkBack ne dirait plus quels jours sonnent.
                    contentDescription =
                        if (state == NiumiFieldState.Locked) {
                            "$accessibilityLabel, ${NiumiComponentTexts.LOCKED}"
                        } else {
                            accessibilityLabel
                        }
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                }.then(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (checked) NiumiType.label16 else NiumiType.body16,
            color = colors.content,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

private data class DayChipColors(
    val fill: Color,
    val outline: Color,
    val content: Color,
)

private fun dayChipColors(
    palette: NiumiPalette,
    checked: Boolean,
    state: NiumiFieldState,
): DayChipColors {
    val greyed = state != NiumiFieldState.Normal
    return when {
        checked && !greyed -> DayChipColors(fill = palette.ink, outline = palette.ink, content = palette.onInk)
        checked -> DayChipColors(fill = palette.disabled, outline = palette.disabled, content = palette.onInk)
        !greyed -> DayChipColors(fill = Color.Transparent, outline = palette.border, content = palette.textSecondary)
        else -> DayChipColors(fill = Color.Transparent, outline = palette.disabledBorder, content = palette.border)
    }
}

@Composable
private fun WeekSample(state: NiumiFieldState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("L", "M", "M", "J", "V", "S", "D").forEachIndexed { index, day ->
            NiumiDayChip(
                label = day,
                accessibilityLabel = day,
                checked = index < WORKDAYS,
                onToggle = {},
                state = state,
            )
        }
    }
}

private const val WORKDAYS = 5

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiDayChipLightPreview() {
    NiumiPreviewFrame(dark = false) {
        WeekSample(NiumiFieldState.Normal)
        WeekSample(NiumiFieldState.Greyed)
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiDayChipDarkPreview() {
    NiumiPreviewFrame(dark = true) { WeekSample(NiumiFieldState.Locked) }
}
