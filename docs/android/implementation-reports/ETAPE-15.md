# Étape 15 — session active, blocage persisté, modification ou annulation par scan

**Date :** 2026-09-12
**Périmètre :** écrans 7 (complet), 9 et 11 ; reconstruction de la projection de blocage depuis la
persistance ; surveillance du service d'accessibilité étendue à toute la durée d'une session.

## Ce que l'étape corrige

Trois manques motivaient cette étape :

1. **La projection de blocage n'existait qu'en mémoire.** `InMemoryBlockedPackagesProjection`
   (étape 5) était écrite par l'effet `APPLY_BLOCKING` et lue par le service d'accessibilité. Un
   processus tué, ou un service recréé seul, faisait disparaître le blocage d'une session encore
   active — ce que SPEC_ANDROID §12.2 et §13 interdisent.
2. **L'écran 7 était minimal** : état, heure, fuseau. Ni applications bloquées, ni santé, ni
   incidents, ni bouton de sortie.
3. **Aucune sortie de session n'était atteignable.** `NiumiRoute.ScanToModify` et
   `NiumiRoute.Cancelled` étaient déclarées depuis l'étape 14 mais enregistrées dans aucun
   `NavHost` : une session armée n'avait aucun chemin vers l'annulation par scan
   (SPEC_CORE_KMP §2 point 4).

## Écarts au plan, validés avec l'utilisateur

### 1. `RoomBlockedPackagesProjection` était irréalisable au chemin annoncé

Le plan demandait
`androidApp/core/database/.../blocking/RoomBlockedPackagesProjection.kt` **implémentant**
`BlockedPackagesProjection`. Deux obstacles, tous deux structurels :

- `BlockedPackagesProjection` vit dans `:core:system`, et SPEC_ANDROID §6 n'autorise que
  `:core:system → :core:database`. Un fichier de `:core:database` ne peut donc pas implémenter
  cette interface. Précédent identique : `DirectBootWriteResult`, inventé à l'étape 10 pour la
  même raison.
- `current()` est synchrone, appelée dans `onAccessibilityEvent` sur le thread principal, alors
  que tous les DAO du module sont `suspend`. Une lecture Room directe y est impossible,
  indépendamment de l'emplacement du fichier.

**Correction retenue** (option « source Room + cache ») :

| Module | Fichier | Rôle |
| --- | --- | --- |
| `:core:database` | `blocking/BlockedPackagesState.kt` | déplacé depuis `:core:system` ; il y importait déjà `BlockedPackage` |
| `:core:database` | `blocking/BlockedPackagesRead.kt` | `Resolved(state)` ou `Unreadable(reason)` |
| `:core:database` | `blocking/BlockedPackagesSource.kt` | interface de lecture |
| `:core:database` | `blocking/RoomBlockedPackagesSource.kt` | toute la logique : Room après déverrouillage, `DirectBootStore` avant |
| `:core:system` | `blocking/PersistedBlockedPackagesProjection.kt` | cache `@Volatile` + `suspend fun refresh()` |
| `:core:system` | `blocking/BlockingProjectionRefresher.kt` | abonnement au flux de décisions |

`BlockedPackagesProjection` n'a pas bougé : ses consommateurs (service, `SessionReconciler`) ne
voient qu'un changement d'import.

**Raffinement sur l'illisibilité.** Le plan prévoyait une quatrième variante `Unreadable` dans
`BlockedPackagesState`, traitée par `BlockingDecision.decide`. Cette forme obligeait l'algorithme
pur de §12.2 à décider seul du sort d'un état qu'il ne peut pas interpréter — il ne connaît pas la
décision précédente — et laissait le type illisible atteignable par le service. L'illisibilité vit
donc dans `BlockedPackagesRead`, le type de **retour de lecture** : le cache ne peut
structurellement mémoriser qu'un `Resolved`, donc un snapshot corrompu ne peut pas, même par erreur
de programmation, se transformer en `Inactive`.

### 2. Pas d'`AccessibilityServiceWatcher`

Le plan demandait un composant émettant `BLOCKING_PERMISSION_REVOKED CRITICAL` une fois par
session. Or `SessionReadinessMonitor` (étape 12) porte **déjà** le mapping
`ACCESSIBILITY_SERVICE → BLOCKING_PERMISSION_REVOKED CRITICAL` et sa garde de déduplication ; un
composant parallèle aurait produit deux incidents `CRITICAL` pour le même fait. Les deux vrais
manques étaient ailleurs :

