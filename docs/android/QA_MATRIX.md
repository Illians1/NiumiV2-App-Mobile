# Matrice de tests physiques — MVP Android

Tableau exigé par SPEC_ANDROID §20. Colonnes imposées : fabricant, modèle, Android, firmware,
permissions, résultat, retard mesuré, logs.

**Règle de remplissage.** Une ligne n'est verte que si un essai a été mené et consigné. Les lignes
jamais exécutées portent « non testé » et la raison — jamais un résultat déduit du code, d'un test
automatisé ou d'un autre appareil. Un test unitaire ou instrumenté vert ne remplit aucune ligne de
ce tableau : §20 existe précisément parce que l'émulateur et la JVM ne disent rien du NFC, de
l'audio, de Doze et des surcouches OEM.

## Périmètre effectivement couvert

| Fabricant | Modèle | Android | Firmware | État |
| --- | --- | --- | --- | --- |
| Xiaomi (Redmi) | 25080RABDG (`lapis`) | 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM | `BP2A.250605.031.A3` | **couvert**, campagnes des 7-8, 13, 14, 15, 16, 17, 24, 25, 27 et 28 septembre 2026 |
| Google Pixel | — | 14, 15, 16, 17 | — | **non testé** — aucun appareil disponible |
| Samsung Galaxy | — | 14, 15, 16 | — | **non testé** — aucun appareil disponible |
| Oppo / Realme | — | — | — | **non testé** — aucun appareil disponible |
| OnePlus, Honor, Motorola, Nothing | — | — | — | **non testé** — aucun appareil disponible |

**Une seule surcouche, la plus restrictive rencontrée.** HyperOS a produit à elle seule quatre
constats bloquants (étapes 5, 6, 19, 20). Cela ne dit rien des autres : l'étape 5 a montré qu'une
surcouche peut rendre le blocage **silencieusement** inopérant, c'est-à-dire sans aucun signal dans
l'application. La couverture Pixel et Samsung reste la première dette de cette matrice, et §21 en
fait un critère d'acceptation (« l'alarme sonne hors ligne avec l'écran éteint sur la matrice P0 »).

**Versions Android.** §20 demande Android 10/11, 12/13, 14, 15, 16 et 17. Seul **Android 16** a été
couvert. Le plancher `minSdk 29` (Android 10), les alarmes exactes d'Android 12/13, l'accès plein
écran d'Android 14, l'arrêt forcé d'Android 15 et l'audio d'arrière-plan d'Android 17 n'ont jamais
été exécutés sur l'appareil correspondant. Trois de ces comportements ont pourtant été observés
indirectement sur Android 16, qui les inclut tous : ils sont notés comme tels ci-dessous, sans être
comptés comme une couverture de version.

## Permissions au moment des essais

Sauf mention contraire dans une ligne : `NFC` accordée, `POST_NOTIFICATIONS` accordée,
`USE_FULL_SCREEN_INTENT` en `allow`, exemption d'énergie AOSP active, exemption de la restriction
d'énergie **constructeur** active (sans elle le blocage est inopérant après ~60 s, étape 5),
service d'accessibilité activé à la main, NFC du téléphone actif.

**Permission OEM « Démarrage automatique en arrière-plan » : refusée** (état par défaut) pour tous
les essais de redémarrage, ce qui est la seule façon de mesurer ce que vit un utilisateur ordinaire.
Elle avait été laissée accordée à l'issue de l'étape 19 et a été remise à « refusé » avant les
essais qui en dépendent.

## Scénarios de §20

Les 50 scénarios du tableau de §20 — dont les neuf du blocage différé (Lot 6), ajoutés le 2026-09-15 —, dans son ordre. « Source » renvoie au rapport qui porte les
mesures détaillées, dans `docs/android/implementation-reports/`.

