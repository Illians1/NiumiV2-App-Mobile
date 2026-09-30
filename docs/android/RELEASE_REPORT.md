# Rapport de release — MVP Android

Les trente et un critères d'acceptation de SPEC_ANDROID §21, un par un, avec leur preuve. Les quatre
critères du blocage différé (Lot 6, 26 à 29) ont été insérés dans §21 le 2026-09-15 entre l'ancien 25
et l'ancien 26 : la numérotation suit l'ordre de §21 depuis l'étape 25, et les anciens 26 et 27
sont devenus 30 et 31.

**Règle.** Un critère n'est coché qu'avec une preuve nommée : un test qui le vérifie, un essai
consigné dans un rapport d'étape, ou une ligne de `QA_MATRIX.md`. Un critère dont la preuve
n'existe pas reste **ouvert**, même si le code paraît correct — c'est la différence entre un
comportement implémenté et un comportement prouvé. Aucun critère n'est coché par défaut.

État au 2026-09-28 (étape 25) : **1 126 exécutions de tests JVM** (dont les tests de `:app` rejoués
sur les variantes debug et release), **151 tests instrumentés** au dernier passage sur appareil
(28/09, un ignoré par hypothèse), `ktlintCheck`, `detekt`, `:app:lintRelease`,
`:app:assembleRelease` et `:app:bundleRelease` verts. Les chiffres de l'étape 21 étaient 916 et 141.

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
| 22 | Tous les tests unitaires et instrumentés passent | Passe finale de l'étape 25, le 2026-09-28, après la dernière modification du code : **1 126 exécutions JVM** et **151 tests instrumentés** verts (un ignoré par hypothèse, `AndroidAccessibilityServiceStatusInstrumentedTest`, sans service activé à comparer), `HelpScreenTest` compris |
| 23 | Lint, ktlint et detekt passent sans erreur | `./gradlew ktlintCheck detekt :app:lintRelease` vert. `lintRelease` est exécuté pour la première fois à l'étape 21, avec `abortOnError` et `warningsAsErrors` |
| 24 | Les limites de l'arrêt forcé, du FGS et du NFC verrouillé sont documentées dans l'application | Écran 13 « Aide et limites » (étape 21), `docs/android/LIMITES.md`, `HelpTextsTest` qui verrouille la correspondance entre les deux. Les six limites de l'onboarding y sont reprises mot pour mot |
| 25 | L'absence de mécanisme de secours est expliquée avant la première activation | `OnboardingTexts.limits`, quatrième limite ; `OnboardingScreenTest` |
| 26 | Un blocage différé n'applique rien avant son instant de début, l'applique à cet instant à moins d'une minute près quand Niumi est en vie, et en retard avec `MISSED_BLOCKING_START_WINDOW` sinon (Lot 6) | **Avant l'heure** : aucun retour à l'accueil ni overlay, `tools/validate_blocking.sh --deferred` (2026-09-24) et `ETAPE-24.md`. **À l'heure** : neuf débuts de blocage mesurés de +43 à +465 ms (17, 24, 25 et 27 septembre), dont Doze profond forcé (+77 ms), téléphone verrouillé après redémarrage (+465 ms), après changement d'heure (+162 ms) — `QA_MATRIX.md`, section « Blocage différé ». **En retard** : téléphone éteint pendant l'heure de début, blocage appliqué au redémarrage avec `MISSED_BLOCKING_START_WINDOW` (+25 min 55 s, 2026-09-28). Tests : `SessionEngineBlockingStartTest`, `SessionReconcilerBlockingStartTest`, `ProcessDeathInstrumentedTest` (deux cas Lot 6). Réserve : un processus tué **délie** le service d'accessibilité sur HyperOS — hors de « Niumi en vie », limite établie, voir les écarts 8 et 9 |
| 27 | Un redémarrage avant l'heure de début reprogramme l'alarme de début avant le premier déverrouillage (Lot 6) | Mesuré le 2026-09-24 (reprogrammée 1 s après le démarrage, même instant, avant déverrouillage) et le 2026-09-27 (trois redémarrages ; téléphone resté verrouillé à l'heure de début : blocage appliqué pendant la phase verrouillée). `SessionReconcilerBootTest`, `tools/validate_blocking_start.sh reboot` |
| 28 | Aucune session n'atteint `RINGING`, `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` sans blocage demandé (Lot 6) | Par construction du moteur : `ALARM_FIRED`, `ALARM_SOUND_STOPPED` et `TRIGGER_ELAPSED` appliquent eux-mêmes un blocage encore en attente (`SessionEngineTriggerTest`, repli de SPEC_CORE_KMP §5.1). Sur appareil (2026-09-28) : début manqué téléphone éteint, blocage appliqué à la réconciliation **avant** le réveil, puis sonnerie à l'heure. Le repli lui-même — début et réveil tous deux dépassés au même moment — n'a pas été provoqué sur appareil |
| 29 | L'annulation d'une session avant le début de son blocage exige le scan du boîtier, et l'écran 5 refuse un début non strictement antérieur au réveil (Lot 6) | Scan avant l'heure → `CANCELLED` et plus aucune alarme Niumi (2026-09-17) ; 08:00 et un 15:00 devenu postérieur refusés sur l'écran 5, « Continuer » inactif, dans les deux conventions horaires. `ETAPE-24.md` ; `WakeTimeViewModelTest`, `SessionEngineNfcTest` |

## Critères partiellement prouvés

| # | Critère §21 | Ce qui est prouvé | Ce qui manque |
| --- | --- | --- | --- |
| 3 | L'alarme sonne hors ligne, écran éteint, sur la matrice P0 | Sur Xiaomi / Android 16 : écran éteint 31 min, Doze profond réel, silencieux, volumes à zéro, Bluetooth — tous verts | **La matrice P0 n'est pas couverte.** Un seul fabricant, une seule version d'Android. Voir `QA_MATRIX.md` |
| 30 | Les scénarios DND, Bluetooth, USB-C et route audio sont consignés sur la matrice P0 | DND (trois modes) et Bluetooth A2DP mesurés | USB-C, casque filaire et changement de route pendant `RINGING` : matériel non disponible |

## Critères ouverts

| # | Critère §21 | État |
| --- | --- | --- |
| 4 | Sur Android 17, l'alarme utilise `USAGE_ALARM` et reste audible en arrière-plan | **Ouvert.** Aucun appareil Android 17. `USAGE_ALARM` est vérifié sur Android 16 (`dumpsys audio`), mais le comportement d'arrière-plan d'Android 17 ne l'est pas |
| 31 | Le dossier Play de l'AccessibilityService est prêt **et** a été soumis sur une piste interne ou fermée, réponse de Google traitée | **Ouvert — porte 0b.** Le dossier est rédigé depuis l'étape 6 (six documents). La vidéo n'est pas tournée, l'application n'est pas soumise, Google n'a pas statué. C'est le risque principal assumé par la décision du 2026-09-07 (`LOT-0.md`) : les étapes 7 à 21 ont été développées avant le verdict |

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

8. **Retirer Niumi des récents coupait le blocage sur HyperOS** (mesuré les 27 et 28/09). Le
   système tue le processus malgré le service d'accessibilité lié, puis ne relie plus ce service
   tant que l'utilisateur ne l'a pas réactivé ; l'alarme est conservée. **Traité le 28/09** : la
   carte de Niumi est retirée des récents pendant la session (plus rien à balayer), et le
   diagnostic exige, sur HyperOS, le verrou de Niumi dans les récents, seule protection mesurée
   contre « Tout effacer » (survie à 14:37 et après redémarrage). Ce qui reste ouvert : le verrou
   appartient à l'utilisateur, qui peut le retirer hors session (le diagnostic bloque alors
   l'activation suivante) ; le contrôle est sans objet hors HyperOS, où le comportement des récents
   n'est pas mesuré (B1, B2).
9. **Même mécanisme pour toute mort du processus** (essai 3, 2026-09-25) : alarme de début
   conservée, blocage suspendu jusqu'à la réactivation du service. C'est ce qui sépare le critère 26
   de la ligne §20 « processus tué avant l'heure de début ». Sans parade connue : Android ne laisse pas une
   application relier son propre service d'accessibilité. **Limite écrite dans l'aide le 30/09**
   (`LIMITES.md`, puce « si Niumi est arrêté pendant une session ») et ligne §20 requalifiée en
   limite consignée.
10. **L'écran 7 ne se rafraîchit pas quand un réglage change depuis le volet rapide** : il rejoue le
    diagnostic à la reprise (`ON_RESUME`), pas au retour du focus. **Traité et mesuré le 29/09** :
    les écrans 2, 6, 7 et 12 rejouent le diagnostic au retour du focus de la fenêtre et sur chaque état
    stable annoncé du NFC, dont l'allumage (~1,4 s) finissait après le retour du focus ; l'écran 7 y
    relance la surveillance de §13.1 (SPEC_ANDROID §13, §13.1). L'écran 12, qui ne rejouait ses
    contrôles qu'à son ouverture, a reçu le même traitement. Cinq essais conformes sur le Xiaomi
    (ligne « volet rapide » de `QA_MATRIX.md`). Le panneau de volume ne prend pas le focus : un
    volume changé depuis lui n'est vu qu'au retour sur l'écran — sans effet ici, le volume d'alarme
    étant borné à 1 (question produit ouverte, `RESTE_A_FAIRE.md`).
11. **`tools/validate_alarm.sh` portait deux défauts** relevés à l'étape 25 : il cherchait
    l'alarme sous `com.niumi.system.alarm.AlarmReceiver`, alors que le receveur est
    `com.niumi.feature.ringing.AlarmReceiver`, et il attendait `when=<epoch>`, que `dumpsys alarm`
    n'affiche pas. **Corrigé le 30/09** : tag du receveur rectifié, bloc d'alarme et
    lecture de l'epoch brut de l'en-tête `Alarm{… origWhen …}` repris de `validate_blocking.sh` et
    `validate_blocking_start.sh`. **Rejoué le 30/09** sur Xiaomi 25080RABDG / Android 16 : alarme
    trouvée (`origWhen` 12:55:00), service de sonnerie détecté +1 s (sondage ± 2 s), lecteur
    `USAGE_ALARM` actif, flux non muté, volume 9, `AlarmActivity` au premier plan, son audible à
    l'oreille ; scan du boîtier, plus aucune alarme Niumi en attente. Le script ne retient que
    l'alarme en attente, plus les statistiques du dump. Reste perfectible : la ligne « notification »
    du relevé capte le résumé de groupe d'Android au lieu de la notification de sonnerie, et le code
    de sortie est toujours 0.

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

Le code du MVP est complet et ses règles métier sont prouvées par les tests. Les quatre critères du
blocage différé (26 à 29) sont prouvés sur un appareil, avec la réserve de l'écart 9. **Le MVP n'est
pas acceptable au sens de §21** : deux critères sont ouverts, deux ne sont que partiellement prouvés,
et la porte finale exige une matrice §20 verte dans le périmètre de §4.1 plus le verdict de Google
sur l'AccessibilityService. Aucun des deux n'est acquis.
