# Étape 23 — Android : Room v3, Direct Boot v2, alarme de début, `BlockingStartReceiver`, réconciliation et projection de blocage

Date : 2026-09-16. **Validé sur appareil** : Xiaomi 25080RABDG, Android 16, HyperOS OS3.0.302.0.WPPEUXM.
83 tests instrumentés verts. Le protocole manuel du blocage différé reste reporté à l'étape 24 — voir
« Ce qui reste à valider ».

## Résumé

L'étape 22 avait livré un moteur commun capable de décrire un blocage différé ; Android ne savait
ni le persister, ni le programmer, ni le déclencher. Cette étape livre ce versant : Room v3 et la
projection Direct Boot v2 portent les quatre champs `blocking*`, une alarme exacte distincte du
réveil déclenche le début à l'heure, la réconciliation la reprogramme et la rattrape, et le service
d'accessibilité ne bloque plus une seule application avant l'instant choisi.

**989 tests JVM verts** (contre 940 avant l'étape, soit 49 nouveaux ou étendus) et **83 tests
instrumentés verts** sur appareil réel, `:app:assembleDebug`, `ktlintCheck`, `detekt` et
`:app:lintDebug` verts.

L'interface du choix arrive à l'étape 24 : `SummaryViewModel` passe `null`, et le comportement
observable par l'utilisateur reste donc le blocage immédiat. Tout le chemin différé est en place et
prouvé par les tests, mais **aucun utilisateur ne peut encore l'emprunter**.

## Décisions et écarts

### 1. Effets requis de l'activation : la table suffit, l'intersection était inutile

Le plan prévoyait `requiredKindsFor(eventKind, producedKinds)` et une intersection avec les kinds
produits par la décision. L'exploration du code a montré que ce paramètre n'apportait rien :
`EffectOutcomes.succeeded(kind)` est un `all {}` sur les outcomes de ce kind, donc **vacuement vrai
pour un kind que la décision n'a pas produit**. Ajouter `SCHEDULE_BLOCKING_START` à `activationRequired`
donne exactement le comportement voulu — une activation immédiate ignore l'effet différé, une
activation différée ignore `APPLY_BLOCKING` — sans changer la signature ni toucher
`SessionReconciler.resumeRelease` et `DefaultSessionCoordinator.completePhase`.

Le plan maître l'anticipait entre parenthèses (« `EffectOutcomes.succeeded` renvoie déjà `true` pour
un kind absent : seule la table change »). L'intersection n'aurait rien protégé de plus : un
`SCHEDULE_BLOCKING_START` que le moteur aurait dû produire et n'aurait pas produit passerait inaperçu
dans les deux conceptions. Ce qui protège, c'est
`SessionCoordinatorActivationTest.aFailedBlockingStartSchedulingRollsBackAndFailsActivation`.

### 2. `BLOCKING_START_RECEIVED` ajouté à §17 — arbitrage tranché avec l'utilisateur

SPEC_ANDROID §12.4 écrit : « Un refus est journalisé et sans effet. » Or §17 est une **liste fermée**
et aucun de ses types ne décrivait « début de blocage reçu ». `BLOCKING_STARTED` ne pouvait pas y
servir : il désigne l'exécution **réussie** d'`APPLY_BLOCKING`, et l'employer pour un refus ferait
lire au journal qu'un blocage a commencé alors qu'il n'a rien commencé.

Premier réflexe, écarté après analyse : ne rien journaliser, comme à l'étape 17 pour le dépassement
de la fenêtre `goAsync()`. Le précédent tient mal ici. À l'étape 17, aucun fait bien défini n'était à
décrire et la fréquence était élevée (un tic toutes les 60 s). Ici le fait est net, rare — un début
de blocage arrive une fois par session — et `ALARM_RECEIVED` fait déjà exactement cela pour le
réveil.

**Ce qui a emporté la décision :** le seul cas réaliste de refus est une alarme de début ayant
survécu à la fin de sa session, `CANCEL_BLOCKING_START` étant best-effort. L'échec de cette
annulation laisse bien une trace en base (outbox `FAILED` + `lastError`), mais §17 n'exporte pas
l'outbox : le déclenchement orphelin qui s'ensuit serait totalement invisible. Or le protocole de
l'étape 24 doit vérifier que « scanner avant l'heure de début → plus aucune alarme Niumi » ;
`dumpsys` prouverait l'absence d'alarme, mais un déclenchement qui passerait quand même resterait
indétectable.

`BLOCKING_START_RECEIVED` est donc ajouté à la liste fermée et journalisé **avant toute décision**,
`sessionId` compris — `null` quand les extras sont invalides. §12.4 est respectée telle qu'écrite,
aucune spec n'a eu à être amendée sur ce point ; §17 porte le nouveau type et sa justification.
Décision validée avec l'utilisateur le 2026-09-16.

