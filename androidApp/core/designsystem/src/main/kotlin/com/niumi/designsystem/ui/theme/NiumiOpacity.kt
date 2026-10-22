package com.niumi.designsystem.ui.theme

/**
 * Opacités sans couleur propre dans le design. Une seule pour l'instant : le contenu grisé d'une
 * case inactive de la grille des applications (écran 4, limite de 50 atteinte, décision de Mehdi
 * du 2026-10-22). Aucune maquette ne dessine cet état : la valeur est celle du contenu inactif de
 * Material 3 (38 %), posée sur une icône passée en niveaux de gris.
 */
object NiumiOpacity {
    const val GREYED_CONTENT = 0.38f
}
