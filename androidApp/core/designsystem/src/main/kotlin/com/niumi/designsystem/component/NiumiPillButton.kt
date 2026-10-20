package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Pastille de choix en pilule (maquette 16, durées « Non », « 1 min »… de la sonnerie
 * progressive) : un bouton radio. Choisie : fond encre, texte sur encre 15/500 ; sinon bord d'un
 * pixel `border`, texte encre 15/400. Coins égaux à la moitié de la hauteur (VALEURS §4).
 */
@Composable
fun NiumiPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = NiumiDimens.pillButton,
) {
    val palette = LocalNiumiPalette.current
    val look =
        if (selected) {
            Modifier.background(palette.ink)
        } else {
            Modifier.border(NiumiDimens.hairline, palette.border, NiumiShapes.pill)
        }
    Box(
        modifier =
            modifier
                .height(height)
                .widthIn(min = NiumiDimens.minTouch)
                .clip(NiumiShapes.pill)
                .then(look)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) NiumiType.chipSelected15 else NiumiType.chip15,
            color = if (selected) palette.onInk else palette.ink,
        )
    }
}

/**
 * Pilule d'action (maquettes 10 et 12) : « Choisir les applications » (pleine, 48 de haut) et
 * « Modifier » (au trait encre, 44 de haut). Texte 16/500, 22 de marge de chaque côté.
 */
@Composable
fun NiumiActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    height: Dp = NiumiDimens.pillButton,
) {
    val palette = LocalNiumiPalette.current
    val look =
        if (filled) {
            Modifier.background(palette.ink)
        } else {
            Modifier.border(NiumiDimens.hairline, palette.ink, NiumiShapes.pill)
        }
    Box(
        modifier =
            modifier
                .height(height)
                .widthIn(min = NiumiDimens.minTouch)
                .clip(NiumiShapes.pill)
                .then(look)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = NiumiDimens.pillHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = NiumiType.label16, color = if (filled) palette.onInk else palette.ink)
    }
}

@Composable
private fun PillsSample() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap)) {
        listOf("Non", "1 min", "2 min", "5 min", "10 min").forEach { label ->
            NiumiPillButton(text = label, selected = label == "2 min", onClick = {}, modifier = Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap)) {
        NiumiActionPill(text = "Modifier", onClick = {})
        NiumiActionPill(
            text = "Choisir les applications",
            onClick = {},
            filled = true,
            height = NiumiDimens.pillButtonLarge,
        )
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiPillButtonLightPreview() {
    NiumiPreviewFrame(dark = false) { PillsSample() }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiPillButtonDarkPreview() {
    NiumiPreviewFrame(dark = true) { PillsSample() }
}
