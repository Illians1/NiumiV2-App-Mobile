# Étape 24 — Interface du blocage différé (écrans 5, 6 et 7)

Date : 2026-09-17. **Validé sur appareil** : Xiaomi 25080RABDG, Android 16, HyperOS
OS3.0.302.0.WPPEUXM. 876 tests JVM verts, six essais du protocole déroulés en 24 h et en 12 h.

Le Lot 6 était complet sous l'interface depuis l'étape 23, mais `SummaryViewModel` passait
`blockingLocalTimeIso = null` : le chemin différé existait sans qu'aucun utilisateur puisse
l'emprunter. Cette étape l'ouvre, et le protocole manuel reporté par l'étape 23 est déroulé ici.

## Ce qui est livré

- **Écran 5** — section « Blocage des applications » : `SingleChoiceSegmentedButtonRow`
  « Maintenant » / « À partir de », sélecteur d'heure Material 3 en boîte de dialogue, phrase de
  confirmation calculée par `computeBlockingSchedule`, message de refus d'antériorité.
  `WakeTimeViewModel` enchaîne `computeWakeSchedule` puis `computeBlockingSchedule` sur **un seul**
  `nowEpochMillis` et un seul fuseau (SPEC_CORE_KMP §8.3) ; `SetupPreferences` mémorise la dernière
  heure de début confirmée, et une confirmation de « Maintenant » **retire** la clé.
- **Écran 6** — ligne « Blocage des applications » sous la date du réveil, « Dès l'activation » ou
  l'instant obtenu. `NiumiRoute.Summary` transporte les deux choix, jamais un instant calculé.
- **Écran 7** — libellé `ARMED` dédoublé, titre de liste qui dit la vérité, ligne « Début du
  blocage » dans les deux fuseaux tant que l'instant n'est pas atteint, et libellé de
  `MISSED_BLOCKING_START_WINDOW`.
- **`BlockingScheduleFormatter`** — traduit un `BlockingScheduleDto` en `WakeScheduleDto`
  équivalent et délègue à `WakeScheduleFormatter` : une seule mise en forme des instants dans le
  module, trou d'heure d'été compris (grep de clôture vert).

## Décisions prises pendant l'étape

1. **`SegmentedButton` à rayon réduit** (validé le 2026-09-17). Le composant imposé par le plan a
   pour forme par défaut la « grande capsule très arrondie » que la charte §16 dit d'éviter :
   `SegmentedButtonDefaults.itemShape` est construit sur un rayon de 8 dp. Les deux exigences
   tiennent ensemble, aucune dérogation n'est nécessaire.
2. **Couleurs explicites du choix.** Le `ColorScheme` Niumi ne définit pas `secondaryContainer`,
   que le `SegmentedButton` emploie par défaut : sans couleurs explicites, le choix sélectionné
   prenait la teinte Material de base au lieu de l'Ambre, que la charte §3 réserve à ce qui est
   « actif, engagé ou en cours » — « heure sélectionnée » y figure nommément. La coche Material est
   conservée : « l'information importante ne doit jamais dépendre uniquement de la couleur » (§3).
3. **Trou de spec comblé** (validé le 2026-09-17). §15 ne disait pas ce qui arrive quand « À partir
   de » est choisi sans qu'aucune heure ne soit connue. Le sélecteur s'ouvre sur la dernière heure
   confirmée, à défaut sur 22:00, et l'annuler laisse « Maintenant » sélectionné : le `ViewModel` ne
   bascule qu'à la confirmation, de sorte qu'un « À partir de » sans heure est structurellement
   impossible. **SPEC_ANDROID §15 mise à jour dans le même changement.**
4. **Texte provisoire supprimé, pas seulement testé.** `SummaryTexts.INVALID_BLOCKING_SCHEDULE_MESSAGE`
   (étape 23) n'était pas la phrase de §15. §15 impose la **même** phrase aux écrans 5 et 6 pour le
   même fait : une seule chaîne existe désormais, portée par `WakeTimeTexts`, et `SummaryTexts`
   délègue — un test verrouille l'égalité mot pour mot.
