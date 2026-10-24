# Étape 40 — Feuille « Sonnerie » et ligne « Sonnerie »

Date : 2026-10-23. **Étape terminée** : code, tests JVM et instrumentés, ktlint, detekt, lint,
assemblage, et essais manuels sur appareil. Huitième étape du plan de refonte
`docs/superpowers/plans/2026-10-10-refonte-design.md` (Lot 11, phase N). Point de départ : `c514ac8`.

## Ce qui est livré

- **Feuille « Sonnerie »** (`:feature:session`, `ringtone/RingtoneSheet.kt`, maquettes 16 et 17 de l'ancienne numérotation,
  `design-plan/SPECIFICATION.md` §10), qui remplace l'écran 14 (Lot 7). Titre « Sonnerie » et croix
  « Fermer » (`NiumiBottomSheet`).
  - Section « Son » : les quatre sonneries de `NiumiRingtones` (Cloche, Énergique, Oiseaux, Piano),
    une radio par ligne (`NiumiRadioIndicator`, nouveau composant), nom en gras si choisi, bouton
    rond de pré-écoute par ligne (`NiumiRoundIconButton`, nouveau composant).
  - Section « Sonnerie progressive » : cinq pastilles « Non, 2 min, 5 min, 10 min, 15 min »
    (`NiumiPillButton`, existant), mention « Volume max en {durée} » sous les pastilles, absente
    sur « Non ».
  - Ligne « Volume du téléphone » : pourcentage du flux `STREAM_ALARM`
    (`AlarmVolumeSource.alarmStreamPercent()`, nouveau), sans curseur ; un toucher ouvre une bulle
    (`NiumiBubble`, nouveau composant, positionnée au-dessus et alignée à droite par un
    `PopupPositionProvider`) « Il se règle dans les réglages de ton téléphone. », qui disparaît au
    toucher ailleurs (`PopupProperties.dismissOnClickOutside`).
  - Palette sombre en session, comme le reste de l'app : aucune logique propre, `ink`/`onInk`
    s'inversent déjà avec le thème (M7).
- **Ligne « Sonnerie »** (`AlarmSoundRow`, `:feature:session/ui`), partagée par les écrans 5, 6 et
  7 : le titre à gauche, à droite le nom du son et, dessous, « Progressive · {durée} » (absent sur
  « Non »). `AlarmSoundTexts.summary()` renvoie désormais `AlarmSoundSummary(name, detail)` plutôt
  qu'une chaîne unique, et assainit ses réglages avant de résumer.
- **`VolumeRampDurations.sanitized(seconds: Int?): Int?`** (`:core:system`) : une durée hors de la
  liste fermée prend la plus proche (égalité vers la plus courte), `null` reste `null`. Appelée par
  `AlarmSoundSettings.sanitized()` et par `RingingSoundResolver` (la montée jouée suit désormais la
  même règle que celle affichée).
- **`AlarmVolumeSource.alarmStreamPercent(): Int?`** (nouveau, `:core:system`), dérivé d'une
  fonction pure `alarmVolumePercent(volume, max)` testée en JVM.
