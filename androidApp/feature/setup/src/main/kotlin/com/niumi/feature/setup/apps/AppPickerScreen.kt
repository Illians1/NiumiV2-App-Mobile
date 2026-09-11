package com.niumi.feature.setup.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.apps.InstalledApp

private val ICON_SIZE = 40.dp

/**
 * Écran 4 — sélection des applications (SPEC_ANDROID §12.1, §15). Composable pur et sans état,
 * testé en isolation.
 *
 * Le nom de package n'est affiché que sur les lignes dont le libellé est en doublon, comme §12.1
 * l'impose : le montrer partout noierait l'information utile.
 */
@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
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
            Text(text = AppPickerTexts.TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = AppPickerTexts.INSTRUCTION, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = AppPickerTexts.counterLabel(state.selectedCount),
                style = MaterialTheme.typography.titleMedium,
            )

            state.message?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }

            when {
                state.isLoading -> {
                    Text(text = AppPickerTexts.LOADING, style = MaterialTheme.typography.bodyLarge)
                }

                state.isEmpty -> {
                    Text(text = AppPickerTexts.EMPTY, style = MaterialTheme.typography.bodyLarge)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.items, key = { it.app.packageName }) { item ->
                            AppRow(item = item, onToggle = { onToggle(item.app.packageName) })
                        }
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = state.canConfirm,
                modifier = Modifier.semantics { contentDescription = AppPickerTexts.CONFIRM_BUTTON_LABEL },
            ) {
                Text(AppPickerTexts.CONFIRM_BUTTON_LABEL)
            }
        }
    }
}

@Composable
private fun AppRow(
    item: AppPickerItem,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics { contentDescription = item.app.label }
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(item.app)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.app.label, style = MaterialTheme.typography.bodyLarge)
            if (item.showPackageName) {
                Text(text = item.app.packageName, style = MaterialTheme.typography.labelSmall)
            }
        }
        Checkbox(checked = item.isSelected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AppIcon(app: InstalledApp) {
    val sizePx = with(LocalDensity.current) { ICON_SIZE.roundToPx() }
    val bitmap = remember(app.packageName, sizePx) { app.icon?.toImageBitmapOrNull(sizePx) }
    if (bitmap != null) {
        // `contentDescription` nul : le libellé porté par la ligne suffit à TalkBack, l'icône
        // répéterait la même information (§15).
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
    } else {
        Text(text = "  ", modifier = Modifier.size(ICON_SIZE))
    }
}

/**
 * Point d'entrée réel. [onSessionInProgress] applique la garde de `SetupGate` : une session en
 * cours interdit de modifier la sélection (SPEC_CORE_KMP §2 point 11, SPEC_ANDROID §12.1).
 */
@Composable
fun AppPickerRoute(
    onConfirmed: () -> Unit,
    onSessionInProgress: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    LaunchedEffect(state.isSessionInProgress) {
        if (state.isSessionInProgress) onSessionInProgress()
    }

    AppPickerScreen(
        state = state,
        onToggle = viewModel::toggle,
        onConfirm = { viewModel.confirm(onConfirmed) },
    )
}

@Preview(showBackground = true)
@Composable
private fun AppPickerScreenPreview() {
    NiumiTheme {
        AppPickerScreen(
            state =
                AppPickerUiState(
                    items =
                        listOf(
                            AppPickerItem(
                                InstalledApp("com.example.chat", "Chat", icon = null),
                                isSelected = true,
                                showPackageName = false,
                            ),
                            AppPickerItem(
                                InstalledApp("com.example.social", "Réseau social", icon = null),
                                isSelected = false,
                                showPackageName = false,
                            ),
                        ),
                    isLoading = false,
                ),
            onToggle = {},
            onConfirm = {},
        )
    }
}
