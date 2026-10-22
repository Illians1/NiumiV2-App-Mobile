package com.niumi.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import kotlinx.coroutines.delay

/**
 * Indicateur de chargement (décision de Mehdi du 2026-10-22, sans maquette) : un cercle animé à
 * l'encre, sans piste ni texte à l'écran ; [label] est lu par TalkBack. Il n'apparaît qu'après
 * [NiumiLoadingIndicatorDefaults.DELAY_MILLIS], pour ne pas clignoter quand la réponse arrive vite.
 */
@Composable
fun NiumiLoadingIndicator(
    label: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(NiumiLoadingIndicatorDefaults.DELAY_MILLIS)
        visible = true
    }
    if (!visible) return
    CircularProgressIndicator(
        modifier = modifier.size(NiumiDimens.loadingIndicator).semantics { contentDescription = label },
        color = LocalNiumiPalette.current.ink,
        trackColor = Color.Transparent,
        strokeWidth = NiumiDimens.loadingIndicatorStroke,
    )
}

object NiumiLoadingIndicatorDefaults {
    /** Délai avant l'apparition : en dessous, une attente ne se remarque pas. */
    const val DELAY_MILLIS = 300L
}

@Preview(name = "Clair")
@Composable
private fun NiumiLoadingIndicatorLightPreview() {
    NiumiPreviewFrame(dark = false) { NiumiLoadingIndicator(label = "Chargement") }
}

@Preview(name = "Sombre")
@Composable
private fun NiumiLoadingIndicatorDarkPreview() {
    NiumiPreviewFrame(dark = true) { NiumiLoadingIndicator(label = "Chargement") }
}
