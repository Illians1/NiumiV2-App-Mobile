# Étape 42 — Contrat KMP 1.5 (2/2) : dix Niumi Spots, sortie de secours, temps gagné

Date : 2026-10-24. **Étape terminée** : code, tests JVM, test instrumenté du dépôt de boîtiers sur
appareil, framework iOS, ktlint, detekt, lint et assemblage. Dixième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 12, phase O), qui clôt le contrat 1.5.
Point de départ : `7eb9c7c`.

## Ce qui est livré

- **Un à dix Niumi Spots** : `PairedBoxLimit.MAX` passe de 5 à 10 (`MIN = 1` inchangé).
  `ActivationPolicy` accepte dix boîtiers et refuse le onzième avec `TOO_MANY_PAIRED_BOXES`.
- **Textes de la sortie de secours** (`:shared:core`, `emergency/`) : `EmergencyExitTexts`, avec un
  seul texte validé, `"v1-01"`, recopié mot pour mot avec les apostrophes typographiques de D13.
  Il compte 196 caractères. La liste est prête à recevoir d'autres textes.
- **Vérification de la recopie** : `EmergencyTranscription.verify(textId, typed, context)`.
  - **Normalisation.** La saisie et le modèle passent en minuscules. Les lettres accentuées du
    français sont repliées (`œ` en `oe`, `æ` en `ae`), et tout ce qui n'est ni lettre, ni
    chiffre, ni espace est retiré. Le découpage se fait ensuite sur les espaces. « J’avais »,
    « J'avais » et « Javais » donnent donc tous `javais`.
  - **Comparaison mot à mot.**
    - `COMPLETE` : tous les mots sont égaux, et en même nombre.
    - `INCOMPLETE` : la saisie est plus courte, et seul son dernier mot peut être le début du mot
      attendu. Une saisie vide est incomplète.
    - `MISMATCH` : un mot diffère ou un mot est en trop. L'index désigne le premier mot qui
      diffère, ou vaut 34 pour un mot de trop.
    - `UNKNOWN_TEXT` : l'identifiant du texte est inconnu.
  - `typedCharCount` est la longueur brute de la saisie, pour le compteur.
  - **Preuve.** `EmergencyExitProof` n'est produite que sur `COMPLETE` avec un contexte. C'est une
    classe opaque à constructeur `internal`, sur le modèle de `NfcVerificationProof`.
- **Sortie de secours dans le moteur.** Nouvel événement `EMERGENCY_EXIT_CONFIRMED`, nouveau champ
  `SessionEvent.emergencyProof` (dernier paramètre, `null` par défaut), nouveau code
  `MISSING_EMERGENCY_PROOF` et nouveau réducteur `EmergencyReducer`.
  - **Transitions acceptées :**
    - depuis `ARMED`, blocage demandé, avant le réveil : `RELEASING` vers `CANCELLED` ;
    - depuis `RINGING`, `AWAITING_NFC` et `TRIGGERED_AWAITING_NFC` : `RELEASING` vers `COMPLETED`.

    Dans les deux cas, `releasedBy = EMERGENCY_EXIT`, sans `nfcVerifiedAtEpochMillis`, avec les
    effets du scan valide.
  - **Refus :**
    - sans preuve : `MISSING_EMERGENCY_PROOF` ;
    - preuve d'un autre événement, ou preuve portée par un autre événement :
      `UNEXPECTED_EVENT_PAYLOAD` ;
    - depuis `ARMED` blocage en attente, ou depuis un état final : `INVALID_STATE_TRANSITION` ;
    - depuis `ARMED` à ou après le réveil : `TRIGGER_ALREADY_ELAPSED`.

  `ReleaseReducer` accepte déjà `EMERGENCY_EXIT` vers `COMPLETED` ou `CANCELLED` sans changement.
  `RELEASE_SUCCEEDED` conserve `releasedBy`.
- **Effets de libération factorisés** : `provenReleaseEffects` (`ReducerSupport.kt`) sert au scan
  et à la sortie de secours. Les effets du scan sont inchangés.
- **Temps gagné** (`insight/`) : `TimeToGetUpCalculator`.
  - Le temps écoulé est arrondi à la minute inférieure, et une libération antérieure à la
    sonnerie compte pour zéro.
  - Les minutes gagnées valent la durée déclarée moins le temps écoulé. Elles valent `null` si le
    résultat n'est pas strictement positif.
- **Façade** : trois nouvelles méthodes, `emergencyExitTexts()`,
  `verifyEmergencyTranscription(input, context)` et `computeTimeToGetUp(input)`.
  - Nouveaux DTO : `EmergencyDtos.kt` et `InsightDtos.kt`.
  - `SessionEventDto.emergencyProof` est `@Transient` et placé en dernier : aucun des neuf appels
    Android ne change.
  - Le schéma reste en version 3 : aucun champ n'est persisté.
