package com.niumi.designsystem.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bouton principal (VALEURS-DE-DESIGN.md §4, « Les boutons, par rôle ») : pleine largeur, 56 de
 * haut, coins de 14, fond encre et texte sur encre. Inactif (D9) : fond `disabledFill`, texte
 * `disabledText`, et **aucune action de clic** : un bouton inactif n'est pas seulement grisé.
 */
@Composable
fun NiumiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalNiumiPalette.current
    val action =
        if (enabled) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier.semantics(mergeDescendants = true) {
                role = Role.Button
                disabled()
            }
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NiumiDimens.primaryButton)
                .clip(NiumiShapes.button)
                .background(if (enabled) palette.ink else palette.disabledFill)
                .then(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = NiumiType.button17,
            color = if (enabled) palette.onInk else palette.disabledText,
        )
    }
}

@Preview(name = "Clair")
@Composable
private fun NiumiPrimaryButtonLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiPrimaryButton(text = "Continuer", onClick = {})
        NiumiPrimaryButton(text = "Continuer", onClick = {}, enabled = false)
    }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiPrimaryButtonDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiPrimaryButton(text = "Scanner le Niumi Point", onClick = {})
        NiumiPrimaryButton(text = "Terminer", onClick = {}, enabled = false)
    }
}
