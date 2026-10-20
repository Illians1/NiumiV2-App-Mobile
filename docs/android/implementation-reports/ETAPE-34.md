# Étape 34 — Composants du design system

Date : 2026-10-20. **Étape terminée** (essai TalkBack fait par Mehdi le même jour, voir « Validation sur
appareil » ; restes en « Validations restantes »). Code, tests JVM (1 453) et instrumentés du module (37), documents ;
captures des composants prises sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS
OS3.0.302.0.WPPEUXM) et comparées aux maquettes. Deux défauts trouvés sur appareil et deux par la
relecture ont été corrigés, chacun vu rouge puis vert. Deuxième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 10, phase M). Point de départ :
`e288dc4` (correctif A8). Pendant l'étape, une autre session a committé `314b2b9` (montée de
kotlinx-serialization-json), sans rapport avec ces fichiers.

## Ce qui est livré

- **Composants** (`:core:designsystem`, `com.niumi.designsystem.component`). Chacun a un aperçu
  clair et un aperçu sombre, et aucune dimension en dur hors `NiumiDimens` / `NiumiShapes` :
  - `NiumiPrimaryButton` : 56 de haut, encre ; inactif selon D9, sans action de clic ;
  - `NiumiOutlinedButton`, `NiumiDiscreetLink` (14, souligné, zone de 44) ;
  - `NiumiPillButton` : pilule de choix, rôle radio, pour les durées de la maquette 16 ;
  - `NiumiActionPill` : « Modifier » au trait, « Choisir les applications » pleine
    (maquettes 10 et 12) ;
  - `NiumiBottomSheet` : socle `ModalBottomSheet` de Material 3 1.4.0, pleine largeur, voile et
    liseré du thème, poignée de 40 × 4. En-tête titre + croix (22/600) ou titre seul (24/600).
    Le contenu reçoit `requestClose` ; une seule fermeture, quel qu'en soit le déclencheur ;
  - `NiumiCard` : remplie à l'encre ou au trait ;
  - `NiumiListRow` : séparateurs, chevron ou cadenas ;
  - `NiumiDayChip` ;
  - `NiumiSwitch` : 52 × 32, pastille de 26, ambre ;
  - `NiumiTopBar` : flèche alignée sur la marge, titre 28 en intertitre ;
  - `NiumiScreenScaffold` : bord à bord, marges 24/56/34 ;
  - `NiumiLogo` (24 ou 54) et `NiumiPageIndicator`.
- **Trois états des champs de l'accueil** (`NiumiFieldState`), sur la pastille de jour et la
  rangée :
  - `Normal` ;
  - `Greyed` : grisé mais modifiable (« Réveil désactivé », maquette 13) ;
  - `Locked` : grisé et verrouillé (session, maquette 15). L'action du champ n'est jamais
    appelée, TalkBack annonce « Verrouillé », et `onLockedClick` reste disponible pour le
    message de l'étape 45.
- **`NiumiComponentTexts`** : « Verrouillé » et « niumi » (texte de remplacement du logo). Les
  autres libellés d'accessibilité viennent du `Texts` de chaque écran.
- **`NiumiTimeFormat`** (`com.niumi.designsystem.format`) : « 7:00 », « 22:30 », « 0:05 »,
  « 56 min », « 9 h 26 », « 1 h 00 », « 8 h 30 de sommeil » ; une durée négative est refusée.
- **Jetons :**
  - rôle de palette `disabledBorder` : `#C6C6C2` en clair, `#3A3A40` en sombre ;
  - mesures de composants relevées dans les sources HTML, chacune commentée avec sa maquette.
- **Icônes au trait** `ic_chevron_right`, `ic_back`, `ic_close`, `ic_lock`, `ic_check`,
  `ic_settings`, `ic_more` : tracés recopiés des maquettes, noires dans le fichier et teintées
  par le composant. Le chevron a quitté `:feature:session`, dont `AlarmSoundRow` l'importe
  désormais de `:core:designsystem`.

