package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType
import kotlinx.coroutines.launch

/**
 * Feuille du bas (VALEURS-DE-DESIGN.md §4 ; maquettes 16, 19, 23), sur le socle `ModalBottomSheet`
 * de Material 3 : pleine largeur et bord à bord, coins de 28 en haut, fond `surface`, liseré d'un
 * pixel en sombre (`sheetOutline`, transparent en clair), voile `veil` (45 % en clair, 60 % en
 * sombre), poignée de 40 × 4 en `disabled`. Toucher le voile, glisser vers le bas ou revenir en
 * arrière ferme la feuille par [onDismiss].
 *
 * En-tête, deux formes relevées dans les maquettes :
 * - avec [closeLabel] (« Fermer », « Fermer sans enregistrer ») : titre et croix sur une ligne de
 *   48, 8 sous la poignée ; titre 22/600 par défaut (feuille « Sonnerie ») ;
 * - sans croix : titre 24/600 à 22 sous la poignée (« Annuler le réveil ? »).
 *
 * [showHandle] à `false` : sans poignée, pour la feuille de scan, que ses maquettes (06, 20, 29 à 33)
 * dessinent sans elle ; le contenu pose alors sa propre marge du haut.
 *
 * La croix range la feuille puis appelle [onDismiss] ; [content] reçoit la même action
 * (`requestClose`) pour ses propres boutons (« Garder le réveil »). Les icônes des barres système suivent
 * `contentColor` (l'encre du thème), jamais le réglage du téléphone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiBottomSheet(
    onDismiss: () -> Unit,
    title: String?,
    modifier: Modifier = Modifier,
    closeLabel: String? = null,
    titleStyle: TextStyle = if (closeLabel != null) NiumiType.alertTitle22 else NiumiType.sheetTitle24,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.(requestClose: () -> Unit) -> Unit,
) {
    val palette = LocalNiumiPalette.current
    // Ouverte en entier, sans état à mi-hauteur. L'état reste interne : l'exposer imposerait
    // l'API expérimentale de Material 3 à chaque écran.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Une seule fermeture : un second toucher pendant l'animation (croix, bouton du contenu,
    // voile ou retour) annulerait le premier `hide()` et rappellerait [onDismiss] (relecture).
    var closing by remember { mutableStateOf(false) }
    val dismissOnce: () -> Unit = {
        if (!closing) {
            closing = true
            onDismiss()
        }
    }
    val close: () -> Unit = {
        if (!closing) {
            scope.launch { sheetState.hide() }.invokeOnCompletion { dismissOnce() }
        }
    }
    // Poignée, liseré et marges sont dessinés dans le contenu, qui couvre toute la surface : un
    // `border` sur le `modifier` de `ModalBottomSheet` encadrait une autre zone que la feuille
    // visible (constaté sur appareil, étape 34). La surface passe sous la barre de navigation ;
    // le contenu s'arrête au-dessus d'elle avec un espace, comme les écrans (`marginsOverSystemBars`,
    // décision du 2026-10-22).
    ModalBottomSheet(
        onDismissRequest = dismissOnce,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        shape = NiumiShapes.sheet,
        containerColor = palette.surface,
        contentColor = palette.ink,
        scrimColor = palette.veil,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .openBottomOutline(palette.sheetOutline)
                    .marginsOver(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        NiumiDimens.sheetMargins,
                    ),
        ) {
            if (showHandle) SheetHandle(Modifier.align(Alignment.CenterHorizontally))
            SheetHeader(title = title, titleStyle = titleStyle, closeLabel = closeLabel, onClose = close)
            content(close)
        }
    }
}

/**
 * Liseré d'un pixel de la feuille (VALEURS §2 sombre, « Liseré de feuille ») : contour de la forme
 * prolongé sous le bas de la surface, qui le coupe ; seuls les côtés et le haut arrondi se voient.
 */
private fun Modifier.openBottomOutline(color: Color): Modifier =
    drawBehind {
        val stroke = NiumiDimens.hairline.toPx()
        val extended = Size(size.width, size.height + stroke * 2)
        val outline = NiumiShapes.sheet.createOutline(extended, layoutDirection, this)
        drawOutline(outline, color, style = Stroke(width = stroke * 2))
    }

/** Titre seul, ou titre et croix sur une ligne de 48 (maquettes 11, 16, 19, 23). */
@Composable
private fun SheetHeader(
    title: String?,
    titleStyle: TextStyle,
    closeLabel: String?,
    onClose: () -> Unit,
) {
    if (closeLabel != null) {
        Row(
            modifier =
                Modifier
                    .padding(top = NiumiDimens.sheetHeaderTop)
                    .height(NiumiDimens.sheetHeaderHeight)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NiumiDimens.rowGap),
        ) {
            val titleModifier = Modifier.weight(1f)
            if (title == null) Spacer(titleModifier) else SheetTitle(title, titleStyle, titleModifier)
            NiumiCloseButton(label = closeLabel, onClick = onClose)
        }
    } else if (title != null) {
        SheetTitle(title, titleStyle, Modifier.padding(top = NiumiDimens.sheetTitleTop))
    }
}

@Composable
private fun SheetHandle(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .padding(top = NiumiDimens.sheetTop)
                .size(width = NiumiDimens.sheetHandleWidth, height = NiumiDimens.sheetHandleHeight)
                .clip(NiumiShapes.pill)
                .background(LocalNiumiPalette.current.disabled),
    )
}

@Composable
private fun SheetTitle(
    title: String,
    style: TextStyle,
    modifier: Modifier,
) {
    Text(text = title, style = style, modifier = modifier.semantics { heading() })
}

@Preview(name = "Clair", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiBottomSheetLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiBottomSheet(onDismiss = {}, title = "Sonnerie", closeLabel = "Fermer") {
            NiumiListRow(title = "Volume du téléphone", onClick = null, bottomSeparator = true) { Text("80 %") }
        }
    }
}

@Preview(name = "Sombre", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiBottomSheetDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiBottomSheet(onDismiss = {}, title = "Annuler le réveil ?") {
            NiumiPrimaryButton(text = "Scanner le Niumi Spot", onClick = {})
            NiumiDiscreetLink(text = "Je n’ai pas accès à mon Niumi Spot", onClick = {})
        }
    }
}