- **Android, limite à dix** : texte transitoire « 10 boîtiers maximum. ». Commentaires « sixième »
  devenus « onzième » ; tests adaptés (voir « Écarts »).

## Décisions de Mehdi (2026-10-24)

- **D2** : le matin, la recopie termine la session comme un scan (`COMPLETED`). Suivent l'écran 11
  sans la phrase « Bravo ! … », et un réveil laissé activé pour le lendemain. Le soir, elle annule
  la session (`CANCELLED`), ce qui éteindra l'interrupteur à l'étape 44.
- **Texte de 196 caractères** : le texte reste inchangé. Le « 197 » de la spécification et des
  maquettes était faux : leurs compteurs sont approximatifs (« 84 » pour 83 caractères réels,
  « 32 » pour 41).
- **D8** : un seul texte, dans une liste prête, sans rotation codée. Tant qu'il est seul, le texte
  ne change pas d'une sortie à l'autre ; les autres textes restent à choisir.
- **D3** : « 1 heure et + » vaut 60. **Q10** : arrondi à la minute inférieure.
- **Limite à dix appliquée à Android dès cette étape**, et non à l'étape 43.

## Écarts au plan

1. **196 et non 197** : `EmergencyExitTextsTest` vérifie 196. La spécification, le plan et les
   points ouverts sont corrigés. `TEXTES-DES-MAQUETTES.md` reste la transcription fidèle des
   maquettes.
2. **Pas de NFD.** `commonMain` n'offre pas de normalisation Unicode. Les accents précomposés sont
   repliés par une table ; les diacritiques combinants disparaissent avec la ponctuation, car ce ne
   sont ni des lettres ni des chiffres. Le résultat est le même pour le texte français, et les deux
   cas sont testés.
3. **Refus ajouté par symétrie avec le scan** : `EMERGENCY_EXIT_CONFIRMED` depuis `ARMED` à ou après
   le réveil est refusé avec `TRIGGER_ALREADY_ELAPSED`. Sans ce refus, une sortie du matin traitée
   avant `TRIGGER_ELAPSED` mènerait à `CANCELLED` et éteindrait le planning.
4. **Android touché.** `PairedBoxLimit` est partagé, donc le dépôt accepte dix boîtiers dès cette
   étape. Changements dans le même changement :
   - `PairingTexts.LIMIT_REACHED` et `PairingTextsTest` ;
   - `RoomPairedBoxStoreTest` (instrumenté) : dixième boîtier accepté, onzième refusé ;
   - `PairingViewModelTest` : neuf boîtiers permettent encore l'ajout, dix le masquent ;
   - `AndroidDeviceReadinessCheckerTest` : dix boîtiers passent, onze échouent.

   Les lignes correspondantes de l'étape 43 sont marquées « déjà fait ».
5. **`SessionEvent.emergencyProof` a une valeur par défaut**, contrairement aux autres champs : sans
   elle, il aurait fallu réécrire chaque construction d'événement.
6. **`SessionEventValidation`** : les contrôles des deux preuves passent par une même fonction de
   fichier, `presenceViolations`. L'objet dépassait sinon le seuil detekt de onze fonctions.
7. **SPEC_IOS hors des sections prévues.** Deux lignes des §22 et §23 citaient encore la liste
   figée et la limite de cinq ; elles sont alignées sur le contrat 1.5.

## Ce que cette étape ne couvre pas, volontairement

- **Ensemble courant au scan côté Android** : `session_box`, la liste figée et le verrou de l'écran 3
  en session restent en place jusqu'à l'étape 43. SPEC_CORE_KMP le signale à chaque endroit
  concerné.
- **Production de `EMERGENCY_EXIT_CONFIRMED` par le coordinateur** (étape 44), puis écrans 8 et 9
  (étape 49).
- **Temps gagné affiché** (écran 11, étape 46) et durée déclarée (écran 3 bis, étape 50). L'instant
  de « la sonnerie » à transmettre au calculateur se décide à l'étape 46.
- **Rotation des textes** : elle sera codée en KMP quand d'autres textes existeront.

## Specs et documents modifiés

