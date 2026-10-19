package com.niumi.designsystem.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.niumi.designsystem.R

/**
 * IBM Plex Sans, validée le 9 octobre 2026 (VALEURS-DE-DESIGN.md §3), embarquée pour être la même
 * sur tous les téléphones. Fichiers TTF de l'archive officielle d'IBM (`@ibm/plex-sans@1.1.0`),
 * licence SIL Open Font License 1.1 dans `res/raw/ofl_ibm_plex_sans.txt`. Trois graisses, pas
 * d'italique ; les chiffres ont tous la même largeur sans réglage.
 */
object NiumiFont {
    val PlexSans: FontFamily =
        FontFamily(
            Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
            Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
            Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
        )
}
