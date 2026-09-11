# Étape 12b — onboarding, écran de diagnostic, navigation typée et accueil

**Date :** 2026-09-11
**Périmètre :** `:feature:setup` (onboarding, diagnostic), `:app` (navigation typée, accueil).
**Specs de référence :** SPEC_ANDROID §3 (dernier point), §4.2 à §4.5, §6, §10.4, §12.3, §13,
§13.1, §14, §15 ; SPEC_CORE_KMP §5, §7.4, §10, §14.

## Résumé

La passe 12a avait livré le moteur de diagnostic, mais rien de tout cela n'était atteignable :
l'accueil affichait « Aucune session » et une route de debug, et aucun écran n'appelait
`DeviceReadinessChecker`. Cette passe livre le premier parcours réel — accueil, onboarding,
diagnostic — et la navigation typée `@Serializable` qui portera les étapes 13 à 17.

`NiumiCoreFacade.evaluateActivation` a désormais un appelant de production.

## Décisions validées avec l'utilisateur (2026-09-11)

1. **Routes typées `@Serializable` centralisées dans `:app`**, en remplacement du mécanisme
   `NavGraphContributor` ; le contributeur ne survit que pour la route POC de debug.
2. **Tests d'écran Compose en `androidTest`**, objets purs et ViewModels en JVM. Pas de Robolectric.
3. **Redirection `ActiveSession` : fonction pure seule.** `homeDestinationFor` est livrée, testée
   et consommée par `HomeViewModel`, mais le NavHost ne navigue pas vers `ActiveSession` :
   l'écran 7 arrive à l'étape 15. Aucune session ne pouvant être armée avant l'étape 14, le cas
   ne peut pas se produire d'ici là. L'alternative — livrer un écran de session minimal dès 12b —
   a été écartée pour ne pas empiéter sur l'étape 15.
4. **Onboarding en page unique défilante**, et non un pager : tout le contenu passe devant
   l'utilisateur dans l'ordre, y compris sous TalkBack, et la case reste l'unique passage.

## Ce qui est livré

### `:feature:setup` — onboarding (écran 2, partie limites)

`onboarding/` : `OnboardingTexts` (objet pur), `OnboardingUiState`, `OnboardingScreen` +
`OnboardingRoute`, `OnboardingViewModel`. Motif d'`AccessibilityConsentScreen` repris tel quel.

