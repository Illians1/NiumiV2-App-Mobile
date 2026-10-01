# Étape 26 — Sonneries, montée progressive, Room v4, Direct Boot v3 et service

Dates : 2026-09-30 et 2026-10-01. **Étape terminée** : code, tests automatisés et deux essais manuels verts sur appareil (voir la fin du rapport) :
1 218 tests JVM verts, ktlint, detekt et Android Lint verts, `:app:assembleDebug` et
`assembleDebugAndroidTest` verts. **Tests instrumentés verts sur appareil** : 97 tests, 0 échec, sur
Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM.

Première étape du Lot 7. Le moteur joue maintenant l'une des quatre sonneries, en boucle, avec ou
sans montée progressive du volume, d'après ce qui est enregistré dans la session. Rien ne change
encore dans l'interface (étape 27) : une session armée aujourd'hui sonne « Piano » avec une montée
sur 2 min.

## Décisions prises pendant l'étape

Le plan interdisait d'ouvrir l'étape avant que l'utilisateur ait tranché les huit décisions de la
phase J. Elles ont été soumises le 2026-09-30 ; trois ont changé.

| Décision | Réponse | Par |
| --- | --- | --- |
| 1, 3, 6, 7, 8 | Confirmées telles qu'écrites | utilisateur |
| 2 — défaut | **« Piano », montée progressive sur 2 min** (au lieu de « Cloche », volume constant) | utilisateur |
| 4 — réglage figé | **Copié dans la session à l'activation, mais modifiable sans scan tant que la session est `ARMED`** | utilisateur |
| 5 — normalisation | **Volume perçu aligné à −16 LUFS avec un limiteur** (au lieu d'un alignement des crêtes) ; les 0,3 s de résonance finale de « Cloche » sont conservées | utilisateur |
| — | Anciennes sessions (`niumi_alarm`) : « Piano » **à volume constant**, puisqu'elles n'avaient pas choisi de montée | plan, présenté avant exécution |
| — | Chemin dégradé (session absente ou illisible) : « Piano » à volume constant, jamais une montée | plan, présenté avant exécution |
| — | `RINGTONE_FALLBACK` écrit à une vraie tentative de démarrage, jamais aux relances du chien de garde (même règle que `RINGING_STARTED`) | exécution |

**Pourquoi la décision 5 a changé.** Mesurés à la réception avec `ffmpeg ebur128`, les volumes
perçus allaient de −13,0 LUFS (Oiseaux) à −25,0 LUFS (Piano). Aligner les crêtes laissait encore
environ 9 dB d'écart entre Cloche et Piano : au même réglage, Piano aurait paru presque deux fois
moins fort.

**Pourquoi la décision 4 a changé.** La sonnerie ne fait pas partie de l'engagement : elle ne touche
ni l'heure, ni le blocage, ni la sortie par scan. SPEC_ANDROID §3 décrit désormais cette unique
exception à « toute modification exige le scan », et §15 en fait la seule action de l'écran 7 qui
touche à la session sans scan.

## Ce qui est livré

- **Fichiers audio** : les quatre sonneries sont normalisées (PCM 16 bits, mono, 44,1 kHz,
  −16 LUFS). `niumi_alarm.wav` et `tools/generate_alarm_wav.py` sont supprimés dans le même
  changement que le branchement des nouvelles clés (point de vigilance 14). `RingtoneResources`
  (`:feature:ringing`) fait la correspondance entre chaque clé et sa ressource `R.raw`, et renvoie
  `null` pour toute autre clé.
- **Catalogue et montée** (`:core:system`, `audio`) :
  - `NiumiRingtones` : `Ringtone`, `ALL`, `DEFAULT_KEY = "niumi_piano"`, `LEGACY_KEY`, `byKey` ;
  - `VolumeRamp`, `VolumeRampDurations` (30, 60, 120, 300 s ; 120 par défaut) ;
  - `VolumeRampPolicy` : courbe `10^(−2·(1−p))`, pas de 250 ms ;
  - `AlarmSound` et `AlarmAudioEngine.start(sound)`.
- **Moteur** : `DefaultAlarmAudioEngine` crée le lecteur directement à l'amplitude correspondant au
  temps déjà écoulé depuis `ringingAtEpochMillis`. Un job sur un scope dédié la réajuste ensuite
  jusqu'à la durée choisie. Tout l'état est protégé par un verrou : un pas de rampe ne touche jamais
  un lecteur déjà libéré. `MediaPlayerAlarmPlayerFactory` applique le volume **avant** `start()`.
- **Pré-écoute** : `RingtonePreviewPlayer`, `DefaultRingtonePreviewPlayer` et `PreviewPlayerFactory`,
  liés dans `AudioModule`. Ils seront consommés à l'étape 27.
- **Ce qui sonne** : `RingingSoundResolver` et `ResolvedAlarmSound`. `AlarmRingingService` lit la
  session puis démarre le son dans son scope ; `RINGTONE_KEY` disparaît. `TechnicalEventType`
  gagne `RINGTONE_FALLBACK`.
- **Room v4** :
  - `AndroidSessionExtras.volumeRampSeconds` (sans valeur par défaut) et
    `AlarmSessionEntity.volumeRampSeconds` ;
  - `MIGRATION_3_4` et `LegacyRingtone` ;
  - `schemas/4.json` exporté ;
  - `freezeFrom` et la fusion Direct Boot → Room figent aussi `volumeRampSeconds`.
- **Direct Boot v3** : `DirectBootSnapshot.Active.volumeRampSeconds`. Une projection v1 ou v2 se
  lit à volume constant, et `niumi_alarm` y est lu comme `niumi_piano`.
- **Modification pendant `ARMED`** : `SessionCoordinator.updateAlarmSound`, exécuté sous le même
  verrou que `dispatch`, puis `SessionPersistenceGateway.updateAlarmSound`.
  `UnlockAwarePersistenceGateway` écrit Room puis recopie la session dans Direct Boot. L'écriture
  Room passe par `RoomSessionAlarmSoundStore` (requête `SessionDao.updateAlarmSound`). Aucun
  événement du moteur commun n'est émis, et la révision ne change pas.
- **`ArmSessionUseCase`** : arme avec `DEFAULT_KEY` et `DEFAULT_SECONDS`.

## Fichiers audio

Les originaux, tels que déposés, sont sauvegardés hors du dépôt dans
`~/Dev/niumi-sonneries-originales/` (sommes SHA-1 vérifiées identiques avant la conversion).

Commande, par fichier ; le gain est ajusté par itérations jusqu'à ±0,2 LU de la cible, mesure
faite sur le fichier produit :

```bash
ffmpeg -i <original>.wav -ac 1 -ar 44100 \
  -af "volume=<gain>dB,alimiter=limit=0.8414:level=disabled:attack=5:release=50,aresample=osf=s16:dither_method=triangular" \
  -c:a pcm_s16le -map_metadata -1 -fflags +bitexact -flags:a +bitexact <clé>.wav
# mesure : ffmpeg -i <clé>.wav -af ebur128=peak=sample -f null -
```

`limit=0.8414` correspond à −1,5 dBFS. `level=disabled` empêche le limiteur de renormaliser sa
sortie.

| Sonnerie | Original (stéréo) | Original ramené en mono | Gain | Après | Crête après | Durée | Taille avant → après |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Cloche | −22,9 LUFS, crête −13,4 | −25,9 LUFS, crête −14,2 | +9,9 dB | −16,0 LUFS | −4,3 dBFS | 7,22 s | 1,27 → 0,64 Mo |
| Énergique | −16,5 LUFS, crête −0,1 | −20,0 LUFS, crête −0,6 | +4,7 dB | −16,2 LUFS | −1,5 dBFS | 16,00 s | 4,24 → 1,41 Mo |
| Oiseaux | −13,0 LUFS, crête −1,0 | −18,1 LUFS, crête −3,6 | +2,1 dB | −16,2 LUFS | −2,8 dBFS | 10,00 s | 1,77 → 0,88 Mo |
| Piano | −25,0 LUFS, crête −6,3 | −28,8 LUFS, crête −9,4 | +16,7 dB | −16,2 LUFS | −1,6 dBFS | 15,00 s | 5,29 → 1,32 Mo |

Total : 12,58 Mo → 4,25 Mo. L'ancienne sonnerie, `niumi_alarm`, était à −9,5 LUFS : **les quatre
nouvelles sont environ 6,5 dB moins fortes qu'elle** au même réglage de volume.

**Écart par rapport à ce qui a été annoncé avant la décision.** Les chiffres présentés à
l'utilisateur (« le limiteur écrête ≈ 4 dB sur Piano, ≈ 1,5 dB sur Énergique ») avaient été
calculés sur les mesures stéréo. Le passage en mono fait perdre 3 à 5 dB de volume perçu. Il a donc
fallu +16,7 dB sur Piano au lieu des +12,8 dB prévus, et le limiteur lui retire environ 4 dB de
volume perçu, ce qui signifie une compression audible possible de ses attaques. Énergique perd
environ 0,7 dB ; Cloche et Oiseaux ne sont pas touchées. **À juger à l'oreille** pendant l'essai
manuel 2. Si le rendu déplaît, il suffit de relancer la commande depuis les originaux avec une
cible plus basse (−18 LUFS réduirait nettement l'écrêtage de Piano, au prix de 2 dB de volume
pour toutes).

**Poids de l'APK debug**, empaqueté à neuf dans les deux cas (l'empaquetage incrémental d'AGP en
debug laisse des trous dans le fichier : l'APK de l'arbre de travail mesurait 51,3 Mo avant d'être
réempaqueté) :

