package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Barre segmentée (maquette 18, durées de la sonnerie progressive ; VALEURS-DE-DESIGN.md §4) : un
 * choix exclusif sur une barre d'un seul tenant, fond « Tuile grise », coins de 14, marge de 4 ;
 * segments de largeur égale, dessinés sur 40 aux coins de 10, le choisi en encre avec texte 15/500
 * sur encre, les autres en texte encre 15/400. Chaque segment se touche sur toute la hauteur de la
 * barre (48), zone de toucher réservée sans l'épaissir. Rôle `RadioButton` par segment, [contentDescription]
 * sur le groupe. Sombre : mêmes rôles (« Tuile grise » et encre du thème, comme la bulle du volume).
 */
@Composable
fun NiumiSegmentedBar(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NiumiDimens.segmentedBarHeight)
                .clip(NiumiShapes.segmentedBar)
                .background(palette.tileGrey)
                .padding(horizontal = NiumiDimens.segmentedBarPadding)
                .selectableGroup()
                .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.segmentedBarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            Segment(text = label, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun RowScope.Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    // Le toucher prend toute la hauteur de la barre ; seul le fond du segment est dessiné sur 40.
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(NiumiDimens.segmentedBarHeight)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(NiumiDimens.segmentHeight)
                    .clip(NiumiShapes.segment)
                    .then(if (selected) Modifier.background(palette.ink) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = if (selected) NiumiType.chipSelected15 else NiumiType.chip15,
                color = if (selected) palette.onInk else palette.ink,
            )
        }
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiSegmentedBarLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiSegmentedBar(
            labels = listOf("2 min", "5 min", "10 min", "15 min"),
            selectedIndex = 2,
            onSelect = {},
            contentDescription = "Sonnerie progressive : durée pour atteindre le volume maximal",
        )
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiSegmentedBarDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiSegmentedBar(
            labels = listOf("2 min", "5 min", "10 min", "15 min"),
            selectedIndex = 0,
            onSelect = {},
            contentDescription = "Sonnerie progressive : durée pour atteindre le volume maximal",
        )
    }
}
