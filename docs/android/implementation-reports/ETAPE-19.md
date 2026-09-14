# Étape 19 — `SystemEventsReceiver`, coordinateur Direct Boot, politique de retard et fusion après déverrouillage

**Date :** 2026-09-14. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 19.

**Produit :** un redémarrage ne fait plus manquer le réveil par accident. `SystemEventsReceiver`
traduit cinq broadcasts système en réconciliations, `SystemEventsRegistrar` en enregistre un sixième
à chaud, et `DirectBootMerger` fait entrer dans Room ce qui a été décidé avant le premier
déverrouillage.

**État avant cette étape.** `ReconcileReason` portait déjà ses neuf valeurs, mais **six n'étaient
jamais produites** faute de receveur. Le rattrapage observé à l'étape 18 — session armée à 12:30,
redémarrage à 12:28:53, alarme sonnée à 12:29:59 — ne tenait qu'à un effet de bord : le service
d'accessibilité relance le processus au démarrage du système, et la réconciliation de démarrage
reprogrammait l'alarme. Ce chemin disparaît si l'utilisateur n'a pas activé l'accessibilité, ou si la
surcouche retarde la relance. Par ailleurs, les décisions prises avant déverrouillage ne vivaient que
dans le fichier Direct Boot : `UnlockAwarePersistenceGateway` renvoyait explicitement la fusion à
cette étape.

---

## Ce qui existait déjà et n'a pas été réécrit

L'analyse préalable a montré que la moitié du travail listé par le plan était livrée depuis
l'étape 11 ou 17 : la politique de retard (`SessionReconciler.reconcileTriggerDelay`, lue via
`NiumiCoreFacade.evaluateTriggerDelay`), la reprogrammation au même instant (`rescheduleAlarm`),
`TRIGGER_ELAPSED` + `MISSED_TRIGGER_WINDOW`, la republication de `PRESENT_SCAN_REQUEST`, l'écriture
atomique et la garde de révision Direct Boot, et la permission `RECEIVE_BOOT_COMPLETED` — déclarée
depuis l'étape 3 et jusqu'ici inutilisée. Le travail réel portait sur quatre points : produire les
raisons, publier depuis un contexte protégé, traiter les changements d'horloge, et fusionner.

## Arbitrages validés avec l'utilisateur avant implémentation

### 1. `USER_UNLOCKED` n'est pas délivrable à un receveur de manifeste — écart de spec

SPEC_ANDROID §9.3 listait `USER_UNLOCKED` parmi les filtres du `SystemEventsReceiver`. **Android ne
délivre `ACTION_USER_UNLOCKED` qu'aux receveurs enregistrés à chaud.** La documentation Direct Boot
officielle demande d'« enregistrer un `BroadcastReceiver` depuis un composant qui tourne ». Déclaré
dans le manifeste, le filtre aurait compilé, se serait installé, et ne se serait jamais déclenché :
la fusion Direct Boot → Room n'aurait jamais eu lieu par ce chemin.

**Retenu :** cinq actions au manifeste, `USER_UNLOCKED` enregistré à chaud par
`SystemEventsRegistrar` depuis `NiumiApplication.onCreate` (`RECEIVER_NOT_EXPORTED`, même patron que
`SessionReadinessWatcher`). Filet : la fusion est aussi tentée sur `BOOT` et `PROCESS_START`, pour le
cas où le processus n'était pas vivant à l'instant du déverrouillage. Elle est idempotente et ne
coûte qu'une lecture de fichier quand il n'y a rien à absorber.

Vérifié au passage que les cinq actions du manifeste sont bien délivrables : `LOCKED_BOOT_COMPLETED`,
`BOOT_COMPLETED`, `TIME_SET` et `TIMEZONE_CHANGED` figurent dans la liste officielle des exceptions
aux restrictions de broadcasts implicites ; `MY_PACKAGE_REPLACED` est explicitement adressé au paquet
lui-même, donc hors du champ de la restriction.

### 2. Contexte protégé : un seul jeu de liaisons, pas de graphe parallèle

