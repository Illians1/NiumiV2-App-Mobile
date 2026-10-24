package com.niumi.feature.session.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette

/**
 * Ligne « Sonnerie » (maquette 16), partagée par les écrans 5, 6 et 7 : [title] à gauche, à
 * droite [summary] — le nom du son et, dessous, « Progressive · {durée} » quand elle n'est pas sur
 * « Non » (D5, D6, étape 40). Une fonction séparée plutôt qu'un bloc dans chaque écran, pour ne pas
 * faire dépasser `LongMethod` de detekt sur des composables déjà proches du plafond
 * (`ETAPE-24.md`, `ETAPE-27.md`).
 *
 * [onClick] nul sur l'écran 6, qui ne fait qu'afficher le résumé de la préférence (§9) ; sur
 * l'écran 5 toujours non nul ; sur l'écran 7 seulement en `ARMED` (§3, dérogation « sans scan »).
 */
@Composable
fun AlarmSoundRow(
    title: String,
    summary: AlarmSoundSummary,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rowModifier =
        if (onClick != null) {
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(vertical = 8.dp)
        } else {
            modifier.fillMaxWidth().padding(vertical = 8.dp)
        }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = summary.name, style = MaterialTheme.typography.bodyLarge)
            summary.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalNiumiPalette.current.textSecondary,
                )
            }
        }
        if (onClick != null) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalNiumiPalette.current.textSecondary),
                modifier = Modifier.semantics { contentDescription = "" },
            )
        }
    }
}
