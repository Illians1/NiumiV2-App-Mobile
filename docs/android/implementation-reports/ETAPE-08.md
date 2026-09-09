# Étape 8 — politique horaire, politique d'activation, façade complète, fixtures et framework iOS

Date : 2026-09-09. Entièrement contenue dans `:shared:core` (Kotlin JVM/commun), plus la
vérification du framework Kotlin Native iOS. Aucun fichier Android touché.

## Résumé

L'étape 8 ferme le Lot 0.5 : elle rend `SessionEngine` (étape 7) et le protocole NFC (étape 2)
accessibles au natif via `NiumiCoreFacade` complète (SPEC_CORE_KMP §14), ajoute les deux règles
communes qui manquaient encore (calcul de l'heure de réveil §8.1, fenêtre de grâce Android de 15
minutes §8.2) et la politique d'activation (§7.4, SPEC_ANDROID §13), et livre les fixtures
communes rejouées par les tests (§17).

Un constat bloquant sur le plan d'origine a été résolu avant implémentation (résolution du trou
d'heure d'été, voir « Constat sur le plan »). Trois décisions de contrat ont été validées avec
l'utilisateur le 2026-09-08 avant d'écrire le code (voir « Décisions validées »). Deux écarts de
structuration ont été nécessaires en cours de route pour rester sous les seuils detekt (voir
« Écarts au plan »).

160 tests JVM verts (105 hérités des étapes 1-2 et 7, 55 nouveaux), `linkDebugFrameworkIosSimulatorArm64`
et `linkReleaseFrameworkIosArm64` verts (Xcode 26.6 disponible), ktlint et detekt verts sur
l'ensemble du dépôt.

## Constat sur le plan d'origine

Le plan affirmait : « `LocalDateTime.toInstant(TimeZone)` applique déjà la règle "premier instant
valide / première occurrence" : le test le prouve, ne pas réimplémenter. »

Vérifié empiriquement (inspection du jar `kotlinx-datetime-jvm-0.8.0` + `java.time`, sur lequel
kotlinx-datetime délègue sur JVM) : **c'est vrai pour le chevauchement d'automne, faux pour le trou
de printemps**.

| Cas | Attendu SPEC_CORE_KMP §8.1 | Rendu par `toInstant(zone)` |
| --- | --- | --- |
| `2026-03-29T02:30` `Europe/Paris` (trou) | `03:00` | `03:30` |
| `2026-10-25T02:30` `Europe/Paris` (répétée) | première occurrence, UTC+2 | UTC+2 ✔ conforme |

`toInstant(zone)` décale l'heure locale de la taille du saut au lieu de retenir le premier instant
valide après le saut. `kotlinx-datetime` 0.8.0 n'expose pas l'API `TransitionHandler` qui résoudrait
ce cas explicitement (inspection du jar : absente, disponible seulement sur le `master` amont non
publié).

Retenu : `WakeScheduleCalculator.resolveInstant()` détecte l'écart par round-trip
(`naive.toLocalDateTime(zone) == local` ?) et résout le trou par bissection sur `toLocalDateTime`
uniquement — jamais sur la politique de trou de `toInstant` — ce qui rend le résultat identique
quelle que soit la plateforme (JVM ou Kotlin/Native, dont la base de fuseaux est indépendante). La
ligne du plan est corrigée dans le même changement.

## Décisions validées avec l'utilisateur (2026-09-08)

1. **`WakeScheduleResult` = `status` + champ nullable**, pas de `sealed interface Success/Failure`.
   Reprend la convention posée à l'étape 2 par `BoxPayloadResult`/`BoxVerificationResult` plutôt que
   d'introduire une deuxième convention de résultat dans le module ; évite aussi le
   `as? WakeScheduleResultSuccess` côté Swift qu'un type scellé imposerait (une sealed interface
   KMP s'exporte comme un protocole Objective-C sans `switch` exhaustif côté Swift). Écart assumé
   au texte du plan.
2. **Les enums du domaine sont ré-exportés par `typealias`** dans `interop`
   (`public typealias SessionStateDto = SessionState`, etc., pour `SessionState`,
   `SessionEventKind`, `SessionEffectKind`, `SessionHealth`, `ReleaseTarget`, `Platform`,
   `IncidentSeverity`). Satisfait à la fois « enums réexportés à l'identique » et « les adaptateurs
   Android n'importent jamais `com.niumi.core.domain` », sans mapping ni risque de divergence entre
   domaine et frontière — le domaine et la frontière ont exactement la même forme pour ces enums,
   contrairement aux structures composites (snapshot, événement), où le round-trip protège
   réellement contre un bug de mapping.
