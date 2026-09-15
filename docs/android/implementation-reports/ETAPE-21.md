# Étape 21 — finalisation release, suppression du POC, documentation QA, porte finale

**Date :** 2026-09-15. **Plan :** `docs/superpowers/plans/2026-09-03-mvp-android.md`, étape 21.

**Produit :** l'application ne contient plus aucune route de debug, un test permanent interdit à
une classe de POC, à une doublure de test ou à une permission interdite d'atteindre l'APK de
publication, le build release passe sous R8 avec sa configuration de signature, un écran « Aide et
limites » énonce dans le produit ce que Niumi ne peut pas garantir, et une chaîne d'intégration
continue rejoue l'ensemble. La matrice QA et le rapport de release consignent ce qui est prouvé —
et surtout ce qui ne l'est pas.

**État avant cette étape.** La route POC de debug vivait encore dans `src/debug` et était le seul
consommateur du point d'extension de navigation. `:app:assembleRelease` et `:app:lintRelease`
n'avaient **jamais été exécutés** dans ce dépôt. Aucune configuration de signature, aucun dossier
`.github/`. `PRIVACY_POLICY.md` et `REVIEW_VIDEO_SCRIPT.md` renvoyaient à un écran « Aide et
limites » qui n'existait pas.

---

## Ce qui a été livré

### 1. Suppression du POC et de ce qu'il portait seul

Onze fichiers supprimés (`androidApp/app/src/debug/` en entier, manifeste compris). La suppression
a rendu sans implémenteur tout le point d'extension de navigation, retiré lui aussi plutôt que
laissé en place vide : `NavGraphContributor`, `NavEntryPoint`, `NavigationModule` (`@Multibinds`),
le paramètre `contributors` de `NiumiNavHost`, `entryPoints` et `onEntryPointClick` de
`HomeScreen`, et l'injection `Set<NavGraphContributor>` de `MainActivity`. `debugImplementation(libs.datastore.preferences)`
disparaît avec son unique consommateur ; l'alias reste au catalogue, `:core:database` et
`:core:system` l'utilisant en production.

Trois commentaires décrivaient encore le POC au présent (`ReadinessSources`, `feature/setup/build.gradle.kts`,
`PairingScreen`) : corrigés. Les mentions purement historiques — `NfcModule`, `ScanToModifyViewModel`,
qui racontent *pourquoi* une indirection a existé puis disparu — sont conservées : elles expliquent
la forme actuelle du code.

### 2. `ReleaseHygieneTest` — cinq garde-fous permanents

`androidApp/app/src/testRelease/kotlin/com/niumi/app/ReleaseHygieneTest.kt`, cinq tests :

1. le manifeste **fusionné release** déclare **exactement** les neuf permissions de §14 ;
2. aucune des trois permissions interdites (§14, §16), nommément, pour que l'échec dise laquelle ;
3. aucune classe `*Poc*`, `*Fake*` ou `Debug…Store` sur le classpath release ;
4. aucun `TODO`, `FIXME` ni `STOP_RINGING_ACTION` dans les sources de production ;
5. les seuls appels de programmation d'alarme sont `setAlarmClock` dans `AndroidAlarmScheduler` et
   `setExactAndAllowWhileIdle` dans `AndroidRingingWatchdog` (§9.1).

**Vus échouer avant d'être vus passer.** Quatre violations réelles injectées temporairement — une
permission `QUERY_ALL_PACKAGES` au manifeste, une classe `FakeProbe` en `src/main` portant un `TODO`
et un appel `alarmManager.set(…)` — font échouer les cinq tests, chacun avec un message qui nomme le
fautif. Sans cette vérification, rien ne prouverait qu'ils testent ce qu'ils prétendent : dans un
dépôt déjà propre, un test d'hygiène passe aussi bien quand il ne regarde rien.

