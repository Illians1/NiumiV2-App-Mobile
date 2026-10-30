package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Carte de section (maquettes de Mehdi du 2026-10-30) : à l'accueil, elle regroupe le cadran, le
 * bloc « Jours de réveil » ou les lignes de réglage ; elle fait aussi les tuiles d'autorisation de
 * l'écran 2 et la carte de sélection de l'écran 4 (même jour). Fond `surface`, coins de 14 ; en clair, sans
 * bord, une ombre en deux couches la détache du fond ([niumiCardShadow]) ; en sombre, sans ombre, un
 * liseré d'un pixel (`sectionCardOutline`). Le contenu n'est pas découpé : rien ne déborde de la
 * carte dans les maquettes. Ne remplace pas [NiumiCard], la carte d'état, noire ou au trait.
 */
@Composable
fun NiumiSectionCard(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .niumiCardShadow(NiumiShapes.card, palette.sectionCardShadow)
                .background(palette.surface, NiumiShapes.card)
                .border(NiumiDimens.hairline, palette.sectionCardOutline, NiumiShapes.card)
                .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
private fun SectionCardsSample() {
    NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDaysPadding) {
        Text("Jours de réveil", style = NiumiType.body16)
    }
    NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDaysPadding) {
        Text("Applications bloquées", style = NiumiType.body16)
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiSectionCardLightPreview() {
    NiumiPreviewFrame(dark = false) { SectionCardsSample() }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiSectionCardDarkPreview() {
    NiumiPreviewFrame(dark = true) { SectionCardsSample() }
}
