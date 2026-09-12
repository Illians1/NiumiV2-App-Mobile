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

/**
 * Une session en cours doit garder une sortie depuis l'accueil : §10.4 en fait la seconde
 * garantie d'accès au scan, indépendante de la notification. Sans ce bouton, l'accueil serait un
 * cul-de-sac dès que l'étape 14 rend une session armable.
 */
const val VIEW_SESSION_BUTTON_LABEL = "Voir ma session"
private const val NO_SESSION_TITLE = "Aucune session"
private const val ACTIVE_SESSION_TITLE = "Une session est en cours."

/**
 * Accueil (SPEC_ANDROID §15, écran 1). En `release`, `entryPoints` est toujours vide : le seul
 * `NavGraphContributor` vit dans `src/debug` et disparaît à l'étape 21.
 *
 * Quand une session non finale existe, l'accueil ne propose pas de préparer un réveil : son bouton
 * mène à l'écran de session active (§10.4, seconde garantie d'accès au scan). La destination est
 * calculée par [homeDestinationFor], jamais par l'écran.
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
            val primaryLabel = if (state.hasActiveSession) VIEW_SESSION_BUTTON_LABEL else PREPARE_BUTTON_LABEL
            Button(
                onClick = onPrepare,
                modifier = Modifier.semantics { contentDescription = primaryLabel },
            ) {
                Text(primaryLabel)
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
