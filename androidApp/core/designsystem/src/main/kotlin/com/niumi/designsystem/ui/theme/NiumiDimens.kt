package com.niumi.designsystem.ui.theme

import androidx.compose.foundation.layout.PaddingValues
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

    /**
     * Marge du haut de l'écran. Sur Android, elle couvre la barre d'état : la plus grande des
     * deux s'applique, pas leur somme (décision du 2026-10-20, VALEURS §4).
     */
    val screenTop = 56.dp

    /** Les quatre marges de l'écran, posées sur les barres système par `marginsOverSystemBars`. */
    val screenMargins =
        PaddingValues(start = screenHorizontal, top = screenTop, end = screenHorizontal, bottom = screenBottom)

    /** Marges du contenu d'une feuille du bas : côtés et bas de l'écran, rien en haut (poignée). */
    val sheetMargins = PaddingValues(start = screenHorizontal, end = screenHorizontal, bottom = screenBottom)

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

    // Mesures relevées dans les sources HTML des maquettes pour les composants (étape 34). Elles
    // ne figurent pas dans `valeurs-de-design.json` ; chaque commentaire cite la maquette.

    /** Trait d'un pixel : bord des cartes et des pastilles, séparateur, liseré de feuille, bouton au trait. */
    val hairline = 1.dp

    /** Icône de barre : flèche de retour, réglages (maquettes 14, 37). */
    val icon = 24.dp

    /** Croix de fermeture d'une feuille (maquettes 11, 16). */
    val closeIcon = 22.dp

    /** Petite icône de rangée : chevron, cadenas (maquettes 14, 15, 37). */
    val smallIcon = 18.dp

    /** Rangée de liste : hauteur minimale (maquette 37) ; 78 à 86 sur l'accueil (VALEURS §4). */
    val listRowMinHeight = 64.dp

    /** Espace entre le titre d'une rangée et sa partie droite (maquettes 14, 37). */
    val rowGap = 12.dp

    /** Espace entre la valeur d'une rangée et son chevron (maquette 37). */
    val trailingGap = 8.dp

    /** Retrait de la flèche de retour, pour aligner son tracé sur la marge (maquette 37). */
    val backButtonInset = (-12).dp

    /** Espace entre la barre de retour et le titre de l'écran (maquette 37). */
    val titleTop = 8.dp

    /** Haut de la feuille du bas, au-dessus de la poignée (maquettes 16, 19, 23). */
    val sheetTop = 10.dp

    /** Poignée de la feuille du bas : 40 × 4 (maquettes 16, 19, 23). */
    val sheetHandleWidth = 40.dp
    val sheetHandleHeight = 4.dp

    /** Titre d'une feuille sans croix, sous la poignée (maquettes 19, 23, 48). */
    val sheetTitleTop = 22.dp

    /** Ligne titre et croix d'une feuille : 8 sous la poignée, 48 de haut (maquettes 11, 16). */
    val sheetHeaderTop = 8.dp
    val sheetHeaderHeight = 48.dp

    /** Retrait de la croix, pour aligner son tracé sur la marge (maquettes 11, 16). */
    val closeButtonInset = (-10).dp

    /** Indicateur de panneau : traits de 24 × 6, espacés de 8 (maquette 01). */
    val pageIndicatorWidth = 24.dp
    val pageIndicatorHeight = 6.dp
    val pageIndicatorGap = 8.dp

    /** Marges intérieures de la carte d'état : 16 à gauche, 12 en haut et en bas, 14 à droite (maquette 14). */
    val cardPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)

    /** Marges latérales d'une pilule d'action : « Modifier », « Choisir les applications » (maquettes 10, 12). */
    val pillHorizontalPadding = 22.dp

    /** Marges latérales d'un lien discret centré (maquette 19). */
    val linkHorizontalPadding = 12.dp

    /** Logo en tête d'écran : 24 de haut (maquettes 01, 02, 05 à 07) ; 54 sur les écrans 7 et « À propos ». */
    val logoHeight = 24.dp
    val logoLargeHeight = 54.dp

    /** Écart entre la pastille et le bord de l'interrupteur (maquette 14). */
    val switchThumbInset = 3.dp
}
