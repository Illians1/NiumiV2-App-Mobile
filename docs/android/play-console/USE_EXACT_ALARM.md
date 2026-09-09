# Justification Play Console — `USE_EXACT_ALARM`

Document préparatoire à la déclaration de la permission sensible `USE_EXACT_ALARM`
(exigence Play : justifier pourquoi l'application ne peut pas se contenter d'une alarme
inexacte ou de `SCHEDULE_EXACT_ALARM`). Statut : préparatoire, pas encore soumis.

## Pourquoi cette permission

Niumi est une application de réveil : le réveil sonore à l'heure exacte choisie par
l'utilisateur est **la fonction centrale du produit** (SPEC_ANDROID §13 : « Niumi déclare
`USE_EXACT_ALARM`, car le réveil est une fonction centrale du produit »), pas une fonctionnalité
secondaire qui s'accommoderait d'un délai.

`USE_EXACT_ALARM` est la voie prévue par Android pour les applications d'alarme et de minuterie
(catégorie « Alarm & clock apps » du Play Console) : l'accès aux alarmes exactes est accordé au
moment de l'installation, sans démarche utilisateur supplémentaire, contrairement à
`SCHEDULE_EXACT_ALARM`.

## Ce que le MVP ne fait pas

- **Pas de `SCHEDULE_EXACT_ALARM`.** Cette permission traite l'accès aux alarmes exactes comme
  une permission utilisateur ordinaire, révocable dans les réglages « Alarmes et rappels ». La
  spec produit l'interdit explicitement (SPEC_ANDROID §14) pour ne pas présenter le réveil comme
  une fonctionnalité optionnelle.
- **Pas de fallback sur une alarme inexacte.** `setInexactRepeating()`, WorkManager ou un
  déclenchement approximatif ne conviennent pas à un réveil : un décalage de plusieurs minutes
  contredirait la promesse produit. SPEC_ANDROID §18 : « Ne jamais remplacer silencieusement une
  alarme exacte par une alarme inexacte. »

## API utilisée

Une seule API programme l'heure de réveil dans tout le MVP :

```kotlin
AlarmManager.setAlarmClock(
    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
    alarmPendingIntent
)
```

`setAlarmClock()` place l'alarme dans la catégorie que le système Android traite avec la plus
haute priorité de réveil (visible dans la barre de statut, exemptée des restrictions Doze les
plus strictes) — c'est le comportement attendu d'un réveil, pas un contournement des économies
d'énergie du système.

## Comportement si l'accès est indisponible

`canScheduleExactAlarms()` est vérifié par sécurité avant chaque activation (contrôle
`BLOCKING_FOR_ALARM` du diagnostic, SPEC_ANDROID §13). S'il renvoie `false` :

- l'activation de la session est **refusée** ;
- l'application signale un état anormal, une incompatibilité de l'appareil ou un problème
  d'éligibilité — jamais une fausse confirmation d'activation ;
- l'application **ne redirige pas automatiquement** vers des réglages d'octroi de permission,
  puisque `USE_EXACT_ALARM` n'est pas une permission que l'utilisateur accorde manuellement.

## Preuve dans le code

`AndroidAlarmScheduler`
([`androidApp/core/system/.../alarm/AndroidAlarmScheduler.kt`](../../../androidApp/core/system/src/main/kotlin/com/niumi/system/alarm/AndroidAlarmScheduler.kt))
est le seul point d'appel à une API de programmation d'alarme dans le dépôt ; `ReleaseHygieneTest`
(prévu à l'étape 21) vérifiera que le manifeste fusionné en `release` ne contient jamais
`SCHEDULE_EXACT_ALARM`.