3. **`ActivationPolicy` prend des contrôles typés et rend des raisons codées** (`code` + `checkId`
   optionnel), jamais de texte : §7.3 interdit les textes utilisateur dans le module commun.

## Écarts au plan

1. **`PolicyDtos.kt`, fichier non prévu.** Le plan ne listait que `SessionDtos.kt` pour les DTO.
   Les DTO horaires et de politique (`WakeScheduleInputDto`, `WakeScheduleResultDto`,
   `TriggerDelayInputDto`, `TriggerDelayResultDto`, `ReadinessCheckInputDto`,
   `ActivationPolicyInputDto`, `ActivationReasonDto`, `ActivationPolicyResultDto`) vivent dans un
   fichier séparé pour garder `SessionDtos.kt` (miroirs stricts de §5 à §7) lisible.
2. **Les mappers sont répartis sur quatre fichiers, pas un seul `DtoMappers.kt`.** 22 fonctions de
   conversion au total dépassent le seuil detekt `TooManyFunctions` (11) dans un seul fichier.
   Répartition par thème : `DtoMappers.kt` (types-valeurs de §7 : `WakeSchedule`,
   `AppSelectionSummary`, `ActivationRequest`, `SessionIncident`, 8 fonctions),
   `SessionSnapshotEventMappers.kt` (`SessionSnapshot`, `SessionEvent`, 4 fonctions),
   `SessionEffectDecisionMappers.kt` (`SessionEffect`, `SessionEffectPayload`, `DomainViolation`,
   `SessionDecision`, 4 fonctions), `PolicyDtoMappers.kt` (politique horaire et d'activation, 6
   fonctions). Chaque type ne porte que les directions réellement empruntées par la façade ou les
   tests de round-trip : `SessionEffect`, `SessionEffectPayload`, `DomainViolation` et
   `SessionDecision` n'ont qu'un `toDto()`, jamais réinjectés dans `reduce()` — même convention que
   `BoxPayloadResult.toDto()` sans réciproque, posée à l'étape 2.
3. **Bornes 1..50 extraites dans `AppSelectionSummary.MIN_COUNT`/`MAX_COUNT`.** Elles étaient des
   constantes privées de `SessionEventValidation` (étape 7). `ActivationPolicy` en avait besoin ;
   les dupliquer aurait créé deux sources de vérité pour la même règle (SPEC_CORE_KMP §7.4).
   `SessionEventValidation` pointe désormais sur le compagnon d'`AppSelectionSummary`.
4. **`session_transitions.json` n'inclut aucun événement `VALID_NFC_SCANNED`.** Sa preuve
   (`NfcVerificationProof`) a un constructeur `internal` et aucune forme sérialisable
   (SPEC_CORE_KMP §14) : rien de représentable en JSON ne pourrait circuler jusqu'au moteur sans
   fabriquer artificiellement une preuve dans le chargeur de fixtures, ce qui aurait contredit
   l'esprit même de la contrainte (une preuve n'est jamais une donnée, seulement un objet vivant
   produit par `verifyBox()`). Ce cas reste couvert intégralement par `SessionEngineNfcTest`
   (commonTest, étape 7) : scan valide depuis `ARMED` → `CANCELLED`, depuis
   `RINGING`/`AWAITING_NFC`/`TRIGGERED_AWAITING_NFC` → `COMPLETED`, `TRIGGER_ALREADY_ELAPSED`,
   preuve absente, preuve mal formée. Les 33 entrées de `session_transitions.json` couvrent les 13
   lignes de §5.1 restantes plus les 8 puces de refus de §5.2 hors NFC (revision périmée, session
   inconnue, identifiant mal formé, horodatage invalide).
5. **`FixturesTest.kt` construit ses snapshots et événements par les constructeurs publics du
   domaine**, indépendamment de `SessionFixtures.kt` (commonTest, étape 7). But : rester
   indépendant de la visibilité `internal` entre source sets (`jvmTest` vs `commonTest`), non
   vérifiée nécessaire au moment d'écrire le fichier. Duplique un peu de données de référence
   (`SESSION_ID`, `WakeSchedule` de référence) mais garde le fichier autonome et son mécanisme de
   chargement JSON identique à celui de `NfcFixturesTest.kt` (étape 2).

