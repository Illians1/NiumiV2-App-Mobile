# Étape 6 — dossier Google Play et porte de validation 0

Date : 2026-09-07. Rédaction du dossier Play, mise à jour des specs **et campagne d'essais sur
appareil réel** (Redmi 25080RABDG, HyperOS V816 OS3.0, Android 16 / API 36, firmware
`BP2A.250605.031.A3` — le même appareil qu'aux étapes 3 à 5).

**Deux constats bloquants ont été trouvés pendant la campagne, aucun visible en test
automatisé** (voir « Constats sur appareil ») : le mode Ne pas déranger en silence total rend le
réveil totalement inopérant, et la notification de sonnerie peut être balayée depuis Android 14,
ce qui prive l'utilisateur de tout accès à l'écran de scan. Le premier a été arbitré et figé
dans les specs ; le second attend une décision d'implémentation.

Aucun code de production n'a été modifié à cette étape.

## Résumé

L'étape 6 clôt le Lot 0 par la preuve de publiabilité, non par du code. Elle livre les six
documents Play Console, la matrice physique `LOT-0.md`, deux outils de validation, et acte deux
déviations de spec assumées avec l'utilisateur.

## Décisions validées avec l'utilisateur

1. **La soumission Play et la vidéo de revue sont reportées à l'étape 21.** SPEC_ANDROID §22 et
   §23 demandaient de soumettre le POC sur une piste Play dès cette étape. La politique Play
   exige une vidéo montrant la divulgation et le consentement en usage normal, or le seul
   parcours existant est la route de debug ; et une piste interne n'est pas soumise aux revues
   de politique standard. La rédaction du dossier reste ici, le verdict part à l'étape 21.
   Risque assumé et consigné : les étapes 7 à 20 sont implémentées avant la réponse de Google.
   La porte 0 est scindée en **0a** (dossier + matrice, débloque l'étape 7) et **0b** (verdict
   Play, rattachée à l'étape 21).
2. **Le silence total devient un contrôle bloquant.** Voir « Constats sur appareil », point 1.
3. **Le Reader Mode ne peut pas être porté par le service.** L'utilisateur avait retenu cette
   option ; vérification faite dans la documentation Android, `enableReaderMode(Activity, ...)`
   exige une `Activity` au premier plan et n'a aucune variante utilisable depuis un `Service`.
   L'option a donc été écartée pour impossibilité technique, et remplacée par une règle de
   republication de la notification — en cours d'arbitrage à la clôture de ce rapport.

## Ce qui a été construit

### Documents Play Console (`docs/android/play-console/`)

| Document | Contenu |
| --- | --- |
| `ACCESSIBILITY_DECLARATION.md` | Réponses au formulaire de déclaration, divulgation reprise mot pour mot du code, preuve runtime `capabilities=0` |
| `USE_EXACT_ALARM.md` | Justification « réveil fonction centrale », `setAlarmClock()` seule API, comportement si l'accès manque |
| `FULL_SCREEN_INTENT.md` | Usage alarme, déclenchement, refus → activation refusée |
| `FGS_MEDIA_PLAYBACK.md` | Justification du type, avec le point de vigilance signalé par avance : Android n'a pas de type FGS « alarme » |
| `PRIVACY_POLICY.md` | Données consultées et conservées, aucune transmission ; champs éditeur à compléter |
| `REVIEW_VIDEO_SCRIPT.md` | Script séquence par séquence, tournage reporté à l'étape 21 |

### Matrice et outils

- `docs/android/implementation-reports/LOT-0.md` : décision de calendrier, préconditions de
  soumission ouvertes, matrice physique (lignes reportées des étapes 3 à 5 + lignes exécutées
  ici), deux constats bloquants détaillés, statut de la porte 0a.
- `tools/capture_device_state.sh` : relevé des colonnes de contexte exigées par §20 (fabricant,
  modèle, Android, firmware, permissions, volumes, DND, Doze, exemption d'énergie).
- `tools/validate_alarm.sh` : programme une alarme depuis le POC, mesure le retard réel par
  sondage, relève service, focus audio, notification et activité au premier plan.

Les deux scripts n'étaient pas prévus au plan. Ils ont été écrits parce que §20 impose de
consigner un retard mesuré et un contexte complet pour *chaque* essai, ce qui n'est pas tenable à
la main sur une dizaine d'essais, et parce qu'ils resserviront à la matrice QA de l'étape 21.
Même motif que `tools/validate_blocking.sh` à l'étape 5.

## Constats sur appareil (invisibles en test automatisé)

### 1. Arbitré — Ne pas déranger en silence total rend le réveil inopérant

Sous `zen_mode=2` (`ZEN_MODE_NO_INTERRUPTIONS`), écran éteint, volume d'alarme à 12 : le système
force `STREAM_ALARM` à `Muted: true` / `streamVolume:0`, l'écran reste `Dozing`, `AlarmActivity`
ne s'ouvre pas et le Reader Mode n'est donc jamais activé. Le service démarre pourtant sans
retard et la notification est postée : la sortie existe, mais elle est manuelle et rien ne
réveille l'utilisateur pour qu'il l'emprunte.

Délimitation mesurée : `zen_mode=1` (interruptions prioritaires) et `zen_mode=3` (alarmes seules)
fonctionnent normalement, son et écran de réveil compris. Seul le silence total est en cause.

**Décision (validée) :** contrôle `BLOCKING_FOR_ALARM` réévalué à chaque activation ; incident
`ANDROID_ALARM_MUTED_BY_DND` (`CRITICAL`) et événement `ALARM_MUTED_BY_DND` si le mode est
enclenché après l'armement ; Niumi ne demande pas `ACCESS_NOTIFICATION_POLICY` et ne modifie
jamais le réglage de l'utilisateur.

**Specs mises à jour :** SPEC_ANDROID §4.1 (condition ajoutée au périmètre garanti), §4.2 (cas
non garanti), §13 (contrôle scindé en deux, avec les mesures), §17 (nouvel événement).
SPEC_CORE_KMP est inchangée : §7.3 autorise déjà les codes plateforme préfixés `ANDROID_`.

**Implémentation :** étape 12 (`DeviceReadinessChecker`) et étape 17 (`AlarmReceiver`), notées
dans le plan.

**Complément du 2026-09-08 — l'horloge d'Android ne fait pas mieux.** L'utilisateur a demandé si
l'alarme système sonnait en silence total. Essai fait sur le même appareil avec
`com.android.deskclock` : `STREAM_ALARM` reste `Muted: true`, aucun son (confirmé à l'oreille).
Seule différence, elle affiche son écran plein écran et rallume l'écran, ce que Niumi ne parvient
pas à faire — vraisemblablement parce qu'une application système préinstallée échappe aux
restrictions de lancement d'activité en arrière-plan.

Cette mesure a écarté l'option `ACCESS_NOTIFICATION_POLICY`, pourtant vérifiée comme
fonctionnelle (basculer le filtre de `none` à `alarms` démute `STREAM_ALARM` instantanément) :
l'adopter rendrait Niumi plus intrusif que l'horloge d'Android pour contourner un réglage
délibéré de l'utilisateur. **Niumi ne modifie aucun réglage système.**

**Surveillance pendant la session (§13.1, ajoutée le 2026-09-08).** À la demande de
l'utilisateur, l'avertissement ne se limite plus au refus d'activation et à l'incident au
déclenchement : tout contrôle bloquant qui devient faux pendant `ARMED` produit un incident, une
notification du canal `niumi_session_warning` et l'événement `SESSION_READINESS_DEGRADED`. La
règle couvre le silence total, le volume d'alarme à zéro, les notifications et le plein écran
révoqués, le service d'accessibilité désactivé et la perte des alarmes exactes.

Limite inscrite dans la spec plutôt que masquée : l'avertissement est émis **au plus tôt, jamais
garanti immédiat**. Entre l'armement et la sonnerie, Niumi n'a aucun composant garanti en vie —
mesuré à cette étape — un receiver enregistré à chaud disparaît avec le processus, et plusieurs
de ces réglages, dont le volume, n'ont aucun broadcast public. §13.1 interdit explicitement
d'utiliser l'`AccessibilityService` comme sentinelle, bien qu'il soit le seul composant vivant en
continu : cela contredirait la déclaration Play rédigée à cette même étape.

### 2 et 3. Arbitrés — l'écran de réveil disparaît, et avec lui la seule sortie de session

Deux constats distincts, trouvés le même jour, qui se sont révélés avoir la même cause.

**Constat 2 — la notification de sonnerie est balayable.** Depuis Android 14, l'utilisateur peut
rejeter la notification d'un service au premier plan malgré `setOngoing(true)`. Mesuré pendant
une sonnerie active : service actif, focus `USAGE_ALARM` en haut de pile, notification à 0,
`mIsReaderMode=false`. L'alarme sonnait et plus aucun chemin ne menait à l'écran de scan ; la
seule issue a été « Forcer l'arrêt », c'est-à-dire sortir du produit.

**Constat 3 — le déverrouillage détruit l'écran de réveil.** Mesuré par échantillonnage continu
au moment exact du déverrouillage : `AlarmActivity` passe de « au premier plan » à « absente de
la pile », `MainActivity` la remplace, `mIsReaderMode` reste `false`, le service continue de
sonner. Le geste le plus naturel du réveil — déverrouiller pour scanner — fait perdre l'écran
qui porte le scan. Observé deux fois.

S'y ajoutent deux défauts signalés par l'utilisateur depuis son propre usage : le texte
« Déverrouille ton téléphone… » reste affiché après le déverrouillage (`refreshState()` n'est
appelé qu'au `onResume()`), et ouvrir Niumi pendant que l'alarme sonne ne ramène pas à l'écran de
réveil.

**Cause commune.** `AlarmActivity` porte seule le Reader Mode, donc la seule sortie possible
d'une session, et elle disparaît dans au moins trois situations ordinaires. Le Reader Mode ne
peut pas être déplacé : `NfcAdapter.enableReaderMode(Activity, ...)` exige une `Activity` au
premier plan et n'a aucune variante pour un `Service` (documentation Android vérifiée). La
réponse est donc de garantir la présence de l'activité, le plein écran d'une notification
republiée étant le seul mécanisme qu'Android autorise pour ouvrir une activité depuis
l'arrière-plan.

**Décision (validée) :** le service surveille sa notification et la republie si elle disparaît
pendant `RINGING` ; l'écran de réveil est re-présenté s'il est détruit ; son état est recalculé
au changement de verrouillage ; ouvrir Niumi pendant une session mène toujours à l'écran de
cette session. Le retour prédictif reste possible : l'utilisateur peut écarter l'écran
volontairement, mais ne peut jamais perdre tout accès. L'écran n'est pas ramené de force en
boucle, ce qui serait hostile et sanctionnable par Google Play.

**Specs mises à jour :** SPEC_ANDROID §10.2 (surveillance et republication), §10.4
(re-présentation, recalcul au changement de verrouillage, ouverture menant à l'écran de session),
§11.2 (contrainte d'API documentée là où le NFC est lu).

**Implémentation :** étape 17 (service et `AlarmActivity`) et étape 12 (`HomeScreen`), notées
dans le plan avec les tests attendus.

### Comment ces constats ont été trouvés

Aucun des trois n'était atteignable par un test automatisé, et deux ont été trouvés par
l'utilisateur en se servant normalement de l'application pendant la campagne : le balayage de la
notification était involontaire, et la remarque « quand l'alarme sonne, on devrait être
automatiquement sur l'écran de l'alarme » a directement mené au constat 3. C'est le deuxième lot
d'affilée où la validation matérielle rapporte davantage que les tests unitaires — l'étape 5
avait livré le gel MIUI et deux crashs.

Une mesure intermédiaire a d'abord été prise pour acquise à tort, puis rejetée : le premier
relevé « après déverrouillage » avait en réalité été fait après la fin de session (service à 0,
`AlarmActivity` marquée `f`), et ne prouvait rien. Le constat 3 n'a été retenu qu'après un second
essai par échantillonnage continu, capable de dater le déverrouillage lui-même.

## Fichiers créés

- `docs/android/play-console/` : les six documents listés plus haut.
- `docs/android/implementation-reports/LOT-0.md`
- `docs/android/implementation-reports/ETAPE-06.md` (ce fichier)
- `tools/capture_device_state.sh`, `tools/validate_alarm.sh`

## Fichiers modifiés

- `specs/SPEC_ANDROID.md` : §4.1, §4.2, §12.3, §13, §17, §21, §22, §23.
- `docs/superpowers/plans/2026-09-03-mvp-android.md` : « Comment utiliser ce plan », étape 6
  (porte 0a), étape 12, étape 17, étape 21 (porte 0b), recette d'acceptation.

## Commandes exécutées et résultat

| Commande | Résultat |
| --- | --- |
| `./gradlew ktlintCheck detekt` | vert |
| `./gradlew :app:assembleDebug` | vert |
| `./gradlew :app:installDebug` | installé |
| `tools/capture_device_state.sh` | relevé complet (corrigé deux fois : `set -e` + `pipefail` avortaient sur les `SIGPIPE` de `grep -m1`/`head`) |
| `tools/validate_alarm.sh 30` | mesures conformes |

`JAVA_HOME` doit être renseigné explicitement sur ce poste
(`/opt/homebrew/opt/openjdk@17`) : aucun JDK n'est sur le `PATH` par défaut.

## Résultats de la campagne

Voir la matrice complète dans `LOT-0.md`. Synthèse :

| Essai | Résultat |
| --- | --- |
| Volumes média et notification à zéro | OK, retard nul |
| Mode silencieux | OK, retard nul |
| DND interruptions prioritaires, écran éteint | OK, son et écran de réveil |
| **DND silence total** | **Réveil inopérant** — constat 1 |
| DND alarmes seules (essai ajouté) | OK, délimite la règle |
| Processus tué après armement | OK, alarme conservée et relancée |
| **Doze profond réel** | **OK** — `deep=IDLE` atteint 3 min après débranchement, retard nul au déclenchement |
| Arrêt forcé | Limite §4.2 confirmée, par `adb` et par le geste utilisateur réel |
| Arrêt du FGS depuis le gestionnaire | **Non reproductible** — écran absent d'HyperOS |
| Écran éteint 30 minutes | OK — 1 s de retard après 31 min ; l'appareil n'est jamais entré en Doze de lui-même |
| Casque Bluetooth connecté | OK — flux dupliqué haut-parleur + A2DP, confirmé au niveau HAL et à l'oreille |
| **Déverrouillage pendant la sonnerie** | **`AlarmActivity` détruite** — constat 3 |

Toutes les sonneries ont été terminées par un scan réel du boîtier, seul moyen prévu par le
produit.

## Le Doze profond, et une erreur de méthode

Deux tentatives ont d'abord échoué : `dumpsys deviceidle force-idle` répondait « Unable to go
deep idle » et douze `step deep` restaient bloqués à `INACTIVE`. J'en avais conclu à une
restriction d'HyperOS et consigné le scénario comme non couvert.

La cause était ailleurs, et de mon fait : **le câble USB était branché**. Android n'entre jamais
en Doze tant que l'appareil est en charge, et `dumpsys battery unplug` ne simule le débranchement
que pour la couche batterie, pas pour la politique d'inactivité. Les deux essais étaient donc
voués à l'échec par construction, et l'essai « écran éteint 30 minutes » mesurait lui aussi un
appareil qui ne pouvait pas dormir.

Rejoué le 2026-09-08 dans les bonnes conditions — `adb` par Wi-Fi, câble retiré, téléphone posé
sans être touché — l'appareil est entré en Doze profond en 3 minutes et l'alarme s'est déclenchée
avec un retard nul. C'est la validation du scénario nocturne réel, et donc de la promesse
centrale du produit, qui était jusque-là le seul point non prouvé de la matrice.

Deux réserves de méthode consignées dans `LOT-0.md` : l'appareil était sorti du Doze depuis
environ sept minutes à l'échéance, si bien que le déclenchement n'a pas eu lieu *pendant*
`deep=IDLE` — le drapeau `FLAG_WAKE_FROM_IDLE` reste la garantie système pour ce cas ; et les
sondages `dumpsys` toutes les trois minutes ont pu contribuer aux sorties de veille observées.

## État de l'appareil en fin de campagne

À remettre en ordre par l'utilisateur, la campagne ayant modifié plusieurs réglages :

Remise en état effectuée le 2026-09-08 en fin de campagne :

| Élément | État final | Conforme à l'initial |
| --- | --- | --- |
| `adb` en mode TCP | **désactivé** (`service.adb.tcp.port = 0`), connexion USB seule. Il avait été ouvert sur le port 5555 pour l'essai Doze sans câble, ce qui exposait l'appareil à tout le réseau Wi-Fi | oui |
| Volume d'alarme | 12/15 sur haut-parleur et Bluetooth (abaissé à 3/15 pendant la campagne à la demande de l'utilisateur) | oui |
| Volumes média, sonnerie, notification | 15, 12, 12 | oui |
| Ne pas déranger | désactivé (`zen_mode=0`), mode sonnerie `NORMAL` | oui |
| Service d'accessibilité Niumi | inactif | oui |
| Alarme Niumi en attente | aucune | oui |

Deux points restent à la charge de l'utilisateur, non réalisables par `adb` :

- **le verrouillage par code PIN est toujours désactivé** — retiré en cours de campagne parce que
  la programmation automatique échouait sur écran verrouillé ;
- **une alarme « TestNiumi » subsiste dans l'application Horloge**, créée par `ACTION_SET_ALARM`
  pour la comparaison du comportement en silence total.

## Incertitudes et limites

- L'arrêt du seul service de premier plan n'est pas reproductible sur HyperOS.
- Pixel et Samsung restent non couverts (décision de l'étape 5, reportée à la bêta).
- Android 17 (`set-enable-hardening throw`) hors de portée : appareil en Android 16.
- Les scénarios « activation refusée » de §20 ne sont pas testables tant que
  `DeviceReadinessChecker` n'existe pas (étape 12).
- Le scan sur écran verrouillé reste bloqué par HyperOS (§4.4, déjà connu depuis l'étape 4).
- `POST_NOTIFICATIONS` a été accordée par `pm grant` : le parcours de demande à l'utilisateur
  n'existe pas encore.

## Statut

**Étape terminée. Porte 0a franchie le 2026-09-08**, validation écrite dans `LOT-0.md` avec les
quatre risques assumés. L'étape 7 (machine à états `SessionEngine`) est ouverte.

La porte 0b — verdict Google Play sur l'AccessibilityService — reste ouverte et est rattachée à
l'étape 21, conformément à la déviation actée le 2026-09-07.

Aucun commit n'a été effectué : `CLAUDE.md` interdit de commiter sans demande explicite.
