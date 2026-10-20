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
| Xiaomi (Redmi) | 25080RABDG (`lapis`) | 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM | `BP2A.250605.031.A3` | **couvert**, campagnes des 7-8, 13, 14, 15, 16, 17, 24, 25, 27 et 28 septembre, 1er, 2, 7, 8, 13, 15, 17 et 18 octobre 2026 |
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
couvert. Le plancher `minSdk 29` (Android 10), les alarmes exactes d'Android 12/13, le rejet des
notifications de service au premier plan d'Android 14 (l'accès plein écran n'est plus demandé depuis
le Lot 9), l'arrêt forcé d'Android 15 et l'audio d'arrière-plan d'Android 17 n'ont jamais été
exécutés sur l'appareil correspondant. Trois de ces comportements ont pourtant été observés
indirectement sur Android 16, qui les inclut tous : ils sont notés comme tels ci-dessous, sans être
comptés comme une couverture de version.

## Permissions au moment des essais

Sauf mention contraire dans une ligne : `NFC` accordée, `POST_NOTIFICATIONS` accordée,
`USE_FULL_SCREEN_INTENT` en `allow` jusqu'à l'étape 31 (permission retirée au Lot 9, plus
déclarée depuis l'étape 32), exemption d'énergie AOSP active, exemption de la restriction
d'énergie **constructeur** active (sans elle le blocage est inopérant après ~60 s, étape 5),
service d'accessibilité activé à la main, NFC du téléphone actif.

**Permission OEM « Démarrage automatique en arrière-plan » : refusée** (état par défaut) pour tous
les essais de redémarrage, ce qui est la seule façon de mesurer ce que vit un utilisateur ordinaire.
Elle avait été laissée accordée à l'issue de l'étape 19 et a été remise à « refusé » avant les
essais qui en dépendent.

## Scénarios de §20

Les 66 scénarios du tableau de §20 — dont les neuf du blocage différé (Lot 6), ajoutés le 2026-09-15, les onze des sonneries (Lot 7), ajoutés à l'étape 26, et les cinq des boîtiers multiples (Lot 8), ajoutés à l'étape 31 —, dans son ordre. « Source » renvoie au rapport qui porte les
mesures détaillées, dans `docs/android/implementation-reports/`.

### Réveil, audio et Doze

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| écran éteint depuis 30 minutes | sonnerie à l'heure, notification de sonnerie lisible sur l'écran de verrouillage (Lot 9) | **OK** — écran éteint 31 min, sonnerie à l'heure (`LOT-0.md`, avec l'écran imposé d'alors, USB branché). **Remesuré avec le Lot 9 le 18/10 (C10)** : armé 18:47:21, ~37 min d'écran éteint et verrouillé, USB débranché, téléphone posé ; Doze léger entré à 19:20:03 (Doze profond resté en attente, état 2) ; 19:25:00,003 `device_idle_wake_from_idle` sur l'alarme de Niumi, `RINGING_STARTED` 19:25:00,49, notification publiée et alertée 19:25:00,32 ; écran resté éteint ; allumé par l'utilisateur 19:25:28, notification lisible sur l'écran de verrouillage ; tap → écran 8 → scan, `COMPLETED` 19:25:40, aucun incident | 1 s ; 0,5 s | `LOT-0.md`, `ETAPE-32.md` |
| Doze forcé | alarme à l'heure | **OK** — Doze profond réel, appareil débranché et immobile (`deep=IDLE` pendant 21 min), cible 19:06:42, déclenchement 19:06:41. Réserve : l'appareil était sorti du Doze depuis ~7 min à l'échéance ; `FLAG_WAKE_FROM_IDLE` reste la garantie système pour un déclenchement *pendant* `IDLE` | nul | `LOT-0.md` |
| mode économie d'énergie | alarme à l'heure dans le périmètre pris en charge | **non testé** — non exécuté sur cet appareil | — | — |
| Ne pas déranger autorisant les alarmes | sonnerie audible | **OK** — `zen_mode=1`, écran rallumé, `AlarmActivity` au premier plan (écran imposé d'avant le Lot 9), focus `USAGE_ALARM`. Audibilité non confirmée à l'oreille sur cet essai précis : le son n'est prouvé qu'indirectement | nul | `LOT-0.md` |
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
| accessibilité désactivée avant activation | activation refusée | **OK** (2026-09-15) — inactif : `✗ Le service d'accessibilité de Niumi est inactif. Sans lui, les applications choisies ne seront pas bloquées.` ; actif : `✓ Service d'accessibilité actif`, service lié par le système | `ETAPE-21.md` |
| réglage changé depuis le volet rapide, écran ouvert (hors §20, écart 10) | écrans 2, 6, 7 et 12 à jour sans quitter l'écran | **OK** (2026-09-29) — écran 2 : NFC coupé et rallumé (trois allers-retours), Ne pas déranger « interruptions importantes » → avertissement « autre mode » puis retrait ; écran 6 : bouton grisé et « ton appareil n'est pas prêt » NFC coupé, rétabli NFC rallumé ; écran 7 (session armée) : encadré NFC, puis « Rétabli depuis. », **un seul** `NFC_DISABLED` en base, enregistré 130 ms après la coupure ; écran 12 : « NFC activé » en échec puis « OK ». Le retour du focus seul ne suffisait pas au rallumage : le NFC met ~1,4 s à s'allumer et le focus revenait avant 8 fois sur 9 ; l'annonce `ADAPTER_STATE_CHANGED` a porté la mise à jour dans ces cas. Panneau de volume : ne prend pas le focus, replié comme déplié (curseur « Alarme » déplacé de 10 à 12 puis 10, aucun changement de focus) — un volume changé ainsi n'est vu qu'au retour sur l'écran | `RELEASE_REPORT.md` (écart 10) |

Ces trois lignes étaient hors périmètre de l'étape 6 (`DeviceReadinessChecker` n'existait pas) et
n'avaient jamais été rejouées depuis. **Les trois sont soldées le 2026-09-15.** Dans les trois cas,
l'écran de diagnostic ne propose aucun chemin vers le choix de l'heure tant qu'un contrôle est
rouge, ce qui est la forme concrète du refus d'activation de §9.2.

### Écran 13 « Aide et limites » (étape 21)

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| ouverture depuis l'accueil, hors session | les limites lisibles de bout en bout | **OK** (2026-09-15) — quatre sections et seize limites restituées, comparées une à une à `LIMITES.md`. Trois limites mesurées à l'étape 25 portent le total à dix-neuf, puis la section « Sonnerie » et ses deux limites mesurées à l'étape 28 à cinq sections et vingt et une limites ; leur correspondance est verrouillée par `HelpTextsTest`, leur affichage n'a pas été relu à l'œil | `ETAPE-21.md`, `ETAPE-25.md` |
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
| processus tué pendant `ARMED`, **autre application d'accessibilité active** (hors §20) | incident `BLOCKING_PERMISSION_REVOKED` et avertissement au retour de Niumi | **OK après correction** (2026-10-16) — le 15/10, avec « Brick App Blocker » actif, l'interrupteur global `accessibility_enabled` restait à 1 et Niumi délié était lu « actif » : aucun incident. Après correction (liaison lue dans le processus) : `am crash`, Niumi dans « Crashed services », interrupteur toujours à 1 ; incident `CRITICAL`, avertissement et notification 30,7 s après la réouverture ; « Rétabli depuis » à la réactivation ; au redémarrage, liaison 5,9 s après le déverrouillage sans faux incident. Niumi fermé : rien avant la prochaine relance du processus | `ETAPE-28.md` |
| restriction d'énergie constructeur active (hors §20) | — | **Limite majeure mesurée** — sans exemption constructeur, le blocage devient **silencieusement** inopérant après ~60 s ; avec exemption, < 1 s. C'est le constat qui justifie le contrôle de §13 | `ETAPE-05.md` |

Le texte exact de l'overlay imposé par §12.2 n'est pas vérifiable par `dumpsys` : il reste un
contrôle visuel, fait à l'œil pendant les campagnes.

### Session, processus et fin de session

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| Niumi retiré des récents après armement | aucune carte à retirer pendant la session ; « Tout effacer » avec Niumi verrouillé : alarme et blocage conservés ; carte rendue à la fin | **OK avec parade** (2026-09-28). Sans parade, limite majeure mesurée deux fois (27/09 involontaire, 28/09 délibéré à 11:49:55) : HyperOS tue Niumi (`SwipeUpClean`, `adj 200` malgré le service lié) et ne relie plus le service d'accessibilité — blocage coupé, alarme conservée ; « Tout effacer » (`OneKeyClean`) tue aussi une tâche cachée (14:18) et un processus sans tâche (14:25). Le cadenas HyperOS ne protège pas d'un balayage individuel. **Parades livrées** : carte retirée des récents pendant la session (`setExcludeFromRecents`, drapeau vérifié dans `dumpsys`, carte rendue après le scan) ; verrou HyperOS exigé par le diagnostic (contrôle `RECENTS_LOCK`). « Tout effacer » avec verrou et carte cachée : Niumi vivant, service lié, Acrobat renvoyé à l'accueil (14:37, puis 14:39 après redémarrage, verrou conservé). Contrôle vérifié à 15:04–15:07 : vert en session, échec bloquant après retrait du cadenas, vert au retour. L'ancien « OK » de `LOT-0.md` ne portait que sur l'alarme, mesurée avec `am kill` | `ETAPE-25.md` |
| Niumi balayé depuis les récents ouverts **depuis Niumi** pendant la session | carte absente | **Limite établie** (2026-10-19), **levée le 2026-10-20** par le service de garde de session (SPEC_ANDROID §12.5) — sans lui : la tâche la plus récente reste affichée malgré l'exclusion (règle AOSP), balayage → `SwipeUpClean`, service d'accessibilité en panne ; retirer la tâche à la sortie ne protège pas (cache du lanceur, kill par paquet) | `ETAPE-33.md`, `A8-RECENTS.md` |
| Carte de Niumi balayée pendant la session, avec le service de garde (2026-10-19 et 20) | processus et service d'accessibilité intacts, blocage appliqué | **OK** — pendant le blocage, notification affichée (19:01) ou écartée (19:04) ; avant le coucher (20/10, 12:27), blocage appliqué à 12:30 et Acrobat renvoyé à l'accueil (12:34:33) ; service arrêté au scan (12:35:34) ; relancé seul après une mort du processus (19/10, 18:52) et après un redémarrage, avant le déverrouillage (20/10, 12:49:55), blocage effectif après déverrouillage (12:53:54). « Tout effacer » sans verrou : **tué malgré le service** (19/10, 18:49), le verrou reste exigé | `A8-RECENTS.md` |
| Carte de Niumi balayée hors session | rien | **Limite établie** (2026-10-20, 09:06) — processus tué, service d'accessibilité en panne jusqu'à sa réactivation ; le diagnostic le signale avant l'activation suivante | `A8-RECENTS.md` |
| processus tué après armement | alarme conservée, état réconcilié | **OK** — `am kill` ne tue pas le processus quand le service d'accessibilité est lié ; `run-as … kill -9` y parvient. Alarme conservée, réconciliation à la relance | `LOT-0.md`, `ETAPE-20.md` |
| processus tué pendant `RINGING` | le watchdog réveille le processus, le son reprend | **OK** — son revenu à l'instant du tic déjà armé ; **cinq livraisons consécutives sous Doze profond forcé**, 58 à 62 s d'intervalle. Le quota Doze de 9 minutes ne s'applique pas (`exactAllowReason=policy_permission`) | `ETAPE-20.md` |
| fermeture de `AlarmActivity` | sonnerie maintenue | **OK** | `ETAPE-03.md` |
| verrouillage pendant la sonnerie | sonnerie maintenue | **OK** | `ETAPE-03.md` |
| scan du bon tag | `RELEASING`, arrêt et déblocage < 1 s, état final | **OK** — `COMPLETED`, watchdog annulé, projection Direct Boot supprimée | `ETAPE-04.md`, `ETAPE-20.md` |
| interruption pendant `RELEASING` | reprise des effets manquants | **couvert en instrumenté, non rejoué à la main** — `SessionCoordinatorReleaseTest` et la reprise de l'outbox | `ETAPE-18.md` |
| scan d'un autre tag | sonnerie maintenue | **OK** (17/10) — tag Niumi valide hors de la liste figée, scanné pendant la sonnerie : `NFC_SCAN_INVALID`, vibration d'erreur ressentie, sonnerie maintenue jusqu'au scan d'un boîtier figé 21 s plus tard (ligne Lot 8 n° 1). Jusqu'au 2026-10-17, un seul tag physique était disponible | `ETAPE-04.md`, `ETAPE-31.md` |
| NFC désactivé pendant la sonnerie | instruction de réactivation, sonnerie maintenue | **OK** — incident `NFC_DISABLED` `CRITICAL` unique, session toujours `RINGING` | `ETAPE-04.md`, `ETAPE-20.md` |
| NFC réactivé pendant la sonnerie | Reader Mode restauré, scan accepté | **OK** | `ETAPE-04.md` |
| scan sur écran verrouillé | déverrouillage demandé si l'appareil l'exige | **Limite constructeur confirmée** — le boîtier n'est lu qu'après déverrouillage sur cet appareil (étape 4, quand `AlarmActivity` s'affichait encore par-dessus le verrouillage). Depuis le Lot 9, l'écran n'est plus affiché verrouillé : le scan demande un téléphone déverrouillé (§4.4, aide) | `ETAPE-04.md`, `ETAPE-32.md` |

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

### Sonneries et volume progressif (Lot 7)

Onze lignes ajoutées à §20 à l'étape 26, mesurées aux étapes 26, 27 et 28 (1er, 2, 7, 8, 13 et
15 octobre 2026). La montée est mesurée exactement : le logcat du Xiaomi journalise chaque
`AudioTrack::setVolume` de la piste Niumi, comparé à `10^(−2·(1−p))` depuis
`ringingAtEpochMillis` ; la boucle par les arrêts et redémarrages de la piste. L'audibilité et la
perception de la montée restent à l'oreille de l'utilisateur. **Depuis le 2026-10-15, le réveil
joue par un `AudioTrack` statique à points de boucle** (SPEC_ANDROID §10.2) : les essais antérieurs
portaient sur le lecteur `MediaPlayer`, ce que la colonne précise.

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| chacune des quatre sonneries, session à +2 min, écran éteint | audible, en boucle sur trois cycles sans blanc ni clic, jusqu'au scan ; aucun arrêt/redémarrage de piste | **Partiel.** « Oiseaux », lecteur `AudioTrack` (15/10) : **une seule piste, aucun arrêt sur ~21 reprises, aucune coupure à l'oreille**, aucun `AUDIO_LOOP_FALLBACK`. Avec l'ancien lecteur `MediaPlayer` : « Piano » (15,19–15,24 s pour 15,00 s), « Cloche » (7,29–7,43 s pour 7,22 s), « Énergique » (16,09–16,25 s pour 16,00 s) sans blanc audible ; « Oiseaux » **coupure audible** (10,07–10,43 s pour 10,00 s, 96 cycles), d'où le changement de lecteur. Piano, Cloche et Énergique **non réécoutées avec `AudioTrack`** | T+79 à T+495 ms | `ETAPE-26.md`, `ETAPE-28.md` |
| montée progressive 2 min, puis 15 min | départ presque inaudible, plein volume à la durée choisie, puis constant | **OK** — 2 min : plein volume à 120,04 s (07/10), 120,17 s (15/10) ; 15 min : 3723 points, ≤ 0,3 % de la courbe, plein volume à 900,12 s (15/10). Perçue progressive. Un redémarrage de la montée observé une fois le 08/10, **non reproduit** sur deux sessions de 15 min (13 et 15/10) | T+108 ms (2 min), T+186 ms (15 min) | `ETAPE-28.md` |
| volume constant | plein volume dès la première seconde | **OK** — « Cloche », 60/60 `setVolume` à 1,0 (07/10) | T+243 ms | `ETAPE-28.md` |
| réglage par défaut, jamais modifié | « Piano », départ presque inaudible, plein volume vers 2 min | **OK** — `niumi_piano` / 120 s lus dans la projection, 488 points, ≤ 0,3 % de la courbe (07/10) | T+108 ms | `ETAPE-28.md` |
| mort du processus pendant la montée (`am crash`) | la montée reprend à l'amplitude du temps écoulé, jamais depuis zéro | **OK** (15/10) — « Oiseaux », 5 min, crash à T+60,3 s : processus relancé en 70 ms (écran de réveil au premier plan), son revenu à T+62,30 s à **0,02592 pour 0,02595 attendu**, plein volume à 300,20 s, ≤ 0,13 % de la courbe ; « court silence, même niveau » à l'oreille. Service d'accessibilité délié ensuite (limite de l'étape 25) | silence ~1,6 s | `ETAPE-28.md` |
| redémarrage sans déverrouillage avant l'heure, sonnerie autre que « Piano » | la sonnerie choisie sonne, avec sa montée, depuis la projection Direct Boot v3 | **OK** (15/10) — `RUNNING_LOCKED` à l'heure, « Oiseaux » / 120 s joué, alarme reprogrammée avant tout déverrouillage. État de la permission OEM de démarrage automatique non relevé | T+495 ms | `ETAPE-28.md` |
| mise à jour par-dessus une session armée en `niumi_alarm` | « Piano » à volume constant, aucun `RINGTONE_FALLBACK` | **OK** (01/10) — base v3 → v4 au premier accès, `niumi_piano` / `NULL`, un seul `RINGING_STARTED`, aucun `RINGTONE_FALLBACK` ; « Piano et le son n'augmente pas » à l'oreille | T+205 ms | `ETAPE-26.md` |
| sonnerie modifiée pendant `ARMED`, puis redémarrage sans déverrouillage | la nouvelle sonnerie sonne, avec sa nouvelle montée | **OK** (15/10) — « Énergique » / 5 min armé, modifié en « Oiseaux » / 2 min depuis l'écran 7, projection relue, redémarrage : « Oiseaux » / 120 s joué verrouillé | T+495 ms | `ETAPE-28.md` |
| Ne pas déranger « alarmes seules », montée progressive | montée audible ; non-régression de la ligne DND | **OK** (07/10) — `zen_mode` alarmes seules, `STREAM_ALARM` non muté, montée conforme à 2 % près, audible | T+79 ms | `ETAPE-28.md` |
| casque Bluetooth connecté, montée progressive | flux dupliqué, montée sur les deux sorties | **OK** (07/10) — `speaker(2), bt_a2dp(80)`, montée mesurée, confirmée à l'oreille | T+108 ms | `ETAPE-28.md` |
| pré-écoute avec volume d'alarme à zéro | message affiché, aucun son, aucun lecteur créé | **OK** (02/10) — volume borné à 1 sur cet appareil : obtenu par le silence total ; message affiché, boutons inactifs, aucun `requestAudioFocus`. Annexe (15/10) : média à 0, alarme à 4/15 → pré-écoute **audible**, lecteur `STREAM_ALARM` | — | `ETAPE-27.md`, `ETAPE-28.md` |

