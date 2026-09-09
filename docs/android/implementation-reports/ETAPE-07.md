# Étape 7 — machine à états `SessionEngine`

Date : 2026-09-08. Réducteur pur du moteur métier commun (`:shared:core`), intégralement en
Kotlin JVM/commun, sans dépendance Android ni Xcode pour l'exécution des tests. Aucun fichier
Android touché.

## Résumé

L'étape 7 livre `SessionEngine.reduce(snapshot, event): SessionDecision`, seul juge des
transitions de session pour Android et iOS (SPEC_CORE_KMP §5, §6, §7). C'est le prérequis dur du
`SessionCoordinator` de l'étape 11 : jusqu'ici, chaque composant Android (receiver, service,
service d'accessibilité) décidait localement de son propre état.

Deux lacunes des spécifications ont été arbitrées avec l'utilisateur avant implémentation et
corrigées dans `specs/SPEC_CORE_KMP.md` dans le même changement (voir « Décisions validées »).
Un écart au plan d'origine a été nécessaire (validation hors `init`, voir point 1 ci-dessous).

105 tests JVM verts (53 hérités des étapes 1-2, 52 nouveaux), ktlint et detekt verts sur
l'ensemble du dépôt, `:shared:core:compileKotlinIosSimulatorArm64` vert.

## Décisions validées avec l'utilisateur (2026-09-08)

1. **`INVALID_APP_SELECTION`, 14e code de violation.** SPEC_CORE_KMP §7.4 refuse une activation
   dont `count` est hors de 1..50, mais aucun des 13 codes de §7.3 ne couvrait ce refus.
   `UNEXPECTED_EVENT_PAYLOAD` aurait alors porté deux causes distinctes (charge fournie hors de
   son événement, sélection hors bornes), rendant le diagnostic natif ambigu. §7.3 dit « au
   minimum les codes suivants » : l'ajout est légitime. Spec mise à jour.
2. **Effets d'`INCIDENT_REPORTED` : `RECORD_INCIDENT` puis `PUBLISH_PLATFORM_SNAPSHOT`.** La
   table des effets de §6 ne couvrait pas cet événement autonome (elle couvre `RELEASE_FAILED`
   mais pas `INCIDENT_REPORTED`). Le même ordre que `RELEASE_FAILED` est retenu : sans
   `RECORD_INCIDENT`, la cause de la dégradation ne serait pas rejouable depuis l'outbox après
   interruption (exigé par §17) et un incident `CRITICAL` n'aurait rien à présenter dans le
   diagnostic visible imposé par §7.3. Spec mise à jour.

## Écart au plan d'origine

3. **Aucune validation dans `init` de `SessionEvent`.** Le plan demandait à la fois que
   `SessionEvent` « valide dans `init` » et que les violations soient « produites par `reduce`,
   pas par une exception » — deux exigences incompatibles, et SPEC_CORE_KMP §14 interdit
   explicitement qu'une exception traverse la frontière native (le `SessionEventDto → SessionEvent`
   de l'étape 8 doit produire une violation typée côté Swift, jamais un crash). Retenu : aucun
   `require` dans les constructeurs du domaine ; toute la validation de forme vit dans
   `SessionEventValidation.validate(snapshot, event)`, appelée en tête de `reduce()`.

## Ce qui a été construit

### Domaine (`shared/core/src/commonMain/kotlin/com/niumi/core/domain/`)

29 nouveaux fichiers (le dossier en contient 30 avec `NiumiCoreVersion.kt`, hérité de l'étape 1),
un type par fichier pour l'essentiel (conforme au plan), plus quelques fichiers de découpage
interne non listés au plan mais nécessaires pour respecter les seuils de complexité par défaut de
detekt
(`config/detekt/detekt.yml` ne les assouplit pas : `CyclomaticComplexMethod` 15, `LongMethod` 60,
`TooManyFunctions` 11, `ReturnCount` 2, `LongParameterList` 5) :

- **Types** : `SessionState`, `ReleaseTarget`, `SessionHealth`, `IncidentSeverity`, `Platform`,
  `SessionIncident`, `WakeSchedule`, `AppSelectionSummary`, `ActivationRequest`, `SessionSnapshot`
  (compagnon `SCHEMA_VERSION = 1`), `SessionEventKind`, `SessionEvent`, `SessionEffectKind`,
  `SessionEffect`, `SessionEffectPayload`/`IncidentEffectPayload`, `DomainViolation`,
  `ViolationCode` (14 constantes), `IncidentCodes` (9 codes + `defaultSeverityOf`),
  `SessionDecision`, `EffectIdFactory`.
- **`SessionEventValidation`** (non listé au plan comme fichier séparé, mais explicitement prévu
  par la logique de l'écart 3) : dix contrôles indépendants de l'état source — identifiants
  canoniques, horodatage strictement positif, révision attendue, présence/absence de chaque
  charge selon `SessionEventKind`, bornes de la sélection d'applications.
- **`SessionEngine`** : aiguillage direct et exhaustif (`when` sans `else`) vers la fonction
  `internal` responsable de chaque `SessionEventKind`, dans cinq objets par famille —
  `ActivationReducer`, `TriggerReducer`, `NfcReducer`, `ReleaseReducer`, `IncidentReducer` — plus
  `ReducerSupport.kt` et `SessionEffectBuilder.kt` pour les invariants partagés (`reject()`,
  `invalidStateTransition()`, `healthAfter()`, `matchesEvent()`, numérotation des `effectId`).
- **`com/niumi/core/common/CanonicalUuid.kt`** (nouveau package) : extraction de
  `BoxPayloadParser.isCanonicalBoxId` (étape 2, était `private`), devenue `internal`, réutilisée
  telle quelle par la validation d'événement pour `eventId` et `sessionId`. `BoxPayloadParser`
  délègue désormais à `CanonicalUuid.isCanonical` ; comportement inchangé, les 53 tests d'étape
  1-2 restent verts.

### Tests (`shared/core/src/commonTest/kotlin/com/niumi/core/domain/`)

`SessionFixtures.kt` regroupe quatre objets de fixtures (`SessionSnapshotFixtures`,
`SessionActivationEventFixtures`, `SessionLifecycleEventFixtures`, `NfcScanEventFixtures`) plutôt
qu'un seul, pour rester sous le seuil `TooManyFunctions` — le plan prévoyait un seul fichier, pas
un seul objet ; la contrainte du plan (« un fichier ») est respectée, celle de detekt impose
plusieurs types par fichier ici, comme dans le reste du domaine pour la complexité plutôt que le
nombre de fichiers.

Huit fichiers de test, un par catégorie du plan : `SessionEngineActivationTest`,
`SessionEngineTriggerTest`, `SessionEngineNfcTest`, `SessionEngineReleaseTest`,
`SessionEngineIncidentTest`, `SessionEngineValidationTest`, `SessionEngineEffectsTest`,
`SessionEngineForbiddenTransitionsTest`. Ce dernier vérifie exhaustivement les 9 états × 11
événements plus la ligne « aucune session » (110 couples) contre la table SPEC_CORE_KMP §5.1 : 26
couples autorisés construits explicitement, les 84 restants doivent produire une violation et
aucun effet — c'est la preuve de couverture demandée par §17.

## Décisions d'implémentation non arbitrées avec l'utilisateur (ambiguïtés mineures des specs)

- **« État actif » de §5.2** (source valide pour `INCIDENT_REPORTED` et `INVALID_NFC_SCANNED`)
  n'est précisé nulle part. Interprété comme tout état non final — `PREPARING` et `RELEASING`
  compris, pas seulement `ARMED`/`RINGING`/`AWAITING_NFC`/`TRIGGERED_AWAITING_NFC`. Cohérent avec
  `SessionEngineIncidentTest` (« incident sur un état final → violation », rien n'exclut
  `PREPARING` ou `RELEASING`).
- **`ACTIVATION_REQUESTED` avec un snapshot existant final** (`COMPLETED`/`CANCELLED`/`FAILED`).
  Le plan ne teste que le cas « non nul non final ». Retenu : tout snapshot non nul est refusé,
  quelle que soit sa finalité — seule l'absence de snapshot (source « aucun » de la table) est
  légale, ce qui correspond au fait que le coordinateur natif efface toujours le pointeur de
  session active avant une nouvelle activation (§10).
- **Comparaison de la preuve NFC** : champ par champ (`sessionId`, `eventId`, `expectedRevision`,
  `verifiedAtEpochMillis` de la preuve contre les champs correspondants de l'événement), jamais
  par `==` sur la preuve — `NfcVerificationProof` n'a volontairement ni `equals` structurel ni
  `copy()` (étape 2).

## Vérifications exécutées

```bash
./gradlew :shared:core:compileKotlinJvm            # vert
./gradlew :shared:core:compileTestKotlinJvm         # vert
./gradlew :shared:core:jvmTest                      # vert — 105 tests, 0 échec
./gradlew ktlintCheck                               # vert, dépôt entier
./gradlew detekt                                    # vert, dépôt entier
./gradlew :shared:core:compileKotlinIosSimulatorArm64   # vert
```

Contrôles manuels :

- chaque ligne de SPEC_CORE_KMP §5.1 et chaque puce de §5.2 porte un test nommé (voir la liste
  ci-dessus) ;
- `grep` sur `commonMain/domain` : aucune horloge (`Clock.System`, `Instant.now`,
  `currentTimeMillis`), aucun aléa (`kotlin.random`) ;
- `grep` sur `commonMain/domain` : aucun `require`/`check`/`throw` de validation. Seuls des
  `requireNotNull` défensifs subsistent dans les réducteurs (ex. `requireNotNull(event.incident)`
  dans `ReleaseReducer.onFailed`), sur des champs déjà garantis présents par
  `SessionEventValidation` avant que le réducteur ne soit atteint : ils ne sont jamais
  atteignables avec une entrée invalide via l'API publique `SessionEngine.reduce()`, donc aucune
  exception ne peut en pratique traverser la frontière native.
- les 53 tests des étapes 1-2 restent verts après l'extraction de `CanonicalUuid`.

## Ce qui n'a pas été fait (hors périmètre de l'étape)

- **`NiumiCoreFacade`** ne gagne aucune méthode `reduce` : la façade et les DTO `Session*Dto`
  restent à l'étape 8, comme prévu. Les adaptateurs Android n'existent pas encore et ne doivent
  pas être écrits avant l'étape 9 (Room) et l'étape 11 (`SessionCoordinator`).
- **Le calcul du retard de 15 minutes** (`TriggerDelayPolicy`, `MISSED_TRIGGER_WINDOW`) n'est pas
  implémenté ici : `TriggerReducer.onTriggerElapsed` accepte tel quel l'incident déjà joint par
  l'appelant. C'est explicitement une décision de l'étape 8.
- Aucune validation sur appareil n'est requise ni pertinente pour cette étape : le moteur est
  entièrement pur, sans API système, testé en JVM.

## Fichiers modifiés ou créés

- `shared/core/src/commonMain/kotlin/com/niumi/core/common/CanonicalUuid.kt` (nouveau)
- `shared/core/src/commonMain/kotlin/com/niumi/core/nfc/BoxPayloadParser.kt` (modifié, délègue à `CanonicalUuid`)
- `shared/core/src/commonMain/kotlin/com/niumi/core/domain/*.kt` (29 fichiers, nouveaux)
- `shared/core/src/commonTest/kotlin/com/niumi/core/domain/*.kt` (9 fichiers, nouveaux : `SessionFixtures.kt` + 8 classes de test)
- `specs/SPEC_CORE_KMP.md` (§6 table des effets, §7.3 liste des codes)
- `docs/superpowers/plans/2026-09-03-mvp-android.md` (cases de l'étape 7)