Le plan demandait des dépendances `@Named("deviceProtected")` « avant déverrouillage », impliquant
deux graphes et un aiguillage sur `UnlockState`. Vérification faite, **aucun des adaptateurs visés
n'accède au stockage** : `AndroidAlarmScheduler` n'utilise qu'`AlarmManager`,
`AndroidScanRequestNotifier` qu'un `NotificationManager`, `AndroidRingingController` que
`startForegroundService`. Un contexte protégé par appareil se comporte à l'identique après
déverrouillage pour ces trois-là — seuls les appels de stockage changent de racine.

**Retenu :** un seul qualificatif `@DeviceProtected`, utilisé en permanence. Six classes de
délégation évitées pour un comportement rigoureusement identique. §7.3 reçoit une précision : la
sûreté avant déverrouillage tient au `directBootAware` du composant et à l'absence d'accès au
stockage chiffré par les identifiants, pas au contexte porté par l'adaptateur.

### 3. Un incident de changement d'horloge par code et par session

`android.intent.action.TIME_SET` n'est pas émis seulement quand l'utilisateur change l'heure : chaque
correction d'horloge par le réseau le produit aussi, plusieurs fois par nuit sur certains appareils.
Sans garde, une seule session accumulerait des dizaines d'incidents `WARNING` identiques sur l'écran
7 — exactement le défaut mesuré et corrigé à l'étape 16.

**Retenu :** un incident par code et par session, via le `SessionIncidentsReader` existant.
**L'alarme, elle, est réenregistrée à chaque réception, sans condition** — c'est la partie qui protège
le réveil. Contrepartie assumée : deux changements de fuseau dans la même session ne laissent qu'un
incident ; le journal technique, non dédupliqué, garde chaque détection horodatée.

## Décisions non couvertes par le plan ni par les specs

