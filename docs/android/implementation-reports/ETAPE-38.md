# Étape 38 — Écran 10 · Réveil, feuille de scan et échecs

Date : 2026-10-22. **Étape terminée** : code, tests JVM et instrumentés, documents, essais manuels
conformes sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0.WPPEUXM, navigation à
trois boutons). Sixième étape du plan de refonte `docs/superpowers/plans/2026-10-10-refonte-design.md`
(Lot 11, phase N). Point de départ : `20f9d10` (étape 37).

## Ce qui est livré

- **Écran 10 · Réveil** (`:feature:ringing`, numéroté 8 dans SPEC_ANDROID §15 jusqu'à l'étape 54),
  toujours sombre, d'après les maquettes 28 à 33, avec les écarts décidés pendant les essais.
  - Date « Jeudi 22 octobre » (« 1er » le premier du mois), 24 dp sous la marge du haut.
  - Heure actuelle en 112, sur une ligne (réduite si le texte agrandi la ferait déborder), relue au
    début de chaque minute et à chaque retour au premier plan.
  - Dessin du téléphone approché du Niumi Point (variante fidèle à la maquette 28).
  - Consigne **« Rejoins ton Niumi Point »** et un seul bouton, « Scanner le Niumi Point ».
  - Place du lien « Je n'ai pas accès à mon Niumi Point » réservée sous le bouton jusqu'à
    l'étape 49 : l'écran ne bougera pas à son arrivée.
  - Sur un écran trop court, le haut défile et le bouton reste visible.
- **Feuille de scan** (`NfcScanSheet`, `:core:designsystem`), dessinée par Niumi, sans poignée.
  - « Prêt à scanner » à l'encre, rond de l'icône, « Approche ton téléphone du Niumi Point »,
    « Annuler ».
  - Un échec remplace son contenu sans la refermer : titre annoncé par TalkBack, phrase d'aide,
    « Réessayer » ou « Ouvrir les réglages ». Fenêtre nommée « Échec du scan ».
  - Voile, glissement et retour ferment la feuille, échec compris.
  - Composant neutre en textes d'échec : les écrans 3 et d'annulation le reprendront (46, 47).
- **Lecture NFC seulement feuille « Prêt à scanner » ouverte** (Q4). Une feuille d'échec n'écoute
  pas ; « Réessayer » rouvre « Prêt à scanner ». NFC coupé : « Scanner » ouvre directement l'échec
  « Le NFC est coupé. » ; au retour du NFC (réglages ou état stable `ON` après un redémarrage),
  l'échec redevient « Prêt à scanner » et la lecture reprend.
- **Quatre échecs distincts.** Jusqu'ici, un tag étranger et une lecture ratée donnaient le même
  résultat. `ScanOutcome.NotANiumiPoint` et `NfcReader.start(…, onForeignTag)` les séparent :
  - pas de NDEF ni d'URI, ou URI refusée par le parseur commun → « Ce n'est pas un Niumi Point. » ;
  - Niumi Point hors de la liste de la session, ou d'une version de protocole inconnue → « Ce Niumi
    Point n'est pas enregistré. » ;
  - `IOException`, `FormatException` → « Ton Niumi Point n'a pas pu être lu. ».
  - La lecture prend d'abord le message NDEF en cache : un frôlement ne donne plus « n'a pas pu
    être lu » alors que tout était lu.
- **Après un scan validé** (`RELEASING`) : la date, l'heure et un cercle de chargement seuls, lu
  « Fin de la session ». Le cercle n'apparaît qu'au-delà de 300 ms ; sur l'appareil, la fin est plus
  rapide et il ne se voit pas. L'ancien affichage de l'avancement de chaque effet
  (`ReleaseProgress`) est supprimé.
- Plus de disparition des messages après 3 s : un échec reste jusqu'à une action.

## Décisions de Mehdi (2026-10-22)

