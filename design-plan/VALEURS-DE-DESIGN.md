# Niumi · Valeurs de design

Ces valeurs sont relevées dans les 58 maquettes validées (dossier `maquettes/`). Elles existent aussi dans `valeurs-de-design.json`, plus facile à lire par un programme.

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
| Encre | `#0E0E10` | Texte principal, bouton principal, carte « Réveil activé », arc du cadran, jours actifs, bandeau de problème |
| Texte secondaire | `#5C5C60` | Sous-titres, phrases d'aide, liens discrets |
| Bordure | `#85858A` | Cartes au trait, cases de pièce, jours non cochés, pastilles de durée non choisies |
| Séparateur | `#DCDCD8` | Lignes entre les rangées des listes |
| Piste | `#E8E8E4` | Piste du cadran, bouton « Annuler » de la feuille de scan, bouton inactif de l'écran 2 |
| Désactivé | `#C6C6C2` | Éléments grisés de l'accueil « Réveil désactivé », bouton inactif de l'écran 4 |
| Tuile grise | `#EFEFEB` | Tuiles d'autorisation, rond de l'icône de scan |
| Case grisée | `#F1F1EE` | Emplacements des icônes d'applications quand l'accueil est grisé (« Réveil désactivé ») |
| Ambre | `#E9A23B` | Interrupteur activé, poignée du réveil sur le cadran, point des Niumi Points, pastille du cadenas (écran 1), soleil (écran 11) |
| Teinte ambre | `#FCF1DF` | Fond de la phrase « Bravo ! » (écran 11) |
| Texte sur encre | `#FFFFFF` sur le bouton principal, `#FAFAF8` sur la carte noire | L'écart entre les deux ne se voit pas à l'œil |
| Texte secondaire sur encre | `#A9A9AE` | Deuxième ligne de la carte « Réveil activé » |
| Voile | noir à 45 % | Derrière une feuille ou une alerte |

Dégradé du soleil de l'écran 11, de haut en bas : `#E9A23B` à 0 %, `#EFB65F` à 26 %, `#F8DDB0` à 58 % (la ligne d'horizon). C'est le seul dégradé des écrans clairs.

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
| Fond désactivé | `#4A4A50` | Jours grisés pendant la session |
| Bord de menu | `#3A3A40` | Menu « trois points » |
| Bouton gris | `#34343A` | Bouton « Annuler » de la feuille de scan |
| Ambre | `#E9A23B` | Partie parcourue du cadran, cadenas, point des Niumi Points, soulignement du mot signalé |
| Teinte du mot signalé | `#3D2C0E` | Fond du mot fautif dans la recopie |
| Texte sur bouton principal | `#0E0E10` | Texte noir sur le bouton blanc cassé |
| Voile | noir à 60 % | Derrière une feuille |

### Règles d'emploi de l'ambre

