# Étape 9 — Room : entités, DAO, transaction de décision, journal et mappings KMP

Date : 2026-09-09. Persistance Android canonique (`:core:database`), premier consommateur natif de
`NiumiCoreFacade` (SPEC_CORE_KMP §13, §6.1 ; SPEC_ANDROID §7.2, §16, §17). Aucun composant système
n'est encore branché dessus : le `SessionCoordinator` (étape 11) et le snapshot Direct Boot
(étape 10) restent à écrire.

## Résumé

Livre le schéma Room v1 (huit entités), l'aller-retour `AlarmSessionEntity ↔ SessionSnapshotDto`
pour les neuf états de SPEC_CORE_KMP §5, la transaction unique `commitDecision` (session, apps
bloquées, pointeur, reçu, effets), l'empreinte canonique d'événement (`EventFingerprint`) et
l'implémentation Room du journal technique (`RoomTechnicalEventLog`), filtrée par une liste
blanche de clés plutôt que par la discipline de chaque appelant.

54 tests JVM verts dans `:core:database` (nouveaux à cette étape), 160 dans `:shared:core`
(non-régression après un changement de `:shared:core`), **19 tests instrumentés verts sur appareil
réel** (Xiaomi 25080RABDG, Android 16 / API 36), ktlint/detekt/lint verts sur tout le dépôt,
`:app:assembleDebug` vert (le graphe Hilt se ferme).

Le premier passage sur appareil a fait échouer 6 tests sur 17 et révélé un défaut réel du code de
production (ordre d'écriture du journal non garanti) plus trois hypothèses de test fausses ; tout
est corrigé et re-vérifié — voir « Ce que les tests sur appareil ont révélé ».

## Décisions validées avec l'utilisateur (2026-09-09)

1. **`TechnicalEventLog.log()` passe de `packageName` à `detailsJson`.** Alternative envisagée :
   garder `packageName` (interface inchangée, mais bloque le « code d'erreur contrôlé » que §17
   autorise aussi pour les autres types). Retenue : `detailsJson`, avec le garde-fou §16/§17 déplacé
   dans l'implémentation — une liste blanche de clés (`TechnicalEventDetails.sanitize`) plutôt que
   la seule discipline de l'appelant, pour que la règle vive en un point et survive même si un
   appelant construit un JSON incorrect. Un seul appelant de production concerné
   (`NiumiBlockingAccessibilityService.kt`).
2. **`log()` reste synchrone, `recent()` devient `suspend`.** `log()` est appelé depuis
   `AlarmReceiver.onReceive`, `AlarmRingingService.onStartCommand` et
   `NiumiBlockingAccessibilityService.onAccessibilityEvent`, tous sur le thread principal, où Room
   interdit toute requête. `recent()` n'avait aucun appelant de production.
3. **`@SerialName("incident")` ajouté à `IncidentEffectPayloadDto` dans `:shared:core`.** Sans lui,
   kotlinx-serialization écrit le nom de classe qualifié comme discriminant du payload polymorphe
   persisté en base v1 ; un renommage de package rendrait illisibles les effets pendants d'une
   session survivant à une mise à jour de l'application (SPEC_ANDROID §9.3,
   `MY_PACKAGE_REPLACED`). Vérifié : aucun test existant n'épinglait ce JSON avant le changement.

## Écarts au plan d'origine

4. **`pendingEffects()`/`activeSession()` rejouent `PENDING` *et* `FAILED`, pas `PENDING` seul.**
   Le plan (ligne 479 avant correction) ne demandait que `PENDING`. Un effet requis (ex.
   `REMOVE_BLOCKING`) resté `FAILED` après une interruption ne serait alors jamais rejoué au
   redémarrage, contre SPEC_CORE_KMP §6.1 (« les effets interrompus sont remis en attente et
   rejoués au redémarrage »). `SUCCEEDED` et `SATISFIED` restent exclus (états terminaux).
5. **`commitDecision` utilise `database.withTransaction {}` (room-ktx), pas `@Transaction` sur un
   DAO.** Un `@Transaction` Room ne peut appeler que des méthodes de son propre DAO ; la décision
   touche cinq tables (session, apps, pointeur, reçu, outbox), ce qui aurait imposé un DAO obèse
   pour les regrouper.
