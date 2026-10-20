package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Cadre d'un écran Niumi, bord à bord (SPEC_ANDROID §15) : fond du thème sous les barres système,
 * marges des maquettes (VALEURS-DE-DESIGN.md §4 : 24 sur les côtés, 56 en haut, 34 en bas) posées
 * sur les barres, l'encoche et le clavier (`safeDrawing`) : de chaque côté, la plus grande des
 * deux, pas leur somme (décision du 2026-10-20).
 * [bottom] reçoit le bouton principal, collé au bas ; [content] prend la place restante et gère
 * lui-même son défilement (une liste paresseuse ne peut pas vivre dans une colonne défilante).
 */
@Composable
fun NiumiScreenScaffold(
    modifier: Modifier = Modifier,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    CompositionLocalProvider(LocalContentColor provides palette.ink) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .marginsOver(WindowInsets.safeDrawing, NiumiDimens.screenMargins),
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
            if (bottom != null) Column(modifier = Modifier.fillMaxWidth(), content = bottom)
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
