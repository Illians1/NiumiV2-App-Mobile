# Niumi · Textes des maquettes validées

Relevé automatique des textes visibles dans chaque maquette, dans l'ordre de lecture, y compris le texte des champs de saisie. Il sert à recopier les textes sans faute. Les valeurs d'exemple (heures, noms d'applications, noms de sons, « Bureau ») ne sont pas des textes de l'app.

Quand une maquette montre une feuille par-dessus un écran, les textes de l'écran du dessous sont repris en premier.

Les textes dont la validation n'est pas certaine sont listés dans `POINTS-OUVERTS.md`, section 2.

Barre d'onglets du 25 octobre 2026 (décision de Mehdi) : les maquettes 15 à 18 et 24 ont perdu l'icône des réglages du haut et gagné la barre « Réveil », « Blocage seul », « Suivi », « Réglages » ; leurs textes ci-dessous sont relevés sur les sources mises à jour.

Numérotation du 25 octobre 2026 (64 maquettes, `maquettes/INDEX.md` fait foi) : les maquettes 18 à 20 sont nouvelles, les anciennes 18 à 61 sont devenues 21 à 64. Les sources régénérées ce jour-là ramènent d'anciens textes dans les maquettes 05, 17, 24 à 27 et 29 à 33 ; ce relevé garde les textes décidés (D23) et signale chaque écart dans la section concernée.

## 01 · Écran 1 · Panneau 1

Fichier : `01_ecran-1-panneau-1`

- À l'heure de te coucher, tes distractions sont bloquées
- À l'heure que tu choisis, Niumi bloque les applications que tu as désignées.
- Continuer

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone dont les applications sont verrouillées
- Panneau 1 sur 2

## 02 · Écran 1 · Panneau 2

Fichier : `02_ecran-1-panneau-2`

- Le matin, Niumi t'aide à te lever

Le titre s'écrit sans point final dans l'app (D12, tranché le 20 octobre 2026, `SPECIFICATION.md` §4) ; la transcription ci-dessus reste celle de l'image.
- Ton alarme sonne jusqu'à ce que tu scannes le Niumi Spot situé dans la pièce de ton choix.
- Configurer Niumi

Libellés d'accessibilité présents dans le fichier source :

- Retour
- Un téléphone approché d'un Niumi Spot
- Panneau 2 sur 2

## 03 · Écran 2 · Les cinq autorisations (Android)

Fichier : `03_ecran-2-les-cinq-autorisations-android`

- Cinq autorisations
- Sans elles, Niumi ne peut pas fonctionner.
- Alarme
- Pour que ton réveil sonne à l'heure, même téléphone verrouillé.
- Blocage des applications
- Pour bloquer les applications que tu choisis, et seulement celles-là.
- Autoriser
- Batterie
- Pour que le téléphone n'arrête pas Niumi pendant la nuit.
- Verrouillage de Niumi
- Pour que Niumi reste actif quand tu vides les applications récentes.
- Notifications
- Pour pouvoir te tenir informé de tout ce qui est important.
- Continuer

Libellés d'accessibilité présents dans le fichier source :

- Retour
- Accordée

## 04 · Écran 2 · Guide ouvert par « ? », images à produire

Fichier : `04_ecran-2-guide-ouvert-par-images-a-produire`

- Autoriser l'alarme
- [Image : la fenêtre du téléphone qui demande l'autorisation, avec le bouton à toucher entouré]
- Étape 1 sur 2
- Une fenêtre du téléphone s'ouvre. Touche « Autoriser ».
- Suivant

Libellés d'accessibilité présents dans le fichier source :

- Fermer
- Image 1 sur 2

## 05 · Écran 2 · Fenêtre d'autorisation (texte long)

Fichier : `05_ecran-2-fenetre-d-autorisation-texte-long`

Par-dessus l'écran 2 (mêmes textes que la maquette 03), la fenêtre :

- Autoriser le blocage des applications
- Niumi observe le nom de l'application affichée.
- Cette information sert uniquement à renvoyer les applications choisies vers l'accueil.
- Niumi ne lit pas le contenu des écrans ou les saisies.
- Le service peut être désactivé dans les réglages Android.
- Sans cette autorisation, Niumi ne peut pas fonctionner.
- Ouvrir les réglages d'accessibilité

La source régénérée le 25 octobre 2026 remplace la dernière phrase de la fenêtre par « Une session ne peut pas être activée si le service est inactif. ». C'est une régression : l'app garde « Sans cette autorisation, Niumi ne peut pas fonctionner. » (D23, `POINTS-OUVERTS.md` §5, E14).

Libellés d'accessibilité présents dans le fichier source :

- Autoriser le blocage des applications
- Fermer

## 06 · Écran 2 · Fenêtre d'autorisation (texte court)

Fichier : `06_ecran-2-fenetre-d-autorisation-texte-court`

Par-dessus l'écran 2 (mêmes textes que la maquette 03), la fenêtre :

- Verrouillage de Niumi
- Sur ce téléphone, « Tout effacer » dans les applications récentes arrête le blocage, sauf si Niumi y est verrouillé. Ouvre les applications récentes, appuie longuement sur Niumi et touche le cadenas. C'est à faire une seule fois.
- J'ai verrouillé Niumi

Libellés d'accessibilité présents dans le fichier source :

- Verrouillage de Niumi
- Fermer

## 07 · Écran 3 · 1. La consigne

Fichier : `07_ecran-3-1-la-consigne`

- Pose ton Niumi Spot là où tu veux te rendre en premier le matin, hors de ta chambre.
- Scanner le Niumi Spot
- Je n'ai pas encore de Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Retour
- Un Niumi Spot

## 08 · Écran 3 · 2. Le scan, iPhone et Android

Fichier : `08_ecran-3-2-le-scan-iphone-et-android`

- Pose ton Niumi Spot là où tu veux te rendre en premier le matin, hors de ta chambre.
- Prêt à scanner
- Approche ton iPhone du Niumi Spot
- Annuler

Libellés d'accessibilité présents dans le fichier source :

- Fenêtre du système : prêt à scanner

## 09 · Écran 3 · 3. Le choix de la pièce

Fichier : `09_ecran-3-3-le-choix-de-la-piece`

- Pose ton Niumi Spot là où tu veux te rendre en premier le matin, hors de ta chambre.
- Où est ton Niumi Spot ?
- Salle de bain
- Cuisine
- Salon
- Entrée
- Une autre pièce
- Continuer

Libellés d'accessibilité présents dans le fichier source :

- Choix de la pièce
- Pièce du Niumi Spot

## 10 · Écran 3 · 3. Cas « Une autre pièce »

Fichier : `10_ecran-3-3-cas-une-autre-piece`

- Pose ton Niumi Spot là où tu veux te rendre en premier le matin, hors de ta chambre.
- Où est ton Niumi Spot ?
- Salle de bain
- Cuisine
- Salon
- Entrée
- Nom de la pièce (texte d'un champ vide)
- Bureau (exemple de saisie)
- Continuer
- Clavier du téléphone

Libellés d'accessibilité présents dans le fichier source :

- Choix de la pièce
- Pièce du Niumi Spot
- Nom de la pièce

## 11 · Écran 3 bis · Temps pour sortir du lit

Fichier : `11_ecran-3-bis-temps-pour-sortir-du-lit`

- Combien de temps mets-tu pour sortir du lit ?
- Compte à partir de la première sonnerie. Une estimation suffit.
- 5 min
- 10 min
- 15 min
- 20 min
- 30 min
- Niumi s'en servira pour te montrer le temps que tu gagnes.
- Continuer

Libellés d'accessibilité présents dans le fichier source :

- Temps pour sortir du lit

## 12 · Écran 4 · 1. Avant le choix

Fichier : `12_ecran-4-1-avant-le-choix`

- Quelles applications veux-tu bloquer ?
- Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Spot.
- Aucune application choisie
- Choisir les applications
- Choisis au moins une application.
- Continuer

## 13 · Écran 4 · 2. La liste sur Android

Fichier : `13_ecran-4-2-la-liste-sur-android`

- Quelles applications veux-tu bloquer ?
- Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Spot.
- Applications à bloquer
- Rechercher (texte d'un champ vide)
- Instagram
- TikTok
- YouTube
- Snapchat
- Facebook
- Netflix
- WhatsApp
- Messages
- Chrome
- Gmail
- Maps
- Spotify
- Photos
- Agenda
- Horloge
- Reddit
- Discord
- Twitch
- Pinterest
- Telegram
- 6 applications choisies
- Valider

Libellés d'accessibilité présents dans le fichier source :

- Applications à bloquer
- Fermer sans enregistrer
- Rechercher une application

## 14 · Écran 4 · 3. Après le choix

Fichier : `14_ecran-4-3-apres-le-choix`

- Quelles applications veux-tu bloquer ?
- Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Spot.
- 6 applications choisies
- Modifier
- Tu pourras changer ce choix plus tard, depuis l'accueil.
- Continuer

Libellés d'accessibilité présents dans le fichier source :

- Icônes des 6 applications choisies

## 15 · Écran 6 · Réveil désactivé, arrivée après l'écran 4

Fichier : `15_ecran-6-reveil-desactive-arrivee-apres-l-ecran-4`

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil
- Désactivé
- Réveil désactivé
- Aucun réveil ni blocage prévu
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil
- Bientôt Blocage seul
- Bientôt Suivi
- Réglages

Libellés d'accessibilité présents dans le fichier source :

- Réveil désactivé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Navigation
- Réveil
- Blocage seul, bientôt disponible
- Suivi, bientôt disponible
- Réglages

## 16 · Écran 6 · Réveil prévu

Fichier : `16_ecran-6-reveil-prevu`

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil
- Bientôt Blocage seul
- Bientôt Suivi
- Réglages

Libellés d'accessibilité présents dans le fichier source :

- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Navigation
- Réveil
- Blocage seul, bientôt disponible
- Suivi, bientôt disponible
- Réglages

## 17 · Écran 6 · Session en cours (sombre)

Fichier : `17_ecran-6-session-en-cours-sombre`

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 7 h 50
- Blocage en cours
- Applications bloquées jusqu'au scan de ton Niumi Spot
- Annuler le réveil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil
- Bientôt Blocage seul
- Bientôt Suivi
- Réglages

La source régénérée le 25 octobre 2026 écrit « Annuler la session ». C'est une régression : D23 (24 octobre 2026) fixe « Annuler le réveil », que l'app garde (`POINTS-OUVERTS.md` §5, E14).

Libellés d'accessibilité présents dans le fichier source :

- Verrouillé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Navigation
- Réveil
- Blocage seul, bientôt disponible
- Suivi, bientôt disponible
- Réglages

## 18 · Écran 6 · Réveil prévu, alarme unique

Fichier : `18_ecran-6-reveil-prevu-alarme-unique` (ajoutée le 25 octobre 2026 : aucun jour coché, onglet « Ponctuel » choisi)

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil
- Bientôt Blocage seul
- Bientôt Suivi
- Réglages

Libellés d'accessibilité présents dans le fichier source :

- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Navigation
- Réveil
- Blocage seul, bientôt disponible
- Suivi, bientôt disponible
- Réglages

## 19 · Écran 6 · Sélecteur d'heure, onglet Coucher

Fichier : `19_ecran-6-selecteur-d-heure-onglet-coucher` (ajoutée le 25 octobre 2026)

Source du 30 octobre 2026 : fenêtre à 28 des bords, traits limités aux roues, phrase sur deux lignes.

Par-dessus l'accueil (mêmes textes que la maquette 16, jours du lundi au vendredi cochés), la fenêtre :

- Coucher
- Réveil
- 20
- 21
- 22
- 23
- 0
- :
- 28
- 29
- 30
- 31
- 32
- 8 h 30 de sommeil avant le réveil de 7:00
- Valider

Les nombres des roues sont des valeurs : heures de 0 à 23 sans zéro devant, minutes de « 00 » à « 59 ».

Libellés d'accessibilité présents dans le fichier source :

- Heure de coucher
- Fermer

Textes de l'app absents des maquettes (décisions du 25 octobre 2026, à valider par Mehdi, `POINTS-OUVERTS.md` §2) : « 24 h de blocage jusqu’au réveil de 7:00 » à la place de la phrase quand les deux heures sont identiques ; libellés d'accessibilité « Heures » et « Minutes » pour les deux roues.

## 20 · Écran 6 · Sélecteur d'heure, onglet Réveil

Fichier : `20_ecran-6-selecteur-d-heure-onglet-reveil` (ajoutée le 25 octobre 2026)

Source du 30 octobre 2026 : fenêtre à 28 des bords, traits limités aux roues, phrase sur deux lignes.

Par-dessus l'accueil (mêmes textes que la maquette 16), la fenêtre :

- Coucher
- Réveil
- 5
- 6
- 7
- 8
- 9
- :
- 58
- 59
- 00
- 01
- 02
- 8 h 30 de sommeil après le coucher de 22:30
- Valider

Libellés d'accessibilité présents dans le fichier source :

- Heure de réveil
- Fermer

Textes de l'app absents des maquettes (même statut) : « 24 h de blocage depuis le coucher de 22:30 » quand les deux heures sont identiques ; sur l'accueil, « 24 h de blocage » à la place de « 8 h 30 de sommeil » sur la carte, « Choisir l’heure » (action du cadran pour le lecteur d'écran) et « Icône de l’application bloquée » (ligne « Applications bloquées » avec une seule application).

## 21 · Sonnerie · La feuille

Fichier : `21_sonnerie-la-feuille` (maquettes de la sonnerie redessinées et renumérotées le 23 octobre 2026, puis de 18-20 à 21-23 le 25 octobre 2026)

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Sonnerie
- Son
- Aube
- Carillon
- Oiseaux
- Classique
- Insistante
- Volume du téléphone 80 %
- Sonnerie progressive
- Monte jusqu’au volume du téléphone en 2 min
- 1 min
- 2 min
- 5 min
- 10 min

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Sonnerie
- Fermer
- Son de la sonnerie
- Écouter Aube
- Écouter Carillon
- Écouter Oiseaux
- Écouter Classique
- Écouter Insistante
- Volume du téléphone : 80 %
- Sonnerie progressive
- Sonnerie progressive : durée pour atteindre le volume maximal

## 22 · Sonnerie · Quand on touche le volume

Fichier : `22_sonnerie-quand-on-touche-le-volume`

Mêmes textes que la maquette 21, plus la bulle :

- Il se règle dans les réglages de ton téléphone.

## 23 · Sonnerie · Progressive désactivée

Fichier : `23_sonnerie-progressive-desactivee`

Mêmes textes que la maquette 21, sans « Monte jusqu’au volume du téléphone en 2 min » ni les durées : la ligne « Sonnerie progressive » reste, interrupteur éteint.

## 24 · Annuler · 1. Le lien sur l'accueil

Fichier : `24_annuler-1-le-lien-sur-l-accueil`

La source régénérée le 25 octobre 2026 écrit « Annuler la session » : régression, ce relevé garde le texte de D23 (`POINTS-OUVERTS.md` §5, E14). Même remarque pour les maquettes 25 à 27 et 29 à 33 ci-dessous.

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 7 h 50
- Blocage en cours
- Applications bloquées jusqu'au scan de ton Niumi Spot
- Annuler le réveil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil
- Bientôt Blocage seul
- Bientôt Suivi
- Réglages

Libellés d'accessibilité présents dans le fichier source :

- Verrouillé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Navigation
- Réveil
- Blocage seul, bientôt disponible
- Suivi, bientôt disponible
- Réglages

## 25 · Annuler · 2. Scanner ou sortie de secours

Fichier : `25_annuler-2-scanner-ou-sortie-de-secours`

Source du 25 octobre 2026 : « Annuler la session » et « Annuler la session ? » (régression, D23).

Source du 30 octobre 2026 : la question devient une fenêtre centrée au format de la fenêtre d'alerte (croix « Fermer », icône du scan), à la place de la feuille du bas.

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 7 h 50
- Blocage en cours
- Applications bloquées jusqu'au scan de ton Niumi Spot
- Annuler le réveil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Annuler le réveil ?
- Scanne ton Niumi Spot pour annuler. Tes applications seront débloquées et ton réveil désactivé.
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Verrouillé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Annuler le réveil ?
- Fermer

## 26 · Annuler · 3. La feuille de scan

Fichier : `26_annuler-3-la-feuille-de-scan`

Source du 25 octobre 2026 : « Annuler la session » et « Approche ton iPhone du Niumi Spot pour annuler la session » (régression, D23).

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 7 h 50
- Blocage en cours
- Applications bloquées jusqu'au scan de ton Niumi Spot
- Annuler le réveil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Prêt à scanner
- Approche ton iPhone du Niumi Spot pour annuler le réveil
- Annuler

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Verrouillé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Fenêtre du système : prêt à scanner

## 27 · Annuler · 4. Réveil annulé : retour au clair

Fichier : `27_annuler-4-session-annulee-retour-au-clair`

Source du 25 octobre 2026 : titre « Annuler · 4. Session annulée : retour au clair » et message « Session annulée. Tes applications sont débloquées et ton réveil est désactivé. » (régression, D23 et E3).

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil
- Désactivé
- Réveil désactivé
- Aucun réveil ni blocage prévu
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Réveil annulé. Tes applications sont débloquées.

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil désactivé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées

## 28 · Écran 7 · Écran de blocage

Fichier : `28_ecran-7-ecran-de-blocage`

- Cette application est bloquée
- Elle reviendra après avoir scanné ton Niumi Spot.
- Fermer
- Ouvrir Niumi

## 29 · Écran 8 · Confirmation, le soir

Fichier : `29_ecran-8-confirmation-le-soir`

Source du 25 octobre 2026 : « Annuler la session », « Sortir de la session sans scanner ton Niumi Spot ? » et « Rester dans la session » (régression, D23).

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 7 h 50
- Blocage en cours
- Applications bloquées jusqu'au scan de ton Niumi Spot
- Annuler le réveil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Arrêter le réveil sans scanner ton Niumi Spot ?
- Recopier un long texte remplace le scan de ton Niumi Spot.
- Garder le réveil
- Recopier le texte

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Verrouillé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Sortie de secours

## 30 · Écran 8 · Confirmation, le matin

Fichier : `30_ecran-8-confirmation-le-matin`

Source du 25 octobre 2026 : « Sortir de la session sans scanner ton Niumi Spot ? » et « Rester dans la session » (régression, D23).

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Arrêter le réveil sans scanner ton Niumi Spot ?
- L'alarme s'arrêtera et tes applications seront débloquées. Pour sortir, tu devras recopier un long texte.
- Garder le réveil
- Recopier le texte

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Sortie de secours

## 31 · Écran 9 · Recopie en cours

Fichier : `31_ecran-9-recopie-en-cours`

Sources 31 à 33 du 25 octobre 2026 : « Recopie ce texte pour sortir de la session. », le texte à recopier d'avant sa révision (« …Je choisis de sortir de cette session sans le scanner… »), le compteur « sur 197 caractères » et le bouton « Sortir de la session » (régression, D8, D23 et E2).

- Annuler
- 84 sur 190 caractères
- Recopie ce texte pour arrêter le réveil.
- J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choisis d'arrêter mon réveil sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.
- J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choi
- Arrêter le réveil
- Clavier du téléphone

Libellés d'accessibilité présents dans le fichier source :

- Ta saisie

## 32 · Écran 9 · Erreur signalée

Fichier : `32_ecran-9-erreur-signalee`

- Annuler
- 32 sur 190 caractères
- Recopie ce texte pour arrêter le réveil.
- J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choisis d'arrêter mon réveil sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.
- J'avais décidé de bloquer mes aplications
- Un mot ne correspond pas. Il est signalé dans le texte.
- Arrêter le réveil
- Clavier du téléphone

Libellés d'accessibilité présents dans le fichier source :

- Ta saisie

## 33 · Écran 9 · Texte complet, bouton actif

Fichier : `33_ecran-9-texte-complet-bouton-actif`

- Annuler
- 190 sur 190 caractères
- Recopie ce texte pour arrêter le réveil.
- J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choisis d'arrêter mon réveil sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.
- J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choisis d'arrêter mon réveil sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.
- Arrêter le réveil
- Clavier du téléphone

Libellés d'accessibilité présents dans le fichier source :

- Ta saisie

## 34 · Écran 10 · Le réveil sonne

Fichier : `34_ecran-10-le-reveil-sonne`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot

## 35 · Écran 10 · La feuille de scan, iPhone et Android

Fichier : `35_ecran-10-la-feuille-de-scan-iphone-et-android`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Prêt à scanner
- Approche ton iPhone du Niumi Spot
- Annuler

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Fenêtre du système : prêt à scanner

## 36 · Écran 10 · Échec : pas un Niumi Spot

Fichier : `36_ecran-10-echec-pas-un-niumi-spot`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Ce n'est pas un Niumi Spot
- Scanne un Niumi Spot que tu as enregistré.
- Réessayer
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Échec du scan

## 37 · Écran 10 · Échec : Niumi Spot non enregistré

Fichier : `37_ecran-10-echec-niumi-spot-non-enregistre`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Ce Niumi Spot n'est pas enregistré
- Scanne un Niumi Spot que tu as enregistré.
- Réessayer
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Échec du scan

## 38 · Écran 10 · Échec : lecture ratée

Fichier : `38_ecran-10-echec-lecture-ratee`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Ton Niumi Spot n'a pas pu être lu
- Rapproche ton téléphone et réessaie.
- Réessayer
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Échec du scan

## 39 · Écran 10 · Échec : NFC coupé (Android)

Fichier : `39_ecran-10-echec-nfc-coupe-android`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Le NFC est coupé
- Active-le pour scanner ton Niumi Spot.
- Ouvrir les réglages
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Échec du scan

## 40 · Écran 10 · iPhone seulement : l'alarme va reprendre

Fichier : `40_ecran-10-iphone-seulement-l-alarme-va-reprendre`

- Vendredi 2 octobre
- 7:00
- Ton alarme va reprendre
- Scanne ton Niumi Spot pour l'arrêter.
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot

## 41 · Écran 10 · iPhone seulement : temps de scan écoulé

Fichier : `41_ecran-10-iphone-seulement-temps-de-scan-ecoule`

- Vendredi 2 octobre
- 7:00
- Scanne ton Niumi Spot
- Scanner le Niumi Spot
- Je n'ai pas accès à mon Niumi Spot
- Le temps de scan est écoulé
- Relance le scan une fois devant ton Niumi Spot.
- Réessayer
- Je n'ai pas accès à mon Niumi Spot

Libellés d'accessibilité présents dans le fichier source :

- Un téléphone approché d'un Niumi Spot
- Échec du scan

## 42 · Écran 11 · Réussite : retour au clair

Fichier : `42_ecran-11-reussite-retour-au-clair`

- Debout à 7:04
- 4 minutes après la sonnerie.
- Bravo ! C'est 11 minutes de moins qu'avant Niumi.
- Tes applications sont de nouveau accessibles.
- Retour à l'accueil

## 43 · Écran 12 · Réglages

Fichier : `43_ecran-12-reglages`

- Réglages
- Niumi Spots 2
- Autorisations 3 sur 3
- Aide
- À propos

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'accueil

## 44 · Réglages · Aide

Fichier : `44_reglages-aide`

- Aide
- Fonctionnement
- Niumi Spot perdu
- Sortie de secours
- Temps gagné

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages

## 45 · Aide · Une rubrique (Fonctionnement)

Fichier : `45_aide-une-rubrique-fonctionnement`

- Fonctionnement
- Le soir
- À l'heure que tu choisis, Niumi bloque les applications que tu as désignées.
- Le matin
- Ton alarme sonne jusqu'à ce que tu scannes le Niumi Spot situé dans la pièce de ton choix.
- Après le scan
- Tes applications sont de nouveau accessibles.

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'aide

## 46 · Aide · Temps gagné

Fichier : `46_aide-temps-gagne`

- Temps gagné
- Après chaque réveil, Niumi compare le temps que tu as mis à te lever avec celui que tu mettais avant.
- Temps pour sortir du lit avant Niumi
- 15 min
- Modifier

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'aide

## 47 · Réglages · À propos

Fichier : `47_reglages-a-propos`

- Version 1.0
- Mentions légales
- Politique de confidentialité
- Licences

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages

## 48 · Écran 12 · Réglages (sombre)

Fichier : `48_ecran-12-reglages-sombre`

- Réglages
- Niumi Spots 2
- Autorisations 3 sur 3
- Aide
- À propos

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'accueil

## 49 · Réglages · Aide (sombre)

Fichier : `49_reglages-aide-sombre`

- Aide
- Fonctionnement
- Niumi Spot perdu
- Sortie de secours
- Temps gagné

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages

## 50 · Aide · Une rubrique (sombre)

Fichier : `50_aide-une-rubrique-sombre`

- Fonctionnement
- Le soir
- À l'heure que tu choisis, Niumi bloque les applications que tu as désignées.
- Le matin
- Ton alarme sonne jusqu'à ce que tu scannes le Niumi Spot situé dans la pièce de ton choix.
- Après le scan
- Tes applications sont de nouveau accessibles.

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'aide

## 51 · Aide · Temps gagné (sombre)

Fichier : `51_aide-temps-gagne-sombre`

- Temps gagné
- Après chaque réveil, Niumi compare le temps que tu as mis à te lever avec celui que tu mettais avant.
- Temps pour sortir du lit avant Niumi
- 15 min
- Modifier

Libellés d'accessibilité présents dans le fichier source :

- Retour à l'aide

## 52 · Réglages · À propos (sombre)

Fichier : `52_reglages-a-propos-sombre`

- Version 1.0
- Mentions légales
- Politique de confidentialité
- Licences

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages

## 53 · Niumi Spots · La liste

Fichier : `53_niumi-spots-la-liste`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine

## 54 · Niumi Spots · Confirmation de suppression

Fichier : `54_niumi-spots-confirmation-de-suppression`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Supprimer le Niumi Spot « Cuisine » ?
- Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter.
- Supprimer
- Annuler

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine
- Supprimer un Niumi Spot

## 55 · Niumi Spots · Dernier Niumi Spot

Fichier : `55_niumi-spots-dernier-niumi-spot`

- Niumi Spots
- Salle de bain
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Renommer la pièce
- Supprimer Tu dois garder au moins un Niumi Spot.

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain

## 56 · Niumi Spots · Renommer la pièce

Fichier : `56_niumi-spots-renommer-la-piece`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Où est ton Niumi Spot ?
- Salle de bain
- Cuisine
- Salon
- Entrée
- Une autre pièce
- Enregistrer

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine
- Renommer la pièce
- Pièce du Niumi Spot

## 57 · Niumi Spots · La liste (sombre)

Fichier : `57_niumi-spots-la-liste-sombre`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine

## 58 · Niumi Spots · Confirmation de suppression (sombre)

Fichier : `58_niumi-spots-confirmation-de-suppression-sombre`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Supprimer le Niumi Spot « Cuisine » ?
- Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter.
- Supprimer
- Annuler

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine
- Supprimer un Niumi Spot

## 59 · Niumi Spots · Dernier Niumi Spot (sombre)

Fichier : `59_niumi-spots-dernier-niumi-spot-sombre`

- Niumi Spots
- Salle de bain
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Renommer la pièce
- Supprimer Tu dois garder au moins un Niumi Spot.

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain

## 60 · Niumi Spots · Renommer la pièce (sombre)

Fichier : `60_niumi-spots-renommer-la-piece-sombre`

- Niumi Spots
- Salle de bain
- Cuisine
- Ajouter un Niumi Spot
- N'importe lequel de tes Niumi Spots arrête l'alarme.
- Où est ton Niumi Spot ?
- Salle de bain
- Cuisine
- Salon
- Entrée
- Une autre pièce
- Enregistrer

Libellés d'accessibilité présents dans le fichier source :

- Retour aux réglages
- Actions pour le Niumi Spot : Salle de bain
- Actions pour le Niumi Spot : Cuisine
- Renommer la pièce
- Pièce du Niumi Spot

## 61 · Bandeau · NFC coupé (Android)

Fichier : `61_bandeau-nfc-coupe-android`

- Le scan du Niumi Spot ne fonctionnera pas : le NFC est coupé.
- Activer le NFC
- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées

## 62 · Bandeau · Aucun Niumi Spot (au cas où)

Fichier : `62_bandeau-aucun-niumi-spot-au-cas-ou`

- Aucun Niumi Spot n'est associé.
- Associer un Niumi Spot
- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées

## 63 · Alerte · Une autorisation retirée

Fichier : `63_alerte-une-autorisation-retiree`

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Il manque une autorisation
- Tes applications ne seront pas bloquées : l'autorisation a été retirée.
- Voir les autorisations
- Plus tard

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Il manque une autorisation

## 64 · Alerte · Plusieurs autorisations retirées

Fichier : `64_alerte-plusieurs-autorisations-retirees`

- Coucher
- 22:30
- Réveil
- 7:00
- Réveil dans
- 9 h 26
- Réveil activé
- Blocage des apps dans 56 min
- 8 h 30 de sommeil
- Ponctuel
- Quotidien
- L
- M
- M
- J
- V
- S
- D
- Applications bloquées 6
- Sonnerie Carillon Progressive · 2 min
- Il manque des autorisations
- Alarme Ton réveil ne pourra pas sonner.
- Blocage des applications Tes applications ne seront pas bloquées.
- Voir les autorisations
- Plus tard

Libellés d'accessibilité présents dans le fichier source :

- Réglages
- Réveil activé
- Lundi
- Mardi
- Mercredi
- Jeudi
- Vendredi
- Samedi
- Dimanche
- Icônes des 6 applications bloquées
- Il manque des autorisations
