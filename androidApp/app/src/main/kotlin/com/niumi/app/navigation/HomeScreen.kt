package com.niumi.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme

const val PREPARE_BUTTON_LABEL = "Préparer mon réveil"
private const val NO_SESSION_TITLE = "Aucune session"
private const val ACTIVE_SESSION_TITLE = "Une session est en cours."

/**
 * Accueil (SPEC_ANDROID §15, écran 1). En `release`, `entryPoints` est toujours vide : le seul
 * `NavGraphContributor` vit dans `src/debug` et disparaît à l'étape 21.
 *
 * Quand une session non finale existe, l'accueil ne propose pas de préparer un réveil. La
 * redirection vers l'écran de session active de §10.4 sera branchée à l'étape 15, avec l'écran 7 ;
 * aucune session ne peut être armée avant l'étape 14.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    entryPoints: List<NavEntryPoint>,
    onPrepare: () -> Unit,
    onEntryPointClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (state.hasActiveSession) ACTIVE_SESSION_TITLE else NO_SESSION_TITLE,
                style = MaterialTheme.typography.headlineMedium,
            )
            if (!state.hasActiveSession) {
                Button(
                    onClick = onPrepare,
                    modifier = Modifier.semantics { contentDescription = PREPARE_BUTTON_LABEL },
                ) {
                    Text(PREPARE_BUTTON_LABEL)
                }
            }
            entryPoints.forEach { entryPoint ->
                TextButton(onClick = { onEntryPointClick(entryPoint.route) }) {
                    Text(entryPoint.label)
                }
            }
        }
    }
}

/** Point d'entrée réel : relit l'accusé de réception de l'onboarding à chaque retour au premier plan. */
@Composable
fun HomeRoute(
    entryPoints: List<NavEntryPoint>,
    onNavigate: (NiumiRoute) -> Unit,
    onEntryPointClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        state = viewModel.state,
        entryPoints = entryPoints,
        onPrepare = { onNavigate(viewModel.state.destination) },
        onEntryPointClick = onEntryPointClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NiumiTheme {
        HomeScreen(state = HomeUiState(), entryPoints = emptyList(), onPrepare = {}, onEntryPointClick = {})
    }
}
