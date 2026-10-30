package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Cadre d'un écran Niumi, bord à bord (SPEC_ANDROID §15) : fond du thème sous les barres système,
 * marges des maquettes (VALEURS-DE-DESIGN.md §4 : 24 sur les côtés, 56 en haut, 34 en bas) posées
 * sur les barres, l'encoche et le clavier (`safeDrawing`) : sur les côtés et en haut, la plus
 * grande des deux (décision du 2026-10-20) ; en bas, la barre plus un espace, jamais moins que la
 * maquette (décision du 2026-10-22, `marginsOverSystemBars`).
 * [bottom] reçoit le bouton principal, collé au bas ; [content] prend la place restante et gère
 * lui-même son défilement (une liste paresseuse ne peut pas vivre dans une colonne défilante).
 * [navigationBar] reçoit la barre d'onglets (`NiumiTabBar`), bord à bord tout en bas, posée sur le
 * contenu : elle prend le bas de l'écran à sa charge, barre système comprise (`marginsAboveTabBar`,
 * décision de Mehdi du 2026-10-25), et le contenu va jusqu'en bas, sous elle, pour se deviner à
 * travers son haut translucide. Le contenu qui défile finit alors par `niumiTabBarClearance()`.
 * [contentUnderTop] fait de même en haut (accueil, demande de Mehdi du 2026-10-30) : le contenu
 * monte jusqu'en haut de l'écran, sous une bande translucide en bas (`niumiTopFade`), et commence par
 * `niumiTopClearance()`.
 */
@Composable
fun NiumiScreenScaffold(
    modifier: Modifier = Modifier,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    navigationBar: (@Composable () -> Unit)? = null,
    contentUnderTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    CompositionLocalProvider(LocalContentColor provides palette.ink) {
        Box(modifier = modifier.fillMaxSize().background(palette.background)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (navigationBar == null) {
                                Modifier.marginsOver(WindowInsets.safeDrawing, NiumiDimens.screenMargins, keepTop = !contentUnderTop)
                            } else {
                                Modifier.marginsAboveTabBar(
                                    WindowInsets.safeDrawing,
                                    NiumiDimens.screenMargins,
                                    keepTop = !contentUnderTop,
                                )
                            },
                        ),
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
                if (bottom != null) Column(modifier = Modifier.fillMaxWidth(), content = bottom)
            }
            if (contentUnderTop) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(niumiTopClearance())
                            .niumiTopFade(palette.background),
                )
            }
            if (navigationBar != null) Box(modifier = Modifier.align(Alignment.BottomCenter)) { navigationBar() }
        }
    }
}

@Composable
private fun ScaffoldSample() {
    NiumiScreenScaffold(bottom = { NiumiPrimaryButton(text = "Continuer", onClick = {}) }) {
        NiumiTopBar(onBack = {}, backLabel = "Retour", title = "Réglages")
        Text("Contenu de l’écran", style = NiumiType.body16)
    }
}

@Preview(name = "Clair", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiScreenScaffoldLightPreview() {
    NiumiTheme(dark = false) { ScaffoldSample() }
}

@Preview(name = "Sombre", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiScreenScaffoldDarkPreview() {
    NiumiTheme(dark = true) { ScaffoldSample() }
}
