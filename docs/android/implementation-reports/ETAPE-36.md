# Étape 36 — Écran 2 · Autorisations

Date : 2026-10-20 au 2026-10-22. **Étape terminée** : code, tests JVM et instrumentés, documents,
onze essais manuels conformes sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS
OS3.0.302.0.WPPEUXM, navigation à trois boutons). Quatrième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 11, phase N). Point de départ :
`b493bce` (étape 35).

## Ce qui est livré

- **Écran 2 · Autorisations** (`:feature:setup`, `com.niumi.feature.setup.permissions`).
  - Il remplace le diagnostic à quatorze lignes (`ReadinessScreen`, supprimé) dans le parcours.
  - Contenu : flèche « Retour », titre qui compte les tuiles (« Cinq autorisations » sur HyperOS,
    « Quatre autorisations » ailleurs), texte « Sans elles, Niumi ne peut pas fonctionner. »,
    tuiles, puis « Continuer », inactif tant qu'une tuile manque.
  - Tuiles Android, dans l'ordre : Alarme, Blocage des applications, Batterie, Verrouiller Niumi,
    Notifications. Les deux du milieu sont propres à Android ; « Verrouiller Niumi » n'apparaît
    que là où le verrou des récents existe.
  - Tuile : icône à l'encre, nom, phrase ; coche « Accordée » à l'encre dans une pastille ambre
    de 28, ou bouton « Autoriser » de 46 (48 réservés).
  - Pas de « ? » ni de guide : M1 reporté, faute de textes et d'images Android.
- **Agrégat des contrôles** (`:core:system`, `com.niumi.system.permissions`).
  - `permissionStatusesOf(report)`, fonction pure : chaque tuile est accordée quand aucun de ses
    contrôles de §13 n'est en échec ; `pending` donne le premier manque.
  - Une tuile sans objet sur l'appareil n'est pas affichée (verrou des récents hors HyperOS).
  - Service d'accessibilité inscrit mais pas encore relié (fenêtre `UnlockSettling` de 30 s) :
    la tuile reste à faire et l'écran se relit toutes les 3 s jusqu'au verdict.
- **« Autoriser », un manque à la fois.**
  - Blocage : fenêtre de consentement (divulgation Google Play, §12.3), puis réglages.
  - Batterie : demande directe d'exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), un
    toucher ; liste système en repli si la fenêtre n'existe pas (`openRecourse`).
  - Verrouiller Niumi : fenêtre qui explique le geste, puis « J’ai verrouillé Niumi » relit.
  - Notifications : fenêtre système ; réglages de notification de Niumi quand Android ne la montre
    plus (demande déjà faite, persistée, et pas de justification à afficher) et avant Android 13.
  - Alarme exacte : fenêtre d'explication seule (`USE_EXACT_ALARM`, rien à demander).
- **Fenêtre d'alerte** (`:core:designsystem`, `NiumiAlertDialog`) : maquette 57 dans la mise en
  page de Mehdi du 2026-10-22 (croix ronde au coin, icône dans un rond gris, titre centré sur
  plusieurs lignes, texte à gauche, bouton de 52). Croix, toucher à côté et retour ferment ; une
  fenêtre se ferme d'elle-même quand son manque disparaît.
