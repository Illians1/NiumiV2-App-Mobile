# Étape 29 — Contrat KMP 1.4 : vérificateur sur liste, limite de boîtiers, specs

Date : 2026-10-13. **Étape terminée pour le code, les tests et les specs**, avec une réserve
d'outillage sans rapport avec son contenu (voir « Vérifications »). Première étape du Lot 8
(boîtiers multiples), conçu et validé le même jour :
`docs/superpowers/specs/2026-10-13-boitiers-multiples-design.md`.

Le contrat commun sait maintenant vérifier un scan contre une **liste** de boîtiers figés et
compter les boîtiers associés ; Android compile et se comporte exactement comme avant, avec une
liste d'un seul boîtier. Aucune interface, aucune persistance ne change à cette étape.

## Ce qui est livré

- **`BoxVerifier.verify(payload, credentials: List<PairedBoxCredential>, context)`** :
  `UNSUPPORTED_VERSION` si la version du payload n'est pas prise en charge ; sinon `MATCH` au
  premier credential dont le `boxId` est égal **et** dont l'empreinte correspond (temps constant,
  inchangé), la preuve opaque portant ce `boxId` ; sinon `NO_PAIRED_BOX_MATCH`, liste vide
  comprise. Une empreinte non canonique dans la liste est un credential qui ne correspond pas et
  les suivants sont examinés — jamais une exception (§14).
- **`BoxVerificationStatus`** : `MATCH`, `NO_PAIRED_BOX_MATCH`, `UNSUPPORTED_VERSION`.
  `BOX_MISMATCH` et `TOKEN_MISMATCH` disparaissent : distinguer « bon identifiant, mauvais
  token » n'avait de sens que face à un boîtier unique, et c'est une information de moins qui
  sort du module.
- **`NiumiCoreFacade.verifyBox(payload, credentials, context)`** : même signature en DTO.
- **`PairedBoxLimit`** (`MIN = 1`, `MAX = 5`), constante commune sur le modèle des bornes 1–50 de
  `AppSelectionSummary`.
- **`ActivationPolicyInput.pairedBoxCount: Int`** remplace `hasPairedBox: Boolean` (idem pour le
  DTO et le mapper) ; `NO_PAIRED_BOX` sous `MIN`, nouveau code `TOO_MANY_PAIRED_BOXES` au-dessus
  de `MAX`.
- **Android, adaptation minimale pour rester vert** : `HandleValidNfcUseCase` passe une liste
  d'un seul credential et traduit `NO_PAIRED_BOX_MATCH` en `ScanOutcome.UnknownBox` ;
  `ReadinessDtoMapper` convertit `hasPairedBox` en `1`/`0` — **transitoire**, l'étape 30 fait
  porter le vrai compte par le rapport de diagnostic.

## Specs mises à jour dans le même changement

- **SPEC_CORE_KMP** : §2 (décision 3 reformulée ; décision 11 réécrite pour une liste ; nouvelle
  décision 16, contrat 1.4, avec ce que l'on accepte en la prenant ; alinéa « secours
  logiciel »), §4 (invariant « boîtiers figés »), §6 (preuve portant le `boxId` reconnu), §9.2
  (alinéa « Plusieurs boîtiers », signature du vérificateur, statuts), §10 (capture de la liste,
  `NO_PAIRED_BOX` / `TOO_MANY_PAIRED_BOXES`), §14 (`verifyBox`, `pairedBoxCount`, association
  sans `verifyBox`), §17 (tests NFC et politiques). Relecture faite : aucune phrase ne suppose plus
  « le boîtier » au singulier hors du §1 (choix industriel du tag) et des textes de scan, qui
  restent vrais avec plusieurs boîtiers.
- **SPEC_ANDROID** : §3 (point 6 et règle de modification), §11.2 (table des issues du scan :
  `MATCH` / `NO_PAIRED_BOX_MATCH`). §7.2, §7.3, §11.1, §11.3, §13, §15, §17, §19 restent pour
  les étapes 30 et 31, comme le plan le prévoit.