- le monitor sortait dès `snapshot.state != ARMED`, rendant invisible un service coupé pendant
  `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou `RELEASING` — alors que le blocage court
  jusqu'au scan (§3) ;
- le déclencheur « passage de l'application au premier plan » de §13.1 n'avait **aucun appelant**
  depuis l'étape 12 : `SessionReadinessWatcher.evaluateAsync()` n'était jamais appelée.

**Correction :** le contrôle `ACCESSIBILITY_SERVICE` est évalué dans tous les états non finaux
(`MonitoredReadinessChecks.blockingOnlyIncidentCodes`), les cinq autres restant limités à `ARMED` —
une fois la sonnerie commencée, avertir d'un volume d'alarme perdu ne décrit plus rien
d'actionnable. Un contrôle qui sort du périmètre voit sa notification retirée, sans quoi elle
resterait affichée sans qu'aucune passe ne puisse plus la réévaluer. Le déclencheur de premier plan
est branché sur le `ON_RESUME` de l'écran 7, derrière la nouvelle interface
`ForegroundReadinessTrigger` — `SessionReadinessWatcher` enregistre un `BroadcastReceiver` et
possède son propre scope, donc n'est pas instanciable en test JVM.

### 3. `PendingNfcScanHandler` sous qualificatif

Le plan demandait une liaison de production non qualifiée pour `NfcScanHandler`. Deux problèmes :

- conflit de binding avec `PocNfcBindingsModule.bindNfcScanHandler` (`src/debug` de `:app`) ;
- surtout, en debug l'écran 9 aurait hérité de `PocNfcScanHandler`, qui retourne `Accepted` sur le
  boîtier associé **sans toucher à la session réelle** : l'écran aurait navigué vers « Session
  annulée » alors que rien n'était annulé.

**Correction :** qualificatif `@SessionNfcScanHandler`, lié à `PendingNfcScanHandler` dans `main`
et dans tous les variants. L'écran 9 ne consomme que ce qualificatif. À l'étape 18, seule la
liaison change : `HandleValidNfcUseCase` prend cette place, `ScanToModifyViewModel` n'est pas
touché.

Le plan prévoyait aussi un texte « Fonction disponible à l'étape suivante » en debug. Il n'a pas
été livré : retourner `Ignored` sur un scan non validable est exactement ce qu'exige
SPEC_CORE_KMP §4 (« un NFC invalide ne modifie ni l'état, ni le blocage, ni le son »), l'écran
n'annonce donc aucun succès, et ce texte aurait été le seul élément à différer entre debug et
release sur un écran dont tout l'intérêt est d'être identique.

## Deux points non prévus, découverts à l'implémentation

### `PREPARING` ne peut pas se lire `Active` sur la seule présence des lignes `blocked_app`

`SessionReconciler.reconcilePreparing` (étape 11) déduit de `current() is Active` que l'activation
a réussi :

```kotlin
val blockingActive =
    (sources.blockedPackagesProjection.current() as? BlockedPackagesState.Active)?.sessionId ==
        snapshot.sessionId
