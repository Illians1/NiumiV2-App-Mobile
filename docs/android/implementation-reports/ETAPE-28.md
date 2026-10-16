# Étape 28 — Validation mesurée des sonneries et de la montée, matrice, aide et rapport

**État au 2026-10-15 : étape faite, avec un manque nommé.** Dix des onze lignes §20 du Lot 7 sont
conformes sur Xiaomi 25080RABDG / Android 16 ; la onzième (boucle des quatre sonneries) est
partielle : seule « Oiseaux » a été réécoutée avec le nouveau lecteur `AudioTrack`, adopté le
15/10 après une coupure de boucle audible avec `MediaPlayer`. Cinq des six critères §21 du Lot 7
sont prouvés, le 30 est partiel. Deux limites mesurées sont écrites dans l'aide. La réécoute de
« Piano », « Cloche » et « Énergique » a été reportée par l'utilisateur ; TalkBack n'a pas été
testé. Détails dans « Reprise du 2026-10-15 » et « Sessions E et F ».

*Historique — état au 2026-10-13 : étape ouverte, non terminée.* La partie sans matériel est faite et verte.
Les sessions A, B et C (réglage par défaut, Cloche à volume constant, Énergique sous DND
« alarmes seules ») sont mesurées avec succès. Un resserrement du catalogue des durées de montée
a été décidé et implémenté (2/5/10/15 min, voir plus bas). La session D (Oiseaux, 15 min) a
révélé un redémarrage de la montée en cours de sonnerie, sans mort de processus ; une tentative de
reproduction le 2026-10-13 (15 min complètes, mêmes conditions) **ne l'a pas reproduit** — ni
confirmé comme un vrai défaut récurrent, ni écarté. Niumi est **désinstallé** de l'appareil de
test (deux sessions terminées par désinstallation faute du bon boîtier). Aucune case du plan
maître n'est cochée : conformément à la règle du plan, « une case cochée signifie prouvé, jamais
implémenté ».

## Ce qui est fait et vérifié

