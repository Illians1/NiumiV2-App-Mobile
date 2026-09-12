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

    private fun TestScope.launchObservation(): Job = launch { refresher.observeDecisions() }
}
