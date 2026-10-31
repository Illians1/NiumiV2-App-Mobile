# Niumi · Points ouverts

État au 9 octobre 2026, complété le 18 octobre (T11, E12), le 19 octobre 2026 (D9, D13, D14, D19, D21, M13, T12, §2), le 20 octobre 2026 (D14 précisé, D12) et le 23 octobre 2026 (D5, D6, D7, M7, étape 40 du plan Android ; puis D22 et E13 avec les 61 nouvelles maquettes) et le 25 octobre 2026 (D17, M10, M11 tranchés, M14 et M15 ajoutés, E13 complété, E14 ; 64 maquettes depuis cette date ; puis, après les essais sur appareil de l'étape 45 du plan Android, M16 et deux lignes de textes à relire, §2 ; puis, à l'étape 46, M18, M19 et les délais de l'écran 11, §2) et le 26 octobre 2026 (M20 et le titre « Jours de réveil », §2), puis le 31 octobre 2026 (M15 tranché). Tout ce qui suit n'est **pas** une décision. Avant d'implémenter un de ces points, il faut poser la question à Mehdi.

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
| D2 | ~~Sortie de secours, le matin~~ | **Tranché le 24 octobre 2026** (étape 42 du plan Android) : la recopie pendant l'alarme termine la session comme un scan. L'écran 11 s'affiche, sans la phrase « Bravo ! … de moins qu'avant Niumi. », et le réveil reste activé pour le lendemain. Le soir, la recopie annule la session et éteint l'interrupteur, comme écrit au §13 |
| D3 | ~~Écran 3 bis~~ | **Tranché le 24 octobre 2026** : « 1 heure et + » compte pour 60 minutes dans le calcul. Le temps mis à se lever est arrondi à la minute inférieure |
| D4 | Écran 3 bis | Quelle durée est présélectionnée à l'ouverture de la roue ? La maquette montre 15 min |
| D5 | ~~Accueil, ligne « Sonnerie »~~ | **Tranché le 23 octobre 2026** (étape 40 du plan Android) : oui, quand la sonnerie progressive est sur « Non », la ligne n'affiche que le nom du son |
| D6 | ~~Feuille de sonnerie~~ | **Tranché le 23 octobre 2026** : la sous-ligne « Volume max en… » (« Monte jusqu’au volume du téléphone en… » depuis le 24 octobre) disparaît quand l'interrupteur « Sonnerie progressive » est éteint, et la barre des durées avec elle (maquette 20, dessinée le même jour en remplacement de la pastille « Non ») |
| D7 | ~~Sonnerie~~ | **Tranché le 23 octobre 2026, sur Android** : les quatre sons déjà empaquetés (Cloche, Énergique, Oiseaux, Piano), sans cinquième son ; les noms des maquettes (Aube, Carillon, Oiseaux, Classique, Insistante) restent des exemples |
| D8 | Sortie de secours | Quels autres textes à recopier ? Un seul est validé, et le texte doit changer à chaque sortie. *Décidé le 24 octobre 2026 (étape 42 du plan Android) : un seul texte est livré, dans une liste prête à en recevoir d'autres ; tant qu'il est seul, il ne change pas d'une sortie à l'autre. Il comptait 196 caractères, et non 197 comme l'écrivaient la spécification et les maquettes ; revu le même jour (D23), il en compte 190. Les autres textes restent à choisir* |
| D9 | ~~Bouton inactif en clair~~ | **Tranché le 19 octobre 2026** : fond `#E8E8E4` et texte `#5C5C60` (style de l'écran 2), partout. Le style de l'écran 4 (`#C6C6C2`, texte blanc) n'est pas repris |
| D10 | ~~Écran 2, NFC~~ | **Tranché le 20 octobre 2026** : pas de tuile NFC. Le NFC est signalé à l'association, au moment du scan et par le bandeau de l'accueil |
| D11 | Écran 3, choix de la pièce | « Continuer » est-il grisé tant qu'aucune pièce n'est choisie ? Rien n'est dit |
| D12 | ~~Ponctuation des titres~~ | **Tranché le 20 octobre 2026** : les deux titres de l'écran 1 s'écrivent sans point final, comme les autres titres de l'app (`SPECIFICATION.md` §4) |
| D13 | ~~Apostrophes~~ | **Tranché le 19 octobre 2026** : apostrophe typographique (’) dans les textes de l'app. À la recopie depuis la spécification, l'apostrophe droite devient ’ |
| D14 | ~~Règle de l'ambre~~ | **Tranché le 19 octobre 2026** : règle validée telle quelle, « Sur fond clair, l'ambre ne porte jamais de texte et reste posé sur l'encre ou dans un dessin. » **Précisé le 20 octobre 2026** : « pas d'ambre dans les icônes » vise les icônes de navigation et d'action ; le cadenas ambre de la carte « Blocage en cours » et le point ambre de l'icône du Niumi Spot restent tels quels (`VALEURS-DE-DESIGN.md` §2) **Exception ajoutée le 22 octobre 2026** : coche à l'encre dans une pastille ambre sur les autorisations accordées de l'écran 2 (`VALEURS-DE-DESIGN.md` §2) |
| D15 | Écran 11 | Le soleil en dégradé est gardé « pour l'instant ». L'écran peut encore changer |
| D16 | Règles communes | Dans la liste de ce qui est exclu de l'app, que vise exactement le mot « confirmation » ? L'app a depuis des confirmations (écran 8, suppression d'un Niumi Spot, et depuis le 25 octobre 2026 la fenêtre avant un blocage immédiat depuis l'accueil, `SPECIFICATION.md` §9) |
| D17 | ~~Accueil : plus aucun jour coché~~ | **Tranché le 25 octobre 2026** (décision de Mehdi, étape 45 du plan Android) : ce n'est plus un cas d'erreur. Aucun jour coché, c'est le réveil **ponctuel**, onglet « Ponctuel » de la maquette 18 : il sonne à la prochaine heure de réveil (aujourd'hui si elle est encore à venir, sinon le lendemain), puis l'interrupteur s'éteint après le scan ou la recopie du matin. « Quotidien » coche les sept jours ; décocher le dernier jour revient à « Ponctuel » (`SPECIFICATION.md` §2, §9). Le moteur commun ne répond plus `NO_DAY_ENABLED` (SPEC_CORE_KMP §8.4). Le même jour, l'écart minimal de 15 minutes entre coucher et réveil (Q8 du plan Android, 24 octobre) est remplacé : une minute au sélecteur, 5 min au cadran, et deux heures identiques forment une période de 24 h, acceptée au cadran comme au sélecteur ; en Quotidien, c'est un blocage permanent, assumé par Mehdi |
| D18 | Écran 2 | Sur une tuile accordée, la maquette retire le bouton « ? » en même temps que « Autoriser ». Le guide doit-il rester accessible ? **Sans objet pour l'instant** (20 octobre 2026) : le guide est reporté avec M1, aucune tuile n'a de « ? » sur Android, et une tuile accordée montre la coche seule |
| D19 | ~~Écran 2, taille du titre~~ | **Tranché le 19 octobre 2026** : 28, comme tous les titres d'écran |
| D20 | Sortie de secours, iPhone | Que fait l'alarme pendant la recopie sur iPhone ? Le journal ne le dit que pour Android : la sonnerie baisse tant que la saisie avance et remonte si elle s'arrête |
| D22 | ~~« Niumi Spot »~~ | **Décidé le 23 octobre 2026, appliqué le 24 octobre 2026** : les 61 maquettes déposées le 23 octobre disent « Niumi Spot » (42 sources) à la place de « Niumi Point ». Le renommage a été appliqué le 24 octobre à `SPECIFICATION.md`, à `TEXTES-DES-MAQUETTES.md`, aux textes de l'app et à leurs tests. Les derniers textes Android qui disaient « boîtier » (écran 3 actuel, écrans 6, 7 et 12 actuels, diagnostic, aide, nom proposé « Niumi Spot 1 ») l'ont été le même jour |
| D23 | ~~Le mot « session » dans les textes affichés~~ | **Tranché le 24 octobre 2026** : « session » reste le nom du concept dans les spécifications, mais les textes affichés parlent du réveil. Lien et feuille « Annuler le réveil » et « Annuler le réveil ? », phrase de scan « …pour annuler le réveil », message « Réveil annulé. Tes applications sont débloquées. », écran 8 « Arrêter le réveil sans scanner ton Niumi Spot ? » avec le bouton « Garder le réveil », écran 9 « Recopie ce texte pour arrêter le réveil. » et le bouton « Arrêter le réveil », texte à recopier « …Je choisis d'arrêter mon réveil sans le scanner… » (190 caractères), lecteur d'écran « Fin du réveil ». Maquettes 05, 17, 21 à 24 et 26 à 30 redessinées depuis leurs sources le même jour. Les écrans Android de l'ancien parcours gardent le mot jusqu'à leur suppression. **Régression du 25 octobre 2026** : les sources régénérées ce jour-là ramènent les anciens textes ; la décision tient, voir E14 |
| D21 | ~~Niumi Spots : libellés de l'option d'ajout sur l'écran 8, du refus « déjà enregistré » et de la limite de dix~~ | **Tranché le 19 octobre 2026** : troisième bouton au trait « Ajouter un Niumi Spot » sous « Recopier le texte » (écran 8, pendant l'alarme seulement) ; « Ce Niumi Spot est déjà enregistré. » avec « Réessayer » ; à dix, la ligne « Ajouter un Niumi Spot » grisée avec « Tu as déjà dix Niumi Spots. » Reporté dans `SPECIFICATION.md` §6, §13, §16 |
| D24 | Barre d'onglets, « Blocage seul » et « Suivi » | Que feront ces deux onglets ? Ni leur contenu ni leurs pages ne sont décidés ou dessinés. D'ici là, ils restent grisés avec le badge « Bientôt » et ne font rien (décision de Mehdi du 25 octobre 2026, `SPECIFICATION.md` §9) |

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
| Accueil en session | Modifiable après avoir scanné ton Niumi Spot. |
| Accueil, carte (deux heures identiques) | 24 h de blocage (non dessiné, écrit le 25 octobre 2026, à valider au premier essai) |
| Sélecteur d'heure (deux heures identiques) | 24 h de blocage jusqu’au réveil de 7:00 ; 24 h de blocage depuis le coucher de 22:30 (même statut) |
| Accueil et sélecteur, libellés d'accessibilité | Choisir l’heure (action du cadran), Heures et Minutes (les deux roues), Icône de l’application bloquée (ligne « Applications bloquées » avec une seule application) (même statut) |
| Accueil, ligne des onglets | Jours de réveil (titre au-dessus des pastilles, décidé par Mehdi le 26 octobre 2026, non dessiné, à valider au premier essai) |
| Accueil, comptes à rebours d'un jour ou plus | 1 j ; 1 j 4 h ; 2 j 4 h ; 6 j, au centre du cadran (« Coucher dans 1 j 4 h », depuis le 31 octobre 2026) et sur la carte (« Blocage des apps dans 2 j 4 h ») ; jours et heures entières, sans les minutes (forme décidée par Mehdi le 25 octobre 2026 après essai sur appareil, non dessinée, à valider mot pour mot ; `SPECIFICATION.md` §2) |
| Notification Android de session, avant le coucher | Réveil à 7:00 ; Réveil lundi à 7:00 ; Réveil dimanche à 0:30 ; Tes applications choisies seront bloquées lundi à partir de 22:30. ; Tes applications choisies seront bloquées samedi à partir de 11:45. (même date et même statut ; `SPECIFICATION.md` §17) |
| Notifications Android de sonnerie et d'attente de scan | ~~Disaient « boîtier »~~ ~~Tranché le 19 octobre 2026 : « Touche pour scanner ton Niumi Spot et terminer la session. » et « Scanne ton Niumi Spot pour terminer la session. »~~ **Retranché le 23 octobre 2026, sans le mot « session »** : « Ton réveil Niumi sonne », « Scanne ton Niumi Spot pour l'arrêter et retrouver tes applis. » et « Scanne ton Niumi Spot pour retrouver tes applis. » (`SPECIFICATION.md` §14, partie Android ; appliqué le même jour) |
| Annulation, Android | Approche ton téléphone du Niumi Spot pour annuler le réveil |
| Écran 11, délais autres que « 4 minutes » | Moins d’une minute après la sonnerie. ; 1 minute après la sonnerie. ; 1 h 15 après la sonnerie. ; C’est 1 minute de moins qu’avant Niumi. (règle décidée par Mehdi le 25 octobre 2026, étape 46 du plan Android : accord, puis format des durées dès 60 min ; textes non dessinés, à valider au premier essai) |
| Annulation, Android, libellé d'accessibilité | Annulation du réveil (lu par TalkBack sur le cercle de chargement qui suit un scan accepté, sans texte à l'écran ; choisi par Mehdi le 26 octobre 2026, non dessiné) |
| Niumi Spots | N'importe lequel de tes Niumi Spots arrête l'alarme. |
| Niumi Spots | Pour l'utiliser de nouveau, il faudra le scanner et l'ajouter. |
| Niumi Spots, renommer | Enregistrer |
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
| T13 | Le nom de l'application dans le titre de l'écran de blocage | Android affiche « L’application YouTube est bloquée » depuis le 23 octobre 2026 (`SPECIFICATION.md` §12). L'extension qui configure l'écran de blocage reçoit l'application ; il faut vérifier qu'elle en donne bien le nom affiché, faute de quoi l'iPhone garde « Cette application est bloquée » |

