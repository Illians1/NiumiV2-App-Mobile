package com.niumi.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens

/**
 * Bouton rond au trait (maquette 16, pré-écoute d'un son) : cercle de 32 bordé `border`, icône de
 * 14 à l'encre, zone tactile de 48 (`minimumInteractiveComponentSize`), le dessin restant 32.
 */
@Composable
fun NiumiRoundIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalNiumiPalette.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.minimumInteractiveComponentSize().semantics {
                this.contentDescription = contentDescription
            },
    ) {
        Box(
            modifier =
                Modifier
                    .size(NiumiDimens.ringtonePreviewCircle)
                    .border(NiumiDimens.hairline, palette.border.copy(alpha = if (enabled) 1f else 0.38f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = palette.ink.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.size(NiumiDimens.ringtonePreviewIcon),
            )
        }
    }
}

@Preview(name = "Clair", widthDp = 120)
@Composable
private fun NiumiRoundIconButtonPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiRoundIconButton(
            icon = painterResource(R.drawable.ic_search),
            contentDescription = "Écouter",
            onClick = {},
        )
    }
}
