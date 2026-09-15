# Rapport de release — MVP Android

Les vingt-sept critères d'acceptation de SPEC_ANDROID §21, un par un, avec leur preuve.

**Règle.** Un critère n'est coché qu'avec une preuve nommée : un test qui le vérifie, un essai
consigné dans un rapport d'étape, ou une ligne de `QA_MATRIX.md`. Un critère dont la preuve
n'existe pas reste **ouvert**, même si le code paraît correct — c'est la différence entre un
comportement implémenté et un comportement prouvé. Aucun critère n'est coché par défaut.

État à la date de ce rapport : **916 exécutions de tests JVM** (878 distincts, les 38 tests de
`:app` étant rejoués sur les variantes debug et release), **141 tests instrumentés** au dernier
passage sur appareil, `ktlintCheck`, `detekt`, `:app:lintRelease` et `:app:assembleRelease` verts.

## Critères prouvés

| # | Critère §21 | Preuve |
| --- | --- | --- |
| 1 | Une session ne peut être confirmée que si le diagnostic est vert | `ActivationPolicy` (`:shared:core`), `ReadinessViewModelTest`, `SummaryViewModelTest` ; parcours suivi sur appareil aux étapes 14 et 17 |
| 2 | `setAlarmClock()` est la seule API de réveil | `ReleaseHygieneTest.theOnlyAlarmSchedulingApisAreTheTwoAllowedByTheSpec` : les seuls appels de programmation dans tout le code de production sont `setAlarmClock` dans `AndroidAlarmScheduler` et `setExactAndAllowWhileIdle` dans `AndroidRingingWatchdog`, cette dernière étant la dérogation explicite de §9.1 pour l'alarme de secours de `RINGING`. Le test échoue sur toute autre API, y compris `AlarmManager.set()` |
| 5 | La sonnerie continue après fermeture de l'activité | Essai sur appareil, `ETAPE-03.md` ; `QA_MATRIX.md` |
| 6 | Aucun bouton logiciel ne termine la session | `AlarmScreenNoStopActionTest` (instrumenté), `HelpScreenTest.theHelpScreenOffersNoAction`, et l'absence de toute action d'arrêt vérifiée à chaque étape |
| 7 | Seul un tag accepté par le parseur et le vérificateur KMP, avec sa preuve opaque, produit `VALID_NFC_SCANNED` | `commonTest` de `:shared:core` (scan valide, invalide, mal formé, surdimensionné, query non canonique, token absent, dupliqué, paddé, trop court, mal encodé) ; `HandleValidNfcUseCaseTest` |
| 8 | Un scan valide depuis `ARMED` produit `RELEASING` vers `CANCELLED` | `SessionEngineNfcTest`, `SessionCoordinatorReleaseTest` ; essai sur appareil `ETAPE-18.md` |
| 9 | Un scan valide depuis `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` produit `RELEASING` vers `COMPLETED` | Idem, plus l'essai `TRIGGERED_AWAITING_NFC` de `ETAPE-19.md` |
| 10 | `COMPLETED` ou `CANCELLED` n'est écrit qu'après `RELEASE_SUCCEEDED` | `commonTest` de la machine à états ; `PhaseCompletionTest` |
| 11 | Une session active ne passe jamais à `FAILED` sur incident technique | `commonTest` ; neuf essais de l'étape 20, aucune session perdue |
| 12 | Un incident postérieur à l'activation met à jour la santé selon sa gravité | `commonTest`, `SessionReadinessMonitorTest`, `RuntimeStatusGapsTest` ; essais 1 et 6 de `ETAPE-20.md` |
| 13 | Un tag invalide ne modifie ni l'état, ni le blocage, ni le son | `commonTest` ; `ScanToModifyViewModelTest` |
| 14 | La fin valide arrête le son et débloque en moins d'une seconde | Essai sur appareil, `ETAPE-04.md` et `ETAPE-20.md` (essai 7) |
| 15 | Le blocage renvoie chaque application sélectionnée à l'accueil sans toucher les autres | `tools/validate_blocking.sh`, huit essais vérifiés par `dumpsys`, `ETAPE-05.md` |
| 16 | Une sélection de plus de 50 applications est refusée | `commonTest` (0, 1, 50 et 51 applications) ; `AppPickerViewModelTest` |
| 17 | Un redémarrage restaure l'alarme avant le premier déverrouillage | **Mesuré à l'étape 19**, trois essais, permission OEM de démarrage automatique refusée. Repris ici sans être refait |
| 18 | `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC` affichent toujours une notification de scan, sans son ni vibration ni plein écran, y compris avant le premier déverrouillage ; retirée par un scan valide | **Mesuré à l'étape 19**, essais 5 à 7. Repris ici sans être refait |
| 19 | Un changement d'heure ou de fuseau réenregistre le même instant | `commonTest` (heure d'été, heure d'hiver, changement de fuseau) ; essais sur appareil `ETAPE-19.md` |
| 20 | L'application ne lit aucun contenu de fenêtre via l'accessibilité | `NiumiBlockingAccessibilityServiceSourceTest` : le source du service ne contient ni `rootInActiveWindow`, ni `getText`, ni `contentDescription`, ni `AccessibilityNodeInfo`. Déclaration Play correspondante dans `ACCESSIBILITY_DECLARATION.md` |
| 21 | Le parcours critique ne réalise aucun appel réseau | `ReleaseHygieneTest.theReleaseManifestDeclaresExactlyTheNinePermissionsOfTheSpec` : le manifeste **fusionné release** déclare exactement les neuf permissions de §14, donc pas `INTERNET`. Lu en DOM, ce qu'un commentaire ne peut pas tromper. Aucune dépendance réseau au classpath |
| 23 | Lint, ktlint et detekt passent sans erreur | `./gradlew ktlintCheck detekt :app:lintRelease` vert. `lintRelease` est exécuté pour la première fois à l'étape 21, avec `abortOnError` et `warningsAsErrors` |
| 24 | Les limites de l'arrêt forcé, du FGS et du NFC verrouillé sont documentées dans l'application | Écran 13 « Aide et limites » (étape 21), `docs/android/LIMITES.md`, `HelpTextsTest` qui verrouille la correspondance entre les deux. Les six limites de l'onboarding y sont reprises mot pour mot |
| 25 | L'absence de mécanisme de secours est expliquée avant la première activation | `OnboardingTexts.limits`, quatrième limite ; `OnboardingScreenTest` |

