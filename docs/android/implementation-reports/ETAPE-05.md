# Étape 5 — blocage par AccessibilityService, overlay et page de consentement

Date : 2026-09-06. Tests automatisés, vérifications statiques **et validation sur appareil réel**
(Redmi 25080RABDG, HyperOS, Android 16 / API 36 — le même appareil qu'aux étapes 3 et 4).

**Trois défauts ont été trouvés pendant la validation matérielle, aucun visible en test
automatisé** (voir « Défauts trouvés sur appareil ») : deux bugs de production corrigés dans le
code, et un troisième dont la cause s'est révélée être l'optimisation de batterie MIUI, réglé
côté appareil. Le protocole manuel a ensuite été déroulé intégralement et passe.

Il reste à répercuter les constats dans les specs (§12.2 et §13) et à écrire le test instrumenté
de bout en bout dans `:app`.

## Résumé

Dernier composant du Lot 0 : `NiumiBlockingAccessibilityService` (`:feature:session`) renvoie à
l'accueil toute application sélectionnée passant au premier plan (SPEC_ANDROID §12.2), affiche
un overlay explicatif, et l'écran de consentement (`:feature:setup`, §12.3) précède toute
ouverture des réglages d'accessibilité. La logique de décision (`BlockingDecision`) et le
parsing (`EnabledAccessibilityServicesParser`) vivent dans `:core:system`, purs et testés en
JVM ; le service, l'overlay (`WindowManager`) et le contrôleur restent dans `:feature:session`,
qui seul connaît le nom qualifié du service.

Une décision a nécessité un écart au plan MVP (« Interfaces transverses »), validée avec
l'utilisateur avant l'implémentation, et deux points ont été volontairement reportés — voir
« Décisions validées avec l'utilisateur ».

## Décisions validées avec l'utilisateur

1. **`BlockingController.apply()` prend `Set<BlockedPackage>` et non `Set<String>`.** §12.2
   impose le texte « {Nom de l'application} reste bloquée jusqu'au scan du boîtier. » ; la
   signature d'origine ne faisait circuler aucun libellé. `BlockedPackage(packageName,
   displayNameSnapshot)` — le type déjà prévu par le plan pour `:core:database` — fige le
   libellé à l'activation plutôt que de le résoudre via `PackageManager` au moment de
   l'affichage. Le bloc « Interfaces transverses » du plan MVP a été mis à jour dans le même
   changement.
2. **`ForegroundAppSource` n'a pas été créé.** L'algorithme §12.2 décide entièrement dans
   `onAccessibilityEvent` ; aucun consommateur de production n'existe avant le coordinateur
   (étape 15-17). CLAUDE.md interdit de documenter à l'avance une architecture inexistante — la
   ligne correspondante du plan MVP a été annotée en conséquence plutôt que supprimée en
   silence.
3. **Portée du service laissée à la configuration exacte de §12.2** (pas de
   `serviceInfo.packageNames` dynamique). La liste bloquée change à chaque session ; la
   restreindre dynamiquement dépend du coordinateur et son comportement varie selon les
   surcouches. Piste consignée ci-dessous pour l'étape 6 (dossier Play), non implémentée.

## Ce qui a été construit

**`:core:database`** — `BlockedPackage(packageName, displayNameSnapshot)`, seul type nouveau
partagé (repris tel quel par `AndroidSessionExtras` à l'étape 9).

**`:core:system` — `com.niumi.system.blocking`** (motif « descripteurs purs », établi aux
étapes 3-4) :
- `BlockedPackagesState` (`Inactive`/`Active`/`Releasing`), `BlockedPackagesProjection`
  (lecture), `InMemoryBlockedPackagesProjection` (écriture, `@Singleton`, remplacée par une
  lecture Room à l'étape 15 sans changer l'interface — même motif que
  `InMemoryTechnicalEventLog`).
- `BlockingDecision` : algorithme pur de §12.2, aucune dépendance Android.
- `EnabledAccessibilityServicesParser` : parsing de
  `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, formes qualifiée et relative.
- `AccessibilityServiceStatus` (interface) + `AndroidAccessibilityServiceStatus` (impl).
- `BlockingController` (interface, écart de décision 1).
- `di/BlockingModule.kt` : lie uniquement `BlockedPackagesProjection` ; `AccessibilityServiceStatus`
  et `BlockingController` ont besoin du nom qualifié de `NiumiBlockingAccessibilityService`
  (`:feature:session`), que `:core:system` ne peut pas référencer (SPEC_ANDROID §6).

**`:feature:session` — `com.niumi.feature.session.blocking`** :
- `NiumiBlockingAccessibilityService` : lit uniquement `event.packageName`, jamais
  `getRootInActiveWindow()`, texte ou saisie.
- `BlockOverlayController` (interface) + `WindowManagerBlockOverlayController` :
  `TYPE_ACCESSIBILITY_OVERLAY` non focusable/non touchable, bandeau `WRAP_CONTENT` en haut
  d'écran (jamais plein écran), retrait automatique à 3 s, couleurs
  `docs/CHARTE_GRAPHIQUE_APP_MOBILE.md` (fond Surface, texte clair, rayon modéré, sans capsule
  ni Terracotta — le blocage est le fonctionnement attendu, pas une alerte).
- `AndroidBlockingController` : traduit `BlockingController` vers la projection et le
  diagnostic.
- `di/SessionBlockingModule.kt` : lie les trois bindings ci-dessus, `expectedComponent` construit
  dynamiquement (`context.packageName` + `NiumiBlockingAccessibilityService::class.java.name`),
  pas de nom de package en dur.
- `res/xml/niumi_accessibility_service.xml` (config exacte de §12.2 + `android:description`),
  `res/values/strings.xml`, manifeste (`exported=true`, `BIND_ACCESSIBILITY_SERVICE`, pas de
  Direct Boot).

**`:feature:setup` — `com.niumi.feature.setup.accessibility`** :
- `AccessibilityConsentTexts` : les cinq points de §12.3 mot pour mot, libellé du bouton,
  `SETTINGS_DESCRIPTION` (doit rester identique au `android:description` XML ci-dessus —
  aucune dépendance feature-à-feature n'existe pour partager une seule source ; les deux
  littéraux sont synchronisés à la main, chacun documenté vers l'autre).
- `AccessibilityConsentUiState`, `AccessibilityConsentViewModel` (recalcule sur `refresh()`,
  jamais automatiquement).
- `AccessibilityConsentScreen` (composable pur, testé en isolation) +
  `AccessibilityConsentRoute` (wiring ViewModel + `ON_RESUME`, motif `PocScreen`).

**`:app` (`src/debug`)** : `PocScreen`/`PocViewModel` complétés (champ « Package à bloquer »,
Bloquer/Débloquer, état du service, résolution de libellé via `PackageManager` avec repli sur le
nom de package) ; route `poc/accessibility` ouvrant l'écran de consentement réel ; `<queries>`
MAIN/LAUNCHER ajouté au manifeste debug (§12.1), qui passera en `main` au Lot 2 avec le vrai
sélecteur.

Les deux fichiers `PackageInfo.kt` (repères vides de `:feature:session` et `:feature:setup`)
ont été supprimés : devenus inutiles une fois du code réel présent dans ces modules.

## Décisions supplémentaires prises pendant l'implémentation

1. **`WindowManager.LayoutParams` construit puis muté**, plutôt qu'un chaînage
   `.apply { ... }` après un appel multi-lignes : ktlint rejetait systématiquement
   l'indentation du bloc chaîné après un constructeur étalé sur plusieurs lignes. Deux
   affectations de champs, aussi lisibles.
2. **`BlockingDecision.decide` et `EnabledAccessibilityServicesParser.isEnabled` réécrits en un
   seul point de sortie** (detekt `ReturnCount`, limite 2) : `decide` calcule d'abord la liste
   effective puis retourne un unique `if`/`else` ; `isEnabled` regroupe ses deux gardes en un
   seul `if` avant le `return` final.
3. **`PocScreen` scindé en `AlarmSection`/`BlockingSection`** (detekt `LongMethod`, limite 60
   lignes) : la fonction dépassait le seuil une fois les champs de blocage ajoutés. Un
   `verticalScroll` a aussi été ajouté au conteneur, le contenu ne tenant plus nécessairement
   sur un petit écran.
4. **`android:description` séparé du texte de consentement.** §12.2 ne mentionne pas cet
   attribut, mais il est nécessaire à l'affichage dans les réglages système et la fiche Play
   (étape 6). Choix du plan, pas une citation de spec — voir décision 1's cross-référence.
5. **Aucun nouveau module Gradle** : `ModuleListTest` reste vert sans modification.

## Écart d'interface transverse

Voir décision 1. Répercuté dans
`docs/superpowers/plans/2026-09-03-mvp-android.md` (bloc « Interfaces transverses »,
`BlockingController`/`ForegroundAppSource`).

## Fichiers créés

- `androidApp/core/database/src/main/kotlin/com/niumi/database/BlockedPackage.kt`.
- `androidApp/core/system/src/main/kotlin/com/niumi/system/blocking/{BlockedPackagesState,
  BlockedPackagesProjection,InMemoryBlockedPackagesProjection,BlockingDecision,
  BlockingController,AccessibilityServiceStatus,AndroidAccessibilityServiceStatus,
  EnabledAccessibilityServicesParser}.kt`, `di/BlockingModule.kt`.
- Tests JVM `:core:system` : `blocking/{BlockingDecisionTest,
  EnabledAccessibilityServicesParserTest,InMemoryBlockedPackagesProjectionTest}.kt`.
- `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking/
  {NiumiBlockingAccessibilityService,BlockOverlayController,
  WindowManagerBlockOverlayController,AndroidBlockingController}.kt`, `di/SessionBlockingModule.kt`.
- `androidApp/feature/session/src/main/res/xml/niumi_accessibility_service.xml`,
  `res/values/strings.xml`.
- Test JVM `:feature:session` : `NiumiBlockingAccessibilityServiceSourceTest.kt` (garde-fou
  permanent, lit le source du service et échoue s'il contient `rootInActiveWindow`, `getText`,
  `contentDescription` ou `AccessibilityNodeInfo` — remplace le grep manuel du plan MVP, même
  mécanisme que `ModuleListTest`/`NiumiAlarmWavTest`).
- Tests instrumentés `:feature:session` : `HiltTestRunner.kt`,
  `blocking/NiumiBlockingAccessibilityServiceTest.kt`, `src/androidTest/AndroidManifest.xml`.
- `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/accessibility/
  {AccessibilityConsentTexts,AccessibilityConsentUiState,AccessibilityConsentViewModel,
  AccessibilityConsentScreen}.kt`.
- Test JVM `:feature:setup` : `accessibility/AccessibilityConsentTextsTest.kt`.
- Test instrumenté `:feature:setup` : `accessibility/AccessibilityConsentScreenTest.kt`,
  `src/androidTest/AndroidManifest.xml`.

## Fichiers modifiés

- `androidApp/feature/session/build.gradle.kts` : `testInstrumentationRunner` →
  `HiltTestRunner`, dépendances `hilt-android-testing`/`kspAndroidTest`, `tasks.withType<Test>`
  pour `niumi.rootDir` (garde-fou source).
- `androidApp/feature/session/src/main/AndroidManifest.xml` : déclaration du service.
- `androidApp/feature/setup/build.gradle.kts` : `lifecycle-runtime-compose`,
  `androidTestImplementation(libs.truth)`.
- `androidApp/app/src/debug/kotlin/com/niumi/app/poc/{PocScreen,PocViewModel,
  PocNavigation}.kt` : champs de blocage, route de consentement.
- `androidApp/app/src/debug/AndroidManifest.xml` : `<queries>` MAIN/LAUNCHER.
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : bloc « Interfaces transverses » (décision
  1 et 2 ci-dessus).
- Suppressions : `androidApp/feature/session/.../PackageInfo.kt`,
  `androidApp/feature/setup/.../PackageInfo.kt`.

## Commandes exécutées et résultat

Environnement : `JAVA_HOME=/opt/homebrew/opt/openjdk@17` (JDK Homebrew présent mais absent du
`PATH` par défaut dans cette session — à signaler si `./gradlew` échoue avec « Unable to locate
a Java Runtime » sur un futur poste).

| Commande | Résultat |
| --- | --- |
| `./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :feature:setup:testDebugUnitTest` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL |
| `./gradlew :shared:core:jvmTest` | BUILD SUCCESSFUL (non affecté, revérifié par précaution) |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew ktlintCheck` | BUILD SUCCESSFUL (après corrections détaillées ci-dessous) |
| `./gradlew detekt` | BUILD SUCCESSFUL (après corrections détaillées ci-dessous) |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL |
| `./gradlew :feature:session:compileDebugAndroidTestKotlin` `:feature:setup:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL (compilation seule : aucun appareil branché dans cette session) |

### Corrections ktlint appliquées pendant la vérification

- `EnabledAccessibilityServicesParserTest.relativeComponentFormIsEnabled` : le cas initial
  utilisait un composant dont le nom de classe ne partageait pas le préfixe de package du
  service réel (`com.niumi.app` vs `com.niumi.feature.session.blocking`), rendant la forme
  relative impossible à observer pour ce service précis. Remplacé par un composant fictif où
  la forme relative s'applique réellement, pour tester le comportement général du parseur.
- `AccessibilityConsentTexts` : `openSettingsButtonLabel`/`settingsDescription` renommés en
  `OPEN_SETTINGS_BUTTON_LABEL`/`SETTINGS_DESCRIPTION` (notation constante exigée pour un
  `const val` immuable).
- `AccessibilityConsentViewModel`, `PocScreen` (×2) : arguments/expressions repliés sur
  plusieurs lignes (longueur de ligne, un argument par ligne).
- `NiumiBlockingAccessibilityService` : branche `BlockAction.None` entourée d'accolades
  (cohérence avec la branche `GoHome`, multi-ligne).
- `WindowManagerBlockOverlayController.layoutParams` : chaînage `.apply` après un constructeur
  multi-lignes remplacé par une construction puis deux affectations de champs (voir « Décisions
  supplémentaires », point 1).

### Corrections detekt appliquées pendant la vérification

- `BlockingDecision.decide` : 4 `return` → 1 (`ReturnCount`, voir « Décisions
  supplémentaires », point 2).
- `EnabledAccessibilityServicesParser.isEnabled` : 3 `return` → 2 (même règle).
- `PocScreen` : fonction de 86 lignes → scindée en `AlarmSection`/`BlockingSection`
  (`LongMethod`, voir « Décisions supplémentaires », point 3).

## Défauts trouvés sur appareil (invisibles en test automatisé)

### 1. Corrigé — crash systématique à l'affichage de l'overlay

```
android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid
    at WindowManagerBlockOverlayController.show(...)
    at NiumiBlockingAccessibilityService.onAccessibilityEvent(...)
```

`WindowManagerBlockOverlayController` recevait l'`@ApplicationContext` par Hilt. Or une fenêtre
`TYPE_ACCESSIBILITY_OVERLAY` ne peut être ajoutée qu'avec le contexte du service d'accessibilité
lui-même, seul porteur du token correspondant.

Conséquence observée : **le premier blocage faisait planter Niumi**, et Android désactivait alors
le service d'accessibilité (un service dont le process est tué de force sort de
`enabled_accessibility_services`). Le blocage disparaissait donc entièrement au premier usage. La
notification « Niumi fermé de force » était le seul indice visible.

Correction : le service construit lui-même son overlay dans `onServiceConnected()` avec `this`
comme `Context` ; le binding Hilt correspondant a été retiré. En complément, `show()` renvoie
désormais un `OperationResult` (convention des adaptateurs du dépôt) et attrape
`BadTokenException` : **une exception dans `onAccessibilityEvent` supprime le blocage entier**,
l'overlay ne doit jamais pouvoir emporter la fonction qu'il explique. Un refus est journalisé en
`OEM_RESTRICTION_SUSPECTED` (§17).

### 2. Corrigé — overlay jamais visible, et contradiction de SPEC_ANDROID §12.2

Une fois le crash corrigé, le blocage fonctionnait mais aucun overlay n'apparaissait, et aucune
fenêtre `ty=ACCESSIBILITY_OVERLAY` n'existait dans `dumpsys window`.

Cause : `performGlobalAction(GLOBAL_ACTION_HOME)` provoque immédiatement un événement pour le
launcher, package non bloqué, donc `BlockAction.None`, donc `hide()` — l'overlay était retiré
quelques millisecondes après avoir été posé.

**C'est une contradiction interne de §12.2**, à trancher au niveau de la spec : l'algorithme
impose de renvoyer *toujours* à l'accueil, et impose en même temps que l'overlay « disparaisse
dès que le package bloqué n'est plus au premier plan ». Les deux règles ensemble rendent
l'overlay structurellement invisible. La seconde règle de §12.2 — « après une durée maximale de
3 secondes » — est la seule applicable dans ce parcours.

Décision retenue : la branche `BlockAction.None` ne retire plus l'overlay ; le retrait est porté
par le minuteur de 3 s. Vérifié sur appareil : overlay affiché, puis retiré entre 3 et 4 s.
**SPEC_ANDROID §12.2 doit être ajustée en conséquence** — non fait dans ce changement, à traiter
avec l'arbitrage du défaut n° 3 ci-dessous, qui touche le même paragraphe.

### 3. Résolu (côté appareil) — l'optimisation de batterie MIUI gelait le service

**Symptôme.** Le blocage fonctionnait de façon fiable juste après une interaction avec Niumi,
puis cessait de fonctionner sans que rien ne le signale : ni crash, ni service désactivé.

**Cause racine, établie par A/B contrôlé.** L'optimisation de batterie de HyperOS gelait le
process de Niumi en arrière-plan, ce qui suspendait la remise des événements d'accessibilité —
alors que le système continuait de déclarer le service `Bound` et `Enabled`, et que le process
restait vivant avec le **même PID**. Aucun signal applicatif ne permettait de le détecter.

| Test (identique dans les deux colonnes) | Restrictions batterie actives | Restrictions levées |
| --- | --- | --- |
| Lancement immédiat après interaction avec Niumi | bloqué < 1 s | bloqué < 1 s |
| Même test 90 s plus tard, Niumi non touché | **non bloqué** | **bloqué < 1 s** |
| Même test 5 min plus tard | non mesuré | **bloqué < 1 s** |
| PID de Niumi | inchangé | inchangé |

**Correctif : côté appareil, pas côté code.** Il a suffi de passer Niumi en « Aucune
restriction » dans l'économiseur de batterie MIUI. Aucune ligne de code n'a été nécessaire.

**Mais l'exemption ne survit pas aux réinstallations.** Après plusieurs `installDebug` successifs,
le blocage a de nouveau cessé de fonctionner, avec le même profil que précédemment : bloqué juste
après une interaction avec Niumi, plus rien ensuite. Vérification faite, la politique d'énergie
de l'application était repassée en mode restrictif d'elle-même. Le réglage a dû être remis à la
main, après quoi le protocole complet est passé au vert. C'est une fragilité produit sérieuse et
pas seulement une gêne de développement : une mise à jour depuis le store remettrait selon toute
vraisemblance l'utilisateur dans cet état, et le blocage cesserait silencieusement. §13 a été
complétée en conséquence — le contrôle doit être réévalué à chaque activation de session, pas
seulement à l'onboarding.

**Conséquence pour le produit — à répercuter dans les specs.** SPEC_ANDROID §13 classe
aujourd'hui « batterie optimisée » en `WARNING`, « sans blocage par défaut ». La mesure montre
que sur HyperOS cette hypothèse est fausse pour le blocage : sans l'exemption, la fonction est
**silencieusement inopérante** au bout d'environ une minute, précisément dans l'usage réel
(l'utilisateur engage une session, pose son téléphone, puis tente d'ouvrir une application
bloquée). Ce contrôle doit devenir `BLOCKING_FOR_NIUMI_EXPERIENCE` au moins sur les surcouches
concernées, avec une action d'onboarding guidant l'utilisateur vers le réglage. À arbitrer et à
écrire dans §13 avant la porte de validation 0.

**Effet secondaire du diagnostic.** Cette découverte invalide plusieurs mesures antérieures de
la session : tous les essais « non bloqué » réalisés hors de la fenêtre d'activité du service
étaient des faux négatifs. Le protocole manuel a été intégralement rejoué après la levée des
restrictions (voir « Résultats de la validation sur appareil »).

**Anti-rebond conservé.** `shouldBlockNow` (1 s) a été introduit pendant l'évaluation et gardé :
dès que les événements arrivent en rafale, il évite un `GLOBAL_ACTION_HOME` et une entrée
`BLOCK_APPLIED` par événement, ce qui saturerait le journal borné à 200 entrées (§17).

**Élargissement des types d'événements : testé, non retenu.** `typeWindowContentChanged` et
`typeViewClicked` ont été ajoutés temporairement, installés et mesurés. Ils n'apportaient rien
(le problème n'était pas le type d'événement mais le gel du process) ; la configuration exacte
de §12.2 a été restaurée et revérifiée au runtime.

**Fiabilité de l'outillage de mesure.** `adb logcat` a cessé de délivrer la moindre ligne en
cours de session (comportement MIUI connu), et `am start -n` sur une tâche existante se contente
de la ramener au premier plan sans toujours la reprendre. Les conclusions tirées de ces deux
outils ont été invalidées et refaites uniquement sur `dumpsys activity` (`topResumedActivity`),
seul signal comportemental fiable observé.

### 3 bis. Diagnostics écartés en cours de route (conservés pour mémoire)

**Piste A — événements résiduels d'une application en arrière-plan.** J'avais conclu que des
événements résiduels renvoyaient l'utilisateur à l'accueil, y compris depuis Niumi. La
journalisation du flux d'événements l'a contredit : un seul événement calculatrice, un seul
blocage, aucun événement résiduel. Le symptôme « Niumi ne revient pas au premier plan » était un
artefact d'`am start -n`, reproduit puis levé avec un lancement de type launcher.

**Piste B — application ramenée depuis une tâche existante.** Mesuré plusieurs fois : une
application dont la tâche existe déjà n'était pas bloquée. Mais les mêmes essais échouent aussi
sur une tâche neuve dès que le service est gelé, et réussissent tous deux juste après une
interaction avec Niumi. Cette piste n'est donc pas indépendante du gel décrit ci-dessus ; elle
reste à réévaluer une fois le gel corrigé.



## Résultats de la validation sur appareil

Redmi 25080RABDG, HyperOS, Android 16 / API 36. `adb devices` vérifié avant chaque lancement.
Application de test : `com.miui.calculator` — délibérément **pas** `com.android.settings`, que
§12.1 exclut de la sélection et dont le blocage empêcherait l'utilisateur d'atteindre les réglages
pour désactiver le service.

### Tests instrumentés — 5/5 verts

`./gradlew :feature:session:connectedDebugAndroidTest :feature:setup:connectedDebugAndroidTest`
→ BUILD SUCCESSFUL (2 tests `:feature:session`, 3 tests `:feature:setup`).

### Essais manuels

| Essai | Résultat |
| --- | --- |
| Activer le service depuis la page de consentement | **OK** — les 5 points de §12.3 affichés, bouton présent, ouverture de `ACTION_ACCESSIBILITY_SETTINGS` |
| Recalcul de l'état au retour des réglages (§13) | **OK** — « Service inactif » → « Service actif » sans recréer l'écran |
| Configuration runtime du service (`dumpsys accessibility`) | **OK** — `eventTypes=[TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED]`, `notificationTimeout=50`, `feedbackType=GENERIC`, et surtout `capabilities=0` : preuve au runtime que `canRetrieveWindowContent=false` s'applique |
| `android:description` dans les réglages système | **OK** — texte affiché, identique à `AccessibilityConsentTexts.SETTINGS_DESCRIPTION` |
| Ouvrir l'application bloquée depuis le launcher | **OK après correction** — retour à l'accueil immédiat + overlay « Calculatrice reste bloquée jusqu'au scan du boîtier. » (texte §12.2 mot pour mot, libellé résolu par `PackageManager`) |
| Overlay non intrusif | **OK** — bandeau en haut d'écran, non plein écran, téléphone utilisable |
| Disparition de l'overlay | **OK** — absent après 4 s (minuteur de 3 s) |
| Ouvrir une application non listée (Horloge) | **OK** — aucun effet, application reste au premier plan |
| Ouvrir l'application bloquée par intent direct (équivalent notification) | **OK** (après levée des restrictions batterie) — retour à l'accueil |
| Ouvrir l'application bloquée depuis les récents | **OK** (après levée des restrictions batterie) — retour à l'accueil |
| Blocage 90 s après le passage de Niumi en arrière-plan | **OK** (après levée des restrictions batterie) — bloqué en < 1 s ; **échouait systématiquement avant** |
| Blocage 5 min après le passage de Niumi en arrière-plan | **OK** — bloqué en < 1 s, même PID : le correctif tient dans la durée |
| Protocole complet via `tools/validate_blocking.sh` | **OK** — 7 contrôles sur 7, code de sortie 0 |
| Désactiver le service pendant un blocage actif | **OK** — le blocage cesse immédiatement et le POC affiche « Service d'accessibilité inactif ». La liste de blocage reste armée, ce qui est le comportement attendu à cette étape : §12.2 demande en outre de *détecter* la désactivation et d'*afficher un incident*, ce qui relève du coordinateur (étape 15) |

### Constat annexe : `am force-stop` désactive le service

`adb shell am force-stop com.niumi.app` retire le service de `enabled_accessibility_services`.
Comportement Android normal, cohérent avec §4.2 (« arrêt forcé depuis les réglages » fait partie
des cas non garantis), mais piège de session de test : il a fallu réactiver le service à la main
trois fois. À ne pas utiliser pendant une validation du blocage.

### État de l'appareil en fin de session

Blocage retiré (« Aucun blocage actif »), `svc power stayon` remis à `false`, service
d'accessibilité laissé **actif** — à désactiver à la main si souhaité.

## Reste à faire avant de clore l'étape

1. Couvrir Pixel et Samsung : reporté à la campagne de bêta-test, aucun appareil de ces marques
   n'étant disponible. Le gel observé est propre à HyperOS ; ces surcouches appliquent d'autres
   politiques d'énergie, à documenter au fur et à mesure. À intégrer au protocole remis aux
   bêta-testeurs plutôt qu'à la porte de validation 0 sur appareils de développement.

Les mises à jour de specs sont faites : §12.2 (overlay porté par le minuteur, exception
interdite dans `onAccessibilityEvent`, contexte du service pour la fenêtre, `android:description`,
anti-rebond), §13 (gravité du contrôle « batterie optimisée », détection partielle, perte de
l'exemption à la réinstallation) et §19.2 (retrait des deux tests instrumentés de blocage au
profit du protocole scripté).

## Conséquences pour les étapes suivantes

Ces constats créent du travail hors du périmètre de l'étape 5, à ne pas perdre de vue :

- **§13 impose désormais de réévaluer l'exemption d'énergie à chaque activation de session**, et
  non seulement à l'onboarding, puisqu'une mise à jour de l'application peut la réinitialiser.
  L'implémentation revient à `DeviceReadinessChecker` et au parcours d'activation (étapes 8
  et suivantes) ; rien n'est à faire dans le code de blocage lui-même.
- **L'aide doit prévenir qu'une mise à jour peut réinitialiser ce réglage** — à traiter avec les
  textes produit, et à couvrir dans le dossier Play de l'étape 6.
- **La projection de blocage reste en mémoire** : un processus tué perd la liste active. La
  lecture depuis Room arrive à l'étape 15, comme prévu.

### Test instrumenté de bout en bout : non automatisable, et pourquoi

SPEC_ANDROID §19.2 demande deux tests instrumentés — « overlay d'accessibilité sur application
factice » et « retour à l'accueil après détection d'un package bloqué ». **Aucun des deux n'est
réalisable par instrumentation**, ce qui a été établi en essayant les deux emplacements
possibles.

**Dans `:feature:session` : mauvais process.** L'APK de test d'un module `library` est une
application distincte (`com.niumi.feature.session.test`), alors que le service activé appartient
à `com.niumi.app` et vit dans son process — deux `InMemoryBlockedPackagesProjection` distincts,
aucune influence possible du test sur le service réel. Le test initial a été supprimé ; son
`assumeTrue` masquait le problème en l'ignorant silencieusement. Il est remplacé dans ce module
par `AndroidAccessibilityServiceStatusInstrumentedTest`, qui vérifie ce qui y est réellement
vérifiable (lecture de `Settings.Secure`).

**Dans `:app` : l'instrumentation désactive le service.** Un `BlockingEndToEndTest` a été écrit,
installé et lancé sur l'appareil, avec le service activé et lié juste avant. Mesure :

| Instant | `accessibility_enabled` | Service lié |
| --- | --- | --- |
| Avant `am instrument` | 1 | oui |
| Après `am instrument` | **0** | **non**, et ne se relie pas |

Android refuse de lier un service d'accessibilité appartenant à un package sous instrumentation
et remet le drapeau global à zéro ; l'état ne se rétablit pas de lui-même, le service doit être
réactivé à la main. Les deux tests se sont donc ignorés (`AssumptionViolatedException`), et
aucun réglage du lanceur de test ne peut contourner une politique système. Le test a été
supprimé et `:app` remis à son runner d'origine, plutôt que de conserver un test qui ne
s'exécutera jamais.

À noter au passage : `connectedDebugAndroidTest` réinstalle l'application avant d'exécuter, et
une réinstallation retire elle aussi le service de la liste active — le problème se pose donc
même avant l'instrumentation.

**Décision prise, et §19.2 mise à jour.** Les deux lignes ont été retirées de la liste des tests
instrumentés, avec l'explication du pourquoi, et remplacées par un renvoi vers
`tools/validate_blocking.sh`. Ce script déroule le protocole sur appareil réel et contrôle
chaque essai par `dumpsys` plutôt que par l'œil de l'opérateur : retour à l'accueil depuis le
lanceur sur tâche neuve puis sur tâche existante, ouverture par intent explicite (équivalent du
chemin « depuis une notification »), présence puis retrait de la fenêtre d'overlay, absence
d'effet sur une application non bloquée, et persistance du blocage après un délai en arrière-plan
— l'essai qui a révélé le gel du process.

Il échoue explicitement, avec un code de sortie non nul et la consigne à suivre, quand ses
préconditions ne sont pas réunies. C'est délibéré : deux fois pendant cette étape, des tests
instrumentés silencieusement ignorés ont donné une illusion de couverture.

Une alternative a été examinée puis écartée pour le MVP : un test UiAutomator en boîte noire,
hébergé dans l'`androidTest` d'un module `library` (dont l'APK, étant une application distincte,
n'instrumente pas Niumi et laisse donc le service lié). Son avantage propre serait de vérifier
automatiquement le **texte exact** de l'overlay imposé par §12.2, que `dumpsys` ne permet pas
d'observer. Son coût : une dépendance UiAutomator et surtout un point d'entrée réservé au debug
capable d'armer le blocage, c'est-à-dire un composant manipulant l'état de blocage embarqué dans
l'application. Écarté à ce stade ; le texte de l'overlay reste un contrôle visuel du protocole.

## Terminé quand (statut)

**Étape close pour l'appareil disponible.** Le blocage fonctionne de bout en bout sur appareil
réel, le protocole complet passe, et les specs sont à jour. Ne reste que la couverture Pixel et
Samsung, reportée à la campagne de bêta-test faute d'appareils.

- code conforme aux spécifications applicables : oui, avec l'écart d'interface transverse
  documenté (décision 1), les points reportés (décisions 2-3) et la contradiction de §12.2
  tranchée en faveur du minuteur de 3 s (défaut n° 2, spec à ajuster) ;
- tests pertinents passent : oui — 76 tests JVM et 5 tests instrumentés verts sur appareil
  réel. Les deux tests instrumentés de blocage demandés par §19.2 ne sont pas réalisables
  (voir « Test instrumenté de bout en bout ») et sont couverts par le protocole manuel ;
- erreurs et avertissements nouveaux traités : oui — ktlint, detekt et lint verts sur tout le
  dépôt, aucun nouvel avertissement de compilation introduit (le seul observé, `hiltViewModel`
  déprécié, préexiste depuis `PocScreen.kt` à l'étape 4) ; les modifications temporaires de
  diagnostic (journalisation, `typeAllMask`, types d'événements élargis) ont été retirées et la
  configuration §12.2 revérifiée au runtime ;
- specs mises à jour si le comportement a changé : plan MVP mis à jour (bloc « Interfaces
  transverses », cases de l'étape 5). **SPEC_ANDROID §12.2, §13 et §19.2 ont été mis à jour dans ce
  changement** (overlay, exception interdite dans `onAccessibilityEvent`, `android:description`,
  gravité et détection partielle du contrôle « batterie optimisée », retrait des deux tests
  instrumentés de blocage au profit du protocole scripté) ;
- validations matérielles : protocole **intégralement déroulé** sur Redmi 25080RABDG
  (Android 16, HyperOS), et rejoué en fin de parcours par `tools/validate_blocking.sh` avec
  7 contrôles sur 7 au vert — blocage depuis le launcher (tâche neuve et tâche existante), par
  intent explicite et depuis les récents, overlay affiché puis retiré, application non listée
  inchangée, tenue du blocage à 90 s et à 5 min en arrière-plan, et désactivation du service en
  cours de blocage. Reste la couverture Pixel et Samsung, reportée à la campagne de bêta-test.
