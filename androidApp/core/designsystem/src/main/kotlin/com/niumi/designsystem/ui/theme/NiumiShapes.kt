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
}
