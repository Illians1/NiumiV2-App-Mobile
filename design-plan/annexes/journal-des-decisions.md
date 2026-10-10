> **Annexe. Ne pas implémenter à partir de ce fichier.**
> C'est le journal de la conception, exporté le 9 octobre 2026. Il garde l'historique : versions essayées puis abandonnées, tableaux dépassés, décisions remplacées par d'autres. Ce qui est décidé aujourd'hui est dans `SPECIFICATION.md`. En cas de contradiction, `SPECIFICATION.md` et les maquettes l'emportent. Ce journal reste utile pour comprendre pourquoi une décision a été prise et pour retrouver les sources.

# Niumi : contenu des écrans

Oct 1, 2026 · @Aouida Mehdi

Vue d'ensemble : la page « Écrans validés » du canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5) rassemble les 58 maquettes gardées, dans l'ordre du parcours. Depuis le 8 octobre 2026, elles y sont en couleur : claires hors session (Encre 3), sombres pendant la session (Nuit 3 bis). C'est la page de référence. Les versions noir et blanc restent sur la page de chaque écran. Les pages « Hors session, en clair » et « Session en sombre », qui faisaient doublon, ont été supprimées le même jour.

## Règles communes

Ces règles viennent de tes réponses et valent pour les 12 écrans. Exemple utilisé partout : réveil à 7:00, coucher à 22:30, 6 applications, Niumi Point dans la salle de bain.

| Sujet | Règle |
| --- | --- |
| Réveil | Récurrent, aux jours choisis, à une heure unique |
| Coucher | Le blocage des applications démarre seul à l'heure du coucher, la veille ou le jour même du réveil selon l'heure choisie |
| Rappel | Notification 30 minutes avant le coucher, sans bouton |
| Avant le coucher | Tout se modifie. Un réveil s'annule en décochant le jour. Un interrupteur général désactive tout le planning |
| Du coucher au scan | Heures, jours et applications verrouillés. Le reste se modifie. Un scan avant l'alarme permet d'annuler la session |
| Fin normale | Scan le matin d'un Niumi Point enregistré, n'importe lequel |
| Sortie de secours | Long texte à recopier. Accents, majuscules, ponctuation et espaces en double ignorés. Elle compte comme un scan |
| Exclu | Lancement anticipé, confirmation, essai à blanc, historique, compte |

Les textes proposés tutoient l'utilisateur.

Règle ajoutée le 8 octobre 2026 : le fonctionnement reste le même sur iPhone et sur Android, autant que possible. Exemple : la feuille de scan de l'iPhone ne peut pas contenir de lien, donc le lien « Je n'ai pas accès à mon Niumi Point » est placé avant le scan sur les deux.

## Écran 1 · Présentation du principe

Deux panneaux expliquent la règle avant toute configuration. C'est le seul endroit où l'utilisateur apprend qu'une session démarre sans lui, donc on ne peut pas le passer.

Maquette gardée pour le moment : « C » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 1 · Présentation », choisie le 6 octobre 2026. Le logo niumi est en haut des deux panneaux. Le panneau 1 montre un téléphone dont les applications sont verrouillées. Le panneau 2 montre le téléphone approché du Niumi Point, le même dessin que sur l'écran 10. Un repère indique le panneau en cours.

| Panneau | Titre | Texte |
| --- | --- | --- |
| 1 | À l'heure de te coucher, tes distractions sont bloquées | À l'heure que tu choisis, Niumi bloque les applications que tu as désignées. |
| 2 | Le matin, Niumi t'aide à te lever. | Ton alarme sonne jusqu'à ce que tu scannes le Niumi Point situé dans la pièce de ton choix. |

Boutons : « Continuer » sur le panneau 1, « Configurer Niumi » sur le panneau 2. Un retour en arrière est possible entre les panneaux.

## Écran 2 · Autorisations

Un seul écran présente les autorisations, une tuile par autorisation, avec une phrase d'explication avant la demande du système. Sans l'alarme, le blocage et les notifications, l'utilisateur ne peut pas continuer.

Maquette gardée pour le moment : « B4a » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 2 · Autorisations », choisie le 6 octobre 2026. Chaque tuile a une icône, le nom de l'autorisation et sa phrase. Le guide en images qui s'ouvre avec « ? » est la maquette « Le guide en images » de la même page, validée le 8 octobre 2026. Ses images restent à produire, pour iPhone et pour Android.

