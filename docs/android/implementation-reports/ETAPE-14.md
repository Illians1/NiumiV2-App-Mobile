# Étape 14 — choix de l'heure, récapitulatif et activation en deux phases

**Date :** 2026-09-12
**Périmètre :** `:core:system` (fuseau injectable, fabrique d'événement d'activation, mémoire de
la dernière heure), `:feature:session` (écrans 5, 6 et 7 minimal, `ArmSessionUseCase`),
`:feature:setup` (sortie du diagnostic vers l'écran 5), `:app` (navigation).
**Specs de référence :** SPEC_CORE_KMP §8.1, §8.2, §10, §14 ;
SPEC_ANDROID §8, §9.2, §13, §15 (écrans 5, 6, 7), §16, §19.1.

## Résumé

Cette étape rend une session armable de bout en bout. Le parcours complet — accueil → onboarding →
diagnostic → association → sélection → heure → récapitulatif → activation — existe désormais, et
`ArmSessionUseCase` applique les points 1 à 3 de §9.2 avant de confier le reste au coordinateur
livré à l'étape 11.

Conséquence indirecte : plusieurs garanties écrites aux étapes 12 et 13 deviennent enfin
**observables** — la garde des écrans 3 et 4 pendant une session, la redirection d'accueil de
§10.4, et le contrôle `FUTURE_TRIGGER` du diagnostic.

## Décisions validées avec l'utilisateur (2026-09-12)

1. **`vibrationEnabled = true`, constante.** Aucun réglage utilisateur n'existe dans le MVP ; le
   champ est figé dans `ArmSessionUseCase` plutôt qu'exposé par un interrupteur qui n'aurait rien
   à commander.
2. **La sortie vers l'écran 5 est un bouton du diagnostic**, affiché quand aucun contrôle bloquant
   n'échoue. Demande une exception à §13 (voir « Écarts » ci-dessous).
3. **L'écran 7 est livré en version minimale dès cette étape**, dans le package `active/` que
   l'étape 15 enrichira.
4. **Le cadran s'ouvre sur la dernière heure confirmée**, 07:00 au premier usage. Persistée dans
   `SetupPreferences` (DataStore `niumi_setup`), hors Room.
5. **Un trou d'heure d'été affiche l'instant réel et l'explique.** Une saisie de 02:30 la nuit du
   changement d'heure affiche 03:00, accompagné d'une phrase qui nomme les deux heures.

## Écarts aux specs, répercutés dans le même changement

1. **SPEC_ANDROID §15** plaçait l'écran 7 à l'étape 15. Il est désormais décrit comme minimal à
   l'étape 14, complet à l'étape 15, avec la raison : l'étape 14 étant la première à rendre une
   session armable, sans lui l'accueil deviendrait un cul-de-sac alors que §10.4 en fait la
   seconde garantie d'accès au scan.
2. **SPEC_ANDROID §13** impose une seule action principale à la fois. Le bouton « Choisir mon
   heure de réveil » en est une seconde ; §13 gagne une exception, dans la continuité de celle
   accordée à l'étape 13 aux deux étapes de parcours. Sa condition est « aucun contrôle bloquant
   en échec » (`isDeviceReady`) et non « activation autorisée » (`isAllowed`), faux tant qu'aucune
   heure n'est choisie — c'est-à-dire tant que l'utilisateur n'a pas suivi ce bouton.
3. **SPEC_CORE_KMP §8.1** ne disait pas ce qui doit être **affiché** quand l'heure saisie n'existe
   pas. Elle impose désormais l'instant réel et son explication.

## Écarts internes au plan MVP

1. **L'ordre des 10 points de §9.2 ne peut pas être prouvé par un seul journal d'appels.** Le plan
   le demandait dans `ArmSessionUseCaseTest` ; les points 4 à 9 ne traversent jamais la frontière
   du use case. La preuve est l'union de deux jeux de tests, et le KDoc d'`ArmSessionUseCase`
   porte le tableau de traçabilité correspondant : points 1-3 ici, 4-9 dans `:core:system`
   (étapes 9 à 11), point 10 dans `SummaryViewModelTest` et le câblage du `NavHost`.
2. **Le test « changement de fuseau entre saisie et confirmation » est scindé en deux.** Le
   ViewModel de l'écran 5 ne peut prouver que le recalcul à l'affichage ; le recalcul *avant
   activation* est une propriété d'`ArmSessionUseCase`, testée là.
3. **`ui/SessionUiState.kt` devient `ui/WakeScheduleDisplay.kt`.** Un état transversal aux trois
   écrans serait redondant — chacun garde le sien, comme partout ailleurs dans le dépôt. Le seul
   élément réellement commun est la projection d'affichage d'un horaire, et la règle ktlint
   `standard:filename` impose alors ce nom de fichier.
4. **`NiumiRoute.Summary` devient une `data class`** portant `localTimeIso` — seule destination à
   argument du graphe. Elle transporte le *choix* de l'utilisateur, jamais l'horaire calculé :
   transmettre un `WakeScheduleDto` rendrait structurellement possible d'armer une session sur un
   horaire périmé, alors que ne transmettre que l'heure locale rend le recalcul impossible à
   contourner.
5. **`ArmSessionUseCase` expose `preview()` en plus de `arm()`.** Les points 1 et 2 de §9.2 sont
   exactement ce dont le récapitulatif a besoin pour décider si son bouton est actif (§19.1) : les
   exposer une fois garantit que l'écran et l'activation appliquent le même verdict, calculé par
   le même code. `arm()` rejoue le diagnostic, il ne réutilise jamais l'aperçu affiché.
6. **`SessionEventFactory` gagne `activationRequested`.** Aucune fabrique ne couvrait
   `ACTIVATION_REQUESTED`, seul événement sans snapshot préalable dont dériver `sessionId` et
   `expectedRevision`. C'est le bon domicile : la fabrique existe précisément parce que
   `SessionEventDto` a neuf arguments sans défaut, et détient déjà `IdGenerator` et `Clock`.
7. **L'accueil gagne un bouton « Voir ma session ».** Il affichait « Une session est en cours. »
   sans aucune action — cul-de-sac invisible tant qu'aucune session ne pouvait être armée.
8. **`ReadinessAction.FixTime` sort de `UNAVAILABLE_ACTIONS`**, son écran existant désormais. Sans
   effet observable avant §13.1 : le contrôle `FUTURE_TRIGGER` reste `NOT_APPLICABLE`, donc
   masqué, tant qu'aucune heure candidate n'existe.

## Livré

**`:core:system`**
- `common/TimeZoneProvider.kt` : fuseau IANA injectable, jumeau de `Clock` (§19.2). Binding dans
  `SystemModule`.
- `session/SessionEventFactory.activationRequested(request)`.
- `setup/SetupPreferences` : `lastWakeTimeIso()` / `setLastWakeTimeIso(value)`.
- `audio/NiumiRingtones.DEFAULT_KEY` : la seule clé de sonnerie résoluble, pour que
  `:feature:session` ne recopie pas le littéral.

**`:feature:session`**
- `ui/WakeScheduleDisplay.kt` + `ui/WakeScheduleFormatter.kt` : projection d'affichage partagée par
  les écrans 5, 6 et 7. Lit exclusivement `triggerAtEpochMillis`, jamais `localTimeIso`.
- `wake/` : écran 5 (`TimePicker` Material 3, phrase complète avec le fuseau).
- `activation/` : `ArmSessionUseCase`, `ActivationFailure`, `ArmSessionResult`, `ActivationSources`.
- `summary/` : écran 6 (date, heure, fuseau, applications, boîtier tronqué, rappel d'engagement).
- `active/` : écran 7 minimal, aux noms attendus par l'étape 15.
- `di/ActivationModule.kt`.

**`:feature:setup`** — bouton de continuation du diagnostic, `FixTime` réactivé.

**`:app`** — trois destinations enregistrées, `Summary` à argument, `navigateToActiveSession()`,
bouton de sortie de l'accueil, déclenchement de la réconciliation au démarrage.

**Ajouts issus de la validation sur appareil** — `session/SessionStartupReconciler.kt`
(`:core:system`), `ReconcilerSources.snapshotPublisher`, republication du snapshot dans
`SessionReconciler`, convention 12/24 h sur l'écran 7.

## Points techniques

- **Deux appels à `DeviceReadinessChecker.check()` par diagnostic.** Le premier, sans candidat, ne
  sert qu'à lire `nowEpochMillis` sur la même horloge que le reste du diagnostic ; le second, avec
  le candidat calculé, est seul transmis à `evaluateActivation`. Un seul appel obligerait soit à
  recopier le mapping de `toActivationPolicyInput()`, soit à évaluer la politique sur
  `triggerAtEpochMillis = nowEpochMillis`, refusé par `TRIGGER_NOT_IN_FUTURE`.
- **`TimePicker` est encore `@ExperimentalMaterial3Api`** avec la BOM 2026.09.00 (vérifié à la
  compilation) : `@OptIn` local sur les deux composables concernés, aucune règle assouplie.
  `rememberTimePickerState` est sans clé — le sourcer sur l'état réinitialiserait le cadran à
  chaque recomposition.
- **Conversion heure → ISO par `java.time.LocalTime.of(h, m).toString()`**, jamais
  `String.format("%02d:%02d", …)` : sans `Locale.ROOT`, ce dernier produit des chiffres
  arabo-indiens sur un appareil en `ar-EG`, que `kotlinx.datetime.LocalTime.parse` rejetterait en
  `INVALID_TIME`. Même raison pour les formateurs d'affichage, dont la locale est toujours
  explicite.
- **`popUpTo(Home) { inclusive = false }` après l'activation** : le récapitulatif et l'écran 5
  disparaissent de la pile — y revenir montrerait un bouton « Activer ma session » que
  `ActivationReducer.onRequested` refuserait toujours — tandis que l'accueil reste dessous.
- **Les gardes lisent `isSessionInProgress()`, jamais `snapshot != null`** :
  `SessionSnapshotPublisher` conserve volontairement le dernier snapshot final pour l'écran de fin.
- **Une seule dépendance Gradle ajoutée** : `lifecycle.runtime.compose` à `:feature:session`
  (`LocalLifecycleOwner`). Pas de `kotlinx-datetime` (`java.time` suffit à `minSdk 29`, et §8
  autorise les types JVM de fuseau dans les adaptateurs natifs), pas de `navigation-compose` (la
  feature ne voit jamais les routes), aucun nouveau module.

## Vérifications automatisées (2026-09-12)

```
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest \
          :feature:setup:testDebugUnitTest :app:testDebugUnitTest \
          :shared:core:jvmTest :core:database:testDebugUnitTest :feature:ringing:testDebugUnitTest
./gradlew :app:assembleDebug :app:lintDebug
./gradlew ktlintCheck detekt
```

**587 tests JVM verts** : `:feature:session` 69 (+68), `:core:system` 154 (+9), `:feature:setup` 76
(+1), `:app` 14 (+3), non-régression sur `:shared:core` (160), `:core:database` (93) et
`:feature:ringing` (21). `:app:assembleDebug`, `:app:lintDebug`, ktlint et detekt verts. Les quatre
derniers tests sont les régressions des deux défauts trouvés sur appareil (deux pour le format
12 h, un pour la republication du snapshot, un pour le déclencheur `PROCESS_START`).

**Aucune règle detekt assouplie.** Les quatre remontées ont été traitées sur le fond : `!!`
remplacé par un branchement sur le `schedule` nullable (le statut porte alors la raison du refus),
nombre magique du `@Preview` dérivé de `DEFAULT_LOCAL_TIME_ISO`, `shiftedFromLocalTime` restructurée
en deux sorties, et `ReturnCount` de `arm()` suppressée localement avec justification — même
convention documentée que `DefaultSessionCoordinator.dispatch` et `NfcReducer.onValidScan`.

## Validations sur appareil réel (2026-09-12)

**Xiaomi 25080RABDG, Android 16 (SDK 36), HyperOS 3.0** — le même appareil qu'aux étapes 12b et 13.

**98 tests instrumentés verts** : 41 `:core:database`, 16 `:core:system` (dont les trois nouveaux
de `DataStoreSetupPreferencesInstrumentedTest`), 38 `:feature:setup` (dont les deux nouveaux de
`ReadinessScreenTest` sur le bouton de continuation), 3 `:feature:session`. Rejoués après les deux
correctifs ci-dessous.

Protocole manuel déroulé essai par essai :

| # | Essai | Résultat |
|---|---|---|
| 1 | Parcours complet accueil → … → activation | ✅ Session armée de bout en bout |
| 2 | `dumpsys alarm` | ✅ `RTC_WAKEUP` + bloc `Alarm clock:`, `triggerTime=2026-09-13 07:30`, `window=0`, `exactAllowReason=policy_permission`, PendingIntent explicite vers `AlarmReceiver` |
| 3 | Processus tué pendant l'activation | ✅ Aucune session fantôme, aucune alarme orpheline, réactivation possible |
| 4 | Garde de l'étape 13 pendant une session | ✅ *partiel* — voir ci-dessous |
| 5 | Bascule du fuseau entre écrans 5 et 6 | ✅ Récapitulatif recalculé en `America/New_York` sur `ON_RESUME` |
| 6 | Retour depuis l'écran 7 | ✅ Atterrit sur l'accueil, jamais sur le récapitulatif |
| 7 | Appareil réglé en 12 h | ❌ puis ✅ — **défaut trouvé**, voir ci-dessous |
| 8 | Redémarrage avec session `ARMED` | ❌ puis ✅ — **défaut trouvé**, voir ci-dessous |
| 9 | Propagation du cadran vers le ViewModel | ✅ 07:00 → 07:30 répercuté instantanément dans la phrase |

**Essai 4, portée réelle.** Ce qui est observable sur appareil, c'est que l'accueil cesse de
proposer le parcours de préparation dès qu'une session existe : il affiche « Une session est en
cours. » et mène à l'écran 7. La garde `SetupGate` de second niveau, elle, reste **inatteignable
par l'interface** dans ce parcours — c'est précisément le comportement voulu — et demeure prouvée
par `SetupGateTest` (5 tests JVM exhaustifs sur `SessionStateDto.entries`).

## Deux défauts trouvés sur appareil et corrigés dans le même changement

Aucun test JVM ne pouvait les attraper : les deux tiennent à ce que le processus meurt et renaît,
et au réglage système de l'appareil.

### 1. Une session armée était invisible après un redémarrage du processus

**Symptôme.** Après `force-stop` puis relance, l'accueil affichait « Aucune session » alors que la
session était `ARMED` en base et l'alarme programmée dans `dumpsys`. Le critère « une session
`ARMED` est visible sur l'accueil après redémarrage de l'application » du plan MVP n'était pas
satisfait.

**Deux manques cumulés.**

1. `ReconcileReason.PROCESS_START` existait depuis l'étape 11 sans que **rien ne l'émette** : le
   réconciliateur n'était jamais déclenché au démarrage. Invisible jusqu'ici — aucune session ne
   pouvait être armée avant cette étape.
2. Même déclenché, `SessionReconciler.reconcile()` chargeait le snapshot mais ne le **republiait
   pas**. `SessionSnapshotPublisher` vit en mémoire et repart à `null` ; une session saine ne
   produit aucune décision, donc aucun `PUBLISH_PLATFORM_SNAPSHOT`. Le KDoc de
   `SessionReadinessWatcher` affirmait pourtant déjà que « la réconciliation reste le chemin qui
   découvre une session après un redémarrage du processus » — le chemin était décrit, jamais
   emprunté.

**Correctif.** `SessionReconciler` publie le snapshot chargé dès qu'il en trouve un, avant toute
décision (`SessionSnapshotPublisher` rejoint `ReconcilerSources`) ; `SessionStartupReconciler`
déclenche `PROCESS_START` depuis `NiumiApplication.onCreate()` — au niveau de l'`Application` et
non d'une activité, le processus pouvant être réveillé sans interface par le receveur d'alarme.

**Effet de bord bénéfique constaté** : la surveillance de §13.1 dépendait elle aussi de cette
réconciliation jamais déclenchée. Au premier redémarrage après correctif, la notification
« Vérifie ton réveil Niumi » est apparue d'elle-même, le `force-stop` ayant débranché le service
d'accessibilité — exactement le comportement prescrit par §13.1.

### 2. L'écran 7 ignorait le format 12/24 h du système

**Symptôme.** Téléphone réglé en 12 h (barre système « 4:12 ») : les écrans 5 et 6 affichaient
« 7:30 AM », l'écran 7 affichait « 07:30 ». Trois écrans portant la même heure, deux conventions.

**Cause.** `ActiveSessionViewModel` appelait `WakeScheduleFormatter.format(...)` sans passer
`use24Hour`, prenant donc le défaut `true`. Les `Route` des écrans 5 et 6 lisaient
`DateFormat.is24HourFormat(context)`, pas celle de l'écran 7.

**Correctif.** `ActiveSessionRoute` fournit la convention système et la relit à chaque `ON_RESUME`
(elle peut changer pendant qu'une session est armée) ; `ActiveSessionViewModel.refresh(use24Hour)`
la mémorise et reprojette le snapshot déjà publié. Deux tests de régression.

## Points de vigilance pour les étapes suivantes

- **`AlarmRingingService` ignore `extras.ringtoneKey`** et emploie sa propre constante. Sans effet
  tant qu'une seule sonnerie existe ; à relier aux extras dès qu'une deuxième sera proposée.
  `NiumiRingtones.DEFAULT_KEY` n'en corrige que la moitié (une seule définition de la clé).
- **`CLEAR_ACTIVE_SESSION` est best-effort** : si son écriture Room échouait, l'activation suivante
  serait rejetée en `INVALID_STATE_TRANSITION`. D'où `ActivationFailure.Rejected`, qui nomme le
  code de violation plutôt que d'afficher un message opaque.
- **`isSetupEditable` (`:feature:setup`) est un doublon conceptuel de `isSessionInProgress()`** :
  non propagé à `:feature:session` ; sa suppression est à envisager à l'étape 15.
- **Le cadran peut afficher 07:00 avant que la dernière heure confirmée soit relue** (lecture
  DataStore asynchrone, `rememberTimePickerState` sans clé). Écart d'une frame au premier
  affichage, à confirmer sur appareil.
