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

    /** Interrupteur de ligne (feuille Sonnerie, maquette 18 ; décision du 2026-10-23) : 44 × 26, pastille de 20. */
    val switchRowWidth = 44.dp
    val switchRowHeight = 26.dp
    val switchRowThumb = 20.dp

    /**
     * Barre segmentée (maquette 18, durées de la sonnerie progressive) : 48 de haut, marge intérieure
     * de 4, écart de 4 entre segments, segments de 40. Le segment se dessine sur 40 mais se touche
     * sur 48 : la hauteur de la barre, sans l'épaissir (décision du 2026-10-20 sur la zone de 48).
     */
    val segmentedBarHeight = 48.dp
    val segmentedBarPadding = 4.dp
    val segmentedBarGap = 4.dp
    val segmentHeight = 40.dp

    /** Emplacement d'icône d'application (rangée de l'accueil). */
    val appIcon = 28.dp

    // Mesures relevées dans les sources HTML des maquettes pour les composants (étape 34). Elles
    // ne figurent pas dans `valeurs-de-design.json` ; chaque commentaire cite la maquette.

    /** Trait d'un pixel : bord des cartes et des pastilles, séparateur, liseré de feuille, bouton au trait. */
    val hairline = 1.dp

    /** Hauteur du fondu du bas d'une zone qui défile, quand du contenu y est caché (2026-10-22). */
    val scrollFadeHeight = 32.dp

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

    /**
     * Espace minimal entre la dernière tuile et « Continuer » (décision de Mehdi du 2026-10-22) :
     * la maquette 03 n'en a pas, et la dernière tuile touchait le bouton quand l'écran était plein.
     */
    val permissionsButtonTop = 16.dp

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
     * Place du lien « Je n'ai pas accès à mon Niumi Spot » sous le bouton de l'écran 10 : l'écart
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

    // Écran 7 · Écran de blocage (étape 39), relevées dans la source de la maquette 22.

    /**
     * Logo de 54 de haut, puis 88 jusqu'au titre : 32 dans la maquette (marge de 20 sous le logo et
     * écart de 12), texte descendu et logo remonté sur décision de Mehdi du 2026-10-23.
     * [blockLogoTopOffset], ajouté au-dessus du bloc centré, règle la hauteur du logo.
     */
    val blockLogoTitleGap = 88.dp
    val blockLogoTopOffset = 16.dp

    /** 12 entre le titre et le texte ; titre de 300 au plus, texte de 290 au plus, centrés. */
    val blockTitleBodyGap = 12.dp
    val blockTitleMaxWidth = 300.dp
    val blockBodyMaxWidth = 290.dp

    /** 6 entre « Fermer » et « Ouvrir Niumi ». */
    val blockButtonGap = 6.dp

    // Feuille « Sonnerie » (étape 40), relevées dans les sources des maquettes 16 et 17.

    /** Rangée d'un son : 48 de haut, séparateur au-dessus. */
    val ringtoneRowHeight = 48.dp

    /** Radio d'un son : cercle de 22, trait de 1,5 ; point intérieur de 12 une fois choisi. */
    val ringtoneRadio = 22.dp
    val ringtoneRadioStroke = 1.5.dp
    val ringtoneRadioDot = 12.dp

    /** Bouton rond de pré-écoute : cercle de 32, icône de 14, retrait de -6 pour aligner sur la marge. */
    val ringtonePreviewCircle = 32.dp
    val ringtonePreviewIcon = 14.dp
    val ringtonePreviewInset = (-6).dp

    /** Étiquette de section (« Son », « Sonnerie progressive ») : 8 au-dessus de « Son », sous l'en-tête. */
    val ringtoneSoundSectionTop = 8.dp

    /** 8 sous chaque étiquette de section, avant son groupe. */
    val ringtoneSectionLabelBottom = 8.dp

    /**
     * Ligne « Sonnerie progressive » (maquettes 18 et 20, révision du 2026-10-23) : 16 au-dessus,
     * 48 de haut au moins, sous-ligne « Volume max en… » à 2 du libellé, barre segmentée à 12 dessous.
     */
    val ringtoneRampRowTop = 16.dp
    val ringtoneRampRowMinHeight = 48.dp
    val ringtoneRampSubtitleGap = 2.dp
    val ringtoneBarTop = 12.dp

    /** 8 au-dessus d'un message d'échec, sous la ligne du volume. */
    val ringtoneMessageTop = 8.dp

    /** Ligne du volume, placée avant la montée progressive depuis le 2026-10-24 : 16 au-dessus, 48 de haut au moins. */
    val ringtoneVolumeRowTop = 16.dp
    val ringtoneVolumeRowMinHeight = 48.dp

    /** Bulle du volume : 204 de large, 11 et 14 de marges intérieures, coins de 12, pointe de 12. */
    val bubbleWidth = 204.dp
    val bubblePaddingVertical = 11.dp
    val bubblePaddingHorizontal = 14.dp
    val bubbleTailSize = 12.dp
    val bubbleTailInsetFromEnd = 22.dp
    val bubbleGap = 8.dp
    val bubbleElevation = 8.dp

    // Accueil (écran 6, étape 45 ; maquettes 15 à 20, VALEURS-DE-DESIGN.md §5).

    /** Zone du cadran : 366 de large, qui déborde de 12 sur les marges de l'écran (maquettes). */
    val dialWidth = 366.dp
    val dialOverflow = 12.dp

    /** Rayon de l'anneau et épaisseur du trait (maquettes). */
    val dialRadius = 112.dp
    val dialStroke = 28.dp

    /**
     * Bande réservée aux étiquettes au-dessus et au-dessous de l'anneau, et écart minimal entre une
     * étiquette et l'anneau (décisions de Mehdi du 2026-10-25, après essais : les 32 et 6 de la
     * maquette ne laissaient pas de place à une poignée tout en haut ou tout en bas ; 44 et 6 étaient
     * trop larges). Une étiquette mesure environ 39 (13 et 17 en IBM Plex Sans) ; plus grande, avec
     * une police agrandie, elle glisse sur le côté (`DialLabelLayout`).
     */
    val dialLabelBand = 40.dp
    val dialLabelGap = 4.dp

    /** Hauteur de la zone (340, contre 290 dans les maquettes) et centre de l'anneau (170, contre 158). */
    val dialHeight = (dialRadius + dialStroke / 2 + dialLabelGap + dialLabelBand) * 2
    val dialCenterY = dialHeight / 2

    /** Poignées (rayon 10) et point de l'heure actuelle (rayon 4). */
    val dialHandleRadius = 10.dp
    val dialNowDotRadius = 4.dp

    /**
     * Flèche vers le point de l'heure actuelle, à l'intérieur de l'anneau (décision de Mehdi du
     * 2026-10-25 après essai, absente des maquettes) : triangle de 8 de long et 8 de base, pointe à
     * 4 du bord intérieur de l'anneau.
     */
    val dialNowArrowLength = 8.dp
    val dialNowArrowWidth = 8.dp
    val dialNowArrowGap = 4.dp

    /** Distance au centre du cadran à laquelle un toucher saisit une poignée, plus large qu'elle. */
    val dialHandleGrabRadius = 24.dp

    /** Centre : libellé (« Réveil dans ») au-dessus de la valeur (« 9 h 26 »). */
    val dialCenterLabelGap = 2.dp

    /**
     * Haut d'écran → cadran → carte d'état → onglets → pastilles → lignes. Réglés après essai sur
     * appareil (décisions de Mehdi du 2026-10-25) : le cadran, élément principal, descend de 32 sous la
     * marge d'écran (collé à la barre d'état sinon) ; les autres espaces, 18, 30, 14 et 34 dans les
     * maquettes 16 et 18, sont resserrés pour « réduire le vide ».
     */
    val homeDialTop = 32.dp
    val homeCardTop = 12.dp
    val homeTabsTop = 8.dp
    val homeDaysTop = 0.dp
    val homeRowsTop = 20.dp

    /** Ligne « Applications bloquées » et ligne « Sonnerie » : 86 et 78 dans les maquettes, resserrées de même. */
    val homeAppsRowHeight = 76.dp
    val homeRingtoneRowHeight = 64.dp
    val homeAppsIconsGap = 8.dp
    val homeAppIconGap = 6.dp
    val homeRowTrailingGap = 6.dp

    /** Cadenas de la carte « Blocage en cours ». */
    val homeLockIcon = 18.dp

    /** Lien « Annuler le réveil » de la carte en session : 44 de haut, texte centré. */
    val homeLinkPadding = 10.dp

    /**
     * Onglets texte (« Ponctuel / Quotidien », « Coucher / Réveil ») : 32 de haut, trait de 2 ; 16
     * d'écart (28 dans les maquettes, resserrés le 2026-10-25 après essai).
     */
    val tabHeight = 32.dp
    val tabGap = 16.dp
    val tabUnderline = 2.dp

    /**
     * Barre d'onglets du bas (maquette 16 du 2026-10-25, décision de Mehdi) : 100 de haut dans la
     * maquette, dont les 34 de la barre d'accueil de l'iPhone. Sur Android, 66 au-dessus de la barre
     * système, que son fond couvre. Quatre onglets de 90, centrés : icône de 28, libellé 2 dessous,
     * badge « Bientôt » de 13 de haut en haut à droite de l'icône (en travers, centré, horizontal),
     * 6 de marge sur les côtés, liseré de 1,5 (demande de Mehdi du 2026-10-25 : sous le libellé, il
     * faisait remonter l'onglet). L'ombre de la
     * maquette (4 vers le haut, flou 16, noir à 8 %) est un dégradé de 12 au-dessus de la barre ; le
     * fond est translucide sur ses 10 premiers, jusqu'au haut des icônes (icône, écart et libellé, 46
     * environ, centrés dans 66 ; demandes de Mehdi du 2026-10-25 : 24 puis 16 descendaient trop).
     */
    val tabBarHeight = 66.dp
    val tabBarItemWidth = 90.dp
    val tabBarIcon = 28.dp
    val tabBarLabelGap = 2.dp
    val tabBarBadgeHeight = 13.dp
    val tabBarBadgePadding = 6.dp
    val tabBarBadgeOutline = 1.5.dp
    val tabBarShadowHeight = 12.dp
    val tabBarTranslucentHeight = 10.dp

    /** Tremblement d'un onglet « Bientôt » touché : 3 au plus de chaque côté (décision du 2026-10-25). */
    val tabBarShake = 3.dp

    /** Sélecteur d'heure (maquettes 19, 20) : fenêtre à 20 des bords, marges 20 / 24 / 24. */
    val timePickerHorizontalMargin = 20.dp
    val timePickerPadding = PaddingValues(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 24.dp)
    val timePickerWheelsTop = 16.dp
    val timePickerSentenceTop = 14.dp
    val timePickerButtonTop = 16.dp

    /** Roue : cinq rangées visibles, rangée choisie de 56 entre deux traits de 1,5, colonnes de 90 à 8 d'écart. */
    val wheelRowHeight = 42.dp
    val wheelSelectedHeight = 56.dp
    val wheelLine = 1.5.dp
    val wheelColumnWidth = 90.dp
    val wheelColumnGap = 8.dp

    /** Le « : » des roues remonte légèrement pour se centrer sur les chiffres. */
    val wheelColonLift = (-2).dp

    /** Bulle de message en bas d'écran (forme de la maquette 27) : 16 des bords, marges 14 × 16, coins 16. */
    val toastHorizontalMargin = 16.dp
    val toastPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    val toastElevation = 10.dp
}