## Décisions prises pendant l'étape

**Décisions de Mehdi du 2026-10-20.**

1. **Trois états des champs.** Le plan prévoyait un simple
`enabled`, qui aurait rendu intouchable l'accueil « Réveil désactivé », alors que SPECIFICATION
§9 l'y veut modifiable. Le bloc « Interfaces transverses » du plan est corrigé.
2. **Le cadenas ambre reste.** VALEURS §2 disait « pas d'ambre dans les icônes », alors que §5 et
   les maquettes 15 et 18 à 27 dessinent un cadenas ambre sur la carte « Blocage en cours », et
   que l'icône du Niumi Point (maquette 47) porte un point ambre. La règle est précisée dans le
   même changement : les icônes de navigation et d'action restent sans ambre ; ces deux icônes
   restent telles quelles (D14 précisé dans `POINTS-OUVERTS.md`). Les sept `ic_*` livrés ici ne
   changent pas.
3. **Marges posées sur les barres système.** Le cadre d'écran et la feuille ajoutaient 56 en
   haut et 34 en bas aux barres d'Android, alors que sur la maquette iPhone ces marges couvrent
   déjà la barre d'état et la barre d'accueil : jusqu'à 80 dp de vide sous le bouton principal
   avec la navigation à trois boutons. De chaque côté s'applique désormais la plus grande de la
   marge et de la zone système, jamais leur somme (`marginsOverSystemBars`, `NiumiMarginsTest`,
   5 tests, rouge par compilation puis vert). VALEURS §4, SPEC_ANDROID §15 et le plan sont mis
   à jour.

Décisions de l'exécutant, à relire :

- **`disabledBorder`, nouveau rôle de palette.** Le bord des jours non cochés grisés vaut
  `#C6C6C2` en clair et `#3A3A40` en sombre. Aucun rôle existant ne donne ces deux valeurs, car
  le bord de menu est transparent en clair. VALEURS §2 précise donc l'usage de « Désactivé »,
  « Fond désactivé » et « Bord de menu ».
- **Pastille verrouillée annoncée « Lundi, Verrouillé ».** « Verrouillé » est ajouté au nom et
  non à l'état : un état de description remplace « coché » dans l'annonce de TalkBack
  (relecture).
- **Deux pilules.** La maquette en montre deux familles : les choix de durée (radio, 15) et les
  actions (bouton, 16/500, 22 de marge). Le plan n'en nommait qu'une ; `NiumiActionPill`
  s'ajoute.
- **Feuille du bas.**
  - L'état de la feuille reste interne : l'exposer imposerait l'API expérimentale de Material
    à chaque écran.
  - La croix et le contenu rangent la feuille puis appellent `onDismiss` une seule fois.
  - Les icônes des barres système suivent `contentColor` (encre du thème), jamais le téléphone
    (vérifié dans le bytecode de material3 1.4.0).
- **Poignée et liseré dessinés dans le contenu de la feuille.** Un `border` posé sur le
  `modifier` de `ModalBottomSheet` encadrait une autre zone que la feuille visible (défaut
  constaté sur appareil). La poignée maison n'est plus le bouton sans libellé que Material
  enveloppe (relecture). Glisser, toucher le voile, revenir en arrière et la croix ferment
  toujours la feuille.
- **Interrupteur en sombre** : aucune maquette. Mêmes rôles qu'en clair (piste ambre / bordure,
  pastille encre / surface).
- **Soulignement des liens** : Compose ne règle pas son écart (4 dans la maquette) ; c'est le
  soulignement de la police.

## Specs et documents modifiés dans le même changement

- **SPEC_ANDROID §6** : rôle de `:core:designsystem` (composants, trois états, libellés, aucune
  dimension en dur) ; **§15 « Règles UI »** : marges posées sur les barres système.
- **VALEURS-DE-DESIGN.md §2** : usages de « Désactivé », « Fond désactivé », « Bord de menu » ;
  règles d'emploi de l'ambre précisées (icônes de navigation sans ambre, deux exceptions).
  **§4** : marges sur Android.
