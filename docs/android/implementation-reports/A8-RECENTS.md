# Correctif A8 — carte de Niumi dans les récents ouverts depuis Niumi

Date : 2026-10-19 et 2026-10-20. **Première parade mesurée puis écartée** (retrait de la tâche),
**seconde parade mesurée, décidée et implémentée** : service de garde de session (`specialUse`),
carte rendue aux récents, validée sur appareil le 2026-10-20. Le changement corrige aussi
l'attribution à HyperOS d'un comportement qui est celui d'Android. Appareil : Xiaomi 25080RABDG /
Android 16 / HyperOS OS3.0.302.0.WPPEUXM. Décision de Mehdi avant la première séance : piste 1,
« retirer la tâche de Niumi quand l'utilisateur le quitte pendant une session », mesurée sur
appareil avant d'être implémentée.

## Ce qui est établi

- **Pas une régression de l'étape 33.** Le code des récents (`RecentsCard`,
  `hideRecentsCardWhileSessionIsInProgress`) est inchangé depuis l'étape 25.
- **La carte visible est une règle d'Android.** `RecentTasks.isInVisibleRange` (AOSP, branche
  `main`, lue le 2026-10-19) garde une tâche exclue des récents si elle est la plus récente
  (`taskIndex == 0`) ou, derrière le drapeau `enableUseTopVisibleActivityForExcludeFromRecentTask`,
  si elle a encore une activité visible. Aucune API ne cache la carte de la tâche au premier plan.
- **Le non-rétablissement du service d'accessibilité est une règle d'Android, pas de HyperOS.**
  `AccessibilityServiceConnection.binderDied` → `AccessibilityUserState.serviceDisconnectedLocked`
  inscrit le service dans `mCrashedServices` ; `AccessibilityManagerService.updateServicesLocked`
  passe ces services (« Skip the component since it may be in process or crashed »). Le service
  n'en sort que si l'utilisateur le désactive (`removeDisabledServicesFromTemporaryStatesLocked`,
  « users may toggle the on/off switch to retry »), à une mise à jour du paquet
  (`onPackageUpdateFinished`) ou à son retrait. C'est ce que les réglages affichent comme « en
  panne ». Jusqu'ici, SPEC_ANDROID §13 et §15, `LIMITES.md`, l'aide et quatre commentaires de code
  l'attribuaient à HyperOS : corrigé dans ce changement.
- **Seule la mort au balayage est propre à HyperOS.** Sur AOSP, `killProcessesForRemovedTask` ne
  tue qu'un processus en état « background » et diffère les autres (`setWaitingToKill`) ; un
  service d'accessibilité lié (`BIND_FOREGROUND_SERVICE_WHILE_AWAKE`) tient le processus au-dessus
  de ce seuil. Non mesuré hors HyperOS (B1, B2).
- `Activity.finishAndRemoveTask()` retire la tâche **sans** tuer le processus
  (`ActivityClientController.finishActivity`, `killProcess = false`) : vérifié sur appareil, même
  PID avant et après neuf retraits.

## Mesure 1 — signaux du cycle de vie, sans code (16:17 à 16:19)

Journal `adb logcat -b events`, session armée, écran 7 au premier plan.

| Geste | `wm_pause_activity` | Pause | Arrêt | Remarque |
| --- | --- | --- | --- | --- |
| Home | `userLeaving=true` | +20 ms | +1,1 s | |
| Récents (balayer et maintenir) | `userLeaving=false` | +26 ms | +1,1 s | la pause arrive **pendant** le panneau |
| Volet de notifications | aucun | — | — | conforme à §13 |
| Verrouillage | `userLeaving=false`, raison `sleep` | +12 ms | +65 ms | |
| Réglage ouvert depuis Niumi | `userLeaving=true` | +17 ms | aucun avant le retour | |

Conséquences : `onUserLeaveHint` ne distingue pas les récents (faux) d'un réglage ouvert par Niumi
(vrai) ; seul un marqueur explicite posé par Niumi avant ses propres lancements le peut. La pause
est le bon point d'accroche : elle précède de plus d'une seconde le moment où l'utilisateur peut
balayer.

