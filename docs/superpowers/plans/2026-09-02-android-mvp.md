# Plan d'implémentation du MVP Android Niumi

> **Pour l'agent chargé de l'exécution :** utiliser `superpowers:subagent-driven-development` ou `superpowers:executing-plans`. Suivre les cases dans l'ordre et s'arrêter aux portes de validation manuelle. Ne pas commit ni push sans demande explicite.

**Objectif :** livrer le MVP Android décrit par les spécifications, avec réveil exact, blocage comportemental des applications et fin ou annulation uniquement par scan NFC.

**Architecture :** créer les modules définitifs dès le POC. `:shared:core` est l'unique autorité métier. Les composants Android convertissent les événements système, persistent les décisions et exécutent les effets retournés par KMP. Aucun composable, service, receiver ou ViewModel ne modifie directement l'état d'une session.

**Stack :** Kotlin, Jetpack Compose Material 3, Hilt, Coroutines et Flow, Room, DataStore, Navigation Compose, Kotlin Multiplatform, `kotlinx-datetime` et `kotlinx-serialization`.

**Spécifications :** `specs/SPEC_CORE_KMP.md`, `specs/SPEC_ANDROID.md`, `AGENTS.md`.

## Contraintes globales

- [ ] Utiliser les chemins physiques `androidApp/` et les modules Gradle `:app`, `:core:database`, `:core:system`, `:feature:setup`, `:feature:session`, `:feature:ringing` et `:shared:core`.
- [ ] Utiliser `applicationId = "com.niumi.app"` et les namespaces `com.niumi.*`.
- [ ] Configurer JDK 17, Gradle 9.3.1, AGP 9.1.1, Kotlin 2.4.10, `minSdk 29`, `targetSdk 36` et `compileSdk 37`.
- [ ] Centraliser les versions dans `gradle/libs.versions.toml`, sans version dynamique. Utiliser Compose BOM 2026.08.00, Navigation Compose 2.9.8, Room 2.8.4, DataStore 1.2.1, Hilt 2.60.1, AndroidX Hilt 1.4.0 et KSP 2.3.10.
- [ ] Vérifier dès le bootstrap que Kotlin 2.4.10 fonctionne avec AGP 9.1.1. API 37 exige AGP 9.1.1, tandis que la matrice KMP cite AGP 9.1.0 comme borne testée. En cas d'incompatibilité, arrêter et proposer une mise à jour explicite des specs. Ne pas réduire silencieusement `compileSdk`.
- [ ] N'ajouter ni permission `INTERNET`, ni appel réseau dans le parcours critique.
- [ ] Déclarer `USE_EXACT_ALARM`, jamais `SCHEDULE_EXACT_ALARM`.
- [ ] Rédiger les textes affichés et la documentation avec le skill `humanizer`.
- [ ] Utiliser Material 3, le tutoiement, un contraste nocturne lisible, TalkBack, le bord à bord et le retour prédictif.
- [ ] Générer une sonnerie WAV originale : mono, PCM 16 bits, 44,1 kHz, six motifs d'une seconde avec 750 ms d'un mélange 740 Hz et 988 Hz, suivis de 250 ms de silence. Appliquer des fondus de 10 ms et empaqueter le résultat dans `res/raw/niumi_alarm.wav`.
- [ ] À la fin de chaque lot, créer `docs/android/implementation-reports/LOT-N.md` avec les fichiers modifiés, commandes exécutées, résultats et validations matérielles restantes.

## Interfaces à établir

### Contrat KMP public

```kotlin
class NiumiCoreFacade {
    fun reduce(snapshot: SessionSnapshotDto?, event: SessionEventDto): SessionDecisionDto
    fun computeWakeSchedule(input: WakeScheduleInputDto): WakeScheduleResultDto
    fun parseBoxPayload(uri: String): BoxPayloadResultDto
    fun verifyBox(
        payload: BoxPayloadDto,
        credential: PairedBoxCredentialDto,
        context: NfcVerificationContextDto?
    ): BoxVerificationResultDto
    fun evaluateActivation(input: ActivationPolicyInputDto): ActivationPolicyResultDto
}
```