### Réveil, audio et Doze

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| écran éteint depuis 30 minutes | alarme à l'heure, écran présenté | **OK** — écran éteint 31 min, `AlarmActivity` au premier plan par-dessus le verrouillage. L'appareil n'est pas entré en Doze de lui-même (USB branché) : valide « écran éteint », pas « 30 min en Doze » | 1 s | `LOT-0.md` |
| Doze forcé | alarme à l'heure | **OK** — Doze profond réel, appareil débranché et immobile (`deep=IDLE` pendant 21 min), cible 19:06:42, déclenchement 19:06:41. Réserve : l'appareil était sorti du Doze depuis ~7 min à l'échéance ; `FLAG_WAKE_FROM_IDLE` reste la garantie système pour un déclenchement *pendant* `IDLE` | nul | `LOT-0.md` |
| mode économie d'énergie | alarme à l'heure dans le périmètre pris en charge | **non testé** — non exécuté sur cet appareil | — | — |
| Ne pas déranger autorisant les alarmes | sonnerie audible | **OK** — `zen_mode=1`, écran rallumé, `AlarmActivity` au premier plan, focus `USAGE_ALARM`. Audibilité non confirmée à l'oreille sur cet essai précis : le son n'est prouvé qu'indirectement | nul | `LOT-0.md` |
| Ne pas déranger interdisant les alarmes | diagnostic et comportement consignés, aucune fausse garantie | **Réveil inopérant, limite établie et traitée** — `STREAM_ALARM Muted: true`, écran reste `Dozing`, `AlarmActivity` ne s'ouvre pas, Reader Mode jamais activé. L'horloge d'Android ne sonne pas davantage (vérifié). Décision produit : contrôle `BLOCKING_FOR_ALARM`, incident `ANDROID_ALARM_MUTED_BY_DND`, §4.1/§4.2/§13/§17 mises à jour | service démarré sans retard | `LOT-0.md`, `ETAPE-06.md` |
| Ne pas déranger « alarmes seules » (hors §20) | sonnerie audible | **OK** — `zen_mode=3`, `Muted: false`, écran rallumé. Essai ajouté pour délimiter la règle : seul le silence total est en cause | nul | `LOT-0.md` |
| mode silencieux, volume d'alarme actif | sonnerie audible | **OK** — `mode = SILENT`, volume alarme 12, audibilité confirmée à l'oreille | nul | `LOT-0.md` |
| volumes média et notification à zéro | sonnerie audible | **OK** — essai plus sévère que prévu : `STREAM_NOTIFICATION` est aliasé vers `STREAM_RING` sur cet appareil, les deux tombent ensemble. Audibilité confirmée à l'oreille | nul | `LOT-0.md` |
| volume alarme à zéro avant activation | activation refusée | **Impossible sur cet appareil** (2026-09-29) — le système borne le volume d'alarme à 1 (`Min: 1`, un 0 forcé est refusé : « should be in [1..15] ») ; c'est aussi le minimum par défaut d'AOSP. Le contrôle ne peut donc pas échouer ici ; question produit ouverte sur le seuil (SPEC_ANDROID §13) | — | `RESTE_A_FAIRE.md` |
| volume mis à zéro après activation | incident documenté | **Impossible sur cet appareil** (2026-09-29) — même borne à 1 : `ANDROID_ALARM_VOLUME_ZERO` ne peut pas être produit ici | — | `RESTE_A_FAIRE.md` |
| Android 17, arrière-plan et écran verrouillé > 30 min | FGS démarré, son `USAGE_ALARM` audible | **non testé** — aucun appareil Android 17 | — | — |
| casque Bluetooth connecté | sortie conforme à la stratégie documentée | **OK** — le flux est **dupliqué** haut-parleur + A2DP (`Devices: speaker(2), bt_a2dp(80)`), confirmé à l'oreille. Un casque appairé ne peut donc pas capter l'alarme seul. Volumes indépendants par sortie | nul | `LOT-0.md` |
| casque Bluetooth déconnecté pendant la nuit | sonnerie audible sur la nouvelle route | **non testé** — exige un second appareil audio manipulable pendant l'essai | — | — |
| écouteurs filaires ou USB-C | sortie conforme | **non testé** — matériel non disponible | — | — |
| route audio modifiée pendant `RINGING` | lecture maintenue ou reprise | **non testé** — même raison | — | — |

