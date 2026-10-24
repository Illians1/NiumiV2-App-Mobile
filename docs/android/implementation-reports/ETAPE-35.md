# Étape 35 — Écran 1 · Présentation

Date : 2026-10-20. **Étape terminée** : code, tests JVM et instrumentés, documents, neuf essais
manuels conformes sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM). Le
passage TalkBack manuel n'est pas fait, par décision de Mehdi (voir « Décisions »). Troisième étape
du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 11, phase N). Point
de départ : `e20449b` (étape 34).

## Ce qui est livré

- **Écran 1 · Présentation** (`:feature:setup`, `com.niumi.feature.setup.presentation`).
  - Contenu : deux panneaux de `design-plan/SPECIFICATION.md` §4, maquettes 01 et 02. Logo en
    tête, flèche « Retour » sur le panneau 2 seulement, dessin, titre, texte, repère à deux
    traits, puis « Continuer » ou « Configurer Niumi ».
  - Composants : uniquement ceux de l'étape 34. Aucune couleur, taille ni forme écrite en dur.
  - Défilement : le contenu défile quand la hauteur manque (petit écran, paysage).
  - `PresentationTexts` : recopie mot pour mot, apostrophe ’ (D13), titres sans point (D12).
  - `PresentationViewModel` : le panneau affiché vit dans le `SavedStateHandle`.
    `acknowledge` n'existe que sur le panneau 2 et n'écrit qu'une fois, même après un double tap.
  - `PresentationRoute` : le retour système suit la flèche sur le panneau 2. Sur le panneau 1, il
    n'est pas intercepté et quitte l'app.
- **Dessins** (`:core:designsystem`, `com.niumi.designsystem.drawing`).
  - `LockedPhoneDrawing` et `PhoneNearSpotDrawing` : des `Canvas` qui reprennent la géométrie des
    SVG des maquettes.
  - Socle commun `NiumiIllustration` : 342 × 240 au plus, proportions gardées, annoncé une fois
    comme image.
  - Couleurs du thème : cases d'applications en `track` (`#E8E8E4`, la valeur de la maquette),
    un seul détail ambre par dessin.
- **Ouverture directe à la première ouverture** (`:app`).
  - Tant que l'accusé n'est pas lu, l'accueil ouvre la présentation de lui-même et se retire de la
    pile (`HomeViewModel.presentationRequired`).
  - Conditions : aucune session en cours, état lisible, et accusé déjà lu (rien n'est décidé
    avant la lecture).
  - « Configurer Niumi » remet l'accueil sous le diagnostic actuel : navigation transitoire
    jusqu'à l'étape 45.
- **Route** : `NiumiRoute.Onboarding` devient `NiumiRoute.Presentation`. La clé
  `onboarding_acknowledged` et l'API de `SetupPreferences` sont gardées : un utilisateur qui avait
  acquitté l'ancien onboarding ne voit pas la présentation.
- **Supprimés** : `OnboardingScreen`, `OnboardingTexts`, `OnboardingUiState`, `OnboardingViewModel`
  et leurs tests ; la comparaison de `HelpTextsTest` avec `OnboardingTexts`.
- **Accessibilité** (décisions du jour, voir ci-dessous).
  - Contrôles automatiques de l'Accessibility Test Framework dans les tests instrumentés de
    l'écran 1 et des composants.
  - Zone tactile de 48 dp réservée autour des sept éléments touchables de 44 :
    - pilule de choix, pilule d'action ;
    - pastille de jour ;
    - lien discret ;
    - interrupteur ;
    - flèche de retour ;
    - croix de feuille.

## Décisions prises pendant l'étape

