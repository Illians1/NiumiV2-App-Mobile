# Étape 18 — `HandleValidNfcUseCase`, libération atomique, reprise de `RELEASING`, notification d'attente de scan

**Date :** 2026-09-14. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 18.

**Produit :** le scan du boîtier termine réellement une session. `HandleValidNfcUseCase` réalise les
points 1 à 4 de SPEC_ANDROID §11.3 — éligibilité de l'état, réconciliation avant scan, vérification
du boîtier **figé dans la session**, envoi de `VALID_NFC_SCANNED` porteur de la preuve — puis laisse
le coordinateur faire les points 5 à 14. `SessionReconciler` reprend un nettoyage interrompu et
republie la demande de scan. Les deux implémentations provisoires de `NfcScanHandler` disparaissent
au profit d'une liaison unique.

**État avant cette étape :** en release, un scan pendant la sonnerie ne faisait **rien**.
`PendingNfcScanHandler` (écran 9) journalisait et rendait `Ignored` ; `PocNfcScanHandler` (écran de
réveil, debug seulement) coupait le son d'une session fictive sans toucher à la persistance. La
seule issue réelle était de forcer l'arrêt de l'application.

---

## Arbitrages validés avec l'utilisateur avant implémentation

Trois écarts au plan, tous sur des points que les specs laissent libres : §11.3 ne fixe ni le module
d'accueil du cas d'usage, ni la forme des liaisons Hilt, ni la liste des raisons de réconciliation.

1. **`HandleValidNfcUseCase` vit dans `:core:system`** (`com.niumi.system.nfc`), et non dans
   `:feature:ringing/nfc/` comme l'écrivait le plan. Il n'a aucune dépendance à la sonnerie — façade
   commune, passerelle de persistance, coordinateur et horloge vivent tous dans `:core:system` — et
   ses deux consommateurs sont dans deux features distinctes (`AlarmActivity` dans
   `:feature:ringing`, `ScanToModifyViewModel` dans `:feature:session`). Le placer dans une feature
   aurait fait dépendre logiquement l'autre d'elle. Même raisonnement et même précédent qu'à
   l'étape 17 pour `AlarmTriggerHandler`. Sa liaison rejoint `NfcHandlerModule` et non
   `SessionModule`, déjà à onze fonctions — plafond `TooManyFunctions` de detekt.
2. **Une seule liaison `NfcScanHandler`**, non qualifiée et non optionnelle. `@BindsOptionalOf` et
   `@SessionNfcScanHandler` n'existaient que parce que deux implémentations concurrentes
   coexistaient, dont une absente de `main` en release. Les deux raisons disparaissent ensemble ;
   les garder aurait laissé un `Optional` qui ne peut plus être vide et un qualificatif à candidat
   unique, morts jusqu'à l'étape 21.
3. **La reprise ne filtre sur aucune `ReconcileReason`.** Le plan la limitait à `PROCESS_START`,
   `USER_UNLOCKED` et `SERVICE_RECREATED` ; or `SystemEventsReceiver` (étape 19) réconciliera un
   redémarrage sous `BOOT`, et un appareil redémarré pendant le nettoyage serait resté bloqué
   jusqu'à ce que l'utilisateur pense à rouvrir Niumi. La reprise est idempotente par construction
   (le rejeu ne touche que les effets `PENDING`/`FAILED`, et `RELEASE_SUCCEEDED` hors `RELEASING`
   est refusé par le moteur), et c'est déjà le traitement des branches `PREPARING` et `RINGING`.
   Couvert par `everyReasonResumesTheRelease`, qui boucle sur `ReconcileReason.entries`.

## Décisions non couvertes par le plan ni par les specs

1. **Aucun `INVALID_NFC_SCANNED` n'est produit.** SPEC_ANDROID §11.2 ne prescrit, pour un tag Niumi
   non associé, que des effets d'interface (« vibration courte d'erreur, alarme maintenue ») ;
   SPEC_CORE_KMP §5.1 autorise l'événement sans l'exiger d'Android. Le produire ferait avancer la
   révision et republierait le snapshot à chaque tag étranger présenté, sans qu'aucun consommateur
   n'en lise le résultat. Le scan est déjà journalisé `NFC_SCAN_INVALID` par
   `AlarmNfcScanCoordinator` et `ScanToModifyViewModel`. L'événement reste défini dans le contrat
   commun pour iOS.
2. **`BoxVerificationStatus.UNSUPPORTED_VERSION` → `Unreadable`**, avec la mention explicite qu'il
   est inatteignable : `BoxPayloadParser` rejette déjà les versions non prises en charge en amont
   (§11.2), donc aucun payload `VALID` ne peut l'atteindre. Branche défensive, pas un chemin vivant.
   `BOX_MISMATCH` et `TOKEN_MISMATCH` donnent `UnknownBox`.
3. **`PairedBoxCredentialDto.protocolVersion` est repris du payload scanné.**
   `AndroidSessionExtras` ne porte pas ce champ, et `BoxVerifier.statusOf` ne le lit jamais sur le
   credential — il compare la version du *payload* à `SUPPORTED_PROTOCOL_VERSION`. Aucune
   information n'est perdue ; vérifié dans le code avant d'écrire la conversion.