1. **La fusion vit dans `SessionReconciler`, pas dans `DefaultSessionCoordinator`.** Le plan écrivait
   « modifier `DefaultSessionCoordinator ». Or §9.3 dit littéralement « **le réconciliateur** fusionne
   de façon idempotente le registre et l'outbox Direct Boot dans Room », et le réconciliateur est
   appelé par le coordinateur **pendant qu'il tient son mutex** : la garantie d'atomicité est la même.
   Placée dans le coordinateur, elle poussait son constructeur à sept paramètres et faisait tomber
   `LongParameterList` de detekt ; placée dans le réconciliateur, elle passe par `ReconcilerSources`,
   qui existe précisément pour cela. Deux raisons concordantes, aucune concession.
2. **Aucun événement technique n'est journalisé pour la fusion.** §17 est une liste fermée de 26
   types et aucun ne décrit une fusion de projection. Ce qu'elle a absorbé ressort de toute façon
   dans les événements que la passe suivante produit en rejouant les effets. Même raisonnement que le
   dépassement de fenêtre d'`AlarmReceiver` (étape 17).
3. **Une session absente de Room fait échouer la fusion sans rien écrire ni effacer**
   (`DirectBootMergeResult.UnknownSession`). Les reçus et les effets portent une clé étrangère vers
   `alarm_session` ; rien ne permet de reconstruire la session, une activation ayant toujours lieu
   appareil déverrouillé (§9.2). La projection orpheline reste lisible pour un diagnostic, plutôt que
   supprimée — SPEC_CORE_KMP §13, « aucune suppression silencieuse ».
4. **La réécriture de la projection depuis Room a lieu même quand la fusion est refusée pour révision
   inférieure.** C'est précisément le cas où la projection est en retard et doit être remise à niveau
   (§9.2, « Room fait foi »).
5. **`mirrorActiveSessionToDirectBoot` est extraite dans `:core:database`** plutôt que recopiée :
   elle a désormais deux appelants — chaque écriture de `UnlockAwarePersistenceGateway` après
   déverrouillage, et la fin de la fusion. La recopier aurait laissé deux projections susceptibles de
   diverger.
6. **La reprogrammation est inconditionnelle sur un changement d'horloge**, alors qu'elle reste
   conditionnée à l'absence de `PendingIntent` sur les autres raisons : `isScheduled` ne prouve que
   l'existence de l'intent, jamais que le système l'a conservé au bon instant après avoir déplacé son
   horloge. `FLAG_UPDATE_CURRENT` rend le geste idempotent.
7. **`SystemEventReasons` est un objet séparé du receveur**, testable en JVM. Les constantes
   `Intent.ACTION_*` sont des constantes de compilation Java, donc inlinées : les référencer ne charge
   pas la classe `Intent` et ne demande aucun Robolectric. Même partage des rôles qu'entre
   `AlarmReceiver` et `AlarmTriggerHandler` (étape 17).

## Fichiers

**Créés — `:core:system`**
- `boot/SystemEventsReceiver.kt` — `@AndroidEntryPoint`, `goAsync()` + `withTimeout(8 s)`, coquille vide de décision.
- `boot/SystemEventReasons.kt` — table action → `ReconcileReason`, et la liste des cinq actions du manifeste.
- `boot/SystemEventsRegistrar.kt` — enregistrement à chaud de `USER_UNLOCKED`.
- `boot/DirectBootMerger.kt` — orchestration de la fusion et réécriture de la projection.
- `boot/di/BootModule.kt` — liaisons Hilt (module dédié : `SessionModule` est au plafond detekt).
- `common/DeviceProtected.kt` — qualificatif Hilt.

**Créés — `:core:database`**
- `directboot/DirectBootRoomMerge.kt` — interface, résultats typés, et la transaction Room.
- `directboot/DirectBootMirror.kt` — projection Room → Direct Boot, partagée.

**Modifiés**
- `:core:system` — `AndroidManifest.xml` (premier `<application>` du module), `SessionReconciler` (fusion en tête de passe, incident d'horloge, reprogrammation inconditionnelle), `ReconcilerSources` (+ `directBootMerger`, + `incidentsReader`), `UnlockAwarePersistenceGateway` (délègue au miroir partagé), `SystemModule`, `SystemNotificationModule`, `ScanRequestModule`, `session/di/SessionModule`.
- `:core:database` — `ReceiptDao` (insertion `IGNORE` dédiée à la fusion), `OutboxDao` (lecture de tous les effets d'une session), `di/SessionStoreModule`.
- `:feature:ringing` — `di/RingingModule` (contexte protégé).
- `:app` — `NiumiApplication` (enregistrement du receveur `USER_UNLOCKED`).
- `specs/SPEC_ANDROID.md` — §9.3 et §7.3.

## Tests

**Ajoutés — 25 tests JVM et 10 instrumentés.**

| Test | Module | Ce qu'il prouve |
| --- | --- | --- |
| `SystemEventReasonsTest` (8) | `:core:system` JVM | chaque action → sa raison ; action inconnue ou nulle → aucune réconciliation ; `USER_UNLOCKED` absent de la table ; manifeste et table alignés |
| `SessionReconcilerBootTest` (9) | `:core:system` JVM | les trois bornes de la fenêtre de grâce ; incident `WARNING` sans dégradation de santé ; déduplication sur trois passes ; reprogrammation inconditionnelle ; aucune sonnerie au-delà de la fenêtre |
| `DirectBootMergerTest` (8) | `:core:system` JVM | appareil verrouillé et projection absente → rien ; projection corrompue signalée sans effacement ; réécriture depuis Room y compris sur révision refusée ; idempotence |
| `RoomDirectBootMergeTest` (10) | `:core:database` instrumenté | la transaction : avance de session, refus d'une révision inférieure, reçus dédoublonnés, remontée de statut jamais inversée, session inconnue, survie du journal (pas de CASCADE), champs figés jamais écrasés |

Un `DirectBootInstrumentedTest` distinct n'a pas été créé : `FileDirectBootStoreTest` couvre déjà
l'écriture puis la lecture par un contexte protégé.

## Commandes exécutées

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew test                                      ✅ (dont 43 classes de test sur :core:system)
./gradlew :core:system:testDebugUnitTest
          :core:database:testDebugUnitTest          ✅
./gradlew :app:assembleDebug                        ✅
./gradlew :app:lintDebug                            ✅ aucune remontée
./gradlew ktlintCheck detekt                        ✅
./gradlew :core:database:compileDebugAndroidTestKotlin
          :core:system:compileDebugAndroidTestKotlin
          :app:compileDebugAndroidTestKotlin        ✅
```

Manifeste fusionné vérifié : le receveur y figure avec ses cinq actions, `directBootAware="true"` et
`exported="false"` ; `RECEIVE_BOOT_COMPLETED` présent une fois ; `QUERY_ALL_PACKAGES` toujours
absent.

