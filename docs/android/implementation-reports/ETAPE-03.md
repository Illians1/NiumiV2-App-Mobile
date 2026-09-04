# Étape 3 — alarme exacte, receiver, service de sonnerie, audio et écran de réveil

Date : 2026-09-04. **Validée sur appareil réel** (Redmi 25080RABDG, HyperOS, Android 16 / API 36).

## Résumé

Première chaîne système complète : `AlarmManager.setAlarmClock()` → `AlarmReceiver` →
`AlarmRingingService` en premier plan → notification `CATEGORY_ALARM` avec full-screen intent
→ `AlarmActivity`, pilotable depuis une route de debug (`PocScreen`). Aucun bouton d'arrêt nulle
part. `:shared:core` n'est pas encore consommé par le receiver (moteur KMP absent avant la
Phase C, étape 17) : écart assumé, documenté ci-dessous et dans le code.

**La validation sur appareil réel a révélé trois bugs de production que les tests automatisés ne
pouvaient pas attraper.** Les trois auraient rendu le réveil totalement muet en conditions
réelles. Ils sont corrigés et désormais couverts par des tests (section « Bugs trouvés »).

Deux décisions structurantes ont été validées avec l'utilisateur avant l'implémentation :

1. **Descripteurs purs + applicateurs Android**, pour que les vérifications système du plan MVP
   (`PendingIntent`, audio, notification) tournent en JVM sans Robolectric.
2. **Huitième module `:core:designsystem`**, pour que les modules `feature` (notamment
   `AlarmActivity`) accèdent au thème Niumi sans dépendre de `:app`.

## Bugs de production trouvés sur appareil et corrigés

Aucun n'était détectable par les tests unitaires JVM : tous vivent dans la couture entre
composants, ou dans une validation que seul le système Android effectue.

### 1. Notification sans petite icône → le service mourait au démarrage

`RingingNotificationFactory` n'appelait pas `setSmallIcon`. Construire la `Notification` ne
valide rien ; c'est `startForeground()` qui la valide, et le système la rejetait avec
`RemoteServiceException$CannotPostForegroundServiceNotificationException: Bad notification for
startForeground`. En production, l'alarme n'aurait jamais sonné.

Correction : `ic_niumi_notification.xml` créé dans `:core:designsystem` (silhouette blanche
24 dp, format imposé par Android), résolu via une nouvelle `NotificationIconResolver` — même
motif que `NiumiComponentResolver`, puisque `:core:system` ne peut pas dépendre de
`:core:designsystem` (SPEC_ANDROID §6).

### 2. Canaux de notification jamais créés

`AndroidNotificationChannelRegistrar.registerAll()` n'était appelé nulle part en production.
Poster sur un canal inexistant fait rejeter la notification par le système — même symptôme que
le bug 1, même conséquence.