| | Taille |
| --- | --- |
| `HEAD` (`5aac41f`, `niumi_alarm` seule) | 37,83 Mo |
| Arbre de travail avant l'étape (+ quatre originaux non suivis, point de vigilance 14) | ≈ 50,4 Mo (37,83 + 12,58) |
| Après l'étape 26 | **41,60 Mo** (+3,77 Mo par rapport à `HEAD`, dont 3,72 Mo de sons et 0,05 Mo de code) |

## Défauts évités pendant l'étape

1. **Un blocage différé en attente aurait été levé à la mise à jour.** `DirectBootMapper`
   reconnaissait une projection v1 par `projectionSchemaVersion < DIRECT_BOOT_PROJECTION_SCHEMA_VERSION`.
   En passant la constante à 3, toute projection v2 aurait été relue comme une v1 : blocage
   immédiat, déjà appliqué à la création. Une session différée en attente, relue avant
   déverrouillage après la mise à jour, aurait perdu son attente. Le seuil est désormais figé
   (`PROJECTION_VERSION_WITH_BLOCKING_SCHEDULE = 2`), et deux tests le prouvent : la lecture d'un vrai
   fichier JSON v2 dans `DirectBootSnapshotJsonTest` et `aVersionTwoProjectionKeepsItsPendingBlocking`.
