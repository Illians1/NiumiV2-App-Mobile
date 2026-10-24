package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bouton principal (VALEURS-DE-DESIGN.md §4, « Les boutons, par rôle ») : pleine largeur, 56 de
 * haut, coins de 14, fond encre et texte sur encre. Inactif (D9) : fond `disabledFill`, texte
 * `disabledText`, et **aucune action de clic** : un bouton inactif n'est pas seulement grisé.
 *
 * [size] : [NiumiButtonSize.Authorize] pour « Autoriser » dans une tuile de l'écran 2 (46 de haut,
 * coins de 12, texte 15/500, VALEURS §3 et §4) ; même rôle, même couleurs.
 */
@Composable
fun NiumiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: NiumiButtonSize = NiumiButtonSize.Primary,
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
                // 48 dp réservés autour du dessin de 46 d'« Autoriser » (décision du 2026-10-20) ;
                // sans effet sur le bouton principal de 56.
                .minimumInteractiveComponentSize()
                .fillMaxWidth()
                .height(size.height)
                .clip(size.shape)
                .background(if (enabled) palette.ink else palette.disabledFill)
                .then(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = size.textStyle,
            color = if (enabled) palette.onInk else palette.disabledText,
        )
    }
}

/** Les deux tailles du bouton plein, relevées dans les maquettes (VALEURS-DE-DESIGN.md §4). */
enum class NiumiButtonSize(
    val height: Dp,
    val shape: Shape,
    val textStyle: TextStyle,
) {
    /** Bouton principal du bas d'écran : 56, coins de 14, 17/500. */
    Primary(NiumiDimens.primaryButton, NiumiShapes.button, NiumiType.button17),

    /** « Autoriser » d'une tuile d'autorisation : 46, coins de 12, 15/500. */
    Authorize(NiumiDimens.authorizeButton, NiumiShapes.authorizeButton, NiumiType.chipSelected15),

    /** Bouton d'une fenêtre d'alerte ou de la feuille de scan : 52, coins de 14, 17/500. */
    Sheet(NiumiDimens.sheetButton, NiumiShapes.button, NiumiType.button17),
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
        NiumiPrimaryButton(text = "Scanner le Niumi Spot", onClick = {})
        NiumiPrimaryButton(text = "Terminer", onClick = {}, enabled = false)
    }
}
