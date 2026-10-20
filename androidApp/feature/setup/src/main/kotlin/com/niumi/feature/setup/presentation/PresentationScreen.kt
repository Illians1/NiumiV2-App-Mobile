package com.niumi.feature.setup.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.niumi.designsystem.component.NiumiLogo
import com.niumi.designsystem.component.NiumiPageIndicator
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiTopBar
import com.niumi.designsystem.drawing.LockedPhoneDrawing
import com.niumi.designsystem.drawing.PhoneNearPointDrawing
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Écran 1 · Présentation (`SPECIFICATION.md` §4, maquettes 01 et 02), sans état. Logo en tête des
 * deux panneaux, flèche de retour sur le panneau 2 seulement, dessin, titre et texte centrés, repère
 * à deux traits et bouton en bas. Le contenu défile si l'écran est trop petit pour le tenir.
 */
@Composable
fun PresentationScreen(
    state: PresentationUiState,
    actions: PresentationActions,
    modifier: Modifier = Modifier,
) {
    val panel = if (state.isLastPanel) SECOND_PANEL else FIRST_PANEL
    NiumiScreenScaffold(
        modifier = modifier,
        bottom = {
            NiumiPageIndicator(
                count = PresentationUiState.LAST_PANEL,
                current = state.panel - 1,
                label = panel.indicator,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(NiumiDimens.pageIndicatorButtonGap))
            NiumiPrimaryButton(
                text = panel.button,
                onClick = if (state.isLastPanel) actions.onConfigure else actions.onContinue,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(NiumiDimens.touchTarget)) {
            if (state.isLastPanel) {
                NiumiTopBar(
                    onBack = actions.onBack,
                    backLabel = PresentationTexts.BACK,
                    title = null,
                )
            }
            NiumiLogo(modifier = Modifier.align(Alignment.Center))
        }
        PanelContent(panel = panel, isLastPanel = state.isLastPanel, modifier = Modifier.weight(1f))
    }
}

/**
 * Point d'entrée réel. Le retour système suit la flèche sur le panneau 2 ; sur le panneau 1, il
 * n'est pas intercepté et quitte l'app : la présentation ne se contourne pas.
 * [onConfigure] n'est appelé qu'une fois l'accusé de lecture écrit.
 */
@Composable
fun PresentationRoute(
    onConfigure: () -> Unit,
    viewModel: PresentationViewModel = hiltViewModel(),
) {
    BackHandler(enabled = viewModel.state.isLastPanel, onBack = viewModel::back)
    PresentationScreen(
        state = viewModel.state,
        actions =
            PresentationActions(
                onContinue = viewModel::next,
                onBack = viewModel::back,
                onConfigure = { viewModel.acknowledge(onConfigure) },
            ),
    )
}

/** Dessin, titre et texte d'un panneau, centrés, défilants si l'écran est trop petit pour les tenir. */
@Composable
private fun PanelContent(
    panel: Panel,
    isLastPanel: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLastPanel) {
                PhoneNearPointDrawing(contentDescription = panel.drawing)
            } else {
                LockedPhoneDrawing(contentDescription = panel.drawing)
            }
            Spacer(modifier = Modifier.height(NiumiDimens.illustrationGap))
            Text(
                text = panel.title,
                style = NiumiType.screenTitle28,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = NiumiDimens.presentationTitleMaxWidth).semantics { heading() },
            )
            Spacer(modifier = Modifier.height(NiumiDimens.titleBodyGap))
            Text(
                text = panel.body,
                style = NiumiType.lead17,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = NiumiDimens.presentationBodyMaxWidth),
            )
        }
    }
}

/** Les textes d'un panneau, lus dans [PresentationTexts]. */
private data class Panel(
    val drawing: String,
    val title: String,
    val body: String,
    val indicator: String,
    val button: String,
)

private val FIRST_PANEL =
    Panel(
        drawing = PresentationTexts.PANEL_1_DRAWING,
        title = PresentationTexts.PANEL_1_TITLE,
        body = PresentationTexts.PANEL_1_BODY,
        indicator = PresentationTexts.PANEL_1_INDICATOR,
        button = PresentationTexts.CONTINUE,
    )

private val SECOND_PANEL =
    Panel(
        drawing = PresentationTexts.PANEL_2_DRAWING,
        title = PresentationTexts.PANEL_2_TITLE,
        body = PresentationTexts.PANEL_2_BODY,
        indicator = PresentationTexts.PANEL_2_INDICATOR,
        button = PresentationTexts.CONFIGURE,
    )

private val noActions = PresentationActions(onContinue = {}, onBack = {}, onConfigure = {})

@Preview(name = "Panneau 1", widthDp = 390, heightDp = 844)
@Composable
private fun PresentationFirstPanelPreview() {
    NiumiTheme(dark = false) { PresentationScreen(state = PresentationUiState(panel = 1), actions = noActions) }
}

@Preview(name = "Panneau 2", widthDp = 390, heightDp = 844)
@Composable
private fun PresentationSecondPanelPreview() {
    NiumiTheme(dark = false) { PresentationScreen(state = PresentationUiState(panel = 2), actions = noActions) }
}

@Preview(name = "Petit écran", widthDp = 320, heightDp = 568)
@Composable
private fun PresentationSmallScreenPreview() {
    NiumiTheme(dark = false) { PresentationScreen(state = PresentationUiState(panel = 1), actions = noActions) }
}
