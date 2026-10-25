package com.niumi.feature.session.transitional

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.component.NiumiListRow
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiTopBar
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.session.tabs.MainTab
import com.niumi.feature.session.tabs.MainTabBar

/**
 * Réglages transitoires (étapes 45 à 48) : trois lignes vers l'écran 3 actuel, le diagnostic
 * d'incident et l'aide. Remplacée par l'écran 12 à l'étape 48.
 *
 * Ouverte par l'onglet « Réglages » de la barre du bas (décision de Mehdi du 2026-10-25) : elle garde
 * la barre, « Réglages » choisi, et n'a pas de flèche de retour ; l'onglet « Réveil » et le retour du
 * téléphone ramènent à l'accueil ([onOpenWake]).
 */
@Composable
fun TransitionalSettingsScreen(
    onOpenWake: () -> Unit,
    onOpenNiumiSpots: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    NiumiScreenScaffold(
        navigationBar = { MainTabBar(selected = MainTab.SETTINGS, onWake = onOpenWake, onSettings = {}) },
    ) {
        NiumiTopBar(onBack = null, backLabel = null, title = TransitionalSettingsTexts.TITLE)
        NiumiListRow(title = TransitionalSettingsTexts.NIUMI_SPOTS, onClick = onOpenNiumiSpots, bottomSeparator = true)
        NiumiListRow(title = TransitionalSettingsTexts.DIAGNOSTIC, onClick = onOpenDiagnostic, bottomSeparator = true)
        NiumiListRow(title = TransitionalSettingsTexts.HELP, onClick = onOpenHelp, bottomSeparator = true)
    }
}

@Preview(name = "Clair", widthDp = 390, heightDp = 844)
@Composable
private fun TransitionalSettingsPreview() {
    NiumiTheme(dark = false) { TransitionalSettingsScreen({}, {}, {}, {}) }
}
