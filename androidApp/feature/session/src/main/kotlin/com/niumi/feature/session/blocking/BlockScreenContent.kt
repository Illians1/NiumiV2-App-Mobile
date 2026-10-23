package com.niumi.feature.session.blocking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.component.NiumiLogo
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiTextButton
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Écran 7 · Écran de blocage (`design-plan/SPECIFICATION.md` §12, maquette 22) : logo, titre et
 * texte centrés, « Fermer » et « Ouvrir Niumi » en bas ; [title] vient de `BlockScreenTexts.title`.
 * Purement présentationnel : il est monté par [WindowManagerBlockOverlayController] dans la fenêtre
 * du service d'accessibilité, toujours sous
 * `NiumiTheme(dark = true)` (le blocage n'a lieu qu'en session). Aucune action ne touche à la
 * session : « Fermer » retire la page, « Ouvrir Niumi » la retire et ouvre Niumi.
 */
@Composable
fun BlockScreenContent(
    title: String,
    onClose: () -> Unit,
    onOpenNiumi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Titre de volet : TalkBack annonce l'écran quand il paraît par-dessus le lanceur.
    NiumiScreenScaffold(
        modifier = modifier.semantics { paneTitle = title },
        bottom = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NiumiDimens.blockButtonGap),
            ) {
                NiumiPrimaryButton(text = BlockScreenTexts.CLOSE, onClick = onClose)
                NiumiTextButton(text = BlockScreenTexts.OPEN_NIUMI, onClick = onOpenNiumi)
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Le texte est descendu sous le logo (2026-10-23) ; cet espace garde le logo où le
            // place la maquette.
            Spacer(modifier = Modifier.height(NiumiDimens.blockLogoTopOffset))
            NiumiLogo(height = NiumiDimens.logoLargeHeight)
            Spacer(modifier = Modifier.height(NiumiDimens.blockLogoTitleGap))
            Text(
                text = title,
                style = NiumiType.sheetTitle24,
                textAlign = TextAlign.Center,
                // Un libellé d'application très long ne repousse pas les boutons.
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = NiumiDimens.blockTitleMaxWidth).semantics { heading() },
            )
            Spacer(modifier = Modifier.height(NiumiDimens.blockTitleBodyGap))
            Text(
                text = BlockScreenTexts.BODY,
                style = NiumiType.body16,
                color = LocalNiumiPalette.current.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = NiumiDimens.blockBodyMaxWidth),
            )
        }
    }
}

private const val TITLE_MAX_LINES = 3

@Preview(name = "Sombre", widthDp = 390, heightDp = 844)
@Composable
private fun BlockScreenContentPreview() {
    NiumiTheme(dark = true) {
        BlockScreenContent(title = BlockScreenTexts.title("YouTube"), onClose = {}, onOpenNiumi = {})
    }
}

@Preview(name = "Sombre, petit écran", widthDp = 320, heightDp = 568)
@Composable
private fun BlockScreenContentSmallPreview() {
    NiumiTheme(dark = true) {
        BlockScreenContent(title = BlockScreenTexts.FALLBACK_TITLE, onClose = {}, onOpenNiumi = {})
    }
}
