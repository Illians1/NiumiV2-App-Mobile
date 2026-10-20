package com.niumi.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bouton secondaire au trait (VALEURS-DE-DESIGN.md §4) : même gabarit que le bouton principal,
 * bord d'un pixel et texte à l'encre (maquette 23, « Recopier le texte »).
 */
@Composable
fun NiumiOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NiumiDimens.primaryButton)
                .clip(NiumiShapes.button)
                .border(NiumiDimens.hairline, palette.ink, NiumiShapes.button)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = NiumiType.button17, color = palette.ink)
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiOutlinedButtonLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiOutlinedButton(text = "Recopier le texte", onClick = {}) }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiOutlinedButtonDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiOutlinedButton(text = "Recopier le texte", onClick = {}) }
}
