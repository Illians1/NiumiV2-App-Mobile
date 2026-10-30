package com.niumi.feature.setup.permissions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.niumi.designsystem.R
import com.niumi.designsystem.component.NiumiButtonSize
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiSectionCard
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Une tuile de l'écran 2 (maquette 03) : icône, nom et phrase dans une carte de section, blanche à
 * double ombre, coins de 14 (maquettes de Mehdi du 2026-10-30 ; fond `tileGrey`, coins de 16, avant).
 * Accordée, une coche à l'encre dans une pastille ambre au bout de la ligne (décision de Mehdi du
 * 2026-10-22, exception écrite à la règle de l'ambre, VALEURS-DE-DESIGN.md §2) ; sinon le bouton
 * « Autoriser » dessous. Les icônes des tuiles restent à l'encre (essai ambre abandonné le même
 * jour). Le petit bouton « ? » de la maquette attend son guide (M1, reporté le 2026-10-20).
 */
@Composable
internal fun PermissionTile(
    content: TileContent,
    granted: Boolean,
    onAuthorize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    NiumiSectionCard(contentPadding = PaddingValues(NiumiDimens.permissionTilePadding), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NiumiDimens.permissionTileInnerGap),
        ) {
            Icon(
                painter = painterResource(content.icon),
                contentDescription = null,
                tint = palette.ink,
                modifier = Modifier.size(NiumiDimens.permissionTileIcon),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NiumiDimens.permissionTileTextGap),
            ) {
                Text(text = content.name, style = NiumiType.labelStrong16, color = palette.ink)
                Text(text = content.phrase, style = NiumiType.secondary14, color = palette.textSecondary)
            }
            if (granted) GrantedBadge()
        }
        if (!granted) {
            Spacer(modifier = Modifier.height(NiumiDimens.permissionTileInnerGap))
            NiumiPrimaryButton(
                text = PermissionsTexts.AUTHORIZE,
                onClick = onAuthorize,
                size = NiumiButtonSize.Authorize,
            )
        }
    }
}

/** Coche à l'encre dans une pastille ambre : 8,9:1 entre la coche et l'ambre. */
@Composable
private fun GrantedBadge() {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            Modifier
                .size(NiumiDimens.grantedBadge)
                .clip(CircleShape)
                .background(palette.amber)
                .semantics { contentDescription = PermissionsTexts.GRANTED },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(NiumiDimens.grantedBadgeCheck),
        )
    }
}

/** Contenu fixe d'une tuile, lu dans [PermissionsTexts] et les icônes du design system. */
internal data class TileContent(
    val name: String,
    val phrase: String,
    @DrawableRes val icon: Int,
)
