package com.niumi.feature.setup.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.niumi.designsystem.component.LocalNiumiAppIconCache
import com.niumi.designsystem.component.NiumiActionPill
import com.niumi.designsystem.component.NiumiAppIcon
import com.niumi.designsystem.component.NiumiAppIconCache
import com.niumi.designsystem.component.NiumiLoadingIndicator
import com.niumi.designsystem.component.NiumiMoreAppsSlot
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiSectionCard
import com.niumi.designsystem.component.NiumiTopBar
import com.niumi.designsystem.component.appSlotCapacity
import com.niumi.designsystem.component.appSlotLayout
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.system.apps.InstalledApp

/**
 * Écran 4 · Choix des applications (`SPECIFICATION.md` §8, maquettes 10 et 12), sans état : titre
 * en 28, texte, carte centrée dans l'espace libre, mention et « Continuer », inactif tant qu'aucune
 * sélection n'est validée. La carte montre la sélection **validée** (Q3, 2026-10-22) : six cases
 * vides avant le choix ; ensuite les icônes choisies, six cases au plus, la dernière devenant
 * « +N » quand elles ne tiennent pas (décision du 2026-10-22). Le contenu défile sur un petit écran.
 */
@Composable
fun AppSelectionScreen(
    state: AppSelectionUiState,
    actions: AppSelectionActions,
    modifier: Modifier = Modifier,
) {
    val iconCache = remember { NiumiAppIconCache() }
    CompositionLocalProvider(LocalNiumiAppIconCache provides iconCache) {
        val page = state.page
        // La page « Applications à bloquer » prend tout l'écran tant qu'elle est ouverte.
        if (page == null) {
            AppSelectionContent(state = state, actions = actions, modifier = modifier)
        } else {
            AppSelectionPage(page = page, actions = actions, modifier = modifier)
        }
    }
}

