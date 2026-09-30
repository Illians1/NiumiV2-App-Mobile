package com.niumi.designsystem.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * Appelle [onRegained] chaque fois que la fenêtre **retrouve** le focus, jamais à la composition
 * initiale ni à la perte du focus.
 *
 * Tirer le volet rapide ne met pas l'activité en pause : la fenêtre perd seulement le focus, et
 * le referme ne produit aucun `ON_RESUME`. Un écran qui ne se recalcule qu'à `ON_RESUME` ignore
 * donc un réglage changé depuis le volet (écart 10 de `RELEASE_REPORT.md`, SPEC_ANDROID §13).
 * Cet effet complète `ON_RESUME`, il ne le remplace pas.
 *
 * Au retour d'une autre activité (réglages système), `ON_RESUME` puis le retour du focus
 * surviennent tous les deux : l'appelant doit tolérer deux rappels rapprochés, ce qui est le cas
 * d'une évaluation en lecture seule dont la dernière relance fait foi.
 */
@Composable
fun WindowFocusRegainedEffect(onRegained: () -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val currentOnRegained by rememberUpdatedState(onRegained)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }
            .distinctUntilChanged()
            .drop(1)
            .filter { focused -> focused }
            .collect { currentOnRegained() }
    }
}
