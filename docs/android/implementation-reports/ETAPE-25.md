# Étape 25 — Résilience mesurée, matrice physique, aide et limites, rapport de release

Dates : 2026-09-17 (partie outillage) puis campagnes des 24, 25, 27 et 28 septembre 2026.
**Validé sur appareil** : Xiaomi 25080RABDG (`lapis`), Android 16, HyperOS OS3.0.302.0.WPPEUXM,
build `BP2A.250605.031.A3`, permission OEM de démarrage automatique **refusée**. 1 137 tests JVM et
151 tests instrumentés verts après la dernière modification (28/09) ; les neuf lignes « blocage différé » de §20 renseignées.

L'étape devait prouver sur appareil ce que les étapes 22 à 24 avaient construit. Elle l'a fait, et
elle a surtout trouvé **six défauts invisibles en test**, tous corrigés, plus **une limite majeure
de HyperOS** qui touchait le MVP entier — le retrait des récents coupait le blocage — traitée le
28/09 par deux parades mesurées (voir « Retrait des récents »).

## Ce qui est livré

- **`ProcessDeathInstrumentedTest`** : deux cas Lot 6, début dépassé d'une minute (aucun incident)
  et de plus de quinze (`MISSED_BLOCKING_START_WINDOW` en `WARNING`), sur une vraie base et une vraie
  projection. **`TestBlockingBindingsModule`** (`@TestInstallIn`) neutralise le seul contrôle que
  l'instrumentation rend structurellement faux (l'accessibilité, §19.2) sans lier le service réel,
  qui renverrait sinon l'instrumentation à l'accueil.
- **`tools/validate_blocking.sh --deferred`** : essai « application déjà ouverte à l'heure de
  début », retard mesuré par une boucle qui tourne **sur l'appareil**, contrôle §9.1 sur une vraie
  session.
- **`tools/validate_blocking_start.sh`** (nouveau) : essais `reboot` (epochs comparés avant et après
  un redémarrage sans déverrouillage) et `doze` (délivrance en Doze profond, adb par Wi-Fi), `trap`
  de nettoyage posé avant tout forçage, verdict explicite si la dérogation §9.1 tombe.
- **Retrait des récents (A7)** : `RecentsCard` (`:core:system.recents`) retire la carte de
  l'activité principale des récents tant que `isSessionInProgress()` est vrai, collecté par
  `MainActivity` ; `RecentsLockStatus` lit le verrou HyperOS (`Settings.System` `locked_apps`,
  `LockedAppsParser`) pour un quinzième contrôle de diagnostic, `RECENTS_LOCK`, avec son recours
  `LockInRecents` et ses textes. Tests : `RecentsTasksTest`, `LockedAppsParserTest`, trois cas dans
  `AndroidDeviceReadinessCheckerTest`.
- Les neuf lignes de §20, les quatre critères §21 du Lot 6, trois limites dans l'aide, et les
  documents de release à jour (voir « Documents »).

## Décisions prises pendant l'étape

| Date | Décision | Par |
| --- | --- | --- |
| 17/09 | Nouveau script `validate_blocking_start.sh` plutôt qu'étendre `validate_alarm.sh`, dont le code de sortie est toujours 0 ; pas de bibliothèque partagée | utilisateur |
| 17/09 | `RELEASE_REPORT.md` renuméroté de 1 à 31 selon l'ordre de §21 | utilisateur |
| 17/09 | Tout le code d'abord, campagne ensuite ; pas de clé de signature, l'essai release reste « non testé » | utilisateur |
| 24/09 | Faux incident NFC : ne pas juger le NFC avant déverrouillage ; délai fixé à 30 s depuis le démarrage — **révisé le 27/09** | utilisateur |
| 24/09 | `BLOCKING_STARTED` : un effet satisfait compte comme réussi (SPEC_CORE_KMP §6) | plan validé |
| 24/09 | `navigation-compose` 2.10.1 → 2.10.2 (lint `GradleDependency` en erreur) ; seule différence : Lifecycle 2.10.0 demandé, 2.11.0 déjà résolu | utilisateur |
| 25/09 | Écran 7 : rejouer le diagnostic, un `CRITICAL` rétabli passe en « Incidents » avec « Rétabli depuis. », sans bouton | utilisateur |
| 27/09 | NFC rejugé au premier plan en rejouant `SessionRuntimeReconciler` après la surveillance §13.1 | utilisateur |
| 27/09 | Accessibilité après déverrouillage : trois états, fenêtre de 30 s, re-contrôle à sa fin | utilisateur |
| 27/09 | Fenêtre NFC ramenée au **déverrouillage**, partagée avec l'accessibilité (mesure de 19:38) | utilisateur |
| 28/09 | Récents : retirer la carte de Niumi pendant la session plutôt que l'exclure en permanence | utilisateur |
| 28/09 | Pas de verrou à poser à chaque session : le verrou HyperOS, durable, est exigé une fois par le diagnostic, **bloquant** comme l'accessibilité, sans objet hors HyperOS | utilisateur |
| 28/09 | Exemption d'énergie **détectée** (liste blanche AOSP) au lieu d'être confirmée par l'utilisateur ; bouton « J'ai levé les restrictions » et préférence retirés | utilisateur |

