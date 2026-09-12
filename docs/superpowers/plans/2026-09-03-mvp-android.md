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
- Les portes de validation (0a après l'étape 6, 0b et finale après l'étape 21 — porte 0 scindée en deux le 2026-09-07, voir étape 6 et `LOT-0.md`) sont manuelles, sur appareils réels ou sur décision Play. Ne pas franchir une porte sans validation humaine explicite.
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
| Kotlin / KMP | 2.4.20 | Compatibilité officielle : Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1, Xcode 26.4. Montée depuis 2.4.10 le 2026-09-11 (étape 12a), voir « Montée de versions » ci-dessous |
| AGP | 9.1.1 | Exigé par Compose BOM 2026.08 pour compileSdk 37 ; JDK 17 ; Kotlin intégré (ne pas appliquer `org.jetbrains.kotlin.android`). Était un patch au-dessus du maximum testé par KGP 2.4.10 (9.1.0) ; **désormais bien à l'intérieur** de la matrice de KGP 2.4.20 (max 9.3.1) |
| Gradle | 9.5.0 | Était le maximum testé par KGP 2.4.10 ; KGP 2.4.20 teste jusqu'à 9.7.0, la marge est donc plus large qu'à l'étape 1 |
| Compose BOM | 2026.09.00 | Compose 1.12.1, material3 1.4.0 (résolution vérifiée). Montée depuis 2026.08.00 (Compose 1.12.0) le 2026-09-11, voir « Montée de versions » ci-dessous |
| Navigation Compose | 2.10.1 | Montée depuis 2.10.0 le 2026-09-11 |
| Room | 2.8.5 | KSP2. Montée depuis 2.8.4 le 2026-09-11 |
| DataStore | 1.2.1 | `createInDeviceProtectedStorage()` disponible |
| Hilt / androidx.hilt | 2.60.1 / 1.4.0 | Reconfirmé à l'étape 1 |
| KSP | 2.3.11 | Reconfirmé à l'étape 1 ; versionnage découplé de Kotlin depuis KSP 2.3.0 |
| kotlinx-datetime | 0.8.0 | `Instant` et `Clock` viennent de `kotlin.time` |
| kotlinx-serialization | 1.11.0 | |
| ktlint | plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, CLI 1.8.0 épinglé | Le plugin embarque 1.5.0 par défaut ; épinglé pour éviter la dérive entre patchs |
| detekt | `dev.detekt` 2.0.0-alpha.6, épinglé, bloquant | Construite contre Kotlin 2.4.x ; la dernière stable (1.23.8) embarque Kotlin 2.0.21 et échoue sur Kotlin 2.4+. Vérifiée verte sous Kotlin 2.4.20 à l'étape 12a. Voir SPEC_ANDROID §5 et `ETAPE-01.md` |

Cet écart n'existe plus depuis la montée de Kotlin 2.4.20 (étape 12a) : AGP 9.1.1 et Gradle 9.5.0 sont tous deux à l'intérieur de la matrice officielle. Si un build KMP échoue pour une raison de version, arrêter et proposer une mise à jour explicite des specs ; ne jamais réduire `compileSdk`.

### Montée de versions du 2026-09-11

`:app:lintDebug` remontait 7 `GradleDependency` : trois bibliothèques avaient publié un correctif
depuis l'épinglage du 3 septembre. Notes de version officielles consultées avant la montée
(Context7 n'avait pas encore indexé ces versions, ses données s'arrêtant à juillet 2026) :

| Montée | Contenu réel | Nouvelle exigence annoncée |
| --- | --- | --- |
| Room 2.8.4 → 2.8.5 (2026-09-09) | Un correctif : les requêtes `suspend` et les opérations de l'invalidation tracker lèvent désormais `IllegalStateException` après fermeture de la base | Aucune |
| Navigation Compose 2.10.0 → 2.10.1 (2026-09-09) | Un correctif : `NavHost` prend en compte `sizeTransform` quand il saute l'animation (évite un rognage inattendu) | Aucune |
| Compose BOM 2026.08.00 → 2026.09.00 (2026-09-09) | Compose 1.12.0 → 1.12.1 ; material3 reste 1.4.0 | Aucune |

Résolution vérifiée empiriquement (`:app:dependencies --configuration debugRuntimeClasspath`) :
`compose-bom:2026.09.00` tire bien `compose.ui/foundation/runtime:1.12.1` et `material3:1.4.0`,
`navigation-compose:2.10.1`, `room-runtime/room-ktx:2.8.5`. Batterie complète verte après montée
(tests, `:app:assembleDebug`, ktlint, detekt) et `:app:lintDebug` sans aucune remontée.

**Risque tranché à l'étape 12a — AGP et Navigation Compose : AGP reste en 9.1.1.** Les notes de
`navigation 2.10.0-alpha03` indiquent : « Updated Compose `compileSdk` to API 37. This means that a
minimum AGP version of 9.2.0 is required when using Compose. » La note d'origine supposait que
l'exigence ne s'était pas manifestée « parce que `navigation-compose` est sur le classpath sans
être compilé contre, `NiumiNavHost` n'arrivant qu'à l'étape 12 ». **Cette prémisse était fausse** :
`androidApp/app/src/main/kotlin/com/niumi/app/ui/NiumiNavHost.kt` et
`androidApp/app/src/debug/kotlin/com/niumi/app/poc/PocNavigation.kt` existent depuis l'étape 3 et
compilent contre `NavHost`, `composable` et `rememberNavController` ; le build est vert depuis,
étape 11 comprise. Aucun blocage n'a donc jamais été observé. Décision validée avec l'utilisateur
le 2026-09-11 : rester en AGP 9.1.1, et ne monter que si un build casse réellement.

### Montée de Kotlin du 2026-09-11 (étape 12a)

`:app:lintDebug` s'est mis à remonter trois `NewerVersionAvailable` sur Kotlin 2.4.10, publiées
après la montée du matin. La matrice officielle (`kotlinlang.org/docs/gradle-configure-project`)
consultée avant la montée donne, pour KGP 2.4.20 : Gradle 7.6.3–9.7.0 et AGP 8.5.2–9.3.1. La montée
**resserre** donc le risque au lieu de l'élargir — AGP 9.1.1 était un patch au-dessus de la borne de
2.4.10 (9.1.0) et se retrouve largement à l'intérieur de celle de 2.4.20. KSP reste en 2.3.11
(versionnage découplé depuis KSP 2.3.0) et detekt 2.0.0-alpha.6 reste vert. Batterie complète
rejouée après la montée : tests JVM des six modules, `:app:assembleDebug`, ktlint, detekt et
`:app:lintDebug`, tous verts.

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
    // Étape 11 : toujours true en dessous d'Android 12 (API 31), où la restriction sur les
    // alarmes exactes n'existe pas. Utilisée par SessionRuntimeStatusProbe et l'incident
    // ALARM_PERMISSION_REVOKED du réconciliateur (SPEC_ANDROID §13.1).
    fun canScheduleExact(): Boolean
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
// Écart validé à l'étape 5 : apply() prend Set<BlockedPackage> et non Set<String>. §12.2 impose
// le texte « {Nom de l'application} reste bloquée jusqu'au scan du boîtier. » ; aucun libellé ne
// circulait dans la signature d'origine. BlockedPackage (:core:database, réutilisé tel quel par
// AndroidSessionExtras à l'étape 9) fige le displayNameSnapshot à l'activation plutôt que de le
// résoudre via PackageManager au moment de l'affichage. Voir ETAPE-05.md.
interface BlockingController {
    fun apply(sessionId: String, packages: Set<BlockedPackage>): OperationResult
    fun remove(sessionId: String): OperationResult
    fun effectivePackages(): Set<String>
    fun isServiceEnabled(): Boolean
}
// ForegroundAppSource reporté à l'étape 5 : aucun consommateur de production n'existe avant le
// coordinateur (l'algorithme §12.2 décide entièrement dans onAccessibilityEvent). Voir ETAPE-05.md.

// :core:system — com.niumi.system.nfc
interface NfcReader {                                   // Reader Mode, livre l'URI brute du 1er enregistrement NDEF URI
    // onUnreadable ajouté à l'étape 4 : SPEC_ANDROID §11.2 impose un texte dédié pour un tag
    // physiquement illisible (pas de NDEF, IOException, FormatException), un cas qu'aucune
    // URI ne peut représenter. Voir ETAPE-04.md.
    fun start(activity: Activity, onUri: (String) -> Unit, onUnreadable: () -> Unit): OperationResult
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
    suspend fun receipts(sessionId: String): List<EventReceipt>      // étape 11 : miroir Direct Boot (eventReceipts, §7.3)
    suspend fun pendingEffects(sessionId: String): List<PendingEffect>
    suspend fun markEffect(effectId: String, status: EffectStatus, error: String?)
    suspend fun clearActivePointer(sessionId: String)
    suspend fun recordIncident(sessionId: String, incident: SessionIncidentDto)  // étape 11 : écrivain de RECORD_INCIDENT
}
interface DirectBootStore {
    fun read(): DirectBootSnapshot?                                   // null si absent ; DirectBootSnapshot.Corrupted si illisible
    fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult  // refuse domainRevision inférieure (même sessionId)
    fun clear()
}
// DirectBootWriteResult remplace OperationResult (étape 10) : OperationResult vit dans
// :core:system, DirectBootStore dans :core:database, et :core:system → :core:database jamais
// l'inverse (règle de dépendance §6). Voir ETAPE-10.md.
sealed interface DirectBootWriteResult {
    data object Written : DirectBootWriteResult
    data object StaleRevision : DirectBootWriteResult
    data class Failed(val reason: String) : DirectBootWriteResult
}

// :core:system — com.niumi.system.session
// `extras` étendu à l'étape 11 : StoredDecision exige androidExtras, non porté par SessionEventDto
// (boîtier figé, sonnerie, sélection d'applications). Lu uniquement pour ACTIVATION_REQUESTED ;
// absent alors, l'activation est rejetée. Pour tout autre événement la session déjà persistée
// fait foi (`freezeFrom`).
interface SessionCoordinator {
    suspend fun dispatch(event: SessionEventDto, extras: AndroidSessionExtras? = null): DispatchResult
    suspend fun reconcile(reason: ReconcileReason): ReconcileResult
}
sealed interface DispatchResult {
    data class Applied(val snapshot: SessionSnapshotDto?, val requiredEffectsSucceeded: Boolean) : DispatchResult
    data class Duplicate(val receipt: EventReceipt) : DispatchResult
    data class Rejected(val violations: List<DomainViolationDto>) : DispatchResult
}
enum class ReconcileReason { PROCESS_START, USER_UNLOCKED, LOCKED_BOOT, BOOT, PACKAGE_REPLACED, TIME_CHANGED, TIMEZONE_CHANGED, BEFORE_SCAN, SERVICE_RECREATED }

// Étape 11 : non définis par le plan avant cette étape.
sealed interface ReconcileAction {
    data class OutboxReplayed(val effectCount: Int) : ReconcileAction
    data class DecisionApplied(val dispatchResult: DispatchResult) : ReconcileAction
    data class AlarmRescheduled(val triggerAtEpochMillis: Long) : ReconcileAction
    data class IncidentDispatched(val code: String, val severity: IncidentSeverityDto) : ReconcileAction
    data object SnapshotCorrupted : ReconcileAction
    data object PointerCleared : ReconcileAction
}
data class ReconcileResult(val sessionId: String?, val actions: List<ReconcileAction>)

// Abstraction fine de NiumiCoreFacade.reduce (classe finale sans interface) : permet de prouver
// qu'un doublon strict n'appelle jamais le moteur, sans instancier de vraie façade en test.
fun interface SessionReducer {
    fun reduce(snapshot: SessionSnapshotDto?, event: SessionEventDto): SessionDecisionDto
}

