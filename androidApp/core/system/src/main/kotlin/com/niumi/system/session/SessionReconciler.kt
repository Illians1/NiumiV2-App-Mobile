package com.niumi.system.session

import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.TriggerDelayInputDto
import com.niumi.core.interop.TriggerDelayOutcomeDto
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.readiness.ReadinessCheckId

/**
 * Reprend un état incomplet et compare l'état métier aux sous-systèmes Android (SPEC_CORE_KMP §6.1
 * dernier alinéa, §13 ; SPEC_ANDROID §9.2, §9.3, §13.1, §18). Reçoit `dispatch`, la fonction non
 * verrouillante de [DefaultSessionCoordinator], en paramètre plutôt qu'en dépendance construite :
 * évite un cycle de DI (le coordinateur détient le réconciliateur).
 *
 * Ordre d'une passe : rejeu de l'outbox d'abord, nouvelle décision ensuite (une seule, l'état
 * courant ne peut appartenir qu'à un cas du tableau). La politique de retard est toujours lue via
 * [NiumiCoreFacade.evaluateTriggerDelay], jamais recalculée côté Android (SPEC_ANDROID §9.3).
 */
class SessionReconciler(
    private val gateway: SessionPersistenceGateway,
    private val effectDispatcher: EffectDispatcher,
    private val sources: ReconcilerSources,
    private val facade: NiumiCoreFacade,
    private val eventFactory: SessionEventFactory,
    private val technicalEventLog: TechnicalEventLog,
) {
    // Clauses de garde séquentielles (snapshot illisible, absent) avant le corps principal — même
    // motif que `NfcReducer.onValidScan` (:shared:core), voir `ETAPE-07.md`.
    @Suppress("ReturnCount")
    suspend fun reconcile(
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReconcileResult {
        // SPEC_ANDROID §9.3, dernier alinéa : « À `USER_UNLOCKED`, le réconciliateur fusionne de
        // façon idempotente le registre et l'outbox Direct Boot dans Room. » Avant `gateway.load()`,
        // sans quoi la passe déciderait sur un Room amputé de ce qui a été fait avant le
        // déverrouillage. Sous le mutex du coordinateur, qui le tient pendant tout cet appel.
        if (reason in MERGING_REASONS) sources.directBootMerger.merge()

        val loaded = gateway.load()
        if (loaded is LoadResult.Unreadable) {
            return ReconcileResult(sessionId = null, actions = listOf(ReconcileAction.SnapshotCorrupted))
        }
        val present = loaded as? LoadResult.Present ?: return ReconcileResult(sessionId = null, actions = emptyList())

        val sessionId = present.snapshot.sessionId
        val actions = mutableListOf<ReconcileAction>()

        // Réamorce le flux observé par l'interface avant toute décision. `SessionSnapshotPublisher`
        // vit en mémoire et repart à `null` à chaque démarrage du processus ; une session saine ne
        // produit aucune décision, donc aucun `PUBLISH_PLATFORM_SNAPSHOT` ne la republierait, et
        // l'accueil afficherait « Aucune session » alors que l'alarme est programmée (défaut mesuré
        // sur appareil à l'étape 14). Republier une valeur identique est sans effet : `StateFlow`
        // n'émet que sur changement.
        sources.snapshotPublisher.publish(present.snapshot)

        val replayed = replayOutbox(present, dispatch, actions)
        val snapshot = reportClockChange(replayed, reason, dispatch, actions)

        when (snapshot.state) {
            SessionStateDto.PREPARING -> {
                reconcilePreparing(snapshot, dispatch, actions)
            }

            SessionStateDto.ARMED -> {
                reconcileArmed(snapshot, reason, dispatch, actions)
            }

            SessionStateDto.RINGING -> {
                resumeRinging(snapshot, actions)
            }

            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            -> {
                republishScanRequest(snapshot, actions)
            }

            SessionStateDto.RELEASING -> {
                resumeRelease(snapshot, dispatch, actions)
            }

            SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED -> {
                gateway.clearActive(sessionId)
                actions += ReconcileAction.PointerCleared
            }
        }

        return ReconcileResult(sessionId, actions)
    }

    /**
     * Rejeu des effets `PENDING`/`FAILED` (SPEC_CORE_KMP §6.1), puis dispatch d'un
     * `INCIDENT_REPORTED` par incident collecté. Renvoie le snapshot à jour : chaque incident
     * incrémente la révision, et la suite de la passe doit travailler sur la dernière connue.
     *
     * **Le snapshot est enchaîné d'un incident au suivant.** La boucle partait auparavant du
     * snapshot d'entrée pour chacun : deux incidents dans la même passe faisaient tomber le second
     * en `STALE_REVISION`, `SessionEventValidation` exigeant l'égalité stricte de
     * `expectedRevision`. Même classe de défaut que celui mesuré sur appareil à l'étape 17 (essai
     * 7), corrigé alors dans `SessionReadinessMonitor` mais pas ici. Même patron que
     * [DefaultSessionCoordinator.dispatchCollectedIncidents].
     */
    private suspend fun replayOutbox(
        present: LoadResult.Present,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ): SessionSnapshotDto {
        val replayable = gateway.pendingEffects(present.snapshot.sessionId)
        if (replayable.isEmpty()) return present.snapshot

        val execution = effectDispatcher.execute(replayable, present.snapshot, present.extras)
        actions += ReconcileAction.OutboxReplayed(replayable.size)

        var current = present.snapshot
        for (incident in execution.incidents) {
            if (current.state in SESSION_FINAL_STATES) break
            val result = dispatch(eventFactory.incidentReported(current, incident))
            actions += ReconcileAction.DecisionApplied(result)
            if (result is DispatchResult.Applied && result.snapshot != null) current = result.snapshot
        }
        return current
    }

    /**
     * SPEC_ANDROID §9.3 : un changement manuel d'heure ou de fuseau consigne un incident `WARNING`.
     * `WARNING` ne dégrade pas la santé (SPEC_CORE_KMP §7.3) — c'est une trace, pas une alerte : le
     * réveil, lui, garde exactement le même instant, réenregistré par [reconcileTriggerDelay].
     *
     * **Un incident par code et par session.** `android.intent.action.TIME_SET` n'est pas émis
     * seulement quand l'utilisateur change l'heure : chaque correction d'horloge par le réseau le
     * produit aussi, plusieurs fois par nuit sur certains appareils. Sans cette garde, une seule
     * session accumulerait des dizaines d'incidents identiques sur l'écran 7 — exactement le défaut
     * mesuré et corrigé à l'étape 16. Même convention et même sonde que
     * [com.niumi.system.readiness.SessionReadinessMonitor], à ceci près que la garde est ici lue en
     * base et non gardée en mémoire : ces deux raisons n'arrivent que par broadcast, donc parfois
     * dans un processus qui vient de naître.
     *
     * Avant déverrouillage, [ReconcilerSources.incidentsReader] renvoie une liste vide sans pouvoir dire si des
     * incidents existent (§7.3) : l'incident est alors enregistré. On ne perd jamais une trace pour
     * cause de stockage indisponible.
     *
     * Renvoie le snapshot à jour : un incident accepté incrémente la révision, et la suite de la
     * passe doit travailler sur la dernière connue — même enchaînement que [replayOutbox].
     */
    private suspend fun reportClockChange(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ): SessionSnapshotDto {
        // Les trois gardes en une seule expression court-circuitée : la lecture en base n'a lieu
        // que si la raison décrit bien un déplacement d'horloge sur une session encore vivante.
        val code = CLOCK_CHANGE_INCIDENT_CODES[reason]
        if (code == null ||
            snapshot.state in SESSION_FINAL_STATES ||
            sources.incidentsReader.incidents(snapshot.sessionId).any { it.code == code }
        ) {
            return snapshot
        }

        val incident = eventFactory.buildIncident(code, IncidentSeverityDto.WARNING)
        val result = dispatch(eventFactory.incidentReported(snapshot, incident))
        actions += ReconcileAction.IncidentDispatched(code, IncidentSeverityDto.WARNING)
        actions += ReconcileAction.DecisionApplied(result)
        return (result as? DispatchResult.Applied)?.snapshot ?: snapshot
    }

    /**
     * §11.3, dernier alinéa : « `SessionReconciler` compare l'état natif au snapshot et reprend
     * uniquement les effets manquants, sans réappliquer un blocage déjà retiré ». Le rejeu a déjà
     * eu lieu ([replayOutbox]) ; il ne reste qu'à décider si la phase peut se refermer.
     *
     * Seuls les trois effets **requis** de la libération retiennent `RELEASING` (SPEC_CORE_KMP §6,
     * dernier alinéa) : une notification non retirée ou un service non arrêté sont best-effort et
     * ne doivent jamais laisser une session éternellement en nettoyage.
     *
     * Une outbox vide referme la phase : c'est le cas du processus mort entre la persistance de la
     * décision et l'envoi de `RELEASE_SUCCEEDED`, où tous les effets ont réussi sans que personne
     * ne l'ait conclu.
     *
     * Aucun `RELEASE_FAILED` n'est redispatché quand des effets manquent encore, et aucun
     * `RELEASE_PARTIAL_FAILURE` n'est rejournalisé : le coordinateur l'a fait au moment de l'échec
     * (`DefaultSessionCoordinator.completePhase`), et le répéter à chaque passe recréerait les
     * doublons d'incidents corrigés à l'étape 16.
     */
    private suspend fun resumeRelease(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val requiredKinds = PhaseCompletion.requiredKindsFor(SessionEventKindDto.VALID_NFC_SCANNED)
        val missing = gateway.pendingEffects(snapshot.sessionId).filter { it.kind in requiredKinds }
        if (missing.isEmpty()) {
            actions += ReconcileAction.DecisionApplied(dispatch(eventFactory.releaseSucceeded(snapshot)))
        } else {
            actions += ReconcileAction.ReleaseStillPending(missing.size)
        }
    }

    /**
     * §10.5 : la notification d'attente de scan est le seul rappel visible une fois l'écran de
     * réveil fermé, et le scan reste la seule sortie de session (§11.2). Si elle a disparu avec le
     * processus, plus rien n'indique à l'utilisateur ce qu'il doit faire.
     *
     * Republiée **sans condition** plutôt qu'après un test de présence : `present()` est idempotent
     * par identifiant de notification — republier remplace en place — et interroger
     * `activeNotifications` ajouterait une méthode à l'interface pour un résultat identique.
     * `AndroidScanRequestNotifier` pose `setOnlyAlertOnce(true)` pour qu'aucune ré-alerte ne soit
     * visible sur ce canal d'importance haute.
     */
    private fun republishScanRequest(
        snapshot: SessionSnapshotDto,
        actions: MutableList<ReconcileAction>,
    ) {
        if (sources.scanRequestNotifier.present(snapshot.sessionId) !is OperationResult.Failure) {
            actions += ReconcileAction.ScanRequestRepublished
        }
    }

    /**
     * §10.2 : « reconstruire son état depuis le snapshot si le processus est recréé ». Le service
     * s'en charge lui-même quand `START_STICKY` le relance — **mais la plateforme ne le fait pas
     * toujours** : mesuré sur appareil à l'étape 17, HyperOS n'a rejoué aucun redémarrage après un
     * crash du processus, et la sonnerie s'est arrêtée définitivement alors que la session restait
     * active et le blocage en place.
     *
     * Le rejeu de l'outbox ne rattrape pas ce cas : `START_RINGING` y est déjà `SUCCEEDED`. Sans
     * cette reprise, aucun chemin ne ranime le son — pas même ouvrir l'application.
     *
     * L'appel est idempotent : c'est exactement le chemin emprunté à chaque `onStartCommand`
     * valide du service, et `AlarmAudioEngine.start` ne double jamais le son. Le relancer alors
     * qu'il tourne déjà est donc sans effet.
     *
     * **Ne garantit pas le réveil pour autant** : encore faut-il que le processus revienne à la
     * vie. Une alarme de secours pendant `RINGING` relève de l'étape 20 (mort du processus).
     */
    private fun resumeRinging(
        snapshot: SessionSnapshotDto,
        actions: MutableList<ReconcileAction>,
    ) {
        sources.ringingController.startRinging(snapshot.sessionId, snapshot.revision)
        actions += ReconcileAction.RingingResumed
    }

    private suspend fun reconcilePreparing(
        snapshot: SessionSnapshotDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val blockingActive =
            (sources.blockedPackagesProjection.current() as? BlockedPackagesState.Active)?.sessionId ==
                snapshot.sessionId
        val followUp =
            if (sources.alarmScheduler.isScheduled(snapshot.sessionId) && blockingActive) {
                eventFactory.activationSucceeded(snapshot)
            } else {
                eventFactory.activationFailed(snapshot, "ANDROID_ACTIVATION_INTERRUPTED")
            }
        actions += ReconcileAction.DecisionApplied(dispatch(followUp))
    }

    /**
     * La surveillance de §13.1 remplace les deux contrôles ad hoc de l'étape 11 : les six
     * contrôles bloquants sont désormais évalués d'un seul tenant, chacun avec son incident et
     * sa notification. Le comportement d'origine est conservé sur un point clé — une permission
     * perdue interrompt la passe avant toute reprogrammation d'alarme. La condition porte sur
     * `failing`, l'état courant, et non sur `newlyReported` : un contrôle cassé depuis la passe
     * précédente n'est plus signalé mais reste cassé.
     *
     * **`BEFORE_SCAN` échappe à cette garde depuis l'étape 18**, sur un défaut mesuré sur appareil.
     * La garde protège une reprogrammation d'alarme ; `BEFORE_SCAN` n'arme rien, il convertit un
     * `ARMED` dont l'heure est atteinte en `TRIGGERED_AWAITING_NFC` pour que le scan puisse aboutir
     * (SPEC_ANDROID §11.3 : « si la session est encore `ARMED` après l'heure sans alarme observée,
     * il envoie d'abord `TRIGGER_ELAPSED` »). L'interrompre ici laissait la session `ARMED` après
     * l'heure, état où `NfcReducer` refuse le scan (`TRIGGER_ALREADY_ELAPSED`) : l'utilisateur
     * **ne pouvait plus terminer sa session**, le seul chemin de sortie du produit (§11.2) devenant
     * inopérant sans le moindre message. Mesuré le 2026-09-14 sur Xiaomi 25080RABDG, service
     * d'accessibilité coupé et heure dépassée de 18 minutes ; voir `ETAPE-18.md`.
     */
    private suspend fun reconcileArmed(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val monitored = sources.readinessMonitor.evaluate(snapshot, dispatch)
        for (degradation in monitored.newlyReported) {
            // `dispatchResult` nul : l'avertissement a été republié, mais un incident de ce code
            // était déjà consigné pour la session (étape 16). La passe n'a donc dispatché aucune
            // décision, et l'annoncer ici ferait mentir le compte rendu de réconciliation.
            val dispatchResult = degradation.dispatchResult ?: continue
            actions += ReconcileAction.IncidentDispatched(degradation.incidentCode, IncidentSeverityDto.CRITICAL)
            actions += ReconcileAction.DecisionApplied(dispatchResult)
        }
        if (reason != ReconcileReason.BEFORE_SCAN && monitored.failing.any { it in PERMISSION_CHECKS }) return

        // Le moniteur vient peut-être de dispatcher un `INCIDENT_REPORTED`, qui incrémente la
        // révision : poursuivre sur le snapshot d'entrée produirait `STALE_REVISION` et le
        // `TRIGGER_ELAPSED` serait perdu. Même relecture qu'`AlarmTriggerHandler` après la même
        // surveillance (étape 17). Un état devenu non-`ARMED` entre-temps ne relève plus d'ici.
        val refreshed = (gateway.load() as? LoadResult.Present)?.snapshot
        if (refreshed != null && refreshed.state == SessionStateDto.ARMED) {
            reconcileTriggerDelay(refreshed, reason, dispatch, actions)
        }
    }

    private suspend fun reconcileTriggerDelay(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        val triggerAt = snapshot.wakeSchedule.triggerAtEpochMillis
        val now = eventFactory.nowEpochMillis()
        when (facade.evaluateTriggerDelay(TriggerDelayInputDto(triggerAt, now)).outcome) {
            TriggerDelayOutcomeDto.NOT_REACHED -> {
                // §9.3 : « Il lit le snapshot et rappelle le programmateur avec le même
                // `triggerAtEpochMillis`. Il ne recalcule pas l'instant depuis l'heure locale. »
                // Sur un changement d'heure ou de fuseau, le rappel est inconditionnel : le test
                // `isScheduled` ne prouve que l'existence d'un `PendingIntent`, jamais que le
                // système l'a conservé au bon instant après avoir bougé son horloge. Réenregistrer
                // est idempotent (`FLAG_UPDATE_CURRENT`) et coûte un appel.
                val clockMoved = reason in CLOCK_CHANGE_INCIDENT_CODES
                if (clockMoved || !sources.alarmScheduler.isScheduled(snapshot.sessionId)) {
                    rescheduleAlarm(snapshot, triggerAt, actions)
                }
            }

            TriggerDelayOutcomeDto.FIRE_NOW -> {
                if (reason == ReconcileReason.BEFORE_SCAN) {
                    actions +=
                        ReconcileAction.DecisionApplied(
                            dispatch(eventFactory.triggerElapsed(snapshot, incident = null)),
                        )
                } else {
                    rescheduleAlarm(snapshot, now, actions)
                }
            }

            TriggerDelayOutcomeDto.MISSED -> {
                technicalEventLog.log(TechnicalEventType.MISSED_TRIGGER_WINDOW, snapshot.sessionId)
                val incident =
                    eventFactory.buildIncident(
                        IncidentCodes.MISSED_TRIGGER_WINDOW,
                        IncidentSeverityDto.DEGRADED,
                    )
                actions += ReconcileAction.DecisionApplied(dispatch(eventFactory.triggerElapsed(snapshot, incident)))
            }
        }
    }

    private fun rescheduleAlarm(
        snapshot: SessionSnapshotDto,
        triggerAtEpochMillis: Long,
        actions: MutableList<ReconcileAction>,
    ) {
        sources.alarmScheduler.schedule(snapshot.sessionId, snapshot.revision, triggerAtEpochMillis)
        technicalEventLog.log(TechnicalEventType.ALARM_RESCHEDULED, snapshot.sessionId)
        actions += ReconcileAction.AlarmRescheduled(triggerAtEpochMillis)
    }

    private companion object {
        /**
         * Les deux pertes de permission qui rendent toute suite de la passe absurde :
         * reprogrammer une alarme sans accès aux alarmes exactes, ou poursuivre un blocage sans
         * service d'accessibilité. Les quatre autres contrôles de §13.1 sont signalés sans
         * interrompre la réconciliation — le réveil reste programmé, seul son audibilité ou son
         * affichage est compromis.
         */
        val PERMISSION_CHECKS =
            setOf(ReadinessCheckId.EXACT_ALARM, ReadinessCheckId.ACCESSIBILITY_SERVICE)

        /**
         * Les trois raisons qui ouvrent la passe par une fusion Direct Boot → Room. Les autres ne
         * peuvent pas suivre une fenêtre Direct Boot : `BEFORE_SCAN` et `SERVICE_RECREATED`
         * arrivent appareil allumé et déjà réconcilié, `LOCKED_BOOT` précède le déverrouillage, et
         * `PACKAGE_REPLACED`, `TIME_CHANGED` et `TIMEZONE_CHANGED` n'ont aucun rapport avec un
         * démarrage. Les y ajouter ferait lire le fichier de projection à chaque correction
         * d'horloge pour n'y trouver jamais rien.
         *
         * `USER_UNLOCKED` est le signal de §9.3 ; Android ne le délivrant qu'à un receveur
         * enregistré à chaud, il n'arrive que si le processus était vivant à cet instant. `BOOT` et
         * `PROCESS_START` sont le filet pour le cas contraire. La fusion est idempotente et sort
         * immédiatement quand il n'y a rien à absorber.
         */
        val MERGING_REASONS =
            setOf(ReconcileReason.USER_UNLOCKED, ReconcileReason.BOOT, ReconcileReason.PROCESS_START)

        /**
         * Les deux raisons qui décrivent un déplacement de l'horloge système, et le code d'incident
         * de chacune (SPEC_ANDROID §9.3). Codes communs et non préfixés `ANDROID_` : le fait est
         * comparable entre plateformes (SPEC_CORE_KMP §7.3). Leur gravité par défaut dans
         * `IncidentCodes.defaultSeverityOf` est bien `WARNING`.
         */
        val CLOCK_CHANGE_INCIDENT_CODES =
            mapOf(
                ReconcileReason.TIME_CHANGED to IncidentCodes.TIME_CHANGED,
                ReconcileReason.TIMEZONE_CHANGED to IncidentCodes.TIMEZONE_CHANGED,
            )
    }
}
