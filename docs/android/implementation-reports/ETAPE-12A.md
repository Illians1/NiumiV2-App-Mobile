# Étape 12a — diagnostic avant activation et surveillance pendant une session armée

**Date :** 2026-09-11
**Périmètre :** `:core:system` (package `readiness`, sources système manquantes, canal et
notification d'avertissement), plus le branchement du receveur dans `:app`.
**Specs de référence :** SPEC_ANDROID §3 (dernier point), §4.1, §4.2, §7.1, §10.3, §13, §13.1,
§17 ; SPEC_CORE_KMP §7.3, §7.4, §10, §14.

## Résumé

L'étape 11 avait livré le coordinateur, mais `NiumiCoreFacade.evaluateActivation`, écrite à
l'étape 8, n'avait toujours aucun appelant : rien côté Android ne décidait si une session pouvait
être activée. Cette passe livre `DeviceReadinessChecker` (les quatorze contrôles de §13), sa
conversion vers la politique commune, et la surveillance de §13.1 pendant une session `ARMED`.

Les écrans, la navigation typée et l'onboarding forment la passe 12b, non commencée.

## Découpage de l'étape

L'étape 12 du plan réunissait cinq livrables lourds. Décision validée avec l'utilisateur le
2026-09-11 : deux passes, **12a** (moteur de diagnostic et surveillance) et **12b** (écrans et
navigation), avec un rapport et une batterie de vérifications chacune.

## Décisions validées avec l'utilisateur (2026-09-11)

1. **Découpage 12a / 12b** ci-dessus.
2. **Routes typées `@Serializable` centralisées dans `:app`** pour la passe 12b, en remplacement
   du mécanisme `NavGraphContributor` ; le contributeur ne survit que pour la route POC de debug.
3. **AGP reste en 9.1.1.** Voir « Correction d'une prémisse du plan » ci-dessous.
4. **Tests d'écran Compose en `androidTest`**, pas de Robolectric : convention déjà en place
   (`AccessibilityConsentScreenTest`, `AlarmScreenNoStopActionTest`).

## Correction d'une prémisse du plan : l'exigence AGP de Navigation Compose

Le plan demandait de trancher, au début de l'étape 12, entre monter AGP à 9.2.0 et vérifier que
`NiumiNavHost` compile en 9.1.1. Sa justification était que « `navigation-compose` est sur le
classpath sans être compilé contre : `NiumiNavHost` n'arrive qu'à l'étape 12 ».

**C'est faux.** `androidApp/app/src/main/kotlin/com/niumi/app/ui/NiumiNavHost.kt` et
`androidApp/app/src/debug/kotlin/com/niumi/app/poc/PocNavigation.kt` existent depuis l'étape 3 et
compilent contre `NavHost`, `composable` et `rememberNavController`. Le build est vert depuis, y
compris à l'étape 11 après la montée en 2.10.1. Aucun blocage n'a jamais été observé : la décision
retenue est de rester en 9.1.1 et de ne monter que si un build casse réellement.

## Montée de Kotlin 2.4.10 → 2.4.20

`:app:lintDebug` s'est mis à échouer sur trois `NewerVersionAvailable` (`kotlin.multiplatform`,
`kotlin.plugin.compose`, `kotlin.plugin.serialization`), Kotlin 2.4.20 ayant été publié depuis
l'épinglage. La matrice officielle a été consultée avant la montée
(`kotlinlang.org/docs/gradle-configure-project`) :

| KGP | Gradle testé | AGP testé |
| --- | --- | --- |
| 2.4.10 | 7.6.3 – 9.5.0 | 8.5.2 – **9.1.0** |
| 2.4.20 | 7.6.3 – 9.7.0 | 8.5.2 – **9.3.1** |

La montée **resserre** le risque plutôt que de l'élargir : AGP 9.1.1 était un patch au-dessus de
la borne de 2.4.10, il est désormais bien à l'intérieur de celle de 2.4.20, et Gradle 9.5.0 gagne
deux versions mineures de marge. KSP reste en 2.3.11 (versionnage découplé depuis KSP 2.3.0) et
detekt 2.0.0-alpha.6 reste vert. Batterie complète rejouée après la montée, tout vert.

**Point d'attention non traité :** KSP 2.3.12 est sorti le 2026-09-09 et lint ne le remonte pas.
Non monté ici, faute de nécessité démontrée.

## Défaut de production trouvé : le graphe Hilt du coordinateur ne se fermait pas