## Défauts trouvés sur appareil et corrigés

Tous reproduits d'abord par un test rouge pour la raison observée, puis corrigés.

1. **`BLOCKING_STARTED` jamais journalisé** (3 sessions sur 3, 24/09). La publication du snapshot,
   premier effet de la décision, fait rafraîchir la projection par le service ; `APPLY_BLOCKING`
   s'exécutait ensuite en `AlreadySatisfied`, que `ApplyBlockingExecutor` ne journalisait pas.
   L'étape 24 avait le même défaut sans le voir : son « +101 ms » venait de la base, pas du journal.
   `ApplyBlockingExecutorTest` créé ; validé sur appareil le 25/09 à 16:00 (`SATISFIED` et
   `BLOCKING_STARTED`).
2. **Faux `NFC_DISABLED` `CRITICAL` après tout redémarrage** (24/09), en double, session
   `DEGRADED` jusqu'à sa fin, écran 7 réclamant un NFC déjà allumé. Touche le MVP entier. Première
   garde (30 s depuis le démarrage) **invalidée le 27/09 à 19:38** : le service NFC ne démarre
   qu'après le déverrouillage ; seconde garde, fenêtre de 30 s depuis le déverrouillage — validée
   par deux redémarrages le 27/09 et un le 28/09.
3. **Avertissement jamais retiré après une mort de processus** (25/09) : le retrait dépendait d'une
   mémoire propre au processus qui l'avait publié. Retrait désormais inconditionnel et idempotent.
   Validé le 25/09 à 15:49.
4. **L'écran 7 demandait de vérifier un réglage déjà rétabli** (25/09). Il rejoue maintenant le
   diagnostic ; validé le 25/09 et le 27/09.
5. **NFC jamais rejugé au premier plan pendant `ARMED`** (27/09) : un NFC coupé le soir n'était
   signalé qu'au réveil. Validé le 27/09 (encadré puis « Rétabli depuis. »).
6. **Faux `BLOCKING_PERMISSION_REVOKED` 0,3 s après le déverrouillage** (27/09) : Android relie le
   service après le déverrouillage. Le test de reproduction a montré pire : la garde de permission
   coupait la passe **avant la politique de retard du réveil**. Validé par trois redémarrages.

S'y ajoutent trois défauts **de test**, sans effet sur le produit. Le premier est de l'étape 25 elle-même :
`BlockingStartReceiverInstrumentedTest` attendait un `BLOCKING_STARTED` quelconque dans le journal
partagé, et les deux nouveaux cas de `ProcessDeathInstrumentedTest` en écrivent un ; selon l'ordre,
l'attente finissait avant l'effet (échec en 96 ms le 28/09) — filtrée désormais par session. Les deux
autres : la garde de
`BlockingStartAlarmVisibilityTest` ne sautait jamais son contrôle positif (elle testait la présence
de `time:`, pas l'ordre des instants), révélé par un rappel d'agenda à 19:40 le 27/09 ; et les deux
scripts, avant leur premier usage, lisaient une date formatée que `date -D` convertit sans l'heure
d'été (décalage d'une heure pile) — ils lisent désormais l'epoch brut de l'en-tête `Alarm{…}`.

## Protocole — lignes §20 « blocage différé »

