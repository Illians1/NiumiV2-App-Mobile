# Réveil sans plein écran — conception (Lot 9, étape 32)

Date : 2026-10-18. Décisions prises avec l'utilisateur le même jour, conception approuvée le même
jour. Plan détaillé : étape 32 de `docs/superpowers/plans/2026-09-03-mvp-android.md` (Phase L —
Lot 9).

## 1. Contexte et intention

Aujourd'hui, quand le réveil sonne, la notification de sonnerie porte un `fullScreenIntent` :
`AlarmActivity` s'impose par-dessus l'écran verrouillé et allume l'écran (SPEC_ANDROID §10.2,
§10.3, §10.4). Cette règle existe pour que l'écran qui porte le lecteur NFC soit « immédiatement
accessible » (§4.1).

Ce que les mesures disent (étapes 4, 6 et 30, Xiaomi 25080RABDG / Android 16) : le lecteur NFC
**n'est pas actif tant que le téléphone est verrouillé** (`mIsReaderMode false` sur l'écran
affiché par-dessus le verrouillage). Le boîtier n'est lu qu'après un vrai déverrouillage. L'écran
forcé n'apporte donc que l'allumage de l'écran et le texte « Déverrouille ton téléphone… ». Le
18/10, il a en plus gêné le déverrouillage : l'activité passée en pause à l'ouverture du clavier
de code, HyperOS a rendu l'accueil, et il a fallu rouvrir Niumi.

**Décisions de l'utilisateur (2026-10-18).**
1. Ne plus forcer l'écran. Quand ça sonne sur un téléphone verrouillé : le son et la vibration
   partent, une notification Niumi est visible sur l'écran de verrouillage, l'utilisateur
   déverrouille normalement, touche la notification, l'écran de réveil s'ouvre, il scanne.
2. Le son suffit à réveiller : Niumi n'allume pas l'écran lui-même.
3. Téléphone en usage à l'heure du réveil : la bannière en haut de l'écran peut disparaître
   après quelques secondes ; le son suffit là aussi. L'autorisation de plein écran, le contrôle
   du diagnostic et l'incident sont retirés **entièrement**.
4. Le dossier de design (`design-plan/`) et le plan de refonte (`2026-10-10-refonte-design.md`)
   sont alignés **dans le même changement** (voir §7).
5. L'étape 31 est **committée avant** la première modification (voir « Préalable »).
6. *Ajoutée à l'exécution, mesure du 2026-10-18 (essais 2 et 3).* L'alarme de secours de 60 s (§10.2)
   fait rappeler `startForeground`, et Android re-présentait la notification à chaque minute pleine
   (bannière et vibration de notification), y compris par-dessus l'écran de réveil. Décision : la
   notification de sonnerie porte `setOnlyAlertOnce(true)` — elle n'alerte qu'à sa première
   publication et après un balayage. L'option « bloquer seulement quand l'écran de réveil est
   affiché » a été écartée : un état partagé entre activité et service pour un rappel qui n'avait
   jamais été conçu.

**Préalable fait.** L'étape 31 a été committée avant toute modification (`6c722ef`, 2026-10-18).

Ce changement contredit SPEC_ANDROID §4.1, §10.2, §10.3, §10.4 et §11.2 : conformément à
`CLAUDE.md`, les specs sont mises à jour dans le même changement, **avant le code**. §11.2 exige
qu'une évolution qui retire la re-présentation « propose d'abord un autre chemin de sortie » : ce
chemin est la notification persistante de sonnerie (tap → écran 8) et l'icône de Niumi (→ écran 8
pendant tout état de scan, §10.4), deux chemins qui existent déjà.

## 2. Objectifs, non-objectifs, anti-objectifs

**Objectifs**
- Aucun écran n'est imposé à l'utilisateur ; le déverrouillage se fait comme d'habitude.
- La sonnerie reste visible : une notification lisible sur l'écran de verrouillage et dans le
  volet, tant que la session sonne ou attend un scan.
- L'accès au scan reste garanti par deux chemins : la notification et l'icône de l'application.
- Une permission spéciale de moins (`USE_FULL_SCREEN_INTENT`) à justifier sur Google Play.

**Non-objectifs**
- Allumer l'écran au départ de la sonnerie (mécanisme ancien, déconseillé par Google, incertain
  sur Xiaomi). On **mesure** si l'appareil l'allume de lui-même ; on n'ajoute rien.