Cinq remontées de detekt ont été traitées **par restructuration et non par suppression** :
`LongParameterList` sur le coordinateur et le réconciliateur (fusion déplacée, `ReconcilerSources`
étendue), deux `ReturnCount` (gardes fusionnées en une expression court-circuitée), et un
`UnusedPrivateProperty` sur `SystemEventsRegistrar` — detekt ne voyait pas l'usage à l'intérieur
d'une expression d'objet ; extraire `reconcileAsync()` le corrige et aligne la classe sur
`SessionReadinessWatcher`.

---

## Défauts trouvés sur appareil et corrigés dans ce changement

Les deux se déclenchent **par la livraison même de cette étape** : avant `SystemEventsReceiver`,
aucun composant Niumi ne tournait avant le premier déverrouillage hors de la chaîne d'alarme. Les
deux étaient invisibles en JVM et en instrumenté — aucun test ne peut verrouiller un appareil.

### 1. Le contrôle d'accessibilité faisait perdre le réveil au redémarrage

**Le défaut que cette étape devait précisément empêcher.** Premier essai 1, alarme armée pour 20:18,
redémarrage à 20:07:50, aucun déverrouillage. Logs :

```
Start proc 4105:com.niumi.app for broadcast {com.niumi.app/com.niumi.system.boot.SystemEventsReceiver}
AccessibilityManagerService: Ignoring non-encryption-aware service ComponentInfo{com.niumi.app/…}
```

Le receveur démarrait bien, mais **zéro alarme reprogrammée** après 60 s, et une notification Niumi
active : « Le service d'accessibilité de Niumi est désactivé. Tes applications ne sont plus
bloquées. »

Mécanisme, confirmé en lisant les réglages depuis l'appareil encore verrouillé :
`enabled_accessibility_services` contenait bien notre composant, mais **`accessibility_enabled`
valait 0** — Android coupe le drapeau global tant qu'aucun service n'est lié, et il refuse de lier un
service non `directBootAware`. `AndroidAccessibilityServiceStatus.isEnabled()` lit ce drapeau en
premier et renvoyait donc `false`. D'où : contrôle `ACCESSIBILITY_SERVICE` en échec → incident
`BLOCKING_PERMISSION_REVOKED` `CRITICAL` mensonger → et surtout la garde de
`SessionReconciler.reconcileArmed` interrompait la passe **avant `reconcileTriggerDelay`**.

C'est la même garde qui avait rendu le scan impossible à l'étape 18, sur une autre raison.

**Correctif :** le contrôle est `NOT_APPLICABLE` tant que `UserManager.isUserUnlocked` est faux. Le
moniteur de §13.1 ne signale que les contrôles `FAILED`, donc l'incident et l'interruption
disparaissent ensemble. C'est aussi la réponse honnête : le blocage n'a aucun objet avant le
déverrouillage, l'utilisateur ne pouvant atteindre aucune application. `UnlockState` rejoint
`ReadinessSources` pour cela. Couvert par `theAccessibilityCheckIsNotApplicableBeforeTheFirstUnlock`,
`theAccessibilityCheckIsEvaluatedAgainAfterUnlock` et
`lockedBootRescheduleEvenWhileTheAccessibilityServiceCannotRun`.

**Après correctif, essai 1 rejoué :** alarme reprogrammée 26 s après le boot, `RUNNING_LOCKED`,
`origWhen=2026-09-14 20:18:00.000` — instant identique, jamais recalculé — et **zéro notification
Niumi**.

### 2. Un `DataStore` né en Direct Boot reste cassé pour la vie du processus

Découvert parce que l'utilisateur ne pouvait plus choisir ses applications après le redémarrage :
l'écran réclamait une sélection qui existait pourtant sur le disque.

Le processus courant avait 10 min 37 s d'existence, donc était **né pendant la fenêtre Direct Boot**.
Les logs du démarrage montrent `Failed to ensure /data/user/0/com.niumi.app/files: mkdir failed:
errno 126 (Required key not available)`. La réconciliation `LOCKED_BOOT` rejoue le diagnostic de §13,
qui lit `AppSelectionStore` et `SetupPreferences` — deux `DataStore` en stockage chiffré par les
identifiants. **L'instance créée dans cette fenêtre continue de servir un état vide après le
déverrouillage**, pour toute la durée de vie du processus.

