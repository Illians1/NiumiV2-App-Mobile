# Étape 13 — association du boîtier et sélecteur d'applications

**Date :** 2026-09-11
**Périmètre :** `:core:database` (dépôt Room du boîtier), `:core:system` (applications installées,
sélection courante), `:feature:setup` (écrans 3 et 4, garde de préparation), `:app` (navigation).
**Specs de référence :** SPEC_CORE_KMP §2 (points 11, 12), §9.2, §9.3, §10 ;
SPEC_ANDROID §7.3, §11.1, §11.2, §12.1, §12.2, §13, §14, §15 (écrans 3 et 4), §16.

## Résumé

Deux des quatorze contrôles de §13 ne pouvaient pas être satisfaits : `PAIRED_BOX` et
`APP_SELECTION` n'avaient ni écran ni stockage de production, et leurs recours étaient affichés
désactivés. Cette étape livre les deux écrans manquants du parcours de préparation, la persistance
Room définitive du boîtier, la source des applications lançables et le dépôt de la sélection
courante. Le parcours accueil → onboarding → diagnostic → association → sélection est désormais
complet ; l'étape 14 pourra armer une session.

## Décisions validées avec l'utilisateur (2026-09-11)

1. **`PairedBoxStore` migre de `:core:system` vers `:core:database`.** Le plan MVP place
   `RoomPairedBoxStore` dans `:core:database`, qui ne peut pas dépendre de `:core:system` (§6).
   L'interface rejoint `com.niumi.database.pairing`, avec les autres interfaces de dépôt
   (`SessionStore`, `DirectBootStore`, `TechnicalEventLog`).
2. **La section `<queries>` vit dans le manifeste de `:core:system`**, avec le seul code qui en
   dépend, plutôt que dans `:app`. Le bloc identique du manifeste `debug` de `:app` est retiré :
   le manifeste fusionné le porte pour tous les variants.
3. **La sélection courante est sérialisée en JSON.** DataStore ne stocke nativement qu'un
   `Set<String>`, insuffisant pour le couple package/libellé exigé par §12.2.
   `kotlinx-serialization-json` est ajouté à `:core:system` — bibliothèque déjà au catalogue et
   déjà utilisée par `:core:database` ; pas le plugin, un `MapSerializer` suffit.

## Deux défauts trouvés sur appareil et corrigés dans le même changement

Aucun test automatisé ne pouvait les attraper : tous deux naissent d'un enchaînement d'écrans que
seul un parcours réel produit.

1. **Les écrans 3 et 4 devenaient définitivement inatteignables.** Le diagnostic ne donne un bouton
   qu'au premier contrôle **en échec** (`primary = items.firstOrNull { outcome == FAILED }`). Une
   fois le boîtier associé et les applications choisies, ces deux lignes passaient au vert et
   perdaient leur bouton — or c'était le seul chemin vers les deux écrans. L'utilisateur ne pouvait
   donc plus jamais changer de boîtier ni modifier sa sélection, alors que §11.1 impose que « toute
   nouvelle association remplace l'ancienne après confirmation » : le comportement était implémenté
   et testé, mais sans porte d'entrée. **Correction** (validée avec l'utilisateur) : les deux
   contrôles de *parcours* gardent une action secondaire une fois satisfaits, avec un libellé
   distinct — « Changer de boîtier », « Modifier ma sélection » ; les contrôles de *blocage* n'en
   gagnent aucune. La distinction existait déjà dans le code depuis l'étape 12a (`journeyChecks()`
   séparé, routé vers les champs dédiés d'`ActivationPolicyInputDto`). §13 gagne un alinéa.
   Couvert par 4 tests JVM et 2 tests instrumentés.
2. **Un message de scan périmé survivait à la perte du lecteur.** Après un tag illisible, couper le
   NFC laissait afficher « Réessaie en approchant le boîtier plus lentement » sous « Le NFC est
   désactivé » — un conseil inapplicable, donc un faux état (§15). **Correction** : perdre le
   lecteur efface le retour du scan précédent ; « Boîtier associé. » survit, parce qu'il énonce un
   fait et non une action à refaire. Couvert par 3 tests JVM.

## Écarts au plan MVP