## Ce qui a été construit

### `shared/core/src/commonMain/kotlin/com/niumi/core/schedule/` (nouveau package)

- **`WakeScheduleInput.kt`**, **`WakeScheduleResult.kt`** (`WakeScheduleStatus`, `WakeScheduleResult`).
- **`WakeScheduleCalculator.kt`** : `compute()` valide zone et heure sans exception (`try`/`catch`
  `IllegalArgumentException`, même motif que `BoxPayloadParser.decodeToken`), calcule le candidat du
  jour, bascule au lendemain s'il n'est pas strictement futur, et résout chaque candidat en instant
  via `resolveInstant()` (round-trip + bissection pour le trou, voir « Constat sur le plan »).
- **`TriggerDelayPolicy.kt`** : `TriggerDelayOutcome` (`NOT_REACHED`, `FIRE_NOW`, `MISSED`),
  `evaluate()` avec la fenêtre de grâce de 15 minutes (`GRACE_WINDOW_MILLIS`).

### `shared/core/src/commonMain/kotlin/com/niumi/core/diagnostics/` (nouveau package)

- **`ReadinessSeverity.kt`** : copié mot pour mot de SPEC_CORE_KMP §7.3.
- **`ActivationPolicyInput.kt`** (`ReadinessCheckInput`, `ActivationPolicyInput`),
  **`ActivationPolicyResult.kt`** (`ActivationReason`, `ActivationReasonCode`,
  `ActivationPolicyResult`).
- **`ActivationPolicy.kt`** : `evaluate()` combine les contrôles natifs échoués (bloquants classés
  par sévérité, avertissements) et les trois règles communes (sélection hors 1..50, `triggerAt` non
  strictement futur, aucun boîtier associé).

### `shared/core/src/commonMain/kotlin/com/niumi/core/interop/`

- **`SessionDtos.kt`** (nouveau) : typealias de ré-export des sept enums du domaine, data classes
  `@Serializable` `WakeScheduleDto`, `AppSelectionSummaryDto`, `ActivationRequestDto`,
  `SessionIncidentDto`, `SessionSnapshotDto`, `SessionEventDto` (`nfcProof` en `@Transient = null`),
  `SessionEffectPayloadDto`/`IncidentEffectPayloadDto`, `SessionEffectDto`, `DomainViolationDto`,
  `SessionDecisionDto`.
- **`PolicyDtos.kt`** (nouveau, écart 1) : DTO horaires et de politique.
- **`DtoMappers.kt`**, **`SessionSnapshotEventMappers.kt`**, **`SessionEffectDecisionMappers.kt`**,
  **`PolicyDtoMappers.kt`** (écart 2) : 22 fonctions de conversion `toDomain()`/`toDto()`.
- **`NiumiCoreFacade.kt`** (modifié) : ajout de `reduce`, `computeWakeSchedule`,
  `evaluateActivation`, `evaluateTriggerDelay` (sixième méthode). `parseBoxPayload` et `verifyBox`
  inchangées depuis l'étape 2.

### Tests (`commonTest`, `jvmTest`)

