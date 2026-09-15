# Limites de Niumi sur Android

Ce fichier **est** le texte de l'écran « Aide et limites » de l'application (SPEC_ANDROID §15,
écran 13). Les deux ne peuvent pas diverger : `HelpTextsTest` vérifie que chaque section `##` et
chaque puce `-` de ce document correspond mot pour mot à `HelpTexts`. Modifier l'un sans l'autre
fait échouer le build.

Il ne liste que des limites **mesurées ou imposées par Android**, jamais des précautions de
principe. Chaque point renvoie à la section de spécification qui l'établit.

## Ce que Niumi ne peut pas garantir

- Un arrêt forcé depuis les réglages supprime le réveil programmé : Niumi ne sonnera pas.
- Le système peut arrêter le service qui fait sonner l'alarme. Niumi ne peut pas l'en empêcher.
- Le mode Ne pas déranger en silence total coupe le son de l'alarme et empêche l'écran de réveil de s'afficher. Niumi refuse d'activer une session dans cet état, et ne modifie jamais ce réglage à ta place.
- Le service d'accessibilité peut être désactivé à tout moment dans les réglages Android, ce qui arrête le blocage.
- Le blocage renvoie les applications choisies à l'accueil ; il ne les rend pas impossibles à ouvrir. Désactiver le service, arrêter Niumi ou le désinstaller suffit à le contourner.
- Le scan sur écran verrouillé n'est pas garanti : ton téléphone peut exiger un déverrouillage avant de lire le boîtier.
- Il n'existe aucun secours logiciel pendant une session : ni code, ni délai, ni bouton « Arrêter quand même ». Sans ton boîtier, il te reste l'arrêt forcé ou l'extinction du téléphone.
- Si une permission est retirée ou le volume d'alarme coupé après l'activation, Niumi le signale par un incident. Il ne peut pas le corriger seul.

## Après un redémarrage ou une mise à jour

- Un redémarrage ne perd pas ton réveil : Niumi le reprogramme avant même le premier déverrouillage.
- Sur Xiaomi et les surcouches proches, un réglage « Démarrage automatique en arrière-plan » décide si Niumi peut démarrer seul. Mesuré : il ne bloque pas la reprogrammation du réveil après un redémarrage, mais il empêche Niumi de se remettre à jour tout seul après une mise à jour de l'application.
- Une mise à jour de Niumi peut réinitialiser l'exemption d'énergie. Le diagnostic la revérifie avant chaque session.
- Juste après un redémarrage, le diagnostic peut annoncer le NFC désactivé alors qu'il ne l'est pas : la pile NFC du système n'a pas fini de démarrer. Rouvrir Niumi corrige l'affichage.

## Notifications et avertissements

- La notification qui te demande de scanner ton boîtier peut être balayée. Depuis Android 14, Niumi ne peut pas l'en empêcher. Elle revient dès que tu rouvres l'application, et l'écran de blocage te rappelle le scan.
- Si un réglage casse ton réveil après l'activation, Niumi t'avertit au plus tôt, jamais immédiatement : le système peut l'avoir arrêté entre-temps.

## Journal et diagnostic

- Le journal technique garde les 200 derniers événements, sur ton téléphone uniquement. Rien n'est envoyé, jamais.
- Les événements écrits avant ton premier déverrouillage après un redémarrage peuvent être perdus si Niumi s'arrête avant que tu déverrouilles. Les incidents, eux, ne sont jamais perdus.