- **POINTS-OUVERTS.md** : D14 précisé le 2026-10-20.
- **Plan de refonte** : bloc « Interfaces transverses » (palette, composants, format), table des
  décisions (D14 précisé, marges), section Étape 34 (fichiers, tests, cases).
- SPEC_CORE_KMP et SPEC_IOS inchangées.

## Tests écrits

**JVM** (rouges par compilation tant que la classe manquait, puis verts) :

- `NiumiTimeFormatTest` (7) ;
- `NiumiComponentTextsTest` (2) ;
- `NiumiPaletteTest` étendu (`disabledBorder`, 22 rôles ; rouge puis vert).

**Instrumentés** (`:core:designsystem`, 37 au total avec `NiumiThemeTest` et
`WindowFocusRegainedEffectTest`) :

- `NiumiComponentsSemanticsTest` (20) : rôles, zone de 44, inactif sans clic, trois états,
  intertitre, logo, indicateur ;
- `NiumiBottomSheetTest` (6) : titre, croix, absence de croix, voile, fermeture par le
  contenu, une seule fermeture ;
- `NiumiComponentsCaptureTest` (4) : aide à la validation visuelle ; images dans
  `files/captures` de l'APK de test, la feuille par capture système de l'écran entier.

Défauts trouvés, tous vus rouges puis verts :

1. **Sur appareil — bouton inactif.** Sa sémantique n'était pas fusionnée : TalkBack aurait lu
   « Continuer » sans « bouton, désactivé ». Test `anInactivePrimaryButtonHasNoClickAction`.
2. **Sur appareil, à la capture — liseré de la feuille sombre.** Il était dessiné comme un grand
   rectangle en haut de l'écran. Corrigé et remesuré à la capture ; aucun test automatique
   (défaut visuel).
3. **Relecture — double fermeture.** Croix puis bouton du contenu pendant l'animation :
   `onDismiss` appelé deux fois. Test `quickRepeatedClosingRequestsCloseTheSheetOnce`. Dans sa
   première version, ce test (deux touchers sur la croix) passait sans correctif : il a été
   durci jusqu'à montrer 2.
4. **Relecture — pastille verrouillée.** TalkBack ne disait plus « coché ». Tests des pastilles
   verrouillées.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug
#   1 453 tests JVM, 0 échec (core:designsystem 45, dont 14 nouveaux) ; APK debug produit
./gradlew ktlintFormat && ./gradlew ktlintCheck detekt :app:lintDebug
#   detekt : LongParameterList (trois composants, @Suppress commenté comme NiumiPalette),
#   MayBeConstant (constantes LOCKED, LOGO), LongMethod (rangée, feuille : fonctions extraites) ; puis vert
./gradlew :core:designsystem:connectedDebugAndroidTest   # 37 tests, 0 échec
./gradlew connectedDebugAndroidTest                        # voir ci-dessous
grep -rn "Color(0x" androidApp --include='*.kt' | grep -v NiumiPalettes.kt                     # vide
grep -rnE "[0-9]+(\.[0-9]+)?\.(dp|sp)" androidApp/core/designsystem/src/main/kotlin/com/niumi/designsystem/component   # vide
grep -rn "isSystemInDarkTheme\|NiumiColors\|DayNight" androidApp --include='*.kt' --include='*.xml' | grep -v /build/   # vide
```

**Suite instrumentée complète.**
- Tous les modules sont verts sauf `:app` : database 90, designsystem 35 (avant les ajouts de la
  relecture), system 29, ringing 6, session 3, setup 41.
- `:app` a échoué de façon instable : 3 tests au premier passage (contrainte d'unicité sur
  `session_event_receipt`, deux `AlarmChain` / `BlockingStartReceiver`, un `ProcessDeath`), 1 au
  second (`BlockingGuardServiceInstrumentedTest.theGuardReachesTheForegroundWithItsNotificationThenStops` :
  texte de la notification `null`).
- **Rejoué sur `HEAD` (`314b2b9`) sans les changements de l'étape**, dans une copie de travail à
  part : même échec. Il précède donc l'étape 34 et touche le service de garde du correctif A8.
  Non corrigé ici.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM.

| Essai | Résultat |
| --- | --- |
| Tests instrumentés du module | **OK**, 37/37 |
| Captures boutons, pilules, jours, rangées, cartes, haut d'écran, en clair et en sombre | **Conformes** aux maquettes 01, 13 à 16, 19, 23, 37 : couleurs par état, D9, `disabledBorder`, chevron ou cadenas, pastille d'interrupteur, logo dans les deux versions |
| Captures des feuilles « Sonnerie » (clair) et « Annuler la session ? » (sombre) | **Défaut** puis **OK** : liseré déplacé (voir défaut 2), corrigé ; voile à 45 % et 60 %, poignée, titres, croix conformes aux maquettes 16 et 19 |
| Après la décision sur les marges : tests instrumentés du module | **OK**, 37/37 |
| Après la décision sur les marges : captures des deux feuilles | **OK** : le contenu s'arrête juste au-dessus de la barre de navigation à trois boutons (48 dp), sans les 34 dp supplémentaires d'avant |
| TalkBack (activé par Mehdi, page de composants temporaire retirée ensuite) | **OK** pour ce qui était visé : chaque pastille de jour est annoncée **en une seule fois** (jour et état ensemble, malgré les deux nœuds du relevé `uiautomator`), et « Verrouillé » est dit sur les pastilles et la rangée verrouillées. Les autres annonces (boutons, pilules, interrupteur, rangées, indicateur, logo) n'ont pas appelé de remarque de Mehdi |

## Relecture finale

Relecteur à contexte neuf, sur tout le changement, avant l'exécution sur appareil.

- **Critiques (2)** : le liseré de la feuille et le bouton inactif non fusionné. Ce sont les
  défauts 2 et 1, trouvés aussi sur appareil et corrigés.
- **Importants corrigés (3)** :
  - double fermeture (défaut 3) ;
  - annonce de la pastille verrouillée (défaut 4) ;
  - contenu de la feuille incapable de la refermer avec animation : ajout de `requestClose`.
- **Important réglé par le correctif du liseré** : la poignée sans libellé.
- **Important tranché par Mehdi** : les marges du bas (décision 3 ci-dessus).

Constats mineurs reportés, à reprendre avec l'écran qui en a besoin :

- rangée de liste : la maquette 37 ajoute 10 de marge verticale au minimum de 64 (un titre sur
  deux lignes toucherait les séparateurs) ;
- écart entre la valeur et l'icône : 6 sur les rangées de l'accueil (maquettes 13 à 16), 8
  ailleurs ;
- marge du bas de la feuille figée à 34 ; la maquette 19 utilise 22 ;
- espaces de « 9 h 26 », « 56 min » : espaces ordinaires, donc coupables en fin de ligne ;
  l'espace insécable est à décider ;
- `NiumiLogo` déduit le thème par égalité avec `NiumiPalettes.Dark` ;
- `backLabel` et `contentDescription` de l'interrupteur sont facultatifs même quand l'action
  existe ;
- `NiumiCard` et `NiumiScreenScaffold` n'ont que la capture comme test.

## Validations restantes

- **Comparaison des `@Preview`** dans Android Studio par Mehdi.
- **Marges du cadre d'écran sur appareil** : le premier écran bâti sur `NiumiScreenScaffold`
  (étape 35) doit montrer le bouton principal à 48 dp du bas avec la navigation à trois boutons,
  à 34 avec la navigation par gestes. L'activité des tests instrumentés n'est pas bord à bord et
  ne peut pas le montrer ; la feuille du bas, elle, l'est (voir « Validation sur appareil »).
- **Échec instable de `:app`** (service de garde A8), préexistant, à traiter hors de l'étape 34.

**Rien n'est committé.**
