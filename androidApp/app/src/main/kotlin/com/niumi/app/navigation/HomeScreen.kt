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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.ringing.AlarmActivity

const val PREPARE_BUTTON_LABEL = "Préparer mon réveil"

/**
 * Une session en cours doit garder une sortie depuis l'accueil : §10.4 en fait la seconde
 * garantie d'accès au scan, indépendante de la notification. Sans ce bouton, l'accueil serait un
 * cul-de-sac dès que l'étape 14 rend une session armable.
 */
const val VIEW_SESSION_BUTTON_LABEL = "Voir ma session"

/** Écran 12 (étape 16) : le journal technique et les contrôles restent consultables hors session. */
const val DIAGNOSTIC_BUTTON_LABEL = "Voir le diagnostic"
private const val NO_SESSION_TITLE = "Aucune session"
private const val ACTIVE_SESSION_TITLE = "Une session est en cours."

/**
 * Étape 20, défaut mesuré sur appareil : quand la persistance est illisible, l'accueil affichait
 * « Aucune session » alors qu'une session était armée et le blocage en place. §15 interdit ce faux
 * état de fiabilité ; l'accueil dit donc ce qu'il sait — et surtout ce qu'il ne sait plus.
 */
private const val STORAGE_UNREADABLE_TITLE = "État illisible"
private const val STORAGE_UNREADABLE_MESSAGE =
    "Niumi ne peut plus lire son état enregistré. Si une session était en cours, tes applications " +
        "restent bloquées et le scan du boîtier reste la seule sortie."
private const val STORAGE_UNREADABLE_BUTTON_LABEL = "Voir le diagnostic"

/**
 * Titre de l'accueil. Fonctions pures et non `when` inline dans le composable : c'est la règle de
 * §15 (« ne jamais afficher un faux état de fiabilité »), et elle se teste sans rendu Compose.
 * L'état illisible prime sur tout : sans lecture, « Aucune session » serait une affirmation que
 * Niumi n'est pas en mesure de faire.
 */
fun homeTitleFor(state: HomeUiState): String =
    when {
        state.storageUnreadable -> STORAGE_UNREADABLE_TITLE
        state.hasActiveSession -> ACTIVE_SESSION_TITLE
        else -> NO_SESSION_TITLE
    }

/** Même règle pour le bouton principal : il mène au diagnostic, seul écran honnête à ce stade. */
fun primaryLabelFor(state: HomeUiState): String =
    when {
        state.storageUnreadable -> STORAGE_UNREADABLE_BUTTON_LABEL
        state.hasActiveSession -> VIEW_SESSION_BUTTON_LABEL
        else -> PREPARE_BUTTON_LABEL
    }

/**
 * Les trois sorties de l'accueil, groupées : l'écran 12 (étape 16) en portait une sixième et
 * `HomeScreen` dépassait `LongParameterList` de detekt. Les regrouper dit aussi ce qu'elles sont —
 * la navigation de l'accueil — là où six paramètres plats ne disaient plus rien.
 */
data class HomeActions(
    val onPrepare: () -> Unit,
    val onOpenDiagnostic: () -> Unit = {},
    val onEntryPointClick: (String) -> Unit = {},
)

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
    actions: HomeActions,
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
                text = homeTitleFor(state),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (state.storageUnreadable) {
                Text(text = STORAGE_UNREADABLE_MESSAGE, style = MaterialTheme.typography.bodyMedium)
            }
            val primaryLabel = primaryLabelFor(state)
            Button(
                onClick = actions.onPrepare,
                modifier = Modifier.semantics { contentDescription = primaryLabel },
            ) {
                Text(primaryLabel)
            }
            // Sur un état illisible, le bouton principal mène déjà au diagnostic : le lien
            // secondaire ferait doublon (constaté à l'écran le 2026-09-15).
            if (!state.storageUnreadable) {
                TextButton(onClick = actions.onOpenDiagnostic) {
                    Text(DIAGNOSTIC_BUTTON_LABEL)
                }
            }
            entryPoints.forEach { entryPoint ->
                TextButton(onClick = { actions.onEntryPointClick(entryPoint.route) }) {
                    Text(entryPoint.label)
                }
            }
        }
    }
}

/**
 * Point d'entrée réel : relit l'accusé de réception de l'onboarding à chaque retour au premier
 * plan. La redirection automatique vers l'écran 8 vit dans `MainActivity` (§10.4) — elle doit
 * s'appliquer quelle que soit la destination du `NavHost`, pas seulement depuis l'accueil.
 */
@Composable
fun HomeRoute(
    entryPoints: List<NavEntryPoint>,
    onNavigate: (NiumiRoute) -> Unit,
    onOpenDiagnostic: () -> Unit,
    onEntryPointClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

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
        actions =
            HomeActions(
                // §10.4 : tant que la session attend un scan, le bouton principal mène à
                // l'écran 8, jamais à l'écran 7 — qui n'active pas le Reader Mode (§11.2).
                onPrepare = {
                    if (viewModel.state.alarmScreenRequired) {
                        context.startActivity(AlarmActivity.intent(context))
                    } else {
                        onNavigate(viewModel.state.destination)
                    }
                },
                onOpenDiagnostic = onOpenDiagnostic,
                onEntryPointClick = onEntryPointClick,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NiumiTheme {
        HomeScreen(state = HomeUiState(), entryPoints = emptyList(), actions = HomeActions(onPrepare = {}))
    }
}