1. **`RoleManager` est inutilisable pour les exclusions de §12.1.** Le plan prévoyait d'exclure le
   rôle Home et l'application d'urgence via `RoleManager`. Or `getRoleHolders()` est `@SystemApi`
   et exige `MANAGE_ROLE_HOLDERS` ; l'API publique `isRoleHeld()` ne renseigne que sur
   l'application appelante. Les exclusions passent donc par des intents publics
   (`CATEGORY_HOME`, `ACTION_SETTINGS`, `TelecomManager.getDefaultDialerPackage()`,
   `ACTION_DIAL`). **L'application d'urgence n'a aucune API publique** : elle n'est exclue que
   dans la mesure où elle coïncide avec le composeur par défaut. Limite inscrite en §12.1.
2. **`RoomPairedBoxStore.current()` ne lève pas avant déverrouillage.** Contrairement à
   `RoomSessionStore`, la lecture du boîtier renvoie « aucun boîtier » sans ouvrir la base :
   `AndroidDeviceReadinessChecker` est rejoué pendant la réconciliation Direct Boot
   (`SessionReconciler.reconcileArmed` → `SessionReadinessMonitor.evaluate`, raison `LOCKED_BOOT`),
   et une levée y interromprait la reprogrammation de l'alarme. Sans effet sur la sûreté :
   `PAIRED_BOX` n'est pas dans `MonitoredReadinessChecks.incidentCodes` (aucun incident faux), et
   SPEC_CORE_KMP §10 impose déjà que la vérification NFC d'une session armée utilise le credential
   figé à l'activation. `replace()` et `clear()` gardent le refus strict. Exception inscrite en §7.3.
3. **`SetupNavigation.kt` n'existe pas** (constat de l'étape 12b : un module `feature` ne peut pas
   dépendre de `:app`). `SetupGate` est une fonction pure appliquée par les `Route` composables,
   qui remontent le refus par une lambda de navigation.
4. **Le POC de debug garde son dépôt sous un qualificatif, pas en classe concrète.** Le plan
   disait d'injecter `DebugPairedBoxStore` directement pour éviter le conflit de binding avec
   `RoomPairedBoxStore`. Un qualificatif `@PocPairedBoxStore` atteint le même but sans faire
   perdre à `PocNfcScanHandlerTest` sa capacité à substituer un faux dépôt — la classe concrète
   exigerait un `Context` Android en test JVM.
5. **`ReadinessSources.pairedBoxStore` n'est plus `Optional`.** Le `@BindsOptionalOf` n'avait de
   sens que tant qu'aucune implémentation de production n'existait ; le test
   `anUnboundPairedBoxStoreCountsAsNoPairedBox`, devenu sans objet, est supprimé.
6. **`SESSION_FINAL_STATES` devient public dans `:core:system`.** L'ensemble était déjà dupliqué
   dans `:app` (`homeDestinationFor`) et `SetupGate` en aurait fait une troisième copie. Une seule
   définition, avec `SessionStateDto?.isSessionInProgress()`.

## Ce qui est livré

### `:core:database` — dépôt du boîtier

`pairing/PairedBoxStore.kt` (interface déplacée), `pairing/RoomPairedBoxStore.kt`,
`mapping/PairedBoxMapper.kt`, `di/PairedBoxStoreModule.kt`. `PairedBoxDao` gagne `current()`
(`SELECT … LIMIT 1`) et `deleteAll()` : `OnConflictStrategy.REPLACE` ne couvre que le
remplacement d'une même clé primaire, or une ré-association change de `boxId`. `replace()` fait
donc `DELETE` puis `INSERT` dans une seule transaction. **Aucun changement de schéma** — pas de
migration, l'`identityHash` de la v1 est intact.

### `:core:system` — applications installées et sélection courante

Nouveau paquet `apps/` : `InstalledApp`, `InstalledAppsSource`, `PackageQuery` (interface fine sur
`PackageManager`), `PackageManagerPackageQuery` (traduction Android), `PackageManagerInstalledAppsSource`
(dédoublonnage, exclusions, tri — logique pure et testable), `AppSelectionCodec`, `AppSelectionStore`
+ `DataStoreAppSelectionStore`, `di/AppsModule`. `EmptyAppSelectionSource` est supprimée :
`AppSelectionSource` est désormais servie par le vrai dépôt.