// Un Corrupted Direct Boot ne doit jamais se lire « pas de session » (§13 : aucune suppression
// silencieuse du blocage à cause d'un snapshot illisible).
sealed interface LoadResult {
    data object Absent : LoadResult
    data class Present(val snapshot: SessionSnapshotDto, val extras: AndroidSessionExtras, val pendingEffects: List<PendingEffect>) : LoadResult
    data class Unreadable(val reason: String) : LoadResult
}
interface SessionPersistenceGateway {
    suspend fun load(): LoadResult
    suspend fun commit(decision: StoredDecision)
    suspend fun receipt(eventId: String): EventReceipt?
    suspend fun pendingEffects(sessionId: String): List<PendingEffect>
    suspend fun markEffect(effectId: String, status: EffectStatus, error: String?)
    suspend fun clearActive(sessionId: String)
    // Avant déverrouillage, le snapshot Direct Boot n'a pas de table d'incidents : renvoie
    // Failure("INCIDENT_DEFERRED_UNTIL_UNLOCK"), best-effort, rejoué à USER_UNLOCKED.
    suspend fun recordIncident(sessionId: String, incident: SessionIncidentDto): OperationResult
}
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
- [x] **Implémenter `AlarmReceiver`** : valide les extras, journalise `ALARM_RECEIVED` (journal en mémoire pour l'instant, Room à l'étape 9), appelle `RingingController.startRinging()`. Aucun travail long. *(`TechnicalEventLog`/`InMemoryTechnicalEventLog` avancés de l'étape 9 à l'étape 3 : le receiver en a besoin dès maintenant. Voir `ETAPE-03.md`. Correction : contrairement à ce qui était affirmé ici, l'interface a changé à l'étape 9 — `packageName` devenu `detailsJson`, conforme au libellé de l'étape 9 elle-même (ligne 472 : `log(type, sessionId?, detailsJson?)`), et `recent()` passé en `suspend`. Voir `ETAPE-09.md`.)*
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

- [x] **Écrire `NfcUriExtractorTest`**, implémenter l'extracteur pur. *(Décision validée avant l'implémentation : décodage RTD-URI maison sur un descripteur pur `NdefRecordData`, plutôt que `NdefRecord.toUri()`. Ce dernier est un stub Android non testable en JVM et normalise le schéma en minuscules, ce qui aurait fait accepter `NIUMI://` que SPEC_CORE_KMP §9.1 interdit d'accepter. Voir `ETAPE-04.md`.)*
- [x] **Implémenter `ReaderModeNfcReader`** : `availability` depuis `PackageManager.FEATURE_NFC` et `NfcAdapter.isEnabled`, `enableReaderMode(activity, callback, FLAG_READER_NFC_A, extras delay 250 ms)`, lecture `Ndef` bornée à 96 octets avant toute allocation (SPEC_ANDROID §16), `disableReaderMode` dans `stop()`.
- [x] **Écrire `PocNfcScanHandlerTest`**, implémenter `DebugPairedBoxStore` et `PocNfcScanHandler` en debug.
- [x] **Compléter `AlarmActivity`** avec le Reader Mode et les textes ; `AlarmScreenStateTest` en vert. *(Logique de scan extraite dans `AlarmNfcScanCoordinator`, TooManyFunctions detekt oblige — même motif que `SystemModule`/`AudioModule` à l'étape 3.)*
- [x] **Ajouter à `PocScreen`** un bouton « Associer ce tag » qui active le Reader Mode dans une `PocPairingActivity` debug et stocke `PairedBoxCredential.fromPayload` ; afficher le `boxId` tronqué, jamais le token.
- [x] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:ringing:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**Tests manuels :** associer un tag écrit avec un payload canonique ; alarme dans 60 s ; scan du bon tag → arrêt en moins d'une seconde ; scan d'un autre tag → vibration courte, sonnerie maintenue ; NFC désactivé → instruction, sonnerie maintenue ; téléphone verrouillé → noter si le scan fonctionne avant déverrouillage (matrice §20). *(Faits le 2026-09-05 sur Redmi 25080RABDG, Android 16 — tous concluants sauf le scan d'un second tag Niumi associé à un autre boîtier, faute de second tag disponible. Un bug trouvé et corrigé pendant la validation : `AlarmActivity` ne se fermait pas après un scan accepté. Restriction NFC-verrouillé spécifique à HyperOS confirmée, conforme à l'incertitude §4.4 ; détails dans `ETAPE-04.md`.)*

**Terminé quand :** tests verts, comportement manuel consigné avec le modèle d'appareil, aucune décision de validité prise hors de `:shared:core`. *(Atteint le 2026-09-05, à l'exception du scan d'un second boîtier — voir `ETAPE-04.md`. Pixel et Samsung restent à couvrir à la porte de validation 0, étape 6.)*

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

- [x] **Écrire `BlockingDecisionTest`** : `Inactive` → `None` ; `Active` et package non listé → `None` ; `Active` et package listé → `GoHome` ; `Releasing` avec liste effective vide → `None` ; package Niumi lui-même → `None`. Implémenter. *(Écart validé : `apply()`/`decide()` portent `BlockedPackage` et non un simple nom de package, pour transporter le libellé exigé par le texte de l'overlay — voir « Interfaces transverses » et `ETAPE-05.md`.)*
- [x] **Écrire `AccessibilityServiceStatusTest`** : chaîne contenant `com.niumi.app/com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService` → actif ; chaîne vide ou autre service → inactif. Implémenter. *(Renommé `EnabledAccessibilityServicesParserTest` : la logique de parsing pur est séparée de `AccessibilityServiceStatus`, l'interface de diagnostic — même motif « descripteurs purs » que les étapes 3-4.)*
- [x] **Implémenter le service** : `onAccessibilityEvent` lit uniquement `event.packageName`, appelle `BlockingDecision`, exécute `performGlobalAction(GLOBAL_ACTION_HOME)`, affiche l'overlay, journalise `BLOCK_APPLIED` avec le package seul ; jamais de `getRootInActiveWindow()`, jamais de lecture de texte. *(Pas d'override de `onServiceConnected` : la projection est relue à chaque événement via l'interface `BlockedPackagesProjection`, rien à « recharger » explicitement tant qu'elle reste en mémoire — limite documentée dans `ETAPE-05.md`, pas un contournement.)*
- [x] **Implémenter `AccessibilityConsentScreen`** et son test Compose.
- [x] **Compléter `PocScreen`** avec l'application factice et l'état du service.
- [x] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :feature:setup:testDebugUnitTest
./gradlew :app:assembleDebug connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

*(Fait le 2026-09-06 sur Redmi 25080RABDG, Android 16 / API 36 — tout vert, 5 tests instrumentés compris. `connectedDebugAndroidTest` lancé par module (`:feature:session`, `:feature:setup`), même restriction d'installation HyperOS qu'à l'étape 3.)*

**Tests manuels :** activer le service via la page de consentement ; ouvrir l'application factice depuis le launcher, les récents et une notification → retour à l'accueil et overlay à chaque fois ; ouvrir une application autorisée → aucun effet ; désactiver le service → le POC affiche l'état inactif. *(Partiellement faits le 2026-09-06. **Deux bugs de production trouvés et corrigés** : crash `BadTokenException` à chaque blocage — l'overlay était construit avec l'`@ApplicationContext` au lieu du contexte du service, ce qui plantait Niumi et faisait désactiver le service par Android ; et overlay retiré en quelques millisecondes par l'événement du launcher consécutif au `GLOBAL_ACTION_HOME`. **Un troisième défaut, le plus grave, s'est révélé venir de l'appareil et non du code** : l'optimisation de batterie MIUI gelait le process de Niumi en arrière-plan, ce qui suspendait la remise des événements d'accessibilité — le système déclarant toujours le service « lié », process vivant, même PID. Le blocage devenait silencieusement inopérant après environ une minute. A/B contrôlé : avec restrictions, non bloqué à T+90 s ; sans restrictions, bloqué en < 1 s. Le passage de Niumi en « Aucune restriction » suffit, aucun code modifié. **Conséquence pour les specs : §13 classe « batterie optimisée » en `WARNING` « sans blocage par défaut » — la mesure montre que ce contrôle doit devenir `BLOCKING_FOR_NIUMI_EXPERIENCE` sur ces surcouches.** Le protocole manuel a ensuite été rejoué et passe (launcher, récents, intent direct, overlay, application non listée). Détails et mesures dans `ETAPE-05.md`.)*

**Terminé quand :** tests verts, `canRetrieveWindowContent=false` vérifié dans le XML, aucun accès au contenu de fenêtre dans le code (grep `rootInActiveWindow`, `getText`, `contentDescription` sur le service vide). *(**Presque atteint** au 2026-09-06. Acquis : tests JVM et instrumentés verts, garde-fou source automatisé (`NiumiBlockingAccessibilityServiceSourceTest`) plutôt qu'un grep manuel, et `capabilities=0` vérifié au runtime via `dumpsys accessibility` — preuve que `canRetrieveWindowContent=false` s'applique. SPEC_ANDROID §12.2, §13 et §19.2 ont été mises à jour dans le même changement, et le protocole manuel a été intégralement déroulé puis rejoué par `tools/validate_blocking.sh` (7 contrôles sur 7, dont la désactivation du service pendant un blocage actif). Trois conséquences restent à traiter dans les étapes suivantes, listées dans `ETAPE-05.md` : réévaluation de l'exemption d'énergie à chaque activation (§13, `DeviceReadinessChecker`), mention dans l'aide qu'une mise à jour peut réinitialiser ce réglage (étape 6), et lecture de la projection depuis Room (étape 15). Le test de bout en bout a été tenté puis abandonné : toute instrumentation de `com.niumi.app` fait passer `accessibility_enabled` à 0 et débranche le service, qui ne se relie pas — les deux vérifications de §19.2 sont donc irréalisables par instrumentation, et §19.2 a été réécrite en conséquence. Elles sont couvertes par `tools/validate_blocking.sh`, qui contrôle chaque essai par `dumpsys` et échoue explicitement si ses préconditions manquent. Pixel et Samsung sont reportés à la campagne de bêta-test, aucun appareil de ces marques n'étant disponible.)*

### Étape 6 : dossier Google Play et porte de validation 0a

**Specs à lire :** SPEC_ANDROID §2, §4, §12.3, §20, §22 (Lot 0), §23.

**Décision validée avec l'utilisateur le 2026-09-07 (déviation de §22/§23, documentée dans les deux specs et dans `LOT-0.md`) :** le tournage de la vidéo de revue et la soumission sur piste Play sont reportés au Lot 5 (étape 21), une fois le POC supprimé et le parcours utilisateur réel livré — la politique Play sur l'AccessibilityService exige une vidéo en usage normal, que le POC de debug ne peut pas représenter, et seule la première publication d'une piste passe une revue de politique. La porte de validation 0 est donc scindée : **porte 0a** ci-dessous (dossier rédigé, matrice physique du POC verte) débloque l'étape 7 ; **porte 0b** (verdict Play sur l'AccessibilityService) est rattachée à l'étape 21. Risque assumé : investir dans l'interface complète avant le verdict de Google.

**Fichiers :**
- Créer `docs/android/play-console/ACCESSIBILITY_DECLARATION.md` (usage déclaré, données observées, finalité, texte de divulgation identique à l'écran de consentement), `docs/android/play-console/USE_EXACT_ALARM.md` (justification « réveil fonction centrale »), `docs/android/play-console/FULL_SCREEN_INTENT.md`, `docs/android/play-console/FGS_MEDIA_PLAYBACK.md`, `docs/android/play-console/PRIVACY_POLICY.md` (données consultées et conservées, aucune transmission), `docs/android/play-console/REVIEW_VIDEO_SCRIPT.md` (plan de la vidéo : divulgation, consentement, alarme, scan, blocage — tournage reporté à l'étape 21).
- Créer `docs/android/implementation-reports/LOT-0.md` : matrice d'essais à remplir (fabricant, modèle, Android, firmware, permissions, scénario, résultat, retard mesuré), décision de calendrier et préconditions de soumission encore ouvertes (identité éditeur, contact, URL politique, keystore, AAB release).

- [x] **Rédiger les six documents Play** en français, sans promesse d'incontournabilité du blocage (§23). *(Fait le 2026-09-07.)*
- [x] **Construire un APK debug** signé avec la clé debug et le déposer manuellement sur les appareils de test : `./gradlew :app:assembleDebug`. *(Fait le 2026-09-07.)*
- [x] **Exécuter la matrice POC** sur le Xiaomi disponible : volumes et Ne pas déranger, cycle de vie (récents, arrêt FGS, arrêt forcé), Doze, écran éteint 30 min. Pixel, Samsung et Android 17 restent hors périmètre, reportés à la campagne de bêta-test. *(Fait les 2026-09-07 et 08 — 13 essais, dont le Doze profond réel appareil débranché : retard nul. Quatre constats bloquants trouvés et arbitrés, specs mises à jour. Détails dans `LOT-0.md` et `ETAPE-06.md`.)*
- [ ] ~~Enregistrer la vidéo de revue~~ — reporté à l'étape 21 (porte 0b).
- [ ] ~~Soumettre sur piste interne ou fermée~~ — reporté à l'étape 21 (porte 0b).

**Porte de validation 0a : franchie le 2026-09-08** (validation écrite dans `LOT-0.md`, avec les quatre risques assumés — Pixel et Samsung non couverts, verdict Play reporté à l'étape 21, arrêt du seul FGS non testable, scan NFC verrouillé bloqué par HyperOS). L'étape 7 est ouverte. Règle d'origine : ne pas commencer l'étape 7 sans validation humaine écrite dans `LOT-0.md` couvrant la campagne d'essais ci-dessus. Une incompatibilité OEM ou un échec du scan verrouillé qui remet en cause le produit exige une révision explicite des specs avant de continuer. La porte 0b (verdict Play) reste ouverte et est vérifiée à l'étape 21.

## Phase C — Lot 0.5 : moteur KMP complet

### Étape 7 : machine à états `SessionEngine`

**Specs à lire :** SPEC_CORE_KMP §2, §4, §5, §6, §7, §12, §17 (Machine à états et Politiques).

**Fichiers :**
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/domain/` : `SessionState.kt`, `ReleaseTarget.kt`, `SessionHealth.kt`, `IncidentSeverity.kt`, `Platform.kt`, `SessionIncident.kt`, `WakeSchedule.kt`, `AppSelectionSummary.kt`, `ActivationRequest.kt`, `SessionSnapshot.kt`, `SessionEventKind.kt`, `SessionEvent.kt`, `SessionEffectKind.kt`, `SessionEffect.kt`, `SessionEffectPayload.kt`, `DomainViolation.kt` (`code: String`, `message: String`), `ViolationCode.kt` (les 13 codes de §7.3), `IncidentCodes.kt` (codes communs et gravités par défaut de §7.3), `SessionDecision.kt`, `SessionEngine.kt`, `EffectIdFactory.kt`.
- Créer dans `shared/core/src/commonTest/kotlin/com/niumi/core/domain/` : `SessionFixtures.kt` (constructeurs de snapshots par état, événements valides, `WakeSchedule` de référence avec `triggerAtEpochMillis = 1_800_000_000_000`), `SessionEngineActivationTest.kt`, `SessionEngineTriggerTest.kt`, `SessionEngineNfcTest.kt`, `SessionEngineReleaseTest.kt`, `SessionEngineIncidentTest.kt`, `SessionEngineValidationTest.kt`, `SessionEngineEffectsTest.kt`, `SessionEngineForbiddenTransitionsTest.kt` (table exhaustive états × événements).

**Interface produite :** exactement les signatures de SPEC_CORE_KMP §5, §6, §7 (`SessionEngine.reduce(snapshot, event): SessionDecision`). La façade et les DTO arrivent à l'étape 8.

- [x] **Écrire les types du domaine** en copiant les blocs Kotlin des §5, §6 et §7. *(Écart validé : aucune validation en `init` — le plan demandait à la fois une validation dans `init` et des violations produites par `reduce` sans exception, deux exigences incompatibles vu que §14 interdit toute exception traversant la frontière native. La validation vit dans `SessionEventValidation.validate()`, appelée en tête de `reduce()`. `CanonicalUuid` (extrait de `nfc.BoxPayloadParser`, non dupliqué) porte la règle de format UUID commune à `boxId`, `eventId` et `sessionId`. Voir `ETAPE-07.md`.)*
- [x] **Écrire `SessionEngineActivationTest`** : `null` + `ACTIVATION_REQUESTED` → `PREPARING`, revision 1, `health HEALTHY`, effets `PUBLISH_PLATFORM_SNAPSHOT`, `SCHEDULE_ALARM`, `APPLY_BLOCKING` dans cet ordre ; snapshot non nul (final ou non) + `ACTIVATION_REQUESTED` → `INVALID_STATE_TRANSITION` ; `count` 0 ou 51 → `INVALID_APP_SELECTION`, 1 et 50 acceptés ; `PREPARING` + `ACTIVATION_SUCCEEDED` → `ARMED`, `armedAt` renseigné, effet `PUBLISH` ; `PREPARING` + `ACTIVATION_FAILED` avec `failureCode` → `FAILED`, effets `CANCEL_ALARM`, `REMOVE_BLOCKING`, `PUBLISH`, `CLEAR_ACTIVE_SESSION` ; sans `failureCode` → `MISSING_FAILURE_CODE` ; `ARMED` + `ACTIVATION_FAILED` → `INVALID_STATE_TRANSITION`. *(`INVALID_APP_SELECTION` : 14e code de violation, absent de §7.3 d'origine — décision validée avec l'utilisateur le 2026-09-08, spec mise à jour dans le même changement.)*
- [x] **Implémenter `reduce()` pour l'activation** ; test vert.
- [x] **Écrire `SessionEngineTriggerTest`** : `ARMED` + `ALARM_FIRED` → `RINGING`, effets `PUBLISH`, `START_RINGING` ; `ARMED` + `TRIGGER_ELAPSED` à `triggerAt` → `TRIGGERED_AWAITING_NFC`, effets `PUBLISH`, `PRESENT_SCAN_REQUEST` ; même chose après l'heure avec incident `MISSED_TRIGGER_WINDOW` joint → effet `RECORD_INCIDENT` avec `IncidentEffectPayload` et `health DEGRADED` ; avant l'heure → `TRIGGER_NOT_REACHED` ; `ARMED`/`RINGING` + `ALARM_SOUND_STOPPED` → `AWAITING_NFC`, effets `PUBLISH`, `PRESENT_SCAN_REQUEST` ; `TRIGGERED_AWAITING_NFC` + `ALARM_SOUND_STOPPED` → état inchangé, revision incrémentée, `alarmSoundStoppedAt` renseigné ; `ALARM_FIRED` dans un état final → violation. *(Le calcul du retard de 15 minutes reste hors périmètre : ce réducteur accepte tel quel l'incident déjà joint par le coordinateur natif — `TriggerDelayPolicy` arrive à l'étape 8.)*
- [x] **Implémenter**, test vert.
- [x] **Écrire `SessionEngineNfcTest`** : `ARMED` avant l'heure + `VALID_NFC_SCANNED` avec preuve cohérente → `RELEASING`, `releaseTarget CANCELLED`, `nfcVerifiedAt` renseigné, effets `PUBLISH`, `CANCEL_ALARM`, `STOP_RINGING`, `CLEAR_SCAN_REQUEST`, `REMOVE_BLOCKING` ; `ARMED` à l'heure ou après → `TRIGGER_ALREADY_ELAPSED` ; `RINGING`/`AWAITING_NFC`/`TRIGGERED_AWAITING_NFC` → `RELEASING` avec `COMPLETED` ; preuve absente → `MISSING_NFC_PROOF` ; preuve dont `sessionId`, `eventId`, `expectedRevision` ou `verifiedAtEpochMillis` diffèrent de l'événement → `UNEXPECTED_EVENT_PAYLOAD` ; `INVALID_NFC_SCANNED` → état inchangé, aucun effet hors `PUBLISH` ; `VALID_NFC_SCANNED` dans `PREPARING` ou un état final → `INVALID_STATE_TRANSITION`.
- [x] **Implémenter** ; la preuve est construite dans les tests via une fonction `internal` de test située dans `commonTest` (même module, donc accès `internal`) — comparaison champ par champ dans le réducteur, jamais par `==` sur la preuve (`NfcVerificationProof` n'a volontairement pas d'`equals` structurel, étape 2).
- [x] **Écrire `SessionEngineReleaseTest`** : `RELEASING` + `RELEASE_FAILED` avec incident → `RELEASING`, `health DEGRADED`, effets `RECORD_INCIDENT`, `PUBLISH` ; sans incident → `MISSING_INCIDENT` ; `RELEASING` + `RELEASE_SUCCEEDED` → cible enregistrée avec `completedAt` ou `cancelledAt`, effets `PUBLISH`, `CLEAR_ACTIVE_SESSION` ; `RELEASE_SUCCEEDED` hors `RELEASING` → violation ; `RELEASING` sans `nfcVerifiedAt` (snapshot forgé) → violation `INVALID_STATE_TRANSITION`.
- [x] **Implémenter**, test vert.
- [x] **Écrire `SessionEngineIncidentTest`** : `INCIDENT_REPORTED` `WARNING` → `health` inchangé ; `DEGRADED` ou `CRITICAL` → `DEGRADED` ; un second incident `WARNING` après `DEGRADED` ne remonte pas à `HEALTHY` ; incident sur un état final → violation ; `ARMED` + `ACTIVATION_FAILED` reste refusé (jamais `FAILED` après armement). *(Effets `RECORD_INCIDENT` puis `PUBLISH` : ligne ajoutée à la table §6, absente à l'origine — décision validée avec l'utilisateur le 2026-09-08, même ordre que `RELEASE_FAILED` pour que l'incident reste rejouable depuis l'outbox après interruption, §17.)*
- [x] **Écrire `SessionEngineValidationTest`** : `expectedRevision` absent hors activation → `STALE_REVISION` ; `expectedRevision` ≠ `snapshot.revision` → `STALE_REVISION` ; `sessionId` différent → `UNKNOWN_SESSION` ; identifiant non canonique → `INVALID_IDENTIFIER` ; horodatage ≤ 0 → `INVALID_TIMESTAMP` ; `activationRequest` fourni hors `ACTIVATION_REQUESTED` → `UNEXPECTED_EVENT_PAYLOAD` ; `incident` fourni sur `ALARM_FIRED` → `UNEXPECTED_EVENT_PAYLOAD` ; une décision refusée renvoie `snapshot` inchangé et `effects` vide.
- [x] **Écrire `SessionEngineEffectsTest`** : `effectId == "$sessionId:$revision:$kind:$ordinal"` ; deux appels identiques produisent les mêmes `effectId` ; `payload` nul sauf `RECORD_INCIDENT`.
- [x] **Écrire `SessionEngineForbiddenTransitionsTest`** : pour chaque paire (état, kind) non listée en §5.1, `reduce` renvoie au moins une violation et aucun effet. *(« état actif » de §5.2 pour `INCIDENT_REPORTED` et `INVALID_NFC_SCANNED`, non précisé par la spec, interprété comme tout état non final — `PREPARING` et `RELEASING` compris. Décision d'implémentation documentée dans `ETAPE-07.md`.)*
- [x] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew ktlintCheck detekt
```

*(Fait le 2026-09-08 — 105 tests JVM verts dont 52 nouveaux pour l'étape 7, ktlint et detekt verts sur tout le dépôt, `:shared:core:compileKotlinIosSimulatorArm64` vert (aucune API JVM n'a fui dans `commonMain`). Détails dans `ETAPE-07.md`.)*

**Terminé quand :** chaque ligne de SPEC_CORE_KMP §5.1 et §5.2 a un test nommé, tous verts, `reduce` sans horloge ni aléa. *(Atteint le 2026-09-08. `SessionEngineForbiddenTransitionsTest` couvre exhaustivement les 9 états × 11 événements plus la ligne « aucune session » contre la table §5.1. Vérifié par grep : aucune horloge, aucun aléa, aucun `require`/`check`/`throw` de validation dans `commonMain/domain` — seuls des `requireNotNull` défensifs subsistent dans les réducteurs sur des champs déjà garantis présents par `SessionEventValidation`, jamais atteignables avec une entrée invalide via l'API publique.)*

### Étape 8 : politique horaire, politique d'activation, façade complète, fixtures et framework iOS

**Specs à lire :** SPEC_CORE_KMP §8, §7.4, §13 (règles partagées), §14, §16, §17 (Date et heure, Politiques), §19.

**Fichiers :**
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/schedule/` : `WakeScheduleInput.kt` (`localTimeIso`, `zoneId`, `nowEpochMillis`), `WakeScheduleCalculator.kt`, `WakeScheduleResult.kt` (`Success(schedule)`, `Failure(code)` avec `INVALID_TIME`, `UNKNOWN_ZONE`), `TriggerDelayPolicy.kt` (`evaluate(triggerAt, nowEpochMillis): TriggerDelayOutcome` avec `NOT_REACHED`, `FIRE_NOW` ≤ 15 min, `MISSED` > 15 min).
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/diagnostics/` : `ReadinessSeverity.kt`, `ActivationPolicy.kt` (`evaluate(input): ActivationPolicyResult` : refus si un contrôle `BLOCKING_*` échoue, si `count` hors 1..50, si `triggerAt ≤ now`, si aucun boîtier), `ActivationPolicyInput.kt`, `ActivationPolicyResult.kt` (`allowed`, `blockingReasons`, `warnings`).
- Créer dans `shared/core/src/commonMain/kotlin/com/niumi/core/interop/` : `SessionDtos.kt` (miroirs `*Dto` de tous les types de §5 à §7, enums réexportés à l'identique), `DtoMappers.kt`, `NiumiCoreFacade.kt` (les cinq méthodes de §14).
- Créer `shared/core/src/commonTest/resources/fixtures/wake_schedules.json`, `session_transitions.json` (état source, événement, état attendu, effets attendus) et `shared/core/src/jvmTest/.../FixturesTest.kt` ; tests `WakeScheduleCalculatorTest`, `TriggerDelayPolicyTest`, `ActivationPolicyTest`, `NiumiCoreFacadeTest`, `DtoRoundTripTest`.

**Interface produite :** `NiumiCoreFacade` complète (§14) plus une sixième méthode `evaluateTriggerDelay(input: TriggerDelayInputDto): TriggerDelayResultDto` (`triggerAtEpochMillis`, `nowEpochMillis` → `NOT_REACHED`, `FIRE_NOW`, `MISSED`). Cette méthode étend §14 : l'ajouter à SPEC_CORE_KMP §14 dans le même changement, avec la mention qu'iOS peut l'ignorer (§8.2). Les adaptateurs Android n'importent jamais `com.niumi.core.domain` directement, seulement `com.niumi.core.interop`.

- [x] **Écrire `WakeScheduleCalculatorTest`** avec `Europe/Paris` : 22:00 saisi à 20:00 → même jour ; 07:00 saisi à 20:00 → lendemain ; 07:00 saisi à 07:00 exact → lendemain (strictement futur) ; 02:30 le 29 mars 2026 (heure inexistante) → 03:00 ; 02:30 le 25 octobre 2026 (heure répétée) → première occurrence (UTC+2) ; `zoneId` inconnu → `UNKNOWN_ZONE` ; `localTimeIso` `25:00` → `INVALID_TIME` ; le résultat conserve `localDateIso`, `localTimeIso`, `zoneIdAtActivation`. *(8 tests, tous les horodatages vérifiés indépendamment contre `java.time` avant écriture.)*
- [x] **Implémenter avec `kotlinx-datetime`.** *(Correction du texte ci-dessus : `toInstant(TimeZone)` n'applique la règle « premier instant valide / première occurrence » que pour le chevauchement d'automne (vérifié par round-trip : reconvertir l'instant obtenu redonne le même `LocalDateTime`) — pas pour le trou de printemps, où il décale l'heure locale de la taille du saut (`02:30` → `03:30` un jour de passage à l'heure d'été, au lieu de `03:00`). `kotlinx-datetime` 0.8.0 n'expose pas encore l'API `TransitionHandler` du dépôt amont. `WakeScheduleCalculator` détecte l'écart par ce même round-trip et résout le trou par bissection sur `toLocalDateTime`, sans dépendre de la politique de trou de `toInstant`. Voir `ETAPE-08.md`.)*
- [x] **Écrire `TriggerDelayPolicyTest`** : retard −1 s → `NOT_REACHED` ; 0, 15 min exactement → `FIRE_NOW` ; 15 min + 1 ms → `MISSED`. Implémenter.
- [x] **Écrire `ActivationPolicyTest`** : tout vert → `allowed` ; un `BLOCKING_FOR_ALARM` → refus avec raison ; un `BLOCKING_FOR_NIUMI_EXPERIENCE` → refus ; `WARNING` seul → autorisé avec avertissement ; `count` 0/51 → refus ; `triggerAt ≤ now` → refus ; boîtier absent → refus. Implémenter. *(11 tests, dont les bornes 1 et 50 acceptées et le cumul de plusieurs refus simultanés.)*
- [x] **Écrire les DTO et mappers** ; `DtoRoundTripTest` vérifie `snapshot → dto → snapshot` égal pour chaque état, et que `SessionEventDto` transporte `NfcVerificationProof` par référence sans le sérialiser (`@Transient`, aucun champ `proof` dans le JSON). *(Écart validé avec l'utilisateur le 2026-09-08 : les enums du domaine sont ré-exportés par `typealias` plutôt que dupliqués en DTO — satisfait à la fois « enums réexportés à l'identique » et « les adaptateurs Android n'importent jamais `com.niumi.core.domain` », sans mapping ni risque de divergence. Les mappers, initialement prévus dans un seul `DtoMappers.kt`, sont répartis sur quatre fichiers (`DtoMappers.kt`, `SessionSnapshotEventMappers.kt`, `SessionEffectDecisionMappers.kt`, `PolicyDtoMappers.kt`) pour rester sous le seuil detekt `TooManyFunctions` — 22 fonctions au total. Voir `ETAPE-08.md`.)*
- [x] **Écrire `NiumiCoreFacadeTest`** : `reduce` renvoie un `SessionDecisionDto` avec violations typées (jamais d'exception, y compris sur un `SessionEventDto` incohérent) ; `computeWakeSchedule` et `evaluateActivation` délèguent ; `verifyBox` inchangé depuis l'étape 2.
- [x] **Écrire les fixtures JSON** (au moins 30 transitions et 8 horaires) et `FixturesTest`. *(33 transitions, 8 horaires. `VALID_NFC_SCANNED` volontairement absent de `session_transitions.json` : sa preuve n'a ni forme sérialisable ni constructeur public, donc rien de représentable en JSON ne pourrait circuler jusqu'au moteur sans en fabriquer une artificiellement — déjà couvert intégralement par `SessionEngineNfcTest`, commonTest, étape 7. Voir `ETAPE-08.md`.)*
- [x] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64 :shared:core:linkReleaseFrameworkIosArm64
./gradlew ktlintCheck detekt
```

**Terminé quand :** les cinq méthodes de §14 existent, aucune n'est `suspend`, aucune ne lance, les fixtures sont rejouées, le framework `NiumiCore` se lie (ou l'absence de Xcode est consignée). *(Fait le 2026-09-09 — 160 tests JVM verts dont 55 nouveaux pour l'étape 8, `linkDebugFrameworkIosSimulatorArm64` et `linkReleaseFrameworkIosArm64` verts (Xcode 26.6 disponible), ktlint et detekt verts sur tout le dépôt. Les six méthodes de §14 existent (`evaluateTriggerDelay` en sixième, ajoutée au contrat dans le même changement). Détails, écarts et décisions validées dans `docs/android/implementation-reports/ETAPE-08.md`.)*

## Phase D — Lot 1 : persistance et coordination Android

### Étape 9 : Room — entités, DAO, transaction de décision, journal et mappings KMP

**Specs à lire :** SPEC_CORE_KMP §6.1, §13 ; SPEC_ANDROID §7.2, §16, §17, §19.1 (`core:system` mappings), §19.2 (migrations).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/` : `NiumiDatabase.kt` (`version = 1`, `exportSchema = true`, schéma dans `androidApp/core/database/schemas/`), `entity/AlarmSessionEntity.kt`, `entity/BlockedAppEntity.kt`, `entity/PairedBoxEntity.kt`, `entity/TechnicalEventEntity.kt`, `entity/SessionIncidentEntity.kt`, `entity/SessionEventReceiptEntity.kt` (`eventId` PK, `sessionId`, `payloadSha256Hex`, `appliedRevision`, `receivedAtEpochMillis`), `entity/SessionEffectOutboxEntity.kt` (`effectId` PK, `sessionId`, `revision`, `kind`, `ordinal`, `payloadJson?`, `status` ∈ `PENDING|SUCCEEDED|FAILED|SATISFIED`, `lastError?`, `updatedAtEpochMillis`), `entity/ActiveSessionPointerEntity.kt` (`id = 1`, `sessionId`), `converter/EnumConverters.kt`, `dao/SessionDao.kt`, `dao/PairedBoxDao.kt`, `dao/TechnicalEventDao.kt`, `dao/IncidentDao.kt`, `dao/ReceiptDao.kt`, `dao/OutboxDao.kt`, `SessionStore.kt` (interface des « Interfaces transverses »), `RoomSessionStore.kt`, `mapping/SessionSnapshotMapper.kt` (`AlarmSessionEntity ↔ SessionSnapshotDto` + `AndroidSessionExtras(boxId, boxTokenSha256Hex, ringtoneKey, vibrationEnabled, blockedPackages)`), `mapping/EventFingerprint.kt` (SHA-256 hex du JSON canonique sans `eventId` ni preuve), `TechnicalEventLog.kt` (interface `log(type, sessionId?, detailsJson?)` + impl Room avec purge au-delà de 200), `di/DatabaseModule.kt`.
- Tests unitaires : `SessionSnapshotMapperTest` (aller-retour pour chaque état, `null` préservés), `EventFingerprintTest` (même événement → même empreinte ; `eventId` différent → même empreinte ; `occurredAt` différent → empreinte différente). Tests instrumentés : `RoomSessionStoreTest` (base en mémoire : `commitDecision` écrit session, apps, reçu, effets dans une transaction ; une exception au milieu ne laisse rien), `TechnicalEventLogTest` (201 insertions → 200 lignes, la plus ancienne supprimée), `MigrationTest` (`MigrationTestHelper`, schéma v1 exporté).

**Produit :** `SessionStore`, `TechnicalEventLog`, `PairedBoxDao`, `SessionSnapshotMapper`, `EventFingerprint`.

- [x] **Écrire `SessionSnapshotMapperTest`**, créer les entités (colonnes exactes de §7.2, `boxTokenSha256Hex` figé) et le mapper. *(9 états couverts + un test dédié aux horodatages tous distincts pour attraper une inversion de colonnes. `AndroidSessionExtras`/`EventReceipt`/`PendingEffect`/`SessionStore` repris mot pour mot des « Interfaces transverses ».)*
- [x] **Écrire `EventFingerprintTest`**, implémenter avec `kotlinx-serialization` et `java.security.MessageDigest`. *(Écart au texte ci-dessus : clés triées récursivement plutôt que « par déclaration » — une réorganisation cosmétique du DTO ne doit pas changer les empreintes déjà écrites en base. Valeur dorée figée par un test. Voir `ETAPE-09.md`.)*
- [x] **Écrire les DAO et `RoomSessionStore`** ; `activeSession()` lit le pointeur puis la session, ses apps, ses effets. *(Écart : `commitDecision` utilise `database.withTransaction {}` plutôt que `@Transaction` sur un DAO — un `@Transaction` ne peut appeler que les méthodes de son propre DAO, ce qui aurait imposé un DAO unique pour cinq tables. Écart : `pendingEffects`/`activeSession` rejouent `PENDING` **et** `FAILED`, pas `PENDING` seul — sinon un effet requis resté `FAILED` après interruption ne serait jamais rejoué (SPEC_CORE_KMP §6.1). 8 DAO au lieu de 6 (un par table, seuil detekt `TooManyFunctions`). Voir `ETAPE-09.md`.)*
- [x] **Implémenter `TechnicalEventLog`** avec une liste blanche des types de §17. *(`TechnicalEventType` complété : `ALARM_MUTED_BY_DND` et `SESSION_READINESS_DEGRADED` manquaient depuis l'étape 3. `RoomTechnicalEventLog` filtre `detailsJson` par une liste blanche de clés (`TechnicalEventDetails`) plutôt que par un test « type inconnu refusé », impossible à écrire sur un enum fermé — substitut : `entries` contient exactement les 26 libellés de §17. `log()` reste synchrone, non bloquant, sur un `CoroutineScope` injecté ; `recent()` passe en `suspend`. **Non branché dans `LoggingModule`** à cette étape : `AlarmReceiver`/`AlarmRingingService`/`AlarmActivity` sont `directBootAware`, et SPEC_ANDROID §7.3 interdit qu'un tel composant crée Room avant déverrouillage — le câblage attend la garde `ROOM_BEFORE_UNLOCK` de l'étape 10. Voir `ETAPE-09.md`.)*
- [x] **Écrire les tests instrumentés** et le schéma exporté. *(Schéma v1 exporté sans sous-dossier de variante (`schemas/com.niumi.database.NiumiDatabase/1.json`), committé. `ExportedSchemaTest` (JVM) fait le contrôle principal ; `NiumiDatabaseSchemaTest` n'apporte rien de plus en v1 sans migration à exercer — et impose l'API `SupportSQLiteDatabase`, le constructeur `(Instrumentation, Class)` de `MigrationTestHelper` ne fournissant pas de `SQLiteDriver`. 19 tests instrumentés verts sur Xiaomi 25080RABDG / Android 16. Le premier passage a fait échouer 6 tests et révélé un défaut réel — l'ordre d'écriture du journal n'était pas garanti (corrigé par un `Mutex` et un tri chronologique), plus trois hypothèses de test fausses. Voir `ETAPE-09.md`.)*
- [x] **Vérifier :**

```bash
./gradlew :core:database:testDebugUnitTest
./gradlew :shared:core:jvmTest :feature:ringing:testDebugUnitTest :feature:session:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :core:database:connectedDebugAndroidTest   # appareil ou émulateur branché
```

**Terminé quand :** aller-retour Room ↔ `SessionSnapshotDto` prouvé pour les neuf états, transaction unique prouvée, journal borné à 200, schéma v1 versionné. *(Fait le 2026-09-09 — 54 tests JVM `:core:database` + 160 `:shared:core` verts, 19 tests instrumentés verts sur Xiaomi 25080RABDG / Android 16 (API 36), ktlint/detekt/lint verts sur tout le dépôt, `:app:assembleDebug` vert. Transaction unique prouvée par deux points d'échec distincts. Détails, écarts et découvertes des tests sur appareil dans `docs/android/implementation-reports/ETAPE-09.md`.)*

### Étape 10 : snapshot Direct Boot

**Specs à lire :** SPEC_CORE_KMP §13 ; SPEC_ANDROID §7.3, §9.2 (paragraphe Room / Direct Boot), §19.1 et §19.2 (Direct Boot).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/directboot/` : `DirectBootSnapshot.kt` (`@Serializable`, champs exacts de SPEC_ANDROID §7.3 avec `projectionSchemaVersion = 1`, `domainSchemaVersion = 1`, `domainRevision`, listes `eventReceipts` et `pendingEffects` avec payload sérialisé ; variante `Corrupted(reason)`), `DirectBootStore.kt` (interface), `FileDirectBootStore.kt` (`context.createDeviceProtectedStorageContext().filesDir/niumi_session.json`, écriture via `AtomicFile`, lecture tolérante retournant `Corrupted` sans exception), `DirectBootMapper.kt` (`DirectBootSnapshot ↔ (SessionSnapshotDto, AndroidSessionExtras, receipts, effects)`), `UnlockState.kt` (`interface UnlockState { val isUserUnlocked: Boolean }`, impl `UserManager`).
- Tests unitaires : `DirectBootMapperTest` (aller-retour complet, `SessionSnapshotDto` reconstruit valide pour `reduce`), `DirectBootSnapshotJsonTest` (sérialisation stable, champ inconnu ignoré, JSON tronqué → `Corrupted`). Tests instrumentés : `FileDirectBootStoreTest` (écriture puis lecture ; `domainRevision` inférieure refusée avec `Failure("STALE_REVISION")` ; égale acceptée ; fichier corrompu → `Corrupted` et aucun effacement automatique ; `clear()`).

**Produit :** `DirectBootStore`, `DirectBootMapper`, `UnlockState`.

- [x] **Écrire `DirectBootSnapshotJsonTest`** et le type sérialisable. *(Types de projection dédiés — `DirectBootBlockedPackage`, `DirectBootReceipt`, `DirectBootEffect` — plutôt que les types de production, mêmes raisons que le `@SerialName` de l'étape 9 ; JSON doré épinglé a révélé qu'un `Json` par défaut (`encodeDefaults = false`) omet silencieusement `projectionSchemaVersion` (valeur par défaut) — corrigé par un `Json` dédié au format persisté, `encodeDefaults = true`. Voir `ETAPE-10.md`.)*
- [x] **Écrire `DirectBootMapperTest`**, implémenter le mapper (réutilise `SessionSnapshotMapper` de l'étape 9 pour les champs communs). *(Écart : ne partage pas de code avec `SessionSnapshotMapper` — deux mappers indépendants vers deux formats physiques distincts, leur cohérence prouvée par `DirectBootRoomParityTest` plutôt que par du partage. `DirectBootReduceTest` prouve explicitement l'exigence du « Terminé quand ». Voir `ETAPE-10.md`.)*
- [x] **Écrire `FileDirectBootStoreTest`**, implémenter `FileDirectBootStore` avec `AtomicFile.startWrite()/finishWrite()` et comparaison de révision avant écriture. *(Politique de révision extraite en une fonction pure `decideWrite`, testée en JVM (`DirectBootWriteDecisionTest`) — même motif que `AlarmPendingIntentSpecs`/`RingingNotificationSpecs`. `DirectBootWriteResult` remplace `OperationResult` des « Interfaces transverses » (`:core:system → :core:database` jamais l'inverse) : écart 4, plan MVP corrigé. Garde de révision scopée par `sessionId` : écart 6. 10 tests, vérifiés sur appareil réel — voir `ETAPE-10.md`.)*
- [x] **Ajouter dans `DatabaseModule`** une règle Hilt : `NiumiDatabase` est fourni via `Lazy`/`Provider` et `RoomSessionStore` vérifie `UnlockState.isUserUnlocked` avant tout accès, sinon `IllegalStateException("ROOM_BEFORE_UNLOCK")` (testé unitairement avec un `UnlockState` faux). *(La garde vit dans `RoomSessionStore` lui-même, pas dans `DatabaseModule` : `RoomSessionStore(databaseProvider: Provider<NiumiDatabase>, unlockState: UnlockState)` lève avant même d'appeler `databaseProvider.get()`, prouvé par `RoomSessionStoreUnlockGuardTest` avec un provider qui échoue s'il est sollicité. `UnlockAwareTechnicalEventLog` branche en plus `RoomTechnicalEventLog`, écrit à l'étape 9 mais jamais utilisé jusqu'ici — décision validée avec l'utilisateur. Voir `ETAPE-10.md`.)*
- [x] **Vérifier :**

```bash
./gradlew :core:database:testDebugUnitTest :core:database:connectedDebugAndroidTest
./gradlew ktlintCheck detekt :app:lintDebug
```

**Terminé quand :** un snapshot Direct Boot actif se convertit en `SessionSnapshotDto` accepté par `NiumiCoreFacade.reduce` (test explicite), écriture atomique, révision protégée, corruption explicite. *(Fait le 2026-09-10 — 90 tests JVM `:core:database` (36 nouveaux) verts, non-régression sur `:shared:core` (160), `:core:system` (56), `:feature:ringing` (21), `:feature:session` (1), `:app` (6) ; **29 tests instrumentés verts sur Xiaomi 25080RABDG / Android 16 (API 36)**, dont les 10 nouveaux de `FileDirectBootStoreTest` ; ktlint/detekt/lint verts sur tout le dépôt, `:app:assembleDebug` vert. Détails dans `docs/android/implementation-reports/ETAPE-10.md`.)*

### Étape 11 : `SessionCoordinator`, registre idempotent, outbox, exécution des effets et réconciliateur

**Specs à lire :** SPEC_CORE_KMP §4, §6, §6.1, §10, §12, §13 ; SPEC_ANDROID §7.1 (`SessionRuntimeStatus`), §9.2, §11.3, §18.

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/session/` : `SessionCoordinator.kt`, `DispatchResult.kt`, `ReconcileReason.kt`, `ReconcileResult.kt`, `DefaultSessionCoordinator.kt`, `SessionPersistenceGateway.kt` (interface : `load()`, `commit(decision)`, `receipt(eventId)`, `pendingEffects()`, `markEffect()`, `clearActive()`, `recordIncident()`), `UnlockAwarePersistenceGateway.kt` (Room si déverrouillé, Direct Boot sinon ; miroir Direct Boot après chaque commit Room), `EffectExecutor.kt` (interface `execute(effect, snapshot, extras): ExecutionOutcome`, un couple résultat + incident optionnel), `EffectDispatcher.kt` (table `SessionEffectKind → EffectExecutor`, ordre de §6, distinction requis / best-effort), `executors/PublishSnapshotExecutor.kt`, `executors/ScheduleAlarmExecutor.kt`, `executors/CancelAlarmExecutor.kt`, `executors/ApplyBlockingExecutor.kt`, `executors/RemoveBlockingExecutor.kt`, `executors/StartRingingExecutor.kt`, `executors/StopRingingExecutor.kt`, `executors/PresentScanRequestExecutor.kt`, `executors/ClearScanRequestExecutor.kt`, `executors/ClearActiveSessionExecutor.kt`, `executors/RecordIncidentExecutor.kt`, `SessionRuntimeStatus.kt`, `SessionRuntimeStatusProbe.kt`, `SessionReconciler.kt`, `ReconcilerSources.kt`, `PhaseCompletion.kt` (décide `ACTIVATION_SUCCEEDED`/`FAILED` et `RELEASE_SUCCEEDED`/`FAILED` selon les effets requis), `SessionSnapshotPublisher.kt` (`StateFlow<SessionSnapshotDto?>` pour l'UI), `SessionEventFactory.kt`, `SessionReducer.kt`, `SessionStates.kt`, `di/SessionModule.kt`, `di/EffectExecutorModule.kt`.
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/notification/` : `ScanRequestNotifier.kt`, `ScanRequestNotificationSpecs.kt`, `ScanRequestPendingIntentSpecs.kt`, `AndroidScanRequestNotifier.kt` (canal `niumi_session_awaiting_scan`, titre « Ton réveil Niumi est passé », texte « Scanne ton boîtier pour débloquer tes applications. », `ongoing`, sans son, vibration, full-screen ni action ; tap → `AlarmActivity` en mode scan ; fonctionne avec un `DeviceProtectedStorageContext`), `NotificationAvailability.kt`, `di/ScanRequestModule.kt`.
- Tests unitaires avec fakes (`FakeAlarmScheduler`, `FakeBlockingController`, `FakeRingingController`, `FakeScanRequestNotifier`, `InMemoryPersistenceGateway`, `TestCoordinatorHarness`) : `SessionCoordinatorActivationTest`, `SessionCoordinatorIdempotenceTest`, `SessionCoordinatorReleaseTest`, `SessionCoordinatorOutboxReplayTest`, `EffectDispatcherTest`, `PhaseCompletionTest`, `SessionReconcilerTest`, `ScanRequestNotificationSpecsTest`, `ScanRequestPendingIntentSpecsTest`, `SessionCoordinatorMutexTest` (deux `dispatch` concurrents du même événement sont sérialisés). Test instrumenté : `AndroidScanRequestNotifierInstrumentedTest`.

**Produit :** `SessionCoordinator` complet, `EffectDispatcher`, `SessionReconciler`, `SessionSnapshotPublisher`, `ScanRequestNotifier`. Consommé par toutes les étapes suivantes.

- [x] **Écrire `SessionCoordinatorIdempotenceTest`** : même `eventId` + même empreinte → `Duplicate`, `reduce` non appelé (compteur d'appels), aucun effet ; même `eventId` + empreinte différente → `Rejected(EVENT_ID_CONFLICT)` ; événement d'une autre session → `Rejected(UNKNOWN_SESSION)`. Implémenter le registre dans `DefaultSessionCoordinator.dispatchLocked` avant l'appel au réducteur. *(Écart : réducteur espionné par comptage d'appels — `RecordingSessionReducer` enrobant la vraie `NiumiCoreFacade` — plutôt qu'une façade mockée : le comportement de domaine réel reste exercé dans tous les scénarios.)*
- [x] **Écrire `SessionCoordinatorActivationTest`** : `ACTIVATION_REQUESTED` → persistance de `PREPARING` + reçu + 3 effets **avant** tout appel aux fakes (ordre vérifié par journal d'appels), puis `SCHEDULE_ALARM` et `APPLY_BLOCKING` exécutés, puis `ACTIVATION_SUCCEEDED` auto-dispatché → `ARMED` ; si `SCHEDULE_ALARM` échoue → `CANCEL_ALARM`, `REMOVE_BLOCKING` puis `ACTIVATION_FAILED` avec `failureCode = "ANDROID_ALARM_SCHEDULE_FAILED"` → `FAILED` et pointeur effacé ; `PUBLISH_PLATFORM_SNAPSHOT` en échec n'empêche pas `ACTIVATION_SUCCEEDED`. *(Écart : `"ANDROID_ALARM_SCHEDULE_FAILED"`, pas `"ALARM_SCHEDULE_FAILED"` — convention de préfixe d'`IncidentCodes` (§14), déjà utilisée par `SessionFixtures`/`FixturesTest`/`RoomSessionStoreEffectsTest` de `:shared:core`. `failureCode` est un `String` libre non validé par le moteur : la valeur exacte vient du code d'erreur du composant Android en échec, jamais codée en dur côté coordinateur.)*
- [x] **Implémenter `EffectDispatcher`, `PhaseCompletion` et les exécuteurs** ; `AlreadySatisfied` compte comme succès et journalise un incident `CRITICAL` uniquement pour `REMOVE_BLOCKING` quand le service est désactivé (`BLOCKING_PERMISSION_REVOKED`).
- [x] **Écrire `SessionCoordinatorReleaseTest`** : `VALID_NFC_SCANNED` → `RELEASING` persisté, puis `CANCEL_ALARM`, `STOP_RINGING`, `CLEAR_SCAN_REQUEST`, `REMOVE_BLOCKING` ; succès des requis → `RELEASE_SUCCEEDED` → état final + `CLEAR_ACTIVE_SESSION` ; échec de `REMOVE_BLOCKING` avec précondition tenue → `RELEASE_FAILED` avec `RELEASE_PARTIAL_FAILURE`, état `RELEASING`, effet conservé `FAILED` dans l'outbox. **Décision validée le 2026-09-10 (écart au texte ci-dessus) : `STOP_RINGING` est désormais requis pour `RELEASE_SUCCEEDED`**, pas best-effort — SPEC_CORE_KMP §6 et SPEC_ANDROID §11.3 se contredisaient (§11.3 corrigée dans le même changement, voir `ETAPE-11.md`) ; un échec isolé de `STOP_RINGING` produit donc `RELEASE_FAILED`, pas `RELEASE_SUCCEEDED`, testé explicitement.
- [x] **Écrire `SessionCoordinatorOutboxReplayTest`** : outbox avec `CANCEL_ALARM SUCCEEDED` et `REMOVE_BLOCKING PENDING` → `reconcile(PROCESS_START)` n'exécute que `REMOVE_BLOCKING`, jamais `APPLY_BLOCKING` ; `RECORD_INCIDENT PENDING` rejoué avec son payload ; `PRESENT_SCAN_REQUEST` rejoué deux fois → notifier appelé, résultat identique à chaque fois.
- [x] **Écrire `SessionReconcilerTest`** : `PREPARING` incomplet avec alarme déjà programmée et blocage appliqué → reprise vers `ARMED` ; `PREPARING` avec alarme absente → rollback vers `FAILED` ; `ARMED` avec `isScheduled() == false` → `ALARM_RESCHEDULED` puis alarme reprogrammée au même `triggerAt` ; `ARMED` et `TriggerDelayPolicy.MISSED` → `TRIGGER_ELAPSED` + `MISSED_TRIGGER_WINDOW` ; `ARMED` et `FIRE_NOW` avec `reason != BEFORE_SCAN` → alarme immédiate reprogrammée ; `ARMED` et `FIRE_NOW` avec `BEFORE_SCAN` → `TRIGGER_ELAPSED` sans incident ; service d'accessibilité inactif pendant `ARMED` → `INCIDENT_REPORTED BLOCKING_PERMISSION_REVOKED CRITICAL`, état conservé ; `canScheduleExactAlarms == false` → `ALARM_PERMISSION_REVOKED`.
- [x] **Implémenter `SessionRuntimeStatusProbe` et `SessionReconciler`** ; la politique de retard est lue via `NiumiCoreFacade.evaluateTriggerDelay()` (ajoutée à l'étape 8), jamais recalculée côté Android. *(Écart de détekt : le constructeur de `SessionReconciler` regroupe `AlarmScheduler`/`AccessibilityServiceStatus`/`BlockedPackagesProjection` dans un petit porteur `ReconcilerSources`, et lit l'horloge via `SessionEventFactory.nowEpochMillis()` plutôt qu'une dépendance `Clock` séparée — `LongParameterList` (6 paramètres) sinon dépassé. `detekt.yml` gagne en plus une exemption `LongParameterList: ignoreAnnotated: ['Provides']`, même motif que l'exemption Compose déjà présente : un `@Provides` Hilt prend un paramètre par dépendance distincte, regrouper ces paramètres en objets artificiels rien que pour la fonction de câblage n'améliorerait pas la lisibilité.)*
- [x] **Écrire `SessionCoordinatorMutexTest`**, garantir le `Mutex` unique partagé par `dispatch` et `reconcile`. *(Écart : pas de Turbine — deux `dispatch` concurrents du même événement sous un `SessionPersistenceGateway` qui force un point de suspension réel dans `commit()` ; sans le mutex, la seconde coroutine passerait la vérification de doublon avant que la première n'ait committé. Turbine reste réservé à l'observation de `Flow`, non nécessaire ici.)*
- [x] **Implémenter `AndroidScanRequestNotifier`** et son test. *(Test JVM du spec pur + test instrumenté de la vraie `Notification`, même répartition que `RingingNotificationSpecs`/`RingingNotificationFactory`. Écart : `ScanRequestPendingIntentSpecs` ajouté — non listé dans le plan — avec un code de requête distinct de `AlarmPendingIntentSpecs.fullScreen` pour ne jamais partager l'identité d'un `PendingIntent` entre la notification de sonnerie et celle d'attente de scan.)*
- [x] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest
./gradlew :shared:core:jvmTest
./gradlew ktlintCheck detekt :app:lintDebug
```

*(Fait le 2026-09-10 — 91 tests JVM `:core:system` verts dont 35 nouveaux à cette étape (`session` et `notification`), non-régression `:core:database` (90), `:shared:core` (160), `:feature:ringing` (21), `:feature:session` (1), `:app` (6) ; `:app:assembleDebug` vert (graphe Hilt fermé, y compris après le retrait de `PocFacadeModule`, devenu redondant avec `SessionModule.provideNiumiCoreFacade`) ; ktlint, detekt et `:app:lintDebug` verts sur tout le dépôt — les 7 `GradleDependency` que lint remontait ont été levées par la montée de versions du 2026-09-11 (Room 2.8.5, Navigation Compose 2.10.1, Compose BOM 2026.09.00, voir « Montée de versions » plus haut). **41 tests instrumentés verts sur Xiaomi 25080RABDG / Android 16 (API 36)** le 2026-09-11, dont 12 nouveaux. Ce passage sur appareil a révélé un défaut de production datant de l'étape 9 : `SessionDao.upsert` était un `@Insert(onConflict = REPLACE)`, or `INSERT OR REPLACE` est un DELETE suivi d'un INSERT en SQLite — il déclenchait les `ForeignKey(onDelete = CASCADE)` des cinq tables enfants et effaçait le registre d'idempotence, les effets non rejoués et les incidents **à chaque transition d'état**. Corrigé par `@Upsert` (aucun changement de schéma), régression couverte par `RoomSessionStoreHistoryTest`. Détails dans `docs/android/implementation-reports/ETAPE-11.md`.)*

**Terminé quand :** tous les scénarios ci-dessus sont verts, aucun exécuteur n'écrit un `SessionState`, la persistance précède toujours l'exécution (test d'ordre), la spec §14 documente `evaluateTriggerDelay`. *(Fait le 2026-09-10, sous réserve des validations sur appareil réel listées ci-dessus.)*

## Phase E — Lot 2 : configuration

### Étape 12 : onboarding, `DeviceReadinessChecker` et écran de diagnostic

**Scindée en deux passes le 2026-09-11, décision validée avec l'utilisateur.** Cinq livrables
lourds tenaient dans une seule étape. **12a** (`:core:system`) : les quatorze contrôles de §13, le
mapper vers la politique commune, le canal `niumi_session_warning` et la surveillance de §13.1.
**12b** (`:feature:setup`, `:app`) : onboarding, écran de diagnostic, navigation typée et accueil.
Un rapport et une batterie de vérifications par passe (`ETAPE-12A.md`, `ETAPE-12B.md`).

**Deux corrections au texte d'origine ci-dessous.** Le « Terminé quand » parle de **13** contrôles :
le tableau de §13 en compte **14** depuis la scission du mode Ne pas déranger à l'étape 6. Et
« Produit : … écrans 1, 2 et 12 » est faux pour l'écran 12 : le diagnostic d'incident est livré à
l'**étape 16**, l'étape 12 n'en crée que la route. Enfin, `NiumiNavHost.kt` et `HomeScreen.kt` ne
sont pas à créer : ils existent depuis l'étape 3 dans `com.niumi.app.ui`, bâtis sur
`NavGraphContributor` (routes en chaînes contribuées par les modules). L'étape 12b les déplace vers
`com.niumi.app.navigation` et les réécrit en routes typées `@Serializable`, décision validée avec
l'utilisateur ; le contributeur ne survit que pour la route POC de debug, supprimée à l'étape 21.

**Specs à lire :** SPEC_ANDROID §3 (dernier point), §4.5, §10.3 (Android 14), §12.3, §13, §15 (écrans 1, 2, 12) ; SPEC_CORE_KMP §7.3 (`ReadinessSeverity`).

**Fichiers :**
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/readiness/` : `ReadinessCheckId.kt` (les 14 contrôles de §13 dans l'ordre du tableau — le mode Ne pas déranger en compte deux depuis la mesure de l'étape 6 : silence total `BLOCKING_FOR_ALARM`, autres modes `WARNING`), `ReadinessAction.kt` (`OpenNfcSettings`, `StartPairing`, `OpenAppPicker`, `ShowExactAlarmDiagnostic`, `OpenFullScreenIntentSettings`, `RequestNotificationPermission`, `OpenChannelSettings(channelId)`, `OpenSoundSettings`, `OpenDndSettings`, `OpenAccessibilitySettings`, `FixTime`, `OpenBatterySettings`, `Unsupported`), `ReadinessCheck.kt`, `DeviceReadinessChecker.kt`, `AndroidDeviceReadinessChecker.kt` (sources injectées : `NfcAvailability`, `PairedBoxStore`, sélection courante, `AlarmManager.canScheduleExactAlarms()`, `NotificationManager.canUseFullScreenIntent()` si ≥ 34, `POST_NOTIFICATIONS` si ≥ 33, état du canal `niumi_alarm_ringing`, `AudioManager.getStreamVolume(STREAM_ALARM)`, `NotificationManager.currentInterruptionFilter`, `AccessibilityServiceStatus`, `PowerManager.isIgnoringBatteryOptimizations`), `ReadinessDtoMapper.kt` (→ `ActivationPolicyInputDto`).
- Créer dans `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/` : `onboarding/OnboardingScreen.kt` (pages : promesse, limites §4.2 et §4.5 incluant arrêt forcé, NFC verrouillé, désactivation de l'accessibilité, absence de secours logiciel ; case « J'ai compris » obligatoire, stockée dans DataStore `onboarding_acknowledged`), `readiness/ReadinessScreen.kt` (une seule action principale, le premier blocage d'abord, messages de §13 mot pour mot, recalcul dans `ON_RESUME`), `readiness/ReadinessViewModel.kt`, `SetupNavigation.kt`.
- Créer dans `androidApp/app/src/main/kotlin/com/niumi/app/navigation/` : `NiumiNavHost.kt` (routes typées `Home`, `Onboarding`, `Readiness`, `AccessibilityConsent`, `Pairing`, `AppPicker`, `WakeTime`, `Summary`, `ActiveSession`, `ScanToModify`, `Completed`, `Cancelled`, `IncidentDiagnostic`), `HomeScreen.kt` (sans session : bouton « Préparer mon réveil » ; avec session : redirection vers `ActiveSession`). Cette redirection est une **garantie d'accès au scan** inscrite en SPEC_ANDROID §10.4 depuis l'étape 6, pas un simple confort : ouvrir Niumi pendant une session active ne doit jamais mener à l'accueil, la mesure ayant montré qu'un déverrouillage détruit `AlarmActivity` et y ramène l'utilisateur pendant que l'alarme sonne.
- Tests : `AndroidDeviceReadinessCheckerTest` (fakes pour chaque source ; chaque contrôle passe/échoue avec la bonne sévérité et la bonne action ; `canScheduleExactAlarms == false` → `BLOCKING_FOR_ALARM` avec `ShowExactAlarmDiagnostic`, jamais une action vers « Alarmes et rappels » ; plein écran sous Android 13 → contrôle non applicable), `ReadinessViewModelTest` (ordre : premier blocage affiché d'abord ; `WARNING` seul → activation permise via `evaluateActivation`), `OnboardingScreenTest` (les quatre limites sont affichées, bouton inactif sans case cochée), `ReadinessScreenTest` (une seule action visible).

**Produit :** `DeviceReadinessChecker`, `ReadinessDtoMapper`, `NiumiNavHost`, écrans 1, 2 et 12.

- [x] **Écrire `AndroidDeviceReadinessCheckerTest`** ligne par ligne du tableau §13, implémenter. Inclure le cas mesuré à l'étape 6 : `currentInterruptionFilter == INTERRUPTION_FILTER_NONE` → `BLOCKING_FOR_ALARM` avec `OpenDndSettings` ; les autres filtres → `WARNING`. *(12a. Quatorze contrôles, pas treize. Trois écarts documentés dans SPEC_ANDROID §13 et `ETAPE-12A.md` : issue à trois valeurs `PASSED`/`FAILED`/`NOT_APPLICABLE` plutôt qu'un booléen ; les trois contrôles de parcours routés vers les champs dédiés d'`ActivationPolicyInputDto` au lieu de sa liste `checks` ; contrôle d'énergie satisfait par la confirmation de l'utilisateur, `isIgnoringBatteryOptimizations()` ne choisissant que le recours proposé.)*
- [x] **Écrire `ReadinessViewModelTest`**, implémenter avec `NiumiCoreFacade.evaluateActivation`. *(12b. Le ViewModel ne décide rien : il rejoue `DeviceReadinessChecker`, convertit par `toActivationPolicyInput()` et laisse la façade trancher — le test passe par la **vraie** `NiumiCoreFacade`, jamais par une politique simulée. Écart : les neuf messages manquants de §13 ont été rédigés et ajoutés à la spec dans le même changement (`ReadinessMessages`, 14 contrôles, exhaustivité prouvée sur `entries`). `ReadinessActionIntents` a été ajouté — non prévu au plan — parce que §13 interdit à `:core:system` de construire des `Intent` ; un test instrumenté énumère les quatorze actions et prouve qu'aucune ne produit `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` ni `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.)*
- [x] **Implémenter la surveillance de session** (SPEC_ANDROID §13.1, ajoutée à l'étape 6) : `DeviceReadinessChecker` réexécuté à chaque réconciliation, à chaque passage au premier plan, sur `ACTION_INTERRUPTION_FILTER_CHANGED` (receiver enregistré à chaud) et au déclenchement ; tout contrôle bloquant devenu faux pendant `ARMED` produit l'incident correspondant (`ANDROID_ALARM_MUTED_BY_DND`, `ANDROID_ALARM_VOLUME_ZERO`, `ANDROID_NOTIFICATIONS_REVOKED`, `ANDROID_FULL_SCREEN_REVOKED`, ou les codes communs `BLOCKING_PERMISSION_REVOKED` / `ALARM_PERMISSION_REVOKED`), une notification du canal `niumi_session_warning` et l'événement `SESSION_READINESS_DEGRADED`. Ne jamais utiliser l'`AccessibilityService` comme sentinelle (§13.1). Tests : chaque contrôle bloquant qui bascule pendant `ARMED` → un incident et une notification, une seule fois tant que l'état ne change pas. *(12a. `SessionReadinessMonitor` remplace les deux contrôles ad hoc que `SessionReconciler` portait depuis l'étape 11 ; `AccessibilityServiceStatus` sort de `ReconcilerSources` au profit du moniteur. Le déclencheur « au déclenchement, avant la sonnerie » reste à l'étape 17, et `MainActivity.ON_RESUME` à la passe 12b.)*
- [x] **Écrire `OnboardingScreenTest` et `ReadinessScreenTest`**, implémenter les écrans avec TalkBack (`contentDescription` sur chaque action) et bord à bord. *(12b. Décision validée : ces deux tests sont **instrumentés** (`androidTest`), comme `AccessibilityConsentScreenTest` — aucun test Compose ne tourne en JVM dans ce dépôt et Robolectric n'y est pas introduit. La logique testable sans rendu — textes, ordre des contrôles, action unique — reste couverte en JVM par des objets purs. Décision validée le 2026-09-11 : **onboarding en page unique défilante**, et non un pager — tout le contenu passe devant l'utilisateur dans l'ordre, y compris sous TalkBack, et la case reste l'unique passage. Six points et non quatre : §13 et §13.1 en ajoutent deux, la réinitialisation possible de l'exemption d'énergie par une mise à jour et le caractère jamais immédiat de l'avertissement.)*
- [x] **Écrire `NiumiNavHost` et `HomeScreen`** ; brancher `SessionSnapshotPublisher` pour rediriger vers la session active. *(12b. Déplacés de `com.niumi.app.ui` vers `com.niumi.app.navigation` et réécrits en routes typées `@Serializable` ; le paquet `ui` est supprimé. Les treize destinations sont déclarées, **quatre seulement sont enregistrées** — celles dont l'écran existe. Écart validé avec l'utilisateur : la redirection vers `ActiveSession` est livrée comme fonction pure testée (`homeDestinationFor`) et consommée par `HomeViewModel`, mais le NavHost n'y navigue pas, l'écran 7 arrivant à l'étape 15 ; aucune session ne peut être armée avant l'étape 14, le cas ne peut donc pas se produire d'ici là. Deux dépendances annoncées par le plan se sont révélées inutiles à `:feature:setup` : `navigation.compose` (le module n'a pas de `NavHost`, les routes vivent dans `:app`) et `datastore.preferences` (`SetupPreferences` est une interface de `:core:system` injectée par Hilt). Seule `activity.compose` est ajoutée, pour le lanceur de `POST_NOTIFICATIONS`. `SetupNavigation.kt` n'existe pas : un module `feature` ne peut pas dépendre de `:app` (§6), il expose des lambdas.)*
- [ ] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :feature:setup:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

*(12b faite le 2026-09-11 — 37 tests JVM `:feature:setup` (29 nouveaux) et 11 `:app` (5 nouveaux) verts, non-régression sur `:core:system` (134), `:shared:core` (160), `:core:database` (90), `:feature:ringing` (21), `:feature:session` (1) ; `:app:assembleDebug`, les deux `compileDebugAndroidTestKotlin`, ktlint, detekt et `:app:lintDebug` verts. `detekt.yml` gagne une exemption `CyclomaticComplexMethod: ignoreSingleWhenExpression` : une fonction dont le corps est un seul `when` exhaustif sur une énumération est une table de correspondance, et la remplacer par une `Map` ferait perdre l'exhaustivité vérifiée à la compilation. Détails dans `docs/android/implementation-reports/ETAPE-12B.md`.)*

**Tests manuels :** refuser les notifications → blocage affiché avec demande de permission ; retirer le plein écran (Android 14+) → blocage avec raccourci réglages ; volume alarme à zéro → blocage ; Ne pas déranger → avertissement ; retour des réglages → recalcul automatique.

**Terminé quand :** les **14** contrôles sont testés, aucune redirection vers `SCHEDULE_EXACT_ALARM`, l'onboarding explique l'absence de secours logiciel avant la première activation. *(12a atteinte le 2026-09-11 : 134 tests JVM `:core:system` verts dont 43 nouveaux, non-régression sur les cinq autres modules, `:app:assembleDebug`, ktlint, detekt et `:app:lintDebug` verts. 12b atteinte le 2026-09-11 : les quatorze contrôles portent chacun leur message de §13, l'onboarding énonce l'absence de secours logiciel avant la première activation, et aucune action ne redirige vers « Alarmes et rappels » — prouvé en énumérant les quatorze actions. **Validé sur appareil le 2026-09-11 (Xiaomi 25080RABDG, Android 16) : 69 tests instrumentés verts (17 `:feature:setup`, 10 `:core:system`, 37 `:core:database`, 5 `:feature:ringing`) et protocole manuel de §13 déroulé essai par essai.** Ce passage a révélé un défaut d'affichage qu'aucun test ne pouvait attraper — la liste montrait le message de remédiation à côté d'un contrôle satisfait, donc l'inverse de la vérité (§15) — corrigé et couvert par trois tests de régression. Deux points ne sont pas observables avant l'étape 13 : la confirmation de l'exemption d'énergie, et le blocage par volume d'alarme, `STREAM_ALARM` ayant `Min: 1` sur cet appareil. Détails dans `ETAPE-12B.md`.)*

### Étape 13 : association du boîtier et sélecteur d'applications

**Deux défauts trouvés sur appareil (2026-09-11) et corrigés dans le même changement**, décrits
dans les tests manuels ci-dessous : écrans 3 et 4 inatteignables une fois leurs contrôles au vert
(§13 gagne une exception pour les étapes de parcours) et message de scan périmé survivant à la
coupure du NFC (§15).

**Six écarts constatés à l'exécution (2026-09-11), détaillés dans `ETAPE-13.md`.** Les trois
premiers touchent le contrat et ont été répercutés dans les specs : `RoleManager` est inutilisable
pour les exclusions de §12.1 (`getRoleHolders()` est `@SystemApi`), l'application d'urgence n'a
aucune API publique et n'est exclue qu'à travers le composeur par défaut (§12.1) ;
`RoomPairedBoxStore.current()` ne lève pas avant déverrouillage, sans quoi la réconciliation
Direct Boot échouerait (§7.3) ; la section `<queries>` est inscrite en §14. Les trois autres sont
internes : `SetupNavigation.kt` n'existe pas (un module `feature` ne peut pas dépendre de `:app`,
constat de l'étape 12b) — `SetupGate` est une fonction pure appliquée par les `Route` ; le POC de
debug garde son dépôt sous un qualificatif plutôt qu'en classe concrète, pour ne pas priver
`PocNfcScanHandlerTest` de son faux dépôt ; `SESSION_FINAL_STATES` devient public dans
`:core:system`, l'ensemble étant déjà dupliqué dans `:app`.

**Correction au texte d'origine ci-dessous.** `PairedBoxStore` vivait dans `:core:system` depuis
l'étape 4 ; l'interface est **déplacée** dans `:core:database` (package `com.niumi.database.pairing`),
faute de quoi `RoomPairedBoxStore` y serait inaccessible (règle de dépendance §6). Elle y rejoint
`SessionStore`, `DirectBootStore` et `TechnicalEventLog`.

**Specs à lire :** SPEC_CORE_KMP §2 (points 11, 12), §9.2, §10 ; SPEC_ANDROID §11.1, §12.1, §14 (`<queries>`), §15 (écrans 3, 4).

**Fichiers :**
- Créer dans `androidApp/core/database/src/main/kotlin/com/niumi/database/pairing/` : `RoomPairedBoxStore.kt` (implémente `PairedBoxStore`, déplacée ici depuis `:core:system`, sur `PairedBoxDao` ; `replace()` supprime l'ancien boîtier).
- Créer dans `androidApp/core/system/src/main/kotlin/com/niumi/system/apps/` : `InstalledAppsSource.kt` (interface : `suspend fun launchableApps(): List<InstalledApp>`), `PackageManagerInstalledAppsSource.kt` (`queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER)`, dédoublonnage par package, exclusions : package Niumi, rôle Home via `RoleManager`/résolution `CATEGORY_HOME`, package des Réglages résolu par `ACTION_SETTINGS`, `com.android.systemui`, composeur résolu par `ACTION_DIAL`, `RoleManager.ROLE_EMERGENCY`, applications sans activité de lancement), `AppSelectionStore.kt` (DataStore : sélection courante hors session, `Set<String>` + libellés).
- Manifeste `:core:system` : `<queries>` avec `<intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent>` uniquement.
- Créer dans `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/` : `pairing/PairingScreen.kt` (Reader Mode via `NfcReader` dans une `PairingActivity` ou l'activité hôte, `parseBoxPayload` → `PairedBoxCredential.fromPayload` via la façade, confirmation « Remplacer le boîtier actuel ? » si un boîtier existe, affichage du `boxId` tronqué), `pairing/PairingViewModel.kt`, `apps/AppPickerScreen.kt` (icône, libellé, package en petit si doublon de libellé, compteur `n / 50`, confirmation bloquée à 0 et à 51), `apps/AppPickerViewModel.kt`, `SetupGate.kt` (refuse l'entrée dans `Pairing` et `AppPicker` si `SessionSnapshotPublisher` expose un état non final).
- Tests : `PackageManagerInstalledAppsSourceTest` (fake `PackageManager` par interface `PackageQuery` : exclusions une par une, dédoublonnage), `PairingViewModelTest` (payload valide → credential stocké avec hash, jamais le token ; payload invalide → message et aucun stockage ; boîtier existant → demande de confirmation), `AppPickerViewModelTest` (0 → confirmation désactivée ; 50 → activée ; 51 → refus et message), `SetupGateTest` (état `ARMED` → accès refusé ; `COMPLETED` ou `null` → autorisé), instrumenté `RoomPairedBoxStoreTest`.

**Produit :** `PairedBoxStore` définitif, `InstalledAppsSource`, `AppSelectionStore`, `SetupGate`, écrans 3 et 4.

- [x] **Écrire `RoomPairedBoxStoreTest`**, implémenter et remplacer la liaison Hilt de `DebugPairedBoxStore` (le debug garde sa propre liaison uniquement pour `PocScreen`). *(Interface déplacée dans `:core:database`. `PairedBoxDao` gagne `current()` et `deleteAll()` — sans changement de schéma, donc sans migration ; `replace()` fait `DELETE` puis `INSERT` en une transaction, `OnConflictStrategy.REPLACE` ne couvrant que le remplacement d'un même `boxId`. Le POC garde `DebugPairedBoxStore` sous le qualificatif `@PocPairedBoxStore`. `ReadinessSources.pairedBoxStore` cesse d'être `Optional`.)*
- [x] **Écrire `PackageManagerInstalledAppsSourceTest`**, implémenter avec la section `<queries>`. *(8 tests, une exclusion par test. `PackageQuery` isole la traduction Android ; `<queries>` déclarée dans le manifeste de `:core:system`, retirée du manifeste debug de `:app`. Écart `RoleManager` documenté ci-dessus.)*
- [x] **Écrire `PairingViewModelTest`** puis l'écran ; aucune trace du token dans les logs (assertion sur un `TechnicalEventLog` faux). *(11 tests. `RecordingTechnicalEventLog` conserve le `detailsJson`, seul moyen de prouver §16 sur le contenu écrit et pas seulement sur le type. Reader Mode branché dans `PairingRoute` (ON_RESUME/ON_PAUSE), `enableReaderMode()` exigeant une `Activity`.)*
- [x] **Écrire `AppPickerViewModelTest`** puis l'écran ; la limite 1..50 est vérifiée par `evaluateActivation`, l'écran ne la duplique que pour l'état du bouton. *(11 tests. Les bornes viennent de `AppSelectionSummary` partout, jamais de littéraux. La sélection courante est persistée en JSON dans DataStore, avec les libellés figés exigés par §12.2 — `kotlinx-serialization-json` ajouté à `:core:system`, accord utilisateur.)*
- [x] **Écrire `SetupGateTest`**, brancher la garde dans `SetupNavigation`. *(5 tests, exhaustifs sur `SessionStateDto.entries`. `SetupNavigation` n'existe pas : la garde est branchée dans `PairingRoute` et `AppPickerRoute`, alimentée par `SessionSnapshotPublisher`. Non observable avant l'étape 14, aucune session ne pouvant être armée.)*
- [x] **Vérifier :**

```bash
./gradlew :core:system:testDebugUnitTest :core:database:connectedDebugAndroidTest :feature:setup:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

*(Faite le 2026-09-11 — 145 tests JVM `:core:system` (+12 nouveaux, −1 devenu sans objet), 68 `:feature:setup` (+31), 93 `:core:database` (+3), non-régression sur `:app` (11), `:shared:core` (160), `:feature:ringing` (21), `:feature:session` (1) ; `:app:assembleDebug`, compilation `androidTest` des trois modules, ktlint, detekt et `:app:lintDebug` verts. Aucune règle detekt assouplie : les quatre remontées ont été corrigées sur le fond — qualificatif `@IoDispatcher` ajouté à côté de `@DefaultDispatcher`, `PairingActions` pour regrouper les rappels, extraction de `drawIntoBitmap`, `if` au lieu d'un `when` à branche vide. Détails dans `ETAPE-13.md`.)*

**Tests manuels :** associer un tag, ré-associer avec confirmation ; le sélecteur ne montre ni Niumi, ni le launcher, ni Réglages, ni Téléphone ; sélectionner 50 puis tenter 51. *(Déroulés essai par essai le 2026-09-11 sur Xiaomi 25080RABDG, Android 16 — 95 tests instrumentés verts (41 `:core:database`, 13 `:core:system`, 36 `:feature:setup`, 5 `:feature:ringing`) et les neuf essais du protocole observés. **Ce passage a révélé deux défauts qu'aucun test ne pouvait attraper**, tous deux corrigés dans le même changement : les écrans 3 et 4 devenaient inatteignables une fois leurs contrôles au vert, le diagnostic ne donnant un bouton qu'au premier contrôle en échec — §13 gagne une exception pour les deux étapes de parcours, décision validée avec l'utilisateur ; et un message de scan périmé survivait à la coupure du NFC, conseillant une action impossible (§15). Sept tests de régression ajoutés. Deux cas non exercés sur appareil et couverts par les tests seuls : le chemin `UNKNOWN_PAYLOAD` (demande un tag NDEF non Niumi) et le remplacement par un `boxId` différent (un seul boîtier disponible). Détails dans `ETAPE-13.md`.)*

**Terminé quand :** tests verts, `QUERY_ALL_PACKAGES` absent du manifeste fusionné (`./gradlew :app:processDebugManifest` puis grep), association et sélecteur inaccessibles pendant une session (test de garde). *(Vérifications automatisées atteintes le 2026-09-11 : manifeste fusionné sans permission de visibilité totale des paquets — exactement les neuf permissions de §14 — et une seule section `<queries>` limitée à `ACTION_MAIN` + `CATEGORY_LAUNCHER` ; garde prouvée sur tous les états par `SetupGateTest`. **Validations sur appareil réel restantes** : 24 tests instrumentés écrits et non encore exécutés, et le protocole manuel en neuf points de `ETAPE-13.md`. L'inaccessibilité pendant une session n'est pas observable avant l'étape 14, aucune session ne pouvant être armée.)*

### Étape 14 : choix de l'heure, récapitulatif et activation en deux phases

**Deux défauts trouvés sur appareil (2026-09-12) et corrigés dans le même changement**, décrits
dans les tests manuels ci-dessous : une session armée restait invisible après un redémarrage du
processus (`PROCESS_START` n'était émis par personne depuis l'étape 11, et le réconciliateur ne
republiait pas le snapshot d'une session saine), et l'écran 7 ignorait le format 12/24 h respecté
par les écrans 5 et 6.

**Cinq décisions validées avec l'utilisateur (2026-09-12)**, détaillées dans `ETAPE-14.md` :
`vibrationEnabled` figé à `true` faute de réglage utilisateur ; sortie vers l'écran 5 par un bouton
du diagnostic ; écran 7 livré en version minimale dès cette étape ; cadran ouvert sur la dernière
heure confirmée (07:00 par défaut) ; trou d'heure d'été affiché à l'instant réel **et** expliqué.

**Trois écarts aux specs, répercutés dans le même changement.** §15 plaçait l'écran 7 à l'étape 15 —
il y est désormais décrit comme minimal à l'étape 14, complet à l'étape 15, faute de quoi l'accueil
serait un cul-de-sac dès qu'une session est armable (§10.4) ; §13 gagne une exception pour le bouton
de continuation, dans la continuité de celle accordée à l'étape 13 aux étapes de parcours, sans quoi
l'écran 5 serait inatteignable (`FixTime` n'apparaît jamais avant qu'une heure candidate existe) ;
SPEC_CORE_KMP §8.1 dit désormais ce qui doit être **affiché** quand l'heure saisie n'existe pas.

**Specs à lire :** SPEC_CORE_KMP §8.1, §10 ; SPEC_ANDROID §8, §9.2, §15 (écrans 5, 6, 7), §19.1 (`feature`).

**Fichiers :**
- Créer dans `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/` : `wake/WakeTimeScreen.kt` (`TimePicker` Material 3, date calculée affichée en clair avec le fuseau : « Demain, jeudi 4 septembre à 07:00 (Europe/Paris) »), `wake/WakeTimeViewModel.kt` (`computeWakeSchedule` avec `Clock` injecté et `TimeZone.currentSystemDefault().id`), `summary/SummaryScreen.kt` (date complète, heure, fuseau, applications choisies, boîtier tronqué, rappel « Seul le scan du boîtier terminera la session », bouton « Activer ma session »), `summary/SummaryViewModel.kt`, `activation/ArmSessionUseCase.kt`, `activation/ActivationFailure.kt`, `ui/SessionUiState.kt`.
- Modifier `HomeScreen`/`NiumiNavHost` : après `ARMED`, navigation vers `ActiveSession` (écran livré à l'étape 15 ; à cette étape, un écran minimal affichant l'état, l'heure et le fuseau).
- Tests : `WakeTimeViewModelTest` (07:00 saisi à 20:00 → lendemain affiché ; changement de fuseau système entre saisie et confirmation → recalcul avant activation), `ArmSessionUseCaseTest` (ordre exact des 10 points de §9.2 vérifié sur un journal d'appels ; diagnostic bloquant → refus sans aucun `dispatch` ; `count` 0 → refus ; boîtier absent → refus ; credential capturé dans `AndroidSessionExtras` au moment de `ACTIVATION_REQUESTED` ; échec du coordinateur → `ActivationFailure` avec `failureCode` remonté), `SummaryViewModelTest` (bouton inactif tant que le dernier diagnostic n'est pas vert).

**Produit :** `ArmSessionUseCase`, écrans 5, 6 et un écran 7 minimal.

- [x] **Écrire `WakeTimeViewModelTest`**, implémenter l'écran et le ViewModel. *(11 tests + 2 de textes. Le test « changement de fuseau entre saisie et confirmation » est scindé : le ViewModel de l'écran 5 ne peut prouver que le recalcul à l'affichage, celui d'avant activation est une propriété d'`ArmSessionUseCase`. `INVALID_TIME` n'est pas atteignable depuis le cadran (heures et minutes entières) : traité par sûreté, le contrat de la façade l'autorisant. `WakeScheduleFormatter` (8 tests) sort en `ui/`, partagé par les écrans 5, 6 et 7 ; il lit exclusivement `triggerAtEpochMillis`, jamais `localTimeIso`, faute de quoi un trou d'heure d'été afficherait l'heure saisie plutôt que celle programmée.)*
- [x] **Écrire `ArmSessionUseCaseTest`**, implémenter : 1) `DeviceReadinessChecker.check()` → `evaluateActivation` ; 2) refus si bloquant ; 3) construire `ActivationRequestDto` (`WakeScheduleDto`, `AppSelectionSummaryDto(count)`) et `AndroidSessionExtras` (credential figé, packages, `ringtoneKey = "niumi_alarm"`, `vibrationEnabled`) ; 4) `SessionCoordinator.dispatch(ACTIVATION_REQUESTED)` ; 5) attendre `DispatchResult.Applied` avec état `ARMED` (le coordinateur enchaîne `ACTIVATION_SUCCEEDED` lui-même) ; 6) retourner `Success(snapshot)` ou `ActivationFailure(failureCode)`. *(21 tests. **L'ordre des 10 points de §9.2 ne peut pas être prouvé par un seul journal d'appels** : les points 4 à 9 ne traversent jamais la frontière du use case. La preuve est l'union de deux jeux de tests, et le KDoc d'`ArmSessionUseCase` porte le tableau de traçabilité (1-3 ici, 4-9 dans `:core:system` aux étapes 9 à 11, 10 dans `SummaryViewModelTest`). Le use case expose aussi `preview()`, points 1-2 sans effet de bord, pour que l'écran 6 et l'activation appliquent le même verdict calculé par le même code ; `arm()` rejoue le diagnostic et ne réutilise jamais l'aperçu. `SessionEventFactory` gagne `activationRequested` (5 tests) : aucune fabrique ne couvrait le seul événement sans snapshot préalable. `TimeZoneProvider` est ajouté à `:core:system`, jumeau de `Clock`.)*
- [x] **Écrire `SummaryViewModelTest`**, implémenter l'écran. *(12 tests + 7 de textes. `SummaryTexts` ne traduit que quatre codes d'`ActivationReasonCode` : les quatorze messages détaillés restent la propriété de l'écran 2, seul porteur de l'action de remédiation (§13). `boxId` tronqué à 8 caractères, jamais le token ni son empreinte (§16). Garde `isActivating` : `dispatch` et `reconcile` partagent un mutex non réentrant, un double appui créerait deux sessions.)*
- [x] **Brancher la navigation** : `Summary` → `ActiveSession` sur succès, message d'échec avec `failureCode` sinon. *(`NiumiRoute.Summary` devient une `data class` portant `localTimeIso` — seule destination à argument du graphe, et volontairement : elle transporte le choix de l'utilisateur, jamais l'horaire calculé, ce qui rend le recalcul du fuseau avant activation impossible à contourner. `popUpTo(Home)` non inclusif après l'activation. L'accueil gagne un bouton « Voir ma session » : il était un cul-de-sac (§10.4), invisible tant qu'aucune session ne pouvait être armée. Le diagnostic gagne sa sortie vers l'écran 5 et `FixTime` sort de `UNAVAILABLE_ACTIONS`.)*
- [x] **Vérifier :**

```bash
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

*(Faite le 2026-09-12 — **587 tests JVM verts** : `:feature:session` 69 (+68), `:core:system` 154 (+9), `:feature:setup` 76 (+1), `:app` 14 (+3), non-régression sur `:shared:core` (160), `:core:database` (93) et `:feature:ringing` (21) ; `:app:assembleDebug`, `:app:lintDebug`, ktlint et detekt verts. `:feature:setup` et `:app` s'ajoutent aux deux modules cités par le plan, tous deux étant modifiés. Aucune règle detekt assouplie : `!!` remplacé par un branchement sur le `schedule` nullable, nombre magique du `@Preview` dérivé de `DEFAULT_LOCAL_TIME_ISO`, `shiftedFromLocalTime` restructurée, et `ReturnCount` de `arm()` suppressé localement avec justification — même convention que `DefaultSessionCoordinator.dispatch`. `TimePicker` est encore expérimental avec la BOM 2026.09.00 : `@OptIn` local. Une seule dépendance ajoutée, `lifecycle.runtime.compose` à `:feature:session`. Détails dans `ETAPE-14.md`.)*

**Tests manuels :** parcours complet accueil → onboarding → diagnostic → association → sélection → heure → récapitulatif → activation ; vérifier `adb shell dumpsys alarm | grep niumi` (alarme `setAlarmClock` présente) ; tuer le processus pendant l'activation puis relancer → `SessionReconciler` reprend ou annule proprement. *(**Déroulés essai par essai le 2026-09-12 sur Xiaomi 25080RABDG, Android 16, HyperOS 3.0** — 98 tests instrumentés verts (41 `:core:database`, 16 `:core:system`, 38 `:feature:setup`, 3 `:feature:session`) et les neuf essais observés. L'alarme est bien un `setAlarmClock` (`RTC_WAKEUP` + bloc `Alarm clock:`, `window=0`, `exactAllowReason=policy_permission`, PendingIntent explicite vers `AlarmReceiver`). **Ce passage a révélé deux défauts qu'aucun test JVM ne pouvait attraper**, tous deux corrigés dans le même changement : une session armée restait invisible après un redémarrage du processus — `ReconcileReason.PROCESS_START` n'était émis par personne depuis l'étape 11, et `SessionReconciler` ne republiait pas le snapshot d'une session saine — et l'écran 7 ignorait le format 12/24 h que les écrans 5 et 6 respectaient. Quatre tests de régression ajoutés. La garde de l'étape 13 n'est observable qu'au premier niveau (l'accueil cesse de proposer la préparation) : `SetupGate` reste inatteignable par l'interface, ce qui est le comportement voulu. Détails dans `ETAPE-14.md`.)*

**Terminé quand :** l'ordre §9.2 est prouvé par test, `FAILED` uniquement depuis `PREPARING`, une session `ARMED` est visible sur l'accueil après redémarrage de l'application. *(**Atteint le 2026-09-12.** L'ordre §9.2 est prouvé par l'union des tests décrite ci-dessus ; `FAILED` depuis `PREPARING` seulement l'était déjà par `ActivationReducer` (étape 7) ; la visibilité d'une session `ARMED` après redémarrage ne l'était pas et a demandé les deux correctifs ci-dessus — elle est désormais vérifiée sur appareil.)*

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

- [ ] **Écrire `AlarmReceiverTest`**, refondre le receiver. Ajouter le cas mesuré à l'étape 6 : si `currentInterruptionFilter == INTERRUPTION_FILTER_NONE` au déclenchement, créer l'incident `ANDROID_ALARM_MUTED_BY_DND` (`CRITICAL`) et journaliser `ALARM_MUTED_BY_DND` (SPEC_ANDROID §13, §17), sans empêcher le reste de la chaîne : la session reste active et le blocage est conservé.
- [ ] **Écrire `AlarmRingingServiceRecreationTest`**, implémenter `RingingServiceRecovery` et l'appeler depuis le service.
- [ ] **Étendre `AlarmScreenStateTest`**, compléter l'écran de réveil et l'écran 10.
- [ ] **Garantir la présence de l'écran de réveil pendant `RINGING`** (SPEC_ANDROID §10.2, §10.4, §11.2 — trois situations mesurées à l'étape 6 où `AlarmActivity` disparaît alors que l'alarme sonne, privant l'utilisateur du seul moyen de terminer sa session) : le service surveille sa notification via `NotificationManager.getActiveNotifications()` et la republie avec son `fullScreenIntent` si elle a été retirée ; `AlarmActivity` recalcule son état au changement de verrouillage (`ACTION_USER_PRESENT` ou `KeyguardManager.addKeyguardLockedStateListener` en API 34+) au lieu du seul `onResume()`. Tests : notification retirée pendant `RINGING` → republication ; verrouillage levé → le texte « Déverrouille ton téléphone… » disparaît.
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

### Étape 21 : finalisation release, suppression du POC, documentation QA, soumission Play et porte finale

**Specs à lire :** SPEC_ANDROID §16, §19, §20, §21, §22 (dernier paragraphe), §23 ; SPEC_CORE_KMP §19 ; les six documents `docs/android/play-console/` rédigés à l'étape 6.

**Rattaché ici (déviation validée à l'étape 6, voir `LOT-0.md`) : la porte de validation 0b.**
Le tournage de la vidéo de revue et la soumission Play, initialement prévus à l'étape 6, se font
maintenant que le POC est supprimé et que le parcours utilisateur réel existe.

**Fichiers :**
- Supprimer `androidApp/app/src/debug/kotlin/com/niumi/app/poc/` entièrement, `tools/` conservé.
- Créer `androidApp/app/proguard-rules.pro` (règles Room, Hilt, kotlinx-serialization, `NiumiCore` DTO conservés), `.github/workflows/mobile.yml` (runner macOS : `jvmTest`, `linkDebugFrameworkIosSimulatorArm64`, `testDebugUnitTest`, `assembleRelease`, `ktlintCheck detekt lintRelease`).
- Créer `docs/android/QA_MATRIX.md` (tableau §20 complet, colonnes fabricant/modèle/Android/firmware/permissions/résultat/retard/logs), `docs/android/RELEASE_REPORT.md` (critères §21 cochés un par un avec preuve), `docs/android/LIMITES.md` (texte de l'aide intégrée : arrêt forcé, FGS, NFC verrouillé, absence de secours logiciel).
- Ajouter dans `:app` un écran « Aide et limites » accessible depuis l'accueil, reprenant `LIMITES.md`.
- Configurer la signature release (keystore d'upload créé par l'utilisateur, hors dépôt) pour produire un AAB.
- Tests : `ReleaseHygieneTest` (`:app` unitaire : le manifeste fusionné release ne contient ni `INTERNET`, ni `SCHEDULE_EXACT_ALARM`, ni `QUERY_ALL_PACKAGES` ; aucune classe `*Poc*`, `*Fake*`, `*Debug*Store` dans le classpath release ; grep du code source `main` sans `TODO`, `FIXME`, `STOP_RINGING_ACTION`).

- [ ] **Supprimer le POC** et vérifier que `:app:assembleDebug` compile encore.
- [ ] **Écrire `ReleaseHygieneTest`**, corriger tout écart.
- [ ] **Configurer R8 et la signature release** et vérifier qu'une session complète fonctionne sur un APK/AAB release signé avec une clé locale non versionnée.
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
- [ ] **Compléter les préconditions de soumission** listées en `LOT-0.md` (identité éditeur, contact, URL de `PRIVACY_POLICY.md` publiée, compte Play vérifié).
- [ ] **Tourner la vidéo de revue** selon `REVIEW_VIDEO_SCRIPT.md`, sur l'application complète.
- [ ] **Soumettre sur piste interne ou fermée** dès que le compte Play le permet (action humaine) ; consigner la date et la réponse de Google dans `LOT-0.md`.

**Porte de validation finale (0b + finale) :** le MVP n'est déclaré terminé qu'avec `QA_MATRIX.md` verte dans le périmètre §4.1, `RELEASE_REPORT.md` complet et les portes §23 traitées (déclarations Play, vidéo, réponse de Google). Un critère non prouvé reste ouvert dans le rapport, jamais coché par défaut.

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
- [ ] Dossier Play AccessibilityService préparé (étape 6) et soumis avec réponse de Google traitée (étape 21).

## Hypothèses et limites du plan

- Le boîtier MVP contient un tag NDEF Type 2 (NFC-A) déjà écrit avec le payload canonique ; l'écriture industrielle et l'anti-clonage sont hors périmètre.
- Les appareils physiques, le compte Play Console, les actions manuelles dans les réglages et la vidéo de revue sont à la charge de l'utilisateur.
- Le framework iOS `NiumiCore` est construit pour garantir l'interopérabilité ; l'application iOS n'est pas développée par ce plan.
- Les versions de bibliothèques sont celles vérifiées le 3 septembre 2026 ; l'étape 1 les reconfirme et peut les ajuster à condition de rester dans les plages de compatibilité citées.
- Aucun commit, push ni publication n'est effectué automatiquement.