### Redémarrage, Direct Boot et horloge

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard | Source |
| --- | --- | --- | --- | --- |
| redémarrage puis aucun déverrouillage | alarme reprogrammée depuis Direct Boot | **OK** — trois essais, permission OEM de démarrage automatique **refusée**. C'est la preuve que la promesse de §9.3 ne dépend pas d'un réglage OEM | — | `ETAPE-19.md` |
| redémarrage, permission OEM refusée | alarme reprogrammée ; consigner si la surcouche bloque le processus | **OK sur HyperOS, question ouverte ailleurs** — `LOCKED_BOOT_COMPLETED` et `BOOT_COMPLETED` sont **exemptés** de la restriction ; `MY_PACKAGE_REPLACED` y est **soumis** (l'alarme survit tout de même par le `PendingIntent`). Aucune généralisation possible à Oppo, Realme, Vivo ou Honor | — | `ETAPE-19.md`, §4.2 |
| redémarrage 2 minutes avant le réveil | alarme reprogrammée et déclenchée à l'heure | **OK** | — | `ETAPE-19.md` |
| redémarrage après l'heure, retard ≤ 15 min | sonnerie immédiate | **OK** | — | `ETAPE-19.md` |
| redémarrage après l'heure, retard > 15 min | `TRIGGERED_AWAITING_NFC`, `DEGRADED`, `MISSED_TRIGGER_WINDOW`, notification sans son avant déverrouillage | **OK** — essai à 19 min de retard, notification visible avant tout déverrouillage, sans son ni vibration ni plein écran | — | `ETAPE-19.md` |
| scan valide depuis `TRIGGERED_AWAITING_NFC` | notification retirée, `RELEASING` puis `COMPLETED` | **OK** | — | `ETAPE-19.md` |
| changement manuel d'heure | même `triggerAtEpochMillis`, politique de retard appliquée | **OK** | — | `ETAPE-19.md` |
| changement de fuseau | instant inchangé, affichage recalculé | **OK** | — | `ETAPE-19.md` |

### Activation refusée (§13)

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| notifications refusées | activation refusée | **OK** (2026-09-15) — permission refusée : `✗ Active les notifications pour que l'écran du réveil puisse s'afficher.` ; accordée : `✓ Notifications autorisées`. Aucun chemin vers le choix de l'heure tant qu'un contrôle est rouge | `ETAPE-21.md` |
| plein écran refusé | activation refusée sur Android 14+ | **OK** (2026-09-15) — refusé : `✗ Autorise les alarmes plein écran, sinon l'écran de réveil ne s'ouvrira pas tout seul au moment de sonner.` ; autorisé : `✓ Alarmes plein écran autorisées`. **Le refus doit être posé au niveau UID** (`appops set --uid`) : au seul niveau paquet, le mode UID reste `allow` et `canUseFullScreenIntent()` répond vrai | `ETAPE-21.md` |
| accessibilité désactivée avant activation | activation refusée | **OK** (2026-09-15) — inactif : `✗ Le service d'accessibilité de Niumi est inactif. Sans lui, les applications choisies ne seront pas bloquées.` ; actif : `✓ Service d'accessibilité actif`, service lié par le système | `ETAPE-21.md` |
| réglage changé depuis le volet rapide, écran ouvert (hors §20, écart 10) | écrans 2, 6, 7 et 12 à jour sans quitter l'écran | **OK** (2026-09-29) — écran 2 : NFC coupé et rallumé (trois allers-retours), Ne pas déranger « interruptions importantes » → avertissement « autre mode » puis retrait ; écran 6 : bouton grisé et « ton appareil n'est pas prêt » NFC coupé, rétabli NFC rallumé ; écran 7 (session armée) : encadré NFC, puis « Rétabli depuis. », **un seul** `NFC_DISABLED` en base, enregistré 130 ms après la coupure ; écran 12 : « NFC activé » en échec puis « OK ». Le retour du focus seul ne suffisait pas au rallumage : le NFC met ~1,4 s à s'allumer et le focus revenait avant 8 fois sur 9 ; l'annonce `ADAPTER_STATE_CHANGED` a porté la mise à jour dans ces cas. Panneau de volume : ne prend pas le focus, replié comme déplié (curseur « Alarme » déplacé de 10 à 12 puis 10, aucun changement de focus) — un volume changé ainsi n'est vu qu'au retour sur l'écran | `RELEASE_REPORT.md` (écart 10) |

Ces trois lignes étaient hors périmètre de l'étape 6 (`DeviceReadinessChecker` n'existait pas) et
n'avaient jamais été rejouées depuis. **Les trois sont soldées le 2026-09-15.** Dans les trois cas,
l'écran de diagnostic ne propose aucun chemin vers le choix de l'heure tant qu'un contrôle est
rouge, ce qui est la forme concrète du refus d'activation de §9.2.