- Sur fond clair, l'ambre seul a un contraste d'environ 2:1. Il ne porte donc jamais de texte, et il est toujours posé sur l'encre ou réservé à un dessin. Cette règle a été proposée par Claude et n'a pas été validée mot pour mot (voir `POINTS-OUVERTS.md`).
- Sur fond sombre, l'ambre atteint 8:1 à 9:1.
- Un détail ambre par dessin : pastille du cadenas (écran 1, panneau 1), point du Niumi Point (tous les dessins), soleil (écran 11).
- Les icônes restent noires en clair et blanc cassé en sombre. Pas d'ambre dans les icônes.

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
| 40 | 600 | Titre de l'écran 11 (« Debout à 7:04. »), valeur du milieu de la roue (« 15 min »). Interlettrage −1 |
| 30 | 600 | Consigne de l'écran de réveil (« Scanne ton Niumi Point », « Ton alarme va reprendre. ») |
| 30 | 500 | « Désactivé » au centre du cadran |
| 28 | 600 | Titres d'écran : écrans 1, 3 bis et 4, « Réglages », « Aide », titres des pages d'aide. Valeur de la carte « Temps gagné » |
| 26 | 600 | Titre de l'écran 2 (« Trois autorisations »). C'est le seul titre d'écran à 26 au lieu de 28 : écart non tranché |
| 26 | 400 | « Prêt à scanner », dans la feuille de scan |
| 24 | 600 | Titres des feuilles et des échecs, titre de l'écran de blocage |
| 24 | 400 | Valeurs voisines du milieu dans la roue |
| 22 | 600 | Titre de la feuille « Sonnerie », titre de l'alerte d'autorisations |
| 22 | 500 | Consigne de l'écran 3 |
| 20 | 600 | Titres de carte et de liste (« Aucune application choisie », « Applications à bloquer ») |
| 20 | 500 | Sous-titre de l'écran 11, phrase du guide des autorisations |
| 20 | 400 | Valeurs éloignées dans la roue |
| 18 | 600 | Titre du guide (« Autoriser l'alarme ») |
| 17 | 600 | Heures autour du cadran (« 22:30 »), consigne de la recopie, intertitres de l'aide |
| 17 | 500 | Texte des boutons principaux |
| 17 | 400 | Texte sous les titres de l'écran 1, phrase de la feuille de scan |
| 16 | 600 | Noms des autorisations, « Blocage en cours », son choisi, « 80 % » |
| 16 | 500 | « Réveil activé », « Réveil désactivé », jours cochés, pilule « Modifier », bouton de la recopie |
| 16 | 400 | Texte courant, noms des pièces. Interligne 1,35 |
| 15 | 500 | Bouton « Autoriser », pastille de durée choisie |
| 15 | 400 | Pastilles de durée, lien « Annuler la session », texte d'introduction de l'écran 2 |
| 14 | 600 | Liens du bandeau de problème |
| 14 | 400 | Phrases des tuiles, deuxième ligne des cartes, liens discrets |
| 13 | 500 | Message d'erreur de la recopie |
| 13 | 400 | Légendes (« Coucher », « Progressive · 2 min »), intitulés de section, mentions de bas d'écran |
| 12 | 400 | Noms sous les icônes d'applications. En 600 quand l'application est cochée |

Les valeurs exactes de chaque écran sont dans les fichiers de `maquettes/sources/`.

## 4. Formes et dimensions

| Élément | Valeur |
| --- | --- |
| Marges de l'écran | 24 à gauche et à droite, 34 en bas, 56 en haut (sous la barre d'état) |
| Zone de toucher minimale | 44 de haut |
| Bouton principal | 56 de haut, coins arrondis de 14, pleine largeur |
| Bouton d'une alerte ou de la feuille de scan | 52 de haut, coins de 14 |
| Bouton de la recopie (au-dessus du clavier) | 48 de haut, coins de 14 |
| Bouton « Autoriser » d'une tuile | 46 de haut, coins de 12 |
| Bouton en pilule | 44 de haut (« Modifier », durées) ou 48 (« Choisir les applications »), coins égaux à la moitié de la hauteur |
| Pastille de jour | Cercle de 44 |
| Carte | Coins de 14 |
| Tuile d'autorisation, case de pièce | Coins de 16 |
| Feuille du bas | Coins de 28 en haut, pleine largeur, bord à bord |
| Emplacement d'icône d'application | 28 × 28, coins de 7 (rangée de l'accueil) |
| Interrupteur | 52 × 32, pastille de 26 |
| Rangée de liste | Séparateur d'un pixel, 78 à 86 de haut sur l'accueil |
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
| « Annuler » de la feuille de scan | Fond `#E8E8E4` | Fond `#34343A` |
| Inactif | Voir la remarque ci-dessous | Fond `#2A2A2F`, texte `#74747B` |

**Remarque : deux boutons inactifs différents en clair.** Les maquettes validées en montrent deux : fond `#E8E8E4` avec texte `#5C5C60` sur l'écran 2, et fond `#C6C6C2` avec texte `#FFFFFF` sur l'écran 4. L'écart n'a pas été tranché. Voir `POINTS-OUVERTS.md`.

## 5. Le cadran de l'accueil

Cadran de 24 heures. Dans les maquettes : centre (183, 158) dans une zone de 366 × 290, rayon 112, trait de 28, bouts ronds. Une heure vaut 15 degrés. Minuit est en haut.

| État | Piste | Arc de la nuit | Repères |
| --- | --- | --- | --- |
| Réveil prévu (clair) | `#E8E8E4` | Encre `#0E0E10`, du coucher au réveil | Poignée du coucher blanche, poignée du réveil ambre, petit point gris pour l'heure actuelle |
| Réveil désactivé (clair) | `#E8E8E4` | Gris `#C6C6C2` | Deux poignées blanches, tout est grisé |
| Session en cours (sombre) | `#2A2A2F` | Blanc cassé `#FAFAF8` | Pas de poignées. Voir ci-dessous |

Pendant la session, l'arc se remplit d'ambre au fil de la nuit :

- L'ambre est plein du coucher jusqu'au point de l'heure actuelle.
- Devant le point, l'ambre se fond dans le blanc cassé sur 34 degrés, avec une courbe douce (demi-cosinus).
- Le point de l'heure actuelle est noir `#0E0E10`, rayon 4. Son contraste sur l'ambre est de 8,9:1.
- En fin de nuit, l'arc est presque entièrement ambre. C'est voulu.
- Le cadenas de la carte « Blocage en cours » est ambre, au trait épais.

Dans les maquettes, ce fondu est simulé par une suite de petits segments de 1,5 degré. C'est une astuce de dessin : dans l'app, il faut le refaire avec un vrai dégradé le long de l'arc.

## 6. Contrastes

Repères utilisés (W3C, WCAG 2.2) : 4,5:1 au moins pour un texte, 3:1 pour un grand texte et pour les éléments graphiques nécessaires à la compréhension. Les éléments inactifs en sont dispensés.

- [Understanding SC 1.4.3, Contrast (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)
- [Understanding SC 1.4.11, Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
