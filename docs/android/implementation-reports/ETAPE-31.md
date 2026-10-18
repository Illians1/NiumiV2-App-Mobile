# Étape 31 — Interface « Mes boîtiers », diagnostic, récapitulatif, validation et documents

Dates : 2026-10-17 et 2026-10-18. **Étape terminée** : code, tests automatisés, passe instrumentée,
cinq lignes §20 et essais de l'écran 3 sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS
OS3.0.302.0.WPPEUXM), relecture finale faite et ses correctifs intégrés. 1 358 exécutions JVM
vertes ; 184 tests instrumentés verts le 17/10, puis les 41 de `:feature:setup` rejoués verts le
18/10 après les correctifs ; ktlint, detekt, Android Lint (debug et release), `assembleRelease`,
`bundleRelease` et le framework iOS verts. Trois lignes §20 sur cinq conformes (dont une avec
réserve), deux partielles.

Dernière étape du Lot 8 (boîtiers multiples, conception
`docs/superpowers/specs/2026-10-13-boitiers-multiples-design.md` §5). Plus, hors Lot 8, la
correction du faux « NFC désactivé » des écrans 8 et 9 révélé par l'essai 2 de l'étape 30.

## Ce qui est livré

- **Écran 3 « Mes boîtiers »** (`:feature:setup/pairing`).
  - **Liste.** Les boîtiers apparaissent par position, chacun avec son nom et le préfixe de huit
    caractères de son `boxId` suivi de « … ». Chaque ligne porte « Renommer » et « Retirer » ; le
    retrait se fait sans confirmation.
  - **Ajout.** « Ajouter un boîtier » ouvre la lecture (instruction et « Annuler »). Un boîtier
    neuf ouvre le dialogue « Nouveau boîtier », prérempli « Boîtier n » où n est le plus petit
    entier libre (`DefaultBoxName`). Un doublon affiche « Ce boîtier est déjà associé. », sans
    dialogue. Dès cinq boîtiers, « 5 boîtiers maximum. » remplace le bouton.
  - **Dialogues de nom.** Le compteur affiche « {n}/30 » ; la confirmation reste inactive tant que
    `PairedBoxName.sanitize` refuse le nom.
  - **Session en cours.** La liste reste visible, toute action est inactive, et le message dit
    « Une session est en cours. Tes boîtiers ne peuvent pas être modifiés avant sa fin. ».
  - **TalkBack.** Sémantique de collection sur la liste et ses lignes ; descriptions
    « Renommer {nom} » et « Retirer {nom} » sur les deux actions.
- **Lecture NFC de l'écran 3** : active d'office seulement sans aucun boîtier, sinon après
  « Ajouter un boîtier » ; arrêtée pendant un dialogue et pendant une session. Un scan arrivé hors
  d'une lecture attendue est ignoré, garde revérifiée après la lecture du dépôt.
- **Diagnostic (écran 2)** : contrôle « Boîtiers associés ».
  - En échec, le message est « Aucun boîtier n'est associé. Associe au moins un boîtier Niumi :
    c'est l'un d'eux qui terminera ta session. », avec l'action « Associer un boîtier ».
  - Satisfait, la ligne affiche « 1 boîtier associé » ou « {n} boîtiers associés », lu dans
    `ReadinessReport.pairedBoxCount`, avec l'action secondaire « Gérer mes boîtiers ».
- **Écrans 6, 7 et 12** :
  - le rappel d'engagement devient « Seul le scan d'un de tes boîtiers terminera la session. »
    (écrans 6 et 7) ;
  - la ligne du récapitulatif s'intitule « Boîtiers associés » (écran 6) ;
  - le refus `NO_PAIRED_BOX` devient « Associe au moins un boîtier… » ;
  - le contrôle est nommé « Boîtiers associés » sur l'écran 12.