5. **Le libellé `ARMED` change aussi pour les sessions immédiates** : « Réveil programmé » devient
   « Réveil programmé · applications bloquées », comme §15 l'impose. Vérifié à l'œil sur appareil.

## Défaut trouvé sur appareil et corrigé

**Faux état de fiabilité sur l'écran 5.** Un début différé refusé rendait `blockingDisplay = null`
comme un blocage immédiat, et l'écran annonçait « Tes applications seront bloquées dès
l'activation. » **juste au-dessus** du message de refus, alors que « À partir de » restait
sélectionné : deux phrases contradictoires, dont l'une promettait un blocage que l'activation
n'aurait pas appliqué. C'est exactement ce que §15 interdit.

La phrase se décide désormais sur le **mode choisi** et non sur la présence d'un affichage, via
`WakeTimeUiState.blockingSentence` — une propriété dérivée, donc couverte par deux tests de
`WakeTimeViewModelTest` (`aRefusedStartConfirmsNothingRatherThanPromisingAnImmediateBlocking`,
`anImmediateBlockingConfirmsItselfAndADeferredOneNamesItsInstant`). Le défaut n'était visible qu'à
l'écran : aucun test existant ne le voyait, parce que la décision vivait dans le composable.

## Second défaut trouvé sur appareil et corrigé : le rôle de la ligne d'heure

**La ligne d'heure de la section blocage restait en 24 h quand le début était refusé** — « 15:00 »
sous un réveil « 7:00 AM », téléphone en 12 h. Le format n'était que le symptôme : la ligne montrait
l'**instant obtenu** quand le choix était valide et la **saisie brute** quand il était refusé, alors
qu'elle est le champ qui ouvre le sélecteur, et que le sélecteur se rouvre toujours sur la saisie. Un
second cas, latent et non encore observé, découlait du même mélange : lors d'un trou d'heure d'été,
la ligne aurait affiché « 03:00 » pendant que le sélecteur se rouvrait sur 02:30.

La ligne fait désormais écho à la **saisie**, dans la convention du système, comme le cadran du
réveil ; seule la phrase de confirmation énonce l'instant obtenu. `WakeScheduleFormatter` gagne
`formatLocalTime`, qui applique **les deux mêmes** formateurs privés à une heure locale seule : ce
n'est pas une seconde règle de mise en forme, et le grep de clôture le reste vrai (une seule mise en
forme des instants dans le module). L'alternative — faire renvoyer l'instant calculé par
`computeBlockingSchedule` avec `NOT_BEFORE_TRIGGER` — a été écartée : elle casse l'invariant
« `schedule` non nul ⟺ `VALID` » sur lequel branchent trois appelants, touche le framework iOS, et
ne réglait pas l'incohérence de rôle. Elle reste la bonne voie le jour où le refus devra
*expliquer* (« ce serait demain à 08:00, après ton réveil »), ce qui est une autre fonctionnalité.

Quatre tests le verrouillent : `aRefusedStartIsStillEchoedInTheSystemConvention`,
`theLineAndTheSentenceAgreeOnAValidStart`, `anImmediateBlockingHasNoTimeToEcho` et
`inADaylightSavingGapTheLineEchoesTheChoiceAndTheSentenceTheInstant`, plus trois sur
`formatLocalTime`. **SPEC_ANDROID §15 mise à jour dans le même changement.**

**Revérifié sur appareil le 2026-09-17**, téléphone en 12 h : le choix mémorisé devenu invalide
s'affiche « 3:10 PM » — et non plus « 15:10 » — sous un réveil « 7:00 AM », avec son message de
refus ; le sélecteur se rouvre sur 03:10 PM, la même valeur que la ligne. Un début valide donne la
ligne « 10:30 PM » et la phrase « … à 10:30 PM (Europe/Paris). ». Le retour en 24 h est relu à
l'`ON_RESUME` : la ligne passe à « 22:30 » et la phrase suit.