6. **8 DAO au lieu des 6 du plan** (`SessionDao`, `BlockedAppDao`, `ActiveSessionPointerDao`,
   `ReceiptDao`, `OutboxDao`, `IncidentDao`, `PairedBoxDao`, `TechnicalEventDao`), un par table.
   Motif : `TooManyFunctions` de detekt (11) — les convertisseurs d'enum sont de même répartis sur
   trois fichiers (`SessionEnumConverters`, `EffectEnumConverters`, `IncidentEnumConverters`) plutôt
   qu'un seul `EnumConverters.kt`, sept enums × deux fonctions dépassant le seuil.
7. **Ajout au périmètre : `mapping/SessionEffectMapper.kt`.** Non listé au plan, mais nécessaire :
   `SessionEffectDto` ne porte pas d'`ordinal` (c'est l'index dans `SessionDecisionDto.effects`,
   affecté par `SessionEffectBuilder` dans `:shared:core`) et rien d'autre ne fabriquait
   `PendingEffect` ni ne sérialisait `payloadJson`. Sans ce mapper, l'étape 11 l'aurait réinventé.
8. **`TechnicalEventType` complété** : `ALARM_MUTED_BY_DND` et `SESSION_READINESS_DEGRADED`
   manquaient depuis l'étape 3, alors que SPEC_ANDROID §17 les liste et §13.1 les exige nommément.
   24 → 26 valeurs.
9. **`RoomTechnicalEventLog` n'est pas branché dans `LoggingModule`.** `LoggingModule` continue de
   fournir `InMemoryTechnicalEventLog`. Motif : `AlarmReceiver`, `AlarmRingingService` et
   `AlarmActivity` sont `directBootAware`, et SPEC_ANDROID §7.3 interdit explicitement qu'un tel
   composant « crée Room ou un dépôt qui ouvre Room » avant `UserManager.isUserUnlocked == true` —
   la garde correspondante (`ROOM_BEFORE_UNLOCK`) est prévue à l'étape 10. Câbler `RoomTechnicalEventLog`
   dans `LoggingModule` dès maintenant ferait résoudre par Hilt, à l'injection de champ de ces
   composants, un `NiumiDatabase` avant cette garde. `RoomTechnicalEventLog` est entièrement écrit et
   testé, prêt à être branché dès que l'étape 10 livre le garde-fou. `DatabaseModule`, `DaoModule` et
   `SessionStoreModule` sont en revanche câblés (aucun consommateur `directBootAware` à cette étape :
   `SessionStore` n'est injecté nulle part avant l'étape 11).
10. **Test « type inconnu refusé » remplacé.** Demandé par le plan pour `TechnicalEventType`,
    irréalisable : c'est un `enum` fermé, aucune valeur hors liste n'est représentable sans
    introduire une API `String` libre — précisément ce que la liste blanche doit éviter. Substitut :
    `TechnicalEventTypeTest` vérifie que `entries` contient exactement les 26 libellés de §17.
11. **`MigrationTestHelper` confirmée `androidx.room.testing`** (et non `androidx.room3.testing`,
    package d'une préversion KMP de Room 3.0 que la documentation en ligne mélange parfois) —
    vérifié en décompilant le jar `room-testing-android-2.8.4` réellement résolu par Gradle.
    `NiumiDatabaseSchemaTest` (une méthode, `createDatabase(1)` puis
    `runMigrationsAndValidate(1, emptyList())`) n'apporte que peu de valeur propre en v1 (aucune
    migration à exercer) : `ExportedSchemaTest` (JVM, sans appareil) fait le contrôle principal.

## Ce que les tests sur appareil ont révélé (2026-09-09, Xiaomi 25080RABDG, Android 16)

Premier passage : 17 tests, 11 verts, 6 rouges. Les quatre causes, et ce qui a été corrigé.

12. **Défaut de production — l'ordre d'écriture du journal n'était pas garanti.** `log()` lançant
    une coroutine indépendante par appel, deux écritures concurrentes pouvaient atteindre la base
    dans un ordre différent de celui des appels. Or la purge « les 200 derniers » reposait sur
    l'ordre des `id` auto-incrémentés : un événement récent pouvait être supprimé à la place d'un
    ancien, et la chronologie affichée par le futur écran de diagnostic aurait menti.
    Corrigé sur deux plans : un `Mutex` sérialise les écritures dans `RoomTechnicalEventLog`, et
    `TechnicalEventDao` trie désormais par `createdAtEpochMillis DESC, id DESC` (lecture *et*
    purge) — l'horodatage étant capturé au moment de l'appel, la chronologie reste juste même si
    une insertion arrivait dans le désordre. Un test dédié
    (`writesKeepTheOrderOfTheCallsEvenThoughLoggingIsFireAndForget`) verrouille la garantie.
