package com.niumi.feature.session.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Écran 10 : session terminée (SPEC_ANDROID §15). Vit à côté de l'écran 11 plutôt que dans
 * `:feature:ringing` — écart au plan, assumé à l'étape 17 : les deux écrans sont jumeaux, §15 les
 * traite ensemble, et celui-ci n'a ni audio, ni NFC, ni service.
 *
 * Atteint uniquement après un état final `COMPLETED`, donc après `RELEASE_SUCCEEDED` (§11.3) : le
 * texte peut affirmer que les applications sont débloquées sans risque de mentir (§15, « ne jamais
 * afficher un faux état de fiabilité »). Il n'est réellement atteignable qu'à partir de l'étape
 * 18, qui livre le scan de libération.
 */
@Composable
fun CompletedScreen(
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = CompletedTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = CompletedTexts.BODY, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
                Text(text = CompletedTexts.BACK_HOME_BUTTON)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompletedScreenPreview() {
    NiumiTheme {
        CompletedScreen(onBackHome = {})
    }
}
