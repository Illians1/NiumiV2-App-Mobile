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

**Fini quand** une colonne Pixel est remplie dans `QA_MATRIX.md`, y compris les lignes en échec.

### B2. Campagne sur Samsung Galaxy

- [ ] **Fait**

**Pourquoi.** Deuxième parc en nombre d'utilisateurs, et surcouche réputée agressive sur la gestion
de l'énergie, ce qui touche directement la promesse du réveil.

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
| Android 14 | l'accès spécial au plein écran |
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

---

## D. Évolution planifiée après le MVP

Le **blocage différé** (l'utilisateur choisit si le blocage commence tout de suite ou à une heure
donnée avant le réveil) est planifié aux **étapes 22 à 25** du plan
`docs/superpowers/plans/2026-09-03-mvp-android.md` (Lot 6, décisions du 2026-09-15). Ce n'est pas
une tâche de publication : le MVP décrit ci-dessus peut être publié sans lui. Le contrat KMP 1.3
et les spécifications Android et iOS le décrivent déjà ; le code, lui, n'existe pas encore.

## Ce qui n'est pas dans ce document

Les limites **assumées et documentées** ne sont pas des tâches. Elles sont énoncées dans
l'application, écran « Aide et limites », et dans `docs/android/LIMITES.md` : un arrêt forcé
supprime le réveil, le mode silence total coupe l'alarme, le scan sur écran verrouillé dépend du
téléphone, il n'existe aucun moyen de terminer une session sans le boîtier. Elles ne se corrigent
pas, elles se disent.

Le détail des preuves déjà obtenues est dans `docs/android/RELEASE_REPORT.md`, critère par critère,
et les mesures dans `docs/android/QA_MATRIX.md`.