```

Mapper `PREPARING` sur `Active` par simple présence des lignes de sélection aurait transformé une
activation interrompue en activation aboutie — exactement l'inverse de ce que la reprise doit
conclure. La projection lit donc le statut de l'effet `APPLY_BLOCKING`, symétriquement à
`REMOVE_BLOCKING` en `RELEASING`. Table complète dans SPEC_ANDROID §12.2.

Cela a demandé une requête que `OutboxDao` n'avait pas : `replayable()` ne renvoie que
`PENDING`/`FAILED` et ne filtre pas par `kind`, donc ne peut pas répondre « cet effet a-t-il
réussi ? ». `forSessionAndKind` est ajoutée, triée par révision décroissante — une reprise peut
avoir produit le même `kind` sur plusieurs révisions, seule la plus récente décrit l'état courant.

**Asymétrie Room / Direct Boot à ne pas confondre.** Dans Room, l'absence de ligne d'effet signifie
« jamais décidé ». Dans le snapshot Direct Boot, que `UnlockAwarePersistenceGateway` alimente depuis
`SessionStore.pendingEffects()` (donc `PENDING`/`FAILED` seuls), l'absence signifie au contraire
« déjà exécuté ». Les deux branches de `RoomBlockedPackagesSource` appliquent donc des règles
inverses, et chacune a ses tests.

### Aucun chemin de lecture des incidents n'existait

`IncidentDao.forSession` était livré depuis l'étape 9 mais n'avait **aucun appelant** (son KDoc le
disait encore), et `SessionStore` n'expose que l'écriture (`recordIncident`). L'écran 7 doit
présenter les `CRITICAL` explicitement (SPEC_CORE_KMP §7.3).

`SessionIncidentsReader` + `RoomSessionIncidentsReader` (`:core:database`, unlock-aware) sont
ajoutés plutôt qu'une douzième méthode sur `RoomSessionStore`, déjà au plafond `TooManyFunctions`
de detekt (11) et dont la lecture d'incidents ne relève d'aucune transaction de décision. Avant
déverrouillage la liste est vide, pas une erreur : le snapshot Direct Boot n'a pas de table
d'incidents, et un incident manquant ne doit jamais empêcher d'afficher le reste de la session.

## Décisions d'implémentation

- **`apply()`/`remove()` conservés sur la projection.** `BlockingController.apply` n'est pas
  `suspend` et doit rendre un `OperationResult` synchrone à l'exécuteur d'effet. Ce ne sont pas des
  écritures concurrentes à Room : l'exécuteur ne s'exécute qu'**après** `commitDecision`, donc les
  paquets qu'il transmet sont déjà ceux de la base. Ils avancent le cache sans attendre le
  prochain `refresh()`, qui reste l'autorité de reprise.
- **`BlockingProjectionRefresher` extrait du service.** Un `AccessibilityService` ne s'instancie
  pas en test JVM et le dépôt n'utilise pas Robolectric. Le service ne garde que le câblage : un
  scope `Dispatchers.Main.immediate` créé dans `onServiceConnected` et annulé dans `onUnbind`.
  `refreshNow()` subsiste malgré la collecte du `StateFlow` (qui émet déjà sa valeur courante à
  l'abonnement) parce qu'un `StateFlow` conflue les valeurs égales : un statut d'effet peut changer
  dans l'outbox sans que le snapshot bouge, en `RELEASING` partiel.
- **`BlockedPackagesSource` est une interface.** `RoomBlockedPackagesSource` étant une classe
  concrète, la règle « une persistance illisible ne lève pas le blocage » n'aurait pu être prouvée
  qu'avec une base réelle corrompue. Même motif que `SessionStore` et `DirectBootStore`.
- **`SessionDtoFixtures.snapshotInState`** ajoutée : aucune fixture ne produisait un snapshot dans
  un état arbitraire, ce dont les tests du refresher et du monitor ont besoin.
- **`ActiveSessionViewModel` distingue « aucune application » de « rien lu ».** `blockedApps` nul
  en interne signifie que la persistance n'a rien pu dire (`LoadResult.Unreadable` ou `Absent`) :
  la projection conserve alors ce qu'elle affichait, plutôt que d'affirmer « aucune application
  bloquée » (§13).
- **`NiumiNavHost` découpée.** Elle dépassait `LongMethod` de detekt (71 > 60) : les trois
  destinations de session sont extraites dans `NavGraphBuilder.activeSessionDestinations`. Aucune
  règle detekt assouplie.
- **`SessionReadModule`** regroupe les deux liaisons de lecture ajoutées ici, `DaoModule` étant
  déjà au plafond `TooManyFunctions`.

## Fichiers

### Créés

`:core:database`
- `blocking/BlockedPackagesState.kt` (déplacé depuis `:core:system`)
- `blocking/BlockedPackagesRead.kt`, `blocking/BlockedPackagesSource.kt`,
  `blocking/RoomBlockedPackagesSource.kt`
- `incident/SessionIncidentsReader.kt` (interface + `RoomSessionIncidentsReader`)
- `di/SessionReadModule.kt`
- tests : `androidTest/.../blocking/RoomBlockedPackagesSourceTest.kt`,
  `androidTest/.../incident/RoomSessionIncidentsReaderTest.kt`,
  `test/.../blocking/DirectBootBlockedPackagesSourceTest.kt`,
  `test/.../incident/RoomSessionIncidentsReaderUnlockGuardTest.kt`

`:core:system`
- `blocking/BlockingProjectionRefresher.kt`
- `nfc/PendingNfcScanHandler.kt` (+ qualificatif `@SessionNfcScanHandler`)
- `readiness/ForegroundReadinessTrigger.kt`
- tests : `blocking/BlockingProjectionRefresherTest.kt`, `blocking/fakes/RecordingBlockedPackagesSource.kt`

`:feature:session`
- `active/ScanToModifyViewModel.kt`, `active/ScanToModifyScreen.kt`, `active/ScanToModifyTexts.kt`
  (+ `CancelledTexts`), `active/CancelledScreen.kt`
- tests : `active/ScanToModifyViewModelTest.kt`, `active/ActiveSessionTextsTest.kt`,
  `active/fakes/ActiveSessionTestFakes.kt`

### Renommés

- `:core:system` — `blocking/InMemoryBlockedPackagesProjection.kt` →
  `blocking/PersistedBlockedPackagesProjection.kt` (et son test)

### Modifiés

- `:core:database` — `dao/OutboxDao.kt` (`forSessionAndKind`),
  `mapping/StoreEntityMappers.kt` (`SessionIncidentEntity.toDomain`)
- `:core:system` — `blocking/BlockedPackagesProjection.kt`, `blocking/BlockingDecision.kt`,
  `di/BlockingModule.kt`, `nfc/di/NfcModule.kt`, `readiness/MonitoredReadinessChecks.kt`,
  `readiness/SessionReadinessMonitor.kt`, `readiness/SessionReadinessWatcher.kt`,
  `readiness/di/ReadinessModule.kt`, `session/SessionReconciler.kt` (import seul) ; tests :
  `blocking/BlockingDecisionTest.kt`, `readiness/SessionReadinessMonitorTest.kt`,
  `session/SessionReconcilerTest.kt`, `session/fakes/FakeBlockedPackagesProjection.kt`,
  `session/fakes/SessionDtoFixtures.kt`
- `:feature:session` — `active/ActiveSessionScreen.kt`, `active/ActiveSessionTexts.kt`,
  `active/ActiveSessionUiState.kt`, `active/ActiveSessionViewModel.kt`,
  `blocking/AndroidBlockingController.kt`, `blocking/NiumiBlockingAccessibilityService.kt`,
  `di/SessionBlockingModule.kt` ; tests : `active/ActiveSessionViewModelTest.kt`,
  `androidTest/.../blocking/AndroidAccessibilityServiceStatusInstrumentedTest.kt` (commentaire)
- `:app` — `navigation/NiumiNavHost.kt`
- specs — `specs/SPEC_ANDROID.md` §12.2, §13.1, §15
- plan — `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 15

