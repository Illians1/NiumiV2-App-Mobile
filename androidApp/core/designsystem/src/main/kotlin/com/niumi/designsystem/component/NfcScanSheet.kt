package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Feuille de scan dessinée par Niumi sur Android (`SPECIFICATION.md` §6 et §14, maquettes 29 à 33 ;
 * étape 38). Une seule feuille, sans poignée, dont le contenu bascule de « Prêt à scanner » à un
 * échec et retour sans se refermer.
 *
 * - **Prêt à scanner** : titre 22 à l'encre (26 dans la maquette, réduit le 2026-10-26 ; la
 *   maquette le grise comme la fenêtre de l'iPhone, rendu plus visible le 2026-10-22 ; décisions de
 *   Mehdi), rond de 112 `tileGrey` portant l'icône de scan,
 *   phrase 17, bouton gris « Annuler » de 52 qui range la feuille puis appelle [onDismiss]. La
 *   phrase est [phrase] : « Approche ton téléphone du Niumi Spot » par défaut, « … pour annuler le
 *   réveil » sur la feuille d'annulation (étape 46, `SPECIFICATION.md` §11).
 * - **Échec** : titre 22/600 (24 dans la maquette, réduit le 2026-10-26) annoncé dès qu'il paraît,
 *   phrase d'aide 16, bouton principal qui appelle [onFailureAction] sans fermer (« Réessayer »,
 *   « Ouvrir les réglages »), puis le lien de secours si [emergencyLinkText] et [onEmergencyExit] sont
 *   fournis. [failurePaneTitle] nomme la fenêtre pour TalkBack (« Échec du scan »).
 *
 * - **Libération** (étape 46, feuille d'annulation) : cercle de chargement seul, à la hauteur du
 *   rond de « Prêt à scanner », après le délai habituel ([NiumiLoadingIndicator]).
 *
 * Le voile, le glissement et le retour ferment la feuille dans tous les cas : la maquette de
 * l'échec n'a pas de croix (arbitrage de l'étape 38). Le composant ne connaît ni le NFC ni la
 * session : l'écran traduit son état en [content] et pilote la lecture.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NfcScanSheet(
    content: NfcScanSheetContent,
    onDismiss: () -> Unit,
    onFailureAction: () -> Unit,
    failurePaneTitle: String,
    modifier: Modifier = Modifier,
    emergencyLinkText: String? = null,
    onEmergencyExit: (() -> Unit)? = null,
    phrase: String = NiumiComponentTexts.SCAN_SHEET_PHRASE,
) {
    NiumiBottomSheet(onDismiss = onDismiss, title = null, modifier = modifier, showHandle = false) { requestClose ->
        Spacer(Modifier.height(NiumiDimens.scanSheetTop))
        when (content) {
            NfcScanSheetContent.Ready -> {
                ReadyContent(phrase = phrase, onCancel = requestClose)
            }

            is NfcScanSheetContent.Releasing -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(NiumiDimens.scanSheetCircle),
                    contentAlignment = Alignment.Center,
                ) { NiumiLoadingIndicator(label = content.label) }
            }

            is NfcScanSheetContent.Failure -> {
                FailureContent(
                    failure = content,
                    onAction = onFailureAction,
                    paneTitle = failurePaneTitle,
                    emergencyLinkText = emergencyLinkText,
                    onEmergencyExit = onEmergencyExit,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    phrase: String,
    onCancel: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.scanSheetGap),
    ) {
        Text(
            text = NiumiComponentTexts.SCAN_SHEET_TITLE,
            style = NiumiType.scanSheetTitle22,
            color = palette.ink,
            modifier = Modifier.semantics { heading() },
        )
        Box(
            modifier =
                Modifier
                    .size(NiumiDimens.scanSheetCircle)
                    .clip(NiumiShapes.pill)
                    .background(palette.tileGrey),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nfc_scan),
                contentDescription = null,
                tint = palette.ink,
                modifier = Modifier.size(NiumiDimens.scanSheetIcon),
            )
        }
        Text(
            text = phrase,
            style = NiumiType.lead17,
            color = palette.ink,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(NiumiDimens.sheetButton)
                    .clip(NiumiShapes.button)
                    .background(palette.cancelButton)
                    .clickable(role = Role.Button, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = NiumiComponentTexts.SCAN_SHEET_CANCEL, style = NiumiType.button17, color = palette.ink)
        }
    }
}

@Composable
private fun FailureContent(
    failure: NfcScanSheetContent.Failure,
    onAction: () -> Unit,
    paneTitle: String,
    emergencyLinkText: String?,
    onEmergencyExit: (() -> Unit)?,
) {
    val palette = LocalNiumiPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().semantics { this.paneTitle = paneTitle },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.scanFailureGap),
    ) {
        Text(
            text = failure.title,
            style = NiumiType.alertTitle22,
            color = palette.ink,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.widthIn(max = NiumiDimens.scanFailureTextMaxWidth).semantics {
                    heading()
                    liveRegion = LiveRegionMode.Assertive
                },
        )
        Text(
            text = failure.help,
            style = NiumiType.body16,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = NiumiDimens.scanFailureTextMaxWidth),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = NiumiDimens.scanFailureButtonTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NiumiDimens.alarmButtonLinkGap),
        ) {
            NiumiPrimaryButton(text = failure.actionLabel, onClick = onAction)
            if (emergencyLinkText != null && onEmergencyExit != null) {
                NiumiDiscreetLink(text = emergencyLinkText, onClick = onEmergencyExit)
            }
        }
    }
}

private val PreviewFailure =
    NfcScanSheetContent.Failure(
        title = "Ce n’est pas un Niumi Spot",
        help = "Scanne un Niumi Spot que tu as enregistré.",
        actionLabel = "Réessayer",
    )

@Preview(name = "Prêt, sombre", widthDp = 390, heightDp = 844)
@Composable
private fun NfcScanSheetReadyDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NfcScanSheet(NfcScanSheetContent.Ready, onDismiss = {}, onFailureAction = {}, failurePaneTitle = "")
    }
}

@Preview(name = "Prêt, clair", widthDp = 390, heightDp = 844)
@Composable
private fun NfcScanSheetReadyLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NfcScanSheet(NfcScanSheetContent.Ready, onDismiss = {}, onFailureAction = {}, failurePaneTitle = "")
    }
}

@Preview(name = "Échec, sombre", widthDp = 390, heightDp = 844)
@Composable
private fun NfcScanSheetFailureDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NfcScanSheet(
            content = PreviewFailure,
            onDismiss = {},
            onFailureAction = {},
            failurePaneTitle = "Échec du scan",
            emergencyLinkText = "Je n’ai pas accès à mon Niumi Spot",
            onEmergencyExit = {},
        )
    }
}