- Scanner sur écran verrouillé : impossible sur l'appareil de test, et pas plus visé qu'avant.
- Toucher au son, à la vibration, au blocage, aux états du moteur ou au contrat KMP.

**Anti-objectifs (ce qui ferait échouer le changement même s'il « marche »)**
- Un bouton, une action ou un geste qui arrête la sonnerie sans scan (§3, §15).
- Une sonnerie sans aucun chemin vers le scan : notification absente et non republiée.
- Un contrôle de diagnostic, un incident ou un texte d'aide qui parle encore du plein écran
  (« faux état de fiabilité », §15).

## 3. Comportement cible

| Situation à l'heure du réveil | Aujourd'hui | Après |
| --- | --- | --- |
| Téléphone verrouillé ou écran éteint | écran de réveil forcé, écran allumé ; scan impossible avant déverrouillage | son et vibration ; notification « Alarme Niumi en cours » sur l'écran de verrouillage ; l'écran ne s'allume que si le téléphone le fait pour ses notifications ; déverrouillage, tap sur la notification → écran 8 → scan |
| Téléphone déverrouillé, en usage dans une autre application | bannière « heads-up » **qui reste affichée** tant qu'on n'y touche pas (Android épingle une bannière qui porte un plein écran) ; tap → écran 8 | bannière « heads-up » **quelques secondes**, puis repliée dans le volet ; le son et la vibration continuent ; tap sur la notification ou icône de Niumi → écran 8. Aucun mécanisme autorisé ne garde la bannière sans plein écran (`CallStyle` est réservé aux appels). Mesuré : bannière d'environ 5 s ; sans `setOnlyAlertOnce`, elle revenait à chaque minute avec le rejeu de l'alarme de secours (décision 6) |
| Utilisateur déjà dans Niumi | `MainActivity` redirige vers l'écran 8 | identique |
| Notification balayée pendant la sonnerie (Android 14+) | republiée dans les 10 s, **avec** plein écran si verrouillé | republiée dans les 10 s avec son seul `contentIntent`, quel que soit l'état de l'appareil |
| Verrouillage pendant que l'écran 8 est affiché | l'écran reste visible par-dessus le verrouillage | le verrouillage se fait normalement ; au déverrouillage, Android rend la tâche de l'écran 8 (à mesurer) |
| Redémarrage sans déverrouillage avant l'heure | écran de réveil forcé depuis la projection Direct Boot | sonnerie et notification depuis la projection ; écran 8 au tap après déverrouillage |
| `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` | notification d'attente de scan, sans plein écran (§10.5) | inchangé |

Textes de la notification de sonnerie, **mon choix** : titre « Alarme Niumi en cours » (inchangé),
texte « Touche pour scanner ton boîtier et terminer la session. » (était « Scanne ton boîtier pour
terminer la session. ») — la notification est désormais le chemin, le texte dit le geste.

## 4. Ce qui ne change pas

- Le service de sonnerie (`AlarmRingingService`) : son, vibration, wake lock, watchdog de 60 s,
  reconstruction après mort du processus, surveillance de la notification toutes les 10 s.
- Le canal `niumi_alarm_ringing` : importance haute, catégorie `ALARM`, visibilité publique.
- `AlarmActivity` : tâche propre (`singleTask`), hors des récents, retour inerte pendant un état
  de scan, Reader Mode à `onResume`, relecture NFC sur état stable (étape 31), sorties vers les
  écrans 10 et 11.
- La redirection de `MainActivity` vers l'écran 8 pendant tout état de scan (§10.4).
- La notification d'attente de scan (§10.5) et les avertissements (§13.1), déjà sans plein écran.
- Les six limites de l'onboarding restent six ; seule la formulation de celle du scan verrouillé
  change (voir §7).

## 5. Ce qui disparaît

- `setFullScreenIntent` de la notification de sonnerie ; `AlarmPendingIntentSpecs.fullScreen`
  devient le `contentIntent` de l'écran de réveil (renommé).
- La branche « republier avec plein écran » de `RingingNotificationWatch` et la lecture
  `deviceInUse` (`PowerManager`, `KeyguardManager`) qui ne servait qu'à elle.
- `showWhenLocked`, `turnScreenOn` (manifeste et `setShowWhenLocked` / `setTurnScreenOn`).
- Le receveur `ACTION_USER_PRESENT`, `deviceLocked`, `onDeviceLockChanged` et le texte
  « Déverrouille ton téléphone, puis approche-le du boîtier. » : l'écran n'est plus jamais
  affiché verrouillé. **Mon choix** : les retirer plutôt que les garder « au cas où » — un
  texte qu'aucune situation ne peut afficher est un faux comportement.
- Le contrôle de diagnostic `FULL_SCREEN_INTENT` (§13 passe de quinze à quatorze contrôles),
  `ReadinessAction.OpenFullScreenIntentSettings`, `NotificationAvailability.fullScreenIntent`,
  `SessionRuntimeStatusProbe.fullScreenReady` (déjà sans consommateur).
- L'incident `ANDROID_FULL_SCREEN_REVOKED`, sa ligne de `MonitoredReadinessChecks`, son texte
  d'avertissement, ses libellés sur les écrans 7 et 12.
- La permission `USE_FULL_SCREEN_INTENT` (manifeste de `:app` et du test de `:feature:ringing`) ;
  la liste figée de §14 passe à huit.
- Le document `docs/android/play-console/FULL_SCREEN_INTENT.md` (remplacé par une note de
  retrait datée, pour l'historique) et le plan 8 du script vidéo.

## 6. Contraintes et points de vigilance

1. **Deux chemins vers le scan, toujours.** La notification est celle d'un service au premier
   plan (`setOngoing`), republiée dans les 10 s si elle disparaît (§10.2, inchangé) ; l'icône de
   Niumi mène à l'écran 8. Aucun des deux ne doit régresser.
2. **Visibilité sur l'écran de verrouillage.** Le canal est déjà public ; la notification
   elle-même reçoit `setVisibility(VISIBILITY_PUBLIC)`, pour que son contenu reste lisible même si
   l'utilisateur masque le contenu sensible. HyperOS a en plus un réglage par application
   (« Afficher sur l'écran de verrouillage ») : à mesurer, et à documenter si la valeur par défaut
   cache la notification.
3. **`TechnicalEventType.FULL_SCREEN_DENIED` reste dans l'enum**, plus jamais émis : le journal
   relit ses lignes par `valueOf`, une ligne ancienne ferait lever la lecture. KDoc « historique ».
4. **Identifiants des notifications d'avertissement.** `SessionWarningNotificationSpecs.notificationId`
   est l'index dans `MonitoredReadinessChecks.incidentCodes` : retirer une ligne décale les
   identifiants des contrôles suivants, et un avertissement publié par l'ancienne version ne
   serait plus retiré après la mise à jour. Les identifiants doivent rester ceux d'aujourd'hui
   (table explicite ou trou conservé) ; un test le verrouille.
5. **Un incident `ANDROID_FULL_SCREEN_REVOKED` déjà en base** (session antérieure) reste
   affichable. **Mon choix** : garder son libellé en clair, marqué historique (« L'autorisation
   plein écran avait été retirée (ancienne version de Niumi, sans objet depuis). »), sans recours
   ni contrôle à rejouer — comme `SNAPSHOT_CORRUPTED`, « faits passés », il reste listé avec sa
   gravité jusqu'à la fin de la session. Le code constant reste dans `AndroidIncidentCodes`, hors
   surveillance. Seul l'appareil de test peut en porter un : l'application n'est pas publiée.
5 bis. **Un avertissement « plein écran » encore affiché au moment de la mise à jour** ne serait
   jamais retiré : `clearAll()` ne retire que les identifiants de la liste courante. L'exécutant
   retire une fois l'ancien identifiant (table des identifiants retirés, annulée avec les autres).
6. **Le contrôle « notifications autorisées » devient le seul garant de la visibilité** : son
   message (« Active les notifications pour que l'écran du réveil puisse s'afficher. ») doit dire
   la vérité nouvelle : sans notification, pas de chemin vers le scan hors l'icône.
7. **Limites écrites après mesure seulement** (règle du dépôt) : la ligne d'aide sur le scan
   verrouillé et, le cas échéant, celle sur le réglage HyperOS.
8. Les quinze → quatorze contrôles : tous les `when` exhaustifs sur `ReadinessCheckId`
   (`ReadinessMessages` ×3, `IncidentDiagnosticTexts`, fakes de test) et les comptes dans
   SPEC_ANDROID §13, §15 (écran 12 « les quinze contrôles ») et les rapports.

## 7. Specs et documents (premier travail, avant tout code)

- **SPEC_ANDROID** : §2 (l'« écran de réveil visible au-dessus de l'écran verrouillé » devient
  « une notification visible sur l'écran de verrouillage ») ; §4.1 (condition « alarmes plein
  écran » retirée, « interface de scan accessible en deux gestes : déverrouiller, toucher la
  notification ») ; §4.2 (argument du watchdog sans « plein écran ») ; §4.4 (le scan exige un
  appareil déverrouillé ; l'écran n'est plus affiché par-dessus le verrouillage, décision du
  2026-10-18 et pourquoi) ; §10.1 (chaîne sans full-screen intent) ; §10.2 (republication
  uniquement silencieuse, « le plein écran est le seul mécanisme… » retiré, table de
  reconstruction) ; §10.3 (plus de `setFullScreenIntent`, `canUseFullScreenIntent`,
  visibilité publique de la notification, texte) ; §10.4 (plus de `setShowWhenLocked`,
  `setTurnScreenOn`, re-présentation, `ACTION_USER_PRESENT`, texte de déverrouillage ; la
  seconde garantie d'accès reste l'icône) ; §11.2 (la garantie de scan repose sur la
  notification et l'icône) ; §13 (tableau à quatorze contrôles, message « notifications »
  revu, `NOT_APPLICABLE` plein écran retiré) ; §13.1 (ligne de l'incident retirée, « sept
  contrôles surveillés » → six) ; §14 (huit permissions, `AlarmActivity` sans « affichage
  verrouillé ») ; §15 (écran 12 : quatorze contrôles ; règle « ne jamais mettre une action
  d'arrêt dans l'écran de réveil » inchangée) ; §17 (`FULL_SCREEN_DENIED` historique) ; §19.1,
  §19.2 ; §20 (lignes « écran éteint depuis 30 minutes » → « sonnerie à l'heure, notification
  sur l'écran de verrouillage », « plein écran refusé » retirée, nouvelles lignes du Lot 9) ;
  §21 (critères du Lot 9) ; §22 (Lot 9) ; §23 (« l'usage du plein écran est déclaré comme
  alarme » retiré).
- **SPEC_CORE_KMP** : correspondance « notifications et plein écran ↔ extensions de shield »
  → « notifications ».
- **Play Console** : `FULL_SCREEN_INTENT.md` remplacé par une note de retrait ;
  `REVIEW_VIDEO_SCRIPT.md` plan 8 : « Écran verrouillé, sonnerie, notification sur l'écran de
  verrouillage, déverrouillage, tap, écran de réveil ».
- **Aide et limites** (après mesure) : `LIMITES.md`, `HelpTexts.kt`, `OnboardingTexts.limits`
  (la limite du scan verrouillé devient, texte à fixer après mesure, de l'ordre de : « Le scan
  demande un téléphone déverrouillé : quand ça sonne, déverrouille-le et touche la
  notification Niumi. ») ; la ligne « le silence total … empêche l'écran de réveil de
  s'afficher » devient « … et la notification de sonnerie n'est pas présentée » si la mesure le
  confirme.
- **Plan maître** : « Phase L — Lot 9 : réveil sans plein écran », étape 32, « Ajouts du
  Lot 9 », recette ; la ligne des hypothèses « Les Lots 8 à 16 (étapes 29 à 51…) vivent dans
  la refonte » est corrigée : les Lots 8 et 9 (étapes 29 à 32) du plan maître ont été consommés
  par les boîtiers multiples (2026-10-13) et par ce lot.
- **Dossier de design `design-plan/`** (décision 4 ; ses règles interdisent de trancher un point
  ouvert sans l'utilisateur, c'est fait ici) :
  - `SPECIFICATION.md` §5, tuile « Alarme », colonne Android : « L'affichage plein écran peut
    être retiré par l'utilisateur » retiré ; la phrase « Pour que ton réveil sonne à l'heure,
    même téléphone verrouillé. » reste vraie et ne change pas ;
  - `SPECIFICATION.md` §14 : « Pendant que l'alarme sonne, c'est lui qui s'affiche, pas
    l'accueil » précisé : « dès que Niumi est ouvert — par la notification ou par son icône ;
    l'écran n'est pas imposé par-dessus le verrouillage (décision du 2026-10-18) » ; partie
    « Android » : la notification de sonnerie, visible sur l'écran de verrouillage, est le
    chemin vers l'écran 10 ;
  - `POINTS-OUVERTS.md` T11 : tranché, sans objet (plus d'affichage plein écran).
- **Plan de refonte `2026-10-10-refonte-design.md`** (décision 4) :
  - note datée en tête : les numéros Lot 8-9 / étapes 29-32 sont consommés par le plan maître ;
    la refonte renumérote ses lots et étapes à son démarrage, et son « Point de départ » (clore
    l'étape 28) est périmé ;
  - étape 32 : `ALARM ⇐ EXACT_ALARM` seul (plus de `FULL_SCREEN_INTENT`) ;
  - étape 34 et table des décisions, ligne Q5 : tranché le 2026-10-18 — aucun texte
    « Déverrouille ton téléphone… », l'écran n'est jamais affiché verrouillé ;
  - étape 51 et table des décisions, ligne T11 : sans objet.
- `QA_MATRIX.md`, `RELEASE_REPORT.md` (critères §21 du Lot 9 ; critères 3, 18, 21, 24 relus ;
  ligne 100 sur « plein écran refusé » retirée), `RESTE_A_FAIRE.md`, `ETAPE-32.md`.

## 8. Code (fichiers, par module)

- `:core:system`
  - `notification/RingingNotificationSpecs.kt` (`hasFullScreenIntent` retiré, `visibilityPublic`,
    texte), `RingingNotificationFactory.kt` (`setContentIntent` + `setVisibility`, plus de
    `fullScreen`), `RingingNotificationWatch.kt` (deux actions : `NONE`, `REPUBLISH`),
    `alarm/AlarmPendingIntentSpecs.kt` (`fullScreen` → `alarmScreen`).
  - `readiness/ReadinessCheckId.kt`, `ReadinessAction.kt`, `ReadinessSettingsIntents.kt`,
    `NotificationAvailability.kt`, `AndroidDeviceReadinessChecker.kt`,
    `MonitoredReadinessChecks.kt` (ligne de surveillance du plein écran retirée ; la constante `AndroidIncidentCodes.FULL_SCREEN_REVOKED` est **conservée**, historique, voir §6.5 — corrigé à l'exécution le 2026-10-18),
    `SessionReadinessMonitor.kt`, `IncidentRemediation.kt`, `IncidentReadinessChecks.kt`,
    `notification/SessionWarningNotificationSpecs.kt` (identifiants stables),
    `session/SessionRuntimeStatusProbe.kt`.
- `:feature:ringing` : `AndroidManifest.xml` (drapeaux), `AlarmRingingService.kt`
  (`startForeground` et republication sans plein écran, `deviceInUse` retiré),
  `AlarmActivity.kt` (plus de `setShowWhenLocked`/`setTurnScreenOn`, receveur, `KeyguardManager`),
  `AlarmViewModel.kt`, `ui/AlarmScreenState.kt` (`deviceLocked`, `LOCKED_TEXT`), `ui/AlarmUiState.kt`,
  manifeste de test.
- `:feature:setup` : `readiness/ReadinessMessages.kt` (entrées plein écran retirées, message
  « notifications » revu), `onboarding/OnboardingTexts.kt` (après mesure).
- `:feature:session` : `incident/IncidentTexts.kt`, `diagnostics/IncidentDiagnosticTexts.kt`,
  `active/ActiveSessionTexts.kt`.
- `:core:database` : `logging/TechnicalEventType.kt` (KDoc historique sur `FULL_SCREEN_DENIED`).
- `:app` : `AndroidManifest.xml` (permission), `help/HelpTexts.kt` (après mesure),
  `ReleaseHygieneTest` (huit permissions).

## 9. Tests

- JVM, rouges d'abord : `RingingNotificationSpecsTest` (plus de plein écran, visibilité
  publique, texte), `RingingNotificationWatchTest` (absente → `REPUBLISH`, présente → `NONE`,
  quel que soit l'état), `AlarmPendingIntentSpecsTest`, `AndroidDeviceReadinessCheckerTest`
  (quatorze contrôles, plus de contrôle plein écran), `SessionReadinessMonitorTest`,
  `SessionWarningNotificationSpecsTest` (identifiants inchangés pour `NOTIFICATIONS`,
  `ALARM_VOLUME`, `DND_TOTAL_SILENCE`, `ACCESSIBILITY_SERVICE`, `BATTERY_OPTIMIZATION`),
  `IncidentRemediationTest`, `RuntimeStatusGapsTest`, `AlarmScreenStateTest` (sans état
  verrouillé), `AlarmViewModelTest`, `ReadinessMessagesTest`, `ReadinessViewModelTest`,
  `IncidentPresentation`/`IncidentTexts` (code inconnu affiché tel quel), `ReleaseHygieneTest`,
  `HelpTextsTest`, `OnboardingTextsTest`, `TechnicalEventTypeTest`.
- Instrumentés : `RingingNotificationInstrumentedTest` et `RingingNotificationFactoryInstrumentedTest`
  (`fullScreenIntent == null`, `contentIntent != null`, `visibility == PUBLIC`),
  `AlarmScreenNoStopActionTest` (inchangé), `ReadinessSettingsIntentsTest`,
  `ReadinessActionIntentsTest`, `AndroidSessionWarningNotifierInstrumentedTest`.
- Grep de clôture : aucun `fullScreen`, `FULL_SCREEN`, `showWhenLocked`, `turnScreenOn`,
  `USER_PRESENT`, `deviceLocked` hors de `TechnicalEventType` et des rapports historiques.

## 10. Mesures sur appareil (Xiaomi 25080RABDG / Android 16, lignes §20 du Lot 9)

| Scénario | Résultat attendu |
| --- | --- |
| écran éteint et verrouillé par code, sonnerie | son et vibration à l'heure ; aucune activité ouverte ; après appui sur le bouton, la notification est lisible sur l'écran de verrouillage ; déverrouillage, tap → écran 8 → scan accepté. Noter si l'écran s'est allumé seul et le réglage HyperOS en cause |
| notification balayée pendant la sonnerie, téléphone verrouillé puis déverrouillé | republiée dans les 10 s, sans ouvrir aucun écran ; tap → écran 8 |
| réveil pendant l'usage d'une autre application | notification « heads-up », aucun écran imposé ; tap → écran 8 |
| verrouillage pendant l'écran 8, puis déverrouillage | l'écran 8 revient (tâche rendue par Android), Reader Mode actif, scan accepté |
| redémarrage sans déverrouillage avant l'heure | sonnerie à l'heure depuis la projection Direct Boot, notification sur l'écran de verrouillage, scan après déverrouillage et tap |
| silence total pendant la sonnerie | comportement consigné sans fausse garantie (la notification est-elle présentée ?) |
| Niumi ouvert par son icône pendant la sonnerie | écran 8 (non-régression) |

Contrôles hors §20 : `dumpsys notification` (`visibility=PUBLIC`, pas de `fullScreenIntent`),
`appops` sans `USE_FULL_SCREEN_INTENT`, diagnostic à quatorze lignes, aucun avertissement
« plein écran » possible.

## 11. Hors périmètre, à ne pas corriger ici

- `TIME_CHANGED` après un redémarrage pendant `ARMED` (étape 31, à qualifier).
- Relecture NFC de l'écran 3 depuis le volet rapide.
- Couverture Pixel et Samsung (`RESTE_A_FAIRE.md` B1, B2).

## 12. Ce qui est laissé à l'exécutant

- L'ordre exact des découpages pour rester sous les plafonds detekt (`AlarmRingingService` et
  `AlarmActivity` sont au plafond `TooManyFunctions` ; retirer des fonctions les en éloigne).
- La forme de la table d'identifiants stables des avertissements.
- La formulation finale des lignes d'aide, après mesure.
- Le rapport `ETAPE-32.md` et la mise à jour de la recette.

## 13. Vérification

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest
./gradlew ktlintCheck detekt :app:lintDebug :app:lintRelease
./gradlew :app:assembleRelease :app:bundleRelease
./gradlew connectedDebugAndroidTest        # appareil requis, puis les sept lignes §10
grep -rn "fullScreen\|FULL_SCREEN\|showWhenLocked\|turnScreenOn\|USER_PRESENT\|deviceLocked" androidApp specs --include='*.kt' --include='*.xml' --include='*.md' | grep -v "TechnicalEventType\|implementation-reports\|FULL_SCREEN_DENIED"
```

Pas de commit sans demande explicite.