**Six limites**, et non quatre comme l'annonçait le plan : §4.2 (arrêt forcé), §4.4 (scan sur
écran verrouillé), §4.3 (désactivation du service d'accessibilité), §3 et §4.5 (absence de tout
secours logiciel), plus deux que le plan omettait — §13 (une mise à jour peut réinitialiser
l'exemption d'énergie) et §13.1 (l'avertissement est émis au plus tôt, jamais garanti immédiat).
Ces deux dernières sont des limites produit au même titre que les autres ; les taire aurait laissé
l'utilisateur croire à une surveillance continue.

L'accusé de réception n'est persisté qu'au clic sur « Continuer », jamais au clic sur la case :
cocher puis quitter l'écran ne vaut pas lecture.

### `:feature:setup` — écran de diagnostic (écran 2, partie contrôles)

`readiness/` : `ReadinessMessages`, `ReadinessUiState`, `ReadinessViewModel`,
`ReadinessActionIntents`, `ReadinessScreen` + `ReadinessRoute`.

Le ViewModel ne décide rien : il rejoue `DeviceReadinessChecker`, convertit par
`toActivationPolicyInput()` et laisse `NiumiCoreFacade.evaluateActivation` trancher. Aucune règle
d'activation n'est réimplémentée côté Android.

`ReadinessUiState.isDeviceReady` distingue « cet appareil est prêt » de « cette session peut être
armée » : un diagnostic lancé avant le choix de l'heure voit tous ses contrôles passer et se voit
malgré tout refuser l'activation par `TRIGGER_NOT_IN_FUTURE`, ce qui est le verdict exact
(§13, point 2). Sans cette distinction l'écran aurait affiché un refus sans cause visible.

### `:app` — navigation typée et accueil (écran 1)

`navigation/` : `NiumiRoute` (13 destinations `@Serializable`), `HomeDestination`,
`HomeUiState`, `HomeViewModel`, `HomeScreen` + `HomeRoute`, `NiumiNavHost`. `NiumiNavHost.kt` et
`HomeScreen.kt` sont **déplacés** depuis `com.niumi.app.ui`, qui est supprimé, et réécrits.

`MainActivity` appelle `SessionReadinessWatcher.evaluateAsync()` sur `ON_RESUME` : c'est le
déclencheur « à chaque passage de l'application au premier plan » de §13.1, resté en suspens à
l'étape 12a.

## Écarts au plan, et pourquoi

1. **`SetupNavigation.kt` dans `:feature:setup` : impossible.** Les routes typées vivent dans
   `:app` et un module `feature` ne peut pas dépendre de `:app` (§6). Les écrans exposent des
   lambdas (`onContinue`, `onNavigate`) et `:app` les câble. La décision « routes typées
   centralisées dans `:app` » et l'exigence d'un `SetupNavigation.kt` côté feature étaient
   contradictoires ; la première l'emporte.

2. **Deux dépendances annoncées se sont révélées inutiles.** `navigation.compose` (le module n'a
   pas de `NavHost`) et `datastore.preferences` (`SetupPreferences` est une interface de
   `:core:system` injectée par Hilt). Seule `activity.compose` est ajoutée à `:feature:setup`,
   pour `rememberLauncherForActivityResult`. Aucune dépendance externe nouvelle n'entre au
   catalogue.

3. **`ReadinessActionIntents` ajouté, non prévu au plan.** §13 interdit à `:core:system` de
   construire des `Intent` ; il fallait bien un endroit pour le faire. Trois choix y sont fixés et
   documentés dans la spec :
   - **Ne pas déranger** → `ACTION_ZEN_MODE_PRIORITY_SETTINGS`. Le SDK (vérifié dans
     `android-36/android.jar`) n'expose aucune action publique ouvrant l'interrupteur lui-même.
   - **Exemption d'énergie** → `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` tant que la liste
     blanche AOSP manque, fiche de l'application une fois acquise. Le plan disait « demande
     d'exemption AOSP », ce qui aurait signifié `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` :
     cette action exige la permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, restreinte par
     Google Play, que Niumi ne déclare pas et dont il n'a pas besoin puisque c'est l'utilisateur
     qui confirme (§13). L'écart est en faveur de la spec.
   - **Alarmes exactes** → aucune redirection. Un test instrumenté énumère les quatorze actions et
     prouve qu'aucune ne produit `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

4. **Les neuf messages manquants de §13 ont été rédigés et ajoutés à la spec.** §13 n'en donnait
   que cinq pour quatorze contrôles alors que le plan exigeait « les messages de §13 mot pour
   mot ». Les cinq existants sont repris à l'identique, les neuf autres figurent désormais dans
   §13 sous « Messages (étape 12b) ».

5. **Un recours sans destination laisse le bouton inactif.** `StartPairing`, `OpenAppPicker`,
   `FixTime` et `Unsupported` n'ont pas d'écran à cette étape. Le blocage est énoncé, le bouton
   est désactivé : promettre une destination absente serait un faux état de fiabilité (§15). Le
   jeu `UNAVAILABLE_ACTIONS` se vide aux étapes 13 et 14 ; il ne doit jamais servir à masquer un
   contrôle.

6. **Le contrôle d'énergie procède en deux temps sous une seule action.** §13 exige à la fois une
   confirmation utilisateur et « une seule action principale à la fois ». Deux boutons côte à côte
   auraient violé la seconde règle : l'action est donc « Lever les restrictions » (ouvre le
   réglage), puis « J'ai levé les restrictions » au retour. L'état de la bascule est en mémoire :
   la question est reposée à chaque visite de l'écran.

7. **Exemption detekt `CyclomaticComplexMethod: ignoreSingleWhenExpression`.**
   `ReadinessMessages.forCheck` est un `when` exhaustif de quatorze branches, une par contrôle :
   sa complexité mesurée (15) ne compte que les valeurs de l'énumération, il n'y a aucune logique
   ramifiée à suivre. Le remplacer par une `Map` aurait fait perdre l'exhaustivité vérifiée à la
   compilation, qui est la propriété de sûreté recherchée. L'exemption retenue est
   `ignoreSingleWhenExpression` et non `ignoreSimpleWhenEntries` : elle ne pardonne rien à une
   fonction qui mêlerait un `when` à d'autres branchements.

## Vérifications (2026-09-11)

