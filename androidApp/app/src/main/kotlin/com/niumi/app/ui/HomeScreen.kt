package com.niumi.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niumi.app.navigation.NavEntryPoint
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Accueil sans session (SPEC_ANDROID §15). En `release`, `entryPoints` est toujours vide
 * (aucun `NavGraphContributor` lié) : l'écran affiche seulement « Aucune session », comme
 * avant l'introduction de la navigation contribuée. En `debug`, la route POC y apparaît.
 */
@Composable
fun HomeScreen(
    entryPoints: List<NavEntryPoint>,
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Aucune session",
                style = MaterialTheme.typography.headlineMedium,
            )
            entryPoints.forEach { entryPoint ->
                TextButton(onClick = { onEntryPointClick(entryPoint.route) }) {
                    Text(entryPoint.label)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NiumiTheme {
        HomeScreen(entryPoints = emptyList(), onEntryPointClick = {})
    }
}