4. **Le cas d'usage s'exécute hors du `Mutex` du coordinateur.** §11.3 écrit « sous le même mutex
   que `AlarmReceiver` et `SessionReconciler` », mais ce mutex est privé à
   `DefaultSessionCoordinator` et non réentrant : aucun appelant ne peut le prendre. Lire puis
   dispatcher est donc le seul motif possible, et c'est déjà celui d'`AlarmTriggerHandler`
   (étape 17). La garantie réelle tient : si la révision bouge entre la lecture et le dispatch, le
   moteur répond `STALE_REVISION` — un scan refusé sans effet de bord, jamais une libération à
   moitié faite. Ce que §11.3 cherche à exclure (deux libérations concurrentes) reste exclu, par la
   sérialisation de `dispatch` lui-même.
5. **`Duplicate` est traité comme `Accepted`.** §11.3 dernier alinéa : « le registre retourne le
   reçu sans rappeler le moteur ». Du point de vue du scan l'événement a bien été pris en compte ;
   répondre `Ignored` ferait vibrer l'appareil comme pour un boîtier étranger. `Rejected` donne
   `Ignored` : rien n'a bougé, et ce n'est pas un mauvais boîtier non plus.
6. **La demande de scan est republiée sans condition** plutôt qu'après un test de présence, comme
   l'écrivait le plan (« si la notification absente »). `present()` est idempotent par identifiant
   de notification — `notify()` remplace en place — et interroger `activeNotifications` aurait
   ajouté une méthode à l'interface pour un résultat identique. `AndroidScanRequestNotifier` pose
   en contrepartie `setOnlyAlertOnce(true)` : sans lui, un canal `IMPORTANCE_HIGH` reproduirait une
   bannière à chaque passe de réconciliation alors que la notification n'a jamais disparu.

## Trois défauts trouvés sur appareil et corrigés

Aucun n'était visible en JVM, et les deux premiers ne l'étaient pas non plus à la lecture du code :
ils naissent de la **composition** de décisions correctes prises à des étapes différentes.

### 1. Une session pouvait devenir impossible à terminer — le plus grave

**Mesuré le 2026-09-14**, service d'accessibilité coupé et heure de réveil dépassée de 18 minutes :
le scan du bon boîtier depuis l'écran 9 ne produisait **rien**. Pas de message, pas de vibration,
session inchangée. Le seul chemin de sortie prévu par le produit (§11.2) devenait inopérant, en
silence.

Contrôle effectué avant de conclure : le tag *étranger* affichait bien « Ce boîtier n'est pas celui
de ta session. » — le Reader Mode fonctionnait donc, et le bon tag était bien lu puis délibérément
ignoré.

Chaîne causale, trois décisions correctes isolément :

1. `reconcileArmed` (étape 12) interrompt la passe si une permission bloquante est perdue, pour ne
   pas reprogrammer une alarme sans droit d'alarme exacte. Cela coupait aussi `reconcileTriggerDelay`,
   donc le `TRIGGER_ELAPSED` de `BEFORE_SCAN`.
2. `NfcReducer` refuse `VALID_NFC_SCANNED` depuis `ARMED` après l'heure (`TRIGGER_ALREADY_ELAPSED`,
   §5.2) — correct : un scan après le réveil ne doit jamais donner `CANCELLED`.
3. `HandleValidNfcUseCase` traduit ce refus en `Ignored`, sans rien afficher.

**Correctif :** la garde ne s'applique plus à `BEFORE_SCAN`. Cette raison n'arme rien — elle
convertit un état périmé pour permettre la sortie, ce que §11.3 exige explicitement (« si la session
est encore `ARMED` après l'heure sans alarme observée, il envoie d'abord `TRIGGER_ELAPSED` »).
C'était donc une violation de §11.3, dans le périmètre de cette étape.

Couvert par `beforeScanStillProducesTriggerElapsedWhenAPermissionIsLost` et
`aLostAccessibilityPermissionStillLetsTheScanEndTheSession` ; la garde reste vérifiée hors
`BEFORE_SCAN` par `aLostPermissionStillHaltsThePassOutsideBeforeScan`.

### 2. Snapshot périmé après un incident — troisième occurrence du même piège

Le correctif 1 laissait le test rouge. `readinessMonitor.evaluate` dispatche `INCIDENT_REPORTED`,
qui incrémente la révision ; `reconcileTriggerDelay` recevait ensuite le snapshot d'entrée et tombait
en `STALE_REVISION`. Le snapshot est désormais relu après la surveillance, comme le fait déjà
`AlarmTriggerHandler` (étape 17).

**C'est la troisième fois que ce motif mord dans ce code** : `SessionReadinessMonitor` à l'étape 17
(essai 7), le rejeu d'outbox plus haut dans ce rapport, et ici. Le patron fautif est toujours le même
— *dispatcher un événement, puis continuer sur le snapshot d'entrée*. À traiter comme un risque connu
dans toute revue future : après un `dispatch`, le snapshot local est périmé.

### 3. L'écran 9 ne savait pas sortir vers « Session terminée »

Révélé par le correctif 1, et mesuré sur appareil : après un scan réussi, l'écran restait bloqué sur
« Scan requis » **alors que la session était terminée**, et rescanner ne faisait plus rien (état
final → `Ignored`).

