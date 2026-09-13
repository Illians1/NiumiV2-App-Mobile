package com.niumi.system.readiness

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.notification.SessionWarningNotifier
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.isSessionInProgress

/**
 * Un contrôle bloquant devenu faux pendant `ARMED`, avec l'incident qu'il a produit.
 *
 * [dispatchResult] est `null` quand un incident de ce code était **déjà enregistré pour cette
 * session** : l'avertissement a bien été republié, mais aucun second incident n'a été écrit. Voir
 * [SessionReadinessMonitor].
 *
 * [snapshotAfter] est le snapshot tel que le moteur l'a laissé après cet incident. Il sert à
 * bâtir l'incident **suivant** de la même passe sur une révision à jour : sans lui, deux contrôles
 * tombés ensemble ne produisaient qu'un seul incident, le second étant rejeté en `STALE_REVISION`
 * (mesuré sur appareil à l'étape 17). `null` quand aucun incident n'a été dispatché.
 */
data class ReadinessDegradation(
    val checkId: ReadinessCheckId,
    val incidentCode: String,
    val dispatchResult: DispatchResult?,
    val snapshotAfter: SessionSnapshotDto? = null,
)

/**
 * [failing] est l'état courant — tous les contrôles surveillés en échec, qu'ils aient déjà été
 * signalés ou non. [newlyReported] ne contient que ceux qui viennent de basculer. Les appelants
 * qui décident d'interrompre leur traitement (le réconciliateur, quand une permission est
 * perdue) doivent lire [failing] : un contrôle cassé depuis la passe précédente n'apparaît plus
 * dans [newlyReported] mais reste cassé.
 */
data class ReadinessMonitorResult(
    val failing: Set<ReadinessCheckId>,
    val newlyReported: List<ReadinessDegradation>,
)

/**
 * Surveillance de SPEC_ANDROID §13.1. Rejoue le diagnostic pendant une session `ARMED` et
 * avertit quand un contrôle surveillé devient faux.
 *
 * **La notification et l'incident n'ont pas le même cycle de vie**, et c'est délibéré depuis
 * l'étape 16 :
 *
 * - La **notification** suit l'état courant et sa garde vit en mémoire, donc disparaît avec le
 *   processus. Un redémarrage republie un avertissement encore valable, ce qui vaut mieux que de
 *   le taire (§13.1, « l'avertissement est émis au plus tôt, jamais garanti immédiat »).
 * - L'**incident** est un fait métier, pas un état d'affichage : il n'est enregistré qu'une fois
 *   par code et par session. Le réécrire à chaque redémarrage consignerait un basculement qui n'a
 *   pas eu lieu — mesuré sur appareil à l'étape 16, où l'écran 7 présentait deux fois le même
 *   incident avec deux boutons identiques.
 *
 * Conséquence assumée : un contrôle réparé puis re-cassé dans la même session republie son
 * avertissement sans produire de second incident. La santé est déjà `DEGRADED` et n'en revient
 * jamais (SPEC_CORE_KMP §7.3), et le journal technique — lui, non dédupliqué — garde la trace
 * horodatée de chaque détection.
 *
 * Avant déverrouillage, [incidentsReader] renvoie une liste vide sans pouvoir dire si des
 * incidents existent (§7.3) : la déduplication est alors impossible et l'incident est enregistré.
 * On ne perd jamais une dégradation pour cause de stockage indisponible.
 *
 * Le service d'accessibilité n'est jamais sollicité comme sentinelle (§13.1, dernier alinéa) :
 * la surveillance n'a d'autre déclencheur que ses appelants.
 */