- **Écrans 8 et 9, hors Lot 8.**
  - **Relecture.** Les deux écrans collectent `nfcAdapterSettledChanges()` sous
    `repeatOnLifecycle(RESUMED)`. À chaque état stable, la disponibilité est relue et le Reader
    Mode relancé.
  - **Journal.** `NFC_DISABLED` n'est pas journalisé de nouveau : `AlarmActivity.onAdapterSettled`
    et `ScanToModifyViewModel.onAdapterSettled` s'en chargent sans journal.
  - **Affichage.** Aucune fenêtre de grâce.
- **Aide et `LIMITES.md`.**
  - Nouvelle section « Boîtiers » : seuls les boîtiers figés à l'activation terminent la session,
    et aucune modification n'est possible pendant une session.
  - Seconde ligne NFC dans « Après un redémarrage ou une mise à jour » : sur les écrans de réveil
    et de scan, le message disparaît seul, peut revenir une ou deux secondes, et il vaut mieux
    attendre qu'il ait disparu pour scanner.

## Décisions prises pendant l'étape

| Décision | Choix | Pourquoi |
| --- | --- | --- |
| Quand l'écran 3 écoute (utilisateur, 17/10) | Après « Ajouter un boîtier » ; d'office sans aucun boîtier | La conception dit que le bouton « ouvre le Reader Mode » ; aucun scan accidentel pendant un renommage. Ajoute `startAdding()` / `cancelScan()` à l'interface du plan |
| Nom proposé (utilisateur, 17/10) | Plus petit `n` libre | « boîtiers + 1 » proposait un second « Boîtier 3 » après un retrait (reporté de l'étape 30). Conception §4.1 et SPEC §15 mises à jour |
| Pluriel des écrans 7 et 12 (utilisateur, 17/10) | Oui | Même phrase que l'écran 6, même intitulé que l'écran 2 |
| « Rôle `listItem` » demandé par le plan | Sémantique de collection | Compose n'a pas de rôle de liste ni d'élément de liste |
| Relecture NFC des écrans 8 et 9 | Relance aussi le Reader Mode | `ScanToModifyViewModel.startReaderMode` ne démarre rien sous un NFC lu désactivé : effacer le message sans écouter aurait invité à scanner un écran sourd |
| `RESUMED` au lieu de `STARTED` | Collecte au premier plan seulement | Le Reader Mode n'y vit que là ; `ON_RESUME` relit de toute façon au retour ; le volet rapide ne met pas en pause |
| `ScanToModifyViewModel.onAdapterSettled()` sans `Activity` | La `Route` rappelle `startReaderMode(activity)` | Testable en JVM ; `refreshAvailability()` journalise et ne convenait pas |
| detekt `TooManyFunctions` / `LongParameterList` | `PairingViewModel.editor` (classe interne d'édition), `NameDialogKind`, `openNfcSettings` / `currentTimeText` hors de `AlarmActivity` | Découpage plutôt que suppression, comme le reste du dépôt |
| `SummaryViewModelTest` | Non étendu | Les noms y sont couverts depuis l'étape 30 |
| Ligne §20 n° 1 « la preuve journalisée porte le `boxId` du second » | Réécrite dans la spec | La preuve ne quitte jamais la mémoire (point de vigilance 4, §16) : ni le journal ni Room ne la consignent. Le `boxId` est prouvé en JVM (`HandleValidNfcUseCaseTest`) ; aucun journal ajouté, ce serait un changement de comportement hors plan |
| Comptage §20 du plan (« quatre lignes ») | Cinq | Quatre du Lot 8 et une hors Lot 8 |
| `NFC_SCAN_VALID` sur l'écran 3 | Journalisé à la lecture d'un boîtier neuf, avant le nom | C'est l'événement de scan ; un ajout annulé laisse donc un scan valide sans association, et un doublon n'est pas journalisé (relevé par la relecture, gardé tel quel) |

## Specs et documents modifiés dans le même changement

- **SPEC_ANDROID** :
  - §11.1 : liste entière figée ; affichage défensif de l'écran 3.
  - §11.2 : alinéa « Lecture de la disponibilité après démarrage », mesures du 18/10 comprises.
  - §13 : ligne et message du tableau, paragraphe « Boîtiers associés », « Gérer mes boîtiers ».
  - §15 : paragraphe « Lot 8 » pour les écrans 2, 3, 6, 7, 12 et 13 ; deux mentions du rappel
    d'engagement.
  - §19.1 : tests de l'étape 31.
  - §20 : cinq lignes, dont la n° 1 corrigée.
  - §21 : cinq critères du Lot 8.
  - §22 : section « Lot 8 ».
- **Conception** : §4.1 (nom par défaut), §5.1 (lecture d'office sans boîtier).
- **Plan maître** : cases de l'étape 31 et recette du Lot 8 cochées, avec leurs écarts ; comptage
  « quatre » corrigé en « cinq ».
- **`QA_MATRIX.md`** :
  - nouvelle sous-section « Boîtiers multiples (Lot 8) » et paragraphe sur l'écran 3 ;
  - « scan d'un autre tag » mesuré pour la première fois (un seul tag existait jusque-là) ;
  - intro et synthèse mises à jour.
- **`RELEASE_REPORT.md`** : critères 36 à 40 du Lot 8 ; les anciens 36 et 37 deviennent 41 et 42 ;
  état, critère 22 et conclusion mis à jour.
- **`RESTE_A_FAIRE.md`** : bloc Lot 8 de la section D.
- **`LIMITES.md` et `HelpTexts.kt`** : section « Boîtiers » et seconde ligne NFC, écrites après
  mesure le 18/10.

## Tests écrits

**JVM** (rouge observé puis vert, sauf mention) :
- `PairingTextsTest` (créé, 4) ; `DefaultBoxNameTest` (4).
- `PairingViewModelTest` réécrit (28) : les tests nommés du plan, plus
  - `theReaderIsOnAtEntryOnlyWithoutAnyBox`, `addStartsTheReaderAndCancelStopsIt` ;
  - `aScanWhileANameDialogIsOpenIsIgnored`, `afterRemovingBoitier2TheNextProposalIsBoitier2` ;
  - `aBlankRenameIsRefusedWithoutWriting`, `removingTheLastBoxIsAllowedOutsideASession` ;
  - `aSessionStartingWhileANameDialogIsOpenClosesItWithoutWriting` ;
  - `theUiStateNeverCarriesAFingerprint` ;
  - deux tests de course ajoutés à la relecture finale (voir plus bas).

  Le faux dépôt refuse désormais le sixième boîtier et compte toutes les écritures.
- `ReadinessMessagesTest` (+2), `ReadinessViewModelTest` (+2 : le résumé satisfait porte le compte,
  l'échec garde son message).
- `SummaryTextsTest` (+2, rappel modifié), `ActiveSessionTextsTest` (rappel modifié),
  `IncidentDiagnosticTextsTest` (créé, 1).
- `ScanToModifyViewModelTest` : `refreshAfterTheAdapterSettlesReportsTheNfcEnabled`,
  `aSettledAdapterNeverLogsNfcDisabledAgain` (le plan nommait
  `aSettledAdapterStartsTheReaderWithoutLoggingNfcDisabledAgain` ; le démarrage du lecteur passe
  par la `Route` et n'est pas testable en JVM).
- `AlarmViewModelTest.anAdapterThatSettlesOnClearsTheDisabledMessageAndShortcut` : **vert
  d'emblée**. `onNfcAvailabilityChanged` recalculait déjà l'état : c'est un verrou de
  non-régression, pas une preuve du correctif, qui est dans l'activité.
- `HelpTextsTest` : rouge (2 échecs de parité) après l'écriture de `LIMITES.md`, vert avec
  `HelpTexts.kt`.

**Instrumentés** : `PairingScreenTest` réécrit (17). Il couvre :
- l'instruction pendant la lecture, le bouton hors lecture ;
- la liste, avec ses noms, préfixes et descriptions ;
- la taille de la liste annoncée (`CollectionInfo`) ;
- le plafond de cinq boîtiers, le NFC coupé ou absent, le doublon sans dialogue ;
- les dialogues d'ajout et de renommage ;
- l'absence d'empreinte et d'identifiant complet ;
- la session en cours.

Première passe rouge sur `aRenamingOpensTheRenameDialog` : « Renommer » désigne aussi l'action
de la ligne, inactive pendant le dialogue. Défaut du test, corrigé.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :feature:setup:testDebugUnitTest :feature:session:testDebugUnitTest :feature:ringing:testDebugUnitTest :app:testDebugUnitTest
./gradlew :feature:setup:compileDebugAndroidTestKotlin :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
#   detekt a d'abord échoué : TooManyFunctions (AlarmActivity 13, PairingViewModel 16), LongParameterList
#   (NameDialog), ReturnCount (confirmAdd, confirmRename), MaxLineLength (test) — découpages ci-dessus
./gradlew connectedDebugAndroidTest
#   1re passe (17/10 18:25) : installations refusées sur le téléphone (INSTALL_FAILED_USER_RESTRICTED)
#   et un échec réel du test de renommage ; 2e passe (18:29–18:32) : 184 tests, 0 échec
#   (core:database 90, feature:setup 41, core:system 27, app 12, feature:ringing 6, core:designsystem 5,
#   feature:session 3)

# Vérification finale (18/10), rejouée après les correctifs de la relecture finale :
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest ktlintCheck detekt :app:lintDebug \
          :app:lintRelease :app:assembleRelease :app:bundleRelease
#   1 358 exécutions JVM, 0 échec (shared:core 215, core:system 530, feature:session 237, core:database 148,
#   feature:setup 103, feature:ringing 44, app 38 en debug et 43 en release)
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64   # vert (avant la relecture ; :shared:core inchangé depuis)
./gradlew :feature:setup:connectedDebugAndroidTest           # 41 tests, 0 échec (18/10, après correctifs)
```

## Validation sur appareil

Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, 17 et 18 octobre 2026 ; horloges du
Mac et du téléphone concordantes à la seconde. Six tags NTAG, chacun avec un `boxId` et un token
distincts. Le token doit être **canonique** : le dernier de ses 22 caractères porte quatre bits de
remplissage nuls (`A`, `Q`, `g` ou `w` seulement) ; un `…NB` est refusé à juste titre par le
parseur strict, et un même `boxId` sur deux tags donne « déjà associé ». Base lue par `run-as`
(`niumi.db` avec son WAL), journal système capturé pendant les redémarrages.

1. **Écran 3 (17/10, conforme).**
   - Instruction affichée d'office sans boîtier.
   - Nom proposé « Boîtier 1 », champ vidé (« 0/30 », confirmation inactive), nommé « Salle de
     bain ».
   - « Cuisine », puis « Boîtier 1 » proposé pour le troisième. C'était le bon résultat, la règle
     du plus petit `n` libre ; l'attendu annoncé à l'utilisateur était faux.
   - Tag approché hors lecture : sans effet.
   - Renommage « Cuisine » → « Entrée », actions de ligne grisées pendant le dialogue.
   - Retrait de « Salle de bain », puis « Boîtier 2 » proposé.
   - Doublon refusé sans dialogue.
   - Cinq boîtiers, « 5 boîtiers maximum. » à la place du bouton ; tag retiré sans effet.
   - **Vérifié :** base (positions 0 à 4) et `uiautomator dump` (descriptions, préfixes) ; journal
     de scan sans détail.
2. **Ligne n° 1 (17/10, conforme).**
   - Diagnostic « 2 boîtiers associés » et « Gérer mes boîtiers » ; écrans 6 et 7 avec la liste et
     le rappel au pluriel.
   - Session `97c98478` figée sur « Entrée », « Boîtier 1 ».
   - Sonnerie 18:59:00. Tag hors liste à 18:59:20 : `NFC_SCAN_INVALID`, vibration d'erreur,
     sonnerie maintenue.
   - Tag du second boîtier à 18:59:41 : `RELEASING`, `COMPLETED`, révision 5, aucun incident.
3. **Ligne n° 2 (17/10, partielle).** Un tag ajouté ensuite (« Boîtier 2 ») annule la session
   suivante depuis l'écran 9 (`093844a9`, `CANCELLED` à 19:02:16). L'identité du tag refusé à
   18:59:20 n'a pas été relevée : il était hors liste, peut-être distinct de celui-ci.
4. **Lignes n° 4 et n° 5 sur l'écran 8 (18/10, conformes).**
   - **Préparation.** APK de `4a03d53` (base v4, projection v3) construit dans un worktree séparé,
     boîtier `c2d24363` associé, session `c2b56e08` armée pour 09:16.
   - **Mise à jour et redémarrage.** APK de l'étape 31 installé par-dessus sans ouvrir
     l'application (projection relue : toujours v3), puis redémarrage à 09:05:41.
     `RUNNING_LOCKED`, NFC absent, alarme reprogrammée.
   - **Sonnerie à 09:16:00 depuis la projection v3.** « NFC désactivé » affiché, ce qui est vrai :
     le service NFC n'existait pas encore. Le PIN SIM saisi ne déverrouille pas (`RUNNING_LOCKED`
     vérifié).
   - **Chronologie après le déverrouillage :**

     | Heure | Fait observé |
     | --- | --- |
     | 09:17:49,7 | Déverrouillage |
     | 09:17:53,9 | Écran 8 rouvert, `NFC_DISABLED` journalisé |
     | 09:17:58,4 | NFC allumé (`enableInternal: end`), message retiré seul |
     | 09:18:04,97 → 09:18:07,1 | HyperOS éteint et relance réellement le NFC, le message revient environ 2 s puis disparaît |
     | 09:19:19 | Scan du boîtier d'origine, `COMPLETED`, `HEALTHY` |

   - **Résultat.** Aucun incident ; `NFC_DISABLED` non rejournalisé à l'extinction. Base migrée en
     v5, `session_box` = « Boîtier 1 ».
5. **Ligne n° 5 sur l'écran 9 et fin de la ligne n° 3 (18/10).**
   - Session `acd5a614` armée avec « Boîtier 1 » figé, nom lu en base. **L'écran 6 n'a pas été
     regardé** : ligne n° 3 partielle.
   - Redémarrage à 09:22:33, déverrouillage 09:23:17,7, Niumi rouvert 09:23:20,9. NFC allumé à
     09:23:25,95, éteint et relancé par HyperOS de 09:23:33,1 à 09:23:34,8 : message affiché
     environ 1 s puis retiré seul.
   - Scan à 09:24:20 accepté sur l'écran 9 : `CANCELLED`.
   - **Réserve :** un incident `TIME_CHANGED` (`WARNING`) a été consigné à 09:23:16, horloge
     resynchronisée au démarrage (alarme reprogrammée deux fois). Sans rapport avec le NFC, mais
     `session_incident` n'est pas vide.
6. **Couleurs de l'écran 3 après la relecture (18/10).** Capture en thème clair : « Renommer »,
   « Retirer » et « Ajouter un boîtier » en couleur de texte, « Continuer » seul en Ambre.

## Observations hors périmètre, non corrigées

- **Retour à l'accueil après le déverrouillage.** Le 18/10, à 09:17:49, HyperOS a affiché
  l'accueil : il a fallu rouvrir Niumi pour revoir l'écran de réveil, qui était passé en pause à
  l'ouverture du clavier de code (09:17:43). SPEC_ANDROID §10.4 attend qu'il reste visible. À
  qualifier selon le geste employé (`RESTE_A_FAIRE.md`).
- **`TIME_CHANGED` après un redémarrage pendant `ARMED`.** À qualifier : faux positif ou incident
  légitime.
- **Écran de réveil à la libération.** Il s'est affiché environ 0,7 s pendant la libération
  déclenchée depuis l'écran 9, avant « Session annulée ». Comportement préexistant.
- **Écran 3 et volet rapide.** L'écran 3 ne relit pas le NFC sur un état stable, contrairement aux
  écrans 2, 6, 7, 8, 9 et 12.

## Relecture finale

Relecture du changement complet par un relecteur à contexte neuf, sur le diff de l'arbre de
travail contre `48743a0` plus les fichiers nouveaux, le 2026-10-18. Verdict : **prêt avec
correctifs**, aucun point critique, trois importants, sept mineurs.

| Constat | Décision | Preuve |
| --- | --- | --- |
| I1. `ETAPE-31.md` absent, plan maître et état de `RELEASE_REPORT.md` périmés | **Corrigé** : ce rapport, cases et texte du plan, état et critère 22 | relecture |
| I2. Synthèse QA : deux lignes comptées « conformes » alors que leur attendu n'a pas été entièrement observé | **Corrigé** : lignes n° 2 et n° 3 en « Partiel. », synthèse 49 conformes et 3 partielles, contrôle visuel de l'écran 6 ajouté à `RESTE_A_FAIRE.md` | relecture |
| I3. Ambre sur toutes les actions secondaires de l'écran 3, ce qui contredit le KDoc et la charte §3 (Ambre sur Crème insuffisant pour un petit texte) | **Corrigé** : actions de ligne, « Ajouter un boîtier », « Annuler » et annulation des dialogues en `onSurface` ; KDoc corrigé | capture sur appareil en thème clair. **Aucun test qui échoue d'abord** : la couleur n'a pas de sémantique testable, vérification visuelle seulement |
| M1. `onUriRead` : la garde n'était pas revérifiée après la lecture du dépôt (deux lectures d'un tag tenu, renommage ou session pendant la lecture) | **Regradé important et corrigé** : un tag tenu émet plusieurs lectures (constat déjà écrit dans `ScanToModifyViewModel`) | `twoReadsOfTheSameTagWhileTheStoreIsReadOpenOneDialogAndLogOnce`, `aRenameOpenedWhileAScanReadsTheStoreIsNotCoveredByANameDialog` : rouges, puis verts |
| M2. Le dialogue reste ouvert pendant l'écriture : « Annuler » à ce moment n'empêche pas l'ajout | Mineur, **reporté** | — |
| M3. `NFC_SCAN_VALID` journalisé au scan et non à l'ajout | Mineur, **gardé et consigné** (tableau des décisions) | — |
| M4. Un `STATE_ON` diffusé entre la lecture de `onResume` et l'inscription du receveur serait perdu | Mineur, **reporté** (fenêtre de l'ordre de la milliseconde, non observée) | — |
| M5. Le câblage `Route` / activité de la relecture n'est vérifié par aucun test automatique | **Consigné** : la preuve est la ligne §20 n° 5 | — |
| M6. §11.2 ne disait rien du réaffichage mesuré, et l'aide généralisait deux mesures (« au plus ») | **Corrigé** : mesures dans §11.2 ; « en général » dans l'aide et `LIMITES.md` | `HelpTextsTest` vert |
| M7. État initial bref, avec le bouton au lieu de l'instruction avant la première lecture du dépôt | Mineur, **reporté** | — |

## Incertitudes et limites

- **Réserves des lignes §20.** Ligne n° 2 : rejouer avec le même tag. Ligne n° 3 : regarder
  l'écran 6. Ligne n° 5 : incident `TIME_CHANGED` (voir `RESTE_A_FAIRE.md`).
- **Relance du Reader Mode.** Un second `enableReaderMode` remplace-t-il l'inscription de
  l'activité ? Ce n'est pas vérifié dans la documentation Android (Context7 indisponible ce
  jour-là) : seul le scan accepté après les relances des 18/10 le montre, sur un appareil.
- **TalkBack.** Non écouté à voix haute ; l'export du diagnostic avec plusieurs boîtiers n'a pas
  été déclenché sur appareil (prouvé en JVM).
- **Mesures du NFC après redémarrage.** Deux mesures sur un seul appareil.

**Rien n'est committé.**