Le manifeste du module, vide jusqu'ici, porte la section `<queries>`. Un qualificatif
`@IoDispatcher` est ajouté à côté de `@DefaultDispatcher` : la résolution des applications passe
par `PackageManager`, bloquante, et monopoliserait le pool de calcul.

### `:feature:setup` — écrans 3 et 4

`pairing/` : `PairingTexts`, `PairingUiState`, `PairingActions`, `PairingViewModel`,
`PairingScreen` + `PairingRoute`. `apps/` : `AppPickerTexts`, `AppPickerUiState`,
`AppPickerViewModel`, `AppPickerScreen` + `AppPickerRoute`, `AppIconPainter`. `SetupGate.kt` à la
racine.

Le Reader Mode est branché dans `PairingRoute` (ON_RESUME / ON_PAUSE) et non dans le ViewModel :
`enableReaderMode()` exige une `Activity` au premier plan. L'`Activity` traverse le ViewModel sans
jamais y être conservée.

Les icônes sont converties localement (`Drawable` → `ImageBitmap`) : aucune bibliothèque de
chargement d'images n'est au catalogue et aucune n'est ajoutée pour un besoin aussi étroit.

Les bornes 1..50 viennent de `AppSelectionSummary` (`:shared:core`) partout — compteur, état du
bouton, refus de la 51ᵉ : l'écran ne réécrit jamais la règle.

### `:app` — navigation

