package com.niumi.feature.setup.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import com.niumi.designsystem.component.NiumiActionPill
import com.niumi.designsystem.component.NiumiLoadingIndicator
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiOpacity
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
    val iconCache = remember { AppIconCache() }
    CompositionLocalProvider(LocalAppIconCache provides iconCache) {
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(NiumiShapes.selectionCard)
                .border(NiumiDimens.hairline, palette.disabled, NiumiShapes.selectionCard)
                .padding(NiumiDimens.selectionCardPadding),
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
        val capacity = slotCapacity(maxWidth)
        val description =
            if (saved.isEmpty()) {
                Modifier.clearAndSetSemantics {}
            } else {
                val label = AppSelectionTexts.iconsLabel(saved.size)
                Modifier.clearAndSetSemantics { contentDescription = label }
            }
        Row(modifier = description, horizontalArrangement = Arrangement.spacedBy(NiumiDimens.appSlotGap)) {
            when {
                saved.isEmpty() -> {
                    repeat(capacity) { EmptySlot() }
                }

                saved.size <= capacity -> {
                    saved.forEach { AppIconImage(it, NiumiDimens.appSlot, NiumiShapes.appSlot) }
                }

                else -> {
                    saved.take(capacity - 1).forEach { AppIconImage(it, NiumiDimens.appSlot, NiumiShapes.appSlot) }
                    MoreSlot(hidden = saved.size - (capacity - 1))
                }
            }
        }
    }
}

private fun slotCapacity(width: Dp): Int {
    val step = NiumiDimens.appSlot + NiumiDimens.appSlotGap
    return ((width + NiumiDimens.appSlotGap) / step).toInt().coerceIn(1, MAX_SLOTS)
}

private const val MAX_SLOTS = 6

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

/** Case « +N » : non dessinée par les maquettes, sur le modèle des cases de la carte. */
@Composable
private fun MoreSlot(hidden: Int) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            Modifier
                .size(NiumiDimens.appSlot)
                .clip(NiumiShapes.appSlot)
                .background(palette.track)
                .border(NiumiDimens.hairline, palette.disabled, NiumiShapes.appSlot),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = AppSelectionTexts.moreApps(hidden), style = NiumiType.label16, color = palette.textSecondary)
    }
}

/**
 * Vraie icône de l'application (VALEURS-DE-DESIGN.md §4 : les carrés gris des maquettes sont des
 * emplacements). Sans icône lisible, l'emplacement gris de la maquette. [greyed] grise une case
 * inactive de la grille : icône en niveaux de gris, à 38 %.
 */
@Composable
internal fun AppIconImage(
    app: InstalledApp,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    greyed: Boolean = false,
) {
    val palette = LocalNiumiPalette.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val cache = LocalAppIconCache.current
    val bitmap = remember(app.packageName, sizePx) { cache.get(app, sizePx) }
    if (bitmap != null) {
        // Pas de description : le nom porté par la case ou la carte suffit à TalkBack.
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
            colorFilter = if (greyed) GREYSCALE else null,
            alpha = if (greyed) NiumiOpacity.GREYED_CONTENT else 1f,
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(size)
                    .alpha(if (greyed) NiumiOpacity.GREYED_CONTENT else 1f)
                    .clip(shape)
                    .background(palette.track)
                    .border(NiumiDimens.hairline, palette.disabled, shape),
        )
    }
}

/**
 * Point d'entrée réel. [onSessionInProgress] applique la garde de `SetupGate` : une session en
 * cours interdit de modifier la sélection (SPEC_CORE_KMP §2 point 11, SPEC_ANDROID §12.1).
 */
@Composable
fun AppSelectionRoute(
    onContinue: () -> Unit,
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
                onContinue = onContinue,
                onToggle = viewModel::toggle,
                onSearch = viewModel::search,
                onDismissPage = viewModel::dismissPage,
                onValidate = viewModel::validate,
            ),
    )
}

private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

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
