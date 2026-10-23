package com.niumi.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bouton texte : libellé à l'encre, taille 16, sans fond ni soulignement, dans une zone de toucher
 * de 44 (maquette 22, « Ouvrir Niumi » de l'écran de blocage, étape 39). Distinct du lien discret,
 * souligné, en 14 et en texte secondaire : la maquette en fait une action à part entière, sous le
 * bouton principal.
 */
@Composable
fun NiumiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = NiumiDimens.minTouch)
                .widthIn(min = NiumiDimens.minTouch)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = NiumiDimens.linkHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = NiumiType.body16,
            color = LocalNiumiPalette.current.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiTextButtonLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiTextButton(text = "Ouvrir Niumi", onClick = {}) }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiTextButtonDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiTextButton(text = "Ouvrir Niumi", onClick = {}) }
}