### Sans appareil

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest   # BUILD SUCCESSFUL
./gradlew ktlintCheck detekt :app:lintRelease                              # BUILD SUCCESSFUL, aucune remontée
./gradlew :app:assembleRelease :app:bundleRelease                          # BUILD SUCCESSFUL
```

`app-release-unsigned.apk` (7,4 Mo) et `app-release.aab` (8,0 Mo) produits, non signés
(`keystore.properties` absent du dépôt, cf. A1 de `RESTE_A_FAIRE.md` — inchangé).

### `tools/validate_alarm.sh` étendu (Lot 7)

Ajouts, conformes au plan de l'étape :

- lecture de `ringtoneKey` et `volumeRampSeconds` depuis la projection Direct Boot sur l'appareil
  (`run-as com.niumi.app cat /data/user_de/0/com.niumi.app/files/niumi_session.json`) avec repli
  sur `--ringtone`/`--ramp` si cette lecture échoue (APK release) ;
- durée du fichier de sonnerie lue dans l'en-tête RIFF du `.wav` du dépôt (recherche du sous-bloc
  `data`, jamais une valeur recopiée en dur) ;
- relevés `dumpsys audio` programmés aux instants `t0`, `t0 + durée/2`, `t0 + durée + 5 s` (avec
  montée) et `t0 + 3 × durée du fichier` (boucle, systématique) ;
- analyse du logcat pour la série de `setVolume` et les reprises de boucle (voir le défaut trouvé
  et corrigé ci-dessous) ;
- code de sortie non nul si le service n'est jamais détecté ; ligne « notification » ciblée sur la
  notification de sonnerie (écart 11 de `RELEASE_REPORT.md`, partiellement traité : le filtre est
  resserré, non revérifié sur une notification de groupe réelle faute de temps).

Vérifié par `bash -n` et par un harnais jetable (`source` du script, qui ne lance jamais `main()`
grâce à la garde `BASH_SOURCE`) : 17 assertions, toutes vertes, portant sur `wav_duration_seconds`
(les quatre sonneries), `amplitude_at` (cinq points de `VolumeRampPolicy`), `read_json_field`
(chaîne, nombre, `null`, absent) et le parseur logcat (voir ci-dessous). Harnais non committé
(scratchpad de session), à reconstituer si une prochaine étape en a besoin.

**Défaut trouvé et corrigé en cours de campagne (2026-10-06).** La première mesure réelle sur
appareil (session A, voir plus bas) a montré que le parseur logcat initial ne reconnaissait **aucune**
ligne de `setVolume`, alors que des lignes correctement formées étaient visibles dans le brut.
Cause : sur ce Xiaomi, la ligne `AudioTrack::setVolume L:… R:…` est **partagée par tout le
système** (musique, NFC, notifications d'autres applications) et ne porte elle-même aucun nom de
paquet ; l'attribuer à Niumi exige de suivre le dernier marqueur de paquet vu sur les lignes
voisines (`AudioTrackImpl: before scaleVolumeForVideo … and the pakage is <pkg>` juste avant,
`AudioTrack: start(...): … packageName: <pkg>` juste après — la faute « pakage » est bien dans le
log du fournisseur, pas une coquille de ce rapport). Corrigé par une fonction `niumi_tagged_lines`
qui porte cet état d'une ligne à l'autre. Revérifié avec un extrait **authentique** du logcat
capturé pendant la session A (NFC et Niumi interleaved dans le même flux) : 2 valeurs de
`setVolume` et 6 évènements de boucle correctement attribués à Niumi, le NFC exclu. La capture
logcat est aussi désormais filtrée à la source (`-s AudioTrack:* AudioTrackImpl:* *:S`) et le
fichier n'est plus supprimé en fin de script (son chemin est imprimé), après que la première
tentative a montré qu'un tampon non filtré de ~2 minutes sur ce téléphone fait tourner le ring
buffer avant qu'une analyse tardive puisse relire les toutes premières valeurs de la montée.

## Campagne sur appareil — Xiaomi 25080RABDG / Android 16 / HyperOS, 2026-10-06 et 2026-10-07

- **Tests instrumentés** : `connectedDebugAndroidTest` complet, 166 tests, 0 échec, 1 ignoré
  (`AndroidAccessibilityServiceStatusInstrumentedTest.aComponentActuallyPresentInSettingsIsReportedAsEnabled`,
  comme documenté §22/critère 22 de `RELEASE_REPORT.md`). **Premier essai en échec** : le même test
  a échoué une fois (service d'accessibilité resté enregistré d'une campagne précédente, désinscrit
  en cours de passe par l'enchaînement d'installations/désinstallations des autres modules — état
  transitoire, pas une régression de ce dépôt ; confirmé par un second essai isolé, vert). À noter
  pour une prochaine campagne : ne pas s'étonner d'un échec ponctuel de ce test précis juste après
  une longue période sans réinstallation.
- **Session A, tentative 1 (défaut Piano, casque Bluetooth connecté)** : `STREAM_ALARM` montrait
  `Devices: speaker(2), bt_a2dp(80)` (flux dupliqué confirmé), service détecté 1 s après l'instant
  cible, volume alarme 5/15, `ringtoneKey`/`volumeRampSeconds` lus avec succès dans la projection
  Direct Boot (`niumi_piano` / `120` — confirme que le chemin device-protected storage deviné dans
  le plan est correct sur cet appareil). **L'analyse logcat a échoué** (0 valeur reconnue, défaut
  ci-dessus) ; l'utilisateur a scanné avant la fin de l'analyse (le son devenait gênant — usage
  normal, aucune action d'arrêt logicielle n'existe, §3).
- **Session A, tentatives 2 et 3** : le script corrigé a été relancé deux fois pour reprendre la
  mesure, mais la coordination en temps réel (armement très rapide côté utilisateur, décalage avec
  le moment où le script écoutait la confirmation) a fait manquer la fenêtre à chaque fois — une
  fois parce que le script avait déjà lu « non armé » avant l'activation réelle, une fois parce
  qu'une alarme pour le lendemain a été armée par erreur puis corrigée à 13 h 55, le temps disponible
  s'épuisant entre-temps. **Aucune des deux n'a produit de mesure.** Dans les deux cas, l'utilisateur
  a mis fin à la sonnerie par un scan normal ; l'état de l'appareil a été vérifié propre après coup
  (aucune alarme Niumi en attente, aucun service de sonnerie actif) dans les deux cas.
- **Décidé avec l'utilisateur le 2026-10-06** : la campagne s'arrête là pour cette session, faute
  de temps ; elle reprend le 2026-10-07.

### Reprise du 2026-10-07 — Session A mesurée avec succès

Reprise avec le script corrigé la veille. Deux incidents de coordination avant la mesure utile,
tous deux sans conséquence sur le produit (sessions terminées normalement par un scan) :

- **Erreur de lecture de l'epoch par l'agent** : l'heure cible de l'alarme a été mal convertie à
  l'oral (« 15:50 » annoncé au lieu de « 15:10 », calcul non fait) ; l'utilisateur l'a repérée
  immédiatement. Leçon : toujours calculer l'epoch, jamais l'estimer.
- **Fenêtre d'écoute manquée de 16 s** : le script, lancé via un FIFO pour attendre la confirmation
  de l'utilisateur avant de démarrer son décompte de `max_wait`, a commencé à compter trop tard par
  rapport à l'armement réel (délai de coordination entre la confirmation orale et l'envoi du
  signal au FIFO) et a expiré 16 s avant le déclenchement réel. **Correctif de méthode, pas de
  code** : l'agent a basculé sur une mesure manuelle (lecture directe de `ringingAtEpochMillis`
  dans la projection Direct Boot dès le déclenchement constaté par sondage `dumpsys`, capture
  logcat continue filtrée en parallèle) plutôt que de relancer le script une troisième fois.

**Mesure obtenue** (défaut jamais configuré, casque Bluetooth connecté, `ringingAtEpochMillis` =
1791378600108, soit **108 ms** après l'instant cible) :

- `niumi_piano`, `volumeRampSeconds = 120` lus dans la projection Direct Boot — confirme le
  correctif `decodeAlarmSoundSettings` de l'étape 27 (le vrai défaut, pas « volume constant ») ;
- `STREAM_ALARM` → `Devices: speaker(2), bt_a2dp(80)`, flux dupliqué confirmé, casque connecté ;
- **488 valeurs de `setVolume` attribuées à Niumi** (NFC exclu du même flux), strictement
  croissantes, de 0,010077 à 1,000000, plein volume atteint à **T+120,04 s** (cible 120 s).
  Comparaison théorie (`VolumeRampPolicy.amplitudeAt`) / mesure à chaque palier, toutes à mieux que
  0,3 % d'écart relatif :

  | Palier | Théorie | Mesure |
  | --- | --- | --- |
  | 0 % | 0,0100 | 0,0101 |
  | 10 % | 0,0158 | 0,0159 |
  | 25 % | 0,0316 | 0,0317 |
  | 50 % | 0,1000 | 0,0997 |
  | 75 % | 0,3162 | 0,3159 |
  | 90 % | 0,6310 | 0,6327 |
  | 100 % | 1,0000 | 1,0000 |

- **Boucle** : période mesurée entre 15,19 et 15,24 s pour un fichier de 15,00 s (neuf reprises sur
  la fenêtre), cohérent avec la mesure de l'étape 26 (15,07–15,23 s). Confirmé à l'oreille par
  l'utilisateur : aucun blanc ni clic perceptible sur « Piano ».
- Fin de session : scan effectué, service arrêté, aucune alarme Niumi restante, projection Direct
  Boot effacée.

**Deux défauts supplémentaires trouvés et corrigés dans `tools/validate_alarm.sh` pendant cette
reprise**, tous deux dans le parseur logcat (pas dans le moteur audio du Lot 7) :

1. Déjà connu de la veille (isolement du flux par paquet via `niumi_tagged_lines`) — confirmé
   fonctionner en conditions réelles cette fois.
2. **Nouveau : l'ancre `^[0-9]+\.[0-9]+` du timestamp ratait 100 % des lignes** d'une capture
   `adb logcat` en flux continu (sans `-d`), qui préfixe chaque ligne d'espaces de cadrage que le
   mode `-d` (dump, utilisé par le script) ne produit pas forcément de la même façon. Corrigé en
   retirant l'ancre (`match($0, /[0-9]+\.[0-9]+/)` sans `^`) : le timestamp reste le premier nombre
   à virgule de la ligne dans les deux cas. Verrouillé par un cas de régression dans le harnais
   jetable (lignes avec espaces en tête, échoue sans le correctif).

Couvre dans §20 (Lot 7) : « réglage par défaut, jamais modifié », « casque Bluetooth connecté,
montée progressive », et la sonnerie « Piano » pour « chacune des quatre sonneries ».

### Session B — Cloche, volume constant (2026-10-07)

Réglage choisi à l'écran 14 (`niumi_bell`, `volumeRampSeconds = null`). Déclenché à **T+243 ms**.

- **Montée** : 60/60 valeurs de `setVolume` à `1.000000` — plein volume dès le départ, aucune
  rampe, conforme à « volume constant ».
- **Boucle** (28 cycles observés) : période 7,29–7,43 s pour un fichier de 7,22 s (écart
  70–210 ms), aucun blanc ni clic audible confirmé par l'utilisateur.
- **Écran 7 pendant `RINGING`** : **constat, pas la vérification prévue par le plan.**
  L'utilisateur n'a accès qu'à l'écran de réveil (hors `NavHost`, §13.1) pendant la sonnerie,
  jamais à l'écran 7 lui-même. La ligne « Sonnerie » ne peut donc pas être testée « non cliquable »
  à cet instant : elle est simplement **inatteignable**, ce qui satisfait le critère (figé dès
  `RINGING`) plus strictement qu'un bouton désactivé l'aurait fait. Le libellé exact du plan («
  écran 7 ouvert ») était imprécis ; la case correspondante de §20 est néanmoins satisfaite par
  construction.
- Fin de session : scan effectué, service arrêté, aucune alarme restante.

### Session C — Énergique, montée 30 s, DND « alarmes seules » (2026-10-07)

Réglage choisi à l'écran 14 (`niumi_energique`, `volumeRampSeconds = 30`). DND réglé par l'agent
via `adb shell cmd notification set_dnd alarms` (`settings put global zen_mode 3` seul n'a pas
suffi — nécessite la commande `notification`). Déclenché à **T+79 ms**.

- **Montée** (208 points) : théorie (`amplitude_at`) et mesure concordent à chaque palier (0, 25,
  50, 75, 100 %), toutes à moins de 2 % d'écart relatif.
- **Boucle** : période 16,09–16,25 s pour un fichier de 16,00 s (90–250 ms), confirmée propre à
  l'oreille — notable car Énergique n'a pas la décroissance finale qui masque le blanc sur Cloche
  et Piano (réserve de l'étape 26).
- **Audible sous DND « alarmes seules »** : `STREAM_ALARM` non muté, lecteur `USAGE_ALARM` actif,
  confirmé à l'oreille par l'utilisateur. Couvre la ligne « Ne pas déranger "alarmes seules",
  montée progressive » de §20.
- **Incident de méthode, sans conséquence sur la mesure** : le flux `adb logcat` de cette session a
  continué à recevoir des lignes de la fin de la session B (chevauchement, buffer non vidé entre
  deux sessions). La comparaison théorie/mesure n'en a pas été affectée (filtrée par fenêtre
  temporelle autour de `ringingAtEpochMillis`), mais la liste brute des évènements de boucle
  mélangeait les deux sonneries. Correctif de méthode appliqué dès la session D : `adb logcat -c`
  (vidage du tampon) avant chaque nouvelle capture.
- **Coupure adb inopinée entre les sessions B et C** : l'appareil s'est déconnecté
  (`adb: no devices/emulators found`), reconnecté après un geste de l'utilisateur (câble ou
  pop-up d'autorisation). Sans conséquence : aucune session n'était en cours à ce moment.
- Fin de session : scan effectué, service arrêté, aucune alarme restante.

### Session D — Oiseaux, montée 5 min : interrompue, non mesurée (2026-10-08)

Armée avec succès (`niumi_oiseaux`, `volumeRampSeconds = 300`), déclenchée à **T+208 ms**. Relevés
partiels avant l'interruption : à T+175,7 s (58,6 % de la montée), amplitude théorique attendue
0,148 (≈ −17 dB), volume alarme réglé à 7/15.

**Observation de l'utilisateur, non tranchée** : la montée sur « Oiseaux » ne se percevait pas
comme progressive — « ça commence déjà relativement fort ». Hypothèse non vérifiée : les
fréquences aiguës d'un chant d'oiseau pourraient percer davantage à amplitude égale qu'un son
grave (Piano, confirmé « bien progressif » à l'étape 26, est plus grave). **À rejouer** avant de
conclure quoi que ce soit : comparer Oiseaux et Piano à amplitude identique et volume alarme
identique, si possible plus bas que 7/15 pour mieux distinguer les premiers paliers.

**Incident, sans rapport avec la mesure** : l'utilisateur a scanné un boîtier NFC qui n'était pas
celui associé à la session (`boxId` attendu : `7c9e6a41-3b2d-4f58-9a0e-1d5c8b3f2a67`), et n'avait
plus accès au bon boîtier. Déroulé, conforme à la conception du produit (§3, aucun secours
logiciel) :

1. `am force-stop com.niumi.app` : son coupé immédiatement, service arrêté, alarme retirée.
2. **La sonnerie est revenue à la réouverture de l'app par l'utilisateur.** Attendu : `force-stop`
   ne fait que tuer le processus, il ne modifie pas l'état persisté (`state: RINGING`, jamais
   relâché par un scan valide) ; au redémarrage, la réconciliation relit cet état et relance le
   service de sonnerie. C'est exactement le filet déjà mesuré à l'étape 20 pour `am kill`/`am
   crash` pendant `RINGING` — il couvre aussi `force-stop` suivi d'une réouverture, ce qui n'avait
   pas été nommément vérifié jusqu'ici.
3. Le bon boîtier restant indisponible, **`pm clear com.niumi.app`** a mis fin à la session de
   façon définitive, à la demande explicite de l'utilisateur (après lui avoir proposé l'alternative
   d'un simple arrêt forcé temporaire). Efface tout : boîtier associé, sélection d'applications,
   préférences de sonnerie, journal technique. Service d'accessibilité désinscrit par le système
   (comportement standard d'un effacement de données), confirmé par `dumpsys`.

**Conséquence pour la suite** : Niumi est reparti dans un état d'installation neuve. Les sessions D
(à refaire), E et F exigent de refaire tout le parcours (diagnostic, accessibilité, exemption
batterie, verrou récents, association du boîtier, sélection d'applications) avant de pouvoir
armer une nouvelle session.

### Déviation produit : resserrement des durées de montée (2026-10-08)

Pendant la session D, l'utilisateur a jugé la montée sur « Oiseaux » (5 min) perçue comme trop
rapide dès le départ. Conflit avec une décision déjà validée (plan maître, Lot 7, décision 3 du
2026-09-30 : liste fermée 30 s / 1 min / 2 min / 5 min) signalé avant toute modification,
conformément à CLAUDE.md. Décidé avec l'utilisateur : nouvelle liste **2 min / 5 min / 10 min /
15 min**, 30 s et 1 min retirées.

**Essai sans risque avant de coder.** Un premier essai informel (écrire `volumeRampSeconds = 600`
directement dans la base Room, hors du code, pour juger « 10 min » avant de formaliser quoi que ce
soit) a **fait planter l'application** dès que l'utilisateur a ouvert l'écran de session : `600`
sort du catalogue fermé que lisait encore le code (`[30, 60, 120, 300]`), et
`AlarmSoundTexts.durationLabel` fait exprès `error(...)` sur une durée hors catalogue (« échoue
bruyamment plutôt que silencieusement », design voulu, pas un bug). Restauré à `300` (valeur
légitime) par la même méthode, confirmé réparé par l'utilisateur. Round-trip de la base Room (avec
WAL) documenté pour une prochaine fois : `adb exec-out run-as … cat` pour tirer `db`/`db-wal`/
`db-shm`, `sqlite3` local pour modifier et **checkpointer en `TRUNCATE`** (fusionne tout dans le
fichier principal, élimine le besoin de retransférer le WAL), `adb push` vers `/data/local/tmp`
puis `run-as … cp` vers `databases/` (un pipe direct via `adb exec-out run-as … sh -c 'cat >
fichier'` s'est bloqué indéfiniment — raison non élucidée, éviter cette forme).