`SessionModule.provideEffectDispatcher` déclarait
`executors: Map<SessionEffectKindDto, EffectExecutor>`, que Kotlin compile en
`Map<SessionEffectKind, ? extends EffectExecutor>` — un type que Dagger considère distinct de la
`Map<SessionEffectKind, EffectExecutor>` fournie par `EffectExecutorModule`. Le défaut datait de
l'étape 11 mais restait **invisible** : aucun composant de production ne demandait encore
`SessionCoordinator`, donc Dagger ne résolvait jamais cette branche du graphe, et
`:app:assembleDebug` passait. L'étape 12a est la première à l'injecter, via
`SessionReadinessWatcher`, et l'erreur `Dagger/MissingBinding` est apparue immédiatement.

Corrigé par `@JvmSuppressWildcards` sur le paramètre. Aucun test ne pouvait l'attraper : c'est une
erreur de génération de graphe, détectée uniquement par `:app:assembleDebug` **avec un
consommateur réel**. Enseignement pour les étapes suivantes : un binding sans consommateur n'est
pas un binding vérifié.

## Écarts au plan MVP et à la spec, et pourquoi

### 1. Quatorze contrôles, pas treize

Le « Terminé quand » de l'étape parlait de treize contrôles. Le tableau de §13 en compte quatorze
depuis la scission du mode Ne pas déranger à l'étape 6. Corrigé dans le plan.

### 2. Issue à trois valeurs plutôt qu'un booléen

`ReadinessOutcome` vaut `PASSED`, `FAILED` ou `NOT_APPLICABLE`. Le troisième cas était nécessaire :
l'autorisation plein écran n'existe pas avant Android 14, proposer d'activer le NFC sur un appareil
sans matériel NFC serait un faux recours, et la validité de l'instant de réveil n'a pas d'objet
tant qu'aucune heure n'a été choisie — or l'écran de diagnostic précède le choix de l'heure dans
le parcours. Un booléen aurait forcé à mentir dans un sens ou dans l'autre. Un contrôle
`NOT_APPLICABLE` est exclu de `ActivationPolicyInputDto`. Documenté en SPEC_ANDROID §13.

### 3. Les trois contrôles de parcours ne passent pas par `checks`

§13 range « boîtier associé », « applications choisies » et « date future valide » parmi les
contrôles de disponibilité, alors qu'`ActivationPolicy` (`:shared:core`) les traite déjà par des
champs dédiés — `hasPairedBox`, `appSelectionCount`, `triggerAtEpochMillis` — avec les codes
`NO_PAIRED_BOX`, `INVALID_APP_SELECTION` et `TRIGGER_NOT_IN_FUTURE`. Les verser **aussi** dans
`checks` aurait produit deux refus pour une seule cause, l'un précis et l'autre générique.
`ReadinessDtoMapper` les route donc vers les champs dédiés ; l'écran continue de les afficher comme
les onze autres. Documenté en SPEC_ANDROID §13, prouvé par
`ReadinessDtoMapperTest.theJourneyCausesAreReportedOnceWithTheirOwnCodesRatherThanAsReadinessFailures`.

### 4. Le contrôle d'énergie repose sur la confirmation de l'utilisateur

Le plan écrivait « batterie `FAILED` tant que `isIgnoringBatteryOptimizations()` **ou**
`batteryExemptionConfirmed` est faux », c'est-à-dire exiger les deux. Appliqué tel quel, ce serait
**bloquant à vie sur HyperOS** : la mesure de l'étape 5 montre que `isIgnoringBatteryOptimizations()`
continue de renvoyer `false` après correction du réglage OEM qui commande réellement le gel. §13
dit d'ailleurs l'inverse du plan — « considérer ce contrôle comme non satisfait tant que
l'utilisateur n'a pas confirmé l'avoir fait ».

Retenu : le contrôle est satisfait par la seule confirmation de l'utilisateur, et
`isIgnoringBatteryOptimizations()` ne choisit que le recours proposé — demande d'exemption AOSP
tant qu'elle est fausse, guide OEM une fois acquise, d'où le paramètre
`ReadinessAction.OpenBatterySettings(aospExemptionGranted)`. Documenté en SPEC_ANDROID §13.

### 5. `PairedBoxStore` pris en `Optional`

Sa seule implémentation vit dans `src/debug` de `:app` jusqu'à l'étape 13. L'injecter directement
aurait cassé le graphe Hilt en `release`. `@BindsOptionalOf`, même motif que `NfcScanHandler` à
l'étape 4 : aucun binding no-op en production (CLAUDE.md), et l'absence de dépôt se traduit
exactement par « aucun boîtier associé ». L'étape 13 lie `RoomPairedBoxStore` et l'optionalité
disparaît.

### 6. `AppSelectionSource` renvoie 0 jusqu'à l'étape 13