Test décisif : `am force-stop` puis relance → la sélection était de nouveau visible, sans que rien
n'ait été ressaisi.

**Portée réelle.** Une session déjà armée n'est pas touchée : son état vit dans Room et dans la
projection Direct Boot, et aucun des six contrôles surveillés pendant `ARMED` ne lit ces `DataStore`.
Le dégât est confiné à la **préparation d'une nouvelle session** depuis un processus né en Direct
Boot — c'est-à-dire, en pratique, le matin suivant un redémarrage nocturne.

**Correctif :** les deux dépôts reçoivent leur `Context` par un `Provider` qui n'est **jamais résolu**
avant déverrouillage — la garde empêche l'instance de naître, elle ne se contente pas d'ignorer son
résultat. Lectures neutres, écritures refusées (`DATASTORE_BEFORE_UNLOCK`). Même patron que le
`Provider<NiumiDatabase>` de `RoomSessionStore`, et §7.3 est étendue pour dire que la règle vaut pour
**tout** dépôt en stockage chiffré par les identifiants, pas seulement Room. Couvert par
`DataStoreUnlockGuardTest`, dont le `Provider` lève si on le résout.

**Mon analyse préalable était fausse sur ce point.** Le plan et la première version de §7.3
concluaient « pas de plantage, sans conséquence », en s'arrêtant au fait que les deux dépôts
interceptent `IOException`. La conséquence n'était pas dans la lecture ratée, elle était dans la
persistance de l'instance ratée.

## Validation sur appareil réel

**Appareil :** Xiaomi 25080RABDG (`lapis`), Android 16 (SDK 36), HyperOS OS3.0, build
`BP2A.250605.031.A3`. Même appareil qu'aux étapes 17 et 18. Session déroulée le 2026-09-14 de 20:00 à
22:20.

### Tests instrumentés — **136 verts, 0 échec, 1 ignoré**

| Module | Tests |
| --- | --- |
| `:core:database` | 70 — dont les 10 de `RoomDirectBootMergeTest` |
| `:feature:setup` | 34 |
| `:core:system` | 23 |
| `:feature:ringing` | 6 |
| `:feature:session` | 2 (+1 ignoré, préexistant) |
| `:app` | 1 |

126 à l'étape 18, 136 ici : les 10 nouveaux sont la transaction de fusion.

**Un échec au premier passage, de mon fait et non du code :**
`aProjectionBehindRoomIsRefusedWithoutWritingAnything` réensemençait Room avec le même `eventId` que
le `setUp`, et `ReceiptDao.insert` est en `ABORT` — c'est le signal `EVENT_ID_CONFLICT` du
coordinateur. La transaction échouait donc avant d'atteindre ce que le test voulait mesurer.
`seedRoomSession` prend désormais l'`eventId` en paramètre.

### Protocole manuel

