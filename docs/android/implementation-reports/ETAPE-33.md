# Étape 33 — Jetons, police, thème par état

Date : 2026-10-19. **Étape terminée** : code, tests JVM (1 413) et instrumentés (188), documents,
et protocole manuel déroulé sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS
OS3.0.302.0.WPPEUXM), voir « Validation sur appareil ». Un défaut trouvé sur appareil (logo de
l'écran de démarrage rogné) a été corrigé et remesuré. Première
étape du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 10, phase M).
Point de départ : `3757548` (plan de refonte révisé), dépôt propre.

## Ce qui est livré

- **Jetons du design dans `:core:designsystem`**, tous comparés à
  `design-plan/valeurs-de-design.json`, lu dans le dépôt par les tests :
  - `NiumiPalette` (21 rôles) et `NiumiPalettes.Light` / `Dark`, avec le dégradé du soleil
    (`SunGradient`) ; seul fichier de l'app où une couleur s'écrit en dur ;
  - `NiumiFont.PlexSans` : IBM Plex Sans 400, 500, 600 embarquée (`res/font`, fichiers TTF de
    l'archive officielle `@ibm/plex-sans@1.1.0`, `ibm-plex-sans.zip`, SHA-256
    `fb365d91…69200`), licence livrée avec l'archive en `res/raw/ofl_ibm_plex_sans.txt` ;
  - `NiumiType` : 24 styles, un par entrée `tailles` du JSON et ses variantes de graisse, plus
    « Désactivé » du cadran (30/500), et `NiumiType.material()` pour la typographie Material 3.
    Cinq lignes du tableau VALEURS §3 n'ont ni clé dans le JSON ni style : 24/400 et 20/400 (roue),
    18/600 (titre du guide), 14/600 (liens du bandeau), 13/500 (erreur de la recopie). Elles
    seront ajoutées avec leurs écrans (étapes 36, 42, 49, 50, 52) ;
  - `NiumiShapes` et `NiumiDimens` (VALEURS §4) ;
  - logo vectoriel `niumi_logo_light.xml` et `niumi_logo_dark.xml`, convertis à la main depuis les
    SVG ajoutés par Mehdi dans `design-plan/logo/`.
- **`NiumiTheme(dark: Boolean)` sans valeur par défaut.** Il fournit `LocalNiumiPalette` et un
  `MaterialTheme` entièrement dérivé de la palette (`toColorScheme`) et de `NiumiType`. Aucune
  couleur d'alerte : `error` vaut l'encre. L'ambre n'est l'accent d'aucun rôle Material.
- **Thème par état de session.** `SessionSnapshotDto?.isSessionDark()` (`:core:system`, à côté de
  `isSessionInProgress`) : sombre pour `ARMED` à blocage appliqué, `RINGING`, `AWAITING_NFC`,
  `TRIGGERED_AWAITING_NFC`, `RELEASING` ; clair sinon. `SessionThemeSource` lit la persistance
  quand le publisher vaut encore `null` (processus neuf), puis suit le publisher.
  `SessionThemeViewModel` (`:app`) expose `StateFlow<Boolean?>` ; `MainActivity` ne compose rien
  tant que le thème est inconnu, puis `NiumiTheme(dark = …)`. `AlarmActivity` est toujours sombre.
- **Fenêtre accordée au thème de l'app** (`applyNiumiWindowTheme(dark)`) : barres système et fond
  de fenêtre. L'appel précédent, `enableEdgeToEdge()` sans argument, suivait le mode sombre du
  téléphone.
- **Écran de démarrage sombre, tenu jusqu'au thème connu** (demande de Mehdi du 2026-10-19 :
  « Il ne faut pas que le fond clair s'affiche lors d'un accueil sombre »). `MainActivity` démarre
  sur `Theme.Niumi.Launch` : fenêtre sombre sur Android 10 et 11, écran de démarrage système sombre
  avec le logo blanc cassé sur Android 12 et plus (`niumi_splash_icon`, logo de 90 × 21,5 dp dans
  les 108 dp imposés, environ 160 × 38 dp affichés). Sa première image est retenue par un
  `OnPreDrawListener` tant que le thème de la session est inconnu. Aucun minimum ; trois secondes au
  plus, après quoi l'app s'ouvre en clair (`LAUNCH_READ_TIMEOUT_MILLIS` dans `SessionThemeSource`).
  Aucune dépendance ajoutée.