## Critères partiellement prouvés

| # | Critère §21 | Ce qui est prouvé | Ce qui manque |
| --- | --- | --- | --- |
| 3 | L'alarme sonne hors ligne, écran éteint, sur la matrice P0 | Sur Xiaomi / Android 16 : écran éteint 31 min, Doze profond réel, silencieux, volumes à zéro, Bluetooth — tous verts | **La matrice P0 n'est pas couverte.** Un seul fabricant, une seule version d'Android. Voir `QA_MATRIX.md` |
| 22 | Tous les tests unitaires et instrumentés passent | 916 exécutions JVM vertes ; 141 instrumentés verts au 2026-09-15 | Les instrumentés n'ont pas été rejoués depuis l'ajout de `HelpScreenTest` (étape 21) : il faut une passe sur appareil |
| 26 | Les scénarios DND, Bluetooth, USB-C et route audio sont consignés sur la matrice P0 | DND (trois modes) et Bluetooth A2DP mesurés | USB-C, casque filaire et changement de route pendant `RINGING` : matériel non disponible |

## Critères ouverts

| # | Critère §21 | État |
| --- | --- | --- |
| 4 | Sur Android 17, l'alarme utilise `USAGE_ALARM` et reste audible en arrière-plan | **Ouvert.** Aucun appareil Android 17. `USAGE_ALARM` est vérifié sur Android 16 (`dumpsys audio`), mais le comportement d'arrière-plan d'Android 17 ne l'est pas |
| 27 | Le dossier Play de l'AccessibilityService est prêt **et** a été soumis sur une piste interne ou fermée, réponse de Google traitée | **Ouvert — porte 0b.** Le dossier est rédigé depuis l'étape 6 (six documents). La vidéo n'est pas tournée, l'application n'est pas soumise, Google n'a pas statué. C'est le risque principal assumé par la décision du 2026-09-07 (`LOT-0.md`) : les étapes 7 à 21 ont été développées avant le verdict |

