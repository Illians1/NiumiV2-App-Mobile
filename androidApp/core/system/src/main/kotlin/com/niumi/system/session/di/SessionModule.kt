package com.niumi.system.session.di

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.SessionStore
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.common.Clock
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.common.IdGenerator
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.session.DefaultSessionCoordinator
import com.niumi.system.session.DefaultSessionRuntimeStatusProbe
import com.niumi.system.session.EffectDispatcher
import com.niumi.system.session.EffectExecutor
import com.niumi.system.session.FacadeSessionReducer
import com.niumi.system.session.ReconcilerSources
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionReconciler
import com.niumi.system.session.SessionReducer
import com.niumi.system.session.SessionRuntimeStatusProbe
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.SessionStartupReconciler
import com.niumi.system.session.UnlockAwarePersistenceGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Bindings du coordinateur (étape 11). Un seul `@Provides` par pièce plutôt que des classes
 * `@Inject constructor` : convention déjà suivie par [com.niumi.system.di.SystemModule] et
 * [com.niumi.system.di.AudioModule]. La table des exécuteurs (12ᵉ fonction potentielle) vit dans
 * [EffectExecutorModule] : `TooManyFunctions` de detekt (11) exige de scinder par responsabilité.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideNiumiCoreFacade(): NiumiCoreFacade = NiumiCoreFacade()

    @Provides
    @Singleton
    fun provideSessionSnapshotPublisher(): SessionSnapshotPublisher = SessionSnapshotPublisher()

    @Provides
    fun provideSessionReducer(facade: NiumiCoreFacade): SessionReducer = FacadeSessionReducer(facade)

    @Provides
    fun provideSessionEventFactory(
        idGenerator: IdGenerator,
        clock: Clock,
    ): SessionEventFactory = SessionEventFactory(idGenerator, clock)

    @Provides
    @Singleton
    fun provideSessionPersistenceGateway(
        sessionStore: SessionStore,
        directBootStore: DirectBootStore,
        unlockState: UnlockState,
    ): SessionPersistenceGateway = UnlockAwarePersistenceGateway(sessionStore, directBootStore, unlockState)

    /**
     * `@JvmSuppressWildcards` : Kotlin compile ce paramètre en
     * `Map<SessionEffectKind, ? extends EffectExecutor>`, que Dagger considère comme un type
     * distinct de la `Map<SessionEffectKind, EffectExecutor>` fournie par [EffectExecutorModule].
     * Le défaut datait de l'étape 11 mais restait invisible : aucun composant de production ne
     * demandait encore `SessionCoordinator`, donc Dagger ne résolvait jamais cette branche.
     * L'étape 12 est la première à l'injecter (`SessionReadinessWatcher`).
     */
    @Provides
    fun provideEffectDispatcher(
        executors: Map<SessionEffectKindDto, @JvmSuppressWildcards EffectExecutor>,
        gateway: SessionPersistenceGateway,
    ): EffectDispatcher = EffectDispatcher(executors, gateway)

    @Provides
    fun provideReconcilerSources(
        alarmScheduler: AlarmScheduler,
        blockedPackagesProjection: BlockedPackagesProjection,
        readinessMonitor: SessionReadinessMonitor,
        snapshotPublisher: SessionSnapshotPublisher,
    ): ReconcilerSources =
        ReconcilerSources(alarmScheduler, blockedPackagesProjection, readinessMonitor, snapshotPublisher)

    @Provides
    fun provideSessionReconciler(
        gateway: SessionPersistenceGateway,
        effectDispatcher: EffectDispatcher,
        sources: ReconcilerSources,
        facade: NiumiCoreFacade,
        eventFactory: SessionEventFactory,
        technicalEventLog: TechnicalEventLog,
    ): SessionReconciler =
        SessionReconciler(gateway, effectDispatcher, sources, facade, eventFactory, technicalEventLog)

    @Provides
    @Singleton
    fun provideSessionCoordinator(
        reducer: SessionReducer,
        gateway: SessionPersistenceGateway,
        effectDispatcher: EffectDispatcher,
        reconciler: SessionReconciler,
        eventFactory: SessionEventFactory,
    ): SessionCoordinator = DefaultSessionCoordinator(reducer, gateway, effectDispatcher, reconciler, eventFactory)

    @Provides
    @Singleton
    fun provideSessionStartupReconciler(
        coordinator: SessionCoordinator,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): SessionStartupReconciler = SessionStartupReconciler(coordinator, dispatcher)

    @Provides
    fun provideSessionRuntimeStatusProbe(
        alarmScheduler: AlarmScheduler,
        accessibilityServiceStatus: AccessibilityServiceStatus,
        notificationAvailability: NotificationAvailability,
        nfcReader: NfcReader,
        alarmVolumeSource: AlarmVolumeSource,
    ): SessionRuntimeStatusProbe =
        DefaultSessionRuntimeStatusProbe(
            alarmScheduler,
            accessibilityServiceStatus,
            notificationAvailability,
            nfcReader,
            alarmVolumeSource,
        )
}
