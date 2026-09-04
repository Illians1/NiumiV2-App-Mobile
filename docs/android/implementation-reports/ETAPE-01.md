# Étape 1 — Bootstrap du monorepo Gradle

Date : 2026-09-04.

## Résumé

Squelette Gradle des sept modules imposés par SPEC_CORE_KMP §15 / SPEC_ANDROID §6, catalogue
de versions, portes qualité (ktlint, detekt, Lint) et une application `:app` qui affiche
un accueil « Aucune session ». Aucune règle métier implémentée à cette étape.

## Prérequis machine installés

Le poste ne disposait ni de JDK, ni des paquets SDK `platforms;android-37.0` /
`build-tools;37.0.0`, ni d'un wrapper Gradle.

- **JDK** : `temurin@17` (cask Homebrew) exige un mot de passe sudo interactif, impossible à
  fournir depuis l'environnement d'exécution. Installé à la place **OpenJDK 17 via la formule
  Homebrew** (`brew install openjdk@17`, keg-only, pas de symlien système) —
  `/opt/homebrew/opt/openjdk@17`. Aucune modification système ; `JAVA_HOME` doit être exporté
  explicitement à chaque invocation de `./gradlew` (voir « Comment builder »).
- **SDK** : `platforms;android-37.0` et `build-tools;37.0.0` installés via `sdkmanager`
  (licences acceptées). `local.properties` créé (non versionné) avec `sdk.dir`.
- **Wrapper Gradle** : généré à partir d'une distribution Gradle 9.5.0 téléchargée et vérifiée
  par SHA-256 (`553c78f5…6d6b746`), exécutée une fois pour produire `gradlew`, `gradlew.bat` et
  `gradle/wrapper/gradle-wrapper.jar`. `distributionSha256Sum` ajouté manuellement au
  `gradle-wrapper.properties` généré.

## Versions reconfirmées le 2026-09-03/04

Vérifiées contre les métadonnées Maven réelles (maven.google.com, Maven Central, Gradle
Plugin Portal, services.gradle.org, releases GitHub officielles), pas seulement contre la
mémoire du modèle. Écarts par rapport à la table du plan MVP, reportés dans ce même plan :

| Composant | Plan MVP | Retenu | Raison |
| --- | --- | --- | --- |
| Gradle | 9.3.1 | **9.5.0** | Maximum testé par KGP 2.4.10 (plage officielle 7.6.3–9.5.0) ; 9.3.1 était seulement le minimum d'AGP 9.1.1. |
| ktlint | non fixé | plugin **14.2.0**, CLI **1.8.0 épinglé** | Le plugin embarque ktlint 1.5.0 par défaut ; ce défaut change entre patchs, d'où l'épinglage. |
| detekt | non fixé | **`dev.detekt` 2.0.0-alpha.6**, bloquant | Voir section dédiée ci-dessous. |
| Reste de la table (AGP 9.1.1, Kotlin 2.4.10, KSP 2.3.11, Compose BOM 2026.08.00, Room 2.8.4, DataStore 1.2.1, Navigation 2.10.0, Hilt 2.60.1/androidx.hilt 1.4.0, kotlinx-datetime 0.8.0, kotlinx-serialization 1.11.0, kotlinx-coroutines 1.11.0) | — | confirmé sans changement | — |

**Écart signalé (CLAUDE.md, regard critique sur les specs) :** aucune combinaison de versions
n'est intégralement dans la matrice officielle JetBrains pour `compileSdk 37`. KGP 2.4.10 est
testé jusqu'à AGP 9.1.0 ; l'API 37 exige AGP ≥ 9.1.1. Le build est donc à un patch au-dessus du
maximum testé. Il compile et tous les tests passent malgré cet écart ; à revalider dès la
sortie d'une version de KGP dont la matrice couvre AGP 9.1.1+.

## detekt : conflit spec/réalité tranché avec l'utilisateur

SPEC_ANDROID §5 impose detekt. La dernière version **stable** (1.23.8) embarque l'analyseur
Kotlin 2.0.21 et casse sur Kotlin 2.2+ (bugs officiels confirmés : NPE sur les context
parameters, `ClassNotFoundException` de parsing, metadata incompatible ≥ 2.3.0) — inutilisable
sur ce projet en Kotlin 2.4.10.

Décision validée avec l'utilisateur : **`dev.detekt` 2.0.0-alpha.6, épinglée, dans la porte
bloquante** (seule variante construite contre Kotlin 2.4.10). `./gradlew detekt` a été exécuté
avec succès sur les sept modules — l'alpha ne crashe pas. Deux ajustements de configuration ont
été nécessaires et documentés dans `config/detekt/detekt.yml` : la règle `FunctionNaming`
ignore les fonctions annotées `@Composable` (convention Compose : PascalCase). Statut alpha à
documenter dans SPEC_ANDROID §5 (fait, voir plus bas) et à revoir dès la sortie d'une version
stable de detekt 2.