Ce n'est pas un faux comportement de production : aucune sélection d'applications n'existe encore
dans le produit, `evaluateActivation` refuse donc l'activation avec `INVALID_APP_SELECTION`, ce qui
est exact. `AppSelectionStore` prend le relais à l'étape 13.

### 7. `AccessibilityServiceStatus` sort de `ReconcilerSources`

Le réconciliateur ne lit plus l'état du service isolément : les six contrôles de §13.1 sont
évalués d'un seul tenant par `SessionReadinessMonitor`, qui prend sa place dans `ReconcilerSources`
(`LongParameterList` de detekt interdisait un septième paramètre de constructeur).

**Comportement de l'étape 11 délibérément conservé :** la passe de réconciliation s'interrompt
avant l'évaluation du retard si — et seulement si — l'accès aux alarmes exactes ou le service
d'accessibilité manque, parce que reprogrammer une alarme exacte sans y avoir droit n'a aucun sens.
Les quatre contrôles `ANDROID_*` sont signalés sans interrompre la passe : le réveil reste
programmé, seules son audibilité ou sa présentation sont compromises. La condition porte sur l'état
courant (`failing`) et non sur « vient d'être signalé » (`newlyReported`) — sans quoi un contrôle
cassé depuis la passe précédente aurait laissé reprogrammer une alarme.

### 8. `ReadinessAction.OpenChannelSettings` sert aussi aux notifications sous Android 13

`POST_NOTIFICATIONS` n'existe qu'à partir d'Android 13 : en dessous, `RequestNotificationPermission`
n'aurait rien à demander. Le recours devient le réglage manuel du canal.

### 9. Un identifiant de notification par contrôle surveillé

§13.1 ne le précise pas. Un identifiant unique aurait fait que deux réglages cassés en même temps
s'écrasent l'un l'autre, l'utilisateur n'en voyant qu'un. Les identifiants 1 (sonnerie) et 2
(demande de scan) étant pris, les avertissements commencent à 3. Catégorie `CATEGORY_ERROR` et non
`CATEGORY_ALARM` : ces notifications signalent un réglage dégradé, jamais une alarme en cours.

### 10. `ANDROID_ALARM_VOLUME_ZERO` manquait au tableau de §7.1

Présent en §13.1, absent du tableau des codes d'incident de §7.1. Ajouté.

## Fichiers créés

`androidApp/core/system/src/main/kotlin/com/niumi/system/` :

- `readiness/ReadinessCheckId.kt`, `ReadinessAction.kt`, `ReadinessCheck.kt`,
  `DeviceReadinessChecker.kt`, `ReadinessSources.kt`, `AndroidDeviceReadinessChecker.kt`,
  `ReadinessDtoMapper.kt`, `MonitoredReadinessChecks.kt`, `SessionReadinessMonitor.kt`,
  `SessionReadinessWatcher.kt`, `di/ReadinessModule.kt`
- `notification/InterruptionFilterSource.kt`, `NotificationChannelStatus.kt`,
  `SessionWarningNotificationSpecs.kt`, `SessionWarningNotifier.kt`
- `power/BatteryOptimizationStatus.kt`
- `setup/SetupPreferences.kt`
- `apps/AppSelectionSource.kt`

Tests : `readiness/AndroidDeviceReadinessCheckerTest.kt`, `readiness/ReadinessDtoMapperTest.kt`,
`readiness/SessionReadinessMonitorTest.kt`, `readiness/fakes/ReadinessTestSources.kt`,
`readiness/fakes/FakeSessionWarningNotifier.kt`,
`notification/SessionWarningNotificationSpecsTest.kt`, et l'instrumenté
`androidTest/.../notification/AndroidSessionWarningNotifierInstrumentedTest.kt`.

## Fichiers modifiés

- `androidApp/core/system/build.gradle.kts` : `datastore-preferences` (déjà au catalogue, déjà
  utilisé par `:core:database` et la route POC — pas une dépendance nouvelle).
- `androidApp/core/system/.../notification/NiumiNotificationChannels.kt` : troisième canal.
- `androidApp/core/system/.../session/ReconcilerSources.kt`, `SessionReconciler.kt`,
  `di/SessionModule.kt`, `di/SystemNotificationModule.kt`.
- `androidApp/core/system/src/test/.../session/fakes/TestCoordinatorHarness.kt`,
  `session/SessionReconcilerTest.kt` (trois tests ajoutés).
- `androidApp/app/src/main/kotlin/com/niumi/app/NiumiApplication.kt` : enregistrement du receveur
  `ACTION_INTERRUPTION_FILTER_CHANGED`.
- `gradle/libs.versions.toml` : Kotlin 2.4.20.
- `specs/SPEC_ANDROID.md` : §7.1, §13, §13.1.
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : versions, note AGP, étape 12.

