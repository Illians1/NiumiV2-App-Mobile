package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes

/**
 * Indicateur de panneau (maquettes 01 et 02) : un trait de 24 × 6 par panneau, espacés de 8, encre
 * pour le panneau [current] (compté depuis 0), `disabled` pour les autres. Annoncé une seule fois
 * par [label] (« Panneau 1 sur 2 », du `Texts` de l'écran) ; jamais touchable.
 */
@Composable
fun NiumiPageIndicator(
    count: Int,
    current: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.pageIndicatorGap),
    ) {
        repeat(count) { index ->
            Box(
                modifier =
                    Modifier
                        .size(width = NiumiDimens.pageIndicatorWidth, height = NiumiDimens.pageIndicatorHeight)
                        .clip(NiumiShapes.pill)
                        .background(if (index == current) palette.ink else palette.disabled),
            )
        }
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiPageIndicatorLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiPageIndicator(count = 2, current = 0, label = "Panneau 1 sur 2") }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiPageIndicatorDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiPageIndicator(count = 2, current = 1, label = "Panneau 2 sur 2") }
}
