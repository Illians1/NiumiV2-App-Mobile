# Niumi · Spécification des écrans

État au 9 octobre 2026. Ce fichier décrit ce qui est décidé aujourd'hui, écran par écran. Il ne raconte pas comment on y est arrivé : l'historique complet est dans `annexes/journal-des-decisions.md`.

Les images citées sont dans `maquettes/images/`. Chaque image a un fichier source du même nom dans `maquettes/sources/`.

Tout ce qui n'est pas décidé, pas validé ou pas dessiné est rassemblé dans `POINTS-OUVERTS.md`. Les principaux passages concernés portent ici la mention **(ouvert)**. La liste complète est dans `POINTS-OUVERTS.md` : c'est elle qui fait référence.

## 1. Ce que fait l'app

Niumi est une app de réveil pour iPhone et Android. À l'heure du coucher, elle bloque les applications que l'utilisateur a choisies. Le matin, l'alarme sonne jusqu'à ce qu'il scanne un Niumi Point : une étiquette NFC posée dans une autre pièce que sa chambre. Le scan arrête l'alarme et débloque les applications.

Une **session** va de l'heure du coucher jusqu'au scan du matin.

## 2. Règles communes

| Sujet | Règle |
| --- | --- |
| Réveil | Récurrent, aux jours choisis, à une heure unique |
| Coucher | Le blocage démarre seul à l'heure du coucher. Si l'heure du coucher est plus tardive que l'heure du réveil, le coucher a lieu la veille du réveil. Sinon, il a lieu le jour même |
| Rappel | Notification 30 minutes avant le coucher, sans bouton |
| Avant le coucher | Tout se modifie. Un réveil s'annule en décochant le jour. Un interrupteur général désactive tout le planning |
| Du coucher au scan | Heures, jours et applications sont verrouillés. La sonnerie reste modifiable. Un scan avant l'alarme annule la session |
| Fin normale | Scan, le matin, d'un Niumi Point enregistré. N'importe lequel convient |
| Sortie de secours | Un long texte à recopier remplace le scan. Elle compte comme un scan |
| Exclu de l'app | Lancement anticipé, confirmation, essai à blanc, historique, compte. Ce sont les mots de la règle d'origine. Le mot « confirmation » ne vise pas les confirmations décidées depuis (écran 8, suppression d'un Niumi Point) **(ouvert : sens exact à confirmer)** |

Autres règles :

- **Tutoiement.** Tous les textes tutoient l'utilisateur.
- **Même fonctionnement sur iPhone et Android, autant que possible.** Exemple : la feuille de scan de l'iPhone ne peut pas contenir de lien, donc le lien « Je n'ai pas accès à mon Niumi Point » est placé avant le scan sur les deux. Les différences voulues sont signalées dans chaque écran.
- **Fonctionnement par période.** Niumi raisonne par période, du coucher au réveil. L'interrupteur s'active toujours et aucune heure n'est refusée. Si l'on se trouve déjà dans la période au moment où l'on active l'interrupteur ou où l'on déplace une poignée du cadran, le blocage commence tout de suite, sans confirmation, et l'accueil passe en « Blocage en cours » jusqu'au scan du matin. Exemple : il est 23:00 et la période va de 22:30 à 7:00 ; activer l'interrupteur lance le blocage aussitôt.
- **Clair ou sombre.** L'app est claire quand aucune session n'est en cours, sombre pendant une session. Elle ne suit pas le réglage du téléphone. Détail dans `VALEURS-DE-DESIGN.md`.
- **Valeurs par défaut.** Lundi à vendredi, réveil à 7:00, coucher à 22:30.
- **Exemple utilisé dans les maquettes.** Réveil à 7:00, coucher à 22:30, 6 applications, Niumi Points dans la salle de bain et la cuisine.
- **Écriture des heures et des durées.** « 22:30 », « 7:00 » (sans zéro devant), « 9 h 26 », « 56 min », « 8 h 30 de sommeil ».

## 3. Le parcours

Mise en route, une seule fois :

1. Écran 1 · Présentation, deux panneaux
2. Écran 2 · Autorisations
3. Écran 3 · Association du Niumi Point : consigne, scan, choix de la pièce
4. Écran 3 bis · Temps pour sortir du lit
5. Écran 4 · Choix des applications
6. Écran 6 · Accueil, dans l'état « Réveil désactivé »

L'écran 5 (premier planning) a été supprimé. Son numéro n'est pas réutilisé.

Ensuite, chaque jour :

- Avant le coucher : accueil « Réveil prévu » (clair).
- Du coucher au réveil : accueil « Session en cours » (sombre). Une application bloquée affiche l'écran 7.
- Au réveil : écran 10, puis écran 11 après le scan, puis retour à l'accueil.
- Avant l'alarme : annulation de la session par scan (section 11).
- À tout moment de la session : sortie de secours (écrans 8 et 9).

## 4. Écran 1 · Présentation du principe

Images : `01_ecran-1-panneau-1`, `02_ecran-1-panneau-2`. Thème clair.

Deux panneaux expliquent la règle avant toute configuration. On ne peut pas les passer : c'est le seul endroit où l'utilisateur apprend qu'une session démarre sans lui.

Le logo est en haut des deux panneaux. Un repère à deux traits indique le panneau en cours. Le panneau 2 a une flèche de retour vers le panneau 1.

| Panneau | Dessin | Titre | Texte | Bouton |
| --- | --- | --- | --- | --- |
| 1 | Un téléphone dont les applications sont verrouillées, avec un cadenas dans une pastille ambre | À l'heure de te coucher, tes distractions sont bloquées | À l'heure que tu choisis, Niumi bloque les applications que tu as désignées. | Continuer |
| 2 | Le téléphone approché du Niumi Point (le même dessin que sur l'écran 10) | Le matin, Niumi t'aide à te lever. | Ton alarme sonne jusqu'à ce que tu scannes le Niumi Point situé dans la pièce de ton choix. | Configurer Niumi |

## 5. Écran 2 · Autorisations

Images : `03_ecran-2-les-trois-autorisations`, `04_ecran-2-guide-ouvert-par-images-a-produire`. Thème clair.

Un seul écran présente les autorisations, une tuile par autorisation, avec une phrase d'explication avant la demande du système.

| Élément | Contenu |
| --- | --- |
| Titre | Trois autorisations |
| Texte | Niumi en a besoin pour fonctionner. Sans elles, tu ne peux pas continuer. |
| Bouton du bas | Continuer |

| Tuile | Phrase | iPhone | Android |
| --- | --- | --- | --- |
| Alarme | Pour que ton réveil sonne à l'heure, même téléphone verrouillé. | Fenêtre du système | Rien à demander en principe pour une app de réveil. L'affichage plein écran peut être retiré par l'utilisateur |
| Blocage des applications | Pour bloquer les applications que tu choisis, et seulement celles-là. | Fenêtre du système (Temps d'écran) | Service d'accessibilité à activer dans les réglages, avec guidage. Approche courante, à valider avec Google Play |
| Notifications | Pour pouvoir te tenir informé de tout ce qui est important. | Fenêtre du système | Fenêtre du système |

Comportement :

- Chaque tuile a une icône, son nom, sa phrase, un bouton « Autoriser » et un petit bouton « ? ».
- Une fois l'autorisation donnée, la tuile affiche une coche. Dans la maquette, la coche remplace à la fois « Autoriser » et « ? » : le guide n'est donc plus accessible depuis une tuile accordée **(ouvert : à confirmer)**.
- « Continuer » reste grisé tant qu'il en manque une. Les trois sont obligatoires, notifications comprises.
- En cas de refus, il ne se passe rien de particulier : la tuile n'est simplement pas affichée comme accordée.
- Si une autorisation est retirée plus tard, c'est l'alerte d'autorisations qui prévient l'utilisateur (section 17).
- Cet écran se rouvre depuis Réglages, ligne « Autorisations », pour revoir ou réparer les autorisations.

Le NFC n'a pas de tuile. Sur iPhone il n'y a rien à demander. Sur Android, s'il est coupé, l'utilisateur est prévenu au moment du scan et par le bandeau de l'accueil **(ouvert : à confirmer)**.

**Le guide ouvert par « ? »** (image 04) : une croix pour fermer, le titre (« Autoriser l'alarme »), une grande image, « Étape 1 sur 2 », une phrase (« Une fenêtre du téléphone s'ouvre. Touche « Autoriser ». »), des points de progression et le bouton « Suivant ». La mise en page est validée. Les images restent à produire, pour iPhone et pour Android, et les textes des étapes ne sont écrits que pour cet exemple **(ouvert)**.

## 6. Écran 3 · Association du Niumi Point

Images : `05_ecran-3-1-la-consigne`, `06_ecran-3-2-le-scan-iphone-et-android`, `07_ecran-3-3-le-choix-de-la-piece`, `08_ecran-3-3-cas-une-autre-piece`. Thème clair.

L'utilisateur scanne son Niumi Point une première fois, puis nomme la pièce où il le pose. Ce scan vérifie aussi que le NFC fonctionne et apprend le geste du matin.

**Temps 1, la consigne.** Le logo en haut, une flèche de retour, le Niumi Point dessiné seul et en grand, puis :

| Élément | Contenu |
| --- | --- |
| Consigne | Pose ton Niumi Point là où tu veux te rendre en premier le matin, hors de ta chambre. |
| Bouton | Scanner le Niumi Point |
| Lien discret | Je n'ai pas encore de Niumi Point |

Le lien ouvre la page web du produit, pour l'acheter.

**Temps 2, le scan.** L'écran de la consigne s'assombrit et une feuille monte du bas, sur toute la largeur.

| Élément | iPhone | Android |
| --- | --- | --- |
| Qui dessine la feuille | Le système. Niumi n'y choisit que la phrase | Niumi, à l'identique |
| Titre | Prêt à scanner | Prêt à scanner |
| Phrase | Approche ton iPhone du Niumi Point | Approche ton téléphone du Niumi Point |
| Bouton | Annuler | Annuler |

**Temps 3, le choix de la pièce.** Après le scan, une feuille monte du bas par-dessus l'écran de la consigne assombri.

| Élément | Contenu |
| --- | --- |
| Titre | Où est ton Niumi Point ? |
| Cases | Salle de bain, Cuisine, Salon, Entrée : quatre grandes cases avec une icône |
| Case large | Une autre pièce |
| Bouton | Continuer |

Rien ne dit si « Continuer » est grisé tant qu'aucune pièce n'est choisie **(ouvert)**.

« Une autre pièce » ouvre la saisie d'un nom libre dans la même feuille : la case devient un champ « Nom de la pièce », le clavier s'ouvre et la feuille remonte au-dessus de lui. Il n'y a pas d'écran de confirmation : « Continuer » mène directement à l'écran 3 bis.

**Échecs du scan.**

| Cas | Texte | Action |
| --- | --- | --- |
| Pas un Niumi Point | Ce n'est pas un Niumi Point. | Réessayer |
| Lecture ratée | Ton Niumi Point n'a pas pu être lu. Rapproche ton téléphone et réessaie. | Réessayer |
| NFC coupé (Android) | Le NFC est coupé sur ton téléphone. | Ouvrir les réglages |
| iPhone, feuille fermée après 60 secondes | Le temps de scan est écoulé. Relance le scan une fois devant ton Niumi Point. | Réessayer, et un lien « Annuler » qui referme la feuille |

L'affichage de ces échecs n'est pas dessiné pour cet écran **(ouvert)**. Sur l'écran 10, ils s'affichent dans une feuille du bas (section 14).

Plusieurs Niumi Points peuvent être enregistrés. Ce parcours sert aussi à en ajouter un depuis les réglages.

## 7. Écran 3 bis · Temps pour sortir du lit

Image : `09_ecran-3-bis-temps-pour-sortir-du-lit`. Thème clair.

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
- « 1 heure et + » compte pour 60 minutes dans le calcul **(ouvert : proposition à confirmer)**.
- La maquette montre « 15 min » au milieu. La valeur présélectionnée n'a pas été décidée **(ouvert)**.

## 8. Écran 4 · Choix des applications

Images : `10_ecran-4-1-avant-le-choix`, `11_ecran-4-2-la-liste-sur-android`, `12_ecran-4-3-apres-le-choix`. Thème clair.

L'utilisateur désigne les applications qui seront bloquées à chaque session. Il en faut au moins une.

| Élément | Contenu |
| --- | --- |
| Titre | Quelles applications veux-tu bloquer ? |
| Texte | Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Point. |
| Carte, avant le choix | Six cases vides, « Aucune application choisie », bouton « Choisir les applications » |
| Carte, après le choix | Les icônes choisies, « 6 applications choisies », bouton « Modifier » |
| Mention du bas, avant le choix | Choisis au moins une application. |
| Mention du bas, après le choix | Tu pourras changer ce choix plus tard, depuis l'accueil. |
| Bouton | Continuer. Grisé tant qu'aucune application n'est choisie |

« Choisir les applications » et « Modifier » ouvrent la liste :

| | iPhone | Android |
| --- | --- | --- |
| Liste | Celle du système (FamilyActivityPicker). Elle propose des catégories, des applications et des sites web | Une feuille du bas dessinée par Niumi |
| Contenu de la feuille | Imposé par le système | Titre « Applications à bloquer », une croix, un champ « Rechercher », les icônes sur quatre colonnes avec une coche sur celles choisies, le compteur, le bouton « Valider » |
| Règles | — | La croix ferme sans enregistrer les changements. « Valider » reste grisé tant qu'aucune application n'est cochée |

Sur iPhone, Niumi ne reçoit que des valeurs opaques : il peut afficher l'icône et le nom de chaque choix et compter les choix, mais pas lire les noms.

« Continuer » mène à l'accueil, dans l'état « Réveil désactivé ». Ce même écran sert ensuite depuis l'accueil pour modifier la sélection ; le retour vers l'accueil n'y est pas dessiné **(ouvert)**.

Points à vérifier, détaillés dans `POINTS-OUVERTS.md` : limite de 50 applications sur iPhone, compteur quand une catégorie est choisie, autorisation de lister les applications sur Android.

## 9. Écran 6 · Accueil

Images : `13_ecran-6-reveil-desactive-arrivee-apres-l-ecran-4`, `14_ecran-6-reveil-prevu`, `15_ecran-6-session-en-cours-sombre`.

L'accueil récapitule le prochain réveil et permet de modifier tout ce qui peut l'être. Il a trois états.

### Ce que contient l'écran, de haut en bas

1. **L'icône des réglages**, en haut à droite. Pas de nom d'application. Elle ouvre l'écran 12.
2. **Un cadran de 24 heures**, avec « Coucher 22:30 » et « Réveil 7:00 » écrits à l'extérieur, et au centre un libellé et une valeur. Deux poignées, une pour le coucher et une pour le réveil, se déplacent sur le cadran pour régler les heures. Un petit point marque l'heure actuelle.
3. **Une carte d'état**, avec l'interrupteur général.
4. **Sept pastilles**, une par jour : L M M J V S D. Toucher une pastille coche ou décoche le jour.
5. **La ligne « Applications bloquées »** : les icônes des applications, puis « +N » quand elles ne tiennent plus sur la ligne ; à droite, le nombre et un chevron. Elle ouvre l'écran 4.
6. **La ligne « Sonnerie »** : à droite, le nom du son et, dessous, « Progressive · 2 min », puis un chevron. Elle ouvre le choix de la sonnerie (section 10).

### Les trois états

| | Réveil prévu | Réveil désactivé | Session en cours |
| --- | --- | --- | --- |
| Image | 14 | 13 | 15 |
| Quand | Interrupteur activé, avant l'heure du coucher | Interrupteur éteint | De l'heure du coucher jusqu'au scan |
| Thème | Clair | Clair | Sombre |
| Centre du cadran | « Réveil dans » puis « 9 h 26 » | « Réveil » puis « Désactivé » | « Réveil dans » puis « 7 h 50 » |
| Cadran | Arc noir, poignée du coucher blanche, poignée du réveil ambre | Arc gris, deux poignées blanches | Pas de poignées. L'arc se remplit d'ambre au fil de la nuit (voir `VALEURS-DE-DESIGN.md`) |
| Carte | Noire. « Réveil activé », « Blocage des apps dans 56 min », « 8 h 30 de sommeil », interrupteur ambre | Blanche, au trait. « Réveil désactivé », « Aucun réveil ni blocage prévu », interrupteur gris | « Blocage en cours » avec un cadenas ambre, « Applications bloquées jusqu'au scan de ton Niumi Point », lien « Annuler la session ». Pas d'interrupteur |
| Jours, heures, applications | Modifiables | Affichés en grisé, mais modifiables | Grisés et verrouillés, avec un cadenas sur la ligne des applications |
| Sonnerie | Modifiable | Affichée en grisé, mais modifiable | Modifiable, non grisée |

- **Réveil désactivé** est aussi l'état d'arrivée à la fin de la mise en route. Les valeurs par défaut sont affichées, tout reste modifiable, et aucun message d'aide n'est ajouté. Rien ne se bloque et rien ne sonne tant que l'interrupteur n'est pas activé.
- **L'interrupteur général** désactive tout le planning : plus aucun réveil ni blocage tant qu'on ne le rallume pas.
- **Pendant une session**, toucher un champ verrouillé affiche : « Modifiable après avoir scanné ton Niumi Point. » La forme de ce message n'est pas dessinée **(ouvert)**.
- Il n'est pas possible de modifier une session en cours. La seule action est « Annuler la session » (section 11).

## 10. Choix de la sonnerie

Images : `16_sonnerie-la-feuille`, `17_sonnerie-quand-on-touche-le-volume`. Thème clair dans les maquettes.

Une feuille monte du bas par-dessus l'accueil quand on touche la ligne « Sonnerie ». Elle reste accessible pendant une session ; sa version sombre n'est pas dessinée **(ouvert)**.

| Élément | Contenu |
| --- | --- |
| Titre | Sonnerie, avec une croix pour fermer |
| Section « Son » | Cinq sons en liste. Chaque ligne a un bouton rond pour choisir et un bouton pour écouter |
| Section « Sonnerie progressive » | Cinq pastilles : Non, 1 min, 2 min, 5 min, 10 min |
| Sous les pastilles | Volume max en 2 min. Ce qui s'affiche quand le choix est « Non » n'est pas décidé **(ouvert)** |
| Dernière ligne | « Volume du téléphone » à gauche, « 80 % » à droite. Pas de curseur |

- Les noms des sons dans les maquettes (Aube, Carillon, Oiseaux, Classique, Insistante) sont des exemples. Les vrais sons restent à choisir **(ouvert)**.
- La sonnerie progressive monte jusqu'au volume maximal en 1, 2, 5 ou 10 minutes, ou ne monte pas (« Non »).
- Niumi affiche le volume du téléphone mais ne permet pas de le modifier, sur iPhone comme sur Android.
- Toucher la ligne du volume affiche une bulle noire au-dessus d'elle : « Il se règle dans les réglages de ton téléphone. » La bulle disparaît quand on touche ailleurs. Aucun signe n'indique que la ligne se touche.
- Sur l'accueil, quand la sonnerie progressive est sur « Non », la ligne n'affiche que le nom du son **(ouvert : à confirmer)**.

Contraintes vérifiées :

- **Android.** Une application peut lire le volume d'alarme du téléphone.
- **iPhone.** Avec AlarmKit, l'alarme suit le volume « Sonnerie et alertes » du téléphone et l'app ne peut pas le régler. La seule valeur qu'une app peut lire est décrite comme le volume de sortie du système, et des développeurs signalent qu'elle n'est pas toujours à jour depuis iOS 18. Si la lecture n'est pas fiable, l'iPhone affichera la ligne sans le pourcentage **(ouvert : à tester)**.
- **iPhone.** Un son personnalisé d'AlarmKit doit durer moins de 30 secondes. Une montée sur plusieurs minutes ne tient donc pas dans un seul fichier **(ouvert : à tester)**.

## 11. Annuler la session

Images : `18_annuler-1-le-lien-sur-l-accueil`, `19_annuler-2-scanner-ou-sortie-de-secours`, `20_annuler-3-la-feuille-de-scan`, `21_annuler-4-session-annulee-retour-au-clair`. Thème sombre, sauf la dernière.

Pendant une session, l'utilisateur peut l'annuler en scannant un Niumi Point.

1. Sur l'accueil en session, il touche le lien « Annuler la session ».
2. Une feuille monte du bas :

| Élément | Contenu |
| --- | --- |
| Titre | Annuler la session ? |
| Texte | Scanne ton Niumi Point pour annuler. Tes applications seront débloquées et ton réveil désactivé. |
| Bouton | Scanner le Niumi Point |
| Lien discret | Je n'ai pas accès à mon Niumi Point |

3. « Scanner le Niumi Point » ouvre la feuille de scan, avec la phrase « Approche ton iPhone du Niumi Point pour annuler la session » (« ton téléphone » sur Android).
4. Après le scan, l'accueil revient en clair, dans l'état « Réveil désactivé », avec un message en bas de l'écran : « Session annulée. Tes applications sont débloquées et ton réveil est désactivé. »

Effets de l'annulation : le blocage est levé et l'interrupteur « Réveil activé » est éteint. Plus aucune alarme ni blocage n'est prévu tant qu'on ne le rallume pas.

Le lien « Je n'ai pas accès à mon Niumi Point » ouvre la sortie de secours (section 13).

Cette étape intermédiaire est identique sur iPhone et sur Android. Elle existe parce que la feuille de scan de l'iPhone n'accepte pas de lien.

## 12. Écran 7 · Écran de blocage

Image : `22_ecran-7-ecran-de-blocage`. Thème sombre.

Il s'affiche quand l'utilisateur ouvre une application bloquée.

| Élément | Contenu |
| --- | --- |
| Icône | Le logo niumi |
| Titre | Cette application est bloquée |
| Texte | Elle reviendra après avoir scanné ton Niumi Point. |
| Bouton principal | Fermer |
| Bouton secondaire | Ouvrir Niumi |

- **iPhone.** Cet écran appartient au système. Niumi peut en régler la couleur de fond, l'icône, le titre, le texte, les libellés des boutons et la couleur du bouton principal, mais pas la disposition. La taille du logo dans l'emplacement de l'icône est à tester sur un appareil **(ouvert)**.
- **iPhone.** « Ouvrir Niumi » n'est pas réalisable directement : un bouton de cet écran peut seulement le fermer. Le seul contournement connu est d'envoyer une notification que l'utilisateur touche pour ouvrir Niumi. Reste à décider si on garde ce bouton sur iPhone **(ouvert)**.
- **Android.** L'écran est entièrement dessiné par Niumi. « Ouvrir Niumi » mène à l'accueil.

## 13. Écrans 8 et 9 · Sortie de secours

Images : `23_ecran-8-confirmation-le-soir`, `24_ecran-8-confirmation-le-matin`, `25_ecran-9-recopie-en-cours`, `26_ecran-9-erreur-signalee`, `27_ecran-9-texte-complet-bouton-actif`. Thème sombre.

La sortie de secours remplace le scan d'un Niumi Point, au prix d'un long texte à recopier. Elle s'ouvre par le lien « Je n'ai pas accès à mon Niumi Point », présent sur la feuille d'annulation, sur l'écran de réveil et sur les feuilles d'échec du scan.

### Écran 8 · Avertissement

Une feuille monte du bas, par-dessus l'accueil en session le soir, par-dessus l'écran de réveil le matin.

| Élément | Contenu |
| --- | --- |
| Titre | Sortir de la session sans scanner ton Niumi Point ? |
| Texte, le soir ou la nuit | Recopier un long texte remplace le scan de ton Niumi Point. |
| Texte, pendant l'alarme | L'alarme s'arrêtera et tes applications seront débloquées. Pour sortir, tu devras recopier un long texte. |
| Bouton principal | Rester dans la session |
| Bouton secondaire, au trait | Recopier le texte |

Le bouton principal est celui qui fait rester. Sortir demande un geste volontaire sur le second.

### Écran 9 · Recopie

| Élément | Contenu |
| --- | --- |
| En haut à gauche | Lien « Annuler » |
| En haut à droite | Compteur : « 84 sur 197 caractères » |
| Consigne | Recopie ce texte pour sortir de la session. |
| Texte modèle | Affiché en entier dans un encadré, au-dessus du champ |
| Champ de saisie | Vide au départ. Le collage est désactivé |
| Bouton, au-dessus du clavier | Sortir de la session. Inactif tant que le texte n'est pas complet et juste |
| Message d'erreur | Un mot ne correspond pas. Il est signalé dans le texte. |

- **Erreur.** Le premier mot qui diffère est surligné dans le texte modèle et souligné dans la saisie, et le message d'erreur s'affiche sous le champ.
- **Comparaison.** Les majuscules, les accents, la ponctuation et les espaces en double sont ignorés.
- **Abandon.** « Annuler » ramène à l'écran d'où l'on vient. La saisie est perdue.
- **Le texte change à chaque sortie**, pour qu'un raccourci clavier ne suffise pas. Un seul texte est validé à ce jour (197 caractères). D'autres textes neutres du même genre seront choisis plus tard **(ouvert)** :

> J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Point. Je choisis de sortir de cette session sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.

Le même texte sert le soir et le matin : il ne doit donc mentionner ni « ce soir » ni une heure.

**Après la recopie**, la session se termine comme après un scan.

- Le soir ou la nuit : le blocage est levé, l'interrupteur « Réveil activé » est éteint, et l'accueil affiche le message « Session annulée. Tes applications sont débloquées et ton réveil est désactivé. »
- Le matin, pendant l'alarme : la sonnerie s'arrête et les applications sont débloquées. L'écran affiché ensuite n'est pas tranché **(ouvert)**. Le journal dit à la fois que la sortie de secours « compte comme un scan et a le même effet », ce qui mènerait à l'écran 11, et qu'après la recopie « l'accueil affiche le message « Session annulée… » », sans distinguer le soir du matin.
- Sur iPhone, ce que fait l'alarme pendant la recopie n'est pas décidé non plus **(ouvert)**.

**Android, le matin.** La sonnerie baisse tant que la saisie avance et remonte si elle s'arrête.

## 14. Écran 10 · Réveil

Images : `28_ecran-10-le-reveil-sonne` à `35_ecran-10-iphone-seulement-temps-de-scan-ecoule`. Thème sombre.

L'écran ne dit qu'une chose : va scanner ton Niumi Point. Il n'a aucun bouton d'arrêt ni de report. Pendant que l'alarme sonne, c'est lui qui s'affiche, pas l'accueil.

| Élément | Contenu |
| --- | --- |
| Date | Vendredi 2 octobre |
| Heure | L'heure actuelle, en très grand |
| Dessin | Le téléphone approché du Niumi Point |
| Consigne | Scanne ton Niumi Point |
| Bouton | Scanner le Niumi Point |
| Lien discret | Je n'ai pas accès à mon Niumi Point |

L'écran n'indique pas la pièce, car plusieurs Niumi Points peuvent être enregistrés. Le lien ouvre la sortie de secours.

### La feuille de scan (image 29)

« Scanner le Niumi Point » ouvre la feuille « Prêt à scanner », la même qu'à l'écran 3 : « Approche ton iPhone du Niumi Point » sur iPhone, « Approche ton téléphone du Niumi Point » sur Android, et le bouton « Annuler ».

Sur iPhone, cette feuille est celle du système. Son apparence pendant une session, claire ou sombre, est à regarder sur un appareil **(ouvert)**.

### Les échecs du scan (images 30 à 33)

Une feuille du bas affiche un titre, une phrase d'aide, un bouton et le lien « Je n'ai pas accès à mon Niumi Point ».

| Cas | Titre | Phrase d'aide | Bouton |
| --- | --- | --- | --- |
| Pas un Niumi Point | Ce n'est pas un Niumi Point. | Scanne un Niumi Point que tu as enregistré. | Réessayer |
| Niumi Point non enregistré | Ce Niumi Point n'est pas enregistré. | Scanne un Niumi Point que tu as enregistré. | Réessayer |
| Lecture ratée | Ton Niumi Point n'a pas pu être lu. | Rapproche ton téléphone et réessaie. | Réessayer |
| NFC coupé (Android) | Le NFC est coupé. | Active-le pour scanner ton Niumi Point. | Ouvrir les réglages |

Aucun de ces cas n'arrête l'alarme ni ne débloque les applications.

Sur iPhone, la feuille de scan du système ne peut pas contenir de bouton. Niumi affiche donc sa propre feuille d'échec après la fermeture de celle du système.

### iPhone seulement : la feuille se ferme après 60 secondes (image 35)

Sur iPhone, une lecture NFC dure 60 secondes au plus, et l'app doit rester au premier plan. Si l'utilisateur touche « Scanner le Niumi Point » depuis son lit et met plus d'une minute à atteindre son Niumi Point, la feuille se ferme d'elle-même. Niumi affiche alors, dans la feuille d'échec :

| Titre | Phrase d'aide | Bouton | Lien |
| --- | --- | --- | --- |
| Le temps de scan est écoulé. | Relance le scan une fois devant ton Niumi Point. | Réessayer | Je n'ai pas accès à mon Niumi Point |

Ce message sert pour tous les scans de l'iPhone qui dépassent 60 secondes : le matin, à l'annulation d'une session, à l'association et à l'ajout d'un Niumi Point. À l'association et à l'ajout, le lien de secours est remplacé par un lien « Annuler », qui referme la feuille sans relancer le scan. Seule la version du matin est dessinée **(ouvert)**.

**Android n'a pas cette limite.** La feuille reste ouverte jusqu'au scan ou jusqu'à « Annuler ». C'est une différence voulue.

Il n'a pas été vérifié si l'iPhone permet de rouvrir la feuille sans geste de l'utilisateur **(ouvert : à tester)**.

### iPhone seulement : l'alarme va reprendre (image 34)

Sur iPhone, l'alarme qui sonne est d'abord présentée par le système, avec une commande d'arrêt et un bouton qui ouvre Niumi. Cette commande ne peut pas être retirée, et les boutons physiques arrêtent aussi la sonnerie en cours. Niumi relance donc l'alarme tant que le scan n'a pas eu lieu. L'arrêt ne termine pas la session et les applications restent bloquées.

Quand l'alarme a été arrêtée de cette façon, l'écran de réveil remplace sa consigne par :

| Titre | Texte |
| --- | --- |
| Ton alarme va reprendre. | Scanne ton Niumi Point pour l'arrêter. |

Le bouton et le lien ne changent pas.

Risque à tester sur un iPhone : la relance dépend de l'avertissement que le système envoie à l'app quand l'alarme est arrêtée. Un développeur signale que cet avertissement n'arrive pas toujours **(ouvert)**.

### Android

L'écran est dessiné en entier par Niumi et la sonnerie continue jusqu'au scan.

## 15. Écran 11 · Réussite

Image : `36_ecran-11-reussite-retour-au-clair`. Thème clair : la session est finie.

Il confirme que le scan a marché et que les applications sont revenues. Il reste affiché jusqu'à ce que l'utilisateur le ferme.

| Élément | Contenu |
| --- | --- |
| Dessin | Un soleil qui se lève au-dessus d'une ligne d'horizon. Le soleil est en dégradé : ambre en haut, ambre très clair à l'horizon. Ce dégradé est gardé « pour l'instant » **(ouvert)** |
| Titre | Debout à 7:04. |
| Délai | 4 minutes après la sonnerie. |
| Comparaison | Bravo ! C'est 11 minutes de moins qu'avant Niumi. |
| Confirmation | Tes applications sont de nouveau accessibles. |
| Bouton | Retour à l'accueil |

- Dans l'exemple : l'alarme a sonné à 7:00 et le scan a eu lieu à 7:04.
- La comparaison s'affiche seulement si l'utilisateur s'est levé plus vite que la durée déclarée à l'écran 3 bis. Dans l'exemple : 15 minutes déclarées, levé en 4 minutes, donc 11 minutes de moins. Sinon, rien ne s'affiche à la place.
- « Retour à l'accueil » ramène à l'accueil, dans l'état « Réveil prévu ».
- Il n'y a pas de lien depuis cet écran vers la rubrique « Temps gagné ».

## 16. Écran 12 · Réglages

Images : `37_ecran-12-reglages` à `41_reglages-a-propos` et `47_niumi-points-la-liste` à `50_niumi-points-renommer-la-piece` en clair ; `42` à `46` et `51` à `54` en sombre.

Les réglages regroupent ce qu'on configure une fois. Tout ce qui décrit la prochaine nuit reste sur l'accueil. Ils s'ouvrent aussi pendant une session : ils sont alors sombres, avec les mêmes mises en page et les mêmes textes. L'écran 2, la roue de « Temps gagné » et le parcours d'ajout d'un Niumi Point n'ont pas de version sombre dessinée **(ouvert)**.

Chaque page a une flèche de retour en haut à gauche.

| Ligne | À droite | Ouvre |
| --- | --- | --- |
| Niumi Points | Le nombre de Niumi Points (« 2 ») | La liste des Niumi Points |
| Autorisations | « 3 sur 3 » | L'écran 2 |
| Aide | — | La liste des rubriques d'aide |
| À propos | — | La page À propos |

### Niumi Points

| Élément | Contenu |
| --- | --- |
| Titre | Niumi Points |
| Lignes | Un Niumi Point par ligne, avec sa pièce et un bouton « trois points » |
| Dernière ligne | Ajouter un Niumi Point |
| Mention | N'importe lequel de tes Niumi Points arrête l'alarme. |

- Le bouton « trois points » ouvre un menu : « Renommer la pièce » et « Supprimer ».
- **Renommer la pièce** rouvre la feuille des pièces de l'écran 3, la pièce actuelle sélectionnée, avec le bouton « Enregistrer ». En sombre, la case choisie est blanc cassé avec un texte noir.
- **Supprimer** demande une confirmation dans une feuille du bas : titre « Supprimer le Niumi Point « Cuisine » ? », texte « Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter. », boutons « Supprimer » et « Annuler ».
- **Il doit toujours rester au moins un Niumi Point.** Quand il n'en reste qu'un, « Supprimer » est grisé dans le menu, avec « Tu dois garder au moins un Niumi Point. »
- **Ajouter un Niumi Point** reprend le parcours de l'écran 3.
- L'ajout et la suppression restent possibles pendant une session.

### Aide

Quatre rubriques : Fonctionnement, Niumi Point perdu, Sortie de secours, Temps gagné.

- **Une page de rubrique** a un titre et des paragraphes, chacun sous un intertitre. « Fonctionnement » reprend les phrases des écrans 1 et 11 sous « Le soir », « Le matin » et « Après le scan » (image 39).
- **Niumi Point perdu** et **Sortie de secours** ne sont pas écrites **(ouvert)**.
- **Temps gagné** (image 40) : le texte « Après chaque réveil, Niumi compare le temps que tu as mis à te lever avec celui que tu mettais avant. », puis une carte « Temps pour sortir du lit avant Niumi » avec la valeur (« 15 min ») et le bouton « Modifier », qui ouvre la roue de l'écran 3 bis. La valeur reste modifiable pendant une session.

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

### Bandeau de problème

Images : `55_bandeau-nfc-coupe-android`, `56_bandeau-aucun-niumi-point-au-cas-ou`.

Un bandeau noir tout en haut de l'accueil, avec un symbole d'avertissement, une phrase et un lien souligné. Il reste affiché tant que le problème existe. Le cadran et l'icône des réglages descendent d'autant. Un seul bandeau à la fois, le plus grave d'abord.

| Problème | Texte | Lien |
| --- | --- | --- |
| NFC coupé (Android seulement) | Le scan du Niumi Point ne fonctionnera pas : le NFC est coupé. | Activer le NFC |
| Aucun Niumi Point | Aucun Niumi Point n'est associé. | Associer un Niumi Point |

Le second cas ne devrait pas arriver, puisque l'association est obligatoire au départ et que supprimer le dernier Niumi Point est refusé. Il est gardé par précaution.

### Alerte d'autorisations

Images : `57_alerte-une-autorisation-retiree`, `58_alerte-plusieurs-autorisations-retirees`.

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
