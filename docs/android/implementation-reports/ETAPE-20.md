# Étape 20 — mort du processus, pertes de permission, snapshot corrompu

**Date :** 2026-09-15. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 20.

**Produit :** une mort de processus ne laisse plus le réveil muet, une perte de permission ou de
NFC pendant une session produit un incident au lieu de rien, et une corruption de stockage —
Direct Boot ou Room — est explicite, journalisée et non destructive. `RingingWatchdogReceiver`
réveille le processus toutes les 60 s tant qu'une session sonne ; `SessionRuntimeReconciler`
reprogramme une alarme disparue et signale un NFC coupé ; `SessionReconciler` consomme enfin le
résultat de `DirectBootMerger.merge()` et distingue trois cas de corruption ; le journal technique
d'avant déverrouillage rejoint Room au lieu de disparaître avec un processus qui meurt trop tôt.

**État avant cette étape.** `SessionReconciler` relançait déjà le son sur `RINGING`
(`resumeRinging`, étape 17), mais rien ne réveillait un processus mort pendant que l'alarme
sonnait — un « second maillon » explicitement reporté ici. `SessionRuntimeStatusProbe` était
construite par Hilt sans aucun appelant. `DirectBootMergeOutcome.Corrupted` était produit puis
jeté. Le journal technique écrit avant déverrouillage ne quittait jamais la mémoire.

---

## Trois héritages de l'étape 19, tranchés ici

### 1. `AppStartReconciler` du plan d'origine : non créé, le vrai trou était ailleurs

`Application.onCreate → reconcile(PROCESS_START)` est livré depuis l'étape 11
(`SessionStartupReconciler`) ; créer une seconde classe aurait été redondant. Le déclencheur
`ON_START` général via `ProcessLifecycleOwner` a été écarté (pas de nouvelle dépendance) après
avoir identifié le défaut réel : `SessionReadinessWatcher.evaluate()` sortait silencieusement quand
`publisher.snapshot.value` était encore `null` — exactement le cas d'un processus recréé après une
mort pendant `RINGING`, celui que cette étape traite. Corrigé : ce cas déclenche désormais une
réconciliation complète. La raison qui portait ce déclenchement, `FOREGROUND_AWAITING_SCAN`, est
renommée `FOREGROUND` pour refléter son périmètre élargi (§10.5).

### 2. Le chemin `Corrupted` : complété, pas seulement journalisé

`SessionReconciler` consomme désormais le résultat de `directBootMerger.merge()`. Trois cas
distincts :

- **Direct Boot corrompu, Room valide (déverrouillé)** — `SNAPSHOT_CORRUPTED` journalisé avec le
  `sessionId` trouvé dans Room, un incident `CRITICAL` consigné une fois par session, la projection
  Direct Boot **réécrite** (voir arbitrage ci-dessous).
- **Room illisible (déverrouillé)** — nouveau, hors du périmètre de `DirectBootMerger` : détecté
  par `SessionStoreUnreadableException`, représenté par `LoadResult.Unreadable`, affiché par
  l'écran de diagnostic sans jamais retirer le blocage.
- **Les deux à la fois** — aucune écriture, aucune suppression, un seul événement technique
  `sessionId = null` : sans session lisible, aucun `SessionIncident` n'est structurellement
  possible (SPEC_CORE_KMP §13 exige une révision).

### 3. Le journal technique d'avant déverrouillage : versé dans Room

