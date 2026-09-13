# Étape 17 — déclenchement via le coordinateur, reconstruction du service, états de l'écran de réveil

**Date :** 2026-09-13. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 17.

**Produit :** `RINGING` n'est plus écrit que par le moteur (`AlarmReceiver` → `AlarmTriggerHandler` →
`ALARM_FIRED` → `StartRingingExecutor`), le service de sonnerie se reconstruit depuis le snapshot
après une mort de processus, l'écran de réveil reflète les états réels de la machine commune, la
notification de sonnerie est surveillée et republiée, l'écran 10 est livré, et l'ouverture depuis
le lanceur pendant la sonnerie mène enfin à l'écran 8.

---

## Arbitrages validés avec l'utilisateur avant implémentation

1. **Garde de révision : monotonie, pas égalité.** Le plan disait « `revision` obsolète → aucun
   `dispatch` ». Vérification faite dans le code avant d'implémenter : `IncidentReducer.onReported`
   fait `revision + 1` et ne produit **aucun** `SCHEDULE_ALARM` ; `SessionReconciler` ne reprogramme
   que si `!isScheduled` ; `SessionEventValidation.revisionViolations` exige l'égalité stricte. La
   règle du plan transformait donc chaque incident de §13.1 survenu pendant `ARMED` — six contrôles
   y sont surveillés — en panne de réveil silencieuse. **Retenu :** refuser seulement une révision
   **supérieure** au snapshot.
2. **Écran 10 dans `:feature:session`**, à côté de l'écran 11. Écart au plan, qui le plaçait dans
   `:feature:ringing`.
3. **Redirection depuis le lanceur traitée ici** (§10.4) : les quatre états de scan mènent à
   l'écran 8.
4. **Republication de notification en escalade** : plein écran à la première disparition,
   republication silencieuse ensuite (§10.2 contre §10.4), vérification toutes les 10 s.
   **Cet arbitrage a été remplacé pendant la validation sur appareil** : la garde à usage unique
   pouvait être consommée par une absence transitoire. Voir « Trois défauts trouvés » ci-dessous.

## Écarts au plan

- **`AlarmTriggerHandler` et `RingingServiceRecovery` vivent dans `:core:system`**, pas dans
  `:feature:ringing`. Ils ont besoin du coordinateur, de la passerelle de persistance, de la
  fabrique d'événements et du moniteur de §13.1 ; le dépôt n'a pas de `testFixtures`, donc les
  placer dans `:feature:ringing` aurait obligé à y redéfinir huit doublures déjà écrites
  (`TestCoordinatorHarness`, `InMemoryPersistenceGateway`, `FakeClock`, `CallJournal`,
  `SessionDtoFixtures`, `ReadinessTestSources`…). Précédents directs : `SessionStartupReconciler` et
  `SessionReadinessWatcher`, points d'entrée de même nature. `AlarmReceiver` reste une coquille.
- **`ServiceCommand`/`ServiceCommandExtras` déplacés** de `:feature:ringing` vers
  `:core:system` (`com.niumi.system.ringing`) : le handler, le receiver et le service les partagent
  désormais. Déplacement mécanique, aucun changement de comportement, test suivi.
- **`AlarmChainInstrumentedTest` est dans `:app/src/androidTest`**, pas dans `:feature:ringing`.
  Injecter `AlarmTriggerHandler` tire le coordinateur, donc `ApplyBlockingExecutor` et
  `SessionReadinessMonitor`, dont les liaisons vivent dans `SessionBlockingModule`
  (`:feature:session`) — absent de l'APK de test d'un module `library`. Constaté à la compilation :
  Dagger refusait `BlockingController` et `AccessibilityServiceStatus`. `:app` est le seul module
  dont le graphe est complet, donc le seul où la chaîne s'exerce **sans aucune doublure**.
  `:app` gagne en conséquence son `HiltTestRunner` et `androidTestImplementation` de Hilt/Truth.