## Difficultés rencontrées et résolues

- **Modules intermédiaires `:core` et `:feature`.** Gradle crée un projet implicite pour
  chaque segment d'un chemin `include(":core:database")`, avec un `projectDir` par défaut
  relatif à la racine (`rootDir/core`). Comme `:core:*` et `:feature:*` vivent sous
  `androidApp/`, il a fallu remapper explicitement `project(":core").projectDir` et
  `project(":feature").projectDir` dans `settings.gradle.kts`, en plus des modules feuilles.
- **Accès au catalogue de versions depuis `subprojects { }`.** L'accesseur généré `libs`
  (`LibrariesForLibs`) n'est disponible que dans le script qui l'a produit ; il n'existe pas
  encore sur les sous-projets tant qu'ils n'ont pas leur propre script évalué. Résolu en lisant
  le catalogue via l'API générique `rootProject.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")`
  dans le `build.gradle.kts` racine.
- **API réelle de `dev.detekt` 2.0.0-alpha.6.** Non supposée : lue directement dans le fichier
  `.api` de la release GitHub (`dev/detekt/gradle/Detekt`, `DetektExtension`,
  `DetektCreateBaselineTask`, tous avec des `Property<T>` Gradle) avant d'écrire la
  configuration, pour éviter de deviner des noms de classe sur une alpha.
- **`android:Theme.DeviceDefault.DayNight.NoActionBar`** n'existe pas comme ressource
  publique de la plateforme (vérifié via `aapt2 dump resources` sur `android.jar` 37.0) : seul
  `Theme.DeviceDefault.DayNight` (avec barre d'action) est public. Remplacé par ce parent avec
  `windowActionBar=false` et `windowNoTitle=true` en surcharge — jour/nuit natif, sans barre
  d'action, sans dépendance externe (Material Components n'est pas utilisé, l'app est 100 %
  Compose).
- **`mipmap-anydpi` sans qualificatif de version.** Lint suggère de fusionner
  `mipmap-anydpi-v26` en `mipmap-anydpi` puisque `minSdk` (29) > 26. Fait, mais **le merger de
  ressources d'AGP 9.1.1 supprime silencieusement ce dossier** (AAPT2 seul le compile sans
  erreur ; `mergeDebugResources` ne le reporte pas dans sa sortie, et l'APK se retrouve sans
  icône, `AAPT: error: resource mipmap/ic_launcher not found` au link). Revenu à
  `mipmap-anydpi-v26` (redondant mais fonctionnel) et suppression documentée du finding
  `ObsoleteSdkInt` dans `androidApp/app/build.gradle.kts`. Point à revoir si une version
  ultérieure d'AGP corrige ce comportement.
- **`ktlint_code_style = official`** est une valeur invalide pour ktlint ≥ 1.x (attend
  `ktlint_official`, `android_studio` ou `intellij_idea`) ; corrigé dans `.editorconfig`.
- Fonctions `@Composable` en PascalCase : ajouté
  `ktlint_function_naming_ignore_when_annotated_with = Composable` dans `.editorconfig` en plus
  de la règle detekt équivalente, les deux outils ayant chacun leur propre check de nommage.

## Correction au plan MVP

Le plan proposait de tester « la présence de `@HiltAndroidApp` par réflexion » dans le smoke
test de `:app`. Une annotation présente sur la classe reste détectable par réflexion quelle
que soit sa rétention déclarée dans le source Kotlin, mais ce n'est pas le signal le plus
robuste : il ne dit rien sur le fait que le processeur d'annotations a réellement tourné. Le
signal fiable, déjà utilisé par la communauté Hilt, est la génération de la classe
`Hilt_NiumiApplication` par KSP : `SmokeTest` vérifie
`NiumiApplication::class.java.superclass == Class.forName("com.niumi.app.Hilt_NiumiApplication")`,
qui échoue franchement si l'annotation n'a pas été traitée.

## Décisions non explicitement couvertes par les specs

- `android:allowBackup="false"` sur `:app` : la base locale contiendra à terme le hash du
  token du boîtier associé (étape 13+), et le parcours est entièrement hors ligne
  (SPEC_ANDROID §16). Aucune sauvegarde cloud n'est pertinente. Non écrit dans les specs à ce
  jour — à ajouter à SPEC_ANDROID §14 si confirmé lors d'une étape ultérieure touchant au
  manifeste.
- Icône de lanceur : silhouette géométrique provisoire (cercle plein), sans lien avec une
  charte graphique Niumi — aucune n'existe dans le dépôt. Commentée comme telle dans
  `ic_launcher_foreground.xml` / `ic_launcher_monochrome.xml`. À remplacer dès qu'une charte
  existe (CLAUDE.md « Conformité graphique »).