@Composable
private fun AppSelectionContent(
    state: AppSelectionUiState,
    actions: AppSelectionActions,
    modifier: Modifier,
) {
    val palette = LocalNiumiPalette.current
    val chosen = state.saved.isNotEmpty()
    NiumiScreenScaffold(
        modifier = modifier,
        bottom = {
            Text(
                text = if (chosen) AppSelectionTexts.HINT_CHOSEN else AppSelectionTexts.HINT_EMPTY,
                style = NiumiType.caption13,
                color = palette.textSecondary,
            )
            Spacer(modifier = Modifier.height(NiumiDimens.appSelectionHintGap))
            NiumiPrimaryButton(
                text = AppSelectionTexts.CONTINUE,
                onClick = actions.onContinue,
                enabled = state.canContinue,
            )
        },
    ) {
        // La carte est centrée dans l'espace libre ; sur un écran trop petit, tout défile.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .fillMaxWidth(),
            ) {
                actions.onBack?.let { onBack ->
                    NiumiTopBar(onBack = onBack, backLabel = AppSelectionTexts.BACK, title = null)
                }
                Text(
                    text = AppSelectionTexts.TITLE,
                    style = NiumiType.screenTitle28,
                    color = palette.ink,
                    modifier = Modifier.padding(top = NiumiDimens.appSelectionTitleTop).semantics { heading() },
                )
                Text(
                    text = AppSelectionTexts.BODY,
                    style = NiumiType.body16,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = NiumiDimens.appSelectionTitleBodyGap),
                )
                Spacer(modifier = Modifier.weight(1f))
                // Rien n'est affirmé avant la lecture de la sélection enregistrée : un indicateur
                // tient la place de la carte (décision du 2026-10-22).
                if (state.isLoading) {
                    NiumiLoadingIndicator(
                        label = AppSelectionTexts.LOADING,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    SelectionCard(saved = state.saved, onOpenPage = actions.onOpenPage)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SelectionCard(
    saved: List<InstalledApp>,
    onOpenPage: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    // Carte de section, blanche à double ombre, coins de 14 (maquettes de Mehdi du 2026-10-30 ;
    // carte au trait `disabled`, coins de 20, avant).
    NiumiSectionCard(
        contentPadding = NiumiDimens.selectionCardPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.selectionCardGap),
    ) {
        AppSlots(saved = saved)
        Text(
            text = AppSelectionTexts.chosen(saved.size),
            style = NiumiType.cardTitle20,
            color = palette.ink,
            textAlign = TextAlign.Center,
        )
        if (saved.isEmpty()) {
            NiumiActionPill(
                text = AppSelectionTexts.CHOOSE,
                onClick = onOpenPage,
                filled = true,
                height = NiumiDimens.pillButtonLarge,
            )
        } else {
            NiumiActionPill(text = AppSelectionTexts.MODIFY, onClick = onOpenPage)
        }
    }
}

/**
 * Rangée de cases : autant que la largeur en tient, six au plus. Vides et pointillées avant le
 * choix, muettes pour TalkBack (maquette 10) ; ensuite une image unique, « Icônes des N
 * applications choisies » (maquette 12).
 */
@Composable
private fun AppSlots(saved: List<InstalledApp>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val capacity = appSlotCapacity(maxWidth.value, NiumiDimens.appSlot.value, NiumiDimens.appSlotGap.value)
        val description =
            if (saved.isEmpty()) {
                Modifier.clearAndSetSemantics {}
            } else {
                val label = AppSelectionTexts.iconsLabel(saved.size)
                Modifier.clearAndSetSemantics { contentDescription = label }
            }
        Row(modifier = description, horizontalArrangement = Arrangement.spacedBy(NiumiDimens.appSlotGap)) {
            if (saved.isEmpty()) {
                repeat(capacity) { EmptySlot() }
            } else {
                val layout = appSlotLayout(saved.size, capacity)
                saved.take(layout.shown).forEach { AppIconImage(it, NiumiDimens.appSlot, NiumiShapes.appSlot) }
                if (layout.hidden > 0) {
                    NiumiMoreAppsSlot(
                        text = AppSelectionTexts.moreApps(layout.hidden),
                        size = NiumiDimens.appSlot,
                        shape = NiumiShapes.appSlot,
                        textStyle = NiumiType.label16,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySlot() {
    val palette = LocalNiumiPalette.current
    val density = LocalDensity.current
    val effect =
        remember(density) {
            with(density) {
                PathEffect.dashPathEffect(
                    floatArrayOf(NiumiDimens.appSlotDash.toPx(), NiumiDimens.appSlotDashGap.toPx()),
                )
            }
        }
    Box(
        modifier =
            Modifier.size(NiumiDimens.appSlot).drawBehind {
                val stroke = NiumiDimens.appSlotDashedStroke.toPx()
                val radius = NiumiShapes.appSlot.topStart.toPx(size, this)
                drawRoundRect(
                    color = palette.disabled,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius),
                    style = Stroke(width = stroke, pathEffect = effect),
                )
            },
    )
}

/** Icône d'une application de la carte ou de la grille (`NiumiAppIcon`, partagée avec l'accueil). */
@Composable
internal fun AppIconImage(
    app: InstalledApp,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    greyed: Boolean = false,
) {
    NiumiAppIcon(
        key = app.packageName,
        icon = app.icon,
        size = size,
        shape = shape,
        modifier = modifier,
        greyed = greyed,
    )
}

/**
 * Point d'entrée réel. [onSessionInProgress] applique la garde de `SetupGate` : du coucher au scan,
 * la sélection ne se modifie pas (décision 11 amendée le 2026-10-24, SPEC_ANDROID §12.1).
 *
 * [editing] (étape 45, M11) : l'écran est rouvert depuis l'accueil ; la flèche, le retour système et
 * « Continuer » y ramènent par [onBack]. Pendant la mise en route, « Continuer » enregistre la fin de
 * la mise en route puis appelle [onContinue].
 */
@Composable
fun AppSelectionRoute(
    editing: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSessionInProgress: () -> Unit,
    viewModel: AppSelectionViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    LaunchedEffect(state.isSessionInProgress) {
        if (state.isSessionInProgress) onSessionInProgress()
    }

    AppSelectionScreen(
        state = state,
        actions =
            AppSelectionActions(
                onOpenPage = viewModel::openPage,
                onContinue = if (editing) onBack else ({ viewModel.completeSetup(onContinue) }),
                onToggle = viewModel::toggle,
                onSearch = viewModel::search,
                onDismissPage = viewModel::dismissPage,
                onValidate = viewModel::validate,
                onBack = if (editing) onBack else null,
            ),
    )
}

private val previewApps =
    listOf("Instagram", "TikTok", "YouTube", "Snapchat", "Facebook", "Netflix", "WhatsApp", "Messages")
        .map { InstalledApp("com.example.${it.lowercase()}", it, icon = null) }

@Preview(name = "Avant le choix", widthDp = 390, heightDp = 844)
@Composable
private fun AppSelectionScreenEmptyPreview() {
    NiumiTheme(dark = false) {
        AppSelectionScreen(state = AppSelectionUiState(isLoading = false), actions = AppSelectionActions({}, {}))
    }
}

@Preview(name = "Après le choix", widthDp = 390, heightDp = 844)
@Composable
private fun AppSelectionScreenChosenPreview() {
    NiumiTheme(dark = false) {
        AppSelectionScreen(
            state = AppSelectionUiState(saved = previewApps, isLoading = false),
            actions = AppSelectionActions({}, {}),
        )
    }
}
