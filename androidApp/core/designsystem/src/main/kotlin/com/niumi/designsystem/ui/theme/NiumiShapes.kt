package com.niumi.designsystem.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Formes des maquettes (VALEURS-DE-DESIGN.md §4), vérifiées par `NiumiShapesAndDimensTest`. */
object NiumiShapes {
    /** Bouton principal, bouton d'alerte ou de feuille de scan, bouton de la recopie : coins de 14. */
    val button = RoundedCornerShape(14.dp)

    /** Carte : coins de 14. */
    val card = RoundedCornerShape(14.dp)

    /** Tuile d'autorisation, case de pièce : coins de 16. */
    val tile = RoundedCornerShape(16.dp)

    /** Bouton « Autoriser » d'une tuile : coins de 12. */
    val authorizeButton = RoundedCornerShape(12.dp)

    /** Bouton en pilule : coins égaux à la moitié de la hauteur. */
    val pill = RoundedCornerShape(percent = 50)

    /** Feuille du bas : coins de 28 en haut, bord à bord en bas. */
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Fenêtre d'alerte (maquette 57) : coins de 24. */
    val alert = RoundedCornerShape(24.dp)

    /** Emplacement d'icône d'application : coins de 7. */
    val appIcon = RoundedCornerShape(7.dp)

    // Écran 4 (étape 37) : valeurs propres à l'écran, relevées dans les sources des maquettes 10 à 12
    // (VALEURS §3 : « les valeurs exactes de chaque écran sont dans les sources »).

    /** Carte de sélection des applications : coins de 20, plus ronds que la carte d'état. */
    val selectionCard = RoundedCornerShape(20.dp)

    /** Case d'icône de la carte de sélection : coins de 10. */
    val appSlot = RoundedCornerShape(10.dp)

    /** Case d'icône de la grille de la feuille : coins de 14. */
    val appTile = RoundedCornerShape(14.dp)

    /** Champ « Rechercher » de la feuille : coins de 14. */
    val searchField = RoundedCornerShape(14.dp)

    /** Corps de la bulle du volume (feuille « Sonnerie », maquette 19) : coins de 12. */
    val bubble = RoundedCornerShape(12.dp)

    /** Barre segmentée (maquette 18) : coins de 14 ; chaque segment, coins de 10. */
    val segmentedBar = RoundedCornerShape(14.dp)
    val segment = RoundedCornerShape(10.dp)

    /** Bulle de message en bas d'écran (étape 45, forme de la maquette 27). */
    val toast = RoundedCornerShape(16.dp)
}
