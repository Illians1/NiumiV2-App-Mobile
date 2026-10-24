# Étape 43 — Room v6, Direct Boot v5, dépôts du planning et des Niumi Spots

Date : 2026-10-24. **Étape terminée** : code, tests JVM, tests instrumentés et quatre essais
manuels sur appareil, framework iOS, ktlint, detekt, lint et assemblage. Onzième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 13, phase P). Point de départ : `b43adb3`.

## Ce qui est livré

- **Ensemble courant des boîtiers** (Q11, tranché le 2026-10-19). `HandleValidNfcUseCase` vérifie
  le scan contre `PairedBoxStore.all()`, lu au moment du scan et après l'éventuelle réconciliation.
  - Un boîtier ajouté après l'activation termine la session.
  - Un boîtier retiré pendant la session n'arrête plus rien.
  - Un ensemble vide, cas d'un scan avant déverrouillage, impossible par conception (§4.4), donne
    `NO_PAIRED_BOX_MATCH` : un refus, jamais une levée ni une acceptation.
- **Room v6** (`MIGRATION_5_6`) :
  - `alarm_session` gagne `releasedBy` et `disarmedAtEpochMillis`. Les deux colonnes sont
    nullables et sans `DEFAULT`, comme en v3.
  - `releasedBy` vaut `NFC_SCAN` si `nfcVerifiedAtEpochMillis` est renseigné, et reste nul sinon.
  - Toutes les sessions passent au schéma 3.
  - `session_box` est supprimée. `alarm_session` n'est pas recréée, donc aucun `CASCADE`.
  - Convertisseur `ReleaseMeansDto` ajouté.
  - Schéma `6.json` committé ; `5.json` reste inchangé.
- **Retraits** :
  - les fichiers `SessionBox`, `SessionBoxEntity`, `SessionBoxDao`, `SessionBoxMapper` et
    `DirectBootBoxMapper` ;
  - les champs `AndroidSessionExtras.boxes` et `DirectBootBox` ;
  - le figeage des boîtiers dans `RoomSessionStore.freezeFrom`. La sonnerie et la vibration
    restent figées.
- **Mappers** : `releasedBy` et `disarmedAtEpochMillis` passent dans les deux sens, côté Room
  (`SessionSnapshotMapper`) comme côté projection (`DirectBootMapper`). La fusion Direct Boot les
  reporte dans Room.
- **Projection Direct Boot v5**.
  - `releasedBy` et `disarmedAtEpochMillis` s'ajoutent, avec la valeur par défaut `null`.
  - `boxes`, `boxId` et `boxTokenSha256Hex` disparaissent. Un fichier v1 à v4 reste lisible : ces
    clés sont ignorées (`ignoreUnknownKeys`), et la façade déduit `NFC_SCAN` comme pour un
    snapshot v2.
  - Seuil figé : `PROJECTION_VERSION_WITH_RELEASE_MEANS = 5`.
- **Dépôt des boîtiers** :
  - `PairedBoxStore.remove` répond `PairedBoxRemoveResult` : `Removed`, `LastBoxKept` (le dernier
    boîtier, sans rien écrire ; décision Q22) ou `Unknown`.
  - `PairedBoxStore.points` est un `Flow` réémis par Room (`PairedBoxDao.observe()`). Avant
    déverrouillage, il émet une seule liste vide, sans résoudre le `Provider`.
- **`RELEASING` vers `DISARMED`** :
  - `isBlockingInForce` et `isSessionDark` répondent « non » : thème clair, garde du blocage en
    phase « avant le blocage ».
  - `requiresAlarmScreen(state, releaseTarget)` répond « non ».
  - `RoomBlockedPackagesSource` ne bloque rien, ni en Room ni dans le miroir Direct Boot, même sans
    `REMOVE_BLOCKING` (constat de la relecture, ci-dessous).

  Android ne produit pas encore `DISARMED` : c'est l'étape 44.
- **Planning** (`SYS/plan/`) :
  - `WakePlan(enabled, enabledIsoDays, wakeTimeIso, bedtimeIso)`. Par défaut : éteint, lundi à
    vendredi, 07:00, 22:30.
  - `WakePlanStore` et `DataStoreWakePlanStore`, fichier `niumi_wake_plan`.
  - Le décodage est pur et se fait champ par champ : une valeur illisible retombe sur le défaut de
    son seul champ, et « aucun jour coché » est un planning valide.
  - L'écriture refuse un planning invalide (`WAKE_PLAN_INVALID`).
  - Le `Flow` émet le défaut avant déverrouillage.