- **Thèmes XML** : `Theme.Niumi` (parent `android:Theme.Material.Light.NoActionBar`, fond
  `#FAFAF8`) pour l'application, `Theme.Niumi.Launch` pour `MainActivity`, `Theme.Niumi.Dark` (parent
  `android:Theme.Material.NoActionBar`, fond `#0E0E10`) pour `AlarmActivity`. Plus aucun parent
  jour/nuit.
- **Écrans existants sur la nouvelle palette**, sans autre changement : sept écrans lisent
  `LocalNiumiPalette.current` au lieu de `MaterialTheme.colorScheme`, l'overlay de blocage
  (`TextView`) prend `NiumiPalettes.Dark`. Les 18 aperçus passent `dark` explicitement.
- **Supprimés** : `NiumiColors.kt` (palette V2) et `docs/CHARTE_GRAPHIQUE_APP_MOBILE.md`.

## Décisions prises pendant l'étape

Décisions de Mehdi du 2026-10-19, consignées dans `POINTS-OUVERTS.md`, `VALEURS-DE-DESIGN.md`,
`valeurs-de-design.json` et la table des décisions du plan :

1. **D9** : bouton inactif en clair, fond `#E8E8E4` et texte `#5C5C60`, partout.
2. **D13** : apostrophe typographique (’) dans les textes de l'app. Aucun texte n'est touché à
   cette étape ; la règle s'applique dès l'écran 1 (étape 35).
3. **D14** : règle de l'ambre validée telle quelle.
4. **D19** : titres d'écran en 28 partout, écran 2 compris.
5. **Charte V2 supprimée**. `design-plan/VALEURS-DE-DESIGN.md` est la seule référence visuelle. Le
   plan de refonte ne prévoit plus de charte V3 à l'étape 54, sauf demande de Mehdi.
6. **Logo** : les deux SVG de `design-plan/logo/` sont la source ; les PNG ne sont pas embarqués.
7. Travail sur `master`, sans commit.
8. **Écran de démarrage** : sombre avec le logo, tenu tant que Niumi ne sait pas s'il y a une
   session ; aucun minimum (comme les apps de référence, Android le retire à la première image) ;
   trois secondes au plus, puis ouverture en clair. Conséquence acceptée : en journée, une
   ouverture à froid passe du sombre au clair. Écartés : un écran de démarrage clair (le flash
   clair restait la nuit) et une couleur mémorisée d'une ouverture à l'autre (flash clair une fois
   par nuit si le coucher passe app fermée).

Décisions de l'exécutant, à relire :

- **Rôles sans valeur dans le design.** En clair, `sheetOutline` et `menuBorder` sont transparents
  et `mismatchTint` vaut la teinte ambre. En sombre, `tileGrey` et `cellGrey` valent la piste,
  `amberTint` vaut la teinte du mot signalé, `onInkSecondary` vaut `#5C5C60` et `disabled` vaut le
  « fond désactivé » `#4A4A50`. `onInk` clair vaut `#FFFFFF` : le `#FAFAF8` de la carte noire n'en
  est pas distingué (VALEURS §2). Chaque valeur est commentée et testée.
- **Dix styles de texte ajoutés à la liste du plan**, pour couvrir toutes les entrées `tailles` du
  JSON :
  `scanSheetTitle26`, `alertTitle22`, `instruction22`, `subtitle20`, `dialHour17`, `lead17`,
  `labelStrong16`, `chipSelected15`, `secondary14`, `appLabelChecked12`. Trois formes et
  dimensions aussi : `authorizeButton`, `pill`, `pillButton`/`pillButtonLarge`.
- **`RELEASING` est sombre quelle que soit sa cible.** `DISARMED` n'existe qu'à l'étape 43.
- **Persistance illisible au démarrage : thème clair.** Aucun état sombre n'est prouvé.
- **`LocalNiumiPalette` vaut le thème clair par défaut**, parce que des tests instrumentés
  composent des écrans sans `NiumiTheme`. Les deux activités passent toujours par `NiumiTheme`.
- **Correspondance Material 3** : boutons et choix segmentés Material à l'encre, cartes d'incident
  sur la tuile grise. **Effet visible sur deux anciens écrans** : l'écran 5 (choix du début du
  blocage) et l'écran 14 (sonnerie) n'utilisent plus l'ambre ; la sonnerie choisie se signale par
  le gras seul. SPEC_ANDROID §15 est amendée. Ces écrans sont refaits aux étapes 40 et 45.
