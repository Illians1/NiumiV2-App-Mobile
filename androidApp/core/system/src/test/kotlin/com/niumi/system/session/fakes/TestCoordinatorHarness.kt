package com.niumi.system.session.fakes

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.system.readiness.AndroidDeviceReadinessChecker
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.readiness.fakes.FakeSessionWarningNotifier
import com.niumi.system.readiness.fakes.ReadinessTestSources
import com.niumi.system.session.DefaultSessionCoordinator
import com.niumi.system.session.EffectDispatcher
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.FacadeSessionReducer
import com.niumi.system.session.ReconcilerSources
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionReconciler
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.executors.ApplyBlockingExecutor
import com.niumi.system.session.executors.CancelAlarmExecutor
import com.niumi.system.session.executors.ClearActiveSessionExecutor
import com.niumi.system.session.executors.ClearScanRequestExecutor
import com.niumi.system.session.executors.PresentScanRequestExecutor
import com.niumi.system.session.executors.PublishSnapshotExecutor
import com.niumi.system.session.executors.RecordIncidentExecutor
import com.niumi.system.session.executors.RemoveBlockingExecutor
import com.niumi.system.session.executors.ScheduleAlarmExecutor
import com.niumi.system.session.executors.StartRingingExecutor
import com.niumi.system.session.executors.StopRingingExecutor

/**
 * Assemble un [SessionCoordinator] complet avec des fakes, réutilisé par tous les tests du
 * coordinateur (« Interfaces transverses » du plan MVP, étendu à l'étape 11). Le réducteur enrobe
 * la vraie [NiumiCoreFacade] (comportement de domaine réel) plutôt qu'un faux réducteur : seule
 * l'idempotence — propre à l'étape 11 — a besoin d'être prouvée par un comptage d'appels.
 *
 * Sans paramètre de construction (aucun site d'appel ne fournit de fake de substitution : les
 * scénarios configurent les propriétés mutables des fakes après coup) : un constructeur à onze
 * paramètres dépasserait `LongParameterList` de detekt sans bénéfice réel.
 */
private const val ANDROID_16 = 36

class TestCoordinatorHarness {
    val journal: CallJournal = CallJournal()
    val gateway: InMemoryPersistenceGateway = InMemoryPersistenceGateway(journal)
    val alarmScheduler: FakeAlarmScheduler = FakeAlarmScheduler(journal)
    val blockingController: FakeBlockingController = FakeBlockingController(journal)
    val ringingController: FakeRingingController = FakeRingingController(journal)
    val scanRequestNotifier: FakeScanRequestNotifier = FakeScanRequestNotifier(journal)
    val accessibilityServiceStatus: FakeAccessibilityServiceStatus = FakeAccessibilityServiceStatus()
    val blockedPackagesProjection: FakeBlockedPackagesProjection = FakeBlockedPackagesProjection()
    val technicalEventLog: FakeTechnicalEventLog = FakeTechnicalEventLog()
    val clock: FakeClock = FakeClock(1_000L)
    val publisher: SessionSnapshotPublisher = SessionSnapshotPublisher()
    val facade: NiumiCoreFacade = NiumiCoreFacade()

    val idGenerator = SequentialIdGenerator()
    val eventFactory = SessionEventFactory(idGenerator, clock)
    val recordingReducer = RecordingSessionReducer(FacadeSessionReducer(facade))

    /**
     * Le moniteur de §13.1 partage les fakes du harnais (`alarmScheduler`,
     * `accessibilityServiceStatus`) : un test qui coupe une permission la coupe donc aussi pour
     * le diagnostic, comme sur un vrai appareil.
     */
    val readinessSources = ReadinessTestSources(alarmScheduler, accessibilityServiceStatus)
    val warningNotifier = FakeSessionWarningNotifier()
    val readinessMonitor =
        SessionReadinessMonitor(
            readinessChecker = AndroidDeviceReadinessChecker(readinessSources.build(), clock, ANDROID_16),
            warningNotifier = warningNotifier,
            eventFactory = eventFactory,
            technicalEventLog = technicalEventLog,
        )

    private val sources = ReconcilerSources(alarmScheduler, blockedPackagesProjection, readinessMonitor)

    private val executors: Map<SessionEffectKindDto, EffectExecutor> =
        mapOf(
            SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to PublishSnapshotExecutor(publisher, technicalEventLog),
            SessionEffectKindDto.SCHEDULE_ALARM to ScheduleAlarmExecutor(alarmScheduler, technicalEventLog),
            SessionEffectKindDto.CANCEL_ALARM to CancelAlarmExecutor(alarmScheduler),
            SessionEffectKindDto.APPLY_BLOCKING to ApplyBlockingExecutor(blockingController, technicalEventLog),
            SessionEffectKindDto.REMOVE_BLOCKING to RemoveBlockingExecutor(blockingController, clock),
            SessionEffectKindDto.START_RINGING to StartRingingExecutor(ringingController, technicalEventLog),
            SessionEffectKindDto.STOP_RINGING to StopRingingExecutor(ringingController),
            SessionEffectKindDto.PRESENT_SCAN_REQUEST to
                PresentScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_SCAN_REQUEST to ClearScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_ACTIVE_SESSION to ClearActiveSessionExecutor(gateway),
            SessionEffectKindDto.RECORD_INCIDENT to RecordIncidentExecutor(gateway),
        )

    val effectDispatcher = EffectDispatcher(executors, gateway)

    val reconciler =
        SessionReconciler(gateway, effectDispatcher, sources, facade, eventFactory, technicalEventLog)

    val coordinator: SessionCoordinator =
        DefaultSessionCoordinator(recordingReducer, gateway, effectDispatcher, reconciler, eventFactory)

    /**
     * Variante où un seul exécuteur est remplacé (`SessionCoordinatorActivationTest` : force
     * `PUBLISH_PLATFORM_SNAPSHOT` en échec pour prouver qu'un effet best-effort ne bloque jamais
     * une phase). Les dix autres exécuteurs restent ceux du harnais.
     */
    fun coordinatorWithExecutorOverride(
        kind: SessionEffectKindDto,
        executor: EffectExecutor,
    ): SessionCoordinator {
        val overridden = executors + (kind to executor)
        val dispatcher = EffectDispatcher(overridden, gateway)
        val recon =
            SessionReconciler(gateway, dispatcher, sources, facade, eventFactory, technicalEventLog)
        return DefaultSessionCoordinator(recordingReducer, gateway, dispatcher, recon, eventFactory)
    }

    /** Variante avec une [SessionPersistenceGateway] de substitution (`SessionCoordinatorMutexTest`). */
    fun coordinatorWith(customGateway: SessionPersistenceGateway): SessionCoordinator {
        val customDispatcher = EffectDispatcher(executors, customGateway)
        val customReconciler =
            SessionReconciler(customGateway, customDispatcher, sources, facade, eventFactory, technicalEventLog)
        return DefaultSessionCoordinator(
            recordingReducer,
            customGateway,
            customDispatcher,
            customReconciler,
            eventFactory,
        )
    }
}
