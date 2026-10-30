package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Carte (VALEURS-DE-DESIGN.md §4 : coins de 14, bord d'un pixel). [filled] : fond encre et texte
 * sur encre, la carte « Réveil activé » (maquette 14) ; sinon fond `surface` et bord `border`, les
 * cartes « Réveil désactivé » et « Blocage en cours » (maquettes 13 et 15). Le contenu reçoit la
 * couleur de texte de la carte ; le texte secondaire posé sur l'encre est `onInkSecondary`. En
 * clair, l'ombre des cartes de l'accueil ([niumiCardShadow]), depuis la dernière version des
 * maquettes du 2026-10-30 ; aucune en sombre.
 */
@Composable
fun NiumiCard(
    filled: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = NiumiDimens.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .niumiCardShadow(NiumiShapes.card, palette.sectionCardShadow)
                .clip(NiumiShapes.card)
                .background(if (filled) palette.ink else palette.surface)
                .border(NiumiDimens.hairline, if (filled) palette.ink else palette.border, NiumiShapes.card)
                .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (filled) palette.onInk else palette.ink) {
            content()
        }
    }
}

@Composable
private fun CardsSample() {
    val palette = LocalNiumiPalette.current
    NiumiCard(filled = true) {
        Text("Réveil activé", style = NiumiType.label16)
        Text("Blocage des apps dans 56 min", style = NiumiType.secondary14, color = palette.onInkSecondary)
    }
    NiumiCard(filled = false) {
        Text("Réveil désactivé", style = NiumiType.label16)
        Text("Aucun réveil ni blocage prévu", style = NiumiType.secondary14, color = palette.textSecondary)
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiCardLightPreview() {
    NiumiPreviewFrame(dark = false) { CardsSample() }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiCardDarkPreview() {
    NiumiPreviewFrame(dark = true) { CardsSample() }
}