- **`:feature:ringing/androidTest` gagne `TestSessionBindingsModule`** (doublures inertes des deux
  liaisons ci-dessus), pour que ses tests instrumentés existants continuent de compiler — même
  justification que `TestComponentResolverModule` écrit à l'étape 3.
- **`SessionModule` n'accueille pas le nouveau `@Provides`** : il était déjà à 11 fonctions, le
  plafond de `TooManyFunctions`. `TriggerModule` est créé à côté, comme `EffectExecutorModule`.

## Décisions non couvertes par le plan ni par les specs

- **Snapshot illisible à la recréation du service.** Ni le plan ni §10.2 ne le traitaient. Les deux
  issues sont mauvaises à moitié : sonner expose l'utilisateur à une alarme **sans bouton d'arrêt**
  (§10.2) pour une session peut-être terminée ; s'arrêter risque de ne pas réveiller. Retenu : ne
  pas sonner, ne rien effacer, réconcilier puis s'arrêter. Le blocage est conservé (§18). §10.2 mise
  à jour.
- **Ce que montre la progression de `RELEASING`** : les **trois effets requis** de la libération
  (`CANCEL_ALARM`, `STOP_RINGING`, `REMOVE_BLOCKING`, SPEC_CORE_KMP §6), pas les cinq. Les
  best-effort (`PUBLISH_PLATFORM_SNAPSHOT`, `CLEAR_SCAN_REQUEST`) ne bloquent jamais la phase : les
  afficher en attente ferait croire à un blocage inexistant. Aucune lecture nouvelle n'est
  introduite — `pendingEffects` ne rend que les effets rejouables, donc un effet absent est terminé.
  Un effet `FAILED` reste « en cours » : il sera rejoué, l'annoncer terminé serait le faux état de
  fiabilité qu'interdit §15.
- **`RELEASING` passe avant tous les rangs NFC** dans le texte de l'écran : le scan a déjà eu lieu,
  demander de déverrouiller ou d'approcher le boîtier n'y décrirait plus rien.
- **`AlarmExitDestination.HOME` pour `FAILED`.** §15 ne prévoit aucun écran pour cet état, et §18 le
  réserve à une activation jamais aboutie — donc inatteignable depuis l'écran de réveil. Classé
  explicitement plutôt que laissé à un `else` : l'exhaustivité du `when` est la propriété
  recherchée.

## Points découverts à l'implémentation

- **`FakeTechnicalEventLog` n'enregistrait que le type**, pas le `sessionId`. L'étape doit prouver
  qu'un `ALARM_RECEIVED` issu d'extras invalides est journalisé **sans** identifiant (§16, §17) :
  une liste `entries` est ajoutée à côté de `logged`, sans toucher aux appelants existants.
- **`RingingNotificationFactory` gagne un `contentIntent`**, et son paramètre est renommé
  (`fullScreenPendingIntent` → `alarmScreenPendingIntent`) : il ne désigne plus seulement le plein
  écran. `actions` reste vide — un `contentIntent` n'est pas une action, il n'apparaît pas comme un
  bouton et ne termine aucune session (§10.2, §19.1).
- **`KeyguardManager.KeyguardLockedStateListener` a d'abord été employé** en API 34+, avec
  `ACTION_USER_PRESENT` en repli. **Cette voie est morte à la première sonnerie réelle** : voir le
  défaut 1 de la validation. Seul `ACTION_USER_PRESENT` subsiste, à tous les niveaux d'API. La
  **valeur** reste `isDeviceLocked` ; l'écouteur n'est qu'un déclencheur de relecture —
  `isKeyguardLocked` n'a pas la même sémantique.
- **Trois `when` ont dû être remaniés** pour que ktlint n'impose pas d'accolades sur une branche
  `-> Unit`, que le compilateur signalait alors en « expression inutilisée ». D'où
  `confirmSessionGone()`, `republish(...)` et le `@Composable AlarmContent` extraits. Aucune règle
  assouplie.
