# Étape 22 — contrat KMP 1.3 : `BlockingSchedule`, `BLOCKING_START_ELAPSED`, effets, politique, calcul et façade

Date : 2026-09-16. Contenue dans `:shared:core`, à une exception documentée plus bas : un test
Android (`EventFingerprintTest`) a dû être repris, sa valeur témoin changeant de façon dérivée.
Aucun fichier de production Android modifié.

## Résumé

L'étape 22 ouvre le Lot 6 (blocage différé). Le moteur commun sait désormais qu'une session peut
être armée sans que ses applications soient bloquées, attendre un instant de début choisi par
l'utilisateur, appliquer le blocage à cet instant, et se rattraper si l'instant a été manqué. La
façade calcule cet instant et refuse ce qui n'est pas strictement antérieur au réveil.

205 tests JVM verts (184 hérités, 21 nouveaux ou étendus), `linkDebugFrameworkIosSimulatorArm64`
vert, `testDebugUnitTest` vert sur l'ensemble des modules Android, ktlint et detekt verts.

Deux décisions ont été validées avec l'utilisateur avant d'écrire le code, une troisième en cours
de route après un échec de test révélateur (voir « Décisions validées »). Deux effets de bord réels
sur Android ont été découverts par les tests plutôt que par le plan (voir « Effets de bord »).

## Décisions validées avec l'utilisateur (2026-09-16)

1. **Fixtures : format existant conservé.** Le plan demandait « un snapshot différé en attente et un
   snapshot différé appliqué » dans `commonTest/resources/fixtures/`, plus « le snapshot v1
   existant ». Or `session_transitions.json` ne décrit aucun snapshot : il porte des *noms*
   (`"armed"`, `"ringing"`…) résolus par un `when` dans `jvmTest/.../FixturesTest.kt`, et aucun
   snapshot v1 n'existe sous forme JSON. Retenu : deux noms de plus
   (`armedBlockingPending`, `armedBlockingApplied`), six entrées de transition, un
   `blocking_schedules.json` calqué sur `wake_schedules.json`. La ligne §17 « lecture d'un snapshot
   de version 1 » est prouvée par la désérialisation d'un JSON de `SessionSnapshotDto` sans les deux
   nouveaux champs (`DtoRoundTripTest`) : c'est bien le DTO sérialisé, et non le snapshot du
   domaine, que relisent Room et Direct Boot.
2. **`NiumiCoreVersion.SCHEMA_VERSION` passe de 1 à 2.** Les DTO exposés gagnent des champs, deux
   valeurs d'enum apparaissent et la façade une septième méthode ; les valeurs par défaut Kotlin ne
   traversent pas la frontière Swift, la rupture est donc réelle pour iOS. `SmokeTest` suit, et
   SPEC_CORE_KMP §14 porte désormais la règle. Cette constante reste distincte de
   `SessionSnapshot.schemaVersion`, qui décrit le snapshot persisté et passe à `2` de son côté.
3. **`CANCEL_BLOCKING_START` n'est produit que pour un blocage différé.** Voir « Effets de bord ».

## Effets de bord découverts par les tests

### 1. `CANCEL_BLOCKING_START` inconditionnel cassait Android (15 tests)

Appliquée à la lettre, la table SPEC_CORE_KMP §6 fait produire `CANCEL_BLOCKING_START` à toute
libération et à toute activation échouée, y compris pour une session à blocage immédiat —
c'est-à-dire pour les seules sessions qu'Android sache armer avant l'étape 24. Aucun exécuteur
n'étant lié avant l'étape 23, `EffectDispatcher.execute` levait
`NoSuchElementException: Key CANCEL_BLOCKING_START is missing in the map` dans 15 tests de
`:core:system`.

Retenu, validé avec l'utilisateur : le moteur ne produit l'effet que si `blockingSchedule` est
différé. Une alarme de début n'existe que si `SCHEDULE_BLOCKING_START` l'a demandée, et une session
immédiate n'en a jamais : l'effet serait une annulation sans objet. Conséquence utile — les effets
d'une session immédiate restent **exactement** ceux du contrat 1.2, ordinaux et `effectId` compris,
ce qui rend la non-régression Android démontrable plutôt que supposée.

SPEC_CORE_KMP §6 a été amendée dans le même changement : les deux lignes de la table portent la
mention « (blocage différé seulement) », suivie d'un paragraphe de justification.