- **Bouton** : `NiumiPrimaryButton` gagne `NiumiButtonSize` (`Primary`, `Authorize`, `Sheet`).
- **Récapitulatif** : un refus d'activation nomme le contrôle en cause par son message de §13
  (Ne pas déranger, volume, NFC n'ont pas de tuile) au lieu de « Reviens au diagnostic ».
- **`ReadinessMessages`** déplacé de `:feature:setup` vers `:core:system` (apostrophe ’).
- **Navigation transitoire** jusqu'à l'étape 45 : Présentation → Autorisations → Association →
  Applications → Heure → Récapitulatif. Routes `Readiness` → `Permissions` ; `AccessibilityConsent`
  supprimée. Le tap des avertissements §13.1 reste sur le diagnostic d'incident (Q2).
- **Manifeste** : dixième permission, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **Supprimés** : `ReadinessScreen`, `ReadinessViewModel`, `ReadinessUiState`,
  `ReadinessActionIntents`, `AccessibilityConsentScreen`, son ViewModel et son état, leurs tests.

## Défaut corrigé au passage

La route de consentement existait, mais aucun écran n'y menait : le diagnostic ouvrait les
réglages d'accessibilité sans divulgation, ce que la politique Google Play n'admet pas. La fenêtre
de consentement est désormais le seul chemin vers ces réglages depuis la mise en route.

## Décisions de Mehdi

| Date | Point | Décision |
| --- | --- | --- |
| 2026-10-20 | Q1 | Batterie et verrou des récents demandés par l'écran 2 ; Ne pas déranger et volume sans écran |
| 2026-10-20 | Q2 | Tap des avertissements §13.1 inchangé jusqu'à l'étape 52 |
| 2026-10-20 | M1, D18 | Guide « ? » reporté ; D18 sans objet |
| 2026-10-20 | D10 | Pas de tuile NFC |
| 2026-10-21 | Écran 2 | Texte « Sans elles, Niumi ne peut pas fonctionner. » ; tuiles dédiées Batterie et Verrouillage ; titre qui compte les tuiles |
| 2026-10-21 | Batterie | Demande directe d'exemption, permission déclarée, incertitude Play assumée |
| 2026-10-21 | Fenêtres | Fenêtre centrée, croix et toucher à côté, pour le consentement, le verrou et l'alarme exacte |
| 2026-10-22 | Fenêtres | Mise en page : croix ronde, icône, titre centré, texte à gauche |
| 2026-10-22 | Nom | « Verrouiller Niumi » pour la tuile et la fenêtre |
| 2026-10-22 | Consentement | Cinquième point « Sans cette autorisation, Niumi ne peut pas fonctionner. » ; chemin générique « Dans les réglages, trouve Niumi et active-le. » |
| 2026-10-22 | Coche | Essai d'icônes ambre abandonné ; coche à l'encre dans une pastille ambre (exception écrite à D14, VALEURS §2) |
| 2026-10-22 | DataStore | Montée en 1.3.0, changement à part de l'étape |
| 2026-10-22 | Batterie | Relecture toutes les 500 ms, six fois au plus, après « Autoriser » : HyperOS reporte le choix après le retour |
| 2026-10-22 | Écran 2 | 16 au moins entre la dernière tuile et « Continuer » (écart à la maquette 03) ; marge du bas déjà revue à l'étape 38 |
| 2026-10-22 | Défilement | Fondu de 32 sur les tuiles cachées sous « Continuer » ; exception au « seul dégradé » écrite dans VALEURS §2 |

## Relecture indépendante

Un agent a relu le code avant les essais. Quatre défauts sérieux, tous corrigés avec un test vu
rouge : tuile Blocage cochée à tort pendant la fenêtre de liaison ; bouton « Autoriser » sans zone
tactile de 48 ; « Autoriser » des notifications sans effet après un refus définitif ; avant
Android 13, ouverture de la page du canal au lieu des réglages de notification. Points mineurs
laissés : double tap sur une flèche ou « Continuer » (motif existant ailleurs), textes de l'aide
et de `LIMITES.md` qui parlent encore du « diagnostic » (étape 53), pas de test de la navigation.

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md` : §12.3 (fenêtre de consentement, textes), §13 (présentation en tuiles,
  exemption d'énergie, fenêtres, notifications, `ReadinessMessages`), §13.1, §14 (dix
  permissions), §15 (écran 2, navigation transitoire, routes).
- `design-plan/SPECIFICATION.md` §5, `TEXTES-DES-MAQUETTES.md` §03, `POINTS-OUVERTS.md` (D10, D18,
  M1, M8, E6).
- `docs/android/play-console/` : `ACCESSIBILITY_DECLARATION.md`, `REVIEW_VIDEO_SCRIPT.md`, nouveau
  `BATTERY_OPTIMIZATION_EXEMPTION.md`.
- Plan de refonte : décisions de l'étape 36, vigilance 14 corrigée, interfaces, cases.
- `gradle/libs.versions.toml` : DataStore 1.3.0 (changement distinct, voir plus bas).
- `CLAUDE.md` n'est pas modifié.

## Tests

- JVM : 1 485, aucun échec. Nouveaux ou réécrits : `PermissionStatusesTest`,
  `PermissionsViewModelTest`, `PermissionsTextsTest`, `NotificationRequestTest`,
  `AccessibilityConsentTextsTest`, `ReadinessMessagesTest` (déplacé), `SummaryTextsTest`,
  `SummaryViewModelTest`, `DataStoreUnlockGuardTest`, `ReleaseHygieneTest` (dix permissions).
- Instrumentés, tous modules : 231 sur 231 (app 14, database 90, designsystem 42, system 32,
  ringing 6, session 3, setup 44). Le test instable du service de garde A8 est passé.
  Nouveaux : `PermissionsScreenTest`, fenêtre d'alerte et zone tactile dans
  `NiumiComponentsSemanticsTest`, `ReadinessSettingsIntentsTest` (demande directe et repli),
  relecture de la préférence de notification.
- Vus rouges sur appareil : zone tactile d'« Autoriser » (contrôles d'accessibilité), titre de
  fenêtre coupé à 48 au lieu de 52, pastille de la coche à 24 au lieu de 28 (réduite le temps de
  l'essai, puis remise).
- ktlint, detekt, lint : verts (lint vert après la montée de DataStore, voir plus bas).

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, navigation à trois
boutons.

| Essai | Résultat |
| --- | --- |
| 1. « Configurer Niumi » | **OK** : écran 2, Alarme cochée, conforme à la maquette 03 |
| 2. Refus des notifications | **OK** : tuile non cochée, « Continuer » grisé, rien d'autre |
| 3. Second refus puis troisième « Autoriser » | **OK** : réglages de notification de Niumi |
| 4. Accord des notifications | **OK** : coche au retour |
| 5. Consentement puis service activé | **OK** : retour à l'écran 2 ; tuile non cochée car batterie manquante (version agrégée) |
| 6. Batterie (liste système) | **OK** mais trois touchers sur Android 16 : à l'origine de la demande directe |
| 7. Batterie, demande directe | **OK** : fenêtre système, un toucher, coche |
| 8. Service planté par une réinstallation | **OK** : tuile « Autoriser » (vrai état, « Crashed services »), rétablie par désactiver/réactiver |
| 9. Verrou des récents retiré puis remis | **OK** : tuile à faire, explication, coche |
| 10. Fenêtre de consentement | **OK** : titre entier, croix et toucher à côté ferment, fermeture d'elle-même au retour |
| 11. Fenêtre du verrou | **OK** |
| DataStore 1.3.0 sur données écrites en 1.2.1 | **OK** : accueil direct (accusé relu), fichier intact |

## Montée de DataStore (changement distinct)

DataStore 1.3.0 (sortie le 2026-10-21) faisait échouer le lint du projet (`GradleDependency`).
Notes officielles : nouveautés hors usage de Niumi (web, Tink, `DataStore.Builder`), corrections
utiles (course lecture-écriture, renommage du fichier temporaire), chemin du fichier dans les
messages de `CorruptionException`, aucun changement de format annoncé. Vérifié : relecture des
données de la 1.2.1 sur l'appareil, tests instrumentés de `:core:system` et `:core:database`. À
committer à part de l'étape 36, comme `314b2b9`.

## Restes et points d'attention

- **Google Play** : la demande directe d'exemption et la divulgation d'accessibilité restent des
  incertitudes jusqu'à la soumission.
- **Icônes Batterie et Verrouiller Niumi** : dessinées à l'étape, sans maquette.
- **Libellés des réglages** d'accessibilité : non nommés à dessein (constructeurs).
- **TalkBack** : pas de passage manuel (écran non listé « à risque ») ; revue complète à l'étape 55.
- **Navigation par gestes** : non mesurée.
- **Relecture de la batterie après ses réglages** (ajoutée le 2026-10-22, après l'étape) : tests JVM
  verts, retour immédiat depuis la page HyperOS pas encore essayé sur appareil.

**Rien n'est committé.**