- **`AlarmTriggerHandler.handle` porte `@Suppress("ReturnCount")`**, avec la même justification que
  `DefaultSessionCoordinator.dispatchLocked` : cinq clauses de garde séquentielles, dont
  l'imbrication produirait cinq niveaux d'indentation pour des conditions indépendantes.

## Fichiers

**Créés** — `:core:system` : `session/AlarmTriggerHandler.kt`, `session/di/TriggerModule.kt`,
`ringing/RingingServiceRecovery.kt`, `notification/RingingNotificationWatch.kt`, et leurs tests
(`AlarmTriggerHandlerTest`, `SessionEventFactoryTriggerTest`, `RingingServiceRecoveryTest`,
`RingingNotificationWatchTest`). `:feature:ringing` : `AlarmViewModel.kt`, `ui/AlarmUiState.kt`,
`ui/ReleaseProgress.kt`, `test/.../fakes/AlarmTestFakes.kt`, `AlarmViewModelTest`,
`ui/AlarmUiStateTest`, `ui/ReleaseProgressTest`, `androidTest/.../TestSessionBindingsModule.kt`,
`androidTest/.../RingingNotificationInstrumentedTest.kt`. `:feature:session` :
`active/CompletedScreen.kt`. `:app` : `navigation/LauncherDestination.kt`,
`test/.../LauncherDestinationTest.kt`, `test/.../HomeViewModelTest.kt`,
`androidTest/.../HiltTestRunner.kt`, `androidTest/.../AlarmChainInstrumentedTest.kt`.

**Déplacés** — `ServiceCommand.kt` et `ServiceCommandTest.kt` de `:feature:ringing` vers
`:core:system/ringing/`.

**Modifiés** — `AlarmReceiver` (coquille `goAsync()`), `AlarmRingingService` (reconstruction et
surveillance de notification), `AlarmActivity` (ViewModel, verrouillage, sortie vers les écrans 10
et 11), `ui/AlarmScreenState` (indexé par `SessionStateDto`, `AlarmRingingPhase` **supprimé**),
`ui/AlarmScreen` (progression de nettoyage) ; `SessionEventFactory` (`alarmFired`) ;
`RingingNotificationFactory` ; `NiumiDeepLink` (deux destinations) ; `DeepLinkDestination`,
`NiumiNavHost` (écran 10 enregistré), `NiumiRoute`, `MainActivity` (redirection continue vers
l'écran 8), `HomeScreen`, `HomeUiState`, `HomeViewModel` ;
`ScanToModifyTexts` (`CompletedTexts`) ; `FakeTechnicalEventLog` ; `AlarmReceiverInstrumentedTest`
et `AlarmScreenNoStopActionTest` réécrits ; `androidApp/app/build.gradle.kts` ;
`specs/SPEC_ANDROID.md`.

## Specs mises à jour

- **§10.1** : le receiver est une coquille ; tableau des trois règles de la garde de révision avec
  la raison pour laquelle l'égalité stricte serait fautive ; `goAsync()` borné à 8 s sans événement
  technique au dépassement.
- **§10.2** : règle d'escalade de la republication (et pourquoi elle est nécessaire pour réconcilier
  §10.2 et §10.4), période de 10 s, tableau état → action à la recréation du service, cas
  `Unreadable` inclus, et pourquoi `SERVICE_RECREATED` n'est pas redondant avec `PROCESS_START`.
- **§10.4** : un publisher à `null` ne ferme jamais l'écran ; les quatre états de scan mènent à
  l'écran 8 depuis le lanceur, avec l'acquittement anti-boucle ; sortie vers les écrans 10 et 11.
- **§13.1** : le déclencheur « au déclenchement » est porté par le moniteur, avant `ALARM_FIRED`,
  avec la conséquence sur la révision ; le handler n'interrompt jamais sa chaîne.
