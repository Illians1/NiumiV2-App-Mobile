# Justification Play Console — service au premier plan `mediaPlayback`

Document préparatoire à la déclaration du type de service au premier plan `mediaPlayback`
(exigence Play depuis Android 14 : chaque type FGS déclaré doit être justifié individuellement).
Statut : préparatoire, pas encore soumis.

## Ce que fait `AlarmRingingService`

Il boucle une sonnerie locale (`MediaPlayer`, attributs `USAGE_ALARM` /
`CONTENT_TYPE_SONIFICATION`) et une vibration répétée depuis le déclenchement du réveil jusqu'à
la validation d'un scan NFC associé. `foregroundServiceType="mediaPlayback"`, `startForeground()`
appelé immédiatement dans `onStartCommand()`, `START_STICKY`.

## Pourquoi `mediaPlayback` et pas un autre type

Android ne propose pas de type de service au premier plan dédié aux applications d'alarme (à la
différence de `phoneCall` pour la téléphonie, par exemple). `mediaPlayback` est le type dont la
définition officielle correspond le mieux à l'activité réelle du service : une lecture audio
continue, au premier plan, dont l'utilisateur a explicitement besoin d'être informé et qu'il n'a
pas initiée manuellement au moment considéré (elle a été programmée à l'avance).

**Point de vigilance signalé par avance, avant tout refus éventuel :** ce choix peut être
contesté en revue Play, un examinateur pouvant estimer que le contenu n'est pas de la « lecture
multimédia » au sens habituel (musique, podcast, vidéo). C'est un point d'incertitude assumé de
la spec (SPEC_ANDROID §10.2, §14) plutôt qu'une certitude — à traiter comme un risque connu de la
porte de validation, pas une découverte de dernière minute si Google le relève.

## Comment il est démarré

Uniquement depuis `AlarmReceiver`, en réponse à un `AlarmManager.setAlarmClock()` programmé par
l'utilisateur lui-même (activation d'une session, geste explicite). Le déclenchement d'une
alarme exacte demandée par l'utilisateur autorise le démarrage d'un service au premier plan
depuis l'arrière-plan (règle Android documentée, SPEC_ANDROID §10.1). Le service ne démarre
jamais spontanément, sans action utilisateur préalable.

## Durée et arrêt

Borné à la durée d'une session : de `ALARM_FIRED` jusqu'au scan NFC valide (`STOP_RINGING`) ou à
un `ALARM_SOUND_STOPPED` défensif. Aucune action `STOP` n'est exposée dans l'intent, la
notification ou un binding (SPEC_ANDROID §10.2, §3 — absence de bouton d'arrêt volontaire dans
tout le parcours de sonnerie). `onDestroy()` arrête audio, vibration et wake lock comme filet de
sécurité.

## Preuve dans le code

`AlarmRingingService`
([`androidApp/feature/ringing/.../AlarmRingingService.kt`](../../../androidApp/feature/ringing/src/main/kotlin/com/niumi/feature/ringing/AlarmRingingService.kt)),
seul service au premier plan déclaré dans le manifeste `:feature:ringing`.
