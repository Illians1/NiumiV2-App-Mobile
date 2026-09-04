# Plan d'implémentation du MVP Android Niumi

> **Pour l'agent chargé de l'exécution :** utiliser `superpowers:executing-plans` (une étape par session) ou `superpowers:subagent-driven-development`. Les cases `- [ ]` servent au suivi. S'arrêter aux portes de validation manuelle. Ne jamais commit ni push sans demande explicite, jamais de ligne `Co-Authored-By`.

**Objectif :** livrer le MVP Android décrit par les specs : réveil exact, blocage comportemental des applications choisies, fin ou annulation de session uniquement par scan du boîtier NFC associé.

**Architecture :** `:shared:core` (Kotlin Multiplatform) est l'unique autorité des transitions métier. Les composants Android convertissent les faits système en événements KMP, persistent atomiquement chaque décision (snapshot, reçu, outbox) puis exécutent les effets retournés. Aucun composable, receiver, service ou ViewModel n'écrit directement un `SessionState`.

**Stack :** Kotlin, Jetpack Compose Material 3, Hilt, Coroutines et Flow, Room, DataStore, Navigation Compose, Kotlin Multiplatform, `kotlinx-datetime`, `kotlinx-serialization`, JUnit 4, Truth, Turbine, Android Lint, ktlint, detekt.

**Specs :** `CLAUDE.md`, `specs/SPEC_CORE_KMP.md` (contrat commun, prioritaire), `specs/SPEC_ANDROID.md` (plateforme), `docs/OBJECTIF_ET_INTERET_PRODUIT.md` (intention produit). Le plan renvoie aux sections des specs au lieu de les recopier : l'exécutant lit les sections citées avant chaque étape.

## Comment utiliser ce plan

- Une étape = une session. Avant de coder : lire `CLAUDE.md`, les sections de spec listées dans l'étape, la section « Interfaces transverses » ci-dessous et le rapport de l'étape précédente.
- Ne pas anticiper une étape suivante. Si une étape révèle une contradiction ou une impossibilité, l'écrire dans le rapport et s'arrêter plutôt que de contourner.
- Chaque étape suit le cycle : écrire le test qui échoue, vérifier l'échec, implémenter le minimum, vérifier le succès, lancer les vérifications de l'étape.
- À la fin de chaque étape, rédiger `docs/android/implementation-reports/ETAPE-NN.md` : fichiers modifiés, commandes exécutées avec leur résultat, tests non exécutés, incertitudes, validations sur appareil restantes.
- Les portes de validation (après l'étape 6 et après l'étape 21) sont manuelles, sur appareils réels. Ne pas franchir une porte sans validation humaine explicite.
- Les versions de bibliothèques ci-dessous ont été vérifiées le 3 septembre 2026. Les reconfirmer à l'étape 1 avec Context7 ou les sources officielles avant de les figer.

## Contraintes globales

Valeurs copiées des specs ; chaque étape les respecte implicitement.

- Modules Gradle : `:app`, `:shared:core`, `:core:database`, `:core:system`, `:core:designsystem`, `:feature:setup`, `:feature:session`, `:feature:ringing` (SPEC_ANDROID §6). Chemins physiques : `shared/core`, `androidApp/app`, `androidApp/core/database`, `androidApp/core/system`, `androidApp/core/designsystem`, `androidApp/feature/setup`, `androidApp/feature/session`, `androidApp/feature/ringing` (SPEC_CORE_KMP §15). Aucun module supplémentaire. `:core:designsystem` ajouté à l'étape 3 (huit modules au lieu de sept prévus initialement) : `:shared:core` ne dépend d'aucun module, les `feature` et `:core:database`/`:core:system` ne peuvent pas dépendre de `:app` (règle de dépendance §6), mais `AlarmActivity` (`:feature:ringing`) a besoin du thème Niumi pour son propre `setContent` ; un module de thème partagé sans dépendance résout ce besoin sans dépendance inverse.
- `minSdk 29`, `targetSdk 36`, `compileSdk 37`, JDK 17 (SPEC_ANDROID §5). Namespace KMP `com.niumi.core` (SPEC_CORE_KMP §16). `applicationId` et namespaces Android : `com.niumi.app`, `com.niumi.database`, `com.niumi.system`, `com.niumi.feature.setup`, `com.niumi.feature.session`, `com.niumi.feature.ringing` (choix du plan, non fixé par les specs).
- Versions centralisées dans `gradle/libs.versions.toml`, sans `+` (SPEC_ANDROID §5).
- `:shared:core` n'importe aucune API Android ou Apple ; façade sans `Flow`, sans `suspend`, sans exception traversant la frontière, sans horloge globale (SPEC_CORE_KMP §3.2 et §14).
- Dépendances : `feature:*`, `core:database`, `core:system` → `:shared:core` ; `core:system` → `core:database` (décision du plan : le coordinateur vit dans `core:system`) ; `feature:*` → `core:*` ; `:app` → tout. Aucune dépendance inverse.
- Manifeste : exactement les permissions de SPEC_ANDROID §14. `USE_EXACT_ALARM` déclaré, `SCHEDULE_EXACT_ALARM` interdit, `QUERY_ALL_PACKAGES` interdit, aucune permission `INTERNET`.
- Aucune action d'arrêt (bouton, action de notification, intent, binding) dans le parcours de sonnerie (SPEC_ANDROID §3, §10.2, §10.4).
- Une seule API de réveil : `AlarmManager.setAlarmClock()` avec `PendingIntent` explicites et `FLAG_IMMUTABLE` (SPEC_ANDROID §9.1).
- Journal technique : 200 événements maximum, types de SPEC_ANDROID §17 uniquement, jamais de token NFC, de hash complet, de texte d'accessibilité ni de contenu d'une autre application (§16, §17).
- Textes UI : tutoiement, textes imposés par SPEC_ANDROID §10.3, §10.4, §10.5, §12.2, §12.3 et §13 repris mot pour mot.
- Aucun mock, fake ou raccourci hors des tests et de la variante `debug` (CLAUDE.md). La route POC vit dans `androidApp/app/src/debug` et est supprimée à l'étape 21.
- Les tests injectent `Clock`, `AlarmScheduler`, `AlarmAudioEngine`, `NfcVerifier` et `ForegroundAppSource` ; aucun test n'attend une vraie heure de réveil (SPEC_ANDROID §19.2).

## Versions de référence

