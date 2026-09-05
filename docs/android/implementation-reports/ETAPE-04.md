# Étape 4 — lecture NFC en Reader Mode et arrêt du POC après scan associé

Date : 2026-09-05. Tests automatisés, vérifications statiques et une partie du protocole
manuel réalisés sur Redmi 25080RABDG (HyperOS, Android 16 / API 36), le même appareil qu'à
l'étape 3. **Association du tag et scan d'arrêt validés avec succès ; un bug trouvé et corrigé
en cours de route (voir ci-dessous) ; les essais « tag non associé » et « NFC désactivé »
restent à faire.**

## Résumé

Ferme la boucle du Lot 0 : `AlarmActivity` lit désormais un tag NFC en Reader Mode, transmet
l'URI brute au parseur commun de `:shared:core` (jamais de décision de validité côté Android)
et arrête la sonnerie sur scan valide — via la route POC (`PocNfcScanHandler`, `src/debug` de
`:app`), le moteur commun (Phase C) n'existant pas encore. `PairedBoxStore` (interface) et son
implémentation debug sur DataStore Preferences (`DebugPairedBoxStore`) permettent d'associer un
tag depuis une nouvelle activité `PocPairingActivity`.

Un défaut de l'étape 2 a été corrigé : `BoxVerifier.hexToBytes` pouvait lancer une exception
sur un `tokenSha256Hex` corrompu, ce que SPEC_CORE_KMP §14 interdit. Cette étape est la première
à alimenter `verifyBox` depuis un stockage persistant (DataStore), donc la première où ce défaut
devenait atteignable.

Quatre décisions ont été validées avec l'utilisateur avant l'implémentation (voir « Décisions »)
et le plan MVP a été mis à jour en conséquence (case à cocher de l'étape 4, section « Interfaces
transverses »).

## Bug trouvé et corrigé pendant la validation manuelle

**`AlarmActivity` ne se fermait pas après un scan accepté.** Le son et la vibration
s'arrêtaient bien (`RingingController.stopRinging()`), mais l'écran restait affiché sur
« Scanne ton boîtier Niumi pour arrêter l'alarme. » sans aucune confirmation, car
`AlarmScreenState` ne traitait pas `ScanOutcome.Accepted` dans son ordre de priorité — il
retombait sur le texte de phase par défaut, identique à avant le scan. Repéré en testant
manuellement sur appareil, aucun test automatisé ne pouvait le voir (aucune notion d'écran
« terminé » n'existait avant ce scan réel). Le nom même de l'étape (« arrêt **du POC** après
scan associé », pas seulement arrêt du son) confirmait qu'il s'agissait bien d'un défaut à
corriger, pas d'une simplification acceptable.

Correction : dans `AlarmActivity`, un `ScanOutcome.Accepted` appelle désormais `finish()` au
lieu de rafraîchir l'état d'écran. Revérifié avec succès : sonnerie coupée puis écran fermé
automatiquement, sans action de l'utilisateur au-delà du scan.

## Constats de la validation manuelle (non-bugs)

Deux comportements observés sur appareil ont d'abord semblé suspects, mais correspondent à un
comportement Android documenté et volontaire, sans rapport avec le code de Niumi :

1. **NFC illisible tant que l'appareil est réellement verrouillé.** `AlarmActivity` s'affiche
   correctement par-dessus le verrou (`canShowWhenLocked`, confirmé dans les logs
   `ActivityTaskManager`), et le texte « Déverrouille ton téléphone, puis approche-le du
   boîtier. » s'affiche bien à ce moment (confirmé par l'utilisateur) — la détection
   `KeyguardManager.isDeviceLocked` fonctionne donc correctement. Mais le scan NFC lui-même ne
   semble pas aboutir tant que le téléphone n'est pas réellement déverrouillé sur ce HyperOS :
   c'est exactement l'incertitude anticipée par SPEC_ANDROID §4.4 (« le scan ne fonctionnera
   pas forcément avant déverrouillage sur tous les appareils »). Confirmé empiriquement sur ce
   modèle ; à reconfirmer sur Pixel et Samsung à la porte de validation 0 (étape 6).
2. **Le plein écran ne se déclenche pas automatiquement si l'écran est déjà allumé/déverrouillé
   avec l'app au premier plan.** `Notification.fullScreenIntent` n'est auto-lancé par Android
   que si l'appareil est verrouillé ou l'écran éteint ; sinon le système affiche volontairement
   une notification (heads-up) et attend un tap, pour ne pas arracher l'utilisateur de ce qu'il
   fait. Comportement identique à l'app Horloge de Google/Samsung, et explicitement toléré par
   SPEC_ANDROID §20 (« écran présenté **ou** notification urgente »). Aucun code applicatif ne
   peut contourner cette politique système.