### Android

| N° | Quoi | Pourquoi |
| --- | --- | --- |
| T9 | ~~Lister les applications installées~~ | **Résolu le 22 octobre 2026** (étape 37 du plan Android) : la liste vient d'une requête ciblée (`<queries>` sur les activités de lancement, SPEC_ANDROID §12.1) ; `QUERY_ALL_PACKAGES` n'est pas demandée |
| T10 | Bloquer les applications par un service d'accessibilité | Approche courante, à valider avec les règles de Google Play. **22 octobre 2026** (étape 39 du plan Android) : la déclaration Play et le script de la vidéo décrivent l'écran 7, affiché jusqu'à « Fermer » ; l'acceptation par Google Play reste à obtenir |
| T12 | ~~Garder Niumi en vie quand on le retire des applications récentes~~ | **Tranché le 19 octobre 2026** : sur Xiaomi, retirer Niumi des récents arrêtait le blocage. Une notification discrète reste affichée pendant toute la session, avec deux textes (avant et après le coucher) ; elle peut être écartée et ne revient pas. Niumi n'est plus caché des récents. « Tout effacer » demande toujours de verrouiller Niumi (`SPECIFICATION.md` §17, SPEC_ANDROID §12.5) |
| T11 | ~~L'affichage plein écran de l'alarme~~ | **Tranché le 18 octobre 2026, sans objet** : Niumi n'utilise plus l'affichage plein écran. L'alarme sonne et une notification visible sur l'écran de verrouillage ouvre l'écran 10 |