- **Durée déclarée** (`SYS/insight/`) :
  - `DeclaredMinutes.ALLOWED` porte les onze valeurs de `SPECIFICATION.md` §7.
  - `TimeToGetUpPreferences` et `DataStoreTimeToGetUpPreferences`, fichier `niumi_time_to_get_up`.
  - Rien de déclaré se lit `null`. Une valeur hors liste est refusée à l'écriture
    (`DECLARED_MINUTES_INVALID`) et ignorée à la lecture.
- **Garde de déverrouillage** : les deux nouveaux dépôts suivent le patron de
  `DataStoreSetupPreferences`. Le binding Hilt est dans `PlanModule`, à part de `ReadinessModule`,
  qui est au plafond detekt.
- **Écran 3 transitoire (« Mes boîtiers »)** :
  - Aucun verrou en session : ajout, renommage et retrait restent actifs, et une session qui
    démarre n'interrompt ni la lecture ni un dialogue.
  - « Continuer » est masqué en session.
  - « Retirer » est masqué quand il ne reste qu'un boîtier (`canRemove`).
  - Le texte « Une session est en cours… » est supprimé, ainsi que la fermeture automatique de la
    route.
- **Écran de session active** : un lien transitoire « Mes boîtiers » ouvre l'écran 3, au-dessus de
  « Voir le diagnostic ». Il disparaît avec cet écran à l'étape 45.
- **Activation et diagnostic** :
  - `ArmSessionUseCase` lit toujours les boîtiers une fois, pour refuser `NO_PAIRED_BOX`, mais ne
    les copie plus dans la session.
  - L'export du diagnostic liste les boîtiers enregistrés : `DiagnosticSources` lit
    `PairedBoxStore` au lieu de la passerelle de session.
- **Aide** : la puce « Boîtiers » de `LIMITES.md` et de `HelpTexts` est réécrite.
- **Écran « Scan requis »** (transitoire jusqu'à l'étape 46) : le refus d'un boîtier non enregistré
  devient « Ce Niumi Spot n'est pas connu. Rien n'a été modifié. ».

## Décisions de Mehdi (2026-10-24)

1. **Accès à l'écran 3 en session.** Un lien transitoire sur l'écran de session active, avec
   « Continuer » masqué. Sans lui, l'écran 3 n'était atteignable que par la mise en route : aucun
   ajout en session n'était possible, ni son test manuel.
2. **Dernier boîtier.** « Retirer » masqué, sans texte nouveau. Le texte « Tu dois garder au moins
   un Niumi Spot. » viendra avec l'écran 12 › Niumi Spots, à l'étape 48.
3. **Aide.** Nouvelle puce : « N'importe quel boîtier enregistré au moment du scan termine la
   session. Tu peux en ajouter, en retirer ou en renommer à tout moment, même pendant une
   session ; il en reste toujours au moins un. Un boîtier retiré ne termine plus rien. »
4. **Message de refus**, demandé pendant les essais : « Ce Niumi Spot n'est pas connu. Rien n'a été
   modifié. », à la place de « Ce boîtier n'est pas celui de ta session. Rien n'a été modifié. »,
   devenu faux sans liste figée. Le point final est ajouté, comme sur les autres messages de
   l'écran.

## Écarts au plan

1. **Test de migration.** Il est dans `NiumiDatabaseSchemaTest`
   (`migrationFiveToSixKeepsTheArmedSessionDropsSessionBoxAndDeducesReleasedBy`), comme toutes les
   migrations précédentes, et non dans un `Migration5To6Test`. L'insertion des lignes enfants est
   factorisée avec le test 4→5.
2. **`requiresAlarmScreen(state, releaseTarget)`** et
   **`launcherDestinationFor(state, releaseTarget, …)`**. La cible de `RELEASING` est indispensable,
   et l'état seul ne la porte pas. Les tests de l'app n'ont pas de fixture de snapshot : deux
   paramètres plutôt qu'un snapshot.
3. **Projection v5 sans `boxId` ni `boxTokenSha256Hex`.** Le plan les gardait à `""` pour relire
   les fichiers v1 à v3. Ce n'est pas nécessaire : `ignoreUnknownKeys` suffit, et le fichier v3
   réel en ressource de test le prouve.
4. **`DiagnosticSources`.** `gateway` n'y lisait que la liste figée ; il est remplacé par
   `pairedBoxStore`.