13. **Hypothèse de test fausse — `TestScope` n'exécute pas les `launch` immédiatement.** `runTest`
    utilise un `StandardTestDispatcher` : les coroutines lancées sur son scope sont mises en file
    et n'avancent qu'à une suspension du test ; une seule des 201 écritures avait abouti. De plus,
    une écriture Room suspend sur des threads que le planificateur de test ne contrôle pas, donc le
    temps virtuel n'aurait rien résolu. `RoomTechnicalEventLogTest` injecte désormais un vrai
    `CoroutineScope` et attend explicitement ses enfants (`job.children.joinAll()`).
14. **Un pointeur orphelin est impossible par construction.** Le test qui insérait un
    `ActiveSessionPointerEntity` vers une session absente échouait sur `FOREIGN KEY constraint
    failed` : la clé étrangère refuse cet état, et `onDelete = CASCADE` emporte le pointeur avec sa
    session. La branche défensive de `activeSession()` est donc inatteignable — elle est conservée
    (§13 interdit d'effacer silencieusement un état incohérent), et le test
    (`aPointerCanNeverReferenceAMissingSession`) documente désormais l'invariant réel.
15. **Un package bloqué en double ne fait pas échouer la transaction.** `BlockedAppDao.insertAll`
    étant en `REPLACE`, un doublon est absorbé (dernier gagnant) au lieu de violer la clé primaire
    composée : mon second « point d'échec » n'en était pas un. Le test le documente désormais
    (`duplicateBlockedPackageIsCollapsedByLastWriteWins` — utile au coordinateur de l'étape 11 : un
    doublon ne casse pas une activation), et un second point d'échec **authentique** l'a remplacé
    (`outboxForeignKeyViolationRollsBackTheEntireTransaction`) : un effet référençant une session
    inconnue fait échouer la toute dernière écriture de la transaction, ce qui prouve que la
    session, ses applications, le pointeur **et le reçu** sont tous annulés — un cran plus loin que
    le test du conflit d'`eventId`.
16. **`MigrationTestHelper` : incompatibilité de constructeur confirmée à l'exécution.** Le
    constructeur `(Instrumentation, Class)` configure un `SupportSQLiteOpenHelper` ; les méthodes
    modernes rendant un `SQLiteConnection` lèvent alors `IllegalStateException`. Le test utilise
    l'API `SupportSQLiteDatabase` correspondante (`createDatabase(name, version)` /
    `runMigrationsAndValidate(name, version, validateDroppedTables)`). C'était l'une des trois
    incertitudes listées au plan ; elle est levée.

## Points de vigilance non arbitrés (à surveiller aux étapes suivantes)

- **Nommage du hash de token** : SPEC_ANDROID §7.2 écrit `boxTokenSha256Hex` pour la session mais
  `tokenSha256` pour `PairedBoxEntity` ; §7.3 (snapshot Direct Boot) et SPEC_CORE_KMP §9.2 utilisent
  encore d'autres variantes. Les noms de §7.2 sont repris tels quels ; à harmoniser explicitement à
  l'étape 10 (mapper de projection Direct Boot).