- **Aperçus** : sombres pour les écrans qui n'existent qu'en session (réveil, session active, scan
  requis), clairs pour les autres. Le plan disait « clair » partout.
- **Rayons de 8 dp de `WakeTimeScreen` et `RingtoneScreen` laissés en place** : ces écrans sont
  remplacés aux étapes 45 et 40. SPEC_ANDROID §6 le dit : seules les couleurs sont déjà toutes
  dans le module.
- **`displayLarge` de la typographie Material vaut `dial42`, pas `clock112`** (relecture finale) :
  le `TimePicker` de l'écran 5 écrit ses heures en `displayLarge` dans des champs de 96 × 80 dp,
  où 112 sp aurait été coupé.
- **Lecture de la persistance au démarrage tolérante** (relecture finale) : une exception que la
  passerelle ne convertit pas se lit « clair » au lieu de faire tomber l'app avant son premier
  écran. L'annulation de coroutine est propagée.

## Specs et documents modifiés dans le même changement

- **SPEC_ANDROID** : §5 (ligne « Police », Material 3 comme socle) ; §6 (rôle de
  `:core:designsystem`, aucune couleur, taille ou forme en dur hors du module) ; §15 « Règles UI »
  (thème par état de session, `MainActivity`, `AlarmActivity`, barres système) ; §15 écran 5 et
  écran 14 (renvois à la charte remplacés, ambre retiré).
