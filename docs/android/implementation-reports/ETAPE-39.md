# Étape 39 — Écran 7 · Écran de blocage

Date : 2026-10-23. **Étape terminée** : code, tests JVM et instrumentés, documents, essais sur appareil (Xiaomi
25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, navigation à trois boutons). Septième étape
du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 11, phase N). Point
de départ : `a21ac70`.

## Ce qui est livré

- **Écran 7 · Écran de blocage** (`:feature:session`, `blocking/`), d'après la maquette 25 et
  `design-plan/SPECIFICATION.md` §12. Il remplace le bandeau non touchable de 3 s des Lots 1 à 9.
  - Logo de 54, titre « L’application YouTube est bloquée » (24/600, trois lignes au plus ; « Cette application est
    bloquée » sans nom), « Tu pourras l’ouvrir après avoir scanné ton Niumi Spot. », « Fermer »
    (bouton principal) et « Ouvrir Niumi » (`NiumiTextButton`, nouveau composant : 16, à l'encre,
    sans soulignement). Toujours sombre.
  - Fenêtre `TYPE_ACCESSIBILITY_OVERLAY` plein écran, touchable, `FLAG_NOT_FOCUSABLE`, étendue sous
    les barres système ; titre de fenêtre `NiumiBlockScreen` ; titre de volet pour TalkBack.
  - `ComposeView` hors activité : `OverlayViewTreeOwner` (cycle de vie, état sauvegardé,
    `ViewModelStore`), un par affichage, détruit au retrait. Aucune minuterie.
