package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalette
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Onglets segmentés de l'accueil (« Ponctuel / Quotidien », à droite du titre « Jours de réveil » ;
 * forme demandée par Mehdi le 2026-10-26, non dessinée dans les maquettes) : une capsule « Tuile
 * grise » de 32 de haut, coins de 10, qui prend la largeur de ses libellés. Le segment choisi est
 * un fond encre aux coins de 7, texte sur encre 14/500, comme la barre segmentée de la sonnerie et
 * les jours cochés (choix de Claude, à valider) ; les autres, texte secondaire 14/400.
 * [selectedIndex] nul : aucun n'est choisi (jours libres).
 *
 * Chaque segment se touche sur 48 de haut, la capsule restant dessinée sur 32 ; l'ondulation du
 * toucher reste dans le segment dessiné.
 *
 * États ([NiumiFieldState]), mêmes couleurs que les pastilles de jour posées dessous : grisés en
 * « Réveil désactivé », toujours touchables ; verrouillés en session, annoncés « Verrouillé »,
 * [onSelect] jamais appelé, [onLockedClick] reçoit le toucher.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: NiumiFieldState = NiumiFieldState.Normal,
    onLockedClick: (() -> Unit)? = null,
) {
    val track = LocalNiumiPalette.current.tileGrey
    Row(
        modifier =
            modifier
                .height(NiumiDimens.touchTarget)
                .drawBehind {
                    val height = NiumiDimens.segmentedTabsHeight.toPx()
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(0f, (size.height - height) / 2),
                        size = Size(size.width, height),
                        cornerRadius = CornerRadius(NiumiDimens.segmentedTabsRadius.toPx()),
                    )
                }.padding(horizontal = NiumiDimens.segmentedTabsPadding)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            SegmentedTab(
                label = label,
                selected = index == selectedIndex,
                state = state,
                onClick = { onSelect(index) },
                onLockedClick = onLockedClick,
            )
        }
    }
}

@Composable
private fun SegmentedTab(
    label: String,
    selected: Boolean,
    state: NiumiFieldState,
    onClick: () -> Unit,
    onLockedClick: (() -> Unit)?,
) {
    val palette = LocalNiumiPalette.current
    val colors = segmentedTabColors(palette, selected, state)
    val interactions = remember { MutableInteractionSource() }
    val action =
        when {
            state != NiumiFieldState.Locked -> {
                Modifier.selectable(
                    selected = selected,
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
            }

            onLockedClick != null -> {
                Modifier.clickable(
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Tab,
                    onClick = onLockedClick,
                )
            }

            else -> {
                Modifier.semantics { disabled() }
            }
        }
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .semantics(mergeDescendants = true) {
                    this.selected = selected
                    if (state == NiumiFieldState.Locked) contentDescription = "$label, ${NiumiComponentTexts.LOCKED}"
                }.then(action),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .height(NiumiDimens.segmentedTabHeight)
                    .clip(NiumiShapes.segmentedTab)
                    .background(colors.first)
                    .indication(interactions, ripple())
                    .padding(horizontal = NiumiDimens.segmentedTabPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = if (selected) NiumiType.tabSelected14 else NiumiType.tab14,
                color = colors.second,
            )
        }
    }
}

/** Fond puis texte du segment, sur le modèle des pastilles de jour (`NiumiDayChip`). */
private fun segmentedTabColors(
    palette: NiumiPalette,
    selected: Boolean,
    state: NiumiFieldState,
): Pair<Color, Color> {
    val greyed = state != NiumiFieldState.Normal
    return when {
        selected && !greyed -> palette.ink to palette.onInk
        selected -> palette.disabled to palette.onInk
        !greyed -> Color.Transparent to palette.textSecondary
        else -> Color.Transparent to palette.border
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiSegmentedTabsLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiSegmentedTabs(labels = listOf("Ponctuel", "Quotidien"), selectedIndex = null, onSelect = {})
        NiumiSegmentedTabs(labels = listOf("Ponctuel", "Quotidien"), selectedIndex = 1, onSelect = {})
        NiumiSegmentedTabs(
            labels = listOf("Ponctuel", "Quotidien"),
            selectedIndex = 0,
            onSelect = {},
            state = NiumiFieldState.Greyed,
        )
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiSegmentedTabsDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiSegmentedTabs(
            labels = listOf("Ponctuel", "Quotidien"),
            selectedIndex = 1,
            onSelect = {},
            state = NiumiFieldState.Locked,
        )
    }
}