Une observation sans conséquence, antérieure au Lot 6 : le cadran du réveil garde la convention avec
laquelle il a été créé (`rememberTimePickerState` n'est volontairement pas re-clé, sinon il se
réinitialiserait à chaque recomposition, voir le KDoc de `WakeTimeRoute`). Changer le format système
pendant que l'écran est ouvert laisse donc le cadran en AM/PM alors que la ligne et les phrases sont
déjà en 24 h. Le cas ne se produit que si l'on modifie ce réglage en pleine préparation.

## Trou de couverture comblé côté écran 6

`previewMessage` était muet sur un début refusé : le calcul ne produit **aucun** instant, donc la
politique commune n'a rien à refuser et `policy.blockingReasons` reste vide. L'écran désactivait
« Activer ma session » sans dire pourquoi. Une branche sur le statut du calcul précède désormais la
lecture de la politique, avec son test.

## Vérification

```
./gradlew testDebugUnitTest                     # 876 tests, 0 échec
./gradlew :app:assembleDebug
./gradlew ktlintCheck detekt :app:lintDebug
```

Répartition : `:feature:session` 180, `:core:system` 377, `:core:database` 124, `:app` 81,
`:feature:setup` 76, `:feature:ringing` 38.

Quatre remarques de detekt ont été traitées **par du code**, jamais par la configuration :
`WakeTimeActions` regroupe les trois événements de l'écran 5, `WakeTimeChoice` les deux heures
choisies transmises à l'écran 6, `BlockingScheduleFormatter` construit son équivalent sans sorties
multiples, et `SetupPreferences.write` accepte une valeur nulle — ce qui retire la clé sous la même
garde de déverrouillage et rend inutile un douzième membre de `DataStoreSetupPreferences`.

## Protocole manuel — six essais, appareil Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0

| # | Essai | Résultat |
|---|---|---|
| 1 | Parcours 07:00 → « À partir de » 22:30 → écrans 6 et 7 | **Vert.** « Tes applications seront bloquées aujourd'hui, jeudi 17 septembre à 22:30 (Europe/Paris). » ; écran 6 « Aujourd'hui, jeudi 17 septembre à 22:30 (Europe/Paris) » ; écran 7 « Réveil programmé · blocage à 22:30 », « Applications qui seront bloquées », « Début du blocage » |
| 2 | Début à 08:00, retour à « Maintenant », mémorisation | **Vert après correction** (voir « Défaut trouvé »). Refus affiché en texte neutre, « Continuer » inactif et sans Ambre ; retour à « Maintenant » efface le message ; le choix confirmé est repris à la préparation suivante, et confirmer « Maintenant » l'oublie |
| 3 | Les trois écrans en 12 h, format système changé en cours | **Vert.** Cadran AM/PM, « Demain, vendredi 18 septembre à 7:00 AM (Europe/Paris) », « Dès l'activation », écran 7 en « 7:00 AM ». Un début mémorisé devenu postérieur au réveil est refusé dans les deux conventions |
| 4 | `dumpsys alarm` | **Vert.** Deux alarmes distinctes : `BlockingStartReceiver` (`RTC_WAKEUP`, `flags=0x9`, sans `showIntent`) et `AlarmReceiver` (`flags=0x3`, avec `showIntent`). « Next alarm clock information » ne porte **jamais** l'instant de début ; seule une alarme d'agenda tierce y figurait |
| 5 | Session réelle à +5 min, application choisie ouverte avant l'heure | **Vert, mesuré.** L'Agenda reste au premier plan de 14:55:28 à 15:00:00 ; à 15:00:00 retour à l'accueil **sans changement de fenêtre**. Journal : `BLOCKING_START_RECEIVED` +90 ms, blocage appliqué **+101 ms**, `BLOCK_APPLIED {"packageName":"com.google.android.calendar"}` +308 ms. Seconde session : **+43 ms**, aucun incident |
| 5b | Bascule de l'écran 7 à l'heure, écran ouvert et non touché | **Vert.** À 15:10, « Réveil programmé · blocage à 15:10 » → « Réveil programmé · applications bloquées », « Applications qui seront bloquées » → « Applications bloquées », ligne « Début du blocage » retirée, **sans relance** |
| 6 | Scan avant l'heure de début ; non-régression immédiate | **Vert.** Scan → `CANCELLED`, « Tes applications sont débloquées. », **aucune** alarme Niumi en attente. Session immédiate : une **seule** alarme en attente (le réveil), blocage effectif dès l'activation, écrans identiques à l'étape 21 au libellé de la décision 5 près |

