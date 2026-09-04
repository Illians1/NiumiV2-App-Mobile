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
- panne du système, du haut-parleur ou du matériel NFC;
- comportement OEM incompatible non détecté.

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
| `:feature:setup` | Onboarding, permissions, association NFC et sélection des applications |
| `:feature:session` | Configuration, activation, session active et blocage |
| `:feature:ringing` | Service de sonnerie, activité plein écran et fin par NFC |

Règles de dépendance:

- les modules `feature`, `core:database` et `core:system` peuvent dépendre de `:shared:core`;
- `:shared:core` ne dépend d'aucune API Android ou Apple;
- `core:database` et `core:system` convertissent leurs modèles vers les DTO KMP;
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
tokenSha256
blockedPackageNames
eventReceipts
pendingEffects
```

Ce snapshot est une projection partielle de Room, mais son enveloppe de session active contient tous les champs requis pour reconstruire un `SessionSnapshot` et appeler KMP avant déverrouillage. Chaque effet de `pendingEffects` conserve aussi son payload sérialisé afin de reprendre `RECORD_INCIDENT`. Il permet de reprogrammer et de déclencher l'alarme avant le premier déverrouillage après un redémarrage. Il ne contient aucune donnée de compte. Son écriture doit être atomique. Utiliser un fichier temporaire dans le même répertoire, puis un renommage, ou des préférences synchrones dédiées avec contrôle de version. Une réécriture à `domainRevision` égale est idempotente; une révision inférieure est refusée.

Les composants `directBootAware` ne doivent pas créer Room ou un dépôt qui ouvre Room avant `UserManager.isUserUnlocked == true`. Utiliser des dépendances différées et le snapshot comme unique source avant le déverrouillage.

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

Créer un `SystemEventsReceiver` pour:

- `LOCKED_BOOT_COMPLETED`;
- `BOOT_COMPLETED`;
- `MY_PACKAGE_REPLACED`;
- `TIME_CHANGED`;
- `TIMEZONE_CHANGED`;
- `USER_UNLOCKED`.

Le receiver est `directBootAware`. Il lit le snapshot et rappelle le programmateur avec le même `triggerAtEpochMillis`. Il ne recalcule pas l'instant depuis l'heure locale. Si la session est `ARMED` et l'instant est dépassé, il applique la politique de retard dans le coordinateur Direct Boot sous mutex: jusqu'à 15 minutes, il reprogramme une alarme immédiate dont `AlarmReceiver` produira `ALARM_FIRED`; au-delà, il applique `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW` dans le snapshot, le registre et l'outbox, exécute `PRESENT_SCAN_REQUEST` et publie la notification décrite en 10.5 depuis le contexte protégé par appareil, sans démarrer le service de sonnerie. À `USER_UNLOCKED`, le réconciliateur fusionne de façon idempotente le registre et l'outbox Direct Boot dans Room.

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
- gérer le retour prédictif en renvoyant vers l'accueil sans modifier la session;
- afficher l'heure, l'état du NFC et l'instruction de scan;
- activer le Reader Mode dans `onResume()`;
- le désactiver dans `onPause()`;
- rouvrir l'écran de réveil si l'état commun est `RINGING`; afficher la progression de nettoyage si l'état est `RELEASING`; afficher le mode scan sans audio si l'état est `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC`;
- ne contenir aucun bouton d'arrêt.

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
- aucune action d'arrêt;
- au tap, ouvrir `AlarmActivity` en mode scan.

La notification est publiée par l'exécution de `PRESENT_SCAN_REQUEST` et retirée par `CLEAR_SCAN_REQUEST`, tous deux idempotents. Lorsque `TRIGGER_ELAPSED` est produit par le coordinateur Direct Boot avant le premier déverrouillage, la notification est publiée depuis le `DeviceProtectedStorageContext`, au même titre que le snapshot Direct Boot.

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

Les effets requis pour `RELEASE_SUCCEEDED` sont l'annulation de l'alarme (étape 7) et le retrait de la liste de blocage (étape 10); l'arrêt du son et de la vibration, la suppression de la notification et l'arrêt du service sont best-effort et consignés en cas d'échec sans bloquer la phase. Si le service d'accessibilité a déjà été désactivé par l'utilisateur avant le scan, le retrait du blocage est considéré satisfait dès que `NiumiBlockingAccessibilityService` n'est plus actif, avec un incident `BLOCKING_PERMISSION_REVOKED` consigné, plutôt que de bloquer indéfiniment `RELEASING`.

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

### 12.2 AccessibilityService

Déclarer `NiumiBlockingAccessibilityService` avec la permission système `BIND_ACCESSIBILITY_SERVICE` et `isAccessibilityTool=false`.

Configuration minimale:

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="50"
    android:canRetrieveWindowContent="false"
    android:isAccessibilityTool="false" />
```

