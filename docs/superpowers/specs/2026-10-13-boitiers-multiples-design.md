# Boîtiers multiples — conception

Date : 2026-10-13. Décisions prises avec l'utilisateur le même jour, dans l'ordre : association
à plusieurs boîtiers (et non « n'importe quel boîtier Niumi ») ; liste figée à l'activation ;
cinq boîtiers au maximum ; un nom libre par boîtier ; règle « l'un des boîtiers suffit » dans
le contrat commun (A1) ; table enfant `session_box` pour figer la liste (B1).

## 1. Problème et intention

Aujourd'hui, un seul boîtier est associé, figé dans la session à l'activation, et seul son
scan termine ou annule la session (SPEC_CORE_KMP §2, décisions 3 et 11). L'utilisateur veut
pouvoir enregistrer plusieurs boîtiers — un par pièce où il veut être conduit le matin — et
terminer sa session en scannant n'importe lequel d'entre eux.

L'intention produit ne change pas : le boîtier reste « le point d'arrivée du réveil »
(`docs/OBJECTIF_ET_INTERET_PRODUIT.md`). Ce qui change, c'est qu'il peut y en avoir plusieurs.

**Ce que l'on accepte en le décidant.** Un second boîtier gardé près du lit permet de
contourner le réveil. La spec admet déjà qu'un tag NDEF est clonable (SPEC_CORE_KMP §9.2) ; la
vérification n'a jamais été une protection contre l'utilisateur lui-même, seulement contre un
tag étranger. Rien n'est perdu que le produit garantissait.

## 2. Ce qui ne change pas

- Le format du payload et le parseur (SPEC_CORE_KMP §9.1, §9.3).
- `NfcVerificationProof` : déjà liée à une session, un événement et une révision, elle porte
  le `boxId` reconnu ; le moteur ne la compare jamais à un boîtier du snapshot.
- La machine à états et le snapshot commun (§5, §7.1) : le boîtier est une donnée de
  plateforme, comme la sonnerie ; il n'est pas dans `SessionSnapshot`.
- L'interdiction de modifier les boîtiers pendant une session (décision 11) : elle s'applique à
  la liste entière.
- Les textes de scan des écrans 7, 8, 9 et 11 (« Scanne ton boîtier Niumi… ») : vrais avec
  plusieurs boîtiers, ils ne sont pas touchés.
- Le comportement face à un tag Niumi non associé (vibration d'erreur, alarme maintenue) et à
  un tag illisible.

## 3. Contrat commun KMP — version 1.4

### 3.1 Vérificateur

```kotlin
object BoxVerifier {
    fun verify(
        payload: BoxPayload,
        credentials: List<PairedBoxCredential>,
        context: NfcVerificationContext?,
    ): BoxVerificationResult
}

enum class BoxVerificationStatus { MATCH, NO_PAIRED_BOX_MATCH, UNSUPPORTED_VERSION }
```

- `UNSUPPORTED_VERSION` si la version du payload n'est pas prise en charge (inchangé).
- `MATCH` au premier boîtier de la liste dont le `boxId` est égal **et** dont l'empreinte du
  token correspond (comparaison en temps constant, inchangée). La preuve, si un contexte est
  fourni, porte le `boxId` de ce boîtier.
- `NO_PAIRED_BOX_MATCH` sinon, liste vide comprise. Il remplace `BOX_MISMATCH` et
  `TOKEN_MISMATCH` : distinguer « bon identifiant, mauvais token » n'avait de sens que face à un
  boîtier unique, et c'est une information de moins qui sort du module.
- Aucune exception ne traverse la frontière (§14) : une empreinte non canonique dans la liste
  est un boîtier qui ne correspond pas, pas un crash.

### 3.2 Façade

`verifyBox(payload, credentials: List<PairedBoxCredentialDto>, context)`. L'association ne
l'appelle plus : elle parse (`parseBoxPayload`), dérive le credential
(`PairedBoxCredentialDto.fromPayload`) et déduplique par `boxId` côté dépôt. Un scan de fin de
session passe la liste figée dans la session et un contexte non nul.

### 3.3 Politique d'activation

`ActivationPolicyInput.pairedBoxCount: Int` remplace `hasPairedBox`. Constante commune
`PairedBoxLimit` : `MIN = 1`, `MAX = 5`, sur le modèle d'`AppSelectionSummary` (1–50).

- `pairedBoxCount < 1` → `NO_PAIRED_BOX` (code existant) ;
- `pairedBoxCount > 5` → `TOO_MANY_PAIRED_BOXES` (nouveau code).

### 3.4 Tests communs (§17)

- `BoxVerifierTest` : liste vide ; un boîtier (correspond, ne correspond pas) ; cinq boîtiers
  avec correspondance en première, en dernière et en aucune position ; `boxId` connu avec
  mauvais token → `NO_PAIRED_BOX_MATCH` ; empreinte non canonique dans la liste → ignorée sans
  exception ; preuve portant le `boxId` reconnu ; version non prise en charge.
- `ActivationPolicyTest` : 0, 1, 5, 6 boîtiers.
- Fixtures `nfc_payloads.json` : au moins deux boîtiers distincts valides.

### 3.5 Version

Contrat 1.3 → 1.4. SPEC_CORE_KMP : §2 (décisions 3 et 11 reformulées : « l'un des boîtiers
associés », « les boîtiers figés à l'activation »), §9.2, §10, §14, §17. Le `schemaVersion` du
snapshot commun ne bouge pas (rien n'y change).

## 4. Persistance Android

### 4.1 Dépôt des boîtiers associés

`paired_box` : `+ name TEXT NOT NULL`, `+ position INTEGER NOT NULL`. `PairedBoxStore` :

```kotlin
interface PairedBoxStore {
    suspend fun all(): List<PairedBoxDto>          // (credential, name), par position
    suspend fun add(credential: PairedBoxCredentialDto, name: String): PairedBoxAddResult
    suspend fun rename(boxId: String, name: String)
    suspend fun remove(boxId: String)
}
sealed interface PairedBoxAddResult { Added, AlreadyPaired, LimitReached }
```

Le refus d'un doublon de `boxId` et d'un sixième boîtier se fait **au dépôt**, en résultat
typé ; l'écran ne fait que l'afficher. La garde de déverrouillage de `RoomPairedBoxStore.current()`
(SPEC_ANDROID §7.3, exception documentée) est reportée sur `all()` : liste vide sans ouvrir la
base avant déverrouillage, mêmes raisons.

Noms : 1 à 30 caractères après `trim()`, vide refusé ; valeur proposée à l'ajout « Boîtier {n} »
où `n` est le nombre de boîtiers déjà associés plus un.

### 4.2 Liste figée dans la session

Table `session_box`, sur le modèle exact de `blocked_app` :

```kotlin
@Entity(tableName = "session_box", primaryKeys = ["sessionId", "boxId"],
        foreignKeys = [ForeignKey(AlarmSessionEntity, ["id"], ["sessionId"], CASCADE)],
        indices = [Index("sessionId")])
data class SessionBoxEntity(sessionId: String, boxId: String, tokenSha256: String,
                            name: String, position: Int)
```

`AndroidSessionExtras.boxes: List<SessionBox(boxId, tokenSha256Hex, name)>` remplace `boxId`
et `boxTokenSha256Hex`. `ArmSessionUseCase` copie `pairedBoxStore.all()` dans les extras à
`ACTIVATION_REQUESTED` ; `freezeFrom` fige la liste comme la sonnerie : la session persistée
fait foi, jamais l'appelant. `HandleValidNfcUseCase` passe `extras.boxes` au vérificateur.

### 4.3 Migration Room v4 → v5

`MIGRATION_4_5`, dans cet ordre :

1. `ALTER TABLE paired_box ADD COLUMN name TEXT NOT NULL DEFAULT ''` puis
   `UPDATE paired_box SET name = 'Boîtier 1'` ; `ADD COLUMN position INTEGER NOT NULL DEFAULT 0`.
2. `CREATE TABLE session_box (...)` avec sa clé étrangère et son index.
3. `INSERT INTO session_box SELECT id, boxId, boxTokenSha256Hex, 'Boîtier 1', 0 FROM alarm_session`.
4. Recréation d'`alarm_session` sans `boxId` ni `boxTokenSha256Hex` (SQLite ne retire pas une
   colonne autrement) : `CREATE TABLE alarm_session_new`, `INSERT … SELECT`, `DROP TABLE
   alarm_session`, `ALTER TABLE … RENAME`. Les six tables enfants (`session_box` comprise)
   portent un `CASCADE` sur `alarm_session` : la recréation se fait pendant que les clés
   étrangères sont inactives (*constaté à l'étape 30 : `PRAGMA foreign_keys` est sans effet dans
   la transaction d'une migration Room ; c'est Room qui ne les active qu'après les migrations —
   SPEC_ANDROID §7.2 « v5 » décrit le mécanisme réel et le `PRAGMA foreign_key_check` final*), et
   `NiumiDatabaseSchemaTest` prouve depuis une base v4 peuplée d'une session `ARMED` avec reçu,
   outbox, application, incident, pointeur actif et boîtier que rien n'est perdu et que
   `session_box` contient la paire d'origine.

`schemas/5.json` committé. Aucun `fallbackToDestructiveMigration`.

### 4.4 Projection Direct Boot v4

`DirectBootSnapshot.Active.boxes: List<DirectBootBox(boxId, tokenSha256Hex)>` remplace les deux
champs. `DIRECT_BOOT_PROJECTION_SCHEMA_VERSION = 4` ; seuil figé
`PROJECTION_VERSION_WITH_MULTIPLE_BOXES = 4` (jamais « < version courante », même règle que les
deux seuils existants) : un fichier v3 se lit comme une liste d'un seul boîtier. Sans les noms :
la projection sert la vérification avant déverrouillage, et `DirectBootRoomMerge` conserve les
extras de Room, noms compris. `DirectBootSnapshotJsonTest` relit un vrai fichier v3.

### 4.5 Export du diagnostic (§17)

La ligne « Boîtier » devient une ligne par boîtier figé : nom et préfixe de huit caractères du
`boxId`. L'empreinte ne transite toujours pas par le modèle d'export.

## 5. Interface Android

### 5.1 Écran 3 — « Mes boîtiers »

- Liste des boîtiers associés, par position : nom, préfixe de huit caractères.
- Par ligne : « Renommer » (champ, mêmes règles qu'à l'ajout), « Retirer » (sans confirmation :
  retirer le dernier est permis hors session, le diagnostic refusera simplement l'activation).
- « Ajouter un boîtier » : ouvre le Reader Mode avec l'instruction actuelle ; masqué dès cinq
  boîtiers, remplacé par « 5 boîtiers maximum. ».
- Payload valide, boîtier nouveau → champ de nom prérempli « Boîtier {n} » → « Boîtier ajouté. ».
- Payload valide, boîtier déjà présent → « Ce boîtier est déjà associé. », rien n'est écrit.
- Payload non Niumi, tag illisible, NFC absent ou désactivé : textes actuels inchangés.
- Session en cours : liste visible, toute action désactivée, « Une session est en cours. Tes
  boîtiers ne peuvent pas être modifiés avant sa fin. » Le dialogue « Remplacer le boîtier
  actuel ? » disparaît avec son explication.

### 5.2 Diagnostic (écran 2, §13)

Contrôle `PAIRED_BOX` : titre « Boîtiers associés » ; échec « Aucun boîtier n'est associé.
Associe au moins un boîtier Niumi : c'est l'un d'eux qui terminera ta session. », action
« Associer un boîtier » ; satisfait « {n} boîtier(s) associé(s) » (« 1 boîtier associé », « 3
boîtiers associés »), action secondaire « Gérer mes boîtiers » (remplace « Changer de boîtier »).
`AndroidDeviceReadinessChecker` fournit `pairedBoxCount = pairedBoxStore.all().size`.

### 5.3 Écran 6 — récapitulatif

La ligne boîtier liste les noms (« Salle de bain, Cuisine »). Rappel d'engagement : « Seul le
scan d'un de tes boîtiers terminera la session. » (était « du boîtier »).

### 5.4 SPEC_IOS (spec seulement)

Keychain : liste de (`boxId`, empreinte, nom), cinq au maximum ; `NiumiSessionRecord` fige la
liste ; §14 association en liste ; « mauvais tag » → « Ce boîtier n'est pas associé à cet
iPhone. » ; verdict d'activation par `pairedBoxCount`.

## 6. Périmètre et limites

Hors périmètre : partage de boîtiers entre appareils ; ajout ou retrait pendant une session
(interdit, comme aujourd'hui) ; anti-clonage (§9.2 inchangé) ; renommage pendant une session.

Limite à écrire dans l'aide et `LIMITES.md`, **après mesure sur appareil** : un boîtier ajouté
après l'activation ne termine pas la session en cours ; un boîtier retiré hors session ne
termine plus rien.

## 7. Spécifications à mettre à jour dans le même changement

- SPEC_CORE_KMP : §2 (3, 11), §9.2, §10, §14, §17 ; version 1.4.
- SPEC_ANDROID : §3, §7.2 (v5), §7.3 (projection v4), §11.1 (plusieurs boîtiers, maximum,
  noms), §11.2 (`NO_PAIRED_BOX_MATCH`), §13, §15 (écrans 3 et 6), §17 (export), §19, §21.
- SPEC_IOS : §14, Keychain, verdict d'activation.
- Plan maître : nouveau Lot 8, étapes produites par `writing-plans`.

## 8. Vérification de bout en bout

- Tests communs (§3.4), tests JVM Android (mapping Room et Direct Boot, `freezeFrom`,
  `HandleValidNfcUseCase` avec liste, `ArmSessionUseCase`, écran 3, diagnostic, écran 6, export).
- Instrumentés : migration 4→5 depuis une base v4 peuplée ; `RoomPairedBoxStore` (ajout,
  doublon, sixième, renommage, retrait, ordre) ; parité Room / Direct Boot.
- Sur appareil (campagne, §20) : deux boîtiers associés, session armée, scan du second → session
  terminée ; boîtier ajouté après l'activation → refusé pendant la session ; mise à jour par-dessus
  une session armée en v4 → la session se termine avec son boîtier d'origine ; redémarrage sans
  déverrouillage avec une projection v3 → le boîtier d'origine termine la session.
