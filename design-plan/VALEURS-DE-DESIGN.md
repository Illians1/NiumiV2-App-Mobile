# Niumi · Valeurs de design

Ces valeurs sont relevées dans les 58 maquettes validées (dossier `maquettes/`), complétées le 25 octobre 2026 par celles des maquettes 18 à 20 (onglets de l'accueil, sélecteur d'heure ; 64 maquettes depuis, `maquettes/INDEX.md` fait foi). Elles existent aussi dans `valeurs-de-design.json`, plus facile à lire par un programme.

Les maquettes sont dessinées pour un écran de 390 × 844 points. Les tailles ci-dessous sont dans cette unité : des points sur iPhone, des dp sur Android.

## 1. La règle clair ou sombre

L'app ne suit pas le réglage clair ou sombre du téléphone. Elle suit son propre état.

| État | Thème | Écrans concernés |
| --- | --- | --- |
| Aucune session en cours | Clair | Mise en route, accueil avant le coucher, sonnerie, réglages, réussite, accueil après une annulation |
| Session en cours, du coucher jusqu'au scan | Sombre | Accueil en session, annulation, écran de blocage, sortie de secours, réveil et ses échecs, réglages ouverts pendant la session |

Le passage au clair se fait au moment où la session se termine : écran de réussite après le scan du matin, accueil après une annulation.

## 2. Couleurs

### Thème clair

| Rôle | Valeur | Où |
| --- | --- | --- |
| Fond d'écran | `#FAFAF8` | Tous les écrans clairs |
| Surface | `#FFFFFF` | Feuilles du bas, cartes, fenêtre d'alerte, menus |
| Encre | `#0E0E10` | Texte principal, bouton principal, carte « Réveil activé », arc du cadran, jours actifs, bandeau de problème, onglet choisi et son trait, traits de la valeur choisie des roues, bulle de message en bas d'écran (25 octobre 2026) |
| Texte secondaire | `#5C5C60` | Sous-titres, phrases d'aide, liens discrets, onglets non choisis, phrase du sélecteur d'heure |
| Bordure | `#85858A` | Cartes au trait, cases de pièce, jours non cochés, pastilles de durée non choisies |
| Séparateur | `#DCDCD8` | Lignes entre les rangées des listes |
| Piste | `#E8E8E4` | Piste du cadran, bouton « Annuler » de la feuille de scan, bouton inactif (tous les écrans, D9) |
| Désactivé | `#C6C6C2` | Éléments grisés de l'accueil « Réveil désactivé », dont le bord des jours non cochés (maquette 15) ; poignée des feuilles du bas ; traits inactifs de l'indicateur de panneau (le bouton inactif de l'écran 4 de la maquette n'est pas repris, D9) |
| Tuile grise | `#EFEFEB` | Tuiles d'autorisation, rond de l'icône de scan, fond de la barre segmentée (23 octobre 2026) |
| Case grisée | `#F1F1EE` | Emplacements des icônes d'applications quand l'accueil est grisé (« Réveil désactivé ») |
| Ambre | `#E9A23B` | Interrupteur activé, poignée du réveil sur le cadran, point des Niumi Spots, pastille du cadenas (écran 1), pastille de la coche d'une autorisation accordée (écran 2), soleil (écran 11) |
| Teinte ambre | `#FCF1DF` | Fond de la phrase « Bravo ! » (écran 11) |
| Texte sur encre | `#FFFFFF` sur le bouton principal, `#FAFAF8` sur la carte noire | L'écart entre les deux ne se voit pas à l'œil |
| Texte secondaire sur encre | `#A9A9AE` | Deuxième ligne de la carte « Réveil activé » |
| Voile | noir à 45 % | Derrière une feuille ou une alerte |

Dégradé du soleil de l'écran 11, de haut en bas : `#E9A23B` à 0 %, `#EFB65F` à 26 %, `#F8DDB0` à 58 % (la ligne d'horizon). C'est le seul dégradé de couleur des écrans clairs. Une exception d'usage (décision de Mehdi du 22 octobre 2026) : quand du contenu est caché sous le bas d'une zone qui défile, ce bas s'estompe dans la couleur du fond, sur 32 points, pour montrer qu'il reste à faire défiler. Le fondu s'efface en arrivant au bout et n'existe pas quand tout tient à l'écran. Il est d'abord utilisé sur l'écran 2, au-dessus de « Continuer ».