## Parade implémentée puis retirée

`MainActivity.onPause` appelait `finishAndRemoveTask()` quand la session était en cours, hors
états de scan, hors rotation, écran interactif, et sans lancement initié par Niumi
(`NiumiInitiatedLeaveHost`, `Context.startActivityFromNiumi`, posé sur les cinq sites qui ouvrent
une autre activité depuis `MainActivity`). Règle pure `shouldRemoveTaskOnPause`, six tests JVM,
ktlint, detekt, lint et build verts.

## Mesure 2 — la parade sur appareil (16:24 à 17:36)

| Essai | Résultat |
| --- | --- |
| Home, récents depuis l'accueil, retour par l'icône | tâche retirée (`wm_task_removed` +1 s), carte absente, PID inchangé, retour sur l'écran 7 après l'écran de démarrage |
| Récents depuis Niumi, sans toucher la carte | tâche retirée pendant le panneau ; **la carte reste affichée** (cache du lanceur HyperOS) et **la toucher ne rouvre rien** |
| Volet | aucun effet |
| Verrouillage, déverrouillage | **défaut** : retour sur l'accueil du téléphone. La pause `sleep` arrive alors que `PowerManager.isInteractive` vaut encore vrai |
| Réglage ouvert depuis l'écran 7, export du diagnostic | tâche conservée, retour sur Niumi : le marqueur fait son office |
| **Récents depuis Niumi, puis balayage de la carte inerte** (17:36) | tâche retirée à 17:36:01 ; à 17:36:08 le lanceur (`RecentsContainer.removeTask` sur sa clé en cache) fait tuer le processus : `am_kill … com.niumi.app,200,SwipeUpClean` ; relance à 17:36:13 ; service d'accessibilité « planté », délié |

**Conclusion.** Sur HyperOS, le lanceur conserve la carte après le retrait de la tâche et tue
**par nom de paquet**, tâche ou non. Retirer la tâche ne protège de rien, dégrade le verrouillage et
laisse une carte morte. La parade est retirée. Le même journal montre, à 13:17, le balayage de
Firefox tuant ses trois processus (`:gpu`, `:tab`, principal) d'un coup : HyperOS tue tous les
processus du paquet, ce qui écarte aussi la piste « service d'accessibilité dans un processus
séparé » sans avoir à la coder. À 14:25, Brick, application de blocage du même genre, a été tuée
de la même façon (`adj 200`, `SwipeUpClean`) ; son service est lui aussi « planté » dans
`dumpsys accessibility`.

## Mesure 3 — service de premier plan (17:45 à 17:47, sur décision de Mehdi)

