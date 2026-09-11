# Étape 10 — Snapshot Direct Boot

Date : 2026-09-10. Projection partielle de Room dans le stockage protégé de l'appareil
(SPEC_ANDROID §7.3, SPEC_CORE_KMP §13), plus la garde `ROOM_BEFORE_UNLOCK` annoncée mais reportée
à l'étape 9 (`ETAPE-09.md`, écart 9). Aucun composant système n'est encore branché sur
`DirectBootStore` : le `SessionCoordinator` qui le consommera arrive à l'étape 11.

## Résumé

Livre `DirectBootSnapshot` (format JSON persisté, types de projection dédiés indépendants des DTO
KMP et des types Room), `DirectBootMapper` (aller-retour vers `SessionSnapshotDto`,
`AndroidSessionExtras`, `EventReceipt`, `PendingEffect`), `FileDirectBootStore`
(`android.util.AtomicFile` sous `createDeviceProtectedStorageContext()`), `UnlockState`
(`UserManager.isUserUnlocked`), la garde `ROOM_BEFORE_UNLOCK` sur `RoomSessionStore`, et
`UnlockAwareTechnicalEventLog` qui bascule `TechnicalEventLog` entre mémoire et Room selon
l'état de déverrouillage — branchant enfin `RoomTechnicalEventLog`, écrit mais jamais utilisé
depuis l'étape 9.

126 tests JVM verts dans `:core:database` (90, dont 36 nouveaux à cette étape) et non-régression
sur `:shared:core` (160), `:core:system` (56), `:feature:ringing` (21), `:feature:session` (1),
`:app` (6) ; **29 tests instrumentés verts sur appareil réel** (Xiaomi 25080RABDG, Android 16 /
API 36, build `BP2A.250605.031.A3`), dont les 10 nouveaux de `FileDirectBootStoreTest`.
`ktlintCheck`, `detekt` et `:app:lintDebug` verts sur tout le dépôt, `:app:assembleDebug` vert (le
graphe Hilt se ferme avec les nouveaux modules).

## Décisions validées avec l'utilisateur (2026-09-10)

1. **`blockedPackages` avec libellé, pas `blockedPackageNames`.** SPEC_ANDROID §7.3 ne listait que
   les noms de packages ; le snapshot stocke les paires `(packageName, displayNameSnapshot)`, le
   libellé figé à l'activation étant requis par le texte imposé de l'overlay (§12.2). **§7.3 mis à
   jour.**
2. **`boxTokenSha256Hex`, pas `tokenSha256`.** Harmonisation renvoyée à cette étape par
   `ETAPE-09.md` (§7.2 écrit `boxTokenSha256Hex` pour la session, `tokenSha256` pour
   `PairedBoxEntity` ; §7.3 écrivait encore `tokenSha256`). Le JSON Direct Boot est un format neuf :
   il adopte le nom canonique de §7.2. `PairedBoxEntity.tokenSha256` reste inchangé (colonne Room v1
   déjà exportée). **§7.3 mis à jour** ; la divergence entre les deux entités Room reste documentée
   dans `ETAPE-09.md`, non résolue (renommer une colonne Room exportée pour un gain cosmétique n'a
   pas été jugé utile).
3. **Le journal technique Room est branché dans cette étape**, derrière la garde, via
   `UnlockAwareTechnicalEventLog` : sans cela, `RoomTechnicalEventLog` restait du code mort depuis
   l'étape 9 et l'écran de diagnostic de l'étape 16 aurait hérité d'un journal toujours vide.

## Écarts au plan MVP

4. **`DirectBootStore.write()` ne peut pas retourner `OperationResult`.** Les « Interfaces
   transverses » du plan MVP placent `DirectBootStore` dans `:core:database` en lui faisant
   retourner `OperationResult`, qui vit dans `:core:system`. Or `:core:system → :core:database`,
   jamais l'inverse (règle de dépendance SPEC_ANDROID §6) : le contrat tel qu'écrit ne compile pas.
   Remplacé par `DirectBootWriteResult` (scellé, local à `:core:database` :
   `Written` / `StaleRevision` / `Failed(reason)`). Le plan MVP est corrigé.