### Thème sombre

| Rôle | Valeur | Où |
| --- | --- | --- |
| Fond d'écran | `#0E0E10` | Tous les écrans sombres |
| Surface | `#1B1B1F` | Feuilles du bas, cartes, menus |
| Liseré de feuille, séparateur | `#2E2E33` | Contour d'un pixel des feuilles du bas, lignes entre les rangées |
| Texte principal | `#FAFAF8` | Texte, bouton principal, arc du cadran |
| Texte secondaire | `#A9A9AE` | Sous-titres, phrases d'aide, liens discrets |
| Bordure, texte désactivé | `#74747B` | Bord d'un pixel des cartes et des cases, libellés grisés pendant la session |
| Piste | `#2A2A2F` | Piste du cadran, fond du bouton inactif, rond de l'icône de scan |
| Fond désactivé | `#4A4A50` | Jours grisés pendant la session, poignée des feuilles du bas |
| Bord de menu | `#3A3A40` | Menu « trois points », bord des jours non cochés grisés pendant la session (maquette 17) |
| Bouton gris | `#34343A` | Bouton « Annuler » de la feuille de scan |
| Ambre | `#E9A23B` | Partie parcourue du cadran, cadenas, point des Niumi Spots, soulignement du mot signalé |
| Teinte du mot signalé | `#3D2C0E` | Fond du mot fautif dans la recopie |
| Texte sur bouton principal | `#0E0E10` | Texte noir sur le bouton blanc cassé |
| Voile | noir à 60 % | Derrière une feuille |

### Règles d'emploi de l'ambre

- Sur fond clair, l'ambre seul a un contraste d'environ 2:1. Il ne porte donc jamais de texte, et il est toujours posé sur l'encre ou réservé à un dessin. Règle validée par Mehdi le 19 octobre 2026 (`POINTS-OUVERTS.md`, D14).
- Sur fond sombre, l'ambre atteint 8:1 à 9:1.
- Un détail ambre par dessin : pastille du cadenas (écran 1, panneau 1), point du Niumi Spot (tous les dessins), soleil (écran 11).
- Les icônes de navigation et d'action (chevron, flèche de retour, croix, réglages, coche, trois points, cadenas des rangées) restent noires en clair et blanc cassé en sombre, sans ambre.
- Deux icônes du design font exception, et elles restent telles quelles (décision de Mehdi du 20 octobre 2026) : le cadenas de la carte « Blocage en cours », ambre au trait épais sur fond sombre (§5, images 17, 24 à 26 et 29), et le point ambre de l'icône du Niumi Spot (images 53 à 60), qui est le détail ambre de ce dessin. Aucune autre icône ne prend l'ambre sans décision écrite ici.
- Troisième exception (décision de Mehdi du 22 octobre 2026) : sur l'écran 2, la coche d'une autorisation accordée est à l'encre dans une pastille ambre de 28 (coche de 16). L'ambre y est posé sous l'encre, comme le cadenas de l'écran 1 ; la coche garde un contraste de 8,9:1. Les icônes des tuiles restent à l'encre (l'essai ambre du 20 octobre est abandonné le même jour).

### Couleurs du logo

Mesurées dans les fichiers du logo : ambre `#E9A23B`, noir `#0B0B0B` sur fond blanc ; sur fond noir, fond `#0E0E10` et lettres `#FAFAF8`.

## 3. Police

**IBM Plex Sans**, validée le 9 octobre 2026. Trois graisses : normal (400), moyen (500), demi-gras (600). Pas d'italique.

Les chiffres d'IBM Plex Sans ont tous la même largeur sans réglage, ce qui évite que le compte à rebours bouge quand un chiffre change.

La police est à embarquer dans l'app sur les deux téléphones, pour qu'elle soit la même partout. Voir `police/LISEZ-MOI.md` pour la source des fichiers et ce que demande la licence.

### Tailles relevées

Relevé fait par programme dans les 58 fichiers sources. Les exemples disent où chaque taille sert.