5. **`EventFingerprintTest`.** Inchangé : il fige l'empreinte d'un `SessionEventDto`, pas d'un
   snapshot, et reste vert.
6. **Fichiers hors liste touchés** : `LIMITES.md`, `HelpTexts`, `ActiveSessionScreen`,
   `ActiveSessionActions`, `ActiveSessionTexts`, `NiumiNavHost`, `PairingScreen`, `PairingTexts`,
   `SetupGate` (KDoc), `SessionCoordinator` (KDoc), `BlockingGuardPolicyTest` (cas `DISARMED`
   ajouté) et trois tests instrumentés de `:app`, qui portaient des boîtiers figés.
7. **Ordre test puis code.** Pour les mappers Room et Direct Boot, les fixtures à valeurs
   distinctes ont été écrites juste après le code. Elles attrapent l'oubli d'un des deux champs,
   mais leur échec n'a pas été observé avant l'implémentation. Pour tout le reste, l'échec a été
   vu d'abord : schéma, scan, états, dépôts, écran 3.

## Ce que cette étape ne couvre pas, volontairement

- `WakePlanScheduler`, désarmement et ré-armement : étape 44. Rien ne lit encore les deux nouveaux
  dépôts.
- L'écran « Réveil » de l'accueil et le cadran : étape 45. L'écran 3 actuel et son lien sont
  transitoires.
