# Ce qui reste à faire avant de publier Niumi

Document de suivi ouvert à la fin de l'étape 21, le 2026-09-15. Il rassemble **tout ce qui sépare
le MVP d'une publication**, et rien d'autre : le code est livré et vérifié, ces tâches ne sont pas
des correctifs mais des validations qui n'ont pas pu être faites.

Chaque tâche indique pourquoi elle compte, qui peut la faire, comment, et à quoi on reconnaît
qu'elle est finie. Elles sont classées par ordre de dépendance : la 1 débloque la 2, et le groupe A
conditionne la soumission.

**Ne rien cocher sans preuve.** Une tâche cochée doit renvoyer à une mesure consignée dans
`QA_MATRIX.md`, `RELEASE_REPORT.md` ou un rapport d'étape. C'est la règle qui a tenu pendant les
vingt et une étapes ; elle vaut surtout pour la dernière.

---

## A. Bloquant pour la publication

### A1. Créer la clé de signature

- [ ] **Fait**

**Pourquoi.** Sans elle, aucune version de publication ne peut être installée sur un téléphone ni
envoyée à Google Play. C'est aussi ce qui bloque la tâche A2.

**Qui.** L'utilisateur, parce que la clé est protégée par un mot de passe qui lui appartient.

**Comment.**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
mkdir -p ~/Niumi-signing
"$JAVA_HOME/bin/keytool" -genkeypair -v \
  -keystore ~/Niumi-signing/niumi-upload.jks -storetype PKCS12 \
  -alias niumi-upload -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Niumi, O=Niumi, C=FR"
