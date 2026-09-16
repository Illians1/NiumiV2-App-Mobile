package com.niumi.system.session.fakes

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.system.boot.DirectBootMerger
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.boot.fakes.InMemoryDirectBootStore
import com.niumi.system.boot.fakes.InMemorySessionStore
import com.niumi.system.boot.fakes.RecordingDirectBootRoomMerge
import com.niumi.system.readiness.AndroidDeviceReadinessChecker
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.readiness.fakes.FakeSessionWarningNotifier
import com.niumi.system.readiness.fakes.ReadinessTestSources
import com.niumi.system.readiness.fakes.RecordingSessionIncidentsReader
import com.niumi.system.session.DefaultSessionCoordinator
import com.niumi.system.session.EffectDispatcher
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.FacadeSessionReducer
import com.niumi.system.session.ReconcilerSources
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionReconciler
import com.niumi.system.session.SessionRuntimeReconciler
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.StorageIntegrityState
import com.niumi.system.session.executors.ApplyBlockingExecutor
import com.niumi.system.session.executors.CancelAlarmExecutor
import com.niumi.system.session.executors.CancelBlockingStartExecutor
import com.niumi.system.session.executors.ClearActiveSessionExecutor
import com.niumi.system.session.executors.ClearScanRequestExecutor
import com.niumi.system.session.executors.PresentScanRequestExecutor
import com.niumi.system.session.executors.PublishSnapshotExecutor
import com.niumi.system.session.executors.RecordIncidentExecutor
import com.niumi.system.session.executors.RemoveBlockingExecutor
import com.niumi.system.session.executors.ScheduleAlarmExecutor
import com.niumi.system.session.executors.ScheduleBlockingStartExecutor
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
    val blockingStartScheduler: FakeBlockingStartScheduler = FakeBlockingStartScheduler(journal)
    val blockingController: FakeBlockingController = FakeBlockingController(journal)
    val ringingController: FakeRingingController = FakeRingingController(journal)
    val ringingWatchdog: FakeRingingWatchdog = FakeRingingWatchdog(journal)
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
    val incidentsReader = RecordingSessionIncidentsReader()
    val readinessMonitor =
        SessionReadinessMonitor(
            readinessChecker = AndroidDeviceReadinessChecker(readinessSources.build(), clock, ANDROID_16),
            warningNotifier = warningNotifier,
            eventFactory = eventFactory,
            technicalEventLog = technicalEventLog,
            incidentsReader = incidentsReader,
        )

    /**
     * Le lecteur d'incidents du réconciliateur est branché sur la passerelle, pas alimenté à la
     * main : la déduplication des incidents de changement d'heure doit se juger sur ce qui a
     * réellement été écrit.
     */
    val reconcilerIncidentsReader = GatewayIncidentsReader(gateway)

    /** [StorageIntegrityState] (étape 20) : ce que l'accueil et l'écran 12 lisent. */
    val storageIntegrity = StorageIntegrityState()

    /** Versement du journal technique d'avant déverrouillage (étape 20). */
    val technicalEventFlush = FakeTechnicalEventLogFlush(journal)

    /** [SessionRuntimeReconciler] (étape 20) : les deux écarts hors du périmètre du moniteur. */
    val runtimeStatusProbe = FakeSessionRuntimeStatusProbe(alarmScheduler)
    val runtimeReconciler =
        SessionRuntimeReconciler(
            runtimeStatusProbe,
            alarmScheduler,
            eventFactory,
            reconcilerIncidentsReader,
            technicalEventLog,
        )

    /**
     * Fusion Direct Boot → Room inerte par défaut : [unlockState] est verrouillé, donc
     * `DirectBootMerger.merge()` sort avant de toucher quoi que ce soit. Les scénarios qui la
     * veulent active passent par `DirectBootMergerTest`, qui monte ses propres doubles.
     */
    val unlockState = FakeUnlockState(isUserUnlocked = false)
    val directBootStore = InMemoryDirectBootStore()
    val roomSessionStore = InMemorySessionStore()
    val roomMerge = RecordingDirectBootRoomMerge()
    val directBootMerger = DirectBootMerger(unlockState, directBootStore, roomSessionStore, roomMerge)

    private val sources =
        ReconcilerSources(
            alarmScheduler = alarmScheduler,
            blockingStartScheduler = blockingStartScheduler,
            blockedPackagesProjection = blockedPackagesProjection,
            readinessMonitor = readinessMonitor,
            snapshotPublisher = publisher,
            ringingController = ringingController,
            scanRequestNotifier = scanRequestNotifier,
            directBootMerger = directBootMerger,
            incidentsReader = reconcilerIncidentsReader,
            ringingWatchdog = ringingWatchdog,
            runtimeReconciler = runtimeReconciler,
            storageIntegrity = storageIntegrity,
            technicalEventFlush = technicalEventFlush,
        )

    private val executors: Map<SessionEffectKindDto, EffectExecutor> =
        mapOf(
            SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to PublishSnapshotExecutor(publisher, technicalEventLog),
            SessionEffectKindDto.SCHEDULE_ALARM to ScheduleAlarmExecutor(alarmScheduler, technicalEventLog),
            SessionEffectKindDto.CANCEL_ALARM to CancelAlarmExecutor(alarmScheduler),
            SessionEffectKindDto.APPLY_BLOCKING to ApplyBlockingExecutor(blockingController, technicalEventLog),
            SessionEffectKindDto.REMOVE_BLOCKING to RemoveBlockingExecutor(blockingController, clock),
            SessionEffectKindDto.START_RINGING to
                StartRingingExecutor(ringingController, ringingWatchdog, technicalEventLog),
            SessionEffectKindDto.STOP_RINGING to StopRingingExecutor(ringingController, ringingWatchdog),
            SessionEffectKindDto.PRESENT_SCAN_REQUEST to
                PresentScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_SCAN_REQUEST to ClearScanRequestExecutor(scanRequestNotifier, technicalEventLog),
            SessionEffectKindDto.CLEAR_ACTIVE_SESSION to ClearActiveSessionExecutor(gateway),
            SessionEffectKindDto.SCHEDULE_BLOCKING_START to
                ScheduleBlockingStartExecutor(blockingStartScheduler, technicalEventLog),
            SessionEffectKindDto.CANCEL_BLOCKING_START to CancelBlockingStartExecutor(blockingStartScheduler),
            SessionEffectKindDto.RECORD_INCIDENT to RecordIncidentExecutor(gateway),
        )

    val effectDispatcher = EffectDispatcher(executors, gateway)

    val reconciler = newReconciler(gateway, effectDispatcher)

    val coordinator: SessionCoordinator =
        DefaultSessionCoordinator(
            recordingReducer,
            gateway,
            effectDispatcher,
            reconciler,
            eventFactory,
            technicalEventLog,
        )

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
        val recon = newReconciler(gateway, dispatcher)
        return DefaultSessionCoordinator(
            recordingReducer,
            gateway,
            dispatcher,
            recon,
            eventFactory,
            technicalEventLog,
        )
    }

    /** Variante avec une [SessionPersistenceGateway] de substitution (`SessionCoordinatorMutexTest`). */
    fun coordinatorWith(customGateway: SessionPersistenceGateway): SessionCoordinator {
        val customDispatcher = EffectDispatcher(executors, customGateway)
        val customReconciler = newReconciler(customGateway, customDispatcher)
        return DefaultSessionCoordinator(
            recordingReducer,
            customGateway,
            customDispatcher,
            customReconciler,
            eventFactory,
            technicalEventLog,
        )
    }

    private fun newReconciler(
        gateway: SessionPersistenceGateway,
        dispatcher: EffectDispatcher,
    ): SessionReconciler = SessionReconciler(gateway, dispatcher, sources, facade, eventFactory, technicalEventLog)
}