- `specs/SPEC_CORE_KMP.md` :
  - en-tête « 1.5 » ;
  - §2 :
    - décision 3 (scan de l'ensemble courant ou sortie de secours) ;
    - décision 7 (la sortie de secours n'est pas un bouton d'arrêt) ;
    - décision 11 (ajout, renommage et suppression en session, de un à dix) ;
    - décision 13 (sortie de secours, seul secours) ;
    - décision 16 (un à dix, ensemble courant) ;
    - paragraphe sur le secours ;
  - §3.1 ;
  - §4 (ensemble courant, invariant de la sortie de secours) ;
  - §5 (diagramme, transitions, paragraphe) et §5.2 (trois refus) ;
  - §6 (événement, `emergencyProof`, `EmergencyExitProof`, ligne d'effets) ;
  - §7.3 (code, exception des textes communs) ;
  - §9.2 (`MAX = 10`, ensemble courant) ;
  - §10 (activation sans liste figée) ;
  - §12 (sortie de secours, normalisation, temps gagné) ;
  - §14 (trois méthodes, DTO) ;
  - §17 (machine à états, politiques, nouvelle rubrique) ;
  - §19 (4.6 réécrit, trois lignes iOS 1.5).

  « aucun secours logiciel » n'y apparaît plus.
- `specs/SPEC_IOS.md` :
  - §4.6 (sortie de secours par recopie, alarme pendant la recopie `À REVOIR · PRODUIT`) ;
  - décisions fixées ;
  - §8 (liste non copiée dans la session) ;
  - §9 (Keychain, de un à dix) ;
  - §14 (association en session, dix au plus, ensemble courant) ;
  - §15 (sortie de secours, temps gagné, reprise sur `releasedBy`) ;
  - §22 et §23 ;
  - tableau des cas « boîtier perdu ».
- `specs/SPEC_ANDROID.md` : la limite passe à dix (§1, §11, tests §19 et recette), avec « 10
  boîtiers maximum. ». La liste figée reste décrite, jusqu'à l'étape 43.
- `design-plan/SPECIFICATION.md` :
  - §7 : 60 tranché ;
  - §13 : compteur « 84 sur 196 », 196 caractères, D8, matin tranché.
- `design-plan/POINTS-OUVERTS.md` : D2 et D3 tranchés, D8 décidé en partie, E11 renvoie à D2.
- Plan de refonte : lignes D2 et D8/D3/Q10 du tableau ; interface `EmergencyExitTexts` ; étape 42
  (écarts, cases) ; étape 43 (texte et limite déjà faits).
- KDoc de `BoxVerifier` et de `NiumiCoreFacade.verifyBox` : liste transmise par le coordinateur, et
  non plus figée.

## Tests

- **JVM** : 1 675 tests, aucun échec (KMP 303, Android 1 372), contre 1 626 à l'étape 41.
  - **Nouveaux tests :**
    - `EmergencyExitTextsTest` (5 essais) ;
    - `EmergencyTranscriptionTest` (18), dont les deux saisies des maquettes 28 et 29 ;
    - `SessionEngineEmergencyExitTest` (12) ;
    - `TimeToGetUpCalculatorTest` (7).
  - **Tests étendus :**
    - `ActivationPolicyTest` (dix acceptés, onze refusés) ;
    - `SessionEngineForbiddenTransitionsTest` (10 × 14) ;
    - `DtoRoundTripTest` (événement avec preuve par référence, JSON sans preuve) ;
    - `NiumiCoreFacadeTest` (trois méthodes, plus un parcours complet recopie → réduction →
      `RELEASING` vers `COMPLETED`) ;
    - fixture partagée « sortie sans preuve refusée » (`session_transitions.json`, 46 cas).
  - **Android adapté** : `PairingTextsTest`, `PairingViewModelTest`,
    `AndroidDeviceReadinessCheckerTest`.
  - **Échecs vérifiés avant implémentation :**
    - `ActivationPolicyTest` à dix boîtiers (`MAX` valait 5) ;
    - compilation des nouveaux tests (types absents).

    Une fois la limite portée à dix, deux tests Android l'ont fait échouer, comme prévu ; ils sont
    adaptés.
- **Framework iOS** (`linkDebugFrameworkIosSimulatorArm64`) : construit. L'en-tête exporte :
  - les méthodes `emergencyExitTexts()`, `verifyEmergencyTranscription(input:context:)` et
    `computeTimeToGetUp(input:)` ;
  - `EmergencyExitProof`, sans initialiseur public ;
  - `emergencyExitConfirmed`.
- **ktlint, detekt, lint, `assembleDebug`** : verts. Detekt a refusé avant correction :
  - une condition trop complexe dans `EmergencyReducer`, extraite en `acceptsEmergencyExit()` ;
  - douze fonctions dans `SessionEventValidation` ;
  - trois lignes trop longues.

## Validation sur appareil

**Faite le 2026-10-24** sur Xiaomi 25080RABDG, Android 16 :
`./gradlew :core:database:connectedDebugAndroidTest` passe ses 91 tests sans échec, dont
`aTenthBoxIsAccepted` et `anEleventhBoxReturnsLimitReachedAndWritesNothing` (`RoomPairedBoxStoreTest`).
Seul le module `:core:database` a été lancé, pas `:app` : le service d'accessibilité n'a pas été
touché. Aucun test manuel n'est prévu : éprouver la limite à la main demanderait onze tags.
