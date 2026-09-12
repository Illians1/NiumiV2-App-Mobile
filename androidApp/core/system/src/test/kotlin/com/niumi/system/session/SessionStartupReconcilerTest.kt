package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §9.2 (dernier alinéa) : au démarrage du processus, la réconciliation reprend toute
 * transaction restée incomplète. Le déclencheur est trivial, mais son absence est précisément le
 * défaut mesuré sur appareil à l'étape 14 : `ReconcileReason.PROCESS_START` existait depuis
 * l'étape 11 sans que rien ne l'émette.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionStartupReconcilerTest {
    private class RecordingCoordinator : SessionCoordinator {
        val reasons = mutableListOf<ReconcileReason>()

        override suspend fun dispatch(
            event: SessionEventDto,
            extras: AndroidSessionExtras?,
        ): DispatchResult = throw UnsupportedOperationException("Non utilisé au démarrage")

        override suspend fun reconcile(reason: ReconcileReason): ReconcileResult {
            reasons += reason
            return ReconcileResult(sessionId = null, actions = emptyList())
        }
    }

    @Test
    fun reconcileAsyncTriggersExactlyOneProcessStartPass() =
        runTest {
            val coordinator = RecordingCoordinator()
            val startup = SessionStartupReconciler(coordinator, StandardTestDispatcher(testScheduler))

            startup.reconcileAsync()
            advanceUntilIdle()

            assertThat(coordinator.reasons).containsExactly(ReconcileReason.PROCESS_START)
        }
}