## Limitation connue, différée à l'étape 17-18

**Aucune navigation automatique vers `AlarmActivity` quand l'utilisateur est déjà dans une
autre activité de Niumi au moment où l'alarme sonne** (par exemple resté sur l'écran POC).
Rien n'empêche techniquement cette navigation (l'app est déjà au premier plan, aucune
restriction système ne s'applique, contrairement au cas précédent) : `AlarmReceiver` démarre
`AlarmRingingService` et pose la notification, mais rien ne signale à l'activité déjà affichée
que la sonnerie a commencé.

Décision prise avec l'utilisateur : ne pas construire de mécanisme temporaire sur la route POC
(supprimée à l'étape 21). Le vrai correctif appartient à la Phase C : `SessionCoordinator`
expose déjà `ReconcileReason.PROCESS_START`, et la réconciliation de session au premier plan
devra naturellement couvrir « une session sonne → afficher l'écran d'alarme ». À reprendre à
l'étape 17-18.

## Décisions validées avec l'utilisateur

1. **`@BindsOptionalOf NfcScanHandler`.** `AlarmActivity` (`src/main` de `:feature:ringing`)
   injecte `Optional<NfcScanHandler>` : présent en debug (`PocNfcScanHandler`), absent en
   release jusqu'à l'étape 18 (`HandleValidNfcUseCase`). Aucun binding no-op dans `main`
   (CLAUDE.md : pas de faux comportement de production).
2. **Décodage NDEF maison.** `NfcUriExtractor` décode le format RTD-URI 1.0 (NFC Forum) sur un
   descripteur pur `NdefRecordData`, sans passer par `NdefRecord.toUri()` : cette dernière est
   un stub Android non testable en JVM, et normalise le schéma en minuscules — ce qui aurait
   fait accepter `NIUMI://` que SPEC_CORE_KMP §9.1 interdit explicitement (« Le parseur ne doit
   pas accepter une URI qu'une normalisation permissive rendrait équivalente »). Un test dédié
   (`uppercaseSchemeIsNeverLowercased`) verrouille ce choix. Le traducteur Android
   (`AndroidNdefReader`, `ReaderModeNfcReader`) reste mince et non testé en JVM, même motif
   « descripteurs purs » que l'étape 3.
3. **Vibration d'erreur avec reprise (`VibrationPatternPolicy`).** `Vibrator.vibrate()` remplace
   tout effet en cours : un pulse d'erreur naïf aurait coupé définitivement la vibration
   d'alarme. La décision du motif (pulse simple, ou pulse + reprise du motif d'alarme) est
   encodée dans une politique pure et testée en JVM ; `AndroidVibrationController` ne fait que
   traduire le motif choisi en `VibrationEffect.createWaveform`, non testable en JVM.
4. **Correctif `BoxVerifier` dans cette étape.** `hexToBytes` appelait `digitToInt()` sans
   valider la forme du hex, ce qui lançait `IllegalArgumentException` sur un hex corrompu.
   `statusOf` valide désormais la forme (64 caractères hexadécimaux minuscules) avant tout appel
   à `hexToBytes` ; un hex corrompu devient `TOKEN_MISMATCH`, jamais une exception.

## Décisions supplémentaires prises pendant l'implémentation

1. **`AlarmNfcScanCoordinator`.** Extrait d'`AlarmActivity` pour rester sous le seuil detekt
   `TooManyFunctions` (11) : garde de réentrance, journalisation technique et vibration d'erreur
   y vivent, testés en JVM avec des fakes (`AlarmNfcScanCoordinatorTest`). Même motif que le
   découpage de `SystemModule` à l'étape 3.
2. **`NfcScanHandler` en `fun interface`.** Cohérent avec `AlarmPlayerFactory`,
   `RingtoneResourceResolver`, `NiumiComponentResolver` (une seule méthode abstraite, SAM) ;
   permet des fakes concis dans les tests (`NfcScanHandler { ScanOutcome.Accepted }`).
3. **`PocSession` extrait de `PocViewModel`.** `PocNfcScanHandler` a besoin du même
   `sessionId`/`revision` fictifs pour appeler `RingingController.stopRinging()`.
4. **`lifecycle-runtime-compose` ajoutée en dépendance directe** (`debugImplementation`) de
   `:app` : `PocScreen` recharge le boîtier associé au retour de `PocPairingActivity` via
   `LocalLifecycleOwner` (package `androidx.lifecycle.compose`, non déprécié, déjà présent
   transitivement à la version 2.11.0 — rendu explicite plutôt que de dépendre du transitif).
5. **Aucun nouveau module Gradle.** Le nouveau code vit dans les modules existants
   (`:core:system` pour NFC et vibration, `:app/src/debug` pour la route POC) ; `ModuleListTest`
   reste vert sans modification.

## Écarts assumés

1. **`NfcReader.start()` gagne un paramètre `onUnreadable`**, absent du plan MVP (« Interfaces
   transverses »). SPEC_ANDROID §11.2 impose le texte « Boîtier non reconnu. Réessaie. » pour un
   tag physiquement illisible (pas de NDEF, `IOException`, `FormatException`), un cas qu'aucune
   URI ne peut représenter avec la seule signature `onUri`. Le plan MVP a été mis à jour dans le
   même changement (section « Interfaces transverses »).
2. **Le scan arrête directement le service via `PocNfcScanHandler`/`RingingController`**, sans
   passer par le moteur commun ni par `RELEASING`/`COMPLETED` (SPEC_ANDROID §11.3). Écart déjà
   assumé à l'étape 3 pour `AlarmReceiver` (§10.1) ; levé à l'étape 17-18 avec
   `HandleValidNfcUseCase`.
3. **Aucune `NfcVerificationProof` n'est produite avant la Phase C** : `PocNfcScanHandler`
   appelle `verifyBox(payload, credential, context = null)`. SPEC_CORE_KMP §14 ne prévoit
   `context = null` que pour une association, mais aucune session Android n'existe encore avant
   la Phase C : il n'y a donc, ici aussi, aucun contexte à fournir.

## Fichiers créés

**`:shared:core`** (correctif) — aucun fichier créé, voir « Fichiers modifiés ».

**`:core:system`** :
- `nfc/{NdefRecordData,NfcUriExtractor,NfcAvailability,NfcScanHandler,ScanOutcome,NfcReader,
  AndroidNdefReader,ReaderModeNfcReader}.kt`, `nfc/di/NfcModule.kt`
  (`NfcHandlerModule.@BindsOptionalOf`, `NfcReaderModule.@Provides`).
- `pairing/PairedBoxStore.kt`.
- `audio/VibrationPatternPolicy.kt`.
- Tests JVM : `nfc/NfcUriExtractorTest.kt`, `audio/VibrationPatternPolicyTest.kt`.

**`:feature:ringing`** :
- `AlarmNfcScanCoordinator.kt`.
- Test JVM : `AlarmNfcScanCoordinatorTest.kt`.

**`:app`** (`src/debug`) :
- `poc/{PocSession,DebugPairedBoxStore,PocNfcScanHandler,PocNfcModule,PocPairingViewModel,
  PocPairingScreen,PocPairingActivity}.kt`.
- `AndroidManifest.xml` (déclare `PocPairingActivity`).
- Test (`src/testDebug`) : `poc/PocNfcScanHandlerTest.kt`.

## Fichiers modifiés

- `shared/core/src/commonMain/kotlin/com/niumi/core/nfc/BoxVerifier.kt` : validation de la
  forme du hex avant décodage (correctif de robustesse, décision 4).
- `shared/core/src/commonTest/kotlin/com/niumi/core/nfc/BoxVerifierTest.kt` : 4 cas de hex
  corrompu.
- `androidApp/core/system/src/main/kotlin/com/niumi/system/audio/{VibrationController,
  AndroidVibrationController}.kt` : `vibrateError()`.
- `androidApp/core/system/src/test/kotlin/com/niumi/system/audio/DefaultAlarmAudioEngineTest.kt` :
  fake mis à jour (nouvelle méthode d'interface).
- `androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/{AlarmActivity,
  ui/AlarmScreenState,ui/AlarmScreen}.kt` : intégration NFC, ordre de priorité du texte affiché,
  raccourci réglages NFC.
- `androidApp/feature/ringing/src/test/kotlin/com/niumi/feature/ringing/ui/AlarmScreenStateTest.kt` :
  5 nouveaux cas (rangs de priorité).
- `androidApp/app/src/debug/kotlin/com/niumi/app/poc/{PocViewModel,PocScreen,PocNavigation}.kt` :
  `PocSession` extrait, bouton d'association, affichage du `boxId`, rafraîchissement au retour.
- `androidApp/app/build.gradle.kts` : `debugImplementation(libs.datastore.preferences)`,
  `debugImplementation(libs.lifecycle.runtime.compose)`.
- `gradle/libs.versions.toml` : entrée `lifecycle-runtime-compose`.
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : cases de l'étape 4, section « Interfaces
  transverses » (`NfcReader.start` avec `onUnreadable`).

## Textes affichés non imposés mot pour mot par la spec

SPEC_ANDROID §11.2 n'impose mot pour mot que « Boîtier non reconnu. Réessaie. » (tag illisible)
et « Déverrouille ton téléphone, puis approche-le du boîtier. » (téléphone verrouillé, §11.2
existant depuis l'étape 3). Les autres textes de `AlarmScreenState` sont des rédactions du plan :

- « Cet appareil ne prend pas en charge le NFC. » (matériel absent) ;
- « Le NFC est désactivé. Active-le pour scanner ton boîtier. » (NFC désactivé) ;
- « Ce boîtier n'est pas celui de ta session. » (boîtier scanné mais non associé) ;
- « Ouvrir les réglages NFC » (libellé du bouton, §11.2 n'impose que l'existence du raccourci).

À valider ou ajuster si une revue produit le juge nécessaire.

## Commandes exécutées et résultat

Environnement : `JAVA_HOME=/opt/homebrew/opt/openjdk@17`.

| Commande | Résultat |
| --- | --- |
| `./gradlew :shared:core:jvmTest` | BUILD SUCCESSFUL |
| `./gradlew :core:system:testDebugUnitTest` | BUILD SUCCESSFUL |
| `./gradlew :core:database:testDebugUnitTest` | BUILD SUCCESSFUL |
| `./gradlew :feature:ringing:testDebugUnitTest` | BUILD SUCCESSFUL (inclut `AlarmNfcScanCoordinatorTest`) |
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL (inclut `PocNfcScanHandlerTest`, `src/testDebug`) |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew ktlintCheck` | BUILD SUCCESSFUL (après `ktlintFormat` sur `:core:system`, `:feature:ringing`, `:app`) |
| `./gradlew detekt` | BUILD SUCCESSFUL (après correctifs `ReturnCount`, `SwallowedException`, `TooManyFunctions`, `UnsafeCallOnNullableType` détaillés ci-dessous) |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL |

### Corrections detekt appliquées pendant la vérification

- `NfcUriExtractor.decodeWellKnownUri`/`decodeAbsoluteUri` : 4 puis 3 `return` → repliés à 1-2
  par fonction (`ReturnCount`).
- `ReaderModeNfcReader.handleTag` : `@Suppress("SwallowedException")` documenté — un tag
  illisible doit devenir `onUnreadable()`, jamais une exception qui remonterait au binder
  Android (même justification que `BoxPayloadParser.decodeToken` à l'étape 2).
- `AlarmActivity` : 13 fonctions (limite 11) → logique de scan extraite dans
  `AlarmNfcScanCoordinator` (10 fonctions restantes).
- `PocNfcScanHandlerTest.pairedCredentialFor` : `!!` remplacé par `checkNotNull` avec message
  (`UnsafeCallOnNullableType`).
- `DebugPairedBoxStore.current`/`PocNfcScanHandler.onUriRead` : repliés à une seule instruction
  `return` chacun (`ReturnCount`).

## Résultats de la validation sur appareil

Redmi 25080RABDG, HyperOS, Android 16 / API 36. `adb devices` vérifié avant chaque lancement.
Tag de test : NTAG213, NDEF URI écrit avec TagWriter by NXP.

### Tests instrumentés — 6/6 verts

`./gradlew :core:system:connectedDebugAndroidTest :feature:ringing:connectedDebugAndroidTest`
→ BUILD SUCCESSFUL (1 test `:core:system`, 5 tests `:feature:ringing`, dont
`AlarmScreenNoStopActionTest` — toujours vert avec le nouveau bouton de réglages NFC, absent
par défaut de l'état testé).

### Essais manuels

| Essai | Résultat |
| --- | --- |
| Route POC → « Associer ce tag », approcher le NTAG213 | **OK** — `14bb2dc6…` affiché, aucun token visible |
| Alarme, écran éteint/verrouillé, scan du bon tag | **Partiellement OK** — écran affiché par-dessus le verrou avec le bon texte, mais le scan n'a abouti qu'après déverrouillage réel du téléphone (voir « Constats », limitation OEM confirmée, pas un bug) |
| Alarme, écran allumé/déverrouillé/app au premier plan, scan du bon tag | **OK** — sonnerie arrêtée quelques secondes après le scan, écran d'alarme fermé automatiquement (après correction du bug ci-dessus) |
| Plein écran automatique quand une autre app ou le verrou est actif | **OK** — confirmé au premier essai |
| Plein écran automatique quand Niumi est déjà au premier plan | **Non applicable** — notification affichée à la place, comportement Android attendu (voir « Constats ») |
| Scan d'un autre tag Niumi (vibration d'erreur, `UnknownBox`) | **Non fait** — aucun second tag Niumi disponible |
| Scan d'une carte/objet NFC quelconque (sans NDEF URI valide) | **OK** — « Boîtier non reconnu. Réessaie. » affiché mot pour mot, sonnerie maintenue. Résultat `Unreadable` (et non `UnknownBox`) : conforme, la vibration d'erreur n'est déclenchée que pour `UnknownBox`. Un son de détection NFC a été entendu, probablement le bip système HyperOS au contact du tag, indépendant de l'app |
| NFC désactivé pendant la sonnerie | **OK** — « Le NFC est désactivé. Active-le pour scanner ton boîtier. » affiché mot pour mot, bouton « Ouvrir les réglages NFC » présent |
| Bouton « Ouvrir les réglages NFC » | **OK** — ouvre les réglages système sans arrêter la sonnerie |
| NFC réactivé pendant la sonnerie, scan du bon tag | **OK** — Reader Mode repris correctement, sonnerie arrêtée (§20) |

## Incertitudes restantes

- **Un seul essai non fait** : scan d'un second tag Niumi associé à un autre boîtier
  (`UnknownBox`, vibration d'erreur, « Ce boîtier n'est pas celui de ta session. »). Nécessite un
  second boîtier/tag NFC inscriptible avec un payload Niumi valide mais un `boxId` différent ;
  aucun disponible pendant cette session. Reste à faire avant de considérer la matrice §20
  complètement couverte pour ce composant.
- **Un seul appareil, une seule marque.** La restriction NFC-verrouillé confirmée ici est
  spécifique à HyperOS ; à reconfirmer sur Pixel et Samsung à la porte de validation 0
  (étape 6), qui pourraient ne pas avoir cette restriction.
- **Textes non imposés par la spec** (matériel absent, NFC désactivé, boîtier inconnu, libellé
  du bouton réglages) : rédactions du plan, à confirmer avec une revue produit.
- **Navigation automatique intra-app vers `AlarmActivity`** : limitation connue, différée à
  l'étape 17-18 (voir section dédiée ci-dessus).
- **Écarts déjà connus, inchangés** : receiver ↔ moteur commun (§10.1, levé à l'étape 17),
  scan ↔ `RELEASING`/`COMPLETED` (§11.3, levé aux étapes 17-18).

## Terminé quand (statut)

- code conforme aux spécifications applicables : oui, avec les écarts assumés listés ci-dessus,
  déjà connus ou documentés dans ce rapport ;
- tests pertinents passent : oui, automatisés (JVM et instrumentés, 6/6) et essais manuels
  d'association et d'arrêt par scan ;
- erreurs et avertissements nouveaux traités : oui (ktlint, detekt, lint verts, aucun nouvel
  avertissement de compilation introduit) ; un bug trouvé en validation manuelle (`AlarmActivity`
  ne se fermait pas après un scan accepté) a été corrigé et revérifié ;
- specs mises à jour si le comportement a changé : plan MVP mis à jour (§ « Interfaces
  transverses », cases de l'étape 4) ; aucune des specs produit/métier n'a changé de contrat ;
- validations matérielles signalées : **presque complètes** — tous les essais manuels de la
  matrice §20 pertinents pour cette étape sont passés avec succès, à l'exception du scan d'un
  second tag Niumi associé à un autre boîtier (`UnknownBox`), faute de second tag disponible
  pendant cette session (voir « Résultats de la validation sur appareil » et « Incertitudes
  restantes »). Un seul appareil/une seule marque testés : Pixel et Samsung restent à couvrir à
  la porte de validation 0 (étape 6), notamment pour confirmer ou infirmer la restriction
  NFC-verrouillé observée sur ce HyperOS.