`ScanToModifyViewModel` ne reconnaissait que `CANCELLED`. Ce n'était pas un oubli de l'étape 15 :
un scan depuis cet écran portait forcément sur une session `ARMED` avant l'heure, donc `CANCELLED`
était la seule issue possible. Le correctif 1 rend atteignable, pour la première fois depuis cet
écran, le chemin `ARMED` après l'heure → `TRIGGER_ELAPSED` → `TRIGGERED_AWAITING_NFC` → `COMPLETED`.

**Correctif :** `isCompleted` dans l'état, `onCompleted` sur la route, `navigateToCompleted()` dans
le graphe — qui dépile comme son équivalent annulé (§15 : ne jamais pouvoir revenir sur une session
finie par Retour). Couvert par `aCompletedSessionLeadsToTheCompletedScreen` et
`aCancelledSessionNeverLeadsToTheCompletedScreen`.

**Diagnostic du symptôme observé par l'utilisateur.** Il a rapporté « deux bips, un changement
d'écran puis un retour à *Scan requis* », en doutant que le second scan soit la cause. Il avait
raison, c'en était la conséquence : le premier scan a réussi et fait passer la session en
`TRIGGERED_AWAITING_NFC`, état dans lequel `MainActivity` ouvre automatiquement l'écran de réveil
(d'où le changement d'écran) ; la libération s'est enchaînée jusqu'à `COMPLETED`, l'écran de réveil
s'est fermé en le ramenant sur l'écran 9 resté dessous ; le second scan portait alors sur une
session déjà finale. Sans son observation de ce détail, le défaut 3 aurait été pris pour un aléa.

## Défaut antérieur corrigé ici

**`SessionReconciler` perdait le second incident d'une même passe de rejeu d'outbox.** La boucle
construisait chaque `INCIDENT_REPORTED` sur le snapshot d'entrée ; comme `IncidentReducer`
incrémente la révision, le second partait avec une révision déjà consommée et tombait en
`STALE_REVISION`. C'est exactement la classe de défaut trouvée par l'essai 7 de l'étape 17, corrigée
alors dans `SessionReadinessMonitor` mais pas dans le rejeu d'outbox. Le correctif reprend le patron
déjà correct de `DefaultSessionCoordinator.dispatchCollectedIncidents` : le snapshot est enchaîné
d'un incident au suivant, et la suite de la passe travaille sur le dernier connu.

Il fallait le corriger ici, et pas plus tard : la branche `RELEASING` dispatche `RELEASE_SUCCEEDED`
juste après le rejeu, donc sur une révision qu'un incident peut avoir fait avancer. Couvert par
`twoIncidentsInOneReplayBothReachTheEngine`.

## Régression assumée de la route debug

`PocNfcScanHandler` était le **seul** moyen d'arrêter la sonnerie lancée depuis `PocScreen` :
`PocViewModel` n'expose aucun contrôle d'arrêt. Après cette étape, une alarme POC n'est plus
arrêtable que par un arrêt forcé de l'application — `HandleValidNfcUseCase` retourne `Ignored`,
cette session fictive n'existant pas en base.

Assumé plutôt que rafistolé : ajouter un arrêt direct du service contredirait §11.3 et le critère de
clôture de l'étape 17 (« aucun composant ne démarre ni n'arrête la sonnerie en contournant le
moteur »). La route POC devient redondante dès lors que le parcours réel fonctionne de bout en
bout ; elle disparaît à l'étape 21. Consigné dans `PocSession.kt` et `PocNfcBindingsModule.kt`.

## Fichiers

**Créés (`:core:system`)**

- `main/kotlin/com/niumi/system/nfc/HandleValidNfcUseCase.kt`
- `main/kotlin/com/niumi/system/nfc/NfcScanEventFactory.kt` — séparée de `SessionEventFactory` :
  `VALID_NFC_SCANNED` est le seul événement dont l'identité doit être frappée **avant** l'événement,
  la preuve liant `eventId`, `sessionId` et `expectedRevision` ; et `SessionEventFactory` est déjà à
  onze fonctions, plafond `TooManyFunctions` en classe comme en fichier.
- `test/kotlin/com/niumi/system/nfc/HandleValidNfcUseCaseTest.kt` (16 tests)
- `test/kotlin/com/niumi/system/session/SessionReconcilerReleasingTest.kt` (10 tests)
- `test/kotlin/com/niumi/system/session/fakes/NfcScanFixtures.kt` — URI canoniques réelles et
  `persistSession`. Les empreintes ne sont jamais écrites à la main : elles sont dérivées du payload
  par `PairedBoxCredentialDto.fromPayload`, comme à l'association réelle. Un hash codé en dur se
  serait désynchronisé en silence du parseur.

**Modifiés (`:core:system`)**

- `session/SessionReconciler.kt` — rejeu d'outbox extrait en `replayOutbox` (correctif ci-dessus),
  branches `RELEASING` (`resumeRelease`) et `AWAITING_NFC`/`TRIGGERED_AWAITING_NFC`
  (`republishScanRequest`).
- `session/ReconcileResult.kt` — `ReleaseStillPending(effectCount)` et `ScanRequestRepublished`.
- `session/SessionReconciler.kt` — **correctifs 1 et 2** : la garde de permission épargne
  `BEFORE_SCAN`, et le snapshot est relu après la surveillance de §13.1.
- `session/ReconcilerSources.kt`, `session/di/SessionModule.kt` — sixième source,
  `ScanRequestNotifier`.
