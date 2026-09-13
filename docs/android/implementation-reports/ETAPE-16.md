# Étape 16 — journal local, diagnostic d'incident et export

**Date :** 2026-09-13. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 16.

**Produit :** les champs de contexte de §17 portés par chaque événement (base en v2), l'émission de
`RELEASE_PARTIAL_FAILURE`, l'écran 12 (diagnostic d'incident) avec son export texte, la remédiation
des incidents sur l'écran 7 — report de l'étape 15 — et la redirection du tap des notifications
d'avertissement vers l'écran 12.

---

## Arbitrages validés avec l'utilisateur avant implémentation

1. **Contexte d'appareil : colonnes Room, base en v2** (plutôt qu'un en-tête d'export mis en
   facteur). Fidèle à la lettre de §17, et seule option qui reste juste si l'application est mise à
   jour pendant la fenêtre des 200 événements.
2. **`settingsIntentFor` descend dans `:core:system`.** L'écran 7 en a besoin et `:feature:session`
   ne peut pas dépendre de `:feature:setup` (§6). Alternative écartée : la première arête
   `feature → feature` du dépôt.
3. **Le deep link de la notification est dans le périmètre**, §13.1 l'exigeant dès lors que l'écran
   existe. Non listé par le plan : ajout assumé.

## Écarts au plan

- **`settingsIntentFor` n'est pas allé dans « un module commun » au sens du plan** mais dans
  `:core:system`. §13 affirmait que les `Intent` devaient vivre dans `:feature:setup` « jamais dans
  `:core:system`, qui reste sans interface » ; **cette justification était factuellement fausse** —
  `:core:system` manipule déjà `AlarmManager`, `NfcAdapter`, `Settings.Secure` et `AudioManager`. Un
  `Intent` de réglages n'est pas de l'interface, un texte affiché si. §13 est corrigée. `explanationFor`
  (texte) reste dans `:feature:setup`, et son test instrumenté a été scindé en conséquence.
- **La table de remédiation est statique**, pas une seconde évaluation de `DeviceReadinessChecker`.
  L'action attachée à un code ne dépend pas de l'état du système, et le checker est explicitement
  sans état : le rejouer pour obtenir un recours aurait doublé le diagnostic à chaque affichage de
  l'écran 7.
- **L'écran 12 n'affiche pas les messages de §13.** Ceux-ci sont rédigés comme des consignes
  d'activation (« Active-le avant de démarrer la session ») et seraient faux pendant une session
  déjà armée. L'écran 12 nomme le contrôle et son résultat ; l'écran 7 porte le recours. Évite aussi
  de dupliquer un texte imposé dans un second module.
- **Aucun nouveau type d'événement technique.** §17 est une liste fermée de 26 valeurs. Les cinq
  exécuteurs sans journal (`CancelAlarmExecutor`, `StopRingingExecutor`, `ClearActiveSessionExecutor`,
  `RemoveBlockingExecutor`, `RecordIncidentExecutor`) n'ont **aucun type correspondant** : ils restent
  silencieux plutôt que d'élargir l'enum. Le seul émetteur réellement manquant était
  `RELEASE_PARTIAL_FAILURE`, désormais branché sur le coordinateur.