| Point | Décision |
| --- | --- |
| Q4 | Lecture NFC seulement feuille « Prêt à scanner » ouverte (parité iPhone) |
| Consigne | « Rejoins ton Niumi Point » (remplace « Scanne ton Niumi Point » de §14, qui laissait croire qu'on pouvait scanner avant d'ouvrir la feuille), pour iPhone et Android, et pour les trois états d'attente |
| Après le scan | Date, heure et chargement seuls, lu « Fin de la session » (une première version avec texte et trois lignes de progression a été retirée pendant les essais) |
| Mise en page | Place du lien de secours réservée ; date à 24 dp sous la marge du haut (60 dans la maquette, essayé à 0) |
| « Prêt à scanner » | À l'encre au lieu du gris : vaut pour toutes les feuilles de scan Android |
| Marge du bas (règle du 2026-10-20 révisée) | La barre de navigation plus 16 dp, jamais moins que 34 : le dernier bouton des écrans et des feuilles ne touche plus la barre à trois boutons (64 dp au-dessus d'elle, environ 40 au-dessus des gestes). Le haut et les côtés ne changent pas. Vaut pour toute l'app (`marginsOverSystemBars`) |

Arbitrages pris sans question, signalés ici :
- `NfcScanSheet` reçoit un contenu présentationnel au lieu de `ScanSheetState` (`:core:designsystem`
  ne dépend pas de `:core:system`) ; écart noté dans le plan.
- Textes de « Prêt à scanner » dans `NiumiComponentTexts`, les mêmes sur trois écrans.
- Version de protocole inconnue → « n'est pas enregistré » ; NDEF corrompu → « n'a pas pu être lu ».
  L'écran 9 (scan requis) dit aussi « boîtier inconnu » pour une version inconnue.
- Vibration d'erreur aussi pour « pas un Niumi Point » (tag lu puis refusé).
- Feuille d'échec fermable par le voile, le glissement et le retour.
- Dessin : variante fidèle à la maquette 28 plutôt qu'un dessin unique.
- Sans matériel NFC (défensif) : « Cet appareil ne prend pas en charge le NFC. », sans bouton.

## Relecture indépendante

Un agent a relu le code avant les essais. Corrigés :
- test d'accessibilité de la feuille qui aurait échoué (deux fenêtres, donc deux racines) ;
- heure figée après une mise en veille (`delay` s'arrête quand le téléphone dort) ;
- lecture NDEF depuis le cache d'abord ;
- disponibilité du NFC relue une fois le receveur inscrit (course au retour des réglages) ;
- heure sur une seule ligne avec le texte agrandi ;
- deux tests renforcés (double « Annuler » horloge arrêtée ; `RINGING` dans la boucle des états).

Laissés en l'état :
- pendant l'animation de fermeture (environ 300 ms), la lecture reste active ;
- un scan refusé déjà en cours vibre même si « Annuler » vient d'être touché ;
- au passage à `RELEASING`, la feuille disparaît sans animation.

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md` : §4.4 ; §10.4 (Reader Mode lié à la feuille, consigne, après-scan) ;
  §11.2 (tableau des résultats réécrit, relecture sur état stable limitée à la feuille ouverte) ;
  §15 (écran 8) ; §19.1 (`AlarmScreenActionsTest`) ; §20 (lignes du scan, tag feuille fermée).
- `design-plan/SPECIFICATION.md` : §6 (titre de la feuille Android à l'encre), §14 (consigne
  remplacée, décisions Android du jour).
- `docs/android/RELEASE_REPORT.md` : critère 6 cité sur `AlarmScreenActionsTest`.
- Plan de refonte : Q4 et décisions du jour, écart de signature de `NfcScanSheet`, cases.
- `TEXTES-DES-MAQUETTES.md` n'est pas modifié : il recopie les maquettes, et `SPECIFICATION.md`
  fait foi. `CLAUDE.md` n'est pas modifié.

## Tests

- **JVM** : 1 532, aucun échec. Nouveaux : `ScanOutcomeMappingTest` (4), `AlarmTextsTest` (7).
  Étendus ou réécrits : `HandleValidNfcUseCaseTest` (+2), `AlarmViewModelTest`,
  `AlarmScreenStateTest`, `AlarmNfcScanCoordinatorTest` (+2), `NiumiTimeFormatTest`,
  `NiumiComponentTextsTest`, `NiumiMarginsTest` (marge du bas). Supprimé avec son code : `ReleaseProgressTest`.
- **Instrumentés**, derniers passages : app 13/14, database 90, designsystem 53, system 33,
  ringing 14, session 3, setup 55.
  - Nouveaux : `AlarmScreenActionsTest` (9, remplace `AlarmScreenNoStopActionTest`, avec contrôles
    d'accessibilité automatiques), `NfcScanSheetTest` (8).
  - **Échec de `:app`** : `BlockingGuardServiceInstrumentedTest.theGuardReachesTheForegroundWithItsNotificationThenStops`
    (titre de la notification `null`). C'est l'échec du service de garde A8 déjà relevé aux étapes
    34 et 35. Il échoue aujourd'hui à chaque passage (4 sur 4), et **aussi sur `HEAD` sans les
    changements de l'étape**, rejoué dans une copie de travail à part : il ne vient pas de
    l'étape 38. Non traité ici.
  - HyperOS a refusé une installation (`INSTALL_FAILED_USER_RESTRICTED`) tant que la fenêtre
    « Installer via USB » restait sans réponse ; relancé une fois acceptée.
- **ktlint, detekt, lint, assemblage** : verts. Aucune couleur en dur hors `NiumiPalettes.kt` ;
  aucun « boîtier » dans `:feature:ringing`.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS OS3.0.302.0.WPPEUXM, 394 dp de large.

| Essai | Résultat |
| --- | --- |
| 1. Écran 10 à la sonnerie | **OK** ; mise en page jugée trop basse → place du lien réservée, date remontée |
| 2. Niumi Point approché feuille fermée (Q4) | **OK** : rien dans Niumi, lecture inactive |
| 3. Scan feuille ouverte | **OK** : session terminée (la feuille ne gêne pas le Reader Mode) |
| 4. Feuille « Prêt à scanner » | **OK** : journal NFC `reader_mode_change flags: 1` à l'ouverture ; titre jugé peu visible → encre |
| 5. Tag étranger | **OK** : « Ce n'est pas un Niumi Point. », vibration, lecture coupée |
| 6. « Réessayer », puis Niumi Point non associé | **OK** : « Ce Niumi Point n'est pas enregistré. » |
| 7. Voile sur la feuille d'échec, puis retour | **OK** : la feuille se ferme, le retour ne fait rien |
| 8. NFC coupé, « Ouvrir les réglages », NFC rallumé | **OK** : « Prêt à scanner » au retour, lecture reprise |
| 9. Bon Niumi Point | **OK** : session terminée ; chargement trop bref pour être vu |
| 10. Écran verrouillé, notification, déverrouillage | **OK** : écran 10 ouvert par la notification |
| 11. TalkBack sur l'écran et la feuille | **OK** |
| 12. Redémarrage pendant la sonnerie, « Scanner » aussitôt | **OK** : « Le NFC est coupé. » environ 2 s, puis « Prêt à scanner » seul (lecture réactivée à 14:41:47 puis 14:41:52, relance HyperOS) ; scan accepté |

`dumpsys nfc` affiche `mIsReaderMode=false` même feuille ouverte : ce champ ne reflète pas le
Reader Mode sur ce téléphone, le journal `reader_mode_change` fait foi.

## Restes et points d'attention

- **Marge du bas révisée en fin d'étape** : avec trois boutons, tout le bas de l'app remonte de
  16 dp. Vérifié par `NiumiMarginsTest`, par les instrumentés repassés (designsystem 53, ringing 14,
  setup 55) et sur l'appareil : écran 2, écran 4, page « Applications à bloquer », et « Valider »
  16 dp au-dessus du clavier ouvert. L'écran 1 et les feuilles de scan n'ont pas été revus après ce
  changement.
- **Message système feuille fermée** : aucun n'a été signalé pendant l'essai 2 sur HyperOS ; d'autres fabricants
  peuvent afficher leur propre message quand un tag est approché sans lecture active.
- **Jusqu'à l'étape 43**, un Niumi Point ajouté après l'activation reste refusé et s'annonce « Ce
  Niumi Point n'est pas enregistré. ».
- **iPhone** : la consigne « Rejoins ton Niumi Point » vaut aussi pour l'iPhone (§14).
- **Rien n'est committé.**
