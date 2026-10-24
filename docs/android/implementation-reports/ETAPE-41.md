# Étape 41 — Contrat KMP 1.5 (1/2) : planning, prochaine occurrence, désarmement, `releasedBy`

Date : 2026-10-24. **Étape terminée** : code, tests JVM, framework iOS, ktlint, detekt, lint et
assemblage. Aucun test manuel ni instrumenté n'est requis : le module commun change, et Android ne
produit pas encore les nouveaux états. Neuvième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 12, phase O). Point de départ : `23bcae9`.

## Ce qui est livré

- **Prochaine occurrence d'un planning** (`:shared:core`, `schedule/`) : `WakePlan`
  (jours ISO de réveil, heure de réveil, heure de coucher), `NextOccurrenceInput`,
  `NextOccurrenceResult` (`VALID`, `NO_DAY_ENABLED`, `INVALID_TIME`, `UNKNOWN_ZONE`,
  `BEDTIME_EQUALS_WAKE`), `NextOccurrence(wakeSchedule, blockingSchedule, blockingStartsImmediately)`
  et `NextOccurrenceCalculator`. Le calculateur retient le premier jour coché, d'aujourd'hui à dans
  sept jours, dont le réveil est strictement futur. Le coucher a lieu la veille s'il est plus tardif
  que le réveil, sinon le jour même. Un coucher déjà atteint donne un blocage immédiat. Les valeurs
  hors de 1..7 sont ignorées. La résolution des heures locales (trou de printemps, chevauchement
  d'automne) est celle de `WakeScheduleCalculator`, dont trois fonctions passent de `private` à
  `internal`, sans changement de comportement.
- **Désarmement** : `SessionState.DISARMED`, `ReleaseTarget.DISARMED`,
  `SessionEventKind.DISARM_REQUESTED` et `DisarmReducer`. Depuis `ARMED`, avec le blocage en attente
  et strictement avant le coucher, la session passe en `RELEASING` vers `DISARMED`
  (`releasedBy = PLAN_CHANGE`). Effets : publier, annuler l'alarme, annuler le début du blocage.
  `RELEASE_SUCCEEDED` mène ensuite à `DISARMED` et renseigne `disarmedAtEpochMillis`. Refus :
  - hors de `ARMED` : `INVALID_STATE_TRANSITION` ;
  - blocage déjà demandé : `BLOCKING_ALREADY_APPLIED` ;
  - à ou après le coucher : `BLOCKING_START_ALREADY_ELAPSED`, nouveau code.
- **Moyen de libération** : `ReleaseMeans { NFC_SCAN, EMERGENCY_EXIT, PLAN_CHANGE }`, plus les
  champs `releasedBy` et `disarmedAtEpochMillis` en fin de `SessionSnapshot` et de
  `SessionSnapshotDto`, avec la valeur par défaut `null`. `VALID_NFC_SCANNED` renseigne `NFC_SCAN`.
  `ReleaseReducer` remplace l'invariant « `nfcVerifiedAt` avant `RELEASING` » :
  - la cible et `releasedBy` sont présents ;
  - `nfcVerifiedAt` existe si et seulement si `releasedBy` vaut `NFC_SCAN` ;
  - la cible `DISARMED` exige `PLAN_CHANGE`.

  `EMERGENCY_EXIT` n'est encore produit par aucun événement (étape 42).
- **Lecture d'un snapshot sans `releasedBy`** (`SessionSnapshotDto.toDomain()`) : la façade déduit
  `NFC_SCAN` si `nfcVerifiedAtEpochMillis` est renseigné, sinon laisse `null`, **quelle que soit
  `schemaVersion`** (voir « Écarts »).
- **Façade** : huitième méthode `computeNextOccurrence(NextOccurrenceInputDto)`, DTO `WakePlanDto`,
  `NextOccurrenceInputDto`, `NextOccurrenceDto`, `NextOccurrenceResultDto`, alias
  `NextOccurrenceStatusDto` et `ReleaseMeansDto`. `SessionSnapshot.SCHEMA_VERSION` et
  `NiumiCoreVersion.SCHEMA_VERSION` passent à 3. Le framework iOS exporte
  `computeNextOccurrence(input:)`.
- **`ActivationPolicy`** : comportement inchangé. Une liste de contrôles vide était déjà acceptée ;
  c'est désormais documenté et prouvé (décision 5 du lot : contrôles non bloquants, appliquée côté
  Android à l'étape 44).
- **`SessionEngine`** : les cinq événements qui mènent à `RELEASING` ou en sortent (deux scans,
  désarmement, réussite et échec du nettoyage) passent par un aiguillage séparé, `reduceRelease`. Le
  treizième événement portait `reduce` à une complexité de 15, au-delà du seuil detekt de 14.

## Décisions de Mehdi (2026-10-24)

- **Désarmement refusé dès le coucher** : refus à ou après `startsAtEpochMillis`, même si l'alarme de
  début n'a pas encore produit `BLOCKING_START_ELAPSED` (téléphone éteint, retard). Le plan ne
  vérifiait que « avant le réveil ».
- **Q8** : `BEDTIME_EQUALS_WAKE` aussi quand le trou de printemps ramène deux heures distinctes au
  même instant (coucher 2:15, réveil 2:45 le 2026-03-29).
- **D17** : reporté à l'étape 45. Le moteur répond seulement `NO_DAY_ENABLED` ; le comportement de
  l'interrupteur et le texte de la carte se décideront avec l'accueil.
- **Q9, autrement que recommandé** : avant le coucher, un changement de fuseau désarme la session en
  attente et la réarme à l'heure locale du nouveau fuseau ; après le coucher, l'instant est conservé.
  Rien à coder ici (le calculateur reçoit le fuseau) ; consigné dans SPEC_CORE_KMP §2 (décision 9)
  et §8.4, et à l'étape 44 du plan.
- **Adaptation minimale d'Android**, acceptée après explication de ce qu'elle couvre (ci-dessous).

## Écarts au plan

1. **Android touché.** Le plan affirmait « aucun test Android modifié ». C'était faux : `DISARMED`
   casse huit `when` exhaustifs de production. Ils reçoivent une branche « état final », inatteignable
   puisque Android ne produit pas `DISARMED` avant l'étape 44 :
   - `SessionStates.isBlockingInForce`, `SessionReconciler` (efface le pointeur),
     `RingingServiceRecovery`, `RingingWatchdogPolicy`, `LauncherDestination` : rangés avec les états
     finaux ;
   - `PublishSnapshotExecutor` : aucun journal (`SESSION_DISARMED` à l'étape 44) ;
   - `AlarmUiState` : sortie vers l'accueil ;
   - `ActiveSessionTexts` : libellé transitoire « Session désarmée ». Cet écran est supprimé à
     l'étape 45 et n'est jamais affiché dans cet état ; aucun texte de la spec ne couvre ce cas.

   `SESSION_FINAL_STATES` reçoit `DISARMED` (avancé de l'étape 43), tout comme sa copie de
   `RoomBlockedPackagesSource`. Quatre tests listaient les états finaux en dur et sont adaptés :
   `AlarmUiStateTest` (+1 essai sur `DISARMED`), `HomeDestinationTest`, `SetupGateTest`,
   `RuntimeStatusGapsTest`. Un commentaire de `ActiveSessionTextsTest` est corrigé.
2. **Déduction de `releasedBy` indépendante de `schemaVersion`.** Room v5 n'a pas de colonne
   `releasedBy` et relit donc chaque snapshot avec `null`, alors que le moteur écrit désormais en
   version 3. Une déduction réservée aux versions inférieures à 3 aurait fait refuser
   `RELEASE_SUCCEEDED` à toute session libérée par scan, qui serait restée en « Déblocage en cours ».
   Le test `aVersionThreeSnapshotStoredWithoutReleaseMeansIsReadAsReleasedByScan` le prouve.
3. **Table de transitions interdites à 10 × 13** à cette étape, et non 10 × 14 :
   `EMERGENCY_EXIT_CONFIRMED` arrive à l'étape 42.
4. **`SessionEventValidation` non modifiée** : `DISARM_REQUESTED` ne porte aucune charge, et les
   contrôles génériques refusent toute charge inattendue (`aRequestCarryingAPayloadIsRejected`).
5. **Fixtures partagées** : six transitions ajoutées à `session_transitions.json` (désarmement
   accepté, refusé au coucher, refusé blocage appliqué, refusé hors `ARMED`, fin vers `DISARMED`,
   refus depuis `DISARMED`), et un nouveau `wake_plans.json` de douze cas, lus par `FixturesTest`.

## Ce que l'adaptation Android ne couvre pas, volontairement

- Colonnes Room et projection Direct Boot pour `releasedBy` et `disarmedAtEpochMillis` : Room v6 et
  projection v5, étape 43.
- Thème, garde du blocage et écran de réveil pendant `RELEASING` vers `DISARMED` : ils sont encore
  traités comme une libération par scan (`isSessionDark`, `isBlockingInForce`,
  `requiresAlarmScreen`). Étape 43, inscrit au plan.
- Traitement de `DISARM_REQUESTED` par le coordinateur : `DefaultSessionCoordinator` lève une erreur
  sur un événement inattendu, ce qui ne peut pas arriver avant l'étape 44 ; même chose pour le
  journal `SESSION_DISARMED`.

## Specs et documents modifiés

- `specs/SPEC_CORE_KMP.md` :
  - en-tête « 1.5 en cours » ;
  - §2 : décisions 4, 9 et 15 ;
  - §3.1, §4 (invariants `releasedBy` et désarmement) ;
  - §5 (enums, `ReleaseMeans`, diagramme, transition, refus) ;
  - §6 (événement, ligne d'effets, effets requis), §7.1 (champs, schéma 3, lecture), §7.3 (code) ;
  - **§8.4, nouvelle** (prochaine occurrence) ;
  - §10 (contrôles non bloquants, engagement), §12 (désarmement), §13 (montée à 3) ;
  - §14 (méthode, DTO, version 3), §17 (tests), §19 (quatre lignes iOS 1.5).
- `specs/SPEC_IOS.md` (contrat seulement) : §8 (`releasedByCode`, `disarmedAt`, dix états), §9
  (diagramme, transition, refus), §11 (calcul de la prochaine occurrence), §15 (`releasedBy`,
  désarmement).
- `specs/SPEC_ANDROID.md` §7.2 : note « Contrat KMP 1.5 avant Room v6 ».
- Plan de refonte : lignes Q8, D17 et Q9 du tableau ; étape 41 (écarts, cases, critère de fin) ;
  étape 43 (`SESSION_FINAL_STATES` déjà fait, `RELEASING` selon la cible) ; étape 44 (Q9,
  `BLOCKING_START_ALREADY_ELAPSED`, aiguillage du coordinateur, `SESSION_DISARMED`).
- `design-plan/POINTS-OUVERTS.md` : D17 annoté « reporté à l'étape 45 ».

## Tests

- **JVM** : 1 626 tests, aucun échec (KMP 255, Android 1 371), contre 1 577 à l'étape 40.
  - Nouveaux : `NextOccurrenceCalculatorTest` (16 essais) et `SessionEngineDisarmTest` (10).
  - Étendus : `SessionEngineReleaseTest` (moyens de libération incohérents refusés),
    `SessionEngineNfcTest` (`releasedBy`), `SessionEngineForbiddenTransitionsTest` (`DISARMED`,
    `DISARM_REQUESTED`), `DtoRoundTripTest` (snapshots v2 et v3 sans `releasedBy`, `DISARMED` en
    JSON), `NiumiCoreFacadeTest` (`computeNextOccurrence`), `ActivationPolicyTest` (liste vide),
    `SmokeTest` (version 3), `FixturesTest`.
  - Échecs vérifiés avant implémentation : compilation pour le calculateur ; `SmokeTest` et une
    fixture incomplète, corrigée.
- **Framework iOS** (`linkDebugFrameworkIosSimulatorArm64`) : construit ; l'en-tête exporte
  `computeNextOccurrence(input:)`, `ReleaseMeans` et `disarmedAtEpochMillis`.
- **ktlint, detekt, lint, `assembleDebug`** : verts. Detekt a refusé deux fois avant correction :
  complexité de `SessionEngine.reduce` (aiguillage scindé), puis longueur de
  `FixturesTest.resolveSnapshot` et de `SessionReconciler.reconcile` (branche gardée sur une ligne).

## Validation sur appareil

Sans objet : aucun comportement Android ne change, et le désarmement n'est pas encore déclenché par
Android. La séquence réelle (désarmer puis réarmer, changement de fuseau avant le coucher) sera
prouvée par les tests du coordinateur à l'étape 44, puis sur appareil à partir de l'étape 45.