- **Deux regroupements imposés par detekt**, sans assouplir aucune règle : `HomeActions`
  (`LongParameterList` sur `HomeScreen`, 6 > 5) et `DiagnosticSources` (`LongParameterList` sur le
  constructeur du ViewModel de l'écran 12, 7 > 6, même motif que `ReconcilerSources`). `NiumiNavHost`
  dépassait `LongMethod` (61 > 60) : les destinations de préparation sont extraites dans
  `NavGraphBuilder.setupDestinations`, comme `activeSessionDestinations` à l'étape 15.

## Déjà acquis, non réimplémenté

- **« Brancher `RecordIncidentExecutor` sur `IncidentDao` » était déjà satisfait** depuis l'étape 11 :
  `RecordIncidentExecutor → SessionPersistenceGateway → RoomSessionStore.recordIncident →
  IncidentDao.insert`. Le plan le listait comme un travail restant ; il ne l'était pas.
- **La règle « `packageName` uniquement pour `BLOCK_APPLIED` » était déjà appliquée** par
  `TechnicalEventDetails.sanitize`. Seule la couverture de test manquait : elle énumère désormais les
  26 types plutôt qu'un échantillon.
- `NiumiRoute.IncidentDiagnostic` existait depuis l'étape 12b, simplement non enregistrée.

## Points découverts à l'implémentation

- **Le schéma Room v2 ne correspondait pas à la migration.** `ALTER TABLE … ADD COLUMN … NOT NULL`
  exige une valeur par défaut en SQLite, mais le schéma généré déclarait `NOT NULL` sans `DEFAULT` :
  `validateMigration` aurait échoué sur cette seule différence. Corrigé par
  `@ColumnInfo(defaultValue = "''")` sur les trois colonnes, ce qui aligne schéma attendu et schéma
  réel. Détecté en inspectant le `createSql` généré, pas au premier passage des tests JVM.
- **`PackageInfoCompat` aurait imposé `androidx.core`**, non déclaré dans `:core:database`. Évité
  (CLAUDE.md : pas de dépendance sans besoin démontré) : `minSdk 29` rend `longVersionCode`
  directement disponible. La lecture Android vit dans `AndroidDeviceContext.kt`, séparée du module
  Hilt pour rester lisible et ne pas y noyer la branche de dépréciation `getPackageInfo`.
- **`ExportedSchemaTest` ne vérifiait que la v1.** Généralisé : chaque version jusqu'à la courante
  doit être committée — `MigrationTestHelper` a besoin de la v1 pour créer une base avant migration.
- **`FakeSessionPersistenceGateway` fait déjà échouer toute écriture depuis un écran** (`error(
  "COMMIT_FROM_SCREEN")`). C'est une garantie plus forte que l'assertion que j'avais d'abord écrite
  pour « aucune action n'annule ni ne modifie la session » : le test s'appuie dessus.
- **`RecordingTechnicalEventLog` ne sait pas relire.** L'écran 12 est le premier consommateur de
  `recent()` ; `ReplayingTechnicalEventLog` est ajouté à côté plutôt que de modifier le double
  existant, dont les appelants n'ont besoin que des types écrits.

## Fichiers

**Créés** — `:core:database` : `logging/DeviceContext.kt`, `logging/AndroidDeviceContext.kt`,
`migration/Migrations.kt`, `schemas/…/2.json`. `:core:system` : `readiness/ReadinessSettingsIntents.kt`
(déplacé), `readiness/IncidentRemediation.kt`, `intent/NiumiDeepLink.kt`,
`androidTest/…/readiness/ReadinessSettingsIntentsTest.kt`, `test/…/readiness/IncidentRemediationTest.kt`.
`:feature:session` : `active/IncidentPresentation.kt`, `incident/IncidentTexts.kt` (libellés partagés
par les écrans 7 et 12, extraits lors de la validation sur appareil), et tout `diagnostics/`
(`IncidentDiagnosticScreen/ViewModel/UiState/Texts`, `DiagnosticExporter`, `DiagnosticSources`) avec
ses tests. `:app` : `navigation/DeepLinkDestination.kt` et son test.

**Modifiés** — journal technique et ses trois implémentations, `TechnicalEventEntity`, `NiumiDatabase`
(v2), `DatabaseModule`, `LoggingModule` ; `DefaultSessionCoordinator` et `SessionModule` ;
`SessionWarningNotificationSpecs` (`tap`) et `SessionWarningNotifier` ; l'écran 7 en entier ;
`ReadinessActionIntents`, `ReadinessScreen` et `PairingScreen` (`:feature:setup`) ; `MainActivity`,
`AndroidManifest.xml` (`launchMode="singleTop"`), `HomeScreen`, `NiumiNavHost` ;
`specs/SPEC_ANDROID.md`.

`MAX_ENTRIES` était dupliqué dans trois fichiers : remplacé par `MAX_TECHNICAL_EVENTS`, exporté.

## Specs mises à jour

- **§7.2** : la base passe en v2, `MIGRATION_1_2`, pourquoi `@ColumnInfo(defaultValue = "''")` est
  nécessaire, pas de `fallbackToDestructiveMigration`.
- **§13** : correction de la règle sur l'emplacement de `settingsIntentFor`, avec la raison de
  l'erreur initiale.
- **§13.1** : la redirection du tap est effective ; `onCreate` **et** `onNewIntent` ; une destination
  inconnue ramène à l'accueil ; **`launchMode="singleTop"` est requis**, avec la mesure qui l'a établi.
- **§15** : nouvelle sous-section « Écran 12 — diagnostic d'incident » (l'écran n'était spécifié nulle
  part, seulement listé) ; « aucune autre action » sur l'écran 7 précisé en « aucune autre action
  **qui touche à la session** », les deux ajouts de l'étape n'en étant pas ; **l'écran 7 présente
  l'état (un code, une fois), l'écran 12 l'historique** ; un incident y est nommé en clair suivi de
  son code.
