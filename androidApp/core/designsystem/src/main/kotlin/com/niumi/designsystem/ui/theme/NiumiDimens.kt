package com.niumi.designsystem.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Dimensions des maquettes (VALEURS-DE-DESIGN.md §4), dessinées pour un écran de 390 × 844 points :
 * des dp sur Android. Les écrans s'adaptent aux autres tailles ; seules ces mesures sont fixes.
 * Vérifiées par `NiumiShapesAndDimensTest`.
 */
object NiumiDimens {
    /** Marges de l'écran : 24 à gauche et à droite. */
    val screenHorizontal = 24.dp

    /** Marge du bas de l'écran. */
    val screenBottom = 34.dp

    /** Marge du haut de l'écran, sous la barre d'état. */
    val screenTop = 56.dp

    /** Zone de toucher minimale. */
    val minTouch = 44.dp

    /** Bouton principal, pleine largeur. */
    val primaryButton = 56.dp

    /** Bouton d'une alerte ou de la feuille de scan. */
    val sheetButton = 52.dp

    /** Bouton de la recopie, au-dessus du clavier. */
    val transcriptionButton = 48.dp

    /** Bouton « Autoriser » d'une tuile. */
    val authorizeButton = 46.dp

    /** Bouton en pilule (« Modifier », durées). */
    val pillButton = 44.dp

    /** Bouton en pilule « Choisir les applications ». */
    val pillButtonLarge = 48.dp

    /** Pastille de jour : cercle de 44. */
    val dayChip = 44.dp

    /** Interrupteur : 52 × 32, pastille de 26. */
    val switchWidth = 52.dp
    val switchHeight = 32.dp
    val switchThumb = 26.dp

    /** Emplacement d'icône d'application (rangée de l'accueil). */
    val appIcon = 28.dp
}
