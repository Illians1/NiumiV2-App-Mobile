package com.niumi.feature.ringing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.core.interop.SessionStateDto
import com.niumi.designsystem.component.NfcScanSheet
import com.niumi.designsystem.component.NfcScanSheetContent
import com.niumi.designsystem.component.NiumiLoadingIndicator
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.drawing.PhoneNearPointDrawing
import com.niumi.designsystem.drawing.PhoneNearPointVariant
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanSheetState
import java.time.LocalDateTime

/**
 * Écran 10 · Réveil (étape 38 ; `SPECIFICATION.md` §14, maquettes 28 à 33), toujours sombre : la
 * date, l'heure actuelle en 112, le téléphone approché du Niumi Point, « Rejoins ton Niumi Point »,
 * et le seul bouton « Scanner le Niumi Point », qui ouvre la feuille de scan. **Aucune action
 * d'arrêt ni de report** (SPEC_ANDROID §10.4, `AlarmScreenActionsTest`).
 *
 * Dessin et consigne sont centrés dans la place qui reste entre l'heure et le bouton ; sur un écran
 * trop court (paysage), le haut défile et le bouton reste visible. La place du lien « Je n'ai pas
 * accès à mon Niumi Point » est réservée sous le bouton jusqu'à son arrivée (étape 49), pour que
 * l'écran tombe où la maquette 28 le dessine. La date commence 24 sous la marge du haut, au lieu
 * des 60 de la maquette (décisions du 2026-10-22).
 *
 * Après un scan validé (`RELEASING`), la date, l'heure et un cercle de chargement seuls (décision
 * du 2026-10-22).
 */
@Composable
fun AlarmScreen(
    state: AlarmScreenState,
    now: LocalDateTime,
    actions: AlarmScreenActions,
    modifier: Modifier = Modifier,
) {
    NiumiScreenScaffold(
        modifier = modifier,
        bottom =
            if (state.showsScanButton) {
                {
                    NiumiPrimaryButton(text = AlarmTexts.SCAN_BUTTON, onClick = actions.openScanSheet)
                    Spacer(Modifier.height(NiumiDimens.alarmEmergencyLinkSpace))
                }
            } else {
                null
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                DateAndClock(now)
                if (state.showsReleaseLoader) {
                    NiumiLoadingIndicator(label = AlarmTexts.RELEASE_LOADER)
                } else {
                    InstructionBlock(state.instructionText.orEmpty())
                }
                // Troisième élément vide : `SpaceBetween` centre la consigne dans la place restante.
                Spacer(Modifier)
            }
        }
    }
    scanSheetContent(state.sheet)?.let { content ->
        NfcScanSheet(
            content = content,
            onDismiss = actions.cancelScan,
            onFailureAction =
                if (state.sheet == ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED)) {
                    actions.openNfcSettings
                } else {
                    actions.retryScan
                },
            failurePaneTitle = AlarmTexts.FAILURE_PANE,
        )
    }
}

private fun scanSheetContent(sheet: ScanSheetState): NfcScanSheetContent? =
    when (sheet) {
        ScanSheetState.Hidden -> null
        ScanSheetState.Ready -> NfcScanSheetContent.Ready
        is ScanSheetState.Failed -> AlarmTexts.failure(sheet.reason)
    }

@Composable
private fun DateAndClock(now: LocalDateTime) {
    Column(
        modifier = Modifier.padding(top = NiumiDimens.alarmDateTop),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.alarmDateClockGap),
    ) {
        Text(
            text = NiumiTimeFormat.dayAndDate(now.toLocalDate()),
            style = NiumiType.body16,
            color = LocalNiumiPalette.current.textSecondary,
        )
        // Une seule ligne, réduite si le texte agrandi du téléphone la ferait déborder.
        BasicText(
            text = NiumiTimeFormat.clock(now.toLocalTime()),
            style = NiumiType.clock112.copy(color = LocalNiumiPalette.current.ink),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = NiumiType.clock112.fontSize),
        )
    }
}

@Composable
private fun InstructionBlock(instruction: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.alarmDrawingGap),
    ) {
        PhoneNearPointDrawing(
            contentDescription = AlarmTexts.DRAWING,
            modifier = Modifier.widthIn(max = NiumiDimens.alarmDrawingWidth),
            variant = PhoneNearPointVariant.Alarm,
        )
        Text(
            text = instruction,
            style = NiumiType.instruction30,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = NiumiDimens.alarmInstructionMaxWidth),
        )
    }
}

private val PreviewNow: LocalDateTime = LocalDateTime.of(2026, 10, 2, 7, 0)

@Composable
private fun AlarmScreenPreview(state: AlarmScreenState) {
    NiumiTheme(dark = true) { AlarmScreen(state = state, now = PreviewNow, actions = AlarmScreenActions()) }
}

@Preview(name = "Sonnerie", widthDp = 390, heightDp = 844)
@Composable
private fun AlarmScreenRingingPreview() {
    AlarmScreenPreview(AlarmScreenState.from(SessionStateDto.RINGING))
}

@Preview(name = "Prêt à scanner", widthDp = 390, heightDp = 844)
@Composable
private fun AlarmScreenReadyPreview() {
    AlarmScreenPreview(AlarmScreenState.from(SessionStateDto.RINGING, sheet = ScanSheetState.Ready))
}

@Preview(name = "NFC coupé", widthDp = 390, heightDp = 844)
@Composable
private fun AlarmScreenNfcDisabledPreview() {
    AlarmScreenPreview(
        AlarmScreenState.from(
            SessionStateDto.RINGING,
            nfcAvailability = NfcAvailability.DISABLED,
            sheet = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED),
        ),
    )
}

@Preview(name = "Scan validé", widthDp = 390, heightDp = 844)
@Composable
private fun AlarmScreenReleasingPreview() {
    AlarmScreenPreview(AlarmScreenState.from(SessionStateDto.RELEASING))
}