- **Design-plan** : `POINTS-OUVERTS.md` (D9, D13, D14, D19 tranchés) ; `VALEURS-DE-DESIGN.md` §2
  (règle de l'ambre validée, piste et désactivé), §3 (ligne 26), §4 (bouton inactif) ;
  `valeurs-de-design.json` (note de `titreEcran`) ; `LISEZ-MOI.md` (logo vectoriel).
- **Plan de refonte** : table des décisions (lignes de l'étape 33 tranchées), cases de l'étape 33,
  étape 54 (plus de charte V3).
- **Commentaires de code** qui citaient la charte V2 : réécrits vers le design-plan
  (`PairingScreen`, `WakeTimeScreen`, `WakeTimeTexts`, `ActiveSessionScreen`, `RingtoneScreen`,
  overlay de blocage, `ic_play.xml`, `ic_launcher_foreground.xml`).
- SPEC_CORE_KMP et SPEC_IOS : inchangées, le contrat commun ne bouge pas.

## Tests écrits

**JVM** (rouge observé, par échec de compilation tant que la classe manquait, puis vert) :

- `:core:designsystem` (nouveau dossier `src/test`, 27 tests) :
  - `NiumiPaletteTest` (7) : chaque rôle des deux thèmes contre sa clé du JSON, rôles sans valeur,
    couverture des 21 rôles, aucune clé du JSON sans jeton, dégradé du soleil, fonds de fenêtre de
    `colors.xml` égaux à la palette ;
  - `NiumiTypeTest` (7) : taille, graisse, interligne et interlettrage de chaque entrée `tailles`,
    variantes « choisie » et « cochée », `dialDisabled30`, police sur tous les styles, trois
    fichiers de police, typographie Material bâtie sur `NiumiType`, `displayLarge` qui tient dans
    le `TimePicker` (57 sp au plus) ;
  - `NiumiShapesAndDimensTest` (6) : chaque entrée `formes`, rayons mesurés à densité 1 ;
  - `NiumiBrandAssetsTest` (3) : trois fichiers TrueType, texte de licence OFL, logos identiques
    trait pour trait aux SVG sources, dans leur couleur ;
  - `NiumiColorSchemeTest` (4) : rôles Material issus de la palette, aucune couleur d'alerte,
    ambre absent des accents, aucune teinte d'élévation.
- `:core:system` : `SessionStatesTest` (8, nouveau : il n'existait pas malgré le KDoc qui le
  citait), `SessionThemeSourceTest` (6, dont l'exception imprévue lue « clair »).
- `:app` : `SessionThemeViewModelTest` (2).
- Écran de démarrage : `NiumiLaunchThemeTest` (4, `:core:designsystem` : thème de lancement
  sombre dans `values` et `values-v31`, logo sombre, insets qui gardent le rapport du logo,
  `MainActivity` lancée avec `Theme.Niumi.Launch`) et
  `SessionThemeSourceTest.aPersistenceThatNeverAnswersOpensLightAfterThreeSeconds` (temps
  virtuel : rien à 2 999 ms, clair à 3 001 ms). Les deux vus rouges, puis verts.
- Vérifiés par mutation (valeur faussée, test rouge, valeur rétablie) : une couleur de la palette,
  un trait du logo, la valeur initiale du thème du ViewModel. Le test des logos et celui de la
  licence passaient dès leur écriture, les fichiers ayant été copiés avant : la mutation du logo
  l'a fait échouer.

**Instrumenté, écrit, pas encore exécuté** : `NiumiThemeTest` (`:core:designsystem`, 2 tests) :
`NiumiTheme(dark)` fournit la palette, le fond Material et la typographie attendus.
`HelpScreenTest` passe `NiumiTheme(dark = false)`.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug
#   batterie finale, après l'écran de démarrage : 1 413 tests JVM, 0 échec : app 40 (+2), app release 45,
#   core:database 148, core:designsystem 31 (nouveaux), core:system 548 (+15),
#   feature:ringing 43, feature:session 239, feature:setup 104, shared:core 215 ; APK debug produit
./gradlew ktlintFormat && ./gradlew ktlintCheck detekt :app:lintDebug
#   1re passe : detekt a signalé une déstructuration à quatre (DesignValues), deux paramètres
#   de SessionThemeSource lus comme inutilisés, un cast inutile, et l'API createComposeRule
#   dépréciée dans le nouveau NiumiThemeTest — corrigés ; 2e passe verte, lint « No issues found »
./gradlew :app:lintRelease   # « No issues found » (après la relecture)
./gradlew :core:designsystem:compileDebugAndroidTestKotlin   # vert
grep -rn "isSystemInDarkTheme\|NiumiColors\|DayNight" androidApp --include='*.kt' --include='*.xml'   # vide
grep -rn "Color(0x" androidApp --include='*.kt' | grep -v NiumiPalettes.kt                           # vide
grep -rn "MaterialTheme.colorScheme" androidApp --include='*.kt' | grep -v core/designsystem         # vide
```

Les avertissements de compilation `hiltViewModel` déprécié (dix écrans) et `createComposeRule`
déprécié (`HelpScreenTest`, `WindowFocusRegainedEffectTest`) existaient avant l'étape.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, navigation à trois
boutons, mode sombre système désactivé sauf mention. Mesures : vidéo de l'écran (`screenrecord`) ou
capture chaque seconde, luminosité moyenne par image (`ffmpeg signalstats`, YAVG de 0 à 255 ; écran
clair ≈ 220, sombre ≈ 16 à 60), journaux `logcat -b events`, `dumpsys`.

`./gradlew connectedDebugAndroidTest` : **188 tests, 0 échec** (app 12, core:database 90,
core:designsystem 7 dont `NiumiThemeTest` 2, core:system 29, feature:ringing 6, feature:session 3,
feature:setup 41).

| N° | Essai | Résultat |
| --- | --- | --- |
| — | écran de démarrage, première version | **Défaut** : logo rogné à un cercle et étiré deux fois en hauteur (« ıum ») ; le système ne dessine pas l'icône à 108 dp et des marges fixes en dp faussaient les proportions. Corrigé : marges en pourcentage (`NiumiLaunchThemeTest` rouge puis vert) ; remesuré : logo entier, ≈ 180 dp de large, point ambre visible. Une réinstallation simple ne suffisait pas à rafraîchir l'icône (cache du système) : désinstallation puis installation |
| 1 | ouverture à froid hors session, téléphone clair | **OK.** Accueil du téléphone, puis écran de démarrage sombre avec le logo ≈ 1,2 s (version debug, `TotalTime` 1,4 à 1,5 s), fondu, puis accueil clair ; police Plex, icônes des barres sombres |
| 2 | même chose, téléphone en mode sombre | **OK.** App claire, icônes de barre d'état et boutons de navigation sombres et lisibles, sans voile ; mode clair rétabli |
| 3 | session armée, blocage immédiat (14:49) | **OK.** Écran de session active sombre, icônes des barres claires ; téléphone en mode sombre : reste sombre (YAVG 34). Trois relances en session : aucune image claire, mais toutes « à chaud » (sans écran de démarrage) : HyperOS ignore « Ne pas conserver les activités », et tuer le processus délierait le service d'accessibilité |
| 4 | sonnerie (15:05) puis scan | **OK pour les couleurs.** Écran de réveil sombre, barres claires (YAVG 30) ; après le scan, écran clair (YAVG 221). Écran affiché : le diagnostic au lieu de « Session terminée », cause antérieure à l'étape (voir « Observation hors périmètre ») |
| 5 | blocage différé à 15:24, écran de session laissé ouvert | **OK.** Clair jusqu'à 15:23:59 (« Réveil programmé · blocage à 15:24 »), sombre dès 15:24:00 (« applications bloquées »), sans geste ; barres basculées avec l'écran |

Non couvert sur appareil : relance de zéro pendant une session (écran de démarrage tenu puis
accueil sombre), Android 10 et 11 (fenêtre de démarrage sans logo).

## Relecture finale

Relecteur à contexte neuf, sur tout le diff non committé. Aucun constat critique. Deux constats
corrigés, chacun par un test vu rouge puis vert : le `TimePicker` de l'écran 5 aurait coupé ses
heures (`displayLarge` à 112 sp) ; une exception imprévue de la persistance aurait fait tomber
l'app au lancement. SPEC_ANDROID §6 et ce rapport corrigés là où ils disaient plus que le réel.
Le constat du fond clair bref avant un accueil sombre est résolu par l'écran de démarrage tenu,
sans dépendance (voir « Ce qui est livré »). Constats mineurs reportés :

- les douze rôles `*Fixed` du `ColorScheme` gardent leurs valeurs Material, qu'aucun composant
  utilisé ne lit ;
- `error` vaut l'encre : un champ Material en erreur ressemblerait au champ actif (aucun aujourd'hui ;
  la recopie des étapes 42 et 49 dira l'erreur en texte) ;
- dans le `TimePicker`, le champ actif (tuile grise) se distingue peu du champ inactif (blanc) en
  clair, jusqu'au remplacement de l'écran 5 (étape 45) ;
- la barre de navigation à trois boutons n'a plus de voile système (`SystemBarStyle` explicite) : à
  regarder sur appareil.

## Observation hors périmètre, non corrigée

Pendant les essais, la carte de Niumi est apparue dans les récents en pleine session armée, et son
balayage (14:52:37) a tué Niumi et mis le service d'accessibilité en panne : blocage coupé jusqu'à
réactivation manuelle. Code des récents inchangé depuis l'étape 25. Cause, d'après AOSP
(`RecentTasks.isInVisibleRange`) et confirmée par l'utilisateur : les récents ouverts **depuis
Niumi** gardent sa carte, tâche la plus récente, malgré l'exclusion. Ma consigne de test disait à
tort que ce geste n'était possible qu'hors session. Consigné dans SPEC_ANDROID §15, `QA_MATRIX.md`
et `RESTE_A_FAIRE.md` (A8, à décider).

Seconde observation, antérieure à l'étape : pendant la sonnerie, l'utilisateur a touché la
notification d'avertissement « service d'accessibilité désactivé » (15:05:32). Elle a créé un
`MainActivity` neuf dans une nouvelle tâche (destination diagnostic), qui a ouvert l'écran de réveil
**dans la même tâche**. Après le scan, `AlarmActivity` a demandé `MainActivity` avec la destination
« session terminée » ; `MainActivity` n'étant pas au sommet de la tâche, Android a ramené la tâche
sans transmettre l'intent (aucun `wm_new_intent`), et l'utilisateur a vu le diagnostic au lieu de
l'écran « Session terminée ». Le chemin mesuré à l'étape 17 (sonnerie ouverte par sa propre
notification, écran de réveil dans sa propre tâche) n'est pas concerné. Non corrigé.

## Incertitudes et limites

- **Relance de zéro pendant une session** non provoquée sur appareil : le chemin « écran de
  démarrage tenu, lecture de la persistance, accueil sombre » est prouvé par les tests JVM
  (`SessionThemeSourceTest`) et par l'essai 1 hors session. Android 10 et 11 non couverts.
- **Garde-fou de trois secondes** : si la session répondait après ce délai, l'app, ouverte en
  clair, basculerait au sombre à la publication suivante. Cas jamais observé.
- **Anciens écrans** : leur rendu change (boutons Material à l'encre, cartes d'incident sur la
  tuile grise, police Plex). Vus sans défaut pendant les essais : accueil, session active (clair
  et sombre), écran de réveil, diagnostic. Les autres (mise en route, heure, récapitulatif,
  sonnerie) ont été parcourus par l'utilisateur sans remarque, mais pas relus à la capture ; tous
  sont remplacés aux étapes 35 à 53.
- **Fichiers à ne pas oublier au commit** : les deux SVG de `design-plan/logo/` ne sont pas encore
  suivis par git, et `NiumiBrandAssetsTest` les lit.

**Rien n'est committé.**