- **§15** : écran 10 **à l'étape 17** (la liste annonçait l'étape 15 — contradiction relevée à
  l'étape 16, corrigée ici), dans `:feature:session`, livré mais atteignable seulement à partir de
  l'étape 18.

## Vérifications exécutées

```
export JAVA_HOME=$(brew --prefix openjdk@17)
./gradlew --rerun-tasks :feature:ringing:testDebugUnitTest :core:system:testDebugUnitTest \
          :feature:session:testDebugUnitTest :app:testDebugUnitTest \
          :feature:setup:testDebugUnitTest :core:database:testDebugUnitTest :shared:core:jvmTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
grep -rn --include="*.kt" "startRinging" androidApp | grep -v "/build/"
```

**749 tests JVM verts** (679 en fin d'étape 16, +70), après les corrections issues de la
validation sur appareil :

| Module | Tests | Écart |
| --- | --- | --- |
| `:core:system` | 217 | +40 |
| `:feature:ringing` | 38 | +17 |
| `:feature:session` | 121 | +1 |
| `:app` | 31 | +14 |
| `:core:database` | 106 | — |
| `:shared:core` | 160 | — |
| `:feature:setup` | 76 | — |

`:app:assembleDebug`, ktlint et detekt verts ; **`:app:lintDebug` sans aucune remontée** (rapport
XML : 0 `<issue>`). Aucune règle detekt ni ktlint assouplie. Les APK de test instrumenté des deux
modules concernés s'assemblent (`:feature:ringing:assembleDebugAndroidTest`,
`:app:assembleDebugAndroidTest`).

**Grep de clôture** : hors tests, `RingingController.startRinging` a **deux** appelants de
production — `StartRingingExecutor`, qui exécute l'effet du moteur, et `SessionReconciler`, ajouté
en fin d'étape pour la reprise de `RINGING`. Le critère du plan disait « aucun appel direct au
service hors exécuteur » ; il est corrigé dans le plan. Ce qu'il interdit réellement — faire sonner
sans décision du moteur, comme `AlarmReceiver` le faisait avant cette étape — reste exclu : le
réconciliateur n'agit que sur un `RINGING` déjà écrit par le moteur, ce que §18 lui demande. Les
occurrences restantes dans `AlarmRingingService` sont sa méthode privée homonyme.

**Deux avertissements de dépréciation préexistants** subsistent, sur des lignes non touchées par
cette étape : `hiltViewModel()` (dix sites dans le dépôt, déplacé vers
`androidx.hilt.lifecycle.viewmodel.compose`) et `createComposeRule()` (v2 recommandée). À traiter
globalement, pas au détour de cette étape.

## Validation sur appareil réel

**Déroulée le 2026-09-13 sur Xiaomi 25080RABDG (lapis), Android 16 (API 36), HyperOS OS3.0**, build
`BP2A.250605.031.A3`. Huit sessions réelles armées et déclenchées.

**125 tests instrumentés verts** : `:core:database` 60, `:feature:setup` 34, `:core:system` 21,
`:feature:ringing` 6, `:feature:session` 3 (1 ignoré, préexistant), `:app` 1.

`AlarmChainInstrumentedTest` est le résultat central : une **vraie alarme exacte** conduit la
session jusqu'à `RINGING`, service au premier plan, `ALARM_RECEIVED` puis `RINGING_STARTED` au
journal. Il a dû être lancé par `am instrument` : HyperOS refuse les installations USB répétées
(`INSTALL_FAILED_USER_RESTRICTED`), ce qui a plusieurs fois désinstallé l'application en cours de
séance et effacé ses données.

### Trois défauts trouvés par la validation, tous corrigés

