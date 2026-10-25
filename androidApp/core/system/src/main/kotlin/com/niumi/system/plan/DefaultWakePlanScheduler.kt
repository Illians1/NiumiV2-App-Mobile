package com.niumi.system.plan

import com.niumi.core.domain.ViolationCode
import com.niumi.core.interop.NextOccurrenceInputDto
import com.niumi.core.interop.NextOccurrenceResultDto
import com.niumi.core.interop.NextOccurrenceStatusDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakePlanDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.directboot.UnlockState
import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.LockedSession
import com.niumi.system.session.ReconcileAction
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.ReconcileResult
import com.niumi.system.session.SESSION_FINAL_STATES
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPassHook
import com.niumi.system.session.disarmRequested
import kotlinx.coroutines.CancellationException

/**
 * Dépôts et sorties du scheduler, regroupés pour rester sous `LongParameterList` de detekt (même
 * motif qu'`ActivationSources`).
 */
data class WakePlanSchedulerSources(
    val planStore: WakePlanStore,
    val unlockState: UnlockState,
    val timeZoneProvider: TimeZoneProvider,
    val technicalEventLog: TechnicalEventLog,
    val armingStatus: PlanArmingStatus,
)

private const val APPLY_BEFORE_UNLOCK = "PLAN_APPLY_BEFORE_UNLOCK"

/** Code contrôlé d'une exception levée par le crochet (§17). */
private const val HOOK_FAILURE_CODE = "PLAN_HOOK_FAILED"

/** Raisons où seul l'instant a pu bouger : rien n'est désarmé si les instants recalculés sont les mêmes. */
private val CLOCK_REASONS = setOf(PlanChangeReason.TIME_CHANGED, PlanChangeReason.TIMEZONE_CHANGED)

/** Leurs homologues de réconciliation (`SystemEventsReceiver`, actions `TIME_SET` et `TIMEZONE_CHANGED`). */
private val CLOCK_RECONCILE_REASONS = setOf(ReconcileReason.TIME_CHANGED, ReconcileReason.TIMEZONE_CHANGED)

/**
 * Le processus vient de démarrer ou l'appareil d'être déverrouillé : sans session, le planning allumé
 * doit armer — c'est aussi le nouvel essai d'un armement échoué (décision Q14 du 2026-10-24). `BOOT`
 * couvre un redémarrage où le receveur à chaud de `USER_UNLOCKED` n'était pas encore enregistré.
 */
private val REARM_REASONS = setOf(ReconcileReason.PROCESS_START, ReconcileReason.USER_UNLOCKED, ReconcileReason.BOOT)

/** États finaux qui ont une conséquence sur le planning ; `FAILED` n'en a aucune. */
private val FINISHED_STATES = setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.DISARMED)

/**
 * [WakePlanScheduler] et [SessionPassHook] d'Android (étape 44 ; SPEC_CORE_KMP §2 décisions 4 et 9,
 * §8.4 ; SPEC_ANDROID §9.2, §9.3). Le calcul de l'occurrence est commun
 * ([NiumiCoreFacade.computeNextOccurrence]) ; l'interrupteur, le stockage et l'enchaînement
 * désarmement puis armement sont natifs (décision 4 du Lot 12 du plan de refonte).
 */
