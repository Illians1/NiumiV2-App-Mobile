package com.niumi.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalettes

/**
 * Logo Niumi, dans la version du thème courant : lettres noires sur fond clair, blanc cassé sur
 * fond sombre (`design-plan/logo/`, VALEURS-DE-DESIGN.md §2 « Couleurs du logo »). Jamais teinté.
 * [height] : 24 en tête d'écran, 54 sur l'écran 7 et « À propos » ; la largeur suit le dessin.
 */
@Composable
fun NiumiLogo(
    modifier: Modifier = Modifier,
    height: Dp = NiumiDimens.logoHeight,
) {
    val dark = LocalNiumiPalette.current == NiumiPalettes.Dark
    val painter = painterResource(if (dark) R.drawable.niumi_logo_dark else R.drawable.niumi_logo_light)
    Image(
        painter = painter,
        contentDescription = NiumiComponentTexts.LOGO,
        modifier =
            modifier
                .height(height)
                .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height),
    )
}

@Preview(name = "Clair")
@Composable
private fun NiumiLogoLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiLogo()
        NiumiLogo(height = NiumiDimens.logoLargeHeight)
    }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiLogoDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiLogo()
        NiumiLogo(height = NiumiDimens.logoLargeHeight)
    }
}