- `nfc/di/NfcModule.kt` — liaison unique `NfcScanHandler → HandleValidNfcUseCase`.
- `notification/AndroidScanRequestNotifier.kt` — `setOnlyAlertOnce(true)`.
- `test/…/fakes/TestCoordinatorHarness.kt`, `androidTest/…/AndroidScanRequestNotifierInstrumentedTest.kt`.

**Modifiés (features)**

- `:feature:ringing` — `AlarmActivity` injecte `NfcScanHandler` au lieu d'`Optional<NfcScanHandler>` ;
  `AlarmNfcScanCoordinator.handleUri` prend un handler non nullable (la garde de réentrance reste :
  un tag posé émet plusieurs lectures) ; `AlarmNfcScanCoordinatorTest` remplace le cas « handler
  absent » par le cas `Ignored`.
- `:feature:session` — `ScanToModifyViewModel` perd le qualificatif ; commentaires de
  `ScanOutcome.Ignored` corrigés (il signifie désormais « état non éligible », plus « pas encore
  implémenté »). **Correctif 3** : `isCompleted` dans l'état, `onCompleted` sur la route.
- `:app` — `NiumiNavHost` câble `onCompleted` sur `navigateToCompleted()`, qui dépile comme son
  équivalent annulé (§15).

**Supprimés**

- `:core:system` — `nfc/PendingNfcScanHandler.kt` (classe **et** qualificatif
  `SessionNfcScanHandler`).
- `:app` (debug) — `poc/PocNfcScanHandler.kt`, `testDebug/…/PocNfcScanHandlerTest.kt`, liaison
  `bindNfcScanHandler` de `PocNfcBindingsModule`.

## Point du plan déjà acquis

Le plan demandait « modifier `AndroidScanRequestNotifier` : le tap ouvre `AlarmActivity` avec
`extra mode = SCAN` ; `AlarmActivity` en mode scan n'attend aucun audio ». **Les deux points
étaient déjà satisfaits** et n'appelaient aucun code :

- `ScanRequestPendingIntentSpecs.tap()` pose `mode = "scan"` et cible `ALARM_ACTIVITY` depuis
  l'étape 11, avec un code de requête distinct du plein écran de la sonnerie
  (`ScanRequestPendingIntentSpecsTest`).
- Depuis l'étape 17, `AlarmActivity` est piloté par `SessionStateDto` via `AlarmViewModel`, dont le
  constructeur ne prend que la passerelle de persistance et le publisher : aucune dépendance audio
  n'existe, donc aucune attente d'audio n'est possible. `AlarmScreenState` porte déjà les textes de
  `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC`, et `launcherDestinationFor` envoie les quatre états de
  scan vers l'écran 8.

Réécrire du code correct aurait été le seul risque ici ; le point est vérifié et consigné plutôt que
réimplémenté.

## Vérifications exécutées

Toutes le 2026-09-14, `JAVA_HOME` sur openjdk@17 (Homebrew).

| Commande | Résultat |
| --- | --- |
| `:shared:core:jvmTest` | 160 tests verts |
| `:core:database:testDebugUnitTest` | 106 tests verts |
| `:core:system:testDebugUnitTest` | **246 tests verts (+29)** |
| `:feature:setup:testDebugUnitTest` | 76 tests verts |
| `:feature:session:testDebugUnitTest` | **123 tests verts (+2)** |
| `:feature:ringing:testDebugUnitTest` | 38 tests verts |
| `:app:testDebugUnitTest` | 27 tests verts (−4 : `PocNfcScanHandlerTest` supprimé) |
| **Total JVM** | **776 tests, 0 échec** |
| `:app:assembleDebug` | vert — le graphe Dagger complet résout la liaison unique |
| `ktlintCheck`, `detekt` | verts |
| `:app:lintDebug` | vert, **aucune remontée** |

**Grep de clôture :** plus aucune occurrence de `PendingNfcScanHandler`, `PocNfcScanHandler`,
`SessionNfcScanHandler` ni `@BindsOptionalOf` hors des commentaires d'historique de `NfcModule.kt`.
`HandleValidNfcUseCase` est la seule implémentation de `NfcScanHandler` du dépôt.

## Specs mises à jour

**SPEC_ANDROID §10.5**, sur un constat mesuré sur appareil : `setOngoing(true)` ne rend plus la
notification d'attente de scan non-écartable depuis Android 14 ; la republication par
`SessionReconciler` est le filet réel, et elle n'a lieu qu'à une réconciliation. `setOnlyAlertOnce(true)`
est ajouté à la liste des attributs du canal. Voir « Écart de spec » ci-dessus. La décision de
comportement, elle, est renvoyée à l'étape 19 avec une mesure à l'appui.

Rien d'autre. Les trois arbitrages et les six décisions d'implémentation portent tous sur des points
que §10.5 et §11.3 laissent libres. Le seul écart de lettre — « sous le même mutex » (décision 4) — est
une impossibilité structurelle déjà actée à l'étape 17 pour `AlarmTriggerHandler`, sans changement
de comportement observable ; si une future étape devait rendre le verrou partageable, c'est là que
la spec mériterait une précision.

## Validation sur appareil réel — tests instrumentés faits, protocole manuel **NON FAIT**

**Appareil :** Xiaomi 25080RABDG, Android 16, HyperOS OS3.0.302.0.WPPEUXM (le même qu'à l'étape 17).

