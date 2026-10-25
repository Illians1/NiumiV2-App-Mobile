package com.niumi.system.plan.fakes

import com.niumi.core.interop.EmergencyTranscriptionInputDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.system.emergency.EmergencyExitEventFactory
import com.niumi.system.plan.ArmOccurrenceUseCase
import com.niumi.system.plan.DefaultWakePlanScheduler
import com.niumi.system.plan.PlanArmingStatus
import com.niumi.system.plan.WakePlan
import com.niumi.system.plan.WakePlanSchedulerSources
import com.niumi.system.readiness.fakes.FakePairedBoxStore
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.HookedSessionCoordinator
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness

/** Lundi à vendredi, 7:00, coucher 22:30, interrupteur allumé. */
val WEEKDAYS_PLAN = WakePlan(enabled = true, enabledIsoDays = setOf(1, 2, 3, 4, 5), "07:00", "22:30")

/** Jeudi 22 octobre 2026, 20:00 à Paris : avant le coucher du jeudi soir. */
val THURSDAY_EVENING = epochMillisAt("2026-10-22T20:00")

/**
 * Le coordinateur complet du [TestCoordinatorHarness] (vrai moteur KMP), enveloppé comme en
 * production par [HookedSessionCoordinator] et branché sur un vrai [DefaultWakePlanScheduler].
 * L'appareil est déverrouillé : c'est la seule condition où le planning se lit et se réapplique.
 */
class PlanTestHarness(
    plan: WakePlan = WEEKDAYS_PLAN,
    now: Long = THURSDAY_EVENING,
) {
    val core: TestCoordinatorHarness =
        TestCoordinatorHarness().apply {
            clock.now = now
            unlockState.isUserUnlocked = true
        }
    val planStore = InMemoryWakePlanStore(plan)
    val pairedBoxStore = FakePairedBoxStore()
    val appSelectionStore = InMemoryAppSelectionStore()
    val alarmSoundPreferences = InMemoryAlarmSoundPreferences()
    val timeZoneProvider = MutableTimeZoneProvider()
    val armingStatus = PlanArmingStatus()

    private val armOccurrence =
        ArmOccurrenceUseCase(core.facade, core.eventFactory, pairedBoxStore, appSelectionStore, alarmSoundPreferences)

    /** Le crochet est le scheduler, construit après le coordinateur qui le reçoit (cycle résolu par `Provider`). */
    private var hook: DefaultWakePlanScheduler? = null

    val coordinator: SessionCoordinator = HookedSessionCoordinator(core.coordinator) { requireNotNull(hook) }

    val scheduler: DefaultWakePlanScheduler =
        DefaultWakePlanScheduler(
            coordinator,
            armOccurrence,
            core.facade,
            core.eventFactory,
            WakePlanSchedulerSources(
                planStore,
                core.unlockState,
                timeZoneProvider,
                core.technicalEventLog,
                armingStatus,
            ),
        ).also { hook = it }

    /** `ALARM_FIRED` à l'heure du réveil de la session active, par le coordinateur enveloppé. */
    suspend fun ring(): SessionSnapshotDto {
        val armed = requireNotNull(activeSnapshot())
        core.clock.now = armed.wakeSchedule.triggerAtEpochMillis
        return (coordinator.dispatch(core.eventFactory.alarmFired(armed)) as DispatchResult.Applied).snapshot!!
    }

    /** Scan valide de la session active (preuve délivrée par la façade). */
    suspend fun scan(): DispatchResult {
        val current = requireNotNull(activeSnapshot())
        return coordinator.dispatch(
            SessionDtoFixtures.validNfcScanned(
                facade = core.facade,
                eventId = core.idGenerator.newId(),
                sessionId = current.sessionId,
                expectedRevision = current.revision,
                occurredAtEpochMillis = core.clock.now,
            ),
        )
    }

    /** Recopie complète du texte de secours pour la session active. */
    suspend fun emergencyExit(): DispatchResult {
        val current = requireNotNull(activeSnapshot())
        val factory = EmergencyExitEventFactory(core.idGenerator, core.clock)
        val context = factory.verificationContext(current)
        val text = core.facade.emergencyExitTexts().first()
        val proof =
            requireNotNull(
                core.facade
                    .verifyEmergencyTranscription(
                        EmergencyTranscriptionInputDto(text.id, text.text),
                        context,
                    ).proof,
            )
        return coordinator.dispatch(factory.emergencyExitConfirmed(context, proof))
    }

    /**
     * Pointeur actif laissé sur une session finie, comme après une libération sur appareil verrouillé
     * (Room ne s'efface qu'une fois déverrouillé) ou un `CLEAR_ACTIVE_SESSION` interrompu.
     */
    suspend fun leaveFinishedSessionPointed(state: SessionStateDto) {
        val snapshot =
            SessionDtoFixtures.snapshotInState(
                state,
                sessionId = SessionDtoFixtures.OTHER_SESSION_ID,
                revision = 5,
            )
        core.gateway.commit(
            StoredDecision(
                snapshot = snapshot,
                receipt = EventReceipt(core.idGenerator.newId(), snapshot.sessionId, "0".repeat(64), 5, core.clock.now),
                effects = emptyList(),
                androidExtras = SessionDtoFixtures.extras(),
            ),
        )
    }

    suspend fun activeSnapshot(): SessionSnapshotDto? = (core.gateway.load() as? LoadResult.Present)?.snapshot

    /** Dernier état committé de chaque session, dans l'ordre de création. */
    fun finalStatesBySession(): List<Pair<String, SessionStateDto>> =
        core.gateway.committed
            .groupBy { it.sessionId }
            .map { (sessionId, snapshots) -> sessionId to snapshots.last().state }
}
