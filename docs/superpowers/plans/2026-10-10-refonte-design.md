# Plan de refonte design Niumi — Lots 8 à 16, étapes 29 à 51

> **Pour l'agent chargé de l'exécution :** utiliser `superpowers:executing-plans` (une étape par session) ou `superpowers:subagent-driven-development`. Les cases `- [ ]` servent au suivi. S'arrêter aux portes de validation manuelle. Ne jamais commit ni push sans demande explicite, jamais de ligne `Co-Authored-By`. **Avant chaque étape, lire la ligne « Points ouverts touchés » et poser à Mehdi les questions non encore tranchées : ne jamais inventer une réponse** (`design-plan/CLAUDE-INSTRUCTIONS.md`).

**Objectif :** livrer l'app Android telle que la décrivent les 58 maquettes et `design-plan/SPECIFICATION.md` : réveil récurrent avec blocage automatique au coucher, accueil à cadran et trois états, plusieurs Niumi Points, sortie de secours, écran de réussite, réglages ; thème clair hors session et sombre pendant la session.

**Architecture :** `:shared:core` reste l'unique autorité des transitions (contrat KMP 1.4 : planning récurrent, désarmement sans scan avant le coucher, libération par sortie de secours, plusieurs credentials, temps gagné). Le planning (jours, heures, interrupteur) est calculé en KMP et stocké en natif ; un `WakePlanScheduler` Android arme, désarme et ré-arme les sessions sous le verrou du coordinateur existant. `:core:designsystem` porte palette, police, formes, dimensions et composants ; aucun écran ne contient de couleur, taille ou forme en dur. Le thème est choisi d'après l'état de la session persistée, jamais d'après le réglage du téléphone.

**Stack :** inchangée (Kotlin 2.4.20, Compose BOM 2026.09.00, Material 3 comme socle de composants, Hilt, Room 2.8.5, DataStore, Navigation Compose 2.10.2, KMP, JUnit 4, Truth, ktlint, detekt). Ajout : police IBM Plex Sans (fichiers OFL dans `res/font`). Aucune dépendance nouvelle.

**Specs :** `design-plan/LISEZ-MOI.md`, `SPECIFICATION.md`, `VALEURS-DE-DESIGN.md`, `valeurs-de-design.json`, `POINTS-OUVERTS.md`, `TEXTES-DES-MAQUETTES.md`, `maquettes/` (prioritaires pour écrans, textes, parcours, apparence) ; `specs/SPEC_CORE_KMP.md` et `specs/SPEC_ANDROID.md` (technique) ; `CLAUDE.md` ; `docs/OBJECTIF_ET_INTERET_PRODUIT.md`. Le plan renvoie aux sections au lieu de les recopier : l'exécutant lit les sections citées avant chaque étape, puis l'image et le fichier source de chaque maquette.

## Comment utiliser ce plan