Les deux options écartées : avancer le programmateur d'alarme et l'exécuteur à l'étape 22 (l'étape
cessait d'être purement KMP et absorbait un tiers de l'étape 23) ; conditionner à `isBlockingPending`
seulement (une alarme de début aurait survécu à un repli du moteur, laissant un réveil système
orphelin contre l'économie de réveils visée par SPEC_ANDROID §9.1).

### 2. L'empreinte canonique des événements d'activation change

`EventFingerprintTest.fingerprintMatchesTheGoldenValueForTheReferenceEvent` est tombé : son
commentaire annonce précisément ce rôle de sentinelle (« tout changement de forme canonique doit
faire échouer ce test plutôt que de dériver silencieusement »). `ActivationRequestDto` gagne
`blockingSchedule`, sérialisé puisque `EventFingerprint` utilise `encodeDefaults = true`.

Étendue exacte, vérifiée avant de reprendre la valeur :

- seuls les événements `ACTIVATION_REQUESTED` sont concernés ; tous les autres portent
  `activationRequest: null`, dont la forme canonique ne bouge pas ;
- l'empreinte n'est recalculée que pour un `eventId` **déjà présent** au registre
  (`DefaultSessionCoordinator.guard`), afin de distinguer un doublon d'un `EVENT_ID_CONFLICT` ;
- un `ACTIVATION_REQUESTED` naît d'une action utilisateur avec un `eventId` neuf et n'est jamais
  reconstruit à l'identique après une mise à jour du binaire.

Aucune session existante n'est donc affectée. La valeur témoin a été reprise, sa justification
écrite dans le test, et un cas a été ajouté (`differentBlockingScheduleProducesADifferentFingerprint`)
pour prouver que le blocage compte bien dans l'empreinte — deux activations identiques au blocage
près sont des événements différents.

C'est le seul fichier Android modifié par cette étape, et il l'est pour acter un changement, pas
pour faire passer un test.

## Ce qui a été construit

### `domain/` — le blocage devient un fait du snapshot

- `BlockingSchedule.kt` (nouveau) : la data class de §7.5, `isImmediate`, `IMMEDIATE`, et
  l'extension `SessionSnapshot.isBlockingPending` — **règle unique** du domaine, dont
  `interop/BlockingStatus.kt` est le seul miroir autorisé.
- `BlockingReducer.kt` (nouveau) : `onStartElapsed`, trois gardes dans l'ordre de §5.2 —
  hors `ARMED` → `INVALID_STATE_TRANSITION` ; blocage déjà demandé ou immédiat →
  `BLOCKING_ALREADY_APPLIED` ; avant l'instant → `BLOCKING_START_NOT_REACHED`.
- `SessionSnapshot` : `blockingSchedule` et `blockingAppliedAtEpochMillis`, `SCHEMA_VERSION = 2`.
  Aucune valeur par défaut : le compilateur devait signaler chaque site de construction.
- `ReducerSupport.applyPendingBlocking(...)` : le repli du moteur de §5.1, appelé par les trois
  fonctions de `TriggerReducer` entre `PUBLISH_PLATFORM_SNAPSHOT` et la sonnerie ou la demande de
  scan. Unique point d'écriture de `blockingAppliedAtEpochMillis` hors `ActivationReducer` et
  `BlockingReducer` — vérifié par grep de clôture en fin d'étape.
- `ActivationReducer.onRequested` : deux branches, `APPLY_BLOCKING` si le blocage est immédiat ou
  si son instant de début est déjà dépassé (§8.3), `SCHEDULE_BLOCKING_START` sinon.
- `SessionEventValidation.blockingScheduleViolations` : champs tous nuls ou tous renseignés, et
  antériorité stricte au réveil, sinon `INVALID_BLOCKING_SCHEDULE`. `incident` devient facultatif
  pour `BLOCKING_START_ELAPSED` comme il l'était pour `TRIGGER_ELAPSED`.

### `schedule/` — l'instant de début

`BlockingScheduleCalculator` **délègue** à `WakeScheduleCalculator` plutôt que de réécrire les
règles de §8.1 : le trou d'heure d'été résolu par bissection, le chevauchement d'automne et le
report au jour suivant sont ainsi identiques par construction, pas par recopie. Seule l'antériorité
stricte s'y ajoute. `localTimeIso` nul renvoie `VALID` avec `IMMEDIATE`.

### `diagnostics/` et `interop/`

`ActivationPolicy` refuse par `BLOCKING_START_NOT_BEFORE_TRIGGER`, sans `checkId`, cumulable avec
`TRIGGER_NOT_IN_FUTURE`. La façade gagne sa septième méthode, `computeBlockingSchedule`. Les
nouveaux champs des DTO portent une valeur par défaut, documentée en KDoc comme **transitoire** :
elle garde la compatibilité JSON des projections v1 et laisse compiler les sites Android qui les
renseigneront explicitement à l'étape 23.

## Vérifications exécutées

```
./gradlew :shared:core:jvmTest                              # 205 tests, vert
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64  # vert
./gradlew testDebugUnitTest                                 # vert, tous modules Android
./gradlew ktlintCheck detekt                                # vert
```

Grep de clôture : `blockingAppliedAtEpochMillis =` n'apparaît que dans `ActivationReducer`,
`BlockingReducer`, `applyPendingBlocking` et les deux mappers interop.

Deux suppressions detekt ont été nécessaires, chacune alignée sur un précédent du dépôt :
`ReturnCount` sur `BlockingScheduleCalculator.compute` (même motif de clauses de garde que
`WakeScheduleCalculator.compute`) et `LongParameterList` sur le helper `input()` d'`ActivationPolicyTest`
(un paramètre par champ de la politique, comme `NfcScanFixtures.persistSession`).

## Tests manuels

Aucun : module commun, sans matériel. La visibilité Swift des nouveaux DTO est prouvée par la tâche
`link`, sans Xcode.

## Ce qui n'a pas été fait (hors périmètre)

Tout le versant Android reste à l'étape 23 : Room v3 et sa migration, Direct Boot v2, l'alarme de
début et son receveur, les deux exécuteurs d'effets, la réconciliation, et la branche `ARMED` de la
projection lue par le service d'accessibilité. Tant qu'elle n'est pas livrée, Android n'arme que
des sessions à blocage immédiat : `ArmSessionUseCase` construit son `ActivationRequestDto` sans
`blockingSchedule` et reçoit donc `IMMEDIATE` par défaut. L'interface du choix arrive à l'étape 24.

**Point de vigilance 13, rappelé pour l'étape 23 :** le repli du moteur est prouvé ici, mais c'est
un filet. Le chemin normal — `BlockingStartReceiver` à l'heure, puis `SessionReconciler` au premier
réveil du processus — n'existe pas encore. Un test qui n'observerait le blocage qu'au réveil
prouverait le filet, pas la fonctionnalité.