## 4. Ce qui n'est pas encore dessiné ou écrit

| N° | Quoi | Remarque |
| --- | --- | --- |
| M1 | Les images du guide des autorisations, pour iPhone et pour Android, et les textes de chaque étape | Seule la mise en page est validée, avec un exemple. **Guide reporté sur Android le 20 octobre 2026** (étape 36) : l'écran 2 est livré sans « ? » jusqu'à ce que les textes et les images existent. L'exemple de l'alarme ne vaut pas pour Android, où l'alarme ne demande rien |
| M2 | Les rubriques d'aide « Niumi Spot perdu » et « Sortie de secours » | Non écrites |
| M3 | La page « Licences » | Elle doit porter « Copyright IBM Corp. » et le texte de la licence de la police |
| M4 | Le contenu de « Mentions légales » et de « Politique de confidentialité » | Non écrit |
| M5 | Le message « temps écoulé » à l'annulation (sombre) et à l'association ou à l'ajout d'un Niumi Spot (clair, avec le lien « Annuler ») | Seule la version du matin est dessinée. Les textes sont décidés |
| M6 | L'affichage des échecs du scan en dehors de l'écran 10 : association, ajout, annulation | Les textes existent pour l'association. La forme n'est pas dessinée |
| M7 | ~~La feuille de sonnerie pendant une session~~ | **Tranché le 23 octobre 2026, sur Android** : la palette sombre du thème, comme la feuille de scan ; la bulle du volume suit `ink`/`onInk` et s'inverse avec elle |
| M8 | L'écran 2 ouvert depuis les réglages pendant une session, et son guide | Versions sombres non dessinées. En attendant, le tap des avertissements de session reste sur le diagnostic d'incident (décision du 20 octobre 2026, revue à l'étape 52 du plan Android) |
| M9 | La roue de « Temps gagné » et le parcours d'ajout d'un Niumi Spot pendant une session | Versions sombres non dessinées |
| M10 | ~~Le message « Modifiable après avoir scanné ton Niumi Spot. »~~ | **Tranché le 25 octobre 2026** (décision de Mehdi) : une bulle encre en bas de l'écran, de la forme du message de la maquette 27 sans la coche, lue par le lecteur d'écran ; elle disparaît après 4 secondes ou au toucher suivant (`SPECIFICATION.md` §9, `VALEURS-DE-DESIGN.md` §4) |
| M11 | ~~L'écran 4 ouvert depuis l'accueil pour modifier la sélection~~ | **Tranché le 25 octobre 2026** (décision de Mehdi) : même écran, avec une flèche de retour ; « Continuer » et le retour du téléphone ramènent à l'accueil. La sélection y est modifiable tant que le blocage n'a pas commencé (`SPECIFICATION.md` §8) |
| M12 | Le bandeau de problème et l'alerte d'autorisations pendant une session | Versions sombres non dessinées. Les quatre maquettes existantes sont claires |
| M13 | ~~L'écran 8 pendant l'alarme avec l'option d'ajout d'un Niumi Spot~~ | **Tranché le 19 octobre 2026, sans maquette dédiée** : la feuille de l'écran 8 et le texte de D21 suffisent |
| M14 | L'accueil quand l'interrupteur est allumé mais qu'aucun réveil n'a pu être programmé (Q14 du plan Android) | Non dessiné. Décision de Mehdi du 25 octobre 2026, valable jusqu'à la fenêtre d'erreur de l'étape 52 : carte « Réveil activé » avec « Aucun réveil ni blocage prévu », centre du cadran « Réveil » sans compte à rebours (`SPECIFICATION.md` §9) |
| M15 | ~~Hauteur des rangées des roues du sélecteur d'heure~~ | **Tranché le 31 octobre 2026** (décision de Mehdi) : les roues ne montrent plus que trois rangées, toutes de 42, comme Android depuis l'étape 45 ; les traits gardent leurs 56 d'écart ; les maquettes 19 et 20 sont redessinées ainsi, avec la fenêtre à 40 des bords (`VALEURS-DE-DESIGN.md` §4) |
| M16 | L'accueil tel que l'app le dessine depuis les essais sur appareil de l'étape 45 | **Décidé par Mehdi le 25 octobre 2026, après essai sur appareil**, non dessiné dans les maquettes 19 et 20, qui gardent les valeurs d'avant. Les maquettes 15 à 18 et 24 ont été réalignées le même jour, avec la barre d'onglets (M17) : icône des réglages retirée, flèche de l'heure actuelle, espaces de 8, 12, 14 et 12, lignes de 80 et 72 (valeurs de la nouvelle maquette 16 de Mehdi, un peu plus larges que celles de l'app). Zone du cadran de 366 × 340, centre à 170, bande de 40 pour les étiquettes et écart de 4 avec l'anneau ; cadran 32 sous la marge du haut ; étiquettes jamais sur l'anneau ni l'une sur l'autre, qui glissent le long de l'anneau en gardant l'ordre de leurs poignées ; flèche de 8 × 8 vers le point de l'heure actuelle, point blanc sur l'arc de la nuit ; fondu de l'ambre de 12° en pente droite ; espaces resserrés (12, 8, 0, 20), lignes de 76 et 64, plus de trait sous « Sonnerie », onglets à 16 d'écart (accueil et sélecteur). Valeurs dans `VALEURS-DE-DESIGN.md` §4 et §5, règles dans `SPECIFICATION.md` §9 |
| M17 | La barre d'onglets du bas (accueil et réglages) | **Décidée par Mehdi le 25 octobre 2026** (nouvelle maquette 16). Restent ouverts : la version sombre, déduite du thème sombre et non dessinée par Mehdi (maquettes 17 et 24) ; l'horloge de « Réveil » quand l'onglet n'est pas choisi (au trait, choix de Claude) et l'engrenage plein de « Réglages » choisi (demandé par Mehdi, forme de Claude) ; le tremblement et la vibration d'un onglet « Bientôt » touché (forme demandée par Mehdi, valeurs choisies par Claude ; sur iPhone, l'équivalent serait la vibration d'erreur du système) ; le contraste de « Réglages » non choisi, `#85858A` sur blanc, 3,7:1, sous les 4,5:1 visés pour un texte (`VALEURS-DE-DESIGN.md` §6) : le texte secondaire `#5C5C60` donnerait 6,7:1 ; sur iPhone, la barre de la maquette remplace-t-elle une `TabView` du système ? ; le haut translucide de la barre (demandé par Mehdi le 25 octobre 2026 ; 72 % sur 10, jusqu'au haut des icônes ; opacité de Claude), non dessiné : les maquettes montrent une barre pleine sans contenu derrière |
| M18 | L'écran 11 quand l'alarme n'a pas sonné, et la bulle après une annulation | **Tranché par Mehdi le 25 octobre 2026** (étape 46 du plan Android), non dessiné : sans sonnerie (téléphone éteint à l'heure du réveil), l'écran 11 dit « Debout à 9:00 » sans délai ni « Bravo ! » ; une sonnerie en retard compte depuis la sonnerie réelle. La bulle « Réveil annulé. Tes applications sont débloquées. » est celle de M10, sans la coche ni le gras de la maquette 27, et disparaît après 4 secondes ou au toucher suivant (`SPECIFICATION.md` §11, §15) |
| M19 | Taille des titres des feuilles du bas | **Décidé par Mehdi le 26 octobre 2026, après essai sur appareil** (étape 46 du plan Android), non dessiné : les titres des feuilles (« Annuler le réveil ? »), de « Prêt à scanner » et des échecs du scan, jugés trop imposants, passent en 22 (600 pour les titres, 400 pour « Prêt à scanner »), au lieu de 24 et 26 dans les maquettes ; sur la feuille « Annuler le réveil ? », le titre et le texte sont centrés. Vaut aussi pour l'écran de réveil et l'association, qui partagent la feuille de scan. Les maquettes 26 et 35 à 39 gardent les anciennes valeurs (`VALEURS-DE-DESIGN.md` §3). **Revu le 30 octobre 2026** (décision de Mehdi) : « Annuler le réveil ? » n'est plus une feuille du bas mais une fenêtre centrée au format de la fenêtre d'alerte ; maquette 25 redessinée |
| M20 | La ligne « Jours de réveil » de l'accueil | **Décidée par Mehdi le 26 octobre 2026**, non dessinée : les maquettes 15 à 18 et 24 gardent les onglets « Ponctuel » et « Quotidien » centrés, sans titre. Dans l'app, le titre « Jours de réveil » est à gauche et les onglets à droite, sur la même ligne, dans une capsule grise dessinée d'après un croquis de Mehdi du même jour (`SPECIFICATION.md` §9, `VALEURS-DE-DESIGN.md` §4). Restent ouverts : l'onglet choisi, segment plein à l'encre (choix de Claude ; le croquis ne montre aucun onglet choisi), et les valeurs relevées sur le croquis (capsule de 32, coins de 10) |
| M21 | L'accueil en cartes | **Décidé par Mehdi le 30 octobre 2026** (maquettes 15 à 27, 29 et 61 à 65 ; fond `#FAFAF8` dans leur dernière version du même jour, après des essais de `#ECEDEE`, `#BFC3C8` et d'un anthracite). La troisième version des maquettes du même jour dessine ce que l'app faisait à l'essai : le cadran sans carte, posé sur un fond blanc qui devient gris `#ECEDEE` en dégradé lent, de la carte d'état à la carte des lignes, l'ombre des cartes sous l'anneau (masquée à l'intérieur de l'anneau dans l'app, demande de Mehdi du même jour, écart aux maquettes), tout le contenu défilant sur ce fond fixe sous une bande du haut au bord translucide ; et le thème sombre éclairci, avec les ombres (17, 24 à 26, 28 à 41, 66). Restent ouverts : la zone de toucher des pastilles, plus étroite que les 48 de la règle Android (un septième de la carte : 44 sur un écran de 390, 40 sur 360), et leur diamètre réduit à 40 sur un écran de 360 (choix de Mehdi) ; l'anneau du cadran, réduit dans sa carte pour que les étiquettes tiennent à côté des poignées (rayon d'environ 97 sur un écran de 390, 84 sur 360 et au moins ; étiquettes à 8 de l'anneau sur un côté, 6 du bord de la carte ; décision de Mehdi du 30 octobre 2026, écart à la maquette) ; l'ombre en deux couches des cartes, de la carte d'état et de l'anneau (décalage 1, flou 2 ; décalage 10, flou 30 ; noir à 10 % et 18 % en clair, 45 % et 80 % en sombre), dessinée telle quelle sur Android (`dropShadow`), dont le rendu reste à comparer à la maquette sur appareil. À valider sur appareil |

