package com.niumi.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Haut d'écran (maquette 37) : une rangée avec la flèche de retour, dont le tracé s'aligne sur la
 * marge, haute de 48 et non de 44 : la zone tactile réservée sur Android (décision du
 * 2026-10-20). Puis le titre 28/600 à 8 dessous, annoncé comme intertitre. [backLabel] dit où
 * mène la flèche (« Retour », « Retour aux réglages », « Retour à l'accueil »…) : il vient du
 * `Texts` de l'écran. Sans [onBack], ni flèche ni rangée.
 */
@Composable
fun NiumiTopBar(
    onBack: (() -> Unit)?,
    backLabel: String?,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack != null) {
            Box(modifier = Modifier.height(NiumiDimens.touchTarget), contentAlignment = Alignment.CenterStart) {
                Box(
                    modifier =
                        Modifier
                            .offset(x = NiumiDimens.backButtonInset)
                            .minimumInteractiveComponentSize()
                            .size(NiumiDimens.minTouch)
                            .clickable(role = Role.Button, onClick = onBack)
                            .semantics { if (backLabel != null) contentDescription = backLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(NiumiDimens.icon),
                    )
                }
            }
        }
        if (title != null) {
            Text(
                text = title,
                style = NiumiType.screenTitle28,
                color = palette.ink,
                modifier =
                    Modifier
                        .then(if (onBack != null) Modifier.padding(top = NiumiDimens.titleTop) else Modifier)
                        .semantics { heading() },
            )
        }
    }
}

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiTopBarLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiTopBar(onBack = {}, backLabel = "Retour à l’accueil", title = "Réglages") }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiTopBarDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiTopBar(onBack = {}, backLabel = "Retour à l’accueil", title = "Réglages") }
}