`NiumiNavHost` enregistre `Pairing` et `AppPicker` (routes déjà déclarées depuis l'étape 12b).
`ReadinessRoute` gagne `onStartPairing` / `onOpenAppPicker` ; `StartPairing` et `OpenAppPicker`
sortent de `UNAVAILABLE_ACTIONS`. `homeDestinationFor` consomme `isSessionInProgress()`.

## Vérifications exécutées (2026-09-11)

| Commande | Résultat |
| --- | --- |
| `:core:system:testDebugUnitTest` | **145 tests verts** (134 avant : +12 nouveaux, −1 supprimé) |
| `:feature:setup:testDebugUnitTest` | **75 tests verts** (37 avant : +38, dont 7 pour les deux défauts trouvés sur appareil) |
| `:app:testDebugUnitTest` | 11 tests verts (non-régression) |
| `:core:database:testDebugUnitTest` | **93 tests verts** (90 avant : +3) |
| `:feature:ringing:testDebugUnitTest` | 21 tests verts (non-régression) |
| `:feature:session:testDebugUnitTest` | 1 test vert (non-régression) |
| `:shared:core:jvmTest` | 160 tests verts (non-régression) |
| `:app:assembleDebug` | vert, graphe Hilt complet résolu (`hiltJavaCompileDebug`) |
| `ktlintCheck`, `detekt` | verts |
| `:app:lintDebug` | vert, aucune remontée |
| compilation `androidTest` des trois modules | verte |

**Manifeste fusionné** (`:app:processDebugManifest`) : aucune permission de visibilité totale des
paquets, exactement les neuf permissions de §14 (plus
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, générée par AndroidX depuis l'étape 12a), et une seule
section `<queries>`, limitée à `ACTION_MAIN` + `CATEGORY_LAUNCHER`.

Le commentaire du manifeste de `:core:system` évite volontairement d'écrire le nom de la
permission interdite, pour qu'un `grep` sur le manifeste fusionné reste une vérification fiable.

Aucun détekt ni ktlint n'a été assoupli : les quatre remontées ont été corrigées sur le fond
(qualificatif `@IoDispatcher` au lieu de `Dispatchers.IO` en dur, `PairingActions` au lieu de six
paramètres, extraction de `drawIntoBitmap`, `if` au lieu d'un `when` à branche vide).

## Validation sur appareil réel (2026-09-11)

**Appareil : Xiaomi 25080RABDG (`lapis_eea`), Android 16 (API 36), build `BP2A.250605.031.A3`.**
118 applications lançables, NFC actif.

### Tests instrumentés

| Module | Tests | Résultat |
| --- | --- | --- |
| `:core:database` | 41 (37 avant + `RoomPairedBoxStoreTest`) | verts |
| `:core:system` | 13 (10 avant + `DataStoreAppSelectionStoreInstrumentedTest`) | verts |
| `:feature:setup` | 36 (17 avant + `PairingScreenTest`, `AppPickerScreenTest`, et 2 pour le premier défaut) | verts |
| `:feature:ringing` | 5 | verts (non-régression) |

### Manifeste sur l'appareil

`dumpsys package com.niumi.app` confirme `queriesIntents=[Intent { act=android.intent.action.MAIN
cat=[android.intent.category.LAUNCHER] }]` et **aucune permission de visibilité totale des
paquets** : la déclaration portée par `:core:system` arrive bien jusqu'au paquet installé.

### Protocole manuel, essai par essai

| Essai | Observé |
| --- | --- |
| 1. Association d'un tag | `boxId` `14bb2dc6-…` et empreinte SHA-256 écrits, **une seule ligne** ; écran affichant `Boîtier associé : 14bb2dc6…` |
| 2. Ré-association | « Remplacer le boîtier actuel ? » avec les deux choix ; base **inchangée** pendant la question (§11.1) |
| 3a. Refus | retour à l'invitation de scan, boîtier d'origine conservé |
| 3b. Confirmation | toujours **une seule ligne** ; `pairedAtEpochMillis` passé de `…577224` à `…903411`, preuve que la transaction `DELETE`+`INSERT` a bien eu lieu |
| 4. Carte NFC étrangère | « Tag illisible. Réessaie en approchant le boîtier plus lentement. » — chemin *tag physiquement illisible* de §11.2 ; aucune écriture |
| 5. NFC désactivé | « Le NFC est désactivé… » + raccourci ouvrant `com.android.settings/.Settings$MiuiNfcActivity` ; plus aucune demande de scan |
| 6. Exclusions §12.1 | ni Niumi, ni Réglages, ni Téléphone, ni lanceur dans la liste. Sur cet appareil, `com.miui.home` et `com.android.systemui` n'ont de toute façon aucune activité de lancement ; les trois exclusions réellement observables sont `com.niumi.app`, `com.android.settings` et `com.google.android.dialer` |
| 6 bis. Libellés en doublon | l'appareil porte **deux applications « YouTube »** (`com.google.android.youtube` et `app.revanced.android.youtube`) : chacune affiche son nom de package en petit texte, les autres lignes non — cas réel, non provoqué |
| 7. Limite haute | 51ᵉ refusée : compteur figé à `50 / 50`, message affiché, case non cochée, confirmation restée active |
| 8. Persistance | 50 couples package/libellé en JSON ; restaurés intacts après `force-stop` et relance |
| 9. Retour au diagnostic | `Boîtier associé` ✓ et `Applications choisies` ✓ |

**Journal technique après le parcours complet :** 3 × `NFC_SCAN_VALID` et 6 × `NFC_SCAN_INVALID`,
**tous avec un `detailsJson` vide**. Aucune occurrence de l'empreinte ni d'un token dans `logcat`
(les seules correspondances trouvées venaient du SDK Braze d'une autre application). §16 vérifié
sur des scans réels, pas seulement sur un faux journal.

**Détail confirmant le choix du JSON :** un libellé de la sélection contient une espace insécable
(`Adobe Scan`), préservée telle quelle par l'aller-retour — un encodage à séparateur maison
aurait demandé un échappement de plus.

### Mesure de performance

Le sélecteur met **~2,6 s** à s'afficher sur cet appareil (118 applications, libellés et icônes
chargés en une passe sur le dispatcher d'entrées-sorties). L'écran annonce ce chargement
explicitement (« Recherche des applications installées… »), donc n'affiche jamais une liste
trompeusement vide. Acceptable en l'état ; si un appareil plus lent rendait l'attente gênante, le
repli serait un chargement paresseux des icônes ligne par ligne, la liste s'affichant d'abord avec
les seuls libellés.

## Validations restantes

- **Le chemin `UNKNOWN_PAYLOAD`** (« Ce tag n'est pas un boîtier Niumi. ») n'a pas été exercé sur
  appareil : il demande un tag portant un NDEF URI valide mais non Niumi. La carte utilisée n'expose
  aucun NDEF et a donc emprunté le chemin *illisible*. Couvert par `PairingViewModelTest`.
- **Le remplacement par un boîtier de `boxId` différent** n'a été vérifié qu'avec le même tag
  rescanné (un seul boîtier disponible) : la transaction est prouvée par l'horodatage, et le cas du
  `boxId` différent par `RoomPairedBoxStoreTest`, exécuté sur cet appareil.
- **La garde `SetupGate` pendant une session active** n'est pas observable à cette étape : aucune
  session ne peut être armée avant l'étape 14. Couverte par `SetupGateTest` sur tous les états de
  `SessionStateDto`, à revalider manuellement à l'étape 14.
- **Point de vigilance de l'étape 12b, toujours ouvert :** sur cet appareil `STREAM_ALARM` a
  `Min: 1`, donc le blocage par volume d'alarme reste non observable.

**Note d'environnement :** MIUI interrompt l'installation des APK de test après quelques
installations successives (`INSTALL_FAILED_USER_RESTRICTED`), sans rapport avec le code. Relancer
la tâche suffit généralement ; sinon, réactiver « Installer via USB » dans les options développeur.

## Intermittence des tests de notification de l'étape 12a, trouvée et corrigée

`AndroidSessionWarningNotifierInstrumentedTest` et `AndroidScanRequestNotifierInstrumentedTest`
échouaient environ une exécution sur trois, avec un test différent à chaque fois
(`presentThenClearRemovesTheNotification`, `twoBrokenControlsProduceTwoDistinctNotifications`,
`aWarningIsDismissibleAndCarriesNeitherFullScreenIntentNorAction`, `clearRemovesOnlyItsOwnWarning`).

**Deux causes, dont une inattendue.**

1. **Lecture immédiate après écriture.** `notify()` et `cancel()` confient le travail à
   `NotificationManagerService`, qui l'empile sur son propre handler et rend la main avant de
   l'avoir appliqué, tandis qu'`activeNotifications` lit l'état déjà appliqué. Corrigé par
   `awaitNotifications` (`androidTest`), un sondage borné à 2 s — le premier tour suffit presque
   toujours, donc aucun ralentissement. Les assertions d'**absence** passent aussi par lui.
   `AndroidScanRequestNotifierInstrumentedTest` ne nettoyait par ailleurs qu'en `@After` : le
   nettoyage est ajouté en `@Before`, et les deux attendent désormais leur effet.
2. **Un résumé de groupe fabriqué par le système.** C'est la cause principale, invisible tant que
   l'échec restait cryptique. Au-delà de trois notifications publiées sans groupe explicite,
   Android en crée un résumé automatique portant `id = 0`, le drapeau `FLAG_GROUP_SUMMARY` et
   **le même canal** ; il survit à l'annulation de ses enfants. `clearAllWithdrawsEveryWarning`
   en publie six, et le test suivant comptait donc le résumé comme un avertissement Niumi —
   d'où `[5, 0]` au lieu de `[5]`. Vérifié sur l'appareil : `dumpsys notification` montre ce
   mécanisme à l'œuvre pour `pkg=android` (`id=0`, `AUTOGROUP_SUMMARY`) et pour
   `com.xiaomi.mi_connect_service`. Les deux tests excluent désormais ce drapeau — Niumi ne publie
   jamais de résumé, donc tout ce qui en porte un vient du système.

**Mesure avant/après**, sur `:core:system:connectedDebugAndroidTest` : environ une exécution sur
trois en échec avant ; **0 échec de test sur 15 exécutions réussies** après. (Les refus
d'installation MIUI, comptés à part, ne sont pas des échecs de test — voir la note d'environnement.)

Aucun code de production touché. Un fichier neuf (`NotificationAwait.kt`) et les deux classes de
test modifiées.

**Observation connexe, sans correction.** En production aussi, quatre avertissements simultanés ou
plus seront regroupés par le système sous un résumé. §13.1 reste satisfaite — chaque contrôle cassé
produit bien sa propre notification, et le résumé n'est qu'une présentation du volet système. Si
Niumi voulait maîtriser cet affichage, il faudrait publier un groupe explicite et son propre
résumé ; c'est une décision produit, pas un défaut.

Par ailleurs, `AndroidScanRequestNotifier.clear()` et `AndroidSessionWarningNotifier.clear()` lisent
`activeNotifications` pour trancher entre `Success` et `AlreadySatisfied` : ils subissent la même
course. En production c'est sans conséquence — les appels du coordinateur sont séparés de plusieurs
secondes, et les deux verdicts sont des non-échecs qui n'alimentent que le statut de l'effet dans
l'outbox. Corriger demanderait une source de vérité autre que le système, pour un gain nul.
