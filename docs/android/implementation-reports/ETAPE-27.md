# Étape 27 — Interface : écran 14 « Sonnerie », lignes des écrans 5, 6 et 7, préférence et activation

Date : 2026-10-01 et 2026-10-02. **Étape terminée pour le code et les tests automatisés**, avec une
validation partielle sur appareil (voir « Ce qui reste » ci-dessous) : code, tests JVM (feature:session,
core:system, app), ktlint, detekt et `:app:lintDebug` verts ; `:app:assembleDebug` vert ; 27 tests
instrumentés `:core:system:connectedDebugAndroidTest` verts (deux passes, avant et après le correctif
du bug trouvé en cours de validation manuelle, voir plus bas).

Seconde étape du Lot 7. L'utilisateur choisit sa sonnerie, l'écoute, règle la montée de volume et
relit son choix sur les écrans 5, 6 et 7 ; une session déjà armée reste modifiable sans scan tant
qu'elle est `ARMED`.

## Ce qui est livré

- **Préférence hors session** (`:core:system`, `audio`) : `AlarmSoundSettings`, `AlarmSoundPreferences`,
  `DataStoreAlarmSoundPreferences` (fichier `niumi_alarm_sound`, même garde de déverrouillage que
  `DataStoreSetupPreferences`). Binding dans `AudioModule`, pas `ReadinessModule` (déjà au plafond
  `TooManyFunctions`).
- **Fin de pré-écoute remontée** (`:core:system`, `audio`) : `RingtonePreviewPlayer.playingKey:
  StateFlow<String?>`, mis à jour par `DefaultRingtonePreviewPlayer` sur `play`, `stop` et la fin
  naturelle du fichier — sans lui, l'écran 14 aurait gardé l'icône « arrêt » affichée après la fin
  d'une pré-écoute sans boucle. Faiblesse de spec signalée (§10.2 ne décrivait que `isPlaying`).
- **Écran 14** (`:feature:session`, `ringtone`) : `RingtoneTexts`, `RingtoneUiState`, `RingtoneViewModel`,
  `RingtoneScreen`/`RingtoneRoute`, `RingtoneActions`. Deux modes décidés par la session lue
  (`SessionPersistenceGateway.load()`), pas par un argument de route : `ARMED` → valeurs et écriture
  de la session, sinon → préférence seule. Sélection en Ambre **et gras** (décision utilisateur du
  2026-10-01, charte §3 : jamais la couleur seule), rôle `RadioButton` pour TalkBack.
- **Lignes partagées** (`:feature:session`, `ui`) : `AlarmSoundTexts` (résumé et libellés de durée,
  sans nombre magique — tables associées à `VolumeRampDurations.SECONDS` et `NiumiRingtones.ALL` par
  position plutôt que recopiées), `AlarmSoundRow` (titre, résumé, chevron optionnel).
- **Écran 5** : `WakeTimeUiState.alarmSoundSummary`, `WakeTimeViewModel` (6ᵉ dépendance
  `AlarmSoundPreferences`, lu à l'`init` et à chaque `refresh`), `WakeTimeActions.onOpenRingtone`,
  ligne sous la section blocage.
- **Écran 6** : `SummaryUiState.alarmSoundSummary`, `SummaryViewModel` (lit la préférence dans
  `refresh`), ligne affichée (pas cliquable) sous « Blocage des applications ».
- **Écran 7** : `ActiveSessionUiState.alarmSoundSummary` (résumé **de la session**, figé à
  l'activation) et `canEditAlarmSound` (`state == ARMED`), `ActiveSessionActions` (regroupe les
  callbacks pour tenir sous `LongParameterList`), ligne cliquable seulement en `ARMED`, ouvrant
  l'écran 14 avec le bandeau « Ce choix s'applique aussi au réveil déjà programmé. ».
- **Activation** : `ActivationSources.alarmSoundPreferences`, `ArmSessionUseCase.arm` lit la
  préférence **au moment d'armer** (`sanitized()`) au lieu des défauts en dur de l'étape 26.
- **Navigation** : `NiumiRoute.Ringtone` (sans argument), enregistrée dans `NiumiNavHost` ; atteinte
  depuis l'écran 5 et, en `ARMED`, depuis l'écran 7 ; retour par la pile. Quinze destinations.
- **Icônes** : trois vector drawables locaux (`ic_play`, `ic_stop`, `ic_chevron_right`) dans
  `:feature:session/res/drawable` — `material3` 1.4.0 n'apporte plus `material-icons-core` (vérifié
  dans son POM), et aucune dépendance n'a été ajoutée.

