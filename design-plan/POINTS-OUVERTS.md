# Niumi · Points ouverts

État au 9 octobre 2026. Tout ce qui suit n'est **pas** une décision. Avant d'implémenter un de ces points, il faut poser la question à Mehdi.

Cinq listes :

1. Décisions qui restent à prendre
2. Textes à relire
3. À tester sur un appareil
4. Ce qui n'est pas encore dessiné ou écrit
5. Écarts relevés entre l'ancien document et les maquettes

## 1. Décisions qui restent à prendre

| N° | Sujet | Question |
| --- | --- | --- |
| D1 | Écran 7, iPhone | Garde-t-on le bouton « Ouvrir Niumi » ? Sur iPhone, un bouton de l'écran de blocage peut seulement le fermer. Le seul contournement connu est une notification que l'utilisateur touche |
| D2 | Sortie de secours, le matin | Après la recopie pendant l'alarme, quel écran s'affiche : l'écran 11, ou l'accueil avec un message ? Et le réveil reste-t-il activé pour le lendemain ? Le journal donne deux pistes qui ne mènent pas au même endroit : « Elle compte comme un scan et a le même effet », et « Après la recopie, la session est annulée, comme après un scan. L'accueil affiche le message « Session annulée… » », sans distinguer le soir du matin |
| D3 | Écran 3 bis | « 1 heure et + » compte-t-il pour 60 minutes dans le calcul ? C'est une proposition, jamais confirmée |
| D4 | Écran 3 bis | Quelle durée est présélectionnée à l'ouverture de la roue ? La maquette montre 15 min |
| D5 | Accueil, ligne « Sonnerie » | Quand la sonnerie progressive est sur « Non », la ligne n'affiche-t-elle que le nom du son ? C'est l'hypothèse retenue |
| D6 | Feuille de sonnerie | Que montre la mention « Volume max en 2 min » quand la sonnerie progressive est sur « Non » ? Rien n'est dit |
| D7 | Sonnerie | Quels sont les vrais sons et leurs noms ? Ceux des maquettes sont des exemples |
| D8 | Sortie de secours | Quels autres textes à recopier ? Un seul est validé, et le texte doit changer à chaque sortie |
| D9 | Bouton inactif en clair | Deux styles coexistent dans les maquettes validées : fond `#E8E8E4` et texte `#5C5C60` sur l'écran 2, fond `#C6C6C2` et texte blanc sur l'écran 4. Lequel garder ? |
| D10 | Écran 2 | Le NFC n'a pas de tuile sur l'écran des autorisations, alors que l'ancien tableau le listait. Confirmer qu'il est géré seulement au moment du scan et par le bandeau |
| D11 | Écran 3, choix de la pièce | « Continuer » est-il grisé tant qu'aucune pièce n'est choisie ? Rien n'est dit |
| D12 | Ponctuation des titres | Le titre du panneau 1 de l'écran 1 n'a pas de point final, celui du panneau 2 en a un. Harmoniser ? |
| D13 | Apostrophes | Les maquettes utilisent l'apostrophe droite ('). Passer à l'apostrophe typographique (’) dans l'app ? |
| D14 | Règle de l'ambre | « Sur fond clair, l'ambre ne porte jamais de texte et reste posé sur l'encre ou dans un dessin. » Règle proposée par Claude, appliquée partout, jamais validée mot pour mot |
| D15 | Écran 11 | Le soleil en dégradé est gardé « pour l'instant ». L'écran peut encore changer |
| D16 | Règles communes | Dans la liste de ce qui est exclu de l'app, que vise exactement le mot « confirmation » ? L'app a depuis des confirmations (écran 8, suppression d'un Niumi Point) |
| D17 | Accueil | Que se passe-t-il quand plus aucun jour n'est coché ? L'ancien écran 5 affichait « Choisis au moins un jour. », mais cet écran est supprimé et rien n'est dit pour l'accueil |
| D18 | Écran 2 | Sur une tuile accordée, la maquette retire le bouton « ? » en même temps que « Autoriser ». Le guide doit-il rester accessible ? |
| D19 | Écran 2 | Le titre « Trois autorisations » est en taille 26 dans la maquette, alors que les autres titres d'écran sont en 28. Harmoniser ? |
| D20 | Sortie de secours, iPhone | Que fait l'alarme pendant la recopie sur iPhone ? Le journal ne le dit que pour Android : la sonnerie baisse tant que la saisie avance et remonte si elle s'arrête |

## 2. Textes à relire

Ces textes ont été écrits par Claude. Ils figurent dans des maquettes validées, mais rien n'indique qu'ils ont été approuvés mot pour mot. La liste est établie à partir du document et des maquettes : un texte peut s'y trouver alors que Mehdi l'a déjà approuvé de vive voix.

Les textes que Mehdi a choisis parmi plusieurs propositions ne sont pas dans cette liste.

| Écran | Texte |
| --- | --- |
| Écran 2 | Trois autorisations |
| Écran 2 | Niumi en a besoin pour fonctionner. Sans elles, tu ne peux pas continuer. |
| Écran 2, guide | Autoriser l'alarme |
| Écran 2, guide | Étape 1 sur 2 |
| Écran 2, guide | Une fenêtre du téléphone s'ouvre. Touche « Autoriser ». |
| Écran 2, guide | Suivant |
| Écran 3 bis | Combien de temps mets-tu pour sortir du lit ? |
| Écran 3 bis | Compte à partir de la première sonnerie. Une estimation suffit. |
| Écran 3 bis | Niumi s'en servira pour te montrer le temps que tu gagnes. |
| Écran 4 | Tu pourras changer ce choix plus tard, depuis l'accueil. |
| Écran 4, liste Android | Rechercher |
| Écran 4, liste Android | Valider |
| Sonnerie | Il se règle dans les réglages de ton téléphone. |
| Accueil en session | Modifiable après avoir scanné ton Niumi Point. |
| Annulation, Android | Approche ton téléphone du Niumi Point pour annuler la session |
| Niumi Points | N'importe lequel de tes Niumi Points arrête l'alarme. |
| Niumi Points | Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter. |
| Niumi Points, renommer | Enregistrer |
| Aide | Les intertitres « Le soir », « Le matin », « Après le scan » |
| Aide, Temps gagné | Après chaque réveil, Niumi compare le temps que tu as mis à te lever avec celui que tu mettais avant. |
| Aide, Temps gagné | Temps pour sortir du lit avant Niumi |
| Alerte d'autorisations | Voir les autorisations |
| Alerte d'autorisations | Plus tard |
| Message « temps écoulé », association et ajout | Annuler (le libellé du lien) |

## 3. À tester sur un appareil

### iPhone

| N° | Quoi | Pourquoi |
| --- | --- | --- |
| T1 | L'alarme se relance après un arrêt, quelle que soit la façon de l'arrêter : commande à l'écran, boutons physiques, balayage | La relance dépend d'un avertissement du système. Un développeur signale sous iOS 26.2.1 qu'il n'arrive pas quand l'alarme est écartée d'un geste. Apple n'a pas répondu sur le fond |
| T2 | Lire le volume de sonnerie du téléphone | La seule valeur lisible est décrite comme le volume de sortie du système, et elle ne serait pas toujours à jour depuis iOS 18. Si la lecture n'est pas fiable, la ligne « Volume du téléphone » s'affiche sans pourcentage |
| T3 | Obtenir une sonnerie progressive | Un son personnalisé d'AlarmKit doit durer moins de 30 secondes. Une montée sur 2 minutes ne tient pas dans un seul fichier |
| T4 | L'apparence de la feuille de scan du système pendant une session | Rien, dans ce qui a été lu, ne dit si elle suit le réglage clair ou sombre du téléphone ou l'apparence de l'app. Sa largeur réelle est aussi à regarder |
| T5 | Rouvrir la feuille de scan sans geste de l'utilisateur après les 60 secondes | Non vérifié. Si c'est possible, la question de rouvrir automatiquement pourra être reposée. Aujourd'hui la décision est d'afficher le message « temps écoulé » |
| T6 | Bloquer plus de 50 applications | Des développeurs signalent qu'au-delà de 50, plus aucune n'est bloquée. Apple ne l'a pas confirmé |
| T7 | Le nombre d'applications quand une catégorie est choisie | Il ne peut pas toujours être calculé. Ce nombre apparaît à trois endroits : « 6 applications choisies » (écran 4), le « 6 » de la ligne « Applications bloquées » (accueil) et « 6 applications seront bloquées » (notification du soir). Sa formulation sur iPhone sera décidée après ce test |
| T8 | La taille du logo dans l'emplacement d'icône de l'écran de blocage | L'emplacement est imposé par le système |

### Android

| N° | Quoi | Pourquoi |
| --- | --- | --- |
| T9 | Lister les applications installées | L'autorisation QUERY_ALL_PACKAGES est réservée par Google Play aux apps dont la fonction principale en a besoin |
| T10 | Bloquer les applications par un service d'accessibilité | Approche courante, à valider avec les règles de Google Play |
| T11 | L'affichage plein écran de l'alarme | L'utilisateur peut retirer cette autorisation |

## 4. Ce qui n'est pas encore dessiné ou écrit

| N° | Quoi | Remarque |
| --- | --- | --- |
| M1 | Les images du guide des autorisations, pour iPhone et pour Android, et les textes de chaque étape | Seule la mise en page est validée, avec un exemple |
| M2 | Les rubriques d'aide « Niumi Point perdu » et « Sortie de secours » | Non écrites |
| M3 | La page « Licences » | Elle doit porter « Copyright IBM Corp. » et le texte de la licence de la police |
| M4 | Le contenu de « Mentions légales » et de « Politique de confidentialité » | Non écrit |
| M5 | Le message « temps écoulé » à l'annulation (sombre) et à l'association ou à l'ajout d'un Niumi Point (clair, avec le lien « Annuler ») | Seule la version du matin est dessinée. Les textes sont décidés |
| M6 | L'affichage des échecs du scan en dehors de l'écran 10 : association, ajout, annulation | Les textes existent pour l'association. La forme n'est pas dessinée |
| M7 | La feuille de sonnerie pendant une session | Version sombre non dessinée |
| M8 | L'écran 2 ouvert depuis les réglages pendant une session, et son guide | Versions sombres non dessinées |
| M9 | La roue de « Temps gagné » et le parcours d'ajout d'un Niumi Point pendant une session | Versions sombres non dessinées |
| M10 | Le message « Modifiable après avoir scanné ton Niumi Point. » | Le texte existe, sa forme n'est pas dessinée |
| M11 | L'écran 4 ouvert depuis l'accueil pour modifier la sélection | Même écran. Le retour vers l'accueil n'est pas dessiné |
| M12 | Le bandeau de problème et l'alerte d'autorisations pendant une session | Versions sombres non dessinées. Les quatre maquettes existantes sont claires |

Pour les versions sombres manquantes, la règle est connue : mêmes mises en page et mêmes textes, avec les couleurs du thème sombre de `VALEURS-DE-DESIGN.md`.

Sujets qui n'ont pas été abordés du tout pendant la conception : les autres tailles d'écran et le mode paysage, les tailles de texte agrandies et le lecteur d'écran, les langues autres que le français.

## 5. Écarts relevés entre l'ancien document et les maquettes

Le journal des décisions (`annexes/journal-des-decisions.md`) contient des passages dépassés. `SPECIFICATION.md` a déjà tranché ces cas comme indiqué. Ils sont listés ici pour que Mehdi puisse les contrôler.

| N° | Sujet | Ancien document | Retenu dans la spécification |
| --- | --- | --- | --- |
| E1 | Écran 7, couleur | « Le fond est blanc » (6 octobre) | Sombre, comme la maquette validée et la règle « sombre pendant la session » (8 octobre) |
| E2 | Écran 9, compteur | « 84 sur 200 caractères » | « 84 sur 197 caractères », comme la maquette : le texte validé fait 197 caractères |
| E3 | Message après une annulation | Deux formulations : « … sont débloquées et ton réveil est désactivé. » et « … sont débloquées. Ton réveil est désactivé. » | La première, validée le 8 octobre et présente dans la maquette |
| E4 | Écran 6 | Deux tableaux décrivent une ancienne disposition en liste et un état « Pas de réveil prévu » | La maquette validée : cadran, carte, pastilles, deux lignes ; état « Réveil désactivé » |
| E5 | Écran 5 | Un tableau décrit l'écran « Premier planning » | L'écran est supprimé |
| E6 | Écran 2 | Le tableau liste quatre autorisations, dont le NFC | Trois tuiles, comme la maquette. Voir D10 |
| E7 | Écran 3, NFC coupé | « Le NFC est coupé sur ton téléphone. » à l'écran 3 ; « Le NFC est coupé. Active-le pour scanner ton Niumi Point. » à l'écran 10 | Les deux sont gardés tels quels, chacun sur son écran. À harmoniser si Mehdi le souhaite |
| E8 | Sonnerie | Plusieurs paragraphes parlent d'un curseur de volume réglable | Pas de curseur : une ligne « Volume du téléphone » et sa valeur (9 octobre) |
| E9 | Palette | Le début de la section dit qu'aucune palette n'est choisie | Clair hors session, sombre pendant la session (8 et 9 octobre) |
| E10 | Écran 11 | « Le point ambre du logo se lève comme un soleil » | Même dessin, avec le soleil en dégradé (9 octobre) |
| E11 | Sortie de secours, le matin | « L'accueil affiche le message « Session annulée… » », sans distinguer le soir du matin | Ce message est retenu pour le soir seulement. Le matin reste ouvert : voir D2 |