La façade reste synchrone, déterministe et sans type Android, exception publique, `Flow`, fonction `suspend` ou horloge globale.

### Contrats Android internes

Créer dans `:core:system` :

```kotlin
interface AlarmScheduler {
    fun schedule(session: AndroidSessionProjection): OperationResult
    fun cancel(sessionId: String): OperationResult
    fun isScheduled(sessionId: String): Boolean
}

interface AlarmAudioEngine {
    suspend fun start(ringtoneKey: String, vibrationEnabled: Boolean): OperationResult
    suspend fun stop(): OperationResult
    fun isPlaying(): Boolean
}

interface BlockingController {
    suspend fun apply(sessionId: String, packages: Set<String>): OperationResult
    suspend fun remove(sessionId: String): OperationResult
    suspend fun effectivePackages(sessionId: String): Set<String>
}

interface SessionCoordinator {
    suspend fun dispatch(event: SessionEventDto): DispatchResult
    suspend fun reconcile(reason: ReconcileReason): ReconcileResult
}
```

Tous les adaptateurs sont idempotents. `SessionCoordinator` sérialise `AlarmReceiver`, les scans NFC et la réconciliation avec un même `Mutex`.

## Étapes d'implémentation

### Lot 1 : bootstrap du monorepo

**Fichiers :** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, les `build.gradle.kts` des modules, `.github/workflows/mobile.yml` et les configurations ktlint, detekt et Lint.

- [ ] Écrire les tests de build minimaux et vérifier qu'ils échouent avant la création des modules.
- [ ] Déclarer les dépôts, plugins, versions et mappings entre les chemins Gradle et `androidApp/`.
- [ ] Configurer le plugin `com.android.kotlin.multiplatform.library` avec les cibles JVM, iOS ARM64 et simulateur ARM64.
- [ ] Créer une application Compose vide, ses manifests et un test de démarrage par module.
- [ ] Configurer la CI macOS avec JDK 17, SDK 37 et un préflight Xcode 26.4.
- [ ] Vérifier :

```bash
./gradlew projects
./gradlew :app:assembleDebug
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64
./gradlew lintDebug detekt ktlintCheck
```

**Résultat attendu :** tous les modules sont résolus, l'APK vide et le framework KMP sont produits sans avertissement nouveau.

### Lot 2 : shell applicatif et limites de modules

**Fichiers :** `androidApp/app/src/main/kotlin/com/niumi/app/NiumiApplication.kt`, `MainActivity.kt`, `navigation/NiumiNavHost.kt`, les interfaces de `androidApp/core-system/`, le thème et les ressources communes.

- [ ] Écrire les tests d'architecture interdisant les dépendances de `:shared:core` vers Android et les accès directs aux API système depuis les features.
- [ ] Configurer Hilt, Navigation Compose, le thème Material 3 et les routes typées.
- [ ] Créer les interfaces système, stores et horloges injectables.
- [ ] Ajouter un écran d'accueil minimal sans session et sans donnée fictive.
- [ ] Vérifier :

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

### Lot 3 : POC système dans le squelette final

**Composants :** `AndroidAlarmScheduler`, `AlarmReceiver`, `AlarmRingingService`, `MediaPlayerAlarmAudioEngine`, `AlarmActivity`, lecteur NFC Reader Mode et `NiumiBlockingAccessibilityService`.

