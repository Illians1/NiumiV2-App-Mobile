package com.niumi.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Rangée de liste (VALEURS-DE-DESIGN.md §4 ; maquettes 13 à 15 et 37) : séparateur d'un pixel au
 * dessus (et au dessous pour la dernière, [bottomSeparator]), titre 16/400 à gauche, [trailing] à
 * droite (valeur en texte secondaire), puis un chevron si la rangée s'ouvre.
 *
 * - [NiumiFieldState.Normal] : titre encre, valeur et chevron `textSecondary` ;
 * - [NiumiFieldState.Greyed] (« Réveil désactivé ») : titre et valeur `border`, chevron
 *   `disabled` ; la rangée s'ouvre toujours ;
 * - [NiumiFieldState.Locked] (session) : titre et valeur `border`, cadenas `border` à la place
 *   du chevron, annoncée « Verrouillé ». [onClick] n'est jamais appelé ; [onLockedClick], s'il
 *   est fourni, reçoit le toucher (message de l'étape 45).
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiListRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    state: NiumiFieldState = NiumiFieldState.Normal,
    onLockedClick: (() -> Unit)? = null,
    bottomSeparator: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalNiumiPalette.current
    val locked = state == NiumiFieldState.Locked
    val greyed = state != NiumiFieldState.Normal
    val action =
        when {
            !locked && onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
            locked && onLockedClick != null -> Modifier.clickable(role = Role.Button, onClick = onLockedClick)
            locked -> Modifier.semantics { disabled() }
            else -> Modifier
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = NiumiDimens.listRowMinHeight)
                .separators(palette.separator, bottomSeparator)
                .then(action)
                .semantics(mergeDescendants = true) {
                    if (locked) stateDescription = NiumiComponentTexts.LOCKED
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NiumiDimens.rowGap),
    ) {
        Text(
            text = title,
            style = NiumiType.body16,
            color = if (greyed) palette.border else palette.ink,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (greyed) palette.border else palette.textSecondary,
                LocalTextStyle provides NiumiType.body16,
            ) {
                trailing()
            }
            RowEndIcon(locked = locked, opens = onClick != null, greyed = greyed)
        }
    }
}

/** Cadenas `border` si la rangée est verrouillée, sinon chevron si elle s'ouvre. */
@Composable
private fun RowEndIcon(
    locked: Boolean,
    opens: Boolean,
    greyed: Boolean,
) {
    val palette = LocalNiumiPalette.current
    val (icon, tint) =
        when {
            locked -> R.drawable.ic_lock to palette.border
            opens -> R.drawable.ic_chevron_right to if (greyed) palette.disabled else palette.textSecondary
            else -> return
        }
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(NiumiDimens.smallIcon),
    )
}

/** Séparateur d'un pixel en haut de la rangée, et en bas si [bottom]. */
private fun Modifier.separators(
    color: Color,
    bottom: Boolean,
): Modifier =
    drawBehind {
        val stroke = NiumiDimens.hairline.toPx()
        drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
        if (bottom) {
            val y = size.height - stroke / 2
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }

@Composable
private fun RowsSample(state: NiumiFieldState) {
    NiumiListRow(title = "Niumi Points", onClick = {}, state = state) { Text("2") }
    NiumiListRow(title = "Applications bloquées", onClick = {}, state = state) { Text("6") }
    NiumiListRow(title = "Aide", onClick = {}, state = state, bottomSeparator = true)
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiListRowLightPreview() {
    NiumiPreviewFrame(dark = false) {
        RowsSample(NiumiFieldState.Normal)
        RowsSample(NiumiFieldState.Greyed)
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiListRowDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        RowsSample(NiumiFieldState.Normal)
        RowsSample(NiumiFieldState.Locked)
    }
}
