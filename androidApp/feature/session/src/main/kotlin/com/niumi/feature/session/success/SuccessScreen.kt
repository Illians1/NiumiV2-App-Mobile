package com.niumi.feature.session.success

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.drawing.SunriseDrawing
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiLightWindowWhileShown
import com.niumi.designsystem.ui.theme.NiumiPalettes
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Point d'entrée de l'écran 11 (étape 46) : relit la session [sessionId] et rend la main à
 * l'accueil si elle est introuvable ou pas terminée. Le retour du téléphone fait comme « Retour à
 * l'accueil ».
 */
@Composable
fun SuccessRoute(
    sessionId: String,
    onBackHome: () -> Unit,
    viewModel: SuccessViewModel = hiltViewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    BackHandler(onBack = onBackHome)
    when (val state = viewModel.state) {
        SuccessUiState.Loading -> NiumiTheme(dark = false) { NiumiScreenScaffold {} }
        SuccessUiState.NotFound -> LaunchedEffect(Unit) { onBackHome() }
        is SuccessUiState.Shown -> SuccessScreen(state = state, onBackHome = onBackHome)
    }
}

/**
 * Écran 11 · Réussite (`design-plan/SPECIFICATION.md` §15, maquette 42). Toujours clair, quel que
 * soit l'état de la session suivante : la session est finie (§15). Une période de 24 h en
 * Quotidien bloque de nouveau dès le scan, et l'app passerait sinon cet écran en sombre ; les barres
 * système et le fond de fenêtre passent en clair avec lui ([NiumiLightWindowWhileShown]).
 */
@Composable
fun SuccessScreen(
    state: SuccessUiState.Shown,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Le thème de la session, posé par `MainActivity`, avant que l'écran ne force le clair.
    NiumiLightWindowWhileShown(sessionDark = LocalNiumiPalette.current == NiumiPalettes.Dark)
    NiumiTheme(dark = false) {
        val palette = LocalNiumiPalette.current
        NiumiScreenScaffold(
            modifier = modifier,
            bottom = {
                NiumiPrimaryButton(
                    text = SuccessTexts.BACK_HOME,
                    onClick = onBackHome,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(NiumiDimens.successSunTop))
                SunriseDrawing()
                Spacer(Modifier.height(NiumiDimens.successTitleTop))
                Text(
                    text = state.title,
                    style = NiumiType.title40,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                state.afterAlarm?.let {
                    Spacer(Modifier.height(NiumiDimens.successLineGap))
                    Text(text = it, style = NiumiType.subtitle20, color = palette.ink, textAlign = TextAlign.Center)
                }
                state.savedComparison?.let {
                    Spacer(Modifier.height(NiumiDimens.successBlockGap))
                    Bravo(comparison = it)
                }
                Spacer(Modifier.height(NiumiDimens.successBlockGap))
                Text(
                    text = SuccessTexts.APPS_BACK,
                    style = NiumiType.body16,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = NiumiDimens.appsBackMaxWidth),
                )
            }
        }
    }
}

/** « Bravo ! » en 600, puis la comparaison, sur la teinte ambre (VALEURS-DE-DESIGN.md §2). */
@Composable
private fun Bravo(comparison: String) {
    val palette = LocalNiumiPalette.current
    Text(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(SuccessTexts.BRAVO) }
                append(" ")
                append(comparison)
            },
        style = NiumiType.body16,
        color = palette.ink,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .widthIn(max = NiumiDimens.bravoMaxWidth)
                .clip(NiumiShapes.card)
                .background(palette.amberTint)
                .padding(NiumiDimens.bravoPadding),
    )
}

@Preview(name = "Clair", widthDp = 390, heightDp = 844)
@Composable
private fun SuccessScreenPreview() {
    SuccessScreen(
        state =
            SuccessUiState.Shown(
                "Debout à 7:04",
                "4 minutes après la sonnerie.",
                "C’est 11 minutes de moins qu’avant Niumi.",
            ),
        onBackHome = {},
    )
}