| # | Essai | Résultat |
| --- | --- | --- |
| 1 | Écran éteint 30 min à l'heure de début | **Vert** (28/09) : écran éteint depuis 44 min, alarme délivrée à 11:30:00.007, blocage **+245 ms**, `BLOCKING_STARTED` +455 ms. Téléphone branché, donc pas en Doze profond (`deep=ACTIVE`) : le Doze est l'essai 9 |
| 2 | Application déjà ouverte | **Vert, mesuré** (24/09) : aucun blocage avant l'heure, retour à l'accueil **+222 ms**, overlay, six essais de blocage ordinaires verts ensuite |
| 3 | Processus tué avant l'heure | **Limite établie** (25/09) : alarme conservée, Android relance Niumi à l'heure, mais le service d'accessibilité reste délié ; incident et avertissement, blocage appliqué en retard après réactivation (+24 min, `MISSED_BLOCKING_START_WINDOW`). Non-régression le 27/09 : avertissement 31 s après l'ouverture |
| 4 | Redémarrage avant l'heure, sans déverrouillage | **Vert** (24/09) : reprogrammée 1 s après le démarrage, même instant, délivrance +133 ms. **27/09** : téléphone resté verrouillé à l'heure de début, Niumi mort entre-temps, blocage appliqué pendant la phase verrouillée (+465 ms) |
| 5 | Redémarrage après l'heure, avant le réveil | **Vert** (28/09) : éteint à 9:55, rallumé à 10:20 ; blocage appliqué pendant la phase verrouillée (+25 min 55 s), `MISSED_BLOCKING_START_WINDOW` `WARNING`, santé inchangée, réveil de 10:45 sonné à +99 ms puis scan |
| 6 | Changement manuel d'heure | **Vert** (27/09) : −5 min puis retour automatique, deux reprogrammations sans condition au même epoch, un seul `TIME_CHANGED` ; début à l'heure (+162 ms) |
| 7 | Scan avant l'heure | **Vert** (17/09, `ETAPE-24.md`) : `CANCELLED`, plus aucune alarme Niumi |
| 8 | Accessibilité coupée avant l'heure | **Vert** (25/09) : incident, session conservée ; réactivée avant l'heure → blocage à +44 ms, avertissement retiré par le processus suivant |
| 9 | Doze forcé à l'heure de début | **Vert** (24/09) : `deep=IDLE`, débranché, adb Wi-Fi ; réception +77 ms, blocage +91 ms. **La seconde dérogation de §9.1 tient.** Réserve : point d'accès du téléphone actif |

**Mesures de référence après déverrouillage** (27/09, deux redémarrages, déverrouillage aussi
rapide que possible à 40,4 et 44,4 s de démarrage) : service d'accessibilité relié **2,3 à 3,6 s**
après ; service NFC absent jusqu'à 44 s, puis `turningon`, `on`, de nouveau `turningon`, stable
**13,2 à 14,2 s** après le déverrouillage. L'écran de verrouillage n'apparaît qu'après ~27 s : un
déverrouillage avant 30 s de démarrage est impossible sur cet appareil.

## Retrait des récents sur HyperOS : limite majeure, puis parades

**La limite.** Le 27/09 à 19:52:10, un geste bref (0,1 s) dans le panneau des applications
récentes — pas un retrait voulu — a été pris par HyperOS pour `RECENT_SWIPE_REMOVE_APP`. Reproduit
**délibérément** le 28/09 à 11:49:55 : HyperOS tue Niumi (`SwipeUpClean`, alors en `adj 200` grâce
au service lié), `accessibility_enabled` retombe à 0, le service est délié 2 s après et ne revient
pas ; 2 min 20 s plus tard, Acrobat s'ouvre librement. **Le blocage est coupé**, l'alarme
conservée, et rien ne le signale tant que Niumi n'est pas rouvert. La ligne de la matrice « Niumi
retiré des récents → alarme et blocage conservés — OK » était fausse pour le blocage : `LOT-0.md`
n'avait mesuré que l'alarme, avec `am kill`, qui ne tue pas le processus quand le service est lié.