### 3. `BlockingScheduleDto.isImmediate` ajouté à `:shared:core` — hors périmètre annoncé

§17 exige de distinguer au journal un `BLOCKING_STARTED` (début différé atteint) d'un `BLOCK_APPLIED`
d'activation. Le critère est « le schedule est différé », c'est-à-dire `isImmediate` — qui existait
dans le domaine (`BlockingSchedule.isImmediate`) mais **pas** dans l'interop, là où vit déjà le
miroir de `isBlockingPending`. Recopier la condition dans `:core:system` aurait fait une troisième
formulation de la règle, ce que le KDoc de `BlockingStatus.kt` interdit explicitement.

L'extension a donc été ajoutée dans `interop/BlockingStatus.kt`, à côté de son homologue. Ajout
purement additif ; `NiumiCoreVersion.SCHEMA_VERSION` reste à 2, monté par l'étape 22 dans le même
lot. C'est le seul fichier de `:shared:core` modifié par cette étape.

### 4. `ReconcilerSources` passe en arguments nommés

Le treizième champ (`blockingStartScheduler`) a fait déraper la construction positionnelle du
`@Provides` : douze arguments de types voisins ont glissé d'un cran et le compilateur a signalé neuf
incompatibilités de type d'affilée. Les deux sites de construction (`SessionModule` et
`TestCoordinatorHarness`) sont désormais en arguments nommés.

## Ce qui a été construit

### `:core:database` — Room v3 et projection v2

- `AlarmSessionEntity` : quatre colonnes nullables (§7.2 v3), `NiumiDatabase` en `version = 3`.
- `MIGRATION_2_3` : additive, sans clause `DEFAULT` — les colonnes sont nullables, et en déclarer une
  ferait échouer `validateMigration` sur cette seule différence (l'inverse exact de `MIGRATION_1_2`).
  Les lignes existantes reçoivent `blockingAppliedAtEpochMillis = createdAtEpochMillis` : les laisser
  nulles les ferait relire comme « blocage en attente » et **lèverait le blocage** d'une session
  active pendant la mise à jour. Schéma `3.json` exporté et committé.
- `SessionSnapshotMapper` et `DirectBootMapper` renseignent les quatre champs **explicitement** dans
  les deux sens ; un test construit le DTO sans les mentionner et vérifie que l'entité les reçoit
  tout de même, pour que les valeurs par défaut transitoires de l'étape 22 cessent de servir ici.
- `DirectBootSnapshot` : `DIRECT_BOOT_PROJECTION_SCHEMA_VERSION = 2`. La lecture d'un fichier v1
  (`projectionSchemaVersion < 2`) rend `IMMEDIATE` + `createdAtEpochMillis`. **La distinction porte
  sur la version du format, jamais sur la nullité des champs** : en v2, un blocage immédiat les laisse
  nuls lui aussi, et les confondre réécrirait l'instant d'application à chaque relecture.
- `RoomBlockedPackagesSource` : branche `ARMED` explicite dans les **deux** `when`. La condition passe
  par `isBlockingPending` de `:shared:core`, atteinte par les mappers existants
  (`toSnapshotDto()`), et jamais par une recopie de la règle. Les deux mappers exposant ce nom, celui
  de Direct Boot est importé sous alias.

### `:core:system` — alarme, receveur, handler, exécuteurs, réconciliation

- `BlockingStartScheduler` / `AndroidBlockingStartScheduler`, calqués sur `AndroidRingingWatchdog` :
  `setExactAndAllowWhileIdle`, double annulation, `SecurityException` → `ANDROID_EXACT_ALARM_DENIED`.
- `BlockingStartPendingIntentSpecs` : sel `0x424C4B53` (« BLKS »), distinct du code nu du réveil et du
  sel du watchdog ; extras `sessionId` (Text) et `revision` (**Number**).
- `BlockingStartReceiver` (`:core:system`, `exported="false"`, `directBootAware="true"`) : coquille
  `goAsync()` bornée à 8 s. Ses constantes d'extras sont redéfinies localement, `AlarmReceiver` vivant
  dans `:feature:ringing` que `:core:system` ne voit pas.
- `BlockingStartHandler` : les gardes d'`AlarmTriggerHandler` (session, révision monotone, snapshot
  illisible) et son `BLOCKING_START_RECEIVED` journalisé avant décision, **sans** la surveillance §13.1 — le début du blocage laisse la session `ARMED`, état où
  §13.1 continue de s'exécuter à chaque réconciliation — et sans contrôle d'état ni de
  `blockingAppliedAtEpochMillis`, que le moteur refuse déjà.
