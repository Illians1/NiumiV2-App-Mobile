# Étape 32 — Réveil sans plein écran : notification de sonnerie, diagnostic à quatorze contrôles, specs, design et mesures

Date : 2026-10-18. **Étape terminée côté code, tests et mesures** : specs et documents mis à jour
avant le code, tests JVM et instrumentés verts, sept lignes §20 du Lot 9 déroulées sur appareil
(Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM), limites écrites après mesure,
relecture finale par un relecteur à contexte neuf (voir « Relecture finale »). Six lignes §20
conformes, une limite établie (silence total). Une décision prise en cours d'étape avec
l'utilisateur, après mesure : `setOnlyAlertOnce(true)` sur la notification de sonnerie.

Seule étape du Lot 9 (conception `docs/superpowers/specs/2026-10-18-reveil-sans-plein-ecran-design.md`,
décisions de l'utilisateur du 2026-10-18). Point de départ : `6c722ef` (étape 31 committée).

## Ce qui est livré

- **Plus aucun écran imposé au réveil.** La notification de sonnerie ne porte plus de
  `fullScreenIntent` ; son `contentIntent` ouvre `AlarmActivity` (`AlarmPendingIntentSpecs.alarmScreen`,
  ex-`fullScreen`, même cible, même code de requête). Elle est **publique**
  (`setVisibility(VISIBILITY_PUBLIC)` sur la notification elle-même, en plus du canal) et son texte
  dit le geste : « Touche pour scanner ton boîtier et terminer la session. ». Aucune action, aucun
  bouton d'arrêt, comme avant.
- **Republication sans condition d'état.** `RingingNotificationWatch.decide(isPosted)` n'a plus que
  deux issues, `NONE` et `REPUBLISH` ; la lecture `deviceInUse` (`PowerManager`, `KeyguardManager`)
  du service disparaît avec elle. Période inchangée : 10 s.
- **`setOnlyAlertOnce(true)`** (décision 6, prise à l'exécution après mesure — voir « Décisions ») :
  la notification n'alerte qu'à sa première publication et après un balayage, plus aux republications
  de l'alarme de secours. `NotificationSpec.alertOnlyOnce`, vrai pour la sonnerie seule.
- **`AlarmActivity` ne s'affiche plus par-dessus le verrouillage** : `showWhenLocked` et
  `turnScreenOn` retirés du manifeste, `setShowWhenLocked`/`setTurnScreenOn` retirés de l'activité,
  receveur `ACTION_USER_PRESENT` et `KeyguardManager` retirés. `AlarmScreenState` perd `deviceLocked`
  et le texte « Déverrouille ton téléphone, puis approche-le du boîtier. » ; `AlarmViewModel` perd
  `onDeviceLockChanged`. Le retour inerte, la relecture NFC sur état stable et les sorties vers les
  écrans 10 et 11 sont inchangés.
- **Diagnostic à quatorze contrôles.** `ReadinessCheckId.FULL_SCREEN_INTENT`,
  `ReadinessAction.OpenFullScreenIntentSettings`, la branche `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`
  de `ReadinessSettingsIntents`, `NotificationAvailability.canUseFullScreenIntent()` et
  `SessionRuntimeStatus.fullScreenReady` sont retirés. Chaque `when` exhaustif sur `ReadinessCheckId`
  a été relu (`ReadinessMessages` ×3, `IncidentDiagnosticTexts`, fakes de test).
- **Six contrôles surveillés** (`MonitoredReadinessChecks`), **identifiants d'avertissement figés**
  par une table dans `SessionWarningNotificationSpecs` (alarme exacte 3, notifications 5, volume 6,
  silence total 7, accessibilité 8, exemption d'énergie 9) : retirer la ligne du plein écran ne
  décale plus rien. `retiredNotificationIds = {4}` ; `clearAll()` annule `idsToClear()` — les six
  identifiants courants **et** le 4 — pour qu'un avertissement publié par la version antérieure et
  encore affiché au moment de la mise à jour ne reste pas pour toujours.
- **Historique conservé, plus jamais émis.** `AndroidIncidentCodes.FULL_SCREEN_REVOKED` (KDoc
  historique) : un incident déjà en base reste affiché avec le libellé « L'autorisation plein écran
  avait été retirée (ancienne version de Niumi, sans objet depuis). », sans recours
  (`IncidentRemediation`) ni contrôle à rejouer (`IncidentReadinessChecks`).
  `TechnicalEventType.FULL_SCREEN_DENIED` reste relisible par `valueOf`.
- **Huit permissions.** `USE_FULL_SCREEN_INTENT` retirée du manifeste de `:app` et de celui
  d'androidTest de `:feature:ringing` ; `ReleaseHygieneTest` passe à huit, en égalité stricte sur
  le manifeste fusionné release.
- **Textes revus.** Message du diagnostic `NOTIFICATIONS` : « Active les notifications de Niumi :
  c'est par elles que tu ouvres l'écran de scan quand ton réveil sonne. » ; avertissement §13.1
  « notifications » : « Les notifications de Niumi sont désactivées. Au réveil, tu devras ouvrir
  Niumi toi-même pour scanner ton boîtier. » (mon choix) ; message `DND_TOTAL_SILENCE` revu après
  mesure (ci-dessous).
- **Limites écrites après mesure** (`LIMITES.md`, `HelpTexts`, `OnboardingTexts.limits`, mêmes
  phrases) : silence total (« coupe le son et la vibration de l'alarme, et la notification du réveil
  n'apparaît pas sur l'écran de verrouillage : pour scanner, déverrouille ton téléphone et ouvre Niumi
  toi-même » ; restreint à l'écran de verrouillage après la relecture, seul endroit observé), scan
  appareil déverrouillé
  (« Le scan demande un téléphone déverrouillé : quand ça sonne, déverrouille-le, puis touche la
  notification Niumi ou ouvre l'application. »), et une nouvelle puce de la section « Notifications
  et avertissements » (aucun écran ouvert ni allumé, notification visible sur l'écran de verrouillage
  et dans le volet, retour après balayage, bannière repliée après quelques secondes si le téléphone
  est en usage). Les six limites de l'onboarding restent six.

## Décisions prises pendant l'étape

1. **`setOnlyAlertOnce(true)` sur la notification de sonnerie** (décision de l'utilisateur, 17:20,
   après les essais 2 et 3). Mesuré d'abord sans : l'alarme de secours de 60 s (§10.2) rejoue
   `START_RINGING`, le service rappelle `startForeground`, et Android traitait chaque rappel comme une
   mise à jour à présenter — bannière et vibration de notification à **chaque minute pleine**
   (17:15:00, 17:16:00, 17:17:00 au journal `notification_alert`, `buzz=1`), y compris par-dessus
   l'écran de réveil une seconde après son ouverture par le tap (17:16:59 → 17:17:00). Ce rappel par
   minute n'avait jamais été conçu. Deux options présentées : (1) ne jamais re-présenter une
   notification déjà affichée ; (2) ne la bloquer que quand l'écran de réveil est visible, par un
   état partagé activité → service. L'utilisateur a retenu la (1), recommandée : une ligne, aucun
   état entre composants, cohérente avec §10.3 qui disait déjà que la bannière se replie. SPEC_ANDROID
   §10.3 et la conception (décision 6, §3) ont été mis à jour **avant** le code, puis la mesure
   refaite (17:30 à 17:35) : alerte à 17:30:00, **aucune** aux republications de 17:31, 17:32, 17:34,
   17:35 (`buzz=0`), alerte de nouveau après chaque balayage (17:32:50, 17:33:00).
2. **Constante `FULL_SCREEN_REVOKED` conservée.** La conception §8 disait « retiré », en
   contradiction avec son §6.5 et le plan (« conservé, historique ») ; §8 corrigé à l'exécution.
3. **Pas de faux `NotificationManager` en JVM** pour `clearAll`. Le plan demandait un test JVM « avec
   le faux gestionnaire » ; `AndroidSessionWarningNotifier` appelle `NotificationManager` directement.
   Le test JVM porte sur la liste `idsToClear()` ; l'annulation réelle de l'identifiant 4 est prouvée
   par `AndroidSessionWarningNotifierInstrumentedTest.clearAllAlsoWithdrawsTheRetiredFullScreenWarning`
   (publication directe d'une notification d'identifiant 4 sur le canal, puis `clearAll()`). Tâche du
   plan corrigée.
4. **Fichiers touchés hors de la liste du plan** : `ScanRequestPendingIntentSpecs.kt`,
   `NotificationChannelStatus.kt`, `ReadinessCheck.kt` et `MainActivity.kt` (KDoc qui supposaient
   encore le plein écran), `ui/AlarmScreen.kt` (aperçus), `design-plan/POINTS-OUVERTS.md` (écart E12
   ajouté pour la ligne « Alarme » du journal des décisions, annexe historique laissée telle quelle).
5. **Grep de clôture.** Il reste des occurrences de `fullScreen`/`FULL_SCREEN` dans des **tests**
   qui vérifient précisément l'absence du plein écran (`fullScreenIntent == null`, identifiant 4
   retiré, libellé historique) et dans les KDoc. Le code de production n'en contient plus hors des
   deux constantes historiques — c'est ce que le plan visait.
6. **Limite HyperOS « Notifications sur l'écran de verrouillage » non écrite** : le réglage était
   **activé par défaut** pour Niumi et n'a pas été désactivé pendant les essais. Rien n'est écrit
   sans mesure (`RESTE_A_FAIRE.md` C11).

## Specs et documents modifiés dans le même changement

**Avant le code** (premier travail de l'étape) :

- **SPEC_ANDROID** : §2 (notification sur l'écran de verrouillage au lieu de l'écran imposé) ; §4.1
  (« interface de scan accessible en deux gestes », condition plein écran retirée, paragraphe « Plus
  d'autorisation plein écran ») ; §4.2 (silence total : présentation mesurée à l'étape 32 ; argument
  du watchdog) ; §4.4 (le scan exige un appareil déverrouillé, écran plus jamais affiché par-dessus
  le verrouillage, parcours) ; §6 (module `:feature:ringing`) ; §7.1 (`ANDROID_FULL_SCREEN_REVOKED`
  historique, `fullScreenReady` retiré, « trois autres champs ») ; §10.1 (chaîne) ; §10.2 (six
  contrôles ; republication sans re-présentation, historique de la republication selon l'état de
  l'appareil ; table de reconstruction) ; §10.3 réécrit (« Notification de sonnerie » : texte,
  `contentIntent`, `setOnlyAlertOnce`, visibilité publique, aucun plein écran, paragraphe mesuré sur
  la republication) ; §10.4 (plus de `showWhenLocked`/`turnScreenOn`, re-présentation remplacée par
  les deux chemins, alinéa `ACTION_USER_PRESENT` retiré, texte de déverrouillage retiré,
  `addKeyguardLockedStateListener` toujours interdit) ; §11.2 (garantie de scan par deux chemins) ;
  §13 (tableau à quatorze, messages « notifications » et « silence total », implémentation,
  `NOT_APPLICABLE`, mesure DND datée) ; §13.1 (quatre contrôles `ARMED`, ligne de l'incident
  retirée, paragraphe « Retrait du plein écran » avec la table d'identifiants) ; §14 (huit
  permissions, `AlarmActivity`) ; §15 (quatorze contrôles) ; §17 (`FULL_SCREEN_DENIED` historique) ;
  §19.2 ; §20 (ligne « écran éteint », ligne « plein écran refusé » retirée, sept lignes du Lot 9) ;
  §21 (quatre critères) ; §22 (Lot 4 annoté, Lot 9) ; §23 (déclaration plein écran retirée) ; §24.
- **SPEC_CORE_KMP** : table de correspondance des plateformes.
- **`design-plan/SPECIFICATION.md`** §5 (tuile « Alarme », colonne Android) et §14 (écran 10 : « dès
  que Niumi est ouvert — par la notification ou par son icône » ; partie Android) ;
  **`POINTS-OUVERTS.md`** T11 tranché sans objet, écart E11 ajouté. L'annexe
  `journal-des-decisions.md` n'est pas réécrite (export historique).
- **Plan de refonte `2026-10-10-refonte-design.md`** : note de tête (numéros consommés, point de
  départ périmé, plein écran retiré), étape 32 (`ALARM ⇐ EXACT_ALARM` seul), étape 34 et ligne Q5
  (tranché : aucun texte de déverrouillage), étape 51 et ligne T11 (sans objet), scénario « scan sur
  écran verrouillé » corrigé.
- **Play Console** : `FULL_SCREEN_INTENT.md` remplacé par une note de retrait datée ;
  `REVIEW_VIDEO_SCRIPT.md` plan 8.
- **Plan maître** : hypothèse sur les Lots 8 à 16 corrigée (déjà faite à la conception), tâche
  `clearAll` corrigée, « Ajouts du Lot 9 ».
- **Conception** : §8 corrigé (constante conservée), décision 6 et ligne §3 (mesure).

**Après mesure** : `LIMITES.md`, `HelpTexts.kt`, `OnboardingTexts.kt`, message `DND_TOTAL_SILENCE`
(`ReadinessMessages.kt` et SPEC_ANDROID §13), `QA_MATRIX.md` (intro, ligne « écran éteint », ligne
« plein écran refusé » retirée, permissions, sous-section « Réveil sans plein écran (Lot 9) »,
synthèse), `RELEASE_REPORT.md` (critères 41 à 44 ajoutés, anciens 41 et 42 → 45 et 46 ; critères 21
et 24 relus ; écart 6 ; conclusion), `RESTE_A_FAIRE.md` (B4, C10, C11), recette du plan maître.

## Tests écrits

**JVM** (rouge observé par échec de compilation puis vert) :

- `RingingNotificationSpecsTest` : `theRingingNotificationOpensTheAlarmScreenWithoutForcingIt`
  (texte, `ongoing`, `visibilityPublic`), `theRingingNotificationAlertsOnlyOnceWhileItIsShowing`,
  `ringingSpecHasNoAction`.
- `RingingNotificationWatchTest` : `aPostedNotificationIsLeftAlone`,
  `aMissingNotificationIsRepublished`, `theWatchOnlyKnowsTwoActions`.
- `AlarmPendingIntentSpecsTest` (`alarmScreen` cible `ALARM_ACTIVITY`, même code de requête),
  `ScanRequestPendingIntentSpecsTest`, `ScanRequestNotificationSpecsTest` (champ retiré,
  `alertOnlyOnce` faux).
- `SessionWarningNotificationSpecsTest` : `onlyTheSixChecksOfTheSpecTableAreMonitored`,
  `warningIdsAreFrozenAcrossVersions` (3, 5, 6, 7, 8, 9), `theRetiredFullScreenWarningIdIsFour`,
  `clearAllAlsoWithdrawsTheRetiredFullScreenWarning` (`idsToClear()` = 3 à 9).
- `AndroidDeviceReadinessCheckerTest.theDiagnosticHasFourteenChecksAndNoneAboutFullScreen` (Android
  10, 13, 14, 16) ; `fullScreenIntentIsNotApplicableBeforeAndroid14` et
  `deniedFullScreenIntentFromAndroid14BlocksTheNiumiExperience` supprimés.
- `IncidentRemediationTest.theHistoricalFullScreenIncidentHasNoRecourse`,
  `IncidentReadinessChecksTest.theHistoricalFullScreenIncidentHasNoCheckToReplay`.
- `SessionReadinessMonitorTest` (deux branches retirées), `RuntimeStatusGapsTest` (trois contrôles),
  fakes `ReadinessTestSources`, `FakeSessionRuntimeStatusProbe`, `ReadinessTestFixtures`.
- `AlarmScreenStateTest.theInstructionNeverAsksToUnlock` (balaye tous les états × disponibilités ×
  résultats de scan) ; tests du texte de verrouillage supprimés ; `AlarmViewModelTest`,
  `AlarmUiStateTest` sans `deviceLocked`.
- `ReadinessMessagesTest` (messages `NOTIFICATIONS` et `DND_TOTAL_SILENCE` mot pour mot), `ReadinessViewModelTest` (cas `NOT_APPLICABLE`
  porté par `RECENTS_LOCK`), `IncidentTextsTest` (nouveau : libellé historique, code inconnu tel
  quel), `TechnicalEventTypeTest` (liste exhaustive inchangée, `FULL_SCREEN_DENIED` annoté historique),
  `ReleaseHygieneTest.theReleaseManifestDeclaresExactlyTheEightPermissionsOfTheSpec`,
  `OnboardingTextsTest` (limite réécrite), `HelpTextsTest` (correspondance avec `LIMITES.md`).

**Instrumentés** : `RingingNotificationFactoryInstrumentedTest` et `RingingNotificationInstrumentedTest`
(`fullScreenIntent == null`, `contentIntent` présent, `visibility == PUBLIC`, `FLAG_ONLY_ALERT_ONCE`) ;
`AndroidSessionWarningNotifierInstrumentedTest.theSixWarningsKeepTheirFrozenIds` et
`clearAllAlsoWithdrawsTheRetiredFullScreenWarning` ; `ReadinessSettingsIntentsTest`,
`ReadinessActionIntentsTest` (action retirée de la liste) ; `AlarmScreenNoStopActionTest` inchangé
dans son intention.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest
#   avant setOnlyAlertOnce : 1 362 exécutions JVM, 0 échec ; batterie finale (rejouée à 18:28 après la relecture) :
#   1 363 exécutions JVM, 0 échec (shared:core 215 ; le reste réparti comme à l'étape 31, plus
#   IncidentTextsTest et les tests du Lot 9)
./gradlew ktlintCheck detekt :app:lintDebug :app:lintRelease :app:assembleDebug
#   ktlint puis detekt ont d'abord signalé : chaîne d'appels à couper (AlarmRingingService), lignes
#   > 120 (KDoc et un texte d'avertissement), `appUri` devenu inutilisé (ReadinessSettingsIntents) —
#   corrigés, tout vert ensuite
./gradlew :app:assembleRelease :app:bundleRelease
#   manifeste fusionné release relu : huit permissions, aucun showWhenLocked/turnScreenOn
./gradlew connectedDebugAndroidTest
#   1re passe (à partir de 11:14) : six modules verts (app 12, core:database 90, core:system 29,
#   core:designsystem 5, feature:session 3, feature:setup 41 = 180), :feature:ringing refusé par le
#   téléphone (INSTALL_FAILED_USER_RESTRICTED) ; :feature:ringing rejoué seul : 6 tests, 0 échec
#   → 186 tests instrumentés verts, avant setOnlyAlertOnce (les deux assertions FLAG_ONLY_ALERT_ONCE
#   ajoutées ensuite n'ont été vérifiées que par compilation ; voir « Incertitudes »)
grep -rn "fullScreen\|FullScreen\|FULL_SCREEN\|showWhenLocked\|turnScreenOn\|USER_PRESENT\|deviceLocked\|isDeviceLocked" \
  androidApp --include='*.kt' --include='*.xml' | grep -v "/build/\|FULL_SCREEN_DENIED\|FULL_SCREEN_REVOKED"
#   code de production : vide ; restent des tests qui vérifient l'absence du plein écran et des KDoc
# Batterie finale (18/10, rejouée à 18:28 après les correctifs de la relecture), en une commande :
#   jvmTest, testDebugUnitTest, testReleaseUnitTest, ktlintCheck, detekt, lintDebug, lintRelease,
#   assembleDebug, assembleRelease, bundleRelease, compileDebugAndroidTestKotlin — exit 0, 0 échec,
#   APK et AAB release produits à 18:28
```

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, APK de debug installé
par `:app:installDebug` après la passe instrumentée (qui désinstalle l'application : mise en route
refaite à la main, boîtier réassocié). Verrouillage par code actif. Permission OEM de démarrage
automatique inchangée. Réglage HyperOS « Notifications sur l'écran de verrouillage » **activé** pour
Niumi, valeur par défaut, non modifié. Relevés : `dumpsys notification --noredact`, `logcat -b events`
(`notification_enqueue`, `notification_alert`, `notification_visibility`, `notification_canceled`,
`notification_clicked`), `dumpsys trust`, `dumpsys power`, `dumpsys activity`, `dumpsys user`,
`dumpsys nfc`, base Room copiée (`technical_event`, `session_incident`).

État de départ : `dumpsys package com.niumi.app` ne liste plus `USE_FULL_SCREEN_INTENT` ; `appops`
n'affiche pour cette opération que le mode `default` générique.

| N° | Essai (§20, Lot 9) | Résultat |
| --- | --- | --- |
| 1 | écran éteint et verrouillé par code, sonnerie (11:35) | **OK.** Service démarré à 11:35:01,4 (+1,4 s). Pendant 67 s : accueil HyperOS au premier plan, `isKeyguardShowing=true`, `deviceLocked=1`, `mWakefulness=Dozing` — **aucune activité ouverte, écran resté éteint**. Notification : `vis=PUBLIC`, `fullscreenIntent=null`, `contentIntent` présent, `ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE`. Allumage par l'utilisateur 11:36:08 : notification **lisible** sur l'écran de verrouillage ; tap depuis l'écran de verrouillage → code → `AlarmActivity` 11:36:12 ; scan accepté du premier coup ; `COMPLETED` 11:36:25 |
| 4 | verrouillage pendant l'écran 8, puis déverrouillage (11:36) | **OK.** Verrouillage volontaire 11:36:16 pendant l'écran 8 ; déverrouillage 11:36:22 : `AlarmActivity` de nouveau au premier plan **sans geste** ; scan accepté 11:36:23 (Reader Mode actif au retour) |
| 3 | réveil pendant l'usage d'une autre application (17:14) | **OK.** Bannière visible de 17:14:01,2 à 17:14:06,8 (**5,5 s**), accueil resté au premier plan, aucun écran imposé. **Constat** : republication re-présentée à chaque minute pleine (voir décision 1) |
| 2 | notification balayée (17:14, puis 17:30 avec `setOnlyAlertOnce`) | **OK.** Sur l'écran de verrouillage HyperOS, **la notification ne se balaie pas**. Déverrouillé, balayage dans le volet 17:15:05,06 → revenue 17:15:10,20 (**5,1 s**), aucun écran ouvert ; tap 17:16:59 → écran 8, scan 17:17:02. Seconde session : balayage 17:32:44,24 → retour 17:32:50,37 (**6,1 s**) avec bannière ; second balayage 17:32:59,22 → retour 17:33:00,35 (tic de la minute), bannière 5,5 s ; tap 17:35:04 → scan 17:35:29 |
| — | mesure de `setOnlyAlertOnce` (17:30 → 17:35) | 7 publications, toutes `ONGOING_EVENT|ONLY_ALERT_ONCE|FOREGROUND_SERVICE` ; `notification_alert` avec `buzz=1` à 17:30:00 (départ), 17:32:50 (après balayage) ; `buzz=0` à 17:31:00, 17:32:00, 17:34:00, 17:35:00 ; l'utilisateur confirme : **aucune bannière** aux minutes 17:31 et 17:32, bannière au retour après balayage |
| 6 | silence total pendant la sonnerie (17:36 → 17:44) | **Limite établie.** `cmd notification set_dnd none` pendant `ARMED` (17:36:07, `zen_mode=2`, `mInterruptionFilter=3`) : incidents `ANDROID_ALARM_MUTED_BY_DND` et `ANDROID_ALARM_VOLUME_ZERO` consignés à 17:36:07 (`SESSION_READINESS_DEGRADED` ×2, `ALARM_MUTED_BY_DND`), avertissements 6 et 7 publiés, retirés à 17:43:41 à la sortie d'`ARMED`. 17:39:00 : service démarré, lecteur `USAGE_ALARM` `state:started` mais `STREAM_ALARM` `Muted: true` ; utilisateur : **ni son, ni vibration** ; notification publiée, **non alertée** (`notification_alert` sans son ni vibration ; ce journal ne dit rien de l'affichage visuel) et **invisible sur l'écran de verrouillage** (utilisateur : « Non ») ; le volet après déverrouillage n'a pas été regardé, Niumi étant revenu par sa tâche. Niumi était ouvert avant le verrouillage : au déverrouillage (17:43:41) sa tâche est revenue sur l'écran 8 (redirection de `MainActivity`), scan 17:44:05. Mode Ne pas déranger rétabli à `off` (`zen_mode=0`, `Muted: false`) |
| 5 | redémarrage sans déverrouillage avant l'heure (17:47 → 17:55) | **OK.** Projection v4 relue avant (`localTime 17:52`) ; `adb reboot` 17:47:26, `boot_completed` 17:48:06, `ALARM_RESCHEDULED` 17:48:08,8 ; utilisateur `RUNNING_LOCKED` jusqu'à 17:53:23 ; `ALARM_RECEIVED` 17:52:00,05, `RINGING_STARTED` 17:52:00,3 (+0,3 s), notification publiée depuis le contexte protégé (`vis=PUBLIC`, `fullscreenIntent=null`), écran resté éteint ; allumage 17:53:16, notification **lisible** sur l'écran de verrouillage ; déverrouillage 17:53:22 (`RUNNING_UNLOCKED` 17:53:23, fusion Direct Boot : deux republications à 17:53:25,99, silencieuses). Fin par l'icône (ligne 7) |
| 7 | Niumi ouvert par son icône pendant la sonnerie (17:53) | **OK.** Tâche Niumi inexistante après le redémarrage : icône du lanceur 17:53:26 → `MainActivity` → redirection → `AlarmActivity` 17:53:27 (`LAUNCH_SINGLE_TASK`), sans passer par l'accueil. « Le NFC est désactivé… » affiché quelques secondes puis effacé seul (`NFC_DISABLED` 17:53:27 ; pile NFC `ON` 17:53:37 ; Reader Mode de Niumi `flags: 1` à 17:53:33,5 et 17:53:37,3 dans `dumpsys nfc` — le `logcat` n'avait pas gardé ces lignes, d'où un doute levé a posteriori). Premier scan passé trop vite refusé 17:54:35 (`NFC_SCAN_INVALID`, « Boîtier non reconnu. Réessaie. »), second accepté 17:54:52 (`COMPLETED`). Bannière vue à l'ouverture : première présentation visuelle d'une notification publiée écran éteint (interprétation, cohérente avec `ONLY_ALERT_ONCE`) |

Observations annexes : à 17:39:00 et 17:44:05, Android a créé puis retiré un résumé de groupe
automatique (`g:Aggregate_AlertingSection`, identifiant 0) quand plus de trois notifications Niumi
étaient actives (1, 6, 7) — sans effet sur Niumi, déjà connu de
`AndroidSessionWarningNotifierInstrumentedTest`. Après le déverrouillage de l'essai 5, l'alarme de
secours est réarmée depuis la passe `USER_UNLOCKED` (tics à 17:54:25 et non 17:54:00).

## Observations hors périmètre, non corrigées

- **Trente minutes d'écran éteint** non remesurées avec le nouveau comportement (5 min seulement, USB
  branché) : `RESTE_A_FAIRE.md` C10.
- **Réglage HyperOS d'affichage sur l'écran de verrouillage désactivé** : non mesuré (C11).
- `TIME_CHANGED` après un redémarrage pendant `ARMED` (étape 31) : non revu ici ; aucun incident de
  ce type n'a été consigné pendant le redémarrage de l'essai 5 (`session_incident` vide pour la
  session, horloge inchangée).

## Relecture finale

Relecteur à contexte neuf (agent séparé, sans accès à cette conversation), le 2026-10-18 : conception,
plan, ce rapport, `CLAUDE.md`, le diff complet et les fichiers non suivis. **Aucun constat bloquant.**
Code conforme à SPEC_ANDROID §10.2 à §10.4, §13, §13.1 et §14 ; régressions vérifiées sans défaut
(republication, table d'identifiants, incident historique, cycle de vie d'`AlarmActivity`, ordre des
textes de l'écran de réveil). Constats corrigés dans le même changement :

1. SPEC_ANDROID : deux « sept contrôles » restants → six ; renvoi de §10.3 à `LIMITES.md` pour le
   réglage HyperOS, absent de l'aide → renvoi à `RESTE_A_FAIRE.md` C11 ; « mesurée à l'étape 32 » en
   §4.2 et §13 → résultat énoncé ; liste du Lot 0 annotée.
2. **Silence total généralisé au-delà de la mesure** : « la notification n'est pas affichée »
   (aide, `LIMITES.md`, diagnostic) → « n'apparaît pas sur l'écran de verrouillage », seul endroit
   observé ; « raison 512 » ne prouvait que l'absence d'alerte, pas la suppression visuelle.
3. Message `DND_TOTAL_SILENCE` sans test mot pour mot alors que cité en preuve → test ajouté.
4. `POINTS-OUVERTS.md` : identifiant E11 en double → E12.
5. `RELEASE_REPORT.md` : lignes 41 à 44 placées avant la 40 → réordonnées ; écart 4 (« écran de
   réveil ») corrigé.
6. `QA_MATRIX.md` : renvoi §4.4 périmé (« `AlarmActivity` s'affiche par-dessus le verrouillage »)
   et ligne DND marqués historiques.
7. Plan : signature `NotificationSpec` et commentaire de fabrique des « Ajouts du Lot 9 » complétés ;
   deux dernières cases cochées.
8. Tests : `TechnicalEventTypeTest.theHistoricalFullScreenDeniedEventIsStillReadable` retiré
   (tautologique, la liste exhaustive l'attrape déjà) ; `repeat(3)` sans objet et nom périmé de
   `RingingNotificationWatchTest` ; nom de `OnboardingTextsTest` ; commentaire « historique » dans
   `ActiveSessionTextsTest` ; KDoc « les neuf autres » de `ReadinessMessages`.
9. Ce rapport : heures de la passe instrumentée, liste des fichiers hors plan.

Laissés en remarque : le texte d'avertissement `NOTIFICATIONS` (mon choix) n'est ni dans la spec ni
figé par un test, comme les autres avertissements (seule leur distinction est testée) ; un ancien
incident `ANDROID_FULL_SCREEN_REVOKED` resterait dans le bloc `CRITICAL` de l'écran 7 avec un libellé
« sans objet » et aucun recours, choix de la conception §6.5 ; trois fichiers non suivis à ajouter au
commit (`IncidentTextsTest.kt`, ce rapport, la conception).

## Incertitudes et limites

- **Les deux assertions `FLAG_ONLY_ALERT_ONCE` des tests instrumentés** ont été ajoutées après la
  passe instrumentée complète et vérifiées par compilation seulement ; la notification réelle porte
  bien le drapeau (`dumpsys notification` à 17:30, 17:39 et 17:52), mais les deux tests n'ont pas été
  rejoués. À rejouer avec la prochaine passe `connectedDebugAndroidTest`.
- **Diagnostic à quatorze lignes** prouvé par test JVM, pas relu à l'écran 2 sur appareil.
- **Affichage des nouveaux textes d'aide** (écran 13, onboarding) non relu à l'écran ; les tests de
  textes font foi.
- **Interprétation** de la bannière vue à l'ouverture par l'icône (essai 7) : première présentation
  visuelle après une publication écran éteint ; non prouvée par un journal de SystemUI.
- **Un seul appareil, une seule surcouche** : le comportement de la notification sur l'écran de
  verrouillage (visibilité, impossibilité de la balayer) et la suppression de sa présentation en
  silence total sont ceux de HyperOS ; Pixel et Samsung restent à couvrir (B1, B2).

**Rien n'est committé.**
