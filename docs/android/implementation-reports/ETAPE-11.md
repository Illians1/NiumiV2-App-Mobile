# Étape 11 — `SessionCoordinator`, registre idempotent, outbox, exécution des effets et réconciliateur

Date : 2026-09-10. Livre le composant central de toute la suite du plan : le coordinateur qui
convertit un fait système en événement KMP, persiste atomiquement (snapshot, reçu, effets) avant
d'exécuter le moindre effet, exécute les effets dans l'ordre canonique de SPEC_CORE_KMP §6, referme
la phase par `ACTIVATION_SUCCEEDED`/`FAILED` ou `RELEASE_SUCCEEDED`/`FAILED`, et reprend un état
incomplet via `SessionReconciler`. Les étapes 9 et 10 avaient livré la persistance (Room, Direct
Boot) sans consommateur ; cette étape la branche.

## Résumé

Livre `SessionCoordinator`/`DefaultSessionCoordinator` (registre idempotent, `Mutex` unique
partagé avec `reconcile`), `SessionPersistenceGateway`/`UnlockAwarePersistenceGateway` (Room après
déverrouillage, Direct Boot avant, miroir après chaque écriture Room), `EffectDispatcher` et ses
onze exécuteurs, `PhaseCompletion`, `SessionReconciler`/`SessionRuntimeStatusProbe`,
`SessionSnapshotPublisher`, `ScanRequestNotifier`/`AndroidScanRequestNotifier` (notification
d'attente de scan, SPEC_ANDROID §10.5).

91 tests JVM `:core:system` verts (35 nouveaux à cette étape, dans `session` et `notification`),
non-régression `:core:database` (90), `:shared:core` (160), `:feature:ringing` (21),
`:feature:session` (1), `:app` (6). `:app:assembleDebug`, ktlint, detekt et `:app:lintDebug` verts
sur tout le dépôt. Trois tests instrumentés compilent mais n'ont pas pu être exécutés sur appareil
(voir « Validations sur appareil réel restantes »).

## Décisions validées avec l'utilisateur (2026-09-10)

1. **`STOP_RINGING` requis pour `RELEASE_SUCCEEDED`.** SPEC_CORE_KMP §6 le classait requis,
   SPEC_ANDROID §11.3 best-effort — contradiction directe entre les deux specs. Après analyse du
   scénario concret (scan valide, alarme et blocage levés avec succès, mais l'arrêt du son échoue
   pendant qu'il sonne encore) : en best-effort, la session serait déclarée « terminée » pendant
   que le réveil continue de sonner, sans aucun moyen de rejouer l'arrêt — le pire résultat
   possible pour un produit de réveil. En requis, `RELEASE_FAILED`/`RELEASE_PARTIAL_FAILURE`
   maintient `RELEASING` et l'outbox rejoue `STOP_RINGING` jusqu'à réussite ; la règle
   d'échappement (`AlreadySatisfied` quand le moteur audio ne joue déjà plus) évite tout blocage
   indu. **SPEC_ANDROID §11.3 corrigée dans ce changement** (le blocage et l'alarme étaient déjà
   requis dans les deux textes, seul le son différait).
2. **`failureCode = "ANDROID_ALARM_SCHEDULE_FAILED"`**, pas `"ALARM_SCHEDULE_FAILED"` (plan MVP
   ligne 537 originale) : convention de préfixe d'`IncidentCodes` (SPEC_CORE_KMP §14, « chaque
   plateforme peut ajouter des codes préfixés `ANDROID_`/`IOS_` »), déjà utilisée par les fixtures
   `:shared:core` (`SessionFixtures`, `FixturesTest`) et `RoomSessionStoreEffectsTest`. Le moteur
   ne valide pas le contenu de `failureCode` (`String` libre) : le coordinateur se contente de
   transmettre le code renvoyé par le composant Android en échec (`AndroidAlarmScheduler` renvoie
   par exemple `"ANDROID_EXACT_ALARM_DENIED"`), jamais une valeur codée en dur.
3. **`SessionRuntimeStatus` livré avec ses six champs** (SPEC_ANDROID §7.1) dès cette étape, les
   trois sondes manquantes (`notificationReady`, `fullScreenReady`, `audioReady`) étant des
   sources injectables triviales (`NotificationAvailability`, `AlarmVolumeSource`) réutilisées
   telles quelles par le `DeviceReadinessChecker` de l'étape 12 plutôt que redéfinies.

## Extensions de contrats (répercutées dans le plan MVP, section « Interfaces transverses »)

| Ajout | Où | Pourquoi |
| --- | --- | --- |
| `SessionStore.receipts(sessionId)` + `ReceiptDao.forSession` | `:core:database` | Le snapshot Direct Boot porte `eventReceipts` (§7.3) ; aucun moyen de les relire avant cette étape. |
| `SessionStore.recordIncident(sessionId, incident)` | `:core:database` | `SessionIncidentEntity`/`IncidentDao` existaient depuis l'étape 9 sans écrivain. |
| `AlarmScheduler.canScheduleExact()` | `:core:system` | `SessionRuntimeStatusProbe` et l'incident `ALARM_PERMISSION_REVOKED` du réconciliateur (§13.1). Toujours `true` en dessous d'Android 12 (API 31), où la restriction n'existe pas. |
| `SessionCoordinator.dispatch(event, extras: AndroidSessionExtras? = null)` | `:core:system` | `StoredDecision` exige `androidExtras` (boîtier figé, sonnerie, sélection d'applications), absent de `SessionEventDto`. Requis uniquement pour `ACTIVATION_REQUESTED` ; sinon la session déjà persistée fait foi (`freezeFrom`). |
| `SessionReducer`, `LoadResult`, `ReconcileResult`/`ReconcileAction` | `:core:system.session` | Non définis par le plan MVP avant cette étape ; nécessaires pour espionner le réducteur sans dupliquer `reduce()`, distinguer un Direct Boot corrompu d'une absence de session, et faire assertions sur les décisions du réconciliateur. |

## Écarts au plan MVP

1. **`ANDROID_ALARM_SCHEDULE_FAILED`** — voir décision 2 ci-dessus.
2. **`STOP_RINGING` requis** — voir décision 1 ci-dessus ; la case « échec de `STOP_RINGING` seul →
   `RELEASE_SUCCEEDED` quand même » du plan original est remplacée par un test explicite du
   contraire.
3. **`build.gradle.kts` de `:core:system` non modifié.** Le plan envisageait de passer
   `:shared:core`/`:core:database` en `api` pour exposer `SessionEventDto`/`AndroidSessionExtras`
   aux `feature:*`. Inutile : chaque module `feature:*` déclare déjà sa propre dépendance directe
   `implementation` sur ces deux modules (établi dès l'étape 1), donc les types sont déjà visibles
   sans transitivité. Changement écarté, plus simple.
4. **`PocFacadeModule` supprimé.** `:app:hiltJavaCompileDebug` a révélé un `Dagger/DuplicateBindings`
   sur `NiumiCoreFacade` : la route POC (`androidApp/app/src/debug/.../poc/PocNfcModule.kt`) la
   fournissait déjà en `debug` uniquement, alors que `SessionModule` (`:core:system`, tous
   variants) doit désormais la fournir en production. `PocFacadeModule` retiré, la route POC
   consomme la même instance ; fichier renommé `PocNfcBindingsModule.kt` (ktlint `standard:filename`
   après le retrait de son second type). Aucune autre référence à `PocFacadeModule` dans le dépôt.
5. **`RoomSessionStore` et `UnlockAwarePersistenceGateway` au plafond detekt `TooManyFunctions`
   (11).** Les deux gagnent des méthodes à cette étape (`receipts`/`recordIncident`,
   `recordIncident`). Quatre fonctions privées (`toStoredSession`, `freezeFrom`,
   `replayableEffectsOf`) extraites en fonctions de fichier plutôt que membres de classe — même
   motif que le partage `DaoModule`/`SystemModule`/`AudioModule` des étapes précédentes.
6. **`DefaultSessionCoordinator.dispatchLocked` dépassait `CyclomaticComplexMethod` (24/14),
   `LongMethod` (106/60) et `ReturnCount` (7/2).** Scindé en `precheckLoadAndDuplicate`,
   `commitDecision`, `dispatchCollectedIncidents`, `completePhase`, `buildFollowUpEvent`.
   `dispatchLocked` et `precheckLoadAndDuplicate` restent des chaînes de clauses de garde
   séquentielles (`@Suppress("ReturnCount")`, même motif documenté que
   `NfcReducer.onValidScan`/`ReleaseReducer.onFailed` dans `:shared:core`, voir `ETAPE-07.md`).
7. **`SessionReconciler` dépassait `LongParameterList` (9/6).** `AlarmScheduler`,
   `AccessibilityServiceStatus` et `BlockedPackagesProjection` regroupés dans un porteur
   `ReconcilerSources` ; `Clock` retiré au profit de `SessionEventFactory.nowEpochMillis()` /
   `.buildIncident(code, severity)`, déjà détentrice de l'horloge. Constructeur final à 6
   paramètres, aucune suppression nécessaire.
8. **`config/detekt/detekt.yml` gagne une exemption `LongParameterList: ignoreAnnotated:
   ['Provides']`.** `EffectExecutorModule.provideEffectExecutors` (8 paramètres : un par type de
   dépendance distinct nécessaire pour construire la table des onze exécuteurs) et
   `SessionModule.provideSessionReconciler` la déclenchaient. Un `@Provides` Hilt n'a pas de
   « trop de paramètres » au sens métier — chaque paramètre est un type de dépendance distinct à
   câbler, jamais un choix de conception à revoir ; regrouper ces paramètres en objets artificiels
   propres à la fonction de câblage n'améliorerait pas la lisibilité. Même famille d'exemption que
   `FunctionNaming`/`UnusedPrivateFunction` déjà accordées à Compose dans ce fichier.
9. **`TestCoordinatorHarness` sans paramètre de construction.** Le brouillon de plan envisageait
   un constructeur à onze fakes substituables ; aucun test n'en avait besoin (tous configurent les
   propriétés mutables des fakes après construction). Simplifié en classe sans constructeur,
   évitant `LongParameterList` sans perte de flexibilité réelle.
10. **`ScanRequestPendingIntentSpecs` ajouté**, non listé dans le plan : le tap sur la notification
    d'attente de scan a besoin d'un `PendingIntent` distinct de
    `AlarmPendingIntentSpecs.fullScreen` (même session, même composant cible `AlarmActivity`) —
    un code de requête partagé écraserait silencieusement les extras de l'un par ceux de l'autre
    (`FLAG_UPDATE_CURRENT`). Testé (`ScanRequestPendingIntentSpecsTest`).
11. **`SessionCoordinatorMutexTest` sans Turbine.** Le plan mentionnait « Turbine + `runTest` » ;
    Turbine sert à observer un `Flow`, pas nécessaire ici. Le test dispatche deux fois le même
    événement en concurrence (`async`) sur un `SessionPersistenceGateway` qui force un point de
    suspension réel dans `commit()` : sans le `Mutex`, la seconde coroutine passerait la
    vérification de doublon avant que la première n'ait committé (TOCTOU), produisant deux
    `Applied` au lieu d'un `Applied` et un `Duplicate`.

## Fichiers

- Créés : `androidApp/core/system/src/main/kotlin/com/niumi/system/session/` (18 fichiers,
  `executors/` compris), `.../notification/{ScanRequestNotifier,ScanRequestNotificationSpecs,
  ScanRequestPendingIntentSpecs,AndroidScanRequestNotifier,NotificationAvailability}.kt`,
  `.../notification/di/ScanRequestModule.kt`, `.../audio/AlarmVolumeSource.kt`.
- Modifiés : `androidApp/core/system/src/main/kotlin/com/niumi/system/alarm/{AlarmScheduler,
  AndroidAlarmScheduler}.kt` (`canScheduleExact`), `.../di/AudioModule.kt` (provider
  `AlarmVolumeSource`), `androidApp/core/database/src/main/kotlin/com/niumi/database/{SessionStore,
  RoomSessionStore}.kt`, `dao/ReceiptDao.kt`, `mapping/StoreEntityMappers.kt` (`receipts`,
  `recordIncident`), `androidApp/app/src/debug/kotlin/com/niumi/app/poc/PocNfcModule.kt` → renommé
  `PocNfcBindingsModule.kt` (retrait de `PocFacadeModule`), `config/detekt/detekt.yml`.
- Tests créés : `androidApp/core/system/src/test/kotlin/com/niumi/system/session/` (9 fichiers de
  test + `fakes/` : 11 fichiers), `.../notification/{ScanRequestNotificationSpecsTest,
  ScanRequestPendingIntentSpecsTest}.kt`, `androidApp/core/system/src/androidTest/.../notification/
  AndroidScanRequestNotifierInstrumentedTest.kt`, `androidApp/core/database/src/androidTest/
  kotlin/com/niumi/database/{RoomSessionStoreReceiptsTest,RoomSessionStoreIncidentsTest}.kt`.

## Vérifications exécutées

```bash
./gradlew :core:system:testDebugUnitTest        # 91 tests verts (35 nouveaux)
./gradlew :core:database:testDebugUnitTest       # 90 tests verts, non-régression
./gradlew :shared:core:jvmTest                   # 160 tests verts, non-régression
./gradlew :feature:ringing:testDebugUnitTest \
          :feature:session:testDebugUnitTest \
          :app:testDebugUnitTest                 # 21 + 1 + 6 tests verts, non-régression
./gradlew :app:assembleDebug                     # vert — graphe Hilt fermé
./gradlew ktlintCheck                            # vert sur tout le dépôt
./gradlew detekt                                 # vert sur tout le dépôt
./gradlew :core:database:compileDebugAndroidTestKotlin \
          :core:system:compileDebugAndroidTestKotlin   # vert — compile-check des tests instrumentés
./gradlew :app:lintDebug                         # vert (après la montée de versions ci-dessous)
```

## Montée de versions du 2026-09-11

`:app:lintDebug` remontait initialement 7 `GradleDependency` — dérive de fraîcheur sans rapport
avec le code de cette étape : trois bibliothèques avaient publié un correctif depuis l'épinglage du
3 septembre. Montée faite après consultation des notes de version officielles (Context7 n'avait pas
encore indexé ces versions : ses données s'arrêtent à juillet 2026, antérieures au BOM 2026.08 déjà
utilisé) :

| Montée | Contenu réel | Nouvelle exigence annoncée |
| --- | --- | --- |
| Room 2.8.4 → 2.8.5 | Un correctif : requêtes `suspend` et invalidation tracker lèvent `IllegalStateException` après fermeture de la base | Aucune |
| Navigation Compose 2.10.0 → 2.10.1 | Un correctif : `sizeTransform` pris en compte quand `NavHost` saute l'animation | Aucune |
| Compose BOM 2026.08.00 → 2026.09.00 | Compose 1.12.0 → 1.12.1 ; material3 inchangé (1.4.0) | Aucune |

Résolution vérifiée (`:app:dependencies --configuration debugRuntimeClasspath`) : le BOM tire bien
`compose.ui/foundation/runtime:1.12.1` et `material3:1.4.0`, aucun saut de version inattendu.
Batterie complète relancée après la montée : tous les compteurs de tests identiques, aucune
régression, `:app:lintDebug` sans aucune remontée.

Le correctif Room 2.8.5 touche un cas que ce dépôt exerce (`RoomSessionStore*Test` ferment la base
dans `tearDown()`) : les 90 tests JVM `:core:database` restent verts, mais les tests instrumentés
n'ont pas encore été rejoués dessus — point ajouté aux validations sur appareil ci-dessous.

## Points restants

- **Exigence AGP de Navigation Compose, à trancher à l'étape 12.** Les notes de
  `navigation 2.10.0-alpha03` indiquent : « Updated Compose `compileSdk` to API 37. This means that
  a minimum AGP version of 9.2.0 is required when using Compose. » Le projet est en AGP 9.1.1, déjà
  un patch au-dessus de la borne testée par KGP 2.4.10 (9.1.0). L'exigence ne s'est pas encore
  manifestée : `navigation-compose` est sur le classpath sans être compilé contre, `NiumiNavHost`
  n'arrivant qu'à l'étape 12. Elle préexiste à la montée ci-dessus (elle vaut déjà pour 2.10.0,
  épinglée à l'étape 1). Deux options à l'étape 12 : monter AGP à 9.2.0 en s'éloignant davantage de
  la borne KMP, ou vérifier que `NiumiNavHost` compile malgré tout en 9.1.1.

## Validations sur appareil réel restantes

Trois tests instrumentés ajoutés à cette étape compilent (`compileDebugAndroidTestKotlin` vert)
mais n'ont pas été exécutés — aucun appareil Android n'était branché pendant cette session :

- `androidApp/core/database` : `RoomSessionStoreReceiptsTest`, `RoomSessionStoreIncidentsTest`.
- `androidApp/core/system` : `AndroidScanRequestNotifierInstrumentedTest`.

À exécuter avec un appareil ou émulateur branché (`adb devices`) :

```bash
./gradlew :core:database:connectedDebugAndroidTest
./gradlew :core:system:connectedDebugAndroidTest
```

Résultat attendu : les trois tests verts, sans régression sur les 29 tests instrumentés déjà
verts de `:core:database` (étapes 9-10). Point de vigilance supplémentaire depuis la montée en
Room 2.8.5 : son unique correctif fait lever `IllegalStateException` aux requêtes `suspend`
appelées après fermeture de la base — les tests instrumentés Room ferment la base dans
`tearDown()`, ce passage sur appareil est donc aussi la vérification de ce changement.

Comportement non validable sans appareil, à garder en tête pour l'étape 17 (parcours réel de la
notification d'attente de scan) : `CATEGORY_ALARM` sans son sous Ne pas déranger, dont le
comportement varie selon la version Android et les surcouches OEM (SPEC_ANDROID §10.5, déjà
signalé comme relevant de la matrice de tests physiques).