class DefaultWakePlanScheduler(
    private val coordinator: SessionCoordinator,
    private val armOccurrence: ArmOccurrenceUseCase,
    private val facade: NiumiCoreFacade,
    private val eventFactory: SessionEventFactory,
    private val sources: WakePlanSchedulerSources,
) : WakePlanScheduler,
    SessionPassHook {
    /**
     * Une exception du planning dans le crochet : un échec d'armement comme un autre (décision Q14).
     * Propriété plutôt que fonction : la classe est au plafond detekt `TooManyFunctions` (11).
     */
    private val hookFailed: () -> List<ReconcileAction> = {
        failed(ActivationFailure.CoordinatorFailed(HOOK_FAILURE_CODE, state = null), sessionId = null)
        listOf(ReconcileAction.PlanRearmFailed(HOOK_FAILURE_CODE))
    }

    /**
     * Un planning invalide, ou un appel avant déverrouillage (le planning ne s'y écrit pas), est
     * refusé **avant** de toucher à la session : sinon la session en attente serait désarmée, puis
     * l'écriture lèverait, et rien ne serait armé.
     */
    override suspend fun apply(
        plan: WakePlan,
        reason: PlanChangeReason,
    ): PlanApplyResult {
        plan.requireValid()
        check(sources.unlockState.isUserUnlocked) { APPLY_BEFORE_UNLOCK }
        return coordinator.withLock { applyLocked(plan, reason, it) }
    }

    /** Scan ou recopie acceptés : la session vient de finir dans cette dispatch. */
    override suspend fun afterDispatch(
        result: DispatchResult,
        session: LockedSession,
    ) {
        val finished = (result as? DispatchResult.Applied)?.snapshot?.takeIf { it.state in FINISHED_STATES } ?: return
        guarded(hookFailed) { onFinished(finished, session) }
    }

    /**
     * Après une passe : d'abord les conséquences d'une session finie pendant la passe (reprise de
     * l'outbox, pointeur resté sur une session close) ; sinon, la suite que le planning doit à la
     * raison de la passe ([planFollowUp]). Rien avant déverrouillage : le planning y est illisible, et
     * sa lecture neutre n'est pas une extinction (point de vigilance 6).
     */
    override suspend fun afterReconcile(
        reason: ReconcileReason,
        result: ReconcileResult,
        session: LockedSession,
    ): List<ReconcileAction> {
        val finished = result.actions.lastFinishedSnapshot()
        return when {
            !sources.unlockState.isUserUnlocked -> {
                emptyList()
            }

            finished != null -> {
                guarded(hookFailed) { onFinished(finished, session) }
            }

            reason in REARM_REASONS || reason in CLOCK_RECONCILE_REASONS -> {
                guarded(hookFailed) { planFollowUp(reason, session) }
            }

            else -> {
                emptyList()
            }
        }
    }

    /**
     * Planning allumé seulement : au démarrage ou au déverrouillage sans session, l'occurrence est
     * armée ; après un déplacement de l'horloge ou du fuseau, la session en attente est recalculée.
     * Planning éteint (session ponctuelle de l'écran 6 transitoire) : rien, l'instant est conservé.
     */
    private suspend fun planFollowUp(
        reason: ReconcileReason,
        session: LockedSession,
    ): List<ReconcileAction> {
        val plan = sources.planStore.read()
        if (!plan.enabled) return emptyList()
        val current = session.load()
        val due =
            when (reason) {
                in REARM_REASONS -> current is LoadResult.Absent
                in CLOCK_RECONCILE_REASONS -> (current as? LoadResult.Present)?.snapshot?.isPendingArmed() == true
                else -> false
            }
        return if (due) applyLocked(plan, reason.toPlanChangeReason(), session).toActions() else emptyList()
    }

    /**
     * `COMPLETED` (scan ou recopie du matin) ou `DISARMED` repris par le réconciliateur : l'occurrence
     * suivante est armée (`design-plan/SPECIFICATION.md` §15). `CANCELLED` (annulation, recopie du
     * soir) : l'interrupteur est éteint (§11, §13). Le pointeur de la session finie est effacé d'abord
     * s'il est encore là, sans quoi le moteur refuserait l'activation (point de vigilance 8).
     */
    private suspend fun onFinished(
        snapshot: SessionSnapshotDto,
        session: LockedSession,
    ): List<ReconcileAction> {
        if (!sources.unlockState.isUserUnlocked) return emptyList()
        val pointed = (session.load() as? LoadResult.Present)?.snapshot
        if (pointed?.sessionId == snapshot.sessionId) session.clearActive(snapshot.sessionId)

        val plan = sources.planStore.read()
        return when {
            !plan.enabled -> {
                emptyList()
            }

            snapshot.state == SessionStateDto.CANCELLED -> {
                sources.planStore.write(plan.copy(enabled = false))
                sources.armingStatus.reportOk()
                listOf(ReconcileAction.PlanSwitchedOff)
            }

            else -> {
                applyLocked(plan, PlanChangeReason.SESSION_FINISHED, session).toActions()
            }
        }
    }

    /**
     * Sous le verrou du coordinateur : libère ce qui peut l'être, enregistre [plan], arme
     * l'occurrence. Rien n'est enregistré quand la session en place l'interdit.
     */
    private suspend fun applyLocked(
        plan: WakePlan,
        reason: PlanChangeReason,
        session: LockedSession,
    ): PlanApplyResult {
        val next =
            if (plan.enabled) {
                facade.nextOccurrence(plan, sources.timeZoneProvider.currentZoneId(), eventFactory.nowEpochMillis())
            } else {
                null
            }
        val held = releaseCurrent(plan, next, reason, session)
        if (held != null) return held
        if (plan != sources.planStore.read()) sources.planStore.write(plan)
        return armNext(next, session)
    }

    /**
     * `null` quand la voie est libre pour armer. Session finie encore pointée (effacement
     * best-effort, passe interrompue) : pointeur effacé. Session en attente : désarmée, sauf si
     * l'horloge ou le fuseau n'ont changé aucun de ses instants. Toute autre session active : rien ne
     * se modifie du coucher au scan (SPEC_CORE_KMP §2, décision 4).
     */
    private suspend fun releaseCurrent(
        plan: WakePlan,
        next: NextOccurrenceResultDto?,
        reason: PlanChangeReason,
        session: LockedSession,
    ): PlanApplyResult? {
        val current = (session.load() as? LoadResult.Present)?.snapshot ?: return null
        return when {
            current.state in SESSION_FINAL_STATES -> {
                session.clearActive(current.sessionId)
                null
            }

            current.isPendingArmed() && reason in CLOCK_REASONS && next.hasSameInstantsAs(current) -> {
                PlanApplyResult.Unchanged
            }

            current.isPendingArmed() -> {
                disarm(current, plan, session)
            }

            else -> {
                PlanApplyResult.LockedBySession
            }
        }
    }

    private suspend fun armNext(
        next: NextOccurrenceResultDto?,
        session: LockedSession,
    ): PlanApplyResult {
        val occurrence = next?.occurrence
        return when {
            next == null -> {
                sources.armingStatus.reportOk()
                PlanApplyResult.Disabled
            }

            next.status == NextOccurrenceStatusDto.NO_DAY_ENABLED -> {
                sources.armingStatus.reportOk()
                PlanApplyResult.NoDayEnabled
            }

            occurrence == null -> {
                failed(ActivationFailure.InvalidPlan(next.status), null)
            }

            else -> {
                when (val armed = armOccurrence.arm(occurrence, session)) {
                    is ArmSessionResult.Armed -> {
                        sources.technicalEventLog.log(TechnicalEventType.PLAN_REARMED, armed.snapshot.sessionId)
                        sources.armingStatus.reportOk()
                        PlanApplyResult.Armed(occurrence)
                    }

                    is ArmSessionResult.Failed -> {
                        failed(armed.failure, null)
                    }
                }
            }
        }
    }

    /**
     * `DISARM_REQUESTED`, refermé par le coordinateur jusqu'à `DISARMED`. `null` si la session est
     * désarmée et que l'armement peut suivre, sinon l'issue à renvoyer telle quelle.
     *
     * Le moteur refuse dès le coucher (`BLOCKING_START_ALREADY_ELAPSED`), même si l'alarme de début
     * n'a pas encore été traitée : le blocage dû est alors appliqué ici, comme `BlockingStartHandler`
     * l'aurait fait, et la session reste verrouillée jusqu'au scan.
     */
    private suspend fun disarm(
        snapshot: SessionSnapshotDto,
        plan: WakePlan,
        session: LockedSession,
    ): PlanApplyResult? =
        when (val result = session.dispatch(eventFactory.disarmRequested(snapshot))) {
            is DispatchResult.Rejected -> {
                if (result.violations.any { it.code == ViolationCode.BLOCKING_START_ALREADY_ELAPSED }) {
                    session.dispatch(eventFactory.blockingStartElapsed(snapshot, incident = null))
                    PlanApplyResult.LockedBySession
                } else {
                    failed(ActivationFailure.Rejected(result.violations), snapshot.sessionId)
                }
            }

            is DispatchResult.Duplicate -> {
                failed(ActivationFailure.Duplicate, snapshot.sessionId)
            }

            is DispatchResult.Applied -> {
                val state = result.snapshot?.state
                when (state) {
                    SessionStateDto.DISARMED -> {
                        null
                    }

                    SessionStateDto.RELEASING -> {
                        disarmInterrupted(result.snapshot, plan)
                    }

                    else -> {
                        failed(
                            ActivationFailure.CoordinatorFailed(result.snapshot?.failureCode, state),
                            snapshot.sessionId,
                        )
                    }
                }
            }
        }

    /**
     * `RELEASING` : l'alarme n'a pas pu être annulée. Jamais d'armement par-dessus (point de
     * vigilance 8) ; mais le moteur a déjà fixé la cible `DISARMED`, donc le planning reçu est
     * enregistré dès maintenant : c'est lui que la reprise de l'outbox armera en refermant le
     * désarmement, et une extinction ne sera jamais suivie d'un réarmement. Un échec d'armement n'est
     * annoncé que si le planning demande un réveil : l'extinction n'en attend aucun, et l'échec de
     * nettoyage est déjà journalisé (`RELEASE_PARTIAL_FAILURE`).
     */
    private suspend fun disarmInterrupted(
        releasing: SessionSnapshotDto?,
        plan: WakePlan,
    ): PlanApplyResult {
        if (plan != sources.planStore.read()) sources.planStore.write(plan)
        val failure = ActivationFailure.CoordinatorFailed(releasing?.failureCode, SessionStateDto.RELEASING)
        return if (plan.enabled) {
            failed(failure, releasing?.sessionId)
        } else {
            sources.armingStatus.reportOk()
            PlanApplyResult.Failed(failure)
        }
    }

    private fun failed(
        failure: ActivationFailure,
        sessionId: String?,
    ): PlanApplyResult {
        sources.technicalEventLog.log(
            TechnicalEventType.PLAN_REARM_FAILED,
            sessionId,
            TechnicalEventDetails.errorCode(failure.diagnosticCode),
        )
        sources.armingStatus.reportFailed(failure, eventFactory.nowEpochMillis())
        return PlanApplyResult.Failed(failure)
    }
}