**Écran de progression du nettoyage : comportement prescrit, pas un défaut.** Pendant les scans
d'annulation, `AlarmActivity` est lancée puis détruite en environ une seconde (observé deux fois sur
deux dans logcat). Cette étape l'a d'abord signalée comme un clignotement indésirable ; **c'est
faux**, et le vérifier a demandé de lire ce que l'activité affichait : §10.2 et §10.4 prescrivent
d'« afficher la progression de nettoyage si l'état est `RELEASING` », et l'écran y dit « Ton scan est
validé. Niumi termine la session. » avec les étapes de libération (`ReleaseProgress`,
`AlarmScreenState`, où `RELEASING` passe avant tous les rangs NFC puisque le scan a déjà eu lieu).
La brièveté observée n'est que celle d'une libération réussie du premier coup ; si elle traînait ou
échouait, c'est précisément cet écran qui le dirait. Rien à corriger.

**Point de vigilance 13 levé.** Le chemin normal est désormais observé de bout en bout sur appareil,
déclenché par une **vraie alarme** à l'heure dite : `BLOCKING_START_RECEIVED` → moteur →
`APPLY_BLOCKING` → retour à l'accueil de l'application déjà ouverte, en 308 ms. Aucun essai ne
reposait sur le repli du moteur.

## Ce qui reste à valider — dette assumée

1. **Reporté à l'étape 25**, avec les scripts `tools/` : Doze forcé à l'heure de début, redémarrage
   avant l'heure et à +20 min (`MISSED_BLOCKING_START_WINDOW`), `am kill`, et les neuf lignes de §20.
2. **Le sélecteur de début n'a pas de mode saisie clavier** : comme le cadran du réveil, il ne
   permet que des minutes par pas de 5 au tap (le glissement reste libre). Cohérent avec l'écran 5
   existant, noté ici parce que le protocole l'a rencontré.

## Frottements d'outillage rencontrés

- **HyperOS** bloque l'installation par `INSTALL_FAILED_USER_RESTRICTED` quand « Installer via USB »
  se désactive — il l'a fait au premier essai, comme à l'étape 23.
- **Réinstaller l'APK délie le service d'accessibilité**, et `adb shell am force-stop` le tue : le
  diagnostic repasse alors au rouge. Le réactiver demande de poser
  `enabled_accessibility_services` **puis** `accessibility_enabled 1` avec quelques secondes
  d'écart, et de ne plus forcer l'arrêt de l'application ensuite.
- **Les données de l'application avaient été effacées** depuis l'étape 23 : association du boîtier,
  sélection d'applications et permissions ont dû être refaites avant le protocole.
- `uiautomator dump` échoue à obtenir l'état « idle » pendant l'animation du cadran et **laisse
  l'ancien fichier en place** : sans effacer `/sdcard/ui.xml` avant chaque capture, on relit un
  écran périmé. Trois taps ont ainsi été envoyés dans le vide avant que la cause soit trouvée.