1. **D12** : les deux titres de l'écran 1 s'écrivent sans point final, comme l'usage dominant
   (guides d'Apple et de Google). Le point du panneau 2 est retiré de `SPECIFICATION.md` ; l'image
   02 le montre encore, le texte fait foi.
2. **Limites avant la première activation** : le design fait foi. Les six limites de l'onboarding
   ne sont plus montrées avant la première session. L'aide (écran 13) et `LIMITES.md` les portent
   seules. Cela contredisait SPEC_ANDROID §3, §4 et deux critères de §19 : ils sont amendés.
3. **TalkBack**.
   - Pas de passage manuel sur l'écran 1 : il est simple et bâti sur des composants écoutés à
     l'étape 34.
   - Passage ciblé sur les écrans à risque au fil des étapes : écran 10 et feuille de scan,
     écran 7, feuilles du bas, cadran de l'accueil.
   - Revue complète à l'étape 55.
4. **Contrôles d'accessibilité automatiques**.
   - Dépendance de test `androidx.compose.ui:ui-test-junit4-accessibility` : version 1.12.1 par le
     BOM, apporte l'Accessibility Test Framework 4.1.1. Absente de l'APK livré.
   - Ajoutée à `:core:designsystem` et `:feature:setup` ; les autres modules la recevront avec
     leur écran refait. Les anciens écrans, appelés à disparaître, ne sont pas rattrapés.
5. **Zone tactile de 48 dp (option A, appliquée maintenant)**.
   - Le problème : les maquettes dessinent 44 (règle d'Apple), Google recommande 48. Compose
     agrandit déjà les petites zones au-delà de leurs bords, mais bride cet agrandissement entre
     deux éléments proches. Le contrôle l'a révélé sur deux pilules empilées (46 dp).
   - La correction : les composants réservent 48 dp (`minimumInteractiveComponentSize()`,
     `NiumiDimens.touchTarget`) ; le dessin garde 44.
   - Conséquence : les voisins s'écartent de 2 dp de chaque côté. La flèche et la croix
     compensent (`backButtonInset` passe de −12 à −14, `closeButtonInset` de −10 à −12) : leur
     tracé ne bouge pas. La rangée de `NiumiTopBar` et l'en-tête de l'écran 1 passent de 44 à 48.

## Specs et documents modifiés dans le même changement

- `design-plan/SPECIFICATION.md` §4 (titre sans point, note D12), `TEXTES-DES-MAQUETTES.md` §02
  (note, transcription de l'image inchangée), `POINTS-OUVERTS.md` (D12 tranché),
  `VALEURS-DE-DESIGN.md` §4 (zone tactile Android).
- `specs/SPEC_ANDROID.md` :
  - §3 et §4 : les limites vont dans l'aide ;
  - §6 : ligne `:feature:setup` ;
  - §13 et §13.1 : « onboarding » devient « diagnostic de la mise en route » ou « aide » ;
  - §15 : liste des écrans, paragraphe de l'écran 1, compte des destinations ;
  - §15 « Règles UI » : vérification TalkBack et zone tactile de 48 dp ;
  - §19 : trois critères amendés.
- `docs/android/RELEASE_REPORT.md`, critère 25 : annoté « remplacé le 2026-10-20 ».
- Plan de refonte :
  - D12 tranché ;
  - contrainte globale « Accessibilité » ;
  - ligne TalkBack de l'étape 55 élargie à une revue complète ;
  - cases de l'étape 35 cochées.
- `CLAUDE.md` n'est pas modifié.

## Tests écrits

- `PresentationTextsTest` (5) : égalité stricte, titres sans point, aucune apostrophe droite,
  aucun vouvoiement.
- `PresentationViewModelTest` (7) :
  - ordre des panneaux et bornes ;
  - écriture unique de l'accusé, y compris au double tap ;
  - panneau 1 incapable d'acquitter ;
  - panneau restauré après une mort du processus.
- `HomeViewModelTest` (+4) : `presentationRequired` vrai sans accusé ; faux avant la lecture,
  avec un accusé, avec une session en cours ou un stockage illisible.
- `HomeDestinationTest`, `LauncherDestinationTest` : `Onboarding` devient `Presentation`.
- `PresentationScreenTest` (8, instrumenté) :
  - textes, dessin, repère et logo de chaque panneau ;
  - flèche absente sur le panneau 1 ;
  - chaque bouton n'appelle que son action ;
  - contrôles d'accessibilité sur les deux panneaux.
- `NiumiComponentsSemanticsTest` (+1, et contrôles d'accessibilité sur chaque composant affiché) :
  chaque dessin est annoncé une fois comme image.
- `FakeSetupPreferences` (setup) compte les acquittements.

Le seul test vu rouge pour une autre raison que l'absence de code :
`aDurationPillIsARadioButtonThatSaysWhetherItIsChosen`. Il a échoué sur la zone tactile de 46 dp,
puis est passé après la décision 5.

## Commandes exécutées

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest
#   1 462 tests JVM, 0 échec
./gradlew ktlintFormat && ./gradlew ktlintCheck detekt :app:lintDebug
#   detekt : 47 MagicNumber dans les dessins (coordonnées des SVG : @file:Suppress commenté, comme
#   Sha256 et EventFingerprint) ; LongMethod et MatchingDeclarationName sur PresentationScreen
#   (contenu du panneau extrait, PresentationActions déplacé) ; MaxLineLength (KDoc) ; puis vert
./gradlew :app:assembleDebug
./gradlew connectedDebugAndroidTest              # voir ci-dessous
grep -rn "OnboardingTexts" androidApp                                          # vide
grep -rn "Color(0x" androidApp --include='*.kt' | grep -v NiumiPalettes.kt     # vide
grep -rn "isSystemInDarkTheme\|NiumiColors\|DayNight" androidApp --include='*.kt' --include='*.xml'   # vide
```

Deux avertissements de dépréciation de mes nouveaux fichiers ont été corrigés au lieu de
recopier l'ancien motif : `hiltViewModel` pris dans `androidx.hilt.lifecycle.viewmodel.compose`, et
`createComposeRule` v2.

**Suite instrumentée.**

| Module | Résultat |
| --- | --- |
| database | 90/90 |
| system | 29/29 |
| ringing | 6/6 |
| session | 3/3 |
| setup | 46/46 |
| designsystem | 38/38 |
| app | 13/14 |

- **setup et designsystem** : résultats après les ajouts d'accessibilité.
- **app** : l'échec est `BlockingGuardServiceInstrumentedTest.theGuardReachesTheForegroundWithItsNotificationThenStops`.
  C'est l'échec instable du service de garde A8, déjà relevé dans `ETAPE-34.md` et reproduit sans
  les changements de l'étape 34. Non traité ici.
- **Installations refusées** : HyperOS a refusé plusieurs installations
  (`INSTALL_FAILED_USER_RESTRICTED`) tant que la fenêtre « Installer via USB » restait sans
  réponse. Les modules concernés ont été relancés une fois la fenêtre acceptée.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, navigation à trois
boutons, densité 520. Données effacées avant le premier essai. Essais pilotés par `adb`, état
relevé par `uiautomator` et captures.

| Essai | Résultat |
| --- | --- |
| 1. Première ouverture | **OK** : panneau 1 directement, conforme à la maquette 01 |
| 2. « Continuer » | **OK** : panneau 2, conforme à la maquette 02, titre sans point |
| 3. Flèche, puis Continuer, puis retour système | **OK** : panneau 1 à chaque retour |
| 4. Retour système sur le panneau 1 | **OK** : Niumi se ferme, l'app précédente revient |
| 5. Réouverture | **OK** : panneau 1 |
| 6. Rotation sur le panneau 2, aller et retour | **OK** : le panneau 2 reste. En paysage, le dessin est coupé par la zone de défilement, et titre et texte se lisent en faisant défiler (voir « Limites ») |
| 7. « Configurer Niumi » | **OK** : diagnostic actuel (« Vérifions ton appareil ») |
| 8. Retour depuis le diagnostic | **OK** : ancien accueil (« Aucune session ») |
| 9. Arrêt forcé et relance | **OK** : accueil, la présentation ne revient pas |
| Marges du cadre (reste de l'étape 34) | **OK** en navigation à trois boutons : bouton posé sur la barre (47 dp), repère à 18 du bouton, haut de la rangée du logo à 56 |
| Après la zone de 48 dp | **OK** : logo 2 dp plus bas, au même endroit sur les deux panneaux ; tracé de la flèche inchangé (1 px d'arrondi) ; zone de la flèche 48 × 48 ; la flèche répond |

Les réglages de rotation du téléphone, modifiés pour l'essai 6, ont été rétablis (rotation
automatique activée).

## Limites et points d'attention

- **Paysage.** Le dessin de 240 de haut ne tient pas avec l'en-tête et le bouton : le haut du
  contenu reste visible, le reste défile. SPEC_ANDROID interdit de verrouiller l'orientation ; les
  maquettes ne prévoient que le portrait.
- **Dessin de l'écran 10.** `SPECIFICATION.md` §4 dit que le panneau 2 et l'écran 10 ont le même
  dessin, mais la maquette 31 trace une variante (`viewBox` 342 × 220, trait 3,5). L'étape 38
  tranchera entre une variante de `PhoneNearSpotDrawing` et un dessin unique.
- **Cadenas du panneau 1** tracé à l'encre : correct en clair, seul thème de l'écran 1. En sombre,
  l'encre serait claire sur l'ambre ; à revoir si le dessin servait un jour en sombre.
- **Espacement des voisins.** Les composants de 44 réservent désormais 48. L'accueil (étape 45),
  avec ses sept pastilles de jour, devra en tenir compte dans sa rangée.

## Validations restantes

- **Navigation par gestes** : le bouton principal doit être à 34 dp du bas. Seule la navigation à
  trois boutons a été mesurée.
- **TalkBack** : pas de passage manuel sur l'écran 1, par décision. Il est couvert par la revue
  complète de l'étape 55.
- **Échec instable de `:app`** (service de garde A8), préexistant, hors de l'étape.

**Rien n'est committé.**
