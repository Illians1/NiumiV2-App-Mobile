# Ligne de conduite — Niumi mobile

## Projet

Niumi est une application mobile Android/iOS de réveil et de blocage temporaire
d'applications. Le projet partage son moteur métier via Kotlin Multiplatform, tandis que
les intégrations système et l'interface restent natives à chaque plateforme.

Le dépôt contient les spécifications produit, métier commun, Android et iOS. Elles
définissent le comportement attendu ; ce fichier définit uniquement la manière de
travailler dans le dépôt.

## Règle d'or — Sources de vérité

Toujours consulter les spécifications concernées avant d'implémenter une demande.

Ordre de priorité :
1. intention produit ;
2. contrat métier commun KMP ;
3. spécification de la plateforme concernée ;
4. code existant.

Ne jamais inventer une règle métier, un comportement système, un format de données ou une
contrainte de plateforme lorsque la réponse se trouve dans les specs.

Les spécifications sont la source de vérité du projet, mais elles ne doivent pas être suivies
aveuglément. Garder un regard critique sur leur cohérence, leur faisabilité technique, leurs
hypothèses et leurs éventuelles contradictions. Si une exigence paraît incorrecte, fragile,
obsolète, ambiguë ou incompatible avec les contraintes réelles d'Android, d'iOS ou de la stack,
le signaler explicitement avant de l'implémenter, expliquer le problème et proposer une
correction ou une clarification. Ne pas modifier silencieusement le comportement pour
contourner une faiblesse de la spec.

Si une demande contredit une spécification :
- signaler clairement le conflit ;
- expliquer la règle actuelle ;
- attendre confirmation avant de dévier ;
- si la déviation est confirmée, mettre à jour la ou les spécifications concernées dans le
  même changement afin qu'elles restent la source de vérité.

Une contrainte réelle d'Android ou d'iOS peut nécessiter une exception au contrat commun.
Dans ce cas, la documenter explicitement dans les specs plutôt que de la laisser uniquement
dans le code.

## Stack

- Domaine partagé : Kotlin Multiplatform (`:shared:core`).
- Android : Kotlin + Jetpack Compose.
- iOS : Swift + SwiftUI.
- Les APIs, versions, frameworks et choix d'architecture détaillés sont définis dans les
  spécifications. Ne pas les recopier ici.

## Principes d'architecture

- Le moteur KMP est l'autorité pour les règles métier communes.
- Ne pas dupliquer une règle commune dans Android et iOS.
- Les intégrations système restent natives et sont isolées derrière des abstractions
  testables lorsque pertinent.
- Les composants d'interface ne doivent pas porter directement la logique système ou métier.
- Respecter l'architecture existante ; ne pas la remplacer sans justification.
- Ne pas ajouter de dépendance externe sans besoin démontré et accord explicite.

## Règles de travail

- **Réponses concises et en français.** Aller droit au but.
- **Inspecter avant de modifier.** Lire le code, les instructions locales et les specs
  concernées avant toute implémentation importante.
- **Planifier les changements complexes.** Pour une feature, un refactoring ou un bug
  non trivial, analyser d'abord l'existant et proposer un plan avant de modifier le code.
- **Documentation à jour.** Avant d'utiliser ou modifier du code dépendant d'une API ou
  bibliothèque externe, consulter sa documentation actuelle. Utiliser Context7 lorsqu'il
  est disponible et privilégier les sources officielles pour Android, Apple et Kotlin.
- **Ne pas masquer les incertitudes.** Un point marqué comme POC, à valider, dépendant
  d'un entitlement, d'une politique de store ou d'un appareil réel reste une incertitude
  jusqu'à validation effective.
- **Conformité graphique.** Si une charte ou un document de design Niumi existe, le
  consulter avant toute modification visuelle. Si une demande le contredit, le signaler
  avant de modifier la référence.
- **Pas de co-auteur dans les commits.** Ne jamais ajouter de ligne `Co-Authored-By`.
- **Ne pas commit ni push automatiquement** sauf demande explicite. Si l'utilisateur demande de push sur github, tu peux par contre le faire sans demander de confirmation
- **Pas de faux comportement de production.** Les mocks, fakes et raccourcis de test doivent
  rester dans les tests ou les configurations de développement prévues à cet effet.

## Documentation du dépôt

La documentation doit décrire le comportement réellement décidé ou implémenté.

Ne pas documenter à l'avance une architecture qui n'existe pas encore. Quand une décision
fonctionnelle ou technique change, mettre à jour la spécification correspondante avec le
code afin d'éviter toute divergence.

## Build et tests

Après chaque modification :
- exécuter les tests automatisés et analyses statiques pertinents pour les fichiers touchés ;
- compiler les cibles concernées lorsque c'est pertinent ;
- ne jamais masquer un échec de build, de test, de lint ou de signature ;
- ajouter ou adapter les tests lorsque le comportement change.

Les tests automatisés ne remplacent pas les validations sur appareil réel pour les
fonctionnalités dépendantes du système ou du matériel.

À la fin d'un changement touchant ces fonctionnalités, indiquer :
- les tests manuels à effectuer ;
- le résultat attendu ;
- les points de vigilance ou limites encore non validés.

## Définition de terminé

Un changement est terminé seulement si :
- le code respecte les spécifications applicables ;
- les tests pertinents passent ;
- les erreurs et avertissements nouveaux ont été traités ;
- les specs ont été mises à jour si le comportement ou le contrat a changé ;
- les validations matérielles ou de plateforme encore nécessaires sont signalées clairement.