- La feuille de confirmation de suppression et le texte du dernier boîtier : étape 48.
- Le mot « boîtier » dans les textes : balayage de l'étape 53.

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md`.
  - §1 et §3 : ensemble courant, boîtiers modifiables en session.
  - §7.2 : colonnes v6, paragraphe « Boîtiers », invariants du dépôt, `SessionBoxEntity` retirée.
    Le paragraphe « Contrat 1.5 avant v6 » devient « v6 ».
  - §7.3 : champs v5, paragraphe « Boîtiers », projection v5, nouveaux `DataStore` sous la garde,
    paragraphe « Planning et durée déclarée », exception `all()` réécrite.
  - §11.1 : l'association n'est plus fermée en session, de un à dix boîtiers, dernier refusé.
  - §11.2 : tableau.
  - §11.3 : point 3 et reprise avant déverrouillage.
  - §13 : `TOO_MANY_PAIRED_BOXES` au-delà de 10 (oubli de l'étape 42).
  - §15 : écran 3 (dernier boîtier, dix, session), écrans 7, 12 et 13.
  - §17 : export des boîtiers enregistrés.
  - §19 : tests de l'étape 43.
  - §20 : critères.
- `specs/SPEC_CORE_KMP.md` : décisions 11 et 16, invariant §4, §10 et §13 au présent. SPEC_IOS
  n'est pas touché : aucune section KMP ne change.
- `docs/android/LIMITES.md` : puce « Boîtiers ».
- Plan de refonte : écarts de l'étape 43, cases cochées, points de vigilance 6 et 16.

## Tests

- **JVM** : 1 699 tests, aucun échec (KMP 303, Android 1 396), contre 1 675 à l'étape 42.
  - **Nouveaux** :
    - `WakePlanStoreTest` (8) ;
    - `TimeToGetUpPreferencesTest` (4) ;
    - `DataStoreUnlockGuardTest`, +4 : planning et durée, lecture neutre et écriture refusée ;
    - `RoomPairedBoxStoreUnlockGuardTest`, +1 : `points` ;
    - `ExportedSchemaTest` : v6, `session_box` absente, colonnes nullables ;
    - `DirectBootSnapshotJsonTest` : v3 réel sans boîtiers, v4 à `boxes` ignorée, v5 ;
    - `DirectBootMapperTest` : aller-retour du moyen de libération ;
    - `SessionStatesTest` : `RELEASING` vers `DISARMED` clair ;
    - `LauncherDestinationTest` : `RELEASING` vers `DISARMED` sans écran de réveil ;
    - `ActiveSessionTextsTest` : « Mes boîtiers » ;
    - `DirectBootBlockedPackagesSourceTest` : `RELEASING` vers `DISARMED` ne bloque rien ;
    - `PairingViewModelTest` : session publiée pendant la lecture des boîtiers (défaut trouvé sur
      appareil, voir plus bas) ;
    - `ActiveSessionTextsTest` : message de refus mot pour mot.
  - **Réécrits** :
    - `HandleValidNfcUseCaseTest` : boîtier ajouté accepté, retiré refusé, ensemble vide, trois
      boîtiers, `DISARMED` non éligible ;
    - `PairingViewModelTest` : session sans verrou et « Continuer » masqué, dernier boîtier
      conservé ;
    - `ArmSessionUseCaseTest` : boîtiers lus une fois, jamais copiés.
  - **Échecs vérifiés avant implémentation** :
    - `ExportedSchemaTest`, sur `6.json` absent ;
    - `HandleValidNfcUseCaseTest`, à la compilation, quand la liste figée était encore lue ;
    - `SessionStatesTest`, sur `releasingTowardsDisarmedIsLightAndNotBlocking` ;
    - les tests des dépôts, à la compilation ;
    - `PairingViewModelTest`, sur `canRemove`.
- **Instrumentés**, lancés sur appareil (voir plus bas) :
  - `NiumiDatabaseSchemaTest` (5→6) ;
  - `RoomPairedBoxStoreTest` (dernier refusé, inconnu, flux `points`) ;
  - `RoomDirectBootMergeTest` (moyen de libération reporté) ;
  - `RoomBlockedPackagesSourceTest` (`RELEASING` vers `DISARMED` ne bloque rien) ;
  - `DataStoreWakePlanStoreInstrumentedTest` ;
  - `DataStoreTimeToGetUpPreferencesInstrumentedTest` ;
  - `ActiveSessionBoxesLinkTest` ;
  - `PairingScreenTest` (session, un seul boîtier).
- **Framework iOS** (`linkDebugFrameworkIosSimulatorArm64`) : vert.
- **ktlint, detekt, `:app:lintDebug`, `:app:assembleDebug`** : verts. Refus de detekt corrigés :
  lignes de KDoc trop longues, `Dispatchers.Default` dans un test.

## Relecture indépendante

Une relecture du diff complet, par un agent distinct, n'a relevé aucun défaut critique.

- **Important, corrigé.** `RoomBlockedPackagesSource` aurait bloqué les applications pendant
  `RELEASING` vers `DISARMED`. `DisarmReducer` n'émet aucun `REMOVE_BLOCKING`, et la règle Room
  lit cette absence comme « retrait pas encore fait », donc blocage. Le thème et la garde, eux,
  disent « hors blocage ». Ce défaut serait apparu dès l'étape 44. Correction : `RELEASING` vers
  `DISARMED` ne bloque rien, en Room comme en Direct Boot. L'échec du test JVM a été vu d'abord.
- **Mineurs, corrigés** :
  - SPEC_ANDROID §19 citait encore l'association fermée en session ;
  - un import inutilisé dans `IncidentDiagnosticViewModel` ;
  - le KDoc de `isBlockingInForce` sur l'annulation avant le coucher ;
  - écart provisoire à la décision 11 documenté en §11.1 : pas d'ajout de boîtier pendant la
    sonnerie avant l'étape 49 ;
  - liste figée non reversée dans `paired_box` par la migration, acceptée et documentée en §7.2.
- **Mineurs, laissés** :
  - **Annulation avant le coucher.** Un scan d'annulation avant le coucher (`RELEASING` vers
    `CANCELLED`, blocage jamais appliqué) bloque en Room jusqu'au `REMOVE_BLOCKING`. Ce défaut est
    antérieur à cette étape et ne dure qu'une seconde. Il est hors du périmètre.
  - **Session finie sous l'écran 3.** Si la session se termine pendant que l'écran 3 est ouvert
    par-dessus l'écran 7, « Continuer » réapparaît au retour. L'écran est transitoire jusqu'à
    l'étape 45.
  - **Fusion Direct Boot.** Une fusion depuis une projection v4 peut réécrire `releasedBy` à `null`
    dans Room. C'est sans effet : la façade déduit `NFC_SCAN` à la lecture.
  - **Constante non lue en production.** `PROJECTION_VERSION_WITH_RELEASE_MEANS` n'est lue par
    aucun code de production. Elle est gardée comme seuil documenté, vérifié par
    `DirectBootSnapshotJsonTest`.

## Validation sur appareil

Faite le 2026-10-24 sur Xiaomi 25080RABDG, Android 16.

**`connectedDebugAndroidTest`.**

- **Premier passage** :
  - `:core:database` n'a pas tourné : le téléphone a refusé l'installation (« Install canceled by
    user », la fenêtre « Installer via USB » n'a pas été acceptée à temps).
  - `PairingScreenTest.fiveBoxesHideTheAddButtonAndSayWhy` a échoué. Ce test était périmé depuis
    l'étape 42 : il attendait encore cinq boîtiers au plus. Il devient
    `tenBoxesHideTheAddButtonAndSayWhy`.
  - `BlockingGuardServiceInstrumentedTest.fromTheAlarmTheGuardTakesOverTheAlarmNotification` a
    échoué sur une notification d'identifiant 8 en trop. C'est l'avertissement « accessibilité »,
    laissé par un test précédent du même passage, sans lien avec cette étape. Relancée seule, la
    classe passe (3/3).
- **Second passage** : `:core:database` (96 tests) et `:feature:setup` (56 tests), tout vert, dont :
  - `migrationFiveToSixKeepsTheArmedSessionDropsSessionBoxAndDeducesReleasedBy` ;
  - `removingTheLastBoxIsRefusedWithLastBoxKept`, `removingAnUnknownBoxAnswersUnknownAndWritesNothing`
    et `pointsEmitsOnAddRenameAndRemove` ;
  - `theReleaseMeansOfANewerProjectionReachesRoom` ;
  - `releasingTowardsDisarmedNeverBlocks` ;
  - `aSessionInProgressKeepsEveryBoxActionButHidesContinue` et `aSingleBoxCanBeRenamedButNotRemoved`.

  Tous les autres modules étaient verts dès le premier passage, dont les dépôts DataStore et
  `ActiveSessionBoxesLinkTest`.

**Essais manuels**, avec deux boîtiers, « Gris » et « Cassé » :

| Essai | Action | Observé |
| --- | --- | --- |
| 1. Migration | Version `b43adb3` (Room v5), deux boîtiers, session armée ; mise à jour par-dessus | Session toujours armée, écran de session active avec « Mes boîtiers ». Relevé de la base : v6, `session_box` absente, session `ARMED` à la même révision, schéma 3, les deux noms dans le même ordre, `releasedBy` nul. Projection Direct Boot réécrite en v5, sans boîtiers |
| 4. Dernier boîtier | Retrait de « Cassé » en session | « Gris » seul, sans « Retirer ». **Mais « Continuer » s'affichait** : défaut corrigé, voir plus bas |
| 3. Retrait en session | Scan de « Cassé » retiré, depuis « Modifier ou annuler » | Refusé, session inchangée (`ARMED`, même révision), trois `NFC_SCAN_INVALID` journalisés. Après le changement de texte : « Ce Niumi Spot n'est pas connu. Rien n'a été modifié. » |
| 2. Ajout en session | Ajout de « Cassé » en session, puis scan | Accepté, session annulée. En base : `CANCELLED`, `releasedBy = 'NFC_SCAN'`, les deux boîtiers enregistrés |

**Défaut trouvé sur appareil, corrigé.**

- **Symptôme** : ouvert pendant une session armée, l'écran 3 montrait « Continuer ».
- **Cause** : `PairingViewModel.refreshBoxes()` écrivait `state = state.copy(boxes = all())`.
  `state` était lu **avant** la lecture suspendue de Room. Le snapshot publié pendant cette lecture
  passait `isSessionInProgress` à vrai, puis la copie, partie de l'ancien état, l'écrasait. Les
  tests JVM ne le voyaient pas : ils appelaient `onSessionStateChanged` directement, et leur faux
  dépôt ne suspendait pas.
- **Correction** : lire d'abord, copier ensuite. L'échec de
  `aSessionPublishedWhileTheBoxesAreReadStillHidesContinue` a été vu avant la correction. Vérifié
  ensuite sur appareil.
- **Même schéma ailleurs** : `RingtoneViewModel.load()` lit lui aussi un dépôt dans l'argument d'un
  `copy`. Une pré-écoute qui démarre ou s'arrête, ou une sonnerie choisie pendant les quelques
  millisecondes de la lecture, serait effacée de l'affichage ; ce n'a jamais été observé. Le
  `ViewModel` n'est pas touché ici : reporté à l'étape 53 avec l'accord de Mehdi du 2026-10-24, la
  tâche est inscrite dans le plan de refonte, section « Étape 53 ».

**Après ces essais**, le service d'accessibilité est peut-être à réactiver : les tests
instrumentés de `:app` le laissent arrêté. L'app de l'étape 43 est installée, avec les deux
boîtiers et aucune session.

**Restent non validés** :

- le scan avant tout déverrouillage, impossible par conception (§4.4) ;
- un vrai `DISARMED`, qui arrive à l'étape 44 ;
- l'ajout d'un boîtier pendant la sonnerie, qui arrive à l'étape 49.
