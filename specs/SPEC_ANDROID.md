# Niumi Android: spécification technique pour Codex

Statut: spécification d'implémentation MVP
Plateforme: Android natif
Date de référence: 2 septembre 2026
Contrat métier commun: `SPEC_CORE_KMP.md`

## 1. Objet du document

Cette spécification décrit l'application Android de Niumi. Elle doit permettre à Codex de créer une première version fonctionnelle, testable sur appareils réels et conforme à la logique produit suivante:

1. l'utilisateur choisit une heure de réveil;
2. il sélectionne les applications à bloquer;
3. il confirme une session;
4. les applications choisies restent bloquées pendant la session;
5. l'alarme sonne à l'heure prévue;
6. seul le scan du boîtier NFC associé termine la session dans le parcours normal;
7. la fin de session arrête la sonnerie et débloque les applications.

Le réveil, le blocage et le NFC appartiennent à une même session métier. Ils ne doivent pas être implémentés comme trois fonctions indépendantes.

## 2. Périmètre du MVP

Le MVP Android comprend:

- une application native en Kotlin et Jetpack Compose;
- un moteur métier Kotlin Multiplatform partagé avec l'application iOS;
- l'association locale d'un boîtier NFC;
- la sélection d'applications installées;
- une seule session active à la fois;
- la programmation d'une alarme système exacte;
- la sonnerie en boucle dans un service au premier plan;
- un écran de réveil visible au-dessus de l'écran verrouillé lorsque le système l'autorise;
- le blocage comportemental des applications sélectionnées avec un `AccessibilityService`;
- la fin de session après validation locale du tag NFC;
- la reprogrammation après redémarrage, changement d'heure, changement de fuseau ou mise à jour de l'application;
- un diagnostic avant l'activation de chaque session;
- un journal technique local limité, sans contenu saisi par l'utilisateur.

Le MVP ne comprend pas:

- de compte utilisateur;
- de synchronisation serveur;
- d'abonnement;
- de statistiques de sommeil;
- de répétition de l'alarme;
- de bouton "Snooze";
- de bouton logiciel permettant d'arrêter l'alarme;
- de blocage MDM avec `DevicePolicyManager`;
- de promesse selon laquelle l'application résiste à un arrêt forcé, une désinstallation ou à l'extinction du téléphone;
- de protection cryptographique contre le clonage du tag NFC.

## 3. Décisions produit fixées pour le MVP

Codex doit appliquer les décisions suivantes sans ajouter de variante cachée:

- Une seule session peut être active.
- Le moteur `NiumiCore` est l'unique autorité pour les transitions métier communes à Android et iOS.
- Une session active ne peut pas être annulée ou modifiée depuis un bouton ordinaire.
- Toute modification ou annulation après activation exige le scan du boîtier associé.
- Une annulation avant la sonnerie place la session dans l'état `CANCELLED`. Elle ne compte pas comme une session terminée au réveil.
- L'alarme ne propose ni arrêt, ni répétition, ni délai.
- Le bouton Retour, le passage à l'accueil, le verrouillage de l'écran et la fermeture de l'activité ne terminent pas la session.
- Le scan NFC valide termine la session même sans réseau.
- Un scan inconnu, illisible ou mal formé ne change aucun état.
- Un scan valide place d'abord la session dans `RELEASING`. L'état final n'est écrit qu'après le nettoyage effectif des sous-systèmes.
- Le blocage commence uniquement après confirmation de la programmation de l'alarme.
- Le blocage reste actif dans `ARMED`, `RINGING`, `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC`. Pendant `RELEASING`, il dépend des effets de libération déjà réussis et de la projection native.
- La sélection contient entre 1 et 50 applications.
- L'instant du réveil est figé après activation. Un changement de fuseau modifie l'affichage local, pas `triggerAtEpochMillis`.
- Les applications système nécessaires à la sécurité et aux réglages ne sont jamais proposées dans le sélecteur.
- Le MVP ne fournit aucun mécanisme logiciel de secours pendant une session active. Si le boîtier est perdu, cassé ou illisible, ou si le NFC tombe en panne, l'utilisateur conserve les mécanismes Android tels que l'arrêt forcé ou l'extinction du téléphone. Cette limite est intentionnelle et doit être expliquée avant la première activation.
- `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC` affichent toujours une notification demandant le scan, y compris lorsque l'alarme n'a jamais sonné. Aucune session bloquante ne reste silencieuse.

## 4. Limites de la promesse Android

La fiabilité doit être définie dans un périmètre vérifiable.

### 4.1 Conditions prises en charge

Niumi doit exécuter le parcours garanti, avec sonnerie et interface de scan immédiatement accessible, lorsque toutes les conditions suivantes sont réunies:

- le téléphone est allumé et possède assez de batterie;
- Niumi est installé et n'a pas été arrêté de force;
- l'accès aux alarmes exactes est disponible;
- les notifications et les alarmes plein écran sont autorisées lorsque la version d'Android les contrôle;
- le volume des alarmes n'est pas nul;
- le mode Ne pas déranger n'est pas en silence total, qui mute le flux d'alarme et supprime l'écran de réveil (13);
- le système Android n'est pas défaillant;
- l'application a terminé l'activation de la session et a affiché sa confirmation.

L'absence d'autorisation plein écran n'empêche pas nécessairement le son de démarrer. Elle empêche toutefois le MVP de garantir l'accès immédiat à l'interface de scan, ce qui suffit pour refuser l'activation selon la politique produit.

### 4.2 Cas impossibles à garantir

L'application ne peut pas garantir la sonnerie dans les cas suivants:

- téléphone éteint ou batterie vide;
- application désinstallée;
- arrêt forcé depuis les réglages;
- arrêt de l'application depuis le gestionnaire des services actifs du système;
- permissions retirées après l'activation;
- volume d'alarme rendu inaudible après l'activation;
- mode Ne pas déranger passé en silence total après l'activation: l'alarme est muette et l'écran de réveil ne s'affiche pas; l'incident `ANDROID_ALARM_MUTED_BY_DND` est créé, sans que Niumi puisse rétablir le son;
- panne du système, du haut-parleur ou du matériel NFC;
- comportement OEM incompatible non détecté.

