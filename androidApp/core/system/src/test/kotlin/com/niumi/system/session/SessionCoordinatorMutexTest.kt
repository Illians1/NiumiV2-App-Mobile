package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.StoredDecision
import com.niumi.system.session.fakes.CallJournal
import com.niumi.system.session.fakes.InMemoryPersistenceGateway
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

/**
 * `dispatch` et `reconcile` partagent un seul `Mutex` (SPEC_CORE_KMP §6.1, §11.3 : `AlarmReceiver`,
 * `SessionReconciler` et `HandleValidNfcUseCase` s'exécutent sous le même mutex). [YieldingGateway]
 * force un vrai point de suspension **à l'intérieur** de la section critique (juste avant
 * `commit`) : sans le `Mutex`, une seconde coroutine dispatchant le même événement aurait une
 * fenêtre réelle pour passer la vérification de doublon avant que la première n'ait committé,
 * produisant deux `Applied` au lieu d'un `Applied` et un `Duplicate`.
 */
class SessionCoordinatorMutexTest {
    private class YieldingGateway(
        private val delegate: SessionPersistenceGateway,
    ) : SessionPersistenceGateway by delegate {
        override suspend fun commit(decision: StoredDecision) {
            yield()
            delegate.commit(decision)
        }
    }

    @Test
    fun concurrentDispatchOfTheSameEventIsSerializedIntoOneAppliedAndOneDuplicate() =
        runTest {
            val harness = TestCoordinatorHarness()
            val yieldingGateway = YieldingGateway(InMemoryPersistenceGateway(CallJournal()))
            val coordinator = harness.coordinatorWith(yieldingGateway)
            val event = SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001")

            val first = async { coordinator.dispatch(event, SessionDtoFixtures.extras()) }
            val second = async { coordinator.dispatch(event, SessionDtoFixtures.extras()) }
            val results = listOf(first.await(), second.await())

            assertThat(results.count { it is DispatchResult.Applied }).isEqualTo(1)
            assertThat(results.count { it is DispatchResult.Duplicate }).isEqualTo(1)
        }

    /**
     * Étape 44 : `withLock` prend le **même** verrou. Pendant qu'un bloc le tient, un `dispatch`
     * concurrent attend : il ne peut pas s'intercaler entre le désarmement et le réarmement du
     * planning (point de vigilance 8 du plan de refonte).
     */
    @Test
    fun aDispatchWaitsForAWithLockBlockToFinish() =
        runTest {
            val harness = TestCoordinatorHarness()
            val order = mutableListOf<String>()

            val locked =
                async {
                    harness.coordinator.withLock {
                        order += "lock:start"
                        yield()
                        order += "lock:end"
                    }
                }
            val dispatched =
                async {
                    harness.coordinator.dispatch(
                        SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                        SessionDtoFixtures.extras(),
                    )
                    order += "dispatch"
                }
            locked.await()
            dispatched.await()

            assertThat(order).containsExactly("lock:start", "lock:end", "dispatch").inOrder()
        }

    /** Le verrou n'est pas réentrant : `LockedSession.dispatch` est la version qui ne le reprend pas. */
    @Test
    fun aLockedSessionDispatchesSeveralEventsWithoutDeadlock() =
        runTest {
            val harness = TestCoordinatorHarness()

            val states =
                harness.coordinator.withLock { session ->
                    val armed =
                        session.dispatch(
                            SessionDtoFixtures.activationRequested(
                                eventId = "00000000-0000-0000-0000-000000000001",
                                blockingStartsAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
                            ),
                            SessionDtoFixtures.extras(),
                        ) as DispatchResult.Applied
                    val disarmed =
                        session.dispatch(
                            harness.eventFactory.disarmRequested(armed.snapshot!!),
                        ) as DispatchResult.Applied
                    listOf(armed.snapshot.state, disarmed.snapshot!!.state, session.load() as? LoadResult.Present)
                }

            assertThat(states)
                .containsExactly(SessionStateDto.ARMED, SessionStateDto.DISARMED, null)
                .inOrder()
        }
}