### Écran 13 « Aide et limites » (étape 21)

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| ouverture depuis l'accueil, hors session | les limites lisibles de bout en bout | **OK** (2026-09-15) — quatre sections et seize limites restituées, comparées une à une à `LIMITES.md`. Trois limites mesurées à l'étape 25 portent le total à dix-neuf ; leur correspondance est verrouillée par `HelpTextsTest`, leur affichage n'a pas été relu à l'œil | `ETAPE-21.md`, `ETAPE-25.md` |
| actions offertes par l'écran | aucune (§3, §10.2) | **OK** — zéro nœud cliquable dans l'arbre d'accessibilité ; le geste Retour ramène à l'accueil | `ETAPE-21.md` |
| ouverture pendant une session active | idem | **non testé** — exige une session armée, donc le boîtier et le parcours complet | à exécuter |

### Blocage d'applications

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| ouverture d'une app bloquée (lanceur, tâche neuve) | retour immédiat à l'accueil et overlay | **OK** — < 1 s, vérifié par `dumpsys` | `ETAPE-05.md` |
| app bloquée ouverte depuis les récents | idem | **OK** | `ETAPE-05.md` |
| app bloquée ouverte par intent explicite | idem | **OK** | `ETAPE-05.md` |
| ouverture d'une app autorisée | aucun effet | **OK** | `ETAPE-05.md` |
| service d'accessibilité tué puis recréé | état rechargé, blocage restauré | **OK** — le service reconstruit sa projection depuis Room à la reconnexion | `ETAPE-05.md`, `ETAPE-15.md` |
| accessibilité désactivée pendant `ARMED` | incident détecté | **OK** — un seul incident `BLOCKING_PERMISSION_REVOKED` `CRITICAL`, session conservée `ARMED`, santé `DEGRADED`, déduplication confirmée | `ETAPE-20.md` |
| exemption d'énergie retirée pendant `ARMED` puis pendant l'attente du scan (C9, hors §20) | incident `ANDROID_BATTERY_EXEMPTION_REVOKED` `CRITICAL` et avertissement, un seul incident, « Rétabli depuis. » au retour | **OK** (2026-09-29, HyperOS OS3.0.302.0) — `ARMED` : exemption retirée à 19:12:30, Niumi rouvert, incident `CRITICAL` à 19:12:53, 173 ms après `SESSION_READINESS_DEGRADED`, avertissement 9 au texte exact, santé `DEGRADED`, session conservée ; bouton « Ouvrir les réglages de batterie » → liste système, « Sans restriction » → avertissement retiré, « Rétabli depuis. », un seul incident. `RINGING` (seconde session ; Android n'a pas d'`AWAITING_NFC` dans le parcours normal) : exemption retirée à 20:20:41, retour au premier plan, incident à 20:20:59, avertissement à côté de celui de la sonnerie ; scan → `COMPLETED`, avertissement retiré. Retrait fait par `cmd deviceidle whitelist -com.niumi.app`, rétablissement par l'interface. Détection par `PACKAGE_REPLACED` non mesurée | nul (au retour au premier plan) | `RESTE_A_FAIRE.md` |
| restriction d'énergie constructeur active (hors §20) | — | **Limite majeure mesurée** — sans exemption constructeur, le blocage devient **silencieusement** inopérant après ~60 s ; avec exemption, < 1 s. C'est le constat qui justifie le contrôle de §13 | `ETAPE-05.md` |

