# Déclaration Play Console — AccessibilityService

Document préparatoire au formulaire de déclaration de politique Play Console
(« Usage de l'API AccessibilityService »), requis pour toute application ciblant Android 12+ et
déclarant un service d'accessibilité. Rédigé pour être copié-collé dans le formulaire, section
par section. Statut : préparatoire, pas encore soumis (voir `LOT-0.md`).

## 1. Finalité déclarée

**Fonctionnalité de l'application**, pas accessibilité, pas analytique, pas sécurité, pas
publicité.

`NiumiBlockingAccessibilityService` est le mécanisme qui empêche l'utilisateur d'utiliser les
applications qu'il a lui-même choisi de bloquer pendant une session de réveil Niumi, tant qu'il
n'a pas scanné le boîtier NFC associé. C'est une fonctionnalité centrale du produit, pas un
usage accessoire.

## 2. Pourquoi une autre API ne suffit pas

Aucune API publique Android ne permet à une application tierce de détecter le changement
d'application au premier plan et d'y réagir en temps réel, hors `AccessibilityService`. Le
MVP ne recourt pas à `UsageStatsManager` (polling, latence, permission distincte à obtenir
séparément) ni à un service de type MDM (`DevicePolicyManager`, hors périmètre — SPEC_ANDROID
§2, « Le MVP ne comprend pas […] de blocage MDM »).

## 3. Ce que le service lit

Uniquement `AccessibilityEvent.packageName`, sur les types d'événement
`TYPE_WINDOW_STATE_CHANGED` et `TYPE_WINDOWS_CHANGED`.

Le service ne lit jamais :
- le contenu affiché à l'écran (`getRootInActiveWindow()`, `getText()`,
  `contentDescription()`) ;
- les saisies clavier ;
- les événements d'une autre catégorie que le changement de fenêtre.

Preuve technique, mesurée sur appareil réel (Redmi 25080RABDG, Android 16, `dumpsys
accessibility`, voir
[`ETAPE-05.md`](../implementation-reports/ETAPE-05.md#résultats-de-la-validation-sur-appareil)) :

| Attribut de configuration | Valeur déclarée | Valeur observée au runtime |
| --- | --- | --- |
| `accessibilityEventTypes` | `typeWindowStateChanged\|typeWindowsChanged` | `[TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED]` |
| `notificationTimeout` | 50 ms | 50 |
| `accessibilityFeedbackType` | `feedbackGeneric` | `GENERIC` |
| `canRetrieveWindowContent` | `false` | `capabilities=0` (aucune capacité de lecture de fenêtre accordée) |
| `isAccessibilityTool` | `false` | inchangé |

`capabilities=0` est la preuve au runtime, pas seulement déclarative, que le service ne peut
pas lire le contenu de fenêtre même s'il essayait.

## 4. Ce que le service fait de cette information

En cas de correspondance avec la liste d'applications bloquées de la session active :
`GLOBAL_ACTION_HOME`, puis affichage bref (3 secondes maximum) d'un overlay explicatif portant
le texte imposé par la spec produit :

> {Nom de l'application} reste bloquée jusqu'au scan du boîtier.

Le nom de package est journalisé localement dans l'événement technique `BLOCK_APPLIED`, seul
événement du journal (200 entrées maximum, purement local) autorisé à porter un nom de package
— voir la politique de confidentialité, `PRIVACY_POLICY.md`.

## 5. Collecte et transmission de données

- Aucune donnée personnelle ou sensible n'est collectée par ce mécanisme.
- Aucune donnée n'est transmise à un serveur, un tiers ou un service d'analytique : le parcours
  est entièrement hors ligne, l'application ne déclare aucune permission `INTERNET`.
- Le seul état conservé est le nom du package bloqué le plus récent, dans un journal local
  borné, à des fins de diagnostic sur l'appareil de l'utilisateur.

## 6. Divulgation utilisateur intégrée à l'application

La politique Play exige que la divulgation soit intégrée au parcours normal de l'application
(pas seulement dans la fiche Store), visible sans navigation particulière, et suivie d'une
action affirmative explicite avant l'octroi du service.

Écran concerné : `AccessibilityConsentScreen`
([`androidApp/feature/setup/.../AccessibilityConsentScreen.kt`](../../../androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/accessibility/AccessibilityConsentScreen.kt)),
affiché avant toute ouverture des réglages d'accessibilité. Texte reproduit ci-dessous **mot
pour mot** depuis
[`AccessibilityConsentTexts.kt`](../../../androidApp/feature/setup/src/main/kotlin/com/niumi/feature/setup/accessibility/AccessibilityConsentTexts.kt)
(source unique de vérité du texte affiché à l'écran — toute divergence entre ce document et le
code est un bug de documentation, pas une variante acceptable).

**Titre :** « Autoriser le blocage des applications »

**Les cinq points affichés :**
1. « Niumi observe le nom de l'application affichée. »
2. « Cette information sert uniquement à renvoyer les applications choisies vers l'accueil. »
3. « Niumi ne lit pas le contenu des écrans ou les saisies. »
4. « Le service peut être désactivé dans les réglages Android. »
5. « Une session ne peut pas être activée si le service est inactif. »

**Bouton :** « Ouvrir les réglages d'accessibilité »

**Mécanisme de consentement :** le bouton ouvre `ACTION_ACCESSIBILITY_SETTINGS`. L'utilisateur
active lui-même le service dans les réglages système Android — Niumi ne simule et ne peut pas
simuler ce clic (SPEC_ANDROID §12.3 : « Ne jamais simuler un consentement ou cliquer à la place
de l'utilisateur »). L'action affirmative explicite exigée par la politique Play est donc
l'activation du service par l'utilisateur lui-même dans les réglages Android, précédée de la
divulgation ci-dessus.

**`android:description` du service** (visible dans les réglages système et sur la fiche Play,
doit rester identique au texte ci-dessus — source :
[`strings.xml`](../../../androidApp/feature/session/src/main/res/values/strings.xml)) :

> « Observe le nom de l'application affichée pour ramener les applications bloquées à l'accueil
> pendant une session Niumi. Ne lit ni le contenu des écrans ni les saisies. »

## 7. Limite à ne jamais masquer (SPEC_ANDROID §4.3, §23)

Le blocage est **comportemental**, pas une garantie technique. Un utilisateur déterminé peut le
contourner en désactivant le service d'accessibilité, en arrêtant Niumi de force ou en le
désinstallant. Cette limite doit apparaître dans la fiche Play et ne doit jamais être présentée
comme résolue ou masquée, ni dans ce document, ni dans le produit.

## 8. Vidéo de démonstration

Requise par la politique : montrer la divulgation, le consentement et l'usage réel du service.
Script détaillé dans `REVIEW_VIDEO_SCRIPT.md`. **Non tournée à cette étape** — le seul parcours
d'activation existant aujourd'hui est la route de debug (`PocScreen`), qui ne représente pas
l'usage normal du produit. Le tournage est reporté à l'étape 21, sur l'application complète.
Voir `LOT-0.md` pour la justification de ce report.
