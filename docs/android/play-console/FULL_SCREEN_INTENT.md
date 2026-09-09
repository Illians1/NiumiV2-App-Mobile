# Justification Play Console — `USE_FULL_SCREEN_INTENT`

Document préparatoire à la déclaration de la permission sensible `USE_FULL_SCREEN_INTENT`
(exigence Play depuis Android 14 : justifier l'usage d'un intent plein écran, réservé aux
appels et alarmes). Statut : préparatoire, pas encore soumis.

## Usage déclaré : alarme

Le plein écran est utilisé exclusivement pour présenter `AlarmActivity` au-dessus de l'écran
verrouillé au moment du déclenchement du réveil — l'usage explicitement prévu par la politique
Android pour cette permission (« Alarms » dans la liste des cas d'usage acceptés par Google,
avec les appels entrants).

## Comment il est déclenché

La notification de sonnerie (canal `niumi_alarm_ringing`, SPEC_ANDROID §10.3) porte :

- `setCategory(NotificationCompat.CATEGORY_ALARM)` ;
- `setOngoing(true)`, sans action d'arrêt ;
- `setFullScreenIntent(fullScreenPendingIntent, true)`, ciblant explicitement `AlarmActivity`.

Aucun autre déclencheur du plein écran n'existe dans l'application : ni promotion, ni
notification marketing, ni rappel non urgent.

## Ce que la permission garantit, et ce qu'elle ne garantit pas

Le son de l'alarme (`USAGE_ALARM`, service au premier plan) démarre indépendamment de
l'autorisation plein écran — l'un ne conditionne pas l'autre techniquement. Mais la politique
produit du MVP exige l'accès immédiat à l'interface de scan NFC dès le déclenchement
(SPEC_ANDROID §10.3 : « Cette distinction doit apparaître dans le diagnostic […] »). Sans
plein écran, l'utilisateur devrait déverrouiller son téléphone et ouvrir l'application
manuellement pour scanner le boîtier, ce qui contredit le parcours garanti (§4.1).

## Comportement si l'autorisation est refusée

Sur Android 14 et plus, `NotificationManager.canUseFullScreenIntent()` est vérifié par le
diagnostic (`DeviceReadinessChecker`). Si l'autorisation manque :

- le contrôle est classé `BLOCKING_FOR_NIUMI_EXPERIENCE` (le son pourrait techniquement
  fonctionner, mais le parcours garanti de Niumi ne peut pas être tenu) ;
- l'activation de la session est refusée ;
- l'application propose l'intent `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` pour guider
  l'utilisateur vers le réglage, sans jamais l'ouvrir à sa place.

## Preuve dans le code

`RingingNotificationFactory`
([`androidApp/core/system/.../notification/RingingNotificationFactory.kt`](../../../androidApp/core/system/src/main/kotlin/com/niumi/system/notification/RingingNotificationFactory.kt))
est le seul point de construction de la notification portant `setFullScreenIntent()`.
