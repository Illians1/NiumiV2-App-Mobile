# Script de la vidéo de revue Play — AccessibilityService

Exigée par la politique Play pour toute déclaration d'usage non-accessibilité d'un
`AccessibilityService` : montrer la divulgation, le consentement, et l'usage réel du service en
conditions normales. **Non tournée à cette étape.** Le seul parcours d'activation qui existe
aujourd'hui est la route de debug (`PocScreen`), un écran technique où l'on saisit un nom de
package à la main — filmer ce parcours donnerait à Google une fausse image du produit et
n'apporterait pas de signal exploitable sur l'acceptation de la politique. Le tournage est
reporté à l'étape 21, une fois le POC supprimé et le parcours utilisateur complet livré (voir
`LOT-0.md` pour la décision et sa justification).

Ce script reste utile dès maintenant : il sert de check-list à l'implémentation des écrans
restants (association du boîtier, sélection des applications, session active, écran d'aide),
pour vérifier qu'ils permettent bien de filmer chaque séquence sans détour par le debug.

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

## Prérequis avant tournage (rappel, détaillés à l'étape 21)

- POC supprimé (`androidApp/app/src/debug/kotlin/com/niumi/app/poc/`) ;
- écran « Aide et limites » livré ;
- build release fonctionnel sur un appareil réel, service d'accessibilité activé manuellement
  avant le début de l'enregistrement (la spec interdit de le simuler, donc aussi de le pré-armer
  hors caméra si la vidéo est censée montrer l'activation) ;
- appareil exempté des restrictions de batterie du fabricant (§13), sans quoi le blocage peut
  échouer silencieusement pendant le tournage (voir `ETAPE-05.md`).