Le texte exact de l'overlay imposé par §12.2 n'est pas vérifiable par `dumpsys` : il reste un
contrôle visuel, fait à l'œil pendant les campagnes.

### Session, processus et fin de session

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| Niumi retiré des récents après armement | aucune carte à retirer pendant la session ; « Tout effacer » avec Niumi verrouillé : alarme et blocage conservés ; carte rendue à la fin | **OK avec parade** (2026-09-28). Sans parade, limite majeure mesurée deux fois (27/09 involontaire, 28/09 délibéré à 11:49:55) : HyperOS tue Niumi (`SwipeUpClean`, `adj 200` malgré le service lié) et ne relie plus le service d'accessibilité — blocage coupé, alarme conservée ; « Tout effacer » (`OneKeyClean`) tue aussi une tâche cachée (14:18) et un processus sans tâche (14:25). Le cadenas HyperOS ne protège pas d'un balayage individuel. **Parades livrées** : carte retirée des récents pendant la session (`setExcludeFromRecents`, drapeau vérifié dans `dumpsys`, carte rendue après le scan) ; verrou HyperOS exigé par le diagnostic (contrôle `RECENTS_LOCK`). « Tout effacer » avec verrou et carte cachée : Niumi vivant, service lié, Acrobat renvoyé à l'accueil (14:37, puis 14:39 après redémarrage, verrou conservé). Contrôle vérifié à 15:04–15:07 : vert en session, échec bloquant après retrait du cadenas, vert au retour. L'ancien « OK » de `LOT-0.md` ne portait que sur l'alarme, mesurée avec `am kill` | `ETAPE-25.md` |
| processus tué après armement | alarme conservée, état réconcilié | **OK** — `am kill` ne tue pas le processus quand le service d'accessibilité est lié ; `run-as … kill -9` y parvient. Alarme conservée, réconciliation à la relance | `LOT-0.md`, `ETAPE-20.md` |
| processus tué pendant `RINGING` | le watchdog réveille le processus, le son reprend | **OK** — son revenu à l'instant du tic déjà armé ; **cinq livraisons consécutives sous Doze profond forcé**, 58 à 62 s d'intervalle. Le quota Doze de 9 minutes ne s'applique pas (`exactAllowReason=policy_permission`) | `ETAPE-20.md` |
| fermeture de `AlarmActivity` | sonnerie maintenue | **OK** | `ETAPE-03.md` |
| verrouillage pendant la sonnerie | sonnerie maintenue | **OK** | `ETAPE-03.md` |
| scan du bon tag | `RELEASING`, arrêt et déblocage < 1 s, état final | **OK** — `COMPLETED`, watchdog annulé, projection Direct Boot supprimée | `ETAPE-04.md`, `ETAPE-20.md` |
| interruption pendant `RELEASING` | reprise des effets manquants | **couvert en instrumenté, non rejoué à la main** — `SessionCoordinatorReleaseTest` et la reprise de l'outbox | `ETAPE-18.md` |
| scan d'un autre tag | sonnerie maintenue | **non testé sur appareil** — un seul tag physique disponible. Le refus est couvert en JVM (payload et boîtier non associés) | `ETAPE-04.md` |
| NFC désactivé pendant la sonnerie | instruction de réactivation, sonnerie maintenue | **OK** — incident `NFC_DISABLED` `CRITICAL` unique, session toujours `RINGING` | `ETAPE-04.md`, `ETAPE-20.md` |
| NFC réactivé pendant la sonnerie | Reader Mode restauré, scan accepté | **OK** | `ETAPE-04.md` |
| scan sur écran verrouillé | déverrouillage demandé si l'appareil l'exige | **Limite constructeur confirmée** — le boîtier n'est lu qu'après déverrouillage sur cet appareil. `AlarmActivity` s'affiche bien par-dessus le verrouillage (§4.4) | `ETAPE-04.md` |

### Blocage différé (Lot 6)