**Ce que les gestes font réellement** (28/09, `logcat` à l'appui) :

| Heure | Situation | Résultat |
| --- | --- | --- |
| 12:06 | carte verrouillée (cadenas), balayage individuel | tuée : le cadenas ne protège pas du balayage |
| 12:17 | processus relancé par le service, sans tâche, verrou posé à 12:06 | épargné par « Tout effacer » |
| 14:18 | carte cachée (`setExcludeFromRecents`), sans verrou, « Tout effacer » | tué : `removetask` puis `OneKeyClean`, la tâche cachée n'échappe pas au nettoyeur |
| 14:25 | processus sans tâche, sans verrou, « Tout effacer » | tué 5 s après son démarrage |
| 14:37 | carte cachée, **verrou posé**, session armée, « Tout effacer » | **vivant**, service lié, Acrobat renvoyé à l'accueil |
| 14:39 | idem après un redémarrage | **vivant**, verrou conservé dans `locked_apps` |

Le verrou vit dans `Settings.System` sous `locked_apps`, tableau JSON par utilisateur
(`[{"u":0,"pkgs":["com.niumi.app"]}, …]`). Il a été perdu une fois entre 14:06 et 14:30, après le
balayage de la carte verrouillée et un « Tout effacer » sans lui ; la cause exacte n'est pas isolée.
Il a survécu à un redémarrage (14:39), à une mise à jour de l'APK (15:03) et même à la
désinstallation faite par `connectedDebugAndroidTest` (18:52) — et, verrou posé, la
mise à jour n'a **pas** délié le service d'accessibilité, contrairement aux réinstallations
précédentes : une seule observation.

**Parades décidées avec l'utilisateur** (aucune application ne peut s'exempter elle-même de
« Tout effacer » sur HyperOS) :

1. **Carte cachée pendant la session.** `MainActivity` applique `setExcludeFromRecents` à la tâche
   de l'activité principale tant que la session est en cours, et le retire dès un état final.
   Mesuré : drapeau `0x00800000` présent pendant la session, carte absente du panneau, revenue
   après le scan. Une tâche née d'`AlarmActivity` hérite de l'exclusion jusqu'à la fin de session
   (comportement antérieur, sans conséquence).
2. **Verrou exigé par le diagnostic** (`RECENTS_LOCK`, `BLOCKING_FOR_NIUMI_EXPERIENCE` ;
   `NOT_APPLICABLE` si le réglage est absent ou illisible). Mesuré de 15:04 à 15:07 : vert pendant
   une session (écran 12) ; cadenas retiré hors session → échec bloquant sur l'écran 2 avec sa
   consigne et « J'ai verrouillé Niumi », sans « Choisir mon heure de réveil » ; cadenas remis →
   vert dès le retour sur l'écran, qui rejoue le diagnostic (§13), avant même le bouton.

Écartés, après examen : supprimer la tâche en quittant Niumi (« Tout effacer » tue aussi un
processus sans tâche, 14:25), service de premier plan permanent (même priorité que le service lié,
contraire au MVP), surveillance périodique (ne répare rien, troisième dérogation à §9.1).

## Exemption d'énergie : la détection est fiable sur HyperOS

SPEC_ANDROID §13 affirmait qu'après le passage en « Aucune restriction »,
`isIgnoringBatteryOptimizations()` « continue de renvoyer `false` » : le contrôle reposait donc
sur une confirmation de l'utilisateur, persistée une fois pour toutes. `ETAPE-05.md` ne contient
pourtant aucun relevé de la liste blanche. Relevé le 28/09 de 19:11 à 19:13, après chacune des
quatre options de « Économiseur de batterie » (fiche de Niumi) :

| Option HyperOS | `dumpsys deviceidle whitelist` | Bucket |
| --- | --- | --- |
| Restreindre les applications en arrière-plan | absent | 10 |
| Économiseur de batterie (recommandé) | absent | 10 |
| Fermez les applications après 10 minutes d'activité en arrière-plan | absent | 10 |
| Pas de restriction | `user,com.niumi.app` | 5 (exempté) |