class SessionReadinessMonitor(
    private val readinessChecker: DeviceReadinessChecker,
    private val warningNotifier: SessionWarningNotifier,
    private val eventFactory: SessionEventFactory,
    private val technicalEventLog: TechnicalEventLog,
    private val incidentsReader: SessionIncidentsReader,
) {
    private val alreadyReported = mutableSetOf<ReadinessCheckId>()

    suspend fun evaluate(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReadinessMonitorResult {
        val monitored = monitoredChecksFor(snapshot.state)
        if (monitored.isEmpty()) {
            // Session terminée : un avertissement encore affiché deviendrait mensonger.
            if (alreadyReported.isNotEmpty()) {
                alreadyReported.clear()
                warningNotifier.clearAll()
            }
            return ReadinessMonitorResult(failing = emptySet(), newlyReported = emptyList())
        }
        clearWarningsOutsideScope(monitored)

        val report = readinessChecker.check(ReadinessInput(snapshot.wakeSchedule.triggerAtEpochMillis))
        val failing = mutableSetOf<ReadinessCheckId>()
        val newlyReported = mutableListOf<ReadinessDegradation>()

        // Le snapshot avance d'un incident à l'autre : chaque `INCIDENT_REPORTED` accepté
        // incrémente la révision, et réutiliser celui du début de passe ferait rejeter le second
        // en `STALE_REVISION`. Mesuré sur appareil à l'étape 17 — voir [reportOn].
        var current = snapshot
        monitored.forEach { (checkId, incidentCode) ->
            if (report.check(checkId).outcome == ReadinessOutcome.FAILED) {
                failing += checkId
                if (alreadyReported.add(checkId)) {
                    val degradation = reportOn(current, checkId, incidentCode, dispatch)
                    newlyReported += degradation
                    current = degradation.snapshotAfter ?: current
                }
            } else if (alreadyReported.remove(checkId)) {
                warningNotifier.clear(checkId)
            }
        }

        return ReadinessMonitorResult(failing, newlyReported)
    }

    /**
     * Les cinq contrôles de réveil n'ont de sens qu'en `ARMED` : une fois la sonnerie commencée,
     * avertir d'un volume d'alarme ou d'un plein écran perdu ne décrit plus rien d'actionnable.
     * Le service d'accessibilité, lui, est surveillé dans **tous** les états non finaux : le
     * blocage court jusqu'au scan (SPEC_ANDROID §3), et §12.2 exige que sa désactivation pendant
     * une session soit détectée « à la prochaine exécution » — pas seulement avant le réveil.
     *
     * Étape 15 : remplace la sortie anticipée sur `state != ARMED`, qui rendait un service coupé
     * pendant `RINGING` ou `RELEASING` totalement invisible.
     */
    private fun monitoredChecksFor(state: SessionStateDto): Map<ReadinessCheckId, String> =
        when {
            !state.isSessionInProgress() -> emptyMap()
            state == SessionStateDto.ARMED -> MonitoredReadinessChecks.incidentCodes
            else -> MonitoredReadinessChecks.blockingOnlyIncidentCodes
        }

    /**
     * Un contrôle qui sort du périmètre surveillé (l'alarme exacte quand la session passe de
     * `ARMED` à `RINGING`) doit voir sa notification retirée : elle resterait affichée sans
     * qu'aucune passe ne puisse plus la réévaluer.
     */
    private fun clearWarningsOutsideScope(monitored: Map<ReadinessCheckId, String>) {
        val outOfScope = alreadyReported - monitored.keys
        outOfScope.forEach { checkId ->
            alreadyReported.remove(checkId)
            warningNotifier.clear(checkId)
        }
    }

    /**
     * **Le snapshot reçu doit être le plus récent**, pas celui du début de passe. Deux contrôles
     * tombés ensemble produisent deux `INCIDENT_REPORTED` successifs, et le premier incrémente la
     * révision : bâtir le second sur le snapshot d'origine le fait rejeter en `STALE_REVISION`,
     * **silencieusement**. Mesuré sur appareil à l'étape 17 — le silence total fait aussi tomber le
     * volume d'alarme à zéro, donc deux contrôles échouent d'un coup, et seul le premier incident
     * était enregistré alors que §13.1 en promet un par contrôle.
     */
    private suspend fun reportOn(
        snapshot: SessionSnapshotDto,
        checkId: ReadinessCheckId,
        incidentCode: String,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReadinessDegradation {
        technicalEventLog.log(TechnicalEventType.SESSION_READINESS_DEGRADED, snapshot.sessionId)
        SPECIFIC_TECHNICAL_EVENTS[checkId]?.let { technicalEventLog.log(it, snapshot.sessionId) }
        warningNotifier.present(checkId)
        val dispatchResult = recordIncidentOnce(snapshot, incidentCode, dispatch)
        return ReadinessDegradation(
            checkId = checkId,
            incidentCode = incidentCode,
            dispatchResult = dispatchResult,
            snapshotAfter = (dispatchResult as? DispatchResult.Applied)?.snapshot,
        )
    }

    /**
     * `null` si un incident de ce code existe déjà pour la session : l'avertissement vient d'être
     * republié, mais le fait métier était déjà consigné.
     */
    private suspend fun recordIncidentOnce(
        snapshot: SessionSnapshotDto,
        incidentCode: String,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): DispatchResult? {
        val alreadyRecorded =
            incidentsReader.incidents(snapshot.sessionId).any { it.code == incidentCode }
        if (alreadyRecorded) return null
        val incident = eventFactory.buildIncident(incidentCode, IncidentSeverityDto.CRITICAL)
        return dispatch(eventFactory.incidentReported(snapshot, incident))
    }

    private companion object {
        /**
         * Événements techniques dédiés de SPEC_ANDROID §17, en plus de
         * `SESSION_READINESS_DEGRADED` qui est journalisé pour toute dégradation (§13.1). Les
         * contrôles sans événement propre n'en produisent qu'un.
         */
        val SPECIFIC_TECHNICAL_EVENTS =
            mapOf(
                ReadinessCheckId.EXACT_ALARM to TechnicalEventType.EXACT_ALARM_LOST,
                ReadinessCheckId.ACCESSIBILITY_SERVICE to TechnicalEventType.ACCESSIBILITY_DISABLED,
                ReadinessCheckId.DND_TOTAL_SILENCE to TechnicalEventType.ALARM_MUTED_BY_DND,
                ReadinessCheckId.FULL_SCREEN_INTENT to TechnicalEventType.FULL_SCREEN_DENIED,
            )
    }
}