## Commandes exécutées

```bash
export JAVA_HOME="$(brew --prefix openjdk@17)"
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest \
          :core:database:testDebugUnitTest :app:testDebugUnitTest \
          :feature:setup:testDebugUnitTest :feature:ringing:testDebugUnitTest :shared:core:jvmTest
./gradlew :app:assembleDebug ktlintCheck detekt :app:lintDebug
```

Résultat : **641 tests JVM verts, 0 échec.**

| Module | Tests | Écart étape 14 |
| --- | --- | --- |
| `:shared:core` | 160 | — |
| `:core:database` | 102 | +9 |
| `:core:system` | 168 | +14 |
| `:feature:setup` | 76 | — |
| `:feature:session` | 100 | +31 |
| `:feature:ringing` | 21 | — |
| `:app` | 14 | — |

`:app:assembleDebug`, `:app:lintDebug`, `ktlintCheck` et `detekt` verts. Aucune règle detekt
assouplie, aucun avertissement nouveau : la seule déprécation remontée
(`androidx.hilt.navigation.compose.hiltViewModel`, déplacée vers
`androidx.hilt.lifecycle.viewmodel.compose`) préexiste dans neuf fichiers depuis l'étape 12b, et
les nouveaux écrans suivent la convention en place — sa migration est un changement transverse qui
n'appartient pas à cette étape.

## Validation sur appareil réel — 2026-09-12

**Appareil :** Xiaomi 25080RABDG, Android 16 (API 36), HyperOS — le même qu'à l'étape 14.

### Tests instrumentés