Correction : appel dans `NiumiApplication.onCreate()` (hygiène : les canaux apparaissent dans
les réglages Android dès l'installation) **et** en tête de `AlarmRingingService.onStartCommand()`
(idempotent, indispensable si le service démarre dans un processus recréé sans passage par
l'interface).

### 3. `revision` écrite comme chaîne, relue comme `Long` → alarme silencieuse

Le plus grave, et le plus discret. `AlarmPendingIntentSpecs` portait
`extras: Map<String, String>` et écrivait `"revision" to revision.toString()` ;
`AlarmReceiver` la relit avec `getLongExtra`. Un extra `String` relu en `Long` ne lève rien :
`hasExtra` répond vrai, `getLongExtra` renvoie sa valeur par défaut (`-1`), `ServiceCommand.from`
rejette alors la commande en `INVALID_REVISION`, et le receiver s'arrête **sans démarrer le
service et sans aucune trace**.

Observé exactement ainsi sur appareil : l'alarme se déclenche à la seconde près
(`SmartPower: com.niumi.app: idle->background R(alarm start)` à l'heure prévue), le processus
est réveillé, et rien ne se passe.

Pourquoi aucun test ne l'a vu : les deux côtés étaient testés séparément et chacun passait —
`AlarmPendingIntentSpecsTest` vérifiait la chaîne `"1"`, `ServiceCommandTest` recevait un vrai
`Long`, et `AlarmReceiverInstrumentedTest` construisait son `Intent` à la main sans traverser la
fabrique.

Correction : type `IntentExtraValue` (`Text` / `Number`) porté jusqu'à `putExtra`, ce qui rend
la classe d'erreur impossible. Deux tests ajoutés :
- `AlarmPendingIntentSpecsTest.revisionExtraIsNumericNotText` — verrouille le type, pas la valeur ;
- `AlarmReceiverInstrumentedTest.realAlarmPendingIntentStartsTheRingingService` — déclenche le
  **vrai** `PendingIntent` produit par la fabrique, comme le fait `AlarmManager` : c'est le test
  qui aurait attrapé le bug.

## Décisions prises pendant l'implémentation

1. **`RingtoneResourceResolver`, `NiumiComponentResolver`, `NotificationIconResolver`.**
   `:core:system` ne peut référencer ni le `R.raw` de `:feature:ringing`, ni les classes cibles
   des `PendingIntent`, ni les assets de `:core:designsystem`. Trois interfaces de résolution
   symbolique, injectées en paramètre ; implémentations dans les modules downstream (`:app` pour
   les composants et l'icône, `:feature:ringing` pour la sonnerie).
2. **`TechnicalEventLog` avancé à l'étape 3.** Le plan MVP le place à l'étape 9, mais
   `AlarmReceiver` en a besoin dès maintenant. `InMemoryTechnicalEventLog` (borné à 200 entrées,
   thread-safe) est provisoire ; l'implémentation Room la remplacera sans changer l'interface.
3. **`NiumiAlarmWavTest` en JVM plutôt qu'instrumenté.** Contrôle statique d'en-tête WAV, sans
   dépendance au runtime Android : rejoué à chaque modification, sans appareil.
4. **Renommage `MediaPlayerAlarmAudioEngine` → `DefaultAlarmAudioEngine`** : la classe orchestre
   des interfaces injectées et ne connaît plus `MediaPlayer`.
5. **`CoroutineDispatcher` injecté (`@DefaultDispatcher`)** — exigé par la règle detekt
   `InjectDispatcher` ; provider unique, `@Suppress` documenté à l'unique endroit légitime.
6. **`SystemModule` scindé en trois** (`SystemModule`, `AudioModule`,
   `SystemNotificationModule`) : 12 méthodes `@Provides` dépassaient le seuil `TooManyFunctions`.
7. **Thème plateforme `Theme.Niumi` déplacé de `:app` vers `:core:designsystem`.** `AlarmActivity`
   le référence dans le manifeste de `:feature:ringing` ; l'APK de test propre à ce module ne
   contient pas les ressources de `:app`, et la liaison des ressources échouait
   (`resource style/Theme.Niumi not found`). Découvert en lançant les tests instrumentés.
8. **`ServiceTestRule` écartée.** Le plan MVP la suggérait ; elle démarre le service via
   `bindServiceAndWait()` et échoue sur `TimeoutException` dès que `onBind()` renvoie `null` —
   c'est-à-dire exactement dans le cas que SPEC_ANDROID §10.2 impose (aucun binder exposé). Elle
   est structurellement incompatible avec ce service. Remplacée par un démarrage identique à la
   production plus l'observation de `RunningServiceInfo.foreground`, et par `onNullBinding` pour
   prouver l'absence de binder.
9. **Tests instrumentés indépendants de `POST_NOTIFICATIONS`.** Vérifier la présence de la
   notification dans le volet exige cette permission d'exécution, que HyperOS refuse d'accorder à
   un APK de test (voir « Contraintes de l'appareil »). Ce qui doit être prouvé — le service
   atteint le premier plan — n'en dépend pas : les tests observent l'état réel du service. Le
   contenu de la notification reste couvert par `RingingNotificationSpecsTest` (JVM) et
   `RingingNotificationFactoryInstrumentedTest` (vraie `Notification`).

## Écarts assumés

1. **`AlarmReceiver` court-circuite le moteur commun.** SPEC_ANDROID §10.1 exige qu'il transmette
   `ALARM_FIRED` à `NiumiCoreFacade`, qui n'existe pas avant la Phase C : il appelle directement
   `RingingController`. Levé à l'étape 17.
2. **`onStartCommand(intent == null)`** publie une notification silencieuse et journalise
   `PROCESS_RECREATED` : la reconstruction depuis le snapshot arrive à l'étape 17.
3. **Journal technique en mémoire**, perdu au redémarrage du processus ; Room à l'étape 9.
4. **`AlarmActivity.intent()`** n'a encore aucun appelant — à rattacher à l'étape 17 ou à
   supprimer.
5. **Deux avertissements de dépréciation du compilateur Kotlin**, sans effet sur le build :
   `hiltViewModel()` (androidx.hilt 1.4.0 annonce un package qui n'existe pas encore dans cette
   version) et `createComposeRule()` (une v2 existe). À revoir lors d'une montée de version.

## Contraintes de l'appareil de test (Xiaomi / HyperOS)

Utiles pour la matrice de tests physiques (§20) et le dossier Play (§23) — HyperOS impose
plusieurs restrictions absentes d'Android AOSP :

- `INSTALL_FAILED_USER_RESTRICTED` **intermittent** sur l'installation par session (split APK)
  utilisée par AGP, alors que `adb install` en flux direct passe toujours. Contournement utilisé
  pendant la mise au point : `adb install` + `adb shell am instrument`. Le chemin Gradle
  standard a fini par fonctionner sans modification.
- `UiAutomation.grantRuntimePermission` (donc `GrantPermissionRule`) → `SecurityException`.
- `adb shell pm grant` → `SecurityException: Neither user 2000 nor current process has
  android.permission.GRANT_RUNTIME_PERMISSIONS`.

Conséquence retenue : ne pas faire dépendre un test instrumenté d'une permission d'exécution.

## Résultats de la validation sur appareil

### Tests instrumentés — 6/6 verts

`./gradlew :core:system:connectedDebugAndroidTest :feature:ringing:connectedDebugAndroidTest`
→ BUILD SUCCESSFUL (1 test `:core:system`, 5 tests `:feature:ringing`).

### Vérification de l'alarme programmée (`dumpsys alarm`)

| Contrôle SPEC_ANDROID §9.1 | Observé |
| --- | --- |
| API imposée | Bloc `Alarm clock:` avec `triggerTime` et `showIntent` → `setAlarmClock()` confirmé |
| `PendingIntent` explicite vers le receiver | `tag=*walarm*:com.niumi.app/com.niumi.feature.ringing.AlarmReceiver` |
| Alarme exacte | `type=RTC_WAKEUP`, `window=0`, `maxWhenElapsed == whenElapsed` (aucun report) |
| Autorisation | `exactAllowReason=policy_permission` (via `USE_EXACT_ALARM`, sans `SCHEDULE_EXACT_ALARM`) |
| Doze | listée comme `Next wake from idle` |

### Essais manuels

| Essai | Résultat |
| --- | --- |
| Alarme à 90 s, écran éteint et verrouillé | **OK** — sonnerie audible, écran allumé sur `AlarmActivity` par-dessus le verrouillage, heure et texte « Scanne ton boîtier Niumi pour arrêter l'alarme. », notification sans aucun bouton, aucun moyen d'arrêter la sonnerie depuis l'écran |
| Fermeture de l'écran de réveil (retour/accueil) | **OK** — sonnerie maintenue ; `isForeground=true`, `topResumedActivity` passé au launcher |
| Retrait de Niumi des applications récentes | **OK** — sonnerie maintenue, processus vivant, wake lock toujours détenu |
| Programmer puis annuler depuis le POC | **OK** — plus aucune alarme Niumi dans `dumpsys alarm`, aucun son au moment prévu |

### Preuves système relevées pendant la sonnerie

- Service : `isForeground=true`, `foregroundId=1`, `types=0x00000002`
  (`FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`).
- Notification : `channel=niumi_alarm_ringing`, `importance=4`, `category=alarm`, `sound=null`
  (pas de double lecture, §10.3), `flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY`,
  **aucune action**.
- Audio : `AudioAttributes: usage=USAGE_ALARM content=CONTENT_TYPE_SONIFICATION`.
- Wake lock : `PARTIAL_WAKE_LOCK 'niumi:AlarmRinging' … LONG`, détenu pendant toute la sonnerie,
  puis `REL niumi:AlarmRinging` à l'arrêt — `onDestroy()` joue bien son rôle de filet de sécurité.

### Note pour la règle Android 17 de §10.2

Le journal audio de l'appareil contient
`AudioHardening focus request for req 3 would be ignored for com.niumi.app … level: full`,
alors que la sonnerie est bel et bien audible. Le durcissement ignorerait donc la **demande de
focus**, sans couper la lecture `USAGE_ALARM`. À reconfirmer sur un appareil Android 17 réel avec
`adb shell cmd audio set-enable-hardening throw` (non applicable ici : Android 16).

## Fichiers créés

**`:core:designsystem`** (nouveau module) : `build.gradle.kts`, `src/main/AndroidManifest.xml`,
`ui/theme/NiumiTheme.kt` (déplacé de `:app`), `res/values/themes.xml` (déplacé de `:app`,
décision 7), `res/drawable/ic_niumi_notification.xml` (bug 1).

**`:core:system`** : `common/{Clock,IdGenerator,OperationResult,DefaultDispatcher}.kt` ;
`intent/{NiumiComponent,NiumiComponentResolver,IntentExtraValue,PendingIntentSpec,
AndroidPendingIntentFactory}.kt` ;
`alarm/{AlarmPendingIntentSpecs,AlarmScheduler,AndroidAlarmScheduler}.kt` ;
`audio/{AlarmAudioConfiguration,AlarmAudioEngine,AlarmPlayer,AudioFocusController,
VibrationController,DefaultAlarmAudioEngine,RingtoneResourceResolver,
MediaPlayerAlarmPlayerFactory,AndroidAudioFocusController,AndroidVibrationController}.kt` ;
`notification/{NotificationChannelSpec,NotificationSpec,NiumiNotificationChannels,
RingingNotificationSpecs,NotificationIconResolver,AndroidNotificationChannelRegistrar,
RingingNotificationFactory}.kt` ; `power/{WakeLockHolder,AndroidWakeLockHolder}.kt` ;
`ringing/RingingController.kt` ; `di/{SystemModule,AudioModule,SystemNotificationModule}.kt`.
Tests JVM : `common/UuidIdGeneratorTest.kt`, `alarm/AlarmPendingIntentSpecsTest.kt`,
`audio/DefaultAlarmAudioEngineTest.kt`, `notification/{NiumiNotificationChannelsTest,
RingingNotificationSpecsTest}.kt`. Instrumenté :
`androidTest/notification/RingingNotificationFactoryInstrumentedTest.kt`.

**`:core:database`** : `logging/{TechnicalEventType,TechnicalEventLog,
InMemoryTechnicalEventLog}.kt`, `logging/di/LoggingModule.kt`. Test :
`logging/InMemoryTechnicalEventLogTest.kt`.

**`:feature:ringing`** : `ServiceCommand.kt`, `AlarmReceiver.kt`, `AlarmRingingService.kt`,
`AndroidRingingController.kt`, `AlarmActivity.kt`, `ui/{AlarmScreenState,AlarmScreen}.kt`,
`di/RingingModule.kt`, `res/raw/niumi_alarm.wav`. Tests JVM : `ServiceCommandTest.kt`,
`NiumiAlarmWavTest.kt`, `ui/AlarmScreenStateTest.kt`. Instrumentés :
`androidTest/AndroidManifest.xml` (permissions et activité hôte Compose, test uniquement),
`androidTest/{HiltTestRunner,TestComponentResolverModule,AlarmReceiverInstrumentedTest,
AlarmRingingServiceInstrumentedTest}.kt`, `androidTest/ui/AlarmScreenNoStopActionTest.kt`.

**`:app`** : `navigation/{NavGraphContributor,NavigationModule}.kt`,
`ui/{HomeScreen,NiumiNavHost}.kt`, `system/AppComponentResolver.kt`, `di/AppModule.kt` ; en
`src/debug` : `poc/{PocViewModel,PocScreen,PocNavigation}.kt`.

**`tools/generate_alarm_wav.py`** : synthèse déterministe de la sonnerie (mono, PCM 16 bits,
44,1 kHz, 6 s, 740 Hz + 988 Hz). Reproductibilité octet pour octet vérifiée.

## Fichiers modifiés

- `settings.gradle.kts` : ajout de `:core:designsystem`.
- `gradle/libs.versions.toml` : ajout de `hilt-android-testing`.
- `androidApp/app/build.gradle.kts`, `androidApp/feature/{ringing,setup,session}/build.gradle.kts` :
  dépendance vers `:core:designsystem` ; `:feature:ringing` gagne `activity-compose`,
  `hilt-android-testing`, `truth` (androidTest), `kspAndroidTest`, `HiltTestRunner` comme
  `testInstrumentationRunner`, et `niumi.rootDir` pour `NiumiAlarmWavTest`.
- `androidApp/core/system/build.gradle.kts` : `truth` en `androidTestImplementation`.
- `androidApp/app/.../MainActivity.kt` : injecte `Set<NavGraphContributor>`, pose `NiumiNavHost`.
- `androidApp/app/.../NiumiApplication.kt` : création des canaux de notification (bug 2).
- `androidApp/app/src/test/.../ModuleListTest.kt` : huit modules attendus.
- `androidApp/feature/ringing/src/main/AndroidManifest.xml` : `AlarmReceiver`,
  `AlarmRingingService`, `AlarmActivity` (§14).
- `specs/SPEC_ANDROID.md` §6 : `:core:designsystem` (modules, responsabilités, dépendances).
- `specs/SPEC_CORE_KMP.md` §15 : `designsystem/` dans l'arborescence.
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : huit modules, cases de l'étape 3.
- `CLAUDE.md` : sous-section « Validation sur appareil réel ».

## Fichiers supprimés

- `androidApp/{core/database,core/system,feature/ringing}/.../PackageInfo.kt` (repères de
  l'étape 1, remplacés par du code réel).
- `androidApp/app/src/main/kotlin/com/niumi/app/ui/theme/NiumiTheme.kt` et
  `androidApp/app/src/main/res/values/themes.xml` (déplacés vers `:core:designsystem`).

## Commandes exécutées et résultat

Environnement : `JAVA_HOME=/opt/homebrew/opt/openjdk@17`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`
exportés à chaque appel (voir `ETAPE-01.md`).

| Commande | Résultat |
| --- | --- |
| `./gradlew :core:system:testDebugUnitTest` | BUILD SUCCESSFUL — 23 tests, 0 échec |
| `./gradlew :core:database:testDebugUnitTest` | BUILD SUCCESSFUL — 4 tests, 0 échec |
| `./gradlew :feature:ringing:testDebugUnitTest` | BUILD SUCCESSFUL — 11 tests, 0 échec |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL — 2 tests, 0 échec |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew ktlintCheck` | BUILD SUCCESSFUL (après `ktlintFormat`) |
| `./gradlew detekt` | BUILD SUCCESSFUL (après découpage de `SystemModule` et un `@Suppress` documenté) |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL (`warningsAsErrors=true`) |
| `./gradlew :shared:core:jvmTest` | BUILD SUCCESSFUL — non-régression |
| `./gradlew :core:system:connectedDebugAndroidTest :feature:ringing:connectedDebugAndroidTest` | BUILD SUCCESSFUL — 6 tests sur Redmi 25080RABDG (Android 16) |

Contrôle complémentaire : `grep -rniE "stop|arrêt|arreter"` sur `:feature:ringing/.../ui` et
`:core:system/.../notification` ne révèle que les textes imposés par la spec et des commentaires
expliquant l'absence d'action d'arrêt.

## Tests non exécutés

- `adb shell cmd audio set-enable-hardening throw` : commande spécifique à Android 17, appareil
  de test en Android 16. Reste à faire sur un appareil Android 17.
- `./gradlew connectedDebugAndroidTest` **à la racine** : installe un APK de test vide pour les
  six modules sans `androidTest`, ce qui multiplie les installations et déclenche la restriction
  HyperOS. Les deux modules qui portent réellement des tests sont ciblés explicitement.

## Incertitudes restantes

- **Un seul appareil, une seule marque.** La matrice §20 exige Pixel, Samsung et Xiaomi. Seul le
  Xiaomi est couvert. Le comportement du full-screen intent et de Doze sur les autres surcouches
  reste inconnu jusqu'à la porte de validation 0 (étape 6).
- **Doze réel non testé.** L'essai s'est fait sur 90 secondes, écran verrouillé, sans Doze forcé
  (`adb shell dumpsys deviceidle force-idle`) ni attente prolongée. À couvrir à l'étape 6.
- **`POST_NOTIFICATIONS` n'est jamais demandée par l'application.** Elle a dû être accordée à la
  main pour l'essai manuel ; sans elle, le full-screen intent ne s'ouvre pas. C'est une
  précondition documentée (§4.1) que le diagnostic d'activation devra vérifier et faire accorder
  (onboarding des permissions, étape ultérieure). À ne pas oublier : aujourd'hui, une
  installation neuve ne réveillerait pas l'utilisateur.
- **Écart §10.1** (receiver ↔ moteur commun) : à lever à l'étape 17.
- **`RingtoneResourceResolver` avec une seule clé** (`"niumi_alarm"`) : suffisant pour le MVP.
