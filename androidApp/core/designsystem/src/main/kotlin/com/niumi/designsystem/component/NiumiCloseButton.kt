package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Croix de fermeture d'une feuille du bas (maquettes 11, 16) ou d'une fenêtre d'alerte (étape 36,
 * décision du 2026-10-21) : icône de 22 dans une zone de 44, 48 réservés, décalée vers le bord
 * pour que son tracé s'aligne sur la marge. [label] vient du `Texts` de l'écran (« Fermer »,
 * « Fermer sans enregistrer »).
 */
@Composable
internal fun NiumiCloseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .offset(x = -NiumiDimens.closeButtonInset)
                .minimumInteractiveComponentSize()
                .size(NiumiDimens.minTouch)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = LocalNiumiPalette.current.ink,
            modifier = Modifier.size(NiumiDimens.closeIcon),
        )
    }
}

/**
 * Croix d'une fenêtre d'alerte (maquette de Mehdi du 2026-10-22) : croix de 18 dans un rond gris de
 * 36 (`tileGrey`), zone tactile de 48 réservée. Placée au coin haut droit par la fenêtre.
 */
@Composable
internal fun NiumiRoundCloseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(NiumiDimens.alertCloseCircle)
                .clip(CircleShape)
                .background(palette.tileGrey)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(NiumiDimens.alertCloseIcon),
        )
    }
}