### Tests instrumentés — **126 verts, 0 échec** (2026-09-14)

| Module | Tests |
| --- | --- |
| `:core:database` | 60 |
| `:feature:setup` | 34 |
| `:core:system` | 23 — dont les 5 d'`AndroidScanRequestNotifierInstrumentedTest` |
| `:feature:ringing` | 6 |
| `:feature:session` | 2 |
| `:app` | 1 — `AlarmChainInstrumentedTest` |

Les deux tests ajoutés par cette étape sont passés sur l'appareil :
`presentHasNoFullScreenIntentButKeepsATapIntent` (la `Notification` réelle n'a pas de
`fullScreenIntent` et garde son `contentIntent`) et `republishingDoesNotStackNotificationsNorReAlert`
(deux `present()` laissent une seule notification, porteuse de `FLAG_ONLY_ALERT_ONCE`).

**Ces 126 tests ont été rejoués après les trois correctifs** (voir plus bas) et non seulement avant :
les correctifs touchent `SessionReconciler`, `ScanToModifyViewModel`, `ScanToModifyScreen` et
`NiumiNavHost`, tous couverts par des tests instrumentés. Une passe antérieure aux correctifs
n'aurait rien prouvé.

**Incident d'exécution récurrent, sans rapport avec le code.** HyperOS demande une confirmation
manuelle à l'écran pour chaque installation d'APK de test, et la fenêtre expire : trois passes sur
quatre ont échoué partiellement (`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`),
laissant un ou plusieurs modules à **0 test**. `adb install` du même APK réussit immédiatement après,
et relancer les modules concernés suffit.

**Piège de méthode à retenir pour les étapes suivantes — il a failli jouer deux fois aujourd'hui :**
un module instrumenté à 0 test **ressemble à un succès dans un total agrégé** (« 0 échec »). Une
passe a même rendu 0 test sur les six modules tout en affichant `0 echecs`. Ne jamais additionner
sans vérifier le compte **par module**, et ne jamais compter un module à 0 comme vert. Le script de
dépouillement utilisé ici marque explicitement les modules à zéro.

### Protocole manuel — **en cours** (4 essais sur 8 validés)

Déroulé le 2026-09-14 sur le même appareil. **Préalable imprévu :** `connectedDebugAndroidTest`
désinstalle les APK en fin de passe — l'application **et toutes ses données** disparaissent
(boîtier associé, sélection d'applications, journal). Il faut réinstaller `app-debug.apk` et
refaire la configuration avant tout essai manuel. À prévoir aux étapes suivantes : lancer les tests
instrumentés **avant** le protocole manuel, jamais entre deux essais.

**Second préalable, propre à HyperOS :** `settings put secure enabled_accessibility_services` est
**annulé par le système dans la seconde**. Le service d'accessibilité ne peut être activé que par
l'utilisateur, via l'écran de consentement de l'application. Le contrôle « Service d'accessibilité
actif » du diagnostic l'a correctement rapporté comme en échec pendant cette tentative — bonne
nouvelle incidente pour `AndroidAccessibilityServiceStatus`.

**Trois faits de plateforme mesurés, utiles aux étapes suivantes :**

| Action | Effet observé sur HyperOS |
| --- | --- |
| `am force-stop com.niumi.app` | annule les alarmes de l'app **et** désactive son service d'accessibilité |
| redémarrage de l'appareil | efface les alarmes `AlarmManager`, mais **ne** désactive **pas** l'accessibilité |
| service d'accessibilité actif | relance le processus Niumi au démarrage du système, donc déclenche une réconciliation |

La dernière ligne a une conséquence directe : tant que l'accessibilité est active, Niumi revient à
la vie après un redémarrage sans attendre `SystemEventsReceiver` (étape 19). C'est ce qui a produit
le « réveil rattrapé » décrit plus bas, et ce qui rend l'état « réveil manqué » difficile à
provoquer — il faut couper l'app **et** son service d'accessibilité pour l'atteindre.

#### Essai 2 — scan avant l'heure → annulation ✅

Session armée pour le lendemain 07:00. Alarme vérifiée avant le scan : `RTC_WAKEUP`,
`origWhen=2026-09-15 07:00:00.000`, `exactAllowReason=policy_permission`, bloc `Alarm clock:
triggerTime` présent (donc bien `setAlarmClock`, §9.1) et `Next wake from idle` (exemptée de Doze).
Blocage vérifié actif : Adobe Acrobat renvoyée au lanceur à chaque ouverture.

Après le scan : écran « Session annulée — Tes applications sont débloquées. », **zéro alarme Niumi
en attente** (`Reason=alarm_cancelled` à 11:37:32 dans l'historique), et Adobe Acrobat s'ouvre
normalement. Les trois effets requis ont donc bien été exécutés.

#### Essais 3, 4 et 1 — dans une même sonnerie ✅

Session à 11:45, déclenchement observé à l'heure (`AlarmRingingService` démarré, `AlarmActivity` au
premier plan).

| Essai | Tag | Observé |
| --- | --- | --- |
| 3 | autre boîtier Niumi (`7c9e6a41…`) | « Ce boîtier n'est pas celui de ta session. », sonnerie maintenue |
| 4 | tag sans NDEF | « Boîtier non reconnu. Réessaie. », sonnerie maintenue |
| 1 | boîtier de la session (`14bb2dc6…`) | son coupé, blocage levé, `COMPLETED` |

**Le journal technique confirme objectivement la non-régression cherchée** (ordre
antichronologique) : `NFC_SCAN_VALID`, `SESSION_COMPLETED`, `SESSION_RELEASING`,
`NFC_SCAN_INVALID`, `NFC_SCAN_INVALID`, `RINGING_STARTED`, `RINGING_STARTED`, `ALARM_RECEIVED`.
Les deux mauvais scans n'ont produit **que** `NFC_SCAN_INVALID` — aucune transition d'état entre eux
et `SESSION_RELEASING`. C'est exactement ce que garantit `HandleValidNfcUseCase` en ne dispatchant
rien quand la vérification échoue.

Diagnostic après l'essai 1 : session `COMPLETED`, santé `Normale`, **aucun incident**.

**Deux doublons du journal examinés, tous deux légitimes** — vérifiés dans le code plutôt que
supposés :

- `RINGING_STARTED` deux fois pour un seul `ALARM_RECEIVED` : `StartRingingExecutor` consigne que le
  moteur a demandé la sonnerie, `AlarmRingingService.onStartCommand` que le service a effectivement
  démarré. Deux faits distincts, deux composants ; le démarrage audio lui-même est idempotent.
- `BLOCK_APPLIED` deux fois sur la session de l'essai 2 : l'exécuteur d'activation, puis
  `NiumiBlockingAccessibilityService` lors de l'interception réelle d'Adobe Acrobat au test de
  blocage. Le second est la preuve du blocage, pas un doublon.

**Écran 10 confirmé lors d'une session ultérieure** (voir « réveil rattrapé » ci-dessous) : après un
scan valide depuis l'écran de réveil, l'utilisateur arrive bien sur « Session terminée » avec un
bouton de retour à l'accueil. La redirection `AlarmActivity` → `MainActivity` + extra
`DESTINATION_SESSION_COMPLETED` fonctionne.

**Faux positif écarté :** un `adb shell am start -n com.niumi.app/.MainActivity` lancé manuellement
après la fin d'une session affiche l'écran 7 (« Ta session est active ») avec le libellé « Session
terminée » et le bouton « Modifier ou annuler ». Ce n'est **pas** un défaut : `MainActivity` est
`singleTop` et ne renavigue que sur un extra de destination ou sur un état de scan ; un `am start`
sans extra ramène simplement la tâche au premier plan, le `NavHost` restant où il était. Le chemin
réel passe toujours par l'extra. Vérifié dans `homeDestinationFor` (qui renvoie bien `Readiness`
pour `COMPLETED`) et confirmé par le comportement observé.

