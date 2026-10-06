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
import com.niumi.feature.session.R

/**
 * Ligne « Sonnerie », partagée par les écrans 5, 6 et 7 (SPEC_ANDROID §15, Lot 7) : même titre,
 * même résumé. Une fonction séparée plutôt qu'un bloc dans chaque écran, pour ne pas faire
 * dépasser `LongMethod` de detekt sur des composables déjà proches du plafond (`ETAPE-24.md`,
 * `ETAPE-27.md`).
 *
 * [onClick] nul sur l'écran 6, qui ne fait qu'afficher le résumé de la préférence (§15) ; sur
 * l'écran 5 toujours non nul ; sur l'écran 7 seulement en `ARMED` (§3, dérogation « sans scan »).
 */
@Composable
fun AlarmSoundRow(
    title: String,
    summary: String,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.semantics { contentDescription = "" },
            )
        }
    }
}
