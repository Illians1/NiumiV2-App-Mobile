package com.niumi.feature.setup.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.niumi.designsystem.R
import com.niumi.designsystem.component.NiumiCloseButton
import com.niumi.designsystem.component.NiumiLoadingIndicator
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Page « Applications à bloquer » (`SPECIFICATION.md` §8, maquette 11), dessinée par Niumi sur
 * Android : en pleine page depuis le 2026-10-22 (décision de Mehdi, la maquette montrait une
 * feuille du bas). Titre et croix « Fermer sans enregistrer », champ « Rechercher », grille de
 * quatre colonnes avec une coche sur les cases choisies, compteur et « Valider », inactif sans
 * coche. En tête de la grille, la section « Applications déjà choisies » montre les applications cochées à
 * l'ouverture ; le clavier réduit la grille.
 *
 * La croix et le retour abandonnent le brouillon ; seul « Valider » enregistre, puis la page se
 * ferme. À 50 applications cochées (`AppSelectionSummary.MAX_COUNT`), les cases non cochées sont
 * grisées et inactives, et le compteur le dit (décision du 2026-10-22).
 */
@Composable
fun AppSelectionPage(
    page: AppSelectionPageState,
    actions: AppSelectionActions,
    modifier: Modifier = Modifier,
) {
    // Après « Valider », la page se ferme d'elle-même.
    LaunchedEffect(page.isSaved) { if (page.isSaved) actions.onDismissPage() }
    BackHandler(onBack = actions.onDismissPage)
    NiumiScreenScaffold(
        modifier = modifier,
        bottom = { PageFooter(page = page, onValidate = actions.onValidate) },
    ) {
        PageHeader(onClose = actions.onDismissPage)
        SearchField(query = page.query, onSearch = actions.onSearch)
        AppGrid(page = page, onToggle = actions.onToggle)
    }
}

/** Titre et croix sur une ligne de 48 (maquette 11). */
@Composable
private fun PageHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(NiumiDimens.sheetHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.rowGap),
    ) {
        Text(
            text = AppSelectionTexts.PAGE_TITLE,
            style = NiumiType.cardTitle20,
            color = LocalNiumiPalette.current.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        NiumiCloseButton(label = AppSelectionTexts.CLOSE_LABEL, onClick = onClose)
    }
}

@Composable
private fun SearchField(
    query: String,
    onSearch: (String) -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onSearch,
        singleLine = true,
        textStyle = NiumiType.body16.copy(color = palette.ink),
        cursorBrush = SolidColor(palette.ink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier =
            Modifier
                .padding(top = NiumiDimens.searchFieldTop)
                .fillMaxWidth()
                .height(NiumiDimens.searchFieldHeight)
                .semantics { contentDescription = AppSelectionTexts.SEARCH_LABEL },
        decorationBox = { field ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(palette.tileGrey, NiumiShapes.searchField)
                        .padding(horizontal = NiumiDimens.searchFieldPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NiumiDimens.searchIconGap),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(NiumiDimens.searchIcon),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(text = AppSelectionTexts.SEARCH, style = NiumiType.body16, color = palette.textSecondary)
                    }
                    field()
                }
                // Croix qui vide le champ, seulement quand il contient du texte (2026-10-22).
                if (query.isNotEmpty()) ClearSearchButton(onClick = { onSearch("") })
            }
        },
    )
}

/** Croix de 18 dans une zone de 44, 48 réservés, à l'encre secondaire comme la loupe. */
@Composable
private fun ClearSearchButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .offset(x = NiumiDimens.clearSearchInset)
                .minimumInteractiveComponentSize()
                .size(NiumiDimens.minTouch)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = AppSelectionTexts.CLEAR_SEARCH },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = LocalNiumiPalette.current.textSecondary,
            modifier = Modifier.size(NiumiDimens.smallIcon),
        )
    }
}

