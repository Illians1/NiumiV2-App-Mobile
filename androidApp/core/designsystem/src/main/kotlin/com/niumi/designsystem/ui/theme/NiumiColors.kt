package com.niumi.designsystem.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur bruts de `docs/CHARTE_GRAPHIQUE_APP_MOBILE.md` §2 à §5. Exposés séparément
 * du `ColorScheme` Material 3 pour les usages que les rôles Material ne couvrent pas
 * directement (ex. Laiton, réservé aux références matérielles hors interface numérique — §4).
 */
object NiumiColors {
    // Sombres (§2 « Sombres »)
    val Encre = Color(0xFF0E0E10)
    val Surface = Color(0xFF1A1A1D)
    val Filet = Color(0xFF1F1F23)

    // Gris de texte (§2 « Gris de texte »)
    val TextPrincipal = Color(0xFFFAFAF7)
    val TextAttenue = Color(0xFF8A8A8F)
    val TextDiscret = Color(0xFF6A6A6E)
    val TextInactif = Color(0xFF3A3A3E)

    // Clairs (§2 « Clairs »)
    val Creme = Color(0xFFF4EEE3)
    val SurCreme = Color(0xFF1A1408)
    val SurCremeAttenue = Color(0xFF6F6252)

    // Couleur signature (§3)
    val Ambre = Color(0xFFE9A23B)

    // Laiton (§4) : élément patrimonial et matériel, pas une couleur fonctionnelle de
    // l'interface numérique — ne pas l'utiliser en concurrence de l'Ambre à l'écran (§4, §21).
    val Laiton = Color(0xFFC9A36B)
    val LaitonSoutenu = Color(0xFFB78F58)

    // Couleur d'alerte (§5)
    val Terracotta = Color(0xFFC4654E)
}
