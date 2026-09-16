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
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.alarm.RingingWatchdogPolicy
import com.niumi.system.alarm.WatchdogAction
import com.niumi.system.boot.DirectBootMergeOutcome
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
    internal val sources: ReconcilerSources,
    private val facade: NiumiCoreFacade,
    private val eventFactory: SessionEventFactory,
    internal val technicalEventLog: TechnicalEventLog,
) {
    // Clauses de garde séquentielles (snapshot illisible, absent) avant le corps principal — même
    // motif que `NfcReducer.onValidScan` (:shared:core), voir `ETAPE-07.md`.
    @Suppress("ReturnCount")
    suspend fun reconcile(
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
    ): ReconcileResult {
        // Versement du journal technique d'avant déverrouillage (§17, étape 20), avant même la
        // fusion : les deux vivent des raisons de « le processus est enfin vivant et déverrouillé »,
        // et le vidage doit avoir lieu y compris quand il n'y a aucune projection Direct Boot à
        // fusionner — c'est de loin le cas le plus fréquent. `flush()` est son propre garde-fou
        // (verrouillé → no-op).
        if (reason in MERGING_REASONS) sources.technicalEventFlush.flush()

        // SPEC_ANDROID §9.3, dernier alinéa : « À `USER_UNLOCKED`, le réconciliateur fusionne de
        // façon idempotente le registre et l'outbox Direct Boot dans Room. » Avant `gateway.load()`,
        // sans quoi la passe déciderait sur un Room amputé de ce qui a été fait avant le
        // déverrouillage. Sous le mutex du coordinateur, qui le tient pendant tout cet appel.
        // Résultat consommé depuis l'étape 20 : un `Corrupted` doit être journalisé et consigné,
        // pas seulement rejoué en silence (auparavant jeté ici, la ligne suivante suffisait déjà à
        // masquer une corruption Direct Boot tant que Room restait lisible).
        val mergeOutcome = if (reason in MERGING_REASONS) sources.directBootMerger.merge() else null

        val loaded = gateway.load()
        if (loaded is LoadResult.Unreadable) {
            // Aucune session lisible : ni `sessionId` ni révision n'existent pour porter un
            // `SessionIncident` (SPEC_CORE_KMP §13, « aucune suppression silencieuse ») — seul
            // l'événement technique est possible dans ce cas, y compris quand la fusion ci-dessus a
            // elle aussi trouvé le Direct Boot corrompu : les deux causes convergent vers la même
            // conclusion, rien à faire de plus qu'à consigner le fait et laisser le blocage en
            // place (§18).
            technicalEventLog.log(TechnicalEventType.SNAPSHOT_CORRUPTED, sessionId = null)
            sources.storageIntegrity.reportUnreadable(loaded.reason)
            return ReconcileResult(sessionId = null, actions = listOf(ReconcileAction.SnapshotCorrupted))
        }
        sources.storageIntegrity.reportReadable()
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

        // La projection Direct Boot était illisible, mais Room, lui, l'est : on connaît maintenant
        // le `sessionId` qui manquait à `DirectBootMerger` pour journaliser et consigner
        // (`DirectBootMerger.merge()` a déjà réécrit la projection depuis Room dans ce cas).
        var workingPresent = present
        if (mergeOutcome is DirectBootMergeOutcome.Corrupted) {
            technicalEventLog.log(TechnicalEventType.SNAPSHOT_CORRUPTED, sessionId)
            val corrected =
                reportIncidentOnce(
                    present.snapshot,
                    IncidentCodes.SNAPSHOT_CORRUPTED,
                    IncidentSeverityDto.CRITICAL,
                    dispatch,
                    actions,
                )
            workingPresent = present.copy(snapshot = corrected)
        }

        val replayed = replayOutbox(workingPresent, dispatch, actions)
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

        // Après la décision de cette passe, jamais avant : sur `RINGING`, le watchdog n'est armé
        // qu'une fois `resumeRinging` (ci-dessus) déjà exécuté (SPEC_ANDROID §10.2, étape 20).
        sources.ringingWatchdog.applyPolicyFor(snapshot)

        // Snapshot relu, pas celui du début de passe : les branches ci-dessus ont pu dispatcher
        // des incidents qui ont avancé la révision, et un état final a pu vider le pointeur actif
        // (`gateway.load()` rend alors `Absent`, ce qui écarte naturellement les sessions closes
        // du contrôle §7.1 sans test explicite sur l'état).
        (gateway.load() as? LoadResult.Present)?.snapshot?.let { latest ->
            sources.runtimeReconciler.reconcile(latest, dispatch)
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
        val code = CLOCK_CHANGE_INCIDENT_CODES[reason] ?: return snapshot
        return reportIncidentOnce(snapshot, code, IncidentSeverityDto.WARNING, dispatch, actions)
    }

    /**
     * Un incident par code et par session, quelle que soit la cause — factorisé à l'étape 20 entre
     * [reportClockChange] et la corruption Direct Boot (même garde, seul le code et la gravité
     * changent). Avant déverrouillage, [ReconcilerSources.incidentsReader] renvoie une liste vide
     * sans pouvoir dire si des incidents existent (§7.3) : l'incident est alors enregistré plutôt
     * que perdu.
     */
    private suspend fun reportIncidentOnce(
        snapshot: SessionSnapshotDto,
        code: String,
        severity: IncidentSeverityDto,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ): SessionSnapshotDto {
        if (snapshot.state in SESSION_FINAL_STATES ||
            sources.incidentsReader.incidents(snapshot.sessionId).any { it.code == code }
        ) {
            return snapshot
        }

        val incident = eventFactory.buildIncident(code, severity)
        val result = dispatch(eventFactory.incidentReported(snapshot, incident))
        actions += ReconcileAction.IncidentDispatched(code, severity)
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
        if (refreshed == null || refreshed.state != SessionStateDto.ARMED) return

        // Le début du blocage **avant** le retard du réveil (§12.4) : une session dont les deux
        // instants sont dépassés doit d'abord voir son blocage demandé, faute de quoi le
        // `TRIGGER_ELAPSED` l'appliquerait par le repli du moteur (SPEC_CORE_KMP §5.1) et le
        // `BLOCKING_START_ELAPSED` serait ensuite refusé — le journal ne garderait alors aucune trace
        // du début manqué, et `MISSED_BLOCKING_START_WINDOW` ne serait jamais consigné.
        reconcileBlockingStart(refreshed, reason, dispatch, actions)

        // Quatrième relecture de ce motif dans le dépôt : `BLOCKING_START_ELAPSED` incrémente la
        // révision, et poursuivre sur `refreshed` ferait tomber le `TRIGGER_ELAPSED` en
        // `STALE_REVISION` (égalité stricte d'`expectedRevision`, `SessionEventValidation`). Même
        // défaut qu'à l'étape 17 (`SessionReadinessMonitor`), au rejeu d'outbox et à l'étape 18
        // (juste au-dessus).
        val afterBlocking = (gateway.load() as? LoadResult.Present)?.snapshot
        if (afterBlocking != null && afterBlocking.state == SessionStateDto.ARMED) {
            reconcileTriggerDelay(afterBlocking, reason, dispatch, actions)
        }
    }

    /**
     * Début d'un blocage différé dont l'instant est atteint, ou alarme de début disparue
     * (SPEC_ANDROID §12.4, §9.3 ; SPEC_CORE_KMP §8.3). Sans objet pour une session à blocage
     * immédiat, ou différé déjà demandé : `isBlockingPending` porte la règle unique, jamais recopiée
     * ici.
     *
     * Le retard est lu par [NiumiCoreFacade.evaluateTriggerDelay], la même politique que celle du
     * réveil appliquée à `blockingStartsAtEpochMillis` — SPEC_CORE_KMP §8.3 l'impose explicitement
     * (« en réutilisant `evaluateTriggerDelay` »), et Android ne recalcule jamais une règle commune.
     *
     * Trois différences voulues avec [reconcileTriggerDelay] : `FIRE_NOW` **produit l'événement** au
     * lieu de reprogrammer une alarme immédiate, et ce quelle que soit la raison de la passe — « un
     * blocage ne se manque pas, il s'applique en retard » (§8.3), là où le réveil réserve ce chemin à
     * `BEFORE_SCAN` parce qu'un réveil, lui, doit sonner ; la gravité de l'incident est `WARNING` et
     * non `DEGRADED`, un blocage appliqué en retard n'ayant dégradé aucune promesse
     * (`IncidentCodes.defaultSeverityOf`) ; et l'instant reprogrammé est **contractuel**, jamais
     * `now`, `startsAtEpochMillis` étant immuable après l'activation.
     */
    private suspend fun reconcileBlockingStart(
        snapshot: SessionSnapshotDto,
        reason: ReconcileReason,
        dispatch: suspend (SessionEventDto) -> DispatchResult,
        actions: MutableList<ReconcileAction>,
    ) {
        if (!snapshot.isBlockingPending) return
        val startsAt = snapshot.blockingSchedule.startsAtEpochMillis ?: return
        val now = eventFactory.nowEpochMillis()

        when (facade.evaluateTriggerDelay(TriggerDelayInputDto(startsAt, now)).outcome) {
            TriggerDelayOutcomeDto.NOT_REACHED -> {
                // Même condition que pour l'alarme du réveil (§9.3) : sur un déplacement d'horloge,
                // le réenregistrement est inconditionnel, un `PendingIntent` encore présent ne
                // prouvant pas que le système l'a conservé au bon instant.
                val clockMoved = reason in CLOCK_CHANGE_INCIDENT_CODES
                if (clockMoved || !sources.blockingStartScheduler.isScheduled(snapshot.sessionId)) {
                    sources.blockingStartScheduler.schedule(snapshot.sessionId, snapshot.revision, startsAt)
                    technicalEventLog.log(TechnicalEventType.BLOCKING_START_RESCHEDULED, snapshot.sessionId)
                    actions += ReconcileAction.BlockingStartRescheduled(startsAt)
                }
            }

            TriggerDelayOutcomeDto.FIRE_NOW -> {
                actions += ReconcileAction.DecisionApplied(dispatch(eventFactory.blockingStartElapsed(snapshot, null)))
            }

            TriggerDelayOutcomeDto.MISSED -> {
                technicalEventLog.log(TechnicalEventType.MISSED_BLOCKING_START_WINDOW, snapshot.sessionId)
                val incident =
                    eventFactory.buildIncident(
                        IncidentCodes.MISSED_BLOCKING_START_WINDOW,
                        IncidentSeverityDto.WARNING,
                    )
                actions +=
                    ReconcileAction.DecisionApplied(dispatch(eventFactory.blockingStartElapsed(snapshot, incident)))
            }
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
                    rescheduleAlarm(sources, technicalEventLog, snapshot, triggerAt, actions)
                }
            }

            TriggerDelayOutcomeDto.FIRE_NOW -> {
                if (reason == ReconcileReason.BEFORE_SCAN) {
                    actions +=
                        ReconcileAction.DecisionApplied(
                            dispatch(eventFactory.triggerElapsed(snapshot, incident = null)),
                        )
                } else {
                    rescheduleAlarm(sources, technicalEventLog, snapshot, now, actions)
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

/**
 * Reprogrammation de l'alarme du réveil au même instant contractuel (§9.3). Fonction de fichier
 * depuis le Lot 6 : la classe est au plafond detekt `TooManyFunctions` (11), et [reconcileBlockingStart]
 * y a pris la place — celle-ci est la plus mécanique des deux, et la seule qui n'ait besoin d'aucun
 * état du réconciliateur hors de ses deux collaborateurs.
 */
private fun rescheduleAlarm(
    sources: ReconcilerSources,
    technicalEventLog: TechnicalEventLog,
    snapshot: SessionSnapshotDto,
    triggerAtEpochMillis: Long,
    actions: MutableList<ReconcileAction>,
) {
    sources.alarmScheduler.schedule(snapshot.sessionId, snapshot.revision, triggerAtEpochMillis)
    technicalEventLog.log(TechnicalEventType.ALARM_RESCHEDULED, snapshot.sessionId)
    actions += ReconcileAction.AlarmRescheduled(triggerAtEpochMillis)
}

// Fonction de fichier plutôt que membre de la classe : même motif qu'`UnlockAwarePersistenceGateway`
// (`replayableEffectsOf`), au plafond detekt `TooManyFunctions` (11) depuis l'ajout de la fusion et
// du changement d'horloge à l'étape 19.
private fun RingingWatchdog.applyPolicyFor(snapshot: SessionSnapshotDto): OperationResult =
    when (RingingWatchdogPolicy.decide(snapshot.state)) {
        WatchdogAction.Arm -> arm(snapshot.sessionId)
        WatchdogAction.Disarm -> disarm(snapshot.sessionId)
    }