**La question n'avait pas besoin d'un second essai sur appareil.** `VolumeRampPolicy.amplitudeAt`
ne dépend que de la fraction écoulée de la durée (`p = elapsed/duration`) : doubler la durée
divise par deux, à tout instant réel, la vitesse de montée en dB. Calculé plutôt que mesuré à
nouveau : à T+60 s, amplitude ≈ 0,025 (5 min) contre ≈ 0,016 (10 min) — sensiblement plus bas,
confirmant que l'allongement répond au ressenti rapporté.

**Changement implémenté avec TDD**, revue complète :

- Code : `VolumeRampDurations.SECONDS` (`:core:system`) → `[120, 300, 600, 900]`,
  `DEFAULT_SECONDS` inchangé (120, déjà dans la nouvelle liste — aucun changement pour une session
  jamais configurée) ; table `AlarmSoundTexts.DURATION_LABELS` (`:feature:session`) →
  « 2 min »/« 5 min »/« 10 min »/« 15 min ».
- Tests mis en rouge puis au vert : `VolumeRampPolicyTest` (liste exacte), `AlarmSoundTextsTest`
  (quatre libellés). Tests qui échantillonnaient une durée retirée (30 ou 60) **pour un usage sans
  rapport avec le catalogue lui-même** (vérifier la sanitisation de la clé de sonnerie, le
  résolveur de sonnerie, l'écriture de session après modification, les extras d'armement) —
  corrigés vers une valeur toujours valide, jamais contournés : `AlarmSoundSettingsTest`,
  `SessionCoordinatorAlarmSoundTest`, `ArmSessionUseCaseTest`, `RingtoneTestFakes.kt`
  (`SESSION_EXTRAS_SETTINGS`), `RingtoneTextsTest`.