## Écarts techniques encore ouverts

Repris des étapes 19 et 20 plutôt que présentés comme résolus.

1. **La matrice §20 hors Xiaomi.** Aucun Pixel, aucun Samsung, aucune autre surcouche. L'étape 5 a
   montré qu'une surcouche peut rendre le blocage silencieusement inopérant ; rien ne permet
   d'extrapoler. C'est la dette la plus lourde du MVP.
2. **Le journal technique écrit avant le premier déverrouillage.** Versé dans Room au
   déverrouillage depuis l'étape 20, mais un processus qui journalise avant et meurt avant d'y
   arriver perd ses entrées. Limite assumée et documentée (§17, `LIMITES.md`). L'incident métier
   correspondant, lui, n'est jamais perdu : il atteint Room par le rejeu de l'outbox.
3. **`MY_PACKAGE_REPLACED` dépend de la permission OEM de démarrage automatique.** Refusée — état
   par défaut — le receveur ne tourne pas après une mise à jour de l'APK. L'alarme survit tout de
   même, Android préservant le `PendingIntent`. À reconsidérer si une surcouche efface aussi les
   alarmes lors d'un remplacement d'APK.
4. **Le seau d'App Standby n'a jamais pu être rétrogradé sous `EXEMPTED`.** Le système a refusé
   `am set-standby-bucket restricted`. Un appareil qui placerait Niumi en `RARE` ou `RESTRICTED`
   pendant que le watchdog compte n'a donc pas été observé. Argument de proportion, pas preuve : le
   watchdog ne tourne que dans la fenêtre qui suit un service de premier plan et un écran de réveil.
5. **L'arrêt du seul service de premier plan n'est pas reproductible sur HyperOS.** Le gestionnaire
   des services actifs d'AOSP y est absent. À rejouer sur un Pixel.
6. **Les trois scénarios « activation refusée » de §20** (notifications, plein écran, accessibilité)
   n'ont jamais été rejoués à la main depuis que `DeviceReadinessChecker` existe.
7. **Aucun essai sur un artefact de publication.** R8 et la suppression de ressources n'ont jamais
   tourné sur un appareil. Le risque nommé : la sérialisation du snapshot Direct Boot. Les règles
   consommateur de kotlinx-serialization sont appliquées et les sérialiseurs portent les noms de
   champs dans leur descripteur, donc l'obfuscation ne devrait pas changer le JSON — **devrait**,
   et c'est exactement pourquoi la ligne reste ouverte.

## Préconditions de publication à la charge de l'utilisateur

| Précondition | État |
| --- | --- |
| Keystore d'upload (hors dépôt, `keystore.properties` non versionné) | à créer |
| Identité de l'éditeur, e-mail de contact public | `<À COMPLÉTER>` dans `PRIVACY_POLICY.md` |
| URL d'hébergement de la politique de confidentialité | `<À COMPLÉTER>` |
| Compte développeur Google Play créé et vérifié | à faire |
| 12 testeurs opt-in pendant 14 jours consécutifs (compte personnel) | non démarré |
| Vidéo de revue tournée selon `REVIEW_VIDEO_SCRIPT.md`, sur l'application complète | non tournée |
| Soumission sur piste interne ou fermée, réponse de Google consignée dans `LOT-0.md` | non soumise |

## Suite

Toutes les tâches qui restent entre ce rapport et une publication sont rassemblées, avec leur mode
opératoire et leur critère de réussite, dans **`docs/android/RESTE_A_FAIRE.md`**.

## Conclusion

Le code du MVP est complet et ses règles métier sont prouvées par les tests. **Le MVP n'est pas
acceptable au sens de §21** : deux critères sont ouverts, trois ne sont que partiellement prouvés,
et la porte finale exige une matrice §20 verte dans le périmètre de §4.1 plus le verdict de Google
sur l'AccessibilityService. Aucun des deux n'est acquis.