**Non observée :** la vibration courte du seul essai 3. L'alarme vibrait en continu, l'utilisateur
n'a pas pu distinguer les deux. Le journal d'alimentation montre bien un `REL` puis `ACQ *vibrator*`
à 11:45:07.83, cohérent avec une vibration d'erreur intercalée, mais ce n'est pas une preuve
formelle. Couvert en JVM par `AlarmNfcScanCoordinatorTest`.

#### Résultat non programmé : le réveil est rattrapé après un redémarrage ✅

En préparant les essais 6-8, une session a été armée pour 12:30 puis l'appareil redémarré à
12:28:53 — le but étant de faire **disparaître** l'alarme (Android efface les alarmes `AlarmManager`
au redémarrage, et `SystemEventsReceiver` n'arrive qu'à l'étape 19). Vérifié juste après le boot
(12:29:37) : **zéro alarme Niumi en attente**.

**Elle a pourtant sonné à 12:29:59.** Le service d'accessibilité relance le processus Niumi dès le
démarrage du système ; la réconciliation de démarrage a constaté que l'heure contractuelle n'était
pas encore atteinte (`TriggerDelayOutcomeDto.NOT_REACHED`) et a **reprogrammé l'alarme**
(`SessionReconciler.rescheduleAlarm`). `dumpsys alarm` confirme : `1 wakeups, 1 alarms`.

C'est un comportement de §9.3 qui n'était pas au programme de cette étape et qui se trouve validé
sur appareil : **un redémarrage entre l'armement et l'heure de réveil ne fait pas manquer le
réveil**. À signaler à l'étape 19, qui livrera `SystemEventsReceiver` : le chemin de rattrapage
existe déjà par un autre biais, et le receiver le rendra indépendant du service d'accessibilité.

#### Essai 5 — mort du processus pendant `RELEASING` : **NON REPRODUCTIBLE sur cet appareil**

**Tenté, raté, reporté.** Un guetteur tournant sur l'appareil surveillait la disparition
d'`AlarmRingingService` (effet `STOP_RINGING`) pour couper le processus avant `REMOVE_BLOCKING`.
Mesures : service disparu à 12:05:33.113, `am force-stop` à 12:05:33.281 — **168 ms de latence**.
Trop tard : le journal montre `SESSION_RELEASING` puis `SESSION_COMPLETED` avant le kill.

**Preuve fine de l'instant du kill :** `NFC_SCAN_VALID` est **absent** de cette session, alors qu'il
figure dans les deux précédentes. Il est journalisé par `AlarmNfcScanCoordinator` **après** le
retour de `onUriRead`. Le processus a donc été tué à l'intérieur du traitement du scan, mais après
que la libération complète eut été persistée. La fenêtre `RELEASING` réelle est inférieure à 168 ms.

