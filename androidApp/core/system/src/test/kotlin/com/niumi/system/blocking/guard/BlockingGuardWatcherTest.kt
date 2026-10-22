package com.niumi.system.blocking.guard

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.fakes.FakeTechnicalEventLog
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §12.5 : le service de garde suit les snapshots publiés, une seule fois par
 * transition, et un démarrage refusé par Android est journalisé puis rejoué.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockingGuardWatcherTest {
    private val publisher = SessionSnapshotPublisher()
    private val controller = FakeBlockingGuardController()
    private val log = FakeTechnicalEventLog()
    private val dispatcher = StandardTestDispatcher()
    private val watcher = BlockingGuardWatcher(publisher, controller, log, dispatcher)

    private val pending = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null)
    private val applied = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L)

    @Test
    fun nothingHappensBeforeTheFirstRead() =
        observing {
            assertThat(controller.calls).isEmpty()
        }

    @Test
    fun theGuardRunsFromArmingToTheEndOfTheSession() =
        observing {
            publish(pending)
            publish(applied)
            publish(applied.copy(state = SessionStateDto.RINGING, revision = 3))
            publish(applied.copy(state = SessionStateDto.RELEASING, revision = 4))
            publish(applied.copy(state = SessionStateDto.COMPLETED, revision = 5))

            // Armement, coucher, alarme, scan (chaque phase change la notification ou la laisse en
            // place, décisions du 2026-10-22), puis fin de session.
            assertThat(controller.calls).containsExactly("keep", "keep", "keep", "keep", "release").inOrder()
            assertThat(controller.phases)
                .containsExactly(
                    BlockingGuardPhase.BEFORE_BLOCKING,
                    BlockingGuardPhase.BLOCKING,
                    BlockingGuardPhase.RINGING,
                    BlockingGuardPhase.RELEASING,
                ).inOrder()
            // Un seul démarrage journalisé : chaque phase remplace la notification d'un service
            // déjà actif.
            assertThat(log.logged)
                .containsExactly(TechnicalEventType.BLOCKING_GUARD_STARTED, TechnicalEventType.BLOCKING_GUARD_STOPPED)
                .inOrder()
        }

    /** Une nouvelle révision de la même phase ne relance pas le service. */
    @Test
    fun aNewRevisionOfTheSameBlockingDoesNotRestartTheGuard() =
        observing {
            publish(applied)
            publish(applied.copy(revision = applied.revision + 1))

            assertThat(controller.calls).containsExactly("keep")
        }

    /** Au démarrage d'un processus sans blocage, l'arrêt d'un service absent n'est pas journalisé. */
    @Test
    fun releasingAGuardThatIsNotRunningIsNotLogged() =
        observing {
            publish(SessionDtoFixtures.snapshotInState(SessionStateDto.COMPLETED))

            assertThat(controller.calls).containsExactly("release")
            assertThat(log.logged).isEmpty()
        }

    @Test
    fun aRefusedStartIsLoggedThenRetriedByTheNextDecision() =
        observing {
            controller.keepResult = OperationResult.Failure("FGS_START_NOT_ALLOWED")
            publish(applied)
            controller.keepResult = OperationResult.Success
            publish(SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING, revision = 3))

            assertThat(controller.calls).containsExactly("keep", "keep").inOrder()
            assertThat(log.logged)
                .containsExactly(
                    TechnicalEventType.BLOCKING_GUARD_START_REFUSED,
                    TechnicalEventType.BLOCKING_GUARD_STARTED,
                ).inOrder()
        }

    @Test
    fun aRefusedStartIsRetriedWhenNiumiComesToTheForeground() =
        observing {
            controller.keepResult = OperationResult.Failure("FGS_START_NOT_ALLOWED")
            publish(applied)
            controller.keepResult = OperationResult.Success

            watcher.retryRefused()
            watcher.retryRefused()

            assertThat(controller.calls).containsExactly("keep", "keep").inOrder()
        }

    @Test
    fun retryingWithoutARefusalDoesNothing() =
        observing {
            publish(applied)

            watcher.retryRefused()

            assertThat(controller.calls).containsExactly("keep")
        }

    private fun observing(block: TestScope.() -> Unit) =
        runTest(dispatcher) {
            val job = launch { watcher.observe() }
            runCurrent()
            block()
            job.cancel()
        }

    private fun TestScope.publish(snapshot: SessionSnapshotDto) {
        publisher.publish(snapshot)
        runCurrent()
    }

    /** Comme le vrai contrôleur : arrêter un service qui ne tourne pas rend `AlreadySatisfied`. */
    private class FakeBlockingGuardController : BlockingGuardController {
        val calls = mutableListOf<String>()
        val phases = mutableListOf<BlockingGuardPhase>()
        var keepResult: OperationResult = OperationResult.Success
        private var running = false

        override fun keep(command: BlockingGuardCommand): OperationResult {
            calls += "keep"
            phases += command.phase
            if (keepResult !is OperationResult.Failure) running = true
            return keepResult
        }

        override fun release(): OperationResult {
            calls += "release"
            val wasRunning = running
            running = false
            return if (wasRunning) OperationResult.Success else OperationResult.AlreadySatisfied
        }
    }
}