## Vérifications exécutées

| Commande | Résultat |
| --- | --- |
| `:core:system:testDebugUnitTest` | **134 tests verts** (91 à l'étape 11, 43 nouveaux) |
| `:shared:core:jvmTest` | vert, non-régression (160) |
| `:core:database:testDebugUnitTest` | vert, non-régression |
| `:feature:ringing:testDebugUnitTest` | vert, non-régression |
| `:feature:session:testDebugUnitTest` | vert, non-régression |
| `:app:testDebugUnitTest` | vert, non-régression |
| `:app:assembleDebug` | vert (graphe Hilt fermé après correction du wildcard) |
| `:core:system:compileDebugAndroidTestKotlin` | vert |
| `ktlintCheck` | vert sur tout le dépôt |
| `detekt` | vert sur tout le dépôt |
| `:app:lintDebug` | vert, aucune remontée |

Le JDK 17 exigé par `jvmToolchain(17)` n'est pas sur le `PATH` de ce poste : les commandes ont été
lancées avec `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.

## Validation sur appareil réel (2026-09-11)

**Xiaomi 25080RABDG, Android 16 (API 36).** Les 6 tests instrumentés de
`AndroidSessionWarningNotifierInstrumentedTest` sont **verts** (10/10 sur `:core:system` avec les
4 préexistants). Le receveur `ACTION_INTERRUPTION_FILTER_CHANGED` de §13.1 est vivant dans le
processus, vu dans `dumpsys activity broadcasts`. Le protocole manuel de §13 a été déroulé avec
l'écran de diagnostic de la passe 12b : résultats détaillés dans `ETAPE-12B.md`.

## Points restants (12a)

- **`MainActivity.ON_RESUME` n'appelle pas encore `SessionReadinessWatcher`** : §13.1 liste le
  passage au premier plan parmi les déclencheurs, il est câblé à la passe 12b avec la réécriture de
  `MainActivity`.
- **Le déclencheur « au déclenchement, avant de démarrer la sonnerie »** relève d'`AlarmReceiver`,
  étape 17.
- **Le tap de l'avertissement ouvre `MainActivity`**, pas encore le diagnostic d'incident : l'écran
  12 est livré à l'étape 16.
- **`SessionReadinessWatcher` ne voit que le snapshot publié en mémoire.** Au démarrage du
  processus, tant qu'aucune réconciliation n'a publié de session, un changement de filtre
  d'interruption ne déclenche rien. La réconciliation reste le chemin qui découvre une session
  depuis la persistance ; elle n'a elle-même aucun appelant de production avant l'étape 17.
- **Trois avertissements de dépréciation préexistants** apparaissent à la compilation sous Kotlin
  2.4.20 : `createComposeRule` (v2 disponible), `hiltViewModel` (package déplacé),
  `ActivityManager.getRunningServices`. Aucun n'est nouveau ni bloquant ; à traiter quand les
  écrans seront retouchés.

## Validation sur appareil réel — **non faite**

Rien de ce qui suit n'a été exécuté : aucun appareil n'était branché et l'utilisateur n'a pas été
sollicité pour en brancher un. À dérouler avant de déclarer l'étape 12 terminée.

1. `adb devices`, puis `./gradlew :core:system:connectedDebugAndroidTest` — 6 tests nouveaux
   (`AndroidSessionWarningNotifierInstrumentedTest`) plus la non-régression des tests existants.
2. Protocole manuel de §13, essai par essai, avec pour chacun le résultat attendu :
   - refuser les notifications → contrôle `NOTIFICATIONS` en échec, recours « demander la
     permission » ;
   - retirer l'autorisation plein écran (Android 14+) → contrôle en échec, recours vers les
     réglages plein écran ;
   - volume d'alarme à zéro → blocage `BLOCKING_FOR_ALARM` ;
   - Ne pas déranger « alarmes seules » → **avertissement seul**, activation toujours permise ;
   - Ne pas déranger en silence total → blocage `BLOCKING_FOR_ALARM` ;
   - service d'accessibilité désactivé → blocage ;
   - couper le canal `niumi_alarm_ringing` dans les réglages → contrôle `ALARM_CHANNEL` en échec.
   *(Ces essais exigent l'écran de diagnostic de la passe 12b pour être observés par l'interface.
   En 12a, ils ne sont vérifiables que par test instrumenté ou journal.)*
3. Surveillance §13.1 : basculer le filtre d'interruption pendant que le processus vit doit
   réveiller le receveur. **Non déroulable complètement avant l'étape 14**, faute de parcours
   d'activation permettant d'obtenir une session `ARMED` réelle.
4. Consigner modèle, version d'Android et résultats observés.