TalkBack sur l'écran 14 (contrôle du plan, hors §20) : **non testé**, passé par choix de
l'utilisateur le 2026-10-15.

### Boîtiers multiples (Lot 8)

Cinq lignes ajoutées à §20 à l'étape 31 — quatre du Lot 8 et une hors Lot 8 (faux « NFC désactivé »
des écrans 8 et 9, révélé par l'essai 2 de l'étape 30) —, mesurées les 17 et 18 octobre 2026 avec six
tags NTAG portant chacun un `boxId` et un token distincts. États lus dans Room (`niumi.db` avec son
WAL), chronologies NFC dans le logcat système (`NfcService` : `enableInternal`, `disableInternal`),
verrouillage par `dumpsys trust` et `dumpsys user`. La preuve NFC ne quittant jamais la mémoire, le
`boxId` reconnu n'est lisible ni dans le journal ni dans Room : sur appareil, c'est le tag scanné et
l'issue de la session qui font foi.

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| deux boîtiers associés, session armée, scan du second | `RELEASING` puis état final ; la preuve porte le `boxId` du second (JVM) | **OK** (17/10) — liste figée « Entrée », « Boîtier 1 » (`session_box`, positions 0 et 1) ; sonnerie 18:59:00, tag hors liste refusé à 18:59:20, tag du second boîtier accepté à 18:59:41 : `RELEASING` puis `COMPLETED`, révision 5, aucun incident. Écrans 6 et 7 : « Seul le scan d'un de tes boîtiers terminera la session. » | — | `ETAPE-31.md` |
| boîtier ajouté après l'activation, scanné pendant la session | refusé (vibration d'erreur), session maintenue ; accepté à la session suivante | **Partiel.** (17/10) — pendant la session : refus avec vibration, sonnerie maintenue. Ajouté ensuite (« Boîtier 2 »), session suivante annulée par son scan depuis l'écran 9 à 19:02:16 (`CANCELLED`, révision 4). **Réserve** : l'identité du tag refusé n'a pas été relevée ; il était hors liste, mais peut-être distinct de celui ajouté ensuite. L'ajout pendant une session est par ailleurs impossible (écran 3 fermé, §11.1) | — | `ETAPE-31.md` |
| mise à jour par-dessus une session armée du Lot 7 | le boîtier d'origine termine la session, nommé « Boîtier 1 » à l'écran 6 de la session suivante | **Partiel.** — mise à jour et scan mesurés à l'étape 30 (17/10, essai 1, sans redémarrage) ; le 18/10, après la mise à jour de l'essai suivant, la session d'après fige « Boîtier 1 » (`session_box`), nom que l'écran 6 lit dans le même dépôt ; l'affichage lui-même n'a pas été confirmé de vive voix | — | `ETAPE-30.md`, `ETAPE-31.md` |
| redémarrage sans déverrouillage après la mise à jour, projection v3 | le boîtier d'origine termine la session après déverrouillage | **OK** (18/10) — APK `4a03d53` (base v4, projection v3), session armée, APK de l'étape 31 par-dessus sans ouvrir l'application, redémarrage : `RUNNING_LOCKED`, alarme reprogrammée et sonnerie à 09:16:00 depuis la projection v3 ; déverrouillage 09:17:49,7 ; base migrée en v5 au premier accès, `session_box` = « Boîtier 1 » ; scan du boîtier d'origine 09:19:19 → `COMPLETED`, `HEALTHY`, aucun incident | T+0,4 s (sonnerie) | `ETAPE-31.md` |
| redémarrage sans déverrouillage, écran de sonnerie observé après le déverrouillage (hors Lot 8) | un éventuel « NFC désactivé » disparaît de lui-même, sans quitter l'écran ni réessayer ; `session_incident` vide ; même résultat sur l'écran 9 | **OK, avec réserve** — écran 8 (18/10) : « NFC désactivé » à la réouverture (09:17:53,9), retiré seul à l'allumage du NFC (09:17:58,4), **réaffiché 2 s** pendant que HyperOS éteint et relance réellement le NFC (09:18:05,2 → 09:18:07,1), puis retiré ; scan accepté sans quitter l'écran ; `NFC_DISABLED` journalisé une fois, pas à l'extinction ; aucun incident. Écran 9 (18/10) : même relance du NFC (09:23:33,1 → 09:23:34,8), message affiché ~1 s puis retiré seul, scan accepté (`CANCELLED`). **Réserve** : un incident `TIME_CHANGED` (`WARNING`) est consigné pour cette seconde session, horloge resynchronisée au démarrage — sans rapport avec le NFC, mais `session_incident` n'est pas vide | NFC prêt 8,6 et 8,3 s après le déverrouillage, stable à 17,4 et 17,1 s | `ETAPE-31.md` |