Le service doit uniquement lire `event.packageName`. Il ne doit pas parcourir l'arbre d'accessibilité, lire le texte affiché, inspecter les saisies ou transmettre des événements à un serveur.

Algorithme:

```text
à chaque changement de fenêtre
  lire le package au premier plan
  si aucune session ARMED, RINGING, AWAITING_NFC, TRIGGERED_AWAITING_NFC ou RELEASING: ne rien faire
  si la session est RELEASING: lire la liste de blocage effective et les effets de libération en attente
  si aucune liste de blocage effective: retirer l'overlay éventuel
  si le package n'est pas bloqué: retirer l'overlay éventuel
  si le package est bloqué:
    exécuter GLOBAL_ACTION_HOME
    afficher brièvement un TYPE_ACCESSIBILITY_OVERLAY explicatif
    journaliser BLOCK_APPLIED avec le package uniquement
```

Utiliser `TYPE_ACCESSIBILITY_OVERLAY`. Ne pas démarrer une `Activity` depuis l'arrière-plan pour bloquer une application. L'overlay disparaît dès que le package bloqué n'est plus au premier plan ou après une durée maximale de 3 secondes. Il ne doit pas rendre tout le téléphone inutilisable.

Texte de l'overlay:

> {Nom de l'application} reste bloquée jusqu'au scan du boîtier.

Le service doit recharger l'état actif depuis Room ou le snapshot après recréation. Si le service est désactivé pendant une session, Niumi doit le détecter à sa prochaine exécution et afficher un incident. Il ne doit pas tenter de réactiver le service ou d'empêcher l'utilisateur d'accéder aux réglages.

### 12.3 Information et consentement

Avant d'ouvrir les réglages d'accessibilité, afficher une page dédiée qui explique:

- que Niumi observe le nom de l'application affichée;
- que cette information sert uniquement à renvoyer les applications choisies vers l'accueil;
- que Niumi ne lit pas le contenu des écrans ou les saisies;
- que le service peut être désactivé dans les réglages Android;
- qu'une session ne peut pas être activée si le service est inactif.

Le bouton peut être intitulé "Ouvrir les réglages d'accessibilité". Ne jamais simuler un consentement ou cliquer à la place de l'utilisateur.

L'acceptation de cet usage par Google Play n'est pas considérée comme acquise. Le POC doit inclure la déclaration Play Console, le texte de divulgation, le consentement et une vidéo montrant le parcours réel. Cette validation de politique fait partie du Lot 0 et peut bloquer la poursuite du produit Android.

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
| mode Ne pas déranger préoccupant | `WARNING` | état d'interruption disponible | réglages Ne pas déranger et explication |
| service d'accessibilité actif | `BLOCKING_FOR_NIUMI_EXPERIENCE` | services activés | réglages d'accessibilité |
| date future valide | `BLOCKING_FOR_ALARM` | calcul métier | corriger l'heure |
| batterie optimisée | `WARNING` | `PowerManager` | aide OEM, sans blocage par défaut |

Niumi déclare `USE_EXACT_ALARM`, car le réveil est une fonction centrale du produit. La spec ne doit pas ajouter `SCHEDULE_EXACT_ALARM` ni présenter l'accès aux alarmes exactes comme une permission utilisateur ordinaire. `canScheduleExactAlarms()` reste vérifié par sécurité. S'il renvoie `false`, l'application signale un état anormal, une incompatibilité ou un problème d'éligibilité. Elle ne redirige pas automatiquement vers les réglages "Alarmes et rappels" comme elle le ferait avec `SCHEDULE_EXACT_ALARM`.

La détection du mode Ne pas déranger varie selon la version Android et les surcouches. Le diagnostic doit signaler les états observables qui risquent de rendre l'alarme inaudible, sans promettre une analyse parfaite de toutes les configurations OEM. Les tests physiques restent la source de validation.

L'écran n'affiche qu'une action principale à la fois, en commençant par le premier blocage. Il doit recalculer l'état après chaque retour des réglages.

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
12. diagnostic d'incident.

Règles UI:

- utiliser le tutoiement partout;
- ne jamais afficher un faux état de fiabilité;
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
```

Chaque événement contient seulement l'heure, le type, l'identifiant de session, le modèle de l'appareil, la version Android, la version de l'application et un code d'erreur contrôlé. Le nom de package est accepté uniquement pour `BLOCK_APPLIED`. Aucun événement n'est envoyé à distance dans le MVP.

Ajouter un écran de diagnostic exportable sous forme de texte après action explicite de l'utilisateur. Masquer le token, son hash complet et tout identifiant matériel. L'export ne doit contenir que les 200 événements locaux et les résultats du contrôle de santé.

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
- Reader Mode avec abstraction ou tag de test;
- overlay d'accessibilité sur application factice;
- retour à l'accueil après détection d'un package bloqué.

Les tests ne doivent pas attendre une vraie heure de réveil. Injecter `Clock`, `AlarmScheduler`, `AlarmAudioEngine`, `NfcVerifier` et `ForegroundAppSource`.

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
| processus Niumi tué pendant `RINGING` | service et sonnerie repris depuis le snapshot, ou incident critique documenté selon le comportement système |
| fermeture de `AlarmActivity` | sonnerie maintenue |
| verrouillage pendant la sonnerie | sonnerie maintenue |
| scan du bon tag | passage par `RELEASING`, arrêt et déblocage en moins d'une seconde, puis état final |
| interruption pendant `RELEASING` | reprise des effets manquants sans déblocage incohérent |
| scan d'un autre tag | sonnerie maintenue |
| NFC désactivé pendant la sonnerie | instruction de réactivation, sonnerie maintenue |
| NFC réactivé pendant la sonnerie | Reader Mode restauré et scan valide accepté |
| arrêt du FGS depuis le système | limite connue consignée |
| arrêt forcé de Niumi | alarme annulée par le système, limite connue consignée |

Pour chaque essai, consigner le fabricant, le modèle, la version Android, la version du firmware, les permissions, le résultat, le retard mesuré et les logs locaux.

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
- le dossier de déclaration Google Play pour l'AccessibilityService est prêt et a été testé sur une piste interne ou fermée dès que le processus Play le permet.

## 22. Ordre d'implémentation demandé à Codex

### Lot 0: POC système

Créer une application minimale qui valide sur appareils réels:

- `setAlarmClock()`;
- receiver puis service `mediaPlayback`;
- son local avec `USAGE_ALARM`;
- notification plein écran;
- activité au-dessus du verrouillage;
- Reader Mode NFC;
- arrêt du service après scan associé;
- détection d'une application factice avec `AccessibilityService`;
- retour à l'accueil et overlay.

Le Lot 0 comprend aussi la preuve de publiabilité liée à l'AccessibilityService:

- préparer la déclaration Play Console;
- intégrer la divulgation et le consentement utilisateur dans le POC;
- enregistrer la vidéo de démonstration demandée pour la revue;
- soumettre une version sur piste interne ou fermée dès que le processus Play le permet;
- traiter un refus ou une demande de justification comme un risque produit bloquant.

Ne pas commencer l'interface complète avant validation du POC sur Pixel, Samsung et Xiaomi.
Ne pas investir dans l'interface complète ou le backend avant validation du parcours système critique et de la stratégie de publication liée à l'accessibilité.

### Lot 0.5: contrat commun KMP

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
- une version du POC a été soumise sur une piste interne ou fermée dès que le processus Play le permet, et les éventuelles demandes de Google ont été traitées;
- l'acceptation Play de l'AccessibilityService est suivie comme une porte de validation produit, jamais comme une formalité garantie;
- l'écran de divulgation et de consentement est visible dans la vidéo de revue;
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
