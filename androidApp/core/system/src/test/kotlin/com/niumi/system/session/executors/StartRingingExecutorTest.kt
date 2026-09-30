package com.niumi.system.session.executors

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.FakeRingingController
import com.niumi.system.session.fakes.FakeRingingWatchdog
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * `RINGING_STARTED` n'a qu'un producteur : le service de sonnerie, qui seul sait si le son a
 * démarré (SPEC_ANDROID §17). L'exécuteur ne fait que lancer le service et armer le chien de garde ;
 * il journalisait aussi l'événement, soit deux pour une seule sonnerie (mesuré le 2026-09-29).
 * Il ne reçoit plus de journal : l'absence est garantie par construction.
 */
class StartRingingExecutorTest {
    private val ringingController = FakeRingingController()
    private val ringingWatchdog = FakeRingingWatchdog()
    private val executor = StartRingingExecutor(ringingController, ringingWatchdog)
    private val ringing = SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING)

    @Test
    fun launchingTheServiceArmsTheWatchdog() =
        runTest {
            val outcome = executor.execute(startRinging(), ringing, SessionDtoFixtures.extras())

            assertThat(outcome.result).isEqualTo(OperationResult.Success)
            assertThat(ringingWatchdog.armed).containsExactly(SessionDtoFixtures.SESSION_ID)
        }

    @Test
    fun aServiceThatCannotBeLaunchedLeavesTheWatchdogUnarmed() =
        runTest {
            ringingController.startResult = OperationResult.Failure("TEST")

            executor.execute(startRinging(), ringing, SessionDtoFixtures.extras())

            assertThat(ringingWatchdog.armed).isEmpty()
        }

    private fun startRinging() =
        PendingEffect(
            effectId = "${SessionDtoFixtures.SESSION_ID}:3:START_RINGING:1",
            sessionId = SessionDtoFixtures.SESSION_ID,
            revision = 3,
            kind = SessionEffectKindDto.START_RINGING,
            ordinal = 1,
            payloadJson = null,
            status = EffectStatus.PENDING,
            lastError = null,
        )
}