| Composant | Version | Note |
| --- | --- | --- |
| Kotlin / KMP | 2.4.10 | Compatibilité officielle : Gradle 7.6.3–9.5.0, AGP 8.5.2–9.1.0, Xcode 26.4 |
| AGP | 9.1.1 | Exigé par Compose BOM 2026.08 pour compileSdk 37 ; JDK 17 ; Kotlin intégré (ne pas appliquer `org.jetbrains.kotlin.android`). Un patch au-dessus du maximum testé par KGP 2.4.10 (9.1.0) — inévitable pour compileSdk 37, reconfirmé à l'étape 1 |
| Gradle | 9.5.0 | Maximum testé par KGP 2.4.10 (reconfirmé à l'étape 1 ; 9.3.1 n'était que le minimum d'AGP 9.1.1) |
| Compose BOM | 2026.08.00 | Compose 1.12 |
| Navigation Compose | 2.10.0 | |
| Room | 2.8.4 | KSP2 |
| DataStore | 1.2.1 | `createInDeviceProtectedStorage()` disponible |
| Hilt / androidx.hilt | 2.60.1 / 1.4.0 | Reconfirmé à l'étape 1 |
| KSP | 2.3.11 | Reconfirmé à l'étape 1 ; versionnage découplé de Kotlin depuis KSP 2.3.0 |
| kotlinx-datetime | 0.8.0 | `Instant` et `Clock` viennent de `kotlin.time` |
| kotlinx-serialization | 1.11.0 | |
| ktlint | plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, CLI 1.8.0 épinglé | Le plugin embarque 1.5.0 par défaut ; épinglé pour éviter la dérive entre patchs |
| detekt | `dev.detekt` 2.0.0-alpha.6, épinglé, bloquant | Seule variante construite contre Kotlin 2.4.10 ; la dernière stable (1.23.8) embarque Kotlin 2.0.21 et échoue sur Kotlin 2.4+. Voir SPEC_ANDROID §5 et `ETAPE-01.md` |

AGP 9.1.1 dépasse d'un patch la borne testée par KMP 2.4.10 (9.1.0). Si le build KMP échoue pour cette raison, arrêter et proposer une mise à jour explicite des specs ; ne jamais réduire `compileSdk`.

## Points de vigilance sur les specs

Signalés ici pour que l'exécutant ne les découvre pas en cours de route.

1. **Ordre Lot 0 / Lot 0.5.** SPEC_ANDROID §22 place le POC natif avant KMP, mais SPEC_CORE_KMP §9.3 interdit à un lecteur natif de décider de la validité d'un payload. Décision validée : l'étape 2 livre le parseur et le vérificateur NFC KMP avant le POC, qui les consomme. Le reste du moteur reste après la porte 0. L'étape 2 met à jour SPEC_ANDROID §22 en conséquence.
2. **Snippet Gradle KMP.** Le bloc `kotlin { android { ... } }` de SPEC_CORE_KMP §16 est correct pour AGP ≥ 8.12. Les tests de la cible Android KMP sont désactivés par défaut ; c'est voulu : `commonTest` s'exécute via la cible `jvm()` (`:shared:core:jvmTest`).
3. **Cibles iOS.** `iosArm64()` et `iosSimulatorArm64()` exigent Xcode 26.4. Si Xcode est absent du poste, la tâche `linkDebugFrameworkIosSimulatorArm64` échoue : le signaler dans le rapport, ne pas retirer les cibles.
4. **`NfcVerificationProof`.** Constructeur `internal`, non sérialisable, transmise en mémoire dans `SessionEventDto`. Sur Android, elle ne traverse jamais un `Intent`, un `Bundle` ou Room.
5. **`effectId` déterministe.** Formule retenue : `"$sessionId:$revision:$kind:$ordinal"` (SPEC_CORE_KMP §6 ne fixe pas la forme exacte).
6. **Reçus et outbox.** SPEC_ANDROID §7.2 décrit `SessionEventReceiptEntity` et `SessionEffectOutboxEntity` en prose ; l'étape 9 fixe leurs colonnes. Empreinte canonique du payload : SHA-256 hexadécimal de la sérialisation JSON stable de l'événement sans `eventId` ni `nfcProof`.
7. **`AWAITING_NFC` sur Android.** Inatteignable en parcours normal (§7.1). L'écran et la notification restent implémentés de façon défensive, testés par injection d'événement.
8. **Acceptation Google Play de l'AccessibilityService.** Risque produit bloquant (SPEC_ANDROID §12.3, §23). Traité à la porte 0, jamais présumé acquis.
9. **`kotlinx-datetime` 0.8.** Les types `Instant` et `Clock` sont dans `kotlin.time` ; les tests reçoivent `nowEpochMillis` explicitement (SPEC_CORE_KMP §8.2).

## Interfaces transverses

Contrats internes Android absents des specs. Ils sont définis une fois ici ; les étapes les créent, puis les consomment sans les renommer. Tous les adaptateurs sont idempotents et renvoient un résultat typé, jamais une exception.

```kotlin
// :core:system — com.niumi.system.common
interface Clock { fun nowEpochMillis(): Long }
interface IdGenerator { fun newId(): String }          // UUID v4 canonique minuscule

sealed interface OperationResult {
    data object Success : OperationResult
    data object AlreadySatisfied : OperationResult     // précondition déjà atteinte (SPEC_CORE_KMP §6, règle d'échappement)
    data class Failure(val code: String, val cause: Throwable? = null) : OperationResult
}

// :core:system — com.niumi.system.alarm
interface AlarmScheduler {
    fun schedule(sessionId: String, revision: Long, triggerAtEpochMillis: Long): OperationResult
    fun cancel(sessionId: String): OperationResult
    fun isScheduled(sessionId: String): Boolean        // PendingIntent.getBroadcast(..., FLAG_NO_CREATE) != null
}

// :core:system — com.niumi.system.audio
interface AlarmAudioEngine {
    fun start(ringtoneKey: String, vibrationEnabled: Boolean): OperationResult
    fun stop(): OperationResult
    val isPlaying: Boolean
}

// :core:system — com.niumi.system.ringing
interface RingingController {                          // démarre/arrête AlarmRingingService
    fun startRinging(sessionId: String, revision: Long): OperationResult
    fun stopRinging(sessionId: String): OperationResult
}

// :core:system — com.niumi.system.blocking
interface BlockingController {
    fun apply(sessionId: String, packageNames: Set<String>): OperationResult
    fun remove(sessionId: String): OperationResult
    fun effectivePackages(): Set<String>
    fun isServiceEnabled(): Boolean
}
interface ForegroundAppSource { val foregroundPackage: Flow<String> }

// :core:system — com.niumi.system.nfc
interface NfcReader {                                   // Reader Mode, livre l'URI brute du 1er enregistrement NDEF URI
    fun start(activity: Activity, onUri: (String) -> Unit): OperationResult
    fun stop(activity: Activity)
    val availability: NfcAvailability                   // ABSENT, DISABLED, ENABLED
}
interface NfcScanHandler { suspend fun onUriRead(uri: String): ScanOutcome }
sealed interface ScanOutcome { data object Accepted : ScanOutcome; data object UnknownBox : ScanOutcome; data object Unreadable : ScanOutcome; data object Ignored : ScanOutcome }

// :core:system — com.niumi.system.notification
interface ScanRequestNotifier { fun present(sessionId: String): OperationResult; fun clear(sessionId: String): OperationResult }

// :core:system — com.niumi.system.readiness
data class ReadinessCheck(val id: ReadinessCheckId, val severity: ReadinessSeverityDto, val passed: Boolean, val action: ReadinessAction?)
interface DeviceReadinessChecker { fun check(): List<ReadinessCheck> }

// :core:database — com.niumi.database
data class AndroidSessionExtras(val boxId: String, val boxTokenSha256Hex: String, val ringtoneKey: String, val vibrationEnabled: Boolean, val blockedPackages: List<BlockedPackage>)
data class BlockedPackage(val packageName: String, val displayNameSnapshot: String)
data class EventReceipt(val eventId: String, val sessionId: String, val payloadSha256Hex: String, val appliedRevision: Long, val receivedAtEpochMillis: Long)
enum class EffectStatus { PENDING, SUCCEEDED, FAILED, SATISFIED }
data class PendingEffect(val effectId: String, val sessionId: String, val revision: Long, val kind: SessionEffectKindDto, val ordinal: Int, val payloadJson: String?, val status: EffectStatus, val lastError: String?)
data class StoredSession(val snapshot: SessionSnapshotDto, val extras: AndroidSessionExtras, val pendingEffects: List<PendingEffect>)
data class StoredDecision(val snapshot: SessionSnapshotDto, val receipt: EventReceipt, val effects: List<PendingEffect>, val androidExtras: AndroidSessionExtras)
interface SessionStore {
    suspend fun activeSession(): StoredSession?
    suspend fun commitDecision(decision: StoredDecision)             // une seule transaction Room
    suspend fun findReceipt(eventId: String): EventReceipt?
    suspend fun pendingEffects(sessionId: String): List<PendingEffect>
    suspend fun markEffect(effectId: String, status: EffectStatus, error: String?)
    suspend fun clearActivePointer(sessionId: String)
}
interface DirectBootStore {
    fun read(): DirectBootSnapshot?                                   // null si absent ; DirectBootSnapshot.Corrupted si illisible
    fun write(snapshot: DirectBootSnapshot): OperationResult        // refuse domainRevision inférieure
    fun clear()
}

// :core:system — com.niumi.system.session
interface SessionCoordinator {
    suspend fun dispatch(event: SessionEventDto): DispatchResult
    suspend fun reconcile(reason: ReconcileReason): ReconcileResult
}
sealed interface DispatchResult {
    data class Applied(val snapshot: SessionSnapshotDto?, val requiredEffectsSucceeded: Boolean) : DispatchResult
    data class Duplicate(val receipt: EventReceipt) : DispatchResult
    data class Rejected(val violations: List<DomainViolationDto>) : DispatchResult
}
enum class ReconcileReason { PROCESS_START, USER_UNLOCKED, LOCKED_BOOT, BOOT, PACKAGE_REPLACED, TIME_CHANGED, TIMEZONE_CHANGED, BEFORE_SCAN, SERVICE_RECREATED }
```

`SessionCoordinator` sérialise `dispatch()` et `reconcile()` sous un unique `Mutex`. Il persiste (snapshot, reçu, effets) avant d'exécuter le moindre effet, puis renvoie `ACTIVATION_SUCCEEDED`/`ACTIVATION_FAILED` ou `RELEASE_SUCCEEDED`/`RELEASE_FAILED` au moteur selon le résultat des effets requis (SPEC_CORE_KMP §6). Avant `UserManager.isUserUnlocked`, il travaille exclusivement sur `DirectBootStore` ; après, Room fait foi et Direct Boot reçoit une copie à chaque décision.

---

## Phase A — Fondations

### Étape 1 : bootstrap du monorepo Gradle

**Specs à lire :** SPEC_CORE_KMP §15, §16 ; SPEC_ANDROID §5, §6, §14.

**Fichiers :**
- Créer : `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.3.1), `.editorconfig`, `config/detekt/detekt.yml`.
- Créer : `shared/core/build.gradle.kts` (snippet SPEC_CORE_KMP §16, plus `withHostTestBuilder` non requis).
- Créer : `androidApp/app/build.gradle.kts`, `androidApp/app/src/main/AndroidManifest.xml` (permissions §14, `<uses-feature android.hardware.nfc required=true>`, `MainActivity` exportée launcher uniquement), `androidApp/app/src/main/kotlin/com/niumi/app/NiumiApplication.kt` (`@HiltAndroidApp`), `MainActivity.kt` (`enableEdgeToEdge()`, écran d'accueil « Aucune session »), `ui/theme/NiumiTheme.kt` (Material 3, `dynamicColor = false`, palette sombre lisible).
- Créer : `androidApp/core/database/build.gradle.kts`, `androidApp/core/system/build.gradle.kts`, `androidApp/feature/setup/build.gradle.kts`, `androidApp/feature/session/build.gradle.kts`, `androidApp/feature/ringing/build.gradle.kts`, chacun avec un manifeste minimal et un fichier Kotlin vide de package.
- Créer : `shared/core/src/commonTest/kotlin/com/niumi/core/SmokeTest.kt`, `androidApp/app/src/test/kotlin/com/niumi/app/SmokeTest.kt`.

**Produit :** un build vert de tous les modules, la catalogue de versions, les plugins qualité, une application qui affiche un accueil vide.

- [x] **Reconfirmer les versions** de la table « Versions de référence » via Context7 ou les pages officielles ; corriger la table dans ce plan si une version a changé. *(Fait le 2026-09-04 contre les métadonnées Maven/Gradle Plugin Portal réelles ; table corrigée — voir `ETAPE-01.md`.)*
- [x] **Écrire `settings.gradle.kts`** : `pluginManagement` (google, mavenCentral, gradlePluginPortal), `dependencyResolutionManagement` avec `FAIL_ON_PROJECT_REPOS`, `rootProject.name = "niumi-mobile"`, `include(":app", ":shared:core", ":core:database", ":core:system", ":feature:setup", ":feature:session", ":feature:ringing")` et `project(":app").projectDir = file("androidApp/app")` pour chaque module Android (`shared/core` conserve son chemin naturel). *(Les modules intermédiaires `:core` et `:feature` ont aussi dû être remappés — voir `ETAPE-01.md`.)*
- [x] **Écrire `gradle.properties`** : `org.gradle.jvmargs=-Xmx4g`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `android.useAndroidX=true`, `kotlin.code.style=official`. Ne pas ajouter `android.builtInKotlin` (déjà `true` par défaut en AGP 9).
- [x] **Écrire `gradle/libs.versions.toml`** : sections `[versions]`, `[libraries]`, `[plugins]` avec `android-application`, `android-kotlin-multiplatform-library`, `kotlin-multiplatform`, `kotlin-serialization`, `kotlin-compose` (`org.jetbrains.kotlin.plugin.compose`), `ksp`, `hilt`, `room`, `ktlint`, `detekt`, et les bibliothèques : Compose BOM, material3, activity-compose, navigation-compose, lifecycle-viewmodel-compose, hilt-android, hilt-compiler, hilt-navigation-compose, room-runtime, room-ktx, room-compiler, datastore-preferences, kotlinx-datetime, kotlinx-serialization-json, kotlinx-coroutines-android/test, junit4, truth, turbine, androidx-test (core, runner, rules, junit, espresso-core), compose-ui-test-junit4.
- [x] **Écrire `shared/core/build.gradle.kts`** en reprenant SPEC_CORE_KMP §16 mot pour mot (`android { namespace = "com.niumi.core"; compileSdk = 37; minSdk = 29 }`, `jvm()`, `iosArm64()`, `iosSimulatorArm64()`, framework statique `NiumiCore`, dépendances `kotlinx-datetime`, `kotlinx-serialization-json`, `kotlin("test")`).
- [x] **Écrire les modules Android** : `:app` avec `com.android.application` + `kotlin-compose` + `hilt` + `ksp`, `applicationId "com.niumi.app"`, `minSdk 29`, `targetSdk 36`, `compileSdk 37`, `buildFeatures.compose = true`, `release { isMinifyEnabled = true; isShrinkResources = true }` ; les autres modules avec `com.android.library` (+ `kotlin-compose` pour les `feature`, + `ksp`/`room` pour `core:database`, + `hilt` partout sauf `shared:core`). Ne pas appliquer `org.jetbrains.kotlin.android`.
- [x] **Déclarer les dépendances entre modules** conformément à « Contraintes globales ». Ajouter dans `:app` un test unitaire qui parcourt `settings.gradle.kts` et échoue si un module hors liste apparaît (`ModuleListTest`).
- [x] **Configurer ktlint, detekt et Lint** : appliquer les plugins à la racine, `detekt.yml` basé sur la config par défaut avec `MaxLineLength 120`, Lint `abortOnError = true`, `warningsAsErrors = true` sur `:app`. *(detekt en `dev.detekt` 2.0.0-alpha.6, seule variante compatible Kotlin 2.4.10 — voir « Versions de référence » et SPEC_ANDROID §5.)*
- [x] **Écrire les deux `SmokeTest`** (`assertTrue(true)` n'est pas accepté : tester `NiumiCoreVersion.SCHEMA_VERSION == 1` dans `commonTest` après avoir créé `object NiumiCoreVersion { const val SCHEMA_VERSION = 1 }` dans `com.niumi.core.domain` ; côté `:app`, tester que `NiumiApplication` est annotée `@HiltAndroidApp` par réflexion). *(Correction appliquée : le signal fiable est la classe générée `Hilt_NiumiApplication`, pas la seule présence de l'annotation — voir `ETAPE-01.md`.)*
- [x] **Vérifier :**

```bash
./gradlew projects
./gradlew :shared:core:jvmTest
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64   # exige Xcode 26.4 ; sinon consigner l'échec
```

**Terminé quand :** les cinq commandes passent (ou la dernière est consignée comme non exécutable faute de Xcode), aucun avertissement de dépréciation Gradle nouveau, rapport `ETAPE-01.md` rédigé. *(Fait le 2026-09-04 — toutes les commandes passent, Xcode 26.6 disponible ; détails et difficultés résolues dans `docs/android/implementation-reports/ETAPE-01.md`.)*

### Étape 2 : KMP — protocole NFC (parseur, credential, vérificateur, preuve) et façade partielle

**Specs à lire :** SPEC_CORE_KMP §6 (bloc `NfcVerificationProof` et `NfcVerificationContext`), §9, §14, §17 (section NFC) ; SPEC_ANDROID §11.1, §11.2, §22 (Lot 0 et 0.5).

**Fichiers :**
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/nfc/` : `BoxPayload.kt`, `BoxPayloadParser.kt`, `BoxPayloadResult.kt`, `PairedBoxCredential.kt`, `NfcVerificationContext.kt`, `NfcVerificationProof.kt`, `BoxVerifier.kt`, `BoxVerificationResult.kt`, `Sha256.kt`, `ConstantTime.kt`.
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/interop/` : `NiumiCoreFacade.kt` (méthodes `parseBoxPayload` et `verifyBox` uniquement à cette étape), `NfcDtos.kt`.
- Créer les tests dans `shared/core/src/commonTest/kotlin/com/niumi/core/nfc/` : `BoxPayloadParserTest.kt`, `Sha256Test.kt`, `ConstantTimeTest.kt`, `BoxVerifierTest.kt`, `NfcVerificationProofTest.kt`, `BoxPayloadFuzzTest.kt`.
- Créer `shared/core/src/commonTest/resources/fixtures/nfc_payloads.json` et `shared/core/src/jvmTest/kotlin/com/niumi/core/nfc/NfcFixturesTest.kt` (lit le JSON par classloader et rejoue chaque cas).
- Modifier `specs/SPEC_ANDROID.md` §22 : dans « Lot 0 », remplacer « arrêt du service après scan associé » par « arrêt du service après scan associé, validé par le parseur et le vérificateur de `:shared:core` (livrés avant le POC) » et ajouter au Lot 0.5 la mention que le protocole NFC a été livré en amont.

**Interfaces produites (consommées par les étapes 4, 13, 18) :**

```kotlin
// com.niumi.core.nfc
data class BoxPayload(val protocolVersion: Int, val boxId: String, val tokenBytes: ByteArray)
enum class BoxPayloadStatus { VALID, UNSUPPORTED_SCHEME, UNSUPPORTED_HOST, UNSUPPORTED_VERSION, INVALID_BOX_ID, MISSING_TOKEN, INVALID_TOKEN, UNEXPECTED_COMPONENT, PAYLOAD_TOO_LONG, MALFORMED_URI }
data class BoxPayloadResult(val status: BoxPayloadStatus, val payload: BoxPayload?)
object BoxPayloadParser { fun parse(uri: String): BoxPayloadResult }
data class PairedBoxCredential(val protocolVersion: Int, val boxId: String, val tokenSha256Hex: String) {
    companion object { fun fromPayload(payload: BoxPayload): PairedBoxCredential }
}
class NfcVerificationProof internal constructor(val boxId: String, val sessionId: String, val eventId: String, val expectedRevision: Long, val verifiedAtEpochMillis: Long)
data class NfcVerificationContext(val sessionId: String, val eventId: String, val expectedRevision: Long, val occurredAtEpochMillis: Long)
enum class BoxVerificationStatus { MATCH, BOX_MISMATCH, TOKEN_MISMATCH, UNSUPPORTED_VERSION }
data class BoxVerificationResult(val status: BoxVerificationStatus, val proof: NfcVerificationProof?)
object BoxVerifier { fun verify(payload: BoxPayload, credential: PairedBoxCredential, context: NfcVerificationContext?): BoxVerificationResult }
object Sha256 { fun hash(bytes: ByteArray): ByteArray; fun hexOf(bytes: ByteArray): String }
object ConstantTime { fun equals(a: ByteArray, b: ByteArray): Boolean }

// com.niumi.core.interop (DTO = data class ou enum sans type plateforme)
class NiumiCoreFacade {
    fun parseBoxPayload(uri: String): BoxPayloadResultDto
    fun verifyBox(payload: BoxPayloadDto, credential: PairedBoxCredentialDto, context: NfcVerificationContextDto?): BoxVerificationResultDto
}
```

- [x] **Écrire `Sha256Test`** avec les vecteurs officiels : `""` → `e3b0c442…b855`, `"abc"` → `ba7816bf…f20015ad`, et un message de 1 000 000 de `a` → `cdc76e5c…c7112cd0`. Vérifier l'échec, implémenter SHA-256 en Kotlin pur (aucune API JVM : la même implémentation sert iOS), vérifier le succès.
- [x] **Écrire `ConstantTimeTest`** : égalité, inégalité, longueurs différentes → `false` sans court-circuit (implémentation par OU cumulatif des XOR sur la longueur maximale). Implémenter.
- [x] **Écrire `BoxPayloadParserTest`** avec un cas par contrainte de SPEC_CORE_KMP §9.1 et §17 : payload canonique → `VALID` avec `boxId` et 16 octets ; `NIUMI://` ou `Niumi://` → `UNSUPPORTED_SCHEME` ; hôte `Box` → `UNSUPPORTED_HOST` ; `/v2/` → `UNSUPPORTED_VERSION` ; UUID en majuscules, sans tirets, de 35 caractères → `INVALID_BOX_ID` ; query vide → `MISSING_TOKEN` ; token dupliqué (`token=a&token=b`) → `UNEXPECTED_COMPONENT` ; token de 21 ou 23 caractères, avec `=`, avec `+` ou `/`, avec dernier caractère à bits de bourrage non nuls → `INVALID_TOKEN` ; `#frag`, `user@`, `:443`, segment `/x`, paramètre `&a=b` → `UNEXPECTED_COMPONENT` ; `%2F` → `UNEXPECTED_COMPONENT` ; 97 octets UTF-8 → `PAYLOAD_TOO_LONG` (test avant toute autre validation) ; chaîne vide, un octet nul `U+0000`, caractère de contrôle, `://` seul → `MALFORMED_URI`.
- [x] **Implémenter `BoxPayloadParser`** sans `java.net.URI` ni regex permissive : découpage manuel sur `://`, `/`, `?`, `&`, `=` ; décodage Base64 URL strict avec `kotlin.io.encoding.Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)` puis contrôle explicite que le dernier caractère appartient à `AQgw` (bits de bourrage nuls). *(Correction : un token de 22 caractères porte 4 bits de bourrage sur le dernier caractère, pas 2 — seules les valeurs 0/16/32/48 de l'alphabet Base64 URL, soit `A`/`Q`/`g`/`w`, ont ces 4 bits nuls ; le jeu `AEIMQUYcgkosw048` initialement listé ici correspondait à 2 bits de bourrage et aurait accepté plusieurs encodages distincts pour un même token de 16 octets. Voir `ETAPE-02.md`.)*
- [x] **Écrire `BoxVerifierTest`** : même `boxId` et même hash → `MATCH` ; `boxId` différent → `BOX_MISMATCH` ; hash différent → `TOKEN_MISMATCH` ; `protocolVersion` 2 → `UNSUPPORTED_VERSION` ; avec `context` non nul, `MATCH` porte une preuve dont les cinq champs reprennent le contexte ; avec `context` nul, `proof == null` ; un mismatch ne porte jamais de preuve. Implémenter avec `ConstantTime.equals` sur les octets du hash.
- [x] **Écrire `NfcVerificationProofTest`** : le constructeur n'est pas accessible depuis un autre module (test de compilation par `internal` documenté dans le test), `toString()` ne contient ni `boxId` complet ni hash (masquer aux 8 premiers caractères).
- [x] **Écrire `BoxPayloadFuzzTest`** : 200 chaînes générées avec un `Random(seed = 42)` mêlant caractères de contrôle, surrogates isolés, longueurs 0 à 200 ; aucun cas ne lance d'exception, aucun ne retourne `VALID`.
- [x] **Écrire `nfc_payloads.json`** (liste `{ "uri": ..., "expected": "STATUS" }`, 20 cas couvrant chaque statut) et `NfcFixturesTest` dans `jvmTest`.
- [x] **Écrire la façade partielle et ses DTO** ; un test `NiumiCoreFacadeNfcTest` vérifie que `verifyBox` avec contexte retourne un `BoxVerificationResultDto` dont `proof` est l'objet opaque et que sans contexte `proof` est nul.
- [x] **Mettre à jour `specs/SPEC_ANDROID.md` §22** comme indiqué dans « Fichiers ».
- [x] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64
./gradlew ktlintCheck detekt
```

**Terminé quand :** tous les tests passent, aucun log ne contient de token, la spec §22 est ajustée, rapport `ETAPE-02.md` rédigé. *(Fait le 2026-09-04 — 53 tests verts, ktlint et detekt verts sur tout le dépôt (y compris `:shared:core`, voir la correction de câblage detekt dans `ETAPE-02.md`) ; détails dans `docs/android/implementation-reports/ETAPE-02.md`.)*

## Phase B — Lot 0 : POC système dans les modules définitifs

Chaque composant est créé à son emplacement final. Seule la route de pilotage du POC vit dans `androidApp/app/src/debug`.

### Étape 3 : alarme exacte, receiver, service de sonnerie, audio et écran de réveil

**Specs à lire :** SPEC_ANDROID §4, §9.1, §10.1 à §10.4, §14, §16.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/` : `common/Clock.kt` (`SystemClock` impl), `common/IdGenerator.kt`, `common/OperationResult.kt`, `alarm/AlarmScheduler.kt`, `alarm/AlarmPendingIntents.kt`, `alarm/AndroidAlarmScheduler.kt`, `audio/AlarmAudioEngine.kt`, `audio/MediaPlayerAlarmAudioEngine.kt`, `notification/NiumiNotificationChannels.kt`, `notification/RingingNotificationFactory.kt`, `ringing/RingingController.kt`, `di/SystemModule.kt`.
- Créer dans `androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/` : `AlarmReceiver.kt`, `AlarmRingingService.kt`, `AndroidRingingController.kt`, `AlarmActivity.kt`, `ui/AlarmScreen.kt`, `ui/AlarmScreenState.kt`, `ServiceCommand.kt` (extras validés : `sessionId`, `revision`).
- Créer `androidApp/feature/ringing/src/main/res/raw/niumi_alarm.wav` généré par `tools/generate_alarm_wav.py` (mono, PCM 16 bits, 44,1 kHz, 6 s : six motifs de 750 ms de mélange 740 Hz + 988 Hz suivis de 250 ms de silence, fondus de 10 ms). Le script est versionné ; aucun fichier audio tiers.
- Créer dans `androidApp/app/src/debug/kotlin/com/niumi/app/poc/` : `PocScreen.kt`, `PocViewModel.kt`, `PocNavigation.kt` (route `poc` ajoutée au NavHost uniquement en debug via un `Set<NavGraphContributor>` Hilt multibinding déclaré dans `:app`).
- Manifeste `:feature:ringing` : `AlarmReceiver` (`exported=false`, `directBootAware=true`), `AlarmRingingService` (`exported=false`, `directBootAware=true`, `foregroundServiceType="mediaPlayback"`), `AlarmActivity` (`exported=false`, `directBootAware=true`, `showWhenLocked`, `turnScreenOn`, `launchMode="singleTask"`, `excludeFromRecents="true"`).
- Tests : `androidApp/core/system/src/test/.../alarm/AlarmPendingIntentsTest.kt` (Robolectric n'est pas dans la stack : tester la construction des `Intent` et des codes de requête avec des fakes purs), `MediaPlayerAlarmAudioEngineTest.kt` (via une interface `MediaPlayerFactory` injectée), `RingingNotificationFactoryTest.kt` ; `androidApp/feature/ringing/src/androidTest/.../AlarmReceiverInstrumentedTest.kt`, `AlarmRingingServiceInstrumentedTest.kt` (`ServiceTestRule`), `NiumiAlarmWavTest.kt` (en-tête WAV : 1 canal, 16 bits, 44 100 Hz, durée 6 s ± 50 ms).

**Produit :** `AlarmScheduler`, `AlarmAudioEngine`, `RingingController`, `AlarmReceiver`, `AlarmRingingService`, `AlarmActivity` conformes aux specs, pilotables depuis la route debug. À cette étape le receiver démarre le service directement ; l'étape 17 insère le coordinateur entre les deux.

- [x] **Écrire `AlarmPendingIntentsTest`** : `alarmPendingIntent(sessionId)` cible explicitement `AlarmReceiver`, porte `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`, un code de requête égal à `sessionId.hashCode()` stable entre deux appels ; `showPendingIntent(sessionId)` cible `MainActivity` ; `fullScreenPendingIntent(sessionId)` cible `AlarmActivity`. Implémenter. *(Renommé `AlarmPendingIntentSpecsTest` : décision « descripteurs purs » validée avant l'implémentation — `Intent`/`PendingIntent` sont des stubs en JVM sans Robolectric, donc `AlarmPendingIntentSpecs` construit un `PendingIntentSpec` pur, testé en JVM ; `AndroidPendingIntentFactory` traduit vers un vrai `PendingIntent`, non testable en JVM. Voir `ETAPE-03.md`.)*
- [x] **Implémenter `AndroidAlarmScheduler`** : `schedule()` appelle `setAlarmClock(AlarmClockInfo(triggerAtEpochMillis, showPendingIntent), alarmPendingIntent)` ; `cancel()` annule l'alarme et `cancel()` le `PendingIntent` ; `isScheduled()` via `FLAG_NO_CREATE`. Aucun `WorkManager`, `Handler` ni `setInexactRepeating`.
- [x] **Générer `niumi_alarm.wav`** avec le script, écrire `NiumiAlarmWavTest`. *(Écrit en JVM plutôt qu'instrumenté : contrôle statique d'en-tête WAV sans dépendance au runtime Android. Voir `ETAPE-03.md`.)*
- [x] **Écrire `MediaPlayerAlarmAudioEngineTest`** (fake `MediaPlayerFactory`) : `start()` configure `USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION`, demande le focus audio avec les mêmes attributs, met `isLooping = true`, démarre la vibration répétée si demandé ; `start()` deux fois est idempotent ; `stop()` libère lecteur, focus et vibration ; une exception du lecteur retourne `Failure("ANDROID_AUDIO_START_FAILED")` sans propager. Implémenter. *(Renommé `DefaultAlarmAudioEngineTest` / `DefaultAlarmAudioEngine` : la classe orchestre `AlarmPlayerFactory`/`AudioFocusController`/`VibrationController` injectés plutôt que de construire un `MediaPlayer` elle-même — même décision « descripteurs purs ». `MediaPlayerAlarmPlayerFactory` porte la traduction Android réelle, non testée en JVM.)*
- [x] **Écrire `RingingNotificationFactoryTest`** : canal `niumi_alarm_ringing` (importance haute, son nul, visibilité publique), notification `CATEGORY_ALARM`, texte « Alarme Niumi en cours », sous-texte « Scanne ton boîtier pour terminer la session. », `ongoing`, `fullScreenIntent(…, true)`, zéro action. Implémenter `NiumiNotificationChannels` (crée aussi `niumi_session_awaiting_scan` dès maintenant, spec §10.5) et la factory. *(Décomposé en `RingingNotificationSpecsTest`, JVM, sur le spec pur `NotificationSpec`, et `RingingNotificationFactoryInstrumentedTest`, sur la vraie `Notification` — écrit, non exécuté faute d'appareil.)*
- [x] **Implémenter `AlarmRingingService`** : `onStartCommand` valide `ServiceCommand`, appelle `startForeground()` immédiatement avec le type `mediaPlayback`, acquiert un `PARTIAL_WAKE_LOCK` de 10 minutes renouvelé toutes les 8 minutes, démarre `AlarmAudioEngine`, retourne `START_STICKY` ; `onDestroy()` arrête audio, vibration et wake lock ; aucune action `STOP`, aucun `Binder` exposant un arrêt. La reconstruction depuis le snapshot arrive à l'étape 17 : à cette étape, un `onStartCommand` avec `intent == null` journalise `PROCESS_RECREATED` et reste au premier plan silencieux.
- [x] **Implémenter `AlarmReceiver`** : valide les extras, journalise `ALARM_RECEIVED` (journal en mémoire pour l'instant, Room à l'étape 9), appelle `RingingController.startRinging()`. Aucun travail long. *(`TechnicalEventLog`/`InMemoryTechnicalEventLog` avancés de l'étape 9 à l'étape 3 : le receiver en a besoin dès maintenant, interface inchangée à l'étape 9. Voir `ETAPE-03.md`.)*
- [x] **Implémenter `AlarmActivity` et `AlarmScreen`** : `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, bord à bord, retour prédictif qui envoie à l'accueil sans toucher le service, heure affichée, texte « Scanne ton boîtier Niumi pour arrêter l'alarme. », état NFC, aucun bouton d'arrêt ; `onStop`/`onDestroy` n'arrêtent rien. Le Reader Mode arrive à l'étape 4.
- [x] **Écrire `PocScreen`** (debug) : champ « dans N secondes », bouton « Programmer », bouton « Annuler », affichage `isScheduled()`. Aucune donnée fictive dans `main`.
- [x] **Écrire les tests instrumentés** `AlarmReceiverInstrumentedTest` (le receiver démarre le service avec les extras) et `AlarmRingingServiceInstrumentedTest` (le service est au premier plan, la notification existe sans action). *(Écrits et compilés ; non exécutés faute d'appareil — voir `ETAPE-03.md`.)*
- [x] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:ringing:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew connectedDebugAndroidTest      # appareil ou émulateur branché
./gradlew ktlintCheck detekt :app:lintDebug
adb shell cmd audio set-enable-hardening throw   # Android 17 : vérifier que le son démarre encore
```

*(Fait le 2026-09-04 sur Redmi 25080RABDG, Android 16 / API 36 — toutes vertes, 6 tests instrumentés compris. `connectedDebugAndroidTest` est lancé sur `:core:system` et `:feature:ringing` explicitement plutôt qu'à la racine : les six modules sans `androidTest` y installent un APK de test vide, ce qui déclenche une restriction d'installation HyperOS. La dernière ligne (`set-enable-hardening`) est spécifique à Android 17, non applicable à cet appareil. Détails dans `ETAPE-03.md`.)*

**Tests manuels (à consigner dans `ETAPE-03.md`) :** alarme dans 90 s, écran éteint et verrouillé → sonnerie audible, écran allumé sur `AlarmActivity`, notification sans action ; fermeture de l'activité → sonnerie maintenue ; retrait de Niumi des récents → alarme conservée. *(Les quatre essais faits et concluants le 2026-09-04, avec relevés `dumpsys` à l'appui — voir `ETAPE-03.md`. **Trois bugs de production ont été trouvés à cette occasion**, tous invisibles aux tests unitaires : notification sans petite icône, canaux de notification jamais créés, et `revision` écrite comme chaîne mais relue comme `Long`. Les trois rendaient le réveil totalement muet ; corrigés et couverts par de nouveaux tests.)*

**Terminé quand :** tests unitaires et instrumentés verts, comportement manuel observé sur au moins un appareil, aucun bouton d'arrêt nulle part. *(Atteint le 2026-09-04 sur un appareil Xiaomi. Restent hors périmètre de cette étape : la matrice multi-marques et le Doze forcé (porte de validation 0, étape 6), Android 17 (`set-enable-hardening`), et la demande de `POST_NOTIFICATIONS` à l'utilisateur — accordée à la main ici, sans elle le full-screen intent ne s'ouvre pas.)*

### Étape 4 : lecture NFC en Reader Mode et arrêt du POC après scan associé

**Specs à lire :** SPEC_CORE_KMP §9 ; SPEC_ANDROID §4.4, §11.1, §11.2.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/nfc/` : `NfcReader.kt`, `NfcAvailability.kt`, `ReaderModeNfcReader.kt` (`enableReaderMode` avec `FLAG_READER_NFC_A` seul, lecture via `Ndef.get(tag)` du premier `NdefRecord` de type URI), `NfcScanHandler.kt`, `ScanOutcome.kt`, `NfcUriExtractor.kt` (pur : `NdefMessage` → `String?`).
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/pairing/` : `PairedBoxStore.kt` (interface : `suspend fun current(): PairedBoxCredentialDto?`, `suspend fun replace(credential)`, `suspend fun clear()`).
- Créer dans `androidApp/app/src/debug/kotlin/com/niumi/app/poc/` : `DebugPairedBoxStore.kt` (DataStore Preferences, variante debug), `PocNfcScanHandler.kt` (parse via `NiumiCoreFacade.parseBoxPayload`, vérifie via `verifyBox` contre `DebugPairedBoxStore`, appelle `RingingController.stopRinging()` si `MATCH`), écran d'association POC dans `PocScreen`.
- Modifier `AlarmActivity` : `NfcReader.start(this, onUri)` dans `onResume()`, `stop()` dans `onPause()`, résultats UI de SPEC_ANDROID §11.2 (vibration courte pour tag Niumi non associé, « Boîtier non reconnu. Réessaie. » pour tag illisible, explication + raccourci réglages si NFC désactivé, « Déverrouille ton téléphone, puis approche-le du boîtier. » si `KeyguardManager.isDeviceLocked`).
- Tests : `NfcUriExtractorTest` (message avec enregistrement URI → chaîne ; enregistrement texte seul → null ; deux enregistrements → premier URI), `AlarmScreenStateTest` (mapping `ScanOutcome` → texte/vibration), `PocNfcScanHandlerTest` (debug : `MATCH` → `stopRinging` appelé une fois ; `BOX_MISMATCH` → `UnknownBox`, aucun arrêt ; `MALFORMED_URI` → `Unreadable`).

**Produit :** `NfcReader`, `NfcScanHandler`, `PairedBoxStore` (interface), `AlarmActivity` complète pour le POC. `PairedBoxStore` reçoit son implémentation Room à l'étape 13 ; `PocNfcScanHandler` est remplacé par `HandleValidNfcUseCase` à l'étape 18 et supprimé à l'étape 21.

- [ ] **Écrire `NfcUriExtractorTest`**, implémenter l'extracteur pur (`NdefRecord.toUri()` puis `toString()`, sans normalisation).
- [ ] **Implémenter `ReaderModeNfcReader`** : `availability` depuis `PackageManager.FEATURE_NFC` et `NfcAdapter.isEnabled`, `enableReaderMode(activity, callback, FLAG_READER_NFC_A, extras delay 250 ms)`, lecture `Ndef` bornée à 96 octets avant toute allocation (SPEC_ANDROID §16), `disableReaderMode` dans `stop()`.
- [ ] **Écrire `PocNfcScanHandlerTest`**, implémenter `DebugPairedBoxStore` et `PocNfcScanHandler` en debug.
- [ ] **Compléter `AlarmActivity`** avec le Reader Mode et les textes ; `AlarmScreenStateTest` en vert.
- [ ] **Ajouter à `PocScreen`** un bouton « Associer ce tag » qui active le Reader Mode dans une `PocPairingActivity` debug et stocke `PairedBoxCredential.fromPayload` ; afficher le `boxId` tronqué, jamais le token.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:ringing:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** associer un tag écrit avec un payload canonique ; alarme dans 60 s ; scan du bon tag → arrêt en moins d'une seconde ; scan d'un autre tag → vibration courte, sonnerie maintenue ; NFC désactivé → instruction, sonnerie maintenue ; téléphone verrouillé → noter si le scan fonctionne avant déverrouillage (matrice §20).

**Terminé quand :** tests verts, comportement manuel consigné avec le modèle d'appareil, aucune décision de validité prise hors de `:shared:core`.

### Étape 5 : blocage par AccessibilityService, overlay et page de consentement

**Specs à lire :** SPEC_ANDROID §4.3, §12.2, §12.3, §14, §16.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/blocking/` : `BlockingController.kt`, `ForegroundAppSource.kt`, `BlockedPackagesProjection.kt` (interface : `fun current(): BlockedPackagesState` avec `Inactive`, `Active(sessionId, packages)`, `Releasing(sessionId, effectivePackages)`), `BlockingDecision.kt` (pur : `decide(state, foregroundPackage): BlockAction` avec `None`, `GoHome(packageName, displayName)`), `AccessibilityServiceStatus.kt` (lit `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`).
- Créer dans `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking/` : `NiumiBlockingAccessibilityService.kt`, `BlockOverlayView.kt` (`TYPE_ACCESSIBILITY_OVERLAY`, texte « {Nom de l'application} reste bloquée jusqu'au scan du boîtier. », retrait à 3 s ou dès changement de package), `AndroidBlockingController.kt` (écrit la projection dans un `BlockedPackagesProjection` mutable ; sur Room à partir de l'étape 15).
- Créer `androidApp/feature/session/src/main/res/xml/niumi_accessibility_service.xml` avec exactement la configuration de SPEC_ANDROID §12.2 ; manifeste : service exporté, `BIND_ACCESSIBILITY_SERVICE`, `isAccessibilityTool=false`.
- Créer dans `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/accessibility/` : `AccessibilityConsentScreen.kt` (les cinq points de §12.3, bouton « Ouvrir les réglages d'accessibilité » → `ACTION_ACCESSIBILITY_SETTINGS`), `AccessibilityConsentViewModel.kt`.
- Debug : `PocScreen` gagne « Application factice à bloquer » (sélecteur simple par nom de package saisi) et affiche l'état du service.
- Tests : `BlockingDecisionTest`, `AccessibilityServiceStatusTest` (parsing de la chaîne `ENABLED_ACCESSIBILITY_SERVICES`), `AccessibilityConsentScreenTest` (Compose : cinq points présents, bouton présent, aucun clic simulé), instrumenté `NiumiBlockingAccessibilityServiceTest` (service activé manuellement sur l'appareil, ouverture d'une application factice → `GLOBAL_ACTION_HOME` et overlay).

**Produit :** `BlockingController`, `BlockedPackagesProjection`, `BlockingDecision`, le service et l'écran de consentement définitifs.

- [ ] **Écrire `BlockingDecisionTest`** : `Inactive` → `None` ; `Active` et package non listé → `None` ; `Active` et package listé → `GoHome` ; `Releasing` avec liste effective vide → `None` ; package Niumi lui-même → `None`. Implémenter.
- [ ] **Écrire `AccessibilityServiceStatusTest`** : chaîne contenant `com.niumi.app/com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService` → actif ; chaîne vide ou autre service → inactif. Implémenter.
- [ ] **Implémenter le service** : `onAccessibilityEvent` lit uniquement `event.packageName`, appelle `BlockingDecision`, exécute `performGlobalAction(GLOBAL_ACTION_HOME)`, affiche l'overlay, journalise `BLOCK_APPLIED` avec le package seul ; `onServiceConnected` recharge la projection ; jamais de `getRootInActiveWindow()`, jamais de lecture de texte.
- [ ] **Implémenter `AccessibilityConsentScreen`** et son test Compose.
- [ ] **Compléter `PocScreen`** avec l'application factice et l'état du service.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :feature:setup:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** activer le service via la page de consentement ; ouvrir l'application factice depuis le launcher, les récents et une notification → retour à l'accueil et overlay à chaque fois ; ouvrir une application autorisée → aucun effet ; désactiver le service → le POC affiche l'état inactif.

**Terminé quand :** tests verts, `canRetrieveWindowContent=false` vérifié dans le XML, aucun accès au contenu de fenêtre dans le code (grep `rootInActiveWindow`, `getText`, `contentDescription` sur le service vide).

### Étape 6 : dossier Google Play et porte de validation 0

**Specs à lire :** SPEC_ANDROID §2, §4, §12.3, §20, §22 (Lot 0), §23.

**Fichiers :**
- Créer `docs/android/play-console/ACCESSIBILITY_DECLARATION.md` (usage déclaré, données observées, finalité, texte de divulgation identique à l'écran de consentement), `docs/android/play-console/USE_EXACT_ALARM.md` (justification « réveil fonction centrale »), `docs/android/play-console/FULL_SCREEN_INTENT.md`, `docs/android/play-console/FGS_MEDIA_PLAYBACK.md`, `docs/android/play-console/PRIVACY_POLICY.md` (données consultées et conservées, aucune transmission), `docs/android/play-console/REVIEW_VIDEO_SCRIPT.md` (plan de la vidéo : divulgation, consentement, alarme, scan, blocage).
- Créer `docs/android/implementation-reports/LOT-0.md` : matrice d'essais vide à remplir (fabricant, modèle, Android, firmware, permissions, scénario, résultat, retard mesuré).

- [ ] **Rédiger les six documents Play** en français, sans promesse d'incontournabilité du blocage (§23).
- [ ] **Construire un APK debug** signé avec la clé debug et le déposer manuellement sur les appareils de test : `./gradlew :app:assembleDebug`.
- [ ] **Exécuter la matrice POC** sur au minimum un Pixel, un Samsung et un Xiaomi : écran éteint 30 min, Doze forcé (`adb shell dumpsys deviceidle force-idle`), verrouillage, NFC réel, scan avant déverrouillage, application bloquée depuis launcher/récents/notification, Android 17 avec `set-enable-hardening throw`.
- [ ] **Enregistrer la vidéo de revue** selon le script.
- [ ] **Soumettre sur piste interne ou fermée** dès que le compte Play le permet (action humaine) ; consigner la date et la réponse de Google dans `LOT-0.md`.

**Porte de validation 0 :** ne pas commencer l'étape 7 sans validation humaine écrite dans `LOT-0.md`. Un refus Play, une incompatibilité OEM ou un échec du scan verrouillé qui remet en cause le produit exige une révision explicite des specs avant de continuer.

## Phase C — Lot 0.5 : moteur KMP complet

### Étape 7 : machine à états `SessionEngine`

**Specs à lire :** SPEC_CORE_KMP §2, §4, §5, §6, §7, §12, §17 (Machine à états et Politiques).

**Fichiers :**
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/domain/` : `SessionState.kt`, `ReleaseTarget.kt`, `SessionHealth.kt`, `IncidentSeverity.kt`, `Platform.kt`, `SessionIncident.kt`, `WakeSchedule.kt`, `AppSelectionSummary.kt`, `ActivationRequest.kt`, `SessionSnapshot.kt`, `SessionEventKind.kt`, `SessionEvent.kt`, `SessionEffectKind.kt`, `SessionEffect.kt`, `SessionEffectPayload.kt`, `DomainViolation.kt` (`code: String`, `message: String`), `ViolationCode.kt` (les 13 codes de §7.3), `IncidentCodes.kt` (codes communs et gravités par défaut de §7.3), `SessionDecision.kt`, `SessionEngine.kt`, `EffectIdFactory.kt`.
- Créer dans `shared/core/src/commonTest/kotlin/com/niumi/core/domain/` : `SessionFixtures.kt` (constructeurs de snapshots par état, événements valides, `WakeSchedule` de référence avec `triggerAtEpochMillis = 1_800_000_000_000`), `SessionEngineActivationTest.kt`, `SessionEngineTriggerTest.kt`, `SessionEngineNfcTest.kt`, `SessionEngineReleaseTest.kt`, `SessionEngineIncidentTest.kt`, `SessionEngineValidationTest.kt`, `SessionEngineEffectsTest.kt`, `SessionEngineForbiddenTransitionsTest.kt` (table exhaustive états × événements).

**Interface produite :** exactement les signatures de SPEC_CORE_KMP §5, §6, §7 (`SessionEngine.reduce(snapshot, event): SessionDecision`). La façade et les DTO arrivent à l'étape 8.

- [ ] **Écrire les types du domaine** en copiant les blocs Kotlin des §5, §6 et §7 ; `SessionEvent` valide dans `init` que `eventId` et `sessionId` sont des UUID canoniques minuscules et que les horodatages sont positifs (les violations `INVALID_IDENTIFIER` et `INVALID_TIMESTAMP` sont produites par `reduce`, pas par une exception : construire via une fonction `SessionEvent.validated()` retournant le problème).
- [ ] **Écrire `SessionEngineActivationTest`** : `null` + `ACTIVATION_REQUESTED` → `PREPARING`, revision 1, `health HEALTHY`, effets `PUBLISH_PLATFORM_SNAPSHOT`, `SCHEDULE_ALARM`, `APPLY_BLOCKING` dans cet ordre ; snapshot non nul non final + `ACTIVATION_REQUESTED` → `INVALID_STATE_TRANSITION` ; `count` 0 ou 51 → violation, 1 et 50 acceptés ; `PREPARING` + `ACTIVATION_SUCCEEDED` → `ARMED`, `armedAt` renseigné, effet `PUBLISH` ; `PREPARING` + `ACTIVATION_FAILED` avec `failureCode` → `FAILED`, effets `CANCEL_ALARM`, `REMOVE_BLOCKING`, `PUBLISH`, `CLEAR_ACTIVE_SESSION` ; sans `failureCode` → `MISSING_FAILURE_CODE` ; `ARMED` + `ACTIVATION_FAILED` → `INVALID_STATE_TRANSITION`.
- [ ] **Implémenter `reduce()` pour l'activation** ; test vert.
- [ ] **Écrire `SessionEngineTriggerTest`** : `ARMED` + `ALARM_FIRED` → `RINGING`, effets `PUBLISH`, `START_RINGING` ; `ARMED` + `TRIGGER_ELAPSED` à `triggerAt` → `TRIGGERED_AWAITING_NFC`, effets `PUBLISH`, `PRESENT_SCAN_REQUEST` ; même chose après l'heure avec incident `MISSED_TRIGGER_WINDOW` joint → effet `RECORD_INCIDENT` avec `IncidentEffectPayload` et `health DEGRADED` ; avant l'heure → `TRIGGER_NOT_REACHED` ; `ARMED`/`RINGING` + `ALARM_SOUND_STOPPED` → `AWAITING_NFC`, effets `PUBLISH`, `PRESENT_SCAN_REQUEST` ; `TRIGGERED_AWAITING_NFC` + `ALARM_SOUND_STOPPED` → état inchangé, revision incrémentée, `alarmSoundStoppedAt` renseigné ; `ALARM_FIRED` dans un état final → violation.
- [ ] **Implémenter**, test vert.
- [ ] **Écrire `SessionEngineNfcTest`** : `ARMED` avant l'heure + `VALID_NFC_SCANNED` avec preuve cohérente → `RELEASING`, `releaseTarget CANCELLED`, `nfcVerifiedAt` renseigné, effets `PUBLISH`, `CANCEL_ALARM`, `STOP_RINGING`, `CLEAR_SCAN_REQUEST`, `REMOVE_BLOCKING` ; `ARMED` à l'heure ou après → `TRIGGER_ALREADY_ELAPSED` ; `RINGING`/`AWAITING_NFC`/`TRIGGERED_AWAITING_NFC` → `RELEASING` avec `COMPLETED` ; preuve absente → `MISSING_NFC_PROOF` ; preuve dont `sessionId`, `eventId`, `expectedRevision` ou `verifiedAtEpochMillis` diffèrent de l'événement → `UNEXPECTED_EVENT_PAYLOAD` ; `INVALID_NFC_SCANNED` → état inchangé, aucun effet hors `PUBLISH` ; `VALID_NFC_SCANNED` dans `PREPARING` ou un état final → `INVALID_STATE_TRANSITION`.
- [ ] **Implémenter** ; la preuve est construite dans les tests via une fonction `internal` de test située dans `commonTest` (même module, donc accès `internal`).
- [ ] **Écrire `SessionEngineReleaseTest`** : `RELEASING` + `RELEASE_FAILED` avec incident → `RELEASING`, `health DEGRADED`, effets `RECORD_INCIDENT`, `PUBLISH` ; sans incident → `MISSING_INCIDENT` ; `RELEASING` + `RELEASE_SUCCEEDED` → cible enregistrée avec `completedAt` ou `cancelledAt`, effets `PUBLISH`, `CLEAR_ACTIVE_SESSION` ; `RELEASE_SUCCEEDED` hors `RELEASING` → violation ; `RELEASING` sans `nfcVerifiedAt` (snapshot forgé) → violation `INVALID_STATE_TRANSITION`.
- [ ] **Implémenter**, test vert.
- [ ] **Écrire `SessionEngineIncidentTest`** : `INCIDENT_REPORTED` `WARNING` → `health` inchangé ; `DEGRADED` ou `CRITICAL` → `DEGRADED` ; un second incident `WARNING` après `DEGRADED` ne remonte pas à `HEALTHY` ; incident sur un état final → violation ; `ARMED` + `ACTIVATION_FAILED` reste refusé (jamais `FAILED` après armement).
- [ ] **Écrire `SessionEngineValidationTest`** : `expectedRevision` absent hors activation → `STALE_REVISION` ; `expectedRevision` ≠ `snapshot.revision` → `STALE_REVISION` ; `sessionId` différent → `UNKNOWN_SESSION` ; identifiant non canonique → `INVALID_IDENTIFIER` ; horodatage ≤ 0 → `INVALID_TIMESTAMP` ; `activationRequest` fourni hors `ACTIVATION_REQUESTED` → `UNEXPECTED_EVENT_PAYLOAD` ; `incident` fourni sur `ALARM_FIRED` → `UNEXPECTED_EVENT_PAYLOAD` ; une décision refusée renvoie `snapshot` inchangé et `effects` vide.
- [ ] **Écrire `SessionEngineEffectsTest`** : `effectId == "$sessionId:$revision:$kind:$ordinal"` ; deux appels identiques produisent les mêmes `effectId` ; `payload` nul sauf `RECORD_INCIDENT`.
- [ ] **Écrire `SessionEngineForbiddenTransitionsTest`** : pour chaque paire (état, kind) non listée en §5.1, `reduce` renvoie au moins une violation et aucun effet.
- [ ] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew ktlintCheck detekt
```

**Terminé quand :** chaque ligne de SPEC_CORE_KMP §5.1 et §5.2 a un test nommé, tous verts, `reduce` sans horloge ni aléa.

### Étape 8 : politique horaire, politique d'activation, façade complète, fixtures et framework iOS

**Specs à lire :** SPEC_CORE_KMP §8, §7.4, §13 (règles partagées), §14, §16, §17 (Date et heure, Politiques), §19.

**Fichiers :**
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/schedule/` : `WakeScheduleInput.kt` (`localTimeIso`, `zoneId`, `nowEpochMillis`), `WakeScheduleCalculator.kt`, `WakeScheduleResult.kt` (`Success(schedule)`, `Failure(code)` avec `INVALID_TIME`, `UNKNOWN_ZONE`), `TriggerDelayPolicy.kt` (`evaluate(triggerAt, nowEpochMillis): TriggerDelayOutcome` avec `NOT_REACHED`, `FIRE_NOW` ≤ 15 min, `MISSED` > 15 min).
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/diagnostics/` : `ReadinessSeverity.kt`, `ActivationPolicy.kt` (`evaluate(input): ActivationPolicyResult` : refus si un contrôle `BLOCKING_*` échoue, si `count` hors 1..50, si `triggerAt ≤ now`, si aucun boîtier), `ActivationPolicyInput.kt`, `ActivationPolicyResult.kt` (`allowed`, `blockingReasons`, `warnings`).
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/interop/` : `SessionDtos.kt` (miroirs `*Dto` de tous les types de §5 à §7, enums réexportés à l'identique), `DtoMappers.kt`, `NiumiCoreFacade.kt` (les cinq méthodes de §14).
- Créer `shared/core/src/commonTest/resources/fixtures/wake_schedules.json`, `session_transitions.json` (état source, événement, état attendu, effets attendus) et `shared/core/src/jvmTest/.../FixturesTest.kt` ; tests `WakeScheduleCalculatorTest`, `TriggerDelayPolicyTest`, `ActivationPolicyTest`, `NiumiCoreFacadeTest`, `DtoRoundTripTest`.

**Interface produite :** `NiumiCoreFacade` complète (§14) plus une sixième méthode `evaluateTriggerDelay(input: TriggerDelayInputDto): TriggerDelayResultDto` (`triggerAtEpochMillis`, `nowEpochMillis` → `NOT_REACHED`, `FIRE_NOW`, `MISSED`). Cette méthode étend §14 : l'ajouter à SPEC_CORE_KMP §14 dans le même changement, avec la mention qu'iOS peut l'ignorer (§8.2). Les adaptateurs Android n'importent jamais `com.niumi.core.domain` directement, seulement `com.niumi.core.interop`.

- [ ] **Écrire `WakeScheduleCalculatorTest`** avec `Europe/Paris` : 22:00 saisi à 20:00 → même jour ; 07:00 saisi à 20:00 → lendemain ; 07:00 saisi à 07:00 exact → lendemain (strictement futur) ; 02:30 le 29 mars 2026 (heure inexistante) → 03:00 ; 02:30 le 25 octobre 2026 (heure répétée) → première occurrence (UTC+2) ; `zoneId` inconnu → `UNKNOWN_ZONE` ; `localTimeIso` `25:00` → `INVALID_TIME` ; le résultat conserve `localDateIso`, `localTimeIso`, `zoneIdAtActivation`.
- [ ] **Implémenter avec `kotlinx-datetime`** (`LocalDateTime.toInstant(TimeZone)` applique déjà la règle « premier instant valide / première occurrence » : le test le prouve, ne pas réimplémenter).
- [ ] **Écrire `TriggerDelayPolicyTest`** : retard −1 s → `NOT_REACHED` ; 0, 15 min exactement → `FIRE_NOW` ; 15 min + 1 ms → `MISSED`. Implémenter.
- [ ] **Écrire `ActivationPolicyTest`** : tout vert → `allowed` ; un `BLOCKING_FOR_ALARM` → refus avec raison ; un `BLOCKING_FOR_NIUMI_EXPERIENCE` → refus ; `WARNING` seul → autorisé avec avertissement ; `count` 0/51 → refus ; `triggerAt ≤ now` → refus ; boîtier absent → refus. Implémenter.
- [ ] **Écrire les DTO et mappers** ; `DtoRoundTripTest` vérifie `snapshot → dto → snapshot` égal pour chaque état, et que `SessionEventDto` transporte `NfcVerificationProof` par référence sans le sérialiser (`@Transient`, aucun champ `proof` dans le JSON).
- [ ] **Écrire `NiumiCoreFacadeTest`** : `reduce` renvoie un `SessionDecisionDto` avec violations typées (jamais d'exception, y compris sur un `SessionEventDto` incohérent) ; `computeWakeSchedule` et `evaluateActivation` délèguent ; `verifyBox` inchangé depuis l'étape 2.
- [ ] **Écrire les fixtures JSON** (au moins 30 transitions et 8 horaires) et `FixturesTest`.
- [ ] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64 :shared:core:linkReleaseFrameworkIosArm64
./gradlew ktlintCheck detekt
```

**Terminé quand :** les cinq méthodes de §14 existent, aucune n'est `suspend`, aucune ne lance, les fixtures sont rejouées, le framework `NiumiCore` se lie (ou l'absence de Xcode est consignée).

## Phase D — Lot 1 : persistance et coordination Android

### Étape 9 : Room — entités, DAO, transaction de décision, journal et mappings KMP

**Specs à lire :** SPEC_CORE_KMP §6.1, §13 ; SPEC_ANDROID §7.2, §16, §17, §19.1 (`core:system` mappings), §19.2 (migrations).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/` : `NiumiDatabase.kt` (`version = 1`, `exportSchema = true`, schéma dans `androidApp/core/database/schemas/`), `entity/AlarmSessionEntity.kt`, `entity/BlockedAppEntity.kt`, `entity/PairedBoxEntity.kt`, `entity/TechnicalEventEntity.kt`, `entity/SessionIncidentEntity.kt`, `entity/SessionEventReceiptEntity.kt` (`eventId` PK, `sessionId`, `payloadSha256Hex`, `appliedRevision`, `receivedAtEpochMillis`), `entity/SessionEffectOutboxEntity.kt` (`effectId` PK, `sessionId`, `revision`, `kind`, `ordinal`, `payloadJson?`, `status` ∈ `PENDING|SUCCEEDED|FAILED|SATISFIED`, `lastError?`, `updatedAtEpochMillis`), `entity/ActiveSessionPointerEntity.kt` (`id = 1`, `sessionId`), `converter/EnumConverters.kt`, `dao/SessionDao.kt`, `dao/PairedBoxDao.kt`, `dao/TechnicalEventDao.kt`, `dao/IncidentDao.kt`, `dao/ReceiptDao.kt`, `dao/OutboxDao.kt`, `SessionStore.kt` (interface des « Interfaces transverses »), `RoomSessionStore.kt`, `mapping/SessionSnapshotMapper.kt` (`AlarmSessionEntity ↔ SessionSnapshotDto` + `AndroidSessionExtras(boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, blockedPackages)`), `mapping/EventFingerprint.kt` (SHA-256 hex du JSON canonique sans `eventId` ni preuve), `TechnicalEventLog.kt` (interface `log(type, sessionId?, detailsJson?)` + impl Room avec purge au-delà de 200), `di/DatabaseModule.kt`.
- Tests unitaires : `SessionSnapshotMapperTest` (aller-retour pour chaque état, `null` préservés), `EventFingerprintTest` (même événement → même empreinte ; `eventId` différent → même empreinte ; `occurredAt` différent → empreinte différente). Tests instrumentés : `RoomSessionStoreTest` (base en mémoire : `commitDecision` écrit session, apps, reçu, effets dans une transaction ; une exception au milieu ne laisse rien), `TechnicalEventLogTest` (201 insertions → 200 lignes, la plus ancienne supprimée), `MigrationTest` (`MigrationTestHelper`, schéma v1 exporté).

**Produit :** `SessionStore`, `TechnicalEventLog`, `PairedBoxDao`, `SessionSnapshotMapper`, `EventFingerprint`.

- [ ] **Écrire `SessionSnapshotMapperTest`**, créer les entités (colonnes exactes de §7.2, `boxTokenSha256Hex` figé) et le mapper.
- [ ] **Écrire `EventFingerprintTest`**, implémenter avec `kotlinx-serialization` (`Json { encodeDefaults = true }`, champs triés par déclaration, `eventId` et `nfcProof` exclus) et `java.security.MessageDigest` (côté Android uniquement).
- [ ] **Écrire les DAO et `RoomSessionStore`** ; `commitDecision` est `@Transaction` ; `activeSession()` lit le pointeur puis la session, ses apps, ses effets `PENDING`.
- [ ] **Implémenter `TechnicalEventLog`** avec une liste blanche des types de §17 (un type inconnu est refusé par test).
- [ ] **Écrire les tests instrumentés** et le schéma exporté.
- [ ] **Vérifier :**

```bash
./gradlew :core:database:testDebugUnitTest
./gradlew :core:database:connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Terminé quand :** aller-retour Room ↔ `SessionSnapshotDto` prouvé pour les neuf états, transaction unique prouvée, journal borné à 200, schéma v1 versionné.

### Étape 10 : snapshot Direct Boot

**Specs à lire :** SPEC_CORE_KMP §13 ; SPEC_ANDROID §7.3, §9.2 (paragraphe Room / Direct Boot), §19.1 et §19.2 (Direct Boot).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/directboot/` : `DirectBootSnapshot.kt` (`@Serializable`, champs exacts de SPEC_ANDROID §7.3 avec `projectionSchemaVersion = 1`, `domainSchemaVersion = 1`, `domainRevision`, listes `eventReceipts` et `pendingEffects` avec payload sérialisé ; variante `Corrupted(reason)`), `DirectBootStore.kt` (interface), `FileDirectBootStore.kt` (`context.createDeviceProtectedStorageContext().filesDir/niumi_session.json`, écriture via `AtomicFile`, lecture tolérante retournant `Corrupted` sans exception), `DirectBootMapper.kt` (`DirectBootSnapshot ↔ (SessionSnapshotDto, AndroidSessionExtras, receipts, effects)`), `UnlockState.kt` (`interface UnlockState { val isUserUnlocked: Boolean }`, impl `UserManager`).
- Tests unitaires : `DirectBootMapperTest` (aller-retour complet, `SessionSnapshotDto` reconstruit valide pour `reduce`), `DirectBootSnapshotJsonTest` (sérialisation stable, champ inconnu ignoré, JSON tronqué → `Corrupted`). Tests instrumentés : `FileDirectBootStoreTest` (écriture puis lecture ; `domainRevision` inférieure refusée avec `Failure("STALE_REVISION")` ; égale acceptée ; fichier corrompu → `Corrupted` et aucun effacement automatique ; `clear()`).

**Produit :** `DirectBootStore`, `DirectBootMapper`, `UnlockState`.

- [ ] **Écrire `DirectBootSnapshotJsonTest`** et le type sérialisable.
- [ ] **Écrire `DirectBootMapperTest`**, implémenter le mapper (réutilise `SessionSnapshotMapper` de l'étape 9 pour les champs communs).
- [ ] **Écrire `FileDirectBootStoreTest`**, implémenter `FileDirectBootStore` avec `AtomicFile.startWrite()/finishWrite()` et comparaison de révision avant écriture.
- [ ] **Ajouter dans `DatabaseModule`** une règle Hilt : `NiumiDatabase` est fourni via `Lazy`/`Provider` et `RoomSessionStore` vérifie `UnlockState.isUserUnlocked` avant tout accès, sinon `IllegalStateException("ROOM_BEFORE_UNLOCK")` (testé unitairement avec un `UnlockState` faux).
- [ ] **Vérifier :**

```bash
./gradlew :core:database:testDebugUnitTest :core:database:connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Terminé quand :** un snapshot Direct Boot actif se convertit en `SessionSnapshotDto` accepté par `NiumiCoreFacade.reduce` (test explicite), écriture atomique, révision protégée, corruption explicite.

### Étape 11 : `SessionCoordinator`, registre idempotent, outbox, exécution des effets et réconciliateur

**Specs à lire :** SPEC_CORE_KMP §4, §6, §6.1, §10, §12, §13 ; SPEC_ANDROID §7.1 (`SessionRuntimeStatus`), §9.2, §11.3, §18.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/session/` : `SessionCoordinator.kt`, `DispatchResult.kt`, `ReconcileReason.kt`, `ReconcileResult.kt`, `DefaultSessionCoordinator.kt`, `SessionPersistenceGateway.kt` (interface : `load()`, `commit(decision)`, `receipt(eventId)`, `pendingEffects()`, `markEffect()`, `clearActive()`), `UnlockAwarePersistenceGateway.kt` (Room si déverrouillé, Direct Boot sinon ; miroir Direct Boot après chaque commit Room), `EffectExecutor.kt` (interface `execute(effect, snapshot, extras): OperationResult`), `EffectDispatcher.kt` (table `SessionEffectKind → EffectExecutor`, ordre de §6, distinction requis / best-effort), `executors/PublishSnapshotExecutor.kt`, `executors/ScheduleAlarmExecutor.kt`, `executors/CancelAlarmExecutor.kt`, `executors/ApplyBlockingExecutor.kt`, `executors/RemoveBlockingExecutor.kt`, `executors/StartRingingExecutor.kt`, `executors/StopRingingExecutor.kt`, `executors/PresentScanRequestExecutor.kt`, `executors/ClearScanRequestExecutor.kt`, `executors/ClearActiveSessionExecutor.kt`, `executors/RecordIncidentExecutor.kt`, `SessionRuntimeStatus.kt`, `SessionRuntimeStatusProbe.kt`, `SessionReconciler.kt`, `PhaseCompletion.kt` (décide `ACTIVATION_SUCCEEDED`/`FAILED` et `RELEASE_SUCCEEDED`/`FAILED` selon les effets requis), `SessionSnapshotPublisher.kt` (`StateFlow<SessionSnapshotDto?>` pour l'UI).
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/notification/` : `ScanRequestNotifier.kt`, `AndroidScanRequestNotifier.kt` (canal `niumi_session_awaiting_scan`, titre « Ton réveil Niumi est passé », texte « Scanne ton boîtier pour débloquer tes applications. », `ongoing`, sans son, vibration, full-screen ni action ; tap → `AlarmActivity` en mode scan ; fonctionne avec un `DeviceProtectedStorageContext`).
- Tests unitaires avec fakes (`FakeAlarmScheduler`, `FakeBlockingController`, `FakeRingingController`, `FakeScanRequestNotifier`, `InMemoryPersistenceGateway`) : `SessionCoordinatorActivationTest`, `SessionCoordinatorIdempotenceTest`, `SessionCoordinatorReleaseTest`, `SessionCoordinatorOutboxReplayTest`, `EffectDispatcherTest`, `PhaseCompletionTest`, `SessionReconcilerTest`, `AndroidScanRequestNotifierTest` (construction de la notification), `SessionCoordinatorMutexTest` (deux `dispatch` concurrents sont sérialisés : Turbine + `runTest`).

**Produit :** `SessionCoordinator` complet, `EffectDispatcher`, `SessionReconciler`, `SessionSnapshotPublisher`, `ScanRequestNotifier`. Consommé par toutes les étapes suivantes.

- [ ] **Écrire `SessionCoordinatorIdempotenceTest`** : même `eventId` + même empreinte → `Duplicate`, `reduce` non appelé (façade espionnée), aucun effet ; même `eventId` + empreinte différente → `Rejected(EVENT_ID_CONFLICT)` ; événement d'une autre session → `Rejected(UNKNOWN_SESSION)`. Implémenter le registre dans `DefaultSessionCoordinator.dispatch` avant l'appel à la façade.
- [ ] **Écrire `SessionCoordinatorActivationTest`** : `ACTIVATION_REQUESTED` → persistance de `PREPARING` + reçu + 3 effets **avant** tout appel aux fakes (ordre vérifié par journal d'appels), puis `SCHEDULE_ALARM` et `APPLY_BLOCKING` exécutés, puis `ACTIVATION_SUCCEEDED` auto-dispatché → `ARMED` ; si `SCHEDULE_ALARM` échoue → `CANCEL_ALARM`, `REMOVE_BLOCKING` puis `ACTIVATION_FAILED` avec `failureCode = "ALARM_SCHEDULE_FAILED"` → `FAILED` et pointeur effacé ; `PUBLISH_PLATFORM_SNAPSHOT` en échec n'empêche pas `ACTIVATION_SUCCEEDED`.
- [ ] **Implémenter `EffectDispatcher`, `PhaseCompletion` et les exécuteurs** ; `AlreadySatisfied` compte comme succès et journalise un incident `CRITICAL` uniquement pour `REMOVE_BLOCKING` quand le service est désactivé (`BLOCKING_PERMISSION_REVOKED`).
- [ ] **Écrire `SessionCoordinatorReleaseTest`** : `VALID_NFC_SCANNED` → `RELEASING` persisté, puis `CANCEL_ALARM`, `STOP_RINGING`, `CLEAR_SCAN_REQUEST`, `REMOVE_BLOCKING` ; succès des requis → `RELEASE_SUCCEEDED` → état final + `CLEAR_ACTIVE_SESSION` ; échec de `REMOVE_BLOCKING` avec précondition tenue → `RELEASE_FAILED` avec `RELEASE_PARTIAL_FAILURE`, état `RELEASING`, effet conservé `FAILED` dans l'outbox ; échec de `STOP_RINGING` seul → `RELEASE_SUCCEEDED` quand même, erreur consignée.
- [ ] **Écrire `SessionCoordinatorOutboxReplayTest`** : outbox avec `CANCEL_ALARM SUCCEEDED` et `REMOVE_BLOCKING PENDING` → `reconcile(PROCESS_START)` n'exécute que `REMOVE_BLOCKING`, jamais `APPLY_BLOCKING` ; `RECORD_INCIDENT PENDING` rejoué avec son payload ; `PRESENT_SCAN_REQUEST` rejoué deux fois → notifier appelé, résultat identique.
- [ ] **Écrire `SessionReconcilerTest`** : `PREPARING` incomplet avec alarme déjà programmée et blocage appliqué → reprise vers `ARMED` ; `PREPARING` avec alarme absente → rollback vers `FAILED` ; `ARMED` avec `isScheduled() == false` → `ALARM_RESCHEDULED` puis alarme reprogrammée au même `triggerAt` ; `ARMED` et `TriggerDelayPolicy.MISSED` → `TRIGGER_ELAPSED` + `MISSED_TRIGGER_WINDOW` ; `ARMED` et `FIRE_NOW` avec `reason != BEFORE_SCAN` → alarme immédiate reprogrammée ; `ARMED` et `FIRE_NOW` avec `BEFORE_SCAN` → `TRIGGER_ELAPSED` sans incident ; service d'accessibilité inactif pendant `ARMED` → `INCIDENT_REPORTED BLOCKING_PERMISSION_REVOKED CRITICAL`, état conservé ; `canScheduleExactAlarms == false` → `ALARM_PERMISSION_REVOKED`.
- [ ] **Implémenter `SessionRuntimeStatusProbe` et `SessionReconciler`** ; la politique de retard est lue via `NiumiCoreFacade.evaluateTriggerDelay()` (ajoutée à l'étape 8), jamais recalculée côté Android.
- [ ] **Écrire `SessionCoordinatorMutexTest`**, garantir le `Mutex` unique partagé par `dispatch` et `reconcile`.
- [ ] **Implémenter `AndroidScanRequestNotifier`** et son test.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest
./gradlew :shared:core:jvmTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Terminé quand :** tous les scénarios ci-dessus sont verts, aucun exécuteur n'écrit un `SessionState`, la persistance précède toujours l'exécution (test d'ordre), la spec §14 documente `evaluateTriggerDelay`.

## Phase E — Lot 2 : configuration

### Étape 12 : onboarding, `DeviceReadinessChecker` et écran de diagnostic

**Specs à lire :** SPEC_ANDROID §3 (dernier point), §4.5, §10.3 (Android 14), §12.3, §13, §15 (écrans 1, 2, 12) ; SPEC_CORE_KMP §7.3 (`ReadinessSeverity`).

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/readiness/` : `ReadinessCheckId.kt` (les 13 contrôles de §13 dans l'ordre du tableau), `ReadinessAction.kt` (`OpenNfcSettings`, `StartPairing`, `OpenAppPicker`, `ShowExactAlarmDiagnostic`, `OpenFullScreenIntentSettings`, `RequestNotificationPermission`, `OpenChannelSettings(channelId)`, `OpenSoundSettings`, `OpenDndSettings`, `OpenAccessibilitySettings`, `FixTime`, `OpenBatterySettings`, `Unsupported`), `ReadinessCheck.kt`, `DeviceReadinessChecker.kt`, `AndroidDeviceReadinessChecker.kt` (sources injectées : `NfcAvailability`, `PairedBoxStore`, sélection courante, `AlarmManager.canScheduleExactAlarms()`, `NotificationManager.canUseFullScreenIntent()` si ≥ 34, `POST_NOTIFICATIONS` si ≥ 33, état du canal `niumi_alarm_ringing`, `AudioManager.getStreamVolume(STREAM_ALARM)`, `NotificationManager.currentInterruptionFilter`, `AccessibilityServiceStatus`, `PowerManager.isIgnoringBatteryOptimizations`), `ReadinessDtoMapper.kt` (→ `ActivationPolicyInputDto`).
- Créer dans `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/` : `onboarding/OnboardingScreen.kt` (pages : promesse, limites §4.2 et §4.5 incluant arrêt forcé, NFC verrouillé, désactivation de l'accessibilité, absence de secours logiciel ; case « J'ai compris » obligatoire, stockée dans DataStore `onboarding_acknowledged`), `readiness/ReadinessScreen.kt` (une seule action principale, le premier blocage d'abord, messages de §13 mot pour mot, recalcul dans `ON_RESUME`), `readiness/ReadinessViewModel.kt`, `SetupNavigation.kt`.
- Créer dans `androidApp/app/src/main/kotlin/com/niumi/app/navigation/` : `NiumiNavHost.kt` (routes typées `Home`, `Onboarding`, `Readiness`, `AccessibilityConsent`, `Pairing`, `AppPicker`, `WakeTime`, `Summary`, `ActiveSession`, `ScanToModify`, `Completed`, `Cancelled`, `IncidentDiagnostic`), `HomeScreen.kt` (sans session : bouton « Préparer mon réveil » ; avec session : redirection vers `ActiveSession`).
- Tests : `AndroidDeviceReadinessCheckerTest` (fakes pour chaque source ; chaque contrôle passe/échoue avec la bonne sévérité et la bonne action ; `canScheduleExactAlarms == false` → `BLOCKING_FOR_ALARM` avec `ShowExactAlarmDiagnostic`, jamais une action vers « Alarmes et rappels » ; plein écran sous Android 13 → contrôle non applicable), `ReadinessViewModelTest` (ordre : premier blocage affiché d'abord ; `WARNING` seul → activation permise via `evaluateActivation`), `OnboardingScreenTest` (les quatre limites sont affichées, bouton inactif sans case cochée), `ReadinessScreenTest` (une seule action visible).

**Produit :** `DeviceReadinessChecker`, `ReadinessDtoMapper`, `NiumiNavHost`, écrans 1, 2 et 12.

- [ ] **Écrire `AndroidDeviceReadinessCheckerTest`** ligne par ligne du tableau §13, implémenter.
- [ ] **Écrire `ReadinessViewModelTest`**, implémenter avec `NiumiCoreFacade.evaluateActivation`.
- [ ] **Écrire `OnboardingScreenTest` et `ReadinessScreenTest`**, implémenter les écrans avec TalkBack (`contentDescription` sur chaque action) et bord à bord.
- [ ] **Écrire `NiumiNavHost` et `HomeScreen`** ; brancher `SessionSnapshotPublisher` pour rediriger vers la session active.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:setup:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** refuser les notifications → blocage affiché avec demande de permission ; retirer le plein écran (Android 14+) → blocage avec raccourci réglages ; volume alarme à zéro → blocage ; Ne pas déranger → avertissement ; retour des réglages → recalcul automatique.

**Terminé quand :** les 13 contrôles sont testés, aucune redirection vers `SCHEDULE_EXACT_ALARM`, l'onboarding explique l'absence de secours logiciel avant la première activation.

### Étape 13 : association du boîtier et sélecteur d'applications

**Specs à lire :** SPEC_CORE_KMP §2 (points 11, 12), §9.2, §10 ; SPEC_ANDROID §11.1, §12.1, §14 (`<queries>`), §15 (écrans 3, 4).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/pairing/` : `RoomPairedBoxStore.kt` (implémente `PairedBoxStore` de l'étape 4 sur `PairedBoxDao` ; `replace()` supprime l'ancien boîtier).
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/apps/` : `InstalledAppsSource.kt` (interface : `suspend fun launchableApps(): List<InstalledApp>`), `PackageManagerInstalledAppsSource.kt` (`queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)`, dédoublonnage par package, exclusions : package Niumi, rôle Home via `RoleManager`/résolution `CATEGORY_HOME`, package des Réglages résolu par `ACTION_SETTINGS`, `com.android.systemui`, composeur résolu par `ACTION_DIAL`, `RoleManager.ROLE_EMERGENCY`, applications sans activité de lancement), `AppSelectionStore.kt` (DataStore : sélection courante hors session, `Set<String>` + libellés).
- Manifeste `:core:system` : `<queries>` avec `<intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent>` uniquement.
- Créer dans `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/` : `pairing/PairingScreen.kt` (Reader Mode via `NfcReader` dans une `PairingActivity` ou l'activité hôte, `parseBoxPayload` → `PairedBoxCredential.fromPayload` via la façade, confirmation « Remplacer le boîtier actuel ? » si un boîtier existe, affichage du `boxId` tronqué), `pairing/PairingViewModel.kt`, `apps/AppPickerScreen.kt` (icône, libellé, package en petit si doublon de libellé, compteur `n / 50`, confirmation bloquée à 0 et à 51), `apps/AppPickerViewModel.kt`, `SetupGate.kt` (refuse l'entrée dans `Pairing` et `AppPicker` si `SessionSnapshotPublisher` expose un état non final).
- Tests : `PackageManagerInstalledAppsSourceTest` (fake `PackageManager` par interface `PackageQuery` : exclusions une par une, dédoublonnage), `PairingViewModelTest` (payload valide → credential stocké avec hash, jamais le token ; payload invalide → message et aucun stockage ; boîtier existant → demande de confirmation), `AppPickerViewModelTest` (0 → confirmation désactivée ; 50 → activée ; 51 → refus et message), `SetupGateTest` (état `ARMED` → accès refusé ; `COMPLETED` ou `null` → autorisé), instrumenté `RoomPairedBoxStoreTest`.

**Produit :** `PairedBoxStore` définitif, `InstalledAppsSource`, `AppSelectionStore`, `SetupGate`, écrans 3 et 4.

- [ ] **Écrire `RoomPairedBoxStoreTest`**, implémenter et remplacer la liaison Hilt de `DebugPairedBoxStore` (le debug garde sa propre liaison uniquement pour `PocScreen`).
- [ ] **Écrire `PackageManagerInstalledAppsSourceTest`**, implémenter avec la section `<queries>`.
- [ ] **Écrire `PairingViewModelTest`** puis l'écran ; aucune trace du token dans les logs (assertion sur un `TechnicalEventLog` faux).
- [ ] **Écrire `AppPickerViewModelTest`** puis l'écran ; la limite 1..50 est vérifiée par `evaluateActivation`, l'écran ne la duplique que pour l'état du bouton.
- [ ] **Écrire `SetupGateTest`**, brancher la garde dans `SetupNavigation`.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :core:database:connectedDebugAndroidTest :feature:setup:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** associer un tag, ré-associer avec confirmation ; le sélecteur ne montre ni Niumi, ni le launcher, ni Réglages, ni Téléphone ; sélectionner 50 puis tenter 51.

**Terminé quand :** tests verts, `QUERY_ALL_PACKAGES` absent du manifeste fusionné (`./gradlew :app:processDebugManifest` puis grep), association et sélecteur inaccessibles pendant une session (test de garde).

### Étape 14 : choix de l'heure, récapitulatif et activation en deux phases

**Specs à lire :** SPEC_CORE_KMP §8.1, §10 ; SPEC_ANDROID §8, §9.2, §15 (écrans 5, 6, 7), §19.1 (`feature`).

**Fichiers :**
- Créer dans `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/` : `wake/WakeTimeScreen.kt` (`TimePicker` Material 3, date calculée affichée en clair avec le fuseau : « Demain, jeudi 4 septembre à 07:00 (Europe/Paris) »), `wake/WakeTimeViewModel.kt` (`computeWakeSchedule` avec `Clock` injecté et `TimeZone.currentSystemDefault().id`), `summary/SummaryScreen.kt` (date complète, heure, fuseau, applications choisies, boîtier tronqué, rappel « Seul le scan du boîtier terminera la session », bouton « Activer ma session »), `summary/SummaryViewModel.kt`, `activation/ArmSessionUseCase.kt`, `activation/ActivationFailure.kt`, `ui/SessionUiState.kt`.
- Modifier `HomeScreen`/`NiumiNavHost` : après `ARMED`, navigation vers `ActiveSession` (écran livré à l'étape 15 ; à cette étape, un écran minimal affichant l'état, l'heure et le fuseau).
- Tests : `WakeTimeViewModelTest` (07:00 saisi à 20:00 → lendemain affiché ; changement de fuseau système entre saisie et confirmation → recalcul avant activation), `ArmSessionUseCaseTest` (ordre exact des 10 points de §9.2 vérifié sur un journal d'appels ; diagnostic bloquant → refus sans aucun `dispatch` ; `count` 0 → refus ; boîtier absent → refus ; credential capturé dans `AndroidSessionExtras` au moment de `ACTIVATION_REQUESTED` ; échec du coordinateur → `ActivationFailure` avec `failureCode` remonté), `SummaryViewModelTest` (bouton inactif tant que le dernier diagnostic n'est pas vert).

**Produit :** `ArmSessionUseCase`, écrans 5, 6 et un écran 7 minimal.

- [ ] **Écrire `WakeTimeViewModelTest`**, implémenter l'écran et le ViewModel.
- [ ] **Écrire `ArmSessionUseCaseTest`**, implémenter : 1) `DeviceReadinessChecker.check()` → `evaluateActivation` ; 2) refus si bloquant ; 3) construire `ActivationRequestDto` (`WakeScheduleDto`, `AppSelectionSummaryDto(count)`) et `AndroidSessionExtras` (credential figé, packages, `ringtoneKey = "niumi_alarm"`, `vibrationEnabled`) ; 4) `SessionCoordinator.dispatch(ACTIVATION_REQUESTED)` ; 5) attendre `DispatchResult.Applied` avec état `ARMED` (le coordinateur enchaîne `ACTIVATION_SUCCEEDED` lui-même) ; 6) retourner `Success(snapshot)` ou `ActivationFailure(failureCode)`.
- [ ] **Écrire `SummaryViewModelTest`**, implémenter l'écran.
- [ ] **Brancher la navigation** : `Summary` → `ActiveSession` sur succès, message d'échec avec `failureCode` sinon.
- [ ] **Vérifier :**

```bash
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** parcours complet accueil → onboarding → diagnostic → association → sélection → heure → récapitulatif → activation ; vérifier `adb shell dumpsys alarm | grep niumi` (alarme `setAlarmClock` présente) ; tuer le processus pendant l'activation puis relancer → `SessionReconciler` reprend ou annule proprement.

**Terminé quand :** l'ordre §9.2 est prouvé par test, `FAILED` uniquement depuis `PREPARING`, une session `ARMED` est visible sur l'accueil après redémarrage de l'application.

## Phase F — Lot 3 : session active

### Étape 15 : écran de session active, blocage branché sur la persistance, modification ou annulation par scan

**Specs à lire :** SPEC_CORE_KMP §2 (points 3, 4, 11), §4 ; SPEC_ANDROID §3, §11.3, §12.2, §15 (écrans 7, 9, 11), §19.1 (`feature`).

**Fichiers :**
- Créer dans `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/active/` : `ActiveSessionScreen.kt` (date, heure et fuseau d'activation, heure recalculée dans le fuseau courant si différent, liste des applications bloquées, santé et incidents récents, bouton « Modifier ou annuler » → `ScanToModify`), `ActiveSessionViewModel.kt`, `ScanToModifyScreen.kt` (Reader Mode via `NfcReader`, texte « Scanne ton boîtier Niumi pour annuler ou modifier ta session. Tes applications resteront bloquées jusqu'au scan. », aucune autre action), `ScanToModifyViewModel.kt` (délègue à `NfcScanHandler` ; après `Accepted` et état final `CANCELLED`, navigation vers `Cancelled`), `CancelledScreen.kt` (écran 11 : « Session annulée », bouton « Préparer un nouveau réveil »).
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/blocking/` : `RoomBlockedPackagesProjection.kt` (implémente `BlockedPackagesProjection` de l'étape 5 : lit le pointeur actif, l'état, les `BlockedAppEntity` et, en `RELEASING`, le statut de l'effet `REMOVE_BLOCKING` dans l'outbox ; avant déverrouillage, lit `DirectBootStore`).
- Modifier `NiumiBlockingAccessibilityService` : injection de `RoomBlockedPackagesProjection` (Hilt `@AndroidEntryPoint` sur le service), rechargement dans `onServiceConnected` et à chaque changement de `SessionSnapshotPublisher`.
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/blocking/` : `AccessibilityServiceWatcher.kt` (à chaque `reconcile` et au retour au premier plan, si session active et service inactif → `INCIDENT_REPORTED BLOCKING_PERMISSION_REVOKED CRITICAL` une seule fois par session).
- `HandleValidNfcUseCase` n'existe pas encore (étape 18) : à cette étape, `ScanToModifyViewModel` consomme l'interface `NfcScanHandler` dont l'implémentation de production est livrée à l'étape 18. En attendant, la liaison Hilt de production pointe vers `PendingNfcScanHandler` qui retourne `Ignored` et journalise ; ce n'est pas un fake de comportement, l'écran affiche « Fonction disponible à l'étape suivante » uniquement en debug.
- Tests : `ActiveSessionViewModelTest` (affichage du fuseau courant différent du fuseau d'activation ; santé `DEGRADED` visible ; incident `CRITICAL` mis en avant), `RoomBlockedPackagesProjectionTest` (instrumenté : `ARMED` → `Active` ; `RELEASING` avec `REMOVE_BLOCKING SUCCEEDED` → `Releasing` liste vide ; `RELEASING` avec `PENDING` → `Releasing` liste pleine ; `COMPLETED` → `Inactive`), `AccessibilityServiceWatcherTest` (incident une seule fois), `ScanToModifyViewModelTest` (aucune donnée modifiée avant `CANCELLED` : le `AppSelectionStore` n'est pas touché tant que l'état n'est pas final).

**Produit :** écrans 7, 9, 11 ; `BlockedPackagesProjection` définitive ; `AccessibilityServiceWatcher`.

- [ ] **Écrire `RoomBlockedPackagesProjectionTest`**, implémenter, remplacer la projection mutable de l'étape 5 (la liaison debug POC disparaît).
- [ ] **Écrire `ActiveSessionViewModelTest`**, implémenter l'écran 7 complet.
- [ ] **Écrire `ScanToModifyViewModelTest`**, implémenter les écrans 9 et 11.
- [ ] **Écrire `AccessibilityServiceWatcherTest`**, implémenter et brancher sur `reconcile` et `ON_RESUME`.
- [ ] **Vérifier :**

```bash
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest :core:database:connectedDebugAndroidTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** session armée → ouvrir une application bloquée depuis launcher, récents, notification → accueil et overlay ; désactiver le service → incident `CRITICAL` visible sur l'écran de session ; changer le fuseau du téléphone → heure locale recalculée, instant inchangé.

**Terminé quand :** le service lit exclusivement la projection persistée, aucune modification de sélection ou de boîtier n'est possible pendant une session (garde de l'étape 13 + test), tests verts.

### Étape 16 : journal local, diagnostic d'incident et export

**Specs à lire :** SPEC_ANDROID §7.1 (incidents), §15 (écran 12), §16, §17, §18.

**Fichiers :**
- Créer dans `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/diagnostics/` : `IncidentDiagnosticScreen.kt` (incidents de la session avec gravité, `CRITICAL` en tête et explicité ; résultats du dernier `DeviceReadinessChecker` ; 200 événements techniques ; bouton « Exporter le diagnostic » → `ACTION_SEND` texte), `IncidentDiagnosticViewModel.kt`, `DiagnosticExporter.kt` (texte : modèle, version Android, version de l'application, contrôles, incidents, événements ; masque `boxId` aux 8 premiers caractères, jamais de hash ni d'identifiant matériel).
- Modifier `TechnicalEventLog` (étape 9) : ajout des champs de contexte (`deviceModel`, `androidVersion`, `appVersion`) et de la règle « `packageName` accepté uniquement pour `BLOCK_APPLIED` » (test).
- Brancher `RecordIncidentExecutor` (étape 11) sur `IncidentDao` ; brancher chaque exécuteur et le coordinateur sur `TechnicalEventLog` avec les types de §17 (`SESSION_PREPARING`, `SESSION_ARMED`, `ALARM_SCHEDULED`, `ALARM_RESCHEDULED`, `SCAN_REQUEST_NOTIFIED`, `SCAN_REQUEST_CLEARED`, `SESSION_RELEASING`, `SESSION_COMPLETED`, `SESSION_CANCELLED`, `SESSION_FAILED`, `RELEASE_PARTIAL_FAILURE`, `PROCESS_RECREATED`, `ACCESSIBILITY_DISABLED`, `EXACT_ALARM_LOST`, `MISSED_TRIGGER_WINDOW`).
- Tests : `DiagnosticExporterTest` (aucune occurrence du hash complet, du token ni d'un `boxId` complet ; 200 lignes maximum), `TechnicalEventLogTest` étendu (`packageName` refusé hors `BLOCK_APPLIED`), `IncidentDiagnosticViewModelTest` (`CRITICAL` avant `DEGRADED` avant `WARNING`).

- [ ] **Écrire `DiagnosticExporterTest`**, implémenter.
- [ ] **Étendre `TechnicalEventLogTest`**, brancher les émetteurs.
- [ ] **Écrire `IncidentDiagnosticViewModelTest`**, implémenter l'écran 12 et sa route depuis l'écran de session et l'accueil.
- [ ] **Vérifier :**

```bash
./gradlew :feature:session:testDebugUnitTest :core:database:testDebugUnitTest :core:system:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Terminé quand :** l'export ne contient aucune donnée interdite (test), chaque événement du coordinateur apparaît dans le journal, l'écran 12 est accessible.

## Phase G — Lot 4 : réveil

### Étape 17 : déclenchement via le coordinateur, reconstruction du service, états de l'écran de réveil

**Specs à lire :** SPEC_CORE_KMP §6 (effets `ALARM_FIRED`), §11.1 ; SPEC_ANDROID §10.1, §10.2, §10.4, §18.

**Fichiers :**
- Modifier `AlarmReceiver` : valide `sessionId` et `revision` contre le snapshot actif (Room si déverrouillé, sinon Direct Boot), construit `SessionEventDto(ALARM_FIRED, expectedRevision = snapshot.revision)`, appelle `SessionCoordinator.dispatch` dans `goAsync()` avec un délai maximal de 8 s ; `START_RINGING` est exécuté par `StartRingingExecutor` → `RingingController.startRinging`. Aucun démarrage direct du service depuis le receiver.
- Modifier `AlarmRingingService` : `onStartCommand` avec `intent == null` (recréation) relit le snapshot actif ; si `RINGING` → redémarre l'audio et journalise `PROCESS_RECREATED` ; si `RELEASING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou final → `stopSelf()` sans toucher l'état ; si `ARMED` → `reconcile(SERVICE_RECREATED)` puis `stopSelf()`.
- Modifier `AlarmActivity`/`AlarmScreen` : observe `SessionSnapshotPublisher` ; `RINGING` → texte §10.4 ; `TRIGGERED_AWAITING_NFC` → « L'heure de ton réveil est passée. Scanne ton boîtier Niumi pour débloquer tes applications. » sans audio ; `AWAITING_NFC` → « Le son est arrêté, mais tes applications restent bloquées. Scanne ton boîtier Niumi pour terminer la session. » ; `RELEASING` → progression de nettoyage (liste des effets `PENDING`/`SUCCEEDED`) ; état final → navigation vers `Completed` ou `Cancelled` ; `null` → fermeture.
- Créer `androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/CompletedScreen.kt` (écran 10 : « Session terminée. Tes applications sont débloquées. », bouton retour à l'accueil).
- Supprimer l'appel direct `RingingController.startRinging()` introduit dans `AlarmReceiver` à l'étape 3 ; après cette étape, seul `StartRingingExecutor` appelle cette méthode (vérifier par grep).
- Tests : `AlarmReceiverTest` (fakes : `revision` obsolète → aucun `dispatch`, journal `ALARM_RECEIVED` tout de même ; `sessionId` inconnu → refus ; nominal → `dispatch(ALARM_FIRED)`), `AlarmRingingServiceRecreationTest` (unitaire sur une classe `RingingServiceRecovery` pure : chaque état → action attendue), `AlarmScreenStateTest` étendu (mapping des cinq états vers les textes), instrumenté `AlarmChainInstrumentedTest` (session `ARMED` en base de test avec `triggerAt = now + 5 s`, alarme réelle → `RINGING` en base, service au premier plan, notification sans action).

- [ ] **Écrire `AlarmReceiverTest`**, refondre le receiver.
- [ ] **Écrire `AlarmRingingServiceRecreationTest`**, implémenter `RingingServiceRecovery` et l'appeler depuis le service.
- [ ] **Étendre `AlarmScreenStateTest`**, compléter l'écran de réveil et l'écran 10.
- [ ] **Écrire `AlarmChainInstrumentedTest`**.
- [ ] **Vérifier :**

```bash
./gradlew :feature:ringing:testDebugUnitTest :core:system:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** session réelle à +2 min, écran éteint → sonnerie, écran de réveil, état `RINGING` visible dans le diagnostic ; `adb shell am kill com.niumi.app` pendant la sonnerie → service et son repris ou incident consigné ; fermeture de l'activité et verrouillage → sonnerie maintenue.

**Terminé quand :** `RINGING` n'est écrit que par le moteur, le service se reconstruit depuis le snapshot, les cinq états ont leur texte, aucun appel direct au service hors exécuteur.

### Étape 18 : `HandleValidNfcUseCase`, libération atomique, reprise de `RELEASING` et notification d'attente de scan

**Specs à lire :** SPEC_CORE_KMP §4, §6, §10 (credential figé), §11.1, §12 ; SPEC_ANDROID §10.5, §11.2, §11.3, §18, §21.

**Fichiers :**
- Créer dans `androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/nfc/` : `HandleValidNfcUseCase.kt` (implémente `NfcScanHandler` ; remplace `PendingNfcScanHandler` et `PocNfcScanHandler` dans les liaisons Hilt de production).
- Modifier `AndroidScanRequestNotifier` (étape 11) : le tap ouvre `AlarmActivity` avec `extra mode = SCAN` ; `AlarmActivity` en mode scan n'attend aucun audio.
- Modifier `SessionReconciler` : à `PROCESS_START`, `USER_UNLOCKED` et `SERVICE_RECREATED`, si l'état est `RELEASING`, rejouer uniquement les effets `PENDING`/`FAILED` de l'outbox puis dispatcher `RELEASE_SUCCEEDED` ou laisser `RELEASING` ; si l'état est `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` et la notification absente, rejouer `PRESENT_SCAN_REQUEST`.
- Tests : `HandleValidNfcUseCaseTest` (fakes + `InMemoryPersistenceGateway`) : état non éligible (`PREPARING`, final, `null`) → `Ignored`, aucun `dispatch` ; `ARMED` avant l'heure → `reconcile(BEFORE_SCAN)` puis `VALID_NFC_SCANNED` → `RELEASING`/`CANCELLED` puis `CANCELLED` ; `ARMED` après l'heure sans alarme observée → `TRIGGER_ELAPSED` puis `VALID_NFC_SCANNED` → `COMPLETED` ; `RINGING` → `COMPLETED` ; scan vérifié contre `boxId`/`boxTokenSha256Hex` de la session : un `PairedBoxStore` modifié depuis l'activation ne change rien, un tag correspondant au nouveau boîtier est refusé → `UnknownBox` ; `MALFORMED_URI` → `Unreadable`, état inchangé, aucun effet ; preuve générée avec `eventId`, `expectedRevision = snapshot.revision`, `occurredAt = clock.now()` ; même `eventId` rejoué → `Duplicate`, aucun effet ; `REMOVE_BLOCKING` en échec → `RELEASE_FAILED`, état `RELEASING`, `Accepted` retourné (le scan est accepté, le nettoyage continue) ; service d'accessibilité déjà désactivé → `AlreadySatisfied`, incident `BLOCKING_PERMISSION_REVOKED`, `RELEASE_SUCCEEDED`. `SessionReconcilerReleasingTest` : reprise partielle sans réappliquer le blocage ; `PRESENT_SCAN_REQUEST` rejoué sans doublon visible. Instrumenté : `ScanRequestNotificationInstrumentedTest` (notification sans son, vibration, full-screen ni action ; retirée après `CLEAR_SCAN_REQUEST`).

- [ ] **Écrire `HandleValidNfcUseCaseTest`** cas par cas, implémenter en suivant les 14 points de SPEC_ANDROID §11.3 (les points 5 à 14 sont réalisés par le coordinateur et ses exécuteurs, le cas d'usage ne fait que 1 à 4 et interprète `DispatchResult`).
- [ ] **Remplacer les liaisons Hilt** (`PendingNfcScanHandler` supprimé ; `PocNfcScanHandler` retiré de la variante debug, `PocScreen` garde seulement programmation d'alarme et association).
- [ ] **Écrire `SessionReconcilerReleasingTest`**, compléter le réconciliateur.
- [ ] **Écrire `ScanRequestNotificationInstrumentedTest`**, compléter le notifier et le mode scan de `AlarmActivity`.
- [ ] **Vérifier :**

```bash
./gradlew :feature:ringing:testDebugUnitTest :core:system:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** scan du bon tag pendant la sonnerie → arrêt et déblocage en moins d'une seconde, écran « Session terminée » ; scan avant l'heure depuis « Modifier ou annuler » → « Session annulée » ; scan d'un autre tag → vibration courte, sonnerie maintenue ; couper le processus entre `RELEASING` et l'état final (`am kill` juste après le scan) puis relancer → nettoyage repris, aucun blocage réappliqué.

**Terminé quand :** chaque ligne de §11.3 est couverte par un test, `COMPLETED`/`CANCELLED` n'apparaissent qu'après `RELEASE_SUCCEEDED`, la notification d'attente de scan est publiée et retirée de façon idempotente.

## Phase H — Lot 5 : résilience

### Étape 19 : `SystemEventsReceiver`, coordinateur Direct Boot, politique de retard et fusion après déverrouillage

**Specs à lire :** SPEC_CORE_KMP §8.2, §13 ; SPEC_ANDROID §7.3, §9.3, §10.5 (Direct Boot), §19.1, §20 (scénarios redémarrage), §21.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/boot/` : `SystemEventsReceiver.kt` (`directBootAware=true`, `exported=false` avec filtres `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`, `USER_UNLOCKED` ; `goAsync()` + `reconcile(reason)`), `DirectBootMerger.kt` (à `USER_UNLOCKED` : fusionne reçus et outbox du snapshot Direct Boot dans Room par `eventId`/`effectId`, refuse une `domainRevision` Direct Boot inférieure à Room, réécrit Direct Boot depuis Room ensuite).
- Modifier `DefaultSessionCoordinator` : avant déverrouillage, tous les effets utilisent des dépendances construites sur `createDeviceProtectedStorageContext()` (`AlarmScheduler`, `ScanRequestNotifier`, `RingingController`), fournies par un `@Named("deviceProtected")` Hilt ; `TIME_CHANGED`/`TIMEZONE_CHANGED` → `INCIDENT_REPORTED` `WARNING` (`TIME_CHANGED`/`TIMEZONE_CHANGED`), alarme réenregistrée au même `triggerAtEpochMillis`.
- Manifeste `:core:system` : `SystemEventsReceiver` déclaré.
- Tests : `SystemEventsReceiverTest` (chaque action → bonne `ReconcileReason`), `DirectBootMergerTest` (reçus dédoublonnés ; effet `SUCCEEDED` en Direct Boot et `PENDING` en Room → `SUCCEEDED` ; révision inférieure refusée ; Room réécrit dans Direct Boot), `SessionReconcilerBootTest` (`LOCKED_BOOT` + `ARMED` futur → reprogrammation ; + retard 10 min → alarme immédiate ; + retard 20 min → `TRIGGER_ELAPSED` + `MISSED_TRIGGER_WINDOW`, `TRIGGERED_AWAITING_NFC`, `DEGRADED`, notification publiée depuis le contexte protégé, aucun service de sonnerie ; `TIMEZONE_CHANGED` → même instant, incident `WARNING`), instrumenté `DirectBootInstrumentedTest` (écriture Direct Boot puis lecture par un contexte protégé).

- [ ] **Écrire `SystemEventsReceiverTest`**, implémenter le receiver.
- [ ] **Écrire `SessionReconcilerBootTest`**, compléter le réconciliateur et les liaisons `deviceProtected`.
- [ ] **Écrire `DirectBootMergerTest`**, implémenter et brancher sur `USER_UNLOCKED`.
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :core:database:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** session à +10 min, redémarrage sans déverrouillage → alarme sonne à l'heure ; redémarrage à +5 min après l'heure → sonnerie immédiate ; redémarrage à +20 min → notification d'attente de scan visible avant déverrouillage, sans son ; changement manuel d'heure et de fuseau → `dumpsys alarm` montre le même instant ; mise à jour de l'APK (`adb install -r`) → alarme conservée.

**Terminé quand :** les six broadcasts sont traités, Direct Boot et Room convergent après déverrouillage (test), la fenêtre de 15 minutes est prouvée aux bornes.

### Étape 20 : mort du processus, pertes de permission, snapshot corrompu

**Specs à lire :** SPEC_ANDROID §4.2, §7.1 (incidents), §9.2 (dernier paragraphe), §18, §20 (scénarios processus et permissions).

**Fichiers :**
- Créer dans `androidApp/app/src/main/kotlin/com/niumi/app/` : `AppStartReconciler.kt` (`Application.onCreate` → `reconcile(PROCESS_START)` hors du thread principal, puis à chaque `ON_START` de l'application via `ProcessLifecycleOwner`).
- Modifier `SessionRuntimeStatusProbe` : `alarmScheduled`, `accessibilityReady`, `notificationReady`, `fullScreenReady`, `nfcReady`, `audioReady` alimentent le réconciliateur ; chaque perte après `ARMED` produit une seule fois par session l'incident correspondant (`ALARM_PERMISSION_REVOKED`, `BLOCKING_PERMISSION_REVOKED`, `ANDROID_FULL_SCREEN_REVOKED`, `NFC_DISABLED`), sans changer l'état.
- Modifier `DirectBootStore`/`RoomSessionStore` : un snapshot `Corrupted` produit `SNAPSHOT_CORRUPTED CRITICAL` dans le journal, conserve le fichier, et Room fait foi si accessible ; si Room est aussi illisible, l'application affiche l'écran de diagnostic sans retirer le blocage (le service d'accessibilité garde sa dernière projection en mémoire).
- Tests : `AppStartReconcilerTest`, `SessionRuntimeStatusProbeTest` (chaque perte → incident unique), `SessionReconcilerCorruptionTest` (Direct Boot corrompu + Room valide → Direct Boot réécrit ; les deux corrompus → aucun `dispatch`, incident journalisé, projection de blocage inchangée), instrumenté `ProcessDeathInstrumentedTest` (session `RINGING` en base, redémarrage du service via `ServiceTestRule` sans intent → audio repris).

- [ ] **Écrire `AppStartReconcilerTest`**, implémenter.
- [ ] **Écrire `SessionRuntimeStatusProbeTest`**, compléter la sonde et le réconciliateur.
- [ ] **Écrire `SessionReconcilerCorruptionTest`**, implémenter le traitement explicite de la corruption.
- [ ] **Écrire `ProcessDeathInstrumentedTest`**.
- [ ] **Vérifier :**

```bash
./gradlew :app:testDebugUnitTest :core:system:testDebugUnitTest :core:database:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** `am kill` après armement → alarme conservée, état réconcilié à la relance ; désactiver l'accessibilité pendant `ARMED` → incident `CRITICAL`, session conservée ; corrompre `niumi_session.json` à la main (`adb shell run-as` impossible en release : tester en debug) → incident, blocage conservé.

**Terminé quand :** aucune session armée ne passe à `FAILED` dans ces scénarios (tests), la corruption est explicite et non destructive, chaque perte de permission produit un incident unique.

### Étape 21 : finalisation release, suppression du POC, documentation QA et porte finale

**Specs à lire :** SPEC_ANDROID §16, §19, §20, §21, §22 (dernier paragraphe), §23 ; SPEC_CORE_KMP §19.

**Fichiers :**
- Supprimer `androidApp/app/src/debug/kotlin/com/niumi/app/poc/` entièrement, `tools/` conservé.
- Créer `androidApp/app/proguard-rules.pro` (règles Room, Hilt, kotlinx-serialization, `NiumiCore` DTO conservés), `.github/workflows/mobile.yml` (runner macOS : `jvmTest`, `linkDebugFrameworkIosSimulatorArm64`, `testDebugUnitTest`, `assembleRelease`, `ktlintCheck detekt lintRelease`).
- Créer `docs/android/QA_MATRIX.md` (tableau §20 complet, colonnes fabricant/modèle/Android/firmware/permissions/résultat/retard/logs), `docs/android/RELEASE_REPORT.md` (critères §21 cochés un par un avec preuve), `docs/android/LIMITES.md` (texte de l'aide intégrée : arrêt forcé, FGS, NFC verrouillé, absence de secours logiciel).
- Ajouter dans `:app` un écran « Aide et limites » accessible depuis l'accueil, reprenant `LIMITES.md`.
- Tests : `ReleaseHygieneTest` (`:app` unitaire : le manifeste fusionné release ne contient ni `INTERNET`, ni `SCHEDULE_EXACT_ALARM`, ni `QUERY_ALL_PACKAGES` ; aucune classe `*Poc*`, `*Fake*`, `*Debug*Store` dans le classpath release ; grep du code source `main` sans `TODO`, `FIXME`, `STOP_RINGING_ACTION`).

- [ ] **Supprimer le POC** et vérifier que `:app:assembleDebug` compile encore.
- [ ] **Écrire `ReleaseHygieneTest`**, corriger tout écart.
- [ ] **Configurer R8** et vérifier qu'une session complète fonctionne sur un APK release signé avec une clé locale non versionnée.
- [ ] **Ajouter l'écran d'aide** et `LIMITES.md`.
- [ ] **Écrire le workflow CI** ; l'exécuter localement commande par commande.
- [ ] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew ktlintCheck detekt lintRelease
./gradlew :app:assembleRelease
```

- [ ] **Remplir `QA_MATRIX.md`** sur la matrice P0 (§20) : au minimum Pixel, Samsung, Xiaomi, sur Android 14, 15, 16 et 17 si disponibles.
- [ ] **Remplir `RELEASE_REPORT.md`** : chaque critère §21 avec la preuve (test, log ou vidéo).

**Porte de validation finale :** le MVP n'est déclaré terminé qu'avec `QA_MATRIX.md` verte dans le périmètre §4.1, `RELEASE_REPORT.md` complet et les portes §23 traitées (déclarations Play, vidéo, réponse de Google). Un critère non prouvé reste ouvert dans le rapport, jamais coché par défaut.

## Recette et critères d'acceptation

Repris de SPEC_ANDROID §21 ; chaque point renvoie à l'étape qui le prouve.

- [ ] Session confirmée seulement si le diagnostic est vert (étapes 12, 14).
- [ ] `setAlarmClock()` seule API de réveil (étape 3, `ReleaseHygieneTest`).
- [ ] Alarme hors ligne, écran éteint, Doze, Android 17 `USAGE_ALARM` (étapes 3, 6, 21).
- [ ] Sonnerie maintenue après fermeture de l'activité, aucun bouton d'arrêt (étapes 3, 17).
- [ ] Seul un tag accepté par KMP avec preuve opaque produit `VALID_NFC_SCANNED` (étapes 2, 18).
- [ ] `ARMED` → `RELEASING`/`CANCELLED` ; `RINGING`/`AWAITING_NFC`/`TRIGGERED_AWAITING_NFC` → `RELEASING`/`COMPLETED` (étapes 7, 18).
- [ ] État final uniquement après `RELEASE_SUCCEEDED` ; session active jamais `FAILED` (étapes 7, 11, 20).
- [ ] Tag invalide sans effet ; fin valide en moins d'une seconde (étape 18).
- [ ] Blocage des seules applications choisies, sans lecture de contenu (étapes 5, 15).
- [ ] Plus de 50 applications refusé (étapes 8, 13).
- [ ] Redémarrage → alarme restaurée avant déverrouillage ; notification d'attente de scan sans son ni full-screen (étapes 18, 19).
- [ ] Changement d'heure ou de fuseau → même instant (étape 19).
- [ ] Aucun appel réseau, aucun `INTERNET` (étape 21).
- [ ] Lint, ktlint, detekt, tests unitaires et instrumentés verts (chaque étape, étape 21).
- [ ] Limites documentées dans l'application et le rapport QA (étapes 12, 21).
- [ ] Dossier Play AccessibilityService préparé et soumis (étape 6).

## Hypothèses et limites du plan

- Le boîtier MVP contient un tag NDEF Type 2 (NFC-A) déjà écrit avec le payload canonique ; l'écriture industrielle et l'anti-clonage sont hors périmètre.
- Les appareils physiques, le compte Play Console, les actions manuelles dans les réglages et la vidéo de revue sont à la charge de l'utilisateur.
- Le framework iOS `NiumiCore` est construit pour garantir l'interopérabilité ; l'application iOS n'est pas développée par ce plan.
- Les versions de bibliothèques sont celles vérifiées le 3 septembre 2026 ; l'étape 1 les reconfirme et peut les ajuster à condition de rester dans les plages de compatibilité citées.
- Aucun commit, push ni publication n'est effectué automatiquement.
