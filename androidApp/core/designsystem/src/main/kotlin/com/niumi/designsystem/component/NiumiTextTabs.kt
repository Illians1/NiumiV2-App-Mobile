package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Onglets texte centrés, soulignés quand ils sont choisis (maquettes 18 à 20, étape 45) : 32 de
 * haut, 28 d'écart, trait de 2 à l'encre. [selectedIndex] nul : aucun n'est choisi. Depuis le
 * 2026-10-26, seul le sélecteur d'heure s'en sert ; « Ponctuel / Quotidien » sont des
 * [NiumiSegmentedTabs] (décision de Mehdi).
 *
 * États ([NiumiFieldState]) : grisés en « Réveil désactivé », toujours touchables ; verrouillés en
 * session, annoncés « Verrouillé », [onSelect] jamais appelé, [onLockedClick] reçoit le toucher.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiTextTabs(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: NiumiTextTabsSize = NiumiTextTabsSize.Home,
    state: NiumiFieldState = NiumiFieldState.Normal,
    onLockedClick: (() -> Unit)? = null,
) {
    val palette = LocalNiumiPalette.current
    val idle =
        when (state) {
            NiumiFieldState.Normal -> palette.textSecondary
            NiumiFieldState.Greyed -> palette.disabled
            NiumiFieldState.Locked -> palette.border
        }
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.tabGap, Alignment.CenterHorizontally),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val color = if (selected && state == NiumiFieldState.Normal) palette.ink else idle
            val action =
                when {
                    state != NiumiFieldState.Locked -> {
                        Modifier.selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    }

                    onLockedClick != null -> {
                        Modifier.clickable(role = Role.Tab, onClick = onLockedClick)
                    }

                    else -> {
                        Modifier.semantics { disabled() }
                    }
                }
            Tab(
                label = label,
                style = if (selected) size.selected else size.regular,
                color = color,
                underline = if (selected) color else Color.Transparent,
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .semantics(mergeDescendants = true) {
                            this.selected = selected
                            if (state == NiumiFieldState.Locked) {
                                contentDescription = "$label, ${NiumiComponentTexts.LOCKED}"
                            }
                        }.then(action),
            )
        }
    }
}

/** Un onglet : le texte centré dans 32 de haut, le trait en bas, de la largeur du texte. */
@Composable
private fun Tab(
    label: String,
    style: TextStyle,
    color: Color,
    underline: Color,
    modifier: Modifier,
) {
    Box(modifier = modifier.height(NiumiDimens.tabHeight).width(IntrinsicSize.Max)) {
        Text(text = label, style = style, color = color, modifier = Modifier.align(Alignment.Center))
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(NiumiDimens.tabUnderline)
                    .background(underline),
        )
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiTextTabsPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiTextTabs(labels = listOf("Ponctuel", "Quotidien"), selectedIndex = 0, onSelect = {})
        NiumiTextTabs(labels = listOf("Ponctuel", "Quotidien"), selectedIndex = null, onSelect = {
        }, state = NiumiFieldState.Greyed)
        NiumiTextTabs(
            labels = listOf("Coucher", "Réveil"),
            selectedIndex = 1,
            onSelect = {},
            size = NiumiTextTabsSize.Picker,
        )
    }
}
