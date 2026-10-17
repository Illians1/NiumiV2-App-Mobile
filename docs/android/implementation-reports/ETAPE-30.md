# Étape 30 — Persistance Android : boîtiers nommés, `session_box`, Room v5, Direct Boot v4

Dates : 2026-10-16 et 2026-10-17. **Étape terminée** : code, tests automatisés, tests instrumentés
et les deux essais manuels du plan verts sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS
OS3.0.302.0.WPPEUXM), relecture finale faite et ses deux correctifs intégrés (voir « Relecture
finale »). 1 065 tests JVM verts sur les six modules Android, 135 tests instrumentés verts en
passe complète rejouée après les correctifs (1 ignoré, préexistant), ktlint, detekt et Android Lint
verts, `:app:assembleDebug` vert.

Deuxième étape du Lot 8 (boîtiers multiples, conception
`docs/superpowers/specs/2026-10-13-boitiers-multiples-design.md` §4). Android enregistre
maintenant jusqu'à cinq boîtiers nommés, fige la liste dans la session à l'activation et reconnaît
n'importe lequel d'entre eux au scan ; la base Room passe en v5 et la projection Direct Boot en v4,
toutes deux migrées depuis une installation du Lot 7 **sur donnée réelle**. Aucune interface
nouvelle : l'écran 3 n'a reçu que l'adaptation minimale prévue, « Mes boîtiers » est l'étape 31.

## Ce qui est livré

- **Dépôt des boîtiers** (`:core:database/pairing`) : `PairedBoxStore` devient `all()`,
  `add(credential, name): PairedBoxAddResult` (`Added` / `AlreadyPaired` / `LimitReached`),
  `rename(boxId, name)`, `remove(boxId)`. `RoomPairedBoxStore` fait chaque écriture dans
  `withTransaction` : `add` vérifie d'abord le doublon (`findById`), puis le plafond
  (`count() >= PairedBoxLimit.MAX`, constante du contrat commun, pas de duplication), puis insère
  en position `count` ; `remove` supprime et renumérote `0..n-1`. `add` et `rename` passent le nom
  par `PairedBoxName.sanitize` **avant tout accès à la base** et écrivent la valeur nettoyée
  (`trim()`, 1 à 30 caractères) ; un nom refusé lève `IllegalArgumentException("PAIRED_BOX_NAME_INVALID")`
  — l'écran de l'étape 31 validera avec la même fonction avant d'appeler. `all()` renvoie une
  liste vide avant déverrouillage **sans résoudre le `Provider`**
  (exception documentée §7.3, reportée de `current()` sur `all()`) ; les écritures lèvent
  `ROOM_BEFORE_UNLOCK`.
- **Room v5** : `PairedBoxEntity` + `name`, `position` ; nouvelle table `session_box`
  (`SessionBoxEntity`, clé `(sessionId, boxId)`, `FOREIGN KEY … ON DELETE CASCADE`, index
  `sessionId`, colonnes `tokenSha256`, `name`, `position`) sur le modèle exact de `blocked_app` ;
  `alarm_session` perd `boxId` et `boxTokenSha256Hex`. `MIGRATION_4_5` enregistrée dans
  `DatabaseModule`, `schemas/5.json` exporté (9 tables, `identityHash
  170d6d31fab3c1750f83d409a8e49b38`).
- **Gel de la liste** : `AndroidSessionExtras.boxes: List<SessionBox>` remplace les deux champs ;
  `RoomSessionStore.writeDecision` n'écrit `session_box` que si la session n'existait pas
  (`existing == null`), dans la transaction de décision ; `freezeFrom` relit
  `sessionBoxDao.forSession(id)` et ignore les `boxes` apportées par une décision suivante — la
  liste est figée comme la sonnerie et les applications.
- **Direct Boot v4** : `DIRECT_BOOT_PROJECTION_SCHEMA_VERSION = 4`, seuil figé
  `PROJECTION_VERSION_WITH_MULTIPLE_BOXES = 4` (jamais « < version courante »),
  `Active.boxes: List<DirectBootBox>` **sans les noms** ; `boxId` et `boxTokenSha256Hex` restent
  dans la classe avec la valeur par défaut `""` pour qu'un fichier v1, v2 ou v3 se désérialise, et
  `DirectBootMapper.toExtras` le relit comme une liste d'un seul boîtier (`name = ""`). La fusion
  `DirectBootRoomMerge` ne touche jamais `session_box` : la projection ne porte pas les noms,
  Room reste la seule autorité sur la liste figée.
