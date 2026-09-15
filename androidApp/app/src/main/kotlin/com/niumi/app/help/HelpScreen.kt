package com.niumi.app.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Écran 13 « Aide et limites » (SPEC_ANDROID §15, étape 21), atteignable depuis l'accueil.
 *
 * Écran de **consultation**, sans état ni ViewModel : son contenu est constant (voir [HelpTexts]),
 * et il ne porte aucune action. Il n'en porte pas davantage pendant une session : §3 et §10.2
 * font du scan du boîtier la seule sortie, et un bouton ici serait précisément le recours logiciel
 * que §4.5 exclut. Le geste Retour du système suffit, comme sur les autres écrans du dépôt.
 *
 * Page unique défilante, comme l'onboarding : tout le contenu passe devant l'utilisateur dans
 * l'ordre, y compris sous TalkBack (§15).
 */
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = HelpTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = HelpTexts.INTRO, style = MaterialTheme.typography.bodyLarge)
            HelpTexts.sections.forEach { section ->
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                section.items.forEach { item ->
                    Text(text = "•  $item", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    NiumiTheme {
        HelpScreen()
    }
}