- **`RoomPairedBoxStore` (étape 13)** ne pourra pas vivre dans `:core:database` si l'interface
  `PairedBoxStore` reste dans `:core:system` (`:core:system → :core:database`, jamais l'inverse).
  Cette étape ne livre que l'entité et `PairedBoxDao`.
- **Contradiction interne de SPEC_ANDROID §17** : la prose exige que chaque événement journalisé
  porte le modèle de l'appareil, la version Android et la version de l'application, alors que
  `TechnicalEventEntity` (§7.2) n'a que 5 colonnes, aucune pour ces métadonnées. Suivi ici : §7.2
  fait foi pour le stockage (répéter 3 constantes sur 200 lignes serait une redondance pure) ; ces
  métadonnées auraient plutôt leur place dans l'en-tête de l'export de diagnostic (étape 18). Non
  tranché avec l'utilisateur, aucune spec modifiée — à soulever explicitement le moment venu.
- **§19.1 range le test de mapping Room ↔ DTO sous `core:system`**, alors que le code (et le test)
  vivent dans `:core:database`, cohérent avec §6 (« `:core:database` : Room, DAO, DataStore et
  stockage Direct Boot »). Divergence mentionnée, aucune spec modifiée.
- **`blockedPackages` n'est pas figé comme les quatre autres champs d'`AndroidSessionExtras`.**
  `RoomSessionStore.freezeFrom` protège `boxId`, `boxTokenSha256Hex`, `ringtoneKey` et
  `vibrationEnabled` contre un appelant qui fournirait des extras différents sur une décision
  ultérieure (testé), mais pas la sélection d'applications elle-même. Non couvert par le texte de
  §7.2 pour cette colonne ; à trancher si un besoin apparaît.

## Ce qui a été construit

**`androidApp/core/database/src/main/kotlin/com/niumi/database/`** :
- `NiumiDatabase.kt` (`version = 1`, `exportSchema = true`), `AndroidSessionExtras.kt`,
  `EventReceipt.kt`, `PendingEffect.kt` (+ `EffectStatus`), `SessionStore.kt` (+ `StoredSession`,
  `StoredDecision`), `RoomSessionStore.kt`.
- `entity/` : `AlarmSessionEntity`, `BlockedAppEntity`, `PairedBoxEntity`, `TechnicalEventEntity`,
  `SessionIncidentEntity`, `SessionEventReceiptEntity`, `SessionEffectOutboxEntity`,
  `ActiveSessionPointerEntity` — colonnes exactes de SPEC_ANDROID §7.2, index sur chaque clé
  étrangère.
- `converter/` : `SessionEnumConverters`, `EffectEnumConverters`, `IncidentEnumConverters` — un
  seul convertisseur par enum (les `*Dto` de `:shared:core/interop` sont des `typealias` effacés à
  la compilation), stockage par `name`, jamais par `ordinal`.
- `dao/` : `SessionDao`, `BlockedAppDao`, `ActiveSessionPointerDao`, `ReceiptDao`, `OutboxDao`,
  `IncidentDao`, `PairedBoxDao`, `TechnicalEventDao`.
- `mapping/` : `SessionSnapshotMapper.kt`, `SessionEffectMapper.kt`, `EventFingerprint.kt`,
  `StoreEntityMappers.kt`.
- `logging/` : `RoomTechnicalEventLog.kt`, `TechnicalEventDetails.kt` ; `TechnicalEventLog.kt` et
  `InMemoryTechnicalEventLog.kt` modifiés (`detailsJson`, `recent()` suspend) ;
  `TechnicalEventType.kt` complété.
- `di/` : `DatabaseModule.kt`, `DaoModule.kt`, `SessionStoreModule.kt`.

**Tests JVM** (`src/test`) : `EnumConvertersTest`, `SessionSnapshotMapperTest` (+
`SessionSnapshotDtoFixtures`), `EventFingerprintTest`, `SessionEffectMapperTest`,
`TechnicalEventTypeTest`, `TechnicalEventDetailsTest`, `ExportedSchemaTest`,
`InMemoryTechnicalEventLogTest` (adapté).

**Tests instrumentés** (`src/androidTest`, 19 tests, exécutés et verts sur appareil réel) :
`RoomSessionStoreCommitTest` (7), `RoomSessionStoreEffectsTest` (5), `RoomTechnicalEventLogTest`
(4), `PairedBoxDaoTest` (2), `NiumiDatabaseSchemaTest` (1) (+ `RoomTestFixtures`).

**Hors module** : `shared/core/.../interop/SessionDtos.kt` (`@SerialName`),
`NiumiBlockingAccessibilityService.kt` (appel `log()` mis à jour),
`AlarmNfcScanCoordinatorTest.kt` (`FakeTechnicalEventLog` alignée), plan MVP (cases cochées,
correction de la ligne 299).

## Vérifications exécutées

```bash
./gradlew :core:database:testDebugUnitTest        # vert — 54 tests, 0 échec
./gradlew :shared:core:jvmTest                     # vert — 160 tests, 0 échec (non-régression @SerialName)
./gradlew :feature:ringing:testDebugUnitTest       # vert
./gradlew :feature:session:testDebugUnitTest       # vert
./gradlew :app:testDebugUnitTest                   # vert
./gradlew :app:assembleDebug                       # vert — le graphe Hilt se ferme
./gradlew :core:database:compileDebugAndroidTestKotlin   # vert
./gradlew :core:database:assembleDebugAndroidTest  # vert — schéma v1 confirmé dans les assets fusionnés
./gradlew ktlintCheck detekt :app:lintDebug         # vert, dépôt entier
./gradlew :core:database:connectedDebugAndroidTest  # vert — 19 tests, 0 échec (appareil réel)
```

Appareil de validation : **Xiaomi 25080RABDG, Android 16 (API 36)**, build `BP2A.250605.031.A3`,
branché en USB (`adb devices` vérifié avant lancement).

Contrôles manuels :
- `find androidApp/core/database/schemas` : `com.niumi.database.NiumiDatabase/1.json` présent,
  8 entités, `version = 1`, `identityHash` non vide (vérifié par `ExportedSchemaTest`) ;
- décompilation de `room-testing-android-2.8.4.aar` (`javap`) pour confirmer le package et les
  signatures réelles de `MigrationTestHelper` avant d'écrire le test dépendant ;
- `grep` : aucun appel `Dispatchers.*` hors du point d'injection déjà existant dans `:core:system`
  (`:core:database` n'introduit aucun nouveau point d'injection de dispatcher : `RoomTechnicalEventLog`
  reçoit son `CoroutineScope` par constructeur, jamais construit lui-même).

## Validations sur appareil réel — faites

Exécutées le 2026-09-09 sur Xiaomi 25080RABDG (Android 16, API 36) : **19 tests, 0 échec**.

- `RoomSessionStoreCommitTest` (7) : les cinq tables sont écrites ensemble et relues à l'identique ;
  **deux points d'échec distincts** annulent toute la transaction (conflit d'`eventId`, et violation
  de clé étrangère sur la dernière écriture — l'outbox — ce qui prouve aussi l'annulation du reçu) ;
  les quatre champs figés d'`AndroidSessionExtras` survivent à une décision ultérieure fournissant
  des valeurs différentes ; un pointeur ne peut jamais désigner une session absente ;
  `clearActivePointer` est idempotent et ne touche pas une autre session ; un package en double est
  absorbé (dernier gagnant) sans faire échouer l'activation.
