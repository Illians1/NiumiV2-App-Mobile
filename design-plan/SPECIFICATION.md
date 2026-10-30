# Niumi · Spécification des écrans

État au 9 octobre 2026. Ce fichier décrit ce qui est décidé aujourd'hui, écran par écran. Il ne raconte pas comment on y est arrivé : l'historique complet est dans `annexes/journal-des-decisions.md`.

Les images citées sont dans `maquettes/images/`. Chaque image a un fichier source du même nom dans `maquettes/sources/`.

Tout ce qui n'est pas décidé, pas validé ou pas dessiné est rassemblé dans `POINTS-OUVERTS.md`. Les principaux passages concernés portent ici la mention **(ouvert)**. La liste complète est dans `POINTS-OUVERTS.md` : c'est elle qui fait référence.

## 1. Ce que fait l'app

Niumi est une app de réveil pour iPhone et Android. À l'heure du coucher, elle bloque les applications que l'utilisateur a choisies. Le matin, l'alarme sonne jusqu'à ce qu'il scanne un Niumi Spot : une étiquette NFC posée dans une autre pièce que sa chambre. L’étiquette s’appelait « Niumi Point » jusqu’au 24 octobre 2026 ; le nom « Niumi Spot » la remplace partout (décision du 24 octobre 2026). Le scan arrête l'alarme et débloque les applications.

Une **session** va de l'heure du coucher jusqu'au scan du matin. Ce mot nomme le concept dans les spécifications ; les textes affichés ne le disent pas et parlent du réveil, du blocage ou des applications (décision du 24 octobre 2026, qui étend aux écrans la règle des notifications du 23 octobre). Les écrans Android de l'ancien parcours, remplacés pendant la refonte, gardent le mot jusqu'à leur suppression.

## 2. Règles communes

