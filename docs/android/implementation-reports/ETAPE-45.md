# Étape 45 — Écran 6 · Accueil à trois états, cadran, Ponctuel / Quotidien, sélecteur d'heure

Date : 2026-10-25. **Code, tests et essais manuels faits ; restent les deux nuits réelles.** Tests JVM,
tests instrumentés des cinq modules touchés, ktlint, detekt, lint et assemblage verts ; essais manuels
1 à 8 conformes sur Xiaomi 25080RABDG, Android 16 (HyperOS). Treizième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 14, phase Q). Point de départ : `3b29745`.
Rien n'est committé.

## Ce qui est livré

- **Moteur commun (contrat 1.5 amendé, SPEC_CORE_KMP §8.4).**
  - Aucun jour coché = réveil **ponctuel** : la prochaine heure de réveil, n'importe quel jour.
  - Deux heures identiques = période de **24 h**, coucher la veille.
  - `NO_DAY_ENABLED` et `BEDTIME_EQUALS_WAKE` disparaissent ; `EMPTY_PERIOD` garde le seul refus
    restant, le trou de printemps qui confond coucher et réveil. Fixtures partagées mises à jour.
- **Planning (`:core:system`).**
  - Un réveil ponctuel terminé (`COMPLETED`, scan ou recopie du matin) éteint l'interrupteur.
  - `PlanApplyResult.NoDayEnabled` retiré ; `nextOccurrence(plan, zone, now)` rendue publique pour
    l'accueil.
  - `SetupPreferences.setupCompleted` (écrite par « Continuer » de l'écran 4) et `SetupCompletion`
    (installation antérieure : finie avec au moins un Niumi Spot et une application). Les deux
    préférences de l'ancien écran 5 ne sont plus lues.
  - Notification de la garde avant le coucher : le jour quand le réveil n'est pas aujourd'hui
    (« Réveil lundi à 7:00 », « …bloquées samedi à partir de 11:45. »), heures sans zéro devant.
- **Design system.** `NiumiDial` (cadran 24 h, glisser de 5 en 5, toucher bref pour le sélecteur,
  sémantique des poignées), `DialGeometry` et `DialLabelLayout` (logique pure), `NiumiTextTabs`,
  `NiumiWheelPicker`, `NiumiTimePickerDialog`, `NiumiToast`, icônes d'applications partagées
  (`NiumiAppIcon`, `NiumiAppIconCache`, règle « +N »), cadenas épais ambre.
- **Accueil (`:feature:session/home`).**
  - Trois états, plus `NothingArmed` (Q14) : « Réveil prévu », « Réveil désactivé », « Blocage en
    cours » (sombre, champs verrouillés, bulle M10), « Aucun réveil ni blocage prévu » si l'armement
    a échoué.
  - Chaque réglage passe par `WakePlanScheduler.apply` ; l'accueil n'écrit jamais d'état de session.
  - Battement à la minute (Q13) ; comptes à rebours « 56 min », « 9 h 26 », « 2 j 4 h ».
  - Après un réglage, la surveillance §13.1 est évaluée aussitôt.
  - Page transitoire des réglages (Niumi Spots, Diagnostic, Aide et limites), jusqu'à l'étape 48.
- **Écran 4.** Modifiable jusqu'au coucher (décision 11 amendée), réarmement `APP_SELECTION` après
  « Valider », flèche de retour depuis l'accueil (M11).
- **Navigation (`:app`).** L'accueil est la destination dès que la mise en route est finie ou qu'une
  session est en cours ; sinon présentation ou écran 2. `HomeGateViewModel`, `NiumiNavHost` réécrit.
- **Supprimés.** Écrans 5, 6 et 7 de l'ancien parcours (`WakeTime`, `Summary`, `ActiveSession`),
  `ArmSessionUseCase` et l'activation ponctuelle, l'ancien accueil de `:app`.

## Décisions de Mehdi (2026-10-25)

Avant le code (plan) : onglets Ponctuel / Quotidien (D17 réglé), contrat KMP amendé, écart minimal
d'une minute et 24 h acceptées, sélecteur d'heure et geste du cadran, bulle M10 et battement Q13,
réglages transitoires, `setupCompleted`, M11, Q14 à l'accueil, régression D23 des maquettes
régénérées non appliquée. Pendant les essais sur appareil :

1. **Zone du cadran agrandie** à 340 (bande de 40 + écart de 4 au-dessus et au-dessous de l'anneau) :
   une étiquette en haut ou en bas recouvrait l'anneau.
2. **Étiquettes** jamais sur l'anneau, l'une sur l'autre ou sous l'icône des réglages ; elles
   s'écartent ensemble et gardent l'ordre de leurs poignées.
3. **Cadran descendu de 32** sous la marge du haut.
4. **Espaces resserrés** (cadran → carte 12, carte → onglets 8, onglets → pastilles 0, pastilles →
   lignes 20, lignes 76 et 64, onglets à 16 d'écart), plus de trait sous la dernière ligne ;
   défilement accepté sur petit écran.
5. **Comptes à rebours** d'un jour ou plus en « 1 j 4 h ».
6. **Flèche** vers le point de l'heure actuelle ; point blanc sur l'arc de la nuit ; fondu ambre de
   12° droit dès le point en session (34° en demi-cosinus dans les maquettes).
7. **Garde dès l'armement, avec le jour** dans sa notification ; jamais « demain », qui deviendrait
   faux après minuit (la notification n'est pas reposée) : le jour de la semaine (« Réveil lundi à
   7:00 »).
8. **Défilement** : toute la page défile, cadran compris.
9. **Ordre du plan conservé** : alerte d'autorisations et bandeau à l'étape 52.

## Écarts au plan

1. **Contrat KMP** : 24 h avec des jours cochés n'est pas toujours immédiate (seul le ponctuel l'est).
2. **Rangées des roues** de hauteur égale (42) au lieu de 36/40/56/40/36.
3. **`DialStateTest` fondu dans `DialGeometryTest`** : la logique testable est dans `DialGeometry`.
4. **Quatre tailles ajoutées** à `valeurs-de-design.json` (roues 34/22/18, onglet 14), exigées par
   `NiumiTypeTest`, et le fondu (12°, droit) amendé avec une note.
5. **« Valider » rend les deux heures** : les onglets gardent le choix en cours.
6. **Compte à rebours arrondi à la minute supérieure** : jamais « 0 min » avant l'heure.
7. **Mise en route sans accueil dessous** : le retour de l'écran 2 en racine quitte l'app.
8. **Préférences de l'ancien écran 5 supprimées.**
9. **Paragraphes des écrans 5/6/7** gardés dans SPEC_ANDROID §15 sous mention « supprimés » (règles
   d'incident reprises à l'étape 52).

## Relecture indépendante

Relecture du code par un agent distinct (lecture seule) : 3 défauts importants, 12 mineurs.

- **Corrigés, chacun avec un test vu en échec :** « Valider » sans changement ne fermait pas le
  sélecteur ; un glissé hors poignée ou un défilement ouvrait le sélecteur ; une erreur d'écriture
  du planning faisait planter l'accueil et l'écran 4 (la fuite faisait aussi échouer
  `RingtoneViewModelTest`) ; « 24 h 00 » au compte à rebours.
- **Corrigé à la demande de Mehdi, avec un test vu en échec :** le remplissage ambre en session,
  un seul dégradé sur un tour, ne tenait pas au-delà de 338° écoulés (période de 24 h, dernière
  heure et demie) ; découpé en une partie pleine et un fondu dessiné à part (`AmberSpans`).
- **Corrigés sans test JVM (interface), constatés sur appareil :** roue qui n'enregistrait sa valeur
  qu'à l'arrêt ; nœuds TalkBack des poignées décalés de 24 dp ; aperçu figé si le geste est annulé ;
  état par défaut affiché au premier rendu ; « Continuer » depuis Réglages → Niumi Spots.
- **Mineurs laissés** :
  - en 24 h, les poignées se superposent et celle du coucher ne se saisit qu'au sélecteur ;
  - TalkBack ne passe pas minuit sur une poignée ni sur la roue des heures ;
  - une ancienne session ponctuelle de l'écran 6 transitoire est désarmée dès qu'on touche un
    réglage en « Réveil désactivé » (cas transitoire) ;
  - `SetupCompletion` déduit sans mémoriser (cas pratiquement inatteignable) ;
  - cache d'icônes par défaut global, icône illisible mémorisée.

## Tests

- **JVM** : 1 628 tests, aucun échec (KMP 306, Android 1 322). Moins qu'à l'étape 44 (1 759) : les
  tests des écrans 5, 6 et 7 sont partis avec eux.
  - Nouveaux : `DialGeometryTest`, `DialLabelLayoutTest`, `NiumiAppSlotsTest`, `NiumiDialDimensTest`,
    `HomeTextsTest`, `HomeViewModelTest`, `TransitionalSettingsTextsTest`, `SetupCompletionTest`,
    `GuardMomentTest`, `HomeDestinationTest` réécrit ; `NextOccurrenceCalculatorTest`,
    `WakePlanSchedulerTest`, `SessionCoordinatorRearmTest`, `AppSelectionViewModelTest`,
    `BlockingGuardNotificationSpecsTest`, `WakeScheduleFormatterTest` étendus.
  - Échecs vus avant implémentation, sauf les exceptions notées au registre : `NiumiDialDimensTest`
    et `TransitionalSettingsTextsTest` écrits après le code ; `HomeDestinationTest` passé au premier
    lancement (le module ne compilait pas sans le nouveau graphe).
- **Instrumentés** (Xiaomi 25080RABDG, Android 16) : `:core:designsystem` 65, `:core:system` 36,
  `:feature:session` 26, `:feature:setup` 56, `:app` 17 — tous verts. Premier passage : un échec réel
  (`HomeScreenTest`, centre du cadran invisible à TalkBack hors session), corrigé puis revérifié.
- **ktlint, detekt, `:app:lintDebug`, `:app:assembleDebug`** : verts.

## Validation sur appareil

Faite le 2026-10-25 sur Xiaomi 25080RABDG, Android 16, HyperOS. Essais conformes, selon Mehdi :

1. fin de mise en route → « Réveil désactivé », retour qui quitte l'app, réouverture sur l'accueil ;
2. interrupteur → carte noire, « Réveil dans … », arc noir, poignée ambre ;
3. période déjà en cours → accueil sombre immédiat, cadenas, verrouillage et bulle, écran de blocage ;
4. sélecteur d'heure (onglet selon la poignée, 22:31, croix sans effet) ;
5. 24 h → arc plein, « 24 h de blocage », blocage immédiat ;
6. écran 4 depuis l'accueil, flèche, retour, réarmement ;
7. comportement de l'accueil avec l'interrupteur et la surveillance §13.1 ;
8. réveil ponctuel jusqu'au scan → interrupteur éteint ;
9. après les derniers correctifs : fondu ambre sur une nuit normale et en 24 h (fin de période), et
   notification « Réveil dimanche à 0:30 », jamais « demain ».

**Constats et limites :**

- **Retirer une autorisation d'exécution coupe le blocage.** Couper les notifications de Niumi fait
  tuer le processus par Android (`am_kill … PermissionHelper`, 13:43:06) ; le service d'accessibilité
  est alors marqué planté et n'est plus relancé avant une réactivation manuelle. Même règle qu'au
  balayage hors session (A8). La notification §13.1 le signale ; l'alerte de l'étape 52 devra le dire.
- **Q14 non provoquable sur appareil par l'autorisation d'alarme.** Niumi déclare `USE_EXACT_ALARM`,
  accordée d'office et non retirable : l'échec d'armement reste prouvé par les tests JVM seulement.
- **Une réinstallation de l'app désactive le service d'accessibilité** ; les tests instrumentés de
  `:app` le laissent planté. À réactiver avant tout essai.
- Le bandeau de problème et l'alerte d'autorisations ne sont pas encore dans l'app : étape 52.

**Restent non validés :**

- deux nuits L–V réelles (réveil, scan, « Réveil prévu » pour le lendemain) ;
- passage TalkBack complet sur le cadran et le sélecteur (centre et poignées corrigés, à rejouer à
  l'étape 55) ;
- restes de l'étape 44 : `TIMEZONE_CHANGED` réel, réarmement au déverrouillage après redémarrage,
  `DISARMED` puis `ARMED` constatés en base ;

## Specs et documents modifiés

- `design-plan/` : SPECIFICATION §2, §3, §8, §9, §13, §15 ; TEXTES-DES-MAQUETTES (sections 18 à 20,
  renumérotation) ; VALEURS-DE-DESIGN §2 à §5 ; POINTS-OUVERTS (D17, M10, M11, Q8, M14, M15, E13,
  E14) ; `valeurs-de-design.json` (quatre tailles, fondu).
- SPEC_CORE_KMP §8.4, §14, §17, §19 ; SPEC_IOS §11, §15 ; SPEC_ANDROID §8, §9.2, §9.3, §10.4,
  §12.1, §12.5, §13.1, §15, §19.1.
- Plan de refonte : décisions, interfaces transverses, étape 45 (écarts), étapes 46 à 52 (maquettes
  renumérotées), étape 50 (roue réutilisée), étape 52 (alerte et service d'accessibilité).
- Signalé à Mehdi, non appliqué : les maquettes régénérées le 2026-10-25 reviennent sur D23
  (« session ») dans 05, 17, 24 à 27, 29 à 33 et l'index (E14).
