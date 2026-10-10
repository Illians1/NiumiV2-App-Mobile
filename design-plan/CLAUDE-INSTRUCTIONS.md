## Design de l'app

Le design validé de Niumi est dans `design-plan/`. Commence par `design-plan/LISEZ-MOI.md`.

- Pour les écrans, les textes, les parcours et l'apparence, ce dossier l'emporte sur `SPEC_ANDROID.md`, `SPEC_CORE_KMP.md` et `SPEC_IOS.md`. Ces fichiers restent la référence pour la technique.
- Avant de construire ou de modifier un écran, lis sa section dans `design-plan/SPECIFICATION.md`, regarde ses images dans `design-plan/maquettes/images/` et lis les valeurs dans le fichier du même nom dans `design-plan/maquettes/sources/`.
- Les couleurs, la police et les dimensions sont dans `design-plan/VALEURS-DE-DESIGN.md`. Ne mets aucune couleur en dur dans un écran : passe par un thème.
- Le thème dépend de l'état de l'app, pas du réglage du téléphone : clair hors session, sombre pendant une session.
- Les maquettes sont des références visuelles. Ne recopie pas leur code : reconstruis chaque écran avec les outils du téléphone.
- Les textes de l'app sont en français et tutoient l'utilisateur. Recopie-les depuis la spécification, sans les reformuler.
- Rien de ce qui figure dans `design-plan/POINTS-OUVERTS.md` n'est décidé. N'invente pas de réponse : pose-moi la question.
- Si la spécification et une image se contredisent, arrête-toi et pose-moi la question.
- Le fonctionnement doit rester le même sur iPhone et sur Android, sauf différence écrite dans la spécification.