| Essai | Résultat |
| --- | --- |
| **1** — redémarrage à −10 min, aucun déverrouillage | ✅ **après correctif.** Alarme reprogrammée 26 s après le boot, `RUNNING_LOCKED`, `origWhen=20:18:00.000` identique. Zéro notification parasite. **Puis l'alarme a réellement sonné à 20:18:00, toujours avant déverrouillage** : audio `USAGE_ALARM` démarré à 20:18:00.732, notification `ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE`, `AlarmActivity` affichée par-dessus l'écran verrouillé. La chaîne Direct Boot complète est validée de bout en bout. |
| **2** — redémarrage avec 5 min de retard | ✅ Téléphone éteint avant 21:06, rallumé à 21:11:11. Sonnerie démarrée à 21:11:38 — 27 s après le boot, `RUNNING_LOCKED`. Audio `USAGE_ALARM`, `AlarmActivity` sur l'écran verrouillé, 0 alarme en attente. |
| **3** — redémarrage avec 21 min de retard | ✅ Éteint avant 21:26, rallumé à 21:47:43. `RUNNING_LOCKED`, **service de sonnerie à 0, audio à 0, 0 alarme en attente**, et la notification d'attente de scan publiée avant tout déverrouillage. Attributs relevés dans `dumpsys notification` : `id=2 channel=niumi_session_awaiting_scan category=alarm sound=null vibrate=null flags=ONGOING_EVENT|ONLY_ALERT_ONCE`, **aucun `fullScreenIntent`**. Conforme à §10.5 point par point. |
| **4** — déverrouillage après l'essai 3 | ✅ Déverrouillé à 21:48:39. La session est restée `TRIGGERED_AWAITING_NFC` : ouvrir Niumi mène directement à l'écran de réveil en mode scan (§10.4), notification toujours présente, **0 alarme reprogrammée et aucune sonnerie**. C'est la preuve de la fusion : sans elle, Room serait resté sur `ARMED`, et la réconciliation post-déverrouillage aurait trouvé l'heure dépassée de 21 min, reproduit un `TRIGGER_ELAPSED` et un second incident. |
| **5** — changement de fuseau puis d'heure | ✅ Fuseau `Europe/Paris` → `Asia/Kabul` : epoch **inchangé** (`1789496280000`), affichage local recalculé de `20:18` à `22:48` — exactement §8.2. Horloge avancée de ~10 min : epoch toujours `1789496280000`. `TIMEZONE_CHANGED` et `TIME_SET` bien reçus par le processus Niumi (visible dans `SmartPower`). **Deux incidents distincts** sur l'écran de session, et un **second** changement d'horloge (+387 s) n'en a **pas** ajouté de troisième : la déduplication fonctionne. |
| **6** — `adb install -r` | ✅ **sur le résultat**, ⚠ **sur le chemin**. L'alarme est conservée au même instant. Mais le receveur n'a **pas** tourné : `Unable to launch app com.niumi.app for broadcast MY_PACKAGE_REPLACED : process is not permitted to auto start`. C'est la restriction d'autostart de HyperOS. L'alarme survit parce qu'Android préserve le `PendingIntent` lors d'un remplacement d'APK, pas grâce à nous. Voir « Limites constatées ». |
| **7** — fenêtre §10.5 | voir ci-dessous. |

### Limites et particularités constatées sur cet appareil

- **La restriction de démarrage automatique de HyperOS**, détaillée plus bas — elle a fait l'objet
  d'une expérience contrôlée après coup, mon premier énoncé étant trop étroit.
- **HyperOS bloque par intermittence l'installation multi-APK** d'AGP
  (`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`), ce qui fait échouer
  `:app:connectedDebugAndroidTest` sans aucun rapport avec le code. Un `adb install -r` manuel puis un
  nouvel essai suffisent.
- **`am force-stop` désactive le service d'accessibilité** (`enabled_accessibility_services` repasse à
  `null`) : confirmation du constat de l'étape 18. Tout essai qui tue le processus impose de
  réactiver le service à la main avant de continuer.
- **Le diagnostic NFC est brièvement faux après un redémarrage.** L'utilisateur a vu « NFC désactivé »
  alors qu'il ne l'était pas ; le contrôle s'est corrigé seul en revenant à l'application. Vérifié
  dans le code : `ReaderModeNfcReader.availability` relit l'adaptateur à chaque appel, sans cache —
  c'est donc la pile NFC du système qui finissait son initialisation. Le diagnostic est rejoué au
  retour au premier plan (`ForegroundReadinessTrigger`, étape 12), ce qui est exactement ce qui s'est
  produit. Aucun correctif : le mécanisme de rattrapage existe déjà et a fonctionné.