- **Scan** : `HandleValidNfcUseCase` construit un credential par boîtier de `extras.boxes` (version
  de protocole du payload portée) et passe la liste au vérificateur commun ; une liste figée vide
  donne `UnknownBox` sans lever.
- **Activation** : `ArmSessionUseCase` lit `pairedBoxStore.all()` une seule fois, refuse une liste
  vide (`NO_PAIRED_BOX`) et fige tous les boîtiers dans l'ordre des positions ;
  `ActivationPreview.boxNames` remplace `boxId`.
- **Diagnostic** : `ReadinessReport.pairedBoxCount` remplace `hasPairedBox` (fin du transitoire de
  l'étape 29), contrôle `PAIRED_BOX` vert pour `1..5`, rouge pour 0 et 6. L'export (§17) écrit une
  ligne par boîtier figé, « `- Boîtier : {nom} ({8 caractères du boxId}…)` », jamais une empreinte.
- **Écran 3, adaptation minimale** : `PairingViewModel` ajoute le boîtier scanné sous le nom
  « Boîtier {n} » (n = boîtiers déjà associés + 1) ; un boîtier déjà associé affiche « Ce boîtier
  est déjà associé. », un sixième « 5 boîtiers maximum. » ; le dialogue de remplacement,
  `pendingReplacement` et les quatre textes `REPLACE_*` disparaissent.
- **Écran 6** : la ligne boîtier du récapitulatif affiche les noms joints par « , » (le reste de
  l'écran est l'étape 31).

## Décisions prises pendant l'étape

| Décision | Choix | Pourquoi |
| --- | --- | --- |
| `PRAGMA foreign_keys = OFF` / `ON` dans `MIGRATION_4_5` (demandé par le plan) | **Non écrit.** La migration se termine par `PRAGMA foreign_key_check`, qui la fait échouer s'il reste une violation. | La pragma est sans effet dans une transaction (doc SQLite `foreignkeys.html` §2) et Room exécute les migrations dans une transaction. Vérifié dans le code généré : Room n'exécute `PRAGMA foreign_keys = ON` que dans `onOpen`, **après** les migrations — c'est cela qui empêche `DROP TABLE alarm_session` de déclencher les `CASCADE`. Écrire la pragma aurait documenté un mécanisme faux. SPEC_ANDROID §7.2 « v5 » décrit le vrai. |
| Nom à l'ajout | « Boîtier {n} » plutôt que « Boîtier 1 » fixe | Conception §4.1. Deux scans de boîtiers distincts avant l'étape 31 ne créent pas deux « Boîtier 1 ». La migration nomme l'existant « Boîtier 1 ». |
| Noms dans la projection Direct Boot | Absents | Avant déverrouillage, seule la vérification du scan a besoin de la liste ; les noms sont une donnée d'affichage, relue dans Room après déverrouillage. Un nom de moins en clair sur le stockage non chiffré. |
| `DirectBootRoomMerge` et `session_box` | La fusion ne lit ni n'écrit `session_box` | `toEntity` n'écrit plus de colonne boîtier ; une session inconnue de Room n'est jamais insérée par la fusion (`UnknownSession`), donc aucun cas « boîtiers sans nom à insérer ». |
| `freezeFrom` | Fonction `suspend` qui relit `session_box` | L'entité ne porte plus la liste ; `RoomSessionStore` est au plafond detekt, la relecture se fait hors de la classe. |

## Défaut évité pendant l'étape

**`schemas/4.json` réécrit silencieusement par KSP.** Modifier `PairedBoxEntity` avant de monter
`NiumiDatabase.version` a fait régénérer le schéma **4** avec la nouvelle forme de `paired_box` :
le schéma historique, déjà committé, aurait été faux sans qu'aucun test ne le dise avant qu'un
`MigrationTestHelper` reconstruise une base v4. Restauré par `git checkout`, puis la version a été
montée à 5 avant tout autre changement d'entité. Règle retenue : **monter la version Room et
régénérer avant de toucher une entité, jamais après.**

## Specs et documents modifiés dans le même changement

- **SPEC_ANDROID** : §7.2 (`PairedBoxEntity` + `name`, `position` ; `SessionBoxEntity` ; paragraphe
  « Lot 8 » sur le gel de la liste à la place du gel des deux colonnes ; alinéa **v5** avec le vrai
  mécanisme anti-cascade), §7.3 (`boxes` dans le snapshot, alinéa **Projection v4** et pourquoi
  sans les noms, exception documentée `RoomPairedBoxStore.all()`), §11.1 (jusqu'à cinq boîtiers
  nommés, 1–30 caractères, doublon et sixième refusés au dépôt ; « un seul boîtier » et « toute
  nouvelle association remplace l'ancienne » supprimés), §11.3 point 3 (« contre la liste
  `session_box` »), §13 point 2 (`pairedBoxCount`, `TOO_MANY_PAIRED_BOXES`), §17 (une ligne par
  boîtier figé), §19.1 et §19.2 (tests du lot).
- **Plan maître** : point de vigilance 11 complété et étape 31 étendue (voir « Observation sur
  appareil »).
- `LIMITES.md` et `RESTE_A_FAIRE.md` : inchangés, le plan les confie à l'étape 31 (« validation et
  documents »).

