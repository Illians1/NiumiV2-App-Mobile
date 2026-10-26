package com.niumi.feature.ringing

import android.content.Intent

/**
 * Retour de l'écran de réveil vers `MainActivity` (SPEC_ANDROID §10.4). `CLEAR_TOP` et `SINGLE_TOP`
 * depuis l'étape 46 : l'exemplaire existant revient devant et reçoit la destination par
 * `onNewIntent`, au lieu d'un nouvel exemplaire empilé à chaque sortie (quatre constatés sur
 * appareil le 2026-10-26), dont l'état — la bulle « Réveil annulé… », l'écran ouvert — restait caché
 * dessous. `NEW_TASK` reste exigé pour démarrer une activité hors de la sienne.
 */
internal object AlarmExitIntent {
    const val FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}