Écran 3 (contrôle du plan, hors §20) : ajout de cinq boîtiers nommés, renommage, retrait, nom
proposé au plus petit `n` libre, doublon refusé sans dialogue, « 5 boîtiers maximum. » à la place
du bouton, tag approché hors lecture sans effet — **OK** (17/10), base et `uiautomator dump`
(descriptions « Renommer {nom} » / « Retirer {nom} », préfixes de huit caractères). TalkBack vocal
**non écouté** ; la taille de la liste annoncée est prouvée par `PairingScreenTest`, pas sur
appareil.

### Réveil sans plein écran (Lot 9)

Sept lignes ajoutées à §20 à l'étape 32, mesurées le 18 octobre 2026 (après-midi, 11:35 à 17:55)
sur l'APK de debug issu de l'arbre de travail de l'étape, `6c722ef` plus les changements du Lot 9.
Sources des relevés : `dumpsys notification --noredact` à la sonnerie (`vis=PUBLIC`,
`fullscreenIntent=null`, `contentIntent` présent, drapeaux `ONGOING_EVENT|ONLY_ALERT_ONCE|NO_CLEAR|
FOREGROUND_SERVICE`), le journal d'événements du système (`logcat -b events` : `notification_enqueue`,
`notification_alert`, `notification_visibility`, `notification_canceled`, `notification_clicked`),
`dumpsys trust` et `dumpsys power` toutes les 0,3 s (verrouillage, allumage de l'écran), l'historique
de `dumpsys nfc` (`reader_mode_change`) et le journal technique de Niumi (Room). Réglage HyperOS
« Notifications sur l'écran de verrouillage » **activé par défaut** pour Niumi, non modifié.
`appops get com.niumi.app` ne liste plus `USE_FULL_SCREEN_INTENT` parmi les permissions déclarées
(`dumpsys package`), seul le mode `default` générique reste affiché.

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Retard mesuré | Source |
| --- | --- | --- | --- | --- |
| écran éteint et verrouillé par code, sonnerie | son et vibration à l'heure ; aucune activité ouverte ; notification lisible sur l'écran de verrouillage ; déverrouillage, tap → écran 8 → scan accepté | **OK** (11:35) — service démarré à 11:35:01,4 ; accueil HyperOS resté l'activité au premier plan, `isKeyguardShowing=true`, `deviceLocked=1`, `mWakefulness=Dozing` pendant 67 s : **l'écran ne s'est pas allumé seul**. Notification publique sans `fullscreenIntent`. Allumage par l'utilisateur à 11:36:08, notification lisible, tap depuis l'écran de verrouillage → code → `AlarmActivity` à 11:36:12, scan accepté du premier coup, `COMPLETED` 11:36:25 | 1,4 s | `ETAPE-32.md` |
| notification de sonnerie balayée, téléphone verrouillé puis déverrouillé | republiée dans les 10 s, sans ouvrir aucun écran ; tap → écran 8 | **OK** (17:14, refaite 17:30) — **sur l'écran de verrouillage, la notification ne se balaie pas** (HyperOS). Déverrouillé, balayage dans le volet à 17:15:05,06 → republiée à 17:15:10,20 (**5,1 s**), aucun écran ouvert (accueil resté au premier plan) ; tap 17:16:59 → écran 8 → scan 17:17:02. Refaite avec `setOnlyAlertOnce` : balayage 17:32:44,24 → retour 17:32:50,37 (**6,1 s**) avec bannière ; second balayage 17:32:59,22 → retour 17:33:00,35 par le tic de la minute, bannière visible 5,5 s | 5,1 s et 6,1 s | `ETAPE-32.md` |
| réveil pendant l'usage d'une autre application | bannière « heads-up », aucun écran imposé ; tap → écran 8 | **OK** (17:14) — bannière visible de 17:14:01,2 à 17:14:06,8 (**5,5 s**), accueil HyperOS resté au premier plan ; **constat** : sans `setOnlyAlertOnce`, la republication de l'alarme de secours re-présentait la bannière à chaque minute pleine (17:15:00, 17:16:00, 17:17:00, `notification_alert` avec vibration), y compris par-dessus l'écran de réveil une seconde après son ouverture. Décision du 18/10 : `setOnlyAlertOnce(true)` ; remesuré 17:30 à 17:35 : alerte au départ (17:30:00), **aucune** aux republications de 17:31, 17:32, 17:34, 17:35 (`buzz=0`), alerte de nouveau après chaque balayage | — | `ETAPE-32.md` |
| verrouillage pendant l'écran 8, puis déverrouillage | l'écran 8 revient, Reader Mode actif, scan accepté | **OK** (11:36) — verrouillage volontaire à 11:36:16 pendant l'écran 8 ; déverrouillage 11:36:22, `AlarmActivity` de nouveau au premier plan sans geste, scan accepté à 11:36:23 (Reader Mode actif) | — | `ETAPE-32.md` |
| redémarrage sans déverrouillage avant l'heure | sonnerie à l'heure depuis la projection Direct Boot, notification sur l'écran de verrouillage, scan après déverrouillage et tap | **OK** (17:47 → 17:55) — `adb reboot` 17:47:26, `boot_completed` 17:48:06, `ALARM_RESCHEDULED` 17:48:08 depuis la projection v4, utilisateur `RUNNING_LOCKED` jusqu'à 17:53:23 ; `RINGING_STARTED` 17:52:00,3, notification publiée depuis le contexte protégé, écran resté éteint ; allumage par l'utilisateur 17:53:16, notification lisible sur l'écran de verrouillage. Fin par l'icône (ligne suivante) et non par le tap, déjà prouvé deux fois | 0,3 s | `ETAPE-32.md` |
| silence total pendant la sonnerie | comportement consigné sans fausse garantie (présentation de la notification) | **Limite établie** (17:36 → 17:44) — silence total posé par `cmd notification set_dnd none` pendant `ARMED` (`zen_mode=2`, `mInterruptionFilter=3`) : incidents `ANDROID_ALARM_MUTED_BY_DND` et `ANDROID_ALARM_VOLUME_ZERO` à 17:36:07 avec leurs avertissements (identifiants 6 et 7, retirés à 17:43:41 à la sortie d'`ARMED`). À 17:39:00 : service démarré, lecteur `USAGE_ALARM` lancé mais `STREAM_ALARM` `Muted: true` : **ni son, ni vibration** ; notification publiée, **non alertée** (`notification_alert` sans son ni vibration) et **invisible sur l'écran de verrouillage** (constat de l'utilisateur) ; le volet après déverrouillage n'a pas été regardé. Seul chemin : Niumi déjà ouvert avant le verrouillage, sa tâche est revenue sur l'écran 8 au déverrouillage (17:43:41) ; scan 17:44:05. Mode Ne pas déranger rétabli à `off` ensuite | — | `ETAPE-32.md` |
| Niumi ouvert par son icône pendant la sonnerie | écran 8 (non-régression) | **OK** (17:53) — après le redémarrage ci-dessus, tâche Niumi inexistante : icône du lanceur à 17:53:26 → `MainActivity` → redirection → `AlarmActivity` à 17:53:27 (`LAUNCH_SINGLE_TASK`), sans passer par l'accueil. « Le NFC est désactivé… » affiché quelques secondes puis effacé seul (`NFC_DISABLED` 17:53:27, pile NFC `ON` 17:53:37, Reader Mode de Niumi activé 17:53:33 et 17:53:37 d'après `dumpsys nfc`) ; premier scan passé trop vite refusé 17:54:35 (`NFC_SCAN_INVALID`, « Boîtier non reconnu. Réessaie. »), second accepté 17:54:52 | — | `ETAPE-32.md` |

Contrôles hors §20 : `dumpsys notification` conforme à chaque sonnerie (ci-dessus) ; le manifeste
fusionné release ne déclare plus `USE_FULL_SCREEN_INTENT` (huit permissions, `ReleaseHygieneTest`) ;
diagnostic à quatorze contrôles prouvé par `AndroidDeviceReadinessCheckerTest`, pas relu à l'écran
sur appareil ; aucun avertissement « plein écran » publiable (identifiant 4 retiré,
`clearAllAlsoWithdrawsTheRetiredFullScreenWarning` instrumenté vert). Réglage HyperOS « Notifications de l'écran de verrouillage » **désactivé** pour Niumi (C11, 18/10
18:43) : notification publiée (`vis=PUBLIC`) mais absente de l'écran de verrouillage, présente dans le
volet après déverrouillage, flux d'alarme audible, première publication sans `notification_alert` ;
icône → écran 8 → scan ; diagnostic vert, le réglage n'apparaissant dans aucune donnée Android
lisible. Limite écrite dans l'aide. La ligne « écran éteint depuis 30 minutes » a été remesurée avec le nouveau comportement (C10,
ci-dessus), en Doze léger seulement.

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
| Mesurées et conformes | 54 (dont 8 des 9 lignes du blocage différé, 10 des 11 lignes des sonneries, 3 des 5 lignes des boîtiers multiples — dont une avec réserve, l'incident `TIME_CHANGED` —, 6 des 7 lignes du réveil sans plein écran, le scan d'un autre tag mesuré le 17/10, et le retrait des récents, conforme avec parade) ; la ligne « plein écran refusé » est retirée de §20 avec l'autorisation (Lot 9) |
| Mesurées partiellement | 3 (boucle des quatre sonneries : « Oiseaux » seule réécoutée avec le lecteur `AudioTrack` ; boîtier ajouté après l'activation : identité du tag refusé non relevée ; « Boîtier 1 » à l'écran 6 après la mise à jour : lu en base, pas vu à l'écran) |
| Mesurées, limite établie et documentée | 6 (silence total à l'activation, **silence total pendant la sonnerie** — mesuré à l'étape 32 : ni son, ni vibration, notification absente de l'écran de verrouillage —, arrêt forcé, NFC verrouillé, restriction d'énergie constructeur, **processus tué avant l'heure de début** — mesurée à l'étape 25 : sur HyperOS, le service d'accessibilité n'est plus relié après la mort du processus) |
| Non reproductibles sur cet appareil | 1 (arrêt du seul FGS) |
| Non testées faute d'appareil ou de matériel audio | 11 |
| Non testées sur le build release | 2 |

Les campagnes qui restent à mener pour combler ces lignes sont détaillées dans
`docs/android/RESTE_A_FAIRE.md`, section B.

**La matrice n'est pas verte au sens de §21.** Elle l'est sur un appareil, une surcouche et une
version d'Android. Les deux manques qui pèsent le plus sur la promesse du produit : aucun Pixel ni
Samsung, et aucun essai sur un artefact de publication.