## Tests écrits

**JVM** (rouge → vert, sauf mention) :
- `PairedBoxNameTest` (6 cas : trim, vide, blanc, 30 gardé, 31 refusé, « Salle de bain »).
- `RoomPairedBoxStoreUnlockGuardTest` : `all()` vide sans résoudre le `Provider`, `add` / `rename` /
  `remove` lèvent `ROOM_BEFORE_UNLOCK`.
- `RoomPairedBoxStoreNameGuardTest` (relecture finale) : `add` d'un nom blanc et `rename` de 31
  caractères lèvent `PAIRED_BOX_NAME_INVALID` **sans résoudre le `Provider`** — rouge observé
  (la base était résolue), vert après correctif.
- `ExportedSchemaTest` : version 5, table `session_box`, colonnes retirées d'`alarm_session`,
  colonnes ajoutées à `paired_box`.
- `SessionSnapshotMapperTest.extrasRoundTripPreservesTheFrozenBoxList`.
- `DirectBootMapperTest` : `aVersionThreeProjectionReadsAsAListOfOneBox`,
  `aVersionFourProjectionRoundTripsItsBoxes`, version courante 4 ;
  `aVersionTwoProjectionKeepsItsPendingBlocking` et la traduction de `LegacyRingtone` conservés.
  Deux tests existants comparent désormais `blockedPackages` et `volumeRampSeconds` au lieu des
  extras entiers, puisque les noms ne traversent plus la projection.
- `DirectBootSnapshotJsonTest` : clé `boxes` dans le doré v4 ; **vrai fichier v3** en ressource
  (`src/test/resources/directboot/projection-v3.json`, généré avec l'encodeur de `HEAD` avant toute
  modification) relu comme une liste d'un boîtier ; aller-retour v4.
- `HandleValidNfcUseCaseTest` : `aTagMatchingTheSecondOfThreeFrozenBoxesEndsTheSession` (la preuve
  porte le `boxId` du deuxième), `aTagMatchingABoxPairedAfterActivationIsRefused`,
  `anEmptyFrozenListRefusesEveryScanWithoutThrowing`. *Écrits après l'implémentation* : la cascade
  de compilation (`extras.boxes`) imposait le code d'abord ; seul le vert a été observé.
- `AndroidDeviceReadinessCheckerTest.fiveOrFewerPairedBoxesPassTheCheckSixDoesNot` (1, 5, 6) et
  `noPairedBoxBlocksAndProposesPairing` (0).
- `ArmSessionUseCaseTest` : `theExtrasFreezeEveryPairedBoxInOrder`, `aBoxRemovedBeforeArmingIsNotFrozen`
  (cinq, un retiré → quatre), `noPairedBoxAtAllIsRefusedWithNoPairedBox`,
  `thePreviewListsTheBoxNamesInOrder` ; journal `pairedBox.all` lu une fois.