Décision utilisateur : verser plutôt qu'assumer une limite. `InMemoryTechnicalEventLog.drain()`
(vidange atomique) + `RoomTechnicalEventLog.restore(...)` (préserve l'horodatage et le contexte
d'appareil d'origine) derrière une nouvelle interface `TechnicalEventLogFlush`, implémentée par
`UnlockAwareTechnicalEventLog` et liée à la même instance `@Singleton` que `TechnicalEventLog`.
Appelée par `SessionReconciler` au même endroit que la fusion Direct Boot. Limite résiduelle
assumée et documentée (§17) : un processus qui journalise avant déverrouillage et meurt avant
d'atteindre ce point perd ses entrées ; l'incident métier correspondant, lui, n'est jamais perdu
(rejeu de l'outbox, §9.3).

---

## Arbitrages validés avec l'utilisateur avant implémentation

### 1. Alarme de secours pendant `RINGING` : `setExactAndAllowWhileIdle`, pas `setAlarmClock`

L'option inexacte (`setAndAllowWhileIdle`) a été écartée sur un fait vérifié dans la documentation
Android, pas au jugé : seule une alarme *exacte* donne l'exemption de démarrage d'un service de
premier plan depuis l'arrière-plan. Entre `setExactAndAllowWhileIdle` et `setAlarmClock`, le second
était disqualifié pour une autre raison : il afficherait un tic invisible de 60 s au réglage
système « prochaine alarme », alors que §10.5 exige déjà la même discrétion pour la notification
d'attente de scan. Retenu : `setExactAndAllowWhileIdle`, réserve consignée — le quota Doze d'une
livraison par application toutes les neuf minutes fait dégénérer la chaîne à ce rythme.

**Correction apportée après coup à ma propre justification.** J'avais présenté ce quota comme non
mesurable, « la mesure exigeant l'état qu'elle romprait en le mesurant ». C'était faux, et l'erreur
méritait d'être nommée : je confondais « ne s'est pas produit pendant le protocole » et « ne peut
pas se mesurer ». Le protocole manuel tourne téléphone branché en USB pour `adb`, et **un appareil
en charge n'entre jamais en Doze** — l'absence d'observation ne mesurait donc que mon montage. Le
forçage existe précisément pour ça (`dumpsys battery unplug`, puis `dumpsys deviceidle force-idle`),
et le processus étant mort dans ce scénario, aucun wake lock ni FGS ne s'y oppose. L'essai est
ajouté au protocole manuel ci-dessous ; le résultat décidera si `setExactAndAllowWhileIdle` tient ou
si `setAlarmClock`, exempt de Doze, redevient le bon choix — décision à prendre sur le chiffre,
comme l'écart §10.5 de l'étape 19 l'a été sur 523 s.

Portée réelle du cas, à dire aussi : le **premier** tic, à 60 s, tombe presque certainement à
l'heure — l'appareil n'a pas eu le temps de se ré-endormir après la mort du processus — et s'il
relance le service, son wake lock et son plein écran empêchent Doze de s'installer. Le quota ne mord
que si ce premier tic échoue lui aussi. Plus rare que ma première formulation ne le laissait croire,
mais atteignable en production : table de nuit, débranché, immobile, écran éteint.

### 2. Corruption Direct Boot : écraser après journalisation, pas de quarantaine

Proposé d'abord en quarantaine (renommer le fichier fautif plutôt que le réécrire), puis révisé
après vérification du contenu du fichier : `niumi_session.json` porte `boxTokenSha256Hex` et la
liste des applications bloquées. §17 interdit que le hash du token traverse l'export de
diagnostic — seul canal d'assistance réellement disponible en release. Une quarantaine n'aurait
donc servi qu'un `adb shell run-as` en debug, au prix d'une rétention de données et d'une
obligation de nettoyage supplémentaire (l'effacer aussi dans `clear()`). Retenu : écraser après
journalisation de la raison exacte et consignation d'un incident.

### 3. `SessionRuntimeStatusProbe` : branchée sur son écart réel, pas ses six champs

Quatre des six champs (`accessibilityReady`, `notificationReady`, `fullScreenReady`, `audioReady`)
doublonnent déjà `SessionReadinessMonitor` (§13.1), avec sa propre déduplication d'incidents — les
brancher aussi ici aurait recréé les doublons corrigés à l'étape 16. Les deux qui restent ont un
écart réel non couvert : `alarmScheduled` teste l'alarme **réellement programmée**
(`isScheduled()`), quand `EXACT_ALARM` de §13.1 ne teste que la **permission**
(`canScheduleExactAlarms()`) — une surcouche OEM peut effacer la première sans toucher la seconde ;
`NFC_ENABLED` n'a aujourd'hui aucun producteur d'incident alors que le scan reste la seule sortie
de session (§11.2). `SessionRuntimeReconciler`, nouvelle classe, porte ces deux-là seuls.

**Défaut trouvé et corrigé en écrivant ce correctif** (§18) : la garde naïve « alarme non
programmée → réparer et signaler » entrait en conflit avec un test existant
(`armedWithExactAlarmPermissionRevokedNeverReschedulesTheAlarm`) qui protège précisément le cas où
la permission d'alarme exacte est elle-même révoquée — retenter `schedule()` dans ce cas est un
second essai voué au même échec, et `SessionReadinessMonitor` couvre déjà l'incident.
`SessionRuntimeReconciler.actionableGaps` retire donc l'écart d'alarme de son périmètre quand
`AlarmScheduler.canScheduleExact()` est faux.

### 4. Périmètre du contrôle NFC : tous les états non finaux sauf `PREPARING`

Retenu sur la cohérence avec §11.2 (le scan reste la seule sortie) et la ligne de §20 « NFC
désactivé pendant la sonnerie ». `PREPARING` est exclu : une activation interrompue n'a jamais
promis de scan à l'utilisateur.

---

## Décisions non couvertes par le plan ni par les specs

1. **`RoomBlockedPackagesSource.readFromRoom()` n'avait aucun `try/catch`.** Une `SQLiteException`
   traversait le `collect` de `BlockingProjectionRefresher.observeDecisions()` sans jamais être
   rattrapée, arrêtant **définitivement** le rafraîchissement du blocage — un défaut réel,
   indépendant du reste de l'étape, découvert en cherchant tous les accès Room non protégés.
   Corrigé par le même patron que `RoomSessionStore` (`SQLiteException` seule, jamais la garde de
   déverrouillage).
2. **`reportClockChange` et le nouveau cas de corruption partagent désormais `reportIncidentOnce`.**
   Même garde (un incident par code et par session), factorisée plutôt que dupliquée — une fonction
   de moins au total, conformément à l'esprit du plafond `TooManyFunctions` de detekt déjà serré sur
   `SessionReconciler`.
3. **La politique du watchdog et les deux écarts de `SessionRuntimeReconciler` sont appliqués en
   fin de passe**, sur le snapshot relu (`gateway.load()`) plutôt que celui du début de passe : les
   branches du `when` ont pu dispatcher des incidents entre-temps, et bâtir dessus un `INCIDENT_REPORTED`
   supplémentaire sur un snapshot périmé l'aurait fait rejeter en `STALE_REVISION` (même défaut que
   celui corrigé à l'étape 17 pour `SessionReadinessMonitor`).
4. **`IncidentDiagnosticViewModel.refresh()` a reçu un filet de sécurité (`runCatching`)** en plus
   des corrections à la racine (Room) : une source qui échouerait pour une tout autre raison ne
   laisse plus l'écran en `isLoading` indéfiniment.

## Fichiers

**Créés — `:core:system`**
- `alarm/RingingWatchdog.kt`, `RingingWatchdogSpecs.kt`, `RingingWatchdogPolicy.kt`,
  `AndroidRingingWatchdog.kt`, `RingingWatchdogReceiver.kt`.
- `session/RuntimeStatusGaps.kt`, `SessionRuntimeReconciler.kt`, `StorageIntegrityState.kt`,
  `session/di/RuntimeModule.kt`.

**Créés — `:core:database`**
- `logging/TechnicalEventLogFlush.kt`.

**Modifiés — `:core:system`**
- `session/ReconcileReason.kt` (`FOREGROUND_AWAITING_SCAN` → `FOREGROUND`, `+ RINGING_WATCHDOG`),
  `readiness/SessionReadinessWatcher.kt` (ne sort plus silencieusement sur snapshot `null`),
  `session/ReconcilerSources.kt` (+4 champs), `session/di/SessionModule.kt`, `di/SystemModule.kt`
  (`provideRingingWatchdog`), `session/SessionReconciler.kt` (consommation de `mergeOutcome`,
  watchdog, `SessionRuntimeReconciler`, `StorageIntegrityState`, `TechnicalEventLogFlush`,
  `reportIncidentOnce` partagé), `session/executors/StartRingingExecutor.kt`/
  `StopRingingExecutor.kt`, `session/di/EffectExecutorModule.kt`, `intent/NiumiComponent.kt`
  (`+ RINGING_WATCHDOG_RECEIVER`), `boot/DirectBootMerger.kt` (réécriture sur `Corrupted`),
  `src/main/AndroidManifest.xml` (receveur du watchdog, sans `intent-filter`).

**Modifiés — `:core:database`**
- `SessionStore.kt` (`+ SessionStoreUnreadableException`), `RoomSessionStore.kt`,
  `blocking/RoomBlockedPackagesSource.kt`, `incident/SessionIncidentsReader.kt`
  (`runCatching` → liste vide), `logging/RoomTechnicalEventLog.kt` (`recent()` protégée,
  `+ restore`), `logging/UnlockAwareTechnicalEventLog.kt` (`+ flush`), `logging/di/LoggingModule.kt`
  (`+ bindTechnicalEventLogFlush`), `logging/TechnicalEventType.kt` (`+ SNAPSHOT_CORRUPTED`),
  `dao/TechnicalEventDao.kt` (`+ insertAll`, `+ insertAllAndPurge`),
  `logging/InMemoryTechnicalEventLog.kt` (`+ drain`).

**Modifiés — `:feature:session`**
- `diagnostics/DiagnosticSources.kt`, `IncidentDiagnosticUiState.kt`
  (`+ storageFailureReason`), `IncidentDiagnosticViewModel.kt` (`runCatching`, lecture de
  `StorageIntegrityState`), `IncidentDiagnosticScreen.kt` (`+ StorageFailureBanner`),
  `IncidentDiagnosticTexts.kt`.

**Modifiés — `:app`**
- `system/AppComponentResolver.kt`, `navigation/HomeDestination.kt` (`+ storageUnreadable`),
  `navigation/HomeViewModel.kt` (lecture de `StorageIntegrityState`).

**Modifiés — `:feature:ringing` (androidTest)**
- `TestComponentResolverModule.kt` (`+ RINGING_WATCHDOG_RECEIVER`).

**Créé — `:app` (androidTest)**
- `ProcessDeathInstrumentedTest.kt`.

## Tests

**Ajoutés — 59 tests JVM, 1 instrumenté (`:app`, non exécuté sur appareil — voir plus bas).**

| Test | Module | Ce qu'il prouve |
| --- | --- | --- |
| `SessionReadinessWatcherTest` (4) | `:core:system` JVM | premier `onResume` sans snapshot publié → réconciliation ; état publié non-scan → surveillance seule ; accessibilité surveillée hors `ARMED` ; état de scan → republication |
| `RingingWatchdogSpecsTest` (6), `RingingWatchdogPolicyTest` (2) | `:core:system` JVM | `PendingIntent` distinct du réveil, stable, exhaustivité `RINGING` seul arme |
| `SessionReconcilerWatchdogTest` (4), `SessionCoordinatorRingingWatchdogTest` (2) | `:core:system` JVM | armement après reprise du son, désarmement sur tout autre état, idempotence, raison sans effet de bord |
| `RuntimeStatusGapsTest` (7), `SessionRuntimeStatusProbeTest` (5) | `:core:system` JVM | réparation silencieuse d'abord, incident unique si l'écart persiste, exception EXACT_ALARM, NFC pendant `RINGING` sans changer l'état |
| `SessionReconcilerCorruptionTest` (4), `UnlockAwarePersistenceGatewayCorruptionTest` (2), `StorageIntegrityStateTest` (3) | `:core:system` JVM | les trois cas de corruption, la garde de déverrouillage jamais avalée, l'état lu par l'accueil |
| `SessionReconcilerTechnicalEventFlushTest` (2) | `:core:system` JVM | vidage même sans projection à fusionner, jamais sur une raison hors fusion |
| `InMemoryTechnicalEventLogTest` (+2), `UnlockAwareTechnicalEventLogTest` (+3) | `:core:database` JVM | vidange atomique, horodatage d'origine préservé, jamais rejoué deux fois |
| `TechnicalEventTypeTest` (liste à 27) | `:core:database` JVM | `SNAPSHOT_CORRUPTED` dans la liste fermée de §17 |
| `HomeDestinationTest` (+2), `HomeViewModelTest` (+1) | `:app` JVM | redirection prioritaire vers le diagnostic, disparition dès que le stockage redevient lisible |
| `IncidentDiagnosticViewModelTest` (+2) | `:feature:session` JVM | écran jamais figé en chargement, limite affichée explicitement |
| `ProcessDeathInstrumentedTest` (4) | `:app` instrumenté | service repris au premier plan, `PendingIntent` du watchdog présent puis absent, reprise par broadcast explicite |

## Commandes exécutées

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew test                                          ✅ (tout le dépôt, à chaque lot)
./gradlew ktlintCheck detekt                             ✅ (tout le dépôt)
./gradlew :app:assembleDebug                             ✅
./gradlew :app:compileDebugAndroidTestKotlin
          :feature:ringing:compileDebugAndroidTestKotlin ✅
./gradlew :app:lintDebug                                 ✅ aucune remontée
./gradlew connectedDebugAndroidTest                      ✅ 141 tests, 0 échec, 1 ignoré
```

Deux détours pour deux règles detekt jamais rencontrées jusqu'ici sur ce fichier :
`SwallowedException` sur `RoomSessionStore.activeSession()` (corrigé en passant l'exception
d'origine comme `cause` de `SessionStoreUnreadableException`, plutôt que de ne garder que son
message) ; plusieurs violations `ktlint` de mise en forme (`when-entry-bracing`,
`argument-list-wrapping`, `class-signature`) corrigées par `ktlintFormat`, jamais à la main.

`connectedDebugAndroidTest` et `:app:lintDebug` n'ont **pas** été exécutés : ils exigent un appareil
Android branché, non disponible pendant cette session. `ProcessDeathInstrumentedTest` compile mais
n'a jamais tourné.

## Validation sur appareil réel

**Appareil :** Xiaomi 25080RABDG (`lapis`), Android 16 (SDK 36), HyperOS OS3.0, build
`BP2A.250605.031.A3` — le même qu'aux étapes 17 à 19. Session déroulée le 2026-09-15 de 12:35 à
13:30.

### Tests instrumentés — **141 verts, 0 échec, 1 ignoré**

| Module | Tests |
| --- | --- |
| `:core:database` | 70 |
| `:feature:setup` | 34 |
| `:core:system` | 23 |
| `:feature:ringing` | 6 |
| `:app` | **5** (1 préexistant + les 4 de `ProcessDeathInstrumentedTest`) |
| `:feature:session` | 3 (1 ignoré, préexistant) |

136 à l'étape 19, 141 ici. `:app:lintDebug` vert, sans remontée.

**Un échec au premier passage, de mon fait et non du code :** `ProcessDeathInstrumentedTest` semait
chaque session avec un `eventId` constant. Les reçus sont le registre d'idempotence et **survivent
volontairement à `clearActive`** (SPEC_CORE_KMP §12), donc tout test suivant échouait sur
`UNIQUE constraint failed: session_event_receipt.eventId` — exactement le défaut rencontré à
l'étape 19 sur `RoomDirectBootMergeTest`, que j'ai refait. `eventId` est désormais tiré au hasard à
chaque appel. Le `tearDown` désarme aussi le watchdog explicitement : une passe de réconciliation
peut l'armer pour de bon, et une alarme laissée en place tirerait toutes les 60 s bien après la
campagne.

**Deux pièges de banc rencontrés en fin de campagne, aucun lié au code.** D'abord
`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` sur `:feature:session` — la restriction
HyperOS intermittente sur l'installation multi-APK d'AGP, déjà consignée à l'étape 19 ; relancer
suffit. Ensuite, les cinq tests de `:app` en échec sur `expected: Absent but was: Present(…)` : la
session du protocole manuel, restée armée en base, que le `@Before` des deux classes refuse à juste
titre. Le nettoyage de l'état l'a réglé, et la campagne est repassée intégralement verte.

**Une flakiness observée une fois, non reproduite ensuite :** l'installation de l'APK de test émet
`MY_PACKAGE_REPLACED`, qui peut atteindre le processus d'instrumentation avant que
`HiltAndroidRule` n'ait créé le composant — `SystemEventsReceiver` plante alors sur
« The component was not created », et la campagne échoue avant le premier test. Relancer suffit.
Invisible à l'étape 19 parce que la permission d'autostart HyperOS était alors refusée et empêchait
ce broadcast de démarrer le processus ; sur un appareil sans cette restriction, elle serait
permanente.

### Protocole manuel

| Essai | Résultat |
| --- | --- |
| **1** — accessibilité coupée pendant `ARMED` | ✅ Un seul incident `BLOCKING_PERMISSION_REVOKED` `CRITICAL`, session conservée `ARMED` avec santé `DEGRADED`, déduplication confirmée à la seconde passe, notification sur `niumi_session_warning` (`category=err`, importance 4). |
| **2** — processus tué pendant `ARMED` | ✅ `am kill` **ne tue pas** le processus quand le service d'accessibilité est lié (constat des étapes 17/19 reconfirmé) ; `run-as … kill -9` y parvient. **Alarme conservée** à 12:55:00. Le processus ne redémarre pas de lui-même — c'est précisément le trou que cette étape comble. |
| **3** — sonnerie réelle | ✅ Alarme délivrée à 12:55, service au premier plan (`ONGOING_EVENT\|NO_CLEAR\|FOREGROUND_SERVICE`, `category=alarm`), audio `USAGE_ALARM`, et **watchdog armé à 12:56:01.211** — exactement T+60 s. |
| **4** — mort de processus pendant `RINGING`, sans Doze | ✅ Tué à 12:55:39, service disparu ; **son revenu à 12:56:02**, à l'instant du tic déjà armé ; **watchdog réarmé à 12:57:02**. Le défaut de l'étape 17 est refermé. |
| **5** — le même, sous **Doze profond forcé** | ✅ **Cinq livraisons consécutives à l'heure, appareil en `IDLE`** : 12:57:05, 12:58:03, 12:59:05, 13:01:06, 13:02:08 pour des tics programmés à 12:57:02, 12:58:02, 12:59:03, 13:01:05, 13:02:06. Intervalles de 58 à 62 s. Le quota Doze de 9 minutes **ne s'applique pas** — voir plus bas. |
| **6** — NFC coupé pendant `RINGING` | ✅ Incident `NFC_DISABLED` `CRITICAL` **unique**, événement technique du même nom, session toujours `RINGING` et sonnerie active. Ce code n'avait aucun producteur avant cette étape. |
| **7** — scan du boîtier | ✅ `COMPLETED` à 13:05:06, sonnerie arrêtée, **watchdog annulé** (`Reason=alarm_cancelled`, rtc=13:05:04.580), `niumi_session.json` supprimé, pointeur vidé. Statistiques de la campagne : 9 tics de watchdog, 1 alarme de réveil. |
| **8** — projection Direct Boot corrompue, Room valide | ✅ Fichier vidé, processus relancé : `SNAPSHOT_CORRUPTED` journalisé, incident `CRITICAL` **unique**, **projection réécrite depuis Room** (0 o → 1667 o, `domainRevision:3`), session toujours `ARMED`, alarme conservée à 14:00. |
| **9** — base Room illisible | ⚠️ **Deux défauts trouvés, tous deux corrigés et rejoués verts** — voir ci-dessous. Après correction : aucun plantage, 16 `SQLiteCantOpenDatabaseException` rattrapées, alarme conservée, écran « État illisible » affiché, et **session intégralement retrouvée** après restauration des droits. |

### Les deux défauts trouvés sur appareil, invisibles en JVM

**1. Room illisible faisait planter le processus à chaque démarrage.** `DirectBootMerger` touche
Room **avant** le `gateway.load()` de la passe, et `RoomDirectBootMerge.merge()` n'était protégé par
rien : la `SQLiteCantOpenDatabaseException` remontait jusqu'au scope du réconciliateur.
`FATAL EXCEPTION: DefaultDispatcher-worker-1`, boucle de plantage, et l'écran de diagnostic que §18
promet jamais atteint. Mon lot 5 n'avait protégé que `RoomSessionStore.activeSession()`,
`RoomBlockedPackagesSource`, `RoomSessionIncidentsReader` et `RoomTechnicalEventLog`.

Aucun test ne pouvait le voir : `bothStoresUnreadableDispatchNothingAndKeepTheBlockingProjection`
simule l'échec sur `gateway.load()` via `forceUnreadable`, et **aucune doublure de Room ne levait
jamais**. Corrigé à la même couche et selon la même règle que `RoomSessionStore` — aucune exception
SQLite brute ne sort de `:core:database` — plus un `DirectBootMergeOutcome.RoomUnreadable` et le
test `anUnreadableRoomIsReportedWithoutLettingTheExceptionEscape`, dont la doublure lève enfin.

**2. L'accueil affichait « Aucune session » alors que la session était armée et le blocage en
place.** Le signal existait pourtant — `StorageIntegrityState` était bien alimenté — mais je
l'avais câblé dans `HomeUiState.destination` seul, **qui n'est lue qu'au clic du bouton principal** :
rien ne l'affichait jamais. C'est le « faux état de fiabilité » que §15 interdit, et la forme la
plus trompeuse possible puisque l'affirmation rassurante était la seule que Niumi ne pouvait pas
faire. L'accueil affiche désormais « État illisible », dit que le blocage tient et que le scan reste
la seule sortie, et son bouton mène au diagnostic. Règle portée par deux fonctions pures
(`homeTitleFor`, `primaryLabelFor`) et verrouillée par `HomeScreenTextsTest`. §15 est complétée.

### Particularité de banc découverte ce jour

**`kill -9` fait classer le service d'accessibilité comme « planté ».** Android refuse alors de le
relier et remet `accessibility_enabled` à 0, pendant que l'écran des réglages affiche un
interrupteur **actif** et « Ce service ne fonctionne pas » — les deux étant exacts, ce qui rend le
diagnostic déroutant. Réparation : `settings delete secure enabled_accessibility_services` (la mise
à `""` est refusée, « Bad arguments »), `accessibility_enabled 0`, puis reposer les deux. À
rapprocher de « `am force-stop` coupe l'accessibilité » (étapes 18/19).

## Ce qui reste ouvert

- **Le seau d'App Standby n'a jamais pu être rétrogradé sous `EXEMPTED` (5)** pendant l'essai, y
  compris processus mort : le système a refusé `am set-standby-bucket restricted`. Un appareil qui
  placerait Niumi en `RARE` ou `RESTRICTED` au moment où le watchdog compte n'a donc pas été
  observé. Argument de proportion, pas preuve : le watchdog ne tourne que dans la fenêtre suivant
  immédiatement un service de premier plan et un écran de réveil plein écran, où un seau bas est
  improbable par construction.
- **La mesure Doze vaut pour un seul appareil et une seule surcouche.** `USE_EXACT_ALARM` est une
  permission de plateforme, donc le résultat devrait tenir ailleurs, mais rien ne le prouve : à
  reprendre dans la matrice §20 fabricant par fabricant.
- **La matrice §20 reste à couvrir sur d'autres fabricants** — inchangé depuis l'étape 19, tout ce
  qui précède n'a jamais été mesuré que sur un seul appareil.

## Specs modifiées dans ce changement

- **SPEC_ANDROID §4.2** — réserve du quota Doze du watchdog.
- **SPEC_ANDROID §7.1** — les deux champs pilotés par `SessionRuntimeReconciler`, distincts des
  quatre propriétés de `SessionReadinessMonitor`.
- **SPEC_ANDROID §9.1** — dérogation à l'API imposée, strictement limitée au watchdog de `RINGING`.
- **SPEC_ANDROID §10.2** — conception livrée du watchdog, en remplacement de « reste à concevoir ».
- **SPEC_ANDROID §10.5** — renommage `FOREGROUND_AWAITING_SCAN` → `FOREGROUND`, périmètre élargi.
- **SPEC_ANDROID §13.1** — ligne NFC ajoutée au tableau, déclencheur de premier plan reformulé sans
  énumération vouée à devenir fausse.
- **SPEC_ANDROID §17** — `SNAPSHOT_CORRUPTED` dans la liste fermée (27 valeurs), versement du
  journal d'avant déverrouillage et sa limite résiduelle.
- **SPEC_ANDROID §18** — l'exécution concrète de l'alinéa `SessionRuntimeStatus`, les trois cas de
  corruption.
- **SPEC_ANDROID §20** — deux lignes de matrice mises à jour ou ajoutées (mort de processus pendant
  `RINGING`, corruption Direct Boot et Room).