`./gradlew :core:database:connectedDebugAndroidTest` : **59 tests verts** (41 préexistants + 18
nouveaux).

**Deux échecs au premier passage, corrigés — défaut dans les tests, pas dans le code.**
`RoomBlockedPackagesSourceTest.ringingAndAwaitingScanStatesStillBlock` et
`finalStatesReadAsInactiveEvenWhileThePointerStillExists` bouclent sur plusieurs états en
réensemençant la même session, et réutilisaient le même `eventId` de reçu à chaque itération :
`SQLiteConstraintException: UNIQUE constraint failed: session_event_receipt.eventId`. C'est le
registre d'idempotence de l'étape 11 qui fonctionnait correctement. `seed()` prend désormais un
`eventId` par appel.

### Protocole manuel, essai par essai

| Essai | Résultat |
| --- | --- |
| 1. Parcours jusqu'à l'activation | ✅ Diagnostic tout au vert (boîtier de l'étape 14 encore associé, 3 applications). Alarme vérifiée : `setAlarmClock`, `RTC_WAKEUP`, bloc `Alarm clock:`, `window=0`, `exactAllowReason=policy_permission`, `PendingIntent` explicite vers `AlarmReceiver`, `2026-09-13 07:00:00` |
| 2. Écran 7 complet | ✅ État, date/heure/fuseau, santé, les 3 applications avec leurs libellés figés, rappel d'engagement, bouton « Modifier ou annuler ». Aucune autre action |
| 3. Application bloquée depuis le launcher | ✅ Retour à l'accueil système + overlay « Adobe Acrobat reste bloquée jusqu'au scan du boîtier. », texte de §12.2 mot pour mot |
| 4. Application bloquée depuis les récents | ✅ Même comportement |
| 5. **Service recréé seul, processus neuf** | ✅ **C'est le correctif de l'étape 15, prouvé.** Service relié par un cycle du réglage d'accessibilité, processus Niumi créé par la seule liaison du service, **aucune activité ouverte** (`topResumedActivity` = launcher) → le blocage s'applique. La projection a été reconstruite depuis Room. Avant cette étape, la projection en mémoire aurait été vide |
| 6. Session armée retrouvée après mort du processus | ✅ « Une session est en cours. » + « Voir ma session » (non-régression du correctif de l'étape 14) |
| 7. Service d'accessibilité désactivé pendant la session | ✅ Notification « Vérifie ton réveil Niumi », puis sur l'écran 7 : bloc « À vérifier maintenant » en tête sur fond `errorContainer`, « Le service d'accessibilité a été désactivé : le blocage ne s'applique plus. », santé passée à `DEGRADED` sans promesse de retour à la normale (§7.3) |
| 8. Changement de fuseau horaire | ✅ Fuseau d'activation conservé (« Demain, dimanche 13 septembre à 07:00 (Europe/Paris) »), nouveau bloc « Dans ton fuseau actuel » (« Aujourd'hui, dimanche 13 septembre à 17:00 (Pacific/Auckland) »). Instant inchangé, libellé relatif recalculé dans le fuseau d'affichage |
| 9. Écran 9 | ✅ Titre, texte imposé mot pour mot, **aucune autre action** (§3, §10.2) |
| 10. Scan du boîtier associé sur l'écran 9 | ✅ Lu et journalisé, **aucun changement d'état, aucune navigation, aucun message** — comportement voulu tant que `HandleValidNfcUseCase` n'existe pas |
| 11. Tag illisible sur l'écran 9 | ✅ « Ce tag n'a pas pu être lu. Réessaie en le posant bien à plat contre le téléphone. » en couleur d'erreur, session inchangée |

### Deux constats de plateforme, hors périmètre de l'étape

**1. Android ne relie pas le service d'accessibilité après une mort de processus.** Observé
précisément : `am kill` ne tue pas le processus (il héberge un service d'accessibilité lié, le
système le protège) ; `am force-stop` le tue **et révoque le service** (il disparaît de
`enabled_accessibility_services`), ce qui est le geste utilisateur « Forcer l'arrêt », pas une mort
système ; `am crash` tue le processus en **laissant le service listé dans
`enabled_accessibility_services`**, et Android ne l'a alors **jamais relié** — ni après 40 s, ni sur
un événement de fenêtre, ni après un cycle verrouillage/déverrouillage, **ni même quand le processus
est relancé** par l'ouverture de l'application. Le journal système ne contient aucune
`Scheduling restart`, seulement `Process com.niumi.app has died: fg TOP`. Une application bloquée
s'est ouverte normalement dans cet état.

**Niumi le détecte correctement**, ce qui a été vérifié et non supposé : Android remet
`Settings.Secure.ACCESSIBILITY_ENABLED` à `0` dès que plus aucun service n'est lié, et
`AndroidAccessibilityServiceStatus` teste ce drapeau global **avant** la liste des services — une
décision de l'étape 12 prise exactement pour ce cas. Le diagnostic affiche donc « Le service
d'accessibilité de Niumi est inactif », l'incident `BLOCKING_PERMISSION_REVOKED CRITICAL` est émis,
et l'écran 7 le présente (essai 7). **Aucun faux état de fiabilité** : §15 est respecté.

Aucun code de Niumi ne tourne dans la fenêtre où le blocage est perdu, et rien ne peut le rétablir —
§12.2 interdit par ailleurs de tenter de réactiver le service. **Ce n'est donc pas un défaut de
l'étape 15**, qui corrige « le service redémarre avec une projection vide » (essai 5, prouvé) et non
« le service n'est pas redémarré ».

**Quand cela peut-il réellement se produire ?** Mesuré, et le résultat relativise fortement le
constat. Avec l'application en arrière-plan et seul le service d'accessibilité maintenant le
processus, `dumpsys activity processes` donne pour Niumi `prcp` et `oom: cur=200`, soit
`PERCEPTIBLE_APP_ADJ`. L'ordre de sacrifice d'Android commence à 900+ (applications en cache, tuées
en permanence), passe par 700 (application précédente), 600 (launcher), 500 (services B) et
n'atteint 200 qu'ensuite. **Le service d'accessibilité protège donc le processus** — c'est aussi
pourquoi `am kill` a refusé de le tuer, cette commande ne visant que les processus en cache.

Les déclencheurs réels se réduisent donc à :

1. **Un bug de Niumi** — une exception non rattrapée, ce que `am crash` simule. **§12.2 l'anticipe
   déjà explicitement** : « Aucune exception ne doit sortir de `onAccessibilityEvent`. Une exception
   non rattrapée y provoque l'arrêt forcé de l'application […] le blocage disparaît entièrement. »
   Ce n'est pas un événement de fonctionnement normal, c'est la conséquence d'un défaut, et la spec
   en fait déjà une contrainte de code.
2. **Un arrêt forcé par l'utilisateur** — volontaire, et déjà annoncé dans l'onboarding.
3. **Un nettoyage agressif d'un OEM** — le seul cas involontaire plausible, et le risque réaliste
   sur cette marque. §17 porte déjà `OEM_RESTRICTION_SUSPECTED` pour cette famille.
4. Une pression mémoire extrême — théorique à `adj=200`, non observée.

**Conséquence sur les priorités :** ce n'est pas une limite de fonctionnement courant, et §4.3 n'a
pas à être réécrite pour un cas qui n'est pas routinier. Deux points restent néanmoins ouverts, sans
urgence :

- **Une action de remédiation sur l'écran 7 — reportée à l'étape 16, décidé avec l'utilisateur le
  2026-09-12.** Spécifiée dans SPEC_ANDROID §15, « Remédiation des incidents sur l'écran 7 », et
  inscrite en tête de l'étape 16 du plan avec sa case, ses tests et son protocole manuel.
  L'incident `CRITICAL` y est aujourd'hui purement informatif :
  l'utilisateur lit « le blocage ne s'applique plus » sans aucun moyen d'agir, depuis le seul écran
  visible pendant une session. L'écran 2 porte déjà l'action « Ouvrir les réglages d'accessibilité »
  (§13) ; l'écran 7 devrait l'offrir pour les incidents remédiables. Une fois le service réactivé à
  la main, le blocage reprend correctement — c'est précisément ce que garantit la reconstruction
  depuis Room livrée ici. Relève de l'étape 16, qui porte le diagnostic d'incident et ses actions.
  Ce manque vaut pour **toute** cause de désactivation du service, pas seulement la mort du
  processus : c'est ce qui le rend utile indépendamment de ce constat.
- **Le comportement d'un kill par l'OEM ou le LMK n'est pas mesuré.** Tout ce qui précède est
  établi pour `am crash`. L'étape 20 porte explicitement « mort du processus » : c'est là que la
  mesure doit être faite, avec une pression mémoire réelle et, sur cette marque, les réglages
  d'économie d'énergie poussés au maximum.

**2. La plateforme vibre à chaque détection de tag.**
`ReaderModeNfcReader.start` appelle `enableReaderMode(activity, callback, FLAG_READER_NFC_A, extras)`
**sans** `NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS` (étape 4). Android émet donc son propre retour
sonore et haptique sur toute détection, indépendamment de Niumi — vérifié sur le tag illisible, où
le code de Niumi ne vibre pas (`record(Unreadable)` ne journalise que `NFC_SCAN_INVALID`).

Conséquence sur le contrat de §11.2 : la vibration d'erreur y est censée **signifier** « ce boîtier
n'est pas le tien ». Si chaque détection vibre, ce signal perd son sens, et à l'étape 18 un scan
accepté vibrera exactement comme un scan refusé (qui vibrera deux fois).

**Décision de l'utilisateur (2026-09-12) : ne rien changer.** Une vibration sur un mauvais scan est
jugée normale — ce qui est d'ailleurs ce que §11.2 prescrit déjà via `vibrateError()`. Aucun
correctif n'est appliqué à l'étape 15.

**Reste ouvert pour l'étape 18**, la décision ne le couvrant pas : un tag *illisible* n'a pas de
vibration prévue par §11.2 (il n'est pas un refus, seulement une lecture manquée) et en reçoit
pourtant une de la plateforme ; et surtout un scan **accepté** en recevra une identique. L'arbitrage
des trois retours haptiques se fera à l'étape 18, quand les trois résultats de scan existeront
réellement. Le correctif éventuel — `FLAG_READER_NO_PLATFORM_SOUNDS` — touche `:core:system`
(étape 4) et change aussi l'écran d'association (étape 13) et l'écran de réveil (étape 17).

### Essai non effectué

- **Service d'accessibilité désactivé pendant que l'alarme sonne.** C'est la seule vérification
  matérielle de l'extension de §13.1 au-delà de `ARMED`. Elle exige une session en train de sonner,
  donc soit attendre 07:00, soit armer une session à quelques minutes — impossible sans annuler la
  session courante, ce qui exige le scan livré à l'étape 18. Couverte en JVM par
  `SessionReadinessMonitorTest.theAccessibilityServiceIsStillMonitoredAfterArmed`, **jamais observée
  sur appareil**.
- `:feature:session:connectedDebugAndroidTest`, `:core:system:connectedDebugAndroidTest`,
  `:feature:setup:connectedDebugAndroidTest` — non rejoués, aucun de leurs tests n'étant touché
  hors d'un commentaire.

## Incertitudes et limites

- **L'annulation par scan n'est pas fonctionnelle.** `HandleValidNfcUseCase` arrive à l'étape 18.
  L'écran 11 n'est donc atteignable qu'en injectant un événement, pas par un parcours réel, et
  l'essai 7 ci-dessus vérifie précisément que rien ne se passe.
- **Le cache de la projection reste en mémoire par construction.** Sa correction dépend de
  `refresh()` aux bons moments : connexion du service et chaque décision publiée. Un statut d'effet
  modifié dans l'outbox sans nouvelle décision ne déclenche pas de réalignement — situation
  possible seulement pendant `RELEASING`, où le prochain événement de session la corrige.
- **La garde de déduplication des incidents vit en mémoire** et disparaît avec le processus :
  comportement assumé et déjà documenté en §13.1.
- **`SessionReconciler.reconcilePreparing` n'a pas été rejoué sur appareil.** Sa non-régression
  face à l'apparition de `Releasing` en production est couverte par `SessionReconcilerTest` (JVM)
  et par la règle `PREPARING` + `APPLY_BLOCKING` décrite plus haut, mais la reprise après mort du
  processus pendant l'activation n'a pas été réobservée physiquement à cette étape.
- **`:feature:session` n'a aucun test instrumenté du service d'accessibilité réel** : le
  rechargement de la projection à `onServiceConnected` est prouvé en JVM sur
  `BlockingProjectionRefresher`, jamais sur le service lui-même. L'essai 3 est la seule
  vérification de bout en bout.