- `SummaryViewModelTest` (`boxNames`), `DiagnosticExporterTest.theExportListsEachFrozenBoxByNameAndEightCharactersAndNeverAHash`
  (deux boîtiers), `IncidentDiagnosticViewModelTest`.
- `PairingViewModelTest` : `aValidTagForAnAlreadyPairedBoxShowsAlreadyPairedWithoutWriting`,
  `aSecondDistinctBoxIsAddedAndNamedByItsPosition` remplacent les quatre tests du remplacement.
- Fakes réécrits en liste : `ReadinessTestSources.FakePairedBoxStore`,
  `ActivationTestFakes.RecordingPairedBoxStore`, `PairingTestFakes.FakePairedBoxStore` ; fixtures
  `SessionDtoFixtures`, `NfcScanFixtures` (deux boîtiers de plus), `SessionSnapshotDtoFixtures`,
  `RoomTestFixtures`, `ActiveSessionTestFakes`, `AlarmTestFakes`, `RingtoneTestFakes`,
  `ReadinessTestFixtures`.

**Instrumentés** (écrits avant le branchement de l'appareil, rouge/vert observé le 2026-10-17) :
- `NiumiDatabaseSchemaTest.migrationFourToFiveMovesTheFrozenBoxIntoSessionBoxAndDropsTheColumns` :
  base v4 où `paired_box` contient B et la session `ARMED` est figée sur A, plus un reçu, une ligne
  d'outbox, une application bloquée, un incident et le pointeur de session active (ajouté à la
  relecture finale : sixième enfant en `CASCADE`, le plus visible à perdre) ; après migration,
  `session_box` contient exactement A « Boîtier 1 » position 0 (**pas B**), `alarm_session` n'a
  plus les deux colonnes et garde les autres, les cinq enfants de la v4 ont toujours leur ligne,
  `paired_box` est nommée.
- `RoomPairedBoxStoreTest` réécrit (les sept cas du plan, plus `addStoresTheTrimmedName` et
  `renameStoresTheTrimmedName` à la relecture finale — rouge observé sur appareil, vert après
  correctif), `PairedBoxDaoTest` (+ 4),
  `RoomSessionStoreCommitTest` (`session_box` écrite à la création, intacte à la décision
  suivante), `RoomDirectBootMergeTest.frozenBoxListIsNeverOverwrittenByTheProjection`,
  `PairingScreenTest.anAlreadyPairedMessageIsShownWithoutAnyDialog` ; les trois tests de `:app`
  construisent leurs extras avec `boxes`.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# Régénération de schemas/5.json : monter NiumiDatabase.version à 5 PUIS compiler avec KSP
# (room { schemaDirectory("$projectDir/schemas") } écrit schemas/com.niumi.database.NiumiDatabase/5.json)
./gradlew :core:database:compileDebugKotlin

./gradlew :core:database:testDebugUnitTest :core:system:testDebugUnitTest :feature:session:testDebugUnitTest \
          :feature:setup:testDebugUnitTest :feature:ringing:testDebugUnitTest :app:testDebugUnitTest
#   avant relecture : 146 + 530 + 232 + 74 + 43 + 38 = 1 063 tests, 0 échec
#   après la passe de correction (batterie complète rejouée le 2026-10-17 à 15:06) : 148 + 530 + 232 + 74 + 43 + 38 = 1 065, 0 échec
./gradlew :app:assembleDebug                      # vert
./gradlew ktlintCheck detekt :app:lintDebug       # verts (ktlintFormat passé sur core:database, core:system test, feature:setup ;
                                                  #   detekt : DirectBootBoxMapper.kt extrait de DirectBootMapper.kt (TooManyFunctions),
                                                  #   deux helpers extraits dans NiumiDatabaseSchemaTest (LongMethod), un KDoc reformaté, un UseOrEmpty).
                                                  #   La batterie rejouée après la relecture a d'abord échoué sur ktlint :
                                                  #   la chaîne `?.` réécrite pour UseOrEmpty (IncidentDiagnosticViewModel) n'avait
                                                  #   pas été repassée à ktlintCheck ; un `?.` par ligne, puis tout vert.
grep -rn "boxTokenSha256Hex\|\.boxId\b" androidApp --include='*.kt' \
  | grep -v "/build/\|/test/\|/androidTest/\|DirectBootSnapshot.kt\|DirectBootMapper.kt\|Migrations.kt\|SessionBox"