- La feuille s'ouvre désormais **par-dessus** l'écran qui la présente (écran 5, et écran 7 en
  `ARMED`), plutôt que par une navigation : `NiumiRoute.Ringtone` et sa destination sont retirées,
  le compte des destinations retombe à quatorze (§15). `RingtoneScreen.kt` supprimé, remplacé par
  `RingtoneSheet.kt` (composable pur) et `RingtoneSheetRoute.kt` (point d'entrée réel).

## Décisions de Mehdi (2026-10-23)

- **Q7** : les durées restent celles mesurées à l'étape 28 (Non, 2, 5, 10, 15 min) plutôt que
  celles de la maquette (Non, 1, 2, 5, 10 min), qui n'avait pas été mise à jour après le
  resserrement du 2026-10-08 (1 min jugée trop rapide). Signalé avant implémentation
  (`CLAUDE.md`, conflit entre une demande et une spécification).
- **D7** : les quatre sons déjà empaquetés (Cloche, Énergique, Oiseaux, Piano), sans cinquième son.
- **D5, D6** : avec « Non », la ligne de l'accueil n'affiche que le nom du son, et la mention sous
  les pastilles disparaît.
- **M7** : la feuille suit la palette sombre du thème en session, comme la feuille de scan.

## Écarts au plan

- `AlarmSoundRow` garde son style Material 3 simple (pas `NiumiListRow`) : les écrans 5, 6 et 7
  restent des écrans transitoires (remplacés à l'étape 45), et la ligne y vit déjà ainsi depuis le
  Lot 7 — seul son contenu change (nom + détail au lieu d'une phrase unique), pas son habillage.
- `NiumiBubble` est positionnée par un `PopupPositionProvider` sur mesure (ancré au-dessus et à
  droite de sa cible) plutôt que par les paramètres `alignment`/`offset` par défaut de `Popup`, qui
  ne permettent pas d'exprimer « au-dessus », seulement un alignement sur les bords de l'ancre.

## Défaut trouvé et corrigé pendant la validation sur appareil (2026-10-23)

Le premier passage de `RingtoneSheetTest.touchingTheVolumeRowOpensTheBubbleAndTouchingElsewhereClosesIt`
sur appareil (Xiaomi 25080RABDG / Android 16 / HyperOS) a échoué : la bulle ne se fermait jamais au
toucher extérieur. Reproduit ensuite par un toucher réel (`adb shell input tap`, hors du cadre de
test), dans la feuille et dans un composant `NiumiBubble` isolé, donc un vrai défaut, pas un artefact
de test.

**Root cause (démarche systématique, `PopupLayout.onTouchEvent` décompilé depuis
`androidx.compose.ui:ui-android:1.12.1`, aucune source disponible en cache local) :** le dismiss
extérieur d'un `Popup` Compose repose sur `ACTION_OUTSIDE`, livré par le `WindowManager` à une
fenêtre *touch-modal* munie de `FLAG_WATCH_OUTSIDE_TOUCH` (toujours posé par `createFlags`,
indépendamment de `focusable`). Sur ce HyperOS, une fenêtre non focusable
(`PopupProperties.focusable = false`, le défaut) ne recevait pas cet événement de façon fiable.

**Correctif, vérifié par un toucher réel avant et après :** `NiumiBubble` passe
`properties = PopupProperties(focusable = true)`. Confirmé sur appareil, dans la feuille et depuis
l'écran 7 en session : le toucher ailleurs ferme la bulle sans fermer la feuille, sans effet sur le
clavier (aucun champ de saisie n'est concerné).

**Limite distincte, qui subsiste après le correctif :** l'injection synthétique de `ComposeTestRule`
(`performTouchInput`) ne déclenche jamais `ACTION_OUTSIDE` d'une fenêtre à l'autre, même avec
`focusable = true` — vérifié après coup. `RingtoneSheetTest` ne peut donc prouver que l'ouverture au
toucher et la fermeture par un second toucher de la même ligne (même fenêtre) ; le toucher extérieur
reste vérifié à la main (voir « Validation sur appareil »).

## Specs et documents modifiés

- `specs/SPEC_ANDROID.md` : §3 (inchangé en substance, les durées ne changent pas), §10.2 (confirmation
  du 2026-10-23, sanitisation dans `RingingSoundResolver`, pré-écoute), §15 (paragraphe « Lot 7 »
  réécrit : feuille, ligne, résumé en deux parties, compte des destinations), §19.1 et §20
  (nouveaux tests).
- `design-plan/POINTS-OUVERTS.md` : D5, D6, D7, M7 tranchés le 2026-10-23.
- Plan de refonte : décisions Q7, D7, D5, D6, M7 marquées « Tranché » dans la table, point de
  vigilance 4 et cas limite 5 mis à jour, cases de l'étape 40 cochées.

## Tests

- **JVM** : 1 577, aucun échec. Nouveaux ou étendus : `VolumeRampPolicyTest` (+4, `sanitized`),
  `AlarmSoundSettingsTest` (+2), `RingingSoundResolverTest` (+1, durée héritée assainie),
  `AlarmVolumePercentTest` (nouveau, 4), `AlarmSoundTextsTest` (réécrit pour `AlarmSoundSummary`),
  `RingtoneTextsTest` (réécrit), `RingtoneViewModelTest` (pastille unique, `volumePercent`),
  `NiumiRouteTest` (route `Ringtone` retirée), `ActiveSessionViewModelTest`,
  `SummaryViewModelTest`, `WakeTimeViewModelTest` (résumé en deux parties).
- **Instrumenté** : `:feature:session:connectedDebugAndroidTest`, 20/20 verts (Xiaomi 25080RABDG /
  Android 16 / HyperOS). `RingtoneSheetTest` (5 essais) : quatre sons et cinq pastilles exposés en
  radio, mention masquée sur « Non », bulle ouverte au toucher puis fermée par un second toucher de
  la même ligne, croix qui ferme, contrôles d'accessibilité. Le toucher extérieur n'y est pas
  prouvé (limite du cadre de test, voir plus haut) ; premier passage sans `focusable = true` :
  1 échec exact sur ce point, résolu par le correctif.
- **ktlint, detekt, lint, `assembleDebug`** : verts. `grep -rn "RingtoneScreen\|NiumiRoute.Ringtone" androidApp`
  vide ; `grep -rn "Color(0x" androidApp --include=*.kt` ne sort que `NiumiPalettes.kt`.

## Validation sur appareil

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS, service d'accessibilité réactivé après
les essais instrumentés (`settings put secure enabled_accessibility_services`, sans `force-stop`
après, qui le désactive à nouveau sur cet appareil). Parcours réel jusqu'à l'écran 5, préférence
déjà « Piano · Progressive · 2 min » d'une étape précédente.

| Essai | Résultat |
| --- | --- |
| Ouverture de la feuille depuis l'écran 5, rendu | **OK** : fidèle aux maquettes 16/17 (ancienne numérotation) |
| Choisir « Oiseaux » et la pastille « 5 min » | **OK** : mention « Volume max en 5 min » à jour |
| Fermer, ligne de l'écran 5 | **OK** : « Oiseaux » / « Progressive · 5 min » |
| Pourcentage du volume | **OK** : « 40 % » = 6/15 (`dumpsys audio`, `STREAM_ALARM`) |
| Pré-écoute d'« Oiseaux » (icône lecture → arrêt) | **OK** : icône change ; arrêtée à la fermeture (`onPause`) |
| Choisir « Non » puis fermer | **OK** : ligne de l'écran 5 affiche « Oiseaux » seul, sans détail (D5, D6) |
| Toucher la ligne du volume (bulle), toucher « Son » dans la feuille | **Échoué puis OK** : voir « Défaut trouvé et corrigé » ; la feuille reste ouverte, seule la bulle se ferme |
| Toucher le voile, en dehors de la feuille | **OK** : ferme toute la feuille (comportement du `ModalBottomSheet`, inchangé) |
| Ouvrir la feuille depuis l'écran 7 en session (`ARMED`) | **OK** : palette sombre (M7), fidèle à la feuille claire |
| Changer la durée (« 2 min ») depuis l'écran 7, fermer | **OK** : ligne de l'écran 7 affiche aussitôt « Progressive · 2 min » (session mise à jour, pas seulement la préférence) |

**Non fait** : passage TalkBack complet (geste par geste) ; les contrôles automatiques de
l'Accessibility Test Framework sont verts dans `RingtoneSheetTest`, mais aucune navigation au
lecteur d'écran n'a été dérouée à la main.

## Révision du 2026-10-23 : interrupteur « Sonnerie progressive »

Après la validation, Mehdi a redessiné la feuille et déposé un nouveau dossier `design-plan/maquettes`
(61 maquettes, renumérotées ; la sonnerie passe en 18, 19 et 20). Conflit avec `SPECIFICATION.md` §10
et la maquette 16 (ancienne numérotation) signalé, déviation confirmée. Décisions du même jour :

- **Interrupteur à la place de la pastille « Non »** : ligne « Sonnerie progressive » (16 au-dessus,
  48 de haut, séparateur), sous-ligne « Volume max en {durée} » quand il est allumé, interrupteur
  de ligne **44 × 26, pastille de 20** à droite (nouveau jeton `interrupteurDeLigne`,
  `NiumiSwitch(size = NiumiSwitchSize.Row)` ; l'interrupteur général reste 52 × 32).
- **Barre segmentée** à la place des pilules (`NiumiSegmentedBar`, nouveau composant : fond « Tuile
  grise », 48 de haut, coins 14, marge 4, segments de 40 aux coins de 10, choisi en encre ; jeton
  `barreSegmentee`). Chaque segment se touche sur 48 sans épaissir la barre.
- **Durées** : Q7 tenu, 2 / 5 / 10 / 15 min (la maquette montre encore 1 / 2 / 5 / 10).
- **Allumage et défaut : 10 min partout.** Allumer l'interrupteur choisit 10 min, et
  `VolumeRampDurations.DEFAULT_SECONDS` passe de 120 à 600 (remplace la décision du 2026-09-30).
  Une session déjà armée garde sa durée (`ArmSessionUseCaseTest` : sans préférence → 600).
- Éteint : ni sous-ligne ni barre (maquette 20) ; la ligne de l'accueil n'affiche que le nom (D5).
- **« Niumi Spot »** : les 61 maquettes renomment « Niumi Spot » (42 sources). Renommage décidé,
  consigné D22, à faire dans une étape dédiée ; rien ne change dans l'app ici. La renumérotation
  des maquettes n'est répercutée que pour la sonnerie (E13).

Code : `RingtoneActions` retrouve `onVolumeRampEnabledChanged`, `onVolumeRampSelected` devient
non nul ; `RingtoneSheet.RampSection` réécrit, `RampPills` supprimé ; `RingtoneTexts.RAMP_OFF`
supprimé, `RAMP_LABELS` à quatre, `RAMP_SWITCH_DESCRIPTION` ajouté ; jetons `NiumiDimens`
`ringtoneRampRowTop/MinHeight`, `ringtoneRampSubtitleGap`, `ringtoneBarTop`, `ringtoneMessageTop`,
`switchRow*`, `segmentedBar*`, `segmentHeight` ; `NiumiShapes.segmentedBar/segment`.

Tests (JVM : 1 581, aucun échec ; ktlint, detekt, lint, `assembleDebug` verts) :
`VolumeRampPolicyTest`, `AlarmSoundSettingsTest`, `AlarmSoundTextsTest`,
`ArmSessionUseCaseTest` (défaut 600) ; `RingtoneTextsTest`, `RingtoneViewModelTest` (allumer →
600, éteindre → `null`, préférence et session) ; `NiumiShapesAndDimensTest` (deux jetons) ;
instrumentés : `NiumiComponentsSemanticsTest` (+2 : interrupteur de ligne, barre segmentée),
`RingtoneSheetTest` (quatre segments, éteindre masque sous-ligne et barre, allumer propose 10 min).

Documents : `SPECIFICATION.md` §10, `TEXTES-DES-MAQUETTES.md` 18–20, `VALEURS-DE-DESIGN.md`
§2–§4 et `valeurs-de-design.json`, `POINTS-OUVERTS.md` (D6, D22, E13), SPEC_ANDROID §3, §10.2,
§15, §19.1, §20, plan de refonte.

### Validation de la révision sur appareil (2026-10-24)

Xiaomi 25080RABDG (`lapis`), Android 16 / API 36, HyperOS. `:core:designsystem:connectedDebugAndroidTest`
56/56 et `:feature:session:connectedDebugAndroidTest` 21/21 verts (`RingtoneSheetTest` 6 essais,
`NiumiComponentsSemanticsTest` +2). La session armée la veille était terminée (voir « Restes ») ;
l'appairage, les autorisations et la préférence « Oiseaux · 2 min » étaient conservés : l'état
« préférence neuve → 10 min » n'a donc pas pu être observé directement ; il l'est par le chemin
« rallumer » (même `DEFAULT_SECONDS`) et par `ArmSessionUseCaseTest`.

| Essai | Résultat |
| --- | --- |
| Feuille depuis l'écran 5, rendu | **OK** : fidèle à la maquette 18 (ligne + interrupteur allumé, « Volume max en 2 min », barre avec « 2 min » en encre) |
| Éteindre l'interrupteur | **OK** : ligne seule, ni sous-ligne ni barre (maquette 20) |
| Fermer, ligne de l'écran 5 | **OK** : « Oiseaux » sans détail (D5) |
| Rallumer | **OK** : « 10 min » choisi, « Volume max en 10 min » |
| Choisir « 5 min » | **OK** : segment en encre, mention « Volume max en 5 min » |
| Bulle du volume : toucher la ligne, puis toucher ailleurs (la barre) | **OK** : s'ouvre, se ferme ; la feuille reste ouverte |
| Fermer, écrans 5 puis 6 | **OK** : « Oiseaux / Progressive · 5 min » sur les deux |
| Activer la session (16:20, Adobe Acrobat), feuille depuis l'écran 7 | **OK** : palette sombre, barre « Tuile grise » sombre, segment choisi inversé (règle M7) |
| Éteindre depuis l'écran 7, fermer | **OK** : ligne « Oiseaux » seule (session mise à jour) |
| Rallumer depuis l'écran 7, fermer | **OK** : « Progressive · 10 min » sur l'écran 7 |

**Non fait** : passage TalkBack complet ; état « préférence jamais réglée » observé à l'écran (voir
ci-dessus).

## Révision du 2026-10-24 : pourcentage du volume en direct

Constat de Mehdi sur l'appareil : un volume d'alarme changé feuille ouverte laissait le
pourcentage figé (relu seulement à l'ouverture et au `ON_RESUME` ; le panneau de HyperOS ne prend
pas le focus). Option retenue après explication : écouter la diffusion système plutôt que relire
toutes les 500 ms.

- `AlarmVolumeSource.changes(): Flow<Unit>` ; `AndroidAlarmVolumeSource` la branche sur
  `Context.alarmVolumeChanges()` (`audio/AlarmVolumeBroadcasts.kt`, motif de
  `nfcAdapterSettledChanges()`) : récepteur à chaud de `android.media.VOLUME_CHANGED_ACTION`,
  `RECEIVER_NOT_EXPORTED` (l'émetteur est `system_server`, livraison constatée sur l'appareil).
  L'action est `@hide` : chaîne recopiée, sans réflexion. Filtre pur
  `AlarmVolumeBroadcasts.concernsAlarm` : flux d'alarme ou extra absent (relecture par prudence).
- `RingtoneViewModel.observeVolumeChanges()` relit pourcentage et « volume à zéro », jamais la
  session ni la préférence. `RingtoneSheetRoute` borne la collecte par
  `repeatOnLifecycle(STARTED)` : le ViewModel appartient à l'écran hôte et survit à la feuille,
  une collecte dans `viewModelScope` laisserait le récepteur inscrit sur les écrans 5 et 7.
- detekt : `sessionArmedSettings()` fondu dans `load()` pour rester à 11 fonctions
  (`TooManyFunctions`, aucun précédent de suppression dans le dépôt) ; comportement inchangé,
  couvert par les tests de mode existants.
- Tests : `AlarmVolumeBroadcastsTest` (3, nouveau), `RingtoneViewModelTest` (+1 : une annonce met
  à jour 60 % puis 0 % et le message zéro, sans faire basculer la feuille vers une session armée
  entre-temps). JVM 1 585, aucun échec ; ktlint, detekt, lint, `assembleDebug` verts.
- Specs : SPEC_ANDROID §13.1 (note HyperOS), §15 (feuille Sonnerie), §19.1, §20 (nouvelle ligne) ;
  `SPECIFICATION.md` §10 (Android seulement, iPhone renvoyé à T2).

Validation sur Xiaomi 25080RABDG / Android 16 / HyperOS, feuille ouverte depuis l'écran 5 :

| Essai | Résultat |
| --- | --- |
| Récepteur inscrit, feuille ouverte (`dumpsys activity broadcasts`) | **OK** : `VOLUME_CHANGED_ACTION` dans les récepteurs de `com.niumi.app` |
| Touches de volume, puis curseur « Alarme » du panneau HyperOS (Mehdi) | **OK** : le pourcentage suit sans fermer ; 11/15 réel ↔ « 73 % » affiché |
| Curseur « Média » du panneau (Mehdi) | **OK** : pourcentage inchangé |
| Fermer la feuille | **OK** : récepteur retiré, restent `USER_UNLOCKED`, `SCREEN_OFF`, `INTERRUPTION_FILTER_CHANGED` |
| `cmd media_session volume --stream 4 --set 12` | **Sans objet** : HyperOS n'applique pas le changement (`streamVolume` reste 7), l'essai a dû se faire au doigt |

## Revue des maquettes du 2026-10-24 : ordre des lignes et sous-ligne

Mehdi a redessiné les sources 18 à 20 le même jour : la ligne « Volume du téléphone » passe
**avant** « Sonnerie progressive » (`margin-top 16` chacune, le volume avait 18 en dernière
ligne), et la sous-ligne devient « Monte jusqu’au volume du téléphone en {durée} », qui renvoie à
la valeur lue juste au-dessus. Rien d'autre ne change (bulle, barre, interrupteur, mesures).

- `RingtoneSheet` : `VolumeRow` avant `RampSection` ; `NiumiDimens.ringtoneVolumeRowTop` 18 → 16.
- `RingtoneTexts.rampMention` : nouveau texte, apostrophe typographique ; `RingtoneTextsTest` adapté
  (rouge puis vert). `RingtoneSheetTest` lit le texte par `RingtoneTexts`, inchangé.
- Documents : `SPECIFICATION.md` §10 (tableau), `TEXTES-DES-MAQUETTES.md` 18 et 20,
  `POINTS-OUVERTS.md` D6, SPEC_ANDROID §15, plan.
- Sur appareil (Xiaomi 25080RABDG / Android 16) : feuille depuis l'écran 5 fidèle aux maquettes 18
  et 19 (volume puis montée progressive, sous-ligne « Monte jusqu’au volume du téléphone en
  10 min », bulle au-dessus de la ligne du volume à sa nouvelle place, fermée au toucher ailleurs).

## Restes et points d'attention

- Passage TalkBack complet non fait (voir ci-dessus).
- Le pourcentage en direct dépend d'une diffusion `@hide` : constatée sur HyperOS / Android 16
  seulement. Un appareil qui ne la diffuserait pas retombe sur la relecture au `ON_RESUME`.
- Sessions armées pour les validations (Adobe Acrobat bloqué) : `dumpsys alarm` montre leurs
  alarmes annulées le 2026-10-23 à 17:55 et le 2026-10-24 à 13:01:11, pendant les essais de Mehdi
  et avant toute installation. Les installations n'ont pas désinstallé l'app (`firstInstallTime`
  inchangé depuis le 2026-10-23 15:37) : la phrase précédente de ce rapport, qui attribuait la fin
  de la première session à la réinstallation des tests instrumentés, était une supposition fausse.
  Aucune session n'est active sur l'appareil à la fin de la validation.
- Rien n'est committé.