- `schedule/WakeScheduleCalculatorTest.kt` (8 tests), `schedule/TriggerDelayPolicyTest.kt` (4),
  `diagnostics/ActivationPolicyTest.kt` (11), `interop/DtoRoundTripTest.kt` (20 : neuf snapshots,
  onze événements dont un round-trip explicite de la preuve NFC par référence, plus un test dédié
  à l'absence de `proof`/`boxId` dans le JSON sérialisé), `interop/NiumiCoreFacadeTest.kt` (7).
- `commonTest/resources/fixtures/wake_schedules.json` (8 entrées), `session_transitions.json` (33
  entrées, voir écart 4), `jvmTest/kotlin/com/niumi/core/FixturesTest.kt` (2 tests, même mécanisme
  de chargement que `NfcFixturesTest.kt`).

## Vérifications exécutées

```bash
./gradlew :shared:core:compileKotlinJvm                                          # vert
./gradlew :shared:core:compileTestKotlinJvm                                      # vert
./gradlew :shared:core:jvmTest                                                   # vert — 160 tests, 0 échec
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64                       # vert
./gradlew :shared:core:linkReleaseFrameworkIosArm64                              # vert
./gradlew ktlintCheck                                                            # vert, dépôt entier
./gradlew detekt                                                                 # vert, dépôt entier
```

`JAVA_HOME` a dû être positionné explicitement sur `/opt/homebrew/opt/openjdk@17` (JDK 17 installé
à l'étape 1) : le JBR embarqué dans Android Studio sur ce poste est en JDK 21, incompatible avec le
toolchain 17 exigé par `shared/core/build.gradle.kts`. Aucun changement de configuration Gradle,
seulement de l'environnement d'exécution du shell.

Deux séries de corrections en cours de route, avant le vert final :

- une entrée de `session_transitions.json` avait un `expectedState` erroné : `reject()` renvoie le
  **snapshot d'entrée inchangé**, jamais `null`, même quand celui-ci est non nul — corrigé sur les
  12 entrées de refus concernées ;
- ktlint et detekt ont signalé un style de `when` incohérent dans `ActivationPolicy.kt`, une ligne
  fusionnable dans `DtoRoundTripTest.kt`, `DtoMappers.kt` au-dessus du seuil `TooManyFunctions`
  (22 fonctions, écart 2) et `WakeScheduleCalculator.compute` au-dessus du seuil `ReturnCount` (3
  clauses de garde, `@Suppress("ReturnCount")` ajouté avec la même justification que
  `BoxPayloadParser.parse`, étape 2).

## Ce qui n'a pas été fait (hors périmètre de l'étape)

- Aucun adaptateur Android n'appelle encore la façade : Room (étape 9), Direct Boot (étape 10) et
  `SessionCoordinator` (étape 11) restent à écrire avant que `NiumiCoreFacade` ait un premier
  appelant natif.
- Aucune validation sur appareil n'est requise ni pertinente pour cette étape : tout est contenu
  dans `:shared:core`, testé en JVM, plus la vérification (non exécutée sur device) de la liaison
  du framework iOS.

## Fichiers modifiés ou créés

- `shared/core/src/commonMain/kotlin/com/niumi/core/schedule/*.kt` (4 fichiers, nouveaux)
- `shared/core/src/commonMain/kotlin/com/niumi/core/diagnostics/*.kt` (4 fichiers, nouveaux)
- `shared/core/src/commonMain/kotlin/com/niumi/core/interop/SessionDtos.kt` (nouveau)
- `shared/core/src/commonMain/kotlin/com/niumi/core/interop/PolicyDtos.kt` (nouveau)
- `shared/core/src/commonMain/kotlin/com/niumi/core/interop/DtoMappers.kt`,
  `SessionSnapshotEventMappers.kt`, `SessionEffectDecisionMappers.kt`, `PolicyDtoMappers.kt`
  (nouveaux)
- `shared/core/src/commonMain/kotlin/com/niumi/core/interop/NiumiCoreFacade.kt` (modifié)
- `shared/core/src/commonMain/kotlin/com/niumi/core/domain/AppSelectionSummary.kt` (modifié : ajout
  du compagnon `MIN_COUNT`/`MAX_COUNT`)
- `shared/core/src/commonMain/kotlin/com/niumi/core/domain/SessionEventValidation.kt` (modifié :
  pointe sur `AppSelectionSummary.MIN_COUNT`/`MAX_COUNT`)
- `shared/core/src/commonTest/kotlin/com/niumi/core/schedule/*.kt`,
  `commonTest/kotlin/com/niumi/core/diagnostics/*.kt`,
  `commonTest/kotlin/com/niumi/core/interop/DtoRoundTripTest.kt`,
  `commonTest/kotlin/com/niumi/core/interop/NiumiCoreFacadeTest.kt` (5 fichiers, nouveaux)
- `shared/core/src/commonTest/resources/fixtures/wake_schedules.json`,
  `session_transitions.json` (nouveaux)
- `shared/core/src/jvmTest/kotlin/com/niumi/core/FixturesTest.kt` (nouveau)
- `specs/SPEC_CORE_KMP.md` (§14 : sixième méthode `evaluateTriggerDelay`, DTO cités non détaillés
  ailleurs)
- `docs/superpowers/plans/2026-09-03-mvp-android.md` (ligne 449 corrigée, cases de l'étape 8)