| Taille | Graisse | Où |
| --- | --- | --- |
| 112 | 500 | Heure de l'écran de réveil. Interligne 1, interlettrage −4 |
| 42 | 500 | Chiffres au centre du cadran (« 9 h 26 »). Interlettrage −1 |
| 40 | 600 | Titre de l'écran 11 (« Debout à 7:04 »), valeur du milieu de la roue de l'écran 3 bis (« 15 min »). Interlettrage −1 |
| 34 | 600 | Valeur choisie des roues du sélecteur d'heure (« 22 », « 30 »), interlettrage −1 ; le « : » qui sépare les roues, sans interlettrage (maquettes 19 et 20, 25 octobre 2026) |
| 30 | 600 | Consigne de l'écran de réveil (« Scanne ton Niumi Spot », « Ton alarme va reprendre. ») |
| 30 | 500 | « Désactivé » au centre du cadran. Interlettrage −0,5 |
| 28 | 600 | Titres d'écran : écrans 1, 3 bis et 4, « Réglages », « Aide », titres des pages d'aide. Valeur de la carte « Temps gagné » |
| 26 | 600 | Titre de l'écran 2 (« Trois autorisations ») dans la maquette. Tranché le 19 octobre 2026 (D19) : l'app l'affiche en 28, comme les autres titres d'écran |
| 26 | 400 | « Prêt à scanner », dans la feuille de scan, dans les maquettes. Réduit à 22 dans l'app (décision de Mehdi du 26 octobre 2026, `POINTS-OUVERTS.md` M19) |
| 24 | 600 | Titre de l'écran de blocage. Titres des feuilles et des échecs dans les maquettes ; 22 dans l'app (même décision) |
| 24 | 400 | Valeurs voisines du milieu dans la roue de l'écran 3 bis |
| 22 | 600 | Titre de la feuille « Sonnerie », titre de l'alerte d'autorisations ; dans l'app, titres de toutes les feuilles du bas et des échecs du scan (M19) |
| 22 | 500 | Consigne de l'écran 3 |
| 22 | 400 | Valeurs voisines de la valeur choisie dans les roues du sélecteur d'heure, en `#85858A` |
| 20 | 600 | Titres de carte et de liste (« Aucune application choisie », « Applications à bloquer ») |
| 20 | 500 | Sous-titre de l'écran 11, phrase du guide des autorisations |
| 20 | 400 | Valeurs éloignées dans la roue de l'écran 3 bis |
| 18 | 600 | Titre du guide (« Autoriser l'alarme ») |
| 18 | 400 | Valeurs éloignées dans les roues du sélecteur d'heure, en `#C6C6C2` |
| 17 | 600 | Heures autour du cadran (« 22:30 »), consigne de la recopie, intertitres de l'aide |
| 17 | 500 | Texte des boutons principaux |
| 17 | 400 | Texte sous les titres de l'écran 1, phrase de la feuille de scan |
| 16 | 600 | Noms des autorisations, « Blocage en cours », son choisi, « 80 % » |
| 16 | 500 | « Réveil activé », « Réveil désactivé », jours cochés, pilule « Modifier », bouton de la recopie |
| 16 | 400 | Texte courant, noms des pièces. Interligne 1,35 |
| 15 | 500 | Bouton « Autoriser », segment de durée choisi, onglet choisi du sélecteur d'heure (« Coucher », « Réveil ») |
| 15 | 400 | Segments de durée, lien « Annuler le réveil », texte d'introduction de l'écran 2, onglet non choisi du sélecteur d'heure |
| 14 | 600 | Liens du bandeau de problème |
| 14 | 500 | Onglet choisi de l'accueil (« Ponctuel » ou « Quotidien », maquette 18) |
| 14 | 400 | Phrases des tuiles, deuxième ligne des cartes, liens discrets, onglets non choisis de l'accueil, phrase du sélecteur d'heure (« 8 h 30 de sommeil avant le réveil de 7:00 ») |
| 13 | 500 | Message d'erreur de la recopie |
| 13 | 400 | Légendes (« Coucher », « Progressive · 2 min »), intitulés de section, mentions de bas d'écran |
| 12 | 400 | Noms sous les icônes d'applications. En 600 quand l'application est cochée |
| 12 | 500 | Libellés des onglets de la barre du bas. En 600 pour l'onglet choisi (maquette 16, 25 octobre 2026) |
| 9 | 600 | Badge « Bientôt » sous un onglet de la barre du bas (maquette 16, 25 octobre 2026) |

Les valeurs exactes de chaque écran sont dans les fichiers de `maquettes/sources/`.

## 4. Formes et dimensions

| Élément | Valeur |
| --- | --- |
| Marges de l'écran | 24 à gauche et à droite, 34 en bas, 56 en haut. Sur la maquette iPhone, les 56 du haut couvrent la barre d'état et les 34 du bas la barre d'accueil. Sur Android, sur les côtés et en haut, la plus grande de la marge et de la zone système (barre d'état, encoche), jamais leur somme (décision de Mehdi du 20 octobre 2026). En bas, la barre de navigation (ou le clavier) plus 16, sans descendre sous les 34 : 64 au-dessus de la barre à trois boutons, 40 au-dessus des gestes (décision de Mehdi du 22 octobre 2026, qui remplace « la plus grande des deux » en bas : le dernier bouton touchait la barre à trois boutons). Même règle pour les côtés et le bas d'une feuille du bas |
| Zone de toucher minimale | 44 de haut. Sur Android, 48 dp de zone tactile sont réservés autour de tout élément touchable plus petit, sans changer son dessin : c'est la recommandation de Google (décision de Mehdi du 20 octobre 2026) |
| Bouton principal | 56 de haut, coins arrondis de 14, pleine largeur |
| Bouton d'une alerte ou de la feuille de scan | 52 de haut, coins de 14 |
| Bouton de la recopie (au-dessus du clavier) | 48 de haut, coins de 14 |
| Bouton « Autoriser » d'une tuile | 46 de haut, coins de 12 |
| Bouton en pilule | 44 de haut (« Modifier ») ou 48 (« Choisir les applications »), coins égaux à la moitié de la hauteur. Les durées de la sonnerie progressive, en pilules jusqu'au 23 octobre 2026, sont passées en barre segmentée |
| Barre segmentée | 48 de haut, coins de 14, marge intérieure de 4, fond « Tuile grise » ; segments de largeur égale, 40 de haut, coins de 10, le choisi en encre (durées de la sonnerie progressive, maquette 21, 23 octobre 2026) |
| Interrupteur de ligne | 44 × 26, pastille de 20 (« Sonnerie progressive », maquette 21, 23 octobre 2026) |
| Pastille de jour | Cercle de 44 |
| Onglets texte | 32 de haut, 28 d'écart, centrés ; l'onglet choisi souligné d'un trait de 2 à l'encre, les autres d'un trait transparent. Accueil (« Ponctuel », « Quotidien ») : 30 sous la carte, pastilles 14 dessous. Sélecteur d'heure (« Coucher », « Réveil ») : remontés de 24 contre la croix (maquettes 18 à 20, 25 octobre 2026). Dans l'app, 16 d'écart au lieu de 28, à l'accueil comme au sélecteur ; à l'accueil, 8 sous la carte et pastilles juste dessous (ligne « Espaces de l'accueil ») (décision de Mehdi du 2026-10-25, après essai sur appareil). Depuis le 26 octobre 2026, l'accueil n'utilise plus d'onglets texte : voir « Onglets segmentés » |
| Onglets segmentés | Accueil, « Ponctuel » et « Quotidien », à droite du titre « Jours de réveil » (16/400, encre, à gauche), centrés verticalement avec lui. Capsule « Tuile grise » de 32 de haut, coins de 10, marge de 3, de la largeur de ses libellés. Segment de 26, coins de 7, texte 14 à 10 de ses bords : choisi, fond encre et texte 14/500 « Texte sur encre » ; sinon texte secondaire 14/400. Grisé ou verrouillé : choisi sur fond « Désactivé », autres en « Bordure », comme les jours. Zone de toucher de 48 de haut par segment. Forme demandée par Mehdi le 26 octobre 2026 sur un croquis, non dessinée ; valeurs relevées sur le croquis et segment choisi à l'encre par Claude (`POINTS-OUVERTS.md` M20) |
| Espaces de l'accueil | Maquettes 16 et 18 : zone du cadran à la marge du haut de l'écran, cadran → carte 18, carte → onglets 30, onglets → pastilles 14, pastilles → première ligne 34. Dans l'app : zone du cadran 32 sous la marge du haut (l'étiquette du haut touchait presque la barre d'état), puis 12, 8, 0 et 20 ; l'écran défile sur les petits écrans (décision de Mehdi du 2026-10-25, après essai sur appareil : « réduire le vide ») |
| Sélecteur d'heure | Fenêtre centrée verticalement, à 20 des bords gauche et droit, coins de 24, fond « Surface », marges intérieures de 20 en haut et de 24 sur les côtés et en bas, ombre de la fenêtre d'alerte. Croix ronde de 36 dans une zone de 44. Roues 16 sous les onglets : deux colonnes de 90 de large à 8 d'écart, le « : » entre elles ; cinq rangées visibles (36, 40, 56, 40, 36 dans les maquettes), la rangée choisie de 56 entre deux traits de 1,5 à l'encre. Phrase 14 sous les roues, bouton « Valider » de 52 (coins de 14) 16 dessous (maquettes 19 et 20, 25 octobre 2026). Sur Android, les cinq rangées font 42 chacune : écart à la maquette, à valider par Mehdi (`POINTS-OUVERTS.md` §4) |
| Bulle de message en bas d'écran | À 16 des bords gauche et droit, 34 au-dessus du bas, marges intérieures de 14 × 16, coins de 16, fond encre, texte sur encre, ombre décalée de 10 vers le bas, flou 30, noir à 25 % (maquette 27 ; reprise sans la coche pour M10, 25 octobre 2026) |
| Barre d'onglets | En bas de l'accueil et de la première page des réglages, bord à bord (maquette 16, décision de Mehdi du 25 octobre 2026). 100 de haut dans la maquette, dont les 34 de la barre d'accueil de l'iPhone : 66 de contenu. Sur Android, 66 au-dessus de la barre système, que son fond couvre ; elle remplace la marge du bas de l'écran. Posée sur le contenu, qui défile sous elle. Fond « Surface », translucide en haut : de 72 % d'opacité au plein sur 10, jusqu'au haut des icônes, puis plein (demandes de Mehdi du 25 octobre 2026 : 24 puis 16 descendaient trop ; opacité choisie par Claude) ; seul le fond est translucide. Ombre décalée de 4 vers le haut, flou 16, noir à 8 %, dessinée en dégradé de 12 au-dessus de la barre. Quatre onglets de 90 de large, centrés, chacun centré verticalement : icône de 28 (trait de 1,7), libellé 12 à 2 dessous. Onglet choisi : encre, 12/600, icône pleine : l'horloge de « Réveil » a ses aiguilles en « Texte sur encre », l'engrenage de « Réglages » est plein, sans trait, percé d'un rond de 3 au centre (demande de Mehdi du 25 octobre 2026, forme choisie par Claude). Onglet disponible non choisi : « Bordure », 12/500. Onglet « Bientôt » : « Désactivé », 12/500, badge centré en travers de l'icône, à l'horizontale, avec un liseré « Surface » de 1,5 : il ne compte pas dans la hauteur de l'onglet, et les icônes et libellés des quatre onglets restent alignés (demande de Mehdi du 25 octobre 2026 ; sous le libellé, dans la maquette d'origine, il faisait remonter l'onglet). Badge de 13 de haut, 6 de marge sur les côtés, en pilule, fond « Tuile grise », texte 9/600 « Bordure ». Touché, il tremble à l'horizontale en 200 ms, deux allers-retours amortis de 3 puis 1,5 (raccourci et adouci à la demande de Mehdi, qui trouvait trop visibles trois allers-retours de 6 en 360 ms), avec la vibration « refus » du téléphone (décision de Mehdi du 25 octobre 2026, valeurs choisies par Claude). En sombre, non dessiné par Mehdi : fond `#1B1B1F`, choisi `#FAFAF8` (aiguilles `#0E0E10`), disponible `#74747B`, « Bientôt » `#4A4A50`, badge `#2A2A2F` et `#74747B` (maquettes 17 et 24). « Réglages » non choisi, `#85858A` sur blanc, n'atteint que 3,7:1 (§6, `POINTS-OUVERTS.md` M17) |
| Carte | Coins de 14 |
| Tuile d'autorisation, case de pièce | Coins de 16 |
| Feuille du bas | Coins de 28 en haut, pleine largeur, bord à bord |
| Emplacement d'icône d'application | 28 × 28, coins de 7 (rangée de l'accueil) |
| Interrupteur | 52 × 32, pastille de 26 |
| Rangée de liste | Séparateur d'un pixel, 78 à 86 de haut sur l'accueil (maquettes : « Applications bloquées » 86, « Sonnerie » 78, un trait sous la dernière). Dans l'app : 76 et 64, et plus de trait sous la dernière ligne (« Sonnerie ») (décision de Mehdi du 2026-10-25, après essai sur appareil) |
| Ombre de la fenêtre d'alerte | Décalage 20 vers le bas, flou 50, noir à 25 % |
| Ombre du menu « trois points » | Décalage 10 vers le bas, flou 30, noir à 18 % |
| Ombre de la bulle du volume | Décalage 8 vers le bas, flou 24, noir à 22 % |

Dans les maquettes, les carrés gris qui tiennent la place des icônes d'applications sont des emplacements vides. Dans l'app, ce sont les vraies icônes.

### Les boutons, par rôle

| Rôle | Clair | Sombre |
| --- | --- | --- |
| Principal | Fond `#0E0E10`, texte `#FFFFFF` | Fond `#FAFAF8`, texte `#0E0E10` |
| Secondaire au trait | Bord `#0E0E10` d'un pixel, texte encre | Bord `#FAFAF8` d'un pixel, texte blanc cassé |
| Lien discret | Texte souligné `#5C5C60`, taille 14 | Texte souligné `#A9A9AE`, taille 14 |
| Bouton texte (« Ouvrir Niumi », écran 7) | Texte `#0E0E10`, taille 16, sans soulignement | Texte `#FAFAF8`, taille 16, sans soulignement |
| « Annuler » de la feuille de scan | Fond `#E8E8E4` | Fond `#34343A` |
| Inactif | Fond `#E8E8E4`, texte `#5C5C60` (voir la remarque ci-dessous) | Fond `#2A2A2F`, texte `#74747B` |

**Bouton inactif en clair.** Les maquettes validées en montrent deux : fond `#E8E8E4` avec texte `#5C5C60` sur l'écran 2, et fond `#C6C6C2` avec texte `#FFFFFF` sur l'écran 4. Tranché le 19 octobre 2026 (D9) : le premier, partout.

## 5. Le cadran de l'accueil

Cadran de 24 heures. Dans les maquettes : centre (183, 158) dans une zone de 366 × 290, rayon 112, trait de 28, bouts ronds. Une heure vaut 15 degrés. Minuit est en haut.

Dans l'app (décision de Mehdi du 2026-10-25, après essai sur appareil) : zone de 366 × 340, centre (183, 170). Au-dessus et au-dessous de l'anneau, une bande de 40 est réservée aux étiquettes, plus un écart minimal de 4 entre une étiquette et l'anneau (112 + 14 + 4 + 40 = 170). Raison : avec une poignée tout en haut ou tout en bas, l'étiquette (« Réveil 12:00 », environ 39 de haut) recouvrait l'anneau. La zone commence 32 sous la marge du haut de l'écran (§4, « Espaces de l'accueil »).

| État | Piste | Arc de la nuit | Repères |
| --- | --- | --- | --- |
| Réveil prévu (clair) | `#E8E8E4` | Encre `#0E0E10`, du coucher au réveil | Poignée du coucher blanche, poignée du réveil ambre, petit point gris pour l'heure actuelle (blanc sur l'arc de la nuit, voir ci-dessous) |
| Réveil désactivé (clair) | `#E8E8E4` | Gris `#C6C6C2` | Deux poignées blanches, tout est grisé |
| Session en cours (sombre) | `#2A2A2F` | Blanc cassé `#FAFAF8` | Pas de poignées. Voir ci-dessous |

Pendant la session, l'arc se remplit d'ambre au fil de la nuit :

- L'ambre est plein du coucher jusqu'au point de l'heure actuelle.
- Devant le point, l'ambre se fond dans le blanc cassé sur 34 degrés, avec une courbe douce (demi-cosinus), dans les maquettes. Dans l'app, le fondu dure 12 degrés (48 minutes), en pente droite dès le point : le début du fondu paraissait loin du point (décision de Mehdi du 2026-10-25, après essai sur appareil).
- Le point de l'heure actuelle est noir `#0E0E10`, rayon 4. Son contraste sur l'ambre est de 8,9:1.
- En fin de nuit, l'arc est presque entièrement ambre. C'est voulu.
- Le cadenas de la carte « Blocage en cours » est ambre, au trait épais.

Dans les maquettes, ce fondu est simulé par une suite de petits segments de 1,5 degré. C'est une astuce de dessin : dans l'app, il faut le refaire avec un vrai dégradé le long de l'arc.

### Étiquettes et heure actuelle (décisions de Mehdi du 2026-10-25, après essai sur appareil)

Ces règles sont absentes des maquettes, qui ne montrent que des heures bien écartées et loin du haut et du bas de l'anneau. La flèche et les repères y sont dessinés depuis les maquettes de Mehdi du 2026-10-30.

- **Étiquettes** (« Coucher 22:30 », « Réveil 7:00 ») : jamais sur l'anneau, jamais l'une sur l'autre, toujours dans la zone du cadran. Faute de place, une étiquette glisse le long de l'anneau. Quand les poignées sont proches, les deux étiquettes s'écartent ensemble, celle du coucher en arrière, celle du réveil en avant : elles gardent l'ordre de leurs poignées (le coucher passait parfois devant le réveil). Avec une police agrandie, une étiquette trop haute pour la bande de 40 glisse sur le côté.
- **Flèche de l'heure actuelle** : un petit triangle plein de 8 × 8 aux coins arrondis (cœur de 5 × 5 et contour de 3 à jointures rondes, soit un arrondi de 1,5, maquettes du 2026-10-30), à l'intérieur de l'anneau, pointe vers le point de l'heure actuelle ; sa pointe est à 4 du bord intérieur de l'anneau. Il est à l'encre (`#0E0E10` en clair, `#FAFAF8` en session), en gris `#85858A` en « Réveil désactivé ».
- **Repères** (décision de Mehdi du 2026-10-26, dessinés dans les maquettes depuis le 2026-10-30) : quatre traits de 8 × 2 à l'intérieur de l'anneau, en haut, à droite, en bas et à gauche (0 h, 6 h, 12 h et 18 h), à la même distance de l'anneau que la flèche (4 du bord intérieur). Couleur « Désactivé » (`#C6C6C2` en clair, `#4A4A50` en session), dans les trois états. Un repère s'efface quand la flèche de l'heure actuelle est à moins de 4° de lui (16 minutes), pour qu'elle ne le chevauche jamais.
- **Point de l'heure actuelle hors session** : sur l'arc de la nuit, il est blanc `#FFFFFF`, comme les poignées ; ailleurs, gris `#5C5C60` en « Réveil prévu », `#C6C6C2` en « Réveil désactivé », comme dans les maquettes. Gris sur l'arc gris, il était invisible en « Réveil désactivé ».

### Régler les heures (décisions de Mehdi du 25 octobre 2026)

| Geste | Effet |
| --- | --- |
| Poser le doigt sur une poignée et bouger | La poignée suit le doigt dès le premier mouvement, sans appui long, de 5 en 5 minutes. L'heure est enregistrée au relâchement |
| Relâcher sans avoir bougé | Ouvre le sélecteur d'heure : onglet « Coucher » sur la poignée du coucher, onglet « Réveil » partout ailleurs sur le cadran |

- La période du coucher au réveil dure au moins une minute (au sélecteur, réglé à la minute) et au plus 24 h. Deux heures identiques valent 24 h : l'arc fait alors le tour complet du cadran.
- La poignée glissée ne traverse jamais l'autre : du côté de la nuit, elle s'arrête au dernier cran de 5 minutes avant elle ; du côté du jour, elle peut la rejoindre (24 h).
- Ces règles remplacent l'écart minimal de 15 minutes décidé le 24 octobre 2026 (Q8).

## 6. Contrastes

Repères utilisés (W3C, WCAG 2.2) : 4,5:1 au moins pour un texte, 3:1 pour un grand texte et pour les éléments graphiques nécessaires à la compréhension. Les éléments inactifs en sont dispensés.

- [Understanding SC 1.4.3, Contrast (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)
- [Understanding SC 1.4.11, Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
