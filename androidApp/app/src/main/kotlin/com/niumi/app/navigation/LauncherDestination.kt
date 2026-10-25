package com.niumi.app.navigation

import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionStateDto

/**
 * Les quatre états qui attendent un scan. Tant que l'un d'eux dure, **aucun autre écran de Niumi
 * n'est atteignable** : `MainActivity` ouvre l'écran de réveil à chaque `onResume`, quelle que soit
 * la destination du `NavHost`.
 *
 * La règle a d'abord été posée sur l'accueil seul ; c'était faux, et mesuré sur appareil à
 * l'étape 17 : après l'armement, le `NavHost` était sur l'ancien écran de session active, donc
 * l'accueil n'était plus composé et sa redirection ne s'exécutait jamais. La règle est globale, sa
 * mise en œuvre doit l'être aussi.
 *
 * [releaseTarget] départage `RELEASING` (étape 43, contrat 1.5) : vers `COMPLETED` ou `CANCELLED`, la
 * libération suit un scan ou une sortie de secours et l'écran de réveil reste devant jusqu'à la fin ;
 * vers `DISARMED`, c'est un désarmement avant le coucher, sans rien à scanner.
 */
fun requiresAlarmScreen(
    state: SessionStateDto?,
    releaseTarget: ReleaseTargetDto?,
): Boolean =
    when (state) {
        SessionStateDto.RINGING,
        SessionStateDto.AWAITING_NFC,
        SessionStateDto.TRIGGERED_AWAITING_NFC,
        -> true

        SessionStateDto.RELEASING -> releaseTarget != ReleaseTargetDto.DISARMED

        // Avant la sonnerie comme après la session, l'accueil ou la mise en route.
        SessionStateDto.PREPARING,
        SessionStateDto.ARMED,
        SessionStateDto.COMPLETED,
        SessionStateDto.CANCELLED,
        SessionStateDto.DISARMED,
        SessionStateDto.FAILED,
        null,
        -> false
    }
