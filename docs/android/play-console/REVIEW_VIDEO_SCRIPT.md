# Script de la vidéo de revue Play — AccessibilityService

Exigée par la politique Play pour toute déclaration d'usage non-accessibilité d'un
`AccessibilityService` : montrer la divulgation, le consentement, et l'usage réel du service en
conditions normales.

**Statut au 2026-09-15 : tournage possible, non encore fait.** Le report décidé le 2026-09-07
(voir `LOT-0.md`) attendait deux choses, toutes deux livrées à l'étape 21 : la suppression de la
route de debug, et l'écran « Aide et limites ». Chaque séquence ci-dessous se filme désormais sur
le parcours utilisateur réel, sans aucun détour par le debug — ce qui était la raison même du
report, la route POC ne pouvant donner à Google qu'une fausse image du produit.

Le tournage lui-même reste une action humaine, sur appareil réel, et conditionne la porte 0b.

## Séquences à filmer, dans l'ordre

| # | Séquence | Ce qui doit être lisible à l'écran | Durée cible |
| --- | --- | --- | --- |
| 1 | Écran de consentement (§12.3) | Les cinq points affichés en entier, avant toute ouverture des réglages | 10 s |
| 2 | Activation du service | Ouverture réelle de `ACTION_ACCESSIBILITY_SETTINGS`, bascule manuelle du switch système par l'utilisateur (aucun clic simulé par l'app) | 10 s |
| 3 | Retour dans Niumi | L'état repasse de « Service inactif » à « Service actif » sans reconstruire l'écran | 5 s |
| 4 | Association du boîtier | Scan NFC réel d'un tag, confirmation de l'association | 10 s |
| 5 | Sélection des applications à bloquer | Le sélecteur, le choix d'au moins une application | 10 s |
| 6 | Diagnostic avant activation | Le diagnostic passe au vert, y compris le contrôle de batterie optimisée (§13) | 10 s |
| 7 | Activation d'une session | Confirmation, retour à l'accueil « session active » | 5 s |
| 8 | Déclenchement de l'alarme | Écran verrouillé puis `AlarmActivity` au-dessus du verrouillage, son audible | 10 s |
| 9 | Tentative d'ouverture d'une application bloquée | Retour immédiat à l'accueil et overlay explicatif, texte lisible | 10 s |
| 10 | Scan du boîtier associé | Alarme et blocage s'arrêtent en moins d'une seconde | 5 s |
| 11 | Session terminée | Écran de fin de session | 5 s |

Durée totale visée : autour de 100 secondes, sans montage trompeur (pas de coupe qui masquerait
un délai réel supérieur à ce qu'affiche l'application).

## Ce que la vidéo ne doit jamais laisser croire

- Que le blocage est incontournable (SPEC_ANDROID §4.3, §23).
- Que le consentement a été accordé par l'application elle-même plutôt que par un geste
  utilisateur réel dans les réglages système.
- Un parcours de debug, un mock, ou des données factices en dehors de ce qu'un utilisateur
  produirait normalement.

## Prérequis avant tournage

- POC supprimé — **fait à l'étape 21** ;
- écran « Aide et limites » livré — **fait à l'étape 21** ;
- build release fonctionnel sur un appareil réel — **reste à vérifier** : l'APK release exige le
  keystore d'upload, et aucune campagne n'a encore tourné sur un artefact de publication
  (`RELEASE_REPORT.md`, écart 7) ;
- service d'accessibilité activé manuellement avant le début de l'enregistrement : la spec
  interdit de simuler le consentement, donc aussi de pré-armer le service hors caméra si la vidéo
  est censée montrer l'activation ;
- appareil exempté des restrictions de batterie du fabricant (§13), sans quoi le blocage peut
  échouer silencieusement pendant le tournage (voir `ETAPE-05.md`) ;
- séquence 11 : l'écran « Aide et limites » n'est pas au script d'origine. L'ajouter après la
  session terminée est utile pour la revue, la politique Play appréciant que les limites du
  blocage soient visibles dans le produit (§4.3, §23).