- **§17** : contexte porté par événement et non en en-tête, avec sa justification ; forme de l'export,
  troncature du `boxId`, hash jamais transporté.

## Vérifications exécutées

```
./gradlew :feature:session:testDebugUnitTest :core:database:testDebugUnitTest \
          :core:system:testDebugUnitTest :feature:setup:testDebugUnitTest \
          :app:testDebugUnitTest :shared:core:jvmTest :feature:ringing:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

**677 tests JVM verts** (641 en fin d'étape 15), après les corrections issues de la validation sur
appareil :

| Module | Tests | Écart |
| --- | --- | --- |
| `:feature:session` | 120 | +20 |
| `:core:system` | 177 | +9 |
| `:core:database` | 106 | +4 |
| `:app` | 17 | +3 |
| `:shared:core` | 160 | — |
| `:feature:setup` | 76 | — |
| `:feature:ringing` | 21 | — |

`:app:assembleDebug`, ktlint, detekt verts. **`:app:lintDebug` sans aucune remontée** (rapport XML :
0 issue). Aucune règle detekt ni ktlint assouplie.

## Validation sur appareil réel

**Déroulée le 2026-09-13 sur Xiaomi 25080RABDG (lapis), Android 16 (API 36), HyperOS OS3.0**, build
`BP2A.250605.031.A3`.

**115 tests instrumentés verts** : `:core:database` 60, `:feature:setup` 34, `:core:system` 21.

| Essai | Résultat |
| --- | --- |
| 1. Service désactivé → écran 7 | **Validé.** Incident `CRITICAL` « Le service d'accessibilité a été désactivé : le blocage ne s'applique plus. » **avec** son bouton « Ouvrir les réglages d'accessibilité ». La notification d'avertissement de §13.1 apparaît en parallèle. |
| 2. Appui sur le recours | **Validé.** `com.android.settings/.accessibility.MiuiAccessibilitySettingsActivity` s'ouvre. |
| 3. Réactivation → reprise du blocage | **Validé.** Adobe Acrobat relancée est renvoyée au launcher sans autre geste ; `BLOCK_APPLIED` en fin de journal. |
| 4. Tap de la notification | **Validé** après correction (voir ci-dessous), avec la vraie notification : l'écran 12 s'ouvre. Chemin `onCreate` **et** chemin `onNewIntent` vérifiés séparément. |
| 5. Export | **Validé partiellement.** Le sélecteur s'ouvre, l'aperçu porte « Niumi — diagnostic Appareil : 25080RABD… ». Le texte intégral n'a pas été relu (voir réserves). |
| 6. Migration 1→2 | **Validé en conditions réelles.** Une base v1 conforme au schéma exporté, portant un événement témoin, a été installée dans le stockage de l'application avant mise à jour : après ouverture, `PRAGMA user_version` = 2, les trois colonnes sont présentes, le témoin est conservé avec `''`, aucune erreur Room ni SQLite. Prouve ce qu'aucun test ne couvrait : que `DatabaseModule` câble réellement la migration. |

Le journal relu en base montre les deux générations de lignes côte à côte — les nouvelles portent
`25080RABDG / 16 (API 36) / 0.1.0 (1)`, le témoin migré garde `''` — validant le travail 1 sur
appareil.

### Trois défauts trouvés par la validation, tous corrigés

1. **`MainActivity` était en `launchMode` standard**, où `onNewIntent` n'est jamais appelé : le tap
   d'un avertissement restait sans effet dès que Niumi était déjà visible. Le KDoc écrit pendant
   l'implémentation affirmait le contraire — **cette affirmation était fausse**. Corrigé par
   `android:launchMode="singleTop"`, revérifié sur les deux chemins, et §13.1 mise à jour.
   Aucun test JVM ne pouvait l'attraper : la lecture de l'extra est une fonction pure, le
   comportement en défaut était celui du système.
2. **L'écran 7 affichait le même incident deux fois, avec deux boutons identiques.** La
   déduplication de `SessionReadinessMonitor` vit en mémoire (étape 12) et ne survit pas à une mort
   de processus ; deux évaluations enregistrent alors deux incidents pour le même fait. L'écran 7
   présente désormais l'**état** — un code, une fois, le plus récent — et l'écran 12 garde
   l'historique complet. Correction de présentation : la logique d'émission n'est pas touchée.
3. **L'écran 12 affichait les codes techniques bruts** (`BLOCKING_PERMISSION_REVOKED`), opaques pour
   l'utilisateur alors que SPEC_CORE_KMP §7.3 veut un diagnostic « visible par l'utilisateur ».
   Il affiche maintenant le libellé en clair suivi du code, §18 exigeant par ailleurs qu'un
   identifiant reste consultable. Les libellés sont extraits dans `IncidentTexts`
   (`:feature:session/incident/`), partagés par les deux écrans : ils ne peuvent plus diverger.

Deux erreurs de ma méthode de test, sans effet sur le code, sont consignées pour mémoire : un tap
lancé sur un bandeau de notification déjà disparu m'a d'abord fait conclure à tort que le deep link
était en défaut ; et `runMigrationsAndValidate` prend la version **cible**, ce que le premier test de
schéma ignorait.

Batterie complète rejouée après corrections : **677 tests JVM**, `:app:assembleDebug`, ktlint,
detekt et `:app:lintDebug` tous verts.

### Réserves subsistantes

- **Le texte intégral de l'export n'a pas été relu sur appareil.** Le sélecteur de partage n'en
  montre que le début, et aucune session associée n'était disponible au moment de l'essai avec un
  `boxId` à masquer. La troncature et l'absence de hash restent couvertes par
  `DiagnosticExporterTest` et `IncidentDiagnosticViewModelTest`, ce dernier partant des `extras`
  réels qui **contiennent** `boxTokenSha256Hex`.
- **`RELEASE_PARTIAL_FAILURE` n'est prouvé qu'en JVM.** Le produire réellement demande un échec de
  `REMOVE_BLOCKING` pendant une libération, donc le scan de l'étape 18.
- **Les deux constats de plateforme de l'étape 15 restent ouverts et hors périmètre** : Android ne
  relie pas le service d'accessibilité après `am crash` (étape 20), et `ReaderModeNfcReader`
  n'emploie pas `FLAG_READER_NO_PLATFORM_SOUNDS` (étape 18).
- **Le doublon d'incidents n'est traité qu'à l'affichage.** La cause — déduplication non persistée —
  reste dans `SessionReadinessMonitor` (étape 12). Persister la déduplication serait un changement
  de comportement d'émission, hors périmètre de cette étape ; à trancher si le sujet revient.

### Constat de plateforme découvert, hors périmètre

**Réinstaller l'application révoque le service d'accessibilité.** Observé à plusieurs reprises
pendant cette validation : après `:app:installDebug` sur une application déjà installée,
`enabled_accessibility_services` revient à vide et `accessibility_enabled` à `0`. Une session armée
survit en base, mais son blocage devient inopérant jusqu'à réactivation manuelle — exactement l'état
que l'écran 7 sait désormais présenter **et** rendre actionnable, ce qui est une bonne nouvelle pour
cette étape.

**Ce qui n'est pas mesuré : le comportement lors d'une mise à jour depuis le Play Store.** Une
réinstallation par `adb` n'est pas une mise à jour de store, et rien ne permet d'en déduire l'une à
partir de l'autre. Si le store produit le même effet, toute mise à jour publiée casserait
silencieusement le blocage des sessions en cours — à mesurer avant publication. Le parallèle existe
déjà pour l'exemption d'énergie, dont §13 dit qu'elle « ne survit ni à une réinstallation ni, selon
toute vraisemblance, à une mise à jour depuis le store ». À rattacher à l'étape 20 (mort du
processus, pertes de permission) ou à l'étape 21 (finalisation release).

## Contradiction de spec repérée, non corrigée

§15, « Étapes de livraison », annonce l'écran 10 (session terminée) livré **à l'étape 15**, alors que
`NiumiNavHost` ne l'enregistre pas et que le plan le place à l'étape 17. La contradiction est
antérieure à cette étape et n'a pas été touchée ici : à trancher à l'étape 17, qui livre cet écran.