## Défaut trouvé et corrigé pendant la validation sur appareil (2026-10-02)

**Une préférence jamais écrite affichait « Piano · volume constant » au lieu du vrai défaut « Piano ·
volume progressif sur 2 min ».** Constaté au tout premier essai manuel, écran 5, sur une installation
dont le fichier `niumi_alarm_sound` n'avait jamais été écrit.

Cause : l'encodage du volume constant par l'absence de la clé `volume_ramp_seconds` (décision de
l'étape 27, nécessaire pour un aller-retour fidèle) faisait lire la même chose dans deux cas
distincts : « rien n'a jamais été écrit » et « l'utilisateur a choisi le volume constant ». La
première lecture, avec `ringtoneKey` absent lui aussi, retombait sur les défauts de
`AlarmSoundSettings` pour la clé mais pas pour la rampe, qui restait `null`.

Correctif : `decodeAlarmSoundSettings(ringtoneKey, volumeRampSeconds)` (`:core:system`, fonction pure
testée en JVM) — `ringtoneKey` absent signifie désormais sans ambiguïté « rien n'a jamais été écrit »
(défauts complets, montée comprise), puisque `write()` écrit toujours les deux clés ensemble.
`DataStoreAlarmSoundPreferences.read()` délègue à cette fonction. Quatre tests dans
`AlarmSoundSettingsTest`. Revérifié sur appareil après correctif : l'écran 5 affiche bien
« Piano · volume progressif sur 2 min » sur une préférence jamais écrite.

## Autres écarts et faiblesses de spec signalés

1. **Textes nouveaux, non prévus par §15** : `PREVIEW_FAILED`, `SAVE_FAILED`,
   `SESSION_UPDATE_FAILED` — §15 ne nomme pas ces échecs. Validés par l'utilisateur le 2026-10-01
   avant implémentation, verrouillés par `RingtoneTextsTest`.
2. **Le volume d'alarme à zéro est un cas rare en usage normal** (plancher Android = 1, constat déjà
   fait §13 à l'étape 25) : vérifié en mode Silence total, seul moyen de l'atteindre réellement.
3. **`RoomSessionAlarmSoundStore`, `updateAlarmSound` et `playingKey` étaient déjà livrés à l'étape
   26** : cette étape les consomme sans les modifier, hormis l'ajout de `playingKey`.

## Specs et documents modifiés

- **SPEC_ANDROID §15** : écran 14 (textes, Ambre + gras, mode décidé par la session), lignes des
  écrans 5, 6, 7 ; « quinze destinations ».
- **SPEC_ANDROID §10.2** : `playingKey`.
- **Plan maître** : « Ajouts du Lot 7 » (`playingKey`, `decodeAlarmSoundSettings`,
  `ActiveSessionActions`, `alarmSoundPreferences` dans `ActivationSources`) ; cases de l'étape 27
  cochées pour le travail automatisable.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :feature:session:testDebugUnitTest :core:system:testDebugUnitTest :app:testDebugUnitTest   # verts
./gradlew ktlintFormat
./gradlew ktlintCheck detekt   # verts
./gradlew :app:assembleDebug :app:lintDebug   # verts, « No issues found »
./gradlew :core:system:connectedDebugAndroidTest   # 27 tests, 0 échec — deux passes (avant/après correctif)
grep -rn "RingtonePreviewPlayer\|AlarmAudioEngine" androidApp/feature/session/src/main   # RingtonePreviewPlayer seul
```

## Validation sur appareil — Xiaomi 25080RABDG / Android 16 / HyperOS, 2026-10-02

L'appareil était déjà branché avec le boîtier associé et la sélection d'applications conservés d'une
session antérieure (`:core:system:connectedDebugAndroidTest` n'installe ni ne désinstalle
`com.niumi.app`, seul son propre APK de test).

1. **Tests instrumentés** : faits, voir ci-dessus.
2. **Écran 5, résumé par défaut** : fait, « Piano · volume progressif sur 2 min » après correctif.
3. **Pré-écoutes** : faites. Lecture confirmée par `dumpsys audio`
   (`requestAudioFocus … AA=USAGE_ALARM/CONTENT_TYPE_SONIFICATION`, `MediaPlayer … state:started`,
   mono 44,1 kHz) ; icône lecture/arrêt qui s'inverse ; bascule d'une sonnerie à l'autre (Cloche →
   Oiseaux) confirmée par l'icône et par `dumpsys audio` ; arrêt immédiat en quittant l'écran
   (vérifié : plus aucun `AudioPlaybackConfiguration` de l'app après le retour, aucune écoute
   effectuée à l'oreille — l'agent ne peut pas juger du rendu audio).
4. **Volume d'alarme à zéro** : fait, en activant le Silence total **après** avoir ouvert l'écran 14
   (l'activer avant bloque la navigation au diagnostic, cohérent avec §13). Message affiché, boutons
   de pré-écoute visuellement inactifs, aucun `requestAudioFocus` consigné par `dumpsys audio`.
5. **« Oiseaux », 5 min, sur les écrans 5 et 6** : fait, résumé identique sur les deux écrans après
   retour par la pile.
6. **Session à +2 min, chronométrée** : **non fait**. La session armée pendant cette validation visait
   demain (choix de l'heure non modifié, 07:00) ; chronométrer la montée aurait exigé soit d'attendre
   l'heure réelle, soit de modifier l'heure armée en base, ce qui aurait débordé du périmètre de
   l'étape. Déjà prouvé à l'étape 26 pour le défaut (Piano, 2 min) ; reste à faire pour une sonnerie
   choisie à l'étape 28.
7. **Reprise du choix à la session suivante** : fait implicitement (le résumé relu sur l'écran 5 après
   retour de l'écran 14 reflète la préférence écrite).
8. **TalkBack** : **non fait**. Les `contentDescription` sont posées et vérifiables par le code
   (`content-desc="Écouter Cloche"`, etc., confirmé par `uiautomator dump`), mais l'annonce vocale
   réelle n'a pas été écoutée.
9. **Modification en `ARMED` depuis l'écran 7** : fait. Bandeau affiché ; `Oiseaux`/5 min → `Piano`/2
   min appliqué ; vérifié **directement en base** (`niumi.db` + `niumi.db-wal` tirés par
   `run-as`) : la session est restée `ARMED`, `ringtoneKey = niumi_piano`, `volumeRampSeconds = 120`,
   `revision = 2` — la modification ne crée pas de nouvel événement ni de nouvelle révision au-delà
   de celle déjà prévue.
10. **Redémarrage sans déverrouillage après modification en `ARMED`** : **non fait** (nécessite un
    redémarrage physique de l'appareil, hors du temps disponible pour cette session).

**État laissé sur l'appareil** : une session `ARMED` existe (réveil demain 2026-10-02 à 12:48,
Piano/2 min, une application bloquée). Volontairement non annulée : l'annulation exige un scan NFC
réel, absent de cette session de validation ; c'est un état normal du produit, pas un résidu à
nettoyer.

## Ce qui reste (à l'étape 28 ou avant)

- Essai 6 du protocole (session à +2 min chronométrée avec une sonnerie choisie, pas seulement le
  défaut) ;
- TalkBack (annonce vocale réelle) ;
- Redémarrage sans déverrouillage après modification en `ARMED` ;
- Écoute humaine de la qualité audio des pré-écoutes (l'agent ne peut constater que les faits
  objectifs : focus demandé, format, démarrage/arrêt).

## Incertitudes et limites

- Le volume d'alarme ne peut atteindre zéro qu'en mode Silence total sur cet appareil (plancher à 1
  sinon, §13) : le message de l'écran 14 reste donc rarement vu en usage normal, comme déjà noté pour
  le contrôle de diagnostic à l'étape 25.
- `decodeAlarmSoundSettings` est testé en JVM pour sa logique pure ; l'aller-retour réel du
  `DataStore` reste couvert par les tests instrumentés existants (écriture puis lecture), qui ne
  peuvent pas isoler un état « jamais écrit » au sein d'une suite partagée sans redémarrer
  l'application — c'est la validation sur appareil qui a fait office de preuve pour ce cas précis.
