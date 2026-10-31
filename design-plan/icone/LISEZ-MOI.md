# Icône de l'app Niumi

Validée par Mehdi le 31 octobre 2026 (essai « 3p » du canevas « Niumi · maquettes simples », page « Icône de l'app »).

## Dessin (grille de 1024)

- Fond : encre `#0E0E10`, plein, sans dégradé.
- Anneau : cercle exact de rayon 300 (sur l'axe du trait), centre (512, 512), trait de 90 en blanc cassé `#FAFAF8`, bouts ronds ; ouvert sur 90°, l'ouverture centrée à 42° (en haut à droite, minuit = 0°, sens horaire) ; l'arc va donc de 87° à 357°.
- Point : disque ambre `#E9A23B` de rayon 101 (2,25 traits, le point rond du logotype), centré à 42° sur un rayon de 336 : il est légèrement sorti de l'anneau (36 au-delà de l'axe du trait). Centre (736,8 ; 262,3).
- Rien d'autre : pas de lueur, pas d'ombre, pas de texte.

Rapports à garder si l'on redessine : trait = 0,088 du côté ; rayon de l'anneau = 3,33 traits ; point = 2,25 traits ; sortie du point = 0,4 trait.

## Fichiers

### `source/`
- `niumi-icone.svg` : le maître, 1024 × 1024, avec le fond.
- `niumi-icone-sans-fond.svg` : le même sans le fond (transparent).

### `ios/`
- `AppIcon-1024.png` : à mettre dans l'App Icon de Xcode (un seul fichier de 1024 suffit depuis Xcode 14, « Single Size »). Sans couche alpha, coins carrés : iOS applique lui-même l'arrondi.
- Les autres tailles (180, 120, 167, 152, 87, 80, 76, 60, 58, 40, 29) si un outil les réclame encore.
- Mode « teinté » d'iOS 18 : le système reprend l'icône en monochrome tout seul ; si un rendu dédié est demandé, prendre `android/ic_launcher_monochrome.xml` comme dessin (anneau et point d'une seule couleur).

### `android/`
Icône adaptative (API 26 et plus) :
- `ic_launcher.xml` → `res/mipmap-anydpi-v26/ic_launcher.xml` (et `ic_launcher_round.xml`, même contenu).
- `ic_launcher_foreground.xml` → `res/drawable/` : premier plan en VectorDrawable, dessin réduit d'un facteur 0,62 : il occupe 53 % de la toile de 108 dp et reste en retrait de la zone sûre de 66 dp (cercle). Réduit le 31 octobre 2026 à la demande de Mehdi, après essai sur le téléphone : au facteur 0,716 d'origine (61 % de la toile, le point ambre au bord de la zone sûre), l'anneau paraissait trop grand et trop près des bords.
- `ic_launcher_monochrome.xml` → `res/drawable/` : la même forme en blanc, pour les icônes thématiques d'Android 13 (le système la teinte).
- `ic_launcher_background.xml` → `res/values/` : la couleur de fond `#0E0E10`.
- `ic_launcher_foreground-<densité>.png` : le premier plan en PNG (108 dp : 108, 162, 216, 324, 432 px) si l'on préfère le bitmap au vecteur.
- `playstore-512.png` : l'icône du Play Store (512 × 512, sans alpha).
- `apercu-decoupes.png` : à quoi ressemble l'icône adaptative découpée en cercle et en « squircle ».
- Petite icône des notifications : pas de fichier ici. Elle est dans l'app (`androidApp/core/designsystem/src/main/res/drawable/ic_niumi_notification.xml`) : le même anneau et le même point d'une seule couleur, comme la version monochrome (Android la teinte lui-même), agrandis d'un facteur 1,22 pour remplir la zone utile de 20 dp sur 24 (31 octobre 2026). Dans le volet, Android 12 et plus la pose en blanc dans un rond de la couleur de la notification : encre `#0E0E10`, le fond de l'icône, pour retrouver la même image (décision de Mehdi du 31 octobre 2026 ; sans couleur, le système en tirait une du fond d'écran). La barre d'état, elle, garde la couleur du système.

Le thème de lancement de l'app (`Theme.Niumi.Launch`) garde son logo ; l'icône n'y intervient pas.