**Restriction OEM de démarrage automatique (mesurée à l'étape 19, MIUI/HyperOS).** Sur ces surcouches, une permission par application — « Démarrage automatique en arrière-plan », gérée par le Security Center de Xiaomi et refusée par défaut — décide si un broadcast a le droit de **démarrer le processus** de l'application. Elle ne bloque pas la réception d'un broadcast par un processus déjà vivant.

Portée mesurée le 2026-09-14 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0, par comparaison contrôlée avec et sans la permission:

- `LOCKED_BOOT_COMPLETED` et `BOOT_COMPLETED` en sont **exemptés**: permission refusée, le processus a été démarré et l'alarme reprogrammée à trois reprises. **La reprogrammation après redémarrage de §9.3 ne dépend donc pas de ce réglage** — c'est le point qui compte pour la promesse du produit.
- `MY_PACKAGE_REPLACED` y est **soumis**: refusée, le système journalise `process is not permitted to auto start` et le receveur ne tourne pas; accordée, le processus est démarré pour `SystemEventsReceiver`. Sans lui, l'alarme survit tout de même à une mise à jour, Android préservant le `PendingIntent`.
- `TIME_CHANGED` et `TIMEZONE_CHANGED` suivent vraisemblablement la même règle quand le processus est mort — non mesuré, les deux n'ayant été testés qu'avec un processus vivant. La conséquence y serait limitée à l'incident non consigné: l'instant du réveil est un `RTC_WAKEUP` absolu qu'Android préserve lui-même à travers un changement d'horloge, et le réenregistrement de §9.3 est une sécurité supplémentaire, pas le mécanisme principal.

Aucun contrôle de diagnostic n'est ajouté pour ce réglage: ce qu'il conditionne est une sécurité supplémentaire, jamais le déclenchement du réveil. L'ajouter au tableau de §13 ferait refuser une activation sur un motif qui ne compromet pas la promesse. Il est en revanche nommé dans l'aide (§21, `LIMITES.md`) et dans la matrice de tests physiques, au même titre que l'arrêt forcé.

**Quota Doze de l'alarme de secours — mesuré et écarté (§9.1, §10.2, étape 20).** La documentation d'Android annonce que `setAndAllowWhileIdle()` et `setExactAndAllowWhileIdle()` ne peuvent pas être délivrées plus d'une fois toutes les neuf minutes par application en Doze. Ce quota aurait fait dégénérer la chaîne du watchdog de 60 secondes à ~9 minutes, et il avait d'abord été consigné ici comme une réserve permanente, au motif que la mesure exigerait l'état qu'elle romprait — **ce raisonnement était faux**, `dumpsys deviceidle` servant précisément à forcer cet état.

**Mesuré le 2026-09-15 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0, par Doze profond forcé** (`dumpsys battery unplug`, puis `dumpsys deviceidle force-idle`), processus tué pendant `RINGING` à chaque cycle: **cinq livraisons consécutives, toutes à l'heure, toutes appareil en `IDLE`** — tics programmés à 12:57:02, 12:58:02, 12:59:03, 13:01:05 et 13:02:06, sons revenus à 12:57:05, 12:58:03, 12:59:05, 13:01:06 et 13:02:08. Intervalles mesurés de 58 à 62 secondes en veille profonde.

**Le quota ne s'applique donc pas à Niumi**, et `dumpsys alarm` dit pourquoi: `exactAllowReason=policy_permission` — l'application détient `USE_EXACT_ALARM` — et la ligne de politique de l'alarme ne porte aucune contrainte (`device_idle=-2s180ms`, `app_standby=-37s222ms`, valeurs passées donc inopérantes). Le choix de `setExactAndAllowWhileIdle` (§9.1) est confirmé par la mesure, et `setAlarmClock` n'est pas nécessaire.

**Réserve résiduelle, honnête et étroite:** un seul appareil, et le seau d'App Standby de Niumi n'a **jamais pu être rétrogradé** sous `EXEMPTED` (5) pendant l'essai, y compris processus mort — le système a refusé un `am set-standby-bucket restricted`. Un appareil qui placerait Niumi en `RARE` ou `RESTRICTED` au moment du watchdog n'a donc pas été observé. L'argument de proportion: le watchdog ne tourne que dans la fenêtre qui suit immédiatement un service de premier plan et un écran de réveil plein écran, état où un seau bas est improbable par construction.

Depuis Android 15, un arrêt forcé annule les `PendingIntent` de l'application. Il supprime donc aussi l'alarme programmée. L'application doit signaler clairement cette limite dans l'aide et dans le plan de test. Elle ne doit pas tenter de bloquer les réglages, la désinstallation ou l'arrêt du service d'accessibilité.

### 4.3 Limite du blocage d'applications

Le blocage Android est un blocage comportemental. Il renvoie l'utilisateur à l'accueil dès qu'une application sélectionnée passe au premier plan. Un utilisateur déterminé peut le contourner en désactivant le service d'accessibilité, en arrêtant Niumi ou en désinstallant l'application. Cette limite ne doit pas être masquée dans le produit.

### 4.4 Limite NFC sur écran verrouillé

Android recherche habituellement les tags NFC lorsque l'écran est déverrouillé. `AlarmActivity` doit s'afficher au-dessus de l'écran verrouillé et activer le Reader Mode, mais Codex ne doit pas supposer que le scan fonctionnera avant le déverrouillage sur tous les appareils. Le parcours doit demander le déverrouillage si le matériel ou la couche OEM l'exige. Ce comportement fait partie des tests physiques obligatoires.

### 4.5 Boîtier ou NFC indisponible pendant une session

Le MVP ne propose pas de code de secours, de délai d'abandon ni de bouton "Arrêter quand même". Si le boîtier associé est perdu, cassé ou inaccessible, ou si le matériel NFC du téléphone tombe en panne, l'application maintient l'état métier et le blocage tant qu'elle continue de fonctionner.

L'utilisateur peut toujours recourir aux mécanismes du système, notamment l'arrêt forcé ou l'extinction du téléphone. Ces actions sortent du parcours Niumi et font partie des limites documentées. L'onboarding doit présenter cette conséquence avant la première session.

## 5. Cibles techniques

| Élément | Valeur MVP |
| --- | --- |
| Langage | Kotlin |
| Domaine partagé | Kotlin Multiplatform, module `:shared:core` |
| Interface | Jetpack Compose + Material 3 |
| `minSdk` | 29, Android 10 |
| `targetSdk` | 36, Android 16 |
| `compileSdk` | 37, Android 17 |
| Injection | Hilt |
| Asynchronisme | Coroutines + Flow |
| Base locale | Room |
| Préférences | DataStore |
| Navigation | Navigation Compose |
| Temps et sérialisation partagés | `kotlinx-datetime`, `kotlinx-serialization` |
| Tests | `kotlin.test` dans `commonTest`, JUnit 4, Truth, Turbine, tests instrumentés AndroidX |
| Qualité | Android Lint, ktlint, detekt |

Le projet doit compiler avec une version stable du plugin Android Gradle compatible avec `compileSdk 37`. Les versions de bibliothèques doivent être centralisées dans `gradle/libs.versions.toml`. Ne pas utiliser de version dynamique avec `+`.

**Statut detekt (constaté à l'étape 1, 2026-09-04) :** la dernière version stable de detekt (1.23.8) embarque un analyseur Kotlin 2.0.21 et échoue sur du Kotlin 2.4 (bugs officiels confirmés : NPE sur les context parameters, erreurs de parsing, metadata incompatible). Le projet utilise donc `dev.detekt` 2.0.0-alpha.6 (`id("dev.detekt")`), seule variante construite contre Kotlin 2.4.10, en porte bloquante et version épinglée. À remplacer par la première version stable de detekt 2 compatible Kotlin 2.4+ dès sa publication.

## 6. Architecture du projet

Utiliser les modules Gradle suivants:

```text
:app
:shared:core
:core:database
:core:system
:core:designsystem
:feature:setup
:feature:session
:feature:ringing
```

Responsabilités:

| Module | Responsabilité |
| --- | --- |
| `:app` | Application Hilt, navigation, manifeste final, thème et assemblage |
| `:shared:core` | États, événements, réducteur, heure, NFC, diagnostics communs, DTO Swift et tests métier |
| `:core:database` | Room, DAO, DataStore et stockage Direct Boot |
| `:core:system` | Adaptateurs et modèles Android pour AlarmManager, notifications, audio, NFC, packages et diagnostics |
| `:core:designsystem` | Thème Material 3, palette et composants visuels partagés |
| `:feature:setup` | Onboarding, permissions, association NFC et sélection des applications |
| `:feature:session` | Configuration, activation, session active et blocage |
| `:feature:ringing` | Service de sonnerie, activité plein écran et fin par NFC |

Règles de dépendance:

- les modules `feature`, `core:database` et `core:system` peuvent dépendre de `:shared:core`;
- `:shared:core` ne dépend d'aucune API Android ou Apple;
- `core:database` et `core:system` convertissent leurs modèles vers les DTO KMP;
- les modules `feature` et `:app` peuvent dépendre de `:core:designsystem`, qui ne dépend
  d'aucun autre module du dépôt (accès au thème depuis un module `feature` sans dépendance
  inverse vers `:app`, ex. `AlarmActivity` dans `:feature:ringing`);
- aucun composable ne parle directement à Room, `AlarmManager`, `NfcAdapter` ou `AccessibilityService`;
- les `ViewModel` appellent des cas d'usage;
- les composants système transmettent des événements à `NiumiCoreFacade` et exécutent les effets retournés;
- aucun receiver, service, `ViewModel` ou dépôt ne modifie directement `SessionState`;
- les types propres à Android, dont les noms de packages et l'état des permissions, ne traversent pas la frontière KMP.

## 7. Modèle métier

### 7.1 État d'une session

```kotlin
enum class SessionState {
    PREPARING,
    ARMED,
    RINGING,
    AWAITING_NFC,
    TRIGGERED_AWAITING_NFC,
    RELEASING,
    COMPLETED,
    CANCELLED,
    FAILED
}

enum class ReleaseTarget {
    COMPLETED,
    CANCELLED
}
```

Transitions autorisées:

| État source | Événement KMP | État cible | Cible de libération |
| --- | --- | --- | --- |
| aucun | `ACTIVATION_REQUESTED` | `PREPARING` | aucune |
| `PREPARING` | `ACTIVATION_SUCCEEDED` | `ARMED` | aucune |
| `PREPARING` | `ACTIVATION_FAILED` | `FAILED` | aucune |
| `ARMED` | `ALARM_FIRED` | `RINGING` | aucune |
| `ARMED` ou `RINGING` | `ALARM_SOUND_STOPPED` | `AWAITING_NFC` | aucune |
| `ARMED` | `TRIGGER_ELAPSED` | `TRIGGERED_AWAITING_NFC` | aucune |
| `TRIGGERED_AWAITING_NFC` | `ALARM_SOUND_STOPPED` | état inchangé | aucune |
| `ARMED`, avant `triggerAtEpochMillis` | `VALID_NFC_SCANNED` | `RELEASING` | `CANCELLED` |
| `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` | `VALID_NFC_SCANNED` | `RELEASING` | `COMPLETED` |
| `RELEASING` | `RELEASE_FAILED` | `RELEASING` | inchangée |
| `RELEASING` | `RELEASE_SUCCEEDED` | cible enregistrée | inchangée |
| état actif | `INVALID_NFC_SCANNED` ou `INCIDENT_REPORTED` | état inchangé | inchangée |

Android ne produit pas `ALARM_SOUND_STOPPED` dans le parcours normal. Cet événement appartient au contrat commun afin de représenter le contrôle Stop imposé par iOS.

`VALID_NFC_SCANNED` reçu depuis `ARMED` à ou après `triggerAtEpochMillis` est refusé par le moteur avec la violation `TRIGGER_ALREADY_ELAPSED`. `HandleValidNfcUseCase` (section 11.3) réconcilie systématiquement l'heure avant un scan depuis `ARMED` et envoie d'abord `TRIGGER_ELAPSED` dans ce cas, si bien que ce refus reste un filet de sécurité et ne doit jamais se produire en parcours normal.

`NiumiCoreFacade` est l'unique composant autorisé à appliquer ces transitions. Chaque événement possède un identifiant stable et une révision attendue, sauf `ACTIVATION_REQUESTED`. `SessionCoordinator` déduplique l'événement dans son registre avant d'appeler le moteur. Un doublon strict ne produit aucune nouvelle décision ni aucun nouvel effet; la réutilisation d'un identifiant avec un payload différent produit `EVENT_ID_CONFLICT`.

`FAILED` signifie que la session n'a jamais pu être armée correctement. Une session déjà `ARMED`, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou `RELEASING` ne passe jamais à `FAILED`. Un incident survenu après l'activation conserve l'état métier. Il ne retire pas le blocage, sauf lorsqu'un effet de libération déjà autorisé a réussi pendant `RELEASING`.

```kotlin
enum class SessionHealth {
    HEALTHY,
    DEGRADED
}

enum class IncidentSeverity {
    WARNING,
    DEGRADED,
    CRITICAL
}

data class SessionIncident(
    val code: String,
    val severity: IncidentSeverity,
    val occurredAtEpochMillis: Long,
    val platform: Platform
)
```

Exemples de codes d'incident:

| Code | Gravité par défaut |
| --- | --- |
| `ALARM_PERMISSION_REVOKED` | `CRITICAL` |
| `BLOCKING_PERMISSION_REVOKED` | `CRITICAL` |
| `ANDROID_AUDIO_START_FAILED` | `CRITICAL` |
| `ANDROID_FULL_SCREEN_REVOKED` | `CRITICAL` |
| `ANDROID_ALARM_VOLUME_ZERO` | `CRITICAL` |
| `NFC_DISABLED` | `CRITICAL` |
| `TIME_CHANGED` | `WARNING` |
| `PROCESS_RECREATED` | `WARNING` |
| `MISSED_TRIGGER_WINDOW` | `DEGRADED` |
| `ANDROID_OEM_RESTRICTION_SUSPECTED` | `WARNING` |
| `RELEASE_PARTIAL_FAILURE` | `DEGRADED` |
| `SNAPSHOT_CORRUPTED` | `CRITICAL` |

`ALARM_PERMISSION_REVOKED` couvre la perte de `canScheduleExactAlarms()` après activation. `BLOCKING_PERMISSION_REVOKED` couvre la désactivation du service d'accessibilité après activation. Ce sont les codes communs du contrat KMP; Android ne les réexprime pas sous un préfixe `ANDROID_` propre, afin qu'un incident de perte de permission reste comparable entre les deux plateformes.

`SessionHealth`, `IncidentSeverity`, `Platform` et `SessionIncident` proviennent de `:shared:core`. Les codes exclusivement Android utilisent le préfixe `ANDROID_` lorsqu'ils sont exposés au contrat partagé. Une gravité `WARNING` ne change pas la santé; `DEGRADED` ou `CRITICAL` la passe à `DEGRADED`. `CRITICAL` doit en plus être présenté explicitement dans le diagnostic d'incident visible par l'utilisateur.

L'état métier doit rester distinct de l'état des sous-systèmes Android:

```kotlin
data class SessionRuntimeStatus(
    val alarmScheduled: Boolean,
    val accessibilityReady: Boolean,
    val notificationReady: Boolean,
    val fullScreenReady: Boolean,
    val nfcReady: Boolean,
    val audioReady: Boolean
)
```

`SessionRuntimeStatus` sert au diagnostic et à la réconciliation. Il ne remplace pas `SessionState`.

**Implémentation (étape 11).** `SessionRuntimeStatusProbe` (`:core:system.session`) construit cette structure à partir de sources déjà existantes, réutilisées telles quelles par le `DeviceReadinessChecker` de l'étape 12 plutôt que redéfinies : `alarmScheduled` via `AlarmScheduler.isScheduled(sessionId)`, `accessibilityReady` via `AccessibilityServiceStatus.isEnabled()`, `notificationReady` et `fullScreenReady` via `NotificationAvailability` (`areNotificationsEnabled()`, `canUseFullScreenIntent()` — toujours `true` avant Android 14, l'API n'existant pas), `nfcReady` via `NfcReader.availability == ENABLED`, `audioReady` via `AlarmVolumeSource.alarmStreamVolume() > 0` (`AudioManager.STREAM_ALARM`).

**Deux consommateurs distincts, pas un seul (étape 20).** La structure entière est construite à chaque sonde, mais seuls deux de ses six champs pilotent une réconciliation : `alarmScheduled` et `nfcReady`, via `SessionRuntimeReconciler`. Les quatre autres — `accessibilityReady`, `notificationReady`, `fullScreenReady`, `audioReady` — sont la propriété de `SessionReadinessMonitor` (§13.1), qui les surveille déjà avec sa propre déduplication d'incidents ; les y faire aussi piloter `SessionRuntimeReconciler` recréerait pour eux les doublons corrigés à l'étape 16, deux producteurs consignant le même fait. La distinction entre les deux champs traités ici et le contrôle `EXACT_ALARM` de §13.1 : celui-ci teste la **permission** (`canScheduleExactAlarms()`), `alarmScheduled` teste l'**alarme réellement programmée** (`isScheduled()`) — une surcouche OEM peut effacer la seconde sans toucher à la première. Voir §18.

Toute nouvelle session reçoit `health = HEALTHY`. Seul un incident postérieur au passage à `ARMED` peut la faire passer à `DEGRADED`. La santé ne revient pas silencieusement à `HEALTHY`; une réconciliation réussie doit être journalisée.

### 7.2 Entités Room

`AlarmSessionEntity`:

```text
id: String UUID, clé primaire
schemaVersion: Int
revision: Long
localDate: String ISO-8601
localTime: String ISO-8601
zoneIdAtActivation: String IANA
triggerAtEpochMillis: Long
state: SessionState
releaseTarget: ReleaseTarget?
health: SessionHealth
boxId: String
boxTokenSha256Hex: String
ringtoneKey: String
vibrationEnabled: Boolean
createdAtEpochMillis: Long
armedAtEpochMillis: Long?
ringingAtEpochMillis: Long?
alarmSoundStoppedAtEpochMillis: Long?
triggerElapsedAtEpochMillis: Long?
nfcVerifiedAtEpochMillis: Long?
releasingAtEpochMillis: Long?
completedAtEpochMillis: Long?
cancelledAtEpochMillis: Long?
failureCode: String?
```

`failureCode` n'est renseigné que lorsqu'une session termine son activation dans l'état `FAILED`. Les incidents postérieurs à l'armement sont stockés séparément.

`boxId` et `boxTokenSha256Hex` sont copiés depuis `PairedBoxEntity` à l'étape `ACTIVATION_REQUESTED` et figés pour la durée de la session. `HandleValidNfcUseCase` vérifie toujours le scan contre ces deux valeurs de la session active, jamais contre `PairedBoxEntity` directement, afin qu'une ré-association ne puisse pas changer le boîtier attendu d'une session en cours.

`BlockedAppEntity`:

```text
sessionId: String
packageName: String
displayNameSnapshot: String
clé primaire composée: sessionId + packageName
```

`PairedBoxEntity`:

```text
boxId: String, clé primaire
protocolVersion: Int
tokenSha256: String hexadécimal
pairedAtEpochMillis: Long
```

`TechnicalEventEntity`:

```text
id: Long auto-généré
sessionId: String?
type: String
createdAtEpochMillis: Long
detailsJson: String?
```

`SessionIncidentEntity`:

```text
id: Long auto-généré
sessionId: String
code: String
severity: IncidentSeverity
occurredAtEpochMillis: Long
platform: Platform = ANDROID
```

`SessionEventReceiptEntity` conserve `eventId`, `sessionId`, l'empreinte canonique du payload et la révision appliquée. `SessionEffectOutboxEntity` conserve `effectId`, `sessionId`, `revision`, `kind`, le payload sérialisé de l'effet, l'état d'exécution et la dernière erreur. Room écrit la session, le reçu et les effets dans une seule transaction. Un effet `RECORD_INCIDENT` conserve ainsi le code, la gravité, l'horodatage et la plateforme nécessaires à sa reprise.

Le journal conserve au maximum les 200 derniers événements. Il ne doit contenir ni texte d'accessibilité, ni nom de fenêtre, ni saisie utilisateur, ni contenu provenant d'une autre application.

**Version de la base.** La base est en **v2** depuis l'étape 16 : `technical_event` y gagne `deviceModel`, `androidVersion` et `appVersion` (17). `MIGRATION_1_2` est additive et ne touche aucune autre table ; les lignes existantes reçoivent `''`, le journal antérieur est conservé. Chaque schéma reste committé sous `androidApp/core/database/schemas/`, v1 comprise — `MigrationTestHelper` en a besoin pour créer une base v1 avant d'y appliquer la migration. Les trois colonnes sont déclarées `@ColumnInfo(defaultValue = "''")` : SQLite exige une valeur par défaut pour ajouter une colonne `NOT NULL` à une table peuplée, et sans cette annotation le schéma attendu par Room n'en déclarerait aucune, faisant échouer `validateMigration` sur cette seule différence. Aucun `fallbackToDestructiveMigration` : une migration manquante doit faire échouer l'ouverture plutôt qu'effacer une session active et son journal (13, 18).

### 7.3 Snapshot Direct Boot

Room reste dans le stockage protégé par les identifiants. Un snapshot minimal doit être copié dans le stockage protégé de l'appareil avec `createDeviceProtectedStorageContext()`.

Le snapshot contient:

```text
projectionSchemaVersion
domainSchemaVersion
domainRevision
sessionId
localDate
localTime
zoneIdAtActivation
triggerAtEpochMillis
state
releaseTarget
health
createdAtEpochMillis
armedAtEpochMillis
ringingAtEpochMillis
alarmSoundStoppedAtEpochMillis
triggerElapsedAtEpochMillis
nfcVerifiedAtEpochMillis
releasingAtEpochMillis
completedAtEpochMillis
cancelledAtEpochMillis
failureCode
ringtoneKey
vibrationEnabled
boxId
boxTokenSha256Hex
blockedPackages
eventReceipts
pendingEffects
```

`boxTokenSha256Hex` reprend le nom de colonne de `AlarmSessionEntity` (§7.2) plutôt que `tokenSha256` (nom propre à `PairedBoxEntity`) : le snapshot projette la session, pas le boîtier associé. `blockedPackages` conserve la paire `(packageName, displayNameSnapshot)` de `BlockedAppEntity`, pas seulement le nom du package : le libellé figé à l'activation est requis par le texte imposé de l'overlay (§12.2, « {Nom de l'application} reste bloquée… ») et doit rester disponible si le blocage doit être reconstruit avant déverrouillage.

Ce snapshot est une projection partielle de Room, mais son enveloppe de session active contient tous les champs requis pour reconstruire un `SessionSnapshot` et appeler KMP avant déverrouillage. Chaque effet de `pendingEffects` conserve aussi son payload sérialisé afin de reprendre `RECORD_INCIDENT`. Il permet de reprogrammer et de déclencher l'alarme avant le premier déverrouillage après un redémarrage. Il ne contient aucune donnée de compte. Son écriture doit être atomique. Utiliser un fichier temporaire dans le même répertoire, puis un renommage, ou des préférences synchrones dédiées avec contrôle de version. Une réécriture à `domainRevision` égale, pour la même session, est idempotente; une révision inférieure pour cette même session est refusée. Une nouvelle session (autre `sessionId`) repart légitimement à une révision inférieure : la garde est scopée par `sessionId`, pas globale.

Les composants `directBootAware` ne doivent pas créer Room ou un dépôt qui ouvre Room avant `UserManager.isUserUnlocked == true`. Utiliser des dépendances différées et le snapshot comme unique source avant le déverrouillage.

**Ce que garantit réellement le contexte protégé par appareil (précision de l'étape 19).** La sûreté avant déverrouillage tient à deux choses: le composant est `directBootAware`, et aucun code de son chemin n'accède au stockage chiffré par les identifiants. Elle ne tient pas au `Context` porté par les adaptateurs: `AlarmManager`, `NotificationManager` et `startForegroundService` se comportent à l'identique avec l'un ou l'autre contexte, puisque seuls les appels de stockage changent de racine. Un `createDeviceProtectedStorageContext()` est donc utilisé **en permanence**, et non aiguillé selon l'état de déverrouillage, par `AlarmScheduler`, `AndroidPendingIntentFactory`, `ScanRequestNotifier`, le registrar de canaux de notification, le notificateur d'avertissement de §13.1 et `RingingController` — un seul jeu de liaisons, ce que §9.3 et §10.5 exigent sans ambiguïté et sans graphe parallèle. Les dépôts dont les données appartiennent légitimement au stockage chiffré par les identifiants (Room, `SetupPreferences`, `AppSelectionStore`) gardent le contexte d'application.

**Garde de déverrouillage des `DataStore` (étape 19, mesuré sur appareil).** La règle ci-dessus ne vaut pas que pour Room: elle vaut pour **tout** dépôt en stockage chiffré par les identifiants. `AppSelectionStore` et `SetupPreferences` sont deux `DataStore` de ce type, et le diagnostic de §13 les lit — or ce diagnostic est rejoué par la réconciliation `LOCKED_BOOT` (§13.1), donc avant le premier déverrouillage.

Le dégât mesuré le 2026-09-14 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0 dépasse largement la lecture d'une valeur fausse: l'instance de `DataStore` créée dans cette fenêtre — où le répertoire n'existe pas encore (`Failed to ensure /data/user/0/… : mkdir failed: errno 126`) — **continue de servir un état vide après le déverrouillage, pour toute la durée de vie du processus**. La sélection d'applications de l'utilisateur devenait invisible et aucune nouvelle session ne pouvait être préparée tant que le processus n'était pas recréé. C'est une régression introduite par `SystemEventsReceiver` lui-même: avant lui, aucun composant Niumi ne tournait avant déverrouillage hors de la chaîne d'alarme.

Les deux dépôts portent donc une garde construite sur le même patron que `RoomSessionStore`: le `Context` leur est fourni par un `Provider` qui n'est **jamais résolu** avant déverrouillage — la garde empêche l'instance de naître, elle ne se contente pas d'ignorer son résultat. Lectures neutres (sélection vide, préférences à leur valeur par défaut), écritures refusées (`DATASTORE_BEFORE_UNLOCK`). Aucune écriture n'a lieu avant déverrouillage en pratique: ces préférences ne changent que depuis l'interface.

Une sélection vide avant déverrouillage est sans conséquence: `APP_SELECTION` et `BATTERY_OPTIMIZATION` ne font pas partie des six contrôles surveillés pendant `ARMED` (§13.1), et les applications bloquées d'une session active sont figées dans Room et dans la projection Direct Boot (§7.2, §7.3), jamais relues dans ces dépôts.

**Exception documentée — `RoomPairedBoxStore.current()` (étape 13).** Tous les dépôts Room refusent l'accès avant déverrouillage en levant (`ROOM_BEFORE_UNLOCK`), sauf la lecture du boîtier associé, qui renvoie « aucun boîtier » sans jamais ouvrir la base. Raison : le diagnostic de §13 est rejoué pendant la réconciliation Direct Boot (raison `LOCKED_BOOT`, §13.1), et une levée y interromprait la reprogrammation de l'alarme. L'exception est sans effet sur la sûreté : le contrôle `PAIRED_BOX` ne fait pas partie des six contrôles surveillés pendant `ARMED` (§13.1), donc aucun incident faux n'est produit, et le contrat commun impose déjà que la vérification NFC d'une session armée utilise le credential figé à l'activation (SPEC_CORE_KMP §10), jamais le dépôt courant. L'écriture (`replace`, `clear`) conserve le refus strict : une association n'a lieu qu'à l'écran dédié, hors session, donc toujours après déverrouillage.

## 8. Calcul de l'heure de déclenchement

Conserver à la fois l'intention locale et l'instant calculé:

- `LocalDate`;
- `LocalTime`;
- `kotlinx.datetime.TimeZone`;
- `Instant` final en millisecondes Unix.

Règles:

- Si l'heure locale n'existe pas lors du passage à l'heure d'été, choisir le premier instant valide après le saut.
- Si l'heure locale existe deux fois lors du passage à l'heure d'hiver, choisir la première occurrence.
- Après `ACTIVATION_SUCCEEDED`, `triggerAtEpochMillis` devient immuable.
- Après `TIME_CHANGED` ou `TIMEZONE_CHANGED`, réenregistrer le même instant auprès d'AlarmManager. Ne pas le recalculer depuis la date et l'heure locales d'origine.
- L'interface recalcule l'heure d'affichage dans le fuseau courant sans modifier la session.
- Si l'instant enregistré se trouve dans le passé, déclencher immédiatement si le retard est inférieur ou égal à 15 minutes, sauf lorsqu'un scan valide est déjà en cours et qu'aucune alarme n'a été observée. Dans ce cas, produire `TRIGGER_ELAPSED` avant le scan.
- Au-delà de 15 minutes, ne pas faire sonner une alarme tardive. Produire `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW`, passer à `TRIGGERED_AWAITING_NFC`, passer la santé à `DEGRADED` et maintenir le blocage jusqu'au scan du boîtier. Une session déjà armée ne doit jamais passer à `FAILED`.
- Le calcul initial appartient à `:shared:core` et utilise `kotlinx-datetime`, notamment `TimeZone`. Les conversions éventuelles vers des types JVM de fuseau horaire restent limitées aux adaptateurs Android natifs. AlarmManager reçoit l'instant final en millisecondes Unix.

## 9. Programmation de l'alarme

### 9.1 API imposée

Utiliser:

```kotlin
AlarmManager.setAlarmClock(
    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
    alarmPendingIntent
)
```

Le `alarmPendingIntent` cible explicitement `AlarmReceiver`. Le `showPendingIntent` ouvre l'écran de la session programmée.

Tous les `PendingIntent` internes doivent être explicites et utiliser `FLAG_IMMUTABLE`. Ajouter `FLAG_UPDATE_CURRENT` lorsque les extras doivent être actualisés. Le code de requête doit être stable pour une session donnée.

Ne pas utiliser WorkManager, `Handler`, `setInexactRepeating()` ou une notification planifiée pour déclencher le réveil.

**Dérogation strictement limitée à l'alarme de secours de `RINGING` (§10.2, étape 20).** `AlarmManager.setExactAndAllowWhileIdle()` y est autorisé, et seulement là: le réveil lui-même reste exclusivement `setAlarmClock()`. La distinction tient à ce que chacune doit paraître. Le réveil est l'alarme visible de l'utilisateur — `setAlarmClock()` l'affiche au réglage système « prochaine alarme », ce qui est le comportement voulu. Le watchdog est un tic invisible toutes les 60 secondes tant que Niumi sonne; l'afficher au même endroit mentirait sur ce qu'il est (§10.5 exige déjà la même discrétion pour la notification d'attente de scan). Un `PendingIntent` distinct de celui du réveil, ciblant `RingingWatchdogReceiver`, garantit que `cancel()` ne peut jamais atteindre l'un en visant l'autre.

Contrepartie annoncée par la documentation — Doze limitant `setExactAndAllowWhileIdle()` à une livraison par application toutes les neuf minutes — **mesurée puis écartée le 2026-09-15**: en Doze profond forcé, cinq tics consécutifs ont été délivrés à l'heure, à 58-62 secondes d'intervalle. La raison tient à `USE_EXACT_ALARM` (`exactAllowReason=policy_permission` dans `dumpsys alarm`), qui affranchit l'alarme des politiques `device_idle` et `app_standby`. `setAlarmClock` n'est donc pas nécessaire ici. Détail de la mesure et réserve résiduelle sur le seau d'App Standby: §4.2.

### 9.2 Activation en deux phases

`ArmSessionUseCase` orchestre les effets retournés par `NiumiCoreFacade` dans cet ordre:

1. exécuter le diagnostic complet;
2. refuser l'activation si une exigence bloquante échoue;
3. envoyer `ACTIVATION_REQUESTED` au moteur commun;
4. écrire atomiquement dans Room, en une seule transaction SQLite, la session `PREPARING`, ses applications, le reçu de l'événement et l'outbox; copier ensuite le même contenu dans le snapshot Direct Boot;
5. créer les `PendingIntent` et appeler `setAlarmClock()`;
6. activer le blocage de la transaction;
7. vérifier les résultats observables;
8. envoyer `ACTIVATION_SUCCEEDED` au moteur commun;
9. persister et publier `ARMED`;
10. confirmer l'activation à l'écran.

Room et le snapshot Direct Boot sont deux stockages distincts: seule l'écriture Room de l'étape 4 est une transaction unique. La copie Direct Boot qui la suit n'est pas garantie atomique avec elle. Si le processus est interrompu entre les deux, Room fait foi au prochain démarrage et `SessionReconciler` réécrit le snapshot Direct Boot à partir de Room, ce que la section 13 du contrat autorise tant que `domainRevision` n'est pas régressée. Un Direct Boot en retard d'une écriture ne doit jamais faire perdre l'alarme programmée: `SystemEventsReceiver` retombe sur Room dès que `UserManager.isUserUnlocked == true`.

En cas d'exception, annuler le `PendingIntent`, retirer uniquement le blocage créé par la transaction, envoyer `ACTIVATION_FAILED` avec `failureCode`, persister `FAILED` et supprimer le pointeur de session active.

Cette transition vers `FAILED` n'est autorisée que pendant `PREPARING`. Une erreur survenue après le passage à `ARMED` crée un `SessionIncident`, dégrade la santé seulement pour une gravité `DEGRADED` ou `CRITICAL` et conserve l'état métier. Pendant `RELEASING`, elle ne restaure pas un blocage déjà retiré.

Au démarrage du processus, `SessionReconciler` traite tout état `PREPARING` resté incomplet. Il compare les effets natifs déjà appliqués, reprend la transaction de façon idempotente si les données sont cohérentes, ou exécute le rollback avant d'envoyer `ACTIVATION_FAILED` avec `failureCode`.

### 9.3 Reprogrammation

Créer un `SystemEventsReceiver`, déclaré dans le manifeste pour:

- `LOCKED_BOOT_COMPLETED`;
- `BOOT_COMPLETED`;
- `MY_PACKAGE_REPLACED`;
- `TIME_CHANGED` (action `android.intent.action.TIME_SET`);
- `TIMEZONE_CHANGED`.

**`USER_UNLOCKED` ne peut pas être déclaré dans le manifeste (constat de l'étape 19).** Android ne délivre `ACTION_USER_UNLOCKED` qu'aux receivers enregistrés à chaud; la documentation Direct Boot demande d'« enregistrer un `BroadcastReceiver` depuis un composant qui tourne ». Déclaré dans le manifeste, le filtre serait mort et la fusion Direct Boot vers Room n'aurait jamais lieu par ce chemin. Il est donc enregistré au démarrage du processus par `SystemEventsRegistrar`, avec `RECEIVER_NOT_EXPORTED`, au même titre que le receveur de filtre d'interruption de §13.1. Les cinq actions du manifeste sont, elles, bien délivrées à un receveur déclaré: quatre figurent dans la liste officielle des exceptions aux restrictions de broadcasts implicites, et `MY_PACKAGE_REPLACED` est explicitement adressé au paquet lui-même.

Le receiver est `directBootAware`. Il lit le snapshot et rappelle le programmateur avec le même `triggerAtEpochMillis`. Il ne recalcule pas l'instant depuis l'heure locale. Si la session est `ARMED` et l'instant est dépassé, il applique la politique de retard dans le coordinateur Direct Boot sous mutex: jusqu'à 15 minutes, il reprogramme une alarme immédiate dont `AlarmReceiver` produira `ALARM_FIRED`; au-delà, il applique `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW` dans le snapshot, le registre et l'outbox, exécute `PRESENT_SCAN_REQUEST` et publie la notification décrite en 10.5 depuis le contexte protégé par appareil, sans démarrer le service de sonnerie.

Sur `TIME_CHANGED` et `TIMEZONE_CHANGED`, l'alarme est réenregistrée **sans condition** au même instant, et un incident de gravité `WARNING` est consigné. Le réenregistrement est inconditionnel parce qu'un `PendingIntent` encore présent ne prouve pas que le système l'a conservé au bon instant après avoir déplacé son horloge; il est idempotent (`FLAG_UPDATE_CURRENT`).

**L'incident, lui, est consigné une seule fois par code et par session (décision de l'étape 19).** `android.intent.action.TIME_SET` n'est pas émis seulement quand l'utilisateur change l'heure: chaque correction d'horloge par le réseau le produit aussi, plusieurs fois par nuit sur certains appareils. Sans cette garde, une seule session accumulerait des dizaines d'incidents identiques sur l'écran de diagnostic — le défaut mesuré et corrigé à l'étape 16. La garde se lit en base, comme celle de §13.1 le fait pour ses propres incidents, et non en mémoire: ces deux raisons n'arrivent que par broadcast, donc parfois dans un processus qui vient de naître. Conséquence assumée: deux changements de fuseau dans la même session ne laissent qu'un incident. Le journal technique, lui, n'est pas dédupliqué.

Au déverrouillage, le réconciliateur fusionne de façon idempotente le registre et l'outbox Direct Boot dans Room, puis réécrit la projection depuis Room (§9.2). La fusion refuse une `domainRevision` inférieure à celle de Room pour la même session (§7.3), n'insère que les reçus et les effets absents, et ne fait jamais redevenir rejouable un effet déjà terminal. Elle s'exécute avant la lecture de Room par la passe, sous le mutex du coordinateur, sans quoi la réconciliation déciderait sur un état amputé de ce qui a été fait avant le déverrouillage.

**Elle est tentée sur trois raisons et non sur la seule `USER_UNLOCKED`:** ce signal n'atteint le processus que s'il était vivant à l'instant du déverrouillage, ce qui est le cas qui compte — un composant `directBootAware` a réveillé Niumi parce que le réveil est passé — mais pas le seul possible. `BOOT_COMPLETED`, délivré après le déverrouillage, et le démarrage du processus servent de filet. La fusion étant idempotente et bornée à la lecture d'un fichier quand il n'y a rien à absorber, la tenter trois fois ne coûte rien.

## 10. Déclenchement et service de sonnerie

### 10.1 Chaîne d'exécution

```text
AlarmManager
  -> AlarmReceiver
  -> événement KMP ALARM_FIRED
  -> AlarmRingingService en premier plan
  -> notification de catégorie ALARM avec full-screen intent
  -> AlarmActivity
  -> Reader Mode NFC
  -> parseur et vérificateur NiumiCore
  -> NfcVerificationProof opaque
  -> événement KMP VALID_NFC_SCANNED
  -> effets de libération Android
```

`AlarmReceiver` ne fait aucun travail long. Il transmet `ALARM_FIRED` à `NiumiCoreFacade`, puis le coordinateur exécute les effets retournés. La commande explicite envoyée au service contient l'identifiant de session, la révision et les données minimales nécessaires. Le déclenchement d'une alarme exacte demandée par l'utilisateur autorise le démarrage du service au premier plan depuis l'arrière-plan.

**Implémentation (étape 17).** Le receiver est une coquille sans décision : il lit ses extras, ouvre `goAsync()` et délègue à `AlarmTriggerHandler` (`:core:system`), ce qui rend toute la chaîne prouvable en JVM. Il ne démarre plus le service lui-même — `START_RINGING` est un effet du moteur, exécuté par `StartRingingExecutor` — et c'est ce qui fait que `RINGING` n'est jamais écrit par un composant Android.

**Garde de révision : une garde de monotonie, pas d'égalité.** L'extra `revision` du `PendingIntent` est figé au moment de `SCHEDULE_ALARM` et n'est réécrit que si cet effet est rejoué. Or la révision du snapshot avance sans cela : `INCIDENT_REPORTED` l'incrémente et ne produit aucun `SCHEDULE_ALARM` (SPEC_CORE_KMP 6), et le réconciliateur ne reprogramme que si l'alarme a disparu. Un extra en retard est donc le cas **courant** dès que la surveillance de 13.1 a signalé quoi que ce soit — et 13.1 surveille six contrôles pendant `ARMED`. Refuser une révision inférieure rendrait le réveil muet précisément quand l'appareil est déjà dégradé, ce qui serait le pire résultat possible pour un produit de réveil.

Les trois règles sont donc :

| Comparaison | Décision |
| --- | --- |
| `sessionId` différent du snapshot actif | aucun dispatch, `ALARM_RECEIVED` journalisé avec `sessionId` nul |
| révision de l'intent **supérieure** à celle du snapshot | aucun dispatch : la persistance est en retard sur ce qui a été programmé (snapshot Direct Boot périmé, restauration) |
| révision **inférieure ou égale** | dispatch de `ALARM_FIRED` avec `expectedRevision = snapshot.revision` |

L'état source n'est pas contrôlé côté Android : `TriggerReducer.onAlarmFired` exige déjà `ARMED` et refuse le reste (SPEC_CORE_KMP 5.1). Le dupliquer contredirait la règle « ne jamais dupliquer une règle commune ».

La fenêtre de `goAsync()` est bornée à 8 s. Au dépassement, la coroutine est annulée et `finish()` appelé ; si l'annulation tombe après le `commit` du coordinateur, les effets restent `PENDING` et la prochaine réconciliation les rejoue (SPEC_CORE_KMP 6.1). **Aucun événement technique n'est journalisé dans ce cas** : 17 est une liste fermée et aucune de ses 26 valeurs ne décrit ce fait. C'est une limite d'observabilité assumée, pas un oubli.

### 10.2 AlarmRingingService

Le service doit:

- être déclaré avec `foregroundServiceType="mediaPlayback"`;
- appeler `startForeground()` immédiatement;
- retourner `START_STICKY`;
- reconstruire son état depuis le snapshot si le processus est recréé;
- exécuter l'effet KMP `START_RINGING` de façon idempotente, sans écrire directement `RINGING`;
- acquérir un `PARTIAL_WAKE_LOCK` avec un délai de sécurité renouvelable et le libérer à la fin;
- lire une sonnerie locale empaquetée dans l'APK;
- boucler jusqu'à la validation NFC;
- activer une vibration répétée si l'option est active;
- maintenir une notification persistante sans action d'arrêt;
- arrêter le son, la vibration et le wake lock dans `onDestroy()` comme filet de sécurité;
- journaliser les erreurs audio sans terminer silencieusement la session.

Le moteur audio utilise `MediaPlayer` ou `AudioTrack` derrière l'interface `AlarmAudioEngine`. Pour le MVP, préférer `MediaPlayer` avec une ressource locale et les attributs suivants:

```kotlin
AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()
```

Demander le focus audio avec les mêmes attributs. Ne pas dépendre d'un fichier distant, d'une URI réseau ou d'un fournisseur de documents.

Règle Android 17: le son du réveil doit toujours utiliser `USAGE_ALARM`. L'application doit conserver son éligibilité et son accès aux alarmes exactes afin de rester dans le comportement prévu par Android pour l'audio d'alarme en arrière-plan.

Le service ne doit exposer aucune action `STOP` dans l'intent, la notification ou le binding.

Tant que la session est en `RINGING`, le service doit garantir que l'écran de réveil reste atteignable. Depuis Android 14, l'utilisateur peut rejeter la notification d'un service au premier plan malgré `setOngoing(true)`: les drapeaux `ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE` n'y suffisent plus. Le service doit donc vérifier périodiquement, via `NotificationManager.getActiveNotifications()`, que sa notification est toujours postée, et la republier avec son `fullScreenIntent` si elle a disparu. Le plein écran est le seul mécanisme qu'Android autorise pour ouvrir une activité depuis l'arrière-plan; c'est par lui que l'écran de réveil revient, que l'appareil soit verrouillé ou non.

Cette exigence n'est pas cosmétique. Le Reader Mode NFC ne peut vivre que dans une activité au premier plan (11.2): si l'écran de réveil disparaît pendant que l'alarme sonne, l'utilisateur n'a plus aucun moyen de terminer sa session par le scan. Trois situations ordinaires ont été mesurées à l'étape 6 où l'écran disparaît: le mode Ne pas déranger en silence total, le balayage de la notification par l'utilisateur, et le simple déverrouillage de l'écran. Voir `docs/android/implementation-reports/LOT-0.md`.

**Republication selon l'état de l'appareil (étape 17).** Republier à l'identique satisferait cet alinéa et violerait 10.4, qui interdit de ramener l'écran « de force en boucle ». Les deux ne se contredisent que si l'on ignore **qui** a fait disparaître la notification :

- notification présente : rien ;
- absente, **appareil verrouillé ou écran éteint** : republication **avec** `fullScreenIntent`. C'est le scénario même pour lequel cet alinéa existe — l'utilisateur dort, et le plein écran est le seul mécanisme qu'Android autorise pour rouvrir une activité depuis l'arrière-plan ;
- absente, **appareil déverrouillé et en cours d'usage** : republication **sans** `fullScreenIntent`, avec un `contentIntent` qui ouvre l'écran de réveil. L'utilisateur est réveillé et l'a écartée délibérément ; la notification revient, l'accès au scan est préservé (11.2), l'écran n'est pas imposé.

Un `contentIntent` n'est pas une action au sens de 10.2 : il n'apparaît pas comme un bouton et ne termine aucune session. Période de vérification : **dix secondes**.

Une garde « plein écran une seule fois » avait été retenue d'abord. Elle est **écartée** : mesurée sur appareil à l'étape 17, elle pouvait être consommée par une absence transitoire de `getActiveNotifications()`, si bien que la première disparition réellement subie — celle qui compte, pendant le sommeil — n'obtenait plus que la republication silencieuse. Le critère d'état de l'appareil n'a rien à épuiser. Il suit aussi le comportement réel du système, qui n'honore un `fullScreenIntent` que si l'appareil est verrouillé ou l'écran éteint, et le dégrade ailleurs en notification « heads-up ».

**Reconstruction après une mort de processus (étape 17).** `onStartCommand` avec `intent == null` passe d'abord au premier plan avec une notification silencieuse — `startForeground()` ne peut pas attendre une lecture suspendue — puis applique cette table, dérivée du seul snapshot persisté :

| État lu | Action |
| --- | --- |
| `RINGING` | reprendre l'audio, republier la notification plein écran, journaliser `PROCESS_RECREATED` |
| `ARMED` | `reconcile(SERVICE_RECREATED)` puis `stopSelf()` |
| `PREPARING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC`, `RELEASING`, état final | `stopSelf()` sans toucher à l'état |
| aucune session | `stopSelf()` : refuser de sonner |
| snapshot **illisible** | ne pas sonner, ne rien effacer, `reconcile(SERVICE_RECREATED)` puis `stopSelf()` |

Le cas illisible n'était couvert par aucune des deux issues possibles : sonner exposerait l'utilisateur à une alarme **sans bouton d'arrêt** pour une session peut-être terminée, s'arrêter risque de ne pas réveiller. Ne pas sonner est retenu ; le blocage est conservé (18), et le prochain `PROCESS_START` ou `USER_UNLOCKED` tranchera.

`SERVICE_RECREATED` n'est pas redondant avec `PROCESS_START` : `START_STICKY` peut relancer le service **sans** recréer le processus, cas où `NiumiApplication.onCreate` ne s'exécute pas.

**La réconciliation est le second filet, et il est nécessaire (étape 17).** `START_STICKY` ne suffit pas : mesuré sur appareil, HyperOS n'a rejoué **aucun** redémarrage du service après un crash du processus. Le rejeu de l'outbox ne rattrape pas ce cas, `START_RINGING` y étant déjà `SUCCEEDED`. Sans reprise explicite, la sonnerie s'arrêtait définitivement alors que la session restait active et le blocage en place — le réveil se taisait sans que rien ne le signale. `SessionReconciler` relance donc `START_RINGING` chaque fois qu'il trouve l'état `RINGING`. L'appel est idempotent : c'est le chemin emprunté à chaque `onStartCommand` valide, et le moteur audio ne double jamais le son.

**Ce que cela ne garantissait pas avant l'étape 20.** Encore fallait-il que le processus revienne à la vie : la reprise n'avait lieu qu'à la prochaine réconciliation, donc au prochain réveil de Niumi — l'utilisateur ouvrant l'application, un redémarrage, un remplacement de paquet. Si rien ne réveillait le processus, le réveil restait muet.

**Alarme de secours (étape 20).** `RingingWatchdogReceiver` réveille le processus toutes les 60 secondes tant que la session est `RINGING`, via `AlarmManager.setExactAndAllowWhileIdle()` (dérogation à §9.1, strictement limitée à ce watchdog) et un `PendingIntent` dont le code de requête est distinct de celui du réveil (`RingingWatchdogSpecs`, target `RingingWatchdogReceiver`). Une seule alarme de secours à la fois : `StartRingingExecutor` l'arme à l'entrée en `RINGING`, `SessionReconciler` la réarme à chaque passe trouvant encore cet état (après avoir relancé le son, jamais avant), et `StopRingingExecutor` la désarme avant même de tenter l'arrêt du service — si celui-ci échoue, l'alarme de secours doit malgré tout disparaître. Chaque tic ne fait que rejouer une passe ordinaire (`ReconcileReason.RINGING_WATCHDOG`) : c'est la réconciliation, pas le receveur, qui décide de tout — relancer le son si la session sonne encore, réarmer le prochain tic, ou se désarmer si l'état a changé. Contrepartie et réserve : voir §4.2, §9.1.

### 10.3 Notification et plein écran

Créer un canal `niumi_alarm_ringing`:

- importance haute;
- catégorie `CATEGORY_ALARM`;
- visibilité publique;
- vibration contrôlée par le service;
- son du canal désactivé pour éviter une double lecture;
- texte: "Alarme Niumi en cours";
- sous-texte: "Scanne ton boîtier pour terminer la session.";
- `setOngoing(true)`;
- `setFullScreenIntent(fullScreenPendingIntent, true)`.

Sur Android 14 et plus, vérifier `NotificationManager.canUseFullScreenIntent()`. Si l'autorisation manque, le diagnostic classe le problème comme `BLOCKING_FOR_NIUMI_EXPERIENCE` et propose l'intent `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.

Le plein écran n'est pas nécessaire au déclenchement sonore lui-même. Il est toutefois requis par la politique produit du MVP pour présenter immédiatement l'interface de scan. Cette distinction doit apparaître dans le diagnostic et les événements techniques afin de faciliter le support.

### 10.4 AlarmActivity

L'activité doit:

- appeler `setShowWhenLocked(true)`;
- appeler `setTurnScreenOn(true)`;
- rester utilisable en mode bord à bord;
- ne pas arrêter le service dans `onStop()` ou `onDestroy()`;
- **rendre le retour prédictif inerte tant que la session attend un scan** (resserré à l'étape 17). Un appui distrait pendant que l'alarme sonne ne doit pas écarter le seul écran qui porte le Reader Mode (11.2). Hors état de scan — écran en cours de chargement, session absente ou terminée — le retour ferme normalement, sans jamais modifier la session. Ce n'est pas l'« activité impossible à quitter » que cette même section interdit : Home, le geste de navigation, les récents et le volet de notifications restent tous disponibles, et rouvrir Niumi ramène à l'écran de réveil. La sortie reste donc volontaire et toujours possible, seule la voie du retour est neutralisée;
- être re-présentée tant que la session sonne: si l'activité est détruite ou quittée alors que l'état est `RINGING`, le service la ramène par le plein écran de sa notification (10.2). L'utilisateur peut écarter l'écran volontairement; il ne doit jamais perdre tout accès au scan. L'écran n'est pas ramené de force en boucle: une activité impossible à quitter serait hostile et contraire aux règles de Google Play;
- recalculer son état quand le verrouillage de l'appareil change, et pas seulement dans `onResume()`: un écran affiché au-dessus du verrouillage reste visible après le déverrouillage, et le texte « Déverrouille ton téléphone, puis approche-le du boîtier. » (11.2) doit disparaître dès que la condition est fausse. Écouter `ACTION_USER_PRESENT`, **à tous les niveaux d'API**. `KeyguardManager.addKeyguardLockedStateListener` (API 34+) était proposé ici comme alternative: **il est interdit**. Il exige `SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE`, absente de la liste figée de 14, et l'appeler sans elle lève une `SecurityException` non rattrapable qui tue le processus — donc la sonnerie — à l'instant précis où le plein écran ouvre l'écran de réveil. Mesuré sur appareil à l'étape 17: l'alarme a sonné une seconde avant que le processus ne meure, le son était inaudible, et le service d'accessibilité s'est délié dans la foulée. La permission n'est pas ajoutée: `ACTION_USER_PRESENT` couvre le besoin réel — faire disparaître un texte quand l'utilisateur déverrouille — sans élargir la surface de permissions;
- afficher l'heure, l'état du NFC et l'instruction de scan;
- activer le Reader Mode dans `onResume()`;
- le désactiver dans `onPause()`;
- rouvrir l'écran de réveil si l'état commun est `RINGING`; afficher la progression de nettoyage si l'état est `RELEASING`; afficher le mode scan sans audio si l'état est `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC`. Ouvrir Niumi depuis le lanceur pendant une session active mène toujours à l'écran correspondant à l'état, jamais à l'accueil: c'est la seconde garantie d'accès au scan, indépendante de la notification;
- **tant que la session attend un scan, aucun autre écran de Niumi n'est atteignable** (resserré à l'étape 17 après mesure sur appareil). La redirection vers l'écran 8 est portée par `MainActivity` et **observée en continu** tant qu'elle est au premier plan, jamais par un écran du `NavHost` ni par le seul `onResume`: deux mesures l'ont imposé. Posée sur l'accueil, elle ne s'exécutait jamais — après l'armement le `NavHost` est sur `ActiveSession`, donc l'accueil n'est plus composé. Posée sur `onResume` seul, elle ne se déclenchait pas quand l'alarme sonnait **pendant** que l'utilisateur était déjà dans Niumi: `onResume` ne se rejoue pas, et le plein écran de la notification n'est honoré par Android que si l'appareil est verrouillé ou l'écran éteint. Le bouton principal de l'accueil y mène aussi: sans cela, un retour depuis l'écran de réveil laissait l'utilisateur sur l'écran 7, qui n'active pas le Reader Mode (11.2) et n'offre donc aucun chemin vers le scan. Le retour prédictif de l'écran de réveil renvoie au **lanceur** et non à l'accueil — sinon l'accueil redirigerait aussitôt, produisant la boucle que cet alinéa interdit. **Quitter Niumi reste possible à tout instant**: la restriction porte sur les écrans de Niumi, jamais sur l'appareil. Verrouiller l'appareil entier exigerait le mode `lock task` (provisionnement « device owner ») et détournerait le service d'accessibilité de l'usage déclaré à Play (12.3): c'est exclu;
- ne contenir aucun bouton d'arrêt.

**Implémentation (étape 17).** L'état affiché vient du moteur (`SessionSnapshotDto`), jamais d'une phase devinée. Deux règles encadrent sa lecture :

- **un publisher à `null` ne ferme jamais l'écran.** `SessionSnapshotPublisher` vit en mémoire et vaut `null` dans tout processus neuf — et le processus est **toujours** neuf quand le plein écran ouvre l'activité après un réveil. L'activité lit donc la persistance d'abord, puis suit le flux. Seule une absence **confirmée** par la persistance ferme l'écran ; un snapshot illisible ne la confirme pas (SPEC_CORE_KMP 13) ;
- **ouvrir Niumi depuis le lanceur pendant `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou `RELEASING` mène à l'écran 8**, et non à l'écran 7. L'écran 7 n'active pas le Reader Mode (11.2) : y renvoyer pendant la sonnerie serait un cul-de-sac, alors que c'est justement le cas mesuré à l'étape 6 où l'activité de réveil est détruite par un déverrouillage. La redirection est acquittée dès qu'elle a eu lieu : quitter l'écran 8 ne doit pas le rouvrir, l'utilisateur pouvant l'écarter volontairement. Une nouvelle ouverture depuis le lanceur redirige de nouveau — l'accès au scan n'est jamais perdu.

Un état final ne ferme pas l'écran : il mène à l'écran 10 (`COMPLETED`) ou 11 (`CANCELLED`). `AlarmActivity` vivant dans sa propre tâche, hors du `NavHost`, elle y parvient en rouvrant `MainActivity` avec un extra de destination, le même mécanisme que le tap d'un avertissement (13.1).

Texte principal si l'état est `RINGING`:

> Scanne ton boîtier Niumi pour arrêter l'alarme.

Texte principal si l'état est `TRIGGERED_AWAITING_NFC`:

> L'heure de ton réveil est passée. Scanne ton boîtier Niumi pour débloquer tes applications.

`TRIGGERED_AWAITING_NFC` couvre aussi bien une alarme jamais déclenchée qu'une alarme déclenchée puis manquée avant observation par le coordinateur; le texte ne doit pas affirmer que le son n'a jamais sonné.

`AWAITING_NFC` n'est atteint sur Android que si le coordinateur reçoit `ALARM_SOUND_STOPPED`, ce qui n'arrive pas dans le parcours normal puisque Android ne fournit aucun bouton d'arrêt (section 7.1). L'écran suivant reste défensif, au cas où cet événement serait produit par une réconciliation ou un test.

Texte principal si l'état est `AWAITING_NFC`, après un arrêt du son:

> Le son est arrêté, mais tes applications restent bloquées. Scanne ton boîtier Niumi pour terminer la session.

Si le téléphone doit être déverrouillé:

> Déverrouille ton téléphone, puis approche-le du boîtier.

### 10.5 Notification d'attente de scan

`TRIGGER_ELAPSED` et `ALARM_SOUND_STOPPED` exécutent l'effet commun `PRESENT_SCAN_REQUEST`. Il se traduit par une notification persistante distincte de celle de la sonnerie, publiée que l'écran soit ouvert ou non.

Créer un canal `niumi_session_awaiting_scan`:

- importance haute;
- catégorie `CATEGORY_ALARM`;
- visibilité publique;
- aucun son, aucune vibration;
- aucun `setFullScreenIntent()`: cette notification ne doit jamais rallumer l'écran ni simuler une alarme active, en cohérence avec la fenêtre de 15 minutes qui empêche justement une sonnerie tardive;
- titre: "Ton réveil Niumi est passé";
- texte: "Scanne ton boîtier pour débloquer tes applications.";
- `setOngoing(true)`;
- `setOnlyAlertOnce(true)`: la notification est republiée à chaque réconciliation (voir ci-dessous), et sans cet attribut un canal d'importance haute reproduirait une bannière alors qu'elle n'a jamais disparu;
- aucune action d'arrêt;
- au tap, ouvrir `AlarmActivity` en mode scan.

La notification est publiée par l'exécution de `PRESENT_SCAN_REQUEST` et retirée par `CLEAR_SCAN_REQUEST`, tous deux idempotents. Lorsque `TRIGGER_ELAPSED` est produit par le coordinateur Direct Boot avant le premier déverrouillage, la notification est publiée depuis le `DeviceProtectedStorageContext`, au même titre que le snapshot Direct Boot.

**`setOngoing(true)` ne rend plus cette notification non-écartable, et ne doit pas être tenu pour tel.** Mesuré le 2026-09-14 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0 (étape 18): l'utilisateur l'a balayée d'un geste, et aucune notification Niumi n'est restée active. Depuis Android 14, `FLAG_ONGOING_EVENT` seul ne suffit plus — la non-écartabilité exige un service de premier plan actif, que cette notification n'a pas et ne doit pas avoir: §10.5 exige précisément qu'elle ne ressemble pas à une alarme active, et l'attente d'un scan peut durer des heures.

L'attribut reste prescrit (il conserve son effet sur les versions antérieures et sur le classement de la notification), mais **le filet réel est la republication**: `SessionReconciler` republie `PRESENT_SCAN_REQUEST` à chaque passe sur une session `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC`. La republication n'a lieu qu'à une réconciliation — démarrage de processus, redémarrage, événements système — et non à chaque passage de l'application au premier plan: une fenêtre sans rappel visible subsiste donc entre un balayage et la réconciliation suivante.

Cette fenêtre n'interrompt aucun chemin de sortie: l'overlay de blocage (§12.2) rappelle explicitement le scan au moment où l'utilisateur rencontre une application bloquée, et ouvrir Niumi dans un état de scan mène directement à l'écran de réveil (§10.4) — les deux vérifiés sur appareil.

**Sa durée a été mesurée à l'étape 19, et elle est illimitée en usage ordinaire.** Le 2026-09-14 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0: après un balayage, la notification est restée absente 523 s sans revenir, et rien ne la ramenait. Ni le passage de l'application au premier plan, ni un verrouillage puis déverrouillage d'écran — `ACTION_USER_UNLOCKED` n'est émis qu'au **premier** déverrouillage après un démarrage. Seuls un démarrage de processus, un redémarrage de l'appareil ou un changement d'horloge republiaient. Le pari de l'étape 18 — « `SystemEventsReceiver` multiplie les occasions de réconciliation et devrait resserrer la fenêtre » — est donc démenti par la mesure: les occasions qu'il ajoute sont toutes liées au démarrage ou à l'horloge, et aucune ne survient pendant qu'on se sert du téléphone.

**Décision prise sur cette mesure (étape 19): la réconciliation est déclenchée au passage de l'application au premier plan quand la session attend un scan** (`ReconcileReason.FOREGROUND`). La republication redevient ainsi liée au seul geste que l'utilisateur fait forcément pour sortir de sa session. Mesurée à 1 s après réouverture.

Le déclencheur est posé sur **`MainActivity` et `AlarmActivity`**, et non sur la seule `MainActivity`. Dans un état de scan, rouvrir Niumi ramène le task au premier plan avec l'écran de réveil au sommet (`launchMode="singleTask"`): `MainActivity.onResume` n'est alors jamais rejoué. Mesuré: posé sur la seule `MainActivity`, le correctif ne se déclenchait pas.

**La même raison couvre un second cas depuis l'étape 20 : aucun snapshot encore publié dans ce processus.** `SessionSnapshotPublisher` repart à `null` à chaque démarrage de processus, et `SessionStartupReconciler` le republie en tâche de fond — un `onResume` peut donc survenir avant cette republication, typiquement un processus recréé après une mort pendant `RINGING` (§10.2). `SessionReadinessWatcher.evaluate()` déclenche alors une réconciliation complète (`ReconcileReason.FOREGROUND`) plutôt que de sortir silencieusement : c'est le seul moyen de découvrir la session dans ce processus avant le prochain déclencheur (démarrage, redémarrage, événement système). Renommée `FOREGROUND_AWAITING_SCAN` → `FOREGROUND` pour refléter ce périmètre élargi ; le comportement sur un état de scan est inchangé.

L'option d'adosser la notification à un service de premier plan reste **écartée**: l'attente d'un scan peut durer des heures, et §10.5 exige que cette notification ne ressemble pas à une alarme active.

Le comportement de `CATEGORY_ALARM` sans son sous Ne pas déranger varie selon la version Android et les surcouches OEM; ce point fait partie de la matrice de tests physiques.

## 11. NFC

### 11.1 Association

L'association se fait dans une activité au premier plan avec `NfcAdapter.enableReaderMode()`. Le parcours d'association est accessible uniquement en dehors d'une session active; l'écran d'association n'est pas atteignable pendant `PREPARING`, `ARMED`, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou `RELEASING`, afin que le boîtier figé dans la session en cours ne puisse jamais être remplacé avant sa fin.

Format NDEF MVP:

```text
niumi://box/v1/{boxId}?token={base64urlToken}
```

Contraintes:

- `boxId` est un UUID canonique minuscule au format `8-4-4-4-12`;
- `token` contient exactement 16 octets aléatoires encodés en Base64 URL sans padding, soit 22 caractères;
- la query contient uniquement `token`, présent une fois;
- le payload ne contient ni utilisateur, ni mot de passe, ni port, ni fragment, ni encodage par pourcentage;
- le payload ne dépasse pas 96 octets UTF-8 et ne contient aucun caractère de contrôle;
- l'application stocke `SHA-256` des 16 octets décodés, et non le token en clair, dans Room;
- le snapshot Direct Boot reçoit le même hash;
- un seul boîtier est associé dans le MVP;
- toute nouvelle association remplace l'ancienne après confirmation.

Le tag MVP reste clonable par une personne qui lit puis recopie son contenu. La protection contre le clonage exige un tag cryptographique capable de produire une preuve dynamique. Elle appartient à une version ultérieure du produit.

### 11.2 Lecture pendant la sonnerie

Activer au minimum les technologies compatibles avec le tag matériel retenu. Pour un tag NFC Type 2 classique, utiliser `FLAG_READER_NFC_A` et lire le premier enregistrement NDEF URI reconnu.

`NfcAdapter.enableReaderMode(Activity, ...)` exige une `Activity` au premier plan et n'offre aucune variante utilisable depuis un `Service`. Le Reader Mode ne peut donc pas être déplacé hors de `AlarmActivity`, alors qu'il porte le seul moyen de terminer une session. La garantie de scan repose par conséquent entièrement sur la présence de l'écran de réveil, assurée par 10.2 et 10.4: si cet écran disparaît, le scan devient inopérant sans qu'aucune erreur ne soit produite. Toute évolution qui retirerait la re-présentation de l'écran doit d'abord proposer un autre chemin de sortie de session.

Le lecteur Android transmet l'URI brute au parseur de `:shared:core`. Il ne duplique aucune règle de validité. Le parseur commun doit:

- rejeter les schémas et hôtes inconnus;
- rejeter les versions de protocole non prises en charge;
- vérifier la forme canonique de l'UUID;
- borner la taille du payload à 96 octets UTF-8;
- décoder le token avec un parseur strict;
- exiger exactement 16 octets après décodage;
- comparer le hash avec une fonction en temps constant;
- ne jamais écrire le token dans les logs;
- ignorer les enregistrements supplémentaires pour la décision d'arrêt.

Résultats UI:

| Résultat | Effet |
| --- | --- |
| tag valide et associé | transmettre la `NfcVerificationProof` opaque avec `VALID_NFC_SCANNED`, puis exécuter les effets KMP |
| tag Niumi non associé | vibration courte d'erreur, alarme maintenue |
| tag illisible | message "Boîtier non reconnu. Réessaie." |
| NFC désactivé | ouvrir une explication et un raccourci vers les réglages NFC |
| matériel absent | appareil non pris en charge |

### 11.3 Fin ou annulation de session

`HandleValidNfcUseCase` doit être idempotent et s'exécuter sous le même mutex que `AlarmReceiver` et `SessionReconciler`. Avant d'envoyer `VALID_NFC_SCANNED`, il réconcilie l'heure contractuelle et l'état AlarmManager: si la session est encore `ARMED` après l'heure sans alarme observée, il envoie d'abord `TRIGGER_ELAPSED`. Le moteur choisit ensuite la cible finale selon l'état source:

| État source | Cas | État intermédiaire | Cible finale |
| --- | --- | --- | --- |
| `ARMED` | annulation ou modification avant le réveil | `RELEASING` | `CANCELLED` |
| `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` | scan pendant ou après la sonnerie | `RELEASING` | `COMPLETED` |

Ordre logique:

1. vérifier que l'état est `ARMED`, `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC`;
2. si l'état est `ARMED`, réconcilier l'heure et AlarmManager avant le scan;
3. créer l'identifiant d'événement et l'horodatage, puis valider le boîtier scanné contre `boxId` et `boxTokenSha256Hex` de la session active (jamais contre `PairedBoxEntity`), avec le parseur et le vérificateur communs, qui retournent une `NfcVerificationProof` opaque liée à la session et à la révision attendue;
4. envoyer `VALID_NFC_SCANNED` avec cette preuve;
5. persister atomiquement `nfcVerifiedAtEpochMillis`, `releaseTarget`, `RELEASING`, le reçu et l'outbox;
6. publier la même `domainRevision` dans le snapshot Direct Boot et Room quand Room est accessible;
7. annuler l'alarme système et les `PendingIntent` de la session;
8. arrêter le moteur audio, la vibration et le wake lock s'ils sont actifs;
9. supprimer la notification et arrêter le service;
10. retirer la liste de blocage active;
11. vérifier les résultats natifs observables;
12. envoyer `RELEASE_SUCCEEDED` au moteur commun seulement lorsque les effets requis ont réussi;
13. persister `CANCELLED` ou `COMPLETED` et son horodatage;
14. effacer le pointeur et le snapshot actifs après réconciliation.

Les effets requis pour `RELEASE_SUCCEEDED` sont l'annulation de l'alarme (étape 7), l'arrêt du son et de la vibration (étape 8) et le retrait de la liste de blocage (étape 10); la suppression de la notification et l'arrêt du service restent best-effort et consignés en cas d'échec sans bloquer la phase. Si le service d'accessibilité a déjà été désactivé par l'utilisateur avant le scan, le retrait du blocage est considéré satisfait dès que `NiumiBlockingAccessibilityService` n'est plus actif, avec un incident `BLOCKING_PERMISSION_REVOKED` consigné, plutôt que de bloquer indéfiniment `RELEASING`.

**Écart corrigé à l'étape 11 (2026-09-10) :** ce paragraphe classait l'arrêt du son en best-effort, alors que SPEC_CORE_KMP §6 le range parmi les effets requis de `RELEASE_SUCCEEDED` (`STOP_RINGING`). Les deux textes se contredisaient. Le contrat commun l'emporte : déclarer une session terminée pendant que le réveil sonne encore serait le pire résultat possible pour un produit de réveil, et l'outbox existe précisément pour rejouer un arrêt du son resté en échec (§6.1) — la règle d'échappement (`AlreadySatisfied` quand le moteur audio ne joue déjà plus) évite tout blocage indu de `RELEASING`. Ce paragraphe est corrigé en conséquence ; voir `docs/android/implementation-reports/ETAPE-11.md`.

Si un effet requis échoue alors que sa précondition tient toujours, envoyer `RELEASE_FAILED`, enregistrer `RELEASE_PARTIAL_FAILURE` et conserver `RELEASING`. `SessionReconciler` compare l'état natif au snapshot et reprend uniquement les effets manquants, sans réappliquer un blocage déjà retiré. L'application ne présente pas la session comme terminée avant `RELEASE_SUCCEEDED`.

Si le même événement est traité de nouveau, le registre retourne le reçu sans rappeler le moteur ni répéter les effets déjà satisfaits. Si Room n'est pas accessible avant déverrouillage, le snapshot, son registre et son outbox font foi. La mise à jour Room est différée jusqu'à `USER_UNLOCKED` ou au prochain démarrage.

## 12. Blocage des applications

### 12.1 Sélection des applications

Construire la liste avec `PackageManager.queryIntentActivities()` pour un intent `ACTION_MAIN` et `CATEGORY_LAUNCHER`. Ajouter une section `<queries>` ciblée dans le manifeste. Ne pas demander `QUERY_ALL_PACKAGES`.

Dédupliquer par nom de package. Afficher l'icône, le libellé et le nom de package en petit texte si plusieurs applications ont le même libellé.

La sélection doit contenir entre 1 et 50 applications. La règle appartient à `:shared:core`; l'écran Android bloque la confirmation pour 0 ou 51 applications.

Le sélecteur n'est pas accessible pendant une session active. La sélection associée à une session en cours ne peut être modifiée qu'après un scan valide, en cohérence avec l'association du boîtier en 11.1.

Exclure:

- le package Niumi;
- les détenteurs du rôle Home;
- le package de l'activité Réglages résolue par le système;
- l'interface système;
- le composeur téléphonique et les composants d'urgence;
- toute application sans activité de lancement.

**Comment ces exclusions sont réellement obtenues (mesuré à l'étape 13).** `RoleManager.getRoleHolders()` est `@SystemApi` et exige la permission `MANAGE_ROLE_HOLDERS`, réservée au système ; l'API publique `isRoleHeld()` ne renseigne que sur l'application appelante. Les rôles ne sont donc pas interrogeables et les exclusions passent par des intents publics : les détenteurs du rôle Home par `queryIntentActivities(ACTION_MAIN + CATEGORY_HOME)` — tous les lanceurs installés, pas seulement celui par défaut, pour qu'un changement de lanceur pendant une session ne rende pas l'appareil inutilisable ; les Réglages par `resolveActivity(ACTION_SETTINGS)` ; le composeur par `TelecomManager.getDefaultDialerPackage()` (publique, sans permission) complété par `resolveActivity(ACTION_DIAL)` ; l'interface système par son package `com.android.systemui`. « Toute application sans activité de lancement » est garanti par la requête elle-même.

**Limite assumée : l'application d'urgence.** Aucune API publique ne permet de l'identifier (`RoleManager.ROLE_EMERGENCY` n'est lisible que par le système). Elle n'est exclue que dans la mesure où elle coïncide avec le composeur par défaut, ce qui est le cas sur AOSP et sur la plupart des surcouches. Sur un appareil où une application d'urgence distincte serait lançable, elle resterait proposable au blocage. Le recours reste celui de §4.2 — extinction ou arrêt forcé — et les appels d'urgence du système, hors application, ne sont jamais affectés par l'`AccessibilityService` de §12.2, qui ne fait que superposer un overlay.

### 12.2 AccessibilityService

Déclarer `NiumiBlockingAccessibilityService` avec la permission système `BIND_ACCESSIBILITY_SERVICE` et `isAccessibilityTool=false`.

Configuration minimale:

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="50"
    android:canRetrieveWindowContent="false"
    android:isAccessibilityTool="false"
    android:description="@string/niumi_accessibility_service_description" />
```

`android:description` est obligatoire en pratique: les réglages d'accessibilité du système et la fiche Play l'affichent à l'utilisateur au moment où il accorde l'autorisation. Son texte doit rester cohérent avec l'écran de consentement de 12.3.

Le service doit uniquement lire `event.packageName`. Il ne doit pas parcourir l'arbre d'accessibilité, lire le texte affiché, inspecter les saisies ou transmettre des événements à un serveur.

Aucune exception ne doit sortir de `onAccessibilityEvent`. Une exception non rattrapée y provoque l'arrêt forcé de l'application, et Android retire alors le service de la liste des services activés: le blocage disparaît entièrement, sans que l'utilisateur en soit informé autrement que par une notification système d'arrêt. Toute opération susceptible d'échouer, en particulier l'ajout de la fenêtre d'overlay, doit renvoyer un résultat typé et être journalisée, jamais propagée.

Algorithme:

```text
à chaque changement de fenêtre
  lire le package au premier plan
  si aucune session ARMED, RINGING, AWAITING_NFC, TRIGGERED_AWAITING_NFC ou RELEASING: ne rien faire
  si la session est RELEASING: lire la liste de blocage effective et les effets de libération en attente
  si aucune liste de blocage effective: ne rien faire
  si le package n'est pas bloqué: ne rien faire
  si le package est bloqué:
    si le même package a déjà été bloqué il y a moins d'une seconde: ne rien faire
    exécuter GLOBAL_ACTION_HOME
    afficher brièvement un TYPE_ACCESSIBILITY_OVERLAY explicatif
    journaliser BLOCK_APPLIED avec le package uniquement
```

L'anti-rebond d'une seconde est nécessaire: une application au premier plan émet des rafales d'événements, et sans lui chaque événement déclencherait un `GLOBAL_ACTION_HOME` et une entrée `BLOCK_APPLIED`, saturant en quelques secondes le journal borné à 200 entrées de 17.

Utiliser `TYPE_ACCESSIBILITY_OVERLAY`. La fenêtre doit être ajoutée avec le contexte du service d'accessibilité lui-même: lui seul porte le token autorisant ce type de fenêtre, et le contexte d'application produit une `BadTokenException`. Ne pas démarrer une `Activity` depuis l'arrière-plan pour bloquer une application. Il ne doit pas rendre tout le téléphone inutilisable.

L'overlay disparaît après une durée maximale de 3 secondes. Il ne doit pas être retiré au motif que le package bloqué n'est plus au premier plan: Niumi vient précisément de renvoyer l'utilisateur à l'accueil, ce qui produit immédiatement un événement pour le lanceur et effacerait l'overlay avant qu'il ne soit lisible. Seule l'expiration du délai, une interruption du service ou son débranchement retirent l'overlay.

Texte de l'overlay:

> {Nom de l'application} reste bloquée jusqu'au scan du boîtier.

Le service doit recharger l'état actif depuis Room ou le snapshot après recréation. Si le service est désactivé pendant une session, Niumi doit le détecter à sa prochaine exécution et afficher un incident. Il ne doit pas tenter de réactiver le service ou d'empêcher l'utilisateur d'accéder aux réglages.

#### Reconstruction de la projection de blocage

La liste de blocage lue par le service est reconstruite depuis la persistance, jamais conservée en mémoire comme seule source de vérité: un processus tué ferait sinon disparaître le blocage d'une session encore active, ce que 13 interdit. La lecture emploie Room une fois l'appareil déverrouillé, le snapshot Direct Boot de 7.3 avant.

`onAccessibilityEvent` doit décider immédiatement, sur le thread principal, alors que toute lecture de persistance est asynchrone. La projection est donc gardée en cache et réalignée sur la persistance à deux moments: à la connexion du service, et à chaque décision de session publiée.

L'état de la session ne suffit pas à décider seul, SPEC_CORE_KMP 4 rappelant que `RELEASING` autorise un nettoyage partiel. Le statut des effets de blocage tranche:

| État persisté | Effet déterminant | Projection |
| --- | --- | --- |
| `PREPARING` | `APPLY_BLOCKING` réussi | blocage actif |
| `PREPARING` | `APPLY_BLOCKING` en attente, échoué ou absent | aucun blocage |
| `ARMED`, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` | — | blocage actif |
| `RELEASING` | `REMOVE_BLOCKING` réussi ou satisfait | libération totale, plus aucun package |
| `RELEASING` | `REMOVE_BLOCKING` en attente, échoué ou absent | blocage maintenu sur tous les packages |
| `COMPLETED`, `CANCELLED`, `FAILED`, ou pointeur absent | — | aucun blocage |

La distinction sur `PREPARING` est nécessaire: une activation interrompue laisse les lignes de sélection en base sans que le blocage ait été posé, et la réconciliation de 9.2 déduit de la projection si l'activation a abouti.

Dans Room, l'absence de ligne d'effet signifie « jamais décidé ». Dans le snapshot Direct Boot, qui ne recopie que les effets rejouables, l'absence signifie au contraire « déjà exécuté ».

Un snapshot illisible ne se lit jamais « aucune session »: la lecture le signale comme tel et la projection conserve ce qu'elle savait. Le blocage n'est jamais levé faute de pouvoir lire.

### 12.3 Information et consentement

Avant d'ouvrir les réglages d'accessibilité, afficher une page dédiée qui explique:

- que Niumi observe le nom de l'application affichée;
- que cette information sert uniquement à renvoyer les applications choisies vers l'accueil;
- que Niumi ne lit pas le contenu des écrans ou les saisies;
- que le service peut être désactivé dans les réglages Android;
- qu'une session ne peut pas être activée si le service est inactif.

Le bouton peut être intitulé "Ouvrir les réglages d'accessibilité". Ne jamais simuler un consentement ou cliquer à la place de l'utilisateur.

L'acceptation de cet usage par Google Play n'est pas considérée comme acquise. Le POC doit inclure la déclaration Play Console et le texte de divulgation et de consentement intégré. La vidéo montrant le parcours réel et la soumission sont tournées et déposées sur l'application complète, une fois le POC supprimé (Lot 5, décision du 2026-09-07 — voir §22 et `docs/android/implementation-reports/LOT-0.md`), le POC de debug ne représentant pas fidèlement le parcours utilisateur. Cette validation de politique reste un risque produit bloquant, quel que soit le lot où elle est instruite.

## 13. Diagnostic avant activation

Créer `DeviceReadinessChecker` qui renvoie une liste typée de contrôles. Les valeurs de `ReadinessSeverity` proviennent de `:shared:core`:

```kotlin
enum class ReadinessSeverity {
    BLOCKING_FOR_ALARM,
    BLOCKING_FOR_NIUMI_EXPERIENCE,
    WARNING
}
```

`DeviceReadinessChecker` conserve la détection et les actions Android. Il convertit chaque résultat vers le DTO commun avant que `NiumiCoreFacade.evaluateActivation()` décide si l'activation est permise.

- `BLOCKING_FOR_ALARM`: le réveil sonore fiable ne peut pas être programmé dans le périmètre garanti.
- `BLOCKING_FOR_NIUMI_EXPERIENCE`: le son peut techniquement fonctionner, mais le parcours Niumi du MVP ne peut pas être garanti.
- `WARNING`: l'activation reste possible, avec une information claire.

La politique produit du MVP refuse l'activation pour les deux niveaux bloquants.

| Contrôle | Niveau | Détection | Action proposée |
| --- | --- | --- | --- |
| NFC présent | `BLOCKING_FOR_NIUMI_EXPERIENCE` | `PackageManager.FEATURE_NFC` | appareil non compatible |
| NFC activé | `BLOCKING_FOR_NIUMI_EXPERIENCE` | `NfcAdapter.isEnabled` | réglages NFC |
| boîtier associé | `BLOCKING_FOR_NIUMI_EXPERIENCE` | dépôt local | lancer l'association |
| applications choisies | `BLOCKING_FOR_NIUMI_EXPERIENCE` | sélection non vide | ouvrir le sélecteur |
| alarme exacte disponible | `BLOCKING_FOR_ALARM` | `canScheduleExactAlarms()` | diagnostic d'incompatibilité ou de déclaration |
| plein écran autorisé | `BLOCKING_FOR_NIUMI_EXPERIENCE` à partir d'Android 14 | `canUseFullScreenIntent()` | réglages plein écran |
| notifications autorisées | `BLOCKING_FOR_NIUMI_EXPERIENCE` à partir d'Android 13 | permission + état du canal | demander la permission |
| canal d'alarme actif | `BLOCKING_FOR_NIUMI_EXPERIENCE` | `NotificationChannel` | réglages du canal |
| volume alarme supérieur à zéro | `BLOCKING_FOR_ALARM` | `AudioManager` | réglages du son |
| Ne pas déranger en silence total | `BLOCKING_FOR_ALARM` | `NotificationManager.getCurrentInterruptionFilter() == INTERRUPTION_FILTER_NONE` | réglages Ne pas déranger, avec explication |
| Ne pas déranger dans un autre mode | `WARNING` | filtre d'interruption courant | réglages Ne pas déranger et explication |
| service d'accessibilité actif | `BLOCKING_FOR_NIUMI_EXPERIENCE` | services activés | réglages d'accessibilité |
| date future valide | `BLOCKING_FOR_ALARM` | calcul métier | corriger l'heure |
| batterie optimisée | `BLOCKING_FOR_NIUMI_EXPERIENCE` | `PowerManager.isIgnoringBatteryOptimizations()`, détection partielle | aide OEM et parcours vers l'exemption |

Niumi déclare `USE_EXACT_ALARM`, car le réveil est une fonction centrale du produit. La spec ne doit pas ajouter `SCHEDULE_EXACT_ALARM` ni présenter l'accès aux alarmes exactes comme une permission utilisateur ordinaire. `canScheduleExactAlarms()` reste vérifié par sécurité. S'il renvoie `false`, l'application signale un état anormal, une incompatibilité ou un problème d'éligibilité. Elle ne redirige pas automatiquement vers les réglages "Alarmes et rappels" comme elle le ferait avec `SCHEDULE_EXACT_ALARM`.

La détection du mode Ne pas déranger varie selon la version Android et les surcouches. Le diagnostic doit signaler les états observables qui risquent de rendre l'alarme inaudible, sans promettre une analyse parfaite de toutes les configurations OEM. Les tests physiques restent la source de validation.

Le mode Ne pas déranger était classé `WARNING` en bloc jusqu'à la validation de l'étape 6. La mesure sur appareil réel a montré que ce niveau est faux pour le silence total, et seulement pour lui. Sur Redmi 25080RABDG, Android 16, avec un volume d'alarme réglé à 12 et l'écran éteint:

- `ZEN_MODE_IMPORTANT_INTERRUPTIONS` (interruptions prioritaires, `alarms=allow`) et `ZEN_MODE_ALARMS` (alarmes seules): `STREAM_ALARM` reste `Muted: false` à son volume réglé, le service démarre sans retard, l'écran se rallume et `AlarmActivity` s'affiche. Le parcours garanti est tenu.
- `ZEN_MODE_NO_INTERRUPTIONS` (silence total): le système force `STREAM_ALARM` à `Muted: true` avec `streamVolume:0` bien que le lecteur emploie `USAGE_ALARM`; l'écran reste éteint, `AlarmActivity` ne s'ouvre pas et, par voie de conséquence, le Reader Mode NFC n'est jamais activé. L'utilisateur n'est pas réveillé et ne peut pas terminer sa session par le scan tant qu'il n'ouvre pas lui-même l'application. Seules la notification et le service au premier plan subsistent.

Le contrôle du silence total est donc `BLOCKING_FOR_ALARM` et doit être réévalué à chaque activation, l'utilisateur pouvant l'enclencher entre deux sessions. Niumi ne demande pas `ACCESS_NOTIFICATION_POLICY` et ne modifie jamais le mode Ne pas déranger de l'utilisateur: le MVP refuse l'activation et l'explique, plutôt que de contourner un réglage système délibéré.

Si le silence total est enclenché après l'armement, le déclenchement ne peut plus être empêché ni rendu audible. `AlarmReceiver` relit alors le filtre d'interruption au moment de sonner et, s'il vaut `INTERRUPTION_FILTER_NONE`, crée un incident `ANDROID_ALARM_MUTED_BY_DND` de gravité `CRITICAL` — le réveil sonore a échoué, l'utilisateur doit le voir explicitement dans le diagnostic d'incident. L'événement technique `ALARM_MUTED_BY_DND` est journalisé (17). La session reste active et le blocage est conservé: l'échec est sonore, pas métier. Voir `docs/android/implementation-reports/LOT-0.md` pour les mesures.

Ce comportement est établi sur HyperOS. Les autres surcouches peuvent traiter le silence total différemment; le contrôle reste bloquant dans tous les cas et la matrice physique documente les écarts au fur et à mesure.

L'optimisation de batterie était classée `WARNING` "sans blocage par défaut" jusqu'à la validation de l'étape 5. La mesure sur appareil réel a montré que cette hypothèse est fausse pour le blocage d'applications: sur HyperOS, tant que Niumi reste soumis aux restrictions de batterie, le système gèle son processus environ une minute après son passage en arrière-plan et cesse de lui remettre les événements d'accessibilité. Le service continue d'apparaître comme actif dans les réglages, le processus reste vivant, aucune erreur n'est produite, mais le blocage devient silencieusement inopérant. Le cas correspond exactement à l'usage réel: l'utilisateur engage sa session, pose son téléphone, puis tente d'ouvrir une application bloquée. Le contrôle est donc bloquant et l'onboarding doit conduire l'utilisateur jusqu'au réglage d'exemption avant la première session. Voir `docs/android/implementation-reports/ETAPE-05.md` pour les mesures.

L'exemption ne survit pas à une réinstallation ni, selon toute vraisemblance, à une mise à jour depuis le store: la politique d'énergie de l'application est repassée d'elle-même en mode restrictif après plusieurs réinstallations pendant la validation de l'étape 5, et le blocage a cessé de fonctionner en conséquence. Le diagnostic ne peut donc pas se contenter d'un contrôle à l'onboarding: il doit être réévalué à chaque activation de session, et l'aide doit prévenir l'utilisateur qu'une mise à jour de Niumi peut réinitialiser ce réglage. C'est un point de fragilité produit à part entière, à couvrir explicitement pendant la campagne de bêta-test.

La détection de ce contrôle est structurellement partielle et la spec ne doit pas prétendre le contraire. `PowerManager.isIgnoringBatteryOptimizations()` n'observe que la liste blanche AOSP. Sur HyperOS, le réglage qui commande réellement le gel est propre à la surcouche ("Économiseur de batterie" de l'application, à passer sur "Aucune restriction"): une fois ce réglage modifié et le blocage rétabli, `isIgnoringBatteryOptimizations()` continue de renvoyer `false` et `dumpsys deviceidle whitelist` ne contient toujours pas l'application. Le diagnostic ne peut donc pas conclure que tout va bien; il doit demander l'exemption dans tous les cas, guider vers le réglage OEM lorsqu'il est identifiable, et considérer ce contrôle comme non satisfait tant que l'utilisateur n'a pas confirmé l'avoir fait.

Ce comportement est établi sur HyperOS. Les autres surcouches appliquent des politiques d'énergie différentes, à documenter au fur et à mesure de leur couverture; l'exemption est demandée dans tous les cas, la gravité du contrôle restant la même.

L'écran n'affiche qu'une action principale à la fois, en commençant par le premier blocage. Il doit recalculer l'état après chaque retour des réglages.

**Exception : les deux étapes de parcours (étape 13, mesurée sur appareil).** `PAIRED_BOX` et `APP_SELECTION` ne décrivent pas l'état de l'appareil mais un choix de l'utilisateur ; ils gardent donc un recours **même satisfaits**, sous forme d'action secondaire attachée à leur ligne (« Changer de boîtier », « Modifier ma sélection »), avec un libellé distinct de celui de la première fois — « Associer mon boîtier » sous une ligne verte affirmerait le contraire de la vérité. Sans cette exception, leur bouton disparaissait avec leur échec et les écrans 3 et 4 devenaient définitivement inatteignables, alors que 11.1 autorise explicitement une nouvelle association et 12.1 une nouvelle sélection tant qu'aucune session n'est en cours. Les contrôles de blocage, eux, n'ont rien à rouvrir une fois satisfaits : ils restent sans action. Cette distinction est la même que celle qui sépare déjà `journeyChecks()` du reste du diagnostic, ces deux contrôles étant convertis vers les champs dédiés d'`ActivationPolicyInputDto` et non vers sa liste `checks`.

**Exception : la continuation vers le choix de l'heure (étape 14).** Quand plus aucun contrôle bloquant n'échoue, l'écran affiche un bouton « Choisir mon heure de réveil » qui mène à l'écran 5, en plus de l'action principale éventuelle. Ce n'est pas une remédiation mais la suite du parcours, au même titre que les deux exceptions ci-dessus. Sa condition d'affichage est « aucun contrôle bloquant en échec », et non « activation autorisée » : la politique commune refuse l'activation par `TRIGGER_NOT_IN_FUTURE` tant qu'aucune heure n'a été choisie, c'est-à-dire précisément tant que l'utilisateur n'a pas suivi ce bouton. Sans lui, l'écran 5 serait inatteignable : le recours `FixTime` du contrôle « date future valide » n'apparaît jamais avant qu'une heure candidate existe, ce contrôle étant alors `NOT_APPLICABLE` et donc masqué. Le recours `FixTime` reste utile pendant la surveillance d'une session armée (13.1), où une heure candidate existe.

**Implémentation (étape 12).** `DeviceReadinessChecker` (`:core:system.readiness`) renvoie les quatorze contrôles dans l'ordre du tableau ci-dessus, chacun avec une issue à trois valeurs — `PASSED`, `FAILED`, `NOT_APPLICABLE` — et non un booléen. Trois points méritent d'être fixés ici plutôt que laissés au code.

1. **Un contrôle sans objet n'est ni réussi ni échoué.** `NOT_APPLICABLE` couvre l'autorisation plein écran avant Android 14 (l'API n'existe pas), l'activation du NFC sur un appareil sans matériel NFC (proposer d'activer ce qui n'existe pas serait un faux recours), et la validité de l'instant de réveil tant qu'aucune heure n'a été choisie — l'écran de diagnostic précède le choix de l'heure dans le parcours. Un contrôle `NOT_APPLICABLE` est exclu de `ActivationPolicyInputDto` : il ne peut ni bloquer ni rassurer.

2. **Les trois contrôles de parcours ne transitent pas par la liste `checks`.** « boîtier associé », « applications choisies » et « date future valide » sont affichés comme les onze autres, mais convertis vers les champs dédiés `hasPairedBox`, `appSelectionCount` et `triggerAtEpochMillis` d'`ActivationPolicyInputDto`. La politique commune les refuse déjà avec `NO_PAIRED_BOX`, `INVALID_APP_SELECTION` et `TRIGGER_NOT_IN_FUTURE` (SPEC_CORE_KMP §7.4, §10) ; les verser aussi dans `checks` ferait remonter deux refus pour une seule cause, l'un précis et l'autre générique. Un diagnostic lancé avant tout choix d'heure reçoit donc `triggerAtEpochMillis = nowEpochMillis` et se voit refuser l'activation par `TRIGGER_NOT_IN_FUTURE`, ce qui est le verdict exact à ce stade.

3. **Le contrôle d'énergie repose sur la confirmation de l'utilisateur, pas sur la détection.** Puisque `isIgnoringBatteryOptimizations()` n'observe que la liste blanche AOSP et reste faux après correction du réglage OEM sur HyperOS, exiger qu'il soit vrai rendrait l'activation impossible sur ces appareils ; s'en contenter laisserait passer un appareil qui gèle Niumi. Le contrôle est donc satisfait quand, et seulement quand, l'utilisateur a confirmé avoir levé les restrictions — confirmation persistée hors Room, réévaluée à chaque diagnostic. La valeur renvoyée par `isIgnoringBatteryOptimizations()` ne décide de rien : elle choisit le recours proposé, demande d'exemption AOSP tant qu'elle est fausse, guide vers le réglage OEM une fois acquise.

**Messages (étape 12b).** §13 n'illustrait que cinq messages pour quatorze contrôles ; les cinq restent identiques et les neuf autres sont fixés ici. Chacun nomme le réglage en cause **et** sa conséquence, en tutoiement (15). Les textes vivent dans `:feature:setup` (`readiness/ReadinessMessages.kt`), jamais dans `:core:system`, qui reste sans interface.

| Contrôle | Message |
| --- | --- |
| NFC présent | Cet appareil n'a pas de puce NFC. Niumi ne peut pas fonctionner sans boîtier à scanner. |
| NFC activé | Le NFC est désactivé. Active-le avant de démarrer la session. |
| boîtier associé | Aucun boîtier n'est associé. Associe ton boîtier Niumi : c'est lui qui terminera ta session. |
| applications choisies | Aucune application n'est choisie. Sélectionne celles que Niumi bloquera pendant ta session. |
| alarme exacte disponible | Niumi ne peut pas programmer ce réveil, car l'accès aux alarmes exactes n'est pas disponible sur cet appareil. |
| plein écran autorisé | Autorise les alarmes plein écran, sinon l'écran de réveil ne s'ouvrira pas tout seul au moment de sonner. |
| notifications autorisées | Active les notifications pour que l'écran du réveil puisse s'afficher. |
| canal d'alarme actif | Le canal de notification du réveil est désactivé. Réactive-le, sinon la sonnerie ne pourra pas démarrer. |
| volume alarme supérieur à zéro | Le volume des alarmes est à zéro. Augmente-le avant de continuer. |
| Ne pas déranger en silence total | Le silence total coupe le son des alarmes et empêche l'écran de réveil de s'afficher. Désactive-le avant de démarrer la session. |
| Ne pas déranger dans un autre mode | Le mode Ne pas déranger peut empêcher la sonnerie d'être audible. Vérifie qu'il autorise les alarmes. |
| service d'accessibilité actif | Le service d'accessibilité de Niumi est inactif. Sans lui, les applications choisies ne seront pas bloquées. |
| date future valide | L'heure de réveil choisie est déjà passée. Choisis une heure future. |
| batterie optimisée | Les restrictions de batterie peuvent geler Niumi et désactiver le blocage sans prévenir. Lève-les, puis confirme ici que c'est fait. |

**Recours système (étape 12b, précisé à l'étape 16).** Les `Intent` de réglages ne sont jamais construits par `DeviceReadinessChecker` : celui-ci ne décrit que le recours, et la traduction en `Intent` appartient à l'appelant.

La formulation initiale plaçait cette traduction « dans `:feature:setup`, jamais dans `:core:system`, qui reste sans interface ». **Cette justification était fausse et a été corrigée à l'étape 16 :** `:core:system` manipule déjà `AlarmManager`, `NfcAdapter`, `Settings.Secure` et `AudioManager` ; un `Intent` de réglages n'est pas de l'interface, un texte affiché si. `settingsIntentFor` vit donc dans `:core:system` (`readiness/ReadinessSettingsIntents.kt`), à côté de `ReadinessAction` qu'elle traduit, et les deux modules qui en ont besoin y accèdent — l'écran 2 (`:feature:setup`) et l'écran 7 (`:feature:session`, remédiation des incidents, 15). Sans ce déplacement, l'écran 7 aurait exigé une dépendance `feature → feature` que 6 n'autorise pas, ou une seconde table de traduction. Les **textes** restent dans les modules `feature` (`ReadinessMessages`, `explanationFor`).

Trois choix méritent d'être fixés ici.

- **Ne pas déranger.** Le SDK n'expose aucune action publique ouvrant l'interrupteur lui-même ; `Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS` est la plus proche. Niumi amène l'utilisateur devant le réglage et ne le modifie jamais, faute de demander `ACCESS_NOTIFICATION_POLICY`.
- **Exemption d'énergie.** `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` n'est **jamais** émise : elle exige la permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, restreinte par Google Play, que Niumi ne déclare pas et dont il n'a pas besoin puisque c'est l'utilisateur qui confirme. Tant que la liste blanche AOSP manque, l'écran ouvre `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (liste système) ; une fois acquise, il ouvre la fiche de l'application, où vit le réglage de la surcouche qui commande réellement le gel.
- **Alarmes exactes.** Aucune branche ne produit `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, un test instrumenté le prouve en énumérant toutes les actions. Un `canScheduleExactAlarms()` faux n'affiche qu'une explication.

**Présentation (étape 12b).** L'écran n'attache un bouton qu'au premier contrôle en échec ; les autres ne sont qu'affichés. Un recours dont l'écran n'existe pas encore laisse le bouton inactif plutôt que de promettre une destination absente (15, « ne jamais afficher un faux état de fiabilité »). Le contrôle d'énergie procède en deux temps sous une seule action à la fois : ouvrir le réglage, puis confirmer au retour.

### 13.1 Surveillance pendant une session armée

Le diagnostic ne sert pas qu'à autoriser l'activation. Un réglage modifié après l'armement peut rendre le réveil inaudible ou le parcours inopérant sans que rien ne le signale, et l'utilisateur ne le découvrirait qu'au matin. `DeviceReadinessChecker` est donc réexécuté pendant la vie d'une session, et tout contrôle bloquant qui devient faux alors que la session est `ARMED` produit un incident et une notification d'avertissement.

Le service d'accessibilité fait exception au périmètre `ARMED`: il est surveillé dans **tous** les états non finaux. Le blocage court jusqu'au scan du boîtier (3), donc bien après le réveil, et 12.2 exige que sa désactivation pendant une session soit détectée et présentée. Les cinq autres contrôles restent limités à `ARMED`: une fois la sonnerie commencée, avertir d'un volume d'alarme ou d'un plein écran perdu ne décrit plus rien d'actionnable, et un tel avertissement encore affiché deviendrait mensonger. Un contrôle qui sort ainsi du périmètre voit sa notification retirée.

Contrôles surveillés, avec le code d'incident associé:

| Contrôle devenu faux | Code d'incident | Gravité | Surveillé pendant |
| --- | --- | --- | --- |
| Ne pas déranger passé en silence total | `ANDROID_ALARM_MUTED_BY_DND` | `CRITICAL` | `ARMED` |
| Volume d'alarme tombé à zéro | `ANDROID_ALARM_VOLUME_ZERO` | `CRITICAL` | `ARMED` |
| Notifications révoquées | `ANDROID_NOTIFICATIONS_REVOKED` | `CRITICAL` | `ARMED` |
| Plein écran révoqué | `ANDROID_FULL_SCREEN_REVOKED` | `CRITICAL` | `ARMED` |
| Service d'accessibilité désactivé | `BLOCKING_PERMISSION_REVOKED` (code commun) | `CRITICAL` | tous les états non finaux |
| Accès aux alarmes exactes perdu | `ALARM_PERMISSION_REVOKED` (code commun) | `CRITICAL` | `ARMED` |
| NFC désactivé | `NFC_DISABLED` (code commun) | `CRITICAL` | `ARMED`, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC`, `RELEASING` |

**La ligne NFC n'est pas portée par `SessionReadinessMonitor` (étape 20).** Elle est produite par `SessionRuntimeReconciler` (§7.1, §18), un chemin distinct pour deux raisons : le périmètre diffère de `ARMED` seul — le scan reste le seul chemin de sortie d'une session tant qu'elle n'est pas terminée (11.2), donc l'avertissement doit rester actionnable dans `RINGING` comme ailleurs — et aucune réparation n'est possible côté NFC, contrairement aux cinq contrôles du tableau ci-dessus qui ont chacun leur écran de réglages. `PREPARING` en est exclu : une activation interrompue n'a jamais promis de scan à l'utilisateur.

**Le contrôle du service d'accessibilité est neutralisé avant le premier déverrouillage (étape 19, mesuré sur appareil).** Android refuse de lier un service d'accessibilité qui n'est pas `directBootAware` (`Ignoring non-encryption-aware service`) et remet `accessibility_enabled` à 0 tant qu'aucun service ne tourne, alors même que le réglage choisi par l'utilisateur (`enabled_accessibility_services`) n'a pas bougé. Évalué tel quel pendant la réconciliation `LOCKED_BOOT`, le contrôle était doublement faux: il consignait un incident `BLOCKING_PERMISSION_REVOKED` `CRITICAL` mensonger avec sa notification (« Tes applications ne sont plus bloquées »), et surtout la garde de permission du réconciliateur interrompait la passe **avant la reprogrammation de l'alarme** — le réveil était perdu par le redémarrage même que §9.3 doit rattraper. Mesuré le 2026-09-14 sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.

Le contrôle est donc `NOT_APPLICABLE` tant que `UserManager.isUserUnlocked` est faux, et redevient évalué dès le déverrouillage. C'est la réponse honnête: le blocage n'a de toute façon aucun objet avant le déverrouillage, l'utilisateur ne pouvant atteindre aucune application. L'écran de diagnostic n'est atteignable qu'appareil déverrouillé et ne voit donc jamais ce cas.

Quand la surveillance s'exécute:

- à chaque réconciliation, quelle qu'en soit la raison — `reconcileArmed` l'appelle sans condition sur `reason` avant sa propre garde de permission ; l'énumérer devient inexact à chaque raison ajoutée, ce qui s'est produit quatre fois depuis l'étape 12 (`BEFORE_SCAN`, `SERVICE_RECREATED`, `FOREGROUND`, `RINGING_WATCHDOG`);
- à chaque passage de l'application au premier plan (`SessionReadinessWatcher`, §10.5) ;
- immédiatement, par un `BroadcastReceiver` enregistré à chaud, pour les seuls changements qu'Android diffuse publiquement, au premier rang desquels `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED`;
- au déclenchement, avant de démarrer la sonnerie.

**Limite à ne pas masquer.** L'avertissement est émis au plus tôt, jamais garanti immédiat. Entre l'armement et la sonnerie, Niumi n'a aucun composant garanti en vie: le processus est régulièrement tué par le système, ce qui a été mesuré à l'étape 6. Un receiver enregistré à chaud disparaît avec lui, et aucun broadcast public n'existe pour plusieurs de ces réglages, notamment le volume. Si l'utilisateur modifie un réglage alors que Niumi est mort, l'avertissement n'arrive qu'au réveil suivant du processus. L'aide et l'onboarding doivent l'énoncer ainsi, sans promettre une surveillance continue.

Le service d'accessibilité ne doit jamais servir de sentinelle pour cette surveillance, bien qu'il soit le seul composant Niumi vivant en continu pendant une session. Son usage déclaré à Google Play (12.3) est le blocage d'applications et la lecture du seul `event.packageName`; l'employer à observer des réglages système contredirait cette déclaration.

Notification d'avertissement: créer un canal `niumi_session_warning`, importance haute, sans son ni vibration, sans `fullScreenIntent`, `setOngoing(false)`, au tap ouvrir le diagnostic d'incident. Le texte nomme le réglage en cause et sa conséquence, par exemple: « Ton réveil ne sonnera pas tant que le silence total est activé. » L'événement technique `SESSION_READINESS_DEGRADED` est journalisé (17).

**Implémentation (étape 12, corrigée à l'étape 16).** `SessionReadinessMonitor` (`:core:system.readiness`) rejoue `DeviceReadinessChecker` et ne signale un contrôle **qu'au basculement** : tant qu'il reste faux, il n'est pas re-signalé.

**La notification et l'incident n'ont pas le même cycle de vie.** L'étape 12 les traitait ensemble, sous une garde unique vivant en mémoire ; l'argument avancé — republier au redémarrage un avertissement encore valable vaut mieux que le taire — est juste pour la notification, mais avait été étendu à l'incident sans que la différence soit examinée. Un incident est un fait horodaté, pas un état d'affichage : le réécrire à chaque mort de processus consigne un basculement qui n'a pas eu lieu. Mesuré sur appareil à l'étape 16, où l'écran 7 présentait deux fois le même incident avec deux boutons identiques.

Depuis l'étape 16 :

- la **notification** garde sa déduplication en mémoire et est donc republiée après un redémarrage, comportement inchangé ;
- l'**incident** n'est enregistré qu'une fois par code et par session, la vérification se faisant sur les incidents déjà en base (`SessionIncidentsReader`).

Conséquence assumée : un contrôle réparé puis re-cassé dans la même session republie son avertissement sans produire de second incident. La santé est déjà `DEGRADED` et n'en revient jamais (SPEC_CORE_KMP 7.3) ; le journal technique, non dédupliqué, garde la trace horodatée de chaque détection. Avant déverrouillage, le lecteur d'incidents ne peut rien lire (7.3) : la déduplication est alors impossible et l'incident est enregistré — on ne perd jamais une dégradation pour cause de stockage indisponible.

Chaque contrôle surveillé porte son propre identifiant de notification : deux réglages cassés en même temps produisent deux avertissements distincts, aucun n'écrasant l'autre — **et chacun son incident**.

**Le snapshot avance d'un incident à l'autre dans une même passe (étape 17).** Chaque `INCIDENT_REPORTED` accepté incrémente la révision ; bâtir le second sur le snapshot du début de passe le fait rejeter en `STALE_REVISION`, **sans trace**. Mesuré sur appareil : le silence total fait aussi tomber le volume d'alarme à zéro, donc deux contrôles échouent d'un seul coup, et seul le premier incident était enregistré alors que le journal technique laissait croire que les deux l'avaient été. La surveillance reprend donc le snapshot rendu par chaque décision pour bâtir la suivante. La catégorie est `CATEGORY_ERROR` et non `CATEGORY_ALARM` : ces notifications signalent un réglage dégradé, jamais une alarme en cours, et les confondre ferait croire que le réveil sonne.

Le réconciliateur n'interrompt sa passe que pour les deux pertes de permission — accès aux alarmes exactes et service d'accessibilité — parce que poursuivre y serait absurde (reprogrammer une alarme exacte sans y avoir droit). Les quatre autres contrôles sont signalés sans interrompre la réconciliation : le réveil reste programmé, seules son audibilité ou sa présentation sont compromises. La condition porte sur l'état courant du contrôle, pas sur le fait qu'il vienne d'être signalé.

Le tap de la notification ouvre `MainActivity`, qui redirige vers le diagnostic d'incident (15, écran 12). Depuis l'étape 16 la redirection est effective : le `PendingIntent` porte un extra de destination, lu à `onCreate` **et** à `onNewIntent` — sans ce second point, un deuxième avertissement tapé pendant que l'application est au premier plan n'ouvrirait rien. Une valeur de destination inconnue, qu'un `PendingIntent` créé par une version antérieure peut porter, ramène à l'accueil plutôt que d'échouer.

**`MainActivity` doit être déclarée `android:launchMode="singleTop"`.** Mesuré sur appareil à l'étape 16 : en `launchMode` standard, Android ramène simplement la tâche au premier plan sans jamais appeler `onNewIntent`, et le tap restait alors sans effet dès que Niumi était déjà visible. Aucun test JVM ne peut couvrir ce point — la lecture de l'extra est une fonction pure, le comportement testé est celui du système.

**Implémentation du déclencheur « au déclenchement » (étape 17).** Il est porté par `AlarmTriggerHandler`, qui appelle `SessionReadinessMonitor` **avant** de dispatcher `ALARM_FIRED`. Deux points en découlent, et aucun n'est négociable :

- **avant, pas après.** Une fois `ALARM_FIRED` appliqué, l'état est `RINGING` et le périmètre surveillé retombe à l'accessibilité seule : l'incident `ANDROID_ALARM_MUTED_BY_DND` que 13 exige au déclenchement ne serait jamais créé ;
- **le snapshot est relu après la passe.** La surveillance peut dispatcher `INCIDENT_REPORTED`, donc incrémenter la révision ; bâtir `ALARM_FIRED` sur le snapshot d'avant produirait `STALE_REVISION` et l'alarme resterait muette.

Aucune seconde détection du filtre d'interruption n'est ajoutée dans le receiver. Le moniteur porte déjà la table code/gravité, la déduplication par session et la notification d'avertissement ; une voie parallèle recréerait exactement les doublons corrigés à l'étape 16. Contrairement au réconciliateur, le handler **n'interrompt jamais** sa chaîne sur un contrôle en échec : 13 impose que la session reste active et que le blocage soit conservé, l'échec étant sonore et non métier.

**Implémentation (étape 15).** Le déclencheur « passage au premier plan » est branché sur le `ON_RESUME` de l'écran de session active, qui est le seul écran affiché pendant qu'une session court (10.4). Il n'avait aucun appelant avant cette étape. Aucun composant distinct ne surveille le service d'accessibilité : `SessionReadinessMonitor` porte déjà le code d'incident et la garde de déduplication, et un second surveillant produirait deux incidents `CRITICAL` pour le même fait.

Exemples de messages:

- "Niumi ne peut pas programmer ce réveil, car l'accès aux alarmes exactes n'est pas disponible sur cet appareil."
- "Active les notifications pour que l'écran du réveil puisse s'afficher."
- "Le NFC est désactivé. Active-le avant de démarrer la session."
- "Le volume des alarmes est à zéro. Augmente-le avant de continuer."
- "Le mode Ne pas déranger peut empêcher la sonnerie d'être audible. Vérifie qu'il autorise les alarmes."

## 14. Manifeste Android

Permissions et fonctionnalité attendues:

```xml
<uses-feature
    android:name="android.hardware.nfc"
    android:required="true" />

<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
```

Ne pas déclarer `SCHEDULE_EXACT_ALARM` dans le MVP. Toute évolution de cette stratégie exige une modification explicite de la spec, du diagnostic et du parcours utilisateur.

**Visibilité des paquets (étape 13).** Le manifeste déclare en plus une section `<queries>` ciblée, limitée à `ACTION_MAIN` + `CATEGORY_LAUNCHER`, sans laquelle `queryIntentActivities()` ne renverrait rien depuis Android 11 et le sélecteur de §12.1 serait vide. Elle vit dans le manifeste de `:core:system`, avec le seul code qui en dépend (`PackageManagerPackageQuery`), et le manifeste fusionné la porte pour tous les variants. La permission de visibilité totale des paquets reste interdite : l'absence de toute permission de ce type dans le manifeste fusionné est un critère vérifié à chaque étape qui touche au sélecteur.

Composants:

| Composant | Export | Direct Boot | Notes |
| --- | --- | --- | --- |
| `MainActivity` | oui | non | intent launcher uniquement |
| `AlarmActivity` | non | oui | affichage verrouillé et Reader Mode |
| `AlarmReceiver` | non | oui | cible du `PendingIntent` explicite |
| `SystemEventsReceiver` | non | oui | broadcasts système et réconciliation après déverrouillage |
| `AlarmRingingService` | non | oui | `mediaPlayback` |
| `NiumiBlockingAccessibilityService` | oui | non | protégé par `BIND_ACCESSIBILITY_SERVICE` |

Tous les composants non destinés à des applications externes restent `exported=false`. Les intents internes sont explicites.

## 15. Interface minimale

Écrans à livrer:

1. accueil sans session;
2. diagnostic et onboarding des autorisations;
3. association du boîtier;
4. sélection des applications;
5. choix de la date et de l'heure;
6. récapitulatif d'engagement;
7. session active;
8. écran de réveil;
9. scan requis pour modifier ou annuler;
10. session terminée;
11. session annulée;
12. diagnostic d'incident;
13. aide et limites.

**Écran 13 — aide et limites (étape 21).** Écran de **consultation**, atteignable depuis l'accueil, en session comme hors session. Il ne porte aucune action: le scan du boîtier reste la seule sortie (3, 10.2), et un bouton y serait précisément le recours logiciel que 4.5 exclut. Son contenu est celui de `docs/android/LIMITES.md`, mot pour mot — le document est la version lisible hors de l'application, à laquelle renvoie la politique de confidentialité publiée sur la fiche Play, et l'écran en est la restitution, jamais une reformulation. Un test compare les deux à chaque build.

Il n'existait pas avant l'étape 21, alors que 4.2 exige depuis l'origine que l'arrêt forcé soit « signalé clairement dans l'aide » et que 21 en fasse un critère d'acceptation: jusque-là, seul l'onboarding portait ces limites, et il n'est présenté qu'une fois, avant la première session. L'aide reprend ses six limites **à l'identique** et y ajoute celles qui ne pouvaient pas être connues avant d'avoir été mesurées: le silence total (étape 6), la restriction OEM de démarrage automatique et sa portée réelle (4.2, étape 19), la notification d'attente de scan écartable depuis Android 14 (10.5, étape 19), le diagnostic NFC brièvement faux après un redémarrage (étape 19), et la perte possible du journal technique écrit avant le premier déverrouillage (17, étape 20).

**Étapes de livraison.** Les écrans n'arrivent pas tous en même temps et l'ordre d'implémentation (22) ne le disait pas explicitement : accueil (1) et diagnostic-onboarding (2) à l'étape 12b ; association (3) et sélection (4) à l'étape 13 ; choix de l'heure (5), récapitulatif (6) et une **version minimale** de la session active (7) à l'étape 14 ; session active complète (7), scan requis (9) et annulée (11) à l'étape 15 ; écran de réveil (8) livré dès l'étape 7 et branché sur l'état réel du moteur à l'étape 17 ; diagnostic d'incident (12) à l'étape 16 ; **session terminée (10) à l'étape 17**.

Cette liste annonçait l'écran 10 à l'étape 15 ; c'était faux, et la contradiction avec le plan avait été relevée à l'étape 16 sans être corrigée. Corrigé ici : l'écran 10 n'est atteint qu'après `COMPLETED`, donc après le scan de libération, et il est livré avec la chaîne du réveil. Comme l'écran 11 à l'étape 15, il est **livré et enregistré mais atteignable seulement à partir de l'étape 18**, qui apporte `HandleValidNfcUseCase` — le livrer maintenant n'annonce donc aucun faux succès.

Les écrans 10 et 11 vivent tous deux dans `:feature:session` : ils sont jumeaux, 15 les traite ensemble, et l'écran 10 n'a ni audio, ni NFC, ni service. Ils sont atteints depuis `AlarmActivity` par le mécanisme de destination de 13.1, l'écran de réveil vivant hors du `NavHost`. L'étape 12b déclare les destinations de navigation en une fois mais n'enregistre que celles dont l'écran existe : naviguer vers une route non enregistrée lève, ce qui vaut mieux qu'un écran vide donnant l'illusion d'une fonctionnalité livrée. Depuis l'étape 21, les quatorze destinations sont toutes enregistrées. Quatorze pour treize écrans : l'écran 2 en occupe deux (onboarding puis diagnostic), le consentement d'accessibilité de 12.3 en est une de plus, et l'écran de réveil vit hors du `NavHost`.

L'écran 7 minimal est livré à l'étape 14 et non à l'étape 15 parce que celle-ci est la première à rendre une session armable : sans lui, l'accueil deviendrait un cul-de-sac dès qu'une session existe, alors que 10.4 en fait la seconde garantie d'accès au scan, indépendante de la notification. Sa version minimale affiche l'état, la date, l'heure et le fuseau d'activation, plus l'heure recalculée dans le fuseau courant s'il diffère (8).

**Contenu de l'écran 7 complet (étape 15).** S'ajoutent à la version minimale :

- la liste des applications bloquées, nommées par le libellé **figé à l'activation** et jamais résolu au moment de l'affichage : une application désinstallée ou renommée pendant la session doit rester nommable (12.2) ;
- la santé de la session. Une session `DEGRADED` ne redevient jamais `HEALTHY` d'elle-même (SPEC_CORE_KMP 7.3) : le texte ne promet donc aucun retour à la normale ;
- les incidents, triés par gravité décroissante puis du plus récent au plus ancien. Les `CRITICAL` sont présentés **à part et en tête**, ce que SPEC_CORE_KMP 7.3 exige pour les distinguer d'un `DEGRADED` simplement consigné. Un code d'incident sans traduction reste affiché tel quel : le taire présenterait la session comme saine ;
- le bouton « Modifier ou annuler », seule action de sortie de session de l'écran, qui mène à l'écran 9.

**Remédiation des incidents sur l'écran 7 (étape 16).** Un incident `CRITICAL` présenté sans recours laisse l'utilisateur devant un constat qu'il ne peut pas lever : pendant une session, l'écran 7 est le seul écran atteignable (10.4), et il n'offre aucun moyen d'agir. L'écran 2 porte déjà la colonne « Action proposée » de 13, adossée à `ReadinessAction` ; l'écran 7 doit proposer la même action pour les incidents remédiables, au minimum « Ouvrir les réglages d'accessibilité » pour `BLOCKING_PERMISSION_REVOKED`.

Cette action n'est pas une exception à 3 : elle ne termine ni ne modifie la session, elle rétablit un sous-système. Le scan du boîtier reste le seul chemin de sortie, et l'écran 7 ne doit gagner aucune autre action **qui touche à la session**. Deux ajouts de l'étape 16 s'y conforment sans y déroger : le recours d'un incident, et l'accès en **consultation** au diagnostic d'incident (écran 12). Ni l'un ni l'autre ne dispatche d'événement ; le ViewModel de l'écran 7 n'a d'ailleurs ni coordinateur ni façade, et ne le **peut** donc pas.

**L'écran 7 présente l'état, l'écran 12 l'historique.** Un même code d'incident n'apparaît qu'une fois sur l'écran 7, dans sa forme la plus récente. La cause première des doublons est corrigée en 13.1 depuis l'étape 16 — un incident n'est plus enregistré qu'une fois par code et par session — mais l'écran garde cette règle de présentation : elle ne dépend d'aucune garantie d'écriture, et tient encore si un incident de même code arrive par un autre chemin. L'historique complet reste consultable sur l'écran 12, dont c'est la raison d'être.

Un incident remédiable porte l'action de la colonne « Action proposée » de 13 ; un incident sans recours n'affiche aucun bouton. Les recours que 13 décrit **sans réglage système à ouvrir** — `ShowExactAlarmDiagnostic` au premier chef, Niumi déclarant `USE_EXACT_ALARM` — n'en affichent pas non plus : le libellé de l'incident porte déjà l'explication, et un bouton sans destination serait le « faux état de fiabilité » que 15 interdit.

Le cas visé est d'abord le plus courant : l'utilisateur désactive le service d'accessibilité depuis les réglages Android, ce que l'onboarding annonce comme possible à tout moment (4.3). Une mort du processus de Niumi produit le même état — le service cesse d'être lié et `Settings.Secure.ACCESSIBILITY_ENABLED` retombe à `0`, ce que le diagnostic détecte correctement — mais reste un cas de défaut, non de fonctionnement normal : un service d'accessibilité lié maintient le processus à `PERCEPTIBLE_APP_ADJ`, bien au-dessus du seuil des processus recyclés en routine (mesuré à l'étape 15, voir `ETAPE-15.md`).

Une fois le service réactivé à la main, le blocage reprend de lui-même : le service reconstruit sa projection depuis Room à la reconnexion (12.2). Aucune action de Niumi ne doit tenter de réactiver le service à la place de l'utilisateur (12.2).

**Écran 12 — diagnostic d'incident (étape 16).** Écran de **consultation**. Il ne porte aucune action sur la session : le scan du boîtier reste le seul chemin de sortie (3, 10.2), et les recours vivent sur l'écran 7. Sa seule action est l'export, que 17 exige explicite.

Il présente, dans cet ordre :

- les incidents `CRITICAL` de la session, à part et en tête, ce que SPEC_CORE_KMP 7.3 exige d'un diagnostic visible par l'utilisateur ;
- l'état et la santé de la session, quand une session existe ;
- les quatorze contrôles de 13 avec leur résultat, relus à l'ouverture — `DeviceReadinessChecker` est sans état (13.1) et un résultat mis en cache afficherait une fiabilité périmée ;
- les incidents restants, triés par gravité décroissante puis du plus récent au plus ancien ;
- les événements techniques, bornés à 200 (17).

L'écran nomme les contrôles ; il ne reprend pas les messages de 13, rédigés comme des consignes d'activation (« Active-le avant de démarrer la session ») qui seraient faux pendant une session déjà armée. L'écran 12 constate, l'écran 7 porte le recours.

Un incident y est nommé **en clair, suivi de son code technique** : `Le service d'accessibilité a été désactivé : le blocage ne s'applique plus. (BLOCKING_PERMISSION_REVOKED)`. SPEC_CORE_KMP 7.3 veut un diagnostic « visible par l'utilisateur », ce que le code seul n'était pas (mesuré sur appareil à l'étape 16) ; 18 veut qu'« une erreur inconnue reçoive un identifiant local consultable dans le diagnostic », ce que le libellé seul retirerait à l'assistance. Les deux écrans partagent une table de libellés unique : ils ne peuvent pas nommer différemment le même fait.

Contrairement à l'écran 7, l'écran 12 **ne déduplique pas** : il liste tous les incidents enregistrés, du plus récent au plus ancien.

Il reste consultable **sans session active** : le journal technique et les contrôles gardent leur intérêt après une session terminée ou échouée. Il est atteignable depuis l'écran 7, depuis l'accueil, et par le tap d'une notification d'avertissement (13.1).

Le bouton d'export est « Exporter le diagnostic ». Il ouvre un `ACTION_SEND` texte, construit par l'écran comme tout `Intent` (13), et n'est jamais émis sans clic.

**Écrans 9 et 11 (étape 15).** L'écran 9 ne porte aucune action en dehors du scan : ni bouton d'annulation, ni confirmation, ni chemin de retour qui libérerait quoi que ce soit (3, 10.2). Son texte est :

> Scanne ton boîtier Niumi pour annuler ou modifier ta session. Tes applications resteront bloquées jusqu'au scan.

Un boîtier inconnu et un tag illisible ont chacun leur message et ne changent aucun état (11.2, SPEC_CORE_KMP 4). L'écran 11 n'est atteint qu'après un état final `CANCELLED`, donc après `RELEASE_SUCCEEDED` (11.3) : il peut affirmer que les applications sont débloquées sans mentir. Ses libellés sont « Session annulée » et « Préparer un nouveau réveil », ce dernier ramenant au diagnostic, entrée du parcours de préparation.

L'annulation par scan n'est pas fonctionnelle à l'étape 15 : `HandleValidNfcUseCase` arrive à l'étape 18. Jusque-là l'écran 9 délègue à un handler qui ignore tout scan, ce qui est le comportement attendu d'un scan non validé (SPEC_CORE_KMP 4) et n'annonce aucun succès.

L'écran 13 vit dans `:app` et non dans un module `feature` : il n'appartient à aucun parcours, il est atteint depuis l'accueil, et son seul contenu est un texte constant. Un neuvième module Gradle serait contraire à 6, et le placer dans `:feature:setup` le rendrait inatteignable depuis une session active sans créer une dépendance que 6 interdit.

Règles UI:

- utiliser le tutoiement partout;
- ne jamais afficher un faux état de fiabilité;
- **ne jamais affirmer « Aucune session » quand la persistance est illisible (étape 20, défaut mesuré sur appareil).** Room rendue illisible, l'accueil annonçait « Aucune session » alors qu'une session était armée, l'alarme programmée et le blocage en place: l'affirmation la plus rassurante était aussi la seule que Niumi n'était pas en mesure de faire. L'accueil affiche alors « État illisible », dit que le blocage tient et que le scan reste la seule sortie, et son bouton principal mène au diagnostic. Le signal vient de `StorageIntegrityState` (§18) et doit alimenter ce que l'écran **affiche**, pas seulement la destination de son bouton — celle-ci n'est lue qu'au clic, ce qui avait laissé le défaut invisible;
- afficher la date, l'heure et le fuseau de la session active;
- afficher la prochaine heure système calculée;
- ne jamais mettre une action d'arrêt dans l'écran de réveil;
- toujours afficher une notification de demande de scan tant que la session attend un scan sans sonnerie active, y compris si l'alarme n'a jamais sonné;
- expliquer les autorisations juste avant leur demande;
- conserver un contraste lisible la nuit;
- prendre en charge TalkBack même si l'application utilise elle-même un service d'accessibilité;
- gérer l'affichage bord à bord et le retour prédictif;
- ne pas verrouiller l'orientation.

## 16. Sécurité et confidentialité

- Tout le parcours critique fonctionne hors ligne.
- Aucun appel réseau ne participe à l'activation, au déclenchement, au blocage, au scan ou à la fin d'une session.
- Ne jamais journaliser le token NFC en clair.
- Ne jamais journaliser le contenu des événements d'accessibilité.
- Les composants internes utilisent des intents explicites et des `PendingIntent` immuables.
- Valider tous les extras reçus par les receivers et services.
- Refuser un `sessionId` inconnu ou qui ne correspond pas au snapshot actif.
- Borner la longueur du payload NFC avant toute allocation importante.
- Comparer les hashes avec `MessageDigest.isEqual()`.
- Aucun secret serveur n'est stocké dans l'APK.
- Le mode release active R8 et la suppression des ressources inutilisées.

## 17. Observabilité locale

Événements autorisés:

```text
SESSION_PREPARING
SESSION_ARMED
SESSION_RELEASING
SESSION_CANCELLED
ALARM_SCHEDULED
ALARM_RESCHEDULED
ALARM_RECEIVED
RINGING_STARTED
AUDIO_START_FAILED
FULL_SCREEN_DENIED
EXACT_ALARM_LOST
MISSED_TRIGGER_WINDOW
ALARM_MUTED_BY_DND
SESSION_READINESS_DEGRADED
SCAN_REQUEST_NOTIFIED
SCAN_REQUEST_CLEARED
NFC_DISABLED
NFC_SCAN_INVALID
NFC_SCAN_VALID
BLOCK_APPLIED
ACCESSIBILITY_DISABLED
PROCESS_RECREATED
OEM_RESTRICTION_SUSPECTED
SESSION_COMPLETED
SESSION_FAILED
RELEASE_PARTIAL_FAILURE
SNAPSHOT_CORRUPTED
```

Chaque événement contient seulement l'heure, le type, l'identifiant de session, le modèle de l'appareil, la version Android, la version de l'application et un code d'erreur contrôlé. Le nom de package est accepté uniquement pour `BLOCK_APPLIED`. Aucun événement n'est envoyé à distance dans le MVP.

**`SNAPSHOT_CORRUPTED` (étape 20).** Journalisé dans deux cas distincts : `sessionId` renseigné, quand la projection Direct Boot est illisible mais que Room permet de retrouver la session concernée (`DirectBootMerger.merge()`, `SessionReconciler`) ; `sessionId` absent, quand aucun stockage lisible ne permet de savoir de quelle session il s'agissait — Direct Boot et Room illisibles à la fois, ou Room seul illisible une fois déverrouillé. Le second cas n'a pas d'équivalent `SessionIncident` : SPEC_CORE_KMP §13 exige une révision pour ouvrir un incident, qu'aucune session lisible ne peut fournir.

**Le journal technique écrit avant le premier déverrouillage est versé dans Room au déverrouillage (étape 20).** `UnlockAwareTechnicalEventLog` route vers la mémoire tant que l'appareil est verrouillé ; au premier passage de ce processus par `DirectBootMerger.merge()` réellement exécuté (donc déverrouillé), la mémoire est vidée et ses entrées sont insérées dans Room en conservant leur horodatage et leur contexte d'appareil d'origine — jamais ceux du moment du versement, pour la même raison que §17 exige déjà ce contexte par ligne : un journal de 200 événements peut enjamber une mise à jour. Vidage atomique, jamais rejoué deux fois. **Limite résiduelle assumée :** un processus qui journalise avant déverrouillage et meurt avant d'atteindre ce point perd ses entrées ; l'incident métier correspondant, lui, atteint Room par le rejeu de l'outbox (§9.3) et n'est jamais perdu.

**Contexte porté par événement (étape 16).** Le modèle de l'appareil, la version Android et la version de l'application sont des colonnes de `technical_event`, pas un en-tête d'export mis en facteur. Ces valeurs sont certes constantes pour un processus donné, mais un journal de 200 événements peut enjamber une mise à jour de l'application ou du système : un en-tête unique attribuerait alors la nouvelle version aux événements antérieurs. Elles sont ajoutées par l'implémentation du journal, jamais par l'appelant — la signature de `log()` ne les expose pas, sans quoi seize sites d'appel devraient les tenir à jour. Les lignes écrites avant cette montée valent `''` : une absence, jamais une erreur de lecture.

Ajouter un écran de diagnostic exportable sous forme de texte après action explicite de l'utilisateur. Masquer le token, son hash complet et tout identifiant matériel. L'export ne doit contenir que les 200 événements locaux et les résultats du contrôle de santé.

**Forme de l'export (étape 16).** Le contexte d'appareil ouvre le texte ; une ligne d'événement ne le répète que s'il **diffère** de cet en-tête, ce qui n'arrive qu'après une mise à jour — sinon 200 lignes répéteraient la même constante. Le `boxId` est tronqué à ses huit premiers caractères, assez pour distinguer deux boîtiers dans un échange d'assistance. Le hash du token ne traverse **jamais** le modèle de données de l'export : il n'y est pas transporté, plutôt que transporté puis masqué à l'affichage — on ne divulgue pas ce qu'on ne reçoit pas.

## 18. Gestion des erreurs

Principes:

- une erreur de configuration empêche l'activation;
- `FAILED` est réservé à une activation qui n'a jamais abouti;
- une erreur après activation conserve l'état `ARMED`, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` ou `RELEASING`, passe la santé à `DEGRADED` seulement si sa gravité est `DEGRADED` ou `CRITICAL` et crée un `SessionIncident`; une gravité `CRITICAL` est en plus présentée explicitement dans le diagnostic d'incident;
- une erreur après activation conserve le blocage hors de `RELEASING`; pendant `RELEASING`, elle conserve les effets incomplets sans restaurer un blocage déjà retiré;
- une erreur audio garde l'activité visible, la vibration active et affiche une alerte forte;
- une erreur NFC n'arrête jamais la sonnerie;
- une erreur Room pendant la sonnerie s'appuie sur le snapshot Direct Boot;
- une transition dupliquée est reconnue par le registre idempotent;
- une erreur pendant le nettoyage conserve `RELEASING`, crée `RELEASE_PARTIAL_FAILURE` et déclenche une reprise idempotente;
- une erreur inconnue reçoit un identifiant local consultable dans le diagnostic.

La réconciliation s'appuie sur `SessionRuntimeStatus` pour comparer l'état métier aux sous-systèmes Android. Elle tente les réparations idempotentes autorisées, puis consigne un incident si l'écart persiste. Elle ne transforme pas une session active en `FAILED` pour simplifier la gestion d'une erreur technique.

**`SessionRuntimeReconciler` (étape 20)** exécute concrètement cet alinéa pour `alarmScheduled` et `nfcReady` (§7.1), en fin de passe, sur le snapshot le plus à jour : réparer d'abord (reprogrammer l'alarme via `AlarmScheduler.schedule`, aucune réparation n'existant côté NFC), re-sonder, puis consigner un incident `CRITICAL` une seule fois par code et par session si l'écart tient bon — même garde que `SessionReadinessMonitor`. Une exception : quand `alarmScheduled` est faux **parce que** la permission d'alarme exacte l'est aussi (`AlarmScheduler.canScheduleExact() == false`), aucune réparation n'est tentée et aucun incident n'est produit ici — `SessionReadinessMonitor` (§13.1, contrôle `EXACT_ALARM`) couvre déjà ce cas, et retenter `schedule()` serait un second essai voué au même échec pour la même cause.

**Aucune exception SQLite brute ne sort de `:core:database` (étape 20, défaut mesuré sur appareil).** `RoomSessionStore.activeSession()` **et** `RoomDirectBootMerge.merge()` traduisent `SQLiteException` en `SessionStoreUnreadableException`; `UnlockAwarePersistenceGateway` en fait un `LoadResult.Unreadable`, `DirectBootMerger` un `DirectBootMergeOutcome.RoomUnreadable`. Les lectures de diagnostic (`RoomSessionIncidentsReader.incidents()`, `RoomTechnicalEventLog.recent()`) rendent une liste vide, et `RoomBlockedPackagesSource` un `BlockedPackagesRead.Unreadable`. La garde `ROOM_BEFORE_UNLOCK` (`IllegalStateException`) continue seule de remonter: c'est un défaut de programmation, pas une corruption.

Le chemin de la fusion n'est pas accessoire: elle touche Room **avant** le `gateway.load()` de la passe, si bien qu'une base illisible y faisait planter le processus à chaque démarrage — une boucle de plantage, et l'écran de diagnostic promis ci-dessous jamais atteint. Mesuré le 2026-09-15 en rendant `niumi.db` illisible (`chmod 000`), corrigé le même jour.

**Trois cas de corruption, traités distinctement (étape 20).** Une projection Direct Boot lisible mais Room valide produit un événement technique `SNAPSHOT_CORRUPTED` et un incident du même nom, `CRITICAL`, une fois par session ; la projection est réécrite depuis Room, jamais laissée corrompue. Room elle-même illisible, appareil déverrouillé, ne peut produire aucun `SessionIncident` — sans session lisible, ni `sessionId` ni révision n'existent pour porter l'événement — seul l'événement technique `SNAPSHOT_CORRUPTED` avec `sessionId = null` est possible ; l'écran de diagnostic doit alors s'afficher sans retirer le blocage, la dernière projection de blocage connue restant en mémoire dans le service d'accessibilité. Les deux stockages illisibles à la fois : aucune écriture, aucune suppression, le même événement technique sans session, la projection de blocage inchangée.

Ne jamais remplacer silencieusement une alarme exacte par une alarme inexacte.

## 19. Tests automatisés

### 19.1 Tests unitaires obligatoires

`:shared:core` dans `commonTest`:

- toutes les transitions autorisées et interdites;
- activation idempotente;
- fin et annulation idempotentes;
- passage de `ARMED` à `RELEASING` avec cible `CANCELLED` après scan valide;
- passage de `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` à `RELEASING` avec cible `COMPLETED`;
- réconciliation du scan depuis `ARMED` après l'heure vers `TRIGGERED_AWAITING_NFC`;
- refus de `VALID_NFC_SCANNED` depuis `ARMED` à ou après `triggerAtEpochMillis` avec `TRIGGER_ALREADY_ELAPSED`;
- passage à l'état final uniquement après `RELEASE_SUCCEEDED`;
- maintien de `RELEASING` après `RELEASE_FAILED`;
- impossibilité de passer une session active à `FAILED`;
- dégradation de santé et création d'incident sans changement d'état métier;
- scan valide, invalide, mal formé, surdimensionné et comportant une query non canonique;
- token absent, dupliqué, paddé, trop court ou mal encodé;
- comparaison du boîtier associé;
- calcul d'heure normal;
- passage à l'heure d'été;
- passage à l'heure d'hiver;
- changement de fuseau sans modification de `triggerAtEpochMillis`;
- retard inférieur, égal et supérieur à 15 minutes;
- retard supérieur à 15 minutes vers `TRIGGERED_AWAITING_NFC`, avec santé `DEGRADED` et incident `MISSED_TRIGGER_WINDOW`;
- `PRESENT_SCAN_REQUEST` produit par `ALARM_SOUND_STOPPED` et par `TRIGGER_ELAPSED`, `CLEAR_SCAN_REQUEST` produit par `VALID_NFC_SCANNED`, tous deux rejouables sans effet en double;
- sélections de 0, 1, 50 et 51 applications;
- révisions métier croissantes, refus d'un événement destiné à une autre session et validation de `failureCode`.

`core:system` avec adaptateurs simulés:

- construction stable des `PendingIntent`;
- diagnostic de chaque permission;
- diagnostic distinct de `USE_EXACT_ALARM` sans parcours `SCHEDULE_EXACT_ALARM`;
- classification `BLOCKING_FOR_ALARM`, `BLOCKING_FOR_NIUMI_EXPERIENCE` et `WARNING`;
- comportement quand le canal de notification est désactivé;
- diagnostic du mode Ne pas déranger lorsque son état est observable;
- calcul de `SessionRuntimeStatus` et réconciliation des écarts;
- filtrage des applications système;
- mapping aller-retour entre Room et les DTO KMP, puis mapping de projection pour Direct Boot;
- reconstruction d'un `SessionSnapshot` valide depuis Direct Boot avant déverrouillage;
- politique de retard appliquée par le receiver Direct Boot, à 15 minutes et au-delà;
- publication et retrait idempotents de la notification d'attente de scan, y compris depuis le coordinateur Direct Boot avant déverrouillage;
- mapping des erreurs Android vers les erreurs métier;
- registre et outbox atomiques, exécution idempotente des effets KMP et reprise partielle de `RELEASING`;
- reconstruction du snapshot Direct Boot depuis Room après une interruption entre les deux écritures de l'étape 4 de l'activation.

`feature`:

- impossibilité de confirmer si un contrôle bloquant échoue;
- écran d'association et sélecteur d'applications inaccessibles pendant une session active;
- scan vérifié contre `boxId` et `boxTokenSha256Hex` de la session active, refusé si le dépôt de boîtiers a changé depuis l'activation;
- absence d'action d'arrêt sur l'écran de sonnerie;
- retour des réglages et nouveau diagnostic;
- messages d'erreur NFC;
- reprise d'un état `RINGING` après recréation;
- affichage de l'avertissement sur l'absence de mécanisme de secours logiciel avant la première activation.

### 19.2 Tests instrumentés

- migrations Room;
- écriture et lecture du snapshot Direct Boot;
- réception des intents explicites;
- affichage de la notification de sonnerie et de son canal;
- affichage de la notification d'attente de scan et de son canal, sans son, vibration ni full-screen intent;
- démarrage du service depuis un receiver de test;
- Reader Mode avec abstraction ou tag de test.

Les tests ne doivent pas attendre une vraie heure de réveil. Injecter `Clock`, `AlarmScheduler`, `AlarmAudioEngine`, `NfcVerifier` et `ForegroundAppSource`.

Le blocage d'applications ne figure pas dans cette liste, et ce n'est pas un oubli. Les deux vérifications attendues — overlay d'accessibilité sur une application factice, et retour à l'accueil après détection d'un package bloqué — ne sont pas réalisables par instrumentation: toute instrumentation de l'application fait passer `accessibility_enabled` à 0 et débranche le service d'accessibilité, qui ne se relie pas de lui-même. Un test instrumenté ne peut donc jamais observer le service en fonctionnement. Un test hébergé dans un module `library` n'y parvient pas davantage: son APK est une application distincte, dans un autre processus, sans accès à l'état de blocage de Niumi.

Ces deux vérifications sont couvertes par `tools/validate_blocking.sh`, qui déroule le protocole sur un appareil réel et contrôle chaque essai par `dumpsys`: retour à l'accueil depuis le lanceur sur tâche neuve puis sur tâche existante, ouverture par intent explicite, présence puis retrait de la fenêtre d'overlay, absence d'effet sur une application non bloquée, et persistance du blocage après un délai en arrière-plan. Le script échoue explicitement lorsque ses préconditions ne sont pas réunies, plutôt que de s'ignorer. Le texte exact de l'overlay imposé par 12.2 n'est pas vérifiable par ce moyen et reste un contrôle visuel.

## 20. Matrice de tests physiques

Le POC puis chaque version candidate doivent être testés sur de vrais appareils. Un émulateur ne suffit pas pour le NFC, l'audio, Doze et les couches OEM.

Fabricants prioritaires:

- Google Pixel;
- Samsung Galaxy;
- Xiaomi, Redmi ou Poco;
- Oppo ou Realme;
- OnePlus;
- Honor;
- Motorola;
- Nothing.

Versions Android minimales à couvrir:

- Android 10 ou 11 pour le plancher de compatibilité;
- Android 12 ou 13 pour les alarmes exactes et les notifications;
- Android 14 pour l'accès spécial au plein écran;
- Android 15 pour l'arrêt forcé et les `PendingIntent`;
- Android 16 pour la cible Play;
- Android 17 pour la compatibilité d'exécution et l'audio d'alarme en arrière-plan.

Scénarios à exécuter:

| Scénario | Résultat attendu |
| --- | --- |
| écran éteint depuis 30 minutes | alarme à l'heure, écran présenté ou notification urgente |
| Doze forcé | alarme à l'heure |
| mode économie d'énergie | alarme à l'heure dans le périmètre pris en charge |
| mode Ne pas déranger autorisant les alarmes | sonnerie audible |
| mode Ne pas déranger interdisant les alarmes | diagnostic et comportement consignés, aucune fausse garantie |
| mode silencieux ou vibration avec volume d'alarme actif | sonnerie audible selon le flux `USAGE_ALARM` |
| volumes média et notification à zéro, volume d'alarme actif | sonnerie audible |
| volume alarme à zéro avant activation | activation refusée |
| volume mis à zéro après activation | incident documenté, pas de fausse garantie |
| Android 17, application en arrière-plan et écran verrouillé depuis plus de 30 minutes | FGS démarré et son `USAGE_ALARM` audible |
| casque Bluetooth connecté | sortie audio conforme à la stratégie documentée et son détectable par l'utilisateur |
| casque Bluetooth déconnecté pendant la nuit | sonnerie audible sur la nouvelle route |
| écouteurs USB-C ou casque filaire connecté | sortie audio conforme à la stratégie documentée |
| route audio modifiée pendant `RINGING` | lecture maintenue ou reprise, incident consigné en cas d'échec |
| redémarrage puis aucun déverrouillage | alarme reprogrammée depuis Direct Boot |
| redémarrage, permission OEM de démarrage automatique **refusée** | alarme reprogrammée depuis Direct Boot ; si la surcouche bloque le démarrage du processus, le consigner et évaluer un contrôle de diagnostic ciblé sur ces appareils (§4.2) |
| redémarrage 2 minutes avant le réveil | alarme reprogrammée et déclenchée à l'heure |
| redémarrage après l'heure, retard inférieur ou égal à 15 minutes | sonnerie immédiate |
| redémarrage après l'heure, retard supérieur à 15 minutes | session `TRIGGERED_AWAITING_NFC`, santé `DEGRADED`, incident `MISSED_TRIGGER_WINDOW`, blocage maintenu, notification d'attente de scan visible avant tout déverrouillage, sans son ni vibration |
| scan valide depuis `TRIGGERED_AWAITING_NFC` | notification d'attente de scan retirée, passage par `RELEASING` puis `COMPLETED` |
| changement manuel d'heure | même `triggerAtEpochMillis` réenregistré, politique de retard appliquée |
| changement de fuseau | instant inchangé et affichage local recalculé |
| notifications refusées | activation refusée |
| plein écran refusé | activation refusée sur Android 14 et plus |
| accessibilité désactivée | activation refusée ou incident détecté |
| service d'accessibilité tué puis recréé par le système ou la surcouche | état actif rechargé et blocage restauré |
| ouverture d'une app bloquée | retour immédiat à l'accueil et overlay |
| app bloquée ouverte depuis les récents | retour immédiat à l'accueil et overlay |
| app bloquée ouverte par notification ou lien profond | retour immédiat à l'accueil et overlay |
| ouverture d'une app autorisée | aucun effet |
| Niumi retiré des applications récentes après armement | alarme et blocage conservés |
| processus Niumi tué par le système après armement | alarme conservée et état réconcilié au redémarrage du processus |
| processus Niumi tué pendant `RINGING` (étape 20) | le watchdog réveille le processus au plus tard toutes les ~9 minutes (Doze, §4.2), le son reprend depuis le snapshot |
| fermeture de `AlarmActivity` | sonnerie maintenue |
| verrouillage pendant la sonnerie | sonnerie maintenue |
| scan du bon tag | passage par `RELEASING`, arrêt et déblocage en moins d'une seconde, puis état final |
| interruption pendant `RELEASING` | reprise des effets manquants sans déblocage incohérent |
| scan d'un autre tag | sonnerie maintenue |
| NFC désactivé pendant la sonnerie | instruction de réactivation, sonnerie maintenue |
| NFC réactivé pendant la sonnerie | Reader Mode restauré et scan valide accepté |
| arrêt du FGS depuis le système | limite connue consignée |
| arrêt forcé de Niumi | alarme annulée par le système, limite connue consignée |
| `niumi_session.json` corrompu à la main, Room valide (étape 20, debug) | incident `SNAPSHOT_CORRUPTED` unique, projection réécrite, session et blocage conservés |
| base Room corrompue, appareil déverrouillé (étape 20, debug) | écran de diagnostic affiché, blocage conservé, aucune session perdue |

Pour chaque essai, consigner le fabricant, le modèle, la version Android, la version du firmware, les permissions, le résultat, le retard mesuré et les logs locaux.

**Consigner en plus l'état de la permission OEM de démarrage automatique** quand le fabricant en propose une (§4.2). Mesuré à l'étape 19: HyperOS exempte `LOCKED_BOOT_COMPLETED` et `BOOT_COMPLETED` de cette restriction, donc la reprogrammation après redémarrage tient sans elle. Rien ne permet de généraliser à Oppo, Realme, Vivo ou Honor, réputés plus agressifs. C'est la question à trancher fabricant par fabricant: **si une surcouche empêche le démarrage du processus au boot sans cette permission, le réveil après redémarrage y est perdu**, et un contrôle de diagnostic ciblé devient nécessaire sur ces appareils. Un essai mené avec la permission accordée ne prouve rien pour l'utilisateur ordinaire, qui ne l'a pas: la mesurer dans l'état par défaut.

## 21. Critères d'acceptation du MVP

Le MVP est accepté si tous les critères suivants sont vrais:

- une session ne peut être confirmée que lorsque le diagnostic est vert;
- `setAlarmClock()` est la seule API utilisée pour l'heure de réveil;
- l'alarme sonne hors ligne avec l'écran éteint sur la matrice P0;
- sur Android 17, l'alarme utilise `USAGE_ALARM` et reste audible dans le scénario arrière-plan P0;
- la sonnerie continue après fermeture de l'activité;
- aucun bouton logiciel ne termine la session;
- seul un tag accepté par le parseur et le vérificateur KMP, avec sa `NfcVerificationProof` opaque, produit `VALID_NFC_SCANNED`;
- un scan valide depuis `ARMED` produit `RELEASING` avec cible `CANCELLED`;
- un scan valide depuis `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` produit `RELEASING` avec cible `COMPLETED`;
- `COMPLETED` ou `CANCELLED` n'est écrit qu'après `RELEASE_SUCCEEDED`;
- une session active ne passe jamais à `FAILED` à cause d'un incident technique;
- un incident postérieur à l'activation met à jour `SessionHealth` selon sa gravité; pendant `RELEASING`, il ne restaure pas un blocage déjà retiré;
- un tag invalide ne modifie pas l'état, le blocage ou le son;
- la fin valide arrête le son et débloque les applications en moins d'une seconde;
- le blocage renvoie chaque application sélectionnée à l'accueil sans bloquer les applications autorisées;
- une sélection de plus de 50 applications est refusée;
- un redémarrage restaure l'alarme avant le premier déverrouillage;
- `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC` affichent toujours une notification demandant le scan, sans son ni vibration ni full-screen intent, y compris avant le premier déverrouillage; elle est retirée par un scan valide;
- un changement d'heure ou de fuseau réenregistre le même instant et recalcule seulement l'affichage local;
- l'application ne lit aucun contenu de fenêtre via l'accessibilité;
- le parcours critique ne réalise aucun appel réseau;
- tous les tests unitaires et instrumentés passent;
- Android Lint, ktlint et detekt passent sans erreur;
- les limites de l'arrêt forcé, du FGS et du NFC verrouillé sont documentées dans l'application et dans le rapport QA;
- l'absence de mécanisme logiciel de secours en cas de boîtier ou de NFC indisponible est expliquée avant la première activation;
- les scénarios DND, Bluetooth, USB-C et changement de route audio sont consignés sur la matrice P0;
- le dossier de déclaration Google Play pour l'AccessibilityService est prêt (Lot 0) et a été testé sur une piste interne ou fermée dès que le processus Play le permet, sur l'application complète plutôt que le POC (Lot 5, décision du 2026-09-07, voir `docs/android/implementation-reports/LOT-0.md`).

## 22. Ordre d'implémentation demandé à Codex

### Lot 0: POC système

Créer une application minimale qui valide sur appareils réels:

- `setAlarmClock()`;
- receiver puis service `mediaPlayback`;
- son local avec `USAGE_ALARM`;
- notification plein écran;
- activité au-dessus du verrouillage;
- Reader Mode NFC;
- arrêt du service après scan associé, validé par le parseur et le vérificateur de
  `:shared:core` (livrés avant le POC, voir Lot 0.5) — SPEC_CORE_KMP §9.3 interdit à un lecteur
  NFC natif de décider seul qu'un payload est valide;
- détection d'une application factice avec `AccessibilityService`;
- retour à l'accueil et overlay.

Le Lot 0 comprend aussi la préparation de la preuve de publiabilité liée à l'AccessibilityService:

- préparer la déclaration Play Console;
- intégrer la divulgation et le consentement utilisateur dans le POC;
- traiter un refus ou une demande de justification comme un risque produit bloquant.

**Décision validée le 2026-09-07 (étape 6, voir `docs/android/implementation-reports/LOT-0.md`)** :
l'enregistrement de la vidéo de démonstration et la soumission sur piste interne ou fermée sont
reportés au Lot 5 (étape 21), après suppression du POC et livraison du parcours utilisateur réel.
La [politique Play pour AccessibilityService](https://support.google.com/googleplay/android-developer/answer/10964491)
exige une vidéo montrant la divulgation et le consentement en usage normal, que le POC de debug
ne peut pas représenter fidèlement; de plus, seule la première publication d'une piste passe une
revue de politique standard, ce qui rend une soumission précoce sur le POC largement formelle.
Cette déviation accepte le risque, explicité ci-dessous, d'investir dans l'interface complète
avant le verdict de Google.

Ne pas commencer l'interface complète avant validation du POC sur Pixel, Samsung et Xiaomi.
**Risque accepté par la décision ci-dessus** : la spec demandait par ailleurs de ne pas investir
dans l'interface complète ou le backend avant validation de la stratégie de publication liée à
l'accessibilité; cette validation (vidéo, soumission, réponse de Google) n'intervient
désormais qu'au Lot 5. Si Google refuse l'usage à ce moment, une révision remontant jusqu'aux
lots précédents peut être nécessaire.

### Lot 0.5: contrat commun KMP

Le protocole NFC (parseur, credential, vérificateur, preuve) de ce lot est livré **avant** le
Lot 0, en amont du reste du moteur commun : SPEC_CORE_KMP §9.3 interdit à un lecteur NFC natif
de décider seul de la validité d'un payload, et le POC du Lot 0 arrête la sonnerie sur un scan
associé — il doit donc consommer un parseur et un vérificateur déjà livrés plutôt que
d'implémenter sa propre logique de validation. Le reste du Lot 0.5 (machine à états, politique
horaire) suit le Lot 0, après la porte de validation manuelle.

- ajouter `:shared:core` avec le plugin de bibliothèque Android KMP;
- implémenter la machine à états, la politique horaire et le protocole NFC de `SPEC_CORE_KMP.md`;
- exposer `NiumiCoreFacade` avec des DTO simples;
- créer les fixtures communes et les tests `commonTest`;
- valider la consommation du module depuis un module Android minimal;
- valider la construction du framework iOS dans la CI du monorepo.

### Lot 1: domaine et persistance

- modules Gradle et dépendance vers `:shared:core`;
- mappings entre KMP, Room et le snapshot Direct Boot;
- Room;
- snapshot Direct Boot;
- coordinateurs d'effets Android;
- tests unitaires Android complémentaires aux tests communs.

### Lot 2: configuration

- onboarding permissions;
- association NFC;
- sélecteur d'applications;
- choix de l'heure;
- diagnostic;
- activation en deux phases.

### Lot 3: session active

- blocage d'applications;
- écran de session;
- modification ou annulation après scan NFC;
- journal local.

### Lot 4: réveil

- receiver;
- service;
- audio et vibration;
- notification plein écran;
- activité de réveil;
- scan et fin atomique.
- reprise de `RELEASING`.

### Lot 5: résilience

- Direct Boot;
- boot et mise à jour;
- changements d'heure et de fuseau;
- reprise après mort du processus;
- diagnostic d'incident;
- tests OEM.

À la fin de chaque lot, Codex doit exécuter les tests concernés et produire un court rapport contenant les fichiers modifiés, les commandes exécutées, les résultats et les limites restantes. Aucun `TODO`, faux service, faux scan ou comportement silencieux ne doit rester dans un lot déclaré terminé.

## 23. Portes de validation avant publication

La publication Google Play reste bloquée tant que les points suivants ne sont pas validés:

- Niumi est présenté comme une application dont le réveil est une fonction centrale;
- la déclaration Play Console pour `USE_EXACT_ALARM` est acceptée;
- l'usage du plein écran est déclaré comme alarme;
- le type de service au premier plan `mediaPlayback` est déclaré;
- l'usage de l'Accessibility Service est déclaré;
- une version de l'application a été soumise sur une piste interne ou fermée dès que le processus Play le permet, et les éventuelles demandes de Google ont été traitées — soumission effectuée sur le parcours utilisateur réel (Lot 5, étape 21), pas sur le POC de debug: voir la décision du 2026-09-07 en `docs/android/implementation-reports/LOT-0.md`;
- l'acceptation Play de l'AccessibilityService est suivie comme une porte de validation produit, jamais comme une formalité garantie;
- l'écran de divulgation et de consentement est visible dans la vidéo de revue, tournée sur l'application complète et non sur le POC;
- `isAccessibilityTool` reste à `false`;
- la fiche Play explique le blocage d'applications sans prétendre qu'il est impossible à contourner;
- la politique de confidentialité décrit exactement les données consultées et conservées;
- la matrice physique P0 est entièrement verte dans le périmètre de fiabilité défini.

## 24. Références officielles

- [Planifier des alarmes Android](https://developer.android.com/develop/background-work/services/alarms)
- [Alarmes exactes sur Android 14](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Restrictions de démarrage des services au premier plan](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Types de services au premier plan](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Notifications urgentes et intents plein écran](https://developer.android.com/develop/ui/views/notifications/time-sensitive)
- [Changements Android 14 pour les intents plein écran](https://developer.android.com/about/versions/14/behavior-changes-14)
- [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)
- [Notions de base NFC](https://developer.android.com/develop/connectivity/nfc/nfc)
- [Créer un service d'accessibilité](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Politique Google Play pour AccessibilityService](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Permissions sensibles et USE_EXACT_ALARM](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Exigences Google Play de niveau d'API cible](https://developer.android.com/google/play/requirements/target-sdk)
- [Changements Android 15 applicables à toutes les applications](https://developer.android.com/about/versions/15/behavior-changes-all)
- [Android 17 et audio en arrière-plan](https://developer.android.com/about/versions/17/changes/bg-audio)
- [Changements de comportement Android 17](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Plugin Android Gradle pour une bibliothèque KMP](https://developer.android.com/kotlin/multiplatform/plugin)
- [Intégration directe du framework KMP dans Xcode](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)
- [kotlinx.datetime `TimeZone`](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-time-zone/)