- **SPEC_IOS** (spec seulement, aucun code iOS) : §14 (association en liste, nom, doublon, scan
  contre la liste figée, message « Ce boîtier n'est pas associé à cet iPhone. »), état métier
  (liste figée dans `NiumiSessionRecord`), Keychain (un à cinq, nommés), tests, critère
  d'acceptation 1, procédure de gestion des boîtiers.

## Tests

- **Communs, réécrits ou étendus** : `BoxVerifierTest` (liste vide ; un boîtier qui correspond ;
  sans contexte, pas de preuve ; autre identifiant ; autre token ; correspondance en première et
  en dernière position parmi cinq ; cinq sans correspondance ; identifiant connu avec mauvais
  token parmi d'autres ; empreinte non canonique ignorée sans exception ; version non prise en
  charge malgré un credential correspondant ; empreinte vide, non hexadécimale, en majuscules),
  `NiumiCoreFacadeNfcTest` (preuve du boîtier reconnu, pas du premier ; liste vide),
  `ActivationPolicyTest` (0, 1, 5, 6 boîtiers). Deux tests absents de la liste du plan ont été
  adaptés à la nouvelle signature sans changer leurs assertions : `NiumiCoreFacadeTest`
  (délégation de `evaluateActivation`) et `NfcVerificationProofTest` (`toString` de la preuve).
- **Android** : `HandleValidNfcUseCaseTest.aTagMatchingANewlyPairedBoxIsRefused` renommé
  `aTagOutsideTheFrozenListIsRefused` ; `theSameBoxWithAnotherTokenIsRefused` inchangé ; tous deux
  attendent toujours `UnknownBox`. `SessionDtoFixtures.validNfcScanned` passe une liste d'un seul
  credential. Les commentaires de `NfcScanFixtures` citent le nouveau statut.
- **Fixtures** `nfc_payloads.json` : non modifiées — elles portent déjà deux boîtiers valides
  distincts, ce que le plan demandait.

## Vérifications

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:core:jvmTest                                              # rouge avant implémentation (signature, statuts, PairedBoxLimit, pairedBoxCount), vert après
./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64                  # vert
./gradlew :core:system:testDebugUnitTest :feature:session:testDebugUnitTest :feature:setup:testDebugUnitTest :app:testDebugUnitTest   # verts
./gradlew :app:assembleDebug ktlintCheck detekt                             # verts (ktlintFormat passé une fois sur les tests communs réécrits ;
                                                                            #   detekt a imposé un seul return dans BoxVerifier.verify, ReturnCount)
./gradlew :app:lintDebug                                                    # ÉCHEC, 3 × NewerVersionAvailable — voir ci-dessous
grep -rn "BOX_MISMATCH\|TOKEN_MISMATCH" shared androidApp --include='*.kt'  # une seule mention : le KDoc de BoxVerificationResult qui explique leur disparition
grep -rln "hasPairedBox" shared androidApp --include='*.kt'                 # deux KDoc KMP (« remplace ») + les usages Android transitoires de l'étape 30
```

**`:app:lintDebug` a d'abord échoué pour une raison étrangère à l'étape** : Kotlin 2.4.21 avait
été publié depuis l'épinglage de 2.4.20 (`libs.versions.toml`), et lint remontait trois
`NewerVersionAvailable` (`kotlin.multiplatform`, `kotlin.plugin.compose`,
`kotlin.plugin.serialization`) — le cas déjà rencontré deux fois le 2026-09-11. Changement de
toolchain hors du périmètre de l'étape, **décidé avec l'utilisateur** le jour même après lecture de
la matrice officielle (ligne commune 2.4.20–2.4.21 : Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1 ; AGP
9.1.1 et Gradle 9.5.0 restent à l'intérieur) et des notes de version (correctifs seulement, aucun
changement de compatibilité). **Montée faite** (`kotlin = "2.4.21"`), batterie complète rejouée
ensuite, en une passe de 9 min, tous verts :

```bash
./gradlew :shared:core:jvmTest :shared:core:linkDebugFrameworkIosSimulatorArm64 testDebugUnitTest :app:testReleaseUnitTest \
  :app:assembleDebug ktlintCheck detekt :app:lintDebug :app:lintRelease :app:assembleRelease :app:bundleRelease   # BUILD SUCCESSFUL
```

Consigné dans le plan maître (« Versions de référence », « Montée de Kotlin du 2026-10-13 »).

## Ce qui ne change pas, et pourquoi c'est sûr

- Machine à états, snapshot commun, parseur, `NfcVerificationProof` : intacts ; la preuve ne
  compare rien au snapshot, elle est liée à la session, à l'événement et à la révision.
- Sur appareil, le comportement est identique à celui de l'étape 28 : une liste d'un seul boîtier
  donne les mêmes verdicts que l'ancien vérificateur. **Aucun test manuel** à cette étape, comme le
  plan le prévoit.

## Incertitudes et limites

- Le transitoire `pairedBoxCount = if (hasPairedBox) 1 else 0` est volontaire et balisé dans le
  code ; il disparaît à l'étape 30 avec le vrai compte du dépôt.

**Rien n'est committé.**