5. **`write()` prend `DirectBootSnapshot.Active`, pas `DirectBootSnapshot`.** Écrire une variante
   `Corrupted` n'a pas de sens ; le type l'interdit plutôt que la documentation en prose.
6. **La garde de révision est scopée par `sessionId`.** `ActivationReducer` repart à `revision = 1`
   pour chaque nouvelle session (`shared/core/.../domain/ActivationReducer.kt:23`) : refuser toute
   révision inférieure sans distinguer la session rendrait impossible l'écriture de la session
   suivante après la fin de la précédente. « Jamais avec une révision inférieure »
   (SPEC_CORE_KMP §13) s'entend donc *pour une même session* ; le texte reste vrai tel quel, aucune
   spec modifiée, mais le point est documenté explicitement dans le code (`DirectBootStore.kt`) et
   testé (`DirectBootWriteDecisionTest`, `FileDirectBootStoreTest`).

## Ce que les tests ont révélé

7. **`encodeDefaults = false` (par défaut de `Json`) aurait omis `projectionSchemaVersion` du
   fichier persisté.** Ce champ a une valeur par défaut (`DIRECT_BOOT_PROJECTION_SCHEMA_VERSION`),
   et le comportement standard de kotlinx-serialization omet silencieusement un champ égal à sa
   valeur par défaut. Pour un format destiné à distinguer un futur changement de version, un champ
   de version absent est pire qu'inutile — un ancien fichier sans le champ redeviendrait
   indiscernable d'un nouveau fichier à la version actuelle par coïncidence. Détecté par
   `DirectBootSnapshotJsonTest` (assertion sur l'ensemble exact des clés du JSON encodé), corrigé en
   centralisant la configuration `Json` du format persisté (`directBootJson`,
   `encodeDefaults = true`), plutôt que de laisser chaque appelant construire son propre `Json`
   local — même motif que `SessionEffectMapper.persistenceJson` et `EventFingerprint` à l'étape 9.
8. **`InMemoryTechnicalEventLog.recent()` et `RoomTechnicalEventLog.recent()` ont des ordres
   internes différents**, sans contrat documenté à ce jour faute d'appelant de production : le
   premier rend l'ordre d'insertion (croissant), le second `ORDER BY createdAtEpochMillis DESC, id
   DESC` (décroissant). `UnlockAwareTechnicalEventLog.recent()` impose désormais un ordre explicite
   (le plus récent en premier) en fusionnant et triant les deux sources, plutôt que d'en hériter un
   par accident. Ni `TechnicalEventLog.recent()` ni les deux implémentations existantes n'ont été
   modifiées : la divergence est documentée dans `UnlockAwareTechnicalEventLog.kt`, à trancher
   explicitement si un futur appelant a besoin d'un contrat d'ordre sur les deux implémentations
   elles-mêmes.
9. **`LoggingModule` en `abstract class` échouait `AbstractClassCanBeInterface` de detekt** (un seul
   membre abstrait `@Binds`, aucun membre concret hors du companion). Corrigé en `interface` +
   `companion object` pour les `@Provides` — Dagger/Hilt accepte cette forme aussi bien qu'une classe
   abstraite pour un module mixte `@Binds`/`@Provides`.
10. **`Dispatchers.IO` dans les nouveaux tests (`UnlockAwareTechnicalEventLogTest`) déclenche
    `InjectDispatcher` de detekt**, comme dans `RoomTechnicalEventLogTest` à l'étape 9 : le test est
    ici le fournisseur légitime du scope de l'objet sous test. `@Suppress("InjectDispatcher")`
    ajouté avec le même commentaire que la convention existante.
11. **`@param:ApplicationContext` est redondant sur un paramètre de constructeur qui n'est pas une
    propriété** (`FileDirectBootStore`) : averti par le compilateur Kotlin lui-même
    (`Redundant annotation target 'param'`), corrigé en `@ApplicationContext` simple. Le motif reste
    nécessaire quand le paramètre est aussi une propriété (`UnlockState`,
    `DebugPairedBoxStore` existant) : ce n'est pas une incohérence de style, mais deux cas
    syntaxiquement différents.
12. **`RoomTechnicalEventLog` et `InMemoryTechnicalEventLog` gardent un paramètre par défaut
    (`nowEpochMillis`)**, ce qui empêche un constructeur `@Inject` (Dagger ne respecte pas les
    valeurs par défaut Kotlin, déjà noté à l'étape 9) : les deux restent fournies par `@Provides`
    dans `LoggingModule`, jamais construites ailleurs. `UnlockAwareTechnicalEventLog`, sans
    paramètre par défaut, a pu garder un constructeur `@Inject` classique.

## Points de vigilance non arbitrés

- **Comportement réel avant le premier déverrouillage non prouvé de bout en bout.** Cette étape
  prouve l'écriture, la lecture, l'atomicité et la garde de révision de `FileDirectBootStore` sur un
  appareil réel, mais pas le parcours Direct Boot complet
  (redémarrage avec verrou d'écran actif, alarme reprogrammée avant déverrouillage) : ce parcours n'a
  de consommateur `directBootAware` qu'à partir de l'étape 11 (`SessionCoordinator`,
  `SessionReconciler`). Prévu par le plan MVP lui-même (étape 10 : « aucun composant système »).
- **Fusion de `UnlockAwareTechnicalEventLog.recent()` non bornée par source avant fusion.** La
  fusion prend l'intégralité de `inMemory.recent()` (borné à 200 par construction) et de
  `roomLog.get().recent()` (borné à 200 par la requête SQL), trie les 400 entrées possibles puis
  coupe à 200 : correct, mais légèrement plus coûteux qu'une fusion à la lecture pré-triée. Aucun
  appelant de production à cette étape (écran de diagnostic à l'étape 16) ; à revisiter si le volume
  devient sensible.

## Ce qui a été construit

**`androidApp/core/database/src/main/kotlin/com/niumi/database/directboot/`** (nouveau) :
- `DirectBootSnapshot.kt` (`Active`, `Corrupted`, `DirectBootBlockedPackage`, `DirectBootReceipt`,
  `DirectBootEffect`), `DirectBootJson.kt` (`directBootJson`, `encodeDefaults = true`),
  `DirectBootMapper.kt` (`projectionOf` + extensions `toSnapshotDto`/`toExtras`/`toReceipts`/
  `toPendingEffects`), `DirectBootStore.kt` (interface, `DirectBootWriteResult`, `decideWrite`
  extrait pour être testable en JVM), `FileDirectBootStore.kt` (`AtomicFile`), `UnlockState.kt`
  (`UnlockState`, `UserManagerUnlockState`), `di/DirectBootModule.kt`.

**`androidApp/core/database/src/main/kotlin/com/niumi/database/logging/`** (modifié/nouveau) :
- `UnlockAwareTechnicalEventLog.kt` (nouveau), `di/TechnicalEventLogScope.kt` (nouveau, qualifier),
  `di/LoggingModule.kt` (réécrit : `interface` + `companion object`, fournit
  `InMemoryTechnicalEventLog`, `RoomTechnicalEventLog`, le scope qualifié, et lie
  `UnlockAwareTechnicalEventLog` comme `TechnicalEventLog`).

**Modifié** : `RoomSessionStore.kt` (`Provider<NiumiDatabase>` + `UnlockState`, garde
`ROOM_BEFORE_UNLOCK`), `di/SessionStoreModule.kt` (fournit le provider et l'état de
déverrouillage), `build.gradle.kts` (`alias(libs.plugins.kotlin.serialization)`).

**Tests JVM** (`src/test`, nouveaux) : `DirectBootSnapshotJsonTest`, `DirectBootMapperTest`,
`DirectBootReduceTest`, `DirectBootRoomParityTest`, `DirectBootWriteDecisionTest`,
`RoomSessionStoreUnlockGuardTest`, `UnlockAwareTechnicalEventLogTest`.

**Tests instrumentés** (`src/androidTest`, nouveaux) : `directboot/FileDirectBootStoreTest` (10
tests) ; `RoomSessionStoreCommitTest`/`RoomSessionStoreEffectsTest` adaptés à la nouvelle
signature ; `RoomTestFixtures.kt` complété (`AlwaysUnlockedState`).

## Vérifications exécutées

```bash
./gradlew :core:database:testDebugUnitTest        # vert — 90 tests, 0 échec (36 nouveaux)
./gradlew :shared:core:jvmTest                     # vert — 160 tests (non-régression)
./gradlew :core:system:testDebugUnitTest           # vert — 56 tests (non-régression)
./gradlew :feature:ringing:testDebugUnitTest       # vert — 21 tests (non-régression)
./gradlew :feature:session:testDebugUnitTest       # vert — 1 test (non-régression)
./gradlew :app:testDebugUnitTest                   # vert — 6 tests (non-régression)
./gradlew :app:assembleDebug                       # vert — le graphe Hilt se ferme
./gradlew :core:database:compileDebugAndroidTestKotlin   # vert
./gradlew :core:database:assembleDebugAndroidTest  # vert — 29 tests instrumentés au total
./gradlew ktlintCheck detekt :app:lintDebug         # vert, dépôt entier
./gradlew :core:database:connectedDebugAndroidTest  # vert — 29 tests, 0 échec (appareil réel)
```

Appareil de validation : **Xiaomi 25080RABDG, Android 16 (API 36)**, build `BP2A.250605.031.A3`,
branché en USB (`adb devices` vérifié avant lancement).

## Validation sur appareil réel — faites

Exécutées le 2026-09-10 sur Xiaomi 25080RABDG (Android 16, API 36) : **29 tests, 0 échec**
(3,73 s), dont les 10 nouveaux de `FileDirectBootStoreTest` : écriture puis lecture d'un snapshot
sous stockage protégé par l'appareil, emplacement du fichier, refus d'une révision inférieure sur
la même session, idempotence à révision égale, acceptation d'une révision inférieure sur une autre
session, tolérance à un fichier corrompu (sans l'effacer), écrasement d'un fichier corrompu par une
écriture valide, `clear()`, absence de fichier temporaire résiduel après une écriture réussie. Les
19 tests hérités de l'étape 9 (`RoomSessionStoreCommitTest`, `RoomSessionStoreEffectsTest`,
`RoomTechnicalEventLogTest`, `PairedBoxDaoTest`, `NiumiDatabaseSchemaTest`) restent verts avec la
nouvelle signature de `RoomSessionStore`.

Aucun autre protocole manuel (alarme, NFC, blocage visible) n'était requis : cette étape ne branche
aucun composant système, conformément à son périmètre (le `SessionCoordinator` de l'étape 11 sera
le premier consommateur réel de `DirectBootStore`).

## Fichiers modifiés ou créés

- `androidApp/core/database/src/main/kotlin/com/niumi/database/directboot/**` (nouveau, voir
  « Ce qui a été construit »)
- `androidApp/core/database/src/main/kotlin/com/niumi/database/logging/UnlockAwareTechnicalEventLog.kt`
  (nouveau), `logging/di/TechnicalEventLogScope.kt` (nouveau), `logging/di/LoggingModule.kt` (réécrit)
- `androidApp/core/database/src/main/kotlin/com/niumi/database/RoomSessionStore.kt`,
  `di/SessionStoreModule.kt` (modifiés)
- `androidApp/core/database/build.gradle.kts` (`kotlin.serialization`)
- `androidApp/core/database/src/test/kotlin/com/niumi/database/**` (7 fichiers nouveaux, voir
  « Ce qui a été construit »)
- `androidApp/core/database/src/androidTest/kotlin/com/niumi/database/directboot/FileDirectBootStoreTest.kt`
  (nouveau), `RoomTestFixtures.kt`, `RoomSessionStoreCommitTest.kt`, `RoomSessionStoreEffectsTest.kt`
  (modifiés)
- `specs/SPEC_ANDROID.md` §7.3 (`boxTokenSha256Hex`, `blockedPackages`, garde de révision scopée
  par session)
- `docs/superpowers/plans/2026-09-03-mvp-android.md` (cases de l'étape 10, correction de
  `DirectBootStore` dans « Interfaces transverses »)