| Commande | Résultat |
| --- | --- |
| `:feature:setup:testDebugUnitTest` | **37 tests verts** (8 à l'étape 5, 29 nouveaux) |
| `:app:testDebugUnitTest` | **11 tests verts** (6 avant, 5 nouveaux) |
| `:core:system:testDebugUnitTest` | 134 verts, non-régression |
| `:shared:core:jvmTest` (160), `:core:database:` (90), `:feature:ringing:` (21), `:feature:session:` (1) | verts, non-régression |
| `:app:assembleDebug` | vert (graphe Hilt fermé avec les trois nouveaux ViewModels) |
| `:feature:setup:compileDebugAndroidTestKotlin`, `:core:system:` | verts |
| `ktlintCheck`, `detekt`, `:app:lintDebug` | verts |

**Observation non liée à 12b.** `detekt` signale « There were 2 compiler errors found during
analysis » sur `:core:database`. Vérifié en remisant l'intégralité des changements de 12b :
l'avertissement préexiste et ne vient pas de cette passe. Il n'échoue pas le build et n'a pas été
poursuivi ici pour ne pas élargir le périmètre ; à traiter séparément.

## Tests ajoutés

| Test | Emplacement | Ce qu'il prouve |
| --- | --- | --- |
| `OnboardingTextsTest` (9) | JVM | les six limites mot pour mot, cardinalité exacte, aucun texte vide |
| `ReadinessMessagesTest` (9) | JVM | un message distinct et non vide pour chacun des 14 `ReadinessCheckId`, les 5 de §13 à l'identique |
| `ReadinessViewModelTest` (11) | JVM | premier blocage en action principale ; `WARNING` seul → activation permise via la vraie façade ; `NOT_APPLICABLE` non affiché ; heure absente → refus sans contrôle en échec ; bascule du contrôle d'énergie ; recalcul sur `refresh()` |
| `HomeDestinationTest` (5) | JVM | tout état non final → `ActiveSession`, exhaustivité prouvée sur `SessionStateDto.entries` |
| `OnboardingScreenTest` (3) | `androidTest` | les six limites affichées ; bouton inactif sans case cochée |
| `ReadinessScreenTest` (5) | `androidTest` | un seul nœud cliquable ; `contentDescription` présente ; recours sans destination désactivé ; explication à la place d'un réglage pour les alarmes exactes |
| `ReadinessActionIntentsTest` (5) | `androidTest` | aucune action ne mène à « Alarmes et rappels » ni à l'exemption restreinte ; extras du canal ; recours internes sans `Intent` |

## Validation sur appareil réel (2026-09-11)

**Appareil :** Xiaomi 25080RABDG, Android 16 (API 36) — le même qu'aux étapes 10 et 11.

### Tests instrumentés

| Module | Résultat |
| --- | --- |
| `:feature:setup` | **17/17 verts**, dont 14 nouveaux (3 onboarding, 6 diagnostic, 5 traduction en `Intent`) |
| `:core:system` | **10/10 verts**, dont les 6 de la passe 12a, jamais passés jusqu'ici |
| `:core:database` | 37/37 verts, non-régression |
| `:feature:ringing` | 5/5 verts, non-régression |

### Défaut trouvé et corrigé sur appareil

**La liste des contrôles affichait le message de remédiation à côté d'un ✓.** « ✓ Le volume des
alarmes est à zéro » pour un volume de 12, « ✓ Cet appareil n'a pas de puce NFC » sur un appareil
qui en a une : l'écran énonçait l'inverse de la vérité, exactement ce que §15 interdit. Aucun test
JVM ni instrumenté ne pouvait l'attraper — tous vérifiaient qu'un texte donné s'affichait, aucun
que le texte affiché **corresponde à l'issue du contrôle**.

Corrigé par `ReadinessMessages.labelFor` (le nom du contrôle) et `ReadinessItem.summary`, qui
choisit le libellé quand le contrôle passe et le message quand il échoue. Trois tests de
régression ajoutés, dont un instrumenté qui vérifie l'absence du message de panne pour un contrôle
satisfait.

### Protocole manuel de §13

| Essai | Attendu | Observé |
| --- | --- | --- |
| Écran d'accueil | « Aucune session » + « Préparer mon réveil » | conforme, orientation libre (paysage et portrait) |
| Onboarding | les 6 limites, bouton inactif sans la case | conforme ; le bouton s'active au cochage |
| Notifications refusées puis accordées | blocage, puis contrôle satisfait | ✗ → ✓ au retour au premier plan |
| Plein écran retiré (Android 14+) | blocage avec raccourci réglages | ✗ avec le message de §13 ; restauré → ✓ |
| Volume d'alarme à zéro | blocage | **non reproductible par le curseur sur cet appareil** (voir ci-dessous) ; observé via le silence total |
| Ne pas déranger « alarmes seules » | avertissement, pas de blocage | `!` sur le contrôle d'avertissement, silence total et volume restés ✓ |
| Silence total | blocage | ✗ sur le silence total **et** sur le volume, `dumpsys audio` confirmant `Muted: true` / `streamVolume:0` |
| Accessibilité désactivée puis activée | blocage, puis contrôle satisfait | ✗ → ✓ au retour |
| Retour des réglages | recalcul sans relancer l'application | conforme sur les six bascules ci-dessus, par `ON_RESUME` |

Aucun crash ni exception Niumi dans `logcat` pendant la session de test.

### Découverte : le volume d'alarme ne peut pas descendre à zéro sur cet appareil

`dumpsys audio` donne `STREAM_ALARM: Min: 1 w/o perm:4`, et `cmd media_session volume --stream 4
--set 0` est refusé avec « invalid volume index 0 for stream 4 (should be in [1..15]) ». Le
contrôle « volume alarme supérieur à zéro » de §13 n'est donc **pas atteignable par le curseur de
volume** sur HyperOS / Android 16 : il ne devient faux que lorsque le système force lui-même le
flux à zéro, ce que fait le silence total (mesure de l'étape 6, reconfirmée ici).

Le contrôle reste utile et correctement implémenté — il attrape le cas où le système mute le flux,
et d'autres surcouches peuvent autoriser l'index 0 — mais il ne faut pas en attendre un blocage
déclenché par l'utilisateur baissant son volume sur cet appareil. À vérifier sur Pixel et Samsung
pendant la campagne de bêta-test.

### Surveillance de §13.1

`dumpsys activity broadcasts` montre le receveur vivant dans le processus Niumi :
`BroadcastFilter{… 10460/u0 ReceiverList{… com.niumi.app/10460/u0}}` sur
`android.app.action.INTERRUPTION_FILTER_CHANGED`. Le reste de §13.1 — l'émission effective d'un
incident et d'une notification d'avertissement — n'est pas observable avant l'étape 14, faute de
parcours d'activation permettant d'armer une session.

### Observation : plantage ponctuel de lint

`:app:lintDebug` a échoué une fois sur `LintDriver.handleDetectorError` /
`UElementVisitor.visitFile`, dans une invocation Gradle groupant tests, assemblage, detekt et lint.
Trois exécutions suivantes — deux isolées et une reprenant la commande groupée à l'identique — sont
vertes. Le plantage n'a pas été reproduit et sa cause n'est pas établie ; il est consigné ici
plutôt que passé sous silence. À resurveiller.

## Points restants

- **Le contrôle « boîtier associé » échoue nécessairement** jusqu'à l'étape 13 :
  `PairedBoxStore` n'a qu'une liaison debug et `EmptyAppSelectionSource` renvoie 0. L'écran de
  diagnostic affiche donc « Aucun boîtier n'est associé » comme premier blocage, avec un bouton
  inactif. Vérifié sur appareil : c'est l'état exact du produit, pas un défaut.
- **La confirmation de l'exemption d'énergie n'est pas atteignable à cette étape.** L'écran
  n'attache un bouton qu'au premier contrôle en échec (§13) ; « applications choisies » précède
  « batterie optimisée » dans le tableau, et échoue tant que le sélecteur n'existe pas. La bascule
  en deux temps est couverte par les tests JVM mais **n'a pas pu être observée sur appareil** ;
  elle le deviendra à l'étape 13.
- **Surveillance de §13.1 non observable en bout de chaîne** avant l'étape 14 : le receveur est
  vivant, mais aucune session ne peut être armée pour produire un incident et une notification.
- **Le tap des notifications d'avertissement de §13.1** ouvre `MainActivity`, qui mène à
  l'accueil : la route `IncidentDiagnostic` est déclarée mais son écran arrive à l'étape 16.
- **Déclencheur « au déclenchement, avant la sonnerie »** de §13.1 : étape 17 (`AlarmReceiver`).
- **Volume d'alarme à zéro par le curseur** : à revérifier sur Pixel et Samsung (bêta-test).