- **Retrait** : « Fermer » ; « Ouvrir Niumi » (puis Niumi ouvert comme depuis son icône) ; fin du
  blocage (`BlockingDeactivationListener`, nouveau, sur `PersistedBlockedPackagesProjection`) ;
  extinction de l'écran (`ACTION_SCREEN_OFF`) ; une application non bloquée au premier plan autre
  que les lanceurs, l'interface système et Niumi (`BlockScreenDismissal`) ; `onInterrupt` et
  `onUnbind`. Écran éteint, l'écran 7 n'est jamais posé (le retour à l'accueil suffit).
- **`<queries>`** de `:core:system` : `ACTION_MAIN` + `CATEGORY_HOME` ajouté, pour lire les
  lanceurs sans dépendre de leur visibilité par défaut. Aucune permission nouvelle.
- `niumi_block_overlay_text` supprimé ; le titre se compose dans `BlockScreenTexts.title`, depuis
  `BlockAction.GoHome.displayName` (libellé figé à l'activation).
- `tools/validate_blocking.sh` : essais 4 (écran encore là après 4 s, retiré par « Fermer »), 4b
  (« Ouvrir Niumi »), 4c (extinction), 5 (application autorisée qui retire l'écran) ; fenêtres
  comptées par leur titre.

## Décisions de Mehdi (2026-10-22)

- **Q6** : l'écran 7 reste jusqu'à un de ses boutons, sans minuterie.
- **Extinction** : l'écran 7 part quand l'écran s'éteint, sinon il reviendrait par-dessus l'écran
  de verrouillage au rallumage.
- **Autre application** (relevé en relecture) : une application non bloquée qui passe devant, un
  appel entrant en premier lieu, retire l'écran 7.
- **T10** : déclaration Play et script de la vidéo refaits ; l'acceptation par Google Play reste
  ouverte.

## Décisions de Mehdi pendant les essais (2026-10-23)

- **Logo et texte plus écartés** : 88 dp entre le logo et le titre au lieu des 32 de la maquette,
  et 16 dp au-dessus du logo, l'ensemble restant centré (texte environ 36 dp plus bas, logo environ
  20 dp plus haut que sur la maquette). Retenu après cinq essais sur le téléphone (64, 80, 112 avec
  le logo fixe, 112 avec le logo remonté, puis 88).
- **Nom de l'application dans le titre** : « L’application YouTube est bloquée », et « Tu pourras
  l’ouvrir après avoir scanné ton Niumi Spot. » au lieu de « Elle reviendra… ». « YouTube est
  bloqué » a été essayé d'abord, puis écarté sur la capture : « Calculatrice est bloqué ». L'accord
  avec « application » reste juste quel que soit le nom. Écart à la maquette, Android seulement ; l'iPhone reste ouvert (T13).
- **« Fermer » reste le bouton principal** : geste attendu, parité avec l'iPhone où « Ouvrir
  Niumi » n'est pas réalisable directement.

## Écarts au plan

- `BlockOverlayInstrumentedTest` (vraie fenêtre) est irréalisable : toute instrumentation débranche
  le service, seul porteur du jeton (SPEC_ANDROID §19.2). Remplacé par `BlockOverlayControllerTest`,
  derrière la couture `OverlayWindowHost`, la vue étant posée dans une fenêtre à part de l'activité
  de test (aucun propriétaire hérité) ; la vraie fenêtre est prouvée par le script.
- `NiumiTextButton` ajouté au design system, et la ligne « Bouton texte » à
  `design-plan/VALEURS-DE-DESIGN.md` §4 : la maquette ne dessine pas « Ouvrir Niumi » en lien discret.
- Ajouts après relecture indépendante : aucun affichage écran éteint (un début différé rejoué sur un
  paquet périmé l'aurait posé sans extinction pour le retirer), retrait par une autre application,
  titre de fenêtre et de volet, journal quand Niumi ne peut pas s'ouvrir, `destroy()` sans `start()`.

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md` : §12.1 (`<queries>` HOME), §12.2 (écran de blocage, conditions de
  retrait, lancement sur toucher, couverture de la barre d'état), §12.4, §15 (écran de blocage), §19
  (garde-fous et tests), §20 (lignes de la matrice), justification de `displayNameSnapshot`.
- `design-plan/POINTS-OUVERTS.md` : T13 (nom de l'application sur l'écran de blocage iPhone).
- `design-plan/SPECIFICATION.md` §12 (Android), `VALEURS-DE-DESIGN.md` §4, `POINTS-OUVERTS.md` T10.
- `docs/android/play-console/ACCESSIBILITY_DECLARATION.md` §4, `REVIEW_VIDEO_SCRIPT.md` séquence 9.
- Plan de refonte : décision Q6, écarts dans « Interfaces transverses », cases de l'étape 39.

## Tests

- **JVM** : 1 562, aucun échec. Nouveaux : `BlockScreenTextsTest` (5), `BlockScreenDismissalTest`
  (4). Étendus : `NiumiBlockingAccessibilityServiceSourceTest` (retrait dans chaque chemin, aucun
  affichage écran éteint, retrait par une autre application, aucune minuterie dans `blocking/`),
  `PersistedBlockedPackagesProjectionTest` (+4), `BlockingProjectionRefresherTest` (+1),
  `BlockingDecisionTest`.
- **Instrumentés** : session 14/14 (nouveaux : `BlockScreenContentTest` 4 avec contrôles
  d'accessibilité, `BlockOverlayControllerTest` 7, dont le changement de nom sans seconde fenêtre), system 33/33, ringing 14/14, database 90/90,
  app 15 (voir ci-dessous), designsystem 53/54, setup 52/55.
  - **Quatre échecs reproduits sur `HEAD`, résolus après l'étape (2026-10-23)** :
    `NiumiComponentsSemanticsTest.aLongTitleWrapsOnTwoLinesWithoutBeingCut` (titre de la fenêtre
    d'alerte mesuré sur une ligne, 28,6 dp au lieu de 52) ; `AppSelectionScreenTest`
    `theChosenSectionComesFirst`, `beyondSixTheLastSlotCountsTheOthers`,
    `onANarrowScreenTheCardShowsFewerSlots` (« Toutes les applications », « +10 », « +11 »
    introuvables). Cause : le téléphone était en paysage (rotation automatique) ; la barre de
    navigation sur le côté rétrécit l'écran 4 (cinq cases, « +11 »), la grille courte ne compose pas
    la seconde section, l'alerte plus large tient sur une ligne. Les quatre échecs reviennent à
    l'identique en paysage forcé (`wm user-rotation lock 1`) ; l'app, elle, défile correctement.
    Correctif : activité hôte des tests fixée en portrait dans les quatre manifestes de test
    (SPEC_ANDROID §19.2) ; en paysage forcé, setup 19/19 et designsystem 26/26. Suite complète
    ensuite, écran tenu allumé : **275/275** (app 15, database 90, designsystem 54, system 33,
    ringing 14, session 14, setup 55).
  - **Instabilité non comprise** : deux fois, le contrôle d'accessibilité a mesuré une zone de
    toucher de 47 dp au lieu de 48, vue décalée de 2 à 4 px (`theSwitchIsASwitchOfFullTouchSize`
    en portrait, `aDayChipIsACheckboxNamedAfterItsDay` et `aGreyedDayChipCanStillBeToggled` en
    paysage). Non reproduite en dix passages (cinq avec animations, cinq sans) ni dans la suite
    complète. Laissée ouverte.
  - Un passage a échoué téléphone en veille et verrouillé : 15 « No compose hierarchies found »
    et installations annulées. Effet d'environnement, d'où `svc power stayon usb` pendant la suite.
  - `BlockingGuardServiceInstrumentedTest.fromTheAlarmTheGuardTakesOverTheAlarmNotification` a
    échoué une fois dans la suite complète (notification 8, avertissement d'accessibilité, en plus) ;
    rejoué seul : 3/3 deux fois avec l'étape 39, 3/3 sur `HEAD`. Instabilité A8 connue.
  - HyperOS a refusé des installations (`INSTALL_FAILED_USER_RESTRICTED`) tant que la fenêtre
    « Installer via USB » restait sans réponse ; relancé une fois acceptée.
- **ktlint, detekt, lint, assemblage** : verts. Aucune couleur en dur hors `NiumiPalettes.kt` ;
  aucun `postDelayed` ni `Handler(` dans `blocking/`.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, 394 dp de large.
Session armée par le parcours réel, Calculatrice bloquée, Notes autorisées.

| Essai | Résultat |
| --- | --- |
| 1. Rendu de l'écran 7 (capture) | **OK** : fidèle à la maquette 25 ; couvre barre d'état et barre de navigation |
| 2. `validate_blocking.sh` essais 1 à 3 (lanceur, tâche existante, intent explicite) | **OK** |
| 3. Essai 4 : écran 7 encore là après 4 s, retiré par « Fermer » | **OK** |
| 4. Essai 4b : « Ouvrir Niumi » | **OK** : écran retiré, Niumi au premier plan (écran de session) |
| 5. Essai 4c : extinction puis rallumage | **OK** : retiré à l'extinction ; écran de verrouillage propre (capture) |
| 6. Essai 5 : Notes ouvertes pendant l'écran 7 | **OK** : Notes au premier plan, écran 7 retiré |
| 7. Essai 6 : blocage après 90 s en arrière-plan | **OK** |
| 8. Paysage forcé (`wm user-rotation lock 1`) | **OK** : reste en portrait, le lanceur imposant le portrait |
| 9. Service désactivé pendant l'affichage (`enabled_accessibility_services` sans Niumi) | **OK** : écran retiré ; réactivé à la main par Mehdi, blocage revenu |
| 10. TalkBack | **OK** (Mehdi) |
| 11. Appel entrant pendant l'écran 7 | **OK** (Mehdi) : écran retiré, appel visible |
| 12. Sonnerie : Calculatrice, écran 7, « Ouvrir Niumi », scan, Calculatrice | **OK** (Mehdi) : écran 7, puis écran de réveil, puis Calculatrice ouverte sans écran 7 après le scan |
| 13. Écart logo-texte révisé (capture après chaque essai) | **OK** (Mehdi) : 88 dp retenus |
| 14. Titre avec le nom (captures) | « Calculatrice est bloqué » écarté, puis « L’application Calculatrice est bloquée » |

Les touchers des essais 3 à 6 ont été injectés par `adb shell input tap`, sur les invites du script,
avec capture avant chaque toucher.

## Restes et points d'attention

- **Une erreur pendant le dessin de l'écran 7** ferait planter Niumi et désactiver le service ;
  aucun test ne peut l'attraper dans la vraie fenêtre. Rien observé sur cet appareil.
- **Android 10** (minSdk 29) : sans `fitInsetsTypes`, la fenêtre peut ne pas couvrir les barres
  système ; non mesuré.
- **Autres surcouches** : une fenêtre passagère ou un clavier sous un autre paquet retirerait
  l'écran 7 ; rien observé sur HyperOS.
- **Fin du blocage** : prouvée en JVM ; sur appareil, l'écran 7 ne peut pas être affiché au moment
  du scan (le scan se fait dans Niumi, qui le retire).
- **Mineurs laissés** : écouteurs de la projection jamais remis à `null` après la déliaison ;
  `onDestroy` sans `onUnbind` ne nettoie pas le récepteur ; « une seule fois par transition » non
  garanti sous accès concurrents (retrait idempotent) ; le test de source lit du texte.
- **Rien n'est committé.**