```

Puis créer `keystore.properties` à la racine du dépôt. Le fichier est déjà dans `.gitignore`, il ne
partira jamais sur GitHub :

```properties
storeFile=/Users/<toi>/Niumi-signing/niumi-upload.jks
storePassword=<le mot de passe choisi>
keyAlias=niumi-upload
keyPassword=<le même mot de passe>
```

En format PKCS12, `keytool` ignore un mot de passe de clé différent de celui du magasin : les deux
doivent donc être identiques.

**À savoir avant de commencer.** Ce fichier et son mot de passe doivent être sauvegardés ailleurs
que sur le poste. Une fois la première version envoyée à Google Play, cette clé devient la clé
d'upload du compte ; la perdre impose une procédure de réinitialisation auprès de Google. Tant que
rien n'a été envoyé, elle reste librement remplaçable.

**Fini quand** `./gradlew :app:assembleRelease` produit `app-release.apk` au lieu de
`app-release-unsigned.apk`, et que `apksigner verify --print-certs` affiche le certificat.

### A2. Dérouler une session complète sur une version de publication

- [ ] **Fait**

**Pourquoi.** Les vingt et une étapes ont toutes été validées sur la variante de développement. La
version de publication passe par R8, qui renomme et supprime du code, et par la suppression des
ressources inutilisées. Le build passe et les règles fournies par Hilt, Room et
kotlinx-serialization sont bien appliquées, mais **rien ne le prouve en fonctionnement**. Le risque
nommé est la relecture du fichier d'état écrit avant le premier déverrouillage.

**Qui.** L'utilisateur pour les gestes physiques, l'agent pour le pilotage et les relevés.
Dépend de A1. Compter environ 30 minutes.

**Comment.** Installer l'APK signé, puis dérouler le parcours entier : autorisations, consentement
d'accessibilité activé à la main, association du boîtier, choix des applications, heure de réveil,
activation. Puis **redémarrer le téléphone sans le déverrouiller** et vérifier dans `dumpsys alarm`
que l'alarme est de retour. Puis déverrouiller, ouvrir une application bloquée, laisser sonner, et
terminer par un scan du boîtier.

**Fini quand** le parcours se comporte comme en développement, ligne ajoutée dans `QA_MATRIX.md`,
section « Build de publication ».

### A3. Compléter les informations d'éditeur

- [ ] **Fait**

**Pourquoi.** Google Play refuse une fiche sans politique de confidentialité accessible
publiquement. Quatre champs sont encore marqués `<À COMPLÉTER>` dans
`docs/android/play-console/PRIVACY_POLICY.md` : identité de l'éditeur, contact public, URL
d'hébergement, date de mise à jour.

**Qui.** L'utilisateur.

**Fini quand** le document ne contient plus aucun `<À COMPLÉTER>` et que son contenu est en ligne à
une adresse publique stable.

### A4. Créer et faire vérifier le compte Google Play

- [ ] **Fait**

**Qui.** L'utilisateur. Compter plusieurs jours de délai côté Google.

**À savoir.** Pour un compte personnel, Google exige **12 testeurs inscrits pendant 14 jours
consécutifs** avant d'autoriser une publication en production. Ce délai court en parallèle du reste
et gagne à être lancé tôt.

### A5. Tourner la vidéo de revue

- [ ] **Fait**

**Pourquoi.** La politique Google Play sur les services d'accessibilité impose une vidéo montrant
l'information de l'utilisateur, son consentement, et l'usage réel. Les deux raisons qui avaient fait
reporter ce tournage le 2026-09-07 sont levées : la route de développement a disparu et l'écran
« Aide et limites » existe.

**Qui.** L'utilisateur, sur un téléphone réel.

**Comment.** Le découpage séquence par séquence est dans
`docs/android/play-console/REVIEW_VIDEO_SCRIPT.md`, onze séquences, environ 100 secondes. Deux
pièges y sont notés : le service d'accessibilité doit être activé **devant la caméra**, et le
téléphone doit être exempté des restrictions d'énergie du fabricant, sans quoi le blocage peut
cesser de fonctionner en plein tournage sans aucun signal.

### A6. Soumettre sur une piste interne et traiter la réponse de Google

- [ ] **Fait**

**Pourquoi.** C'est **la porte 0b**, ouverte depuis l'étape 6 et le risque principal assumé par tout
le projet. Google n'a jamais statué sur l'usage du service d'accessibilité par Niumi. Un refus
remettrait en cause le blocage des applications, c'est-à-dire la moitié de la promesse du produit.

**Qui.** L'utilisateur. Dépend de A1 à A5.

**Fini quand** la date de soumission et la réponse de Google sont consignées dans
`docs/android/implementation-reports/LOT-0.md`, section « Statut de la porte 0b ».

### A7. Décider du traitement du retrait des récents sur HyperOS

- [x] **Fait** (2026-09-28)

**Pourquoi.** Mesuré le 2026-09-27 puis reproduit délibérément le 28 : un balayage de la carte de
Niumi dans les applications récentes, ou « Tout effacer », tue Niumi — malgré la priorité que lui
donne le service d'accessibilité — et HyperOS ne relie plus ce service tant que l'utilisateur ne
l'a pas réactivé. **Le blocage est coupé**, l'alarme conservée.

**Ce qui a été fait.** Décidé avec l'utilisateur et mesuré sur appareil (`ETAPE-25.md`) :

- **carte cachée pendant la session** (`RecentsCard`, `setExcludeFromRecents`) : plus de carte à
  balayer ; elle revient à la fin de la session ;
- **verrou HyperOS exigé par le diagnostic** (contrôle `RECENTS_LOCK`, bloquant sur HyperOS, sans
  objet ailleurs) : seul moyen mesuré d'échapper à « Tout effacer », qui tue même une tâche cachée
  et un processus sans tâche. Le verrou est par application et durable : il se pose une fois.

Écartés : supprimer la tâche en quittant Niumi (« Tout effacer » tue même sans tâche, mesuré),
service de premier plan permanent (même priorité, contraire au MVP), surveillance périodique (ne
répare rien, troisième dérogation à §9.1).

**Reste** : le cas sans objet (appareil sans verrou HyperOS) n'est pas mesurable sur le Xiaomi ;
le comportement des récents sur Pixel et Samsung relève de B1 et B2. Le trou mesuré le 2026-10-19
est suivi en A8.

---

### A8. Carte de Niumi visible dans les récents ouverts depuis Niumi

- [ ] **À décider**

**Pourquoi.** Mesuré le 2026-10-19 (Xiaomi 25080RABDG, Android 16, HyperOS OS3.0.302.0, essais de
l'étape 33) : session armée, carte marquée exclue (`0x00800000`), l'utilisateur ouvre les récents
**depuis Niumi** : la carte est affichée, il la balaie. HyperOS tue Niumi (`SwipeUpClean`, 14:52:37),
le service d'accessibilité passe « en panne », n'est plus lié, et le blocage est coupé jusqu'à sa
réactivation manuelle. Cause : Android garde dans les récents la tâche la plus récente même exclue
(AOSP, `RecentTasks.isInVisibleRange`). Ouverts depuis l'accueil, les récents ne montrent pas la
carte (confirmé par l'utilisateur). La parade A7 a donc un trou que l'application ne peut pas
fermer seule : aucune API ne retire des récents la tâche au premier plan.

**À décider** : traitement (limite écrite dans l'aide, mesure d'un service dans un processus
séparé, ou autre). Rien n'est implémenté.

---

## B. Couverture des appareils

C'est la dette la plus lourde du MVP, et elle ne se comble qu'avec du matériel.

### B1. Campagne sur Google Pixel

- [ ] **Fait**

**Pourquoi.** Tout ce qui a été mesuré depuis l'étape 3 l'a été sur **un seul téléphone**, un Xiaomi
sous HyperOS. Ce n'est pas un détail de couverture : l'étape 5 a montré qu'une surcouche
constructeur peut rendre le blocage **silencieusement inopérant**, sans aucun message dans
l'application. Rien ne permet d'extrapoler d'un fabricant à un autre.

Le Pixel est le premier à faire, pour deux raisons précises :

- il est le plus proche d'Android tel que Google le publie, donc la référence à laquelle comparer
  les écarts des autres ;
- un scénario de la matrice n'a **jamais pu être testé** faute d'appareil adapté : l'arrêt du seul
  service de premier plan depuis le gestionnaire du système, absent de HyperOS.

**Comment.** Dérouler la matrice de `docs/android/QA_MATRIX.md`, en commençant par les lignes qui
touchent au réveil et au blocage. Les deux scripts `tools/validate_alarm.sh` et
`tools/validate_blocking.sh` automatisent les mesures et refusent de tourner si leurs conditions ne
sont pas réunies.

Y ajouter les **neuf lignes « blocage différé »** de la matrice : `tools/validate_blocking.sh
--deferred` et `tools/validate_blocking_start.sh` (`reboot`, puis `doze` par adb Wi-Fi, câble
retiré) les outillent en partie. Deux questions y sont propres au Pixel : la mort du processus
délie-t-elle aussi le service d'accessibilité, et « Tout effacer » dans les récents coupe-t-il aussi
le blocage, sachant que la carte est cachée pendant la session et que le contrôle du verrou y est
sans objet (voir A7) ?

Y ajouter les **onze lignes « sonneries et volume progressif »** (Lot 7) : la montée se mesure au
logcat sur le Xiaomi (`AudioTrack::setVolume`), rien ne garantit que le Pixel journalise de même —
à défaut, montée à l'oreille et `dumpsys audio`. Question propre au Pixel : l'`AudioTrack` statique
du réveil y est-il accepté, ou le repli `MediaPlayer` joue-t-il (`AUDIO_LOOP_FALLBACK` dans le
journal) ?

**Fini quand** une colonne Pixel est remplie dans `QA_MATRIX.md`, y compris les lignes en échec.

### B2. Campagne sur Samsung Galaxy

- [ ] **Fait**

**Pourquoi.** Deuxième parc en nombre d'utilisateurs, et surcouche réputée agressive sur la gestion
de l'énergie, ce qui touche directement la promesse du réveil.

**Comment.** Même déroulé que B1, lignes « blocage différé » et « sonneries » comprises, avec les mêmes deux
questions propres à la surcouche : mort du processus et retrait des récents.

**Fini quand** une colonne Samsung est remplie dans `QA_MATRIX.md`.

### B3. Trancher la question des démarrages automatiques chez les autres fabricants

- [ ] **Fait**

**Pourquoi.** Question ouverte depuis l'étape 19, et la seule qui puisse faire **perdre le réveil
après un redémarrage**. Sur Xiaomi, un réglage refusé par défaut décide si une application a le
droit de démarrer seule. Mesuré : les démarrages du téléphone en sont exemptés, donc le réveil est
préservé. Rien ne dit qu'il en va de même chez Oppo, Realme, Vivo ou Honor, réputés plus stricts.

**Comment.** Sur chaque appareil, **laisser le réglage dans son état par défaut**, armer une
session, redémarrer, et vérifier que l'alarme est reprogrammée avant tout déverrouillage. Un essai
mené avec le réglage accordé ne prouve rien pour un utilisateur ordinaire, qui ne l'a pas.

**Si un fabricant bloque**, le réveil après redémarrage y est perdu et il faudra ajouter un contrôle
de diagnostic ciblé sur ces appareils. Voir SPEC_ANDROID §4.2 et §20.

### B4. Couvrir les autres versions d'Android

- [ ] **Fait**

Seul **Android 16** a été couvert. La spécification en demande six.

| Version | Ce qu'elle seule permet de vérifier |
| --- | --- |
| Android 10 ou 11 | le plancher de compatibilité annoncé |
| Android 12 ou 13 | les alarmes exactes et les notifications |
| Android 14 | le rejet par l'utilisateur de la notification d'un service au premier plan (l'accès plein écran n'est plus demandé depuis le Lot 9) |
| Android 15 | l'arrêt forcé et l'annulation des alarmes |
| Android 17 | l'audio d'alarme quand l'application est en arrière-plan |

Android 17 porte un critère d'acceptation à lui seul et n'a aucun appareil pour être vérifié.

---

## C. Mesures complémentaires, sans matériel dédié

- [ ] **C1. Route audio modifiée pendant la sonnerie.** Déconnecter un casque Bluetooth en pleine
      alarme et vérifier que le son reprend sur le haut-parleur. Demande un second appareil audio
      manipulable pendant l'essai.
- [ ] **C2. Casque filaire ou USB-C.** Non testé, matériel absent. À rapprocher d'un constat déjà
      mesuré : le volume d'alarme est **indépendant par sortie audio**, et le diagnostic ne lit que
      celui de la sortie active au moment où il tourne.
- [ ] **C3. Mode économie d'énergie.** Ligne de la matrice jamais exécutée.
- [ ] **C4. Volume d'alarme coupé après l'activation.** Doit produire un incident, jamais une
      fausse promesse. Jamais rejoué à la main.
- [ ] **C5. Scan d'un second boîtier.** Un seul tag physique existe. Le refus d'un boîtier étranger
      est couvert par les tests, jamais sur le matériel.
- [ ] **C6. Appareil placé dans un seau d'économie d'énergie bas.** Le téléphone de test a refusé
      d'y descendre. L'alarme de secours qui rallume le son après un plantage n'a donc jamais été
      observée dans cette situation.
- [x] **C7. Observer la première exécution de la chaîne d'intégration continue.** *(Faite le
      2026-09-15 sur le commit `c8bb92d`, **verte du premier coup**, treize étapes sur treize,
      29,7 minutes. La compilation du composant iOS, le point le plus incertain sur une machine
      distante, passe en 2,1 minutes. Détail des durées dans `ETAPE-21.md`. Les deux caches ont été
      **écrits** pendant cette exécution mais pas encore relus : leur effet réel reste à constater
      au prochain envoi, voir C8.)*
- [ ] **C8. Confirmer que les caches de la chaîne servent à quelque chose.** La première exécution
      les a remplis ; seule la deuxième dira s'ils sont relus et combien de temps ils font gagner.
      Si le gain est nul, la clé de cache est probablement mal choisie. Attendu : nettement moins
      de 29 minutes.
- [x] **C9. Surveiller l'exemption d'énergie pendant une session.** *(Décidé, codé et mesuré le
      2026-09-29 sur Xiaomi 25080RABDG / Android 16, pendant `ARMED` et pendant `RINGING` : ligne
      de `QA_MATRIX.md`. La détection par la réconciliation `PACKAGE_REPLACED` n'est pas mesurée.)* Contrôle `BATTERY_OPTIMIZATION` ajouté aux
      contrôles surveillés de SPEC_ANDROID §13.1 : incident `ANDROID_BATTERY_EXEMPTION_REVOKED`,
      `CRITICAL`, surveillé dans tous les états non finaux comme le service d'accessibilité, recours
      « Ouvrir les réglages de batterie » sur l'écran 7, avertissement d'identifiant 9. Aucun
      broadcast public n'annonce le changement (`ACTION_POWER_SAVE_WHITELIST_CHANGED` est `@hide`) :
      détection au premier plan, à chaque réconciliation (dont `PACKAGE_REPLACED`) et au réveil.
      L'aide, l'onboarding et `LIMITES.md` le disent depuis la mesure.
- [x] **C10. Trente minutes d'écran éteint avec le réveil sans plein écran.** *(Mesuré le
      2026-10-18 : armé 18:47:21, ~37 min d'écran éteint, USB débranché ; Doze léger, Doze profond
      resté en attente ; sonnerie à +0,5 s après sortie de veille par l'alarme, écran resté éteint,
      notification lisible au réveil de l'écran, scan accepté. Doze **profond** au moment du réveil
      non obtenu sur cet essai : le Lot 0 l'avait mesuré avec l'écran imposé.)*
- [x] **C11. Réglage HyperOS « Notifications de l'écran de verrouillage » désactivé pour Niumi.**
      *(Mesuré le 2026-10-18, 18:43 : notification de sonnerie publiée mais absente de l'écran de
      verrouillage, présente dans le volet après déverrouillage, son inchangé ; icône → écran de
      réveil → scan. Le réglage n'apparaît dans aucune donnée Android lisible : le diagnostic reste
      vert. Limite écrite dans l'aide et `LIMITES.md`.)*

---

## D. Évolution planifiée après le MVP

Le **blocage différé** (l'utilisateur choisit si le blocage commence tout de suite ou à une heure
donnée avant le réveil) est planifié aux **étapes 22 à 25** du plan
`docs/superpowers/plans/2026-09-03-mvp-android.md` (Lot 6, décisions du 2026-09-15). Ce n'est pas
une tâche de publication : le MVP décrit ci-dessus peut être publié sans lui.

**État au 2026-09-28 : les étapes 22 à 25 sont faites.** Le blocage différé est construit et mesuré
sur Xiaomi / Android 16 : neuf lignes §20 renseignées dans `QA_MATRIX.md`, les quatre critères §21
du Lot 6 prouvés sur cet appareil (`RELEASE_REPORT.md`, critères 26 à 29), les limites écrites dans
l'aide après mesure (`LIMITES.md`). Le début du blocage part à l'heure à moins d'une demi-seconde, y
compris en Doze profond, téléphone verrouillé ou après un redémarrage ; téléphone éteint, il part au
redémarrage avec l'avertissement prévu. Détail dans `ETAPE-25.md`.

Ce qui reste ouvert pour le Lot 6 :

- une **mort du processus** délie le service d'accessibilité sur HyperOS : l'alarme de début est
  conservée, le blocage attend la réactivation (limite établie, écart 9 de `RELEASE_REPORT.md`) ;
  même cause que A7, dont les parades couvrent les gestes des récents mais pas les autres morts.
  Sans parade connue ; limite écrite dans l'aide le 30/09 (`LIMITES.md`) et ligne §20 requalifiée ;
- la couverture Pixel et Samsung (B1, B2) ;
- une session différée sur **APK de publication** (A2), impossible sans la clé (A1) ;
- ~~l'écran 7 ne se rafraîchit pas quand un réglage change depuis le volet rapide~~ : traité et
  mesuré le 29/09 (écart 10 de `RELEASE_REPORT.md`) ;
- ~~l'écran 12 ne rejoue ses contrôles qu'à son ouverture~~ : traité et mesuré le 29/09 ;
- **retrait de l'écran 12 de la version finale** (intention de l'utilisateur, 29/09, non décidé
  dans la spec) : l'écran porte aujourd'hui la destination du tap des avertissements (§13.1) et
  l'export du journal (§17) ; les retirer demande d'abord de leur trouver une autre place et de
  mettre SPEC_ANDROID §13.1, §15 et §17 à jour ;
- l'annonce d'état du NFC n'est écoutée qu'écran 7 affiché ; l'étendre à toute la session (§13.1)
  n'est pas décidé ;
- **seuil du contrôle de volume d'alarme** (question produit, 29/09) : Android borne ce volume à 1
  (AOSP, et mesuré sur le Xiaomi), si bien que « volume supérieur à zéro » et
  `ANDROID_ALARM_VOLUME_ZERO` ne se déclenchent pratiquement jamais, et qu'un réveil à 1/15 passe
  pour audible. À trancher : garder, relever le seuil, ou transformer en avertissement.

Les **sonneries et le volume progressif** (Lot 7) sont planifiés aux étapes 26 à 28 du même plan.

**État au 2026-10-15 : les étapes 26 à 28 sont faites**, avec un manque nommé. Quatre sonneries,
montée progressive sur 2, 5, 10 ou 15 min (liste resserrée le 08/10), réglage copié dans la session
et modifiable pendant `ARMED`. Mesuré sur Xiaomi / Android 16 : dix des onze lignes §20 conformes
(`QA_MATRIX.md`), montée à moins de 0,3 % de la courbe, reprise après mort du processus au bon
niveau, redémarrage verrouillé. Le réveil joue depuis le 15/10 par un `AudioTrack` statique à
points de boucle : `MediaPlayer` laissait 70 à 430 ms de silence à chaque tour, audible sur
« Oiseaux ». Deux limites écrites dans l'aide après mesure. Détail dans `ETAPE-28.md`.

Ce qui reste ouvert pour le Lot 7 :

- **réécouter « Piano », « Cloche » et « Énergique » avec le lecteur `AudioTrack`** (critère 30 de
  `RELEASE_REPORT.md`, partiel) : trois sessions courtes, volume constant, trente secondes
  d'écoute chacune, journal `AudioTrack` sans arrêt de piste ;
- TalkBack sur l'écran 14, passé le 15/10 ;
- le redémarrage de la montée observé une fois le 08/10, non reproduit depuis (13 et 15/10) : à
  consigner s'il revient, capture `ActivityManager` comprise ;
- une session sur **APK de publication** (A2) : `RingtoneResources` référence les quatre ressources
  `raw`, que la réduction de ressources doit donc garder — à prouver ;
- la couverture Pixel et Samsung (B1, B2) ;
- vibration progressive (impulsions qui se resserrent) proposée par l'utilisateur, hors périmètre
  du Lot 7, à spécifier séparément.

Les **boîtiers multiples** (Lot 8) sont planifiés aux étapes 29 à 31 du même plan (conception
`docs/superpowers/specs/2026-10-13-boitiers-multiples-design.md`). Ce n'est pas une tâche de
publication : le lot étend le MVP sans en changer la porte finale.

**État au 2026-10-18 : les étapes 29 à 31 sont faites.** Contrat KMP 1.4 (vérification contre une
liste), jusqu'à cinq boîtiers nommés figés dans `session_box` (Room v5, projection Direct Boot v4),
écran 3 « Mes boîtiers », diagnostic et récapitulatif au pluriel. Mesuré sur Xiaomi / Android 16 les
17 et 18/10 avec six tags : trois des cinq lignes §20 du lot conformes, dont une avec réserve, et deux
partielles (`QA_MATRIX.md`), les cinq critères §21 prouvés (36 à 40 de `RELEASE_REPORT.md`). Le faux
« NFC désactivé » des écrans 8 et 9 après un redémarrage, révélé à l'étape 30, est corrigé : le
message disparaît seul dès que le NFC est prêt. Détail dans `ETAPE-29.md` à `ETAPE-31.md`.

Ce qui reste ouvert pour le Lot 8 :

- la réserve de la ligne « boîtier ajouté après l'activation » : rejouer avec **le même tag**,
  refusé pendant une session puis accepté à la suivante une fois ajouté (deux sessions courtes) ;
- après une mise à jour depuis le Lot 7, **voir** « Boîtier 1 » à l'écran 6 de la session suivante
  (lu dans `session_box` le 18/10, l'écran lui-même n'a pas été regardé) ;
- TalkBack à voix haute sur l'écran 3 (l'arbre d'accessibilité a été relevé, l'annonce de la taille
  de la liste n'est prouvée qu'en instrumenté) ;
- l'export du diagnostic avec plusieurs boîtiers, déclenché sur appareil (prouvé en JVM) ;
- l'incident `TIME_CHANGED` (`WARNING`) consigné après un redémarrage pendant `ARMED` le 18/10,
  horloge resynchronisée au démarrage : comportement préexistant, à qualifier (faux positif ou
  incident légitime) hors du Lot 8 ;
- après un déverrouillage depuis l'écran de réveil, HyperOS a laissé l'accueil au premier plan le
  18/10 : il a fallu rouvrir Niumi pour revoir l'écran de réveil (l'écran de réveil était passé en
  pause à l'ouverture du clavier de code, 6 s avant le déverrouillage). SPEC_ANDROID §10.4 attend
  qu'un écran affiché au-dessus du verrouillage reste visible après lui : à qualifier selon le geste
  employé, hors du Lot 8 ;
- l'écran 3 ne relit pas le NFC sur un état stable de l'adaptateur (volet rapide), contrairement
  aux écrans 2, 6, 7, 8, 9 et 12 : hors du Lot 8 ;
- la couverture Pixel et Samsung (B1, B2).

## Ce qui n'est pas dans ce document

Les limites **assumées et documentées** ne sont pas des tâches. Elles sont énoncées dans
l'application, écran « Aide et limites », et dans `docs/android/LIMITES.md` : un arrêt forcé
supprime le réveil, le mode silence total coupe l'alarme, le scan sur écran verrouillé dépend du
téléphone, il n'existe aucun moyen de terminer une session sans le boîtier. Elles ne se corrigent
pas, elles se disent.

Le détail des preuves déjà obtenues est dans `docs/android/RELEASE_REPORT.md`, critère par critère,
et les mesures dans `docs/android/QA_MATRIX.md`.