- `ScheduleBlockingStartExecutor` (journalise `BLOCKING_SCHEDULED`, rend
  `Failure("BLOCKING_START_WITHOUT_SCHEDULE")` sur un schedule nul plutôt qu'un `!!`) et
  `CancelBlockingStartExecutor` (best-effort). `ApplyBlockingExecutor` journalise en plus
  `BLOCKING_STARTED` quand le schedule est différé.
- `SessionReconciler.reconcileBlockingStart`, inséré dans `reconcileArmed` **après** la garde de
  permission et la relecture, **avant** le retard du réveil. Trois différences voulues avec
  `reconcileTriggerDelay` : `FIRE_NOW` produit l'événement au lieu de reprogrammer, et ce quelle que
  soit la raison de la passe ; l'incident est `WARNING` et non `DEGRADED` ; l'instant reposé est
  contractuel.
- **Le snapshot est relu entre les deux** : `BLOCKING_START_ELAPSED` incrémente la révision, et
  poursuivre dessus ferait tomber le `TRIGGER_ELAPSED` en `STALE_REVISION` — quatrième occurrence
  potentielle du motif corrigé aux étapes 17 et 18. Prouvé par
  `bothInstantsElapsedApplyTheBlockingFirstThenTheTriggerWithoutStaleRevision`.
- Deux plafonds detekt traités par extraction plutôt que par suppression : `rescheduleAlarm` sort du
  corps de `SessionReconciler`, `base` de celui de `SessionEventFactory`. Le programmateur est lié par
  un `BlockingStartModule` dédié, `SystemModule` étant déjà à onze fonctions.

### `:feature:session` — relecture du dernier package, activation à deux heures

- La détection de la transition `Inactive → Active` vit dans `PersistedBlockedPackagesProjection`,
  **seul point de passage** de tout changement d'état : elle peut venir du rafraîchissement déclenché
  par la publication du snapshot **ou** de l'exécution d'`APPLY_BLOCKING`, et les deux arrivent dans
  la décision `BLOCKING_START_ELAPSED`. N'observer que l'un des deux aurait laissé une course décider
  si l'application déjà ouverte est renvoyée à l'accueil.
- Le service gagne `lastForegroundPackage`, distinct de `lastBlockedPackage` qui ne retenait que les
  blocages effectifs. À l'activation, il rejoue `BlockingDecision.decide` sur ce package, par le même
  chemin et sous le même anti-rebond. La projection est **relue** au moment de décider, la session
  notifiée servant de garde : un scan a pu libérer la session entre-temps.
- `ArmSessionUseCase.preview/arm(localTimeIso, blockingLocalTimeIso)` : `computeBlockingSchedule` sur
  la même zone et le même `now` que le réveil, refus `InvalidBlockingSchedule` **avant** `Blocked` —
  un début mal choisi est une erreur de saisie, pas un défaut d'appareil.
- `ReadinessInput`/`ReadinessReport` transportent le candidat jusqu'à la politique commune, **sans
  quinzième contrôle** (§13, point 4).

## Sentinelles tombées, et pourquoi c'est le signe qu'elles servent

Cinq tests de garde ont échoué à dessein et ont été repris en connaissance de cause :

| Test | Ce qu'il protège |
| --- | --- |
| `ExportedSchemaTest.CURRENT_VERSION` | force le commit de `3.json` |
| `DirectBootSnapshotJsonTest` (doré, 28 → 32 clés) | fige le format persisté de la projection |
| `TechnicalEventTypeTest` | §17 est une liste **fermée** |
| `NiumiBlockingAccessibilityServiceSourceTest` | interdit les jetons d'arbre d'accessibilité **jusque dans les commentaires** — mon propre KDoc l'a déclenché |
| `PhaseCompletionTest` (`containsExactly`) | table des effets requis |

Le quatrième mérite d'être signalé : le garde-fou cherche les jetons littéralement dans le source, et
un commentaire qui les **nomme pour dire qu'on ne les emploie pas** le déclenche. C'est délibérément
strict, et c'est la bonne rigueur pour une garantie faite à Google Play.

## Vérifications exécutées

```
./gradlew testDebugUnitTest :shared:core:jvmTest              # 989 tests, vert
./gradlew :app:assembleDebug                                  # vert
./gradlew ktlintCheck detekt :app:lintDebug                   # vert
./gradlew :core:database:connectedDebugAndroidTest            # 73 tests, vert
./gradlew :app:connectedDebugAndroidTest                      # 10 tests, vert
```

**Appareil :** Xiaomi 25080RABDG, Android 16, HyperOS OS3.0.302.0.WPPEUXM — le même que celui des
étapes 5, 6, 17, 18, 19 et 20.

Les trois tests du Lot 6 passent sur appareil :
`migrationTwoToThreeAddsTheBlockingColumnsAndKeepsTheSessionBlocking`,
`anArmedSessionAwaitingItsBlockingStartReadsAsInactive` / `…WhoseBlockingStartHasPassedReadsAsActive`,
`anExplicitBlockingStartIntentAppliesTheBlockingOfADeferredSession`,
`everyEffectKindOfTheSharedContractHasABoundExecutor` et
`theWakeAlarmIsVisibleAsTheNextAlarmClockButTheBlockingStartNeverIs`.

**Un défaut trouvé sur appareil, dans le test et non dans le code.**
`BlockingStartReceiverInstrumentedTest` attendait que `blockingAppliedAtEpochMillis` soit renseigné
avant de lire le journal. Or le coordinateur **persiste la décision avant d'exécuter le moindre
effet** (SPEC_CORE_KMP §6) : l'attente sortait dès le commit, et la lecture du journal courait contre
`APPLY_BLOCKING`. Course gagnée quand le test tournait seul, perdue en suite — exactement le genre
d'échec intermittent qu'un test mal synchronisé produit. L'attente porte désormais sur
`BLOCKING_STARTED`, dernier maillon de la chaîne, et le test vérifie en plus que l'outbox est vide.

**Le contrôle `dumpsys alarm` de §9.1 est automatisé plutôt que manuel.** Le plan en faisait un
contrôle à l'œil et un critère de clôture ; `BlockingStartAlarmVisibilityTest` le rejoue désormais à
chaque exécution : il programme les deux alarmes, lit `dumpsys alarm` par l'instrumentation et vérifie
que l'instant du début du blocage **ne figure jamais** dans « Next alarm clock information », tandis
que celui du réveil y figure. Mesuré vert sur l'appareil : le réglage système « prochaine alarme »
n'annonce que le réveil.

**Frottement d'outillage rencontré :** HyperOS bloque l'installation par `INSTALL_FAILED_USER_RESTRICTED`
quand « Installer via USB » (options développeur) se désactive — ce qu'il fait périodiquement. Sans ce
réglage, une fenêtre de confirmation apparaît à chaque installation et expire en quelques secondes.
À prévoir dans le protocole de l'étape 24.

Manifeste fusionné vérifié : `BlockingStartReceiver` y figure en `exported="false"` et
`directBootAware="true"` (§14), sans `intent-filter`.

## Ce qui reste à valider — dette assumée

Les tests instrumentés sont **faits et verts** (voir ci-dessus). Restent :

1. **Protocole manuel du blocage différé : reporté à l'étape 24**, décision prise avec l'utilisateur
   avant d'écrire le code. Sans l'écran 5, armer une vraie session différée exigerait un outil de
   debug jetable, que CLAUDE.md et la suppression du POC à l'étape 21 déconseillent. Le plan maître
   autorise explicitement ce report. À dérouler à l'étape 24, dans cet ordre :
   - `dumpsys alarm | grep niumi` → **deux** alarmes distinctes (`setAlarmClock` du réveil,
     `setExactAndAllowWhileIdle` du début) et **aucune** « prochaine alarme » système à l'heure du
     début — si le début y apparaît, retour à §9.1 avant de continuer ;
   - ouvrir une application choisie avant l'heure → **aucun** blocage ;
   - y rester à l'heure du début → retour à l'accueil et overlay **sans changer de fenêtre** (c'est
     le seul essai qui prouve la relecture du dernier package) ;
   - `am kill` avant l'heure → blocage appliqué à l'heure ;
   - redémarrer avant l'heure sans déverrouiller → alarme de début reprogrammée ;
   - redémarrer 20 min après l'heure de début → `MISSED_BLOCKING_START_WINDOW`, réveil intact ;
   - scanner avant l'heure de début → `CANCELLED`, plus aucune alarme Niumi.

2. **Confirmation en Doze** que `setExactAndAllowWhileIdle` tient pour cette alarme comme il tient
   pour le watchdog (mesuré le 2026-09-15, §9.1). Non transposé sans mesure : l'alarme de début est
   posée une seule fois, là où le watchdog se réarme toutes les 60 s, et rien ne prouve que le seau
   d'App Standby les traite identiquement.

**Point de vigilance 13, partiellement levé.** Le repli du moteur reste un filet ; ce qui prouve la
**fonctionnalité** est le chemin normal, et il est désormais observé sur appareil :
`BlockingStartReceiverInstrumentedTest` déroule `Intent` → receveur → handler → moteur →
`APPLY_BLOCKING` → projection active, avec `BLOCKING_STARTED` au journal. Ce qu'aucun essai n'a encore
observé, c'est ce même chemin **déclenché par une vraie alarme à l'heure dite**, écran éteint — l'essai
de la matrice §20 qui appartient à l'étape 24.
