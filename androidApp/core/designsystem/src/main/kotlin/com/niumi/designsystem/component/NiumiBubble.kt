package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Bulle du volume (maquette 17) : [content] est la cible (la ligne « Volume du téléphone ») ;
 * [visible] fait apparaître, au-dessus et alignée à droite de [content], une bulle noire (inversée
 * en sombre, M7, étape 40) portant [text]. Toucher ailleurs appelle [onDismissRequest]
 * (`PopupProperties.dismissOnClickOutside`, par défaut). Aucun signe n'indique que la ligne se
 * touche (SPECIFICATION.md §10) : c'est à l'appelant de rendre [content] cliquable.
 *
 * **`focusable = true` (étape 40, trouvé sur appareil).** Avec la valeur par défaut
 * (`focusable = false`), la bulle ne se fermait jamais au toucher extérieur sur ce Xiaomi /
 * HyperOS : `ACTION_OUTSIDE`, le mécanisme dont dépend `PopupLayout.onTouchEvent`
 * (`androidx.compose.ui:ui-android:1.12.1`, décompilé pour le constater), n'était pas livré de
 * façon fiable à une fenêtre non focusable par cette surcouche. Confirmé corrigé par un toucher
 * réel (`adb shell input tap`), dans et hors `NiumiBottomSheet`. Aucun champ de saisie n'est
 * concerné, le clavier ne peut donc pas être perturbé par ce focus.
 *
 * L'injection synthétique de `ComposeTestRule` (`RingtoneSheetTest`), elle, ne déclenche jamais
 * `ACTION_OUTSIDE` d'une fenêtre à l'autre, même après ce correctif : limite distincte du cadre de
 * test, pas de l'implémentation — le toucher extérieur reste vérifié à la main (`ETAPE-40.md`).
 */
@Composable
fun NiumiBubble(
    visible: Boolean,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Box(modifier = modifier) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider = remember(density) { AboveEndPositionProvider(density) },
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true),
            ) {
                NiumiBubbleContent(text)
            }
        }
    }
}

@Composable
private fun NiumiBubbleContent(text: String) {
    val palette = LocalNiumiPalette.current
    Box {
        Box(
            modifier =
                Modifier
                    .width(NiumiDimens.bubbleWidth)
                    .shadow(NiumiDimens.bubbleElevation, NiumiShapes.bubble)
                    .clip(NiumiShapes.bubble)
                    .background(palette.ink)
                    .padding(
                        horizontal = NiumiDimens.bubblePaddingHorizontal,
                        vertical = NiumiDimens.bubblePaddingVertical,
                    ),
        ) {
            Text(text = text, style = NiumiType.secondary14, color = palette.onInk)
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = -(NiumiDimens.bubbleTailInsetFromEnd),
                        y = NiumiDimens.bubbleTailSize / 2,
                    ).size(NiumiDimens.bubbleTailSize)
                    .rotate(TAIL_ROTATION_DEGREES)
                    .background(palette.ink),
        )
    }
}

/** Carré tourné de 45° : un losange, comme la pointe de la bulle dans les sources des maquettes. */
private const val TAIL_ROTATION_DEGREES = 45f

/** Place la bulle au-dessus de sa cible, alignée sur son bord droit, avec un écart de [NiumiDimens.bubbleGap]. */
private class AboveEndPositionProvider(
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gapPx = with(density) { NiumiDimens.bubbleGap.roundToPx() }
        val x = anchorBounds.right - popupContentSize.width
        val y = anchorBounds.top - popupContentSize.height - gapPx
        return IntOffset(x, y)
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiBubbleLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiBubble(visible = true, text = "Il se règle dans les réglages de ton téléphone.", onDismissRequest = {}) {
            NiumiListRow(title = "Volume du téléphone", onClick = null) { Text("80 %") }
        }
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiBubbleDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiBubble(visible = true, text = "Il se règle dans les réglages de ton téléphone.", onDismissRequest = {}) {
            NiumiListRow(title = "Volume du téléphone", onClick = null) { Text("80 %") }
        }
    }
}