- `RingtoneScreen.kt` (segmented buttons) n'a nécessité aucun changement : il itère déjà
  `VolumeRampDurations.SECONDS` sans valeur recopiée en dur.
- Vérifications, toutes vertes après le changement :

  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17
  ./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :app:testDebugUnitTest :core:database:testDebugUnitTest :feature:ringing:testDebugUnitTest
  ./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest ktlintCheck detekt :app:lintRelease
  ./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease
  ./gradlew :app:installDebug   # réinstallé sur l'appareil de test, données conservées
  ```

- Specs mises à jour dans le même changement : SPEC_ANDROID §3, §10.2 (nouveau paragraphe
  « Resserrement du 2026-10-08 » expliquant le calcul), §15, §20. Plan maître : note datée
  ajoutée après la section « Validation du 2026-09-30 » (sans réécrire les décisions historiques),
  bloc d'interfaces transverses et ligne §20 de l'étape 28 mis à jour.

**Conséquence pour la suite de la campagne.** Les sessions déjà mesurées (A : 120 s, B : constant,
C : 30 s) restent valides comme preuves de la **courbe elle-même** (la formule n'a pas changé,
seules les durées proposées à l'utilisateur ont changé) mais la session C utilisait une durée (30
s) qui n'existe plus dans le produit : à noter dans `QA_MATRIX.md` comme mesure historique, pas
comme ligne du catalogue actuel. Les sessions D, E, F restantes doivent utiliser les nouvelles
durées (2, 5, 10 ou 15 min).

### Session D — Oiseaux, montée 15 min (2026-10-08, après le resserrement) : défaut trouvé en cours de mesure

Armée avec le nouveau catalogue (`niumi_oiseaux`, `volumeRampSeconds = 900`), déclenchée à
**T+153 ms**, volume alarme 7/15 (même réglage que l'essai « trop rapide » du 5 min, pour une
comparaison directe). **La montée à 15 min a été confirmée perçue comme progressive** par
l'utilisateur, contrairement aux 5 min — le resserrement répond au problème initial.

**Défaut trouvé pendant la mesure, non corrigé, ouvert.** Vers **T+240 s**, le moteur a créé un
tout nouveau lecteur `MediaPlayer`/`AudioTrack` (nouveau `sessionId` audio, même `pid` Niumi
avant/après — **pas de mort de processus**) et l'amplitude est repartie à `START_AMPLITUDE`
(0,01). Les valeurs mesurées après ce point correspondent exactement, au calcul, à
`amplitude_at(temps écoulé depuis T+240s, 900000)` et non à `amplitude_at(temps écoulé depuis le
vrai déclenchement, 900000)` : par exemple à l'instant réel T+563,7 s, la valeur mesurée (0,0524)
correspond à un temps écoulé de 323,6 s depuis le redémarrage (T+240s), pas de 563,7 s depuis le
vrai début. La montée a donc **recommencé un cycle complet de 15 min depuis zéro**, en
contradiction avec le contrat documenté de `DefaultAlarmAudioEngine` (« la montée reprend à
l'amplitude attendue pour le temps écoulé, jamais depuis zéro », SPEC_ANDROID §10.2) et avec son
propre test (`DefaultAlarmAudioEngineTest`, cas de reprise après rampe déjà entamée).

**Cause non identifiée.** La capture logcat de cette session était filtrée aux seuls tags
`AudioTrack`/`AudioTrackImpl` (pour éviter la rotation du tampon, correctif de la veille) : elle
exclut les journaux propres de Niumi (`PROCESS_RECREATED`, autres événements techniques) qui
auraient montré ce qui a déclenché ce second démarrage. Lecture du code (`AlarmRingingService`,
`DefaultAlarmAudioEngine`) : la seule façon d'obtenir un second appel à `playerFactory.create()`
sans mort de processus est qu'`audioEngine.stop()` ait été appelé entre-temps (`onDestroy()` du
service, ou l'exécuteur de libération) puis `start()` à nouveau — et que la session relue à ce
second démarrage ait fait traiter `ringingAtEpochMillis` comme absent par `RingingSoundResolver`
(qui démarre alors la rampe à « maintenant », exactement ce qui est observé). Hypothèse la plus
probable, **non vérifiée** : le service Android a été détruit puis recréé par le système sans que
le processus hébergeant Niumi ne meure (possible sous pression mémoire), déclenchant `recover()` ;
quelque chose dans cette relecture aurait fait lire `ringingAtEpochMillis` comme `null`. Aucune
autre sonnerie testée (Piano, Cloche, Énergique, toutes avant 240 s de sonnerie) n'a montré ce
comportement — la durée d'exposition (240 s de sonnerie ininterrompue) est peut-être elle-même une
condition nécessaire, pas la sonnerie « Oiseaux » en tant que telle.

**Décidé avec l'utilisateur : la campagne s'arrête ici.** L'enquête (capture logcat complète,
tags Niumi inclus, pour identifier le déclencheur exact) est reportée à la prochaine reprise,
avant tout correctif — pas de modification de code à l'aveugle sur un mécanisme mal compris.

### Enquête du 2026-10-13 — tentative de reproduction, échec (information négative)

**Constat préalable : Niumi n'écrit rien dans logcat lui-même** (aucun `Log.d`/Timber en
production, grep confirmé sur tout le dépôt) ; le journal technique ne va qu'en base Room. La
piste « lire les tags Niumi dans logcat » du plan d'enquête précédent n'était donc pas
réalisable telle qu'écrite. **Seul appelant de `audioEngine.stop()` dans tout le dépôt :
`AlarmRingingService.onDestroy()`** — confirmé par grep. Pour que le redémarrage observé en
session D se reproduise, `onDestroy()` doit avoir été appelé sans que le processus hébergeant
Niumi ne meure (le pid restait identique avant/après dans les deux mesures), ce qui pointe vers
une destruction du **service** sans destruction du **processus** — rare mais possible sous
pression mémoire — ou vers une réutilisation de pid par l'OS, non vérifiable sans plus de signal.

**Protocole** : nouvelle session Oiseaux/15 min, mêmes conditions. Capture logcat élargie
(`ActivityManager:*` en plus d'`AudioTrack:*`/`AudioTrackImpl:*`, pour voir un éventuel
« Killing » ou « Scheduling restart » du système) ; suivi du `pid` par `pidof` toutes les
quelques secondes ; comptage des créations d'`AudioTrack` du pid Niumi dans le brut capturé.

**Résultat : le défaut ne s'est PAS reproduit sur les 15 minutes complètes.** Une seule création
d'`AudioTrack` du début à la fin (`grep -c` sur le fichier capturé = 1), pid stable (8137) tout du
long, y compris largement après T+240s, l'instant où le redémarrage avait eu lieu la fois
précédente. **Ceci élimine l'hypothèse d'un minuteur fixe dans le code** (rien ne se déclenche
mécaniquement à 240s) : s'il y a un vrai défaut, son déclencheur est soit un évènement externe qui
ne s'est pas reproduit cette fois (pression mémoire, action système), soit rare/non
déterministe.

**Session terminée par désinstallation**, faute du bon boîtier à portée de main pour scanner
(même situation que lors de la session D elle-même). `adb uninstall com.niumi.app` : confirmé
propre (plus d'alarme en attente, paquet absent).

**Conclusion de cette enquête.** Un seul échantillon positif (session D, étape 28) contre un seul
échantillon négatif (cette reprise) ne permet ni de confirmer un vrai défaut récurrent ni de
l'écarter. Ce n'est **pas classé comme résolu** : c'est noté comme rare ou dépendant de
conditions non reproduites (mémoire disponible, état du téléphone, autre application active). La
campagne du Lot 7 peut reprendre (sessions D à refaire, E, F) en gardant cette observation ouverte
plutôt qu'en bloquant indéfiniment sur un évènement qui ne se reproduit pas à la demande — mais
sans la classer comme acquise non plus : à surveiller sur les prochaines sessions longues, et à
consigner immédiatement si elle revient, avec cette fois une capture qui inclut
`ActivityManager` dès le départ (déjà fait pour cette tentative, à garder pour les suivantes).

## Reprise du 2026-10-15 — contrôles annexes et session D mesurée complète

Remise en place complète faite par l'utilisateur (installation, diagnostic vert, bon boîtier à
portée de main). État vérifié par adb avant tout essai : service d'accessibilité actif, exemption
batterie, `zen_mode = 0`, volume alarme **4/15**.

**Pré-écoute avec volume média à zéro (contrôle annexe).** HyperOS refuse le réglage du volume
média par adb (`cmd media_session volume` et `media volume` restent sans effet) : réglé à la main
par l'utilisateur. Média 0/15, alarme 4/15 : la pré-écoute de l'écran 14 est **audible**, et
`dumpsys audio` montre un nouveau lecteur `USAGE_ALARM` / `STREAM_ALARM(4)`. Mesuré : la
pré-écoute suit le volume des alarmes, pas celui des médias. Candidat de `LIMITES.md` confirmé
pour sa moitié « pré-écoute » ; la moitié « alarme » est couverte par les sessions A à D (toutes
jouées sur `STREAM_ALARM`).

**TalkBack sur l'écran 14 : non testé**, passé par choix de l'utilisateur (contrôle jugé déroutant
à mener seul) — reste ouvert.

### Session D — Oiseaux, montée 15 min, mesurée complète (2026-10-15)

Capture logcat lancée **avant** l'armement, `ActivityManager` inclus (protocole de l'enquête du
13/10). Déclenchement à 12:40:00,186 (`ringingAtEpochMillis = 1792060800186`, **T+186 ms**),
pid 7237, inchangé depuis l'installation et jusqu'à la fin.

| Mesure | Résultat |
| --- | --- |
| lecteurs Niumi créés | **1** sur toute la sonnerie (aucun redémarrage de la montée) |
| points de montée relevés | 3723 |
| amplitude à p = 0 / 10 / 25 / 50 / 75 / 90 / 100 % | 0,010033 / 0,015859 / 0,031603 / 0,099945 / 0,316292 / 0,631067 / 1,000000 |
| théorie `10^(−2·(1−p))` | 0,010000 / 0,015849 / 0,031623 / 0,100000 / 0,316228 / 0,630957 / 1,000000 |
| écart maximal | ≈ 0,3 % (au départ, 0,010033 pour 0,010000) |
| série monotone | oui |
| plein volume atteint à | **T+900,12 s** (15 min attendues) |
| boucle (`MediaPlayer.isLooping`) | 96 cycles, période **10,072 à 10,431 s** pour un fichier de 10,00 s |

**Écoute de l'utilisateur** : « Montée perçue comme progressive », mais **« on entend la coupure de
la boucle »**. La mesure l'explique : chaque tour arrive 70 à 430 ms après la fin du fichier, le
journal `AudioTrack` montrant un arrêt puis une relance de la piste à chaque reprise. Sur « Piano »
(étape 26, 15,07–15,23 s) et « Cloche » (session B), ce blanc tombe dans la décroissance de la
dernière note ; sur « Oiseaux », sans décroissance finale, le chant est coupé net — exactement le
risque que l'étape 26 avait signalé pour « Énergique » et « Oiseaux ».

**Défaut de redémarrage de la montée du 2026-10-08 : non reproduit**, deuxième session longue
consécutive (13/10 et 15/10) sans second lecteur ni changement de pid. Toujours non classé comme
résolu (voir l'enquête ci-dessus).

**Ligne §20 « montée progressive 2 min, puis 15 min »** : 15 min mesurée et conforme ; 2 min
mesurée en session A. **Ligne « chacune des quatre sonneries »** : échec sur « Oiseaux » avec le
lecteur `MediaPlayer` — d'où la décision qui suit.

### Décision du 2026-10-15 : lecteur de réveil `AudioTrack` sans blanc

Trois réponses présentées à l'utilisateur, en langage simple : (1) fondus en fin et début de
fichier — masque sans corriger, blanc jusqu'à 430 ms ; (2) deux `MediaPlayer` en relais
(`setNextMediaPlayer`) — réduit le blanc sans garantie de l'échantillon près ; (3) `AudioTrack` en
mode statique avec points de boucle — le fichier est rejoué en continu sans arrêter la piste.
**Choix de l'utilisateur : option 3.** SPEC_ANDROID §10.2 l'autorisait déjà (« `MediaPlayer` ou
`AudioTrack` derrière `AlarmAudioEngine` ») ; aucune dépendance ajoutée.

**Implémentation (TDD, tests rouges d'abord)** — `:core:system` :
- `PcmWav` (pur Kotlin) : lit l'en-tête RIFF, blocs dans n'importe quel ordre, n'accepte que PCM
  16 bits mono 44 100 Hz, sinon `IllegalArgumentException` ;
- `AudioTrackAlarmPlayerFactory` : `AudioTrack` `MODE_STATIC`, tampon = taille des échantillons,
  écriture complète, `setLoopPoints(0, frameCount, -1)`, amplitude initiale posée avant `play()` ;
- `FallbackAlarmPlayerFactory` : tout refus de l'`AudioTrack` repasse sur `MediaPlayer` avec la
  même demande — un réveil muet n'est jamais acceptable ;
- `AlarmPlayer.gaplessLoop`, `AlarmAudioEngine.isGaplessLoop` ; `AudioModule` lie la fabrique de
  repli pour le réveil, la pré-écoute reste sur `MediaPlayer` (pas de boucle, besoin du signal de
  fin de fichier) ;
- journal : `TechnicalEventType.AUDIO_LOOP_FALLBACK`, `RingingStartJournal.eventsFor(…,
  loopFallback)`, `AlarmRingingService` le calcule après un démarrage réussi.

Tests ajoutés : `PcmWavTest` (9 cas), `FallbackAlarmPlayerFactoryTest` (3),
`DefaultAlarmAudioEngineTest.isGaplessLoopReflectsTheActivePlayer`, `RingingStartJournalTest`
(3), `TechnicalEventTypeTest` (liste), `NiumiRingtoneWavTest.everyCatalogRingtoneIsReadableByThePlayerWavReader`
(les quatre vrais fichiers passent `PcmWav`). Un `LongParameterList` detekt sur l'aide de test
de `PcmWavTest` a été résolu en regroupant les champs du format dans une classe, sans suppression.

Specs mises à jour dans le même changement : SPEC_ANDROID §10.2 (*Boucle* révisé, *Repli*), §17
(`AUDIO_LOOP_FALLBACK`), §19.1, §20 (critère objectif : aucun arrêt/redémarrage de piste sur trois
cycles) ; plan maître (note datée sous la phase J, point de vigilance 16, ligne de l'étape 28).

## Sessions E et F — lecteur `AudioTrack` sur appareil (2026-10-15, après-midi)

**Tests instrumentés d'abord** (`connectedDebugAndroidTest`, désinstalle l'application) : 165
tests, **2 échecs à la première passe**, tous deux corrigés le jour même, puis modules rejoués verts :

1. `DataStoreAlarmSoundPreferencesInstrumentedTest.writeOverwritesThePreviousSettings` écrivait
   encore `volumeRampSeconds = 30`, retirée de la liste le 08/10 : la lecture la ramène au défaut
   120 — comportement voulu (`AlarmSoundSettings.sanitized`). Test oublié au resserrement ;
   données passées à 600 et 900. Aucun autre test ne relit une durée retirée à travers la
   préférence (les mappings Room et Direct Boot la transmettent telle quelle, à raison).
2. `AndroidAccessibilityServiceStatusInstrumentedTest.aComponentActuallyPresentInSettingsIsReportedAsEnabled`
   prend la première entrée de `enabled_accessibility_services` comme composant attendu. Niumi venant
   d'être désinstallé, c'était celle d'une autre application, écrite en forme courte
   (`com.brickllc.brick/.core…`) — et `EnabledAccessibilityServicesParser` ne développait la forme
   courte que du côté de la liste. **Sans effet en production** (le composant attendu de Niumi est
   toujours qualifié), mais le parseur est rendu symétrique, avec deux tests JVM vus rouges puis
   verts (`aRelativeExpectedComponentMatchesItsQualifiedEntry`,
   `aRelativeExpectedComponentMatchesTheSameRelativeEntry`). Probablement aussi la cause de
   l'échec dit « transitoire » du 06/10 (ci-dessus).

`AlarmChainInstrumentedTest` (`:app`, moteur réel) vert dès la première passe : l'`AudioTrack`
statique est créé et joue sur l'appareil.

### Session E — modification en `ARMED`, redémarrage sans déverrouillage, boucle « Oiseaux »

Remise en place complète par l'utilisateur. « Énergique » / 5 min choisi, session armée pour
13:35:00, puis modifiée depuis l'écran 7 en « Oiseaux » / 2 min. Projection Direct Boot relue avant
le redémarrage : `ARMED`, `niumi_oiseaux`, `volumeRampSeconds = 120`. Redémarrage par `adb reboot`
(démarrage complet en ~24 s) ; utilisateur **`RUNNING_LOCKED`** vérifié avant et pendant
l'essai — le code PIN de la SIM, saisi pour rétablir le partage de connexion, ne déverrouille pas
le stockage. Alarme `RTC_WAKEUP` de 13:35:00 présente avant tout déverrouillage. Le processus,
au repos, est tué par HyperOS à 13:33:49 (`clean_up_mem`, `adj 701`) ; l'alarme le relance à
l'heure.

**Constat d'outillage** : `run-as … cat /data/user_de/…` échoue tant que l'utilisateur n'a pas
déverrouillé (`run-as` exige le stockage chiffré par les identifiants). La surveillance a basculé
sur le journal `AudioTrack` ; `ringingAtEpochMillis` a été relu après le déverrouillage.

| Mesure | Résultat |
| --- | --- |
| déclenchement | `ringingAtEpochMillis` = 13:35:00,495 (**T+495 ms**), téléphone verrouillé |
| lecteur | `AudioTrack` **statique** : `transferType 4`, `sharedBuffer` 882 000 octets (10 s mono 16 bits), `USAGE_ALARM` |
| boucle | **1 piste, 1 `start`, aucun `stop` ni `pause`** sur 211 s (~21 reprises) |
| montée 2 min | 473 points, strictement croissante, départ 0,0101, plein volume à **120,17 s** après `ringingAtEpochMillis` |
| journal technique | un seul `RINGING_STARTED`, **aucun `AUDIO_LOOP_FALLBACK`** |
| oreille | « **aucune coupure** » ; montée « progressive » |

Couvre « redémarrage sans déverrouillage, sonnerie autre que Piano », « sonnerie modifiée pendant
`ARMED`, puis redémarrage » et, pour « Oiseaux », « chacune des quatre sonneries ».

**Observation, non traitée** : deux `NFC_DISABLED` sans `sessionId`, l'un à T+1,2 s (verrouillé),
l'autre au déverrouillage. Ressemble au démarrage de la pile NFC (étape 25), mais vient d'un
producteur qui ne rattache pas de session — à examiner séparément.

### Session F — `am crash` pendant une montée

« Oiseaux » / 5 min, réveil à 13:45:00 ; `ringingAtEpochMillis` = 13:45:00,167. `am crash
com.niumi.app` envoyé à T+60,32 s.

| Mesure | Résultat |
| --- | --- |
| mort | T+60,67 s (`has died: fg TOP` — écran de réveil au premier plan) |
| relance | processus à T+60,74 s (pour l'activité au premier plan), service à T+61,62 s, nouvelle piste `AudioTrack` statique à **T+62,30 s** |
| reprise | **0,02592 mesuré pour 0,02595 attendu** au temps écoulé (dernier point avant le crash : 0,02527) — jamais depuis zéro |
| fin de montée | 940 points, strictement croissante, ≤ 0,13 % de la courbe, plein volume à **300,20 s** |
| boucle après reprise | 1 piste, aucun `stop` jusqu'au scan |
| oreille | « court silence, même niveau » |
| journal | `RINGING_STARTED` à T+0,5 s puis à T+62,4 s ; **pas de `PROCESS_RECREATED`** : le processus a été relancé par l'activité, et le service a reçu une commande ordinaire, pas une reconstruction `START_STICKY` |
| accessibilité | service Niumi en « Crashed services », non relié (limite connue de l'étape 25) ; réactivé à la main |

Couvre « mort du processus pendant la montée ». La reprise par la reconstruction `START_STICKY`
(écran éteint, aucune activité) n'a pas été provoquée ici ; elle est mesurée pour le son à
l'étape 20 (watchdog) et la rampe s'y appuie sur le même `ringingAtEpochMillis`.

### Limites écrites dans l'aide

Section « Sonnerie » ajoutée à `LIMITES.md` et `HelpTexts` (rouge puis vert par `HelpTextsTest`),
deux limites, chacune mesurée : la pré-écoute et l'alarme suivent le volume des alarmes et non
celui des médias (contrôle annexe du matin, sessions A à F sur `STREAM_ALARM`) ; après une mort du
processus relancé, une coupure d'une ou deux secondes puis la reprise au même niveau (session F).

## Défaut trouvé après la campagne — mort du processus invisible au diagnostic (15/10, corrigé le 16/10)

Après la session F, l'utilisateur a remarqué que Niumi n'affichait **aucun avertissement** alors que
son service d'accessibilité n'était plus relié. État réel relevé par adb :

```text
enabled_accessibility_services = com.niumi.app/…NiumiBlockingAccessibilityService:com.brickllc.brick/…BrickAccessibilityService
accessibility_enabled          = 1
dumpsys accessibility : Bound services {Brick App Blocker} ; Crashed services {com.niumi.app/…}
```

**Cause.** `AndroidAccessibilityServiceStatus` déduisait « relié » de l'interrupteur global
`Settings.Secure.ACCESSIBILITY_ENABLED`, qu'Android met à 1 dès qu'**un** service d'accessibilité
tourne. « Brick App Blocker » le tenait à 1 pendant que Niumi, mort puis relancé, n'était plus
relié. Les mesures de l'étape 25 (25/09, `BLOCKING_PERMISSION_REVOKED` bien consigné après une mort
du processus) n'avaient réussi que parce que Niumi était alors le seul service d'accessibilité de
l'appareil. Conséquence : avec une autre application d'accessibilité — profil réaliste pour un
utilisateur de Niumi —, une mort du processus coupait le blocage en silence, et l'aide (« Niumi te
prévient dès qu'il tourne à nouveau ») mentait.

**Correction (plan approuvé par l'utilisateur, TDD).** Le service annonce lui-même sa liaison dans
le processus : `AccessibilityServiceBinding` / `InMemoryAccessibilityServiceBinding` (`:core:system`,
lié par `BlockingModule`), marqué relié à `onServiceConnected`, délié à `onUnbind` et à un
`onDestroy` ajouté. `AccessibilityServiceState.of(listed, bound)` remplace `of(globallyEnabled,
listed)` ; `ACCESSIBILITY_ENABLED` n'est plus lu. Rien d'autre ne change : `PENDING` garde son sens
(« inscrit mais non relié »), la fenêtre `UnlockSettling` de 30 s et le `ReadinessRecheck` couvrent à
l'identique la liaison après déverrouillage et, pour un processus relancé après une mort, le verdict
`FAILED` à la fin de sa propre fenêtre. Tests : `InMemoryAccessibilityServiceBindingTest` (nouveau),
`AccessibilityServiceStateTest` (table réécrite, cas du défaut), instrumenté
`AndroidAccessibilityServiceStatusInstrumentedTest` (lecture réelle de la liste, liaison simulée :
inconnu → `DISABLED`, listé non relié → `PENDING`, listé relié → `ENABLED`). Specs : SPEC_ANDROID
§7.1, §12.2, §13 (tableau), §13.1 (nouvel alinéa daté), §15, §19.2 ; `RELEASE_REPORT.md` écart 12 ;
plan maître, point de vigilance 17.

`AccessibilityManager.getEnabledAccessibilityServiceList` a été envisagé puis écarté : la
documentation officielle (relue par Context7) n'en fixe pas la sémantique « services liés seulement ».

**Validé sur appareil le 2026-10-16**, Xiaomi 25080RABDG / Android 16, « Brick App Blocker » actif
pendant tout le protocole :

| Essai | Résultat |
| --- | --- |
| tests instrumentés | `:feature:session` 3/3 (les trois cas de `AndroidAccessibilityServiceStatusInstrumentedTest` exécutés, aucun ignoré : la liste contenait Brick), `:app` 12/12 |
| écran de consentement, retour des réglages après activation | « Service actif » immédiatement (constat de l'utilisateur) ; Niumi et Brick reliés dans `dumpsys accessibility` |
| session `ARMED`, blocage immédiat, `am crash` à 18:08:04 | Niumi dans « Crashed services », seul Brick relié, **`accessibility_enabled` toujours à 1** — l'ancien critère aurait lu « actif » |
| réouverture de Niumi 22 s après | `SESSION_READINESS_DEGRADED`, `ACCESSIBILITY_DISABLED`, incident `BLOCKING_PERMISSION_REVOKED` `CRITICAL` **30,7 s après le démarrage du processus** (fenêtre `UnlockSettling`), avertissement à l'écran, notification « Vérifie ton réveil Niumi — Le service d'accessibilité de Niumi est désactivé. Tes applications ne sont plus bloquées. » |
| réactivation dans les réglages | avertissement retiré, « Rétabli depuis » (constat de l'utilisateur), notification retirée, Niumi relié |
| redémarrage, session `ARMED` | `ALARM_RESCHEDULED` 6,7 s **avant** le déverrouillage ; Niumi relié **5,9 s après** le déverrouillage ; sur les 56 s suivantes : aucun `ACCESSIBILITY_DISABLED`, aucun nouvel incident, aucune notification d'avertissement, session toujours `ARMED` |

Réserve sur le dernier essai : l'incident est enregistré une fois par code et par session, et celui de
l'essai 2 existait déjà ; c'est l'absence d'`ACCESSIBILITY_DISABLED` et de notification qui prouve
que le contrôle n'a pas échoué pendant la liaison.

**Limite confirmée, hors de ce correctif** : application fermée après une mort du processus, rien ne
relance Niumi (processus absent jusqu'à la réouverture). L'avertissement n'arrive qu'au prochain
démarrage du processus — ouverture, réveil, redémarrage, alarme de début différé. C'est ce que dit
l'aide (« dès qu'il tourne à nouveau »). Le combler demanderait un réveil périodique de Niumi :
décision produit non prise.

## Ce qui reste

- **Réécouter « Piano », « Cloche » et « Énergique » avec le lecteur `AudioTrack`** — reporté par
  l'utilisateur le 15/10. Trois sessions courtes, volume constant, trente secondes d'écoute,
  journal `AudioTrack` sans arrêt de piste. Critère §21 n° 30 partiel d'ici là.
- TalkBack sur l'écran 14 : non testé.
- Le redémarrage de la montée du 08/10 : non reproduit sur trois sessions longues ou à crash
  (13/10, 15/10 D, 15/10 F) ; toujours ouvert, à consigner s'il revient.
- Session sur APK de publication (A1/A2 de `RESTE_A_FAIRE.md`) : la clé n'existe pas.
- Les deux `NFC_DISABLED` sans session au redémarrage.
- Hors périmètre : vibration progressive (impulsions qui se resserrent) proposée par l'utilisateur,
  à spécifier séparément (décision 8 du Lot 7 : vibration simple on/off).

## Fichiers modifiés

- `tools/validate_alarm.sh` (étendu, corrigé une fois en cours de campagne — voir ci-dessus).
- Resserrement des durées (2026-10-08) : voir la section dédiée.
- Lecteur sans blanc (2026-10-15) : `audio/AlarmPlayer.kt`, `AlarmAudioEngine.kt`,
  `DefaultAlarmAudioEngine.kt`, `di/AudioModule.kt` ; créés `audio/PcmWav.kt`,
  `audio/AudioTrackAlarmPlayerFactory.kt`, `audio/FallbackAlarmPlayerFactory.kt` (`:core:system`) ;
  `logging/TechnicalEventType.kt` (`:core:database`) ; `ringing/RingingStartJournal.kt` ;
  `AlarmRingingService.kt` (`:feature:ringing`) ; tests associés ; SPEC_ANDROID et plan maître.
- Campagne du 15/10 après-midi : `blocking/EnabledAccessibilityServicesParser.kt` et son test,
  `DataStoreAlarmSoundPreferencesInstrumentedTest.kt`, `LIMITES.md`, `HelpTexts.kt`,
  `QA_MATRIX.md` (sous-section Lot 7), `RELEASE_REPORT.md` (critères 30 à 35, renumérotation),
  `RESTE_A_FAIRE.md` (B1, B2, section D).
- Liaison du service d'accessibilité (16/10) : créé `blocking/AccessibilityServiceBinding.kt`
  (`:core:system`) ; modifiés `blocking/AccessibilityServiceStatus.kt`,
  `blocking/AndroidAccessibilityServiceStatus.kt`, `di/BlockingModule.kt`,
  `readiness/AndroidDeviceReadinessChecker.kt` (commentaire), `di/SessionBlockingModule.kt` et
  `blocking/NiumiBlockingAccessibilityService.kt` (`:feature:session`), tests JVM et instrumenté,
  KDoc de `TestBlockingBindingsModule` (`:app`) ; SPEC_ANDROID, `RELEASE_REPORT.md`, plan maître.

## Incertitudes et limites

- Le parseur logcat est maintenant confirmé par une mesure réelle complète (488 points de montée,
  81 évènements de boucle, session A du 2026-10-07) en plus de l'extrait authentique de la veille.
  Reste à confirmer sur les trois autres sonneries (Cloche, Énergique, Oiseaux), dont les
  comportements de boucle pourraient différer (étape 26 : « Énergique et Oiseaux, sans décroissance
  finale, y sont les plus exposés » à un blanc audible).
- Le chemin `run-as … cat /data/user_de/0/com.niumi.app/files/niumi_session.json` est validé deux
  fois sur ce Xiaomi (lecture réussie de `ringtoneKey`/`volumeRampSeconds`/`ringingAtEpochMillis`)
  mais reste une supposition sur un autre appareil ou une autre version d'Android.
- La mesure de la session A s'est faite **manuellement** (hors du script, qui a raté sa fenêtre de
  16 s) : le script lui-même n'a donc pas été éprouvé de bout en bout en conditions réelles pour sa
  partie « relevés dumpsys programmés » et son enchaînement complet. Ses fonctions pures
  (extraction logcat, durée de fichier, amplitude théorique) le sont, via le harnais et via la
  réutilisation manuelle de ces mêmes fonctions sourcées. À surveiller à la prochaine session B :
  si le script réussit cette fois de bout en bout, le dire ; sinon, consolider le repli manuel en
  option du script plutôt que de le refaire à la main chaque fois.
- Rien n'indique un défaut du moteur audio ou de l'interface du Lot 7 : les sessions interrompues
  des deux jours se sont terminées par un scan normal, comme prévu par le produit, et la session A
  aboutie confirme le moteur (montée, résolveur de sonnerie, décodage de la préférence par défaut).
  Les incidents de cette séance sont des incidents d'outillage et de coordination, pas des
  régressions produit.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest     # BUILD SUCCESSFUL
./gradlew ktlintCheck detekt :app:lintRelease                                 # BUILD SUCCESSFUL
./gradlew :app:assembleRelease :app:bundleRelease                             # BUILD SUCCESSFUL
adb devices                                                                   # 1 appareil (lapis)
./gradlew connectedDebugAndroidTest                                          # 1er essai : 1 échec (AndroidAccessibilityServiceStatusInstrumentedTest, état transitoire)
./gradlew :feature:session:connectedDebugAndroidTest                         # relance isolée : vert, 1 ignoré
./gradlew connectedDebugAndroidTest                                          # relance complète : 166 tests, 0 échec, 1 ignoré
./gradlew :app:installDebug
bash -n tools/validate_alarm.sh                                              # syntaxe OK, à chaque correctif
tools/validate_alarm.sh 300                                                  # 2026-10-06, tentative 1 : dumpsys complet, logcat 0 valeur (défaut 1 trouvé)
tools/validate_alarm.sh 300                                                  # 2026-10-06, tentatives 2 et 3 : fenêtre manquée (coordination)
tools/validate_alarm.sh 300                                                  # 2026-10-07, tentative 1 (via FIFO) : fenêtre manquée de 16 s
# 2026-10-07 : mesure manuelle de secours (ringingAtEpochMillis lu en base, capture logcat
# continue filtrée, fonctions de tools/validate_alarm.sh sourcées et réutilisées directement) :
# 488 points de montée, 81 évènements de boucle, défaut 2 trouvé (ancre de timestamp) et corrigé
# 2026-10-15 : session D, 3723 points de montée, 96 cycles de boucle (méthode manuelle)
./gradlew testDebugUnitTest :shared:core:jvmTest :app:testReleaseUnitTest :app:assembleDebug \
  ktlintCheck detekt :app:lintDebug                                           # 2026-10-15, après le lecteur AudioTrack : BUILD SUCCESSFUL
./gradlew connectedDebugAndroidTest                                          # 2026-10-15 : 165 tests, 2 échecs (voir sessions E et F)
./gradlew :core:system:connectedDebugAndroidTest :feature:session:connectedDebugAndroidTest   # après correctifs : 27 + 2, verts
./gradlew :app:installDebug
# sessions E et F : capture `adb logcat -v epoch -s ActivityManager AudioTrack AudioTrackImpl AudioFlinger`,
# `adb reboot`, `adb shell am crash com.niumi.app`, base Room copiée par `run-as … cat` et lue en sqlite3
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest ktlintCheck detekt \
  :app:lintRelease :app:assembleRelease :app:bundleRelease                    # 2026-10-15, final : BUILD SUCCESSFUL, 1 296 exécutions JVM
./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :app:testDebugUnitTest \
  :app:assembleDebug ktlintCheck detekt :app:lintDebug                        # 2026-10-16, liaison du service : BUILD SUCCESSFUL
```

**Rien n'est committé.**
