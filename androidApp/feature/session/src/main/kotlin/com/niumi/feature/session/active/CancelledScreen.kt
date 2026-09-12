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
 * Écran 11 : session annulée (SPEC_ANDROID §15). Atteint uniquement après un état final
 * `CANCELLED`, donc après `RELEASE_SUCCEEDED` (§11.3) — le texte peut affirmer que les
 * applications sont débloquées sans risque de mentir (§15 : « ne jamais afficher un faux état de
 * fiabilité »).
 *
 * Une annulation avant la sonnerie ne compte pas comme une session terminée au réveil (§3) : cet
 * écran ne félicite donc de rien, il constate et propose de repartir.
 */
@Composable
fun CancelledScreen(
    onPrepareAgain: () -> Unit,
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
            Text(text = CancelledTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = CancelledTexts.BODY, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onPrepareAgain, modifier = Modifier.fillMaxWidth()) {
                Text(text = CancelledTexts.PREPARE_AGAIN_BUTTON)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CancelledScreenPreview() {
    NiumiTheme {
        CancelledScreen(onPrepareAgain = {})
    }
}