Pour les versions sombres manquantes, la règle est connue : mêmes mises en page et mêmes textes, avec les couleurs du thème sombre de `VALEURS-DE-DESIGN.md`.

Sujets qui n'ont pas été abordés du tout pendant la conception : les autres tailles d'écran, les tailles de texte agrandies et le lecteur d'écran, les langues autres que le français. Le mode paysage, lui, est **tranché le 23 octobre 2026** : l'app ne s'utilise qu'en portrait, sur Android comme sur iPhone, et les tablettes ne sont pas visées (`SPEC_ANDROID.md` §15, `SPEC_IOS.md` §20).

## 5. Écarts relevés entre l'ancien document et les maquettes

Le journal des décisions (`annexes/journal-des-decisions.md`) contient des passages dépassés. `SPECIFICATION.md` a déjà tranché ces cas comme indiqué. Ils sont listés ici pour que Mehdi puisse les contrôler.

| N° | Sujet | Ancien document | Retenu dans la spécification |
| --- | --- | --- | --- |
| E1 | Écran 7, couleur | « Le fond est blanc » (6 octobre) | Sombre, comme la maquette validée et la règle « sombre pendant la session » (8 octobre) |
| E2 | Écran 9, compteur | « 84 sur 200 caractères » | « 84 sur 197 caractères », comme la maquette : le texte validé fait 197 caractères. Corrigé depuis : 196, puis 190 après la révision du 24 octobre 2026 (D8, D23) |
| E3 | Message après une annulation | Deux formulations : « … sont débloquées et ton réveil est désactivé. » et « … sont débloquées. Ton réveil est désactivé. » | La première, validée le 8 octobre et présente dans la maquette. Remplacée le 24 octobre 2026 par « Réveil annulé. Tes applications sont débloquées. » (D23) |
| E4 | Écran 6 | Deux tableaux décrivent une ancienne disposition en liste et un état « Pas de réveil prévu » | La maquette validée : cadran, carte, pastilles, deux lignes ; état « Réveil désactivé » |
| E5 | Écran 5 | Un tableau décrit l'écran « Premier planning » | L'écran est supprimé |
| E6 | Écran 2 | Le tableau liste quatre autorisations, dont le NFC | Trois tuiles, comme la maquette. Voir D10. Sur Android, deux tuiles de plus depuis le 21 octobre 2026 (« Batterie », « Verrouiller Niumi ») : `SPECIFICATION.md` §5 |
| E7 | Écran 3, NFC coupé | « Le NFC est coupé sur ton téléphone. » à l'écran 3 ; « Le NFC est coupé. Active-le pour scanner ton Niumi Spot. » à l'écran 10 | Les deux sont gardés tels quels, chacun sur son écran. À harmoniser si Mehdi le souhaite |
| E8 | Sonnerie | Plusieurs paragraphes parlent d'un curseur de volume réglable | Pas de curseur : une ligne « Volume du téléphone » et sa valeur (9 octobre) |
| E9 | Palette | Le début de la section dit qu'aucune palette n'est choisie | Clair hors session, sombre pendant la session (8 et 9 octobre) |
| E10 | Écran 11 | « Le point ambre du logo se lève comme un soleil » | Même dessin, avec le soleil en dégradé (9 octobre) |
| E11 | Sortie de secours, le matin | « L'accueil affiche le message « Session annulée… » », sans distinguer le soir du matin | Ce message est retenu pour le soir seulement. Le matin : voir D2, tranché le 24 octobre 2026 (écran 11) |
| E12 | Écran 2, tuile « Alarme » sur Android | « L'affichage plein écran peut être retiré par l'utilisateur » | Retiré : Niumi n'utilise plus l'affichage plein écran (18 octobre, voir T11) |
| E14 | Maquettes régénérées le 25 octobre 2026 | Les sources HTML (et `INDEX.md`) ramènent des textes antérieurs au 24 octobre 2026 : maquette 05, « Une session ne peut pas être activée si le service est inactif. » à la place de « Sans cette autorisation, Niumi ne peut pas fonctionner. » ; maquette 17 et 24, lien « Annuler la session » ; 25, « Annuler la session ? » ; 26, « …pour annuler la session » ; 27 et `INDEX.md`, « Session annulée » et le message « Session annulée. Tes applications sont débloquées et ton réveil est désactivé. » ; 29 et 30, « Sortir de la session sans scanner ton Niumi Spot ? » et « Rester dans la session » ; 31 à 33, « Recopie ce texte pour sortir de la session. », l'ancien texte à recopier (« …de sortir de cette session… »), « sur 197 caractères » et « Sortir de la session » ; `INDEX.md`, section « 11 · Annuler la session » | D23 tient : `SPECIFICATION.md` et `TEXTES-DES-MAQUETTES.md` gardent « Annuler le réveil », « Réveil annulé. Tes applications sont débloquées. », les textes des écrans 8 et 9 et le texte de 190 caractères ; l'app garde « Annuler le réveil » et le texte actuel de l'écran 2, et la feuille d'annulation et les écrans 8 et 9, à venir (étapes 46 et 49), suivront D23. **À corriger par Mehdi** dans les sources, les images et `INDEX.md` (régression constatée à l'étape 45 du plan Android) |
| E13 | Toutes les sections | Numéros de maquettes cités par `SPECIFICATION.md` (« Images : »), `TEXTES-DES-MAQUETTES.md`, `VALEURS-DE-DESIGN.md`, le plan Android et SPEC_ANDROID | Le dossier `maquettes/` a été entièrement renuméroté le 23 octobre 2026 (61 maquettes, `INDEX.md` fait foi). Seules les références de la sonnerie (18 à 20) ont été mises à jour ce jour-là. **Nouvelle renumérotation le 25 octobre 2026** (64 maquettes) : trois maquettes de l'accueil insérées en 18, 19 et 20, les anciennes 18 à 61 deviennent 21 à 64. Ce jour-là, `SPECIFICATION.md`, `TEXTES-DES-MAQUETTES.md` et `VALEURS-DE-DESIGN.md` ont été alignés sur `INDEX.md`, ainsi que les références de SPEC_ANDROID et des étapes 45 à 52 du plan Android ; les étapes déjà faites du plan gardent leurs anciens numéros. Reste dans `INDEX.md` lui-même : la remarque finale parle de « la maquette 20 (sonnerie progressive désactivée) », devenue la 23 |