2. **Une sonnerie sans bouton d'arrêt.** Le son démarre maintenant dans une coroutine, après la
   lecture de la session. Si le scan arrête le service pendant cette lecture, `start()` pouvait
   passer **après** le `stop()` de `onDestroy`, et plus rien ne faisait taire le son. Le service
   sérialise les deux sous un verrou, avec un drapeau `destroyed`. **Non testable en JVM**, faute de
   pouvoir instancier le service : c'est un point de revue, à surveiller sur appareil.
3. **Une rampe qui fait planter le réveil.** Une exception levée par `setVolume` dans le scope de la
   rampe aurait été non rattrapée, donc fatale au processus. Elle arrête désormais la rampe seule,
   et le son continue.

## Specs et documents modifiés

- **SPEC_ANDROID** :
  - §3 : quatre sonneries, défaut, exception « sans scan » en `ARMED` ;
  - §7.2 : colonne, gel, alinéa v4 ;
  - §7.3 : champ, alinéa « Projection v3 » ;
  - §10.1 : compte « 26 valeurs » de §17 retiré, déjà faux avant l'étape ;
  - §10.2 : puces du service et alinéa complet du Lot 7 (catalogue, fichiers, boucle, montée,
    résolveur, modification en `ARMED`, pré-écoute) ;
  - §15 : écran 14, lignes des écrans 5, 6 et 7, textes figés ;
  - §17 : `RINGTONE_FALLBACK` ;
  - §19.1 et §19.2 ;
  - §20 : onze lignes du Lot 7 ;
  - §21 : six critères ;
  - §22 : Lot 7.