Neuf lignes ajoutées à §20 le 2026-09-15, mesurées aux étapes 24 et 25 (17, 24, 25, 27 et 28
septembre). Permission OEM de démarrage automatique **refusée** pour toutes. Les retards sont
comptés depuis l'instant contractuel `blockingStartsAtEpochMillis` jusqu'à
`blockingAppliedAtEpochMillis`, lus dans la base de l'appareil ; le journal technique date
`BLOCKING_STARTED`.

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| heure de début atteinte, écran éteint depuis 30 minutes | blocage appliqué à l'heure, `BLOCKING_STARTED` journalisé, retard < 1 min | **OK** (2026-09-28) — écran éteint depuis 44 min, alarme de début délivrée à 11:30:00.007, `BLOCKING_STARTED` journalisé, santé inchangée. Comme l'essai équivalent du réveil : téléphone branché, donc **pas en Doze profond** (`deep=ACTIVE`) — valide « écran éteint », pas « 30 min en Doze », mesuré à part (dernière ligne) | +245 ms | `ETAPE-25.md` |
| application bloquée déjà au premier plan à l'heure de début | retour à l'accueil et overlay sans changement de fenêtre | **OK** — aucun blocage avant l'heure, retour à l'accueil sans changement de fenêtre, overlay présent ; mesuré par `tools/validate_blocking.sh --deferred` (2026-09-24) et à la main (2026-09-17) | 222 ms (sondage `dumpsys`), 308 ms | `ETAPE-24.md`, `ETAPE-25.md` |
| processus tué avant l'heure de début | alarme de début conservée, blocage appliqué à l'heure | **Limite établie** (2026-09-25) — alarme de début **conservée** (relance du processus à 14:30:00.049 pour elle), mais le blocage **n'est pas appliqué** : sur HyperOS, la mort du processus délie le service d'accessibilité, que le système ne relie plus avant réactivation manuelle. Niumi le détecte (incident `BLOCKING_PERMISSION_REVOKED`, avertissement) ; après réactivation, blocage appliqué en retard avec `MISSED_BLOCKING_START_WINDOW`. Mort provoquée par `run-as … kill -9` ; voir aussi la ligne « Niumi retiré des récents » | +24 min après réactivation | `ETAPE-25.md` |
| redémarrage avant l'heure de début, aucun déverrouillage | alarme de début reprogrammée depuis Direct Boot, blocage appliqué à l'heure après déverrouillage | **OK** — alarme de début reprogrammée 1 s après le démarrage, **au même instant**, avant tout déverrouillage (2026-09-24) ; variante plus dure le 2026-09-27 : téléphone **resté verrouillé** à l'heure de début, Niumi mort entre-temps, blocage appliqué pendant la phase verrouillée | +133 ms ; +465 ms verrouillé | `ETAPE-25.md` |
| redémarrage après l'heure de début et avant le réveil | blocage appliqué à la réconciliation, `MISSED_BLOCKING_START_WINDOW` au-delà de 15 minutes, réveil intact | **OK** (2026-09-28) — téléphone éteint à l'heure de début (9:55), rallumé 25 min après : blocage appliqué **pendant la phase verrouillée**, avant le déverrouillage, incident `MISSED_BLOCKING_START_WINDOW` (`WARNING`, santé inchangée), réveil reprogrammé et sonné à l'heure (+99 ms) | 25 min 55 s | `ETAPE-25.md` |
| changement manuel d'heure entre l'activation et le début | même `blockingStartsAtEpochMillis` réenregistré, un seul incident `TIME_CHANGED` | **OK** (2026-09-27) — horloge reculée de 5 min puis rendue à l'heure automatique : les deux alarmes reprogrammées sans condition, **même epoch** ; un seul `TIME_CHANGED` sur les deux changements ; blocage appliqué à l'heure ensuite | +162 ms | `ETAPE-25.md` |
| scan avant l'heure de début | `RELEASING` puis `CANCELLED`, alarme de début annulée, aucune application jamais bloquée | **OK** (2026-09-17) — `CANCELLED`, plus aucune alarme Niumi en attente | — | `ETAPE-24.md` |
| service d'accessibilité désactivé avant l'heure de début | incident `BLOCKING_PERMISSION_REVOKED`, session conservée, blocage appliqué à l'heure si le service est réactivé avant | **OK** (2026-09-25) — incident et avertissement, session conservée `ARMED` ; service réactivé avant l'heure → blocage appliqué à l'heure, avertissement retiré par le processus suivant | +44 ms | `ETAPE-25.md` |
| Doze forcé à l'heure de début | alarme de début délivrée à l'heure, comme le watchdog de §4.2 | **OK** (2026-09-24) — Doze profond (`deep=IDLE`), téléphone débranché, adb par Wi-Fi ; `exactAllowReason=policy_permission`. **La seconde dérogation de §9.1 tient** sur cet appareil. Réserve : point d'accès Wi-Fi du téléphone actif pendant l'essai | +77 ms (réception), +91 ms (blocage) | `ETAPE-25.md` |