**Ce que l'essai a tout de même établi :**

- **Le nettoyage complet tient en moins de 168 ms** — quatre effets, `RELEASE_SUCCEEDED` et
  `COMPLETED` compris. Le critère §21 « arrêt et déblocage en moins d'une seconde » est largement
  tenu, et c'est désormais mesuré et non estimé.
- L'écran de nettoyage de `RELEASING` s'affiche réellement : l'utilisateur a lu ses trois lignes
  (« Réveil annulé », « Sonnerie arrêtée », « Applications débloquées »), livrées à l'étape 17.
- **`am force-stop` désactive le service d'accessibilité sur HyperOS** (`accessibility_enabled`
  repasse à 0, aucun service lié). À retenir pour toutes les étapes suivantes : un test qui tue le
  processus invalide le blocage par effet de bord, et le service doit être réactivé à la main avant
  la session suivante.
- `SESSION_READINESS_DEGRADED` et `ACCESSIBILITY_DISABLED` apparaissent au redémarrage, conséquence
  correcte du point précédent ; la session étant finale, aucun incident n'est créé.

**Dette de validation, à lever avant la porte finale.** La branche `resumeRelease` reste prouvée
par les seuls tests JVM (`SessionReconcilerReleasingTest`, 3 cas dédiés sur 10). Ce qui n'est pas
démontré sur appareil : que Room restitue un `RELEASING` avec son outbox après une vraie mort de
processus, et que la réconciliation au démarrage le reprend. **Décision validée avec l'utilisateur
le 2026-09-14 : reporté à l'étape 20** (« mort du processus »), qui devra fournir un moyen de
provoquer cet état — la fenêtre est trop courte pour être attrapée par un polling adb.

Risque résiduel jugé **modéré** : la mécanique Room/outbox est prouvée depuis les étapes 9 à 11, et
la reprise au démarrage est déjà validée sur appareil pour `PREPARING` (étape 14) et `RINGING`
(étape 17). Ce qui est neuf ici est une décision de logique pure, bien couverte en JVM. Ce n'est pas
une raison de la compter comme validée.

#### Essais 6, 7 et 8 — notification d'attente de scan ✅

**Mise en scène.** L'état `TRIGGERED_AWAITING_NFC` est difficile à provoquer, précisément parce que
le système est conçu pour l'éviter (voir « réveil rattrapé »). Séquence retenue : armer une session à
+2 min, `am force-stop` (qui annule l'alarme **et** coupe l'accessibilité, donc rien ne relance
l'app), attendre 16 minutes pour dépasser la fenêtre de grâce, puis **réactiver l'accessibilité** —
ce qui relance le processus et déclenche la réconciliation avec la permission présente.

**Essai 6 — la notification.** Publiée comme attendu. Caractéristiques réelles lues dans
`dumpsys notification`, toutes conformes à §10.5 :

```
channel=niumi_session_awaiting_scan   category=alarm
sound=null   vibrate=null
flags=ONGOING_EVENT|ONLY_ALERT_ONCE   (aucun fullScreenIntent)
```

Absence de son et de vibration confirmée à la fois par le système et par l'utilisateur. Service de
sonnerie à 0 et alarme à 0 : rien ne simule une alarme active hors de sa fenêtre. `ONLY_ALERT_ONCE`
est l'attribut ajouté par cette étape.

**Essai 7 — la republication.** Deux constats.

1. **`setOngoing(true)` n'empêche plus le balayage.** L'utilisateur a pu écarter la notification d'un
   geste ; vérification faite, plus aucune notification Niumi n'était active. Depuis Android 14,
   `FLAG_ONGOING_EVENT` seul ne rend plus une notification non-écartable — il y faut un service de
   premier plan actif, ce que la notification d'attente de scan n'a pas (et ne doit pas avoir).
   **§10.5 prescrit `setOngoing(true)` en supposant une garantie que la plateforme n'offre plus.**
   L'implémentation respecte la lettre de la spec ; c'est l'effet escompté qui a disparu. Voir
   « Écart de spec à trancher » ci-dessous.
2. **La republication fonctionne**, et devient de ce fait nécessaire plutôt que prudente. Après un
   vrai démarrage de processus, `republishScanRequest` la remet : `id=2`,
   `flags=ONGOING_EVENT|ONLY_ALERT_ONCE`, **une seule instance**, aucun empilement.

   **Nuance mesurée :** la republication n'a lieu qu'à une *réconciliation* (démarrage de processus,
   redémarrage), pas à chaque passage au premier plan — `MainActivity` n'appelle à `onResume` que la
   surveillance de §13.1, jamais `reconcile()`. Entre un balayage et le prochain démarrage de
   processus, l'utilisateur n'a donc plus de rappel. Le chemin de sortie reste néanmoins atteignable :
   ouvrir Niumi dans un état de scan ouvre directement l'écran de réveil (§10.4).

**Essai 8 — scan depuis la notification.** Les trois gestes ont fonctionné : le tap ouvre l'écran de
scan (§10.5, dernier point), le scan termine la session, et la notification est retirée
(`CLEAR_SCAN_REQUEST`). Vérifié après coup : aucune notification Niumi active, alarme à 0, service à 0.

#### Non-régression après les trois correctifs ✅

L'essai 2 a été rejoué avec la version corrigée : session armée pour le lendemain 07:00, scan depuis
l'écran 9 → « Session annulée », **0 alarme en attente**, Adobe Acrobat de nouveau ouvrable. Le chemin
nominal `ARMED` avant l'heure → `CANCELLED` est intact, y compris la sortie vers l'écran 11 — c'est
précisément le code touché par le correctif 3.

## Écart de spec à trancher

**§10.5 — `setOngoing(true)` ne protège plus la notification d'attente de scan.** La spec la veut
persistante pour qu'elle ne puisse pas être écartée d'un geste distrait tant que les applications
sont bloquées. Sur Android 14+, cet attribut seul ne suffit plus.

Options, à arbitrer avec l'utilisateur avant la porte finale :

1. **Accepter et documenter** : la republication à chaque réconciliation devient le filet, et l'écran
   de réveil reste atteignable en ouvrant Niumi. Coût : une fenêtre sans rappel visible entre un
   balayage et le prochain démarrage de processus.
2. **Republier plus souvent** : appeler `reconcile()` au passage au premier plan quand l'état attend
   un scan. Referme la fenêtre, au prix d'une passe de réconciliation supplémentaire.
3. **Adosser la notification à un service de premier plan**, comme la sonnerie. La rend réellement
   non-écartable, mais fait tourner un service pendant une attente qui peut durer des heures — et
   §10.5 insiste pour que cette notification ne ressemble pas à une alarme active.

**Décidé avec l'utilisateur le 2026-09-14 :**

- **§10.5 est corrigée dans ce changement** — constat factuel, indépendant du choix technique.
  L'attribut `setOngoing(true)` reste prescrit (il conserve son effet sur les versions antérieures et
  sur le classement de la notification), mais la spec dit désormais qu'il ne suffit plus sur
  Android 14+, que le filet réel est la republication, et que celle-ci n'a lieu qu'à une
  réconciliation. `setOnlyAlertOnce(true)` y est ajouté à la liste des attributs, avec sa raison.
  Sans cette correction, un lecteur en tirerait une garantie fausse.