1. **`KeyguardManager.addKeyguardLockedStateListener` tuait le processus.** Il exige
   `SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE`, absente de la liste figée de §14 ; l'appel lève une
   `SecurityException` non rattrapable, à l'instant précis où le plein écran ouvre l'écran de
   réveil. Mesuré : l'alarme a sonné **une seconde** avant que le processus ne meure, le son était
   inaudible, le service d'accessibilité s'est délié dans la foulée. §10.4 proposait deux
   mécanismes au choix ; le second est désormais **interdit**, `ACTION_USER_PRESENT` est employé à
   tous les niveaux d'API. Aucun test JVM ne pouvait l'attraper : c'est un contrôle de permission
   du système.
2. **La garde « plein écran une seule fois » pouvait être consommée par une absence transitoire**
   de `getActiveNotifications()`, si bien que la disparition réellement subie pendant le sommeil
   n'obtenait plus que la republication silencieuse. Remplacée par un critère d'**état de
   l'appareil** (verrouillé ou écran éteint → plein écran ; en cours d'usage → silencieux), qui
   n'a rien à épuiser et suit le comportement réel d'Android. §10.2 réécrite.
3. **La redirection vers l'écran de réveil ne se déclenchait pas**, en deux temps. Posée sur
   l'accueil, elle ne s'exécutait jamais : après l'armement le `NavHost` est sur `ActiveSession`,
   donc `HomeRoute` n'est plus composé. Déplacée sur `MainActivity.onResume`, elle ne se
   déclenchait toujours pas quand l'alarme sonnait **pendant** que l'utilisateur était dans Niumi.
   Elle est désormais **observée en continu** (`repeatOnLifecycle(RESUMED)` sur le publisher).

### Deux demandes produit arbitrées pendant la validation

- **Pendant un état de scan, aucun autre écran de Niumi n'est atteignable.** Demandé par
  l'utilisateur après avoir constaté qu'un retour depuis l'écran de réveil le laissait sur l'écran
  7, qui n'active pas le Reader Mode. Le périmètre « tout l'appareil » a été explicitement écarté :
  il contredit §10.4, expose au refus Play (§23) et n'est pas réalisable sans mode `lock task`.
- **Le retour prédictif est inerte tant que la session attend un scan.** §10.4 demandait de
  renvoyer vers l'accueil ; corrigée. Ce n'est pas une « activité impossible à quitter » : Home, le
  geste de navigation, les récents et le volet de notifications restent disponibles.

### Essais du protocole

| Essai | Résultat |
| --- | --- |
| 1. Sonnerie réelle, écran éteint | **Validé** après correction du défaut 1 : sonnerie audible et continue, écran de réveil par-dessus le verrouillage, service au premier plan (`ONGOING_EVENT\|NO_CLEAR\|FOREGROUND_SERVICE`, `category=alarm`, `types=0x2`), session `HEALTHY`, aucun crash. |
| 2. Mort du processus pendant la sonnerie | **Non validé — impossible à exercer.** `am kill` est inapplicable (il ne tue que les processus en arrière-plan, or Niumi tient un service au premier plan) ; après `am crash`, HyperOS ne rejoue pas le redémarrage `START_STICKY` — aucun « Scheduling restart », aucun `PROCESS_RECREATED` après 85 s. Voir la réserve ci-dessous. |
| 3. Activité fermée puis verrouillage | **Validé** : sonnerie maintenue. |
| 4. Balayage de la notification | **Validé à moitié.** La notification revient. La republication **avec plein écran** n'a pas été démontrée : mesurée `fullscreenIntent=null`, donc branche « appareil en cours d'usage ». Hypothèse à vérifier : `KeyguardManager.isDeviceLocked` ne rend `true` que si l'écran de verrouillage est **sécurisé**. |
| 5. Déverrouillage pendant la sonnerie | **Non observable sur cet appareil** : le déverrouillage écarte l'activité — c'est la troisième situation mesurée à l'étape 6 et citée par §10.2. La transition du texte ne peut donc pas être vue. |
| 6. Ouverture depuis le lanceur | **Validé** après correction du défaut 3 : écran de réveil et non écran 7 ; Retour mène au lanceur ; réouverture par l'icône ramène à l'écran de réveil. |
| 7. Silence total après armement | **Effectué, et il a trouvé un défaut.** Le receveur à chaud de §13.1 a réagi immédiatement au changement de filtre, `ALARM_MUTED_BY_DND` est journalisé, la chaîne s'est déroulée normalement au déclenchement et la session est restée active avec son blocage — conforme à §13. **Mais l'incident `ANDROID_ALARM_MUTED_BY_DND` n'était pas enregistré** : voir ci-dessous. |
| 8. Incident pendant `ARMED` puis sonnerie | **Validé, et plus largement que prévu** — voir ci-dessous. |

