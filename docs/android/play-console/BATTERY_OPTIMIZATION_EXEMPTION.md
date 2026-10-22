# Justification Play Console — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

Document préparatoire à la revue Google Play de la demande directe d'exemption d'énergie
(politique « Abus d'appareil et de réseau », règle Android : la demande directe n'est admise que
si la fonction principale de l'application est dégradée par Doze et App Standby). Statut :
préparatoire, pas encore soumis. Décision de Mehdi du 2026-10-21, étape 36 du plan de refonte.

## Fonction principale concernée

Niumi bloque, pendant la nuit, les applications que l'utilisateur a lui-même choisies, jusqu'au
scan d'un Niumi Point le matin. Le blocage repose sur un service d'accessibilité
(`ACCESSIBILITY_DECLARATION.md`) qui doit recevoir les changements d'application au premier plan
pendant toute la session.

## Pourquoi la fonction est dégradée sans exemption

Mesuré sur appareil réel (Xiaomi 25080RABDG, Android 16, HyperOS ; `ETAPE-05.md`,
SPEC_ANDROID §13) : tant que Niumi reste soumis aux restrictions de batterie, le système gèle son
processus environ une minute après son passage en arrière-plan et cesse de lui remettre les
événements d'accessibilité. Le service apparaît actif, aucune erreur n'est produite, mais le
blocage devient **silencieusement inopérant** : l'utilisateur pose son téléphone, puis ouvre une
application bloquée, qui s'ouvre. Mesuré le 2026-09-28 : inscrit dans la liste blanche, Niumi
renvoie une application bloquée à l'accueil en 206 ms, même classé inactif par HyperOS.

FCM ne s'applique pas : Niumi n'a aucun serveur et ne déclare pas `INTERNET`
(`PRIVACY_POLICY.md`). Rien ne peut réveiller l'application de l'extérieur.

## Pourquoi la demande directe plutôt que la liste système

La liste `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` reste le repli. Sur Android 16 (mesuré le
2026-10-21), elle demande trois touchers dans trois écrans différents, pour un réglage que
l'utilisateur ne connaît pas ; un utilisateur qui s'y perd garde un blocage qui ne tient pas.

## Ce que Niumi fait et ne fait pas

- La demande n'est émise que depuis la tuile « Batterie » de l'écran des autorisations, après
  un toucher sur « Autoriser » sous la phrase « Retire les restrictions de batterie, pour que le
  téléphone n’arrête pas Niumi pendant la nuit. » (revue le 2026-10-22), et depuis le recours de l'écran de session quand l'exemption a été perdue.
- Jamais au démarrage, jamais en boucle, jamais sans action de l'utilisateur.
- Un refus ne déclenche rien : la tuile reste simplement à faire.
- Niumi ne demande l'exemption que pour lui-même (`package:com.niumi.app`).

## Précédents observés

Applications publiées sur le Play Store qui déclarent la même permission (relevé du 2026-10-21
sur l'appareil de test) : bloqueurs d'applications à service d'accessibilité et applications de
réveil, dont Brick, AppBlock et Alarmy.

## Si la revue refuse

Retirer la permission du manifeste et la ligne de `ReleaseHygieneTest`, faire pointer
`settingsIntentFor(OpenBatterySettings)` sur la liste système (le repli existe déjà), puis
resoumettre ; la version déjà publiée n'est pas touchée. Un appel avec ce document reste possible.