POC jetable dans la variante `debug`, retiré ensuite : `PocForegroundService`, un `Service` sans
Hilt, `startForeground` avec le type `mediaPlayback` (seule permission de type déjà déclarée) et
une notification silencieuse, démarré par `adb shell am start-foreground-service`. État avant
l'essai : `isForeground=true`, processus 21128 à `adj 200`, service d'accessibilité lié (la
réinstallation l'avait réhabilité : `onPackageUpdateFinished` vide `mCrashedServices`, règle AOSP).

| Heure | Événement |
| --- | --- |
| 17:46:22 | Niumi ouvert par l'icône (processus 21128) |
| 17:46:24 | récents ouverts depuis Niumi (`pauseBackTasks`) |
| 17:46:29 | balayage : `RecentsContainer.removeTask` puis **`doSwapUPClean`**, activité détruite (`removeTaskLocked`), tâche retirée |
| 17:46:29 | **aucun `am_kill`** : processus 21128 vivant, `adj 0` une fois Niumi rouvert |
| 17:46:31 | Niumi rouvert par l'icône, même processus, service d'accessibilité toujours lié |

**Conclusion.** Sur HyperOS, le nettoyeur du balayage épargne un processus qui tient un service de
premier plan, comme le fait AOSP (`killTaskProcessesIfPossible`, « Don't kill process(es) with
foreground service »). L'argument de l'étape 25 (« même priorité que le service lié, donc inutile »)
est démenti : les deux processus sont à `adj 200`, mais seul celui du service de premier plan
survit. Une seule mesure, un seul appareil.

## Décisions de Mehdi (2026-10-19, soir)

Après une première version limitée à la phase de blocage, discutée au fil des mesures :

1. service de garde pendant **toute la session**, de l'armement à l'état final : avant le coucher,
   une session sans lui mourait au balayage et le blocage échouait à l'heure prévue, sans que
   l'utilisateur le voie avant le lendemain ;
2. **deux notifications** : « Réveil programmé à 07:00 » / « Tes applications choisies seront
   bloquées à partir de 22:30. » avant le coucher (« à partir de » demandé le 2026-10-20), puis
   « Blocage en cours jusqu'à 07:00 » / « Tes applications choisies restent bloquées jusqu'au
   réveil. » ;
3. une notification écartée **n'est jamais ramenée** : la protection vient du service, pas de la
   notification (mesuré à 19:04) ;
4. **la carte de Niumi n'est plus cachée** des récents (`RecentsCard` retiré) ;
5. le **verrou HyperOS reste exigé** : le service ne protège pas de « Tout effacer ».

Comparaison faite par Mehdi avec AppBlock, installé sur le même téléphone : service de premier plan
`specialUse` permanent, détection par les statistiques d'usage, service d'accessibilité optionnel.

## Implémentation

- `:core:system` : `isBlockingInForce()` (`isSessionDark` y délègue) ; `blocking/guard/` :
  `BlockingGuardPolicy` (gardé sur `isSessionInProgress`, phase d'après `isBlockingInForce`,
  `afterRestart` pour la relance sans commande), `BlockingGuardController`,
  `BlockingGuardWatcher` (observe le snapshot publié, lancé par `NiumiApplication`, refus
  journalisé et rejoué au retour de `MainActivity`) ; canal `niumi_blocking_guard` (importance
  basse), `BlockingGuardNotificationSpecs`, `BlockingGuardNotificationFactory`.
- `:core:database` : `BLOCKING_GUARD_STARTED`, `BLOCKING_GUARD_STOPPED`,
  `BLOCKING_GUARD_START_REFUSED`.
- `:feature:session` : `BlockingGuardService` (`specialUse`, `START_STICKY`, `directBootAware`,
  identifiant de notification 10), `AndroidBlockingGuardController`, `BlockingGuardModule` ;
  `WakeScheduleFormatter.formatTime`.
- `:app` : neuvième permission `FOREGROUND_SERVICE_SPECIAL_USE`, `ReleaseHygieneTest` ;
  `RecentsCard`, `RecentsTasks` et leur test supprimés, `MainActivity` ne cache plus rien.
- Aucun effet nouveau dans le contrat commun KMP.

Écartés en route : une surveillance qui republiait la notification toutes les trente secondes
(ajoutée sur une première demande, retirée sur la décision 3) ; un test instrumenté dans
`:feature:session`, dont le graphe Hilt est incomplet (le service est testé dans `:app`).

## Tests

- JVM : `BlockingGuardPolicyTest`, `BlockingGuardWatcherTest`, `BlockingGuardNotificationSpecsTest`,
  `SessionStatesTest` (thème égal au blocage en vigueur sur tous les états),
  `NiumiNotificationChannelsTest`, `SessionWarningNotificationSpecsTest` (quatre canaux),
  `TechnicalEventTypeTest`, `WakeScheduleFormatterTest` (heure seule), `HelpTextsTest`,
  `ReleaseHygieneTest` (neuf permissions). Tous verts, avec ktlint, detekt, lint et le build.
- Instrumentés, le 2026-10-20 sur le Xiaomi : **190 tests, 0 échec** — `:core:database` 90,
  `:core:system` 29, `:core:designsystem` 7, `:feature:ringing` 6, `:feature:session` 3,
  `:feature:setup` 41 (`connectedDebugAndroidTest`), `:app` 14 dont les deux nouveaux de
  `BlockingGuardServiceInstrumentedTest` (premier plan réel et notification avec l'heure, arrêt sur
  demande, arrêt seul après une relance sans commande et sans session), lancés par
  `am instrument` sans désinstaller l'application.

## Mesure 4 — service de garde réel (2026-10-19 18:48 à 2026-10-20 12:36)

| Heure | Essai | Résultat |
| --- | --- | --- |
| 19/10 18:48:37 | activation, blocage immédiat | service démarré (`PROC_STATE_TOP`), type `0x40000000` (`specialUse`) |
| 19/10 18:49:09 | verrou retiré puis « Tout effacer » | **tué** (`OneKeyClean`, processus à `adj 50`), service d'accessibilité planté : le service ne protège pas de « Tout effacer » |
| 19/10 18:52:04 | processus relancé par la réinstallation, sans interface | service revenu seul (réconciliation) |
| 19/10 18:58:54 | sonnerie puis scan | service arrêté avec la sonnerie (`BLOCKING_GUARD_STOPPED`) |
| 19/10 18:58:58 | carte de l'écran de réveil balayée **après** la fin de session | tué : hors session, aucune protection |
| 19/10 19:01:53 | pendant le blocage, carte balayée | processus et service d'accessibilité intacts |
| 19/10 19:04:44 | notification écartée, carte balayée | intacts : la protection vient du service |
| 19/10 19:10:50 | scan | les deux services arrêtés dans la seconde |
| 20/10 09:06:12 | carte balayée hors session | tué, service d'accessibilité planté (limite connue) |
| 20/10 12:27:10 | activation, blocage différé à 12:30, réveil à 12:35 | service démarré à l'armement ; carte visible dans les récents |
| 20/10 12:27:39 | carte balayée **avant le coucher** | processus et service d'accessibilité intacts |
| 20/10 12:30:00 | heure du blocage | blocage appliqué à l'heure |
| 20/10 12:34:33 | Acrobat ouvert | renvoyé à l'accueil |
| 20/10 12:34 | notification écartée, attente d'une minute | **pas revenue** (constaté par Mehdi, non relevé par adb) |
| 20/10 12:35:34 | sonnerie puis scan | sonnerie puis service de garde arrêtés, notification retirée, service d'accessibilité lié |
| 20/10 12:48:52 | activation, blocage immédiat, réveil à 13:10 | service démarré |
| 20/10 12:49:14 | `adb reboot` pendant la session | — |
| 20/10 12:49:55 | démarrage, **avant le déverrouillage** (`RUNNING_LOCKED`) | processus réveillé par `SystemEventsReceiver`, service de garde au premier plan moins d'une seconde après (`SYSTEM_ALLOW_LISTED`), alarme reprogrammée ; service d'accessibilité pas encore lié (attendu) |
| 20/10 12:53:54 | après déverrouillage, Acrobat ouvert | service d'accessibilité lié, Acrobat renvoyé à l'accueil ; incident `TIME_CHANGED` au démarrage, réserve connue du Lot 8 |

Le verrou HyperOS a été perdu deux fois le 19/10 pendant ces essais, sans geste de l'utilisateur
sur le cadenas : suivi en `RESTE_A_FAIRE.md`, A9.

## Ce qui reste

- Déclaration Play du type `specialUse` (`play-console/FGS_SPECIAL_USE.md`), avec A6.
- Hors session, balayer la carte de Niumi plante encore le service d'accessibilité sur HyperOS ;
  le diagnostic le signale avant l'activation suivante.
- Non mesurés : changement d'heure du téléphone pendant une session avec le service de garde ;
  Pixel et Samsung (B1, B2).
- Le test instrumenté redémarre le processus de Niumi : il laisse le service d'accessibilité
  planté, réhabilité par une réinstallation de l'APK (`onPackageUpdateFinished`).