/** Même réveil, même coucher : le déplacement d'horloge ou de fuseau n'a rien changé à cette session. */
private fun NextOccurrenceResultDto?.hasSameInstantsAs(snapshot: SessionSnapshotDto): Boolean {
    val occurrence = this?.occurrence ?: return false
    return occurrence.wakeSchedule.triggerAtEpochMillis == snapshot.wakeSchedule.triggerAtEpochMillis &&
        occurrence.blockingSchedule.startsAtEpochMillis == snapshot.blockingSchedule.startsAtEpochMillis
}

/** Seules les raisons de [REARM_REASONS] et [CLOCK_RECONCILE_REASONS] y parviennent. */
private fun ReconcileReason.toPlanChangeReason(): PlanChangeReason =
    when (this) {
        ReconcileReason.USER_UNLOCKED -> PlanChangeReason.USER_UNLOCKED
        ReconcileReason.TIME_CHANGED -> PlanChangeReason.TIME_CHANGED
        ReconcileReason.TIMEZONE_CHANGED -> PlanChangeReason.TIMEZONE_CHANGED
        else -> PlanChangeReason.PROCESS_START
    }

private fun SessionSnapshotDto.isPendingArmed(): Boolean = state == SessionStateDto.ARMED && isBlockingPending

/** La dernière session finie pendant la passe : refermée par la reprise de l'outbox, ou pointeur effacé. */
private fun List<ReconcileAction>.lastFinishedSnapshot(): SessionSnapshotDto? =
    mapNotNull { action ->
        when (action) {
            is ReconcileAction.DecisionApplied -> (action.dispatchResult as? DispatchResult.Applied)?.snapshot
            is ReconcileAction.PointerCleared -> action.snapshot
            else -> null
        }
    }.lastOrNull { it.state in FINISHED_STATES }

