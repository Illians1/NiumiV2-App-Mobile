package com.niumi.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Lien discret (VALEURS-DE-DESIGN.md §4) : texte souligné, taille 14, couleur du texte
 * secondaire, dans une zone de toucher de 44 (maquette 19, « Je n'ai pas accès à mon Niumi
 * Point »). Compose ne règle pas l'écart du soulignement (4 dans la maquette) : soulignement
 * standard de la police.
 */
@Composable
fun NiumiDiscreetLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = NiumiDimens.minTouch)
                .widthIn(min = NiumiDimens.minTouch)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = NiumiDimens.linkHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = NiumiType.secondary14,
            color = LocalNiumiPalette.current.textSecondary,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiDiscreetLinkLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiDiscreetLink(text = "Je n’ai pas accès à mon Niumi Point", onClick = {}) }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiDiscreetLinkDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiDiscreetLink(text = "Je n’ai pas accès à mon Niumi Point", onClick = {}) }
}
