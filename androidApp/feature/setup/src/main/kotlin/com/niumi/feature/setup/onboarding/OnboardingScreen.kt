package com.niumi.feature.setup.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Onboarding des limites produit (SPEC_ANDROID §3 dernier point, §4.2 à §4.5, §13, §13.1).
 * Composable pur et sans état. Page unique défilante : tout le contenu passe devant l'utilisateur
 * dans l'ordre, y compris sous TalkBack, et la case de confirmation reste le seul passage.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAcknowledgedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text(text = OnboardingTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = OnboardingTexts.INTRO, style = MaterialTheme.typography.bodyLarge)
            OnboardingTexts.limits.forEach { limit ->
                Text(text = "•  $limit", style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.isAcknowledged,
                    onCheckedChange = onAcknowledgedChange,
                    modifier =
                        Modifier.semantics {
                            contentDescription = OnboardingTexts.ACKNOWLEDGEMENT_LABEL
                        },
                )
                Text(text = OnboardingTexts.ACKNOWLEDGEMENT_LABEL, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.semantics { contentDescription = OnboardingTexts.CONTINUE_BUTTON_LABEL },
            ) {
                Text(OnboardingTexts.CONTINUE_BUTTON_LABEL)
            }
        }
    }
}

/** Point d'entrée réel. [onContinue] n'est appelé qu'une fois l'accusé de réception persisté. */
@Composable
fun OnboardingRoute(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(
        state = viewModel.state,
        onAcknowledgedChange = viewModel::setAcknowledged,
        onContinue = { viewModel.confirm(onContinue) },
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    NiumiTheme {
        OnboardingScreen(state = OnboardingUiState(), onAcknowledgedChange = {}, onContinue = {})
    }
}
