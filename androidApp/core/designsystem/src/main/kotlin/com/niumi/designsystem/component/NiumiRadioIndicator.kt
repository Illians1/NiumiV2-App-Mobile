package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Indicateur rond d'un choix exclusif (maquette 16, section « Son ») : cercle de 22, trait
 * `border` d'1,5, ou encre pleine avec un point de 12 une fois choisi. Purement visuel : le rôle
 * `RadioButton` et la sélection vivent sur la ligne qui le contient (`Modifier.selectable`).
 */
@Composable
fun NiumiRadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .size(NiumiDimens.ringtoneRadio)
                .border(NiumiDimens.ringtoneRadioStroke, if (selected) palette.ink else palette.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(NiumiDimens.ringtoneRadioDot).background(palette.ink, CircleShape))
        }
    }
}

@Preview(name = "Clair", widthDp = 120)
@Composable
private fun NiumiRadioIndicatorPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiRadioIndicator(selected = false)
        NiumiRadioIndicator(selected = true)
    }
}