- `RoomSessionStoreEffectsTest` (5) : tri `revision ASC, ordinal ASC`, `PENDING`+`FAILED` rejoués,
  `SUCCEEDED`/`SATISFIED` exclus, `markEffect` met à jour statut et erreur, `markEffect` sur un
  `effectId` inconnu ne lève rien, `findReceipt` inconnu rend `null`.
- `RoomTechnicalEventLogTest` (4) : 201 insertions → 200 lignes, la plus ancienne supprimée ;
  `detailsJson` filtré selon le type ; `sessionId` inconnu accepté (pas de clé étrangère) ; **ordre
  des écritures conforme à l'ordre des appels** malgré le fire-and-forget.
- `NiumiDatabaseSchemaTest` (1) : le schéma v1 exporté est lu depuis les assets du module de test et
  se valide.
- `PairedBoxDaoTest` (2) : aller-retour et remplacement par `boxId`.

Aucun protocole manuel (alarme, NFC, blocage visible) n'était requis : cette étape ne branche aucun
composant système, conformément à son périmètre.

**Reste non validé sur appareil** (hors périmètre de l'étape, à couvrir plus tard) : le
comportement de `RoomTechnicalEventLog` et de `RoomSessionStore` **avant le premier déverrouillage**
(Direct Boot), puisque ni l'un ni l'autre n'est encore branché en production — c'est l'objet de la
garde `ROOM_BEFORE_UNLOCK` de l'étape 10.

## Fichiers modifiés ou créés

- `androidApp/core/database/src/main/kotlin/com/niumi/database/**` (voir « Ce qui a été construit »)
- `androidApp/core/database/src/test/kotlin/com/niumi/database/**` (8 fichiers, nouveaux)
- `androidApp/core/database/src/androidTest/kotlin/com/niumi/database/**` (6 fichiers, nouveaux)
- `androidApp/core/database/build.gradle.kts` (dépendances `kotlinx-serialization-json`,
  `truth`/`kotlinx-coroutines-test`/`room-testing` en androidTest, assets `androidTest` vers
  `schemas/`, `systemProperty("niumi.rootDir", …)`)
- `androidApp/core/database/schemas/com.niumi.database.NiumiDatabase/1.json` (généré, committé)
- `gradle/libs.versions.toml` (`room-testing`)
- `shared/core/src/commonMain/kotlin/com/niumi/core/interop/SessionDtos.kt` (`@SerialName`)
- `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking/NiumiBlockingAccessibilityService.kt`
- `androidApp/feature/ringing/src/test/kotlin/com/niumi/feature/ringing/AlarmNfcScanCoordinatorTest.kt`
- `docs/superpowers/plans/2026-09-03-mvp-android.md` (cases de l'étape 9, correction ligne 299)
