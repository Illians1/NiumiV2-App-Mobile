package com.niumi.system.session

import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto

/**
 * États finaux de la machine commune. `FINAL_STATES` existe déjà dans `:shared:core`
 * (`ReducerSupport.kt`) mais est `internal` à `commonMain`, inaccessible depuis Android — redéfini
 * ici pour les mêmes quatre valeurs (SPEC_CORE_KMP §5.1 ; `DISARMED` depuis le contrat 1.5).
 *
 * Public depuis l'étape 13 : trois modules avaient besoin de la même règle (le coordinateur ici,
 * la destination de l'accueil dans `:app`, la garde du parcours de préparation dans
 * `:feature:setup`). Une seule définition, prouvée exhaustive sur `SessionStateDto.entries` par
 * `SessionScanStatesTest`, vaut mieux que trois copies à maintenir ensemble.
 */
val SESSION_FINAL_STATES: Set<SessionStateDto> =
    setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.DISARMED, SessionStateDto.FAILED)

/**
 * Les deux états où le scan du boîtier est la seule issue et où la notification de §10.5 doit être
 * visible. Introduit à l'étape 19 : `SessionReconciler` les traitait déjà ensemble, et le
 * déclencheur de premier plan a besoin de la même règle.
 */
val SESSION_SCAN_STATES: Set<SessionStateDto> =
    setOf(SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC)

/**
 * Une session dans un état non final tient encore l'appareil : alarme programmée ou blocage
 * appliqué. `null` (aucune session) n'est pas un état en cours.
 */
fun SessionStateDto?.isSessionInProgress(): Boolean = this != null && this !in SESSION_FINAL_STATES

/**
 * Le blocage des applications est-il en vigueur, du coucher jusqu'à la fin de la session ?
 *
 * Le coucher est l'instant où le blocage est appliqué : une session `ARMED` dont le blocage
 * différé attend encore son heure ne bloque rien. `RELEASING` vers `COMPLETED` ou `CANCELLED` est
 * tenu pour en vigueur : le blocage n'est levé qu'à la fin de la libération (VALEURS §1, thème
 * sombre jusqu'à l'état final), y compris pour une annulation avant le coucher, où il n'avait pas
 * encore commencé. `RELEASING` vers
 * `DISARMED` ne l'est pas : le désarmement n'a lieu qu'avant le coucher, blocage en attente
 * (SPEC_CORE_KMP §5.1, contrat 1.5 ; étape 43). Aucune session, ou une persistance illisible, se
 * lit « non ».
 *
 * Deux consommateurs, une seule règle : le thème de l'app ([isSessionDark]) et le service de garde
 * du blocage (SPEC_ANDROID §12.5, correctif A8 du 2026-10-19).
 */
fun SessionSnapshotDto?.isBlockingInForce(): Boolean =
    when (this?.state) {
        null -> false

        SessionStateDto.ARMED -> blockingAppliedAtEpochMillis != null

        SessionStateDto.RELEASING -> releaseTarget != ReleaseTargetDto.DISARMED

        SessionStateDto.RINGING,
        SessionStateDto.AWAITING_NFC,
        SessionStateDto.TRIGGERED_AWAITING_NFC,
        -> true

        SessionStateDto.PREPARING,
        SessionStateDto.COMPLETED,
        SessionStateDto.CANCELLED,
        SessionStateDto.DISARMED,
        SessionStateDto.FAILED,
        -> false
    }

/**
 * Thème de l'app d'après la session persistée (`design-plan/VALEURS-DE-DESIGN.md` §1, étape 33) :
 * sombre du coucher jusqu'au scan, clair sinon. Jamais d'après le réglage clair ou sombre du
 * téléphone. Sombre exactement quand le blocage est en vigueur ([isBlockingInForce]) : une session
 * qui attend son coucher est claire (accueil « Réveil prévu »), et le passage au clair se fait à la
 * fin de la session (écran de réussite, accueil après une annulation).
 */
fun SessionSnapshotDto?.isSessionDark(): Boolean = isBlockingInForce()