**Une cinquième vérification, dans l'autre sens.** Une permission interdite citée dans un
**commentaire** XML laisse les tests verts. C'est ce qui justifie la lecture du manifeste en DOM
plutôt qu'en texte : le fusionneur recopie les commentaires de chaque module, et ceux de
`:core:system` parlent précisément des permissions interdites. Un `contains` aurait pris ces
explications pour des déclarations. Même raison pour le contrôle des API d'alarme, qui retire les
commentaires avant de chercher les appels — la KDoc d'`AndroidAlarmScheduler` cite
`setInexactRepeating()` pour l'interdire.

### 3. Deux décisions de build imposées par AGP 9.1.1

**Le test ne peut pas vivre dans `src/test`.** Le classpath de `testDebugUnitTest` contient par
construction les classes de `src/debug` : y chercher l'absence d'une classe de debug ne prouverait
rien. D'où `src/testRelease`, compilé pour la seule variante qui part sur Play.

**AGP 9 ne crée plus la tâche de test unitaire release.** `testReleaseUnitTest` n'existait pas :
AGP 9.1.1 ne génère les tâches de test unitaire que pour `testBuildType` (« debug »).
`variantBuilder.enableUnitTest` de `HasUnitTestBuilder` **ne se résout pas** depuis le DSL Kotlin ;
la forme qui fonctionne est `variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true`
dans `beforeVariants`. Trouvée en décompilant `gradle-api-9.1.1.jar`, faute de documentation AGP 9
publiée sur ce point.

**Le manifeste fusionné est passé par l'API Variant**, pas par un chemin codé en dur :
`variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)` transmis au test par une classe
`SystemPropertyFileArgument : CommandLineArgumentProvider` portant `@InputFile`. La propriété
annotée fait deux choses qu'une lambda ne ferait pas : elle rend `processReleaseManifest`
dépendance de la tâche de test, et elle refait tourner le test quand le manifeste change. Même
mécanisme que `niumi.rootDir`, déjà utilisé par `ModuleListTest`.

### 4. R8 et signature

**`proguard-rules.pro` reste vide, et c'est un constat mesuré.** Le premier `assembleRelease` du
dépôt passe sans aucune règle ajoutée, et R8 ne produit pas de `missing_rules.txt`. Les règles
consommateur de Hilt 2.60.1, Room 2.8.5, kotlinx-serialization 1.11.0 (y compris son fichier
`r8.pro` pour le mode complet), kotlinx-datetime, DataStore, Compose et Navigation sont appliquées
automatiquement — vérifié dans `build/outputs/mapping/release/configuration.txt`. Le fichier porte
donc cette preuve en commentaire plutôt qu'un `-keep` préventif, qui désactiverait silencieusement
l'optimisation qu'il prétend protéger.

Sorties : APK de 3 450 883 octets, AAB de 4 068 051 octets, tous deux **non signés** — le keystore
d'upload appartient à l'utilisateur. `signingConfigs` lit `keystore.properties` à la racine via
`providers.fileContents`, ce qui en fait une entrée de configuration : le configuration cache est
invalidé si le fichier apparaît, change ou disparaît. **Son absence ne fait pas échouer le build** :
un build de vérification n'a pas besoin de la clé d'upload, et l'exiger rendrait la CI impossible.
`keystore.properties` rejoint `.gitignore`, qui n'ignorait que `*.jks` et `*.keystore`.

**`:app:lintRelease` passe du premier coup**, avec `abortOnError` et `warningsAsErrors`, sans
baseline et sans nouvelle exclusion.

### 5. Écran 13 « Aide et limites »

`docs/android/LIMITES.md` et `com.niumi.app.help` : quatre sections, seize limites. Le document
**est** le texte de l'écran, et `HelpTextsTest` compare les deux à chaque build — sections et puces,
dans l'ordre. Vu échouer : modifier une puce du document sans toucher `HelpTexts` casse le build.

Ce verrou n'est pas une coquetterie. `PRIVACY_POLICY.md` renvoie le lecteur de la fiche Play à « l'écran
Aide et limites » : si les deux divergent, c'est un document Play qui ment. Les six limites de
`OnboardingTexts.limits` y sont reprises **mot pour mot**, ce qu'un test vérifie aussi — deux
formulations du même fait laisseraient croire à deux règles distinctes.

