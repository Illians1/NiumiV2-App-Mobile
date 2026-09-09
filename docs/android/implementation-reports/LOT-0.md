# Lot 0 — dossier Google Play et porte de validation 0

Statut : **porte 0a franchie le 2026-09-08** (voir « Statut de la porte 0a » en fin de document).
Dossier documentaire rédigé le 2026-09-07, campagne d'essais physiques déroulée les 7 et 8
septembre 2026. La porte 0b — verdict Google Play sur l'AccessibilityService — reste ouverte et
est rattachée à l'étape 21.

## Décision de calendrier (2026-09-07)

SPEC_ANDROID §22 Lot 0 et §23 demandaient de soumettre le POC sur une piste Play (interne ou
fermée) dès l'étape 6. **Décision validée avec l'utilisateur : la soumission et le tournage de
la vidéo de revue sont reportés à l'étape 21**, une fois le POC supprimé et le parcours
utilisateur réel livré. Raisons :

1. La [politique AccessibilityService de Play](https://support.google.com/googleplay/android-developer/answer/10964491)
   exige une vidéo montrant la divulgation et le consentement **en usage normal**. Le seul
   parcours existant aujourd'hui (`PocScreen`) est un écran de debug où l'on saisit un nom de
   package à la main — le filmer ne produirait pas de signal exploitable sur l'acceptation
   Google de l'usage réel.
2. Une [piste interne n'est pas soumise aux revues de politique standard](https://support.google.com/googleplay/android-developer/answer/9845334) ;
   seule la première publication passe une revue de politique. Soumettre maintenant valide la
   tuyauterie technique de Play Console, pas la politique elle-même — ce qui compte réellement
   pour la décision produit.

**Risque assumé :** SPEC_ANDROID §22 Lot 0 déconseille explicitement d'investir dans
l'interface complète avant validation de la stratégie de publication liée à l'accessibilité.
Cette déviation accepte ce risque : les étapes 7 à 20 seront implémentées avant que Google ait
statué. Si Google refuse l'usage à l'étape 21, la révision devra remonter jusqu'ici. Cette
information a été communiquée à l'utilisateur, qui l'a acceptée en connaissance de cause.

Specs mises à jour en conséquence : `SPEC_ANDROID.md` §22 (Lot 0) et §23. Plan mis à jour :
`docs/superpowers/plans/2026-09-03-mvp-android.md`, étapes 6 et 21, et la ligne correspondante
de la section « Recette et critères d'acceptation ».

## Préconditions de soumission encore ouvertes

Non traitées à cette étape, à lever avant l'étape 21 :

| Précondition | État | Qui |
| --- | --- | --- |
| Identité de l'éditeur (nom, adresse) | `<À COMPLÉTER>` | utilisateur |
| E-mail de contact public | `<À COMPLÉTER>` | utilisateur |
| URL d'hébergement de `PRIVACY_POLICY.md` | `<À COMPLÉTER>` | utilisateur |
| Compte développeur Google Play créé et vérifié | à faire | utilisateur |
| Keystore d'upload (release) | inexistant, non versionné, généré à l'étape 21 | agent + utilisateur |
| AAB release signé (R8, suppression des ressources, §16) | configuration inexistante ; `assembleRelease` prévu à l'étape 21 | agent |
| 12 testeurs opt-in pendant 14 jours consécutifs si compte personnel ([exigence Play](https://support.google.com/googleplay/android-developer/answer/14151465)) | non démarré | utilisateur |

## Documents Play rédigés (`docs/android/play-console/`)

| Document | Contenu | Statut |
| --- | --- | --- |
| `ACCESSIBILITY_DECLARATION.md` | Formulaire de déclaration, divulgation mot pour mot, preuve `capabilities=0` | rédigé |
| `USE_EXACT_ALARM.md` | Justification, API imposée, comportement de repli | rédigé |
| `FULL_SCREEN_INTENT.md` | Justification, déclenchement, comportement si refusé | rédigé |
| `FGS_MEDIA_PLAYBACK.md` | Justification du type `mediaPlayback`, point de vigilance signalé | rédigé |
| `PRIVACY_POLICY.md` | Données consultées et conservées, aucune transmission | rédigé, champs éditeur à compléter |
| `REVIEW_VIDEO_SCRIPT.md` | Script séquence par séquence | rédigé, tournage reporté à l'étape 21 |

## Matrice physique (SPEC_ANDROID §20)

Colonnes : fabricant, modèle, Android, firmware, permissions, scénario, résultat, retard mesuré,
logs.

### Lignes déjà prouvées aux étapes 3 à 5 (reportées, pas rejouées)

| Fabricant | Modèle | Android | Scénario | Résultat | Rapport source |
| --- | --- | --- | --- | --- | --- |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Écran éteint et verrouillé, alarme dans 90 s | Sonnerie audible, `AlarmActivity` au-dessus du verrouillage, notification sans action | `ETAPE-03.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Fermeture de l'activité pendant la sonnerie | Sonnerie maintenue | `ETAPE-03.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Retrait de Niumi des récents pendant la sonnerie | Alarme conservée | `ETAPE-03.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Association d'un tag, scan du bon tag | Arrêt en moins d'une seconde | `ETAPE-04.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | NFC désactivé pendant la sonnerie | Instruction affichée, sonnerie maintenue | `ETAPE-04.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Téléphone verrouillé, tentative de scan | Restriction NFC-verrouillé confirmée, spécifique HyperOS (§4.4) | `ETAPE-04.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Ouverture app bloquée : launcher, récents, intent direct | Retour à l'accueil + overlay, < 1 s après correctif batterie | `ETAPE-05.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Ouverture app non listée | Aucun effet | `ETAPE-05.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Désactivation du service pendant blocage actif | Blocage cesse immédiatement | `ETAPE-05.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | Batterie optimisée (défaut HyperOS) vs. « Aucune restriction » | Sans exemption : blocage silencieusement inopérant après ~60 s. Avec exemption : bloqué en < 1 s | `ETAPE-05.md` |
| Xiaomi (Redmi) | 25080RABDG | 16 / API 36 | `am force-stop` | Service retiré des services activés (limite §4.2 confirmée) | `ETAPE-05.md` |

### Lignes exécutées pendant la campagne de cette étape

Campagne du 2026-09-07 sur Redmi 25080RABDG, Xiaomi HyperOS V816 OS3.0, Android 16 / API 36,
firmware `BP2A.250605.031.A3`, Niumi 0.1.0 debug. Permissions au moment des essais :
`android.permission.NFC` accordée, `POST_NOTIFICATIONS` accordée par `pm grant` (le parcours de
demande à l'utilisateur n'existe pas encore, il arrive à l'étape 12), `USE_FULL_SCREEN_INTENT`
en `allow`, exemption de batterie AOSP active. Service d'accessibilité **inactif** pendant toute
la campagne : les essais ci-dessous portent sur l'alarme, le blocage ayant été validé à
l'étape 5 par `tools/validate_blocking.sh`.

Outils créés pour cette campagne : `tools/capture_device_state.sh` (colonnes de contexte
exigées par §20) et `tools/validate_alarm.sh` (programmation depuis le POC, mesure du retard,
relevé du service, de l'audio, de la notification et de l'activité au premier plan).

Chaque sonnerie a été terminée par un scan réel du boîtier associé, seul moyen prévu par le
produit : aucune action d'arrêt logicielle n'existe (§3, §10.2).

| Fabricant | Modèle | Android | Scénario | Résultat attendu | Résultat observé |
| --- | --- | --- | --- | --- | --- |
| Xiaomi (Redmi) | 25080RABDG | 16 | Volumes média et notification à zéro, volume alarme actif | Sonnerie audible | **OK** (2026-09-07) — retard nul (29 s mesurés pour 30 s demandés, sondage ± 2 s) ; focus `usage=USAGE_ALARM content=CONTENT_TYPE_SONIFICATION` obtenu ; `STREAM_ALARM Muted: false`, volume 12 ; notification `niumi_alarm_ringing`, `category=alarm`, importance 4, `ONGOING`, `sound=null` ; audibilité confirmée à l'oreille. Essai plus sévère que prévu : sur cet appareil `STREAM_NOTIFICATION` est aliasé vers `STREAM_RING`, les deux tombent donc à zéro ensemble |
| Xiaomi (Redmi) | 25080RABDG | 16 | Mode silencieux, volume alarme actif | Sonnerie audible (flux `USAGE_ALARM`) | **OK** (2026-09-07) — `mode (internal) = SILENT`, volume alarme 12 ; retard nul (27 s pour 30 s demandés) ; focus `USAGE_ALARM` obtenu, `STREAM_ALARM Muted: false` ; audibilité confirmée à l'oreille |
| Xiaomi (Redmi) | 25080RABDG | 16 | Ne pas déranger autorisant les alarmes, **écran éteint** | Sonnerie audible | **OK** (2026-09-07) — `zen_mode=1` (`ZEN_MODE_IMPORTANT_INTERRUPTIONS`), politique active `alarms=allow` mais `fullScreenIntent=disallow`. Malgré cette dernière, l'écran est passé de `Dozing` à `Awake` et `AlarmActivity` est arrivée au premier plan : la suppression du plein écran ne s'applique pas aux notifications que la politique autorise. Retard nul (~27 s pour 30 s), focus `USAGE_ALARM` actif. Audibilité non confirmée explicitement par l'opérateur pour cet essai : seule la fin de session par scan est attestée, le son n'est prouvé qu'indirectement (focus obtenu, flux non muté) |
| Xiaomi (Redmi) | 25080RABDG | 16 | Ne pas déranger interdisant les alarmes (silence total), écran éteint | Diagnostic et comportement consignés, aucune fausse garantie | **Réveil inopérant** (2026-09-07) — voir « Constat bloquant » ci-dessous. Le service démarre (~27 s, retard nul) et la notification est postée, mais `STREAM_ALARM Muted: true` / `streamVolume:0`, l'écran reste `Dozing`, `AlarmActivity` ne s'ouvre pas et `mIsReaderMode=false` |
| Xiaomi (Redmi) | 25080RABDG | 16 | Ne pas déranger « alarmes seules » (`zen_mode=3`), écran éteint | Sonnerie audible | **OK** (2026-09-07) — `ZEN_MODE_ALARMS` ; `STREAM_ALARM Muted: false`, volume 12 ; service démarré ~27 s ; écran `Awake` et `AlarmActivity` au premier plan. Essai ajouté hors matrice §20 pour délimiter la règle : il établit que seul le silence total est en cause, pas le DND en général |
| Xiaomi (Redmi) | 25080RABDG | 16 | Casque Bluetooth connecté (A2DP), volume alarme 6/15 haut-parleur et 4/15 casque | Sortie audio conforme à la stratégie documentée, son détectable | **OK** (2026-09-07) — le flux est **dupliqué sur les deux sorties**. `STREAM_ALARM` porte `Devices: speaker(2), bt_a2dp(80)`, et le thread de sortie primaire du HAL confirme `Output devices: 0x2, 0x80 (AUDIO_DEVICE_OUT_SPEAKER, AUDIO_DEVICE_OUT_BLUETOOTH_A2DP)`. Un casque appairé ne peut donc pas capter l'alarme à lui seul et laisser dormir l'utilisateur. Volumes indépendants par périphérique : régler `STREAM_ALARM` ne touche que la sortie courante, le casque conservait 12/15 quand le haut-parleur était à 6. **Audibilité confirmée à l'oreille par l'opérateur : le son sort bien des deux sorties simultanément** |
| Xiaomi (Redmi) | 25080RABDG | 16 | Processus Niumi tué après armement, hors sonnerie (`am kill`, équivalent au retrait des récents) | Alarme conservée | **OK** (2026-09-07) — après `am kill`, `pidof` vide et l'alarme reste dans `dumpsys alarm` ; au déclenchement le système relance le processus (nouveau PID), `AlarmRingingService` démarre, focus `USAGE_ALARM` obtenu, `STREAM_ALARM Muted: false` volume 12. Écran allumé et déverrouillé pendant l'essai, donc pas de plein écran : comportement Android normal (notification heads-up), le launcher reste au premier plan |
| Xiaomi (Redmi) | 25080RABDG | 16 | Arrêt du FGS depuis le système (gestionnaire des services actifs) | Limite connue consignée | **Non reproductible sur cette surcouche** (2026-09-07) — le gestionnaire des services actifs d'AOSP (chip « N applications actives » des réglages rapides, qui arrête un service de premier plan sans tuer le processus) n'a pas été trouvé sur HyperOS V816 OS3.0, après recherche par l'opérateur dans le volet de notifications et les réglages rapides. Le geste équivalent accessible à l'utilisateur sur cet appareil est « Forcer l'arrêt » depuis la fiche de l'application, dont l'effet est mesuré à la ligne précédente (processus tué, alarme annulée, notification retirée). À rejouer sur un appareil AOSP ou proche d'AOSP — Pixel de préférence — pendant la campagne de bêta-test, pour distinguer l'arrêt du seul service de l'arrêt forcé complet |
| Xiaomi (Redmi) | 25080RABDG | 16 | Arrêt forcé de Niumi | Alarme annulée par le système, limite connue consignée | **OK, limite confirmée** (2026-09-07) — alarme armée pour `16:14:02.428` (`type=RTC_WAKEUP`, `flags=0x3`), puis `am force-stop`. Immédiatement après : processus mort (`pidof` vide) et **plus aucune alarme en attente** dans `dumpsys alarm`. L'heure cible est passée (vérifié à `16:14:30`) sans aucun déclenchement, service jamais démarré. Confirme §4.2 : depuis Android 15, l'arrêt forcé annule les `PendingIntent` et donc le réveil. Rien à corriger côté Niumi ; la limite doit rester documentée dans l'aide (`LIMITES.md`, étape 21). **Rejoué ensuite par le geste utilisateur réel** (Réglages → Applications → Niumi → Forcer l'arrêt), pendant une sonnerie active cette fois : service arrêté à `16:17:40`, processus tué (`pidof` vide), notification retirée, aucune alarme en attente. Même résultat que par `adb`, y compris pendant `RINGING` |
| Xiaomi (Redmi) | 25080RABDG | 16 | **Doze profond réel, appareil débranché et immobile** | Alarme à l'heure | **OK** (2026-09-08) — essai mené via `adb` par Wi-Fi, câble USB retiré : `USB powered: false`, `status: 3` (déchargement), écran éteint, téléphone posé sans être touché. L'appareil est entré en Doze profond (`deviceidle deep=IDLE`, `light=OVERRIDE`) à `18:35:52`, soit 3 min après le débranchement, et y est resté jusqu'à `18:56:55` au moins, avant d'en ressortir à `18:59:56` (fenêtre de maintenance, comportement normal). Cible `19:06:42.237`, déclenchement observé à `19:06:41` d'après `dumpsys alarm` (`last -35s152ms` relevé à `19:07:16`) → **retard nul**. `STREAM_ALARM Muted: false` volume 3/15, écran rallumé, `AlarmActivity` au premier plan, son confirmé à l'oreille. **C'est la validation du scénario nocturne réel**, et donc de la promesse centrale du produit. Deux réserves de méthode : l'appareil était sorti du Doze depuis ~7 min à l'échéance, si bien que le déclenchement n'a pas eu lieu *pendant* `deep=IDLE` — le drapeau `FLAG_WAKE_FROM_IDLE` reste la garantie système pour ce cas ; et les sondages `dumpsys` toutes les 3 min ont pu contribuer aux sorties de veille |
| Xiaomi (Redmi) | 25080RABDG | 16 | Doze forcé (`dumpsys deviceidle force-idle`), câble branché | Alarme à l'heure | **Non concluant** (2026-09-07) — le Doze profond n'a pas pu être atteint sur cet appareil : `force-idle` répond « Unable to go deep idle; stopped at INACTIVE » et douze `step deep` consécutifs restent à `INACTIVE`, alors que les préconditions sont réunies (`mScreenOn=false`, `mCharging=false`, `status: 3`, `AC/USB/Wireless powered: false`). **Cause identifiée après coup** : le câble USB était branché, or Android n'entre jamais en Doze tant que l'appareil est en charge — `dumpsys battery unplug` ne simule le débranchement que pour la couche batterie, pas pour la politique d'inactivité. L'essai était donc voué à l'échec par construction. Rejoué correctement le 2026-09-08 par Wi-Fi, câble retiré : voir la ligne précédente. **Preuve statique relevée à la place** : `dumpsys alarm` donne `type=RTC_WAKEUP`, `window=0`, `exactAllowReason=policy_permission`, `flags=0x3` (`FLAG_STANDALONE\|FLAG_WAKE_FROM_IDLE`) et `device_idle=--` dans `policyWhenElapsed` — l'alarme porte le drapeau qui la fait percer le Doze et aucune politique d'inactivité ne la retarde. **Essai réel associé** : sur batterie simulée déchargée et écran éteint, cible `16:09:09.734`, déclenchement observé à `16:09:11`, soit **~1 s de retard** ; écran rallumé, `AlarmActivity` au premier plan, `STREAM_ALARM Muted: false` volume 12 |
| Xiaomi (Redmi) | 25080RABDG | 16 | Écran éteint depuis 30 minutes | Alarme à l'heure, écran présenté ou notification urgente | **OK** (2026-09-07) — écran éteint de `16:26:28` à `16:57:24`, soit 31 min ; cible `16:57:23.739`, déclenchement observé à `16:57:24` → **1 s de retard**. Écran rallumé (`Dozing` → `Awake`), `AlarmActivity` au premier plan par-dessus le verrouillage, `STREAM_ALARM Muted: false` volume 12. **Nuance à ne pas surinterpréter** : `deviceidle` est resté à `ACTIVE`/`ACTIVE` pendant toute la durée — l'appareil n'est jamais entré en Doze de lui-même, USB branché. Cet essai valide donc « écran éteint 30 minutes », pas « 30 minutes en Doze », qui reste non couvert sur cet appareil (voir la ligne Doze) |

### Constat bloquant — Ne pas déranger en silence total rend le réveil inopérant

Mesuré le 2026-09-07 sur le Redmi 25080RABDG, Android 16, `zen_mode=2`
(`ZEN_MODE_NO_INTERRUPTIONS`), volume d'alarme réglé à 12, écran éteint. Trois effets se
cumulent, aucun visible en test automatisé :

1. **Le son est mué par le système.** `STREAM_ALARM` passe à `Muted: true` avec
   `streamVolume:0`, alors que le volume réglé par l'utilisateur est 12 et que le lecteur
   emploie bien `USAGE_ALARM`. Le paramétrage de Niumi est correct ; c'est la politique DND qui
   coupe le flux.
2. **L'écran ne s'allume pas et `AlarmActivity` ne s'ouvre pas.** L'appareil reste `Dozing`,
   aucune activité au premier plan, y compris après un réveil manuel de l'écran. À comparer avec
   le mode « interruptions prioritaires » (`zen_mode=1`, essai précédent), où l'écran se rallume
   et l'écran de réveil s'affiche normalement.
3. **Conséquence la plus grave : le Reader Mode NFC n'est jamais activé** (`mIsReaderMode=false`).
   Le Reader Mode vit dans `AlarmActivity.onResume()` (SPEC_ANDROID §10.4) ; sans cette activité,
   approcher le boîtier ne produit rien. L'utilisateur ne peut donc pas terminer sa session par
   le geste prévu par le produit.

Ce qui fonctionne malgré tout : `AlarmRingingService` démarre au premier plan avec un retard nul,
et la notification du canal `niumi_alarm_ringing` (importance 4, `category=alarm`) est bien
postée. La sortie existe donc, mais elle est manuelle et non découvrable : l'utilisateur doit
rallumer son écran de lui-même, repérer la notification et la toucher pour atteindre l'écran de
scan. Rien ne le réveille pour qu'il le fasse.

**Conséquence pour les specs.** SPEC_ANDROID §13 classe « mode Ne pas déranger préoccupant » en
`WARNING`, c'est-à-dire « activation possible avec une information claire ». La mesure montre que
ce niveau est faux au moins pour le silence total : ni le réveil sonore (`BLOCKING_FOR_ALARM`),
ni le parcours de scan (`BLOCKING_FOR_NIUMI_EXPERIENCE`) ne peuvent être garantis. C'est le même
schéma qu'à l'étape 5 avec l'optimisation de batterie : une hypothèse de spec invalidée par la
mesure sur appareil réel. §13 doit distinguer les modes DND au lieu de les traiter en bloc, et le
diagnostic doit lire `NotificationManager.getCurrentInterruptionFilter()` plutôt que de supposer
que les alarmes passent toujours.

**Délimitation mesurée de la règle.** Le mode « alarmes seules » (`zen_mode=3`,
`ZEN_MODE_ALARMS`) a été testé ensuite dans les mêmes conditions : `STREAM_ALARM Muted: false` à
son volume réglé, service démarré sans retard, écran rallumé et `AlarmActivity` affichée. Seul
`ZEN_MODE_NO_INTERRUPTIONS` est donc en cause, et il se détecte proprement par
`getCurrentInterruptionFilter() == INTERRUPTION_FILTER_NONE`. À noter :
`fullScreenIntent=disallow` figure dans la politique des trois modes alors que le plein écran
fonctionne dans deux d'entre eux — cet attribut ne permet pas de prédire le comportement, seule
la mesure le fait.

**Comparaison avec l'horloge d'Android (2026-09-08).** Question posée par l'utilisateur avant de
trancher : l'alarme système sonne-t-elle en silence total ? Essai réalisé sur le même appareil,
en programmant une alarme dans `com.android.deskclock` par `ACTION_SET_ALARM`, `zen_mode=2`,
écran éteint. Résultat : `STREAM_ALARM` reste `Muted: true` et **aucun son n'est émis**
(confirmé à l'oreille). L'horloge système ne sonne donc pas davantage que Niumi. Une seule
différence à son avantage : elle parvient à afficher son `AlarmAlertFullScreenActivity` et à
rallumer l'écran, là où Niumi reste invisible — explication la plus probable, une application
système préinstallée n'est pas soumise aux mêmes restrictions de lancement d'activité depuis
l'arrière-plan.

Cette mesure a écarté l'option consistant à demander `ACCESS_NOTIFICATION_POLICY` pour lever le
filtre au déclenchement : le mécanisme fonctionne techniquement (vérifié — basculer le filtre de
`none` à `alarms` démute `STREAM_ALARM` instantanément), mais l'adopter rendrait Niumi plus
intrusif que l'horloge d'Android elle-même, pour contourner un réglage que l'utilisateur a
délibérément posé. Niumi ne modifie aucun réglage système.

**Décision produit (2026-09-07, validée avec l'utilisateur).** Le silence total devient un
contrôle `BLOCKING_FOR_ALARM` réévalué à chaque activation ; s'il est enclenché après
l'armement, le déclenchement crée l'incident `ANDROID_ALARM_MUTED_BY_DND` (`CRITICAL`) et
journalise `ALARM_MUTED_BY_DND`. Niumi ne demande pas `ACCESS_NOTIFICATION_POLICY` et ne modifie
jamais le réglage de l'utilisateur. SPEC_ANDROID §4.1, §4.2, §13 et §17 ont été mises à jour dans
le même changement. Implémentation : étape 8 (`ActivationPolicy`) et étape 12
(`DeviceReadinessChecker`), plus `AlarmReceiver` à l'étape 17 pour l'incident au déclenchement.

### Second constat bloquant — la notification de sonnerie est balayable, et son balayage enferme l'utilisateur

Trouvé fortuitement le 2026-09-07, pendant la campagne, par un balayage involontaire de la
notification de sonnerie. Mesuré immédiatement après, sonnerie toujours active :

| Mesure | État |
| --- | --- |
| `AlarmRingingService` | actif (PID 15570), sonnerie en cours |
| Focus audio `USAGE_ALARM` de Niumi | en haut de la pile de focus |
| Notification Niumi | **0** — disparue du panneau |
| Activité au premier plan | `MainActivity` (l'utilisateur avait rouvert Niumi) |
| `mIsReaderMode` | **false** |

L'alarme sonne, et plus aucun chemin ne mène à `AlarmActivity`. Comme le Reader Mode NFC vit
dans `AlarmActivity.onResume()` (§10.4), approcher le boîtier ne produit rien : le geste censé
terminer la session est inopérant. La seule issue observée a été « Forcer l'arrêt » depuis la
fiche de l'application — c'est-à-dire sortir du produit.

**Cause.** Depuis Android 14, l'utilisateur peut rejeter la notification d'un service de premier
plan malgré `setOngoing(true)`. SPEC_ANDROID §10.3 suppose l'inverse et fait reposer tout
l'accès à l'écran de scan sur cette notification et sur le plein écran. La notification observée
portait bien `flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY` : ces drapeaux ne
suffisent plus.

**Portée.** Le défaut n'est pas propre au POC. Le produit final prévoit certes qu'ouvrir Niumi
pendant une session active redirige vers l'écran adéquat (étape 12 pour `HomeScreen`, étape 17
pour la reprise d'état de `AlarmActivity`), ce qui offrira une seconde porte d'entrée. Mais deux
questions restent entières et doivent être tranchées avant l'étape 17 : la notification doit-elle
être republiée quand elle est rejetée, et le Reader Mode doit-il rester confiné à
`AlarmActivity` alors qu'il est le seul moyen de sortir d'une session ?

**Ce constat n'a pas encore de correctif décidé** : aucune spec n'a été modifiée sur ce point,
contrairement au constat DND. À arbitrer avec l'utilisateur avant l'implémentation de l'étape 17.

### Troisième constat bloquant — le déverrouillage détruit l'écran de réveil

Mesuré le 2026-09-07 par échantillonnage continu de `dumpsys nfc` et `dumpsys activity
activities`, pendant une sonnerie, au moment exact où l'opérateur déverrouille l'écran :

| Mesure | Avant déverrouillage | Après déverrouillage |
| --- | --- | --- |
| `mScreenState` | `ON_LOCKED` | `ON_UNLOCKED` |
| Activité au premier plan | `AlarmActivity` | **`MainActivity`** |
| `AlarmActivity` dans la pile | présente | **absente** (seule `MainActivity` en `Hist #0`) |
| `mIsReaderMode` | `false` | **`false`** |
| `AlarmRingingService` | actif | actif |
| Notification | présente | présente |

L'écran de réveil est donc **détruit** au déverrouillage et l'utilisateur retombe sur l'accueil,
alarme toujours en train de sonner et Reader Mode inactif. Le geste le plus naturel du réveil —
déverrouiller son téléphone pour scanner le boîtier — fait perdre l'écran qui porte le scan.
Observé deux fois. La notification subsiste ici, donc la sortie existe (la toucher rouvre
`AlarmActivity`), mais elle n'est ni évidente ni annoncée.

Deux défauts distincts s'ajoutent, signalés par l'utilisateur à partir de son propre usage :

1. **Le texte de l'écran de réveil ne se met pas à jour au déverrouillage.** `refreshState()`
   n'est appelé que dans `onResume()` et après un scan
   (`AlarmActivity.kt`) : `isDeviceLocked()` n'est évalué qu'à l'affichage. Si le verrouillage
   change pendant que l'écran est visible, le texte « Déverrouille ton téléphone, puis approche-le
   du boîtier. » (§11.2) reste affiché alors que la condition est fausse. Correctif :
   écouter le changement d'état du keyguard (`ACTION_USER_PRESENT`, ou
   `KeyguardManager.addKeyguardLockedStateListener` en API 34+) et recalculer l'état.
2. **Ouvrir Niumi pendant que l'alarme sonne ne ramène pas à l'écran de réveil.** Prévu au plan
   (étape 12 pour `HomeScreen`, étape 17 pour la reprise d'état), mais à inscrire comme règle de
   spec et non comme simple tâche : c'est l'une des garanties d'accès au scan.

### Lecture d'ensemble des trois constats

Les trois ne sont pas des bugs indépendants : ils partagent une cause unique. **`AlarmActivity`
porte seule le Reader Mode, donc la seule sortie possible d'une session**, et elle disparaît dans
au moins trois situations ordinaires — silence total, notification balayée, déverrouillage.

Le Reader Mode ne peut pas être déplacé ailleurs : `NfcAdapter.enableReaderMode(Activity, ...)`
exige une `Activity` au premier plan et n'a aucune variante pour un `Service` (documentation
Android vérifiée le 2026-09-07). La réponse ne peut donc pas être de découpler, mais de
**garantir la présence de l'activité** tant que la session sonne, quelle que soit la cause de sa
disparition, le plein écran d'une notification republiée étant le seul mécanisme qu'Android
autorise pour ouvrir une activité depuis l'arrière-plan.

### Conséquences pour les étapes suivantes

- **Le volume d'alarme est indépendant par périphérique de sortie.** Mesuré pendant l'essai
  casque : `STREAM_ALARM` valait 6 sur `speaker` et 12 sur `bt_a2dp` au même instant, et
  `cmd audio set-volume` ne modifie que la sortie courante. Le contrôle « volume alarme
  supérieur à zéro » de §13 s'appuie sur `AudioManager.getStreamVolume(STREAM_ALARM)`, qui
  renvoie la valeur de la route active au moment du diagnostic — pas nécessairement celle qui
  s'appliquera au déclenchement si la route a changé entre-temps. Le risque reste atténué par la
  duplication haut-parleur + Bluetooth constatée ci-dessus, qui garantit une sortie audible tant
  que le haut-parleur n'est pas à zéro. À prendre en compte à l'étape 12 (`DeviceReadinessChecker`),
  et à vérifier pour un casque filaire, non testé ici.
- **Stratégie de sortie audio à documenter.** §20 exige une sortie « conforme à la stratégie
  documentée », mais aucune stratégie ne figurait dans les specs. La mesure en fournit une :
  Android duplique le flux `USAGE_ALARM` sur le haut-parleur et le périphérique Bluetooth
  connecté. À inscrire dans §10.2 lors de l'étape 17, une fois confirmée sur une seconde marque.

### Hors périmètre de cette étape (consigné, pas ignoré)

| Point | Raison |
| --- | --- |
| Tous les scénarios « activation refusée » (volume alarme à zéro, notifications refusées, plein écran refusé, accessibilité désactivée) | `DeviceReadinessChecker` n'existe pas encore : le diagnostic avant activation (§13) est livré à l'étape 12, et le POC n'a aucun parcours d'activation à refuser. Vérifié le 2026-09-07 : aucune classe `DeviceReadinessChecker` ni `ReadinessCheck` dans le dépôt. Ces lignes sont donc reportées à l'étape 12 puis à la matrice QA de l'étape 21, et non « échouées » |
| Google Pixel | Aucun appareil disponible ; reporté à la campagne de bêta-test (décidé à l'étape 5) |
| Samsung Galaxy | Idem |
| Android 17 (`set-enable-hardening throw`) | Appareil disponible en Android 16 |
| Redémarrage / Direct Boot | Dépend du coordinateur, livré à l'étape 17 ; scénarios repris à l'étape 19 |
| Scan d'un second boîtier associé à un autre tag | Un seul tag physique disponible (limite déjà notée en `ETAPE-04.md`) |
| Route audio modifiée pendant `RINGING` (déconnexion casque en cours de sonnerie) | Nécessite un second appareil audio manipulable pendant l'essai, non disponible pour cette session |

## Statut de la porte 0a

**Franchie le 2026-09-08.** L'étape 7 est ouverte. La porte 0b (verdict Google Play sur
l'AccessibilityService) reste ouverte et est rattachée à l'étape 21.

- [x] Campagne d'essais exécutée et consignée ci-dessus — 13 essais sur Redmi 25080RABDG,
      HyperOS V816 OS3.0, Android 16 / API 36, les 7 et 8 septembre 2026.
- [x] Résultats inattendus traités. Quatre constats bloquants ont été trouvés pendant la
      campagne ; aucun n'est resté ouvert. Chacun a donné lieu à une décision validée et à une
      mise à jour de spec dans le même changement : SPEC_ANDROID §4.1, §4.2, §10.2, §10.4,
      §11.2, §13, §13.1 et §17. L'implémentation est rattachée aux étapes 12 et 17 du plan.
- [x] Validation explicite : **Mehdi Aouida, 2026-09-08.**

### Risques explicitement assumés par cette validation

1. **Pixel et Samsung ne sont pas couverts.** Un seul appareil, une seule surcouche. L'étape 5
   avait déjà montré qu'une surcouche peut rendre le blocage silencieusement inopérant
   (gel de processus sur HyperOS) ; d'autres constructeurs appliquent d'autres politiques.
   Reporté à la campagne de bêta-test.
2. **Google n'a pas statué sur l'usage de l'AccessibilityService.** C'est la déviation actée le
   2026-09-07 : le dossier est rédigé mais la soumission part à l'étape 21, sur l'application
   réelle. Un refus à ce moment remettrait en cause le produit Android après l'essentiel du
   développement. C'est le risque principal de cette signature.
3. **L'arrêt du seul service de premier plan n'a pas pu être testé**, l'écran correspondant étant
   absent d'HyperOS.
4. **Le scan NFC ne fonctionne pas sur écran verrouillé** sur cet appareil (limite constructeur
   documentée en §4.4, connue depuis l'étape 4). L'écran de réveil s'affiche bien par-dessus le
   verrouillage, mais le boîtier n'est lu qu'après déverrouillage.