#   vide hors KDoc explicatif et accès légitimes à SessionBox.boxId / PairedBoxDto.credential.boxId / DiagnosticBox.boxId
grep -rn "hasPairedBox\|pendingReplacement\|REPLACE_" androidApp --include='*.kt' | grep -v /build/   # vide

# Appareil branché (2026-10-17 13:31–13:32)
./gradlew :core:database:connectedDebugAndroidTest :feature:setup:connectedDebugAndroidTest :app:connectedDebugAndroidTest
#   core:database 88/88, feature:setup 33/33, app 11/12 (1 assumeTrue préexistant, BlockingStartAlarmVisibilityTest)
#   Rejouée en entier après les correctifs de relecture (15:09–15:13) : core:database 90/90, feature:setup 33/33,
#   app 11/12 (même test ignoré) — 135 tests, 0 échec. HyperOS annule l'installation du paquet de test si l'écran
#   est verrouillé ou si la fenêtre « Installer via USB » n'est pas acceptée (INSTALL_FAILED_USER_RESTRICTED) :
#   téléphone déverrouillé et fenêtre acceptée à la main, puis :app relancé seul.
```

**Première passe instrumentée rouge sur le test de migration** : `28 values for 29 columns` — le
`INSERT` de `seedV4ArmedSession` oubliait le `NULL` de `failureCode`. Faute de frappe dans le test
lui-même, invisible en JVM puisque ce test est instrumenté uniquement ; corrigée, deuxième passe
verte.

## Validation sur appareil

Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, 2026-10-17. Horloges du Mac et du
téléphone concordantes à la seconde (epochs comparés avant chaque armement). Lecture de la base par
`adb exec-out run-as com.niumi.app cat` de `niumi.db`, `niumi.db-wal` **et** `niumi.db-shm` (la base
est en WAL : le fichier principal seul est en retard), `sqlite3` 3.50.6 côté hôte.

1. **Essai manuel 1 — mise à jour par-dessus une session armée (vert, 13:33–13:51).**
   - APK de `HEAD` (4a03d53, base v4, projection v3) installé, boîtier associé
     (`7c9e6a41…`), une application choisie, tous les contrôles au vert (accessibilité et exemption
     batterie accordées à la main par l'utilisateur), session `a697d214…` armée pour 13:50.
   - Poids avant mise à jour : `niumi.db` **4 096 o** + `niumi.db-wal` **304 912 o** (base peu
     peuplée, une seule session).
   - APK de l'étape 30 installé par-dessus (`adb install -r`), application rouverte : aucun crash,
     aucun `SNAPSHOT_CORRUPTED`, écran « Une session est en cours. »
   - Base après migration : `session_box` = **une ligne**
     (`a697d214…`, `7c9e6a41…`, `'Boîtier 1'`, 0) ; `paired_box` migrée avec `name` et `position` ;
     `alarm_session` sans `boxId` ni `boxTokenSha256Hex` (`PRAGMA table_info`) ; enfants intacts :
     2 reçus, 4 effets d'outbox, 1 application bloquée, 0 incident — **aucune perte par
     `CASCADE`**. Poids après : `niumi.db` **114 688 o** (112 KiB), WAL vidé par le checkpoint
     consécutif à la migration.
   - 13:50 : `AlarmActivity` affichée, alarme sonnante. Scan du boîtier d'origine → arrêt
     immédiat, écran « Aucune session ».
   - Base finale : `COMPLETED` / `COMPLETED`, révision 5, `completedAtEpochMillis` renseigné,
     `active_session_pointer` vide ; journal technique dans l'ordre `SESSION_PREPARING`,
     `ALARM_SCHEDULED`, `BLOCK_APPLIED`, `SESSION_ARMED`, `ALARM_RECEIVED`, `RINGING_STARTED`,
     `SESSION_RELEASING`, `SESSION_COMPLETED`, `NFC_SCAN_VALID`.
2. **Essai manuel 2 — mise à jour puis redémarrage sans déverrouillage (vert, 14:01–14:14).**
   - `pm clear`, APK de `HEAD` réinstallé, boîtier re-scanné, session `acd0e9be…` armée pour 14:10 ;
     APK de l'étape 30 installé par-dessus **sans rouvrir l'application** (migration Room non
     déclenchée, projection restée en v3), `adb reboot` aussitôt.
   - `isKeyguardShowing = true` après le démarrage complet (`boot_completed = 1`), et **toujours
     vrai au déclenchement** : l'alarme de 14:10 a sonné sur un appareil jamais déverrouillé, donc
     reconstruite entièrement depuis la **projection v3 lue comme liste d'un seul boîtier**, avant
     tout accès à Room.
   - Après déverrouillage, premier scan refusé avec « Le NFC est désactivé… » (NFC pourtant actif,
     voir ci-dessous) ; second scan quelques secondes plus tard accepté, session `COMPLETED`,
     révision 5. `session_incident` **vide** : les trois événements `NFC_DISABLED` du journal
     technique (un à `RINGING_STARTED` avant déverrouillage, deux juste après) n'ont créé aucun
     incident, la fenêtre `UnlockSettling` de l'étape 25 ayant joué côté
     `SessionReadinessMonitor`.

## Observation sur appareil, hors Lot 8 : faux « NFC désactivé » sur l'écran de sonnerie

Le refus du premier scan de l'essai 2 ne vient pas de la liste figée (le second scan du même
boîtier a réussi) mais d'une lecture unique de `NfcAdapter.isEnabled` au `ON_RESUME` de
`AlarmActivity`, sans relecture : après un démarrage, la pile NFC du système met environ 13 s à
revenir après le déverrouillage (mesure de l'étape 25), et l'écran garde le message tant qu'il
n'est pas relancé. Lacune préexistante des étapes 17–18, déjà connue pour l'écran 2 (point de
vigilance 11 du plan) ; `ScanToModifyViewModel` (écran 9) a la même lecture unique.

Décision prise avec l'utilisateur le 2026-10-17 : **correction à l'étape 31**, par relecture
événementielle sur `Context.nfcAdapterSettledChanges()` (déjà utilisée par l'écran 7), sans
fenêtre de grâce sur l'affichage, sans nouveau journal. Le plan maître porte le constat (point de
vigilance 11), la tâche cochable, ses tests nommés, la cinquième ligne §20 et le test manuel
associé. Aucun code n'a été touché pour cela à l'étape 30.

## Relecture finale

Relecture de l'ensemble du changement par un relecteur à contexte neuf (paquet : diff complet de
l'arbre de travail contre `4a03d53`), le 2026-10-17. Verdict « prêt avec correctifs » : aucun point
critique, trois importants, cinq mineurs. Après re-gradation par l'effet pour l'utilisateur :

| Constat | Décision | Preuve |
| --- | --- | --- |
| `RoomPairedBoxStore.add`/`rename` écrivaient `name` tel quel : `PairedBoxName.sanitize` existait, était testé, mais **n'était appelé nulle part en production** (point de vigilance 2 du lot, invariant §7.2) | **Corrigé** : le dépôt nettoie avant tout accès à la base et écrit la valeur nettoyée | `RoomPairedBoxStoreNameGuardTest` (2, JVM) et `addStoresTheTrimmedName`, `renameStoresTheTrimmedName` (instrumentés) rouges puis verts ; spec §11.1 et KDoc de `PairedBoxStore` complétés |
| Le test de migration ne semait ni ne vérifiait `active_session_pointer`, sixième table en `CASCADE` sur `alarm_session` | **Corrigé** : graine et assertions étendues | test instrumenté vert sur appareil (lacune de test : aucun rouge possible) |
| SPEC_ANDROID §7.2 « v5 » et conception §4.3 comptaient « cinq » tables enfants : il y en a six | **Corrigé** (erreur factuelle introduite par ce changement dans la source de vérité) | relecture ; la conception renvoie désormais à la spec pour le mécanisme réel de la pragma |
| KDoc inversé dans `SessionBoxEntity` (« nom historique d'`alarm_session` » pour `tokenSha256`, qui est le nom de `PairedBoxEntity`) | Mineur, **reporté** | — |
| Nommage « Boîtier {n} » après un retrait : « Boîtier 1 » et « Boîtier 3 » restants → le prochain s'appelle « Boîtier 3 » | Mineur, **reporté à l'étape 31** (inatteignable sans interface de retrait ; règle « plus petit n libre » à trancher dans la conception §4.1 / SPEC §15) | — |
| Commentaire périmé dans `DirectBootMerger.kt` (`boxTokenSha256Hex` → `boxes[].tokenSha256Hex`) | Mineur, **reporté** | — |
| `DiagnosticExporter` imprimerait « - Boîtier :  (xxxxxxxx…) » pour un nom vide | Mineur, **reporté** (inatteignable : l'écran 12 lit Room, où les noms existent) | — |
| `remove` renumérote par `upsert` (`INSERT OR REPLACE`) plutôt qu'un `UPDATE` de `position` | Mineur, **reporté** (correct, `paired_box` n'a pas d'enfant) | — |

Points que le relecteur a écartés et que je confirme : rétrogradation d'APK non prise en charge ;
faux « NFC désactivé » déjà tranché pour l'étape 31 ; `PairingViewModel` lit `all().size` puis
`add()` en deux appels (un seul écrivain) ; `INSERT INTO session_box … FROM alarm_session` copie
aussi les sessions terminées (voulu : une ligne par session, historique borné) ;
`DirectBootRoomParityTest` non étendu à `boxes`, couvert par
`aVersionFourProjectionRoundTripsItsBoxes` et `frozenBoxListIsNeverOverwrittenByTheProjection`.

Après la passe de correction : `:core:database` 148 tests JVM verts, les deux classes instrumentées
touchées rejouées (14 tests verts), puis batterie complète rejouée — JVM, qualité et passe
instrumentée entière sur appareil (voir « Commandes exécutées »).

## Ce qui ne change pas, et pourquoi c'est sûr

- Format du payload, parseur, `NfcVerificationProof`, machine à états, snapshot commun
  (`schemaVersion` 2) : intacts. La preuve porte le `boxId` du boîtier de la liste qui a
  correspondu.
- Une installation du Lot 7 arrive avec un seul boîtier : la migration le nomme « Boîtier 1 », la
  liste figée de sa session en cours en compte un, le scan donne le même verdict qu'avant.
- Aucune empreinte de token à l'écran, dans l'export ni dans le journal (§16, §17) ; la projection
  Direct Boot, en clair sur le stockage non chiffré, ne porte toujours aucun nom.

## Incertitudes et limites

- **Nommage « Boîtier {n} » et retrait** : `remove` renumérote les positions mais ne renomme pas ;
  après le retrait de « Boîtier 2 » parmi trois, le prochain ajout s'appelle « Boîtier 3 » comme
  le survivant. Sans effet sur la sûreté (le doublon est détecté sur `boxId`, le dépôt n'impose pas
  l'unicité des noms) et impossible à atteindre avant l'étape 31, qui apporte le renommage et où
  l'écran décide de la règle (voir « Relecture finale »).
- **Mineurs reportés de la relecture finale** : quatre points de forme ou de robustesse listés
  ci-dessus, aucun atteignable par l'utilisateur à cette étape.
- **Projection v4 avec `boxes` vide** : jamais écrite par le code (une session s'arme avec au moins
  un boîtier), lue comme une liste vide → `UnknownBox` à chaque scan, sans lever (point de
  vigilance 3). Une telle session se termine par l'arrêt forcé ou l'extinction, comme toute session
  sans boîtier reconnu (`LIMITES.md`).
- **Écart à TDD strict, consigné** : les tests instrumentés ont été écrits et compilés avant le
  branchement de l'appareil, et leur première exécution a eu lieu sur le code final ; les trois
  tests de `HandleValidNfcUseCaseTest` et le test de migration ont été écrits après le code qu'ils
  couvrent, la cascade de compilation ne permettant pas l'ordre inverse sans compiler tout le
  module. Pour eux, seul le vert a été observé (sauf le rouge accidentel « 28 values for 29
  columns » du test de migration, qui prouve au moins qu'il s'exécute).
- **Faux « NFC désactivé »** sur les écrans 8 et 9 après un redémarrage : voir ci-dessus, corrigé
  à l'étape 31 ; d'ici là, il suffit de scanner de nouveau quelques secondes plus tard.

**Rien n'est committé.**