- [ ] Écrire les tests des `PendingIntent` explicites, immuables et stables.
- [ ] Implémenter `setAlarmClock()` avec `AlarmClockInfo` et les deux `PendingIntent`.
- [ ] Générer et empaqueter `niumi_alarm.wav`, puis vérifier son format et sa durée par test.
- [ ] Démarrer le service `mediaPlayback`, appeler immédiatement `startForeground()`, utiliser `USAGE_ALARM`, boucler le WAV et gérer vibration et wake lock.
- [ ] Ajouter la notification `CATEGORY_ALARM`, sans action d'arrêt, avec full-screen intent vers `AlarmActivity`.
- [ ] Afficher l'activité au-dessus du verrouillage et activer Reader Mode dans son cycle de vie.
- [ ] Ajouter en configuration debug une preuve d'association NFC et une application factice à bloquer. Aucun fake ne doit entrer dans `main` ou `release`.
- [ ] Implémenter le retour à l'accueil et l'overlay d'accessibilité limité à trois secondes.
- [ ] Préparer la divulgation, le consentement et les documents Play dans `docs/android/play-console/`.
- [ ] Vérifier les tests unitaires et instrumentés, ainsi que le comportement Android 17 :

```bash
adb shell cmd audio set-enable-hardening throw
```

### Porte de validation 0

- [ ] Tester le POC sur un Pixel, un Samsung et un Xiaomi avec écran éteint, Doze, verrouillage, NFC réel et application bloquée.
- [ ] Vérifier le déclenchement, le son, le plein écran, le scan et l'overlay.
- [ ] Préparer la vidéo Play montrant divulgation, consentement et usage réel de l'AccessibilityService.
- [ ] Soumettre une version sur piste interne ou fermée dès que le compte le permet.
- [ ] Inscrire les résultats dans `docs/android/implementation-reports/LOT-0.md`.

Ne poursuivre qu'après validation humaine du POC. Un refus Play ou une incompatibilité OEM qui remet en cause le produit exige une révision explicite des specs.

### Lot 4 : moteur KMP développé en TDD

**Fichiers :** `shared/core/src/commonMain/kotlin/com/niumi/core/{domain,schedule,nfc,diagnostics,interop}/` et `shared/core/src/commonTest/`.

