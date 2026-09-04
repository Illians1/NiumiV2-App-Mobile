# Étape 2 — KMP : protocole NFC et façade partielle

Date : 2026-09-04.

## Résumé

Premier bloc métier réel de `:shared:core` : parseur strict du payload NFC canonique
(SPEC_CORE_KMP §9.1), SHA-256 et comparaison à temps constant en Kotlin pur, credential associé,
vérificateur (`BoxVerifier`) et preuve opaque (`NfcVerificationProof`), plus la façade partielle
`NiumiCoreFacade` (`parseBoxPayload`, `verifyBox`). Aucune règle métier n'existait avant cette
étape ; aucune API Android ou Apple n'est importée par `:shared:core`. `specs/SPEC_ANDROID.md`
§22 est corrigée pour refléter l'ordre réel (protocole NFC avant le Lot 0).

## Décisions prises

1. **Enums à la frontière interop réutilisés, non dupliqués.** `com.niumi.core.interop` définit
   des data classes `*Dto` (`BoxPayloadDto`, `BoxPayloadResultDto`, `PairedBoxCredentialDto`,
   `NfcVerificationContextDto`, `BoxVerificationResultDto`) mais réutilise directement
   `BoxPayloadStatus` et `BoxVerificationStatus` : ce sont déjà des enums stables sans type de
   plateforme (SPEC_CORE_KMP §14). Validé avec l'utilisateur avant implémentation ; vaut pour les
   DTO des étapes suivantes de la façade (moteur d'états, effets, incidents…).
2. **Correction du plan MVP — bits de bourrage Base64.** Le plan fixait le jeu de derniers
   caractères valides d'un token à `AEIMQUYcgkosw048` (multiples de 4 → 2 bits de bourrage).
   C'est faux : 22 caractères Base64 encodent 132 bits, dont 128 utiles (16 octets) — il reste
   donc **4 bits de bourrage** sur le dernier caractère, pas 2. Seules les valeurs 0/16/32/48 de
   l'alphabet Base64 URL (`A`, `Q`, `g`, `w`) ont ces 4 bits nuls ; le jeu du plan aurait accepté
   plusieurs encodages Base64 distincts pour un même token de 16 octets. Implémenté avec `AQgw`,
   plan corrigé, testé (`tokenWithNonZeroPaddingBitsIsInvalid`, fixture dédiée). SPEC_CORE_KMP
   §9.1 n'a pas été modifiée : elle n'énonce que « 16 octets, Base64 URL sans padding », sans
   fixer la forme du contrôle.
3. **`kotlin.io.encoding.Base64` est stable, pas expérimental.** Vérifié dans le bytecode de
   `kotlin-stdlib` 2.4.10 (`Base64` et `Base64.PaddingOption` ne portent que `@WasExperimental` /
   `@SinceKotlin`, jamais `@ExperimentalEncodingApi` actif) avant de l'utiliser sans opt-in.
4. **`NfcVerificationProof` — limite du test de portée du constructeur.** Le plan demandait un
   « test de compilation par `internal` ». Impossible à écrire : `commonTest` est un *friend
   module* de `commonMain` en Kotlin, donc `internal` y est visible par construction, comme
   partout ailleurs dans `commonMain`. `NfcVerificationProofTest` vérifie ce qui est vérifiable
   par un test (masquage du `boxId` dans `toString()`) ; l'inaccessibilité réelle depuis
   `:core:system` ou tout autre module Android/iOS reste garantie par le mot-clé `internal`
   lui-même, pas par ce test. Documenté en commentaire dans le fichier de test.
5. **Ordre de validation du parseur, figé pour cette étape** (non fixé par SPEC_CORE_KMP §9.3,
   qui ne liste que l'ensemble des statuts) :

   | Ordre | Contrôle | Statut |
   | --- | --- | --- |
   | 1 | > 96 octets UTF-8 | `PAYLOAD_TOO_LONG` |
   | 2 | vide, octet nul, caractère de contrôle, surrogate isolé | `MALFORMED_URI` |
   | 3 | présence d'un `%` | `UNEXPECTED_COMPONENT` |
   | 4 | pas de `://`, schéma vide | `MALFORMED_URI` |
   | 5 | schéma ≠ `niumi` | `UNSUPPORTED_SCHEME` |
   | 6 | fragment (`#`) présent | `UNEXPECTED_COMPONENT` |
   | 7 | autorité avec `@`/`:` | `UNEXPECTED_COMPONENT` |
   | 8 | autorité ≠ `box` | `UNSUPPORTED_HOST` |
   | 9 | premier segment ≠ `v1` | `UNSUPPORTED_VERSION` |
   | 10 | plus de 2 segments | `UNEXPECTED_COMPONENT` |
   | 11 | `boxId` non canonique | `INVALID_BOX_ID` |
   | 12 | query absente/vide | `MISSING_TOKEN` |
   | 13 | plus d'un paramètre, ou clé ≠ `token` | `UNEXPECTED_COMPONENT` |
   | 14 | token invalide (longueur, alphabet, bourrage, décodage, taille décodée) | `INVALID_TOKEN` |

6. **Exception de configuration detekt `ReturnCount`.** `BoxPayloadParser.parse`,
   `isCanonicalBoxId` et `decodeToken` sont des validateurs à clauses de garde (guard clauses)
   avec plus de deux points de sortie : c'est la forme la plus lisible pour une chaîne de
   contrôles séquentiels dont le premier échec détermine le résultat (14 contrôles pour `parse`
   seul). `ReturnCount(max=2)` par défaut ne convient pas à ce style ; suppression locale
   `@Suppress("ReturnCount")` sur ces trois fonctions, documentée en commentaire à chaque endroit.
   Toutes les autres fonctions du fichier respectent la limite par défaut (souvent en écrivant le
   corps comme une unique expression `when`, qui ne compte aucun `return` explicite).
   `@Suppress("MagicNumber")` posé au niveau de l'objet `Sha256` : les décalages de bits et
   masques d'octet sont les constantes de la norme FIPS 180-4 elle-même, les nommer n'ajouterait
   aucune clarté. Les magic numbers de `BoxVerifier.hexToBytes` (radix hexadécimal, décalage de
   nibble), en revanche, ont été nommés (`HEX_RADIX`, `NIBBLE_BITS`) plutôt que supprimés : ce
   n'était pas justifié de la même façon.
7. **Correction d'un trou de configuration detekt affectant tout le dépôt, pas seulement cette
   étape.** En vérifiant la couverture réelle de `./gradlew detekt` sur `:shared:core`, la tâche
   agrégée `detekt` s'est révélée **NO-SOURCE** pour ce module : `dev.detekt` crée une tâche par
   source set sur un module Kotlin Multiplatform (`detektCommonMainSourceSet`,
   `detektJvmTestSourceSet`…) mais la tâche `detekt` elle-même n'analyse aucun de ces source
   sets. En corrigeant `build.gradle.kts` pour câbler `detekt` sur toutes les tâches `Detekt` du
   même projet, un second trou pré-existant est apparu sur `:app` : les tâches par variante
   Android (`detektDebug`, `detektRelease`) n'étaient pas non plus rattachées à `detekt`, et
   `detektDebug` a immédiatement révélé un vrai problème resté invisible depuis l'étape 1 : la
   fonction `HomeScreenPreview` de `MainActivity.kt` (créée à l'étape 1) est signalée
   `UnusedPrivateFunction` — faux positif classique pour une fonction `@Preview`, qui n'est
   appelée que par l'outillage Android Studio. Corrigé par `ignoreAnnotated: ['Preview']` dans
   `config/detekt/detekt.yml`, sur le même modèle que l'exception déjà posée pour
   `FunctionNaming`/`@Composable`. **Conséquence pour l'étape 1** : sa case « `./gradlew ktlintCheck detekt :app:lintDebug` : BUILD SUCCESSFUL » restait correcte en tant que
   sortie Gradle, mais `detekt` n'avait jamais réellement analysé `:app:detektDebug`,
   `:app:detektRelease` ni aucun source set de `:shared:core` avant ce correctif. Après
   correction, `./gradlew detekt` est de nouveau vert sur les huit modules avec une couverture
   réelle vérifiée manuellement (voir « Commandes exécutées »).

## Fichiers créés

`shared/core/src/commonMain/kotlin/com/niumi/core/nfc/` : `Sha256.kt`, `ConstantTime.kt`,
`BoxPayload.kt`, `BoxPayloadResult.kt`, `BoxPayloadParser.kt`, `PairedBoxCredential.kt`,
`NfcVerificationContext.kt`, `NfcVerificationProof.kt`, `BoxVerificationResult.kt`,
`BoxVerifier.kt`.

`shared/core/src/commonMain/kotlin/com/niumi/core/interop/` : `NfcDtos.kt`,
`NiumiCoreFacade.kt`.

`shared/core/src/commonTest/kotlin/com/niumi/core/nfc/` : `Sha256Test.kt`, `ConstantTimeTest.kt`,
`BoxPayloadParserTest.kt`, `BoxVerifierTest.kt`, `NfcVerificationProofTest.kt`,
`BoxPayloadFuzzTest.kt`.

`shared/core/src/commonTest/kotlin/com/niumi/core/interop/NiumiCoreFacadeNfcTest.kt`.

`shared/core/src/commonTest/resources/fixtures/nfc_payloads.json` (29 cas, 10 statuts couverts).

`shared/core/src/jvmTest/kotlin/com/niumi/core/nfc/NfcFixturesTest.kt`.

## Fichiers modifiés

- `build.gradle.kts` : câblage de `detekt` sur les tâches `Detekt` du même projet (décision 7).
- `config/detekt/detekt.yml` : exception `UnusedPrivateFunction`/`@Preview` (décision 7).
- `specs/SPEC_ANDROID.md` §22 : Lot 0 (arrêt du service validé par `:shared:core`) et Lot 0.5
  (protocole NFC livré en amont, motif explicité).
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : cases de l'étape 2 cochées, ligne « bits
  de bourrage » corrigée (décision 2).

## Commandes exécutées et résultat

Environnement : voir `ETAPE-01.md` § « Comment builder » (JDK et SDK exportés à chaque appel).

| Commande | Résultat |
| --- | --- |
| `./gradlew :shared:core:jvmTest` | BUILD SUCCESSFUL — 53 tests, 0 échec, 0 erreur (vérifié dans les 9 rapports XML, pas seulement au résumé Gradle) |
| `./gradlew :shared:core:linkDebugFrameworkIosSimulatorArm64` | BUILD SUCCESSFUL (Xcode 26.6) |
| `./gradlew ktlintCheck` (racine) | BUILD SUCCESSFUL sur les huit modules, après un `ktlintFormat` ciblé sur `:shared:core` (retours à la ligne de paramètres) |
| `./gradlew detekt` (racine) | BUILD SUCCESSFUL sur les huit modules, après les deux corrections de la décision 7 et les corrections de code ci-dessous |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest` | BUILD SUCCESSFUL — non-régression sur `:app` après modification de fichiers racine |

Répartition des 53 tests JVM : `SmokeTest` (1), `Sha256Test` (4), `ConstantTimeTest` (4),
`BoxPayloadParserTest` (31), `BoxVerifierTest` (5), `NfcVerificationProofTest` (1),
`BoxPayloadFuzzTest` (1, 200 itérations internes), `NfcFixturesTest` (1, 29 fixtures rejouées),
`NiumiCoreFacadeNfcTest` (5).

Corrections de code nécessaires pour faire passer `detekt` sur le nouveau code (au-delà des
suppressions documentées en décision 6) : découpage de `BoxPayloadParser.parse` en fonctions
dédiées par étape (`checkPreconditions`, `extractAfterScheme`, `splitPathAndQuery`,
`checkAuthority`, `checkVersionSegment`, `extractTokenValue`) pour rester sous les seuils
`LongMethod`/`CyclomaticComplexMethod`/`MaxLineLength` ; déplacement de deux fonctions
d'extension `Char` hors de l'objet `BoxPayloadParser` (`TooManyFunctions`, max 11 par objet).

Contrôles complémentaires :

- `grep -rniE "println|System\.out|Log\." shared/core/src/{commonMain,commonTest,jvmTest}` → aucun résultat ;
- `grep -rn "^import android\.\|^import java\.\|^import platform\."
  shared/core/src/commonMain` → aucun résultat (`kotlin.io.encoding` est de la stdlib commune,
  pas de la JVM).

Aucun test instrumenté ni validation sur appareil réel à cette étape : `:shared:core` ne touche
à aucun matériel. La première validation NFC physique reste celle de l'étape 4.

## Tests non exécutés / non applicables à cette étape

Aucun. Les deux commandes conditionnelles du plan (`jvmTest`, `linkDebugFrameworkIosSimulatorArm64`)
ont toutes deux pu s'exécuter (Xcode disponible).

## Incertitudes restantes

- **Ordre de validation du parseur** (décision 5) est un choix de cette étape, pas une exigence
  de spec ; SPEC_CORE_KMP §9.3 ne fixe que l'ensemble des statuts possibles. Si une étape
  ultérieure (natif Android/iOS) observe un besoin d'ordre différent pour l'UX de scan, revenir
  ici plutôt que dupliquer une règle de validité côté plateforme.
- **`dev.detekt` 2.0.0-alpha.6** reste une alpha (voir `ETAPE-01.md`) ; le comportement
  NO-SOURCE sur les tâches agrégées KMP/variantes Android corrigé en décision 7 pourrait changer
  ou être résolu nativement dans une version ultérieure du plugin — à revérifier avant de retirer
  le câblage manuel ajouté à `build.gradle.kts`.
- **`NfcVerificationProofTest`** ne peut pas prouver par un test d'exécution que le constructeur
  `internal` est inaccessible depuis un autre module Gradle (décision 4) ; cette garantie repose
  sur la lecture du code, pas sur une assertion automatisée.