| Sujet | Règle |
| --- | --- |
| Réveil | Récurrent, aux jours choisis, à une heure unique. Sans aucun jour coché, il est **ponctuel** : il sonne une seule fois, à la prochaine heure de réveil (aujourd'hui si elle est encore à venir, sinon le lendemain, quel que soit le jour), puis l'interrupteur s'éteint après le scan du matin ou la recopie du matin. « Quotidien » coche les sept jours (décisions de Mehdi du 2026-10-25, section 9) |
| Coucher | Le blocage démarre seul à l'heure du coucher. Si l'heure du coucher est plus tardive que l'heure du réveil, le coucher a lieu la veille du réveil. Sinon, il a lieu le jour même. Deux heures identiques forment une période de 24 h : le coucher a lieu la veille, à la même heure. Aucun écart minimal n'est imposé : une minute d'écart au sélecteur d'heure, 5 min au cadran, ou aucune (24 h) (décision de Mehdi du 2026-10-25, qui remplace les 15 minutes d'écart du 2026-10-24) |
| Rappel | Notification 30 minutes avant le coucher, sans bouton |
| Avant le coucher | Tout se modifie. Un réveil s'annule en décochant le jour ; décocher le dernier jour coché rend le réveil ponctuel, il ne le supprime pas (décision de Mehdi du 2026-10-25). Un interrupteur général désactive tout le planning |
| Du coucher au scan | Heures, jours et applications sont verrouillés. La sonnerie et les Niumi Spots restent modifiables (décision du 19 octobre 2026). Un scan avant l'alarme annule la session |
| Fin normale | Scan, le matin, d'un Niumi Spot enregistré. N'importe lequel convient |
| Sortie de secours | Un long texte à recopier remplace le scan. Elle compte comme un scan |
| Exclu de l'app | Lancement anticipé, confirmation, essai à blanc, historique, compte. Ce sont les mots de la règle d'origine. Le mot « confirmation » ne vise pas les confirmations décidées depuis (écran 8, suppression d'un Niumi Spot, blocage immédiat depuis l'accueil, §9) **(ouvert : sens exact à confirmer)** |

Autres règles :

- **Tutoiement.** Tous les textes tutoient l'utilisateur.
- **Titres sans point final.** Aucun titre ne se termine par un point : titres d'écran, de feuille, de fenêtre et d'échec (« Debout à 7:04 », « Le NFC est coupé »). Les phrases sous un titre gardent le leur. Un titre en forme de question garde son point d'interrogation (« Annuler le réveil ? »). Décision de Mehdi du 26 octobre 2026.
- **Même fonctionnement sur iPhone et Android, autant que possible.** Exemple : la feuille de scan de l'iPhone ne peut pas contenir de lien, donc le lien « Je n'ai pas accès à mon Niumi Spot » est placé avant le scan sur les deux. Les différences voulues sont signalées dans chaque écran.
- **Fonctionnement par période.** Niumi raisonne par période, du coucher au réveil. L'interrupteur s'active toujours et aucune heure n'est refusée. Si l'on se trouve déjà dans la période au moment où l'on active l'interrupteur, coche un jour, touche « Ponctuel » ou « Quotidien », ou change une heure (poignée du cadran ou sélecteur), une fenêtre demande d'abord confirmation (§9, « Confirmation avant un blocage immédiat ») ; si l'on confirme, le blocage commence tout de suite et l'accueil passe en « Blocage en cours » jusqu'au scan du matin. Exemple : il est 23:00 et la période va de 22:30 à 7:00 ; activer l'interrupteur ouvre la fenêtre, et « Bloquer maintenant » lance le blocage. Une période de 24 h est toujours en cours en ponctuel : l'activer demande aussi confirmation. Décision de Mehdi du 2026-10-25, après un blocage parti sans prévenir en ajoutant un jour et en changeant l'heure : elle remplace « le blocage commence tout de suite, sans confirmation » (décision du 7 octobre 2026). En Quotidien, elle bloque en permanence, puisque la période suivante commence au scan du matin : conséquence assumée par Mehdi (décision du 2026-10-25).
- **Clair ou sombre.** L'app est claire quand aucune session n'est en cours, sombre pendant une session. Elle ne suit pas le réglage du téléphone. Détail dans `VALEURS-DE-DESIGN.md`.
- **Valeurs par défaut.** Lundi à vendredi, réveil à 7:00, coucher à 22:30.
- **Exemple utilisé dans les maquettes.** Réveil à 7:00, coucher à 22:30, 6 applications, Niumi Spots dans la salle de bain et la cuisine.
- **Écriture des heures et des durées.** « 22:30 », « 7:00 » (sans zéro devant), « 9 h 26 », « 56 min », « 8 h 30 de sommeil ». Une période de 24 h s'écrit « 24 h de blocage », jamais « 24 h 00 de sommeil » (texte non dessiné, à valider par Mehdi, `POINTS-OUVERTS.md` §2). À partir d'un jour, un compte à rebours (« Réveil dans … », « Blocage des apps dans … ») s'écrit en jours et en heures entières, sans les minutes : « 1 j », « 1 j 4 h », « 2 j 4 h », « 6 j ». Un compte à rebours de 24 h pile, comme une période de 24 h réglée à l'heure même du réveil, se lit « 1 j ». Jamais « 52 h 10 » ni « 24 h 00 » (décision de Mehdi du 2026-10-25, après essai sur appareil ; texte non dessiné, à valider, `POINTS-OUVERTS.md` §2). Les notifications suivent la même règle des heures, sans zéro devant (section 17).

## 3. Le parcours

Mise en route, une seule fois :

1. Écran 1 · Présentation, deux panneaux
2. Écran 2 · Autorisations
3. Écran 3 · Association du Niumi Spot : consigne, scan, choix de la pièce
4. Écran 3 bis · Temps pour sortir du lit
5. Écran 4 · Choix des applications
6. Écran 6 · Accueil, dans l'état « Réveil désactivé »

L'écran 5 (premier planning) a été supprimé. Son numéro n'est pas réutilisé.

La mise en route est finie quand l'utilisateur touche « Continuer » sur l'écran 4. Elle ne se refait pas par le retour, et Niumi s'ouvre ensuite directement sur l'accueil. Une mise en route interrompue reprend à la présentation, ou à l'écran 2 si la présentation a été lue (décisions de Mehdi du 2026-10-25).

Ensuite, chaque jour :

- Avant le coucher : accueil « Réveil prévu » (clair).
- Du coucher au réveil : accueil « Session en cours » (sombre). Une application bloquée affiche l'écran 7.
- Au réveil : écran 10, puis écran 11 après le scan, puis retour à l'accueil. Après un réveil ponctuel, l'interrupteur est éteint : l'accueil est en « Réveil désactivé ».
- Avant l'alarme : annulation de la session par scan (section 11).
- À tout moment de la session : sortie de secours (écrans 8 et 9).

La barre d'onglets en bas de l'accueil mène aux réglages, en session comme hors session. « Blocage seul » et « Suivi » y sont annoncés « Bientôt » et ne font rien (décision de Mehdi du 25 octobre 2026, section 9).

## 4. Écran 1 · Présentation du principe

Images : `01_ecran-1-panneau-1`, `02_ecran-1-panneau-2`. Thème clair.

Deux panneaux expliquent la règle avant toute configuration. On ne peut pas les passer : c'est le seul endroit où l'utilisateur apprend qu'une session démarre sans lui.

Le logo est en haut des deux panneaux. Un repère à deux traits indique le panneau en cours. Le panneau 2 a une flèche de retour vers le panneau 1.

| Panneau | Dessin | Titre | Texte | Bouton |
| --- | --- | --- | --- | --- |
| 1 | Un téléphone dont les applications sont verrouillées, avec un cadenas dans une pastille ambre | À l'heure de te coucher, tes distractions sont bloquées | À l'heure que tu choisis, Niumi bloque les applications que tu as désignées. | Continuer |
| 2 | Le téléphone approché du Niumi Spot (le même dessin que sur l'écran 10) | Le matin, Niumi t'aide à te lever | Ton alarme sonne jusqu'à ce que tu scannes le Niumi Spot situé dans la pièce de ton choix. | Configurer Niumi |

Les deux titres s'écrivent sans point final (D12, tranché le 20 octobre 2026). L'image `02_ecran-1-panneau-2` montre encore le point : c'est ce texte qui fait foi.

## 5. Écran 2 · Autorisations

Images : `03_ecran-2-les-cinq-autorisations-android`, `04_ecran-2-guide-ouvert-par-images-a-produire`, `05_ecran-2-fenetre-d-autorisation-texte-long`, `06_ecran-2-fenetre-d-autorisation-texte-court`. Thème clair.

Un seul écran présente les autorisations, une tuile par autorisation, avec une phrase d'explication avant la demande du système.

| Élément | Contenu |
| --- | --- |
| Titre | Trois autorisations sur iPhone. Sur Android, le titre compte les tuiles affichées : « Cinq autorisations », ou « Quatre autorisations » sur un téléphone sans verrou des applications récentes (décision du 21 octobre 2026) |
| Texte | Sans elles, Niumi ne peut pas fonctionner. (décision du 21 octobre 2026) |
| Bouton du bas | Continuer |

| Tuile | Phrase | iPhone | Android |
| --- | --- | --- | --- |
| Alarme | Pour que ton réveil sonne à l'heure, même téléphone verrouillé. | Fenêtre du système | Rien à demander en principe pour une app de réveil |
| Blocage des applications | Pour bloquer les applications que tu choisis, et seulement celles-là. | Fenêtre du système (Temps d'écran) | Service d'accessibilité à activer dans les réglages, après une fenêtre d'explication exigée par Google Play. Approche courante, à valider avec Google Play |
| Batterie (Android seulement) | Retire les restrictions de batterie, pour que le téléphone n’arrête pas Niumi pendant la nuit. | — | Fenêtre du système (demande d'exemption, décision du 21 octobre 2026). À défaut, la liste des optimisations de batterie |
| Verrouiller Niumi (Android seulement, téléphones qui ont ce verrou, comme HyperOS) | Pour que Niumi reste actif quand tu vides les applications récentes. | — | Fenêtre centrée qui explique le geste (appui long sur Niumi dans les applications récentes, puis cadenas), puis relecture |
| Notifications | Pour pouvoir te tenir informé de tout ce qui est important. | Fenêtre du système | Fenêtre du système |

Comportement :

- Chaque tuile a une icône, son nom, sa phrase, un bouton « Autoriser » et un petit bouton « ? ».
- Une fois l'autorisation donnée, la tuile affiche une coche, à l'encre dans une pastille ambre (décision du 22 octobre 2026). Dans la maquette, la coche remplace à la fois « Autoriser » et « ? » : le guide n'est donc plus accessible depuis une tuile accordée **(ouvert : à confirmer)**.
- « Continuer » reste grisé tant qu'il en manque une. Toutes sont obligatoires, notifications comprises.
- Sur un écran trop petit pour toutes les tuiles, la liste défile au-dessus de « Continuer », qui reste en place à 16 points au moins de la dernière tuile. Les tuiles cachées sous le bouton s'estompent dans le fond pour montrer qu'il reste à faire défiler (décisions du 22 octobre 2026, absentes de la maquette 03).
- **Différence Android (décision du 21 octobre 2026).** Deux tuiles de plus, entre « Blocage des applications » et « Notifications » : « Batterie » et « Verrouiller Niumi ». Sans elles, le téléphone arrête Niumi pendant la nuit et le blocage cesse sans prévenir (mesures sur HyperOS). La tuile « Verrouiller Niumi » n'apparaît que sur les téléphones qui ont ce verrou. Ces tuiles et leurs icônes ne sont pas dessinées. Sur Android, les explications qui précèdent un réglage (consentement du blocage, verrou) s'ouvrent dans une fenêtre centrée de la forme de l'alerte d'autorisations (image 63), fermée par une croix ou par un toucher à côté (décision du 21 octobre 2026). Mise en page fixée le 22 octobre 2026 : croix ronde au coin, icône de la tuile dans un rond gris, titre centré, texte aligné à gauche, bouton. La tuile du verrou s'appelle « Verrouiller Niumi » depuis le même jour.
- En cas de refus, il ne se passe rien de particulier : la tuile n'est simplement pas affichée comme accordée.
- Si une autorisation est retirée plus tard, c'est l'alerte d'autorisations qui prévient l'utilisateur (section 17).
- Cet écran se rouvre depuis Réglages, ligne « Autorisations », pour revoir ou réparer les autorisations.

Le NFC n'a pas de tuile. Sur iPhone il n'y a rien à demander. Sur Android, s'il est coupé, l'utilisateur est prévenu au moment du scan et par le bandeau de l'accueil (D10, tranché le 20 octobre 2026).

**Le guide ouvert par « ? »** (image 04) : une croix pour fermer, le titre (« Autoriser l'alarme »), une grande image, « Étape 1 sur 2 », une phrase (« Une fenêtre du téléphone s'ouvre. Touche « Autoriser ». »), des points de progression et le bouton « Suivant ». La mise en page est validée. Les images restent à produire, pour iPhone et pour Android, et les textes des étapes ne sont écrits que pour cet exemple **(ouvert)**. Sur Android, l'écran 2 est livré sans « ? » jusque-là (décision du 20 octobre 2026).

## 6. Écran 3 · Association du Niumi Spot

Images : `07_ecran-3-1-la-consigne`, `08_ecran-3-2-le-scan-iphone-et-android`, `09_ecran-3-3-le-choix-de-la-piece`, `10_ecran-3-3-cas-une-autre-piece`. Thème clair.

L'utilisateur scanne son Niumi Spot une première fois, puis nomme la pièce où il le pose. Ce scan vérifie aussi que le NFC fonctionne et apprend le geste du matin.

**Temps 1, la consigne.** Le logo en haut, une flèche de retour, le Niumi Spot dessiné seul et en grand, puis :

| Élément | Contenu |
| --- | --- |
| Consigne | Pose ton Niumi Spot là où tu veux te rendre en premier le matin, hors de ta chambre. |
| Bouton | Scanner le Niumi Spot |
| Lien discret | Je n'ai pas encore de Niumi Spot |

Le lien ouvre la page web du produit, pour l'acheter.

**Temps 2, le scan.** L'écran de la consigne s'assombrit et une feuille monte du bas, sur toute la largeur.

| Élément | iPhone | Android |
| --- | --- | --- |
| Qui dessine la feuille | Le système. Niumi n'y choisit que la phrase | Niumi, à l'identique, sauf le titre à l'encre plutôt qu'en gris (décision du 22 octobre 2026) |
| Titre | Prêt à scanner | Prêt à scanner |
| Phrase | Approche ton iPhone du Niumi Spot | Approche ton téléphone du Niumi Spot |
| Bouton | Annuler | Annuler |

**Temps 3, le choix de la pièce.** Après le scan, une feuille monte du bas par-dessus l'écran de la consigne assombri.

| Élément | Contenu |
| --- | --- |
| Titre | Où est ton Niumi Spot ? |
| Cases | Salle de bain, Cuisine, Salon, Entrée : quatre grandes cases avec une icône |
| Case large | Une autre pièce |
| Bouton | Continuer |

Rien ne dit si « Continuer » est grisé tant qu'aucune pièce n'est choisie **(ouvert)**.

« Une autre pièce » ouvre la saisie d'un nom libre dans la même feuille : la case devient un champ « Nom de la pièce », le clavier s'ouvre et la feuille remonte au-dessus de lui. Il n'y a pas d'écran de confirmation : « Continuer » mène directement à l'écran 3 bis.

**Échecs du scan.**

| Cas | Texte | Action |
| --- | --- | --- |
| Pas un Niumi Spot | Ce n'est pas un Niumi Spot. | Réessayer |
| Lecture ratée | Ton Niumi Spot n'a pas pu être lu. Rapproche ton téléphone et réessaie. | Réessayer |
| NFC coupé (Android) | Le NFC est coupé sur ton téléphone. | Ouvrir les réglages |
| Déjà enregistré | Ce Niumi Spot est déjà enregistré. | Réessayer |
| iPhone, feuille fermée après 60 secondes | Le temps de scan est écoulé. Relance le scan une fois devant ton Niumi Spot. | Réessayer, et un lien « Annuler » qui referme la feuille |

L'affichage de ces échecs n'est pas dessiné pour cet écran **(ouvert)** ; le cas « déjà enregistré » a été ajouté le 19 octobre 2026. Sur l'écran 10, ils s'affichent dans une feuille du bas (section 14).

Jusqu'à dix Niumi Spots peuvent être enregistrés (décision du 19 octobre 2026). Ce parcours sert aussi à en ajouter un depuis les réglages, à tout moment, et pendant l'alarme depuis l'écran 8 (section 13).

## 7. Écran 3 bis · Temps pour sortir du lit

Image : `11_ecran-3-bis-temps-pour-sortir-du-lit`. Thème clair.

Niumi demande à l'utilisateur combien de temps il met aujourd'hui pour sortir du lit. La réponse est un nombre de minutes, gardé sur le téléphone, sans compte. Elle sert à l'écran 11.

| Élément | Contenu |
| --- | --- |
| Titre | Combien de temps mets-tu pour sortir du lit ? |
| Texte | Compte à partir de la première sonnerie. Une estimation suffit. |
| Roue | Onze durées : 1, 2, 3, 4, 5, 10, 15, 20, 30 et 45 minutes, puis « 1 heure et + ». Celle du milieu, affichée en gros, est la réponse |
| Mention du bas | Niumi s'en servira pour te montrer le temps que tu gagnes. |
| Bouton | Continuer |

- La question est obligatoire : on ne peut pas la passer.
- La valeur se corrige ensuite dans Réglages, Aide, rubrique « Temps gagné ».
- « 1 heure et + » compte pour 60 minutes dans le calcul (décision du 24 octobre 2026).
- La maquette montre « 15 min » au milieu. La valeur présélectionnée n'a pas été décidée **(ouvert)**.

## 8. Écran 4 · Choix des applications

Images : `12_ecran-4-1-avant-le-choix`, `13_ecran-4-2-la-liste-sur-android`, `14_ecran-4-3-apres-le-choix`. Thème clair.

L'utilisateur désigne les applications qui seront bloquées à chaque session. Il en faut au moins une.

| Élément | Contenu |
| --- | --- |
| Titre | Quelles applications veux-tu bloquer ? |
| Texte | Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Spot. |
| Carte, avant le choix | Six cases vides, « Aucune application choisie », bouton « Choisir les applications » |
| Carte, après le choix | Les icônes choisies, « 6 applications choisies », bouton « Modifier » |
| Mention du bas, avant le choix | Choisis au moins une application. |
| Mention du bas, après le choix | Tu pourras changer ce choix plus tard, depuis l'accueil. |
| Bouton | Continuer. Grisé tant qu'aucune application n'est choisie |

« Choisir les applications » et « Modifier » ouvrent la liste :

| | iPhone | Android |
| --- | --- | --- |
| Liste | Celle du système (FamilyActivityPicker). Elle propose des catégories, des applications et des sites web | Une page entière dessinée par Niumi (feuille du bas sur la maquette 13, remplacée le 22 octobre 2026) |
| Contenu de la liste | Imposé par le système | Titre « Applications à bloquer », une croix, un champ « Rechercher », les icônes sur quatre colonnes avec une coche sur celles choisies, le compteur, le bouton « Valider ». En tête, la section « Applications déjà choisies », puis « Toutes les applications ». Une croix vide le champ de recherche quand il contient du texte |
| Règles | — | La croix et le retour ferment sans enregistrer les changements. « Valider » reste grisé tant qu'aucune application n'est cochée |

Décisions du 22 octobre 2026 :

- La carte montre la sélection enregistrée par « Valider », jamais les coches en cours dans la liste. Avec une seule application : « 1 application choisie ».
- Six emplacements au plus sur la carte. Moins de six applications : seulement leurs icônes, centrées. Plus de six : cinq icônes puis un emplacement « +N ». Sur un écran trop étroit, moins d'emplacements, avec la même règle.
- Liste Android : le compteur suit les coches en cours (« Aucune application choisie » sans coche). À 50 applications cochées, les autres sont grisées et ne se cochent plus, et le compteur devient « 50 applications choisies, c'est le maximum. ». Recherche sans résultat : « Aucune application ne correspond. » Aucune application proposable : « Aucune application à bloquer sur ce téléphone. » Pendant le chargement, un cercle animé tient la place de la carte, puis de la grille, sans texte (TalkBack dit « Chargement des applications ») ; il n'apparaît qu'après 300 ms.
- Liste Android en pleine page, et non en feuille du bas : la feuille, à peine plus courte que l'écran, ne montrait que quatre rangées. La croix reste à droite du titre.
- Section « Applications déjà choisies » en tête de la liste : les applications cochées à l'ouverture, pour les retrouver et les décocher sans parcourir toute la liste. Suit « Toutes les applications », sans elles. Une application cochée ou décochée pendant la visite reste à sa place ; la section se met à jour à l'ouverture suivante. Sans application choisie, la liste n'a pas de titre de section.

Sur iPhone, Niumi ne reçoit que des valeurs opaques : il peut afficher l'icône et le nom de chaque choix et compter les choix, mais pas lire les noms.

« Continuer » mène à l'accueil, dans l'état « Réveil désactivé ». Ce même écran sert ensuite depuis l'accueil, ligne « Applications bloquées », pour modifier la sélection. Le retour vers l'accueil n'y est pas dessiné ; M11, tranché le 25 octobre 2026 (décision de Mehdi) : ouvert depuis l'accueil, l'écran porte une flèche de retour en haut à gauche, et « Continuer », la flèche et le retour du téléphone ramènent à l'accueil. La sélection y est modifiable tant que le blocage n'a pas commencé ; du coucher au scan, la ligne est verrouillée (section 9).

Points à vérifier, détaillés dans `POINTS-OUVERTS.md` : limite de 50 applications sur iPhone, compteur quand une catégorie est choisie. L'autorisation de lister les applications sur Android est résolue (T9).

## 9. Écran 6 · Accueil

Images : `15_ecran-6-reveil-desactive-arrivee-apres-l-ecran-4`, `16_ecran-6-reveil-prevu`, `17_ecran-6-session-en-cours-sombre`, `18_ecran-6-reveil-prevu-alarme-unique`, `19_ecran-6-selecteur-d-heure-onglet-coucher`, `20_ecran-6-selecteur-d-heure-onglet-reveil` (maquettes 15 à 17 mises à jour et 18 à 20 ajoutées le 25 octobre 2026). La maquette 17 régénérée ce jour-là écrit « Annuler la session » : c'est une régression, l'app garde « Annuler le réveil » (D23, `POINTS-OUVERTS.md` §5).

L'accueil récapitule le prochain réveil et permet de modifier tout ce qui peut l'être. Il a trois états.

### Ce que contient l'écran, de haut en bas

1. **Un cadran de 24 heures**, avec « Coucher 22:30 » et « Réveil 7:00 » écrits à l'extérieur, et au centre un libellé et une valeur. Deux poignées, une pour le coucher et une pour le réveil, se déplacent sur le cadran pour régler les heures. Un toucher bref ouvre le sélecteur d'heure. Un petit point marque l'heure actuelle. Ajouts du 2026-10-25 (décision de Mehdi du 2026-10-25, après essai sur appareil) : une petite flèche, à l'intérieur de l'anneau, pointe vers ce point ; sur l'arc de la nuit, le point est blanc, sinon il disparaissait sur l'arc gris de « Réveil désactivé ». Les deux étiquettes ne recouvrent jamais l'anneau ni l'une l'autre : faute de place, elles glissent le long de l'anneau en gardant l'ordre de leurs poignées. Valeurs dans `VALEURS-DE-DESIGN.md` §5.
2. **Une carte d'état**, avec l'interrupteur général.
3. **Une ligne « Jours de réveil »** au-dessus des pastilles : le titre à gauche, en 16, comme ceux des lignes du bas, et les deux onglets « Ponctuel » et « Quotidien » à droite, dans une capsule grise (onglets ajoutés le 25 octobre 2026, maquette 18 ; titre, onglets à droite et capsule décidés par Mehdi le 26 octobre 2026 sur un croquis, non dessinés dans les maquettes : les maquettes 15 à 18 et 24 gardent les onglets soulignés et centrés, sans titre ; `POINTS-OUVERTS.md` M20). L'onglet choisi est un segment plein à l'encre, texte blanc, comme la barre des durées de la sonnerie et les jours cochés (choix de Claude, à valider). Le titre lève une ambiguïté relevée par Mehdi : quand la période passe minuit, on ne savait pas si l'on cochait le soir du coucher ou le matin du réveil.
4. **Sept pastilles**, une par jour : L M M J V S D. Toucher une pastille coche ou décoche le jour. Un jour coché est un jour de réveil : le coucher a lieu la veille quand son heure est plus tardive que celle du réveil (section 2). Avec lundi à vendredi cochés et 22:30 → 7:00, le blocage va du dimanche soir au jeudi soir.
5. **La ligne « Applications bloquées »** : les icônes des applications, puis « +N » quand elles ne tiennent plus sur la ligne ; à droite, le nombre et un chevron. Elle ouvre l'écran 4.
6. **La ligne « Sonnerie »** : à droite, le nom du son et, dessous, « Progressive · 2 min », puis un chevron. Elle ouvre le choix de la sonnerie (section 10). Dans l'app, aucun trait sous cette dernière ligne (décision de Mehdi du 2026-10-25, après essai sur appareil).
7. **La barre d'onglets**, tout en bas, bord à bord, avec quatre onglets : « Réveil », choisi ; « Blocage seul » et « Suivi », grisés, avec un badge « Bientôt » posé à l'horizontale en travers de leur icône, qui n'ouvrent rien et sont lus « Blocage seul, bientôt disponible » et « Suivi, bientôt disponible ». Touchés, ils tremblent brièvement de gauche à droite et le téléphone vibre, pour montrer que le toucher a été reçu mais refusé (décision de Mehdi du 25 octobre 2026, effet choisi par Claude) ; « Réglages », qui ouvre l'écran 12. Elle est la même dans les trois états et reste utilisable pendant la session, sombre comme l'écran. Le contenu défile sous elle : le haut de son fond est un peu transparent et laisse deviner ce qui passe derrière, pour montrer qu'il reste à faire défiler ; ses icônes et ses textes restent pleins (demande de Mehdi du 25 octobre 2026). Une fois en bas de page, la dernière ligne remonte au-dessus de la barre. Un toucher sur la barre ne traverse jamais jusqu'au contenu caché. La bulle du bas se pose au-dessus de la barre. Ce que feront « Blocage seul » et « Suivi » n'est pas décidé (`POINTS-OUVERTS.md`, D24).

La barre d'onglets remplace l'icône des réglages, qui était en haut à droite (décision de Mehdi du 25 octobre 2026, nouvelle maquette 16). Les maquettes 15 à 18 et 24 ont été mises à jour le même jour : icône retirée, barre ajoutée, espaces et flèche de l'heure actuelle de l'app. La version sombre de la barre (maquettes 17 et 24) n'a pas été dessinée par Mehdi : elle reprend les couleurs du thème sombre (`VALEURS-DE-DESIGN.md` §4, à valider, `POINTS-OUVERTS.md` M17).

Dans l'app, le cadran descend un peu sous le haut de l'écran et les espaces entre les éléments sont plus serrés que dans les maquettes ; l'écran défile sur les petits écrans (décision de Mehdi du 2026-10-25, après essai sur appareil : « réduire le vide » ; valeurs dans `VALEURS-DE-DESIGN.md` §4).

Le planning (interrupteur, jours, heures) commande le cadran, la carte, les onglets et les pastilles. La session en cours commande le verrouillage et le thème.

### Les trois états

| | Réveil prévu | Réveil désactivé | Session en cours |
| --- | --- | --- | --- |
| Image | 16, et 18 en ponctuel | 15 | 17 |
| Quand | Interrupteur activé, avant l'heure du coucher | Interrupteur éteint | De l'heure du coucher jusqu'au scan |
| Thème | Clair | Clair | Sombre |
| Centre du cadran | « Réveil dans » puis « 9 h 26 » | « Réveil » puis « Désactivé » | « Réveil dans » puis « 7 h 50 » |
| Cadran | Arc noir, poignée du coucher blanche, poignée du réveil ambre | Arc gris, deux poignées blanches | Pas de poignées. L'arc se remplit d'ambre au fil de la nuit ; devant le point de l'heure actuelle, l'ambre s'efface sur 48 minutes (12°), plus court que dans la maquette (voir `VALEURS-DE-DESIGN.md` §5) |
| Carte | Noire. « Réveil activé », « Blocage des apps dans 56 min », « 8 h 30 de sommeil » (« 24 h de blocage » pour deux heures identiques), interrupteur ambre | Blanche, au trait. « Réveil désactivé », « Aucun réveil ni blocage prévu », interrupteur gris | « Blocage en cours » avec un cadenas ambre, « Applications bloquées jusqu'au scan de ton Niumi Spot », lien « Annuler le réveil ». Pas d'interrupteur |
| Onglets | « Ponctuel » ou « Quotidien » en segment encre selon les jours cochés, aucun pour des jours libres | Affichés en grisé, mais modifiables : segment choisi gris, comme un jour coché grisé | Grisés et verrouillés |
| Titre « Jours de réveil » | À l'encre | Grisé, comme le titre « Applications bloquées » | Grisé, comme le titre « Applications bloquées » |
| Jours, heures, applications | Modifiables | Affichés en grisé, mais modifiables | Grisés et verrouillés, avec un cadenas sur la ligne des applications |
| Sonnerie | Modifiable | Affichée en grisé, mais modifiable | Modifiable, non grisée |

- **Réveil désactivé** est aussi l'état d'arrivée à la fin de la mise en route. Les valeurs par défaut sont affichées, tout reste modifiable, et aucun message d'aide n'est ajouté. Rien ne se bloque et rien ne sonne tant que l'interrupteur n'est pas activé.
- **L'interrupteur général** désactive tout le planning : plus aucun réveil ni blocage tant qu'on ne le rallume pas.
- **Interrupteur allumé, mais aucun réveil n'a pu être programmé** (Q14, décision de Mehdi du 25 octobre 2026, jusqu'à la fenêtre d'erreur de l'étape 52 du plan Android) : la carte reste noire avec « Réveil activé » et l'interrupteur allumé, mais sa deuxième ligne dit « Aucun réveil ni blocage prévu » ; le centre du cadran affiche « Réveil » sans compte à rebours. L'accueil ne montre jamais un faux « Réveil prévu ». Cet état n'est pas dessiné.
- **Le point de l'heure actuelle et les comptes à rebours** (« Réveil dans », « Blocage des apps dans ») avancent à chaque minute (Q13, décision du 25 octobre 2026). À partir d'un jour, les comptes à rebours s'écrivent « 1 j 4 h » (section 2) : avec des jours cochés, le prochain réveil peut être à plusieurs jours.
- **Pendant une session**, toucher un champ verrouillé (heure, jour, onglet, ligne des applications) affiche : « Modifiable après avoir scanné ton Niumi Spot. » Forme décidée le 25 octobre 2026 (M10, décision de Mehdi) : une bulle encre en bas de l'écran, de la forme du message de la maquette 27 mais sans coche, lue par le lecteur d'écran dès qu'elle paraît ; elle disparaît après 4 secondes ou au toucher suivant.
- Il n'est pas possible de modifier une session en cours. La seule action est « Annuler le réveil » (section 11).

### Ponctuel et Quotidien

Image : `18_ecran-6-reveil-prevu-alarme-unique`. Décisions de Mehdi du 25 octobre 2026.

| Onglet souligné | Jours cochés | Réveil |
| --- | --- | --- |
| Ponctuel | Aucun | Une seule fois, à la prochaine heure de réveil : aujourd'hui si elle est encore à venir, sinon le lendemain, quel que soit le jour. Après le scan du matin (ou la recopie du matin), l'interrupteur s'éteint |
| Quotidien | Les sept | Tous les jours |
| Aucun | Certains seulement (jours libres, par exemple du lundi au vendredi, maquette 16) | Les jours cochés |

- Toucher « Quotidien » coche les sept jours. Toucher « Ponctuel » les décoche tous.
- Cocher ou décocher une pastille depuis l'un des deux onglets passe en jours libres, sauf quand on décoche le dernier jour : le réveil devient ponctuel. « Aucun jour coché » n'est donc jamais une erreur (D17, tranché le 25 octobre 2026).
- Les onglets s'écrivent en 14, au texte secondaire ; l'onglet choisi en 500, texte blanc sur un segment encre (`VALEURS-DE-DESIGN.md` §3, §4). Jusqu'au 26 octobre 2026, il était à l'encre et souligné, comme dans les maquettes.

### Le sélecteur d'heure

Images : `19_ecran-6-selecteur-d-heure-onglet-coucher`, `20_ecran-6-selecteur-d-heure-onglet-reveil`. Décisions de Mehdi du 25 octobre 2026.

Une fenêtre centrée par-dessus l'accueil assombri.

| Élément | Contenu |
| --- | --- |
| Croix | En haut à droite, « Fermer » |
| Onglets | « Coucher » et « Réveil », l'onglet ouvert souligné. Passer d'un onglet à l'autre garde le choix en cours |
| Roues | Une roue des heures, de 0 à 23 sans zéro devant, et une roue des minutes, de « 00 » à « 59 », séparées par « : ». La valeur du milieu, en gros entre deux traits, est la réponse ; les roues tournent sans fin |
| Phrase | Suit le choix en direct. Onglet Coucher : « 8 h 30 de sommeil avant le réveil de 7:00 ». Onglet Réveil : « 8 h 30 de sommeil après le coucher de 22:30 ». Pour deux heures identiques : « 24 h de blocage jusqu'au réveil de 7:00 » ou « 24 h de blocage depuis le coucher de 22:30 » (textes non dessinés, à valider par Mehdi) |
| Bouton | Valider : enregistre les deux heures |

- Toucher la poignée du coucher ouvre l'onglet Coucher ; toucher ailleurs sur le cadran, poignée du réveil comprise, ouvre l'onglet Réveil.
- La croix, un toucher sur l'accueil assombri et le retour du téléphone ferment la fenêtre sans rien changer.
- Toutes les heures sont acceptées, à la minute près ; deux heures identiques forment une période de 24 h (section 2).
- Le titre de la fenêtre, pour le lecteur d'écran, est « Heure de coucher » ou « Heure de réveil » ; les roues s'annoncent « Heures » et « Minutes » (ces deux libellés, absents des maquettes, sont à valider par Mehdi).

### Les gestes du cadran

- **Glisser** : poser le doigt sur une poignée et bouger la déplace dès le premier mouvement, sans appui long, de 5 en 5 minutes. L'heure enregistrée est celle du relâchement.
- La poignée glissée ne passe jamais par-dessus l'autre. Du côté de la nuit, elle s'arrête au dernier cran avant elle ; du côté du jour, elle peut la rejoindre, ce qui fait une période de 24 h.
- **Toucher bref** : relâcher sans avoir bougé ouvre le sélecteur d'heure (ci-dessus).
- Pour le lecteur d'écran, chaque poignée est un réglage qui avance ou recule de 5 minutes, et une action « Choisir l'heure » ouvre le sélecteur (libellé à valider par Mehdi).
- Pendant une session, il n'y a pas de poignées : toucher le cadran affiche la bulle « Modifiable après avoir scanné ton Niumi Spot. ».

### Confirmation avant un blocage immédiat

Décision de Mehdi du 25 octobre 2026 (section 2). Non dessinée. Textes revus par Mehdi le même jour, après un premier essai : le titre dit ce qui arrive aux applications, le texte en donne la raison.

- **Quand** : un changement fait depuis l'accueil, interrupteur allumé, lancerait le blocage tout de suite, parce que l'heure actuelle est déjà dans la période coucher → réveil du planning modifié. Gestes concernés : allumer l'interrupteur, cocher ou décocher une pastille, toucher « Ponctuel » ou « Quotidien », relâcher une poignée, « Valider » du sélecteur d'heure. Éteindre l'interrupteur ou un changement qui laisse la période à venir n'ouvre rien. « Valider » de l'écran 4 n'est pas concerné.
- **Forme** : fenêtre centrée sur l'accueil assombri, comme le sélecteur d'heure, sans croix. Icône du cadenas dans un rond gris.

| Élément | Contenu |
| --- | --- |
| Titre | Tes applications seront bloquées dès maintenant |
| Texte | Ton coucher de 22:30 est déjà passé. Tes applications resteront bloquées jusqu'au scan de ton Niumi Spot. L'heure est celle du coucher demandé par le changement, écrite comme sur le cadran (« 7:00 ») |
| Bouton | Bloquer maintenant |
| Lien | Annuler |

- « Bloquer maintenant » applique le changement : l'accueil passe en « Blocage en cours ».
- « Annuler », un toucher sur le voile et le retour du téléphone ne changent rien : l'interrupteur reste éteint, la pastille garde son état, la poignée revient à son heure.

## 10. Choix de la sonnerie

Images : `21_sonnerie-la-feuille`, `22_sonnerie-quand-on-touche-le-volume`, `23_sonnerie-progressive-desactivee` (maquettes redessinées le 23 octobre 2026 : l'ancienne pastille « Non » est devenue un interrupteur ; renumérotées de 18-20 à 21-23 le 25 octobre 2026). Thème clair dans les maquettes.

Une feuille monte du bas par-dessus l'accueil quand on touche la ligne « Sonnerie ». Elle reste accessible pendant une session ; sa version sombre n'est pas dessinée : sur Android, c'est la même feuille sous la palette sombre, comme la feuille de scan (M7, tranché le 23 octobre 2026).

| Élément | Contenu |
| --- | --- |
| Titre | Sonnerie, avec une croix pour fermer |
| Section « Son » | Les sons en liste. Chaque ligne a un bouton rond pour choisir et un bouton pour écouter. Sur Android, les quatre sons déjà empaquetés (Cloche, Énergique, Oiseaux, Piano ; D7, tranché le 23 octobre 2026) |
| Ligne « Volume du téléphone » | « Volume du téléphone » à gauche, « 80 % » à droite. Pas de curseur. Placée avant la sonnerie progressive depuis le 24 octobre 2026 : la sous-ligne de celle-ci renvoie à ce volume |
| Ligne « Sonnerie progressive » | Un interrupteur à droite ; dessous le libellé, « Monte jusqu’au volume du téléphone en 2 min » quand il est allumé (texte revu le 24 octobre 2026, « Volume max en 2 min » auparavant), rien quand il est éteint (D6, tranché le 23 octobre 2026) |
| Barre des durées | Sous la ligne, seulement quand l'interrupteur est allumé : une barre segmentée, le choix en noir. La maquette montre 1 min, 2 min, 5 min, 10 min ; sur Android, 2, 5, 10 et 15 min (Q7, tranché le 23 octobre 2026 : liste mesurée le 8 octobre, 1 min jugée trop rapide) |

- Les noms des sons dans les maquettes (Aube, Carillon, Oiseaux, Classique, Insistante) sont des exemples. Les vrais sons restent à choisir **(ouvert, sauf sur Android : D7)**.
- La sonnerie progressive monte jusqu'au volume maximal en 1, 2, 5 ou 10 minutes (2, 5, 10 ou 15 sur Android), ou ne monte pas quand l'interrupteur est éteint.
- Sur Android, la vibration suit la sonnerie progressive : des impulsions espacées au départ, qui se resserrent jusqu'au rythme plein à la durée choisie. Sans sonnerie progressive, elle vibre au rythme plein dès le départ (décision de Mehdi du 26 octobre 2026).
- Allumer l'interrupteur choisit 10 min, et 10 min est aussi le réglage d'une installation neuve (décision du 23 octobre 2026, qui remplace les 2 min du 30 septembre).
- Niumi affiche le volume du téléphone mais ne permet pas de le modifier, sur iPhone comme sur Android.
- Sur Android, le pourcentage suit les changements faits pendant que la feuille est ouverte (touches ou panneau de volume ; décision du 24 octobre 2026). Sur iPhone, voir T2.
- Toucher la ligne du volume affiche une bulle noire au-dessus d'elle : « Il se règle dans les réglages de ton téléphone. » La bulle disparaît quand on touche ailleurs. Aucun signe n'indique que la ligne se touche.
- Sur l'accueil, quand l'interrupteur est éteint, la ligne n'affiche que le nom du son (D5, tranché le 23 octobre 2026).

Contraintes vérifiées :

- **Android.** Une application peut lire le volume d'alarme du téléphone.
- **iPhone.** Avec AlarmKit, l'alarme suit le volume « Sonnerie et alertes » du téléphone et l'app ne peut pas le régler. La seule valeur qu'une app peut lire est décrite comme le volume de sortie du système, et des développeurs signalent qu'elle n'est pas toujours à jour depuis iOS 18. Si la lecture n'est pas fiable, l'iPhone affichera la ligne sans le pourcentage **(ouvert : à tester)**.
- **iPhone.** Un son personnalisé d'AlarmKit doit durer moins de 30 secondes. Une montée sur plusieurs minutes ne tient donc pas dans un seul fichier **(ouvert : à tester)**.

## 11. Annuler le réveil

Images : `24_annuler-1-le-lien-sur-l-accueil`, `25_annuler-2-scanner-ou-sortie-de-secours`, `26_annuler-3-la-feuille-de-scan`, `27_annuler-4-session-annulee-retour-au-clair`. Thème sombre, sauf la dernière. Les sources régénérées le 25 octobre 2026 disent de nouveau « session » (« Annuler la session », « Session annulée… ») : c'est une régression, les textes ci-dessous font foi (D23, `POINTS-OUVERTS.md` §5).

Pendant une session, l'utilisateur peut l'annuler en scannant un Niumi Spot.

1. Sur l'accueil en session, il touche le lien « Annuler le réveil ».
2. Une feuille monte du bas. Dans l'app, son titre et son texte sont centrés et le titre est un peu plus petit que dans la maquette (décision de Mehdi du 26 octobre 2026, `POINTS-OUVERTS.md` M19) :

| Élément | Contenu |
| --- | --- |
| Titre | Annuler le réveil ? |
| Texte | Scanne ton Niumi Spot pour annuler. Tes applications seront débloquées et ton réveil désactivé. |
| Bouton | Scanner le Niumi Spot |
| Lien discret | Je n'ai pas accès à mon Niumi Spot |

3. « Scanner le Niumi Spot » ouvre la feuille de scan, avec la phrase « Approche ton iPhone du Niumi Spot pour annuler le réveil » (« ton téléphone » sur Android).
4. Après le scan, le temps que Niumi lève le blocage, la feuille de scan ne montre qu'un cercle de chargement, sans texte ; le lecteur d'écran dit « Annulation du réveil » (Android, décision de Mehdi du 26 octobre 2026 ; non dessiné). Puis l'accueil revient en clair, dans l'état « Réveil désactivé », avec un message en bas de l'écran : « Réveil annulé. Tes applications sont débloquées. » C'est la bulle des champs verrouillés (§9, M10), sans la coche ni le gras de la maquette 27 ; elle disparaît après 4 secondes ou au toucher suivant (décision de Mehdi du 25 octobre 2026, `POINTS-OUVERTS.md` M18).

Effets de l'annulation : le blocage est levé et l'interrupteur « Réveil activé » est éteint. Plus aucune alarme ni blocage n'est prévu tant qu'on ne le rallume pas.

Le lien « Je n'ai pas accès à mon Niumi Spot » ouvre la sortie de secours (section 13).

Cette étape intermédiaire est identique sur iPhone et sur Android. Elle existe parce que la feuille de scan de l'iPhone n'accepte pas de lien.

## 12. Écran 7 · Écran de blocage

Image : `28_ecran-7-ecran-de-blocage`. Thème sombre.

Il s'affiche quand l'utilisateur ouvre une application bloquée.

| Élément | Contenu |
| --- | --- |
| Icône | Le logo niumi |
| Titre | Cette application est bloquée |
| Texte | Elle reviendra après avoir scanné ton Niumi Spot. |
| Bouton principal | Fermer |
| Bouton secondaire | Ouvrir Niumi |

- **iPhone.** Cet écran appartient au système. Niumi peut en régler la couleur de fond, l'icône, le titre, le texte, les libellés des boutons et la couleur du bouton principal, mais pas la disposition. La taille du logo dans l'emplacement de l'icône est à tester sur un appareil **(ouvert)**.
- **iPhone.** « Ouvrir Niumi » n'est pas réalisable directement : un bouton de cet écran peut seulement le fermer. Le seul contournement connu est d'envoyer une notification que l'utilisateur touche pour ouvrir Niumi. Reste à décider si on garde ce bouton sur iPhone **(ouvert)**.
- **Android.** L'écran est entièrement dessiné par Niumi. « Ouvrir Niumi » mène à l'accueil. Le titre nomme l'application : « L’application YouTube est bloquée » (« Cette application est bloquée » si son nom manque), accordé avec « application » pour rester juste quel que soit le nom, et le texte devient « Tu pourras l’ouvrir après avoir scanné ton Niumi Spot. », sans pronom qui donnerait un genre au nom (décision de Mehdi du 23 octobre 2026). Reste à savoir si l'iPhone peut afficher ce nom (T13). Il reste affiché jusqu'à « Fermer » ou « Ouvrir Niumi » ; il disparaît aussi de lui-même quand le blocage prend fin (scan), quand l'écran du téléphone s'éteint, pour ne pas réapparaître par-dessus l'écran de verrouillage, et quand une autre application non bloquée passe devant, un appel entrant par exemple (décisions de Mehdi du 22 octobre 2026). Le logo et le texte sont plus écartés que sur la maquette : 88 entre le logo et le titre au lieu de 32, et 16 de plus au-dessus du logo, l'ensemble restant centré (décision de Mehdi du 23 octobre 2026, après essais sur le téléphone).

## 13. Écrans 8 et 9 · Sortie de secours

Images : `29_ecran-8-confirmation-le-soir`, `30_ecran-8-confirmation-le-matin`, `31_ecran-9-recopie-en-cours`, `32_ecran-9-erreur-signalee`, `33_ecran-9-texte-complet-bouton-actif`. Thème sombre. Les sources régénérées le 25 octobre 2026 disent de nouveau « session » et reprennent l'ancien texte à recopier : c'est une régression, les textes ci-dessous font foi (D23, `POINTS-OUVERTS.md` §5).

La sortie de secours remplace le scan d'un Niumi Spot, au prix d'un long texte à recopier. Elle s'ouvre par le lien « Je n'ai pas accès à mon Niumi Spot », présent sur la feuille d'annulation, sur l'écran de réveil et sur les feuilles d'échec du scan.

### Écran 8 · Avertissement

Une feuille monte du bas, par-dessus l'accueil en session le soir, par-dessus l'écran de réveil le matin.

| Élément | Contenu |
| --- | --- |
| Titre | Arrêter le réveil sans scanner ton Niumi Spot ? |
| Texte, le soir ou la nuit | Recopier un long texte remplace le scan de ton Niumi Spot. |
| Texte, pendant l'alarme | L'alarme s'arrêtera et tes applications seront débloquées. Pour sortir, tu devras recopier un long texte. |
| Bouton principal | Garder le réveil |
| Bouton secondaire, au trait | Recopier le texte |
| Troisième bouton, au trait, pendant l'alarme seulement | Ajouter un Niumi Spot |

Le bouton principal est celui qui fait rester. Sortir demande un geste volontaire sur le second.

**Pendant l'alarme, l'écran 8 propose aussi d'ajouter un Niumi Spot** (décision du 19 octobre 2026) : le parcours de l'écran 3 s'ouvre par-dessus l'écran de réveil (scan, puis choix de la pièce), puis l'écran 10 revient avec la feuille de scan ouverte, et le nouveau Niumi Spot arrête l'alarme comme les autres. L'option est un troisième bouton au trait, sous « Recopier le texte » : « Ajouter un Niumi Spot ». Pas de maquette dédiée : la feuille de l'écran 8 et ce texte suffisent (décision du 19 octobre 2026). Le soir, l'ajout passe par les réglages ; le bouton n'est pas proposé sur l'écran 8 du soir.

### Écran 9 · Recopie

| Élément | Contenu |
| --- | --- |
| En haut à gauche | Lien « Annuler » |
| En haut à droite | Compteur : « 84 sur 190 caractères » |
| Consigne | Recopie ce texte pour arrêter le réveil. |
| Texte modèle | Affiché en entier dans un encadré, au-dessus du champ |
| Champ de saisie | Vide au départ. Le collage est désactivé |
| Bouton, au-dessus du clavier | Arrêter le réveil. Inactif tant que le texte n'est pas complet et juste |
| Message d'erreur | Un mot ne correspond pas. Il est signalé dans le texte. |

- **Erreur.** Le premier mot qui diffère est surligné dans le texte modèle et souligné dans la saisie, et le message d'erreur s'affiche sous le champ.
- **Comparaison.** Les majuscules, les accents, la ponctuation et les espaces en double sont ignorés.
- **Abandon.** « Annuler » ramène à l'écran d'où l'on vient. La saisie est perdue.
- **Le texte change à chaque sortie**, pour qu'un raccourci clavier ne suffise pas. Un seul texte est validé à ce jour (190 caractères). Il a été revu le 24 octobre 2026 : « arrêter mon réveil » remplace « sortir de cette session », et le texte passe de 196 à 190 caractères (les anciennes maquettes disaient 197, à tort). D'autres textes neutres du même genre seront choisis plus tard **(ouvert)**. Tant qu'il est seul, le texte ne change pas d'une sortie à l'autre (décision du 24 octobre 2026) :

> J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choisis d'arrêter mon réveil sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.

Le même texte sert le soir et le matin : il ne doit donc mentionner ni « ce soir » ni une heure.

**Après la recopie**, la session se termine comme après un scan.

- Le soir ou la nuit : le blocage est levé, l'interrupteur « Réveil activé » est éteint, et l'accueil affiche le message « Réveil annulé. Tes applications sont débloquées. »
- Le matin, pendant l'alarme : la sonnerie s'arrête et les applications sont débloquées. La session se termine comme après un scan : l'écran 11 s'affiche, sans la phrase « Bravo ! … de moins qu'avant Niumi. », et le réveil reste activé pour le lendemain (décision du 24 octobre 2026), sauf un réveil ponctuel, dont l'interrupteur s'éteint (décision de Mehdi du 25 octobre 2026).
- Sur iPhone, ce que fait l'alarme pendant la recopie n'est pas décidé non plus **(ouvert)**.

**Android, le matin.** La sonnerie baisse tant que la saisie avance et remonte si elle s'arrête.

## 14. Écran 10 · Réveil

Images : `34_ecran-10-le-reveil-sonne` à `41_ecran-10-iphone-seulement-temps-de-scan-ecoule`. Thème sombre.

L'écran ne dit qu'une chose : va scanner ton Niumi Spot. Il n'a aucun bouton d'arrêt ni de report. Pendant que l'alarme sonne, c'est lui qui s'affiche, pas l'accueil, dès que Niumi est ouvert — par la notification ou par son icône. L'écran n'est pas imposé par-dessus le verrouillage (décision du 18 octobre 2026).

| Élément | Contenu |
| --- | --- |
| Date | Vendredi 2 octobre |
| Heure | L'heure actuelle, en très grand |
| Dessin | Le téléphone approché du Niumi Spot |
| Consigne | Rejoins ton Niumi Spot |
| Bouton | Scanner le Niumi Spot |
| Lien discret | Je n'ai pas accès à mon Niumi Spot |

La consigne était « Scanne ton Niumi Spot » dans les maquettes. Elle est remplacée le 22 octobre 2026 (décision de Mehdi) : le téléphone ne lit le Niumi Spot qu'après « Scanner le Niumi Spot », et la consigne ne doit pas laisser croire qu'on peut scanner tout de suite. Elle parle donc du déplacement. La règle vaut pour l'iPhone et pour Android.

L'écran n'indique pas la pièce, car plusieurs Niumi Spots peuvent être enregistrés. Le lien ouvre la sortie de secours.

### La feuille de scan (image 35)

« Scanner le Niumi Spot » ouvre la feuille « Prêt à scanner », la même qu'à l'écran 3 : « Approche ton iPhone du Niumi Spot » sur iPhone, « Approche ton téléphone du Niumi Spot » sur Android, et le bouton « Annuler ».

Sur iPhone, cette feuille est celle du système. Son apparence pendant une session, claire ou sombre, est à regarder sur un appareil **(ouvert)**.

### Les échecs du scan (images 36 à 39)

Une feuille du bas affiche un titre, une phrase d'aide, un bouton et le lien « Je n'ai pas accès à mon Niumi Spot ».

| Cas | Titre | Phrase d'aide | Bouton |
| --- | --- | --- | --- |
| Pas un Niumi Spot | Ce n'est pas un Niumi Spot | Scanne un Niumi Spot que tu as enregistré. | Réessayer |
| Niumi Spot non enregistré | Ce Niumi Spot n'est pas enregistré | Scanne un Niumi Spot que tu as enregistré. | Réessayer |
| Lecture ratée | Ton Niumi Spot n'a pas pu être lu | Rapproche ton téléphone et réessaie. | Réessayer |
| NFC coupé (Android) | Le NFC est coupé | Active-le pour scanner ton Niumi Spot. | Ouvrir les réglages |

Aucun de ces cas n'arrête l'alarme ni ne débloque les applications.

Sur iPhone, la feuille de scan du système ne peut pas contenir de bouton. Niumi affiche donc sa propre feuille d'échec après la fermeture de celle du système.

### iPhone seulement : la feuille se ferme après 60 secondes (image 41)

Sur iPhone, une lecture NFC dure 60 secondes au plus, et l'app doit rester au premier plan. Si l'utilisateur touche « Scanner le Niumi Spot » depuis son lit et met plus d'une minute à atteindre son Niumi Spot, la feuille se ferme d'elle-même. Niumi affiche alors, dans la feuille d'échec :

| Titre | Phrase d'aide | Bouton | Lien |
| --- | --- | --- | --- |
| Le temps de scan est écoulé | Relance le scan une fois devant ton Niumi Spot. | Réessayer | Je n'ai pas accès à mon Niumi Spot |

Ce message sert pour tous les scans de l'iPhone qui dépassent 60 secondes : le matin, à l'annulation d'une session, à l'association et à l'ajout d'un Niumi Spot. À l'association et à l'ajout, le lien de secours est remplacé par un lien « Annuler », qui referme la feuille sans relancer le scan. Seule la version du matin est dessinée **(ouvert)**.

**Android n'a pas cette limite.** La feuille reste ouverte jusqu'au scan ou jusqu'à « Annuler ». C'est une différence voulue.

Il n'a pas été vérifié si l'iPhone permet de rouvrir la feuille sans geste de l'utilisateur **(ouvert : à tester)**.

### iPhone seulement : l'alarme va reprendre (image 40)

Sur iPhone, l'alarme qui sonne est d'abord présentée par le système, avec une commande d'arrêt et un bouton qui ouvre Niumi. Cette commande ne peut pas être retirée, et les boutons physiques arrêtent aussi la sonnerie en cours. Niumi relance donc l'alarme tant que le scan n'a pas eu lieu. L'arrêt ne termine pas la session et les applications restent bloquées.

Quand l'alarme a été arrêtée de cette façon, l'écran de réveil remplace sa consigne par :

| Titre | Texte |
| --- | --- |
| Ton alarme va reprendre | Scanne ton Niumi Spot pour l'arrêter. |

Le bouton et le lien ne changent pas.

Risque à tester sur un iPhone : la relance dépend de l'avertissement que le système envoie à l'app quand l'alarme est arrêtée. Un développeur signale que cet avertissement n'arrive pas toujours **(ouvert)**.

### Android

L'écran est dessiné en entier par Niumi et la sonnerie continue jusqu'au scan.

Décisions du 22 octobre 2026 (étape 38 du plan Android) :

- **Le téléphone ne lit le Niumi Spot que feuille « Prêt à scanner » ouverte**, comme sur iPhone. Approcher le téléphone sans avoir touché « Scanner le Niumi Spot » ne fait rien. Une feuille d'échec ne lit pas non plus : « Réessayer » rouvre « Prêt à scanner ».
- **Une seule consigne**, « Rejoins ton Niumi Spot », que l'alarme sonne encore ou que l'heure du réveil soit passée sans sonnerie.
- **Après le scan**, le temps que Niumi termine la session, la feuille se ferme et seules restent la date, l'heure et un cercle de chargement, sans texte ; le lecteur d'écran dit « Fin du réveil ». Le cercle n'apparaît que si l'attente dépasse 300 ms. Ce moment n'est pas dessiné.
- La feuille d'échec se ferme en touchant l'écran assombri, en la glissant vers le bas ou par le retour : sa maquette n'a pas de croix.
- **Mise en page** : la date commence 24 points sous la marge du haut, au lieu des 60 de la maquette. Tant que le lien « Je n'ai pas accès à mon Niumi Spot » n'est pas livré, sa place reste réservée sous le bouton, pour que l'écran ne bouge pas à son arrivée.
- **« Prêt à scanner »** s'écrit à l'encre (blanc sur la feuille sombre, noir sur la feuille claire), pas en gris : la maquette copie la fenêtre de l'iPhone, et le titre gris se lisait mal. Vaut pour toutes les feuilles de scan d'Android (écran 3, écran 10, annulation).

Quand l'alarme sonne, Niumi affiche une notification « Ton réveil Niumi sonne », visible sur l'écran de verrouillage. L'utilisateur déverrouille son téléphone comme d'habitude, touche la notification, et l'écran 10 s'ouvre. Le scan ne marche pas tant que le téléphone est verrouillé : c'est pour cela que l'écran n'est pas imposé (décision du 18 octobre 2026). Son texte : « Scanne ton Niumi Spot pour l'arrêter et retrouver tes applis. » Quand la session attend encore le scan sans que la sonnerie joue, la notification d'attente dit « Ton réveil Niumi est passé », « Scanne ton Niumi Spot pour retrouver tes applis. » (textes du 23 octobre 2026, sans le mot « session » ; ils remplacent ceux du 19 octobre, qui remplaçaient les versions disant « boîtier »).

## 15. Écran 11 · Réussite

Image : `42_ecran-11-reussite-retour-au-clair`. Thème clair : la session est finie.

Il confirme que le scan a marché et que les applications sont revenues. Il reste affiché jusqu'à ce que l'utilisateur le ferme.

| Élément | Contenu |
| --- | --- |
| Dessin | Un soleil qui se lève au-dessus d'une ligne d'horizon. Le soleil est en dégradé : ambre en haut, ambre très clair à l'horizon. Ce dégradé est gardé « pour l'instant » **(ouvert)** |
| Titre | Debout à 7:04 |
| Délai | 4 minutes après la sonnerie. |
| Comparaison | Bravo ! C'est 11 minutes de moins qu'avant Niumi. |
| Confirmation | Tes applications sont de nouveau accessibles. |
| Bouton | Retour à l'accueil |

- Dans l'exemple : l'alarme a sonné à 7:00 et le scan a eu lieu à 7:04. Le délai compte depuis la sonnerie réelle, même partie en retard.
- Le délai s'accorde : « Moins d'une minute après la sonnerie. », « 1 minute après la sonnerie. », « 4 minutes après la sonnerie. », puis, à partir d'une heure, comme les autres durées de l'app : « 1 h 15 après la sonnerie. » (section 2). La comparaison suit la même règle (« C'est 1 minute de moins… »). Décision de Mehdi du 25 octobre 2026 ; textes non dessinés, à valider (`POINTS-OUVERTS.md` §2).
- Si l'alarme n'a pas sonné (téléphone éteint à l'heure du réveil), l'écran dit seulement « Debout à 9:00 », sans délai ni comparaison (décision de Mehdi du 25 octobre 2026, `POINTS-OUVERTS.md` M18).
- La comparaison s'affiche seulement si l'utilisateur s'est levé plus vite que la durée déclarée à l'écran 3 bis. Dans l'exemple : 15 minutes déclarées, levé en 4 minutes, donc 11 minutes de moins. Sinon, rien ne s'affiche à la place.
- « Retour à l'accueil » ramène à l'accueil, dans l'état « Réveil prévu ». Après un réveil ponctuel, l'interrupteur s'est éteint : l'accueil est en « Réveil désactivé » (section 2, décision de Mehdi du 25 octobre 2026).
- Il n'y a pas de lien depuis cet écran vers la rubrique « Temps gagné ».

## 16. Écran 12 · Réglages

Images : `43_ecran-12-reglages` à `47_reglages-a-propos` et `53_niumi-spots-la-liste` à `56_niumi-spots-renommer-la-piece` en clair ; `48` à `52` et `57` à `60` en sombre.

Les réglages regroupent ce qu'on configure une fois. Tout ce qui décrit la prochaine nuit reste sur l'accueil. Ils s'ouvrent aussi pendant une session : ils sont alors sombres, avec les mêmes mises en page et les mêmes textes. L'écran 2, la roue de « Temps gagné » et le parcours d'ajout d'un Niumi Spot n'ont pas de version sombre dessinée **(ouvert)**.

Chaque page a une flèche de retour en haut à gauche, sauf la première page des réglages. Elle s'ouvre par l'onglet « Réglages » de la barre d'onglets et garde cette barre en bas, avec « Réglages » choisi. Son onglet « Réveil » et le retour du téléphone ramènent à l'accueil (décision de Mehdi du 25 octobre 2026).

| Ligne | À droite | Ouvre |
| --- | --- | --- |
| Niumi Spots | Le nombre de Niumi Spots (« 2 ») | La liste des Niumi Spots |
| Autorisations | « 3 sur 3 » | L'écran 2 |
| Aide | — | La liste des rubriques d'aide |
| À propos | — | La page À propos |

### Niumi Spots

| Élément | Contenu |
| --- | --- |
| Titre | Niumi Spots |
| Lignes | Un Niumi Spot par ligne, avec sa pièce et un bouton « trois points » |
| Dernière ligne | Ajouter un Niumi Spot |
| Mention | N'importe lequel de tes Niumi Spots arrête l'alarme. |

- Le bouton « trois points » ouvre un menu : « Renommer la pièce » et « Supprimer ».
- **Renommer la pièce** rouvre la feuille des pièces de l'écran 3, la pièce actuelle sélectionnée, avec le bouton « Enregistrer ». En sombre, la case choisie est blanc cassé avec un texte noir.
- **Supprimer** demande une confirmation dans une feuille du bas : titre « Supprimer le Niumi Spot « Cuisine » ? », texte « Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter. », boutons « Supprimer » et « Annuler ».
- **Il doit toujours rester au moins un Niumi Spot.** Quand il n'en reste qu'un, « Supprimer » est grisé dans le menu, avec « Tu dois garder au moins un Niumi Spot. »
- **Ajouter un Niumi Spot** reprend le parcours de l'écran 3.
- L'ajout, le renommage et la suppression restent possibles pendant une session, y compris pendant l'alarme (écran 8, section 13). Dix Niumi Spots au maximum (décision du 19 octobre 2026) : la ligne « Ajouter un Niumi Spot » reste visible mais grisée, avec la mention « Tu as déjà dix Niumi Spots. »

### Aide

Quatre rubriques : Fonctionnement, Niumi Spot perdu, Sortie de secours, Temps gagné.

- **Une page de rubrique** a un titre et des paragraphes, chacun sous un intertitre. « Fonctionnement » reprend les phrases des écrans 1 et 11 sous « Le soir », « Le matin » et « Après le scan » (image 45).
- **Niumi Spot perdu** et **Sortie de secours** ne sont pas écrites **(ouvert)**.
- **Temps gagné** (image 46) : le texte « Après chaque réveil, Niumi compare le temps que tu as mis à te lever avec celui que tu mettais avant. », puis une carte « Temps pour sortir du lit avant Niumi » avec la valeur (« 15 min ») et le bouton « Modifier », qui ouvre la roue de l'écran 3 bis. La valeur reste modifiable pendant une session.

### À propos

Le logo en grand, « Version 1.0 » dessous, puis trois liens en bas : « Mentions légales », « Politique de confidentialité », « Licences ».

- Le lien vers la politique de confidentialité est demandé par les règles de l'App Store. Le contenu des mentions légales et de la politique n'est pas écrit **(ouvert)**.
- « Licences » ouvrira une page avec la mention de droit d'auteur et le texte de la licence de la police. Cette page n'est pas dessinée **(ouvert)**. Voir `police/LISEZ-MOI.md`.
- L'éditeur et le contact ne sont pas affichés.

## 17. Hors écrans

### Notification du soir

Elle part 30 minutes avant chaque coucher et ne porte aucun bouton. La toucher ouvre l'accueil. Elle n'est pas envoyée si le réveil a été annulé.

| Élément | Contenu |
| --- | --- |
| Titre | Tu te réveilles toujours demain à 7:00 ? |
| Texte | Ton heure de coucher est programmée à 22:30, 6 applications seront bloquées. |

### Notification de session (Android)

Ajoutée le 20 octobre 2026 (décisions du 19 octobre, correctif A8). Pendant toute la session, de l'activation au scan, une notification Niumi reste affichée. Elle tient Niumi en vie : sur certains téléphones (Xiaomi), retirer Niumi des applications récentes sans elle arrêtait le blocage.

Avant l'alarme, c'est une notification discrète : sans son, sans vibration, sans bannière, sans bouton. La toucher ouvre Niumi. L'utilisateur peut l'écarter ; elle ne revient pas, sauf au coucher, où son texte change.

| Moment | Titre | Texte |
| --- | --- | --- |
| Avant le coucher | Réveil lundi à 7:00 | Tes applications choisies seront bloquées à partir de 22:30. |
| Après le coucher | Réveil prévu à 7:00 | Tes applications choisies restent bloquées jusqu'au scan de ton Niumi Spot. |

À l'alarme, elle laisse la place à la notification de l'alarme : « Ton réveil Niumi sonne » pendant la sonnerie, ou « Ton réveil Niumi est passé » si l'alarme n'a pas sonné. Une seule notification Niumi est alors visible. Elle revient une dizaine de secondes après avoir été écartée, jusqu'au scan.

Revu le 22 octobre 2026 : la notification parle du réveil, le blocage est dit dans le texte, et une seule notification reste affichée de l'alarme au scan.

Revu le 25 octobre 2026 (décision de Mehdi du 2026-10-25, après essai sur appareil) : avec le planning, un réveil peut être programmé jusqu'à sept jours à l'avance, et « Réveil programmé à 00:30 » restait affiché six jours sans dire lequel. La notification reste affichée dès la programmation (c'est elle qui protège Niumi), mais elle dit le jour du réveil et celui du blocage, chacun dans le fuseau de la programmation : rien le jour même (« Réveil à 7:00 », « … seront bloquées à partir de 22:30. »), le jour de la semaine en minuscules sinon, jamais « demain », qui deviendrait faux après minuit (« Réveil lundi à 7:00 », « Réveil dimanche à 0:30 », « Tes applications choisies seront bloquées samedi à partir de 11:45. »). Les heures s'écrivent sans zéro devant (« 7:00 », section 2). Textes non dessinés, à valider (`POINTS-OUVERTS.md` §2).

Elle n'est pas dessinée : c'est une notification standard d'Android, avec l'icône de Niumi.

### Bandeau de problème

Images : `61_bandeau-nfc-coupe-android`, `62_bandeau-aucun-niumi-spot-au-cas-ou`.

Un bandeau noir tout en haut de l'accueil, avec un symbole d'avertissement, une phrase et un lien souligné. Il reste affiché tant que le problème existe. Le cadran descend d'autant. Un seul bandeau à la fois, le plus grave d'abord.

| Problème | Texte | Lien |
| --- | --- | --- |
| NFC coupé (Android seulement) | Le scan du Niumi Spot ne fonctionnera pas : le NFC est coupé. | Activer le NFC |
| Aucun Niumi Spot | Aucun Niumi Spot n'est associé. | Associer un Niumi Spot |

Le second cas ne devrait pas arriver, puisque l'association est obligatoire au départ et que supprimer le dernier Niumi Spot est refusé. Il est gardé par précaution.

### Alerte d'autorisations

Images : `63_alerte-une-autorisation-retiree`, `64_alerte-plusieurs-autorisations-retirees`.

Si une autorisation est retirée après l'installation, une fenêtre s'affiche à chaque ouverture de l'application, centrée par-dessus l'accueil.

| Élément | Une autorisation | Plusieurs |
| --- | --- | --- |
| Titre | Il manque une autorisation | Il manque des autorisations |
| Corps | La phrase de conséquence | Une ligne par autorisation : son nom, puis sa conséquence |
| Bouton | Voir les autorisations | Voir les autorisations |
| Lien | Plus tard | Plus tard |

| Autorisation | Conséquence, seule | Conséquence, dans la liste |
| --- | --- | --- |
| Blocage des applications | Tes applications ne seront pas bloquées : l'autorisation a été retirée. | Tes applications ne seront pas bloquées. |
| Alarme | Ton réveil ne pourra pas sonner. | Ton réveil ne pourra pas sonner. |
| Notifications | On risque de ne pas pouvoir te transmettre les informations nécessaires. | On risque de ne pas pouvoir te transmettre les informations nécessaires. |

« Voir les autorisations » ouvre l'écran 2. Cette alerte remplace le bandeau pour l'alarme, le blocage et les notifications.

Le bandeau et l'alerte ne sont dessinés qu'en clair. Leur version pendant une session n'existe pas **(ouvert)**.

## 18. Sources des contraintes techniques

Ces pages ont été lues par extraits pendant la conception. Elles sont à relire avant le développement.

iPhone, alarme :

- [Programmer une alarme avec AlarmKit](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit.md), Apple
- [Questions fréquentes sur AlarmKit](https://developer.apple.com/forums/thread/797158), forum des développeurs Apple : tout bouton physique arrête l'alarme en cours, et l'app en est prévenue
- [Ce qui se passe quand on arrête une alarme AlarmKit](https://developer.apple.com/forums/thread/815064), forum Apple : un développeur signale que l'app n'est pas toujours prévenue
- [Volume de l'alarme avec AlarmKit](https://developer.apple.com/forums/thread/813519), forum Apple : l'alarme suit le volume de sonnerie du système
- [Durée des sons personnalisés avec AlarmKit](https://developer.apple.com/forums/thread/797172), forum Apple
- [AVAudioSession.outputVolume](https://developer.apple.com/documentation/avfaudio/avaudiosession/outputvolume), Apple
- [outputVolume pas toujours à jour depuis iOS 18](https://developer.apple.com/forums/thread/799104), forum Apple

iPhone, blocage des applications :

- [FamilyActivityPicker](https://developer.apple.com/documentation/familycontrols/familyactivitypicker), Apple
- [FamilyActivitySelection](https://developer.apple.com/documentation/familycontrols/familyactivityselection), Apple
- [ShieldConfiguration](https://developer.apple.com/documentation/managedsettingsui/shieldconfiguration), Apple
- [Limite de 50 applications](https://developer.apple.com/forums/thread/733361), forum Apple : témoignages de développeurs, sans réponse d'Apple
- [Ouvrir l'application depuis l'écran de blocage](https://developer.apple.com/forums/thread/719905), forum Apple

iPhone, NFC :

- [Introducing Core NFC](https://developer.apple.com/videos/play/wwdc2017/718), Apple, WWDC 2017 : lecture limitée à 60 secondes, app au premier plan
- [NFCReaderSessionProtocol](https://developer.apple.com/documentation/corenfc/nfcreadersessionprotocol), Apple
- [alertMessage](https://developer.apple.com/documentation/corenfc/nfcreadersessionprotocol/alertmessage), Apple : le texte de la feuille peut être mis à jour pendant la lecture
- [NFCReaderError](https://developer.apple.com/documentation/corenfc/nfcreadererror-swift.struct), Apple : l'app est prévenue quand la feuille est fermée ou expire

Android :

- [Alarmes exactes depuis Android 14](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms), Android
- [Autorisation QUERY_ALL_PACKAGES](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en), aide de Google Play
- [AudioManager](https://developer.android.com/reference/android/media/AudioManager), Android : lecture et réglage du volume d'un flux

Boutiques :

- [App Review Guidelines, 5.1.1 (i)](https://developer.apple.com/app-store/review/guidelines/), Apple : lien vers la politique de confidentialité dans l'app