@Composable
private fun ColumnScope.AppGrid(
    page: AppSelectionPageState,
    onToggle: (String) -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val modifier = Modifier.padding(top = NiumiDimens.appGridTop).weight(1f).fillMaxWidth()
    if (page.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            NiumiLoadingIndicator(label = AppSelectionTexts.LOADING)
        }
        return
    }
    val emptyMessage = page.emptyMessage
    if (emptyMessage != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = emptyMessage,
                style = NiumiType.secondary14,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    // Sans section « Applications déjà choisies », la grille n'a pas de titre : la maquette 11 n'en montre aucun.
    val withSections = page.chosenItems.isNotEmpty()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier,
        // 8 au-dessus de la première rangée : la pastille de la coche déborde de la case.
        contentPadding = PaddingValues(top = NiumiDimens.appGridInnerTop, bottom = NiumiDimens.appGridRowGap),
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.appGridRowGap),
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.appGridColumnGap),
    ) {
        if (withSections) {
            sectionTitle(AppSelectionTexts.CHOSEN_SECTION)
            items(page.chosenItems, key = { "chosen:${it.app.packageName}" }) { item ->
                AppTile(item = item, onToggle = { onToggle(item.app.packageName) })
            }
            if (page.items.isNotEmpty()) sectionTitle(AppSelectionTexts.ALL_SECTION)
        }
        items(page.items, key = { it.app.packageName }) { item ->
            AppTile(item = item, onToggle = { onToggle(item.app.packageName) })
        }
    }
}

private fun LazyGridScope.sectionTitle(title: String) {
    item(key = "title:$title", span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title,
            style = NiumiType.label16,
            color = LocalNiumiPalette.current.textSecondary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

private const val GRID_COLUMNS = 4

/** Une case : icône de 56, coche dans une pastille encre de 22 si choisie, nom en 12. */
@Composable
private fun AppTile(
    item: AppSelectionItem,
    onToggle: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val description =
        if (item.showPackageName) "${item.app.label}, ${item.app.packageName}" else item.app.label
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = item.isSelected,
                    enabled = item.isEnabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.appTileLabelGap),
    ) {
        Box(modifier = Modifier.size(NiumiDimens.appTile)) {
            AppIconImage(
                app = item.app,
                size = NiumiDimens.appTile,
                shape = NiumiShapes.appTile,
                greyed = !item.isEnabled,
            )
            if (item.isSelected) {
                Box(
                    modifier =
                        Modifier
                            .size(NiumiDimens.appTile)
                            .border(NiumiDimens.appTileCheckedBorder, palette.ink, NiumiShapes.appTile),
                )
                CheckBadge(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        val labelColor = if (item.isEnabled) palette.ink else palette.textSecondary
        Text(
            text = item.app.label,
            style = if (item.isSelected) NiumiType.appLabelChecked12 else NiumiType.appLabel12,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.showPackageName) {
            Text(
                text = item.app.packageName,
                style = NiumiType.appLabel12,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CheckBadge(modifier: Modifier) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .offset(x = NiumiDimens.appTileBadgeOverflow, y = -NiumiDimens.appTileBadgeOverflow)
                .size(NiumiDimens.appTileBadge)
                .background(palette.ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = palette.onInk,
            modifier = Modifier.size(NiumiDimens.appTileBadgeCheck),
        )
    }
}

/** Compteur et « Valider », sous un trait qui court d'un bord à l'autre de l'écran. */
@Composable
private fun PageFooter(
    page: AppSelectionPageState,
    onValidate: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    // Le trait déborde de la marge réelle de l'écran : 24, ou plus sous une barre de navigation
    // latérale ou une encoche en paysage (la plus grande des deux, comme `NiumiScreenScaffold`).
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val margin = with(density) { NiumiDimens.screenHorizontal.roundToPx() }
    val bleedStart = maxOf(margin, insets.getLeft(density, direction)).toFloat()
    val bleedEnd = maxOf(margin, insets.getRight(density, direction)).toFloat()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = NiumiDimens.hairline.toPx()
                    drawLine(
                        color = palette.separator,
                        start = Offset(-bleedStart, stroke / 2),
                        end = Offset(size.width + bleedEnd, stroke / 2),
                        strokeWidth = stroke,
                    )
                }.padding(top = NiumiDimens.appSelectionFooterTop),
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.appSelectionFooterGap),
    ) {
        Text(
            text = page.counter,
            style = NiumiType.chip15,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        NiumiPrimaryButton(text = AppSelectionTexts.VALIDATE, onClick = onValidate, enabled = page.canValidate)
    }
}
