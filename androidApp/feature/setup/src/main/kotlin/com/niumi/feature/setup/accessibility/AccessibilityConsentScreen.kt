package com.niumi.feature.setup.accessibility

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Écran de consentement avant blocage (SPEC_ANDROID §12.3). Composable pur et sans état, testé
 * en isolation (même motif que `AlarmScreen` : aucun clic n'y est simulé, seul le nombre de
 * points affichés et la présence du bouton sont vérifiés).
 */
@Composable
fun AccessibilityConsentScreen(
    state: AccessibilityConsentUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = AccessibilityConsentTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            AccessibilityConsentTexts.points.forEach { point ->
                Text(text = "•  $point", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = if (state.isServiceEnabled) "Service actif" else "Service inactif",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
                Text(AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL)
            }
        }
    }
}

/**
 * Point d'entrée réel, connecté au ViewModel et aux réglages système. Recalcule l'état à
 * chaque retour au premier plan (SPEC_ANDROID §13), sans jamais ouvrir les réglages ni cliquer
 * à la place de l'utilisateur (§12.3) : [onOpenSettings] ne fait qu'ouvrir
 * `ACTION_ACCESSIBILITY_SETTINGS`.
 */
@Composable
fun AccessibilityConsentRoute(viewModel: AccessibilityConsentViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AccessibilityConsentScreen(
        state = viewModel.state,
        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
    )
}

@Preview(showBackground = true)
@Composable
private fun AccessibilityConsentScreenPreview() {
    NiumiTheme {
        AccessibilityConsentScreen(state = AccessibilityConsentUiState(isServiceEnabled = false), onOpenSettings = {})
    }
}
