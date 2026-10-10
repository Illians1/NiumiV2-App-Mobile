# Niumi · Dossier de design

Ce dossier contient tout ce qui a été décidé pour les écrans de l'app Niumi (iPhone et Android) entre le 1er et le 9 octobre 2026. Il est fait pour être lu par Claude Code, qui implémente l'app, et par Mehdi, qui en a validé le contenu.

Il a été préparé par Claude à partir de deux sources : le document « Niumi : contenu des écrans » et le canevas « Niumi · maquettes simples », page « Écrans validés ».

## Ce que contient le dossier

| Fichier | Rôle |
| --- | --- |
| `LISEZ-MOI.md` | Ce fichier. À lire en premier |
| `SPECIFICATION.md` | Ce qui est décidé, écran par écran : contenu, textes exacts, comportements, différences entre iPhone et Android |
| `VALEURS-DE-DESIGN.md` | Couleurs, police, tailles, formes, cadran, règle clair ou sombre |
| `valeurs-de-design.json` | Les mêmes valeurs, pour un programme |
| `POINTS-OUVERTS.md` | Ce qui n'est pas décidé, pas validé, pas testé ou pas dessiné |
| `TEXTES-DES-MAQUETTES.md` | Tous les textes visibles dans chaque maquette, relevés automatiquement |
| `maquettes/INDEX.md` | La liste des 58 maquettes, avec leur thème, leur téléphone et la section correspondante |
| `maquettes/images/` | Une image par maquette |
| `maquettes/sources/` | Le fichier source de chaque maquette, pour lire les valeurs exactes |
| `logo/` | Le logo, sur fond clair et sur fond sombre |
| `police/` | La source de la police et ce que demande sa licence |
| `A-AJOUTER-DANS-CLAUDE.md` | Les lignes à copier dans le fichier `CLAUDE.md` du dépôt |
| `annexes/journal-des-decisions.md` | L'historique de la conception. À ne pas implémenter tel quel |

## Ordre de lecture

1. Ce fichier.
2. `SPECIFICATION.md`, sections 1 à 3 : ce que fait l'app, les règles communes, le parcours.
3. `VALEURS-DE-DESIGN.md`.
4. `POINTS-OUVERTS.md`, pour savoir ce qu'il ne faut pas tenir pour acquis.
5. Ensuite, pour chaque écran à construire : sa section dans `SPECIFICATION.md`, ses images, puis son fichier source pour les valeurs exactes.

## Ce qui fait foi

1. **Pour les écrans, les textes, les parcours et l'apparence, ce dossier l'emporte** sur les anciens fichiers de spécification du projet (`SPEC_ANDROID.md`, `SPEC_CORE_KMP.md`, `SPEC_IOS.md`). Ces fichiers ont été volontairement laissés de côté pendant la conception. Ils restent utiles pour la technique. En cas de contradiction sur un écran ou un texte, c'est ce dossier qui gagne. C'est une décision de Mehdi, prise le 9 octobre 2026.
2. **`SPECIFICATION.md` et les images doivent dire la même chose.** Si elles se contredisent, ne pas choisir : s'arrêter et poser la question à Mehdi, en citant le fichier et le passage.
3. **Rien de ce qui figure dans `POINTS-OUVERTS.md` n'est une décision.** Ne pas inventer de réponse. Poser la question, ou laisser le point de côté et le signaler.
4. **Le journal en annexe n'est qu'un historique.** Il contient des versions abandonnées et des tableaux dépassés.

## Comment se servir des maquettes

- **Ce sont des références visuelles, pas du code à recopier.** Les fichiers sources sont des pages web écrites pour un écran de 390 × 844. Il faut reconstruire chaque écran avec les outils propres à chaque téléphone, et le faire s'adapter aux autres tailles d'écran.
- **L'image montre le résultat attendu. Le fichier source donne les valeurs.** Couleurs, tailles de texte, marges, arrondis et hauteurs s'y lisent dans les attributs `style`.
- **Les textes se recopient depuis `SPECIFICATION.md` ou `TEXTES-DES-MAQUETTES.md`**, pas depuis l'image. Les textes de l'app sont en français et tutoient l'utilisateur.
- **Une partie des maquettes montre des éléments que le système dessine lui-même sur iPhone** : la feuille « Prêt à scanner », la liste des applications à bloquer, l'écran de blocage, la première présentation de l'alarme. Les maquettes en donnent une approximation. Il ne faut pas chercher à les reproduire au pixel près sur iPhone. Sur Android, c'est Niumi qui les dessine, à l'identique de la maquette.
- **Le thème dépend de l'état de l'app, pas du réglage du téléphone.** Clair hors session, sombre pendant la session. Les deux jeux de couleurs sont dans `VALEURS-DE-DESIGN.md`.
- **Quelques astuces de dessin ne sont pas à reprendre** : le fondu ambre du cadran est fait de petits segments, les icônes d'applications sont des carrés gris, le clavier est un rectangle marqué « Clavier du téléphone ».
- **Les fichiers sources contiennent des libellés d'accessibilité** (attributs `aria-label`). Ils peuvent servir de point de départ. Ils ont été écrits par Claude et n'ont pas été relus.
- **Les fichiers sources chargent la police depuis Google Fonts et le logo depuis `../../logo/`.** Ouverts dans un navigateur sans connexion, ils s'affichent avec une police de remplacement. Les images, elles, ont été produites avec la vraie police.

## Ce que le dossier ne contient pas

- Aucun code de l'app.
- Les fichiers de la police. Voir `police/LISEZ-MOI.md`.
- Une version vectorielle du logo. Les deux images de `logo/` sont celles des maquettes (684 × 168 pixels). Celle pour fond sombre a un fond opaque `#0E0E10`. Si Mehdi a un fichier vectoriel du logo, il vaut mieux l'utiliser.
- Les images du guide des autorisations, les rubriques d'aide non écrites et les autres éléments listés dans `POINTS-OUVERTS.md`, section 4.

## Comment ce dossier a été vérifié

- Les 58 images ont été produites à partir des fichiers du canevas, avec la police IBM Plex Sans, puis toutes regardées.
- Les textes de `TEXTES-DES-MAQUETTES.md` sont extraits des fichiers sources par un programme, pas recopiés à la main.
- Les textes de `SPECIFICATION.md` ont été comparés par un programme à ceux des maquettes.
- Les valeurs de `VALEURS-DE-DESIGN.md` sont relevées dans les fichiers sources.
- Les écarts trouvés entre l'ancien document et les maquettes sont listés dans `POINTS-OUVERTS.md`, section 5.
- Une relecture séparée a ensuite comparé la spécification et les valeurs de design au journal et aux fichiers sources. Ses corrections sont intégrées.

Ce qui n'a pas été vérifié : rien n'a été testé sur un téléphone. Les contraintes d'iPhone et d'Android citées dans la spécification viennent de la documentation et des forums d'Apple et de Google, lus par extraits. Les liens sont à la fin de `SPECIFICATION.md`.