| Autorisation | Phrase d'explication | iPhone | Android |
| --- | --- | --- | --- |
| Alarme | Pour que ton réveil sonne à l'heure, même téléphone verrouillé. | Fenêtre du système | Rien à demander en principe pour une app de réveil. L'affichage plein écran peut être retiré par l'utilisateur |
| Blocage des applications | Pour bloquer les applications que tu choisis, et seulement celles-là. | Fenêtre du système (Temps d'écran) | Service d'accessibilité à activer dans les réglages, avec guidage. Approche courante, à valider avec Google Play |
| Notifications | Pour pouvoir te tenir informé de tout ce qui est important. | Fenêtre du système | Fenêtre du système |
| NFC | Pour pouvoir scanner ton Niumi Point. | Rien à demander | À activer dans les réglages s'il est coupé |

Chaque tuile porte un bouton « Autoriser » et un petit bouton « ? », qui ouvre un guide en images pour cette autorisation. Une fois l'autorisation donnée, la tuile affiche une coche. Le bouton « Continuer », en bas de l'écran, reste grisé tant qu'il en manque une. En cas de refus, il ne se passe rien de particulier : la tuile n'est simplement pas affichée comme accordée (décision du 8 octobre 2026).

Quatre versions d'un état « refusée » avaient été proposées le 8 octobre 2026 dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 2 · Autorisations », rangée du bas. Aucune n'est gardée. Si une autorisation est retirée plus tard, c'est l'alerte d'autorisations qui prévient l'utilisateur (voir « Hors écrans »).

Les notifications ne peuvent pas être refusées sans bloquer la suite.

## Écran 3 · Association du Niumi Point

L'utilisateur scanne son Niumi Point une première fois, puis nomme la pièce où il le pose. Ce scan vérifie aussi que le NFC fonctionne et apprend le geste du matin.

Maquette gardée pour le premier temps, la consigne : « A3 » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 3 · Association du Niumi Point », choisie le 6 octobre 2026. Le logo est en haut, le Niumi Point est dessiné seul et en grand, puis viennent la consigne, le bouton et le lien discret. Pour le deuxième temps, le scan, la maquette gardée est « Scan 3 », sur la même page, choisie le 7 octobre 2026. L'écran de la consigne s'assombrit et une feuille monte du bas sur toute la largeur du téléphone : « Prêt à scanner », une icône, « Approche ton iPhone du Niumi Point » et le bouton « Annuler ». Sur iPhone, cette feuille est dessinée par le système. Niumi n'y choisit que la phrase, donc sa largeur réelle sera celle que le système lui donne, à regarder sur un appareil. Sur Android, Niumi dessine lui-même la même feuille (décision du 7 octobre 2026), avec la phrase « Approche ton téléphone du Niumi Point ». Pour le troisième temps, le choix de la pièce, la maquette gardée est « Pièce 4b », sur la même page, choisie le 7 octobre 2026. Après le scan, une feuille monte du bas sur toute la largeur, par-dessus l'écran de la consigne assombri : « Où est ton Niumi Point ? », quatre grandes cases avec une icône (Salle de bain, Cuisine, Salon, Entrée), une case « Une autre pièce » sur toute la largeur, puis le bouton « Continuer ». « Une autre pièce » ouvre la saisie d'un nom libre dans la même feuille : la case devient un champ « Nom de la pièce », le clavier s'ouvre et la feuille remonte au-dessus de lui (décision du 7 octobre 2026, maquette « Pièce 4b · Une autre pièce » sur la même page). Il n'y a pas de temps de confirmation : « Continuer » mène directement à l'écran 3 bis (décision du 7 octobre 2026).

1. Consigne : « Pose ton Niumi Point là où tu veux te rendre en premier le matin, hors de ta chambre. » Bouton « Scanner le Niumi Point ».
2. Scan. Sur iPhone, une fenêtre du système s'ouvre, avec le message « Approche ton iPhone du Niumi Point ». Sur Android, Niumi affiche la même feuille, avec « Approche ton téléphone du Niumi Point ».
3. Nom de la pièce : « Où est ton Niumi Point ? » Choix proposés : Salle de bain, Cuisine, Salon, Entrée, ou « Une autre pièce » pour saisir un nom libre dans la même feuille. Bouton « Continuer », qui mène à l'écran 3 bis.

| Cas d'échec | Texte | Action |
| --- | --- | --- |
| Pas un Niumi Point | Ce n'est pas un Niumi Point. | Réessayer |
| Lecture ratée | Ton Niumi Point n'a pas pu être lu. Rapproche ton téléphone et réessaie. | Réessayer |
| NFC coupé (Android) | Le NFC est coupé sur ton téléphone. | Ouvrir les réglages |
| Pas de Niumi Point | Lien discret : « Je n'ai pas encore de Niumi Point » | Ouvre la page web du produit, pour l'acheter |

Plusieurs Niumi Points peuvent être enregistrés. Ce parcours sert aussi à en ajouter un depuis les réglages.

## Écran 3 bis · Temps pour sortir du lit

Ajouté le 6 octobre 2026, juste après l'association du premier Niumi Point. Niumi demande à l'utilisateur combien de temps il met aujourd'hui pour sortir du lit. La réponse est un nombre de minutes, gardé sur le téléphone, sans compte.

Elle sert à l'écran 11. Quand l'utilisateur se lève plus vite que ce qu'il avait déclaré, l'écran affiche un message du type « Bravo ! C'est 11 minutes de moins qu'avant Niumi. »

Maquette gardée pour le moment : « A4 », la roue de durées, dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 3 bis ». Elle a été choisie le 6 octobre 2026. L'utilisateur fait défiler des durées, et celle du milieu, affichée en gros, est sa réponse. La roue propose onze durées : 1, 2, 3, 4, 5, 10, 15, 20, 30 et 45 minutes, puis « 1 heure et + ».

Décisions prises le 6 octobre 2026 :

- La question est obligatoire : on ne peut pas la passer.
- La valeur se corrige ensuite dans l'aide, rubrique « Temps gagné » (décision du 7 octobre 2026). Elle n'est plus une ligne des réglages.
- Les matins où l'utilisateur n'a pas fait mieux que la durée déclarée, l'écran 11 n'affiche rien à la place du message « Bravo ! ».
- « 1 heure et + » compte pour 60 minutes dans le calcul. Ce dernier point est une proposition, à confirmer.

## Écran 4 · Choix des applications

L'utilisateur désigne les applications qui seront bloquées chaque soir de session. Il en faut au moins une.

| Élément | Contenu |
| --- | --- |
| Titre | Quelles applications veux-tu bloquer ? |
| Texte | Elles seront inaccessibles de l'heure du coucher jusqu'au scan de ton Niumi Point. |
| Liste | Sur iPhone, la liste du système. Sur Android, les applications installées en grille d'icônes, avec une recherche et une coche sur celles choisies |
| Compteur | 6 applications choisies |
| Bouton | Continuer |
| Aucune application cochée | Choisis au moins une application. |

Sur iPhone, la liste est fournie par le système : l'écran se réduit au titre, au texte, à un bouton « Choisir les applications » et au compteur. Sur Android, l'écran est le même et Niumi dessine la liste, qui s'ouvre en feuille du bas.

Vérifié le 7 octobre 2026 dans la documentation d'Apple. La liste du système (FamilyActivityPicker) peut être posée dans la page comme n'importe quel élément, ou ouverte en feuille par-dessus l'écran. Elle propose des catégories, des applications et des sites web. Niumi ne reçoit que des valeurs opaques : il peut afficher l'icône et le nom de chaque choix, et compter les choix, mais pas lire les noms. Une option (includeEntireCategory) indique si la sélection doit inclure les applications des catégories choisies. Niumi peut ajouter à la liste un texte d'en-tête et un texte de pied, et un titre à partir d'iOS 26.2.

Deux points à tester sur un appareil. Des développeurs signalent sur le forum d'Apple qu'au-delà de 50 applications choisies, plus aucune n'est bloquée ; Apple ne l'a pas confirmé dans ce fil. Et quand une catégorie est choisie, le compteur « 6 applications choisies » ne peut pas toujours être calculé : sa formulation sur iPhone sera décidée après un test sur un appareil (décision du 7 octobre 2026). En attendant, la maquette garde « 6 applications choisies ».

Sur Android, lister toutes les applications installées passe par une autorisation (QUERY\_ALL\_PACKAGES) que Google Play réserve aux applications dont la fonction principale en a besoin. À vérifier au développement.

Maquette gardée : « A » sur iPhone et sur Android, dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 4 · Choix des applications », rangée du bas, choisie le 7 octobre 2026. Avant le choix, une carte montre six cases vides, « Aucune application choisie » et le bouton « Choisir les applications » ; « Continuer » est grisé, avec « Choisis au moins une application. » Le bouton ouvre la liste. Sur iPhone, c'est celle du système. Sur Android, c'est une feuille du bas dessinée par Niumi, « Applications à bloquer », avec une recherche, les icônes sur quatre colonnes (version « D »), le compteur et le bouton « Valider ». Après le choix, la carte montre les icônes choisies, le compteur et le bouton « Modifier », et « Continuer » s'active.

Règles de la feuille Android (décision du 7 octobre 2026) : la croix ferme la feuille sans enregistrer les changements, et « Valider » reste grisé tant qu'aucune application n'est cochée.

Ce même écran sert ensuite depuis l'accueil pour modifier la sélection.

## Écran 5 · Premier planning (supprimé)

Écran supprimé le 7 octobre 2026. Après le choix des applications (écran 4), « Continuer » mène directement à l'accueil (écran 6), dans l'état « Réveil désactivé ». Les valeurs par défaut y sont affichées et modifiables. Rien ne se bloque et rien ne sonne tant que l'utilisateur n'a pas activé l'interrupteur. Aucun message d'aide n'est ajouté à cette première arrivée. Le tableau et la phrase sur le résumé ci-dessous décrivaient l'ancien écran et ne s'appliquent plus. Les valeurs par défaut et les règles sur l'heure du coucher restent valables : elles s'appliquent à l'accueil.

Quatre versions (A à D) avaient été proposées le 7 octobre 2026 dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 5 ». Aucune n'est gardée.

| Élément | Contenu |
| --- | --- |
| Titre | Règle ton réveil |
| Jours | Sept pastilles L M M J V S D |
| Heure du réveil | Sélecteur d'heure |
| Heure du coucher | Sélecteur d'heure |
| Résumé | Tes applications seront bloquées dans...\
Ton alarme sonnera dans... |
| Bouton | Enregistrer |
| Aucun jour coché | Choisis au moins un jour. |

Le résumé se met à jour à chaque changement d'heure. Après l'enregistrement, l'utilisateur arrive sur l'accueil.

Valeurs par défaut : lundi à vendredi, réveil à 7:00, coucher à 22:30.

Si on choisit une heure de coucher plus tardive que l'heure de réveil, le coucher a lieu la veille. Sinon, il a lieu le jour même.

Règle changée le 7 octobre 2026 : Niumi fonctionne par période, du coucher au réveil. L'interrupteur s'active toujours et aucune heure n'est refusée. Si l'on se trouve déjà dans la période au moment où l'on active l'interrupteur ou où l'on déplace une poignée, le blocage commence tout de suite, sans confirmation, et l'accueil passe en « Blocage en cours » jusqu'au scan du matin. Exemple : il est 23:00 et la période va de 22:30 à 7:00. Activer l'interrupteur, ou régler le coucher sur 22:30, lance le blocage aussitôt. L'ancien message « Cette heure est déjà passée. Choisis une heure à venir. » est supprimé.

## Écran 6 · Accueil

L'accueil récapitule le prochain réveil et permet de modifier tout ce qui peut l'être. Il a trois états selon le moment.

Maquette gardée pour le moment, pour l'état « Réveil prévu » : « Retenue 2 » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), choisie le 2 octobre 2026. Elle contient, de haut en bas :

- L'icône des réglages en haut à droite, sans le nom de l'application.
- Un cadran de 24 h à deux poignées, avec « Coucher 22:30 » et « Réveil 7:00 » à l'extérieur et « Réveil dans 9 h 26 » au centre.
- Une carte « Réveil activé » avec l'interrupteur, « Blocage des apps dans 56 min » et « 8 h 30 de sommeil ».
- Les sept pastilles des jours.
- La ligne « Applications bloquées » avec les icônes des applications, puis « +N » quand elles ne tiennent plus sur la ligne.
- La ligne « Sonnerie » : à droite, le nom du son et, dessous, « Progressive · 2 min ». Depuis le 9 octobre 2026, la courbe de son et « Volume max en 2 min » ne sont plus sur l'accueil.

Les deux autres états sont gardés aussi, dans le même canevas :

- « Réveil désactivé » : l'interrupteur est éteint et la carte indique « Réveil désactivé, aucun réveil ni blocage prévu ». Tout le reste est grisé, et le centre du cadran affiche « Réveil désactivé ». C'est aussi l'état d'arrivée à la fin de l'onboarding, juste après l'écran 4 (décision du 7 octobre 2026) : les valeurs par défaut sont affichées, tout reste modifiable, et aucun message d'aide n'est ajouté.
- « Session 3 », pour la session en cours : le cadran n'a plus de poignées et affiche « Réveil dans 7 h 50 ». Une carte noire indique « Blocage en cours » et « Applications bloquées jusqu'au scan de ton Niumi Point », avec le lien « Annuler la session » (texte changé le 8 octobre 2026). Heures, jours et applications sont grisés, avec un cadenas. La sonnerie reste active.

Le tableau ci-dessous décrit encore l'ancienne disposition en liste. Il reste à aligner sur cette maquette.

### Contenu commun aux trois états

| Élément | Contenu | Au toucher |
| --- | --- | --- |
| Planning | Interrupteur général, activé ou désactivé | Désactivé : plus aucun réveil ni blocage tant qu'on ne le rallume pas. Indisponible pendant une session |
| Prochain réveil | L'heure en grand.\
En dessous en retrait : Dans... | Ouvre le sélecteur d'heure |
| Jours | L M M J V S D | Coche ou décoche un jour |
| Coucher | 22:30\
En dessous en retrait : Dans... | Ouvre le sélecteur d'heure |
| Applications | 6 applications : TikTok, Instagram et 4 autres | Ouvre l'écran 4 |
| Sonnerie | Nom de la sonnerie. Si la sonnerie progressive est activée, « Progressive · » suivi de la durée pour atteindre le volume maximal (exemple : « Progressive · 2 min ») | Ouvre le choix de la sonnerie et de la sonnerie progressive. Le volume du téléphone y est affiché, sans réglage. Sonnerie progressive : désactivée, ou durée pour atteindre le volume maximal, au choix 1, 2, 5 ou 10 min. Reste modifiable pendant une session |
| Réglages | Icône en haut à droite de l'accueil | Ouvre l'écran 12 |
| Erreurs | Seulement s'il y a un problème à corriger | Voir « Hors écrans » |

### Les trois états

| État | Quand | Actions propres à l'état |
| --- | --- | --- |
| Réveil prévu | Avant l'heure du coucher | Tout est modifiable. |
| Pas de réveil prévu | Sans réveil prévu | Tout est modifiable. |
| Session en cours | De l'heure du coucher au scan | Heures, jours et applications grisés. Lien « Annuler la session » |

Dans l'état « Session en cours », un champ grisé affiche au toucher : « Modifiable après avoir scanné ton Niumi Point. »

Décision du 8 octobre 2026 : la carte noire ne propose plus qu'une action, « Annuler la session ». Elle ouvre la feuille de scan, et le scan d'un Niumi Point annule la session. Il n'est plus possible de modifier une session en cours. La sortie de secours remplace le scan si aucun Niumi Point n'est à portée.

Le parcours gardé est dessiné dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Annuler la session », rangée du bas : l'accueil en session avec le lien « Annuler la session », la feuille de scan « Approche ton iPhone du Niumi Point pour annuler la session », puis l'accueil, revenu à l'état « Réveil désactivé », avec le message « Session annulée. Tes applications sont débloquées et ton réveil est désactivé. » (validé le 8 octobre 2026). Avant le scan, une étape identique sur iPhone et sur Android propose « Scanner le Niumi Point » ou le lien « Je n'ai pas accès à mon Niumi Point » (décision du 8 octobre 2026, parce que la feuille de scan de l'iPhone n'accepte pas de lien). On garde la version « A » de cette étape (8 octobre 2026) : une feuille du bas « Annuler la session ? », avec la phrase « Scanne ton Niumi Point pour annuler. Tes applications seront débloquées et ton réveil désactivé. », le bouton « Scanner le Niumi Point » et le lien de secours. Les deux maquettes qui séparaient iPhone et Android ne sont pas retenues. Quatre versions d'un choix « Annuler ou modifier » avaient été proposées le même jour ; aucune n'est gardée.

« Annuler la session » lève le blocage et éteint l'interrupteur « Réveil activé » : plus aucune alarme ni blocage n'est prévu tant qu'on ne le rallume pas (décision du 8 octobre 2026). L'ancienne action « Modifier », qui débloquait les champs avant de reprendre la session, est abandonnée (8 octobre 2026).

Hors session, un réveil s'annule en décochant le jour. Un interrupteur général désactive tout le planning : plus aucun réveil ni blocage tant qu'on ne le rallume pas.

## Choix de la sonnerie

Il s'ouvre depuis la ligne « Sonnerie » de l'accueil. Il règle le son et la sonnerie progressive, et affiche le volume du téléphone sans permettre de le modifier (décision du 8 octobre 2026). Valeurs de la sonnerie progressive : désactivée, ou volume maximal en 1, 2, 5 ou 10 min. Il reste modifiable pendant une session. Sur iPhone, le volume et la sonnerie progressive dépendent d'un test à faire (voir « Points à trancher »).

Vérifié le 8 octobre 2026. Sur Android, une application peut régler le volume d'alarme du téléphone, ou seulement le volume de son propre son, entre 0 et 1. Deux réserves figurent dans la documentation : le réglage est sans effet sur un appareil à volume fixe, et un changement qui basculerait « Ne pas déranger » est refusé sans l'accès correspondant. Sur iPhone, c'est non : un ingénieur d'Apple écrit sur le forum des développeurs qu'AlarmKit n'offre aucun réglage de volume et suit le volume de sonnerie du système, et la documentation précise que seul l'utilisateur peut régler le volume du système. Le curseur « Volume » des maquettes ne peut donc pas être tenu sur iPhone avec AlarmKit.

Décision du 8 octobre 2026 : sur iPhone comme sur Android, l'écran affiche le volume du téléphone, que Niumi ne permet pas de modifier. Il porte la phrase « Il se règle dans les réglages de ton téléphone. » (texte à valider). Sur Android, lire le volume d'alarme est prévu par la documentation. Sur iPhone, c'est à tester sur un appareil : l'alarme suit le volume « Sonnerie et alertes », alors que la seule valeur qu'une application peut lire est décrite comme le volume de sortie du système, sans préciser s'il s'agit de celui de la sonnerie. Des développeurs signalent aussi que cette valeur n'est pas toujours à jour depuis iOS 18, et un ingénieur d'Apple répond qu'il n'y a pas de contournement connu. Si la lecture n'est pas fiable, l'iPhone affichera la phrase sans le pourcentage.

Maquettes en cours : quatre versions (A à D) dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Choix de la sonnerie », proposées le 7 octobre 2026. « D », la feuille du bas par-dessus l'accueil, est préférée, et quatre variantes (D1 à D4) en sont proposées. Elles ont été refaites le 8 octobre 2026 avec le volume affiché sans réglage, sur la rangée du bas. La première version de D1, la feuille avec les sons en liste, est gardée pour le moment (8 octobre 2026), et des modifications sont prévues plus tard. Cette maquette montre encore un curseur de volume réglable : il sera remplacé par l'affichage du volume sans réglage lors des prochaines modifications, décision confirmée le 8 octobre 2026. Les noms de sons des maquettes (Aube, Carillon, Oiseaux, Classique, Insistante) sont des exemples : les vrais sons restent à choisir.

Décidé le 9 octobre 2026, dans la feuille gardée passée en couleur. Le curseur de volume est remplacé par une simple ligne en bas de la feuille : « Volume du téléphone » à gauche, la valeur à droite (exemple : 80 %), sans barre. La phrase « Il se règle dans les réglages de ton téléphone. » n'est plus affichée en permanence : elle apparaît dans une bulle noire, au-dessus du volume, quand on touche la ligne ; la bulle disparaît quand on touche ailleurs. Aucun signe n'indique que la ligne se touche. La phrase « Volume max en 2 min » reste sous les boutons de la sonnerie progressive (retirée puis remise le même jour). Les deux maquettes, la feuille et la bulle, sont sur la page « Écrans validés ».

Les essais sont page « Choix de la sonnerie » : quatre affichages du volume (la ligne de texte est gardée) et quatre formes de popup (la bulle est gardée ; la fenêtre au centre, le message en haut de l'écran et la ligne qui se déplie ne le sont pas).

Ligne « Sonnerie » de l'accueil, décidé le 9 octobre 2026 : « Sonnerie » à gauche ; à droite, le nom du son et, dessous, « Progressive · 2 min », puis le chevron. La petite courbe et « Volume max en 2 min » quittent l'accueil ; cette phrase reste dans la feuille de sonnerie, sous sa forme « Volume max en 2 min », confirmée le même jour. Quatre versions avaient été dessinées page « Choix de la sonnerie », dernière rangée ; c'est la 3 qui est gardée. Elle est appliquée aux quatorze maquettes de la page « Écrans validés » où l'accueil apparaît, en clair, en grisé et en sombre. « Carillon » est un exemple de nom. À valider : quand la sonnerie progressive est sur « Non », la ligne n'affiche que le nom du son.

## Écran 7 · Écran de blocage

Il s'affiche quand l'utilisateur ouvre une application bloquée. Il dit pourquoi, et comment en sortir.

Maquette gardée pour le moment : « B » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 7 · Écran de blocage », choisie le 6 octobre 2026. Le fond est blanc et l'icône est le logo niumi. Dessous viennent le titre, le texte et les deux boutons du tableau. Sur iPhone, la taille du logo dans l'emplacement de l'icône reste à tester sur un appareil.

| Élément | Contenu |
| --- | --- |
| Titre | Cette application est bloquée |
| Texte | Elle reviendra après avoir scanné ton Niumi Point |
| Bouton principal | Fermer |
| Bouton secondaire | Ouvrir Niumi |

Sur iPhone, cet écran appartient au système. Seuls le fond, l'icône, le titre, le texte et les libellés des boutons se personnalisent. Sur Android, il est entièrement à dessiner.

Le bouton « Ouvrir Niumi » mène à l'accueil. Vérifié le 6 octobre 2026 : sur iPhone, ce n'est pas réalisable directement. Un bouton de cet écran peut seulement le fermer, et Apple confirme qu'aucun moyen prévu n'ouvre l'application depuis là. Le seul contournement connu est d'envoyer une notification que l'utilisateur touche pour ouvrir Niumi. Reste à décider si on garde ce bouton sur iPhone.

## Écrans 8 et 9 · Sortie de secours

La sortie de secours remplace le scan d'un Niumi Point, au prix d'un long texte à recopier. Elle compte comme un scan et a le même effet. Elle s'ouvre depuis l'accueil en session et depuis l'écran de réveil.

Maquettes gardées le 7 octobre 2026, dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écrans 8 et 9 · Sortie de secours ». Pour l'écran 8, « 8B » : la confirmation est une feuille qui monte du bas, par-dessus l'accueil en session le soir et par-dessus l'écran de réveil le matin. Pour l'écran 9, « 9A » : « Annuler » et le compteur en haut, la consigne, le texte modèle dans un encadré gris, le champ de saisie dessous, puis « Sortir de la session » au-dessus du clavier. En cas d'erreur, le premier mot qui diffère est surligné dans le modèle et souligné dans la saisie, avec le message « Un mot ne correspond pas. Il est signalé dans le texte. » (texte validé le 7 octobre 2026). Quand le texte est complet, le bouton devient noir.

### Écran 8 · Avertissement

| Élément | Contenu |
| --- | --- |
| Titre | Sortir de la session sans scanner ton Niumi Point ? |
| Texte, le soir ou la nuit | Recopier un long texte remplace le scan de ton Niumi Point. |
| Texte, pendant l'alarme | L'alarme s'arrêtera et tes applications seront débloquées. Pour sortir, tu devras recopier un long texte. |
| Bouton principal | Rester dans la session |
| Bouton secondaire | Recopier le texte |

Le bouton principal est celui qui fait rester. Sortir demande un geste volontaire sur le second.

Le soir, la sortie de secours annule la session, comme le ferait le scan : le blocage est levé et l'interrupteur « Réveil activé » est éteint. Le matin, pendant l'alarme, elle arrête la sonnerie et débloque les applications.

### Écran 9 · Recopie

| Élément | Contenu |
| --- | --- |
| Consigne | Recopie ce texte pour sortir de la session. |
| Texte modèle | Affiché en entier au-dessus du champ |
| Champ de saisie | Vide au départ, collage désactivé |
| Progression | 84 sur 200 caractères. La longueur du texte reste à ajuster |
| Erreur | Le premier mot qui diffère est signalé dans le texte modèle |
| Bouton | « Sortir de la session », actif quand le texte est complet et juste |
| Abandon | « Annuler » ramène à l'écran d'où l'on vient. La saisie est perdue |

Règles de comparaison : les majuscules et les accents sont ignorés, comme tu l'as demandé. La ponctuation et les espaces en double le sont aussi.

Le texte change à chaque sortie, pour qu'un raccourci clavier ne suffise pas. Exemple :

> J'avais décidé de bloquer mes applications ce soir et de me lever demain à 7:00. Je choisis d'arrêter cette session maintenant. Mes applications seront débloquées et mon alarme de demain sera annulée.

Décision du 7 octobre 2026 : un seul texte sert dans tous les cas, le soir comme le matin. L'exemple ci-dessus ne convient donc plus, car il parle de « ce soir » et de « demain à 7:00 ». Texte validé le 7 octobre 2026, 197 caractères :

> J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Point. Je choisis de sortir de cette session sans le scanner. Je recopie ce texte en entier pour confirmer que c'est mon choix.

Le texte change toujours à chaque sortie (règle confirmée le 7 octobre 2026). Quelques autres textes neutres de ce genre seront choisis plus tard.

Après la recopie, la session est annulée, comme après un scan. L'accueil affiche le message « Session annulée. Tes applications sont débloquées. Ton réveil est désactivé. »

Sur Android, le matin, la sonnerie baisse tant que la saisie avance et remonte si elle s'arrête.

## Écran 10 · Réveil

L'écran ne dit qu'une chose : va scanner ton Niumi Point. Il n'a aucun bouton d'arrêt ni de report.

Maquette gardée pour le moment : « D2c » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 10 · Réveil », choisie le 2 octobre 2026. De haut en bas : la date, l'heure en grand, un dessin du téléphone approché du Niumi Point, « Scanne ton Niumi Point » en gros, le bouton et le lien discret. L'écran n'indique pas la pièce, car plusieurs Niumi Points peuvent être enregistrés.

| Élément | Contenu |
| --- | --- |
| Heure | L'heure actuelle, en grand, avec la date au-dessus |
| Consigne | Scanne ton Niumi Point |
| Bouton | « Scanner le Niumi Point », sur iPhone comme sur Android (décision du 8 octobre 2026). Il ouvre la feuille « Prêt à scanner » |
| Lien discret | « Je n'ai pas accès à mon Niumi Point ». Il ouvre la sortie de secours |

| Cas d'échec | Texte |
| --- | --- |
| Pas un Niumi Point | Ce n'est pas un Niumi Point. |
| Niumi Point non enregistré | Ce Niumi Point n'est pas enregistré. |
| Lecture ratée | Ton Niumi Point n'a pas pu être lu. Rapproche ton téléphone et réessaie. |
| NFC coupé (Android) | Le NFC est coupé. Active-le pour scanner ton Niumi Point. |

Aucun de ces cas n'arrête l'alarme ni ne débloque les applications.

Affichage d'un échec, maquettes en cours : quatre versions (A à D) dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 10 · Réveil », rangée du bas, proposées le 8 octobre 2026 avec le cas « Ce n'est pas un Niumi Point. » On garde « B4 » (8 octobre 2026) : une feuille du bas affiche le message, une phrase d'aide, le bouton « Réessayer » et le lien « Je n'ai pas accès à mon Niumi Point ». Elle est appliquée aux quatre cas du tableau ; pour le NFC coupé, le bouton est « Ouvrir les réglages ». Phrases d'aide validées le 8 octobre 2026, sans « Ton alarme continue. » devant : « Scanne un Niumi Point que tu as enregistré. », « Rapproche ton téléphone et réessaie. » et « Active-le pour scanner ton Niumi Point. » L'état iPhone « Ton alarme va reprendre. » est validé le 8 octobre 2026, pour l'iPhone seulement, et dessiné sur la même rangée. Vérifié le même jour dans la documentation d'Apple : sur iPhone, la feuille de scan est celle du système. Niumi peut en changer le texte pendant que le scan continue, ou la fermer en affichant un message d'erreur, mais ne peut pas y ajouter de bouton. B1 et B2 sont donc faisables dans la feuille du système ; B3 et B4 demandent sur iPhone une feuille dessinée par Niumi, affichée après la fermeture de celle du système.

Feuille de scan du matin : quatre versions dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 10 · Réveil », dernière rangée, proposées le 9 octobre 2026. La feuille « Prêt à scanner » monte par-dessus l'écran de réveil assombri, en sombre. Sur iPhone elle est dessinée par le système et Niumi n'y choisit que la phrase, donc les quatre versions ne diffèrent que par la phrase : 1, celle de l'écran 3, « Approche ton iPhone du Niumi Point » ; 2, avec le but, « Approche ton iPhone du Niumi Point pour arrêter l'alarme » ; 3, avec la pièce, « Approche ton iPhone du Niumi Point « Cuisine » » ; 4, la phrase de l'écran 3 puis, après 20 secondes sans lecture, un conseil, « Pose le haut de ton iPhone sur le Niumi Point ». Ces phrases et le délai de 20 secondes sont proposés par Claude. Sur Android, « ton iPhone » devient « ton téléphone », et l'endroit à poser dépend du modèle.

Limite vérifiée le 9 octobre 2026 : sur iPhone, une lecture NFC dure 60 secondes au plus, après quoi il faut en lancer une nouvelle, et l'application doit rester visible au premier plan (présentation d'Apple « Introducing Core NFC », WWDC 2017). La durée n'est pas écrite dans les pages de documentation lues ce jour-là, qui mentionnent seulement une erreur « The reader session timed out ». Conséquence : si l'utilisateur touche « Scanner le Niumi Point » depuis son lit et met plus d'une minute à atteindre son Niumi Point, la feuille se ferme d'elle-même. Ce qui se passe alors est décidé au paragraphe suivant. À vérifier sur un iPhone : si Niumi peut rouvrir la feuille sans nouveau geste de l'utilisateur.

Décisions du 9 octobre 2026. Phrase gardée pour la feuille de scan du matin : la 1, « Approche ton iPhone du Niumi Point », comme à l'écran 3 (« ton téléphone » sur Android). La maquette est ajoutée à la page « Écrans validés », après « Le réveil sonne ». Sur iPhone, quand la feuille se ferme toute seule après 60 secondes, Niumi affiche un message dédié, dans la feuille d'échec déjà gardée, avec « Réessayer » et le lien de secours. Sur Android, il n'y a pas de limite : la feuille reste ouverte jusqu'au scan ou jusqu'à « Annuler ». C'est une différence voulue entre les deux.

Texte du message dédié, en cours : quatre propositions de Claude sur la page « Écran 10 · Réveil », dernière rangée. 1, « Le temps de scan est écoulé. » puis « Relance le scan une fois devant ton Niumi Point. » ; 2, « Le scan s'est arrêté. » puis « Il dure une minute au plus. Relance-le devant ton Niumi Point. » ; 3, « Aucun Niumi Point détecté. » puis « Va jusqu'à ton Niumi Point, puis réessaie. » ; 4, « Pas de Niumi Point à portée. » puis « Le scan s'arrête après une minute. Réessaie quand tu y es. » Le choix est au paragraphe suivant.

Décidé le 9 octobre 2026 : on garde le texte 1, « Le temps de scan est écoulé. » puis « Relance le scan une fois devant ton Niumi Point. », avec le bouton « Réessayer » et le lien « Je n'ai pas accès à mon Niumi Point ». La maquette est ajoutée à la page « Écrans validés », rangée « Écran 10 ». Ce message sert pour tous les scans de l'iPhone qui dépassent 60 secondes : le matin, à l'annulation d'une session, à l'association et à l'ajout d'un Niumi Point. Seule la version du matin est dessinée. Hors session, la feuille sera claire, comme les autres ; cette version et celle de l'annulation restent à dessiner. À l'association et à l'ajout d'un Niumi Point, le lien de secours est remplacé par un lien « Annuler », qui referme la feuille sans relancer le scan (décision du 9 octobre 2026).

Sur iPhone, l'alarme qui sonne est d'abord présentée par le système, avec une commande d'arrêt et un bouton qui ouvre Niumi. Cette commande ne peut pas être retirée, et les boutons physiques arrêtent aussi la sonnerie en cours. Niumi relance donc l'alarme tant que le scan n'a pas eu lieu. L'arrêt ne termine pas la session et les applications restent bloquées. L'écran affiche alors : « Ton alarme va reprendre. Scanne ton Niumi Point pour l'arrêter. »

Relu le 8 octobre 2026. Un ingénieur d'Apple confirme sur le [forum des développeurs](https://developer.apple.com/forums/thread/797158) que tout bouton physique arrête l'alarme en cours, et que l'application est prévenue de l'arrêt. La [documentation d'AlarmKit](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit.md) indique que le système gère lui-même la commande d'arrêt de l'alerte. Risque à tester sur un iPhone : dans un [autre fil du forum](https://developer.apple.com/forums/thread/815064), un développeur signale sous iOS 26.2.1 que l'application n'est pas prévenue quand l'alarme est écartée d'un geste sur l'écran déverrouillé ou sur la bannière ; Apple n'a pas encore répondu sur le fond. La relance de l'alarme par Niumi dépend de cet avertissement.

Sur Android, l'écran est à dessiner en entier et la sonnerie continue jusqu'au scan.

## Écran 11 · Réussite

Il confirme que le scan a marché et que les applications sont revenues. Il reste affiché jusqu'à ce que l'utilisateur le ferme.

Maquette gardée pour le moment : « Textes 2b » dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 11 · Réussite », choisie le 6 octobre 2026. En haut, le point ambre du logo se lève comme un soleil au-dessus d'une ligne d'horizon. Dessous viennent les textes du tableau, puis le bouton.

Écran repris le 9 octobre 2026 pour être plus soigné. Quatre mises en page sont dessinées sur la page « Écran 11 · Réussite », dernière rangée, avec les textes validés et les couleurs des écrans clairs : 1, un grand soleil à l'horizon, coupé par le bord droit, et les textes alignés à gauche ; 2, le cadran de l'accueil, la nuit entièrement parcourue en ambre, avec « Debout à » et « 7:04 » au centre ; 3, un soleil entier entouré d'un halo de cercles ; 4, un très grand soleil qui descend du haut de l'écran et contient le titre. Aucune n'est gardée : un simple effet de couleur est préféré (9 octobre 2026). Dans la version 2, le titre « Debout à 7:04. » est coupé en deux et perd son point. Dans les versions 3 et 4, la phrase « Bravo ! » n'a plus de fond coloré.

Effet de couleur : quatre dégradés sur la même page, rangée suivante, proposés le 9 octobre 2026. 1, un ciel d'aube sur tout l'écran, de l'ambre en haut au blanc cassé en bas, sans dessin, avec la phrase « Bravo ! » sur fond blanc ; 2, une lueur ambre qui monte du bas de l'écran, derrière le bouton, sans dessin ; 3, le soleil actuel avec un ciel en dégradé au-dessus de l'horizon, la ligne d'horizon allant d'un bord à l'autre ; 4, l'écran actuel, seul le soleil passe en dégradé. Les textes validés ne changent pas. Contrastes mesurés sur le rendu : le texte noir reste au-dessus de 16:1 et le texte gris au-dessus de 5,6:1 là où ils sont posés. Ces dégradés n'utilisent que l'ambre et ses versions éclaircies.

Décidé le 9 octobre 2026 : on garde le dégradé 4 pour l'instant, validé tel quel. L'écran 11 reste celui d'avant, et seul le soleil change : ambre en haut (#E9A23B), il s'éclaircit jusqu'à un ambre très clair (#F8DDB0) à la ligne d'horizon. La maquette de la page « Écrans validés » est mise à jour. C'est le seul dégradé des écrans clairs.

| Élément | Contenu |
| --- | --- |
| Titre | Debout à 7:04. |
| Délai depuis la sonnerie | 4 minutes après la sonnerie. |
| Comparaison | Bravo ! C'est 11 minutes de moins qu'avant Niumi (exemple corrigé le 7 octobre 2026 : 15 min déclarées, levé en 4 min). Affiché seulement si l'utilisateur s'est levé plus vite que le temps déclaré à l'écran 3 bis. Sinon, rien ne s'affiche à la place. |
| Confirmation | Tes applications sont de nouveau accessibles. |
| Bouton | Retour à l'accueil |

« Retour à l'accueil » ramène à l'accueil, dans l'état « Réveil prévu ».

## Écran 12 · Réglages

Les réglages regroupent ce qu'on configure une fois. Tout ce qui décrit la prochaine nuit reste sur l'accueil.

Maquettes gardées le 7 octobre 2026, dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Écran 12 · Réglages », rangée du bas. Les réglages sont la liste simple « A » : une flèche de retour, le titre « Réglages » et quatre lignes, Niumi Points, Autorisations, Aide, À propos. Le temps pour sortir du lit avant Niumi n'y figure pas. Il se trouve dans l'aide (variante « A3 ») : l'écran « Aide » a une rubrique « Temps gagné », dont la page explique le calcul, affiche la valeur et propose « Modifier ». Il n'y a pas de lien depuis l'écran de réussite.

| Ligne | Contenu | Pendant une session |
| --- | --- | --- |
| Niumi Points | Liste des Niumi Points enregistrés, chacun avec sa pièce. « Ajouter un Niumi Point », « Renommer la pièce », « Supprimer » | Ajout et suppression possibles. Il doit toujours en rester au moins un |
| Autorisations | L'état des autorisations de l'écran 2. La toucher ouvre l'écran 2 pour les revoir ou les réparer. Ligne ajoutée le 7 octobre 2026 | Consultable |
| Temps pour sortir du lit | N'est plus une ligne des réglages (décision du 7 octobre 2026). La durée déclarée à l'écran 3 bis se modifie dans Aide, rubrique « Temps gagné » | Modifiable (décision du 7 octobre 2026) |
| Aide | Fonctionnement, Niumi Point perdu, sortie de secours, temps gagné. La rubrique « Temps gagné » explique le calcul, affiche le temps pour sortir du lit avant Niumi et propose « Modifier », qui ouvre la roue | Consultable |
| À propos | Version, mentions légales | Consultable |

Plusieurs Niumi Points peuvent être enregistrés, et n'importe lequel termine une session. L'ajout et la suppression restent possibles pendant une session, mais il doit toujours rester au moins un Niumi Point. Supprimer le dernier est refusé avec « Tu dois garder au moins un Niumi Point. » La sonnerie et le volume sont sur l'accueil.

Liste des Niumi Points, maquette gardée le 8 octobre 2026 : « B », dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Réglages · Niumi Points ». Chaque ligne montre le Niumi Point et sa pièce, avec un bouton « trois points » qui ouvre un menu « Renommer la pièce » et « Supprimer ». « Ajouter un Niumi Point » est la dernière ligne. Décisions du même jour : « Supprimer » demande une confirmation dans une feuille du bas, « Supprimer le Niumi Point « Cuisine » ? », avec « Supprimer » et « Annuler » ; « Renommer la pièce » rouvre la feuille des pièces de l'écran 3, la pièce actuelle sélectionnée, avec le bouton « Enregistrer ». Quand il ne reste qu'un Niumi Point, « Supprimer » est grisé dans le menu, avec « Tu dois garder au moins un Niumi Point. » Textes à valider : « N'importe lequel de tes Niumi Points arrête l'alarme. », « Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter. » et « Enregistrer ».

Aide et À propos, maquettes gardées le 8 octobre 2026, dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Aide et À propos ». Pour une page de rubrique d'aide, « A » : une flèche de retour, le titre de la rubrique et des paragraphes de texte avec un intertitre chacun. La page « Fonctionnement », prise en exemple, reprend les phrases déjà validées des écrans 1 et 11 sous les intertitres « Le soir », « Le matin » et « Après le scan ». Pour « À propos », « B » : le logo en grand et la version au centre, puis deux liens en bas, « Mentions légales » et « Politique de confidentialité ». Ce second lien a été ajouté parce que les règles de l'App Store demandent un lien vers cette politique dans l'application elle-même. L'éditeur et le contact ne sont pas affichés. Reste à écrire : le contenu des rubriques « Niumi Point perdu » et « Sortie de secours ».

Ajouté le 9 octobre 2026 sur « À propos », en clair et en sombre : un troisième lien « Licences », sous les deux autres. Il ouvrira une page qui porte la mention de droit d'auteur et le texte de la licence de la police (voir « Police »). Le mot « Licences » et sa place sont validés le 9 octobre 2026. La page que le lien ouvre sera dessinée plus tard (décision du même jour).

## Hors écrans

Deux éléments ne sont pas des écrans mais demandent des textes : la notification du soir et le bandeau de problème.

### Notification du soir

Elle part 30 minutes avant chaque coucher et ne porte aucun bouton. La toucher ouvre l'accueil. Elle n'est pas envoyée si le réveil a été annulé.

| Élément | Contenu |
| --- | --- |
| Titre | Tu te réveilles toujours demain à 7:00 ? |
| Texte | Ton heure de coucher est programmée à 22:30, 6 applications seront bloquées. |

### Bandeau de problème

Il s'affiche en haut de l'accueil tant que le problème existe. Un seul bandeau à la fois, le plus grave d'abord.

Depuis le 8 octobre 2026, le bandeau ne sert plus pour l'alarme, le blocage et les notifications : l'alerte d'autorisations le remplace (voir plus bas). Il reste prévu pour « NFC coupé » et « Aucun Niumi Point ». Les trois lignes concernées du tableau ne servent plus que pour leurs phrases, reprises dans l'alerte.

Maquette gardée le 8 octobre 2026 : « A », dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Accueil · Bandeau de problème ». C'est un bandeau noir tout en haut de l'accueil, avec un symbole d'avertissement, la phrase du tableau et son lien souligné. Le cadran et l'icône des réglages descendent d'autant. Les deux cas sont dessinés : « NFC coupé », sur Android seulement, et « Aucun Niumi Point ». Ce second cas ne devrait pas arriver, puisque l'association est obligatoire au départ et que supprimer le dernier Niumi Point est refusé ; il est gardé par précaution.

| Problème | Texte | Bouton |
| --- | --- | --- |
| Blocage non autorisé | Tes applications ne seront pas bloquées : l'autorisation a été retirée. | Rétablir |
| Alarme non autorisée | Ton réveil ne pourra pas sonner. | Autoriser |
| NFC coupé (Android) | Le scan du Niumi Point ne fonctionnera pas : le NFC est coupé. | Activer le NFC |
| Aucun Niumi Point | Aucun Niumi Point n'est associé. | Associer un Niumi Point |
| Notifications coupées | On risque de ne pas pouvoir te transmettre les informations nécessaires. | Activer |

### Alerte d'autorisations

Décision du 8 octobre 2026 : si une autorisation est retirée après l'installation, une fenêtre d'alerte s'affiche à chaque ouverture de l'application. Elle signale le problème et propose un lien vers le menu des autorisations, c'est-à-dire la ligne « Autorisations » des réglages, qui ouvre l'écran 2.

Maquette gardée le 8 octobre 2026 : « A », dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Alerte · Autorisations retirées ». C'est une fenêtre centrée par-dessus l'accueil : le titre « Il manque une autorisation », la phrase de conséquence, le bouton « Voir les autorisations » et le lien « Plus tard ». Une seule alerte liste tout ce qui manque : avec plusieurs autorisations retirées, le titre devient « Il manque des autorisations » et chaque autorisation a sa ligne, avec sa conséquence. Validés le 8 octobre 2026 : le titre au pluriel et, pour le blocage, la phrase raccourcie « Tes applications ne seront pas bloquées. » Les phrases de conséquence reprennent celles du bandeau.

Décision du 8 octobre 2026 : cette alerte remplace le bandeau de l'accueil pour l'alarme, le blocage et les notifications.

## Palette de couleurs

En cours depuis le 8 octobre 2026. Seule couleur imposée : l'ambre. Six propositions sont dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Palette de couleurs », appliquées à l'écran 6 « Réveil prévu » : trois claires (Encre, Sable, Sauge) et trois sombres (Nuit, Bleu nuit, Crépuscule). Aucune n'est encore choisie.

Le même jour, Encre et Nuit sont retenues pour être creusées, sans préférence encore entre clair et sombre. Quatre versions de chacune sont sur la même page ; elles gardent les couleurs du logo et changent la place de l'ambre. Encre : 1, cadran noir et arc ambre ; 2, commandes ambre bordées de noir ; 3, carte noire et interrupteur ambre ; 4, haut de l'écran sombre avec le cadran ambre. Nuit : 1, cadran clair et point ambre ; 2, ambre partout ; 3, carte ambre ; 4, chiffres ambre. Ajoutée à la demande : Nuit 3 bis, c'est-à-dire Nuit 3 avec la carte sombre et l'interrupteur ambre de Nuit 2.

Piste du 8 octobre 2026, à confirmer : la palette suit l'état de l'accueil, et non le réglage clair ou sombre du téléphone. Avant l'heure du coucher, l'écran est clair (Encre 3) : la carte du réveil est noire avec un interrupteur ambre quand le réveil est activé, blanche quand il est désactivé. Une fois l'heure du coucher passée avec le réveil activé, l'écran passe en sombre (Nuit 3 bis). Les trois états sont dessinés sur la page « Palette de couleurs », rangée du bas. Pour le troisième, les couleurs de Nuit 3 bis sont appliquées à l'écran « Session en cours », qui n'a ni interrupteur ni poignées : l'ambre y est porté par le point qui avance sur le cadran, petit et sans contour, et par le cadenas de la carte, au trait épais (demandé le 8 octobre 2026). À noter : ce point ambre sans contour, posé sur le cadran clair, a un contraste de 2,1:1, sous le repère de 3:1. Décidé le même jour : la règle vaut pour tous les écrans, sombres tant qu'une session est en cours, clairs sinon. L'écran de réveil (écran 10) est donc sombre ; pendant que l'alarme sonne, c'est lui qui s'affiche, pas l'accueil. Reste à faire : appliquer la règle aux autres écrans, et choisir la couleur du bouton principal sur fond sombre.

Bouton principal, décidé le 8 octobre 2026 : sur fond sombre, il est blanc cassé avec un texte noir ; sur fond clair, noir avec un texte blanc. Quatre paires avaient été dessinées sur l'écran de réveil, page « Palette de couleurs », deux dernières rangées ; c'est la paire 2 qui est gardée. Décidé le même jour : le point du Niumi Point est ambre dans tous les dessins de l'application, en clair comme en sombre.

Écrans de la session en sombre : 17 maquettes dessinées le 8 octobre 2026 dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), aujourd'hui sur la page « Écrans validés ». Elles couvrent l'accueil pendant la session, l'annulation, l'écran de blocage, la sortie de secours, le réveil et ses échecs. Deux écrans y sont clairs parce que la session est finie : l'accueil après une annulation et l'écran de réussite. Couleurs validées le 9 octobre 2026 : les feuilles du bas sont en gris très sombre (#1B1B1F) avec un liseré (#2E2E33) ; dans la recopie, le mot signalé a un fond brun ambré (#3D2C0E), souligné d'ambre ; le bouton inactif a un fond gris sombre (#2A2A2F) et un texte gris (#74747B) ; le bouton « Annuler » de la feuille de scan est gris (#34343A) ; les jours grisés pendant la session ont un fond #4A4A50. Le logo sur fond noir a été ajouté au canevas pour l'écran de blocage.

Réglages pendant une session, en sombre : neuf maquettes dessinées et validées le 9 octobre 2026, ajoutées à la page « Écrans validés » sous leurs versions claires. Ce sont les réglages, l'aide, une rubrique d'aide, « Temps gagné », « À propos » et les quatre états de la liste des Niumi Points. Les mises en page et les textes sont ceux des versions claires ; les couleurs sont celles des écrans sombres. Le menu « trois points » est en gris très sombre (#1B1B1F) avec un bord #3A3A40. Dans la feuille « Renommer la pièce », la case choisie est blanc cassé avec un texte noir.

Écrans hors session en clair : 28 maquettes dessinées le 8 octobre 2026, aujourd'hui sur la page « Écrans validés » du même canevas. Elles couvrent la mise en route (écrans 1 à 4), l'accueil avant le coucher, la sonnerie, le bandeau, l'alerte d'autorisations, les réglages, l'aide, À propos et les Niumi Points. Couleurs : fond blanc cassé (#FAFAF8), feuilles et cartes blanches, noir du logo (#0E0E10), ambre (#E9A23B). Les gris des maquettes noir et blanc sont repris presque tels quels, légèrement réchauffés. Un logo à fond transparent a été ajouté au canevas, parce que l'ancien avait un fond blanc qui se serait vu sur le blanc cassé. Sur ces écrans, l'ambre n'apparaît que là où il était déjà (points des Niumi Points) et sur l'accueil (interrupteur, poignée du réveil) : rien n'a été ajouté ailleurs.

Plus d'ambre sur les écrans clairs, décidé le 8 octobre 2026 : l'ambre vit dans les dessins, un détail par dessin. Quatre idées avaient été dessinées sur l'écran 1, page « Palette de couleurs » ; les trois autres (mot surligné, aplat ambre clair, progression) ne sont pas gardées. En pratique, un seul dessin n'avait pas encore d'ambre : celui du panneau 1 de l'écran 1, dont la pastille du cadenas devient ambre avec un cadenas noir. Les autres l'avaient déjà : le point du Niumi Point (écran 1 panneau 2, écran 3, écran 10) et le soleil de l'écran 11. Les icônes (autorisations, pièces, réglages) restent noires. Un essai rassemble ces dessins sur la même page ; il a été validé le 8 octobre 2026. Les icônes sont à l'étude : quatre idées sont dessinées sur l'écran 2, dernière rangée de la page (la coche « accordée » en ambre, des pastilles ambre, un détail ambre par icône, un carré noir au trait ambre) ; aucune n'est gardée. Décision du 8 octobre 2026 : pas d'ambre dans les icônes, elles restent noires.

Cadran de l'écran 6 en sombre, en cours : une note ambre y est demandée le 8 octobre 2026, en plus du petit point qui marque l'heure. Quatre versions sont dessinées page « Palette de couleurs », dernière rangée : 1, un point ambre à l'heure du réveil ; 2, le bout de l'arc en ambre, côté réveil ; 3, les chiffres du centre en ambre ; 4, l'arc qui se remplit d'ambre au fil de la nuit. Aucune n'est gardée telle quelle : la piste d'un contour ambre est préférée. Quatre contours sont dessinés sur la rangée suivante : 1, un liseré tout autour de l'arc ; 2, un trait le long du bord extérieur de l'arc ; 3, un anneau fin autour du cadran entier ; 4, l'arc réduit à son contour. Aucun n'est gardé : les traits fins ne fonctionnent pas. Quatre notes plus douces sont dessinées sur la rangée suivante : une lueur ambre autour de l'arc, la piste du cadran en ambre sombre (#3B2B12), l'arc en dégradé du blanc cassé vers l'ambre côté réveil, une lueur au bout de l'arc. Aucune n'est gardée telle quelle. Piste suivante, proposée le même jour : un dégradé ambre qui suit le point de l'heure. Quatre versions sont dessinées sur les deux rangées du bas, à 23:10 puis à 3:00 pour voir le dégradé avancer : 1, un halo ambre autour du point, sur l'arc ; 2, une traîne ambre derrière le point ; 3, un dégradé du blanc cassé à l'ambre, du coucher jusqu'au point ; 4, une lueur autour du point, hors de l'arc. Dans les versions 1 à 3, le point devient noir pour rester visible sur l'ambre. Le halo (version 1) plaît, avec une demande : que l'ambre parte du coucher et aille jusqu'au point. C'est dessiné sur les trois rangées suivantes, à 23:10, 3:00 et 6:00, avec trois longueurs de fondu devant le point (celle du halo, une courte, une longue). Décidé le 8 octobre 2026 : on garde le fondu du halo, et l'arc qui devient presque entièrement ambre en fin de nuit est voulu. La page « Écrans validés » est mise à jour : accueil pendant la session, et les trois feuilles posées dessus (annulation, scan, sortie de secours). Le point de l'heure est noir (décidé le 8 octobre 2026, après un essai en blanc cassé dessiné sur les trois dernières rangées de la page « Palette de couleurs ») : 8,9:1 sur l'ambre, contre 2,1:1 pour le blanc cassé. Le cadran sombre est donc fixé : arc blanc cassé, ambre du coucher jusqu'au point, fondu du halo devant le point, point noir. Depuis la série des notes douces, le rendu est contrôlé par Claude dans un navigateur, hors du canevas.

Forme de l'arc du cadran, décidé le 8 octobre 2026 : on garde les bouts ronds. Un arc moins arrondi a été essayé (bouts droits, coins de 4, coins de 8), page « Palette de couleurs », deux dernières rangées ; aucune de ces formes n'est retenue.

Correction du 8 octobre 2026 sur l'écran 8, le soir : la fin de phrase « Ton choix, modifier ou annuler, s'appliquera ensuite. » est retirée, puisque « Modifier » n'existe plus.

À vérifier sur un iPhone. La feuille de scan est celle du système : rien, dans ce qui a été consulté le 8 octobre 2026, ne dit si elle suit le réglage clair ou sombre du téléphone ou l'apparence de l'application. L'écran de blocage est lui aussi affiché par le système : Niumi peut en régler la couleur de fond, l'icône, les textes et la couleur du bouton principal, mais pas la disposition ([ShieldConfiguration](https://developer.apple.com/documentation/managedsettingsui/shieldconfiguration), Apple, lu le 8 octobre 2026).

Mesuré le 8 octobre 2026 dans les deux fichiers du logo : l'ambre est #E9A23B, le noir #0B0B0B sur fond blanc, et sur fond noir le fond est #0E0E10 et les lettres #FAFAF8. Les maquettes en noir et blanc utilisaient #E9A13B, à un cran près ; les palettes reprennent la valeur du logo.

Règle proposée, à valider. Sur un fond clair, l'ambre seul a un contraste d'environ 2:1, sous le repère de 3:1 demandé pour un élément graphique : il est donc toujours posé sur la couleur principale. Sur un fond sombre, il atteint 8:1 à 9:1 et peut porter le cadran. Les contrastes de chaque palette sont calculés et affichés dans le canevas.

- [Understanding SC 1.4.3, Contrast (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), W3C, lu le 8 octobre 2026 : 4,5:1 au moins pour un texte, 3:1 pour un grand texte.
- [Understanding SC 1.4.11, Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html), W3C, lu le 8 octobre 2026 : 3:1 au moins pour les composants d'interface et les éléments graphiques nécessaires à la compréhension.

## Police

Décidé le 9 octobre 2026 : on garde IBM Plex Sans, la police utilisée depuis le début dans toutes les maquettes. Aucune maquette n'est à retoucher. Trois graisses servent : normal (400), moyen (500) et demi-gras (600). Les cinq autres propositions ne sont pas gardées. Reste à prévoir dans l'app la mention « Copyright IBM Corp. » et le texte de la licence, sur la page ouverte par le lien « Licences » de la page À propos. Le lien est ajouté aux maquettes depuis le 9 octobre 2026, la page qu'il ouvre reste à dessiner.

Les six propositions comparées sont dans le canevas [Niumi · maquettes simples](https://claude.ai/artifact/6AkcFF2m36f9qFkiURpqC5), page « Police ». Une colonne par police : un échantillon, puis l'accueil « réveil prévu », le premier panneau de l'écran 1 et l'écran de réveil.

| N° | Police | Caractère | Chiffres |
| --- | --- | --- | --- |
| 1 | IBM Plex Sans | La police actuelle. Un peu technique, lettres légèrement carrées | De même largeur d'office |
| 2 | Inter | Neutre et très lisible, proche des polices d'origine des téléphones | De même largeur sur réglage |
| 3 | Outfit | Géométrique, faite de cercles : la plus proche des lettres du logo. Textes les plus courts en largeur | De même largeur sur réglage |
| 4 | Manrope | Moderne et ouverte | De même largeur sur réglage |
| 5 | Sora | Large et affirmée. Prend le plus de place : le titre de l'écran 1 passe sur quatre lignes | De même largeur sur réglage |
| 6 | Figtree | Douce et amicale | De même largeur sur réglage |

Vérifié le 9 octobre 2026 sur les fichiers des six polices (paquets Fontsource 5.3.0, graisse 500, jeu latin) : toutes contiennent les caractères du français (é è ê ë à â ç î ï ô ù û ü œ Œ É È À Ç, guillemets « », apostrophe ’, points de suspension, point médian). Les chiffres d'IBM Plex Sans ont tous la même largeur. Ceux des cinq autres ont des largeurs différentes par défaut, et un réglage de la police (« tnum ») les met à la même largeur. Ce réglage sert aux heures et au compte à rebours, pour que le texte ne bouge pas quand un chiffre change. DM Sans et Lexend ont été écartées parce que ce réglage manque dans les fichiers examinés.

Licence : le fichier de licence livré avec chacune des six polices indique la SIL Open Font License 1.1. Son [texte officiel](https://openfontlicense.org/open-font-license-official-text/) autorise à inclure la police dans un logiciel, même vendu. Il interdit de vendre la police seule et demande de joindre la mention de droit d'auteur et le texte de la licence. Pour Niumi, cela veut dire une ligne « Licences » dans la page À propos.

Précision vérifiée le 9 octobre 2026 : cette obligation est la même pour les six polices comparées, puisqu'elles ont toutes la même licence. Garder IBM Plex Sans n'ajoute donc aucune contrainte par rapport aux autres. La [FAQ officielle de la licence](https://openfontlicense.org/ofl-faq/) précise, pour une app mobile (question 1.20), qu'il faut au minimum la mention de droit d'auteur, la notice de licence et le texte de la licence, et suggère de les placer dans une boîte À propos. Seuls les fichiers de police réellement utilisés sont concernés. L'app elle-même n'a pas à être open source et peut être vendue (questions 1.3 et 1.4). La seule façon de ne rien avoir à afficher serait de n'embarquer aucune police et d'utiliser celle de chaque téléphone, solution écartée plus bas.

Autre possibilité, non dessinée : la police d'origine de chaque téléphone (San Francisco sur iPhone, Roboto sur Android). L'app n'aurait alors pas la même police sur les deux, ce qui va contre la règle « même fonctionnement sur Android et iPhone », et le canevas ne peut pas afficher San Francisco.

Note sur les contrôles de rendu : jusqu'au 9 octobre 2026, les rendus contrôlés par Claude avant publication étaient affichés avec Inter et non avec IBM Plex Sans, parce que Google Fonts n'est pas joignable depuis l'environnement de Claude. Les couleurs et les formes n'étaient pas touchées. Les largeurs de texte étaient approximatives. Inter étant un peu plus large qu'IBM Plex Sans, un texte qui tenait dans ces contrôles tient aussi dans le canevas. Les six polices sont maintenant installées dans l'environnement de Claude et les rendus de la page « Police » ont été contrôlés avec les vrais fichiers.

## Points à trancher

Deux points restent ouverts. Ils concernent l'iPhone : l'écran 10 et le volume de la sonnerie.

- [ ] Vérifier sur un iPhone que l'alarme se relance bien après l'arrêt, quelle que soit la façon de l'arrêter : commande à l'écran, boutons physiques, balayage.

* [ ] Tester sur un iPhone le volume de l'alarme. Avec AlarmKit, elle suit le volume de sonnerie du téléphone et Niumi ne peut pas le régler. Décider ensuite si le réglage du volume reste sur l'accueil, et comment obtenir une sonnerie progressive. Un son personnalisé d'AlarmKit doit durer moins de 30 secondes, donc une montée sur 2 minutes ne tient pas dans un seul fichier.

### Sources des contraintes iPhone et Android

Je n'ai lu que des extraits de ces pages. Elles sont à relire avant le développement.

- [Apple, programmer une alarme avec AlarmKit](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit.md)
- [Apple, présentation d'AlarmKit à la WWDC 2025](https://developer.apple.com/videos/play/wwdc2025/230/)
- [Notes sur l'API Temps d'écran d'Apple](https://wwdcnotes.com/documentation/wwdc21-10123-meet-the-screen-time-api/)
- [Android, alarmes exactes depuis Android 14](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

* [Apple, forum des développeurs : ce qui se passe quand on arrête une alarme AlarmKit](https://developer.apple.com/forums/thread/815064)
* [Apple, forum des développeurs : questions fréquentes sur AlarmKit](https://developer.apple.com/forums/thread/797158)

- [Apple, forum des développeurs : volume de l'alarme avec AlarmKit](https://developer.apple.com/forums/thread/813519)

* [Apple, forum des développeurs : durée des sons personnalisés avec AlarmKit](https://developer.apple.com/forums/thread/797172)

- [Apple, forum des développeurs : ouvrir l'application depuis l'écran de blocage](https://developer.apple.com/forums/thread/719905)

* [Apple, forum des développeurs : personnalisation de la fenêtre de scan NFC](https://developer.apple.com/forums/thread/837129)

- [FamilyActivityPicker](https://developer.apple.com/documentation/familycontrols/familyactivitypicker), documentation d'Apple, lue le 7 octobre 2026 : liste du système, dans la page ou en feuille, valeurs opaques, textes d'en-tête et de pied.
- [familyActivityPicker(title:headerText:footerText:isPresented:selection:)](<https://developer.apple.com/documentation/swiftui/view/familyactivitypicker(title:headertext:footertext:ispresented:selection:)>), documentation d'Apple, lue le 7 octobre 2026 : titre de la feuille, à partir d'iOS 26.2.
- [FamilyActivitySelection](https://developer.apple.com/documentation/familycontrols/familyactivityselection), documentation d'Apple, lue le 7 octobre 2026 : applications, catégories et sites choisis, option includeEntireCategory.
- [ManagedSettingsStore shield 50 token limit](https://developer.apple.com/forums/thread/733361), forum des développeurs Apple, lu le 7 octobre 2026 : témoignages de développeurs, sans réponse d'Apple dans le fil.
- [Use of the broad package (App) visibility (QUERY\_ALL\_PACKAGES) permission](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en), aide de Google Play, lue le 7 octobre 2026.

* [Is there any way to control alarm volume independently when using AlarmKit?](https://developer.apple.com/forums/thread/813519), forum des développeurs Apple, relu le 8 octobre 2026 : réponse d'un ingénieur d'Apple, AlarmKit suit le volume de sonnerie du système et n'a pas de réglage de volume.
* [AVAudioSession.outputVolume](https://developer.apple.com/documentation/avfaudio/avaudiosession/outputvolume), documentation d'Apple, lue le 8 octobre 2026 : « Only the user can directly set the system volume. »
* [AlarmKit Volume and Volume Buttons](https://developer.apple.com/forums/thread/800265), forum des développeurs Apple, lu le 8 octobre 2026 : un développeur signale une alarme plus faible que le son de l'application, sans réponse technique d'Apple.
* [AudioManager.setStreamVolume](<https://developer.android.com/reference/android/media/AudioManager#setStreamVolume(int,%20int,%20int)>), documentation d'Android, lue le 8 octobre 2026 dans sa reprise par Microsoft Learn : volume d'un flux, réserves sur le volume fixe et « Ne pas déranger ».
* [MediaPlayer.setVolume](<https://developer.android.com/reference/android/media/MediaPlayer#setVolume(float,%20float)>), documentation d'Android, lue le 8 octobre 2026 dans sa reprise par Microsoft Learn : volume propre au lecteur, de 0 à 1.

- [How to set and change alarms on your iPhone](https://support.apple.com/118444), assistance Apple, lue le 8 octobre 2026 : le volume de l'alarme se règle avec le curseur « Sonnerie et alertes ».
- [How to play sound using media volume instead of ring volume in iOS?](https://developer.apple.com/forums/thread/70451), forum des développeurs Apple, lu le 8 octobre 2026 : volume de la sonnerie et volume des médias sont deux réglages distincts.
- [AVAudioSession.outputVolume not reporting correctly in iOS 18+ devices](https://developer.apple.com/forums/thread/799104), forum des développeurs Apple, lu le 8 octobre 2026 : valeur pas toujours à jour, pas de contournement connu selon un ingénieur d'Apple.
- [AudioManager.getStreamVolume](<https://developer.android.com/reference/android/media/AudioManager#getStreamVolume(int)>), documentation d'Android, lue le 8 octobre 2026 dans sa reprise par Microsoft Learn : lecture du volume d'un flux.

* [NFCReaderSessionProtocol](https://developer.apple.com/documentation/corenfc/nfcreadersessionprotocol), documentation d'Apple, lue le 8 octobre 2026 : « invalidate(errorMessage:) » ferme la session de lecture et affiche un message d'erreur.

- [alertMessage](https://developer.apple.com/documentation/corenfc/nfcreadersessionprotocol/alertmessage), documentation d'Apple, lue le 8 octobre 2026 : le texte de la feuille de scan peut être mis à jour tant que la session est valide.
- [NFCTagReaderSession](https://developer.apple.com/documentation/corenfc/nfctagreadersession), documentation d'Apple, lue le 8 octobre 2026 : « restartPolling() » relance la recherche pour détecter un autre objet.

* [NFCReaderError](https://developer.apple.com/documentation/corenfc/nfcreadererror-swift.struct), documentation d'Apple, lue le 8 octobre 2026 : l'application est prévenue quand l'utilisateur ferme la feuille de scan ou quand elle expire.

- [App Review Guidelines, 5.1.1 (i)](https://developer.apple.com/app-store/review/guidelines/), Apple, lu le 8 octobre 2026 : toute application doit proposer un lien vers sa politique de confidentialité, facile d'accès, dans l'application.

* [SIL Open Font License 1.1, texte officiel](https://openfontlicense.org/open-font-license-official-text/), lu le 9 octobre 2026 : une police peut être incluse dans un logiciel, même vendu, si chaque copie contient la mention de droit d'auteur et la licence ; elle ne peut pas être vendue seule.
* [FAQ de la SIL Open Font License](https://openfontlicense.org/ofl-faq/), lue le 9 octobre 2026 : questions 1.3 et 1.4 (l'application n'a pas à être open source et peut être vendue) et 1.20 (applications mobiles : mention de droit d'auteur, notice et texte de la licence, par exemple dans une boîte À propos).
* [Introducing Core NFC](https://developer.apple.com/videos/play/wwdc2017/718), présentation d'Apple, WWDC 2017, transcription lue le 9 octobre 2026 : chaque lecture est limitée à 60 secondes, une nouvelle session est nécessaire ensuite, et l'application doit être visible au premier plan.
* [Make purchases using Apple Pay](https://support.apple.com/en-us/102626), assistance Apple, lue le 9 octobre 2026 : pour le sans-contact, c'est le haut de l'iPhone qu'il faut approcher du lecteur.