## Specs mises à jour

- `specs/SPEC_ANDROID.md` §5 : ligne « Qualité » précise l'usage de `dev.detekt`
  2.0.0-alpha.6 et le motif (incompatibilité de detekt 1.23.8 avec Kotlin 2.4).
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : table de versions alignée (Gradle
  9.5.0, ktlint/detekt fixés), case « reconfirmer les versions » et étape 1 cochées.

## Fichiers créés

Racine : `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}`,
`gradlew`, `gradlew.bat`, `.editorconfig`, `config/detekt/detekt.yml`.

`shared/core/` : `build.gradle.kts`,
`src/commonMain/kotlin/com/niumi/core/domain/NiumiCoreVersion.kt`,
`src/commonTest/kotlin/com/niumi/core/SmokeTest.kt`.

`androidApp/app/` : `build.gradle.kts`, `proguard-rules.pro`, `src/main/AndroidManifest.xml`,
`src/main/kotlin/com/niumi/app/{NiumiApplication.kt,MainActivity.kt,ui/theme/NiumiTheme.kt}`,
`src/main/res/{values/{strings,colors,themes}.xml,drawable/ic_launcher_{foreground,monochrome}.xml,mipmap-anydpi-v26/ic_launcher.xml}`,
`src/test/kotlin/com/niumi/app/{SmokeTest.kt,ModuleListTest.kt}`.

`androidApp/{core/database,core/system,feature/setup,feature/session,feature/ringing}/` :
`build.gradle.kts`, `src/main/AndroidManifest.xml`,
`src/main/kotlin/<package>/PackageInfo.kt` (repère vide de tout code métier).

Fichier local non versionné créé : `local.properties` (`sdk.dir`).

## Comment builder sur ce poste

Aucun JDK système ; exporter avant tout `./gradlew` :

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

## Commandes exécutées et résultat

Toutes exécutées avec `--no-daemon` (daemon Gradle non conservé entre appels dans cet
environnement) :

| Commande | Résultat |
| --- | --- |
| `./gradlew projects` | BUILD SUCCESSFUL — sept modules listés, chemins physiques corrects |
| `./gradlew :shared:core:jvmTest` | BUILD SUCCESSFUL — `SmokeTest.schemaVersionIsOne` passe |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL — `SmokeTest` et `ModuleListTest` : 2/2, 0 échec (vérifié dans les rapports XML, pas seulement au résumé Gradle) |
| `./gradlew ktlintCheck` | BUILD SUCCESSFUL sur les sept modules, après correction de `.editorconfig` et reformatage (`ktlintFormat`) de trois fichiers |
| `./gradlew detekt` | BUILD SUCCESSFUL sur les sept modules (alpha stable à l'usage) |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL après correction du thème, suppression ciblée de 4 faux positifs documentés (`UnnecessaryRequiredFeature`, `DataExtractionRules`, `UnusedAttribute`, `OldTargetApi`, `AndroidGradlePluginVersion`, `ObsoleteSdkInt`) et un vrai bug d'outillage contourné (voir « Difficultés ») |
| `./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64` | BUILD SUCCESSFUL (Xcode 26.6 détecté ; téléchargement ponctuel de la distribution Kotlin/Native LLVM/libffi, ~1 min) |

Séquence complète rejouée une seconde fois après tous les correctifs (`projects`,
`:shared:core:jvmTest`, `:app:assembleDebug`, `:app:testDebugUnitTest`, `ktlintCheck`,
`detekt`, `:app:lintDebug`) : BUILD SUCCESSFUL sans régression.

## Tests non exécutés / non applicables à cette étape

Aucun test instrumenté (`connectedDebugAndroidTest`) : aucun composant Android nécessitant un
appareil n'existe encore (première fonctionnalité système à l'étape 3). Aucune validation
manuelle sur appareil requise à ce stade.

## Incertitudes restantes

- **detekt alpha** : `dev.detekt` 2.0.0-alpha.6 est la seule option compatible Kotlin 2.4.10 ;
  son API peut changer avant la sortie stable. À réévaluer à chaque étape si une mise à jour
  est publiée.
- **AGP 9.1.1 hors matrice testée de KGP 2.4.10** (par un patch). Fonctionne à ce jour ; aucune
  garantie officielle au-delà.
- **Comportement du merger de ressources AGP sur `mipmap-anydpi` sans qualificatif de
  version** : contourné, non corrigé à la source. À surveiller sur les futures versions d'AGP.
- **Icône de lanceur** : placeholder sans valeur de marque, à remplacer.
- **`allowBackup="false"`** : décision technique appliquée mais pas encore actée dans
  SPEC_ANDROID.
