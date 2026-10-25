# Étape 44 — `WakePlanScheduler`, désarmement, sortie de secours et ré-armement dans le coordinateur

Date : 2026-10-24, tests instrumentés le 2026-10-25. **Étape terminée** : code, tests JVM, tests
instrumentés de `:core:system` et `:app` sur appareil, ktlint, detekt, lint et assemblage. Aucun essai
manuel n'est prévu à cette étape : l'interface arrive à l'étape 45. Douzième étape du plan de
refonte `docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 13, phase P). Point de départ :
`fc2b872`.

## Ce qui est livré

- **Le verrou du coordinateur est exposé.**
  - `SessionCoordinator.withLock(block)` exécute un bloc sous le mutex de `dispatch` et `reconcile`.
  - Le bloc reçoit une `LockedSession` (`load`, `clearActive`, `dispatch`, `reconcile`) dont les
    fonctions ne reprennent pas le verrou, qui n'est pas réentrant.
  - Aucun événement ne peut donc s'intercaler entre le désarmement d'une session et l'armement de
    la suivante (point de vigilance 8).
- **Désarmement et sortie de secours refermés par le coordinateur.**
  - `PhaseCompletion` donne à `DISARM_REQUESTED` et `EMERGENCY_EXIT_CONFIRMED` les effets requis du
    scan. Un effet non produit est satisfait d'office : le désarmement n'exige donc que
    `CANCEL_ALARM`.
  - `RELEASE_SUCCEEDED` ou `RELEASE_FAILED` suivent dans la même dispatch.
    `RELEASE_PARTIAL_FAILURE` vaut pour les trois kinds.
  - Avant l'étape 44, ces deux kinds n'avaient aucun effet requis : une session désarmée ou sortie
    par la recopie serait restée en `RELEASING` jusqu'à la réconciliation suivante.
  - `PublishSnapshotExecutor` journalise `DISARMED` en `SESSION_DISARMED`.
- **Fabriques.**
  - `SessionEventFactory.disarmRequested(snapshot)` est une fonction de fichier, la classe étant
    au plafond `TooManyFunctions`.
  - `EmergencyExitEventFactory` (`SYS/emergency/`) construit la sortie de secours en deux temps,
    `verificationContext`, puis `emergencyExitConfirmed(context, proof)`. C'est le patron de
    `NfcScanEventFactory` : la preuve lie l'identité de l'événement avant la vérification.
- **Armement d'une occurrence** (`ArmOccurrenceUseCase`, `SYS/plan/`).
  - La sélection, la sonnerie et les Niumi Spots sont lus au moment d'armer.
  - `evaluateActivation` est appelée **sans contrôle de préparation** (`checks = emptyList()`,
    SPEC_CORE_KMP §10). Restent les bornes communes : un à dix Niumi Spots, une à cinquante
    applications, réveil dans le futur.
  - `ACTIVATION_REQUESTED` est envoyé par la `LockedSession`.
  - `ActivationFailure` et `ArmSessionResult` passent de `:feature:session` à `SYS/plan/`, avec
    `InvalidPlan` en plus et un `diagnosticCode` pour le journal.
- **Planning** (`DefaultWakePlanScheduler`, qui implémente `WakePlanScheduler` et `SessionPassHook`).
  - `apply(plan, reason)`, sous le verrou :
    - session finie encore pointée : pointeur effacé ;
    - session en attente : désarmée (`DISARMED`) ;
    - du coucher au scan : `LockedBySession`, sans rien enregistrer. Si le coucher est atteint
      mais pas encore traité, le moteur refuse le désarmement (`BLOCKING_START_ALREADY_ELAPSED`) :
      le scheduler applique alors le blocage, comme `BlockingStartHandler` ;
    - ensuite : enregistrement du planning, calcul de l'occurrence (`computeNextOccurrence`, fuseau
      courant), puis armement ;
    - issues : `Disabled`, `NoDayEnabled`, `Unchanged`, `Armed` ou `Failed`.
  - **Crochet de passe** : `HookedSessionCoordinator` enveloppe le coordinateur et appelle le
    scheduler après chaque `dispatch` et chaque `reconcile`, sous le même verrou.
    - `COMPLETED` (scan ou recopie du matin) arme l'occurrence suivante.
    - `CANCELLED` (annulation, recopie du soir) éteint l'interrupteur.
    - `DISARMED` repris par le réconciliateur arme l'occurrence.
    - Au démarrage, au déverrouillage et au `BOOT` sans session, planning allumé, l'occurrence est
      armée.
    - Sur `TIME_CHANGED` et `TIMEZONE_CHANGED`, la session en attente est recalculée ; elle n'est
      désarmée et réarmée que si un instant change.
    - Rien n'est fait avant déverrouillage.
  - `ReconcileAction` gagne `PlanRearmed`, `PlanRearmFailed` et `PlanSwitchedOff`.
    `PointerCleared` porte désormais le snapshot final.
  - **Échec d'armement** (Q14) : l'interrupteur reste allumé. L'échec est journalisé
    (`PLAN_REARM_FAILED` avec code), publié dans `PlanArmingStatus` (en mémoire, `Ok` ou
    `Failed`) et retenté au démarrage et au déverrouillage.
- **Journal technique** (§17) : `SESSION_DISARMED`, `PLAN_REARMED`, `PLAN_REARM_FAILED`,
  `EMERGENCY_EXIT_CONFIRMED`.
- **Hilt.**
  - `SessionModule` fournit le coordinateur enveloppé, avec un `Provider<SessionPassHook>`.
  - `PlanModule` fournit `PlanArmingStatus`, `ArmOccurrenceUseCase` et `DefaultWakePlanScheduler`
    (singleton), lié à `WakePlanScheduler` et à `SessionPassHook` : une seule instance.
- **Écran 6 transitoire.** `ArmSessionUseCase` garde son diagnostic (aperçu, refus sur un contrôle
  bloquant) et délègue l'armement à `ArmOccurrenceUseCase` sous le verrou. Il n'écrit jamais le
  planning : resté éteint, celui-ci ne réarme rien après une session ponctuelle. Le comportement
  visible est inchangé jusqu'à l'étape 45.

## Décisions de Mehdi (2026-10-24)

1. **Q14.** Un ré-armement échoué laisse l'interrupteur allumé. Une fenêtre d'erreur et une
   notification l'annoncent ; elles arrivent à l'étape 52, quand leurs textes seront fixés dans
   `SPECIFICATION.md`. L'étape 44 livre l'état observable, le journal et le nouvel essai.
2. **Horloge comme fuseau.** Q9 ne visait que le fuseau. `TIME_CHANGED` suit la même règle, et
   dans les deux cas le désarmement n'a lieu que si un instant recalculé change.
3. **Décision 11 amendée.** La sélection d'applications se modifie avant le coucher (désarmement
   puis nouvelle session) et reste verrouillée du coucher au scan.
4. **Q12.** La sonnerie reste modifiable en `ARMED` seulement.

## Écarts au plan

1. **`withLock` et `LockedSession` ajoutés.** Le plan citait un `SessionCoordinator.withLock` qui
   n'existait pas, et le mutex n'est pas réentrant.
2. **Décorateur plutôt que septième dépendance.** Le crochet passe par `HookedSessionCoordinator`.
   Une septième dépendance du coordinateur aurait dépassé `LongParameterList`, et le cycle
   coordinateur ↔ scheduler n'est résolu que par `Provider`.
3. **`ArmOccurrenceUseCase` dans `SYS/plan/`** et non dans `SESSION/activation/`. C'est le
   coordinateur (`:core:system`) qui déclenche le ré-armement. `ArmSessionUseCase` reste, et
   délègue.
4. **Pas de `rearmAfterFinalState` public.** Le ré-armement est interne au crochet.
5. **`PlanApplyResult.Unchanged` ajouté**, pour un recalcul d'horloge ou de fuseau sans écart.
6. **Sortie de secours en deux temps.** La signature `emergencyExitConfirmed(snapshot, proof)` du
   plan était intenable : la preuve lie `eventId` et `occurredAt` avant la vérification.
7. **`EVENING_REMINDER_SCHEDULED` reporté à l'étape 52**, qui programme le rappel.
8. **Pas de `DisarmResult.kt`.** `PlanApplyResult` suffit.
9. **Le planning reçu n'est enregistré que si la session le permet.** Il ne l'est pas en
   `LockedBySession`. Désarmement interrompu (`RELEASING`) : il l'est, puisque la cible `DISARMED`
   est acquise, et c'est lui que la reprise arme (corrigé après relecture). Un planning invalide ou
   un appel avant déverrouillage est refusé avant de toucher à la session.
10. **Plan corrigé** : la borne du désarmement est « avant `blockingStartsAtEpochMillis` »
    (l. 362, périmée depuis l'étape 41).

## Ce que cette étape ne couvre pas, volontairement

- Aucun écran ne lit encore le planning ni `PlanArmingStatus` : l'accueil arrive à l'étape 45, la
  fenêtre et la notification de Q14 à l'étape 52.
- Le cas d'usage de la recopie (écran 9) arrive à l'étape 49. Seuls le coordinateur et la fabrique
  sont prêts.
- **Fin d'une session ponctuelle avec planning allumé.** Si l'utilisateur allume le planning à
  l'étape 45 pendant qu'une session ponctuelle de l'écran 6 est armée, `apply(SWITCH)` la désarme
  et arme l'occurrence. C'est voulu, et inaccessible avant l'étape 45.
- **Après un réveil terminé, le dernier snapshot publié est la nouvelle session `ARMED`**, et non
  `COMPLETED`, dès que le planning est allumé. L'écran 11 de l'étape 46 devra s'appuyer sur le
  résultat du scan, et non sur le seul `SessionSnapshotPublisher`. Sans effet aujourd'hui : le
  planning est éteint.

## Specs et documents modifiés

- **SPEC_ANDROID** :
  - §7.2 : `DISARM_REQUESTED` produit depuis l'étape 44 ;
  - §8 : exception au « même instant » pour une session en attente avec le planning allumé ;
  - §9.2 : activation depuis le planning, contrôles non bloquants, fin de session, échec
    d'armement, écran 6 transitoire ;
  - §9.3 : recalcul sur `TIME_CHANGED` et `TIMEZONE_CHANGED`, ré-armement seulement après
    déverrouillage ;
  - §11.3 : désarmement et sortie de secours ;
  - §17 : quatre événements, sept événements d'état.
- **SPEC_CORE_KMP** : décision 9 (horloge comme fuseau, désarmement seulement si un instant
  change), décision 11 (amendée), §10 (contrôles non transmis, sélecteur).
- **SPEC_IOS** §11 : parité des décisions 9 et 11.
- **Plan de refonte** :
  - tableau des décisions (Q12, Q14, horloge, décision 11) ;
  - interfaces transverses (scheduler, crochet, fabriques, actions, journal) ;
  - borne du désarmement ;
  - étape 44 (écarts, cases) ;
  - étape 52 (lecture de `PlanArmingStatus`).

## Tests

- **JVM** : 1 759 tests, aucun échec (KMP 303, Android 1 456), contre 1 699 à l'étape 43.
  - **Nouveaux** :
    - `WakePlanSchedulerTest` (14) ;
    - `SessionReconcilerRearmTest` (12) ;
    - `SessionCoordinatorRearmTest` (9) ;
    - `ArmOccurrenceUseCaseTest` (7) ;
    - `SessionCoordinatorDisarmTest` (5) ;
    - `SessionCoordinatorEmergencyExitTest` (5) ;
    - `EmergencyExitEventFactoryTest` (2) ;
    - `SessionCoordinatorMutexTest`, +2 : `withLock` sérialisé avec `dispatch`, `LockedSession`
      sans interblocage ;
    - `PhaseCompletionTest`, +2 ;
    - `SessionEventFactoryTriggerTest`, +1.
  - **Réécrits** :
    - `PhaseCompletionTest.otherEventKindsHaveNoRequiredEffects`, qui couvre tous les kinds ;
    - `TechnicalEventTypeTest` (+4) ;
    - `ArmSessionUseCaseTest` et `SummaryViewModelTest` (construction) ;
    - les quatre faux de `SessionCoordinator` (`withLock`).
  - **Échecs vérifiés avant implémentation** :
    - `TechnicalEventTypeTest` ;
    - `PhaseCompletionTest` et `SessionCoordinatorDisarmTest` (aucun effet requis, `RELEASING`
      jamais refermé, `SESSION_DISARMED` absent) ;
    - `SessionCoordinatorEmergencyExitTest` (journal absent) ;
    - `ArmOccurrenceUseCaseTest` (7/7, implémentation vide) ;
    - `WakePlanSchedulerTest` (12/12) ;
    - les tests du crochet (12/19 : les sept autres affirment qu'il ne se passe rien).
  - **Écrits après le code**, donc jamais vus en échec : `EmergencyExitEventFactoryTest`,
    `disarmRequestedCarriesTheSnapshotRevisionAndNoPayload` et les deux tests du verrou, ajoutés en
    même temps que `withLock` (échec à la compilation seulement).
- **Instrumentés** : lancés sur appareil (voir plus bas). Le graphe Hilt (cycle coordinateur ↔ scheduler
  par `Provider`) est validé à la compilation (`hiltJavaCompileDebug`, `compileDebugAndroidTestKotlin`).
  `EffectExecutorCoverageTest` n'injecte que la table des exécuteurs : la seule preuve à l'exécution
  est `ProcessDeathInstrumentedTest` (`:app`), qui résout le vrai coordinateur.
- **Framework iOS** : non relancé, aucun changement dans `:shared:core`.
- **ktlint, detekt, `:app:lintDebug`, `:app:assembleDebug`** : verts. Refus de detekt corrigés :
  - complexité et nombre de `return` du scheduler, découpé en `releaseCurrent` et `armNext` ;
  - faux positif `UnusedPrivateProperty` sur un objet anonyme, devenu une classe interne ;
  - une ligne trop longue ;
  - une fonction de test à six paramètres.

## Relecture indépendante

Une relecture du diff complet, par un agent distinct, n'a relevé aucun interblocage ni double
armement. Elle a vérifié que tous les appelants de production reçoivent le coordinateur enveloppé
hors de tout bloc verrouillé, et que le comportement transitoire, planning éteint, est inchangé.

- **Important, corrigé.** **Un désarmement interrompu réarmait l'ancien planning.**
  - Scénario : `CANCEL_ALARM` échoue, la session reste `RELEASING`, et le planning reçu n'était
    pas enregistré. La reprise de l'outbox refermait ensuite en `DISARMED`, et le crochet réarmait
    l'**ancien** planning.
  - Conséquence : la modification était perdue. Après une extinction, un réveil était réarmé.
  - Mon test figeait ce défaut : il attendait 07:00 après une demande à 06:30.
  - Correction : le planning est enregistré dès que le moteur a accepté `DISARM_REQUESTED`. Une
    extinction interrompue n'annonce plus d'échec d'armement. Les deux échecs ont été vus avant la
    correction.
- **Important, laissé à trancher (étape 52).** Le nouvel essai de Q14 n'a lieu qu'au démarrage du
  processus et au **premier** déverrouillage après redémarrage, seul moment où Android émet
  `ACTION_USER_UNLOCKED`. Tant que le processus vit, corriger la permission puis revenir dans
  l'application ne réessaie pas. C'est écrit dans SPEC_ANDROID §9.2 et inscrit à l'étape 52 du plan
  comme question pour Mehdi.
- **Mineurs, corrigés** :
  - une exception du planning dans le crochet remontait à l'appelant d'une passe déjà enregistrée
    (plantage possible après un scan accepté). Elle est désormais journalisée et publiée comme un
    échec d'armement (`PLAN_HOOK_FAILED`) ; échec vu avant la correction ;
  - `apply` désarmait avant qu'une écriture refusée (planning invalide, appareil verrouillé) ne
    lève : les deux refus passent en tête ; échecs vus avant la correction ;
  - la réconciliation lisait le planning et Room quelle que soit la raison, `BEFORE_SCAN` compris :
    la raison est testée d'abord ;
  - `ActivationSources.alarmSoundPreferences` était devenu inutile : retiré ;
  - le rapport et le plan disaient qu'`EffectExecutorCoverageTest` prouverait le cycle Hilt ;
  - « `DISARM_REQUESTED` uniquement par le scheduler » documenté sur `disarmRequested` ;
  - délai de 8 s du receveur noté en §9.3.
- **Mineur, laissé** : `aSessionFinishedOnALockedDeviceIsRearmedOnlyAfterUnlock` passe par le
  chemin « sans session », car le faux gateway efface le pointeur même verrouillé. Le chemin réel
  (pointeur Room conservé) est couvert par `aCompletionStillPointedAtUnlockArmsTheNextOccurrence`.
- **Pour l'étape 49** : si la recopie est possible avant déverrouillage, vérifier que l'état final
  atteint Room (`clearActive` vide la projection Direct Boot avant la fusion).

## Validation sur appareil

Faite le 2026-10-25 sur Xiaomi 25080RABDG, Android 16.

- **`:core:system:connectedDebugAndroidTest`** : 38 sur 38.
- **`:app:connectedDebugAndroidTest`** : 16 sur 17 au premier passage.
  - `ProcessDeathInstrumentedTest` (4 sur 4), le seul à résoudre le vrai coordinateur enveloppé et
    son crochet : le cycle Hilt se construit et la réconciliation de démarrage fonctionne.
  - `BlockingGuardServiceInstrumentedTest.fromTheAlarmTheGuardTakesOverTheAlarmNotification` a
    échoué sur une notification d'identifiant 8 en trop. C'est l'avertissement « accessibilité »
    laissé par un test précédent du même passage : même symptôme qu'à l'étape 43, sans lien avec
    cette étape. Relancée seule, la classe passe (3 sur 3).
  - **Corrigé le 2026-10-25, avec l'accord de Mehdi** : le test efface les notifications de Niumi
    au début de chaque cas (`notificationManager.cancelAll()` dans `setUp`), et l'assertion « une
    seule notification reste » est inchangée. Deux passages complets de `:app:connectedDebugAndroidTest`
    à la suite : 17 sur 17 chacun.

Aucun essai manuel : il faut l'accueil de l'étape 45. Les tests instrumentés de `:app` laissent le
service d'accessibilité arrêté : il est à réactiver avant tout essai.

**Restent non validés**, à l'étape 45, avec l'accueil :
  - un vrai `DISARMED` (heure changée avant le coucher) ;
  - le ré-armement après le scan du matin ;
  - un `TIMEZONE_CHANGED` réel ;
  - le ré-armement au déverrouillage après un redémarrage ;
  - un échec d'armement (alarme exacte retirée) puis le nouvel essai.
