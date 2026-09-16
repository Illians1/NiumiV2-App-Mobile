package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.BlockedPackage
import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.system.blocking.fakes.RecordingBlockedPackagesSource
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

/**
 * Réalignement de la projection sur la persistance (SPEC_ANDROID §12.2 : « le service doit
 * recharger l'état actif depuis Room ou le snapshot après recréation »). Les deux déclencheurs
 * sont testés : la connexion du service et chaque décision publiée.
 */
class BlockingProjectionRefresherTest {
    private val packages = setOf(BlockedPackage("com.exemple.jeu", "Jeu"))
    private val source = RecordingBlockedPackagesSource()
    private val projection = PersistedBlockedPackagesProjection(source)
    private val publisher = SessionSnapshotPublisher()
    private val refresher = BlockingProjectionRefresher(projection, publisher)

    @Test
    fun refreshNowRebuildsTheProjectionFromThePersistence() =
        runTest {
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Active(SESSION_ID, packages))

            refresher.refreshNow()

            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
        }

    /**
     * La collecte d'un `StateFlow` émet d'abord sa valeur courante : s'abonner suffit donc à
     * réaligner une première fois, sans attendre une décision.
     */
    @Test
    fun subscribingAlreadyTriggersAFirstRefresh() =
        runTest(UnconfinedTestDispatcher()) {
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Active(SESSION_ID, packages))

            val job = launchObservation()

            assertThat(source.reads).isEqualTo(1)
            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
            job.cancel()
        }

    @Test
    fun everyPublishedDecisionTriggersARefresh() =
        runTest(UnconfinedTestDispatcher()) {
            val job = launchObservation()
            val readsAfterSubscription = source.reads

            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID))
            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.RELEASING, SESSION_ID, revision = 2))

            assertThat(source.reads).isEqualTo(readsAfterSubscription + 2)
            job.cancel()
        }

    @Test
    fun anUnreadablePersistenceLeavesTheBlockingInPlaceAcrossDecisions() =
        runTest(UnconfinedTestDispatcher()) {
            projection.apply(SESSION_ID, packages)
            source.next = BlockedPackagesRead.Unreadable("json")

            val job = launchObservation()
            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID))

            assertThat(projection.current()).isEqualTo(BlockedPackagesState.Active(SESSION_ID, packages))
            job.cancel()
        }

    /**
     * SPEC_ANDROID §12.4 (Lot 6) : au début d'un blocage différé, la projection passe d'inactive à
     * active et le service doit rejouer sa décision sur la dernière application vue.
     */
    @Test
    fun theListenerIsCalledOnceWhenTheProjectionBecomesActive() =
        runTest(UnconfinedTestDispatcher()) {
            val activations = mutableListOf<BlockedPackagesState.Active>()
            val job = launchObservation { activations += it }
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Active(SESSION_ID, packages))

            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID))

            assertThat(activations).hasSize(1)
            assertThat(activations.single().packages).isEqualTo(packages)
            job.cancel()
        }

    /**
     * Chaque appel peut déclencher un `GLOBAL_ACTION_HOME` : une projection restée active ne doit
     * jamais en provoquer un second, même si sa liste de paquets change.
     */
    @Test
    fun theListenerIsNotCalledAgainWhileTheProjectionStaysActive() =
        runTest(UnconfinedTestDispatcher()) {
            val activations = mutableListOf<BlockedPackagesState.Active>()
            val job = launchObservation { activations += it }
            source.next = BlockedPackagesRead.Resolved(BlockedPackagesState.Active(SESSION_ID, packages))
            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID))

            source.next =
                BlockedPackagesRead.Resolved(
                    BlockedPackagesState.Active(SESSION_ID, packages + BlockedPackage("com.exemple.autre", "Autre")),
                )
            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID, revision = 2))

            assertThat(activations).hasSize(1)
            job.cancel()
        }

    /** Une persistance illisible ne lève pas le blocage (§13) et n'est donc pas une activation. */
    @Test
    fun anUnreadablePersistenceNeverNotifiesAnActivation() =
        runTest(UnconfinedTestDispatcher()) {
            val activations = mutableListOf<BlockedPackagesState.Active>()
            val job = launchObservation { activations += it }
            source.next = BlockedPackagesRead.Unreadable("json")

            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, SESSION_ID))

            assertThat(activations).isEmpty()
            job.cancel()
        }

    /**
     * L'autre chemin de la même transition : `APPLY_BLOCKING` avance le cache sans attendre le
     * rafraîchissement suivant. Les deux arrivent dans la décision `BLOCKING_START_ELAPSED`, et
     * n'observer que le rafraîchissement laisserait une course décider si l'application déjà ouverte
     * est renvoyée à l'accueil.
     */
    @Test
    fun applyingTheBlockingDirectlyAlsoNotifiesTheActivation() =
        runTest(UnconfinedTestDispatcher()) {
            val activations = mutableListOf<BlockedPackagesState.Active>()
            val job = launchObservation { activations += it }

            projection.apply(SESSION_ID, packages)

            assertThat(activations).hasSize(1)
            job.cancel()
        }

    private fun TestScope.launchObservation(listener: BlockingActivationListener? = null): Job =
        launch { refresher.observeDecisions(listener) }
}