S'y ajoutent les limites qui ne pouvaient pas être connues avant d'avoir été mesurées : le silence
total (étape 6), la restriction OEM de démarrage automatique **avec sa portée réelle** — elle
n'empêche pas la reprogrammation du réveil, seulement la réaction à une mise à jour — la
notification de scan écartable depuis Android 14, le diagnostic NFC brièvement faux après un
redémarrage, et la perte possible du journal technique d'avant déverrouillage.

L'écran ne porte **aucune action**, et `HelpScreenTest` le vérifie en comptant les nœuds cliquables.
C'est la règle de §3 et §10.2 : pendant une session, le scan du boîtier est la seule sortie, et un
bouton sur cet écran serait exactement le recours logiciel que §4.5 exclut.

Il vit dans `:app` : il n'appartient à aucun parcours, et un neuvième module Gradle serait contraire
à §6 (`ModuleListTest` l'interdit d'ailleurs).

### 6. Intégration continue

`.github/workflows/mobile.yml`, runner `macos-26`, dans l'ordre de SPEC_CORE_KMP §19 : tests JVM du
moteur commun, framework iOS, tests unitaires Android, hygiène release, analyse statique, build de
publication. Cache `~/.konan` pour la distribution Kotlin/Native. Actions GitHub uniquement.
**Chaque commande a été exécutée localement**, dans l'ordre du workflow, toutes vertes. La CI
distante ne peut pas être observée sans push.

« Tests et build iOS » de §19 n'a pas d'étape : le plan MVP ne développe pas l'application iOS. Le
`link` du framework reste la garantie d'interopérabilité. C'est noté en commentaire du workflow
plutôt que passé sous silence.

### 7. Outils rebranchés

`tools/validate_alarm.sh` pilotait l'écran POC par `input tap`. Il demande désormais à l'opérateur
d'armer une session par le parcours réel, puis lit l'instant cible dans `dumpsys alarm` (`when=`) et
mesure le retard par rapport à **cet instant** plutôt qu'à un délai saisi — une mesure qui vient du
système, pas de l'opérateur. Il ne retient que les alarmes visant `AlarmReceiver`, pour ne jamais
confondre le réveil avec le watchdog de `RINGING`. `tools/validate_blocking.sh` : deux consignes
manuelles réécrites, et une précondition ajoutée.

Le plan disait « `tools/` conservé » ; conservés tels quels, ces deux scripts auraient été
inopérants.

---

## Fichiers

**Supprimés (13)** : `androidApp/app/src/debug/` en entier (11 fichiers),
`navigation/NavGraphContributor.kt`, `navigation/NavigationModule.kt`.

**Créés (10)** : `androidApp/app/src/testRelease/kotlin/com/niumi/app/ReleaseHygieneTest.kt`,
`androidApp/app/src/main/kotlin/com/niumi/app/help/{HelpTexts.kt, HelpScreen.kt}`,
`androidApp/app/src/test/kotlin/com/niumi/app/help/HelpTextsTest.kt`,
`androidApp/app/src/androidTest/kotlin/com/niumi/app/help/HelpScreenTest.kt`,
`.github/workflows/mobile.yml`, `docs/android/{LIMITES.md, QA_MATRIX.md, RELEASE_REPORT.md}`,
ce rapport.

**Modifiés (17)** : `androidApp/app/build.gradle.kts` (signature, `hostTests`, manifeste fusionné),
`androidApp/app/proguard-rules.pro`, `.gitignore`, `MainActivity.kt`, `HomeScreen.kt`,
`NiumiNavHost.kt`, `NiumiRoute.kt`, `ReadinessSources.kt`, `feature/setup/build.gradle.kts`,
`PairingScreen.kt`, `tools/validate_alarm.sh`, `tools/validate_blocking.sh`,
`specs/SPEC_ANDROID.md`, `docs/android/implementation-reports/LOT-0.md`,
`docs/android/play-console/{PRIVACY_POLICY.md, REVIEW_VIDEO_SCRIPT.md}`, le plan MVP.

## Tests