### Arrêts système et corruption

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| arrêt du FGS depuis le système | limite connue consignée | **non reproductible sur cette surcouche** — le gestionnaire des services actifs d'AOSP est absent d'HyperOS. À rejouer sur un Pixel | `LOT-0.md` |
| arrêt forcé de Niumi | alarme annulée par le système, limite consignée | **OK, limite confirmée** — après `force-stop`, plus aucune alarme en attente, l'heure cible passe sans déclenchement. Rejoué par le geste utilisateur réel, pendant une sonnerie : même résultat | `LOT-0.md` |
| `niumi_session.json` corrompu, Room valide | incident unique, projection réécrite, session conservée | **OK** — `SNAPSHOT_CORRUPTED` journalisé, incident `CRITICAL` unique, projection réécrite depuis Room (0 o → 1667 o), session toujours `ARMED` | `ETAPE-20.md` |
| base Room corrompue, appareil déverrouillé | diagnostic affiché, blocage conservé, aucune session perdue | **OK après correction** — deux défauts trouvés sur appareil et corrigés le jour même ; 16 exceptions rattrapées, alarme conservée, écran « État illisible », session **intégralement retrouvée** à la restauration des droits | `ETAPE-20.md` |

## Build de publication

| Scénario | Résultat attendu | État |
| --- | --- | --- |
| session complète sur APK **release signé** (R8, ressources réduites) | parcours identique au debug | **non testé** — exige le keystore d'upload, à créer par l'utilisateur. C'est la dernière vérification matérielle de l'étape 21, et la seule qui puisse révéler un effet de R8 sur la sérialisation du snapshot Direct Boot |
| redémarrage sans déverrouillage, en release | alarme restaurée | **non testé** — même raison |

Toutes les campagnes des étapes 3 à 20 ont porté sur la variante **debug**. R8 et la suppression de
ressources n'ont jamais tourné sur un appareil : le build passe (étape 21) et les règles
consommateur de Hilt, Room et kotlinx-serialization sont appliquées, mais aucun essai ne le prouve
en fonctionnement.

## Synthèse

| | Lignes |
| --- | --- |
| Mesurées et conformes | 35 (dont 8 des 9 lignes du blocage différé, et le retrait des récents, conforme avec parade) |
| Mesurées, limite établie et documentée | 5 (silence total, arrêt forcé, NFC verrouillé, restriction d'énergie constructeur, **processus tué avant l'heure de début** — mesurée à l'étape 25 : sur HyperOS, le service d'accessibilité n'est plus relié après la mort du processus) |
| Non reproductibles sur cet appareil | 1 (arrêt du seul FGS) |
| Non testées faute d'appareil, de matériel audio ou de second tag | 12 |
| Non testées sur le build release | 2 |

Les campagnes qui restent à mener pour combler ces lignes sont détaillées dans
`docs/android/RESTE_A_FAIRE.md`, section B.

**La matrice n'est pas verte au sens de §21.** Elle l'est sur un appareil, une surcouche et une
version d'Android. Les deux manques qui pèsent le plus sur la promesse du produit : aucun Pixel ni
Samsung, et aucun essai sur un artefact de publication.
