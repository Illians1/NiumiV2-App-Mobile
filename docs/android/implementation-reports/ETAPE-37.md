# Étape 37 — Écran 4 · Choix des applications

Date : 2026-10-22. **Étape terminée** : code, tests JVM et instrumentés, documents, essais manuels
conformes sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, navigation à
trois boutons). Cinquième étape du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md`
(Lot 11, phase N). Point de départ : `ffb267f` (étape 36).

## Ce qui est livré

- **Écran 4 · Choix des applications** (`:feature:setup`, `com.niumi.feature.setup.apps`).
  - Il remplace le sélecteur en liste (`AppPickerScreen`, son ViewModel, ses textes et leurs
    tests, supprimés ; route `NiumiRoute.AppPicker` → `AppSelection`).
  - Titre, texte, carte centrée, mention du bas et « Continuer », inactif tant qu'aucune sélection
    n'est validée (maquettes 12 et 14).
  - La carte montre la sélection **validée**, jamais les coches en cours (Q3). Avant le choix :
    six cases pointillées, « Aucune application choisie », pilule « Choisir les applications ».
    Ensuite : les vraies icônes, « N applications choisies » (singulier à 1), pilule « Modifier ».
  - Six cases au plus ; au-delà de six applications, cinq icônes puis « +N ». Sur un écran trop
    étroit, moins de cases (cinq à 360 dp, jugé bien par Mehdi).
- **Page « Applications à bloquer »**, en pleine page (la maquette 13 dessine une feuille du bas,
  remplacée sur décision de Mehdi).
  - Titre, croix « Fermer sans enregistrer », champ « Rechercher » avec une croix qui le vide,
    grille de quatre colonnes (vraies icônes, coche dans une pastille encre), compteur, « Valider ».
  - Section « Applications déjà choisies » en tête (les applications cochées à l'ouverture), puis
    « Toutes les applications ». Rien ne change de place pendant la visite.
  - Les coches forment un brouillon : la croix et le retour l'abandonnent ; seul « Valider » écrit
    `niumi_app_selection`, une fois, puis la page se ferme.
  - Limite commune de 50 : les cases non cochées sont grisées et inactives, le compteur dit
    « 50 applications choisies, c’est le maximum. ».
  - Recherche sur le nom, sans tenir compte de la casse ni des accents ; une coche masquée par la
    recherche reste comptée.
  - États non dessinés : indicateur de chargement (cercle encre, après 300 ms, lu « Chargement des
    applications »), « Aucune application ne correspond. », « Aucune application à bloquer sur ce
    téléphone. ».
  - Page ouverte, brouillon, section et recherche survivent à la rotation et à une mort du
    processus (`SavedStateHandle`).
- **Chargement plus rapide** (`:core:system`) : noms et icônes lus en parallèle, huit à la fois,
  dans l'ordre de la requête. Mesuré sur l'appareil, 125 applications, processus neuf : 2,5 s
  avant, 0,6 s après. Les icônes ne sont converties qu'à l'affichage, une fois par taille
  (`AppIconCache`).
- **Design system** : `NiumiLoadingIndicator`, `NiumiOpacity`, `NiumiCloseButton` rendu public,
  `ic_search`, formes et mesures de l'écran 4.
- **`ReadinessAction.OpenAppPicker`** renommé `OpenAppSelection` (`grep AppPicker` vide).

## Corrections apportées à l'étape 36 pendant les essais

- **Tuile Batterie cochée alors que la page Xiaomi affiche une option restrictive.** Ce n'est pas
  un défaut de Niumi. Sur HyperOS, la demande d'exemption ouvre la page « Économiseur de batterie »
  de Xiaomi. Quand on y change vite d'option, son composant `DeviceIdlePolicyHelper` retire Niumi
  de la liste blanche d'Android puis l'y remet 2 ms plus tard (journal de
  `com.miui.powerkeeper`, reproduit deux fois). Niumi lit la liste blanche, qui seule empêche le
  gel (mesuré à l'étape 25) : la restriction qui compte est bien levée. Limite écrite dans
  SPEC_ANDROID §13. Sur décision de Mehdi, la phrase de la tuile devient « Retire les restrictions
  de batterie, pour que le téléphone n’arrête pas Niumi pendant la nuit. ».

## Changement à part : logo

Les sources `design-plan/logo/*` ont été modifiées le 2026-10-22 à 10:55, hors de cette session
(point ambre de rayon 18 au lieu de 14,5, cadre « 0 2 510 128 » au lieu de « 0 9 506.5 121 »).
Sur décision de Mehdi, les logos clair et sombre de l'app et les marges de l'écran de démarrage
(42,09 %) sont mis à jour. `NiumiBrandAssetsTest` et `NiumiLaunchThemeTest` lisent désormais le
cadre dans le SVG : un prochain changement des sources sera détecté. À committer à part de
l'étape 37.

## Décisions de Mehdi (2026-10-22)

| Point | Décision |
| --- | --- |
| Q3 | La carte montre la sélection validée ; le compteur de la liste suit les coches en cours |
| Limite de 50 | Cases non cochées grisées et inactives, compteur « 50 applications choisies, c’est le maximum. » |
| Carte | Six cases au plus, « +N » au-delà ; quatre icônes et « +N » à 360 dp, c'est bien |
| États vides | Textes « Aucune application ne correspond. » et « Aucune application à bloquer sur ce téléphone. » |
| Liste | En pleine page, croix à droite, au lieu de la feuille de la maquette 13 |
| Section | « Applications déjà choisies » en tête, puis « Toutes les applications » |
| Chargement | Cercle seul, lu « Chargement des applications » par TalkBack |
| Recherche | Croix « Effacer la recherche » |
| Batterie | Phrase de la tuile revue ; limite HyperOS écrite dans SPEC_ANDROID §13 |
| Logo | Mettre l'app à jour d'après les nouvelles sources |
| Mineurs de la relecture | Tous traités |

Arbitrages pris sans question, signalés ici : « 1 application choisie » et « Aucune application
choisie » à zéro ; recherche insensible aux accents ; nom de package sous le libellé en cas de
doublon ; grisé = icône en niveaux de gris à 38 % ; carte de l'écran 4 à coins de 20 et bord
`disabled` (sources des maquettes) ; case « +N » dessinée sur le modèle des autres.

## Relecture indépendante

Un agent a relu le code avant les essais : aucun défaut critique, trois importants, tous corrigés
avec un test vu rouge.

- « Valider » actif pendant le chargement qui suit une mort du processus : il écrivait une
  sélection vide.
- Feuille de hauteur nulle en paysage (sans objet depuis la pleine page).
- Test « +N » dépendant de la largeur de l'appareil.

Mineurs, traités à la demande de Mehdi :
- fermeture animée après « Valider » (sans objet depuis la pleine page) ;
- trait du pied aligné sur la marge réelle ;
- icônes converties une seule fois.

La course « croix puis Valider pendant l'animation » n'a pas été reproduite : un test témoin
prouve que le clic arrive, et Material 3 ignore déjà le contenu pendant la fermeture. Les deux
tests sont gardés.

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md` :
  - §12.1 : brouillon, « Valider », limite, recherche, section, lecture parallèle, T9 clos ;
  - §13 : limite HyperOS de la page batterie ;
  - §15 : écran 4, page, navigation transitoire, logo de démarrage.
- `design-plan/SPECIFICATION.md` §5 (phrase Batterie) et §8 (décisions du jour, pleine page,
  section, croix, chargement).
- `design-plan/POINTS-OUVERTS.md` : T9 résolu.
- `docs/android/play-console/BATTERY_OPTIMIZATION_EXEMPTION.md` : nouvelle phrase.
- `docs/android/RELEASE_REPORT.md` : critère 16 cité sur `AppSelectionViewModelTest`.
- Plan de refonte : cases de l'étape 37.
- `CLAUDE.md` n'est pas modifié.

## Tests

- **JVM** : 1 515, aucun échec. Nouveaux : `AppSelectionTextsTest` (10),
  `AppSelectionViewModelTest` (28), `AppIconCacheTest` (2). Modifiés : `PermissionsTextsTest`,
  `NiumiBrandAssetsTest`, `NiumiLaunchThemeTest`, `PackageManagerInstalledAppsSourceTest`,
  `AndroidDeviceReadinessCheckerTest`.
- **Instrumentés**, derniers passages sur l'appareil : app 14, database 90, designsystem 45,
  system 33, ringing 6, session 3, setup 55, soit 246 sur 246.
  - Nouveaux : `AppSelectionScreenTest` (avec contrôles d'accessibilité automatiques),
    `NiumiLoadingIndicatorTest`, `PackageManagerPackageQueryTest`, deux cas de
    `NiumiBottomSheetTest`.
  - Un passage a échoué quatre fois parce que le téléphone était passé en paysage (rotation
    automatique activée) ; tout passe en portrait.
  - HyperOS a plusieurs fois refusé d'installer l'APK de test (`INSTALL_FAILED_USER_RESTRICTED`) :
    il faut accepter la fenêtre d'installation.
- **ktlint, detekt, lint** : verts. Aucune couleur en dur hors `NiumiPalettes.kt` ; `<queries>`
  inchangé.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, 394 dp de large.

| Essai | Résultat |
| --- | --- |
| 1. Écran 4 avant le choix | **OK** : six cases, « Aucune application choisie », « Continuer » grisé |
| 2. Cocher 6 applications, fermer par la croix | **OK** : rien d'enregistré |
| 3. Cocher 6 applications, « Valider » | **OK** : vraies icônes, « 6 applications choisies », « Modifier » |
| 4. « Modifier » | **OK** : section « Applications déjà choisies » en tête |
| 5. Recherche et croix d'effacement | **OK** |
| 6. Chargement | **OK** : nettement plus court, indicateur |
| 7. Rotation, liste ouverte | **OK** : coches et recherche conservées, trait jusqu'aux bords |
| 8. Retour système, liste ouverte | **OK** : rien d'enregistré |
| 9. « Continuer » | **OK** : choix de l'heure |
| 10. Relecture après fermeture de l'app | **OK** : même sélection |
| Feuille du bas (première version) | **Refusée** : quatre rangées visibles, titre sous la barre d'état → pleine page |
| Tuile Batterie après un changement rapide sur la page Xiaomi | Limite HyperOS mesurée, phrase revue |

## Restes et points d'attention

- **Limite de 50** : couverte par les tests JVM et instrumentés, pas essayée à la main.
- **M11** (écran 4 ouvert depuis l'accueil, et son retour) : arrive avec l'accueil, à l'étape 45.
- **TalkBack** : pas de passage manuel (écran non listé « à risque ») ; revue complète à
  l'étape 55.
- **Google Play** : la demande directe d'exemption reste une incertitude jusqu'à la soumission.
- **Données de l'app** : les tests instrumentés désinstallent l'app ; il faut refaire la mise en
  route après chaque passage complet.

**Rien n'est committé.**
