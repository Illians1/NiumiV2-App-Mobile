package com.niumi.feature.session.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.niumi.designsystem.component.NiumiDayChip
import com.niumi.designsystem.component.NiumiFieldState
import com.niumi.designsystem.component.NiumiSectionCard
import com.niumi.designsystem.component.NiumiSegmentedTabs
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Carte « Jours de réveil » de l'accueil (maquettes du 2026-10-30) : la ligne du titre et des
 * onglets, puis les sept pastilles.
 */
@Composable
internal fun HomeDaysCard(
    state: HomeUiState,
    fieldState: NiumiFieldState,
    actions: HomeActions,
) {
    NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDaysPadding) {
        DaysHeader(state, fieldState, actions)
        Spacer(modifier = Modifier.height(NiumiDimens.homeDaysHeaderGap))
        DayChips(state, fieldState, actions)
    }
}

/**
 * Les sept pastilles, une par créneau de même largeur. Leur diamètre est de 44, réduit quand la
 * carte est trop étroite pour sept (écran de 360, décision de Mehdi du 2026-10-30) ; chacune se
 * touche sur toute la largeur de son créneau.
 */
@Composable
private fun DayChips(
    state: HomeUiState,
    fieldState: NiumiFieldState,
    actions: HomeActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val chipSize = minOf(NiumiDimens.dayChip, maxWidth / HomeTexts.DAY_LABELS.size)
        Row(modifier = Modifier.fillMaxWidth()) {
            HomeTexts.DAY_LABELS.forEachIndexed { index, label ->
                val isoDay = index + 1
                NiumiDayChip(
                    label = label,
                    accessibilityLabel = HomeTexts.DAY_NAMES[index],
                    checked = isoDay in state.enabledIsoDays,
                    onToggle = { actions.onDayToggled(isoDay) },
                    state = fieldState,
                    onLockedClick = actions.onLockedTouch,
                    size = chipSize,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Ligne au-dessus des pastilles (décision de Mehdi du 2026-10-26, non dessinée) : le titre
 * « Jours de réveil » à gauche, comme ceux des lignes du bas, et les onglets segmentés
 * « Ponctuel » et « Quotidien » à droite. Le titre est un intitulé pour TalkBack, lu avant les sept jours.
 */
@Composable
private fun DaysHeader(
    state: HomeUiState,
    fieldState: NiumiFieldState,
    actions: HomeActions,
) {
    val palette = LocalNiumiPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = NiumiDimens.segmentedTabsHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = HomeTexts.WAKE_DAYS,
            style = NiumiType.body16,
            color = if (fieldState == NiumiFieldState.Normal) palette.ink else palette.border,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        NiumiSegmentedTabs(
            labels = listOf(HomeTexts.ONE_OFF, HomeTexts.DAILY),
            selectedIndex =
                when (state.dayMode) {
                    DayMode.ONE_OFF -> 0
                    DayMode.DAILY -> 1
                    DayMode.CUSTOM -> null
                },
            onSelect = { index -> actions.onDayModeSelected(if (index == 0) DayMode.ONE_OFF else DayMode.DAILY) },
            state = fieldState,
            onLockedClick = actions.onLockedTouch,
        )
    }
}