Le réglage HyperOS inscrit donc Niumi dans la liste blanche. Mais la page Android des
optimisations de batterie, celle qu'ouvre le diagnostic et que l'utilisateur a toujours employée
pendant le développement, l'y inscrit aussi (19:42), **sans** toucher au réglage HyperOS, resté sur
« recommandé ». Lequel empêche le gel ? Essai dans cette combinaison, session immédiate armée avec
Acrobat : Niumi classé `idle` par HyperOS (`SmartPower`) de 20:19:04 à 20:23:08, puis Acrobat lancé à
20:23:08.485, renvoyé à l'accueil à 20:23:08.691 (**206 ms**). Un premier essai à 20:18 ne comptait
pas : HyperOS tenait encore Niumi pour « visible ». **C'est la liste blanche qui compte** ; la
détection lit donc le bon signal, et le recours ouvre le bon écran. La confirmation, elle, restait vraie après un réglage revenu en arrière (une mise à jour, mesurée à
l'étape 5) : faux état de fiabilité. Décision de l'utilisateur : **détection seule**. Le contrôle
passe si et seulement si `isIgnoringBatteryOptimizations()` renvoie `true` ; le recours ouvre la
liste système ; le retour des réglages suffit (`ON_RESUME`). Préférence
`battery_exemption_confirmed` retirée sans migration : la clé déjà écrite n'est plus relue.
Réserve : autres surcouches non mesurées (B1, B2).

## Limites écrites dans l'aide

Trois puces ajoutées à « Ce que Niumi ne peut pas garantir », identiques dans `LIMITES.md` et
`HelpTexts` (`HelpTextsTest`), chacune adossée à une mesure :

- récents sur Xiaomi : « Tout effacer » sans verrou, carte absente pendant la session (27 et 28/09) ;
- début du blocage à l'heure tant que le téléphone est allumé, en retard et signalé s'il est éteint
  (24, 27 et 28/09) ;
- application déjà ouverte renvoyée à l'accueil en moins d'une seconde (222 et 308 ms). Le texte
  proposé par le plan (« cela peut prendre quelques secondes ») contredisait la mesure.

## Vérification

```bash
./gradlew testDebugUnitTest :app:testReleaseUnitTest :shared:core:jvmTest   # 1 137, 0 échec
./gradlew ktlintCheck detekt :app:lintDebug :app:assembleDebug assembleDebugAndroidTest   # verts
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64 :app:lintRelease :app:assembleRelease :app:bundleRelease   # verts le 28/09 après la dernière modification
./gradlew connectedDebugAndroidTest   # 151 verts le 28/09 après la dernière modification, un ignoré par hypothèse
```

## Specs et documents modifiés

- **SPEC_ANDROID** : §7.1 (`nfcEvaluable`), §13 (contrôle du verrou dans les récents, quinze
  contrôles ; exemption d'énergie détectée et non plus confirmée), §13.1 (NFC et accessibilité après déverrouillage, premier plan, retrait des
  avertissements), §15 (incidents rétablis sur l'écran 7 ; carte cachée pendant la session et
  gestes des récents), §17 (`BLOCKING_STARTED` sur effet satisfait), §18, §20 (ligne des récents).
- **Plan maître** : points de vigilance 11 et 14, particularités de l'appareil, défauts 1 à 6 de
  l'étape 25, version de Navigation. Les ajouts du Lot 7, écrits par une autre session, n'ont pas été
  touchés.
- **Documents de release** : `QA_MATRIX.md` (section Lot 6, ligne des récents requalifiée, 50
  scénarios), `RELEASE_REPORT.md` (31 critères, écarts 8 à 11), `RESTE_A_FAIRE.md` (A7, B1, B2, D),
  `LIMITES.md`.
- **`ACCESSIBILITY_DECLARATION.md` : inchangée.** L'usage déclaré ne change pas : la relecture du
  dernier package à l'heure de début n'emploie que le `packageName` déjà reçu par les événements
  (§12.4), sans `windows` ni `rootInActiveWindow`. §23 ne crée aucune porte nouvelle pour le Lot 6.

## Ce qui reste à valider — dette assumée

1. **Récents** (A7, traité) : le cas « sans objet » (appareil sans verrou HyperOS) n'est pas
   mesurable sur ce Xiaomi ; la perte du verrou observée entre 14:06 et 14:30 n'est pas expliquée.
2. **Mort du processus** : même cause, même traitement.
3. **Écran 7 et volet rapide** : l'écran ne rejoue le diagnostic qu'à `ON_RESUME` ; un réglage
   changé depuis le volet n'est vu qu'en quittant l'écran. Correctif proposé (retour du focus).
4. **Pixel, Samsung, APK de publication** : inchangés (B1, B2, A2).
5. `tools/validate_alarm.sh` : deux défauts relevés, corrigés et rejoués sur appareil le 30/09
   (sonnerie détectée +1 s ; écart 11 de `RELEASE_REPORT.md`).

## Frottements d'outillage rencontrés

- HyperOS annule en quelques secondes la demande d'installation de chaque APK de test : six refus
  sur trois passes ; une surveillance du journal Gradle a permis de rejouer le seul module manqué.
- Le Mac étant relié au point d'accès du téléphone, chaque redémarrage coupait le poste ; campagne
  poursuivie depuis le Wi-Fi domestique.
- Téléphone éteint câble branché : il se rallume seul. L'essai « éteint à l'heure de début » exige
  le câble débranché.
- `adb shell` recolle ses arguments : `date '+%Y-%m-%d %H:%M:%S'` échoue sur l'appareil sans
  guillemets supplémentaires.