- Une étape = une session. Avant de coder : `CLAUDE.md`, les sections citées, « Contraintes globales », « Interfaces transverses », le rapport de l'étape précédente, et les points ouverts touchés.
- Chaque étape suit le cycle : test qui échoue, échec vérifié, implémentation minimale, succès vérifié, batterie de vérification.
- **Batterie standard** (`export JAVA_HOME=/opt/homebrew/opt/openjdk@17` d'abord) :

```bash
./gradlew :shared:core:jvmTest testDebugUnitTest :app:testReleaseUnitTest
./gradlew ktlintFormat && ./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :app:assembleDebug
./gradlew connectedDebugAndroidTest   # appareil requis, avant tout protocole manuel (il désinstalle l'app et ses données)
```

- Chaque étape qui touche un écran met à jour `specs/SPEC_ANDROID.md` dans le même changement ; chaque étape qui touche le contrat met à jour `specs/SPEC_CORE_KMP.md` et les sections KMP de `specs/SPEC_IOS.md` (§8, §9, §11 « Calcul de la date », §12, §14, §15) ; rien d'autre dans SPEC_IOS.
- À la fin de chaque étape : `docs/android/implementation-reports/ETAPE-NN.md` (même structure que `ETAPE-27.md`).
- Les textes de l'app se recopient depuis `SPECIFICATION.md` ou `TEXTES-DES-MAQUETTES.md`, sans reformulation, dans un `object XxxTexts` verrouillé par un `XxxTextsTest` (égalité stricte, garde « pas de vouvoiement »).
- Si `SPECIFICATION.md` et une image se contredisent, s'arrêter et demander, en citant le fichier et le passage.
- Chemins abrégés utilisés ci-dessous : `DS/` = `androidApp/core/designsystem/src/main/kotlin/com/niumi/designsystem/` ; `SYS/` = `androidApp/core/system/src/main/kotlin/com/niumi/system/` ; `DB/` = `androidApp/core/database/src/main/kotlin/com/niumi/database/` ; `SETUP/` = `androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/` ; `SESSION/` = `androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/` ; `RING/` = `androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/` ; `APP/` = `androidApp/app/src/main/kotlin/com/niumi/app/` ; `KMP/` = `shared/core/src/commonMain/kotlin/com/niumi/core/`. Les tests vivent au même chemin sous `src/test/kotlin` (JVM), `src/androidTest/kotlin` (instrumentés) ou `shared/core/src/commonTest/kotlin`.

## Point de départ et préalable

- **Clore l'étape 28 avant l'étape 29.** `git status` montre des modifications non commitées (durées de montée 2/5/10/15, `tools/validate_alarm.sh`, `SPEC_ANDROID.md`, le plan MVP) et `ETAPE-28.md` suspendu sur un défaut de montée non expliqué (session D). Soit finir la campagne et commiter, soit commiter l'état « suspendu » avec un rapport honnête. L'étape 36 remettra les durées en cause (voir décision Q7) : ne pas laisser deux changements de durées se chevaucher.
- Le dépôt compile et tous les tests passent au départ (`ETAPE-28.md`, partie sans appareil verte).
- `design-plan/` et `ETAPE-28.md` ne sont pas suivis par git : les ajouter au premier commit demandé.

## Contraintes globales

Valeurs copiées des sources ; chaque étape les respecte implicitement.

- `design-plan/` fait foi pour écrans, textes, parcours, apparence ; les specs restent la référence technique (`design-plan/LISEZ-MOI.md` « Ce qui fait foi », décision du 9 octobre 2026).
- **Thème par état** : `NiumiTheme(dark = snapshot.isSessionDark())`, jamais `isSystemInDarkTheme()` ni `DayNight`. Clair : aucune session, `ARMED` avec blocage en attente, états finaux, `RELEASING` vers `DISARMED`. Sombre : `ARMED` avec `blockingAppliedAtEpochMillis` renseigné, `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC`, `RELEASING` vers `COMPLETED` ou `CANCELLED` (`VALEURS-DE-DESIGN.md` §1).
- **Aucune couleur, taille de texte, forme ou dimension en dur** hors `:core:designsystem` ; `grep -rn "Color(0x" androidApp --include=*.kt` ne doit sortir que `NiumiPalettes.kt` (et `colors.xml` de l'icône de lanceur).
- Palette, dégradé unique (soleil écran 11), police IBM Plex Sans 400/500/600 embarquée, échelle de tailles, formes et cadran : `VALEURS-DE-DESIGN.md` §2 à §5, valeurs exactes dans `valeurs-de-design.json`.
- Maquettes dessinées pour 390 × 844 : les écrans doivent s'adapter aux autres tailles (`LISEZ-MOI.md`) ; ne jamais recopier le code des sources HTML.
- Textes : tutoiement, mot pour mot, apostrophe selon D13 ; heures « 22:30 », « 7:00 » (sans zéro devant), durées « 9 h 26 », « 56 min », « 8 h 30 de sommeil » (`SPECIFICATION.md` §2).
- Valeurs par défaut : lundi à vendredi, réveil 7:00, coucher 22:30 ; interrupteur éteint à la fin de la mise en route (`SPECIFICATION.md` §2, §9).
- Même fonctionnement iPhone/Android sauf différence écrite ; les différences Android voulues : feuille de scan dessinée par Niumi, écran 7 dessiné par Niumi, feuille d'applications dessinée par Niumi, pas de limite de 60 s au scan.
- Modules Gradle inchangés (8, `ModuleListTest`) : l'accueil et les réglages vivent dans `:feature:session`, la mise en route dans `:feature:setup`, le design system dans `:core:designsystem`.
- Règles techniques conservées : garde de déverrouillage sur tout dépôt (`Provider<Context>` jamais résolu avant `isUserUnlocked`, SPEC_ANDROID §7.3) ; aucune action d'arrêt dans le parcours de sonnerie (la sortie de secours n'en est pas une : elle est une libération prouvée, SPEC_CORE_KMP 1.4 §12) ; `setAlarmClock()` pour le réveil avec les dérogations §9.1 ; permissions du manifeste §14 sans ajout ; journal technique §17.
- Un écran obsolète est supprimé, avec son `Texts`, son `TextsTest` et sa route, **à l'étape qui le remplace** ; l'étape 49 balaie ce qui n'a pas de remplaçant.
- Rien de `design-plan/POINTS-OUVERTS.md` n'est décidé : chaque étape liste les points qu'elle touche ; sans réponse, l'étape s'arrête ou laisse l'élément de côté en le signalant dans le rapport.

## Table de correspondance des écrans

| Design-plan | Section | Ancien n° SPEC_ANDROID §15 | Étape | Sort de l'ancien |
| --- | --- | --- | --- | --- |
| Écran 1 · Présentation (2 panneaux) | §4 | — (2a onboarding) | 31 | `OnboardingScreen` supprimé |
| Écran 2 · Autorisations (3 tuiles + guide) | §5 | 2 diagnostic, consentement §12.3 | 32 | `ReadinessScreen`, `AccessibilityConsentScreen` supprimés |
| Écran 3 · Association (consigne, scan, pièce) | §6 | 3 | 43 | `PairingScreen` supprimé |
| Écran 3 bis · Temps pour sortir du lit | §7 | — | 46 | nouveau |
| Écran 4 · Choix des applications | §8 | 4 | 33 | `AppPickerScreen` supprimé |
| Écran 6 · Accueil, 3 états | §9 | 1 accueil, 5 heure, 6 récap, 7 session active | 41 | `HomeScreen`, `WakeTimeScreen`, `SummaryScreen`, `ActiveSessionScreen` supprimés |
| Sonnerie (feuille) | §10 | 14 | 36 | `RingtoneScreen` → feuille |
| Annuler la session (feuille + scan + message) | §11 | 9 scan requis, 11 annulée | 42 | `ScanToModifyScreen`, `CancelledScreen` supprimés |
| Écran 7 · Blocage | §12 | overlay §12.2 | 35 | overlay 3 s remplacé |
| Écrans 8/9 · Sortie de secours | §13 | — | 45 | nouveau |
| Écran 10 · Réveil + feuille + 4 échecs | §14 | 8 | 34 | `AlarmScreen` refait |
| Écran 11 · Réussite | §15 | 10 terminée | 42 | `CompletedScreen` supprimé |
| Écran 12 · Réglages, Niumi Points, Aide, À propos, Licences | §16 | 13 aide (partiel), 12 diagnostic (sans équivalent) | 44, 47 | `HelpScreen`, `IncidentDiagnosticScreen` supprimés (47, 49) |
| Notification du soir, bandeau, alerte | §17 | notification §13.1 (gardée) | 48 | nouveau |

SPEC_ANDROID §15 adopte la numérotation du design-plan à l'étape 50, avec cette table en colonne « ancien n° ».

## Points de vigilance

1. **`AlarmScreenNoStopActionTest` devient faux à l'étape 34** : l'écran 10 porte un bouton et un lien. Il est remplacé par `AlarmScreenActionsTest` (liste blanche des nœuds cliquables, chacun prouvé sans effet sur l'état), jamais supprimé sans remplaçant.
2. **`ModuleListTest`** verrouille 8 modules : aucun `:feature:settings`.
3. **`HelpTextsTest` compare `HelpTexts` à `docs/android/LIMITES.md` mot pour mot** et `docs/android/play-console/PRIVACY_POLICY.md` affirme l'absence de secours logiciel : faux dès l'étape 45. `LIMITES.md` reste le document publié hors app (v2 à l'étape 50) ; la politique est relue aux étapes 45 et 50.
4. **Durées de montée** : l'étape 28 (non commitée) a porté `VolumeRampDurations.SECONDS` à `[120, 300, 600, 900]` ; le design-plan montre « Non, 1 min, 2 min, 5 min, 10 min ». Décision Q7 avant l'étape 36 ; les sessions armées avec 900 s doivent rester lisibles (`sanitized()` → valeur la plus proche de la liste, jamais une sonnerie muette, point de vigilance 15 du plan MVP).
5. **Overlay touchable (étape 35)** : un `ComposeView` dans une fenêtre `TYPE_ACCESSIBILITY_OVERLAY` exige un `LifecycleOwner`, un `SavedStateRegistryOwner` et un `ViewModelStoreOwner` posés sur l'arbre de vues par le service ; `GLOBAL_ACTION_HOME` avant l'overlay comme aujourd'hui ; aucune exception possible dans `onAccessibilityEvent` ; retrait à la libération et dans `onUnbind`/`onInterrupt`. L'usage déclaré à Google Play change (`ACCESSIBILITY_DECLARATION.md`, `REVIEW_VIDEO_SCRIPT.md`).
6. **Direct Boot et planning** : le planning et les Niumi Points vivent dans le stockage chiffré ; un ré-armement ne se fait qu'après `USER_UNLOCKED` ; une session `COMPLETED` sur appareil verrouillé laisse l'appareil sans session jusqu'au déverrouillage (§9.3 réconciliation) ; la projection Direct Boot v4 porte la liste des Niumi Points pour le scan sur écran verrouillé (§4.4).
7. **`ReleaseHygieneTest`** compte les sites `AlarmManager` : le rappel du soir en ajoute un (`setWindow`, 3e dérogation §9.1, décision Q18). Pas de `TODO` dans le code de production (le mot est interdit par le test) : un emplacement d'image non produite se nomme `PermissionGuideImagePlaceholder` sans ce mot.
8. **Sessions successives** : `CLEAR_ACTIVE_SESSION` puis `ACTIVATION_REQUESTED` sous le même mutex ; garde Direct Boot scopée par `sessionId` ; jamais de ré-armement pendant `RELEASING` ; `EventFingerprintTest` reprend ses témoins au schéma 3.
9. **Thème au démarrage** : `SessionSnapshotPublisher.snapshot` vaut `null` avant la première lecture ; `MainActivity` lit la persistance avant de composer ; `AlarmActivity` est toujours sombre (`Theme.Niumi.Dark`).
10. **Trois invariants KMP tombent** (boîtier figé, `nfcVerifiedAt` avant `RELEASING`, aucun secours) : chaque amendement a sa ligne SPEC_CORE_KMP §17, sa ligne SPEC_IOS §19, et `SessionEngineForbiddenTransitionsTest` reste exhaustif (10 états × 14 événements).
11. **Point ouvert T9 déjà résolu par le code** : SPEC_ANDROID §12.1 et §14 utilisent `<queries>` ciblé, pas `QUERY_ALL_PACKAGES`. À reporter dans `POINTS-OUVERTS.md` à l'étape 33.
12. **`DeviceReadinessChecker`, `SessionReadinessMonitor` et la notification §13.1 ne sont jamais supprimés** : ils alimentent les trois tuiles, l'alerte et le bandeau.

### Cas limites que le design n'écrit pas, à verrouiller par un test

Le design-plan dit ce que l'app doit faire, pas tout ce qu'elle rencontrera. Chaque ligne a son test dans l'étape nommée.

1. **Coucher après minuit** (coucher 1:00, réveil 7:00) : le coucher a lieu le jour même du réveil, pas la veille ; `NextOccurrenceCalculatorTest`, étape 37.
2. **Heure déjà passée le jour coché** (jeudi coché, il est jeudi 8:00, réveil 7:00) : prochaine occurrence = vendredi si coché, sinon le jour coché suivant, jamais aujourd'hui ; `NextOccurrenceCalculatorTest`, étape 37.
3. **Apostrophes et majuscules automatiques du clavier** dans la recopie (le texte modèle contient « J'avais », « c'est », « jusqu'au ») : la comparaison ne doit pas signaler de faute ; `EmergencyTranscriptionTest`, étape 38.
4. **Compte à rebours qui traverse minuit et durée inférieure à une heure** : « Réveil dans 56 min », « 9 h 26 », jamais « 0 h 56 » ; `NiumiTimeFormatTest` (étape 30) et `HomeViewModelTest` (étape 41).
5. **Session armée avec une durée de montée retirée de la liste** (900 s de l'étape 28) : l'alarme sonne avec la durée assainie, jamais muette ; `AlarmSoundSettingsTest`, étape 36.
6. **Scan d'un Niumi Point ajouté pendant la session, sur écran verrouillé** : accepté depuis la projection Direct Boot ; `HandleValidNfcUseCaseTest` et `FileDirectBootStoreTest`, étapes 39 et 44.
7. **Rotation ou mort du processus pendant la recopie** : la saisie survit à la rotation ; après une mort du processus, l'écran 9 revient vide et l'alarme sonne toujours ; `EmergencyExitViewModelTest`, étape 45.

## Décisions à soumettre à Mehdi, par étape

Recommandations de l'auteur du plan ; aucune n'est appliquée sans réponse. Les numéros D/M/T/E viennent de `POINTS-OUVERTS.md` ; les Q sont nouvelles.

| Étape | Point | Question | Recommandation |
| --- | --- | --- | --- |
| 29 | D9 | Bouton inactif en clair : `#E8E8E4`/`#5C5C60` (écran 2) ou `#C6C6C2`/blanc (écran 4) ? | Le premier, partout |
| 29 | D13 | Apostrophe typographique (’) dans l'app ? | Oui, portée par les `Texts` et testée |
| 29 | D14, D19 | Règle de l'ambre ; titre de l'écran 2 en 26 ou 28 ? | Valider la règle ; 28 |
| 31 | D12 | Point final des titres de l'écran 1 ? | Sans point sur les deux panneaux |
| 32 | Q1 | Batterie, verrou des récents (HyperOS), Ne pas déranger, volume : aucune place dans le design. Où ? | Batterie et récents = étapes 2 et 3 du guide « Blocage » (coche exigeant les trois contrôles) ; DND et volume restent dans la notification §13.1 et le rapport, sans écran |
| 32 | Q2 | Garder la notification d'avertissement §13.1 pendant `ARMED` ? | Oui ; son tap ouvre l'écran 2 |
| 32 | D10, D18, M1 | Pas de tuile NFC ; « ? » retiré sur tuile accordée ; images du guide à produire | D10 oui ; garder « ? » ; emplacement vide sans texte de substitution tant que les images manquent |
| 33 | Q3 | Compteur « N applications choisies » = sélection validée ; T9 clos par `<queries>` | Oui |
| 34 | Q4 | Lecture NFC active seulement feuille ouverte (parité iPhone) ou dès l'écran 10 (comme aujourd'hui) ? | Seulement feuille ouverte ; §10.4/§11.2 amendés |
| 34 | Q5, E7 | Texte « Déverrouille ton téléphone… » (§4.4) ; deux textes « NFC coupé » | Garder le texte dans la feuille quand verrouillé ; chaque texte sur son écran |
| 35 | Q6, T10 | Écran 7 affiché jusqu'à « Fermer » ; déclaration Play à refaire | Oui, dans la même étape |
| 36 | Q7 | Durées : design Non/1/2/5/10 ou mesure de l'étape 28 (2/5/10/15, 1 min jugée trop rapide) ? | Suivre le design (Non/1/2/5/10) sauf avis contraire ; la rangée accepte cinq pastilles |
| 36 | D7 | Cinq sons dans la maquette, quatre dans l'APK | Les quatre existants ; cinquième ligne absente jusqu'à décision |
| 36 | D5, D6 | Ligne accueil et mention « Volume max en… » quand « Non » | Nom du son seul ; mention masquée |
| 37 | Q8 | Coucher égal au réveil | Le cadran impose 15 min d'écart ; `BEDTIME_EQUALS_WAKE` défensif en KMP |
| 37 | D17 | Aucun jour coché | Interrupteur allumé mais rien d'armé, carte « Aucun jour choisi » ; texte à écrire par Mehdi |
| 37 | Q9 | Fuseau pendant `ARMED` | Figé pour la session en cours (décision 9) ; la suivante au nouveau fuseau |
| 38 | D2 | Sortie de secours le matin : écran suivant, réveil du lendemain | Écran 11 sans « Bravo ! » ; planning laissé allumé |
| 38 | D8, D3, Q10 | Autres textes à recopier ; « 1 heure et + » = 60 ; arrondi | Un seul texte livré, rotation prête ; 60 ; minute inférieure |
| 39 | Q11 | Niumi Points valides en session : ensemble courant ou figé au coucher ? | Courant |
| 40 | Q12 | Sonnerie modifiable en `RINGING` ? | Non, `ARMED` seulement (comme aujourd'hui) |
| 41 | M10, Q13 | Forme du message « Modifiable après avoir scanné… » ; point de l'heure actuelle | Message bas d'écran, même place que « Session annulée… » ; tick à la minute |
| 42 | Q14 | Ré-armement échoué après le scan du matin | Accueil « Réveil désactivé » + bandeau ou alerte ; jamais un faux « Réveil prévu » |
| 43 | D11, M5, M6, Q15 | « Continuer » grisé sans pièce ; échecs non dessinés ; URL de la page produit | Oui ; même feuille d'échec qu'à l'écran 10 avec « Annuler » ; URL à fournir |
| 45 | D20, Q16 | Atténuation pendant la frappe (Android) | −20 dB pendant la saisie, remontée 2 s après la dernière touche ; D20 hors Android |
| 46 | D4 | Durée présélectionnée de la roue | 15 min |
| 47 | M2, M3, M4, Q17 | Rubriques non écrites ; licences ; mentions légales ; « Version 1.0 » | Lignes absentes tant que non écrites ; OFL + « Copyright IBM Corp. » ; liens inertes ; `versionName` |
| 48 | Q18, Q19, M12 | API du rappel ; « à chaque ouverture » ; bandeau/alerte sombres | `setWindow` 10 min (3e dérogation §9.1) ; chaque `onStart`, une fois ; palette sombre |
| 49 | Q20 | Export du diagnostic (écran 12 ancien supprimé) | Dernier lien de la page À propos, sinon §17 perd son export |
| 51 | T11 | Autorisation plein écran retirable | Couverte par l'alerte ; à mesurer |

Hors périmètre Android, à ne pas traiter : D1, T1 à T8 (iPhone), D16 (sans effet sur le code).

## Interfaces transverses — Ajouts des Lots 8 à 16

Définies ici une fois, créées à l'étape indiquée, consommées ensuite sans renommage.

```kotlin
// :core:designsystem — com.niumi.designsystem.ui.theme (étape 29)
@Immutable data class NiumiPalette(
    val background: Color, val surface: Color, val ink: Color, val textSecondary: Color, val border: Color,
    val separator: Color, val track: Color, val disabled: Color, val tileGrey: Color, val cellGrey: Color,
    val amber: Color, val amberTint: Color, val onInk: Color, val onInkSecondary: Color, val veil: Color,
    val sheetOutline: Color, val menuBorder: Color, val cancelButton: Color, val disabledFill: Color,
    val disabledText: Color, val mismatchTint: Color,
)
object NiumiPalettes { val Light: NiumiPalette; val Dark: NiumiPalette }            // valeurs = valeurs-de-design.json
val LocalNiumiPalette: ProvidableCompositionLocal<NiumiPalette>
object NiumiFont { val PlexSans: FontFamily }                                      // 400 / 500 / 600
object NiumiType { val clock112, dial42, title40, instruction30, dialDisabled30, screenTitle28, sheetTitle24,
                   cardTitle20, body16, button17, label16, chip15, caption13, appLabel12: TextStyle }
object NiumiShapes { val button, card: RoundedCornerShape /*14*/; val tile /*16*/; val sheet /*28 haut*/; val appIcon /*7*/ }
object NiumiDimens { val screenHorizontal = 24.dp; val screenBottom = 34.dp; val screenTop = 56.dp; val minTouch = 44.dp;
                     val primaryButton = 56.dp; val sheetButton = 52.dp; val transcriptionButton = 48.dp; val dayChip = 44.dp;
                     val switchWidth = 52.dp; val switchHeight = 32.dp; val switchThumb = 26.dp; val appIcon = 28.dp }
@Composable fun NiumiTheme(dark: Boolean, content: @Composable () -> Unit)        // fournit LocalNiumiPalette + MaterialTheme dérivé

// :core:designsystem — com.niumi.designsystem.component (étapes 30, 34, 41, 48)
@Composable fun NiumiPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier)
@Composable fun NiumiOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun NiumiDiscreetLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun NiumiPillButton(text: String, selected: Boolean, onClick: () -> Unit, height: Dp = 44.dp)
@Composable fun NiumiBottomSheet(onDismiss: () -> Unit, title: String?, showClose: Boolean, content: @Composable ColumnScope.() -> Unit)
@Composable fun NiumiCard(filled: Boolean, content: @Composable ColumnScope.() -> Unit)                 // filled = encre (carte « Réveil activé »)
@Composable fun NiumiListRow(title: String, trailing: @Composable RowScope.() -> Unit, onClick: (() -> Unit)?, enabled: Boolean = true, locked: Boolean = false)
@Composable fun NiumiDayChip(label: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit)
@Composable fun NiumiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true)
@Composable fun NiumiTopBar(onBack: (() -> Unit)?, title: String?)                                      // flèche de retour + titre 28
@Composable fun NiumiScreenScaffold(bottom: @Composable (() -> Unit)?, content: @Composable ColumnScope.() -> Unit)
@Composable fun NiumiLogo(modifier: Modifier = Modifier)                                                  // logo clair/sombre selon la palette
@Composable fun NiumiPageIndicator(count: Int, current: Int)
@Composable fun NfcScanSheet(state: ScanSheetState, phrase: String, onCancel: () -> Unit, onRetry: () -> Unit,
                             onOpenNfcSettings: () -> Unit, onEmergencyExit: (() -> Unit)?)             // étape 34
@Composable fun NiumiDial(state: DialState, onBedtimeChanged: ((LocalTime) -> Unit)?, onWakeTimeChanged: ((LocalTime) -> Unit)?) // étape 41
sealed interface DialState { data class Scheduled(...); data class Disabled(...); data class InSession(...) } // bedtime, wake, now
@Composable fun NiumiProblemBanner(text: String, linkText: String, onLink: () -> Unit)                   // étape 48
@Composable fun NiumiAlertDialog(title: String, body: @Composable () -> Unit, primary: String, onPrimary: () -> Unit, secondaryLink: String, onSecondary: () -> Unit)
object NiumiTimeFormat { fun clock(time: LocalTime): String /* "7:00", "22:30" */; fun duration(minutes: Int): String /* "9 h 26", "56 min" */; fun sleep(minutes: Int): String /* "8 h 30 de sommeil" */ }

// :core:system — com.niumi.system.session (étapes 29, 39)
fun SessionSnapshotDto?.isSessionDark(): Boolean   // voir « Contraintes globales », thème par état
// SESSION_FINAL_STATES : + DISARMED (étape 39)

// :core:system — com.niumi.system.nfc (étape 34)
sealed interface ScanSheetState { data object Hidden; data object Ready; data class Failed(val reason: ScanFailureReason) }
enum class ScanFailureReason { NOT_A_NIUMI_POINT, NOT_REGISTERED, UNREADABLE, NFC_DISABLED }
fun scanFailureOf(outcome: ScanOutcome?, availability: NfcAvailability): ScanFailureReason?

// :core:system — com.niumi.system.permissions (étapes 32, 48)
enum class NiumiPermission { ALARM, APP_BLOCKING, NOTIFICATIONS }
data class PermissionStatus(val permission: NiumiPermission, val granted: Boolean)
interface PermissionStatusSource { fun status(): List<PermissionStatus> }   // agrège DeviceReadinessChecker ; NOT_APPLICABLE = accordé
object PermissionAlertPolicy { fun alertFor(statuses: List<PermissionStatus>): PermissionAlert? }
data class PermissionAlert(val missing: List<NiumiPermission>)
sealed interface HomeProblem { data object NfcDisabled; data object NoNiumiPoint }
object ProblemBannerPolicy { fun bannerFor(nfc: NfcAvailability, pointCount: Int): HomeProblem? }   // un seul, le plus grave d'abord

// :feature:session — com.niumi.feature.session.blocking (étape 35)
interface BlockOverlayController { val isShowing: Boolean; fun show(): OperationResult; fun hide() }  // ComposeView, FLAG_NOT_FOCUSABLE seul, sans minuterie

// :core:system — com.niumi.system.plan (étapes 39, 40, 48)
data class WakePlan(val enabled: Boolean, val enabledIsoDays: Set<Int>, val wakeTimeIso: String, val bedtimeIso: String) {
    companion object { val DEFAULT = WakePlan(enabled = false, enabledIsoDays = setOf(1, 2, 3, 4, 5), "07:00", "22:30") }
}
interface WakePlanStore { suspend fun read(): WakePlan; suspend fun write(plan: WakePlan); val plan: Flow<WakePlan> }   // DataStore niumi_wake_plan, garde de déverrouillage
interface TimeToGetUpPreferences { suspend fun declaredMinutes(): Int?; suspend fun setDeclaredMinutes(value: Int) }   // DataStore niumi_time_to_get_up
enum class PlanChangeReason { SWITCH, DAYS, WAKE_TIME, BEDTIME, APP_SELECTION, PROCESS_START, USER_UNLOCKED, TIME_CHANGED, TIMEZONE_CHANGED, SESSION_FINISHED }
sealed interface PlanApplyResult { data class Armed(val occurrence: NextOccurrenceDto); data object Disabled; data object NoDayEnabled;
                                   data object LockedBySession; data class Failed(val failure: ActivationFailure) }
interface WakePlanScheduler {
    suspend fun apply(plan: WakePlan, reason: PlanChangeReason): PlanApplyResult   // mutex : DISARM_REQUESTED si ARMED en attente, puis ACTIVATION ; programme le rappel du soir
    suspend fun rearmAfterFinalState(): PlanApplyResult                           // COMPLETED / DISARMED → apply ; CANCELLED → enabled = false
}
interface EveningReminderScheduler { fun schedule(occurrence: NextOccurrenceDto, plan: WakePlan, appCount: Int): OperationResult; fun cancel(): OperationResult }
// setWindow RTC_WAKEUP à startsAt − 30 min, fenêtre 10 min ; canal niumi_evening_reminder ; aucune action ; tap → accueil

// :core:system — com.niumi.system.session (étape 40)
// SessionEventFactory : + disarmRequested(snapshot), + emergencyExitConfirmed(snapshot, proof: EmergencyExitProof)
// ReconcileAction : + PlanRearmed(triggerAtEpochMillis), + PlanDisarmed
// TechnicalEventType : + SESSION_DISARMED, PLAN_REARMED, EMERGENCY_EXIT_CONFIRMED, EVENING_REMINDER_SCHEDULED

// :core:database (étape 39)
data class NiumiPoint(val credential: PairedBoxCredentialDto, val roomName: String, val pairedAtEpochMillis: Long)
sealed interface AddPointResult { data object Added; data class AlreadyPaired(val roomName: String) }
sealed interface RemovePointResult { data object Removed; data object LastPointKept; data object Unknown }
interface PairedBoxStore {   // remplace current()/replace()/clear()
    suspend fun all(): List<NiumiPoint>; val points: Flow<List<NiumiPoint>>
    suspend fun add(point: NiumiPoint): AddPointResult; suspend fun rename(boxId: String, roomName: String); suspend fun remove(boxId: String): RemovePointResult
}
// AndroidSessionExtras : boxId / boxTokenSha256Hex retirés ; AlarmSessionEntity + releasedBy: String?, disarmedAtEpochMillis: Long? ; NiumiDatabase v5 (MIGRATION_4_5)
// DirectBootSnapshot.Active : + releasedBy, disarmedAtEpochMillis, pairedBoxes: List<NiumiPoint> ; DIRECT_BOOT_PROJECTION_SCHEMA_VERSION = 4
// LoadResult.Present : + pairedBoxes

// :app — routes finales (NiumiRoute, étapes 31 à 47)
// Presentation, Permissions, PermissionGuide(permission: String), PointPairing(mode: String /* SETUP | ADD */),
// TimeToGetUp(mode: String /* SETUP | EDIT */), AppSelection, Home, Success, EmergencyExit(origin: String /* HOME | ALARM */),
// Settings, NiumiPoints, Help, HelpTopic(id: String), About, Licenses
```

### Contrat KMP 1.4 (étapes 37 et 38)

```kotlin
// :shared:core — com.niumi.core.domain
enum class SessionState { PREPARING, ARMED, RINGING, AWAITING_NFC, TRIGGERED_AWAITING_NFC, RELEASING, COMPLETED, CANCELLED, DISARMED, FAILED }
enum class ReleaseTarget { COMPLETED, CANCELLED, DISARMED }
enum class ReleaseMeans { NFC_SCAN, EMERGENCY_EXIT, PLAN_CHANGE }
// SessionEventKind : + DISARM_REQUESTED, + EMERGENCY_EXIT_CONFIRMED
// SessionSnapshot : + releasedBy: ReleaseMeans? = null, + disarmedAtEpochMillis: Long? = null ; SCHEMA_VERSION = 3
//   (lecture v2 : releasedBy = NFC_SCAN si nfcVerifiedAtEpochMillis != null, sinon null)
// SessionEvent : + emergencyProof: EmergencyExitProof? ; ViolationCode : + MISSING_EMERGENCY_PROOF, + NO_PAIRED_BOX
class EmergencyExitProof internal constructor(val textId: String, val sessionId: String, val eventId: String, val expectedRevision: Long, val verifiedAtEpochMillis: Long)

// :shared:core — com.niumi.core.schedule
data class WakePlan(val enabledIsoDays: Set<Int>, val wakeTimeIso: String, val bedtimeIso: String)   // ISO 1 = lundi … 7 = dimanche
data class NextOccurrenceInput(val plan: WakePlan, val zoneId: String, val nowEpochMillis: Long)
enum class NextOccurrenceStatus { VALID, NO_DAY_ENABLED, INVALID_TIME, UNKNOWN_ZONE, BEDTIME_EQUALS_WAKE }
data class NextOccurrence(val wakeSchedule: WakeSchedule, val blockingSchedule: BlockingSchedule, val blockingStartsImmediately: Boolean)
data class NextOccurrenceResult(val status: NextOccurrenceStatus, val occurrence: NextOccurrence?)
object NextOccurrenceCalculator { fun compute(input: NextOccurrenceInput): NextOccurrenceResult }
// Règle §8.4 : pour d ∈ 0..7, candidat = (aujourd'hui + d) si son jour ISO est coché, à wakeTimeIso ; première occurrence strictement future ;
// coucher la veille si bedtime > wakeTime, sinon le jour même ; coucher ≤ now → blockingStartsImmediately = true, blockingSchedule immédiat ;
// DST délégué à WakeScheduleCalculator ; bedtime == wake → BEDTIME_EQUALS_WAKE.

// :shared:core — com.niumi.core.nfc
object BoxSetVerifier { fun verify(payload: BoxPayload, credentials: List<PairedBoxCredential>, context: NfcVerificationContext?): BoxVerificationResult }
// MATCH sur le premier credential concordant ; boxId connu + token faux → TOKEN_MISMATCH ; aucun → BOX_MISMATCH ; liste vide → NO_PAIRED_BOX

// :shared:core — com.niumi.core.emergency
object EmergencyExitTexts { val ids: List<String>; fun text(id: String): String?; fun charCount(id: String): Int? }   // texte validé de 197 caractères, id "v1-01"
enum class TranscriptionStatus { COMPLETE, INCOMPLETE, MISMATCH, UNKNOWN_TEXT }
data class TranscriptionResult(val status: TranscriptionStatus, val matchedWordCount: Int, val mismatchWordIndex: Int?,
                               val expectedCharCount: Int, val typedCharCount: Int, val proof: EmergencyExitProof?)
object EmergencyTranscription { fun verify(textId: String, typed: String, context: NfcVerificationContext?): TranscriptionResult }
// Normalisation : minuscules, accents retirés (NFD), ponctuation retirée, espaces multiples réduits ; comparaison mot à mot ;
// le dernier mot tapé peut être un préfixe du mot attendu (INCOMPLETE) ; preuve seulement sur COMPLETE avec contexte.
// Écart assumé à SPEC_CORE_KMP §7.3 (« pas de texte affiché dans le module commun ») : le texte est un élément de protocole partagé, documenté §12.

// :shared:core — com.niumi.core.insight
data class TimeToGetUpInput(val alarmAtEpochMillis: Long, val releasedAtEpochMillis: Long, val declaredMinutes: Int)
data class TimeToGetUpResult(val elapsedMinutes: Int, val savedMinutes: Int?)   // savedMinutes = declared − elapsed si > 0, sinon null
object TimeToGetUpCalculator { fun compute(input: TimeToGetUpInput): TimeToGetUpResult }

// :shared:core — com.niumi.core.interop (NiumiCoreVersion.SCHEMA_VERSION = 3)
data class WakePlanDto(val enabledIsoDays: Set<Int>, val wakeTimeIso: String, val bedtimeIso: String)
data class NextOccurrenceInputDto(val plan: WakePlanDto, val zoneId: String, val nowEpochMillis: Long)
data class NextOccurrenceDto(val wakeSchedule: WakeScheduleDto, val blockingSchedule: BlockingScheduleDto, val blockingStartsImmediately: Boolean)
data class NextOccurrenceResultDto(val status: NextOccurrenceStatus, val occurrence: NextOccurrenceDto?)
data class EmergencyExitTextDto(val id: String, val text: String, val charCount: Int)
data class EmergencyTranscriptionInputDto(val textId: String, val typed: String)
data class EmergencyTranscriptionResultDto(val status: TranscriptionStatus, val matchedWordCount: Int, val mismatchWordIndex: Int?,
                                           val expectedCharCount: Int, val typedCharCount: Int, val proof: EmergencyExitProof?)
data class TimeToGetUpInputDto(val alarmAtEpochMillis: Long, val releasedAtEpochMillis: Long, val declaredMinutes: Int)
data class TimeToGetUpResultDto(val elapsedMinutes: Int, val savedMinutes: Int?)
// SessionSnapshotDto : + releasedBy: ReleaseMeans? = null, + disarmedAtEpochMillis: Long? = null ; SessionEventDto : + @Transient emergencyProof
class NiumiCoreFacade {   // les 7 méthodes actuelles conservées ; verifyBox conservée pour l'association (credential unique)
    fun computeNextOccurrence(input: NextOccurrenceInputDto): NextOccurrenceResultDto
    fun verifyBoxAmong(payload: BoxPayloadDto, credentials: List<PairedBoxCredentialDto>, context: NfcVerificationContextDto?): BoxVerificationResultDto
    fun emergencyExitTexts(): List<EmergencyExitTextDto>
    fun verifyEmergencyTranscription(input: EmergencyTranscriptionInputDto, context: NfcVerificationContextDto?): EmergencyTranscriptionResultDto
    fun computeTimeToGetUp(input: TimeToGetUpInputDto): TimeToGetUpResultDto
}
```

Transitions ajoutées à §5.1 :

| État source | Événement | État cible | Cible de libération |
| --- | --- | --- | --- |
| `ARMED`, blocage en attente, avant `triggerAtEpochMillis` | `DISARM_REQUESTED` | `RELEASING` | `DISARMED` (effets : `PUBLISH`, `CANCEL_ALARM`, `CANCEL_BLOCKING_START` ; seul `CANCEL_ALARM` requis) |
| `ARMED` blocage appliqué | `EMERGENCY_EXIT_CONFIRMED` (preuve) | `RELEASING` | `CANCELLED` |
| `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` | `EMERGENCY_EXIT_CONFIRMED` (preuve) | `RELEASING` | `COMPLETED` |

Refus ajoutés à §5.2 : `DISARM_REQUESTED` avec blocage appliqué → `BLOCKING_ALREADY_APPLIED` ; hors `ARMED` → `INVALID_STATE_TRANSITION` ; `EMERGENCY_EXIT_CONFIRMED` sans preuve → `MISSING_EMERGENCY_PROOF` ; depuis `ARMED` en attente → `INVALID_STATE_TRANSITION` (avant le coucher, on désarme). Invariants §4 réécrits : « `releasedBy` est renseigné avant l'entrée dans `RELEASING` ; `nfcVerifiedAt` existe si et seulement si `releasedBy == NFC_SCAN` ; le Niumi Point vérifié appartient à l'ensemble **courant** des points enregistrés ». Décisions §2 réécrites : 3 (n'importe quel Niumi Point enregistré), 4 (avant le coucher, le planning se modifie sans scan ; après, scan ou sortie de secours), 7 et 13 (la sortie de secours est le seul secours, prouvée par la recopie), 11 (ajout, renommage, suppression possibles en session ; au moins un point), 15 (inchangée sur l'alarme programmée dès l'activation). Politique §10 : les contrôles de préparation ne bloquent plus l'activation (`checks = emptyList()` côté Android ; `hasPairedBox` = au moins un point) ; `TRIGGER_NOT_IN_FUTURE` reste. Relance iOS (`AWAITING_NFC`) documentée en §11.2 comme effet natif répété, sans transition nouvelle.

---

## Phase K — Lot 8 : fondations visuelles (étapes 29 et 30)

**Lot ajouté le 2026-10-10.** Quatre décisions proposées, à valider avec les points D9, D13, D14, D19 :

1. **`NiumiPalette` via `CompositionLocal`, et un `ColorScheme` Material 3 dérivé** pour les composants Material résiduels (`TimePicker` n'est plus utilisé ; `TextField` de la recherche, `ModalBottomSheet` comme socle de `NiumiBottomSheet`). Les rôles du design (piste, tuile grise, case grisée, teinte ambre, liseré de feuille, bouton gris d'annulation) n'ont pas d'équivalent M3 : les écrans lisent `LocalNiumiPalette.current`, jamais `MaterialTheme.colorScheme`.
2. **`NiumiTheme(dark: Boolean)` sans valeur par défaut** ; la règle `isSessionDark()` vit dans `:core:system` à côté de `isSessionInProgress()`.
3. **Palette V2 retirée** (Laiton, Terracotta, Crème, `NiumiColors`) ; aucune couleur d'alerte : les échecs se disent en texte.
4. **Police embarquée dès maintenant** avec son fichier de licence dans `res/raw`, lu par la page Licences à l'étape 47.

**Ce que chaque étape produit.** 29 : l'app actuelle sur le nouveau thème, la police et la règle clair/sombre. 30 : les composants des maquettes, testés, prêts pour les écrans.

### Étape 29 : jetons, police, thème par état

**Specs à lire :** `VALEURS-DE-DESIGN.md` §1 à §4 et §6 ; `valeurs-de-design.json` ; `design-plan/police/LISEZ-MOI.md` ; `logo/` ; SPEC_ANDROID §5, §6, §15 « Règles UI » ; points de vigilance 9 et 12.

**Points ouverts touchés :** D9, D13, D14, D19.

**Fichiers :**
- `:core:designsystem` — créer `DS/ui/theme/NiumiPalette.kt`, `NiumiPalettes.kt` (deux objets, un commentaire par rôle citant la ligne de `VALEURS-DE-DESIGN.md`), `NiumiFont.kt`, `NiumiType.kt`, `NiumiShapes.kt`, `NiumiDimens.kt` ; `androidApp/core/designsystem/src/main/res/font/ibm_plex_sans_regular.ttf`, `_medium.ttf`, `_semibold.ttf` (téléchargés depuis la page des versions de `github.com/IBM/plex`, formats TTF pour Android) ; `res/raw/ofl_ibm_plex_sans.txt` (le fichier de licence livré avec l'archive téléchargée, pas celui de Fontsource) ; `res/drawable/niumi_logo_light.png` et `niumi_logo_dark.png` copiés de `design-plan/logo/` (vectoriel si Mehdi en fournit un) ; modifier `DS/ui/theme/NiumiTheme.kt` (signature `NiumiTheme(dark: Boolean, content)`, fournit `LocalNiumiPalette`, `MaterialTheme(colorScheme = derived, typography = NiumiType.material())`), `res/values/themes.xml` (`Theme.Niumi` parent `android:Theme.Material.Light.NoActionBar` fond `#FAFAF8` ; `Theme.Niumi.Dark` fond `#0E0E10`) ; supprimer `DS/ui/theme/NiumiColors.kt`.
- `:core:system` — modifier `SYS/session/SessionStates.kt` (+ `fun SessionSnapshotDto?.isSessionDark(): Boolean`).
- `:app` — modifier `APP/MainActivity.kt` (lit `SessionSnapshotPublisher.snapshot` ; avant la première valeur, lit `SessionPersistenceGateway.load()` une fois ; `NiumiTheme(dark = snapshot.isSessionDark())`) ; `androidApp/app/src/main/AndroidManifest.xml` (thème).
- `:feature:ringing` — modifier `RING/AlarmActivity.kt` (`NiumiTheme(dark = true)`), `androidApp/feature/ringing/src/main/AndroidManifest.xml` (`Theme.Niumi.Dark`).
- Tous les écrans existants : remplacer `MaterialTheme.colorScheme.*` et `NiumiColors.*` par `LocalNiumiPalette.current.*` ; aucun autre changement visuel à cette étape.
- Tests : créer `NiumiPaletteTest` (chaque valeur de `NiumiPalettes` égale à `valeurs-de-design.json`, lu comme ressource de test), `NiumiTypeTest` (tailles et graisses du tableau §3), `SessionStatesTest` étendu (`isSessionDark` : `null` → faux ; `ARMED` en attente → faux ; `ARMED` appliqué → vrai ; `RINGING`, `AWAITING_NFC`, `TRIGGERED_AWAITING_NFC` → vrai ; `RELEASING` → vrai ; finaux → faux).

**Produit :** l'application actuelle, sur la nouvelle palette, la nouvelle police et la règle clair/sombre par état.

- [ ] **Écrire `NiumiPaletteTest` et `NiumiTypeTest`**, vérifier l'échec, implémenter jetons, police, formes, dimensions.
- [ ] **Étendre `SessionStatesTest`**, implémenter `isSessionDark()`, `NiumiTheme(dark)`, les deux thèmes XML, `MainActivity` et `AlarmActivity`.
- [ ] **Remplacer toutes les lectures de couleurs** dans les écrans existants ; supprimer `NiumiColors.kt`.
- [ ] **Vérifier :** batterie standard, puis :

```bash
grep -rn "isSystemInDarkTheme\|NiumiColors\|DayNight" androidApp --include=*.kt --include=*.xml   # attendu : rien
grep -rn "Color(0x" androidApp --include=*.kt | grep -v NiumiPalettes.kt                           # attendu : rien
```

- [ ] **Valider sur appareil, rédiger `ETAPE-29.md`.**

**Tests manuels :** téléphone en mode sombre système, app hors session → claire ; armer une session (ancien parcours) avec blocage immédiat → l'accueil et l'écran de session passent en sombre ; blocage différé → clair jusqu'à l'heure, puis sombre ; écran de réveil sombre ; après le scan, écran « terminée » clair. Police visible (chiffres à largeur fixe sur l'heure).

**Terminé quand :** les deux greps sont vides ; `NiumiPaletteTest` prouve chaque valeur ; la bascule clair/sombre ne dépend pas du téléphone (constaté) ; SPEC_ANDROID §5 et §6 décrivent la police embarquée, le thème par état et le rôle de `:core:designsystem`.

### Étape 30 : composants du design system

**Specs à lire :** `VALEURS-DE-DESIGN.md` §4 (formes, boutons par rôle, ombres) ; `SPECIFICATION.md` §2 (écriture des heures) ; maquettes 13, 14, 16, 19, 23, 37 (sources HTML pour les valeurs) ; SPEC_ANDROID §15 « Règles UI » (TalkBack, 44 dp).

**Points ouverts touchés :** D9 (bouton inactif), D13.

**Fichiers :**
- `:core:designsystem` — créer `DS/component/NiumiPrimaryButton.kt`, `NiumiOutlinedButton.kt`, `NiumiDiscreetLink.kt`, `NiumiPillButton.kt`, `NiumiBottomSheet.kt` (socle `ModalBottomSheet`, coins 28, pleine largeur, poignée, voile 45 %/60 %, liseré 1 px en sombre), `NiumiCard.kt`, `NiumiListRow.kt` (séparateur 1 px, chevron ou cadenas, grisé si `enabled = false`), `NiumiDayChip.kt`, `NiumiSwitch.kt` (52 × 32, pastille 26, ambre si activé), `NiumiTopBar.kt`, `NiumiScreenScaffold.kt` (marges 24/34/56, bord à bord), `NiumiLogo.kt`, `NiumiPageIndicator.kt`, `DS/format/NiumiTimeFormat.kt` ; `res/drawable/ic_chevron_right.xml`, `ic_lock.xml`, `ic_close.xml`, `ic_back.xml`, `ic_settings.xml`, `ic_check.xml`, `ic_more.xml` (traits simples, pas d'ambre dans les icônes) ; déplacer `ic_chevron_right.xml` depuis `:feature:session`.
- Tests : `NiumiTimeFormatTest` (7:00, 22:30, 0:05 ; « 9 h 26 », « 56 min », « 1 h 00 » ; « 8 h 30 de sommeil ») ; instrumentés `NiumiComponentsSemanticsTest` (chaque composant cliquable a un rôle, une zone ≥ 44 dp, et aucun `onClick` quand `enabled = false`), `NiumiBottomSheetTest` (titre, croix, fermeture par le voile).

**Produit :** une bibliothèque de composants avec `@Preview` clair et sombre, utilisée par toutes les étapes suivantes.

- [ ] **Écrire `NiumiTimeFormatTest`**, implémenter le formateur.
- [ ] **Écrire `NiumiComponentsSemanticsTest` et `NiumiBottomSheetTest`**, implémenter les composants un par un avec leurs deux `@Preview`.
- [ ] **Vérifier :** batterie standard ; `./gradlew :core:designsystem:connectedDebugAndroidTest` (appareil).
- [ ] **Rédiger `ETAPE-30.md`.**

**Tests manuels :** l'écran de `@Preview` de chaque composant comparé à sa maquette, en clair et en sombre ; TalkBack annonce rôle et état.

**Terminé quand :** chaque composant a ses deux preview et un test ; aucune valeur numérique de dimension dans un fichier de composant hors `NiumiDimens`/`NiumiShapes`.

## Phase L — Lot 9 : écrans sans changement métier (étapes 31 à 36)

**Lot ajouté le 2026-10-10.** Ces écrans n'attendent pas le contrat 1.4 ; ils valident le design system sur de vrais écrans et gardent l'app utilisable de bout en bout. **Navigation transitoire** : jusqu'à l'étape 41, le parcours reste Présentation → Autorisations → Association (ancienne) → Applications → ancien choix de l'heure (écran 5) → récapitulatif (6) → session active (7) ; l'étape 32 branche « Continuer » de l'écran 2 sur `Pairing`, et `HomeDestination` mène à `Permissions` quand la présentation est acquittée. Quatre décisions proposées :

1. **L'écran 2 remplace `ReadinessScreen` et `AccessibilityConsentScreen`.** Les trois tuiles sont des agrégats des contrôles de §13 (`PermissionStatusSource`) ; le consentement §12.3 (obligation Google Play) est porté par le guide « ? » de la tuile « Blocage des applications » et par le texte de la tuile. Les contrôles sans tuile suivent la décision Q1.
2. **Lecture NFC pilotée par la feuille « Prêt à scanner »** (décision Q4) ; `AlarmActivity` n'active le Reader Mode que feuille ouverte.
3. **Écran 7 plein écran** : fin de l'overlay de 3 s ; un `ComposeView` touchable, retiré par « Fermer », « Ouvrir Niumi », la libération, `onInterrupt` ou `onUnbind`.
4. **Durées de montée selon Q7** ; `VolumeRampDurations.sanitized(seconds)` ramène toute valeur hors liste à la plus proche, pour les préférences et les sessions déjà armées (jamais de sonnerie muette).

**Ce que chaque étape produit.** 31 : écran 1. 32 : écran 2 et son guide. 33 : écran 4 et sa feuille. 34 : écran 10, feuille de scan, quatre échecs. 35 : écran 7. 36 : feuille Sonnerie et ligne « Sonnerie ».

### Étape 31 : écran 1 · Présentation

**Specs à lire :** `SPECIFICATION.md` §4 ; maquettes 01 et 02 ; `TEXTES-DES-MAQUETTES.md` §01–02 ; SPEC_ANDROID §15 (écran 2a) ; `OnboardingTexts.kt` existant (ce qui disparaît : les limites, désormais dans l'aide).

**Points ouverts touchés :** D12, D13.

**Fichiers :**
- `:feature:setup` — créer `SETUP/presentation/PresentationScreen.kt` (`PresentationScreen(state, actions)`, `PresentationRoute(onConfigure)`), `PresentationTexts.kt` (`PANEL_1_TITLE`, `PANEL_1_BODY`, `PANEL_2_TITLE`, `PANEL_2_BODY`, `CONTINUE`, `CONFIGURE`), `PresentationViewModel.kt` (`state: PresentationUiState(panel: Int)`, `next()`, `back()`, `acknowledge()` → `SetupPreferences.acknowledgeOnboarding()`) ; `DS/drawing/LockedPhoneDrawing.kt` et `PhoneNearPointDrawing.kt` (dessins vectoriels Compose, un détail ambre chacun) ; supprimer `SETUP/onboarding/*` et ses tests.
- `:app` — modifier `APP/navigation/NiumiRoute.kt` (`Onboarding` → `Presentation`), `NiumiNavHost.kt`, `HomeDestination.kt` ; modifier `androidApp/app/src/test/kotlin/com/niumi/app/help/HelpTextsTest.kt` (retirer la comparaison avec `OnboardingTexts`).
- Tests : `PresentationTextsTest`, `PresentationViewModelTest` (panneau 1 → 2 → acquittement une seule fois), `PresentationScreenTest` (instrumenté : textes, deux traits, flèche de retour seulement sur le panneau 2), `HomeDestinationTest` mis à jour.

**Produit :** les deux panneaux, impossibles à passer, qui mènent à l'écran 2.

- [ ] **Écrire `PresentationTextsTest` et `PresentationViewModelTest`**, implémenter textes, ViewModel, écran et dessins.
- [ ] **Mettre à jour les routes et `HelpTextsTest`**, supprimer l'onboarding.
- [ ] **Vérifier :** batterie standard ; `grep -rn "OnboardingTexts" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-31.md`.** SPEC_ANDROID §15 : écran 1 décrit, écran 2a retiré.

**Tests manuels :** première ouverture → panneau 1 → Continuer → panneau 2 → retour → panneau 1 → Configurer Niumi → écran 2 ; relancer l'app : la présentation ne revient pas.

**Terminé quand :** textes verrouillés ; `OnboardingTexts` absent ; retour arrière sur le panneau 1 quitte l'app (pas de contournement).

### Étape 32 : écran 2 · Autorisations et guide

**Specs à lire :** `SPECIFICATION.md` §5 ; maquettes 03, 04 ; SPEC_ANDROID §12.3, §13 (tableau des contrôles et actions), §13.1, §14 ; `SYS/readiness/ReadinessCheckId.kt`, `ReadinessAction.kt`, `ReadinessSettingsIntents.kt` ; `docs/android/play-console/ACCESSIBILITY_DECLARATION.md`.

**Points ouverts touchés :** D10, D18, D19, M1, M8, Q1, Q2.

**Fichiers :**
- `:core:system` — créer `SYS/permissions/NiumiPermission.kt`, `PermissionStatusSource.kt`, `AndroidPermissionStatusSource.kt` (agrège `DeviceReadinessChecker.check()` : `ALARM` ⇐ `EXACT_ALARM` ∧ `FULL_SCREEN_INTENT` ; `APP_BLOCKING` ⇐ `ACCESSIBILITY_SERVICE` ∧ `BATTERY_OPTIMIZATION` ∧ `RECENTS_LOCK` selon Q1 ; `NOTIFICATIONS` ⇐ `NOTIFICATIONS` ∧ `ALARM_CHANNEL` ; `NOT_APPLICABLE` compte accordé), `SYS/permissions/di/PermissionsModule.kt`.
- `:feature:setup` — créer `SETUP/permissions/PermissionsScreen.kt` (`PermissionsScreen(state, actions)`, `PermissionsRoute(onContinue, onOpenGuide)`), `PermissionsTexts.kt` (titre, texte, noms, phrases, `AUTHORIZE`, `CONTINUE`, `GUIDE_*`), `PermissionsViewModel.kt` (`state: PermissionsUiState(tiles: List<TileState>, canContinue)`, `refresh()` sur `ON_RESUME`, `authorize(permission)` → lance la demande système ou ouvre le réglage via `ReadinessSettingsIntents`), `PermissionGuideScreen.kt`, `PermissionGuideSteps.kt` (étapes par autorisation ; images = emplacement vide tant que M1) ; supprimer `SETUP/readiness/*`, `SETUP/accessibility/*` et leurs tests (`ReadinessMessages` conservé dans `:core:system` pour la notification §13.1).
- `:app` — modifier `NiumiRoute.kt` (`Readiness`, `AccessibilityConsent` → `Permissions`, `PermissionGuide(permission)`), `NiumiNavHost.kt` (`Permissions.onContinue` → `Pairing` tant que l'étape 43 n'existe pas), `HomeDestination.kt`, `APP/navigation/DeepLinkDestination.kt` (tap de la notification §13.1 → `Permissions`), `SetupGate` si présent.
- Tests : `PermissionStatusSourceTest` (table des agrégats, `NOT_APPLICABLE`), `PermissionsTextsTest`, `PermissionsViewModelTest` (trois accordées → `canContinue` ; refus → tuile non cochée, rien d'autre), `PermissionsScreenTest` (instrumenté : coche remplace « Autoriser » et « ? » selon D18 ; « Continuer » grisé).

**Produit :** un écran à trois tuiles, fidèle aux maquettes, qui reflète l'état réel des contrôles et rouvre depuis les réglages (étape 47).

- [ ] **Écrire `PermissionStatusSourceTest`**, implémenter la source.
- [ ] **Écrire `PermissionsTextsTest`, `PermissionsViewModelTest`**, implémenter écran, guide, routes ; supprimer diagnostic et consentement.
- [ ] **Vérifier :** batterie standard ; `grep -rn "ReadinessScreen\|AccessibilityConsent" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-32.md`.** SPEC_ANDROID §12.3 (consentement porté par la tuile et le guide), §13 (présentation : trois tuiles, le reste selon Q1), §15.

**Tests manuels :** refuser les notifications → tuile non cochée, « Continuer » grisé ; activer le service d'accessibilité depuis le guide → coche au retour ; retirer une autorisation après coup → l'app la détecte au retour (l'alerte n'arrive qu'à l'étape 48).

**Terminé quand :** chaque tuile reflète les contrôles réels (test) ; `DeviceReadinessChecker` et §13.1 inchangés ; `ACCESSIBILITY_DECLARATION.md` cite le nouveau chemin de consentement.

### Étape 33 : écran 4 · Choix des applications

**Specs à lire :** `SPECIFICATION.md` §8 ; maquettes 10, 11, 12 ; SPEC_ANDROID §12.1 ; `SETUP/apps/AppPickerViewModel.kt` et `SYS/apps/AppSelectionStore.kt` existants.

**Points ouverts touchés :** T9 (clos, point de vigilance 11), M11, Q3.

**Fichiers :**
- `:feature:setup` — créer `SETUP/apps/AppSelectionScreen.kt` (carte avant/après, mentions, `Continuer`), `AppSelectionSheet.kt` (feuille « Applications à bloquer », croix, champ « Rechercher », grille 4 colonnes, coche, compteur, « Valider » grisé sans choix), `AppSelectionTexts.kt` ; renommer `AppPickerViewModel` → `AppSelectionViewModel.kt` (brouillon de feuille distinct de la sélection enregistrée ; la croix abandonne le brouillon ; « Valider » écrit `AppSelectionStore.replace()`), `AppSelectionUiState.kt` ; supprimer `AppPickerScreen.kt`, `AppPickerTexts.kt` et leurs tests.
- `:app` — `NiumiRoute.AppPicker` → `AppSelection` ; `NiumiNavHost.kt` (`onContinue` → `WakeTime` jusqu'à l'étape 41).
- Tests : `AppSelectionViewModelTest` (brouillon, abandon, validation, recherche insensible à la casse, compteur), `AppSelectionTextsTest`, `AppSelectionScreenTest` (instrumenté : six cases vides puis icônes, « Continuer » grisé).

**Produit :** l'écran 4 et sa feuille Android, la sélection enregistrée seulement à « Valider ».

- [ ] **Écrire `AppSelectionViewModelTest`, `AppSelectionTextsTest`**, implémenter écran, feuille, textes.
- [ ] **Vérifier :** batterie standard ; `grep -rn "AppPicker" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-33.md`.** SPEC_ANDROID §12.1, §15 ; `POINTS-OUVERTS.md` T9 annoté « résolu : `<queries>` ciblé ».

**Tests manuels :** choisir 6 applications, fermer par la croix → « Aucune application choisie » ; recommencer et valider → icônes, « 6 applications choisies », « Modifier » ; rechercher « insta ».

**Terminé quand :** `niumi_app_selection` ne change qu'à « Valider » (test) ; `<queries>` inchangé.

### Étape 34 : écran 10 · Réveil, feuille de scan et échecs

**Specs à lire :** `SPECIFICATION.md` §14 (sans les parties iPhone) et §6 « Échecs du scan » ; maquettes 28 à 33 ; SPEC_ANDROID §10.4, §11.2, §4.4, §19.1 ; `RING/AlarmActivity.kt`, `AlarmNfcScanCoordinator.kt`, `AlarmViewModel.kt`, `RING/ui/AlarmScreen.kt`, `AlarmScreenState.kt` ; `SYS/nfc/ScanOutcome.kt`, `NfcAvailability.kt`.

**Points ouverts touchés :** Q4, Q5, E7, M6.

**Fichiers :**
- `:core:system` — créer `SYS/nfc/ScanSheetState.kt`, `ScanFailureReason.kt`, `ScanOutcomeMapping.kt`.
- `:core:designsystem` — créer `DS/component/NfcScanSheet.kt` (titre « Prêt à scanner », rond d'icône, phrase, bouton « Annuler » gris ; état `Failed` : titre, phrase d'aide, bouton « Réessayer » ou « Ouvrir les réglages », lien de secours facultatif), `DS/drawing/PhoneNearPointDrawing.kt` réutilisé.
- `:feature:ringing` — modifier `RING/ui/AlarmScreen.kt` (date « Vendredi 2 octobre », heure 112, dessin, consigne 30, bouton « Scanner le Niumi Point », lien « Je n'ai pas accès à mon Niumi Point » **masqué jusqu'à l'étape 45**), `AlarmScreenState.kt` (textes `ScanTexts` : consigne, phrase de la feuille « Approche ton téléphone du Niumi Point », quatre échecs mot pour mot, « Déverrouille ton téléphone… » selon Q5), `AlarmUiState.kt` (+ `sheet: ScanSheetState`), `AlarmViewModel.kt` (`openScanSheet()`, `cancelScan()`, `retryScan()`, `onScanOutcome()`), `AlarmActivity.kt` (Reader Mode activé/désactivé avec la feuille, `AlarmNfcScanCoordinator` inchangé), `RING/ui/AlarmTexts.kt` (nouvel objet, remplace les constantes privées).
- Tests : remplacer `AlarmScreenNoStopActionTest` par `AlarmScreenActionsTest` (instrumenté : les seuls nœuds cliquables sont « Scanner le Niumi Point » et, feuille ouverte, « Annuler »/« Réessayer » ; aucun ne dispatche d'événement ; vérifié par un `AlarmViewModel` fake qui enregistre les appels) ; `AlarmViewModelTest` (feuille ouverte → Reader Mode demandé ; échec → `Failed(reason)` ; `RELEASING` → feuille fermée) ; `ScanOutcomeMappingTest` (table `ScanOutcome` × `NfcAvailability` → `ScanFailureReason`) ; `AlarmTextsTest` ; `AlarmScreenStateTest` mis à jour.

**Produit :** l'écran 10 et sa feuille, les quatre échecs, sans aucune action d'arrêt.

- [ ] **Écrire `ScanOutcomeMappingTest`, `AlarmTextsTest`, `AlarmViewModelTest`**, implémenter états, feuille, écran, activité.
- [ ] **Écrire `AlarmScreenActionsTest`**, supprimer `AlarmScreenNoStopActionTest`.
- [ ] **Vérifier :** batterie standard ; `./gradlew :feature:ringing:connectedDebugAndroidTest`.
- [ ] **Valider sur appareil, rédiger `ETAPE-34.md`.** SPEC_ANDROID §10.4 (bouton et feuille ; Reader Mode selon Q4), §11.2 (tableau des échecs réécrit avec les textes du design), §19.1 (nouveau garde-fou), §15.

**Tests manuels :** alarme à l'heure → écran 10 sombre ; « Scanner » → feuille ; tag quelconque → « Ce n'est pas un Niumi Point. » ; tag inconnu → « Ce Niumi Point n'est pas enregistré. » ; NFC coupé → « Le NFC est coupé. » + « Ouvrir les réglages » ; le bon tag → `RELEASING`, feuille fermée ; écran verrouillé selon Q5.

**Terminé quand :** quatre échecs mot pour mot (test) ; Reader Mode inactif feuille fermée (constaté avec Q4) ; aucune action n'arrête l'alarme (test).

### Étape 35 : écran 7 · Écran de blocage

**Specs à lire :** `SPECIFICATION.md` §12 (partie Android) ; maquette 22 ; SPEC_ANDROID §12.2 (overlay, « ne pas démarrer une Activity depuis l'arrière-plan »), §12.3 ; `SESSION/blocking/*` ; `docs/android/play-console/ACCESSIBILITY_DECLARATION.md`, `REVIEW_VIDEO_SCRIPT.md` ; point de vigilance 5.

**Points ouverts touchés :** Q6, T10.

**Fichiers :**
- `:feature:session` — modifier `SESSION/blocking/BlockOverlayController.kt` (nouvelle interface), `WindowManagerBlockOverlayController.kt` (`ComposeView` plein écran, `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE` seul, propriétaires de cycle de vie posés par `setViewTreeLifecycleOwner` etc., `NiumiTheme(dark = true)`, aucune minuterie), créer `BlockScreenContent.kt` (logo, titre, texte, « Fermer », « Ouvrir Niumi »), `BlockScreenTexts.kt` ; modifier `NiumiBlockingAccessibilityService.kt` (`GLOBAL_ACTION_HOME` puis `show()` ; « Fermer » → `hide()` ; « Ouvrir Niumi » → `hide()` puis `startActivity(MainActivity, NEW_TASK)` ; `hide()` sur projection inactive, `onInterrupt`, `onUnbind`) ; supprimer `niumi_block_overlay_text` de `androidApp/feature/session/src/main/res/values/strings.xml`.
- Documentation : `ACCESSIBILITY_DECLARATION.md`, `REVIEW_VIDEO_SCRIPT.md` (nouvel usage déclaré : écran de blocage affiché jusqu'à « Fermer »).
- Tests : `BlockScreenTextsTest` ; `NiumiBlockingAccessibilityServiceSourceTest` (existant, étendu : aucune exception possible, `hide()` dans les trois chemins) ; `BlockOverlayInstrumentedTest` (`show()` puis `hide()` sans fuite de fenêtre).

**Produit :** l'écran 7 sombre, touchable, qui reste affiché jusqu'à « Fermer » ou « Ouvrir Niumi ».

- [ ] **Écrire `BlockScreenTextsTest`**, étendre le test du service, implémenter le contrôleur et le contenu.
- [ ] **Mettre à jour la déclaration Play et le script vidéo.**
- [ ] **Vérifier :** batterie standard ; `./gradlew :feature:session:connectedDebugAndroidTest`.
- [ ] **Valider sur appareil, rédiger `ETAPE-35.md`.** SPEC_ANDROID §12.2, §12.3, §15.

**Tests manuels :** session en cours, ouvrir une application bloquée → retour à l'accueil du téléphone puis écran 7 ; « Fermer » → disparaît ; rouvrir → « Ouvrir Niumi » → accueil Niumi ; scanner → l'écran 7 ne réapparaît plus ; désactiver le service pendant l'affichage → l'écran disparaît.

**Terminé quand :** l'overlay disparaît sur les deux boutons, à la libération et à la déliaison (constaté) ; aucune minuterie dans le code (grep `postDelayed` vide dans `blocking/`).

### Étape 36 : feuille « Sonnerie » et ligne « Sonnerie »

**Specs à lire :** `SPECIFICATION.md` §10 ; maquettes 16, 17 ; SPEC_ANDROID §10.2 (montée), §15 « Lot 7 » ; `SESSION/ringtone/*`, `SYS/audio/VolumeRamp.kt`, `AlarmSoundPreferences.kt`, `AlarmVolumeSource.kt`, `NiumiRingtones.kt` ; `ETAPE-28.md` (mesure du 2026-10-08) ; point de vigilance 4.

**Points ouverts touchés :** Q7, D7, D5, D6, M7.

**Fichiers :**
- `:core:system` — modifier `SYS/audio/VolumeRamp.kt` (`VolumeRampDurations.SECONDS` selon Q7 ; `fun sanitized(seconds: Int?): Int?` → plus proche valeur de la liste), `AlarmSoundPreferences.kt` (lecture assainie), `AlarmVolumeSource.kt` (+ `percent(): Int?` du flux `STREAM_ALARM`).
- `:feature:session` — créer `SESSION/ringtone/RingtoneSheet.kt` (feuille : titre « Sonnerie » + croix ; section « Son » en liste avec bouton rond et bouton d'écoute ; section « Sonnerie progressive » en pastilles ; mention « Volume max en N min » selon D6 ; ligne « Volume du téléphone · 80 % » ; bulle « Il se règle dans les réglages de ton téléphone. » au toucher, disparaît au toucher extérieur) ; modifier `RingtoneViewModel.kt` (« Non » = `null`, pastilles), `RingtoneTexts.kt`, `SESSION/ui/AlarmSoundTexts.kt` (résumé « Progressive · 2 min » ou nom seul selon D5), `AlarmSoundRow.kt` (nom à droite, sous-ligne, chevron) ; supprimer `RingtoneScreen.kt`.
- `:app` — route `Ringtone` supprimée ; la feuille s'ouvre depuis la ligne (écrans 5/6/7 transitoires, puis accueil à l'étape 41).
- Tests : `RingtoneTextsTest`, `RingtoneViewModelTest`, `VolumeRampPolicyTest`, `AlarmSoundSettingsTest` (`sanitized(900)`, `sanitized(60)`), `AlarmSoundTextsTest`.

**Produit :** la feuille Sonnerie des maquettes, les nouvelles durées, le volume affiché sans réglage.

- [ ] **Écrire les tests de durées et de textes**, implémenter `sanitized`, la feuille, les textes.
- [ ] **Vérifier :** batterie standard ; `grep -rn "RingtoneScreen\|NiumiRoute.Ringtone" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-36.md`.** SPEC_ANDROID §3, §10.2, §15 (Lot 7 réécrit) ; `POINTS-OUVERTS.md` Q7 consigné.

**Tests manuels :** choisir « Oiseaux » et 5 min → l'accueil (ou l'écran 5 transitoire) affiche « Oiseaux / Progressive · 5 min » ; « Non » → selon D5/D6 ; toucher la ligne du volume → bulle, toucher ailleurs → disparaît ; session armée avec 900 s avant la mise à jour → sonne avec la durée assainie.

**Terminé quand :** préférence et session reçoivent les durées de la liste ; `sanitized` testé ; la feuille remplace l'écran 14.

## Phase M — Lot 10 : contrat KMP 1.4 (étapes 37 et 38)

**Lot ajouté le 2026-10-10.** Les specs sont mises à jour **avec** ces étapes, comme le Lot 6 l'a fait pour 1.3 : SPEC_CORE_KMP §1, §2, §3.1, §4, §5.1, §5.2, §6, §7.1, §8 (+ §8.4), §9.2, §10, §11.2, §12, §14, §17, §19 ; SPEC_IOS §8, §9, §11, §12, §14, §15 (parité du contrat seulement). Cinq décisions proposées :

1. **Désarmement via `RELEASING` vers `DISARMED`** : la décision commune 8 (« état final seulement après `RELEASING` ») tient, `CANCEL_ALARM` est requis, les réducteurs de libération sont réutilisés. `DISARMED` est distinct de `CANCELLED` : le second éteint l'interrupteur, le premier précède un ré-armement.
2. **`releasedBy` remplace l'invariant « `nfcVerifiedAt` avant `RELEASING` »**.
3. **Preuve opaque de recopie** produite par la façade, sur le modèle de `NfcVerificationProof` ; normalisation et corpus en KMP (écart assumé à §7.3, documenté §12).
4. **Planning calculé en KMP, stocké en natif** : `NextOccurrenceCalculator` est pur ; l'interrupteur et le dépôt sont Android.
5. **Contrôles de préparation non bloquants** (`SPECIFICATION.md` §2 « l'interrupteur s'active toujours ») : `ActivationPolicy` ne reçoit plus de contrôle bloquant ; les manques se signalent par l'alerte et le bandeau (étape 48).

**Ce que chaque étape produit.** 37 : le moteur sait calculer la prochaine occurrence, se désarmer sans scan avant le coucher, et dire comment une session a été libérée. 38 : il reconnaît n'importe quel Niumi Point enregistré, accepte la sortie de secours prouvée, et calcule le temps gagné.

### Étape 37 : planning, prochaine occurrence, désarmement, `releasedBy`

**Specs à lire :** SPEC_CORE_KMP §2 (décisions 1, 4, 11, 15), §4, §5 à §5.2, §6, §7.1, §8, §10, §12, §14, §17 ; SPEC_IOS §8, §9, §11, §15, §16 ; `SPECIFICATION.md` §2 et §9 ; « Contrat KMP 1.4 » ci-dessus ; `KMP/domain/*Reducer.kt`, `ReducerSupport.kt`, `SessionEventValidation.kt`, `KMP/schedule/WakeScheduleCalculator.kt`, `BlockingScheduleCalculator.kt`, `KMP/interop/*` ; points de vigilance 8 et 10.

**Points ouverts touchés :** D17, Q8, Q9.

**Fichiers :**
- `:shared:core` — créer `KMP/schedule/WakePlan.kt`, `NextOccurrenceCalculator.kt`, `NextOccurrenceInput.kt`, `NextOccurrenceResult.kt`, `KMP/domain/ReleaseMeans.kt`, `DisarmReducer.kt` (`onDisarmRequested` : exige `ARMED`, `isBlockingPending`, `occurredAt < triggerAt` ; snapshot `RELEASING` cible `DISARMED`, `releasedBy = PLAN_CHANGE`, `disarmedAtEpochMillis` renseigné à `RELEASE_SUCCEEDED`), `KMP/interop/PlanDtos.kt`, `PlanDtoMappers.kt` ; modifier `SessionState.kt`, `ReleaseTarget.kt`, `SessionEventKind.kt`, `SessionSnapshot.kt` (`SCHEMA_VERSION = 3`, deux champs), `NfcReducer.kt` (`releasedBy = NFC_SCAN`), `ReleaseReducer.kt` (`DISARMED` à `RELEASE_SUCCEEDED`), `SessionEngine.kt`, `SessionEventValidation.kt`, `ReducerSupport.kt`, `ViolationCode.kt`, `KMP/diagnostics/ActivationPolicy.kt` (contrôles ignorés si liste vide ; `hasPairedBox` documenté « au moins un »), `interop/SessionDtos.kt`, `DtoMappers.kt`, `SessionSnapshotEventMappers.kt`, `NiumiCoreFacade.kt` (+ `computeNextOccurrence`), `NiumiCoreVersion.kt` (1.4, schéma 3).
- Fixtures : `shared/core/src/commonTest/resources/fixtures/wake_plans.json` ; snapshot v2 conservé pour la lecture par défaut ; snapshot v3 `DISARMED`.
- Tests : créer `NextOccurrenceCalculatorTest` (L–V 7:00/22:30 à jeudi 20:00 → vendredi 7:00, coucher jeudi 22:30 ; vendredi 23:00 → lundi 7:00, coucher dimanche 22:30 ; coucher 1:00 → jour même ; période en cours → `blockingStartsImmediately` ; aucun jour → `NO_DAY_ENABLED` ; DST printemps/automne comme `WakeScheduleCalculatorTest` ; égalité → `BEDTIME_EQUALS_WAKE`), `SessionEngineDisarmTest` (en attente → `RELEASING`/`DISARMED`, effets ; appliqué → `BLOCKING_ALREADY_APPLIED` ; hors `ARMED` → `INVALID_STATE_TRANSITION` ; `RELEASE_SUCCEEDED` → `DISARMED`, `disarmedAt`), étendre `SessionEngineReleaseTest`, `SessionEngineNfcTest` (`releasedBy`), `SessionEngineForbiddenTransitionsTest` (10 × 14), `DtoRoundTripTest` (v2 → `releasedBy` déduit), `NiumiCoreFacadeTest`, `ActivationPolicyTest` (liste vide autorisée).

**Produit :** un moteur qui calcule l'occurrence suivante d'un planning et se désarme proprement avant le coucher.

- [ ] **Écrire `NextOccurrenceCalculatorTest`**, implémenter le calculateur (délégation à `WakeScheduleCalculator` pour la DST).
- [ ] **Écrire `SessionEngineDisarmTest`**, étendre les tests de libération et de transitions interdites, implémenter `DISARMED`, `releasedBy`, `DisarmReducer`.
- [ ] **Étendre les tests de mapping et de façade**, implémenter DTO, façade, version.
- [ ] **Vérifier :**

```bash
./gradlew :shared:core:jvmTest
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64
./gradlew testDebugUnitTest          # aucun test Android modifié pour passer : les défauts des DTO suffisent
./gradlew ktlintCheck detekt
```

- [ ] **Rédiger `ETAPE-37.md`.** SPEC_CORE_KMP et SPEC_IOS (sections listées) mises à jour.

**Tests manuels :** aucun (module commun). Le framework iOS doit se construire.

**Terminé quand :** chaque ligne ajoutée à §17 a son test ; `SessionEngineForbiddenTransitionsTest` couvre `DISARM_REQUESTED` depuis les dix états ; aucun test Android modifié ; SPEC_IOS §8/§9 alignés.

### Étape 38 : Niumi Points multiples, sortie de secours, temps gagné

**Specs à lire :** SPEC_CORE_KMP §2 (3, 7, 13), §4, §6, §9.2, §10, §12, §14, §17 ; SPEC_IOS §4.6, §14, §15 ; `SPECIFICATION.md` §13, §15, §16 « Niumi Points » ; `KMP/nfc/BoxVerifier.kt`, `NfcVerificationProof.kt`, `ConstantTime.kt` ; « Contrat KMP 1.4 ».

**Points ouverts touchés :** D2, D3, D8, Q10.

**Fichiers :**
- `:shared:core` — créer `KMP/nfc/BoxSetVerifier.kt`, `KMP/emergency/EmergencyExitTexts.kt`, `EmergencyTranscription.kt`, `EmergencyExitProof.kt`, `KMP/domain/EmergencyReducer.kt` (sources et cibles du tableau 1.4 ; `releasedBy = EMERGENCY_EXIT` ; effets identiques à `VALID_NFC_SCANNED`), `KMP/insight/TimeToGetUpCalculator.kt`, `KMP/interop/EmergencyDtos.kt`, `InsightDtos.kt` ; modifier `SessionEventKind.kt`, `SessionEvent.kt` (+ `emergencyProof`), `ViolationCode.kt` (+ `MISSING_EMERGENCY_PROOF`, `NO_PAIRED_BOX`), `SessionEventValidation.kt`, `SessionEngine.kt`, `NiumiCoreFacade.kt` (+ 4 méthodes), `SessionDtos.kt`, `NfcDtos.kt`, `BoxVerificationResult.kt` (+ `NO_PAIRED_BOX`).
- Tests : `BoxSetVerifierTest` (premier concordant ; boxId connu + token faux ; aucun ; liste vide ; temps constant conservé), `EmergencyTranscriptionTest` (texte de 197 caractères ; casse, accents, ponctuation, espaces doubles ignorés ; apostrophe droite tapée au clavier contre apostrophe typographique du modèle → indifférent ; majuscule automatique du clavier en début de phrase → indifférent ; premier mot divergent → `MISMATCH` avec index ; préfixe du dernier mot → `INCOMPLETE` ; preuve seulement sur `COMPLETE` avec contexte ; id inconnu), `SessionEngineEmergencyExitTest` (depuis `ARMED` appliqué → `CANCELLED` ; depuis `RINGING` → `COMPLETED` ; sans preuve → `MISSING_EMERGENCY_PROOF` ; depuis `ARMED` en attente → refus), `TimeToGetUpCalculatorTest` (7:00 → 7:04, 15 déclarées → 4 / 11 ; 3 déclarées → `null` ; arrondi selon Q10), `EmergencyExitTextsTest` (197 caractères exactement, texte mot pour mot), `SessionEngineForbiddenTransitionsTest` étendu.

**Produit :** le moteur accepte n'importe quel Niumi Point enregistré, la sortie de secours prouvée, et sait calculer le temps gagné.

- [ ] **Écrire `BoxSetVerifierTest`**, implémenter.
- [ ] **Écrire `EmergencyTranscriptionTest`, `EmergencyExitTextsTest`, `SessionEngineEmergencyExitTest`**, implémenter textes, normalisation, preuve, réducteur.
- [ ] **Écrire `TimeToGetUpCalculatorTest`**, implémenter ; DTO et façade.
- [ ] **Vérifier :** mêmes commandes qu'à l'étape 37.
- [ ] **Rédiger `ETAPE-38.md`.** SPEC_CORE_KMP §2 réécrit (décisions 3, 7, 11, 13), §12 (scan ou recopie), SPEC_IOS §4.6, §14, §15.

**Terminé quand :** « aucun secours logiciel » n'apparaît plus dans SPEC_CORE_KMP ; chaque ligne §17 a son test ; framework iOS vert.

## Phase N — Lot 11 : persistance et coordination (étapes 39 et 40)

**Lot ajouté le 2026-10-10.** Quatre décisions proposées :

1. **Ensemble courant des Niumi Points** (Q11) : `AlarmSessionEntity.boxId`/`boxTokenSha256Hex` deviennent nullables et ne sont plus écrits ; `HandleValidNfcUseCase` vérifie contre `PairedBoxStore.all()` (après déverrouillage) ou contre la projection Direct Boot (avant).
2. **Projection Direct Boot v4** porte `pairedBoxes`, réécrite à chaque décision et à chaque écriture de `PairedBoxStore`.
3. **Ré-armement natif sous le mutex du coordinateur** (`WakePlanScheduler`), jamais avant déverrouillage ; déclenché par l'interface, par `RELEASE_SUCCEEDED` (`COMPLETED`/`DISARMED`), par le démarrage et le déverrouillage.
4. **Le planning est la source de la carte et du cadran ; la session est la source du verrouillage et du thème.**

### Étape 39 : Room v5, Direct Boot v4, dépôts du planning et des Niumi Points

**Specs à lire :** SPEC_ANDROID §7.2, §7.3, §11.1, §11.3, §16 ; SPEC_CORE_KMP §13 ; `SPECIFICATION.md` §2, §16 ; `DB/entity/PairedBoxEntity.kt`, `dao/PairedBoxDao.kt`, `pairing/*`, `migration/Migrations.kt`, `directboot/*`, `SYS/nfc/HandleValidNfcUseCase.kt`, `SYS/setup/SetupPreferences.kt` (patron de garde) ; points de vigilance 6 et 8.

**Points ouverts touchés :** Q11.

**Fichiers :**
- `:core:database` — modifier `PairedBoxEntity.kt` (+ `roomName: String`, `displayOrder: Int`), `PairedBoxDao.kt` (`all()`, `observe()`, `insert`, `rename`, `delete`, `count`), `pairing/PairedBoxStore.kt` et `RoomPairedBoxStore.kt` (nouvelle interface ; `remove` refuse le dernier), `entity/AlarmSessionEntity.kt` (+ `releasedBy`, `disarmedAtEpochMillis` ; `boxId`, `boxTokenSha256Hex` nullables), `migration/Migrations.kt` (`MIGRATION_4_5` : table temporaire pour la nullabilité, `roomName = "Niumi Point"`, `displayOrder = 0`, `releasedBy = 'NFC_SCAN'` si `nfcVerifiedAtEpochMillis` non nul, `schemaVersion = 3`), `NiumiDatabase.kt` (version 5, `schemas/5.json`), `directboot/DirectBootSnapshot.kt` (v4 : + `releasedBy`, `disarmedAtEpochMillis`, `pairedBoxes`), `DirectBootMapper.kt`, `DirectBootMirror.kt` (+ `refreshPairedBoxes()`), `DirectBootJson.kt` (lecture v3 : `pairedBoxes` = l'ancien couple `boxId`/`token` s'il existe), `AndroidSessionExtras.kt` (sans boîtier), `SessionSnapshotMapper.kt`, `NiumiPoint.kt`, `AddPointResult.kt`, `RemovePointResult.kt`.
- `:core:system` — modifier `SYS/nfc/HandleValidNfcUseCase.kt` (`verifyBoxAmong` sur `LoadResult.Present.pairedBoxes`), `SYS/session/SessionStates.kt` (`SESSION_FINAL_STATES` + `DISARMED`, `isSessionDark` sur `RELEASING` selon la cible) ; créer `SYS/plan/WakePlan.kt`, `WakePlanStore.kt`, `DataStoreWakePlanStore.kt` (`niumi_wake_plan`, garde de déverrouillage identique à `DataStoreSetupPreferences`), `SYS/insight/TimeToGetUpPreferences.kt`, `DataStoreTimeToGetUpPreferences.kt` (`niumi_time_to_get_up`), bindings Hilt.
- Tests : `Migration4To5Test` (instrumenté, `MigrationTestHelper`), `PairedBoxDaoTest`, `RoomPairedBoxStoreTest` (dernier non supprimable, renommage), `DirectBootSnapshotJsonTest` (v3 lisible, v4 aller-retour), `DirectBootRoomParityTest`, `HandleValidNfcUseCaseTest` (deuxième point accepté ; inconnu refusé ; liste vide → `NO_PAIRED_BOX`), `WakePlanStoreTest`, `DataStoreUnlockGuardTest` (les deux nouveaux dépôts), `SessionStatesTest`, `EventFingerprintTest` (témoins schéma 3), `NiumiDatabaseSchemaTest` (v5).

**Produit :** la base et la projection savent stocker plusieurs Niumi Points nommés, le planning et la durée déclarée, et lire les sessions des versions précédentes.

- [ ] **Écrire `Migration4To5Test`, `PairedBoxDaoTest`, `RoomPairedBoxStoreTest`**, implémenter entités, DAO, migration, dépôt.
- [ ] **Écrire les tests Direct Boot et `HandleValidNfcUseCaseTest`**, implémenter la projection v4 et la vérification sur l'ensemble.
- [ ] **Écrire `WakePlanStoreTest` et `DataStoreUnlockGuardTest`**, implémenter les deux dépôts.
- [ ] **Vérifier :** batterie standard ; `./gradlew :core:database:connectedDebugAndroidTest :core:system:connectedDebugAndroidTest`.
- [ ] **Valider sur appareil, rédiger `ETAPE-39.md`.** SPEC_ANDROID §7.2 (v5), §7.3 (v4), §11.1 (plusieurs points, pièce), §11.3.

**Tests manuels :** installer sur une base v4 avec un boîtier et une session armée → migration sans perte, boîtier renommé « Niumi Point » ; scanner un second point enregistré sur écran verrouillé (Direct Boot) → accepté.

**Terminé quand :** `NiumiDatabaseSchemaTest` v5 ; projection v3 lisible ; aucun `Provider<Context>` résolu avant déverrouillage (test).

### Étape 40 : `WakePlanScheduler`, désarmement, sortie de secours et ré-armement dans le coordinateur

**Specs à lire :** SPEC_ANDROID §9.2, §9.3, §11.3, §13.1, §17, §18 ; SPEC_CORE_KMP §6, §10, §12 (1.4) ; `SPECIFICATION.md` §2, §9, §11, §15 ; `SYS/session/DefaultSessionCoordinator.kt`, `SessionEventFactory.kt`, `SessionReconciler.kt`, `SessionStartupReconciler.kt`, `ReconcileResult.kt`, `SESSION/activation/ArmSessionUseCase.kt` ; point de vigilance 8.

**Points ouverts touchés :** Q12, Q14.

**Fichiers :**
- `:core:system` — créer `SYS/plan/WakePlanScheduler.kt`, `DefaultWakePlanScheduler.kt` (sous `SessionCoordinator.withLock` : si `ARMED` en attente → `DISARM_REQUESTED` et attente de `DISARMED` ; si session après le coucher → `LockedBySession` ; puis `computeNextOccurrence` et `ACTIVATION_REQUESTED` avec `checks = emptyList()` ; `enabled = false` → désarmement seul), `PlanChangeReason.kt`, `PlanApplyResult.kt`, `SYS/session/DisarmResult.kt` ; modifier `SessionEventFactory.kt` (+ `disarmRequested`, `emergencyExitConfirmed`), `DefaultSessionCoordinator.kt` (après `RELEASE_SUCCEEDED` : `COMPLETED`/`DISARMED` → `rearmAfterFinalState()` ; `CANCELLED` → `WakePlanStore.write(enabled = false)`), `SessionReconciler.kt`, `SessionStartupReconciler.kt` (au démarrage et à `USER_UNLOCKED` : aucune session active et planning allumé → `apply(PROCESS_START | USER_UNLOCKED)`), `ReconcileResult.kt`, `TechnicalEventType.kt`.
- `:feature:session` — `ArmSessionUseCase` → `SESSION/activation/ArmOccurrenceUseCase.kt` (reçoit une `NextOccurrenceDto`, lit sonnerie et sélection au moment d'armer).
- Tests : `WakePlanSchedulerTest` (interrupteur allumé → armé ; changement d'heure → `DISARMED` puis `ARMED` avec deux `sessionId` ; après coucher → `LockedBySession` ; période en cours → blocage immédiat ; aucun jour → `NoDayEnabled`), `SessionCoordinatorDisarmTest`, `SessionCoordinatorEmergencyExitTest`, `SessionCoordinatorRearmTest` (`COMPLETED` → nouvelle session ; `CANCELLED` → interrupteur éteint, rien d'armé), `SessionReconcilerRearmTest`, `ArmOccurrenceUseCaseTest`, `TechnicalEventTypeTest`.

**Produit :** l'option B prouvée en JVM : le planning arme, désarme et ré-arme sans que l'interface écrive jamais un état de session.

- [ ] **Écrire `WakePlanSchedulerTest`**, implémenter le scheduler.
- [ ] **Écrire les tests du coordinateur**, implémenter le ré-armement et l'extinction.
- [ ] **Écrire `ArmOccurrenceUseCaseTest`**, remplacer `ArmSessionUseCase` (l'écran 5/6 transitoire l'utilise jusqu'à l'étape 41 : adapter `SummaryViewModel` sans le refondre).
- [ ] **Vérifier :** batterie standard ; `./gradlew :core:system:connectedDebugAndroidTest`.
- [ ] **Rédiger `ETAPE-40.md`.** SPEC_ANDROID §9.2 (activation en deux phases depuis le planning), §9.3, §11.3, §17.

**Tests manuels :** aucun à cette étape (l'interface arrive à l'étape 41) ; la séquence est prouvée par les tests du coordinateur.

**Terminé quand :** un changement d'heure avant le coucher laisse en base une session `DISARMED` et une `ARMED` ; `CANCELLED` éteint le planning (tests).

## Phase O — Lot 12 : accueil et fin de session (étapes 41 et 42)

**Lot ajouté le 2026-10-10.** Décisions : l'accueil vit dans `SESSION/home/` ; `NiumiDial` est un composant du design system (fondu ambre par `Brush.sweepGradient` sur l'arc, demi-cosinus sur 34°) ; les poignées imposent 15 minutes d'écart (Q8) ; le point de l'heure actuelle avance à la minute (Q13).

### Étape 41 : écran 6 · Accueil à trois états et cadran

**Specs à lire :** `SPECIFICATION.md` §9, §2, §10 ; `VALEURS-DE-DESIGN.md` §5 ; maquettes 13, 14, 15, 18 (sources pour le cadran) ; SPEC_ANDROID §15 ; `APP/navigation/HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt`, `HomeDestination.kt`, `SESSION/wake/*`, `summary/*`, `active/ActiveSession*`.

**Points ouverts touchés :** D5, D17, M10, Q8, Q13.

**Fichiers :**
- `:core:designsystem` — créer `DS/component/NiumiDial.kt`, `DialState.kt`, `DS/drawing/DialGeometry.kt` (rayon 112, trait 28, 15°/h, minuit en haut ; poignées déplaçables ; arc ambre + fondu en session).
- `:feature:session` — créer `SESSION/home/HomeScreen.kt` (icône réglages, cadran, carte, sept pastilles, ligne « Applications bloquées » avec icônes et « +N », ligne « Sonnerie » ; message bas d'écran), `HomeTexts.kt` (`WAKE_IN`, `WAKE`, `DISABLED`, `WAKE_ENABLED`, `blockingIn(min)`, `sleep(min)`, `WAKE_DISABLED`, `NOTHING_PLANNED`, `BLOCKING_IN_PROGRESS`, `BLOCKED_UNTIL_SCAN`, `CANCEL_SESSION`, `BLOCKED_APPS`, `RINGTONE`, `LOCKED_UNTIL_SCAN`, `SESSION_CANCELLED`, jours « L M M J V S D »), `HomeUiState.kt` (`mode: Scheduled | Disabled | InSession`, `plan`, `countdown`, `blockingIn`, `sleep`, `apps`, `ringtoneSummary`, `banner`, `message`), `HomeViewModel.kt` (observe `WakePlanStore.plan`, `SessionSnapshotPublisher.snapshot`, `AppSelectionStore`, `AlarmSoundPreferences` ; tick à la minute ; chaque modification → `WakePlanScheduler.apply(reason)`), `AppIconsRow.kt` ; supprimer `wake/*`, `summary/*`, `active/ActiveSessionScreen.kt`, `ActiveSessionViewModel.kt`, `ActiveSessionTexts.kt`, `IncidentPresentation.kt`, `ui/WakeScheduleFormatter.kt`, `BlockingScheduleFormatter.kt`, `AlarmSoundRow.kt` et leurs tests (`ScanToModify`, `Cancelled`, `Completed` restent jusqu'à l'étape 42).
- `:app` — supprimer `APP/navigation/HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt` ; modifier `NiumiRoute.kt` (`WakeTime`, `Summary`, `ActiveSession` supprimées), `NiumiNavHost.kt` (`Home` → `SESSION/home`, `onOpenSettings` vers `Settings` à l'étape 44, `onOpenApps` → `AppSelection`), `HomeDestination.kt` (session en cours → `Home` sombre, plus `ActiveSession`), `MainActivity.kt`.
- Tests : `HomeTextsTest`, `HomeViewModelTest` (trois modes ; « 9 h 26 » ; « 56 min » ; « 8 h 30 de sommeil » ; verrouillage en session ; interrupteur → `apply(SWITCH)` ; décocher un jour → `apply(DAYS)` ; activation dans la période → sombre immédiat ; aucun jour selon D17), `DialStateTest` (angles, écart minimal, fondu), `HomeDestinationTest`, `HomeScreenTest` (instrumenté : trois états, cadenas en session).

**Produit :** l'accueil des maquettes, qui pilote le planning et reflète la session.

- [ ] **Écrire `DialStateTest`**, implémenter le cadran.
- [ ] **Écrire `HomeTextsTest`, `HomeViewModelTest`**, implémenter l'accueil ; supprimer les écrans 5, 6, 7 anciens.
- [ ] **Vérifier :** batterie standard ; `grep -rn "WakeTimeScreen\|SummaryScreen\|ActiveSessionScreen" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-41.md`.** SPEC_ANDROID §15 (écran 6 à trois états), §8 (prochaine occurrence), §9.2.

**Tests manuels :** fin de mise en route → « Réveil désactivé » grisé ; interrupteur → carte noire, « Réveil dans … » ; déplacer la poignée du coucher → la carte se met à jour, une session `DISARMED` puis `ARMED` en base ; régler une période déjà en cours → blocage immédiat, accueil sombre ; toucher un jour en session → « Modifiable après avoir scanné ton Niumi Point. » ; deux nuits L–V réelles.

**Terminé quand :** les trois états sont conformes aux maquettes (constaté) ; aucune écriture d'état de session depuis l'interface (grep `SessionCoordinator` dans `home/` vide : seul `WakePlanScheduler` est injecté).

### Étape 42 : annulation par scan et écran 11 · Réussite

**Specs à lire :** `SPECIFICATION.md` §11, §15 ; maquettes 18 à 21, 36 ; SPEC_ANDROID §11.3 ; `SESSION/active/ScanToModify*`, `CancelledScreen.kt`, `CompletedScreen.kt`, `RING/AlarmViewModel.kt` (`AlarmUiState.Exit`), `APP/navigation/DeepLinkDestination.kt`.

**Points ouverts touchés :** D2, D15, Q14.

**Fichiers :**
- `:feature:session` — créer `SESSION/home/CancelSessionSheet.kt` (titre, texte, bouton « Scanner le Niumi Point » → `NfcScanSheet` avec la phrase « Approche ton téléphone du Niumi Point pour annuler la session », lien de secours masqué jusqu'à l'étape 45), `CancelSessionViewModel.kt` (Reader Mode via `NfcReader` + `NfcScanHandler` existants ; succès → fermeture, message « Session annulée… »), `SESSION/success/SuccessScreen.kt`, `SuccessTexts.kt` (`standingAt(time)`, `afterAlarm(min)`, `bravo(min)`, `APPS_BACK`, `BACK_HOME`), `SuccessViewModel.kt` (lit la dernière session `COMPLETED`, `TimeToGetUpPreferences`, `computeTimeToGetUp` ; sans durée déclarée → pas de « Bravo ! »), `DS/drawing/SunriseDrawing.kt` (dégradé §2) ; supprimer `ScanToModify*`, `CancelledScreen.kt`, `CompletedScreen.kt`, `CancelledTexts`, `CompletedTexts` et leurs tests.
- `:feature:ringing` — `AlarmUiState.Exit` → destination `Success` (`COMPLETED`) ou `Home` (`CANCELLED`).
- `:app` — `NiumiRoute.kt` (+ `Success` ; `ScanToModify`, `Cancelled`, `Completed` supprimées), `NiumiNavHost.kt`, `DeepLinkDestination.kt`.
- Tests : `CancelSessionViewModelTest`, `SuccessTextsTest`, `SuccessViewModelTest` (7:04 / 4 min / 11 min ; pas de « Bravo ! » si plus lent ou sans durée), `AlarmUiStateTest`, `DeepLinkDestinationTest`.

**Produit :** l'annulation de session par scan et l'écran de réussite.

- [ ] **Écrire `CancelSessionViewModelTest`**, implémenter la feuille.
- [ ] **Écrire `SuccessTextsTest`, `SuccessViewModelTest`**, implémenter l'écran 11 et les sorties de `AlarmActivity`.
- [ ] **Vérifier :** batterie standard ; `grep -rn "ScanToModify\|CancelledScreen\|CompletedScreen" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-42.md`.** SPEC_ANDROID §11.3, §15.

**Tests manuels :** session en cours → « Annuler la session » → feuille → scan → accueil clair « Réveil désactivé » + message ; matin : scan → écran 11 « Debout à 7:04. », « Retour à l'accueil » → « Réveil prévu » (prochaine occurrence armée) ; sans durée déclarée (avant l'étape 46) → pas de « Bravo ! ».

**Terminé quand :** deux nuits réelles passent par l'écran 11 puis « Réveil prévu » ; l'annulation éteint l'interrupteur (constaté en base).

## Phase P — Lot 13 : Niumi Points et réglages (étapes 43 et 44)

### Étape 43 : écran 3 · Association du Niumi Point en trois temps

**Specs à lire :** `SPECIFICATION.md` §6 ; maquettes 05 à 08 ; SPEC_ANDROID §11.1 ; `SETUP/pairing/*` existant ; étape 34 (`NfcScanSheet`).

**Points ouverts touchés :** D11, M5, M6, E7, Q15.

**Fichiers :**
- `:feature:setup` — créer `SETUP/pointpairing/PointPairingScreen.kt` (logo, flèche, dessin du Niumi Point, consigne, bouton, lien « Je n'ai pas encore de Niumi Point » → URL Q15), `RoomChoiceSheet.kt` (quatre cases avec icône, « Une autre pièce » → champ « Nom de la pièce », clavier, « Continuer » / « Enregistrer » en mode renommage), `PointPairingTexts.kt`, `PointPairingViewModel.kt` (temps 1 → 2 → 3 ; échecs par `NfcScanSheet` avec « Annuler » à la place du lien ; déjà enregistré → `AlreadyPaired(roomName)` affiché ; `PairedBoxStore.add`), icônes `ic_bath`, `ic_kitchen`, `ic_sofa`, `ic_door` ; supprimer `SETUP/pairing/*` et ses tests.
- `:app` — `NiumiRoute.Pairing` → `PointPairing(mode)` ; `NiumiNavHost.kt` (`SETUP` → `TimeToGetUp` à partir de l'étape 46, `AppSelection` avant ; `ADD` → retour à `NiumiPoints`).
- Tests : `PointPairingTextsTest`, `PointPairingViewModelTest` (trois temps, échecs, pièce libre, « Continuer » selon D11), `PointPairingScreenTest`.

**Produit :** l'association et l'ajout d'un Niumi Point nommé.

- [ ] **Écrire les tests**, implémenter écran, feuilles, ViewModel ; supprimer `pairing/*`.
- [ ] **Vérifier :** batterie standard ; `grep -rn "PairingScreen\|PairingTexts" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-43.md`.** SPEC_ANDROID §11.1, §15.

**Tests manuels :** associer deux points (« Salle de bain », « Bureau » saisi) ; scanner un point déjà enregistré ; NFC coupé → texte de l'écran 3.

**Terminé quand :** deux points nommés en base ; le lien ouvre l'URL ; aucun échec silencieux.

### Étape 44 : écran 12 · Réglages (squelette) et liste des Niumi Points

**Specs à lire :** `SPECIFICATION.md` §16 (introduction, Niumi Points) ; maquettes 37, 42, 47 à 54 ; SPEC_ANDROID §15.

**Points ouverts touchés :** textes à relire du §2 de `POINTS-OUVERTS.md` (mentions Niumi Points, « Enregistrer »).

**Fichiers :**
- `:feature:session` — créer `SESSION/settings/SettingsScreen.kt` (quatre lignes : « Niumi Points · 2 », « Autorisations · 3 sur 3 », « Aide », « À propos » ; Aide et À propos inertes jusqu'à l'étape 47), `SettingsTexts.kt`, `SettingsViewModel.kt` (compte des points, `PermissionStatusSource`), `SESSION/niumipoints/NiumiPointsScreen.kt`, `NiumiPointsTexts.kt`, `NiumiPointsViewModel.kt`, `RenameRoomSheet.kt` (réutilise `RoomChoiceSheet` en mode « Enregistrer »), `DeletePointSheet.kt` (titre avec la pièce, texte, « Supprimer », « Annuler » ; « Supprimer » grisé avec « Tu dois garder au moins un Niumi Point. » dans le menu).
- `:app` — routes `Settings`, `NiumiPoints` ; `Permissions` rouverte depuis « Autorisations » ; icône réglages de l'accueil branchée.
- Tests : `SettingsTextsTest`, `SettingsViewModelTest`, `NiumiPointsTextsTest`, `NiumiPointsViewModelTest` (renommer, supprimer, dernier refusé, ajout en session), `FileDirectBootStoreTest` étendu (projection reflète un ajout en session).

**Produit :** les réglages, la liste des Niumi Points avec ajout, renommage et suppression, en clair et en sombre.

- [ ] **Écrire les tests**, implémenter.
- [ ] **Vérifier :** batterie standard.
- [ ] **Valider sur appareil, rédiger `ETAPE-44.md`.** SPEC_ANDROID §15.

**Tests manuels :** réglages en clair puis en session (sombre) ; renommer ; supprimer jusqu'au dernier (refusé) ; ajouter pendant une session puis scanner le nouveau point le matin.

**Terminé quand :** la projection Direct Boot reflète un ajout en session (test) ; les quatre lignes sont conformes aux maquettes.

## Phase Q — Lot 14 : sortie de secours et temps gagné (étapes 45 et 46)

### Étape 45 : écrans 8 et 9 · Sortie de secours

**Specs à lire :** `SPECIFICATION.md` §13 ; maquettes 23 à 27 ; SPEC_CORE_KMP §12 (1.4) ; SPEC_ANDROID §3, §4.5 (à réécrire), §10.2 ; `SYS/audio/AlarmAudioEngine.kt`, `DefaultAlarmAudioEngine.kt`, `VolumeRampPolicy.kt` ; `docs/android/LIMITES.md`, `play-console/PRIVACY_POLICY.md` ; point de vigilance 3.

**Points ouverts touchés :** D2, D8, D20, Q16.

**Fichiers :**
- `:core:system` — modifier `AlarmAudioEngine.kt` (+ `setAttenuated(active: Boolean)`), `DefaultAlarmAudioEngine.kt` (−20 dB selon Q16, la montée continue en dessous), créer `SYS/audio/TypingAttenuation.kt` (remontée 2 s après la dernière touche), `SYS/session/EmergencyExitUseCase.kt` (`verifyEmergencyTranscription` avec contexte → `EMERGENCY_EXIT_CONFIRMED` sous le verrou).
- `:feature:session` — créer `SESSION/emergency/EmergencyWarningSheet.kt` (deux textes selon l'origine ; « Rester dans la session » principal ; « Recopier le texte » au trait), `EmergencyTranscriptionScreen.kt` (« Annuler », compteur « N sur 197 caractères », consigne, texte modèle avec mot surligné, champ sans collage, message d'erreur, bouton « Sortir de la session » au-dessus du clavier), `EmergencyTexts.kt`, `EmergencyExitViewModel.kt` (texte choisi parmi `emergencyExitTexts()`, vérification à chaque frappe, atténuation si origine alarme).
- `:feature:ringing` — `AlarmScreen.kt` et `NfcScanSheet` : lien « Je n'ai pas accès à mon Niumi Point » visible ; `CancelSessionSheet.kt` idem.
- `:app` — route `EmergencyExit(origin)` ; depuis `AlarmActivity`, l'écran 9 s'ouvre dans `AlarmActivity` (l'app doit rester au premier plan).
- Tests : `EmergencyTextsTest`, `EmergencyExitViewModelTest` (« 84 sur 197 caractères » ; premier mot divergent ; bouton actif seulement `COMPLETE` ; « Annuler » perd la saisie ; collage refusé ; rotation conserve la saisie), `TypingAttenuationTest`, `EmergencyExitUseCaseTest`, `AlarmScreenActionsTest` étendu (le lien ne dispatche rien).

**Produit :** la sortie de secours, le soir et le matin, avec l'atténuation pendant la frappe.

- [ ] **Écrire les tests**, implémenter feuilles, écran, use case, atténuation.
- [ ] **Vérifier :** batterie standard ; `./gradlew :feature:ringing:connectedDebugAndroidTest`.
- [ ] **Valider sur appareil, rédiger `ETAPE-45.md`.** SPEC_ANDROID §3, §4.5 (réécrits : la sortie de secours existe, prouvée par la recopie), §10.2 (atténuation), §15 ; `LIMITES.md` et `PRIVACY_POLICY.md` relus (plus de « aucun secours »).

**Tests manuels :** soir : lien → écran 8 → « Rester » ; puis « Recopier » → recopie avec faute → mot signalé → correction → « Sortir » → accueil clair, message ; matin pendant l'alarme : la sonnerie baisse pendant la frappe, remonte à l'arrêt, s'arrête à la validation → écran 11 selon D2.

**Terminé quand :** `releasedBy = EMERGENCY_EXIT` en base après une sortie (constaté) ; aucun texte du dépôt n'affirme plus l'absence de secours.

### Étape 46 : écran 3 bis et rubrique « Temps gagné »

**Specs à lire :** `SPECIFICATION.md` §7, §16 « Aide / Temps gagné », §15 ; maquettes 09, 40, 45 ; étape 39 (`TimeToGetUpPreferences`).

**Points ouverts touchés :** D3, D4, M9.

**Fichiers :**
- `:feature:setup` — créer `SETUP/timetogetup/TimeToGetUpScreen.kt` (roue de onze durées, valeur du milieu en 40/600, mention, « Continuer » en mode `SETUP`, « Enregistrer » en mode `EDIT`), `TimeToGetUpTexts.kt`, `TimeToGetUpViewModel.kt` (écrit `TimeToGetUpPreferences`).
- `:feature:session` — `SuccessViewModel.kt` déjà branché (étape 42) : vérifier que « Bravo ! » apparaît dès qu'une durée est déclarée. La page d'aide « Temps gagné » (carte « Temps pour sortir du lit avant Niumi · 15 min », « Modifier » → roue en mode `EDIT`) arrive avec l'aide à l'étape 47.
- `:app` — route `TimeToGetUp(mode)` ; NavHost `PointPairing(SETUP)` → `TimeToGetUp(SETUP)` → `AppSelection`.
- Tests : `TimeToGetUpTextsTest`, `TimeToGetUpViewModelTest` (onze valeurs, « 1 heure et + » = 60 selon D3, présélection D4, obligatoire, mode `EDIT` enregistre sans naviguer), `SuccessViewModelTest` étendu.

**Produit :** la question obligatoire de la mise en route, et sa valeur utilisée par l'écran 11.

- [ ] **Écrire les tests**, implémenter.
- [ ] **Vérifier :** batterie standard.
- [ ] **Valider sur appareil, rédiger `ETAPE-46.md`.** SPEC_ANDROID §15.

**Tests manuels :** mise en route complète 1 → 2 → 3 → 3 bis → 4 → accueil ; redémarrer → valeur conservée ; matin : « Bravo ! » juste (15 déclarées, levé en 4 → « 11 minutes de moins »), absent si plus lent.

**Terminé quand :** la valeur survit au redémarrage (test de garde) ; l'écran 11 affiche la comparaison seulement si plus rapide.

## Phase R — Lot 15 : réglages complets et hors écrans (étapes 47 et 48)

### Étape 47 : Autorisations depuis les réglages, Aide, À propos, Licences

**Specs à lire :** `SPECIFICATION.md` §16 (Aide, À propos), §5 (réouverture de l'écran 2) ; maquettes 38 à 46 ; `design-plan/police/LISEZ-MOI.md` ; `APP/help/HelpScreen.kt`, `HelpTexts.kt` ; `docs/android/LIMITES.md`.

**Points ouverts touchés :** M2, M3, M4, M8, Q17, Q20.

**Fichiers :**
- `:feature:session` — créer `SESSION/settings/help/HelpScreen.kt` (liste des rubriques existantes : Fonctionnement, Temps gagné ; « Niumi Point perdu » et « Sortie de secours » absentes tant que M2), `HelpTopicScreen.kt`, `HelpTopics.kt` (« Fonctionnement » : phrases des écrans 1 et 11 sous « Le soir », « Le matin », « Après le scan »), `TimeSavedTopicScreen.kt` (texte « Après chaque réveil, Niumi compare… », carte « Temps pour sortir du lit avant Niumi · 15 min », « Modifier » → `TimeToGetUp(EDIT)`, modifiable en session), `SESSION/settings/about/AboutScreen.kt` (logo, « Version {versionName} », trois liens inertes selon M4, lien « Exporter le diagnostic » selon Q20), `AboutTexts.kt`, `LicensesScreen.kt` (« Copyright IBM Corp. » + texte de `res/raw/ofl_ibm_plex_sans.txt`) ; supprimer `APP/help/*`, `HelpTextsTest`, `HelpScreenTest`.
- `:app` — routes `Help`, `HelpTopic(id)`, `About`, `Licenses` ; `Permissions` depuis « Autorisations ».
- Tests : `HelpTopicsTest` (textes mot pour mot), `AboutTextsTest`, `LicensesScreenTest` (instrumenté : le texte de la licence est affiché en entier).

**Produit :** les réglages complets.

- [ ] **Écrire les tests**, implémenter.
- [ ] **Vérifier :** batterie standard ; `grep -rn "HelpTexts" androidApp` vide.
- [ ] **Valider sur appareil, rédiger `ETAPE-47.md`.** SPEC_ANDROID §15 (l'écran 13 disparaît ; `LIMITES.md` reste le document publié, acté).

**Tests manuels :** chaque page en clair et en sombre ; « Autorisations » rouvre l'écran 2 avec les coches ; « Temps gagné » → « Modifier » pendant une session → roue → « Enregistrer » → valeur mise à jour ; Licences lisible.

**Terminé quand :** `LIMITES.md` n'est plus restitué dans l'app (acté dans §15) ; la licence OFL est affichée mot pour mot (test).

### Étape 48 : notification du soir, bandeau de problème, alerte d'autorisations

**Specs à lire :** `SPECIFICATION.md` §17 ; maquettes 55 à 58 ; SPEC_ANDROID §9.1 (dérogations), §10.5, §13.1, §14 ; `SYS/notification/*`, `SYS/readiness/SessionReadinessWatcher.kt` ; `ReleaseHygieneTest` ; point de vigilance 7.

**Points ouverts touchés :** D10, M12, Q18, Q19.

**Fichiers :**
- `:core:system` — créer `SYS/notification/EveningReminderScheduler.kt`, `AndroidEveningReminderScheduler.kt` (`setWindow` selon Q18), `EveningReminderReceiver.kt`, `EveningReminderSpecs.kt` (titre « Tu te réveilles toujours demain à 7:00 ? », texte « Ton heure de coucher est programmée à 22:30, 6 applications seront bloquées. », canal `niumi_evening_reminder`, tap → accueil, aucune action), `SYS/permissions/PermissionAlertPolicy.kt`, `SYS/home/ProblemBannerPolicy.kt` ; modifier `DefaultWakePlanScheduler.kt` (programme/annule le rappel avec la session), `NiumiComponent` (+ `EVENING_REMINDER_RECEIVER`), manifeste.
- `:core:designsystem` — `NiumiProblemBanner.kt`, `NiumiAlertDialog.kt`.
- `:feature:session` — `HomeViewModel.kt` (+ `banner`, `alert`), `HomeScreen.kt` (bandeau en haut, cadran décalé ; alerte centrée), `PermissionAlertTexts.kt`.
- `:app` — `MainActivity.kt` (alerte évaluée à chaque `onStart`, une fois par passage, selon Q19).
- Tests : `EveningReminderSpecsTest` (textes, H−30, annulée avec la session), `PermissionAlertPolicyTest` (une / plusieurs ; conséquences mot pour mot), `ProblemBannerPolicyTest` (NFC avant « aucun point » ; un seul), `ReleaseHygieneTest` (quatrième site `AlarmManager` autorisé), `AndroidEveningReminderInstrumentedTest`.

**Produit :** les trois éléments hors écrans.

- [ ] **Écrire les tests**, implémenter.
- [ ] **Vérifier :** batterie standard ; `./gradlew :core:system:connectedDebugAndroidTest`.
- [ ] **Valider sur appareil, rédiger `ETAPE-48.md`.** SPEC_ANDROID §9.1 (3e dérogation), §13.1 (présentation), §14, §15, §17, §20.

**Tests manuels :** notification à H−30 ; absente si l'interrupteur est éteint ; couper le NFC → bandeau ; retirer le service d'accessibilité, rouvrir l'app → alerte, « Voir les autorisations » → écran 2 ; « Plus tard » → pas de seconde alerte avant le prochain passage.

**Terminé quand :** `ReleaseHygieneTest` vert ; les deux bandeaux et les deux alertes sont conformes (constaté).

## Phase S — Lot 16 : clôture (étapes 49 à 51)

### Étape 49 : balayage des écrans et textes obsolètes

**Fichiers :** supprimer `SESSION/diagnostics/*` (écran 12 ancien), `DiagnosticExporter` selon Q20 (déplacé vers À propos sinon), `SESSION/incident/IncidentTexts.kt` (libellés repris par la notification §13.1 si encore utilisés : vérifier), route `IncidentDiagnostic`, `APP/navigation/DeepLinkDestination.kt` (→ `Permissions`), `docs/android/QA_MATRIX.md` (scénarios des écrans supprimés retirés). Tests : `NiumiRouteTest`, `DeepLinkDestinationTest`, `SmokeTest`.

- [ ] **Supprimer, adapter, vérifier :** batterie standard, puis :

```bash
grep -rn "boîtier\|boitier" androidApp --include=*.kt | grep -v "boxId\|BoxPayload\|PairedBox\|niumi://box"   # attendu : rien
grep -rln "IncidentDiagnostic\|ActiveSession\|WakeTime\|Summary\|ScanToModify\|Readiness\|AppPicker\|Onboarding" androidApp --include=*.kt   # attendu : rien
```

- [ ] **Rédiger `ETAPE-49.md`.**

**Terminé quand :** les deux greps sont vides ; `docs/android/RESTE_A_FAIRE.md` section D mise à jour.

### Étape 50 : charte V3 et specs finales

**Fichiers :** réécrire `docs/CHARTE_GRAPHIQUE_APP_MOBILE.md` en V3 à partir de `VALEURS-DE-DESIGN.md` (palette clair/sombre, IBM Plex Sans, règle de l'ambre, clair hors session / sombre en session, formes, cadran, contrastes ; sections objet/photo/site conservées seulement si Mehdi le demande) ; SPEC_ANDROID §15 (numérotation du design-plan avec table des anciens numéros, renvois internes corrigés : lignes citant « écran 7 », « écran 8 », etc.), §1, §2, §3, §4.5, §11.1, §12.2, §13, §22 (Lots 8 à 16), §21 (critères réécrits) ; SPEC_CORE_KMP relecture d'ensemble 1.4 ; SPEC_IOS parité des sections KMP ; `docs/android/LIMITES.md` v2 ; `play-console/*` ; `POINTS-OUVERTS.md` annoté des décisions prises (sans supprimer les questions ouvertes). `CLAUDE.md` n'est pas touché.

- [ ] **Réécrire, relire, vérifier** que chaque amendement KMP a sa ligne SPEC_IOS ; **rédiger `ETAPE-50.md`.**

**Terminé quand :** aucune section de spec ne décrit un écran supprimé ; la charte V3 ne contredit plus `VALEURS-DE-DESIGN.md`.

### Étape 51 : validation sur appareil et rapport de release

**Préalable :** appareil Android branché, débogage USB ; `connectedDebugAndroidTest` **avant** le protocole manuel (il désinstalle l'app et ses données).

| Scénario | Résultat attendu |
| --- | --- |
| Mise en route complète 1 → 2 → 3 (deux points) → 3 bis → 4 → accueil | « Réveil désactivé », valeurs par défaut |
| Interrupteur, deux nuits L–V | Rappel du soir à H−30 ; blocage au coucher ; écran 7 sur une app bloquée ; écran 10 ; scan ; écran 11 ; « Réveil prévu » ré-armé |
| Modification d'heure avant le coucher | `DISARMED` puis `ARMED` en base ; aucune notification en double |
| Activation dans la période | Blocage immédiat, accueil sombre |
| Annulation par scan | Clair, « Réveil désactivé », message |
| Sortie de secours soir et matin | Atténuation pendant la frappe ; `releasedBy = EMERGENCY_EXIT` |
| Ajout d'un Niumi Point en session, scan sur écran verrouillé | Accepté |
| Redémarrage avant le coucher, sans déverrouillage jusqu'après le coucher | Blocage appliqué en retard, `MISSED_BLOCKING_START_WINDOW` |
| Retrait du service d'accessibilité | Alerte à l'ouverture ; notification §13.1 |
| NFC coupé | Bandeau ; échec sur l'écran 10 |
| TalkBack sur accueil, écran 10, écran 9 | Rôles et états annoncés |

- [ ] **Dérouler, consigner modèle et version Android, rédiger `ETAPE-51.md`**, mettre à jour `QA_MATRIX.md`, `RELEASE_REPORT.md`, `RESTE_A_FAIRE.md`.

**Terminé quand :** chaque case des étapes 29 à 50 est « prouvée » ou listée comme restante.

## Recette et critères d'acceptation

Une case cochée signifie « prouvé », jamais « implémenté ».

- [ ] Les 58 maquettes ont un écran ou un état correspondant, en clair et en sombre selon `INDEX.md` (étapes 29 à 48).
- [ ] Aucune couleur, taille ou forme en dur hors `:core:designsystem` (grep, étape 29 et suivantes).
- [ ] Le thème suit l'état de la session, jamais le téléphone (étape 29, constaté).
- [ ] Un planning allumé arme la prochaine occurrence ; une modification avant le coucher ne demande pas de scan ; après le coucher, seul le scan ou la sortie de secours libère (étapes 37, 40, 41, 51).
- [ ] N'importe quel Niumi Point enregistré arrête l'alarme, y compris ajouté pendant la session ; il en reste toujours au moins un (étapes 38, 39, 44, 51).
- [ ] La sortie de secours exige la recopie exacte du texte de 197 caractères et compte comme un scan (étapes 38, 45, 51).
- [ ] L'écran 11 affiche le temps gagné seulement s'il est positif (étapes 42, 46).
- [ ] Les specs décrivent l'app livrée ; aucun écran supprimé n'y survit ; SPEC_IOS reflète le contrat 1.4 (étape 50).
- [ ] Chaque point de `POINTS-OUVERTS.md` touché par une étape est soit tranché (annoté), soit laissé de côté et signalé (toutes étapes).

## Hypothèses et limites du plan

- Le plan suppose l'étape 28 close et le dépôt propre avant l'étape 29.
- Les fichiers de la police viennent de `github.com/IBM/plex` ; leur licence exacte est celle de l'archive téléchargée.
- Les images du guide des autorisations (M1), les rubriques d'aide (M2), les mentions légales et la politique (M4) et les autres textes de secours (D8) ne sont pas produits par ce plan : les emplacements existent, les contenus viendront de Mehdi.
- L'app iOS n'est pas développée ; SPEC_IOS n'est tenue à jour que pour le contrat KMP.
- Les contraintes Google Play (overlay touchable, service d'accessibilité) sont documentées mais non présumées acceptées : porte de validation à l'étape 51 et avant toute soumission.
- Versions de bibliothèques inchangées ; aucune montée prévue, sauf si un build l'impose (à signaler, jamais silencieusement).
- Aucun commit automatique ; aucune ligne `Co-Authored-By`.
