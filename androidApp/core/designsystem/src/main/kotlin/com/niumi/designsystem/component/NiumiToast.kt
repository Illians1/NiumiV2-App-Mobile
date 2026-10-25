package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bulle de message en bas de l'écran (forme de la maquette 27, sans la coche ; décision M10 de Mehdi
 * du 2026-10-25) : fond encre, texte sur encre 16, coins de 16, ombre. Annoncée par TalkBack dès
 * qu'elle paraît. L'écran la pose au-dessus de son contenu et décide quand elle disparaît.
 */
@Composable
fun NiumiToast(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(NiumiDimens.toastElevation, NiumiShapes.toast)
                .clip(NiumiShapes.toast)
                .background(palette.ink)
                .padding(NiumiDimens.toastPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text = text, style = NiumiType.body16, color = palette.onInk)
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiToastPreview() {
    NiumiPreviewFrame(dark = false) { NiumiToast(text = "Modifiable après avoir scanné ton Niumi Spot.") }
}
