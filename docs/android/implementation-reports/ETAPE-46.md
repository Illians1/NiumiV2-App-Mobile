# Étape 46 — Annulation par scan et écran 11 · Réussite

Date : 2026-10-25 et 2026-10-26. **Code, tests et essais manuels faits ; restent les deux nuits
réelles et le passage TalkBack.** Tests JVM, tests instrumentés des sept modules, ktlint, detekt,
lint et assemblage verts ; essais manuels 1 à 5 conformes sur Xiaomi 25080RABDG, Android 16
(HyperOS). Quatorzième étape du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md`
(Lot 14, phase Q). Point de départ : `b624d49`. Rien n'est committé.

## Ce qui est livré

- **Feuille « Annuler le réveil ? »** (`:feature:session/home`), ouverte par le lien de l'accueil en
  session. Elle remplace l'écran de scan requis et l'écran « Session annulée ».
  - Question, puis feuille de scan avec « Approche ton téléphone du Niumi Spot pour annuler le
    réveil » ; mêmes échecs, même vibration et même lecture « Prêt à scanner » seulement qu'à
    l'écran de réveil.
  - Après le scan : accueil clair « Réveil désactivé », interrupteur éteint, bulle « Réveil annulé.
    Tes applications sont débloquées. » pendant 4 secondes.
  - Pendant la libération, cercle de chargement seul, lu « Annulation du réveil ».
- **Écran 11 · Réussite** (`:feature:session/success`). Il remplace l'écran « Session terminée ».
  - Soleil levant, « Debout à 7:04. », délai depuis la sonnerie réelle, « Bravo ! » seulement après
    un scan plus rapide que la durée déclarée (jamais avant l'étape 50), « Retour à l'accueil ».
  - Toujours clair, barres système comprises.
- **Suivi de la session par identifiant.** Après le scan du matin, le crochet du planning arme la
  nuit suivante dans la même passe : le publisher montrait la nouvelle session avant que l'écran de
  réveil voie la fin, et l'écran 11 ne s'ouvrait jamais (constat de l'étape 44).
  - L'écran de réveil et la feuille relisent leur session dans Room par son identifiant
    (`SessionHistoryReader`, `RoomSessionHistoryStore`).
  - Route `Success(sessionId)` et extra `EXTRA_SESSION_ID` : la route change chaque matin, la
    navigation repart même si `MainActivity` est restée ouverte.
- **Partages.** Textes d'échec du scan dans `ScanFailureTexts` (`:core:system`) ; phrase et
  contenu « libération » de `NfcScanSheet` (`:core:designsystem`) ; dessin `SunriseDrawing`.
- **Supprimés.** `ScanToModify*`, `CancelledScreen`, `CompletedScreen`, leurs textes et leur test ;
  routes `ScanToModify`, `Completed`, `Cancelled` ; extra `session_cancelled`.

## Décisions de Mehdi

Avant le code (2026-10-25) :

1. **Délais de l'écran 11** accordés (« Moins d'une minute », « 1 minute », « 4 minutes »), puis
   « 1 h 15 » dès 60 min ; même règle pour « Bravo ! ».
2. **Sans sonnerie** (téléphone éteint à l'heure), ni délai ni « Bravo ! » ; une sonnerie en retard
   compte depuis la sonnerie réelle.
3. **Bulle d'annulation** identique à celle des champs verrouillés, sans la coche ni le gras de la
   maquette 27 (M18).

Pendant la session (2026-10-26) :

4. **Les quatre défauts mineurs de la relecture** corrigés tout de suite ; pour la libération,
   cercle seul, lu « Annulation du réveil ».
5. **Titres des feuilles en 22** partout (question, « Prêt à scanner », échecs ; 24 et 26 dans les
   maquettes) ; titre et texte de la question centrés (M19). Vaut aussi pour l'écran de réveil.

## Écarts au plan

1. **`SessionHistoryStore` séparé** de `RoomSessionStore`, au plafond `TooManyFunctions`.
2. **`Success(sessionId)`** au lieu de « lit la dernière session `COMPLETED` ».
3. **`AlarmExitDestination`** devient `Success(sessionId)` ou `Home` : l'annulation n'a plus d'écran.
4. **`SuccessRoute(sessionId)`** reçoit l'identifiant en paramètre : `:feature:session` ne connaît
   pas `NiumiRoute`.
5. **« Debout à »** dans le fuseau courant du téléphone.
6. **Feuille alignée sur l'écran de réveil** : un tag étranger vibre aussi ; « Annuler » de la
   feuille de scan ramène à la question ; pendant la libération, fermer ferme tout.
7. **`CancelSessionActions.kt`** s'ajoute aux fichiers prévus.

## Relecture indépendante

Relecture du code par un agent distinct (lecture seule) : 0 critique, 3 importants, 5 mineurs.
Tous corrigés, chacun avec un test vu en échec, sauf exception notée :

- rappels du lecteur NFC qui modifiaient l'état hors du fil principal ;
- base Room illisible qui faisait planter l'app (mineur remonté en important : le projet ne plante
  jamais sur une base illisible, SPEC_ANDROID §18) ;
- écran 11 rejoué après une recréation de `MainActivity` (`readsLaunchDeepLink`) ;
- barres système sombres sur l'écran 11 en période de 24 h (`NiumiLightWindowWhileShown`, test
  instrumenté vu en échec sur appareil sans la correction) ;
- relecture périmée après une réouverture de la feuille ;
- feuille restaurée après une mort du processus qui pouvait suivre la nuit suivante, pas encore en
  blocage, et l'annuler ;
- aucun retour visuel pendant la libération ;
- NFC non relu après l'inscription du receveur d'état (sans test automatique : câblage du cycle de
  vie, comme la correction de l'étape 38 sur l'écran de réveil).

## Défauts trouvés sur appareil

1. **Bulle « Réveil annulé » absente** (essai 1). Une vidéo de l'écran a montré l'écran de réveil
   intercalé pendant la libération : `requiresAlarmScreen` ouvrait l'écran de réveil pour toute
   libération, annulation comprise. Corrigé : `RELEASING` vers `CANCELLED` ne l'ouvre plus
   (SPEC_ANDROID §10.4 amendée ; `SPECIFICATION.md` §11 prime).
2. **Quatre `MainActivity` empilées** dans la tâche, défaut présent depuis l'étape 17 : chaque
   sortie de l'écran de réveil en ouvrait une nouvelle. Corrigé : `NEW_TASK`, `CLEAR_TOP` et
   `SINGLE_TOP` (`AlarmExitIntent`) ; un seul exemplaire constaté ensuite.

## Tests

- **JVM** : KMP 306, Android 1 369 et `:app` en release 31, aucun échec.
  - Nouveaux : `UnlockAwareSessionHistoryReaderTest`, `TrackedSessionEndTest`,
    `ScanFailureTextsTest`, `CancelSessionTextsTest`, `CancelSessionViewModelTest`,
    `SuccessTextsTest`, `SuccessViewModelTest`, `AlarmExitIntentTest`.
  - Étendus : `HomeViewModelTest`, `AlarmUiStateTest`, `AlarmViewModelTest`,
    `DeepLinkDestinationTest`, `LauncherDestinationTest`, `NiumiTypeTest`.
  - Supprimé : `ScanToModifyViewModelTest` (repris dans `CancelSessionViewModelTest`).
- **Instrumentés** (Xiaomi 25080RABDG, Android 16), avant les corrections faites sur appareil :
  `:core:database` 99, `:core:designsystem` 68, `:core:system` 36, `:feature:session` 39,
  `:feature:ringing` 14, `:feature:setup` 56, `:app` 17, tous verts. Nouveaux :
  `RoomSessionHistoryStoreTest`, `CancelSessionSheetTest`, `SuccessScreenTest`,
  `SuccessScreenWindowTest` ; `NfcScanSheetTest` étendu. Les corrections faites ensuite (règle
  d'ouverture, drapeaux, titres en 22) sont couvertes par les tests JVM ; les tests instrumentés
  n'ont pas été relancés depuis, mais compilent.
- **ktlint, detekt, `:app:lintDebug`, `:app:assembleDebug`** : verts.

## Validation sur appareil

Faite le 2026-10-26 sur Xiaomi 25080RABDG, Android 16, HyperOS. Essais conformes, selon Mehdi et,
pour l'essai 1, d'après deux enregistrements de l'écran :

1. annulation par scan : feuille, scan, accueil clair « Réveil désactivé », interrupteur éteint à
   l'écran, bulle d'environ 4 s, applications débloquées (après correction des deux défauts
   ci-dessus) ;
2. échecs et fermetures : voile, « Annuler » qui ramène à la question, retour, tag étranger avec
   vibration, « Réessayer », NFC coupé puis rallumé feuille ouverte ;
3. écran 11 après le scan du matin, Niumi au premier plan, puis « Réveil prévu » le lendemain ;
   titres en 22 constatés ;
4. deuxième écran 11 dans le même processus, Niumi en arrière-plan, réveil ponctuel, puis
   « Réveil désactivé » ;
5. recréation de l'activité (réglage du téléphone) et réouverture depuis les récents : l'écran 11
   ne réapparaît pas.

Le cercle de libération n'a pas été vu : la libération a duré moins de 300 ms. Interrupteur éteint
constaté à l'écran, puis après fermeture de Niumi depuis les récents et réouverture (« Réveil
désactivé » relu du planning enregistré, vérifié par Mehdi) ; le DataStore n'a pas été lu par adb.

**Restent non validés :**

- deux nuits réelles par l'écran 11 puis « Réveil prévu » (critère « Terminé quand » du plan) ;
- période de 24 h en Quotidien sur l'écran 11 (barres système) : prouvé par
  `SuccessScreenWindowTest`, pas constaté à l'œil ;
- passage TalkBack sur la feuille d'annulation et l'écran 11 (étape 55) ;
- cercle de libération quand la libération dure (échec partiel) ;
- restes des étapes 44 et 45 (`TIMEZONE_CHANGED` réel, réarmement au déverrouillage après
  redémarrage, deux nuits L–V).

## Specs et documents modifiés

- `design-plan/` : SPECIFICATION §11, §15 ; POINTS-OUVERTS (en-tête, §2, M18, M19) ;
  VALEURS-DE-DESIGN §3 ; `valeurs-de-design.json` (`titreFeuilleScan`, note de `titreFeuille`).
- SPEC_ANDROID §10.4, §11.2, §11.3, §15.
- Plan de refonte : décisions (étape 46), interfaces transverses, écarts et cases de l'étape 46.
- Signalé, non appliqué : les maquettes 25 à 27 régénérées le 2026-10-25 disent encore « session »
  (E14) ; la maquette 27 garde sa coche (M18).

## Après l'étape 46 : titre « Jours de réveil » (2026-10-26)

Hors plan, à la demande de Mehdi : les pastilles de l'accueil ne disaient pas si l'on cochait le
soir du coucher ou le matin du réveil. La règle ne change pas (jour coché = jour de réveil,
§8.4). Une ligne « Jours de réveil » remplace les onglets centrés : titre à gauche, onglets
« Ponctuel » / « Quotidien » à droite. Après un premier essai, Mehdi a demandé sur croquis une
capsule grise : nouveau composant `NiumiSegmentedTabs`, segment choisi à l'encre (choix de
Claude) ; `NiumiTextTabs` ne sert plus qu'au sélecteur d'heure. La carte ne change pas. Texte et
disposition non dessinés (M20).

- Tests : `HomeTextsTest.theDayChipsAreTitledAsWakeDays` ; `HomeScreenTest` vérifie le titre
  comme intitulé. Instrumentés lancés le 2026-10-26 sur Xiaomi 25080RABDG, Android 16 :
  `:feature:session` 39 tests, `:core:designsystem` 71 tests (dont trois pour les onglets
  segmentés), aucun échec.
- Constaté à l'œil sur le même appareil, état « Réveil désactivé » en Ponctuel : titre à gauche,
  capsule à droite, « Ponctuel » en segment gris, pastilles dessous, rien ne se chevauche. L'état
  « Réveil prévu », segment encre, n'a pas été vu : il aurait fallu allumer le vrai réveil.
- Specs : `design-plan/` SPECIFICATION §9, VALEURS-DE-DESIGN §4, POINTS-OUVERTS (§2, M20),
  journal ; SPEC_ANDROID écran 6.
- Restent à valider sur appareil : avis de Mehdi sur le visuel des onglets à droite (M20),
  couleurs en « Réveil désactivé » et en session, lecture TalkBack, police agrandie.