### Quatrième défaut, trouvé par l'essai 7 : un incident perdu sur deux

Le silence total fait aussi tomber le volume d'alarme à zéro : **deux contrôles surveillés
échouent d'un seul coup**. `SessionReadinessMonitor.evaluate` bouclait sur les contrôles en échec
en passant chaque fois **le snapshot du début de passe**. Le premier `INCIDENT_REPORTED` incrémente
la révision ; le second portait donc une `expectedRevision` périmée et se faisait rejeter en
`STALE_REVISION`, **silencieusement**. Résultat mesuré : un seul incident en base
(`ANDROID_ALARM_VOLUME_ZERO`), un seul `RECORD_INCIDENT` dans l'outbox, révision passée de 2 à 3
puis directement à 4 — alors que le journal technique portait bien les deux détections et que les
deux notifications d'avertissement avaient été présentées. §13.1 promet pourtant un incident par
contrôle.

C'est **la même classe de défaut que l'arbitrage 1** : une révision devenue périmée en cours de
route. Il est antérieur à l'étape 17 — il date de l'étape 12 — et ne se manifeste que si deux
contrôles tombent ensemble, ce qu'aucun test ne couvrait.

Corrigé : `ReadinessDegradation` porte désormais `snapshotAfter`, et la passe bâtit chaque incident
sur le snapshot rendu par la décision précédente. `SessionReadinessMonitorMultipleFailuresTest`
(2 tests) fige le comportement ; les deux échouent si l'on retire le report du snapshot, vérifié
en simulant la régression.

### La garde de révision, confirmée et sous-estimée

L'arbitrage 1 est validé sur appareil, à chaque déclenchement. Et la mesure a montré que le rapport
initial **sous-estimait le problème** : `SCHEDULE_ALARM` s'exécute pendant `PREPARING`, donc à la
révision 1, puis `ACTIVATION_SUCCEEDED` porte la session à `ARMED` en révision 2 sans reprogrammer
l'alarme. **Le `PendingIntent` porte donc toujours une révision inférieure à celle du snapshot, dès
l'activation et sans qu'aucun incident soit nécessaire.** La règle littérale du plan n'aurait pas
seulement cassé les réveils après incident : elle aurait rendu **toutes** les alarmes Niumi muettes.

### Réserves subsistantes

- **Le travail 2 n'est prouvé qu'en JVM** (9 tests). `RingingServiceRecovery` n'a jamais été appelé
  sur appareil, faute de pouvoir provoquer une mort de processus suivie d'un redémarrage du
  service. Réserve sur la représentativité : un crash induit par `adb` n'est pas une mort par
  manque de mémoire, qui pourrait, elle, déclencher le redémarrage `START_STICKY`.
