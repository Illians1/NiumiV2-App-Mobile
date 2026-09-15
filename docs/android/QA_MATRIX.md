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
| Xiaomi (Redmi) | 25080RABDG (`lapis`) | 16 / API 36, HyperOS OS3.0 | `BP2A.250605.031.A3` | **couvert**, campagnes des 7-8, 13, 14 et 15 septembre 2026 |
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

Les 41 scénarios du tableau de §20, dans son ordre. « Source » renvoie au rapport qui porte les
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
| volume alarme à zéro avant activation | activation refusée | **non testé sur appareil** — le contrôle existe (`DeviceReadinessChecker`, §13) et est couvert en JVM, mais le refus d'activation n'a jamais été rejoué à la main depuis l'étape 12 | — | à exécuter |
| volume mis à zéro après activation | incident documenté | **non testé** — non exécuté | — | à exécuter |
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

Ces trois lignes étaient hors périmètre de l'étape 6 (`DeviceReadinessChecker` n'existait pas) et
n'avaient jamais été rejouées depuis. **Les trois sont soldées le 2026-09-15.** Dans les trois cas,
l'écran de diagnostic ne propose aucun chemin vers le choix de l'heure tant qu'un contrôle est
rouge, ce qui est la forme concrète du refus d'activation de §9.2.

### Écran 13 « Aide et limites » (étape 21)

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| ouverture depuis l'accueil, hors session | les limites lisibles de bout en bout | **OK** (2026-09-15) — quatre sections et seize limites restituées, comparées une à une à `LIMITES.md` | `ETAPE-21.md` |
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
| restriction d'énergie constructeur active (hors §20) | — | **Limite majeure mesurée** — sans exemption constructeur, le blocage devient **silencieusement** inopérant après ~60 s ; avec exemption, < 1 s. C'est le constat qui justifie le contrôle de §13 | `ETAPE-05.md` |

Le texte exact de l'overlay imposé par §12.2 n'est pas vérifiable par `dumpsys` : il reste un
contrôle visuel, fait à l'œil pendant les campagnes.

### Session, processus et fin de session

| Scénario | Résultat attendu | Xiaomi 25080RABDG / Android 16 | Source |
| --- | --- | --- | --- |
| Niumi retiré des récents après armement | alarme et blocage conservés | **OK** | `LOT-0.md` |
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
| Mesurées et conformes | 27 |
| Mesurées, limite établie et documentée | 4 (silence total, arrêt forcé, NFC verrouillé, restriction d'énergie constructeur) |
| Non reproductibles sur cet appareil | 1 (arrêt du seul FGS) |
| Non testées faute d'appareil, de matériel audio ou de second tag | 12 |
| Non testées sur le build release | 2 |

Les campagnes qui restent à mener pour combler ces lignes sont détaillées dans
`docs/android/RESTE_A_FAIRE.md`, section B.

**La matrice n'est pas verte au sens de §21.** Elle l'est sur un appareil, une surcouche et une
version d'Android. Les deux manques qui pèsent le plus sur la promesse du produit : aucun Pixel ni
Samsung, et aucun essai sur un artefact de publication.