- **Le journal technique écrit avant déverrouillage ne vit qu'en mémoire** (`UnlockAwareTechnicalEventLog`,
  étape 10) et disparaît avec le processus. `MISSED_TRIGGER_WINDOW`, journalisé pendant la fenêtre
  Direct Boot, n'atteint donc jamais Room. L'**incident** métier, lui, y arrive bien par le rejeu de
  l'outbox — c'est celui qui compte pour §7.3. Limite préexistante, hors du contrat de fusion de §9.3
  (« le registre et l'outbox »), signalée ici parce que l'étape 19 est la première à produire
  réellement des événements techniques avant déverrouillage.

## Essai 7 et décision §10.5 — l'écart hérité de l'étape 18 est tranché et refermé

### La mesure

État `TRIGGERED_AWAITING_NFC` obtenu par un redémarrage avec 19 min de retard, notification publiée,
puis balayée par l'utilisateur à 22:18:50, appareil déverrouillé et au repos.

| Événement | Republie la notification ? |
| --- | --- |
| Attente passive | **Non — 523 s mesurées**, sans retour |
| Passage de l'application au premier plan | **Non** |
| Verrouillage puis déverrouillage ordinaire de l'écran | **Non** |
| Premier déverrouillage après démarrage (`USER_UNLOCKED`) | Oui — 12 s |
| Redémarrage de l'appareil | Oui |
| Démarrage de processus | Oui |

**Le pari de l'étape 18 est démenti par la mesure.** `SystemEventsReceiver` ajoute bien des occasions
de réconciliation, mais toutes sont liées au démarrage ou à l'horloge : aucune ne survient pendant
qu'on se sert du téléphone. Après un balayage, la fenêtre dure jusqu'au prochain démarrage de
processus — potentiellement toute la session. Un déverrouillage d'écran n'y change rien :
`ACTION_USER_UNLOCKED` n'est émis qu'au **premier** déverrouillage après un démarrage, ce qui a été
vérifié en verrouillant puis déverrouillant l'appareil sans aucun effet.

Les deux arguments de proportion de l'étape 18 ont en revanche été **vérifiés sur appareil** :
l'overlay de §12.2 rappelle le scan devant une application bloquée, et ouvrir Niumi dans un état de
scan mène bien directement à l'écran de réveil (`AlarmActivity`).

### La décision, validée avec l'utilisateur

Option 2 du plan, appliquée **dans cette étape** plutôt que reportée à l'étape 20 : la réconciliation
est déclenchée au passage de l'application au premier plan quand la session attend un scan, via une
neuvième `ReconcileReason`, `FOREGROUND_AWAITING_SCAN`. L'option du service de premier plan reste
écartée — l'attente d'un scan peut durer des heures et §10.5 exige que cette notification ne
ressemble pas à une alarme active.

### Un second défaut trouvé en vérifiant le correctif

Posé d'abord sur le seul `MainActivity.onResume`, **le correctif ne se déclenchait pas** : mesuré,
notification toujours absente 36 s après réouverture. Dans un état de scan, rouvrir Niumi ramène le
task au premier plan avec `AlarmActivity` au sommet (`launchMode="singleTask"`), et
`MainActivity.onResume` n'est jamais rejoué. Le déclencheur est donc posé sur **les deux écrans**.

**Après correction : republiée en 1 seconde.**

### Portée du changement

`SessionReadinessWatcher.evaluate()` appelle la surveillance de §13.1 **puis** réconcilie si l'état
est un état de scan — elle ne la remplace pas : `reconcile` ne rejoue le diagnostic que sur une
session `ARMED`, alors que le service d'accessibilité reste surveillé dans tous les états non finaux.
La nouvelle raison ne déclenche ni fusion Direct Boot ni incident d'horloge ni reprogrammation
d'alarme — vérifié par `aForegroundPassNeitherReschedulesAnAlarmNorRecordsAnIncident`, parce qu'elle
survient à chaque ouverture de l'application.

## Bilan des tests

| | Total | Échecs |
| --- | --- | --- |
| JVM (tous modules, `./gradlew test`) | **786** | 0 |
| Instrumentés (`connectedDebugAndroidTest`) | **136** | 0 (1 ignoré, préexistant) |

`ktlintCheck`, `detekt`, `:app:assembleDebug` et `:app:lintDebug` verts, cette dernière sans aucune
remontée.

Tests ajoutés par l'étape : `SystemEventReasonsTest` (8), `SessionReconcilerBootTest` (12),
`DirectBootMergerTest` (8), `SessionScanStatesTest` (3), `DataStoreUnlockGuardTest` (4), deux cas
dans `AndroidDeviceReadinessCheckerTest`, et `RoomDirectBootMergeTest` (10, instrumenté).

## Ce qui reste ouvert

- **Le journal technique écrit avant déverrouillage ne rejoint jamais Room.** Limite préexistante
  (étape 10), hors du contrat de fusion de §9.3 qui ne porte que sur « le registre et l'outbox ».
  L'incident métier, lui, y arrive bien. Signalé parce que l'étape 19 est la première à produire
  réellement des événements techniques avant déverrouillage.
- **`MY_PACKAGE_REPLACED` est bloqué par l'autostart HyperOS.** Le résultat attendu est tenu par la
  survie du `PendingIntent`, pas par le receveur. À reconsidérer si une surcouche efface aussi les
  alarmes lors d'un remplacement d'APK.
- **La matrice §20 reste à couvrir sur d'autres fabricants.** Tout ce qui précède a été mesuré sur un
  seul appareil, dont la surcouche s'est révélée particulièrement restrictive.

## Specs modifiées dans ce changement

- **SPEC_ANDROID §9.3** — `USER_UNLOCKED` enregistré à chaud et non déclaré ; réenregistrement
  inconditionnel de l'alarme sur changement d'horloge ; déduplication de l'incident par code et par
  session ; fusion tentée sur trois raisons, avec ses règles.
- **SPEC_ANDROID §7.3** — ce que garantit réellement le contexte protégé ; garde de déverrouillage
  étendue à **tout** dépôt en stockage chiffré par les identifiants, avec le dégât mesuré.
- **SPEC_ANDROID §13.1** — contrôle du service d'accessibilité neutralisé avant le premier
  déverrouillage, avec le mécanisme et la mesure.
- **SPEC_ANDROID §10.5** — fenêtre sans rappel mesurée, décision prise, et la raison pour laquelle le
  déclencheur porte sur deux écrans. L'écart hérité de l'étape 18 est refermé.


## Complément : la restriction de démarrage automatique, mesurée par expérience contrôlée

L'essai 6 avait montré `MY_PACKAGE_REPLACED` bloqué par HyperOS. J'en avais tiré un énoncé trop
étroit — « `MY_PACKAGE_REPLACED` est bloqué » — alors que la règle porte sur **tout broadcast qui
doit démarrer le processus**, et que la question qui comptait n'était pas posée : les broadcasts de
démarrage, eux, sont-ils soumis à la même règle ? Si oui, le cœur de l'étape 19 dépendrait d'un
réglage refusé par défaut et que personne ne pense à activer.

**Vérification par comparaison contrôlée**, permission refusée puis accordée, tout le reste égal :

```
refusée  → Unable to launch app com.niumi.app/10551 for broadcast
           Intent { act=android.intent.action.MY_PACKAGE_REPLACED } :
           process is not permitted to  auto start

accordée → Start proc 31433:com.niumi.app/u0a559 for broadcast
           {com.niumi.app/com.niumi.system.boot.SystemEventsReceiver}
           puis background->idle(4081ms) R(broadcast end … MY_PACKAGE_REPLACED)
```

Le système démarre le processus **pour notre receveur nommément**, et le tient 4081 ms — la fenêtre
`goAsync`. Le lien de cause à effet est établi.

**Le résultat est rassurant.** La permission était **refusée** (état par défaut, vérifié sur l'écran
« Démarrage automatique en arrière-plan ») pendant les essais 1, 2 et 3 — ceux où le processus a été
démarré par `LOCKED_BOOT_COMPLETED` et l'alarme reprogrammée. **Les broadcasts de démarrage sont donc
exemptés de cette restriction**, et la promesse de §9.3 ne dépend pas d'un réglage OEM.

Reste hors de portée sans cette permission : la réconciliation après une mise à jour de l'APK — où
l'alarme survit de toute façon par le `PendingIntent` — et, vraisemblablement, les changements
d'horloge survenant processus mort. Ce dernier cas n'a pas été mesuré ; sa conséquence se limiterait
à l'incident non consigné, l'instant du réveil étant un `RTC_WAKEUP` absolu qu'Android préserve
lui-même.

**Aucun contrôle de diagnostic n'est ajouté pour ce réglage.** Ce qu'il conditionne est une sécurité
supplémentaire, jamais le déclenchement du réveil ; l'ajouter au tableau de §13 ferait refuser une
activation sur un motif qui ne compromet pas la promesse. §4.2 le nomme désormais avec sa portée
exacte, et §21 le reprendra dans `LIMITES.md` et la matrice de tests physiques.

**Attention pour les prochaines campagnes sur cet appareil :** la permission a été **laissée
activée** à l'issue de cette expérience. Elle ne reflète donc plus la configuration par défaut d'un
utilisateur, et il faudra la remettre à l'état refusé avant de mesurer quoi que ce soit qui en
dépend.