- **Rien ne ranimait la sonnerie — corrigé à moitié, sur décision de l'utilisateur.**
  `RingingServiceRecovery` n'est appelé que par le service lui-même, et `SessionReconciler` ne
  faisait délibérément rien pour `RINGING`. Si la plateforme ne relançait pas le service, le réveil
  s'arrêtait définitivement et en silence, alors que la session restait active et le blocage en
  place — et **ouvrir l'application n'y changeait rien**.

  **Premier maillon, fait ici** (écart au plan assumé, `RINGING` n'étant prévu par aucune étape) :
  `SessionReconciler` relance `START_RINGING` chaque fois qu'il trouve l'état `RINGING`, via
  `ReconcilerSources.ringingController` et la nouvelle action `ReconcileAction.RingingResumed`.
  L'appel est idempotent. Une panne définitive devient donc une panne réparable : le son revient
  dès que Niumi se réveille, pour quelque raison que ce soit.

  **Second maillon, à l'étape 20** : garantir que le processus revienne. Une alarme de secours
  posée pendant `RINGING` réveillerait Niumi périodiquement et laisserait la réconciliation faire
  le reste. Sa conception reste ouverte — période, annulation, distinction d'avec l'alarme du
  réveil, coût en alarmes exactes. Tant qu'elle n'existe pas, **un processus mort que rien ne
  réveille laisse le réveil muet**.
- **La republication avec plein écran (§10.2) n'est pas démontrée sur appareil** (essai 4).
- **Le message « Ce boîtier n'est pas celui de ta session » est trompeur en variante debug** :
  `PocNfcScanHandler` compare le tag à `@PocPairedBoxStore`, pas au boîtier figé de la session. En
  release aucun handler n'est lié et le scan est ignoré. Disparaît à l'étape 18.
- **Le volume d'alarme ne peut pas être réglé par `adb` sur cet appareil** ; il l'a été à la main.

### Leçon de méthode

**Room est en mode WAL.** Copier `niumi.db` sans `niumi.db-wal` donne un état figé, parfois
vieux de plusieurs minutes. J'en ai conclu à tort à un défaut reproductible de la réconciliation,
avant de constater que tout avait fonctionné. Toute lecture de la base depuis l'appareil doit
copier les trois fichiers (`.db`, `-wal`, `-shm`). À rapprocher de la leçon de l'étape 16 sur
`runMigrationsAndValidate`.

Deux autres pièges du banc de test, sans rapport avec le code : `adb shell am force-stop` **annule
les alarmes** du paquet et délie le service d'accessibilité — c'est ce qui a fait manquer un premier
déclenchement, puis empêché sa réparation (la garde `PERMISSION_CHECKS` de `reconcileArmed`
interrompant la passe). Et `am start` sur une activité déjà au sommet d'une autre tâche ne démarre
pas le processus.

## Constats hors périmètre, toujours ouverts

- **Régression volontaire du POC de debug** : `PocViewModel.schedule()` programme une alarme pour
  `PocSession.ID`, qui n'existe dans aucune session Room. Depuis cette étape, ce bouton **ne fait
  plus sonner** : le handler ne trouve pas de session et refuse, ce qui est le comportement voulu.
  Son libellé doit être requalifié (« prouve que l'alarme exacte se déclenche et que le receiver
  journalise `ALARM_RECEIVED` »), et la phrase de l'étape 18 du plan — « `PocScreen` garde seulement
  programmation d'alarme et association » — corrigée en conséquence.
- **§18 « une erreur Room pendant la sonnerie s'appuie sur le snapshot Direct Boot » n'est pas
  implémenté** : `UnlockAwarePersistenceGateway` choisit sa source par `isUserUnlocked` et ne se
  replie jamais sur Direct Boot en cas d'erreur Room. Hors périmètre (Direct Boot = étape 19), à ne
  pas présenter comme couvert.
- Les deux constats de plateforme de l'étape 15 restent ouverts : Android ne relie pas le service
  d'accessibilité après `am crash` (étape 20) ; `ReaderModeNfcReader` n'emploie pas
  `FLAG_READER_NO_PLATFORM_SOUNDS` (étape 18).
- Réinstaller l'application révoque le service d'accessibilité (étape 16) ; l'équivalent Play Store
  n'est toujours pas mesuré.