- **Le comportement se tranche à l'étape 19**, avec une mesure et non au jugé : une fois
  `SystemEventsReceiver` livré, rejouer l'essai 7 et mesurer la durée réelle de la fenêtre sans
  rappel. L'encadré en tête de l'étape 19 du plan porte la marche à suivre et une case de suivi.

**Proportion, réévaluée après coup :** l'écart dégrade un confort, il ne bloque rien. Aucun chemin de
sortie n'est interrompu — l'overlay de §12.2 rappelle le scan au moment exact où l'utilisateur
rencontre une application bloquée, et ouvrir Niumi dans un état de scan mène directement à l'écran de
réveil. Le rapport initial le présentait comme plus urgent qu'il ne l'est.

**Spec modifiée dans ce changement :** SPEC_ANDROID §10.5 (constat `setOngoing` + `setOnlyAlertOnce`).
Aucune autre.

| Essai | Action | Résultat attendu |
| --- | --- | --- |
| 1 | Session réelle à +2 min, laisser sonner, scanner le bon tag | Son coupé et applications débloquées en moins d'une seconde, écran « Session terminée » |
| 2 | Session armée, écran 9, scanner le bon tag avant l'heure | « Session annulée », alarme déprogrammée, blocage retiré |
| 3 | Pendant la sonnerie, scanner un autre tag Niumi | Vibration courte, sonnerie maintenue, « Ce boîtier n'est pas celui de ta session. » |
| 4 | Pendant la sonnerie, approcher un tag non NDEF | « Boîtier non reconnu. Réessaie. », sonnerie maintenue |
| 5 | `adb shell am force-stop com.niumi.app` juste après un scan valide, puis relancer | Nettoyage repris, état final atteint, **aucun blocage réappliqué** |
| 6 | Laisser passer l'heure sans interagir (au-delà de la fenêtre de 15 min) | Notification « Ton réveil Niumi est passé », **sans son ni plein écran** ; le tap ouvre l'écran de scan |
| 7 | Balayer la notification d'attente de scan, puis redémarrer l'appareil | Notification republiée, sans ré-alerte ni doublon |
| 8 | Scan valide depuis l'écran ouvert par la notification | Session terminée, notification retirée |

Points de vigilance à ne pas présumer acquis : le comportement de `CATEGORY_ALARM` sans son sous Ne
pas déranger varie selon la surcouche OEM (§10.5, matrice §20) ; l'essai 5 dépend de la politique de
relance du constructeur — HyperOS n'a rejoué aucun `START_STICKY` à l'étape 17 ; l'essai 7 dépend de
l'étape 19 pour le chemin `BOOT` réel (`SystemEventsReceiver` n'existe pas encore), il se teste
d'ici là par une relance de l'application.

## Constats hors périmètre, toujours ouverts

- **Réserve de l'étape 17 non traitée :** rien ne ranime la sonnerie si la plateforme ne relance pas
  le service après une mort de processus. `SessionReconciler.resumeRinging` couvre le cas où le
  processus revient ; le faire revenir relève de l'étape 20 (alarme de secours). À trancher avant la
  porte finale.
- **`AWAITING_NFC` reste inatteignable en parcours Android normal** (§7.1, point de vigilance 7 du
  plan) : la branche est implémentée et testée par injection d'état, pas par un parcours réel.