- **SPEC_CORE_KMP : aucune modification.** La sonnerie est une donnée de plateforme, hors du
  snapshot commun ; `updateAlarmSound` n'émet aucun événement et ne change pas la révision.
- **Plan maître** :
  - « Validation du 2026-09-30 » (décisions 2, 4 et 5) ;
  - points de vigilance 14 et 15 ;
  - « Ajouts du Lot 7 » (nouvelles interfaces, défauts, `LegacyRingtone`, seuils de projection,
    noms de la pré-écoute) ;
  - textes des étapes 26, 27 (ligne de l'écran 7, bandeau et message de l'écran 14) et 28 (onze
    lignes) ;
  - recette du Lot 7 ;
  - hypothèses.
- **`docs/BRIEF_PROPOSITIONS_FRONT_END.md`** : le plan demandait de le modifier, mais **ce document
  n'existe pas dans le dépôt**. Le plan le signale désormais.

Textes d'interface figés dans §15 **sans avoir encore été relus par l'utilisateur** :
- « Ce choix s'applique aussi au réveil déjà programmé. » (écran 14 ouvert depuis l'écran 7) ;
- « Le réveil a déjà commencé : ce choix s'appliquera à ta prochaine session. » (session sortie
  de `ARMED` entre-temps).

À confirmer à l'ouverture de l'étape 27.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :feature:ringing:testDebugUnitTest --tests '*NiumiRingtoneWavTest*'   # rouge avant normalisation (stéréo, niumi_alarm.wav présent), vert après
./gradlew :core:system:testDebugUnitTest --tests '*VolumeRampPolicyTest*'       # rouge (non compilé), puis vert
./gradlew ktlintFormat                                                           # ne touche que des fichiers de l'étape
./gradlew ktlintCheck detekt                                                     # code retour 0
./gradlew testDebugUnitTest :shared:core:jvmTest :app:testReleaseUnitTest :app:assembleDebug assembleDebugAndroidTest   # verts
./gradlew :app:lintDebug                                                         # « No issues found »
```

Tests JVM : `:shared:core` 205, `:core:system` 493, `:core:database` 131, `:feature:session` 190,
`:feature:setup` 76, `:feature:ringing` 42, `:app` 38 (debug) et 43 (release). Total : 1 218, aucun
échec.

`:app:lintDebug` a échoué une fois lancé dans la même commande que `assembleDebugAndroidTest`
(`FileNotFoundException` sur une source Hilt générée pendant son analyse) : c'est une course entre
tâches parallèles, sans rapport avec le code. Relancé seul, il est vert.

Grep de clôture `niumi_alarm` : `NiumiRingtones.LEGACY_KEY`, `LegacyRingtone` (utilisé par
`MIGRATION_3_4` et `DirectBootMapper`), des KDoc et les tests, rien d'autre. Aucune référence à
`R.raw.niumi_alarm`.

## Tests écrits

- **JVM, nouveaux** :
  - `VolumeRampPolicyTest`, `NiumiRingtonesTest`, `RingtonePreviewPlayerTest` ;
  - `LegacyRingtoneTest` (concordance entre `:core:database` et le catalogue) ;
  - `RingingSoundResolverTest` ;
  - `SessionCoordinatorAlarmSoundTest` (`ARMED`, `RINGING`, clé ou durée hors liste, appareil
    verrouillé, décision après modification) ;
  - `UnlockAwarePersistenceGatewayAlarmSoundTest` (recopie dans Direct Boot) ;
  - `NiumiRingtoneWavTest`, qui remplace `NiumiAlarmWavTest` et parcourt les blocs RIFF dans
    n'importe quel ordre ;
  - `RingtoneResourcesTest`.
- **JVM, étendus** :
  - `DefaultAlarmAudioEngineTest` : volume constant, rampe au pas de 250 ms, reprise à 45 s,
    arrêt, idempotence, échec sans rampe orpheline ;
  - `RingingStartJournalTest` ;
  - `DirectBootMapperTest` et `DirectBootSnapshotJsonTest` (vrai fichier v2) ;
  - `ExportedSchemaTest` (v4) ;
  - `TechnicalEventTypeTest` ;
  - `ArmSessionUseCaseTest` ;
  - toutes les doublures de `SessionCoordinator` et de `SessionPersistenceGateway`.
- **Instrumentés, verts sur appareil le 2026-09-30** :
  - `NiumiDatabaseSchemaTest.migrationThreeToFourRewritesTheLegacyRingtoneAndAddsTheRampColumn` ;
  - `RoomSessionStoreCommitTest` (gel de la sonnerie et de la montée) ;
  - `RoomDirectBootMergeTest` ;
  - `RoomSessionAlarmSoundStoreTest` (nouveau) ;
  - `AlarmChainInstrumentedTest` : session en `niumi_oiseaux` avec une montée de 30 s, pas de
    `RINGTONE_FALLBACK`, moteur réel en lecture. « Oiseaux » plutôt que « Piano », comme le plan le
    prévoyait : Piano étant devenu le défaut, seule une autre sonnerie prouve que c'est bien le
    réglage de la session qui joue ;
  - `ProcessDeathInstrumentedTest` et `BlockingStartReceiverInstrumentedTest` (clé et champ mis à
    jour).

## Validation sur appareil et ce qui reste

1. ~~Tests instrumentés~~ — **faits le 2026-09-30 à 13:43**, Xiaomi 25080RABDG / Android 16 /
   HyperOS OS3.0.302.0.WPPEUXM, en une passe de 1 min :
   ```bash
   ./gradlew :core:database:connectedDebugAndroidTest :feature:ringing:connectedDebugAndroidTest :app:connectedDebugAndroidTest
   ```
   `:core:database` 79, `:feature:ringing` 6, `:app` 12 : 97 tests, 0 échec, 0 ignoré. La migration
   3→4 sur une base v3 peuplée, `RoomSessionAlarmSoundStoreTest` (Room et projection Direct Boot
   relue) et `AlarmChainInstrumentedTest` (vraie alarme exacte, « Oiseaux » avec montée, moteur réel
   en lecture, aucun `RINGTONE_FALLBACK`) sont passés. Aucun `INSTALL_FAILED_USER_RESTRICTED` cette
   fois. **L'application et ses données ont été désinstallées par la passe** : boîtier et sélection
   sont à refaire avant les essais manuels.
2. ~~Essai manuel 1 — mise à jour par-dessus une session armée~~ — **vert le 2026-10-01**, Xiaomi
   25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, permission OEM de démarrage automatique
   dans son état courant, horloges du Mac et du téléphone concordantes à la seconde.
   - 12:32 : APK de `89220b0` installé sur un appareil vierge (désinstallé par la passe
     instrumentée), parcours complet, une application bloquée (`com.adobe.reader`), réveil à 12:42,
     blocage « Maintenant ». Session armée à 12:34:33.
   - Avant la mise à jour : projection Direct Boot **v2**, `ARMED`, `ringtoneKey = "niumi_alarm"`,
     révision 2 ; alarme `RTC_WAKEUP` à 12:42:00.000 (`exactAllowReason=policy_permission`).
   - 12:34:58 : APK de l'étape 26 installé par-dessus (`adb install -r`), sans ouvrir l'application
     ensuite. Alarme de 12:42:00 toujours présente après la mise à jour.
   - Volume des alarmes baissé avant l'heure (personnes à proximité ; non nul, pour ne pas créer
     d'incident). Cela ne change rien à la mesure : l'amplitude est relative à ce plafond.
   - **12:42:00.205 : lecteur `MediaPlayer` créé**, `USAGE_ALARM` / `CONTENT_TYPE_SONIFICATION`,
     mono 44,1 kHz, focus `GAIN_TRANSIENT_MAY_DUCK`. Relevé à 12:42:33 : lecteur `started`,
     `AlarmRingingService` au premier plan (`types=0x2`, `mediaPlayback`), notification de catégorie
     `alarm`.
   - **Base après coup** : `user_version = 4`, `ringtoneKey = "niumi_piano"`,
     `volumeRampSeconds = NULL`. La migration 3→4 a donc tourné au premier accès et réécrit la clé.
     Journal : `ALARM_RECEIVED` puis **un seul** `RINGING_STARTED` à 12:42:00, **aucun
     `RINGTONE_FALLBACK`**, et aucune exception Niumi dans le logcat.
   - **À l'oreille (utilisateur)** : « C'est Piano et le son n'augmente pas » : plein niveau dès le
     départ, comme attendu pour une session antérieure au Lot 7.
   - Scan vers 12:43 : session close, service arrêté, lecteur libéré, projection effacée, aucune
     alarme Niumi en attente (l'alarme de secours est désarmée).
   - L'écoute des trois boucles et le jugement sur le limiteur n'ont **pas** été faits (personnes à
     proximité, sonnerie coupée après une trentaine de secondes) : ils passent à l'essai 2.
3. ~~Essai manuel 2 — défaut~~ — **vert le 2026-10-01**, même appareil. Session armée avec le défaut
   (projection v3 relue avant le réveil : `niumi_piano`, `volumeRampSeconds = 120`, vibration),
   réveil à 12:48:00, volume des alarmes à un niveau de réveil normal, écran éteint, scan vers
   12:50:30.
   - **Mesure objective de la montée.** Le logcat de l'appareil journalise chaque
     `AudioTrack::setVolume` de la session audio du lecteur (3761). Sur 490 écritures, la série est
     strictement croissante et reste à moins de 1 % de la courbe `10^(−2·(1−p))`. Départ à 0,01006
     (−40 dB), appliqué avant le premier échantillon. 30 s : 0,0315 (théorie 0,0316) ; 60 s : 0,0999
     (0,1000) ; 90 s : 0,3170 (0,3162). Plein volume à **T+120,3 s**, puis constant. L'origine de la
     rampe, déduite de la série, tombe à T+0,105 s : c'est bien `ringingAtEpochMillis`.
   - **À l'oreille (utilisateur)** : « Le son était bien progressif ». Sur le Piano normalisé :
     « à peu près ok », donc pas de dégradation gênante due au limiteur.
   - **Boucle : limite mesurée, non audible à l'écoute.** Sur ce Xiaomi, `MediaPlayer` arrête puis
     relance son `AudioTrack` à chaque reprise (`stop` → `start` → `pause` → `flush` dans le
     logcat). Les reprises tombent toutes les **15,07 à 15,23 s** pour un fichier de 15,000 s, soit
     probablement un silence de 70 à 230 ms par jonction. L'utilisateur n'a **rien entendu de
     particulier** sur neuf reprises : le blanc tombe dans la décroissance de la dernière note du
     Piano. Il reste à réécouter sur les trois autres sonneries à l'étape 28 (ligne « chacune des
     quatre sonneries » de §20). Énergique et Oiseaux, sans décroissance finale, y sont les plus
     exposés. Correctifs possibles sans dépendance nouvelle, si un blanc devient audible : fondu
     de raccord dans les fichiers, ou `MediaPlayer.setNextMediaPlayer` en alternance.
   - Après le scan : service arrêté, projection effacée, aucune alarme Niumi en attente.
4. **`updateAlarmSound` sur appareil** : reporté à l'étape 27, faute d'interface pour le déclencher.
5. **Course entre `start` et `stop`** dans le service : protégée par construction, non observable
   en test. À surveiller lors du scan pendant les premières secondes de sonnerie.

## Incertitudes et limites

- Le `MediaPlayer` du réveil est maintenant créé sur un fil `Dispatchers.Default` (auparavant sur
  le fil principal, dans `onStartCommand`). `MediaPlayer` renvoie alors ses événements au fil
  principal, ce que la documentation d'Android prévoit. Aucun rappel n'est utilisé pour le réveil,
  mais c'est un changement à confirmer sur appareil (essai 2).
- Le service lit désormais `vibrationEnabled` depuis la session (auparavant, toujours `true`). En
  production, la valeur est toujours `true` ; seul `AlarmChainInstrumentedTest` arme sans vibration.
- La montée ne module que l'amplitude du lecteur de Niumi. Le volume des alarmes du système reste le
  plafond : une montée « jusqu'au maximum » atteint le réglage de l'utilisateur, pas le maximum du
  téléphone.
- Avec le nouveau défaut, une session où l'on n'a rien réglé démarre presque inaudible. Les lignes
  audio de `QA_MATRIX.md`, mesurées à volume constant, sont à relire à l'étape 28 (onzième ligne
  ajoutée à §20).
- Le dossier racine `ui/` (non suivi, contient un `classes.jar`) est étranger à cette étape et n'a
  pas été touché.