| | Total | Échecs |
| --- | --- | --- |
| JVM, exécutions (`./gradlew test` + `:app:testReleaseUnitTest`) | **916** | 0 |
| JVM, tests distincts | **878** | 0 |
| Instrumentés | **non rejoués** (141 au 2026-09-15, avant `HelpScreenTest`) | — |

Les 38 tests de `:app` sont rejoués sur les deux variantes, d'où l'écart entre exécutions et tests
distincts. Ajoutés par l'étape : `ReleaseHygieneTest` (5), `HelpTextsTest` (5), `HelpScreenTest` (2,
instrumenté, jamais exécuté).

## Commandes exécutées

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest                                   ✅
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64       ✅ (Xcode 26.6)
./gradlew test                                                   ✅ 916 exécutions
./gradlew :app:testReleaseUnitTest                               ✅
./gradlew ktlintCheck detekt :app:lintRelease                    ✅ lintRelease, première exécution
./gradlew :app:assembleRelease :app:bundleRelease                ✅ APK 3,4 Mo, AAB 4,0 Mo, non signés
./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin  ✅
```

`connectedDebugAndroidTest` n'a **pas** été exécuté : aucun appareil branché pendant cette session.
`HelpScreenTest` compile mais n'a jamais tourné.

Deux détours de mise en forme, corrigés par `ktlintFormat` et non à la main : le nouveau source set
`testRelease` est analysé par ktlint (`ktlintTestReleaseSourceSetCheck`) et par detekt, sans les
assouplissements que ces outils appliquent d'ordinaire aux tests.

## Défaut trouvé sur appareil, invisible en JVM

**`HelpScreenTest` ne pouvait pas démarrer dans `:app`.** Les deux tests échouaient avant d'avoir
rien vérifié, sur « Unable to resolve activity for
`com.niumi.app.test/androidx.activity.ComponentActivity` » : `createComposeRule()` lance une
activité vide, qui doit être déclarée quelque part. `:app` n'avait jamais eu de test d'écran
Compose, donc aucune déclaration.

La copier depuis `:feature:setup`, où elle vit dans le manifeste `androidTest`, **n'a pas
fonctionné** — et l'erreur suivante a donné la vraie raison : « Intent in process `com.niumi.app`
resolved to different process `com.niumi.app.test` ». C'est une différence de nature entre les deux
types de module, et non un détail de configuration :

- module `library` : l'APK de test s'instrumente lui-même, l'activité hôte de son propre manifeste
  vit dans le processus du test ;
- module `application` : l'instrumentation s'exécute dans le processus de l'application testée. Une
  activité déclarée dans l'APK de test appartient à un autre processus, et `startActivitySync` la
  refuse.

La déclaration doit donc vivre dans la variante **debug de l'application**. C'est exactement ce que
fait l'artefact `androidx.compose.ui:ui-test-manifest` de Google, qui s'ajoute en
`debugImplementation` et non en `androidTestImplementation` — sa raison d'être, jusque-là opaque,
devient lisible. Trois lignes de manifeste évitent ici une dépendance externe de plus.

Un essai intermédiaire a aussi montré que `@style/Theme.Niumi` n'est pas résoluble depuis l'APK de
test d'un module `application`, qui ne contient aucune ressource de l'application testée. Sans
objet une fois la déclaration replacée dans la variante debug, où le thème existe.

**Conséquence, à signaler comme un écart au plan :** `androidApp/app/src/debug/` réapparaît, avec un
seul fichier et **aucun code** — un manifeste d'outillage de test, absent de la variante release
comme l'était le POC. Le plan demandait de supprimer le POC, ce qui est fait ; il ne demandait pas
de rendre `src/debug` impossible, et l'y interdire coûterait une dépendance externe pour le même
résultat. `ReleaseHygieneTest` continue de garantir que rien de cette variante n'atteint la
publication.

## Validation sur appareil réel — première passe

**Appareil :** Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0, build
`BP2A.250605.031.A3` — le même qu'aux étapes 17 à 20. Horloges de l'appareil et du poste
comparées par leur epoch avant de commencer : identiques à la seconde.

### Tests instrumentés — 142 verts, 0 échec, 1 ignoré

| Module | Tests |
| --- | --- |
| `:core:database` | 70 |
| `:feature:setup` | 34 |
| `:core:system` | 23 |
| `:app` | **7** (1 + 4 préexistants, 2 de `HelpScreenTest`) |
| `:feature:ringing` | 6 |
| `:feature:session` | 2 (1 ignoré, préexistant) |

L'étape 20 annonçait 141 avec `:feature:session` à 3. Le module n'a que deux tests instrumentés
dans son code, et un seul fichier en porte : **le décompte de l'étape 20 était faux d'une unité**,
aucun test n'a été perdu. 141 − 3 + 2 + 2 = 142.

### Écran 13 vérifié à l'écran, pas seulement en test

Parcours réel sur l'appareil : le bouton « Aide et limites » est présent sur l'accueil, à côté de
« Voir le diagnostic ». L'écran ouvert et parcouru jusqu'en bas restitue **les quatre sections et
les seize limites** de `LIMITES.md`, comparées une à une au document. **Zéro nœud cliquable** dans
l'arbre d'accessibilité, ce qui confirme sur l'appareil ce que `HelpScreenTest` vérifie en
instrumenté. Le geste Retour ramène à l'accueil.

### Les trois scénarios « activation refusée » de §20, soldés

Jamais rejoués à la main depuis que `DeviceReadinessChecker` existe (étape 12b). Chacun mesuré en
basculant le réglage dans les deux sens, l'écran étant relu au retour au premier plan :

| Réglage | Refusé | Accordé |
| --- | --- | --- |
| `POST_NOTIFICATIONS` | `✗ Active les notifications pour que l'écran du réveil puisse s'afficher.` | `✓ Notifications autorisées` |
| `USE_FULL_SCREEN_INTENT` | `✗ Autorise les alarmes plein écran, sinon l'écran de réveil ne s'ouvrira pas tout seul au moment de sonner.` | `✓ Alarmes plein écran autorisées` |
| Service d'accessibilité | `✗ Le service d'accessibilité de Niumi est inactif. Sans lui, les applications choisies ne seront pas bloquées.` | `✓ Service d'accessibilité actif` |

**L'écran de diagnostic n'offre aucun chemin vers le choix de l'heure** tant qu'un contrôle est
rouge : relevé dans l'arbre d'accessibilité, aucune action « heure », « activer » ou « continuer ».
C'est la forme concrète du refus d'activation de §9.2. Le diagnostic réagit bien au retour au
premier plan (`ForegroundReadinessTrigger`, étape 12).

**Un piège de banc, qui a failli passer pour un défaut du produit.** `appops set <paquet>
USE_FULL_SCREEN_INTENT deny` laisse le contrôle au vert. Ce n'est pas le contrôle qui ment :
`appops get` montre deux modes, et seul celui du **UID** gouverne `canUseFullScreenIntent()`. Le
refus doit être posé par `appops set --uid`. À consigner pour les campagnes suivantes, au même
titre que « `am force-stop` coupe l'accessibilité ».

Ce dernier constat a d'ailleurs servi : la navigation vers le diagnostic repart normalement d'un
`force-stop` pour partir d'un état connu, ce qui aurait désactivé le service d'accessibilité au
moment précis où l'essai C voulait le voir actif. Le chemin sans `force-stop` a été utilisé là.

Observé au passage, conforme à §15 : sur l'onboarding, le bouton « Continuer » est
`clickable="false"` tant que la case n'est pas cochée, plutôt que cliquable et sans effet.

### Ce que cette passe n'a pas pu établir

**Tout ce qui demande l'artefact de publication**, faute de keystore d'upload : la session complète
en release, et le redémarrage sans déverrouillage sur un APK minifié. C'est le seul essai capable
de révéler un effet de R8 en fonctionnement.

État dans lequel l'appareil est laissé : notifications jamais accordées, plein écran autorisé,
aucune alarme en attente, **service d'accessibilité laissé actif** (activé pour l'essai C, et
nécessaire à la suite du protocole). `svc power stayon usb` a été activé pour empêcher l'écran de
se verrouiller pendant les essais ; à remettre à `false` en fin de campagne.

## Validations restantes sur appareil réel

Rien de ce qui suit n'est acquis, et rien n'a été coché par anticipation.

1. **`connectedDebugAndroidTest`** — ≈ 143 tests, dont les 2 nouveaux de `HelpScreenTest`. À lancer
   **avant** tout protocole manuel : la tâche désinstalle l'application et toutes ses données.
2. **Session complète sur APK release signé.** Exige le keystore d'upload. C'est la seule
   vérification capable de révéler un effet de R8 en fonctionnement, en particulier sur la
   sérialisation du snapshot Direct Boot. Attendu : parcours identique au debug, et une alarme
   restaurée après redémarrage sans déverrouillage.
3. **Écran « Aide et limites »** ouvert depuis l'accueil, avec et sans session active : les seize
   limites lisibles de bout en bout, aucun bouton.
4. **Les trois scénarios « activation refusée » de §20** (notifications, plein écran, accessibilité),
   jamais rejoués à la main depuis que `DeviceReadinessChecker` existe.
5. **Remettre la permission OEM « Démarrage automatique » à « refusé »** avant toute mesure qui en
   dépend : elle avait été laissée accordée à l'issue de l'étape 19.

## Ce qui reste ouvert

- **La matrice §20 hors Xiaomi.** Un appareil, une surcouche, une version d'Android. Inchangé
  depuis l'étape 19, et c'est la dette la plus lourde du MVP : l'étape 5 a montré qu'une surcouche
  peut rendre le blocage silencieusement inopérant.
- **Android 17.** Le critère §21 sur l'audio d'arrière-plan n'a aucun appareil pour être vérifié.
- **La porte 0b.** Vidéo non tournée, application non soumise, Google n'a pas statué. Ses
  préconditions techniques sont levées ; le reste est hors de portée d'un agent.
- **Aucun essai sur artefact de publication.** Le build passe, les règles consommateur sont
  appliquées ; rien ne le prouve en fonctionnement.
- Le seau d'App Standby jamais rétrogradable et l'arrêt du seul FGS non reproductible sur HyperOS,
  hérités de l'étape 20.

## Suivi ouvert

`docs/android/RESTE_A_FAIRE.md`, créé à la fin de cette étape à la demande de l'utilisateur,
rassemble tout ce qui sépare le MVP d'une publication : la clé de signature et la campagne sur un
artefact de publication, les préconditions Play, la porte 0b, et les campagnes constructeurs jamais
menées. Chaque tâche y porte son mode opératoire et son critère de réussite.

## Specs modifiées dans ce changement

- **SPEC_ANDROID §15** — écran 13 « Aide et limites » ajouté à la liste des écrans, avec ce qu'il
  porte, pourquoi il vit dans `:app`, pourquoi il n'a aucune action, et pourquoi il n'existait pas
  avant alors que §4.2 et §21 l'exigeaient depuis l'origine. Décompte des destinations de
  navigation corrigé : quatorze pour treize écrans.

## Écarts au plan de l'étape

1. **`:app:testReleaseUnitTest` s'ajoute à la liste de commandes du plan.** Le test d'hygiène ne
   peut pas tourner sur le classpath debug sans devenir vide de sens.
2. **Le grep vise `STOP_RINGING_ACTION` exactement.** Le plan écrivait « `STOP_RINGING_ACTION` » ;
   `STOP_RINGING` seul est l'effet métier de SPEC_CORE_KMP §6, présent en production dans huit
   fichiers. Un grep sur la chaîne courte aurait échoué immédiatement sur du code correct.
3. **Les deux scripts de `tools/` sont rebranchés**, alors que le plan les disait simplement
   conservés.
4. **La porte finale n'est pas franchie**, ce que le plan prévoyait comme issue possible : « Un
   critère non prouvé reste ouvert dans le rapport, jamais coché par défaut. »
