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

    /** Marge du bas de l'écran ; sur Android, jamais moins que la barre de navigation plus [systemBarGap]. */
    val screenBottom = 34.dp

    /**
     * Espace gardé entre le contenu et la barre de navigation d'Android (décision de Mehdi du
     * 2026-10-22, VALEURS §4) : 64 au-dessus de la barre à trois boutons, 40 au-dessus des gestes.
     */
    val systemBarGap = 16.dp

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

    /** Zone de toucher minimale des maquettes : la taille visible des éléments touchables. */
    val minTouch = 44.dp

    /**
     * Zone tactile réservée sur Android autour de tout élément touchable plus petit (décision de
     * Mehdi du 2026-10-20, VALEURS §4) : 48, la recommandation de Google, posée par
     * `minimumInteractiveComponentSize()` de Material 3. Le dessin garde [minTouch].
     */
    val touchTarget = 48.dp

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

    /**
     * Retrait de la flèche de retour, pour aligner son tracé sur la marge (maquette 37) : -12 pour
     * la boîte de 44 de la maquette, -14 avec la zone de 48 réservée autour (décision du 2026-10-20).
     */
    val backButtonInset = (-14).dp

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

    /**
     * Retrait de la croix, pour aligner son tracé sur la marge (maquettes 11, 16) : -10 pour la
     * boîte de 44 de la maquette, -12 avec la zone de 48 réservée autour (décision du 2026-10-20).
     */
    val closeButtonInset = (-12).dp

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

    // Écran 1 · Présentation (étape 35), relevées dans les sources des maquettes 01 et 02.

    /** Dessin d'un panneau : 342 × 240 au plus, réduit en gardant ses proportions sur un écran étroit. */
    val illustrationWidth = 342.dp
    val illustrationHeight = 240.dp

    /** Largeur maximale du titre et du texte d'un panneau, centrés. */
    val presentationTitleMaxWidth = 320.dp
    val presentationBodyMaxWidth = 300.dp

    /** Espace entre le dessin et le titre d'un panneau. */
    val illustrationGap = 24.dp

    /** Espace entre le titre et le texte d'un panneau. */
    val titleBodyGap = 12.dp

    /** Espace entre l'indicateur de panneau et le bouton principal. */
    val pageIndicatorButtonGap = 18.dp

    // Écran 2 · Autorisations (étape 36), relevées dans la source de la maquette 03.

    /** Espace entre la rangée de la flèche et le titre. */
    val permissionsTitleTop = 4.dp

    /** Espace entre le titre et le texte d'introduction. */
    val permissionsTitleBodyGap = 6.dp

    /** Espace entre le texte d'introduction et la première tuile. */
    val permissionsTilesTop = 18.dp

    /** Espace entre deux tuiles. */
    val permissionTileSpacing = 10.dp

    /** Marge intérieure d'une tuile, et espace entre sa ligne et son bouton « Autoriser ». */
    val permissionTilePadding = 14.dp
    val permissionTileInnerGap = 12.dp

    /** Icône d'une tuile (cloche, cadenas, bulle). */
    val permissionTileIcon = 32.dp

    /** Espace entre le nom d'une autorisation et sa phrase. */
    val permissionTileTextGap = 2.dp

    // Fenêtre d'alerte (maquette 57 ; VALEURS §4 « Ombre de la fenêtre d'alerte »), étape 36.

    /** Marge de chaque côté de la fenêtre. */
    val alertHorizontalMargin = 28.dp

    /**
     * Marges intérieures, maquette de Mehdi du 2026-10-22 : 24 sur les côtés, 32 en haut au-dessus
     * de l'icône, 20 en bas.
     */
    val alertPadding = PaddingValues(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 20.dp)

    /** Rond gris de l'icône en tête de fenêtre, et l'espace qui le sépare du titre. */
    val alertIconCircle = 64.dp
    val alertIconTitleGap = 14.dp

    /**
     * Croix ronde au coin : rond de 36, croix de 18. La zone tactile de 48 est posée à 8 du coin,
     * ce qui met le rond dessiné à 14 du bord, comme dans la maquette.
     */
    val alertCloseCircle = 36.dp
    val alertCloseIcon = 18.dp
    val alertCloseInset = 8.dp

    /** Espace entre le titre et le corps, puis entre le corps et le bouton. */
    val alertTitleBodyGap = 10.dp
    val alertBodyButtonGap = 22.dp

    /** Ombre « décalage 20, flou 50, noir à 25 % », approchée par une élévation. */
    val alertElevation = 20.dp

    /**
     * Pastille ambre de la coche d'une tuile accordée (écran 2, décision de Mehdi du 2026-10-22,
     * exception écrite à la règle de l'ambre, VALEURS §2) : rond de 28, coche de 16.
     */
    val grantedBadge = 28.dp
    val grantedBadgeCheck = 16.dp

    // Écran 4 · Choix des applications (étape 37), relevées dans les sources des maquettes 10 à 12.

    /** Espace entre le titre de l'écran et son texte. */
    val appSelectionTitleBodyGap = 10.dp

    /** Espace au-dessus du titre, sous la marge du haut. */
    val appSelectionTitleTop = 24.dp

    /** Carte de sélection : 26 en haut et en bas, 20 sur les côtés, 16 entre ses éléments. */
    val selectionCardPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp)
    val selectionCardGap = 16.dp

    /** Case d'icône de la carte : 40, espacées de 10 ; trait pointillé de 1,5 quand elle est vide. */
    val appSlot = 40.dp
    val appSlotGap = 10.dp
    val appSlotDashedStroke = 1.5.dp

    /** Pointillé des cases vides : tirets de 4, espaces de 3 (rendu du `dashed` des navigateurs). */
    val appSlotDash = 4.dp
    val appSlotDashGap = 3.dp

    /** Espace entre la mention du bas et le bouton « Continuer ». */
    val appSelectionHintGap = 12.dp

    /** Champ « Rechercher » : 48 de haut, 14 de marge, loupe de 20 à 10 du texte, 8 sous le titre. */
    val searchFieldHeight = 48.dp
    val searchFieldPadding = 14.dp
    val searchIcon = 20.dp
    val searchIconGap = 10.dp
    val searchFieldTop = 8.dp

    /**
     * Croix qui vide la recherche (décision du 2026-10-22, sans maquette) : décalée de 15 vers la
     * droite (13 pour la boîte de 44, 2 de plus pour la zone de 48 réservée), pour que son tracé de
     * 18 finisse à 14 du bord du champ, comme la loupe commence.
     */
    val clearSearchInset = 15.dp

    /** Grille : 20 sous la recherche (et 8 de plus pour la pastille), 18 entre rangées, 10 entre colonnes. */
    val appGridTop = 20.dp
    val appGridInnerTop = 8.dp
    val appGridRowGap = 18.dp
    val appGridColumnGap = 10.dp

    /** Case de la grille : 56, bord de 2 si cochée, 6 au-dessus du libellé. */
    val appTile = 56.dp
    val appTileCheckedBorder = 2.dp
    val appTileLabelGap = 6.dp

    /** Pastille de la coche : rond de 22 débordant de 8 en haut à droite, coche de 14. */
    val appTileBadge = 22.dp
    val appTileBadgeOverflow = 8.dp
    val appTileBadgeCheck = 14.dp

    /** Pied de la page : 12 au-dessus du compteur, 12 entre le compteur et « Valider ». */
    val appSelectionFooterTop = 12.dp
    val appSelectionFooterGap = 12.dp

    /** Titre d'une section de la grille (« Choisies »), et l'espace qui le sépare des cases. */
    val appSectionTitleGap = 12.dp

    /**
     * Indicateur de chargement (décision du 2026-10-22) : cercle de 32, trait de 3. Aucune maquette
     * ne le dessine ; valeurs choisies à l'étape 37.
     */
    val loadingIndicator = 32.dp
    val loadingIndicatorStroke = 3.dp

    // Écran 10 · Réveil et feuille de scan (étape 38), relevées dans les sources des maquettes 28 à 33.

    /**
     * Espace au-dessus de la date, sous la marge du haut : 24 au lieu des 60 de la maquette
     * (décision de Mehdi du 2026-10-22, la date plus haut) ; puis 8 entre la date et l'heure.
     */
    val alarmDateTop = 24.dp
    val alarmDateClockGap = 8.dp

    /** Dessin de l'écran 10 : 264 × 170, et l'espace qui le sépare de la consigne. */
    val alarmDrawingWidth = 264.dp
    val alarmDrawingHeight = 170.dp
    val alarmDrawingGap = 14.dp

    /** Largeur maximale de la consigne, centrée. */
    val alarmInstructionMaxWidth = 300.dp

    /** Espace entre le bouton principal et le lien discret, en bas de l'écran comme dans la feuille d'échec. */
    val alarmButtonLinkGap = 6.dp

    /**
     * Place du lien « Je n'ai pas accès à mon Niumi Point » sous le bouton de l'écran 10 : l'écart
     * de 6 et la zone de 44 du lien. Réservée avant l'arrivée du lien (étape 49, décision du
     * 2026-10-22) pour que l'écran tombe où la maquette le dessine.
     */
    val alarmEmergencyLinkSpace = alarmButtonLinkGap + minTouch

    /** Feuille de scan, sans poignée : 28 au-dessus du contenu. */
    val scanSheetTop = 28.dp

    /** « Prêt à scanner » : 18 entre le titre, le rond, la phrase et « Annuler ». */
    val scanSheetGap = 18.dp

    /** Rond gris de l'icône de scan, 112, et son icône, 60. */
    val scanSheetCircle = 112.dp
    val scanSheetIcon = 60.dp

    /** Feuille d'échec : 12 entre le titre et l'aide, 6 de plus au-dessus du bouton ; texte de 310 au plus. */
    val scanFailureGap = 12.dp
    val scanFailureButtonTop = 6.dp
    val scanFailureTextMaxWidth = 310.dp
}