- [ ] Écrire les tests des DTO, révisions, états, événements, violations et effets avant les implémentations.
- [ ] Implémenter `SessionEngine.reduce()` et toutes les transitions, avec `RELEASING` obligatoire avant `COMPLETED` ou `CANCELLED`.
- [ ] Ajouter une politique pure de classification des reçus en `NEW`, `DUPLICATE` ou `CONFLICT`. Le stockage du registre reste natif.
- [ ] Implémenter la politique horaire, les cas DST, l'instant immuable après activation et le retard de 15 minutes.
- [ ] Implémenter le parseur NFC canonique, le hash SHA-256, la comparaison constante et la preuve opaque liée à un seul événement.
- [ ] Implémenter la limite de 1 à 50 applications et la politique d'activation.
- [ ] Exposer uniquement `NiumiCoreFacade` et ses DTO simples.
- [ ] Ajouter les fixtures partagées et les tests de fuzzing des payloads.
- [ ] Vérifier :

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64
```

### Lot 5 : Room, outbox et snapshot Direct Boot

**Fichiers :** `androidApp/core-database/src/main/kotlin/com/niumi/database/{entity,dao,directboot}/` et `NiumiDatabase.kt`.

- [ ] Créer les entités, convertisseurs d'enums et DAO décrits dans les specs.
- [ ] Écrire une transaction Room qui persiste le snapshot canonique, le reçu et l'outbox ensemble.
- [ ] Limiter le journal technique aux 200 dernières entrées autorisées.
- [ ] Écrire le snapshot Direct Boot JSON avec `AtomicFile` dans un `DeviceProtectedStorageContext`.
- [ ] Refuser une projection de révision inférieure et accepter une réécriture idempotente à révision égale.
- [ ] Interdire l'ouverture de Room avant `UserManager.isUserUnlocked`.
- [ ] Tester les mappings Room vers KMP, KMP vers Room et Direct Boot vers `SessionSnapshotDto`.
- [ ] Ajouter les migrations Room et leurs tests instrumentés.

### Lot 6 : coordinateur et exécution des effets

- [ ] Écrire les tests de doublon strict, conflit d'identifiant, reprise d'effet et interruption pendant `RELEASING`.
- [ ] Implémenter `SessionCoordinator` sous mutex avec persistance avant exécution des effets.
- [ ] Exécuter les effets selon leurs dépendances, puis renvoyer `ACTIVATION_SUCCEEDED`, `ACTIVATION_FAILED`, `RELEASE_SUCCEEDED` ou `RELEASE_FAILED` au moteur.
- [ ] Implémenter `SessionRuntimeStatusProbe` et `SessionReconciler`.
- [ ] Après déverrouillage, traiter Room comme source canonique. Avant déverrouillage, utiliser exclusivement le snapshot Direct Boot.
- [ ] Fusionner registre et outbox Direct Boot à `USER_UNLOCKED`, par révision et de façon idempotente.
- [ ] Ne jamais remettre un blocage déjà retiré pendant `RELEASING`.

### Lot 7 : onboarding et configuration

**Feature :** `:feature:setup`.

- [ ] Tester puis créer l'onboarding sur les limites d'arrêt forcé, NFC verrouillé, désactivation de l'accessibilité et absence de secours logiciel.
- [ ] Créer l'explication et le consentement précédant les réglages d'accessibilité.
- [ ] Implémenter l'association NFC et le remplacement confirmé du boîtier.
- [ ] Construire le sélecteur avec `ACTION_MAIN` et `CATEGORY_LAUNCHER`, sans `QUERY_ALL_PACKAGES`.
- [ ] Exclure Niumi, Home, Réglages, System UI, composeur et composants d'urgence.
- [ ] Implémenter le choix de la date et de l'heure avec `computeWakeSchedule()`.
- [ ] Créer `DeviceReadinessChecker`, une action principale à la fois et un nouveau diagnostic au retour des réglages.
- [ ] Tester les sélections 0, 1, 50 et 51, chaque contrôle de diagnostic et les textes imposés.

### Lot 8 : activation en deux phases

**Feature :** `:feature:session`.

- [ ] Tester l'ordre diagnostic, `ACTIVATION_REQUESTED`, persistance, alarme, blocage, vérification et `ACTIVATION_SUCCEEDED`.
- [ ] Implémenter `ArmSessionUseCase` dans cet ordre exact.
- [ ] En cas d'échec pendant `PREPARING`, annuler uniquement les effets de la transaction, produire `ACTIVATION_FAILED` et retirer le pointeur actif.
- [ ] Afficher la date complète, le fuseau, les applications choisies et l'engagement NFC avant confirmation.
- [ ] Refuser toute confirmation lorsque le diagnostic contient un niveau bloquant.
- [ ] Tester la reprise d'un `PREPARING` interrompu.

### Lot 9 : session active et blocage

- [ ] Créer l'écran de session active avec instant contractuel et heure locale affichée.
- [ ] Implémenter l'action de modification ou d'annulation qui demande d'abord le scan.
- [ ] Ne modifier aucune donnée avant l'état final `CANCELLED`.
- [ ] Connecter l'AccessibilityService à la projection effective des blocages.
- [ ] Lire seulement `event.packageName`, exécuter `GLOBAL_ACTION_HOME` et afficher l'overlay.
- [ ] Détecter la désactivation du service comme incident sans tenter de la contourner.
- [ ] Ajouter le journal local et son export texte expurgé.
- [ ] Tester application bloquée, autorisée, récents, notification, lien profond et recréation du service.

### Lot 10 : réveil et libération NFC

**Feature :** `:feature:ringing`.

- [ ] Remplacer les preuves debug du POC par le parseur et le vérificateur KMP.
- [ ] Faire produire `ALARM_FIRED` par `AlarmReceiver`, puis exécuter `START_RINGING`.
- [ ] Reprendre l'état depuis Direct Boot si le processus ou le service est recréé.
- [ ] Implémenter `AlarmActivity` sans bouton d'arrêt, avec les textes NFC des specs.
- [ ] Sérialiser `HandleValidNfcUseCase` avec le même mutex que le receiver et le réconciliateur.
- [ ] Depuis `ARMED` après l'heure, produire `TRIGGER_ELAPSED` avant de traiter le scan.
- [ ] Persister `RELEASING`, annuler l'alarme, arrêter son et vibration, retirer le blocage et vérifier les résultats.
- [ ] Produire `RELEASE_SUCCEEDED` uniquement lorsque tout le nettoyage requis a réussi.
- [ ] En cas d'échec, produire `RELEASE_FAILED`, conserver `RELEASING` et reprendre uniquement les effets manquants.
- [ ] Tester qu'un tag invalide ne modifie ni état, ni son, ni blocage, et qu'une fin valide prend moins d'une seconde.

### Lot 11 : redémarrage, temps et résilience

- [ ] Déclarer `SystemEventsReceiver` pour tous les broadcasts imposés.
- [ ] Reprogrammer le même `triggerAtEpochMillis` après boot, mise à jour, changement d'heure ou de fuseau.
- [ ] Jusqu'à 15 minutes de retard, programmer un déclenchement immédiat.
- [ ] Au-delà, produire `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW`, dégrader la santé et conserver le blocage.
- [ ] Restaurer une session `RINGING` ou `RELEASING` après mort du processus.
- [ ] Détecter les pertes de permission comme incidents sans passer une session armée à `FAILED`.
- [ ] Tester les parcours avant premier déverrouillage, après `USER_UNLOCKED` et en cas de snapshot corrompu.

### Lot 12 : finalisation et préparation release

- [ ] Supprimer la route et les fournisseurs debug du POC devenus inutiles.
- [ ] Vérifier qu'aucun fake, token de test, marqueur d'implémentation inachevé, bouton d'arrêt ou permission réseau n'entre dans `release`.
- [ ] Activer R8 et la suppression des ressources pour la release.
- [ ] Finaliser la divulgation d'accessibilité, la politique de confidentialité et les déclarations `USE_EXACT_ALARM`, plein écran et FGS.
- [ ] Exécuter :

```bash
./gradlew :shared:core:jvmTest
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug detekt ktlintCheck
./gradlew :app:assembleRelease
```

- [ ] Produire `docs/android/QA_MATRIX.md` et `docs/android/RELEASE_REPORT.md`.

## Recette et critères d'acceptation

- [ ] Couvrir la matrice physique de `SPEC_ANDROID.md` sur les fabricants et versions Android indiqués.
- [ ] Prouver l'alarme exacte hors ligne, écran éteint et en Doze.
- [ ] Prouver l'audio `USAGE_ALARM` audible sur Android 17 et les routes Bluetooth, USB-C ou filaires.
- [ ] Prouver la restauration avant le premier déverrouillage et après mort du processus.
- [ ] Prouver le blocage des seules applications choisies sans lecture de contenu de fenêtre.
- [ ] Prouver qu'un scan valide mène à `CANCELLED` avant l'heure et `COMPLETED` après le déclenchement, seulement après réussite de `RELEASING`.
- [ ] Prouver qu'un tag invalide ne modifie ni état, ni son, ni blocage.
- [ ] Prouver l'arrêt et le déblocage en moins d'une seconde après scan valide.
- [ ] Consigner pour chaque essai l'appareil, le firmware, la version Android, les permissions, le retard mesuré, le résultat et les événements locaux.

## Hypothèses et limites

- Le boîtier du MVP contient un tag NDEF Type 2 compatible NFC-A, déjà écrit avec le payload canonique.
- L'écriture industrielle, le verrouillage du tag et la protection anti-clonage restent hors périmètre.
- Le compte Play Console, les appareils physiques et les actions manuelles dans les réglages restent sous la responsabilité de l'utilisateur.
- Le plan construit le framework iOS KMP pour garantir l'interopérabilité, mais ne développe pas l'application iOS.
- Aucune publication, aucun commit et aucun push ne sont exécutés automatiquement.
