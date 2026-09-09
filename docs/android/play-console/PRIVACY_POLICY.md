# Politique de confidentialité — Niumi

Document préparatoire à la politique de confidentialité publique exigée par Google Play
(URL à héberger et renseigner dans la fiche Store). Statut : préparatoire, pas encore publié.

Éditeur : `<À COMPLÉTER>`
Contact : `<À COMPLÉTER>`
URL d'hébergement de cette politique : `<À COMPLÉTER>`
Dernière mise à jour : `<À COMPLÉTER à la publication>`

---

## Ce que Niumi ne fait pas

Niumi fonctionne entièrement hors ligne. L'application ne déclare aucune permission `INTERNET`
et n'effectue aucun appel réseau : aucune donnée n'est transmise à l'éditeur, à un tiers, à un
service d'analytique ou de publicité. Niumi ne propose ni compte, ni synchronisation, ni
identifiant publicitaire.

## Données consultées

| Donnée | Pourquoi | Conservée où | Transmise |
| --- | --- | --- | --- |
| Nom de l'application affichée au premier plan | détecter quand ramener l'utilisateur à l'accueil pendant une session | non conservée, sauf la dernière occurrence dans le journal technique local (`BLOCK_APPLIED`) | jamais |
| Liste des applications choisies par l'utilisateur pour le blocage | exécuter la session qu'il a configurée | base de données locale de l'appareil | jamais |
| Identifiant et empreinte cryptographique (SHA-256) du boîtier NFC associé | vérifier qu'un scan provient du bon boîtier | base de données locale de l'appareil, hash uniquement, jamais le token en clair | jamais |
| Heure de réveil choisie | programmer l'alarme | base de données locale et zone protégée par appareil (Direct Boot) | jamais |
| Journal technique (jusqu'à 200 événements) | diagnostiquer un incident, sur demande explicite de l'utilisateur | localement, effacé en continu au-delà de 200 entrées | jamais transmis automatiquement ; exportable sous forme de texte par l'utilisateur lui-même, pour son propre usage (support, diagnostic) |

Le journal technique ne contient jamais : le token NFC en clair, un hash complet, un texte
provenant d'un événement d'accessibilité, ou le contenu d'une autre application. Le nom de
package n'apparaît que dans l'événement `BLOCK_APPLIED`.

## Ce que Niumi ne collecte jamais

- le contenu affiché à l'écran d'une autre application ;
- les saisies clavier ;
- la localisation ;
- les contacts, messages, photos, fichiers ;
- un identifiant publicitaire ou un identifiant d'appareil transmis à l'extérieur.

## Durée de conservation et suppression

Toutes les données listées ci-dessus restent sur l'appareil de l'utilisateur, dans le stockage
privé de l'application. Aucune sauvegarde cloud n'est effectuée (`allowBackup="false"`). La
désinstallation de Niumi supprime l'intégralité des données locales.

## Droits de l'utilisateur

L'utilisateur peut à tout moment : consulter le journal technique et l'exporter, désactiver le
service d'accessibilité dans les réglages Android, dissocier le boîtier, désinstaller
l'application pour supprimer toutes ses données locales.

## Blocage d'applications : limite à connaître

Niumi bloque le passage au premier plan des applications choisies par l'utilisateur en
observant uniquement leur nom de package. Ce blocage est comportemental et peut être contourné
en désactivant le service d'accessibilité, en arrêtant Niumi de force ou en le désinstallant —
voir le détail dans l'application, écran « Aide et limites » (ajouté à l'étape 21).

## Contact

Pour toute question relative à cette politique : `<À COMPLÉTER>`.