private fun PlanApplyResult.toActions(): List<ReconcileAction> =
    when (this) {
        is PlanApplyResult.Armed -> listOf(ReconcileAction.PlanRearmed(occurrence.wakeSchedule.triggerAtEpochMillis))
        is PlanApplyResult.Failed -> listOf(ReconcileAction.PlanRearmFailed(failure.diagnosticCode))
        else -> emptyList()
    }

/**
 * La passe qui précède est déjà enregistrée : une exception du planning (DataStore qui lève, dépôt
 * illisible) ne doit jamais remonter à son appelant — un scan accepté apparaîtrait comme un échec, ou
 * le receveur système planterait. [onFailure] la journalise et la publie comme un échec d'armement ;
 * `CancellationException` reste propagée. Fonction de fichier : la classe est au plafond detekt
 * `TooManyFunctions` (11), même motif que `rescheduleAlarm` dans `SessionReconciler`.
 *
 * `SwallowedException` : le journal technique n'accepte qu'un code contrôlé (§17), jamais le message
 * d'une exception, qui pourrait porter un chemin ou une donnée de l'utilisateur.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
private suspend fun guarded(
    onFailure: () -> List<ReconcileAction>,
    block: suspend () -> List<ReconcileAction>,
): List<ReconcileAction> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        onFailure()
    }

private fun NiumiCoreFacade.nextOccurrence(
    plan: WakePlan,
    zoneId: String,
    nowEpochMillis: Long,
): NextOccurrenceResultDto =
    computeNextOccurrence(
        NextOccurrenceInputDto(
            plan = WakePlanDto(plan.enabledIsoDays, plan.wakeTimeIso, plan.bedtimeIso),
            zoneId = zoneId,
            nowEpochMillis = nowEpochMillis,
        ),
    )
